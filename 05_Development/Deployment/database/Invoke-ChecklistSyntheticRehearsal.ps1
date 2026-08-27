[CmdletBinding()]
param(
    [string]$PostgresBinDirectory = $(
        if ([string]::IsNullOrWhiteSpace($env:CAREBRIDGE_PSQL_PATH)) { '' }
        else { Split-Path -Parent $env:CAREBRIDGE_PSQL_PATH }
    ),
    [string]$WorkspaceRoot,
    [Parameter(Mandatory)]
    [string]$EvidenceDirectory,
    [string]$ScratchRoot = 'D:\tmp',
    [string]$MavenPath = 'mvn.cmd',
    [string]$MavenLocalRepository = $(Join-Path $env:USERPROFILE '.m2\repository'),
    [string]$ExpectedBaseFlywayVersion = '20260730050000',
    [string]$ExpectedCorrectedFlywayVersion = '20260730060000',
    [ValidateRange(0, [long]::MaxValue)]
    [long]$MaxUnresolvedQuarantineCount = 2,
    [ValidateRange(0.0, 100.0)]
    [double]$MaxLegacyQuarantineRatePercent = 0.02,
    [ValidateRange(0.001, [double]::MaxValue)]
    [double]$MaxLockSeconds = 5.0,
    [ValidateRange(0.001, [double]::MaxValue)]
    [double]$MinBackfillRowsPerSecond = 500.0,
    [ValidateRange(0.001, [double]::MaxValue)]
    [double]$MaxFullMigrationSeconds = 1800.0
)

$ErrorActionPreference = 'Stop'
$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($WorkspaceRoot)) {
    $WorkspaceRoot = Join-Path $scriptDirectory '..\..\..'
}
$requiredPostgresMajor = 18
$operatorTest = 'ChecklistSyntheticRehearsalExternalPostgresTest'
$verifierScript = Join-Path $scriptDirectory 'Invoke-ChecklistMigrationRehearsal.ps1'
$backendDirectory = Join-Path $WorkspaceRoot '05_Development\CareBridgeAPI'
$script:postgresStarted = $false
$script:runRoot = $null
$script:dataDirectory = $null
$script:sentinelPath = $null
$script:sentinelSha256 = $null
$script:pgpassFile = $null
$script:cleanupSentinelPath = $null
$script:cleanupSentinelSha256 = $null

function Resolve-FullPath([string]$Path, [string]$ErrorCode) {
    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw $ErrorCode
    }
    try {
        return [System.IO.Path]::GetFullPath($Path).TrimEnd('\')
    }
    catch {
        throw $ErrorCode
    }
}

function Test-IsWithin([string]$Candidate, [string]$Parent) {
    $candidateFull = Resolve-FullPath $Candidate 'CHECKLIST_SYNTHETIC_PATH_INVALID'
    $parentFull = Resolve-FullPath $Parent 'CHECKLIST_SYNTHETIC_PATH_INVALID'
    return $candidateFull.Equals($parentFull, [StringComparison]::OrdinalIgnoreCase) -or
        $candidateFull.StartsWith($parentFull + '\', [StringComparison]::OrdinalIgnoreCase)
}

function Assert-PathOutsideRepository([string]$Path) {
    if (Test-IsWithin $Path $script:workspaceRoot) {
        throw 'CHECKLIST_SYNTHETIC_RUNTIME_PATH_INSIDE_REPOSITORY_FORBIDDEN'
    }
}

function Assert-NotReparsePoint([string]$Path, [string]$ErrorCode) {
    $item = Get-Item -LiteralPath $Path -Force
    if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw $ErrorCode
    }
}

function Write-SanitizedJson([string]$Path, [object]$Value) {
    $json = $Value | ConvertTo-Json -Depth 20
    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $json, $utf8WithoutBom)
}

function Write-ProgressMarker([string]$Stage) {
    $line = 'CHK041_PROGRESS {0} {1}' -f [DateTimeOffset]::UtcNow.ToString('o'), $Stage
    if (-not [string]::IsNullOrWhiteSpace($script:internalEvidenceRoot) -and
            (Test-Path -LiteralPath $script:internalEvidenceRoot -PathType Container)) {
        Add-Content -LiteralPath (Join-Path $script:internalEvidenceRoot 'runner-progress.log') `
            -Value $line -Encoding utf8
    }
    Write-Host $line
}

function New-RandomSecret {
    $bytes = New-Object byte[] 36
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }
    return 'A1!' + [Convert]::ToBase64String($bytes).Replace('+', 'x').Replace('/', 'y').TrimEnd('=')
}

function Set-ProtectedCredentialFile([string]$Path, [string]$Content) {
    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8WithoutBom)
    $sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
    & icacls.exe $Path '/inheritance:r' '/grant:r' "*$($sid.Value):(F)" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'CHECKLIST_SYNTHETIC_CREDENTIAL_ACL_FAILED'
    }
    $security = Get-Acl -LiteralPath $Path
    $unexpectedAllow = @($security.Access | Where-Object {
        $_.AccessControlType -eq [System.Security.AccessControl.AccessControlType]::Allow -and
        $_.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value -ne $sid.Value
    })
    if (-not $security.AreAccessRulesProtected -or $unexpectedAllow.Count -gt 0) {
        throw 'CHECKLIST_SYNTHETIC_CREDENTIAL_ACL_TOO_BROAD'
    }
}

function Resolve-PostgresBinary([string]$Name) {
    $path = Join-Path $script:postgresBinDirectory $Name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "CHECKLIST_SYNTHETIC_POSTGRES_BINARY_REQUIRED:$Name"
    }
    Assert-NotReparsePoint $path 'CHECKLIST_SYNTHETIC_POSTGRES_BINARY_REPARSE_FORBIDDEN'
    $resolved = (Resolve-Path -LiteralPath $path).Path
    $versionOutput = & $resolved '--version' 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0 -or $versionOutput -notmatch "(?i)PostgreSQL\)\s+$requiredPostgresMajor(?:\.|\s)") {
        throw 'CHECKLIST_SYNTHETIC_POSTGRES_18_REQUIRED'
    }
    return $resolved
}

function Get-FreeLoopbackPort {
    $listener = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback, 0)
    try {
        $listener.Start()
        return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    }
    finally {
        $listener.Stop()
    }
}

function Test-LoopbackPortClosed([int]$Port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $attempt = $client.ConnectAsync([System.Net.IPAddress]::Loopback, $Port)
        if (-not $attempt.Wait(750)) {
            return $true
        }
        return -not $client.Connected
    }
    catch {
        return $true
    }
    finally {
        $client.Dispose()
    }
}

function Invoke-CheckedNative(
    [string]$Executable,
    [string[]]$Arguments,
    [string]$ErrorCode,
    [string]$LogPath,
    [int]$TimeoutSeconds = 600
) {
    $captureRoot = if (-not [string]::IsNullOrWhiteSpace($script:credentialDirectory) -and
            (Test-Path -LiteralPath $script:credentialDirectory -PathType Container)) {
        $script:credentialDirectory
    } else { [System.IO.Path]::GetTempPath() }
    $captureId = [Guid]::NewGuid().ToString('N')
    $stdoutPath = Join-Path $captureRoot "$captureId.stdout.log"
    $stderrPath = Join-Path $captureRoot "$captureId.stderr.log"
    $process = $null
    try {
        $process = Start-Process -FilePath $Executable -ArgumentList $Arguments -PassThru `
            -WindowStyle Hidden -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            throw "$ErrorCode`:TIMEOUT"
        }
        # Windows PowerShell 5.1 may not populate ExitCode/redirect buffers after the
        # timed overload alone. The parameterless wait is non-blocking now and flushes both.
        $process.WaitForExit()
        $process.Refresh()
        $exitCode = [int]$process.ExitCode
        $output = @()
        if (Test-Path -LiteralPath $stdoutPath -PathType Leaf) {
            $output += @(Get-Content -LiteralPath $stdoutPath | ForEach-Object { "$_" })
        }
        if (Test-Path -LiteralPath $stderrPath -PathType Leaf) {
            $output += @(Get-Content -LiteralPath $stderrPath | ForEach-Object { "$_" })
        }
        if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
            Add-Content -LiteralPath $LogPath -Value $output -Encoding utf8
        }
        if ($exitCode -ne 0) {
            throw "$ErrorCode`:$exitCode"
        }
        return $output
    }
    finally {
        Remove-Item -LiteralPath $stdoutPath,$stderrPath -Force -ErrorAction SilentlyContinue
        if ($null -ne $process) { $process.Dispose() }
    }
}

function Set-PgpassEntry([string]$Database, [string]$User, [string]$Password) {
    $entry = "127.0.0.1:$script:port`:$Database`:$User`:$Password"
    Set-ProtectedCredentialFile $script:pgpassFile ($entry + [Environment]::NewLine)
    $env:PGPASSFILE = $script:pgpassFile
}

function Invoke-AdminPsqlFile([string]$Database, [string]$SqlFile, [string]$ErrorCode) {
    Set-PgpassEntry $Database 'postgres' $script:bootstrapPassword
    $arguments = @(
        '--no-psqlrc', '--no-password', '--quiet', '--set=ON_ERROR_STOP=on',
        '--host=127.0.0.1', "--port=$script:port", '--username=postgres',
        "--dbname=$Database", '--file', $SqlFile)
    [void](Invoke-CheckedNative $script:psqlPath $arguments $ErrorCode $null)
}

function New-DisposableDatabase([string]$Database) {
    Set-PgpassEntry 'postgres' 'postgres' $script:bootstrapPassword
    $arguments = @(
        '--no-password', '--host=127.0.0.1', "--port=$script:port",
        '--username=postgres', $Database)
    [void](Invoke-CheckedNative $script:createdbPath $arguments `
            'CHECKLIST_SYNTHETIC_CREATE_DATABASE_FAILED' $null)
}

function Grant-ChecklistVerifierReads([string]$Database) {
    $sqlPath = Join-Path $script:credentialDirectory "grant-verifier-$Database.sql"
    Set-ProtectedCredentialFile $sqlPath @'
GRANT SELECT ON public.flyway_schema_history,
    public.preparation_checklist_items,
    public.care_item_templates,
    public.checklist_care_group_contexts,
    public.checklist_instances,
    public.checklist_task_instances,
    public.checklist_migration_quarantine
TO checklist_operations;
'@
    try {
        Invoke-AdminPsqlFile $Database $sqlPath 'CHECKLIST_SYNTHETIC_VERIFIER_GRANT_FAILED'
    }
    finally {
        Remove-Item -LiteralPath $sqlPath -Force -ErrorAction SilentlyContinue
    }
    Write-ProgressMarker "verifier-reads-granted-$Database"
}

function Start-DisposablePostgres {
    New-Item -ItemType Directory -Path $script:dataDirectory | Out-Null
    $bootstrapPasswordFile = Join-Path $script:credentialDirectory 'bootstrap-password.txt'
    Set-ProtectedCredentialFile $bootstrapPasswordFile ($script:bootstrapPassword + [Environment]::NewLine)
    try {
        $arguments = @(
            "--pgdata=$script:dataDirectory", '--username=postgres',
            "--pwfile=$bootstrapPasswordFile", '--auth-host=scram-sha-256',
            '--auth-local=scram-sha-256', '--encoding=UTF8', '--no-locale')
        [void](Invoke-CheckedNative $script:initdbPath $arguments `
                'CHECKLIST_SYNTHETIC_INITDB_FAILED' $null)
    }
    finally {
        Remove-Item -LiteralPath $bootstrapPasswordFile -Force -ErrorAction SilentlyContinue
    }

    $configuration = [Environment]::NewLine + @"
listen_addresses = '127.0.0.1'
port = $script:port
password_encryption = 'scram-sha-256'
ssl = off
"@
    [System.IO.File]::AppendAllText(
        (Join-Path $script:dataDirectory 'postgresql.conf'),
        $configuration,
        (New-Object System.Text.UTF8Encoding($false)))

    $sentinel = [ordered]@{
        schemaVersion = 1
        runId = $script:runId
        workRoot = $script:runRoot
        dataDirectory = $script:dataDirectory
        createdAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
    }
    Write-SanitizedJson $script:sentinelPath $sentinel
    $script:sentinelSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:sentinelPath).Hash.ToLowerInvariant()

    $arguments = @(
        "--pgdata=$script:dataDirectory", '--wait', '--timeout=30',
        '--log', (Join-Path $script:internalEvidenceRoot 'postgres.log'), 'start')
    [void](Invoke-CheckedNative $script:pgCtlPath $arguments `
            'CHECKLIST_SYNTHETIC_POSTGRES_START_FAILED' $null)
    $script:postgresStarted = $true
}

function Stop-DisposablePostgres {
    if (-not $script:postgresStarted) {
        return
    }
    $arguments = @("--pgdata=$script:dataDirectory", '--wait', '--timeout=30', '--mode=fast', 'stop')
    [void](Invoke-CheckedNative $script:pgCtlPath $arguments `
            'CHECKLIST_SYNTHETIC_POSTGRES_STOP_FAILED' $null)
    $script:postgresStarted = $false
}

function Remove-SentinelBoundDirectory {
    if (-not (Test-Path -LiteralPath $script:cleanupSentinelPath -PathType Leaf)) {
        throw 'CHECKLIST_SYNTHETIC_CLEANUP_SENTINEL_MISSING'
    }
    $actualCleanupSentinelSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:cleanupSentinelPath).Hash.ToLowerInvariant()
    if ($actualCleanupSentinelSha256 -ne $script:cleanupSentinelSha256) {
        throw 'CHECKLIST_SYNTHETIC_CLEANUP_SENTINEL_MISMATCH'
    }
    if (Test-Path -LiteralPath $script:sentinelPath -PathType Leaf) {
        $actualSentinelSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:sentinelPath).Hash.ToLowerInvariant()
        if ($actualSentinelSha256 -ne $script:sentinelSha256) {
            throw 'CHECKLIST_SYNTHETIC_CLEANUP_SENTINEL_MISMATCH'
        }
    }
    if (-not (Test-IsWithin $script:dataDirectory $script:runRoot) -or
            -not (Test-IsWithin $script:runRoot 'D:\tmp') -or
            (Test-IsWithin $script:runRoot $script:workspaceRoot)) {
        throw 'CHECKLIST_SYNTHETIC_CLEANUP_PATH_FORBIDDEN'
    }
    Remove-Item -Recurse -Force -LiteralPath $script:runRoot
}

function Invoke-SyntheticOperator([string]$Phase, [string]$Database, [string]$PhaseEvidence) {
    Write-ProgressMarker "operator-$Phase-start"
    New-Item -ItemType Directory -Path $PhaseEvidence -Force | Out-Null
    $phaseRawFlywayLog = Join-Path $PhaseEvidence "$Phase-raw-flyway.log"
    if (-not (Test-Path -LiteralPath $phaseRawFlywayLog -PathType Leaf)) {
        Set-Content -LiteralPath $phaseRawFlywayLog -Value '' -Encoding utf8
    }
    $previous = [ordered]@{
        enabled = $env:CAREBRIDGE_CHK041_OPERATOR_ENABLED
        url = $env:CAREBRIDGE_CHK041_JDBC_URL
        user = $env:CAREBRIDGE_CHK041_JDBC_USER
        password = $env:CAREBRIDGE_CHK041_JDBC_PASSWORD
    }
    try {
        $env:CAREBRIDGE_CHK041_OPERATOR_ENABLED = 'true'
        $env:CAREBRIDGE_CHK041_JDBC_URL = "jdbc:postgresql://127.0.0.1:$script:port/$Database"
        $env:CAREBRIDGE_CHK041_JDBC_USER = 'postgres'
        $env:CAREBRIDGE_CHK041_JDBC_PASSWORD = $script:bootstrapPassword
        $arguments = @(
            '-q', '--offline', "-Dmaven.repo.local=$script:mavenLocalRepository",
            "-Dcarebridge.build.directory=$script:mavenBuildDirectory",
            "-Dtest=$operatorTest", "-Dchecklist.rehearsal.phase=$Phase",
            "-Dchecklist.rehearsal.evidence-dir=$PhaseEvidence", 'test')
        $previousErrorActionPreference = $ErrorActionPreference
        try {
            # Native stderr can contain non-fatal JVM/Mockito warnings. Capture it,
            # then decide strictly from the process exit code and required artifacts.
            $ErrorActionPreference = 'Continue'
            $output = & $MavenPath @arguments 2>&1
            $exitCode = $LASTEXITCODE
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        Add-Content -LiteralPath $phaseRawFlywayLog `
            -Value @($output | ForEach-Object { "$_" }) -Encoding utf8
        if ($exitCode -ne 0) {
            @($output | ForEach-Object { "$_" } | Where-Object {
                -not $_.Contains($script:bootstrapPassword) -and
                -not $_.Contains($script:operationsPassword)
            }) | ForEach-Object { Write-Output "CHK041_OPERATOR_ERROR $_" }
            throw "CHECKLIST_SYNTHETIC_OPERATOR_FAILED:$Phase`:$exitCode"
        }
        Write-ProgressMarker "operator-$Phase-complete"
    }
    finally {
        $env:CAREBRIDGE_CHK041_OPERATOR_ENABLED = $previous.enabled
        $env:CAREBRIDGE_CHK041_JDBC_URL = $previous.url
        $env:CAREBRIDGE_CHK041_JDBC_USER = $previous.user
        $env:CAREBRIDGE_CHK041_JDBC_PASSWORD = $previous.password
    }
}

function New-DisposableAttestation([string]$Database, [string]$Phase) {
    $path = Join-Path $script:internalEvidenceRoot "$Phase-disposable-attestation.json"
    $value = [ordered]@{
        schemaVersion = 1
        runId = $script:runId
        host = '127.0.0.1'
        port = $script:port
        database = $Database
        user = 'checklist_operations'
        workRoot = $script:runRoot
        dataDirectory = $script:dataDirectory
        sentinelPath = $script:sentinelPath
        sentinelSha256 = $script:sentinelSha256
        evidenceDirectory = $script:internalEvidenceRoot
        postgresMajor = $requiredPostgresMajor
        psqlSha256 = $script:psqlSha256
        postgresSha256 = $script:postgresSha256
        initdbSha256 = $script:initdbSha256
        pgCtlSha256 = $script:pgCtlSha256
        pgDumpSha256 = $script:pgDumpSha256
        pgRestoreSha256 = $script:pgRestoreSha256
    }
    Write-SanitizedJson $path $value
    return $path
}

function New-CohortAttestation([string]$Phase, [string]$datasetFingerprint) {
    $path = Join-Path $script:internalEvidenceRoot "$Phase-cohort-attestation.json"
    $value = [ordered]@{
        schemaVersion = 1
        mode = 'approved-local-control-plane-simulation'
        cohortEnabled = $false
        datasetFingerprint = $datasetFingerprint
        disposableRunId = $script:runId
        capturedAtUtc = [DateTime]::UtcNow.ToString(
            'yyyy-MM-ddTHH:mm:ss.fffffffZ',
            [System.Globalization.CultureInfo]::InvariantCulture)
    }
    Write-SanitizedJson $path $value
    return $path
}

function Bind-RawEvidenceToMetrics(
    [string]$MetricsPath,
    [string]$FlywayLogPath,
    [string]$LockLogPath
) {
    if (-not (Test-Path -LiteralPath $MetricsPath -PathType Leaf) -or
            -not (Test-Path -LiteralPath $LockLogPath -PathType Leaf) -or
            -not (Test-Path -LiteralPath $FlywayLogPath -PathType Leaf)) {
        throw 'CHECKLIST_SYNTHETIC_RAW_EVIDENCE_REQUIRED'
    }
    $metrics = Get-Content -Raw -LiteralPath $MetricsPath | ConvertFrom-Json
    $metrics | Add-Member -NotePropertyName rawFlywayLogPath -NotePropertyValue $FlywayLogPath -Force
    $metrics | Add-Member -NotePropertyName rawFlywayLogSha256 -NotePropertyValue `
        (Get-FileHash -Algorithm SHA256 -LiteralPath $FlywayLogPath).Hash.ToLowerInvariant() -Force
    $metrics | Add-Member -NotePropertyName rawLockLogPath -NotePropertyValue $LockLogPath -Force
    $metrics | Add-Member -NotePropertyName rawLockLogSha256 -NotePropertyValue `
        (Get-FileHash -Algorithm SHA256 -LiteralPath $LockLogPath).Hash.ToLowerInvariant() -Force
    Write-SanitizedJson $MetricsPath $metrics
}

function Invoke-RehearsalVerifier(
    [string]$Phase,
    [string]$Database,
    [string]$datasetFingerprint,
    [string]$PreManifestPath,
    [string]$MetricsPath,
    [string]$ExpectedFinalVersion,
    [string]$ExpectedHistorySha256,
    [string]$PreviousAbortArtifact,
    [string]$CorrectionArtifactPath,
    [string]$ReferenceDatasetManifestPath
) {
    Write-ProgressMarker "verifier-$Phase-start"
    Set-PgpassEntry $Database 'checklist_operations' $script:operationsPassword
    $databaseUrl = "postgresql://checklist_operations@127.0.0.1:$script:port/$Database"
    $disposableAttestationPath = New-DisposableAttestation $Database $Phase.ToLowerInvariant()
    $cohortAttestationPath = New-CohortAttestation $Phase.ToLowerInvariant() $datasetFingerprint
    $arguments = @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $verifierScript,
        '-Phase', $Phase, '-PsqlPath', $script:psqlPath, '-DatabaseUrl', $databaseUrl,
        '-EvidenceDirectory', $script:internalEvidenceRoot,
        '-ReferenceDatasetFingerprint', $datasetFingerprint,
        '-ReferenceDatasetManifestPath', $ReferenceDatasetManifestPath,
        '-DisposableAttestationPath', $disposableAttestationPath,
        '-CohortAttestationPath', $cohortAttestationPath,
        '-MaxUnresolvedQuarantineCount', "$MaxUnresolvedQuarantineCount",
        '-MaxLegacyQuarantineRatePercent', "$MaxLegacyQuarantineRatePercent",
        '-MaxLockSeconds', "$MaxLockSeconds",
        '-MinBackfillRowsPerSecond', "$MinBackfillRowsPerSecond",
        '-MaxFullMigrationSeconds', "$MaxFullMigrationSeconds")
    if (-not [string]::IsNullOrWhiteSpace($PreManifestPath)) {
        $arguments += @('-PreManifestPath', $PreManifestPath)
    }
    if (-not [string]::IsNullOrWhiteSpace($MetricsPath)) {
        $arguments += @('-MetricsPath', $MetricsPath)
    }
    if (-not [string]::IsNullOrWhiteSpace($ExpectedFinalVersion)) {
        $arguments += @('-ExpectedFinalFlywayVersion', $ExpectedFinalVersion)
    }
    if (-not [string]::IsNullOrWhiteSpace($ExpectedHistorySha256)) {
        $arguments += @('-ExpectedFlywayHistorySha256', $ExpectedHistorySha256)
    }
    if (-not [string]::IsNullOrWhiteSpace($PreviousAbortArtifact)) {
        $arguments += @(
            '-PreviousAbortArtifact', $PreviousAbortArtifact,
            '-PreviousAbortArtifactSha256',
            (Get-FileHash -Algorithm SHA256 -LiteralPath $PreviousAbortArtifact).Hash.ToLowerInvariant())
    }
    if (-not [string]::IsNullOrWhiteSpace($CorrectionArtifactPath)) {
        $arguments += @(
            '-CorrectionArtifactPath', $CorrectionArtifactPath,
            '-CorrectionArtifactSha256',
            (Get-FileHash -Algorithm SHA256 -LiteralPath $CorrectionArtifactPath).Hash.ToLowerInvariant())
    }
    $output = & powershell.exe @arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "CHECKLIST_SYNTHETIC_VERIFIER_FAILED:$Phase`:$LASTEXITCODE"
    }
    $marker = @($output | ForEach-Object { "$_" } | Where-Object {
        $_ -match '^CHECKLIST_REHEARSAL_(?:PRE_CAPTURED|PASS|ABORT_PROVEN|ROLL_FORWARD_PASS):'
    } | Select-Object -Last 1)
    if ($marker.Count -ne 1) {
        throw "CHECKLIST_SYNTHETIC_VERIFIER_MARKER_MISSING:$Phase"
    }
    Write-ProgressMarker "verifier-$Phase-complete"
    return ($marker[0] -replace '^[^:]+:', '')
}

function Get-RequiredJson([string]$Path, [string]$ErrorCode) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw $ErrorCode
    }
    try {
        return Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
    }
    catch {
        throw $ErrorCode
    }
}

function Assert-NoSecretInEvidence {
    foreach ($file in Get-ChildItem -LiteralPath $script:internalEvidenceRoot -File -Recurse) {
        $content = [System.IO.File]::ReadAllText($file.FullName)
        if ($content.Contains($script:bootstrapPassword) -or
                $content.Contains($script:operationsPassword)) {
            throw 'CHECKLIST_SYNTHETIC_SECRET_LEAK_DETECTED'
        }
    }
}

function Export-SanitizedEvidence {
    $exportRoot = Resolve-FullPath $EvidenceDirectory 'CHECKLIST_SYNTHETIC_EVIDENCE_PATH_INVALID'
    Assert-PathOutsideRepository $exportRoot
    if ((Test-IsWithin $exportRoot $script:runRoot) -or
            (Test-IsWithin $script:runRoot $exportRoot)) {
        throw 'CHECKLIST_SYNTHETIC_EXPORT_INSIDE_DISPOSABLE_ROOT_FORBIDDEN'
    }
    if (Test-Path -LiteralPath $exportRoot) {
        Assert-NotReparsePoint $exportRoot 'CHECKLIST_SYNTHETIC_EXPORT_REPARSE_FORBIDDEN'
        if (@(Get-ChildItem -LiteralPath $exportRoot -Force).Count -ne 0) {
            throw 'CHECKLIST_SYNTHETIC_EXPORT_DIRECTORY_NOT_EMPTY'
        }
    }
    else {
        New-Item -ItemType Directory -Path $exportRoot | Out-Null
    }
    Assert-NotReparsePoint $exportRoot 'CHECKLIST_SYNTHETIC_EXPORT_REPARSE_FORBIDDEN'
    Copy-Item -Path (Join-Path $script:internalEvidenceRoot '*') -Destination $exportRoot -Recurse -Force
    return $exportRoot
}

$script:workspaceRoot = Resolve-FullPath $WorkspaceRoot 'CHECKLIST_SYNTHETIC_WORKSPACE_REQUIRED'
$script:postgresBinDirectory = Resolve-FullPath $PostgresBinDirectory 'CHECKLIST_SYNTHETIC_POSTGRES_BIN_REQUIRED'
$script:scratchRoot = Resolve-FullPath $ScratchRoot 'CHECKLIST_SYNTHETIC_SCRATCH_ROOT_REQUIRED'
$script:mavenLocalRepository = Resolve-FullPath $MavenLocalRepository `
    'CHECKLIST_SYNTHETIC_MAVEN_LOCAL_REPOSITORY_REQUIRED'
Assert-PathOutsideRepository $script:scratchRoot
if (-not (Test-IsWithin $script:scratchRoot 'D:\tmp')) {
    throw 'CHECKLIST_SYNTHETIC_SCRATCH_ROOT_FORBIDDEN'
}
if (-not (Test-Path -LiteralPath $script:workspaceRoot -PathType Container) -or
        -not (Test-Path -LiteralPath $backendDirectory -PathType Container) -or
        -not (Test-Path -LiteralPath $verifierScript -PathType Leaf) -or
        -not (Test-Path -LiteralPath $script:mavenLocalRepository -PathType Container)) {
    throw 'CHECKLIST_SYNTHETIC_REPOSITORY_LAYOUT_INVALID'
}
Assert-NotReparsePoint $script:scratchRoot 'CHECKLIST_SYNTHETIC_SCRATCH_REPARSE_FORBIDDEN'

$script:initdbPath = Resolve-PostgresBinary 'initdb.exe'
$script:pgCtlPath = Resolve-PostgresBinary 'pg_ctl.exe'
$script:psqlPath = Resolve-PostgresBinary 'psql.exe'
$script:createdbPath = Resolve-PostgresBinary 'createdb.exe'
$script:postgresPath = Resolve-PostgresBinary 'postgres.exe'
$script:pgDumpPath = Resolve-PostgresBinary 'pg_dump.exe'
$script:pgRestorePath = Resolve-PostgresBinary 'pg_restore.exe'
$script:psqlSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:psqlPath).Hash.ToLowerInvariant()
$script:postgresSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:postgresPath).Hash.ToLowerInvariant()
$script:initdbSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:initdbPath).Hash.ToLowerInvariant()
$script:pgCtlSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:pgCtlPath).Hash.ToLowerInvariant()
$script:pgDumpSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:pgDumpPath).Hash.ToLowerInvariant()
$script:pgRestoreSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:pgRestorePath).Hash.ToLowerInvariant()
$script:runId = ('chk041-' + [DateTimeOffset]::UtcNow.ToString('yyyyMMddTHHmmssfffZ') + '-' +
    [Guid]::NewGuid().ToString('N')).ToLowerInvariant()
$script:runRoot = Join-Path $script:scratchRoot "carebridge-$script:runId"
$script:mavenBuildDirectory = Join-Path $script:runRoot 'maven-build'
$script:dataDirectory = Join-Path $script:runRoot 'postgres-data'
$script:internalEvidenceRoot = Join-Path $script:runRoot 'evidence'
$script:credentialDirectory = Join-Path $script:runRoot 'credentials'
$script:sentinelPath = Join-Path $script:dataDirectory '.carebridge-chk041-disposable.json'
$script:cleanupSentinelPath = Join-Path $script:runRoot '.carebridge-chk041-cleanup.json'
$script:pgpassFile = Join-Path $script:credentialDirectory 'pgpass.conf'
$script:port = Get-FreeLoopbackPort
$script:bootstrapPassword = New-RandomSecret
$script:operationsPassword = New-RandomSecret

$previousPgpass = $env:PGPASSFILE
$previousPgpassword = $env:PGPASSWORD
$libpqTargetNames = @(
    'PGHOST', 'PGHOSTADDR', 'PGPORT', 'PGDATABASE', 'PGUSER',
    'PGSERVICE', 'PGSERVICEFILE', 'PGOPTIONS', 'PGTARGETSESSIONATTRS')
$previousLibpqTargets = [ordered]@{}
foreach ($name in $libpqTargetNames) {
    $previousLibpqTargets[$name] = [Environment]::GetEnvironmentVariable($name)
    [Environment]::SetEnvironmentVariable($name, $null)
}
Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
$completed = $false
$exportedEvidence = $null
try {
    New-Item -ItemType Directory -Path $script:runRoot | Out-Null
    New-Item -ItemType Directory -Path $script:internalEvidenceRoot | Out-Null
    New-Item -ItemType Directory -Path $script:credentialDirectory | Out-Null
    Write-ProgressMarker 'run-root-created'
    Write-SanitizedJson $script:cleanupSentinelPath ([ordered]@{
        schemaVersion = 1
        runId = $script:runId
        runRoot = $script:runRoot
    })
    $script:cleanupSentinelSha256 = (Get-FileHash -Algorithm SHA256 `
            -LiteralPath $script:cleanupSentinelPath).Hash.ToLowerInvariant()
    Start-DisposablePostgres
    Write-ProgressMarker 'postgres-started'

    $roleSql = Join-Path $script:credentialDirectory 'provision-role.sql'
    Set-ProtectedCredentialFile $roleSql @"
CREATE ROLE carebridge_checklist_schema_owner
    NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
CREATE ROLE carebridge_checklist_retention_owner
    NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
CREATE ROLE checklist_operations LOGIN PASSWORD '$script:operationsPassword'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
CREATE ROLE carebridge_application LOGIN
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS;
"@
    try {
        Invoke-AdminPsqlFile 'postgres' $roleSql 'CHECKLIST_SYNTHETIC_ROLE_PROVISION_FAILED'
        Write-ProgressMarker 'operations-role-provisioned'
    }
    finally {
        Remove-Item -LiteralPath $roleSql -Force -ErrorAction SilentlyContinue
    }

    $databaseSuffix = [Guid]::NewGuid().ToString('N').Substring(0, 12)
    $cleanDatabase = "carebridge_chk041_clean_$databaseSuffix"
    $normalDatabase = "carebridge_chk041_normal_$databaseSuffix"
    $abortDatabase = "carebridge_chk041_abort_$databaseSuffix"
    $rollForwardDatabase = "carebridge_chk041_rollforward_$databaseSuffix"
    foreach ($database in @($cleanDatabase, $normalDatabase, $abortDatabase, $rollForwardDatabase)) {
        New-DisposableDatabase $database
        Write-ProgressMarker "database-created-$database"
    }

    Push-Location $backendDirectory
    try {
        $cleanEvidence = Join-Path $script:internalEvidenceRoot 'clean-chain'
        Invoke-SyntheticOperator 'clean-chain' $cleanDatabase $cleanEvidence
        $cleanHistory = Get-RequiredJson (Join-Path $cleanEvidence 'clean-chain-history.json') `
            'CHECKLIST_SYNTHETIC_CLEAN_HISTORY_REQUIRED'
        Invoke-SyntheticOperator 'clean-corrected-chain' $cleanDatabase $cleanEvidence
        $independentCorrectedHistory = Get-RequiredJson `
            (Join-Path $cleanEvidence 'corrected-clean-chain-history.json') `
            'CHECKLIST_SYNTHETIC_INDEPENDENT_CORRECTED_HISTORY_REQUIRED'
        $independentCorrectionSql = Join-Path $cleanEvidence `
            'V20260730060000__resolve_chk041_verified_task_collision.sql'
        if (-not (Test-Path -LiteralPath $independentCorrectionSql -PathType Leaf)) {
            throw 'CHECKLIST_SYNTHETIC_CORRECTION_SQL_REQUIRED'
        }

        $normalEvidence = Join-Path $script:internalEvidenceRoot 'normal'
        Invoke-SyntheticOperator 'expand' $normalDatabase $normalEvidence
        Grant-ChecklistVerifierReads $normalDatabase
        $normalState = Get-RequiredJson (Join-Path $normalEvidence 'operator-state.json') `
            'CHECKLIST_SYNTHETIC_OPERATOR_STATE_REQUIRED'
        $normalDatasetManifest = Join-Path $normalEvidence 'dataset-manifest.json'
        [void](Get-RequiredJson $normalDatasetManifest 'CHECKLIST_SYNTHETIC_DATASET_MANIFEST_REQUIRED')
        $normalFingerprint = (Get-FileHash -Algorithm SHA256 -LiteralPath $normalDatasetManifest).Hash.ToLowerInvariant()
        if ($normalState.referenceDatasetFingerprint -ne $normalFingerprint) {
            throw 'CHECKLIST_SYNTHETIC_DATASET_FINGERPRINT_MISMATCH'
        }
        $normalPre = Invoke-RehearsalVerifier 'CapturePre' $normalDatabase $normalFingerprint `
            $null $null $null $null $null $null $normalDatasetManifest
        Invoke-SyntheticOperator 'remainder' $normalDatabase $normalEvidence
        $normalMetrics = Join-Path $normalEvidence 'migration-metrics.json'
        $normalLockLog = Join-Path $normalEvidence 'raw-lock-samples.json'
        Bind-RawEvidenceToMetrics $normalMetrics `
            (Join-Path $normalEvidence 'remainder-raw-flyway.log') $normalLockLog
        [void](Invoke-RehearsalVerifier 'VerifyPost' $normalDatabase $normalFingerprint `
                $normalPre $normalMetrics $ExpectedBaseFlywayVersion `
                "$($cleanHistory.flywayHistorySha256)" $null $null $normalDatasetManifest)
        Invoke-SyntheticOperator 'today' $normalDatabase $normalEvidence
        [void](Get-RequiredJson (Join-Path $normalEvidence 'today-metrics.json') `
            'CHECKLIST_SYNTHETIC_TODAY_METRICS_REQUIRED')
        Invoke-SyntheticOperator 'reconcile' $normalDatabase $normalEvidence
        [void](Get-RequiredJson (Join-Path $normalEvidence 'reconciliation-metrics.json') `
            'CHECKLIST_SYNTHETIC_RECONCILIATION_METRICS_REQUIRED')

        $abortEvidence = Join-Path $script:internalEvidenceRoot 'abort'
        Invoke-SyntheticOperator 'expand-challenge' $abortDatabase $abortEvidence
        Grant-ChecklistVerifierReads $abortDatabase
        $abortState = Get-RequiredJson (Join-Path $abortEvidence 'operator-state.json') `
            'CHECKLIST_SYNTHETIC_OPERATOR_STATE_REQUIRED'
        $abortDatasetManifest = Join-Path $abortEvidence 'dataset-manifest.json'
        [void](Get-RequiredJson $abortDatasetManifest 'CHECKLIST_SYNTHETIC_DATASET_MANIFEST_REQUIRED')
        $abortFingerprint = (Get-FileHash -Algorithm SHA256 -LiteralPath $abortDatasetManifest).Hash.ToLowerInvariant()
        if ($abortState.referenceDatasetFingerprint -ne $abortFingerprint) {
            throw 'CHECKLIST_SYNTHETIC_DATASET_FINGERPRINT_MISMATCH'
        }
        $abortPre = Invoke-RehearsalVerifier 'CapturePre' $abortDatabase $abortFingerprint `
            $null $null $null $null $null $null $abortDatasetManifest
        Invoke-SyntheticOperator 'challenge' $abortDatabase $abortEvidence
        $abortMetrics = Join-Path $abortEvidence 'migration-metrics.json'
        $abortLockLog = Join-Path $abortEvidence 'raw-lock-samples.json'
        Bind-RawEvidenceToMetrics $abortMetrics `
            (Join-Path $abortEvidence 'challenge-raw-flyway.log') $abortLockLog
        $abortArtifact = Invoke-RehearsalVerifier 'ProveAbort' $abortDatabase $abortFingerprint `
            $abortPre $abortMetrics $ExpectedBaseFlywayVersion `
            "$($cleanHistory.flywayHistorySha256)" $null $null $abortDatasetManifest
        $previousHistory = Get-RequiredJson (Join-Path $abortEvidence 'flyway-history.json') `
            'CHECKLIST_SYNTHETIC_PREVIOUS_HISTORY_REQUIRED'
        if ($previousHistory.flywayHistorySha256 -ne $cleanHistory.flywayHistorySha256) {
            throw 'CHECKLIST_SYNTHETIC_BASE_HISTORY_NOT_INDEPENDENTLY_VERIFIED'
        }

        $rollForwardEvidence = Join-Path $script:internalEvidenceRoot 'roll-forward'
        Invoke-SyntheticOperator 'expand-challenge' $rollForwardDatabase $rollForwardEvidence
        Grant-ChecklistVerifierReads $rollForwardDatabase
        $rollForwardState = Get-RequiredJson (Join-Path $rollForwardEvidence 'operator-state.json') `
            'CHECKLIST_SYNTHETIC_OPERATOR_STATE_REQUIRED'
        $rollForwardDatasetManifest = Join-Path $rollForwardEvidence 'dataset-manifest.json'
        [void](Get-RequiredJson $rollForwardDatasetManifest 'CHECKLIST_SYNTHETIC_DATASET_MANIFEST_REQUIRED')
        $rollForwardFingerprint = (Get-FileHash -Algorithm SHA256 `
                -LiteralPath $rollForwardDatasetManifest).Hash.ToLowerInvariant()
        if ($rollForwardState.referenceDatasetFingerprint -ne $rollForwardFingerprint -or
                $rollForwardFingerprint -ne $abortFingerprint) {
            throw 'CHECKLIST_SYNTHETIC_ROLL_FORWARD_DATASET_MISMATCH'
        }
        $rollForwardPre = Invoke-RehearsalVerifier 'CapturePre' $rollForwardDatabase `
            $rollForwardFingerprint $null $null $null $null $null $null $rollForwardDatasetManifest
        Invoke-SyntheticOperator 'correct' $rollForwardDatabase $rollForwardEvidence
        $rollForwardHistory = Get-RequiredJson `
            (Join-Path $rollForwardEvidence 'corrected-flyway-history.json') `
            'CHECKLIST_SYNTHETIC_CORRECTED_HISTORY_REQUIRED'
        if ($rollForwardHistory.flywayHistorySha256 -ne $independentCorrectedHistory.flywayHistorySha256 -or
                [long]$rollForwardHistory.historyRowCount -ne [long]$independentCorrectedHistory.historyRowCount -or
                [long]$independentCorrectedHistory.historyRowCount -ne
                ([long]$previousHistory.historyRowCount + 1L)) {
            throw 'CHECKLIST_SYNTHETIC_CORRECTED_HISTORY_NOT_INDEPENDENTLY_VERIFIED'
        }
        $rollForwardCorrectionSql = Join-Path $rollForwardEvidence `
            'V20260730060000__resolve_chk041_verified_task_collision.sql'
        if (-not (Test-Path -LiteralPath $rollForwardCorrectionSql -PathType Leaf) -or
                (Get-FileHash -Algorithm SHA256 -LiteralPath $rollForwardCorrectionSql).Hash -ne
                (Get-FileHash -Algorithm SHA256 -LiteralPath $independentCorrectionSql).Hash) {
            throw 'CHECKLIST_SYNTHETIC_CORRECTION_SQL_REQUIRED'
        }
        $correctionArtifactPath = Join-Path $script:internalEvidenceRoot 'correction-artifact.json'
        $correctionArtifact = [ordered]@{
            schemaVersion = 1
            datasetFingerprint = $rollForwardFingerprint
            previousFinalFlywayVersion = $ExpectedBaseFlywayVersion
            correctedFinalFlywayVersion = $ExpectedCorrectedFlywayVersion
            previousFlywayHistorySha256 = "$($previousHistory.flywayHistorySha256)"
            correctedFlywayHistorySha256 = "$($independentCorrectedHistory.flywayHistorySha256)"
            correctionMigrationVersion = $ExpectedCorrectedFlywayVersion
            correctionApplied = $true
            historyExtensionVerified = $true
            previousHistoryRowCount = [long]$previousHistory.historyRowCount
            correctedHistoryRowCount = [long]$independentCorrectedHistory.historyRowCount
            correctionSqlSha256 = (Get-FileHash -Algorithm SHA256 `
                -LiteralPath $independentCorrectionSql).Hash.ToLowerInvariant()
        }
        Write-SanitizedJson $correctionArtifactPath $correctionArtifact
        $correctedMetrics = Join-Path $rollForwardEvidence 'corrected-migration-metrics.json'
        $correctedLockLog = Join-Path $rollForwardEvidence 'corrected-raw-lock-samples.json'
        Bind-RawEvidenceToMetrics $correctedMetrics `
            (Join-Path $rollForwardEvidence 'correct-raw-flyway.log') $correctedLockLog
        [void](Invoke-RehearsalVerifier 'VerifyRollForward' $rollForwardDatabase $rollForwardFingerprint `
                $rollForwardPre $correctedMetrics $ExpectedCorrectedFlywayVersion `
                "$($independentCorrectedHistory.flywayHistorySha256)" $abortArtifact `
                $correctionArtifactPath $rollForwardDatasetManifest)
    }
    finally {
        Pop-Location
    }

    Assert-NoSecretInEvidence
    $exportedEvidence = Export-SanitizedEvidence
    $completed = $true
}
catch {
    $originalFailure = $_
    try {
        if ($script:postgresStarted) {
            Stop-DisposablePostgres
        }
        if (Test-Path -LiteralPath $script:internalEvidenceRoot -PathType Container) {
            Write-SanitizedJson (Join-Path $script:internalEvidenceRoot 'runner-failure.json') ([ordered]@{
                schemaVersion = 1
                disposableRunId = $script:runId
                capturedAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
                errorId = "$($originalFailure.FullyQualifiedErrorId)"
                exceptionType = $originalFailure.Exception.GetType().FullName
                message = "$($originalFailure.Exception.Message)"
            })
            Assert-NoSecretInEvidence
            $exportedEvidence = Export-SanitizedEvidence
        }
    }
    catch {
        # Preserve the original operational failure. Evidence export is best-effort
        # here; database/credential cleanup in finally remains mandatory.
        Write-Output "CHK041_FAILURE_EXPORT_SKIPPED $($_.Exception.Message)"
    }
    throw $originalFailure
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    if ($null -ne $previousPgpassword) {
        $env:PGPASSWORD = $previousPgpassword
    }
    if ($null -eq $previousPgpass) {
        Remove-Item Env:PGPASSFILE -ErrorAction SilentlyContinue
    }
    else {
        $env:PGPASSFILE = $previousPgpass
    }
    foreach ($name in $libpqTargetNames) {
        [Environment]::SetEnvironmentVariable($name, $previousLibpqTargets[$name])
    }
    if ($null -ne $script:runRoot -and (Test-Path -LiteralPath $script:runRoot -PathType Container)) {
        Stop-DisposablePostgres
        Remove-SentinelBoundDirectory
    }
}

if (-not $completed) {
    throw 'CHECKLIST_SYNTHETIC_REHEARSAL_INCOMPLETE'
}
$portClosed = Test-LoopbackPortClosed $script:port
$runRootRemoved = -not (Test-Path -LiteralPath $script:runRoot)
$dataDirectoryRemoved = -not (Test-Path -LiteralPath $script:dataDirectory)
$credentialFileRemoved = -not (Test-Path -LiteralPath $script:pgpassFile)
if (-not $portClosed -or -not $runRootRemoved -or -not $dataDirectoryRemoved -or
        -not $credentialFileRemoved -or $script:postgresStarted) {
    throw 'CHECKLIST_SYNTHETIC_CLEANUP_ATTESTATION_FAILED'
}
$cleanupAttestationPath = Join-Path $exportedEvidence 'cleanup-attestation.json'
Write-SanitizedJson $cleanupAttestationPath ([ordered]@{
    schemaVersion = 1
    disposableRunId = $script:runId
    capturedAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
    postgresStopped = $true
    port = $script:port
    portClosed = $portClosed
    runRootRemoved = $runRootRemoved
    dataDirectoryRemoved = $dataDirectoryRemoved
    credentialFileRemoved = $credentialFileRemoved
})
$indexPath = Join-Path $exportedEvidence 'evidence-index.json'
$indexEntries = @(Get-ChildItem -LiteralPath $exportedEvidence -File -Recurse | Where-Object {
    $_.FullName -ne $indexPath
} | Sort-Object FullName | ForEach-Object {
    [ordered]@{
        path = $_.FullName.Substring($exportedEvidence.Length).TrimStart('\').Replace('\', '/')
        length = $_.Length
        sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash.ToLowerInvariant()
    }
})
Write-SanitizedJson $indexPath ([ordered]@{
    schemaVersion = 1
    disposableRunId = $script:runId
    capturedAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
    artifactCount = $indexEntries.Count
    artifacts = $indexEntries
})
Write-Output "CHECKLIST_SYNTHETIC_REHEARSAL_COMPLETE:$exportedEvidence"
