[CmdletBinding()]
param(
    [ValidateSet('Help', 'CapturePre', 'VerifyPost', 'ProveAbort', 'VerifyRollForward')]
    [string]$Phase = 'Help',
    [string]$PsqlPath = $env:CAREBRIDGE_PSQL_PATH,
    [string]$DatabaseUrl = $env:CAREBRIDGE_CHECKLIST_REHEARSAL_DB_URL,
    [string]$EvidenceDirectory,
    [string]$PreManifestPath,
    [string]$MetricsPath,
    [string]$PreviousAbortArtifact,
    [string]$PreviousAbortArtifactSha256,
    [string]$DisposableAttestationPath = $env:CAREBRIDGE_CHECKLIST_DISPOSABLE_ATTESTATION_PATH,
    [string]$CohortAttestationPath,
    [string]$CorrectionArtifactPath,
    [string]$CorrectionArtifactSha256,
    [string]$ExpectedFinalFlywayVersion,
    [string]$ExpectedFlywayHistorySha256,
    [string]$ReferenceDatasetFingerprint,
    [string]$ReferenceDatasetManifestPath = $env:CAREBRIDGE_CHECKLIST_DATASET_MANIFEST_PATH,
    [ValidateRange(0, [long]::MaxValue)]
    [long]$MaxUnresolvedQuarantineCount = 0,
    [ValidateRange(0.0, 100.0)]
    [double]$MaxLegacyQuarantineRatePercent = 0.0,
    [ValidateRange(0.001, [double]::MaxValue)]
    [double]$MaxLockSeconds = 5.0,
    [ValidateRange(0.001, [double]::MaxValue)]
    [double]$MinBackfillRowsPerSecond = 500.0,
    [ValidateRange(0.001, [double]::MaxValue)]
    [double]$MaxFullMigrationSeconds = 1800.0
)

$ErrorActionPreference = 'Stop'

function Show-Usage {
    @'
CareBridge checklist migration rehearsal (read-only verifier)

Phases:
  CapturePre        Capture the immutable manifest after expand target 20260729060000,
                    immediately before the legacy backfill starts.
  VerifyPost        Compare the post-migration database and external metrics to the manifest.
  ProveAbort        Expect at least one gate to fail; exits zero only when ABORT is proven.
  VerifyRollForward Require a prior abort artifact and prove the corrected forward run passes.

Authentication:
  Set CAREBRIDGE_PSQL_PATH and CAREBRIDGE_CHECKLIST_REHEARSAL_DB_URL.
  Only a single 127.0.0.1 URI authenticated as checklist_operations is accepted.
  Database URLs containing passwords/options and libpq target overrides are rejected.
  A protected, exact-match PGPASSFILE and disposable/cohort attestations are mandatory.
  PGPASSWORD is deliberately ignored for the psql child process.

The script never applies migrations, changes feature flags, or writes to the database.
'@ | Write-Output
}

if ($Phase -eq 'Help') {
    Show-Usage
    exit 0
}

function Resolve-RequiredFile([string]$Path, [string]$ErrorCode) {
    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw $ErrorCode
    }
    return (Resolve-Path -LiteralPath $Path).Path
}

function Assert-SafeDatabaseUrl([string]$Url) {
    if ([string]::IsNullOrWhiteSpace($Url)) {
        throw 'CHECKLIST_REHEARSAL_DATABASE_URL_REQUIRED'
    }
    $trimmed = $Url.TrimStart()
    if ($trimmed.StartsWith('-')) {
        throw 'CHECKLIST_REHEARSAL_DATABASE_URL_OPTION_REJECTED'
    }
    $databaseUri = $null
    $isPostgresUri = [Uri]::TryCreate($Url, [UriKind]::Absolute, [ref]$databaseUri) -and
        ($databaseUri.Scheme -ieq 'postgres' -or $databaseUri.Scheme -ieq 'postgresql')
    if ($isPostgresUri) {
        if ($databaseUri.Host -ne '127.0.0.1' -or
                $databaseUri.Port -le 0 -or
                $databaseUri.UserInfo -ne 'checklist_operations' -or
                [string]::IsNullOrWhiteSpace($databaseUri.AbsolutePath.Trim('/')) -or
                $databaseUri.AbsolutePath.Trim('/').Contains('/') -or
                -not [string]::IsNullOrWhiteSpace($databaseUri.Query) -or
                -not [string]::IsNullOrWhiteSpace($databaseUri.Fragment)) {
            throw 'CHECKLIST_REHEARSAL_DATABASE_URL_NOT_LOCAL_DISPOSABLE'
        }
        if ($databaseUri.UserInfo.Contains(':')) {
            throw 'CHECKLIST_REHEARSAL_DATABASE_URL_PASSWORD_FORBIDDEN'
        }
        $decodedQuery = [Uri]::UnescapeDataString($databaseUri.Query)
        if ($decodedQuery -match '(?i)(?:^|[?&;])password(?:\s*=|[&;]|$)') {
            throw 'CHECKLIST_REHEARSAL_DATABASE_URL_PASSWORD_FORBIDDEN'
        }
    }
    elseif ($Url -match '(?i)(?:^|\s)password\s*=') {
        throw 'CHECKLIST_REHEARSAL_DATABASE_URL_PASSWORD_FORBIDDEN'
    }
    else {
        throw 'CHECKLIST_REHEARSAL_DATABASE_URL_URI_REQUIRED'
    }
    return $databaseUri
}

function Assert-NoInheritedLibpqTarget {
    foreach ($name in @(
            'PGHOST', 'PGHOSTADDR', 'PGPORT', 'PGDATABASE', 'PGUSER',
            'PGSERVICE', 'PGSERVICEFILE', 'PGOPTIONS', 'PGTARGETSESSIONATTRS')) {
        if (-not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
            throw "CHECKLIST_REHEARSAL_LIBPQ_TARGET_ENV_FORBIDDEN:$name"
        }
    }
}

function Assert-NotReparsePoint([string]$Path, [string]$ErrorCode) {
    $item = Get-Item -LiteralPath $Path -Force
    if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw $ErrorCode
    }
}

function Assert-PathOutside([string]$Candidate, [string[]]$ForbiddenRoots, [string]$ErrorCode) {
    $resolvedCandidate = [System.IO.Path]::GetFullPath($Candidate).TrimEnd('\')
    foreach ($root in $ForbiddenRoots) {
        if ([string]::IsNullOrWhiteSpace($root)) { continue }
        $resolvedRoot = [System.IO.Path]::GetFullPath($root).TrimEnd('\')
        if ($resolvedCandidate.Equals($resolvedRoot, [StringComparison]::OrdinalIgnoreCase) -or
                $resolvedCandidate.StartsWith($resolvedRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
                $resolvedRoot.StartsWith($resolvedCandidate + '\', [StringComparison]::OrdinalIgnoreCase)) {
            throw $ErrorCode
        }
    }
}

function Read-JsonFile([string]$Path, [string]$ErrorCode) {
    $resolved = Resolve-RequiredFile $Path $ErrorCode
    Assert-NotReparsePoint $resolved $ErrorCode
    try {
        return [ordered]@{
            path = $resolved
            sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolved).Hash.ToLowerInvariant()
            value = Get-Content -Raw -LiteralPath $resolved | ConvertFrom-Json
        }
    }
    catch {
        throw $ErrorCode
    }
}

function Assert-ProtectedPgpass([Uri]$DatabaseUri, [object]$DisposableAttestation) {
    $pgpassPath = $env:PGPASSFILE
    $resolved = Resolve-RequiredFile $pgpassPath 'CHECKLIST_REHEARSAL_PGPASSFILE_REQUIRED'
    Assert-NotReparsePoint $resolved 'CHECKLIST_REHEARSAL_PGPASSFILE_REPARSE_FORBIDDEN'
    Assert-PathOutside $resolved @($script:RepositoryRoot, $script:EvidenceRoot, $DisposableAttestation.dataDirectory) `
        'CHECKLIST_REHEARSAL_PGPASSFILE_LOCATION_FORBIDDEN'

    $acl = Get-Acl -LiteralPath $resolved
    if (-not $acl.AreAccessRulesProtected) {
        throw 'CHECKLIST_REHEARSAL_PGPASSFILE_ACL_NOT_PROTECTED'
    }
    $currentSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    $unexpectedAllow = @($acl.Access | Where-Object {
        $_.AccessControlType -eq [System.Security.AccessControl.AccessControlType]::Allow -and
        $_.FileSystemRights -band [System.Security.AccessControl.FileSystemRights]::Read -and
        $_.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value -ne $currentSid
    })
    if ($unexpectedAllow.Count -gt 0) {
        throw 'CHECKLIST_REHEARSAL_PGPASSFILE_ACL_TOO_BROAD'
    }
    $lines = @(Get-Content -LiteralPath $resolved | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($lines.Count -ne 1) {
        throw 'CHECKLIST_REHEARSAL_PGPASSFILE_ENTRY_INVALID'
    }
    $parts = $lines[0].Split(':', 5)
    if ($parts.Count -ne 5 -or $parts[0] -ne '127.0.0.1' -or
            $parts[1] -ne "$($DatabaseUri.Port)" -or
            $parts[2] -ne $DatabaseUri.AbsolutePath.Trim('/') -or
            $parts[3] -ne 'checklist_operations' -or
            [string]::IsNullOrWhiteSpace($parts[4]) -or
            $parts[0..3] -contains '*') {
        throw 'CHECKLIST_REHEARSAL_PGPASSFILE_ENTRY_INVALID'
    }
    return $resolved
}

function Assert-CohortDisabled([string]$Path, [string]$DatasetFingerprint, [string]$RunId) {
    $record = Read-JsonFile $Path 'CHECKLIST_REHEARSAL_COHORT_ATTESTATION_REQUIRED'
    $value = $record.value
    $capturedAt = [DateTimeOffset]::MinValue
    if ([int]$value.schemaVersion -ne 1 -or $value.mode -ne 'approved-local-control-plane-simulation' -or
            [bool]$value.cohortEnabled -or $value.datasetFingerprint -ne $DatasetFingerprint -or
            $value.disposableRunId -ne $RunId -or "$($value.capturedAtUtc)" -notmatch '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z$') {
        throw 'CHECKLIST_REHEARSAL_COHORT_ATTESTATION_INVALID'
    }
    if (-not $record.path.StartsWith($script:EvidenceRoot.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase) -or
            -not [DateTimeOffset]::TryParse(
                "$($value.capturedAtUtc)",
                [System.Globalization.CultureInfo]::InvariantCulture,
                ([System.Globalization.DateTimeStyles]::AssumeUniversal -bor
                    [System.Globalization.DateTimeStyles]::AdjustToUniversal),
                [ref]$capturedAt) -or
            [Math]::Abs(([DateTimeOffset]::UtcNow - $capturedAt.ToUniversalTime()).TotalMinutes) -gt 15.0) {
        throw 'CHECKLIST_REHEARSAL_COHORT_ATTESTATION_INVALID'
    }
    return $record
}

function Assert-DisposableAttestation([string]$Path, [Uri]$DatabaseUri) {
    $record = Read-JsonFile $Path 'CHECKLIST_REHEARSAL_DISPOSABLE_ATTESTATION_REQUIRED'
    $value = $record.value
    if ([int]$value.schemaVersion -ne 1 -or "$($value.runId)" -notmatch '^[a-z0-9][a-z0-9-]{7,79}$' -or
            $value.host -ne '127.0.0.1' -or [int]$value.port -ne $DatabaseUri.Port -or
            $value.database -ne $DatabaseUri.AbsolutePath.Trim('/') -or
            $value.user -ne 'checklist_operations' -or [int]$value.postgresMajor -ne 18 -or
            "$($value.psqlSha256)" -notmatch '^[0-9a-f]{64}$' -or
            "$($value.postgresSha256)" -notmatch '^[0-9a-f]{64}$' -or
            "$($value.initdbSha256)" -notmatch '^[0-9a-f]{64}$' -or
            "$($value.pgCtlSha256)" -notmatch '^[0-9a-f]{64}$' -or
            "$($value.pgDumpSha256)" -notmatch '^[0-9a-f]{64}$' -or
            "$($value.pgRestoreSha256)" -notmatch '^[0-9a-f]{64}$') {
        throw 'CHECKLIST_REHEARSAL_DISPOSABLE_ATTESTATION_INVALID'
    }
    $actualPsqlHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:ResolvedPsqlPath).Hash.ToLowerInvariant()
    if ($actualPsqlHash -ne $value.psqlSha256) {
        throw 'CHECKLIST_REHEARSAL_PSQL_IDENTITY_MISMATCH'
    }

    $workRoot = [System.IO.Path]::GetFullPath("$($value.workRoot)").TrimEnd('\')
    $dataRoot = [System.IO.Path]::GetFullPath("$($value.dataDirectory)").TrimEnd('\')
    $attestedEvidence = [System.IO.Path]::GetFullPath("$($value.evidenceDirectory)").TrimEnd('\')
    $sentinel = Resolve-RequiredFile "$($value.sentinelPath)" 'CHECKLIST_REHEARSAL_DISPOSABLE_SENTINEL_REQUIRED'
    $approvedRoot = [System.IO.Path]::GetFullPath('D:\tmp').TrimEnd('\')
    if (-not $workRoot.StartsWith($approvedRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
            -not $dataRoot.StartsWith($workRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
            -not $attestedEvidence.StartsWith($workRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
            $attestedEvidence.StartsWith($dataRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
            -not $sentinel.StartsWith($dataRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
            [System.IO.Path]::GetFileName($sentinel) -ne '.carebridge-chk041-disposable.json') {
        throw 'CHECKLIST_REHEARSAL_DISPOSABLE_PATH_BINDING_INVALID'
    }
    foreach ($directory in @($workRoot, $dataRoot, $attestedEvidence)) {
        if (-not (Test-Path -LiteralPath $directory -PathType Container)) {
            throw 'CHECKLIST_REHEARSAL_DISPOSABLE_PATH_MISSING'
        }
        Assert-NotReparsePoint $directory 'CHECKLIST_REHEARSAL_DISPOSABLE_REPARSE_FORBIDDEN'
    }
    Assert-NotReparsePoint $sentinel 'CHECKLIST_REHEARSAL_DISPOSABLE_REPARSE_FORBIDDEN'
    Assert-Sha256 "$($value.sentinelSha256)" 'CHECKLIST_REHEARSAL_DISPOSABLE_SENTINEL_HASH_INVALID'
    $actualSentinelHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sentinel).Hash.ToLowerInvariant()
    if ($actualSentinelHash -ne $value.sentinelSha256) {
        throw 'CHECKLIST_REHEARSAL_DISPOSABLE_SENTINEL_HASH_MISMATCH'
    }
    $runtimeSql = @"
BEGIN TRANSACTION READ ONLY;
SELECT json_build_object(
    'host', host(inet_server_addr()),
    'port', inet_server_port(),
    'database', current_database(),
    'currentUser', current_user,
    'sessionUser', session_user,
    'postgresMajor', current_setting('server_version_num')::integer / 10000
)::text;
ROLLBACK;
"@
    $runtime = Invoke-ReadOnlyJsonQuery $runtimeSql
    if ($runtime.host -ne '127.0.0.1' -or [int]$runtime.port -ne [int]$value.port -or
            $runtime.database -ne $value.database -or $runtime.currentUser -ne 'checklist_operations' -or
            $runtime.sessionUser -ne 'checklist_operations' -or [int]$runtime.postgresMajor -ne 18) {
        throw ('CHECKLIST_REHEARSAL_DISPOSABLE_RUNTIME_MISMATCH:' +
            "host=$($runtime.host)/127.0.0.1," +
            "port=$($runtime.port)/$($value.port)," +
            "database=$($runtime.database)/$($value.database)," +
            "currentUser=$($runtime.currentUser)/checklist_operations," +
            "sessionUser=$($runtime.sessionUser)/checklist_operations," +
            "postgresMajor=$($runtime.postgresMajor)/18")
    }
    return $record
}

function Assert-ReferenceDatasetManifest([string]$Path, [string]$Fingerprint) {
    $record = Read-JsonFile $Path 'CHECKLIST_REHEARSAL_DATASET_MANIFEST_REQUIRED'
    if ($record.sha256 -ne $Fingerprint -or
            -not $record.path.StartsWith($script:EvidenceRoot.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'CHECKLIST_REHEARSAL_DATASET_MANIFEST_FINGERPRINT_MISMATCH'
    }
    $value = $record.value
    if ([int]$value.schemaVersion -ne 1 -or $value.generatorVersion -ne 'chk041-synthetic-v1' -or
            $value.deterministicSeed -ne 'carebridge:chk041:v1' -or
            [long]$value.legacySourceCount -ne 10002L -or [long]$value.expectedTargetCount -ne 10000L -or
            [long]$value.reviewedUnresolvedQuarantineCount -ne 2L -or
            [double]$value.reviewedLegacyQuarantineRatePercent -ne 0.02 -or
            "$($value.legacySourceSha256)" -notmatch '^[0-9a-f]{64}$' -or
            "$($value.fixtureSqlSha256)" -notmatch '^[0-9a-f]{64}$' -or
            "$($value.migrationInventorySha256)" -notmatch '^[0-9a-f]{64}$') {
        throw 'CHECKLIST_REHEARSAL_DATASET_MANIFEST_INVALID'
    }
    return $record
}

function Invoke-ReadOnlyJsonQuery([string]$Sql) {
    $psqlArguments = @(
        '--no-psqlrc'
        '--no-password'
        '--quiet'
        '--tuples-only'
        '--no-align'
        '--set=ON_ERROR_STOP=on'
        "--dbname=$DatabaseUrl"
        '--command'
        $Sql
    )
    $rawOutput = & $script:ResolvedPsqlPath @psqlArguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "CHECKLIST_REHEARSAL_READ_ONLY_QUERY_FAILED:$LASTEXITCODE"
    }
    $jsonLine = ($rawOutput | ForEach-Object { "$_" } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
            Select-Object -Last 1).Trim()
    if ([string]::IsNullOrWhiteSpace($jsonLine)) {
        throw 'CHECKLIST_REHEARSAL_EMPTY_QUERY_RESULT'
    }
    try {
        return $jsonLine | ConvertFrom-Json
    }
    catch {
        throw 'CHECKLIST_REHEARSAL_INVALID_QUERY_RESULT'
    }
}

function Write-SanitizedJson([string]$Path, [object]$Value) {
    $json = $Value | ConvertTo-Json -Depth 12
    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $json, $utf8WithoutBom)
}

function New-Check([string]$Name, [bool]$Passed, [object]$Actual, [object]$Expected) {
    [ordered]@{
        name = $Name
        passed = $Passed
        actual = $Actual
        expected = $Expected
    }
}

function Assert-Sha256([string]$Value, [string]$ErrorCode) {
    if ($Value -notmatch '^[0-9a-f]{64}$') {
        throw $ErrorCode
    }
}

function Assert-MetricsContract([object]$Metrics) {
    $required = @(
        'expandMigrationSucceeded'
        'remainderMigrationSucceeded'
        'flywayValidationSuccessful'
        'lockSeconds'
        'backfillRows'
        'backfillSeconds'
        'backfillRowsPerSecond'
        'expandMigrationSeconds'
        'remainderMigrationSeconds'
        'fullMigrationSeconds'
        'measurementStartedAtUtc'
        'measurementCompletedAtUtc'
        'referenceDatasetFingerprint'
        'operatorEvidenceReference'
        'rawFlywayLogPath'
        'rawFlywayLogSha256'
        'rawLockLogPath'
        'rawLockLogSha256'
    )
    $available = @($Metrics.PSObject.Properties.Name)
    foreach ($name in $required) {
        if ($available -notcontains $name -or $null -eq $Metrics.$name) {
            throw "CHECKLIST_REHEARSAL_METRICS_FIELD_REQUIRED:$name"
        }
    }
    foreach ($name in @(
            'lockSeconds',
            'backfillSeconds',
            'backfillRowsPerSecond',
            'expandMigrationSeconds',
            'remainderMigrationSeconds',
            'fullMigrationSeconds')) {
        $number = 0.0
        if (-not [double]::TryParse(
                "$($Metrics.$name)",
                [System.Globalization.NumberStyles]::Float,
                [System.Globalization.CultureInfo]::InvariantCulture,
                [ref]$number) -or
                [double]::IsNaN($number) -or
                [double]::IsInfinity($number) -or
                $number -le 0.0) {
            throw "CHECKLIST_REHEARSAL_METRICS_FIELD_INVALID:$name"
        }
    }
    $backfillRows = 0L
    if (-not [long]::TryParse(
            "$($Metrics.backfillRows)",
            [System.Globalization.NumberStyles]::Integer,
            [System.Globalization.CultureInfo]::InvariantCulture,
            [ref]$backfillRows) -or $backfillRows -le 0L) {
        throw 'CHECKLIST_REHEARSAL_METRICS_FIELD_INVALID:backfillRows'
    }
    foreach ($name in @(
            'expandMigrationSucceeded',
            'remainderMigrationSucceeded',
            'flywayValidationSuccessful')) {
        if ($Metrics.$name -isnot [bool]) {
            throw "CHECKLIST_REHEARSAL_METRICS_FIELD_INVALID:$name"
        }
    }
    if ($Metrics.referenceDatasetFingerprint -notmatch '^[0-9a-f]{64}$') {
        throw 'CHECKLIST_REHEARSAL_METRICS_FIELD_INVALID:referenceDatasetFingerprint'
    }
    if ($Metrics.operatorEvidenceReference -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]{0,119}$') {
        throw 'CHECKLIST_REHEARSAL_METRICS_FIELD_INVALID:operatorEvidenceReference'
    }
    foreach ($name in @('rawFlywayLogSha256', 'rawLockLogSha256')) {
        if ($Metrics.$name -notmatch '^[0-9a-f]{64}$') {
            throw "CHECKLIST_REHEARSAL_METRICS_FIELD_INVALID:$name"
        }
    }
    $startedAt = [DateTimeOffset]::MinValue
    $completedAt = [DateTimeOffset]::MinValue
    if ("$($Metrics.measurementStartedAtUtc)" -notmatch '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z$' -or
            -not [DateTimeOffset]::TryParse(
            "$($Metrics.measurementStartedAtUtc)",
            [System.Globalization.CultureInfo]::InvariantCulture,
            [System.Globalization.DateTimeStyles]::AssumeUniversal,
            [ref]$startedAt)) {
        throw 'CHECKLIST_REHEARSAL_METRICS_FIELD_INVALID:measurementStartedAtUtc'
    }
    if ("$($Metrics.measurementCompletedAtUtc)" -notmatch '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z$' -or
            -not [DateTimeOffset]::TryParse(
            "$($Metrics.measurementCompletedAtUtc)",
            [System.Globalization.CultureInfo]::InvariantCulture,
            [System.Globalization.DateTimeStyles]::AssumeUniversal,
            [ref]$completedAt) -or $completedAt -le $startedAt) {
        throw 'CHECKLIST_REHEARSAL_METRICS_FIELD_INVALID:measurementCompletedAtUtc'
    }
}

function Resolve-HashedEvidenceArtifact([string]$Path, [string]$ExpectedSha256, [string]$ErrorCode) {
    $resolved = Resolve-RequiredFile $Path $ErrorCode
    Assert-NotReparsePoint $resolved $ErrorCode
    $full = [System.IO.Path]::GetFullPath($resolved)
    if (-not $full.StartsWith($script:EvidenceRoot.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw $ErrorCode
    }
    Assert-Sha256 $ExpectedSha256 $ErrorCode
    $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $full).Hash.ToLowerInvariant()
    if ($actual -ne $ExpectedSha256) {
        throw $ErrorCode
    }
    return [ordered]@{
        path = $full
        sha256 = $actual
        lastWriteAtUtc = (Get-Item -LiteralPath $full).LastWriteTimeUtc
    }
}

$preSql = @'
BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '5min';
SET LOCAL TIME ZONE 'UTC';
WITH source_rows AS (
    SELECT
        legacy.checklist_item_id AS source_id,
        jsonb_build_array(
            legacy.checklist_item_id,
            legacy.owner_user_id,
            legacy.mother_journey_id,
            legacy.baby_id,
            legacy.template_entry_id,
            legacy.title,
            COALESCE(legacy.display_order, 0),
            upper(legacy.status),
            legacy.due_at,
            legacy.completed_at,
            legacy.created_at,
            legacy.updated_at
        )::text AS normalized_row
    FROM public.preparation_checklist_items legacy
), source_manifest AS (
    SELECT
        count(*)::bigint AS row_count,
        encode(sha256(convert_to(
            COALESCE(string_agg(normalized_row, E'\n' ORDER BY source_id), ''),
            'UTF8')), 'hex') AS row_hash
    FROM source_rows
), quarantine AS (
    SELECT
        count(*)::bigint AS unresolved_count,
        count(DISTINCT source_id) FILTER (
            WHERE source_table = 'preparation_checklist_items')::bigint AS legacy_source_count
    FROM public.checklist_migration_quarantine
    WHERE resolved_at IS NULL
), quarantine_reasons AS (
    SELECT COALESCE(jsonb_object_agg(reason_code, reason_count ORDER BY reason_code), '{}'::jsonb) AS reasons
    FROM (
        SELECT reason_code, count(*)::bigint AS reason_count
        FROM public.checklist_migration_quarantine
        WHERE resolved_at IS NULL
        GROUP BY reason_code
    ) grouped_reasons
), flyway_rows AS (
    SELECT
        installed_rank,
        version,
        success,
        jsonb_build_array(
            installed_rank, version, description, type, script, checksum, success)::text AS normalized_row
    FROM public.flyway_schema_history
), flyway AS (
    SELECT
        max(version) FILTER (WHERE success) AS current_version,
        count(*) FILTER (WHERE NOT success)::bigint AS failed_count,
        encode(sha256(convert_to(
            COALESCE(string_agg(normalized_row, E'\n' ORDER BY installed_rank), ''),
            'UTF8')), 'hex') AS history_hash
    FROM flyway_rows
), security_preflight AS (
    SELECT
        current_user = 'checklist_operations' AS role_authorized,
        session_user = 'checklist_operations' AS session_role_authorized,
        EXISTS (
            SELECT 1
            FROM pg_catalog.pg_roles role
            WHERE role.rolname = current_user
              AND NOT role.rolsuper
              AND NOT role.rolbypassrls
              AND NOT role.rolcreatedb
              AND NOT role.rolcreaterole
              AND NOT role.rolinherit
              AND NOT EXISTS (
                  SELECT 1
                  FROM pg_catalog.pg_auth_members membership
                  WHERE membership.member = role.oid)) AS role_non_bypass_verified,
        has_table_privilege(current_user, 'public.checklist_migration_quarantine', 'SELECT') AS select_granted,
        EXISTS (
            SELECT 1
            FROM pg_catalog.pg_policies policy
            WHERE policy.schemaname = 'public'
              AND policy.tablename = 'checklist_migration_quarantine'
              AND policy.policyname = 'checklist_migration_quarantine_operations_select'
              AND policy.cmd = 'SELECT'
              AND policy.permissive = 'PERMISSIVE'
              AND policy.roles @> ARRAY['checklist_operations']::name[]
              AND policy.qual = 'true')
        AND NOT EXISTS (
            SELECT 1
            FROM pg_catalog.pg_policies policy
            WHERE policy.schemaname = 'public'
              AND policy.tablename = 'checklist_migration_quarantine'
              AND policy.cmd IN ('SELECT', 'ALL')
              AND policy.permissive = 'RESTRICTIVE'
              AND policy.roles && ARRAY['public', 'checklist_operations']::name[])
        AS policy_verified,
        EXISTS (
            SELECT 1
            FROM pg_catalog.pg_class relation
            JOIN pg_catalog.pg_namespace namespace ON namespace.oid = relation.relnamespace
            WHERE namespace.nspname = 'public'
              AND relation.relname = 'checklist_migration_quarantine'
              AND relation.relrowsecurity
              AND relation.relforcerowsecurity) AS force_rls_verified
)
SELECT json_build_object(
    'capturedAtUtc', to_char(clock_timestamp() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
    'flywayVersion', flyway.current_version,
    'flywayHistorySha256', flyway.history_hash,
    'failedFlywayMigrationCount', flyway.failed_count,
    'legacySourceCount', source_manifest.row_count,
    'legacySourceSha256', source_manifest.row_hash,
    'unresolvedQuarantineCount', quarantine.unresolved_count,
    'unresolvedLegacyQuarantineSourceCount', quarantine.legacy_source_count,
    'unresolvedQuarantineReasons', quarantine_reasons.reasons,
    'quarantineVisibilityRoleAuthorized', security_preflight.role_authorized,
    'quarantineVisibilitySessionRoleAuthorized', security_preflight.session_role_authorized,
    'quarantineRoleNonBypassVerified', security_preflight.role_non_bypass_verified,
    'quarantineSelectGranted', security_preflight.select_granted,
    'quarantineRlsPolicyVerified', security_preflight.policy_verified,
    'quarantineForceRlsVerified', security_preflight.force_rls_verified
)::text
FROM source_manifest
CROSS JOIN quarantine
CROSS JOIN quarantine_reasons
CROSS JOIN flyway
CROSS JOIN security_preflight;
ROLLBACK;
'@

$requiredPreCaptureFlywayVersion = '20260729060000'

$postSql = @'
BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '10min';
SET LOCAL TIME ZONE 'UTC';
WITH source_base AS (
    SELECT
        legacy.checklist_item_id AS source_id,
        legacy.owner_user_id,
        legacy.template_entry_id,
        legacy.title,
        COALESCE(legacy.display_order, 0) AS source_display_order,
        legacy.status AS legacy_status,
        legacy.due_at,
        legacy.completed_at,
        legacy.created_at,
        legacy.updated_at,
        CASE
            WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN 'BABY'
            WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN 'JOURNEY'
        END AS care_context_type,
        CASE
            WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN legacy.baby_id
            WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN legacy.mother_journey_id
        END AS care_context_id,
        context_match.care_group_id,
        template_root.template_lineage_id,
        template_root.template_version_id,
        template_item.template_id AS template_item_version_id,
        COALESCE(template_item.target_subject,
            CASE WHEN legacy.baby_id IS NOT NULL THEN 'BABY' ELSE 'MOTHER' END) AS target_subject,
        CASE WHEN legacy.template_entry_id IS NULL THEN 'USER_CREATED' ELSE 'SYSTEM_TEMPLATE' END AS origin,
        CASE WHEN legacy.template_entry_id IS NOT NULL THEN
            (legacy.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
        END AS occurrence_date,
        jsonb_build_array(
            legacy.checklist_item_id,
            legacy.owner_user_id,
            legacy.mother_journey_id,
            legacy.baby_id,
            legacy.template_entry_id,
            legacy.title,
            COALESCE(legacy.display_order, 0),
            upper(legacy.status),
            legacy.due_at,
            legacy.completed_at,
            legacy.created_at,
            legacy.updated_at
        )::text AS source_normalized_row
    FROM public.preparation_checklist_items legacy
    LEFT JOIN public.care_item_templates template_item
      ON template_item.template_id = legacy.template_entry_id
     AND template_item.entry_type = 'CHECKLIST_ENTRY'
    LEFT JOIN public.care_item_templates template_root
      ON template_root.template_id = template_item.parent_template_id
     AND template_root.entry_type = 'TEMPLATE_ROOT'
    LEFT JOIN LATERAL (
        SELECT candidate.care_group_id
        FROM public.checklist_care_group_contexts candidate
        WHERE candidate.owner_user_id = legacy.owner_user_id
          AND candidate.review_status = 'REVIEWED'
          AND candidate.distribution_blocked = false
          AND ((legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL
                AND candidate.care_context_type = 'BABY'
                AND candidate.care_context_id = legacy.baby_id)
            OR (legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL
                AND candidate.care_context_type = 'JOURNEY'
                AND candidate.care_context_id = legacy.mother_journey_id))
        ORDER BY candidate.care_group_id
        LIMIT 1
    ) context_match ON true
), source_keyed AS (
    SELECT source.*, parent_key.value AS expected_parent_key
    FROM source_base source
    CROSS JOIN LATERAL (
        SELECT encode(sha256(convert_to(
            'v1' || COALESCE(string_agg(
                octet_length(convert_to(token, 'UTF8'))::text || ':' || token,
                '' ORDER BY ordinal), ''), 'UTF8')), 'hex') AS value
        FROM unnest(CASE WHEN source.origin = 'SYSTEM_TEMPLATE' THEN ARRAY[
                source.template_version_id::text,
                source.owner_user_id::text,
                'MOTHER',
                source.care_group_id::text,
                source.care_context_type,
                source.care_context_id::text,
                source.occurrence_date::text,
                source.occurrence_date::text]
            ELSE ARRAY[
                'LEGACY_USER_CREATED_PARENT',
                source.owner_user_id::text,
                'MOTHER',
                source.care_group_id::text,
                source.care_context_type,
                source.care_context_id::text,
                source.source_id::text]
            END) WITH ORDINALITY AS tokens(token, ordinal)
    ) parent_key
), source_identified AS (
    SELECT
        source.*,
        (substr(parent_identity.value, 1, 8) || '-' ||
         substr(parent_identity.value, 9, 4) || '-' ||
         substr(parent_identity.value, 13, 4) || '-' ||
         substr(parent_identity.value, 17, 4) || '-' ||
         substr(parent_identity.value, 21, 12))::uuid AS expected_parent_id
    FROM source_keyed source
    CROSS JOIN LATERAL (
        SELECT encode(sha256(convert_to(
            'v1' ||
            octet_length(convert_to('LEGACY_PARENT_ID', 'UTF8'))::text || ':LEGACY_PARENT_ID' ||
            octet_length(convert_to(source.expected_parent_key, 'UTF8'))::text || ':' || source.expected_parent_key,
            'UTF8')), 'hex') AS value
    ) parent_identity
), source_ordered AS (
    SELECT
        source.*,
        row_number() OVER (
            PARTITION BY expected_parent_id
            ORDER BY source_display_order, source_id) - 1 AS expected_display_order
    FROM source_identified source
), source_rows AS (
    SELECT
        source.*,
        jsonb_build_array(
            source.source_id,
            source.title,
            source.due_at,
            CASE
                WHEN upper(source.legacy_status) IN ('COMPLETED', 'DONE') THEN 'COMPLETED'
                WHEN upper(source.legacy_status) = 'SKIPPED' THEN 'SKIPPED'
                WHEN upper(source.legacy_status) = 'CANCELLED' THEN 'CANCELLED'
                WHEN upper(source.legacy_status) = 'IN_PROGRESS' THEN 'IN_PROGRESS'
                ELSE 'PENDING'
            END,
            CASE WHEN upper(source.legacy_status) IN ('COMPLETED', 'DONE')
                THEN COALESCE(source.completed_at, source.updated_at, source.created_at) END,
            CASE WHEN upper(source.legacy_status) = 'SKIPPED'
                THEN COALESCE(source.completed_at, source.updated_at, source.created_at) END,
            CASE WHEN upper(source.legacy_status) = 'CANCELLED'
                THEN COALESCE(source.updated_at, source.created_at) END,
            CASE
                WHEN upper(source.legacy_status) = 'SKIPPED' THEN 'LEGACY_SKIPPED'
                WHEN upper(source.legacy_status) = 'CANCELLED' THEN 'LEGACY_CANCELLED'
            END,
            source.target_subject,
            source.template_item_version_id,
            source.origin,
            source.expected_parent_id,
            source.expected_parent_key,
            source.owner_user_id,
            'MOTHER',
            source.care_group_id,
            source.care_context_type,
            source.care_context_id,
            source.owner_user_id,
            source.origin,
            NULL,
            NULL,
            source.template_lineage_id,
            source.template_version_id,
            task_key.value,
            'v1',
            'v1',
            source.expected_display_order,
            false,
            source.template_version_id,
            source.template_item_version_id
        )::text AS expected_target_row
    FROM source_ordered source
    CROSS JOIN LATERAL (
        SELECT encode(sha256(convert_to(
            'v1' || COALESCE(string_agg(
                octet_length(convert_to(token, 'UTF8'))::text || ':' || token,
                '' ORDER BY ordinal), ''), 'UTF8')), 'hex') AS value
        FROM unnest(CASE WHEN source.origin = 'SYSTEM_TEMPLATE' THEN ARRAY[
                source.expected_parent_id::text,
                source.template_item_version_id::text]
            ELSE ARRAY[
                source.expected_parent_id::text,
                'USER_CREATED',
                source.source_id::text]
            END) WITH ORDINALITY AS tokens(token, ordinal)
    ) task_key
), unresolved_quarantine AS (
    SELECT source_table, source_id, reason_code
    FROM public.checklist_migration_quarantine
    WHERE resolved_at IS NULL
), unresolved_legacy_sources AS (
    SELECT DISTINCT source_id
    FROM unresolved_quarantine
    WHERE source_table = 'preparation_checklist_items'
), eligible_source AS (
    SELECT source.*
    FROM source_rows source
    LEFT JOIN unresolved_legacy_sources quarantined ON quarantined.source_id = source.source_id
    WHERE quarantined.source_id IS NULL
), actual_target AS (
    SELECT
        task.checklist_task_instance_id AS source_id,
        jsonb_build_array(
            task.checklist_task_instance_id,
            task.title_snapshot,
            task.due_at,
            task.status,
            task.completed_at,
            task.skipped_at,
            task.cancelled_at,
            task.action_reason_code,
            task.target_subject,
            task.template_item_version_id,
            parent.origin,
            parent.checklist_instance_id,
            parent.distribution_key,
            parent.recipient_user_id,
            parent.recipient_role,
            parent.care_group_id,
            parent.care_context_type,
            parent.care_context_id,
            parent.context_owner_user_id,
            parent.origin,
            parent.window_start,
            parent.window_end,
            parent.template_lineage_id,
            parent.template_version_id,
            task.task_key,
            parent.key_version,
            task.key_version,
            task.display_order,
            task.is_required,
            task.template_version_id,
            task.template_item_version_id
        )::text AS actual_target_row
    FROM public.checklist_task_instances task
    JOIN public.checklist_instances parent
      ON parent.checklist_instance_id = task.checklist_instance_id
    JOIN source_rows source ON source.source_id = task.checklist_task_instance_id
), source_manifest AS (
    SELECT
        count(*)::bigint AS row_count,
        encode(sha256(convert_to(
            COALESCE(string_agg(source_normalized_row, E'\n' ORDER BY source_id), ''),
            'UTF8')), 'hex') AS row_hash
    FROM source_rows
), expected_manifest AS (
    SELECT
        count(*)::bigint AS row_count,
        encode(sha256(convert_to(
            COALESCE(string_agg(expected_target_row, E'\n' ORDER BY source_id), ''),
            'UTF8')), 'hex') AS row_hash
    FROM eligible_source
), actual_manifest AS (
    SELECT
        count(*)::bigint AS row_count,
        encode(sha256(convert_to(
            COALESCE(string_agg(actual.actual_target_row, E'\n' ORDER BY actual.source_id), ''),
            'UTF8')), 'hex') AS row_hash
    FROM actual_target actual
    JOIN eligible_source expected ON expected.source_id = actual.source_id
), reconciliation AS (
    SELECT
        count(*) FILTER (
            WHERE target.source_id IS NULL AND quarantined.source_id IS NULL)::bigint AS missing_outcome_count,
        count(*) FILTER (
            WHERE target.source_id IS NOT NULL AND quarantined.source_id IS NOT NULL)::bigint AS dual_outcome_count,
        count(*) FILTER (
            WHERE target.source_id IS NOT NULL
              AND quarantined.source_id IS NULL
              AND target.actual_target_row IS DISTINCT FROM source.expected_target_row)::bigint AS semantic_mismatch_count
    FROM source_rows source
    LEFT JOIN actual_target target ON target.source_id = source.source_id
    LEFT JOIN unresolved_legacy_sources quarantined ON quarantined.source_id = source.source_id
), quarantine AS (
    SELECT
        count(*)::bigint AS unresolved_count,
        count(DISTINCT source_id) FILTER (
            WHERE source_table = 'preparation_checklist_items')::bigint AS legacy_source_count
    FROM unresolved_quarantine
), quarantine_reasons AS (
    SELECT COALESCE(jsonb_object_agg(reason_code, reason_count ORDER BY reason_code), '{}'::jsonb) AS reasons
    FROM (
        SELECT reason_code, count(*)::bigint AS reason_count
        FROM unresolved_quarantine
        GROUP BY reason_code
    ) grouped_reasons
), flyway_rows AS (
    SELECT
        installed_rank,
        version,
        success,
        jsonb_build_array(
            installed_rank, version, description, type, script, checksum, success)::text AS normalized_row
    FROM public.flyway_schema_history
), flyway AS (
    SELECT
        max(version) FILTER (WHERE success) AS current_version,
        count(*) FILTER (WHERE NOT success)::bigint AS failed_count,
        encode(sha256(convert_to(
            COALESCE(string_agg(normalized_row, E'\n' ORDER BY installed_rank), ''),
            'UTF8')), 'hex') AS history_hash
    FROM flyway_rows
), security_preflight AS (
    SELECT
        current_user = 'checklist_operations' AS role_authorized,
        session_user = 'checklist_operations' AS session_role_authorized,
        EXISTS (
            SELECT 1
            FROM pg_catalog.pg_roles role
            WHERE role.rolname = current_user
              AND NOT role.rolsuper
              AND NOT role.rolbypassrls
              AND NOT role.rolcreatedb
              AND NOT role.rolcreaterole
              AND NOT role.rolinherit
              AND NOT EXISTS (
                  SELECT 1
                  FROM pg_catalog.pg_auth_members membership
                  WHERE membership.member = role.oid)) AS role_non_bypass_verified,
        has_table_privilege(current_user, 'public.checklist_migration_quarantine', 'SELECT') AS select_granted,
        EXISTS (
            SELECT 1
            FROM pg_catalog.pg_policies policy
            WHERE policy.schemaname = 'public'
              AND policy.tablename = 'checklist_migration_quarantine'
              AND policy.policyname = 'checklist_migration_quarantine_operations_select'
              AND policy.cmd = 'SELECT'
              AND policy.permissive = 'PERMISSIVE'
              AND policy.roles @> ARRAY['checklist_operations']::name[]
              AND policy.qual = 'true')
        AND NOT EXISTS (
            SELECT 1
            FROM pg_catalog.pg_policies policy
            WHERE policy.schemaname = 'public'
              AND policy.tablename = 'checklist_migration_quarantine'
              AND policy.cmd IN ('SELECT', 'ALL')
              AND policy.permissive = 'RESTRICTIVE'
              AND policy.roles && ARRAY['public', 'checklist_operations']::name[])
        AS policy_verified,
        EXISTS (
            SELECT 1
            FROM pg_catalog.pg_class relation
            JOIN pg_catalog.pg_namespace namespace ON namespace.oid = relation.relnamespace
            WHERE namespace.nspname = 'public'
              AND relation.relname = 'checklist_migration_quarantine'
              AND relation.relrowsecurity
              AND relation.relforcerowsecurity) AS force_rls_verified
)
SELECT json_build_object(
    'capturedAtUtc', to_char(clock_timestamp() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
    'flywayVersion', flyway.current_version,
    'flywayHistorySha256', flyway.history_hash,
    'failedFlywayMigrationCount', flyway.failed_count,
    'legacySourceCount', source_manifest.row_count,
    'legacySourceSha256', source_manifest.row_hash,
    'expectedTargetCount', expected_manifest.row_count,
    'actualTargetCount', actual_manifest.row_count,
    'expectedTargetSha256', expected_manifest.row_hash,
    'actualTargetSha256', actual_manifest.row_hash,
    'missingOutcomeCount', reconciliation.missing_outcome_count,
    'dualOutcomeCount', reconciliation.dual_outcome_count,
    'semanticMismatchCount', reconciliation.semantic_mismatch_count,
    'unresolvedQuarantineCount', quarantine.unresolved_count,
    'unresolvedLegacyQuarantineSourceCount', quarantine.legacy_source_count,
    'legacyQuarantineRateDefined',
        source_manifest.row_count > 0 OR quarantine.legacy_source_count = 0,
    'legacyQuarantineRatePercent', CASE
        WHEN source_manifest.row_count = 0 AND quarantine.legacy_source_count = 0 THEN 0.0
        WHEN source_manifest.row_count = 0 THEN NULL
        ELSE round((quarantine.legacy_source_count::numeric * 100.0) / source_manifest.row_count, 6)
    END,
    'unresolvedQuarantineReasons', quarantine_reasons.reasons,
    'quarantineVisibilityRoleAuthorized', security_preflight.role_authorized,
    'quarantineVisibilitySessionRoleAuthorized', security_preflight.session_role_authorized,
    'quarantineRoleNonBypassVerified', security_preflight.role_non_bypass_verified,
    'quarantineSelectGranted', security_preflight.select_granted,
    'quarantineRlsPolicyVerified', security_preflight.policy_verified,
    'quarantineForceRlsVerified', security_preflight.force_rls_verified
)::text
FROM source_manifest
CROSS JOIN expected_manifest
CROSS JOIN actual_manifest
CROSS JOIN reconciliation
CROSS JOIN quarantine
CROSS JOIN quarantine_reasons
CROSS JOIN flyway
CROSS JOIN security_preflight;
ROLLBACK;
'@

if ([string]::IsNullOrWhiteSpace($PsqlPath)) {
    throw 'CHECKLIST_REHEARSAL_PSQL_REQUIRED'
}
$psqlCommand = Get-Command -Name $PsqlPath -ErrorAction SilentlyContinue
if ($null -eq $psqlCommand -or [string]::IsNullOrWhiteSpace($psqlCommand.Source)) {
    throw 'CHECKLIST_REHEARSAL_PSQL_NOT_FOUND'
}
$script:ResolvedPsqlPath = $psqlCommand.Source
if ([System.IO.Path]::GetFileName($script:ResolvedPsqlPath) -ine 'psql.exe') {
    throw 'CHECKLIST_REHEARSAL_PSQL_EXECUTABLE_REQUIRED'
}
$psqlVersion = & $script:ResolvedPsqlPath --version 2>&1
if ($LASTEXITCODE -ne 0 -or "$psqlVersion" -notmatch 'psql \(PostgreSQL\) 18(?:\.|\s)') {
    throw 'CHECKLIST_REHEARSAL_PSQL_18_REQUIRED'
}
Assert-NoInheritedLibpqTarget
$databaseUri = Assert-SafeDatabaseUrl $DatabaseUrl
Assert-Sha256 $ReferenceDatasetFingerprint 'CHECKLIST_REHEARSAL_DATASET_FINGERPRINT_REQUIRED'
if ($Phase -ne 'CapturePre') {
    if ($ExpectedFinalFlywayVersion -notmatch '^[0-9]{14}$') {
        throw 'CHECKLIST_REHEARSAL_FINAL_FLYWAY_VERSION_REQUIRED'
    }
    Assert-Sha256 $ExpectedFlywayHistorySha256 'CHECKLIST_REHEARSAL_FLYWAY_HISTORY_HASH_REQUIRED'
}

if ([string]::IsNullOrWhiteSpace($EvidenceDirectory)) {
    throw 'CHECKLIST_REHEARSAL_EVIDENCE_DIRECTORY_REQUIRED'
}
$script:RepositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..\..')).Path
$script:EvidenceRoot = [System.IO.Path]::GetFullPath($EvidenceDirectory).TrimEnd('\')
if (-not (Test-Path -LiteralPath $script:EvidenceRoot -PathType Container)) {
    throw 'CHECKLIST_REHEARSAL_EVIDENCE_DIRECTORY_MISSING'
}
Assert-NotReparsePoint $script:EvidenceRoot 'CHECKLIST_REHEARSAL_EVIDENCE_DIRECTORY_REPARSE_FORBIDDEN'
$evidenceRoot = $script:EvidenceRoot
$runId = '{0}-{1}' -f (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssfffZ'), ([Guid]::NewGuid().ToString('N'))

$previousPassword = $env:PGPASSWORD
try {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    $rawDisposableRecord = Read-JsonFile $DisposableAttestationPath 'CHECKLIST_REHEARSAL_DISPOSABLE_ATTESTATION_REQUIRED'
    $null = Assert-ProtectedPgpass $databaseUri $rawDisposableRecord.value
    $disposableRecord = Assert-DisposableAttestation $DisposableAttestationPath $databaseUri
    $datasetRecord = Assert-ReferenceDatasetManifest $ReferenceDatasetManifestPath $ReferenceDatasetFingerprint
    $cohortRecord = Assert-CohortDisabled $CohortAttestationPath $ReferenceDatasetFingerprint $disposableRecord.value.runId

    if ($Phase -eq 'CapturePre') {
        $pre = Invoke-ReadOnlyJsonQuery $preSql
        if (-not [bool]$pre.quarantineVisibilityRoleAuthorized -or
                -not [bool]$pre.quarantineVisibilitySessionRoleAuthorized -or
                -not [bool]$pre.quarantineRoleNonBypassVerified -or
                -not [bool]$pre.quarantineSelectGranted -or
                -not [bool]$pre.quarantineRlsPolicyVerified -or
                -not [bool]$pre.quarantineForceRlsVerified) {
            throw 'CHECKLIST_REHEARSAL_QUARANTINE_VISIBILITY_PREFLIGHT_FAILED'
        }
        if ([long]$pre.failedFlywayMigrationCount -ne 0L) {
            throw 'CHECKLIST_REHEARSAL_PRE_CAPTURE_FLYWAY_FAILURE'
        }
        if ([long]$pre.legacySourceCount -ne [long]$datasetRecord.value.legacySourceCount -or
                $pre.legacySourceSha256 -ne $datasetRecord.value.legacySourceSha256) {
            throw 'CHECKLIST_REHEARSAL_DATASET_DATABASE_BINDING_MISMATCH'
        }
        if ("$($pre.flywayVersion)" -ne $requiredPreCaptureFlywayVersion) {
            throw "CHECKLIST_REHEARSAL_PRE_CAPTURE_TARGET_REQUIRED:$requiredPreCaptureFlywayVersion"
        }
        $manifest = [ordered]@{
            schemaVersion = 1
            runId = $runId
            phase = 'PRE_MIGRATION'
            capturedAtUtc = $pre.capturedAtUtc
            flywayVersion = $pre.flywayVersion
            flywayHistorySha256 = $pre.flywayHistorySha256
            failedFlywayMigrationCount = [long]$pre.failedFlywayMigrationCount
            referenceDatasetFingerprint = $ReferenceDatasetFingerprint
            datasetManifestSha256 = $datasetRecord.sha256
            legacySourceCount = [long]$pre.legacySourceCount
            legacySourceSha256 = $pre.legacySourceSha256
            unresolvedQuarantineCount = [long]$pre.unresolvedQuarantineCount
            unresolvedLegacyQuarantineSourceCount = [long]$pre.unresolvedLegacyQuarantineSourceCount
            unresolvedQuarantineReasons = $pre.unresolvedQuarantineReasons
            quarantineVisibilityRoleAuthorized = [bool]$pre.quarantineVisibilityRoleAuthorized
            quarantineVisibilitySessionRoleAuthorized = [bool]$pre.quarantineVisibilitySessionRoleAuthorized
            quarantineRoleNonBypassVerified = [bool]$pre.quarantineRoleNonBypassVerified
            quarantineSelectGranted = [bool]$pre.quarantineSelectGranted
            quarantineRlsPolicyVerified = [bool]$pre.quarantineRlsPolicyVerified
            quarantineForceRlsVerified = [bool]$pre.quarantineForceRlsVerified
            disposableRunId = $disposableRecord.value.runId
            disposableAttestationSha256 = $disposableRecord.sha256
            cohortAttestationSha256 = $cohortRecord.sha256
        }
        $manifestPath = Join-Path $evidenceRoot "$runId-pre-manifest.json"
        Write-SanitizedJson $manifestPath $manifest
        Write-Output "CHECKLIST_REHEARSAL_PRE_CAPTURED:$manifestPath"
        exit 0
    }

    $resolvedPreManifest = Resolve-RequiredFile $PreManifestPath 'CHECKLIST_REHEARSAL_PRE_MANIFEST_REQUIRED'
    $resolvedMetrics = Resolve-RequiredFile $MetricsPath 'CHECKLIST_REHEARSAL_METRICS_REQUIRED'
    $pre = Get-Content -Raw -LiteralPath $resolvedPreManifest | ConvertFrom-Json
    $metrics = Get-Content -Raw -LiteralPath $resolvedMetrics | ConvertFrom-Json
    if ([int]$pre.schemaVersion -ne 1 -or $pre.phase -ne 'PRE_MIGRATION') {
        throw 'CHECKLIST_REHEARSAL_PRE_MANIFEST_INVALID'
    }
    if ($pre.disposableRunId -ne $disposableRecord.value.runId -or
            $pre.disposableAttestationSha256 -ne $disposableRecord.sha256) {
        throw 'CHECKLIST_REHEARSAL_PRE_MANIFEST_DISPOSABLE_BINDING_INVALID'
    }
    if ($pre.datasetManifestSha256 -ne $datasetRecord.sha256 -or
            [long]$pre.legacySourceCount -ne [long]$datasetRecord.value.legacySourceCount -or
            $pre.legacySourceSha256 -ne $datasetRecord.value.legacySourceSha256) {
        throw 'CHECKLIST_REHEARSAL_PRE_MANIFEST_DATASET_BINDING_INVALID'
    }
    if ($pre.legacySourceSha256 -notmatch '^[0-9a-f]{64}$') {
        throw 'CHECKLIST_REHEARSAL_PRE_MANIFEST_HASH_INVALID'
    }
    if ($pre.referenceDatasetFingerprint -ne $ReferenceDatasetFingerprint) {
        throw 'CHECKLIST_REHEARSAL_DATASET_FINGERPRINT_MISMATCH'
    }
    Assert-MetricsContract $metrics
    if ($metrics.referenceDatasetFingerprint -ne $ReferenceDatasetFingerprint) {
        throw 'CHECKLIST_REHEARSAL_METRICS_DATASET_FINGERPRINT_MISMATCH'
    }
    $rawFlywayEvidence = Resolve-HashedEvidenceArtifact `
        "$($metrics.rawFlywayLogPath)" "$($metrics.rawFlywayLogSha256)" `
        'CHECKLIST_REHEARSAL_RAW_FLYWAY_EVIDENCE_INVALID'
    $rawLockEvidence = Resolve-HashedEvidenceArtifact `
        "$($metrics.rawLockLogPath)" "$($metrics.rawLockLogSha256)" `
        'CHECKLIST_REHEARSAL_RAW_LOCK_EVIDENCE_INVALID'
    $sanitizedMetrics = [ordered]@{
        expandMigrationSucceeded = [bool]$metrics.expandMigrationSucceeded
        remainderMigrationSucceeded = [bool]$metrics.remainderMigrationSucceeded
        flywayValidationSuccessful = [bool]$metrics.flywayValidationSuccessful
        lockSeconds = [double]$metrics.lockSeconds
        backfillRows = [long]$metrics.backfillRows
        backfillSeconds = [double]$metrics.backfillSeconds
        backfillRowsPerSecond = [double]$metrics.backfillRowsPerSecond
        expandMigrationSeconds = [double]$metrics.expandMigrationSeconds
        remainderMigrationSeconds = [double]$metrics.remainderMigrationSeconds
        fullMigrationSeconds = [double]$metrics.fullMigrationSeconds
        measurementStartedAtUtc = "$($metrics.measurementStartedAtUtc)"
        measurementCompletedAtUtc = "$($metrics.measurementCompletedAtUtc)"
        referenceDatasetFingerprint = "$($metrics.referenceDatasetFingerprint)"
        operatorEvidenceReference = "$($metrics.operatorEvidenceReference)"
        rawFlywayLogSha256 = $rawFlywayEvidence.sha256
        rawLockLogSha256 = $rawLockEvidence.sha256
    }

    $post = Invoke-ReadOnlyJsonQuery $postSql
    $derivedBackfillRowsPerSecond = [long]$metrics.backfillRows / [double]$metrics.backfillSeconds
    $reportedThroughputMatches = [Math]::Abs(
        [double]$metrics.backfillRowsPerSecond - $derivedBackfillRowsPerSecond) -le 0.01
    $measuredMigrationSum = [double]$metrics.expandMigrationSeconds + [double]$metrics.remainderMigrationSeconds
    $migrationSumMatches = [Math]::Abs([double]$metrics.fullMigrationSeconds - $measuredMigrationSum) -le 0.01
    $measurementStarted = [DateTimeOffset]::Parse(
        "$($metrics.measurementStartedAtUtc)", [System.Globalization.CultureInfo]::InvariantCulture)
    $measurementCompleted = [DateTimeOffset]::Parse(
        "$($metrics.measurementCompletedAtUtc)", [System.Globalization.CultureInfo]::InvariantCulture)
    $measurementWindowSeconds = ($measurementCompleted - $measurementStarted).TotalSeconds
    $rawFlywayWithinWindow = $rawFlywayEvidence.lastWriteAtUtc -ge $measurementStarted.UtcDateTime.AddSeconds(-2) -and
        $rawFlywayEvidence.lastWriteAtUtc -le $measurementCompleted.UtcDateTime.AddSeconds(2)
    $rawLockWithinWindow = $rawLockEvidence.lastWriteAtUtc -ge $measurementStarted.UtcDateTime.AddSeconds(-2) -and
        $rawLockEvidence.lastWriteAtUtc -le $measurementCompleted.UtcDateTime.AddSeconds(2)
    $checks = @(
        (New-Check 'flyway-expand-success' ([bool]$metrics.expandMigrationSucceeded) $metrics.expandMigrationSucceeded $true)
        (New-Check 'flyway-remainder-success' ([bool]$metrics.remainderMigrationSucceeded) $metrics.remainderMigrationSucceeded $true)
        (New-Check 'flyway-validation-success' ([bool]$metrics.flywayValidationSuccessful) $metrics.flywayValidationSuccessful $true)
        (New-Check 'flyway-history-has-no-failure' ([long]$post.failedFlywayMigrationCount -eq 0) ([long]$post.failedFlywayMigrationCount) 0)
        (New-Check 'flyway-final-version-reviewed' ($post.flywayVersion -eq $ExpectedFinalFlywayVersion) $post.flywayVersion $ExpectedFinalFlywayVersion)
        (New-Check 'flyway-history-checksums-reviewed' ($post.flywayHistorySha256 -eq $ExpectedFlywayHistorySha256) $post.flywayHistorySha256 $ExpectedFlywayHistorySha256)
        (New-Check 'dataset-fingerprint-bound' ($pre.referenceDatasetFingerprint -eq $metrics.referenceDatasetFingerprint) $metrics.referenceDatasetFingerprint $pre.referenceDatasetFingerprint)
        (New-Check 'quarantine-visibility-role-authorized' ([bool]$post.quarantineVisibilityRoleAuthorized) $post.quarantineVisibilityRoleAuthorized $true)
        (New-Check 'quarantine-visibility-session-role-authorized' ([bool]$post.quarantineVisibilitySessionRoleAuthorized) $post.quarantineVisibilitySessionRoleAuthorized $true)
        (New-Check 'quarantine-role-non-bypass-verified' ([bool]$post.quarantineRoleNonBypassVerified) $post.quarantineRoleNonBypassVerified $true)
        (New-Check 'quarantine-select-granted' ([bool]$post.quarantineSelectGranted) $post.quarantineSelectGranted $true)
        (New-Check 'quarantine-rls-policy-verified' ([bool]$post.quarantineRlsPolicyVerified) $post.quarantineRlsPolicyVerified $true)
        (New-Check 'quarantine-force-rls-verified' ([bool]$post.quarantineForceRlsVerified) $post.quarantineForceRlsVerified $true)
        (New-Check 'legacy-source-count-preserved' ([long]$post.legacySourceCount -eq [long]$pre.legacySourceCount) ([long]$post.legacySourceCount) ([long]$pre.legacySourceCount))
        (New-Check 'legacy-source-hash-preserved' ($post.legacySourceSha256 -eq $pre.legacySourceSha256) $post.legacySourceSha256 $pre.legacySourceSha256)
        (New-Check 'target-count-reconciled' ([long]$post.actualTargetCount -eq [long]$post.expectedTargetCount) ([long]$post.actualTargetCount) ([long]$post.expectedTargetCount))
        (New-Check 'target-hash-reconciled' ($post.actualTargetSha256 -eq $post.expectedTargetSha256) $post.actualTargetSha256 $post.expectedTargetSha256)
        (New-Check 'every-source-has-outcome' ([long]$post.missingOutcomeCount -eq 0) ([long]$post.missingOutcomeCount) 0)
        (New-Check 'every-source-has-exactly-one-outcome' ([long]$post.dualOutcomeCount -eq 0) ([long]$post.dualOutcomeCount) 0)
        (New-Check 'no-semantic-mismatch' ([long]$post.semanticMismatchCount -eq 0) ([long]$post.semanticMismatchCount) 0)
        (New-Check 'unresolved-quarantine-within-reviewed-count' ([long]$post.unresolvedQuarantineCount -le $MaxUnresolvedQuarantineCount) ([long]$post.unresolvedQuarantineCount) $MaxUnresolvedQuarantineCount)
        (New-Check 'legacy-quarantine-rate-defined' ([bool]$post.legacyQuarantineRateDefined) $post.legacyQuarantineRateDefined $true)
        (New-Check 'legacy-quarantine-within-reviewed-rate' ([double]$post.legacyQuarantineRatePercent -le $MaxLegacyQuarantineRatePercent) ([double]$post.legacyQuarantineRatePercent) $MaxLegacyQuarantineRatePercent)
        (New-Check 'blocking-lock-threshold' ([double]$metrics.lockSeconds -le $MaxLockSeconds) ([double]$metrics.lockSeconds) $MaxLockSeconds)
        (New-Check 'backfill-throughput-derived-from-raw-measurements' $reportedThroughputMatches ([double]$metrics.backfillRowsPerSecond) $derivedBackfillRowsPerSecond)
        (New-Check 'backfill-throughput-threshold' ($derivedBackfillRowsPerSecond -ge $MinBackfillRowsPerSecond) $derivedBackfillRowsPerSecond $MinBackfillRowsPerSecond)
        (New-Check 'full-duration-equals-expand-plus-remainder' $migrationSumMatches ([double]$metrics.fullMigrationSeconds) $measuredMigrationSum)
        (New-Check 'full-duration-fits-measurement-window' ([double]$metrics.fullMigrationSeconds -le $measurementWindowSeconds) ([double]$metrics.fullMigrationSeconds) $measurementWindowSeconds)
        (New-Check 'full-migration-duration-threshold' ([double]$metrics.fullMigrationSeconds -le $MaxFullMigrationSeconds) ([double]$metrics.fullMigrationSeconds) $MaxFullMigrationSeconds)
        (New-Check 'raw-flyway-log-bound-to-measurement-window' $rawFlywayWithinWindow $rawFlywayEvidence.lastWriteAtUtc $true)
        (New-Check 'raw-lock-log-bound-to-measurement-window' $rawLockWithinWindow $rawLockEvidence.lastWriteAtUtc $true)
        (New-Check 'cohort-remains-disabled' (-not [bool]$cohortRecord.value.cohortEnabled) $cohortRecord.value.cohortEnabled $false)
    )
    $failedChecks = @($checks | Where-Object { -not $_.passed })

    $previousAbort = $null
    $correctionRecord = $null
    if ($Phase -eq 'VerifyRollForward') {
        $resolvedAbort = Resolve-RequiredFile $PreviousAbortArtifact 'CHECKLIST_REHEARSAL_PREVIOUS_ABORT_ARTIFACT_REQUIRED'
        $previousAbort = Get-Content -Raw -LiteralPath $resolvedAbort | ConvertFrom-Json
        Assert-Sha256 $PreviousAbortArtifactSha256 'CHECKLIST_REHEARSAL_PREVIOUS_ABORT_HASH_REQUIRED'
        $actualPreviousAbortSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedAbort).Hash.ToLowerInvariant()
        $previousFailedChecks = @($previousAbort.checks | Where-Object {
            $_.passed -eq $false -and "$($_.name)" -match '^[a-z0-9][a-z0-9-]{0,119}$'
        })
        if ([int]$previousAbort.schemaVersion -ne 1 -or
                $previousAbort.phase -ne 'ProveAbort' -or
                $previousAbort.verdict -ne 'ABORT_PROVEN' -or
                [int]$previousAbort.failedCheckCount -lt 1 -or
                "$($previousAbort.runId)" -notmatch '^[0-9]{8}T[0-9]{9}Z-[0-9a-f]{32}$' -or
                $actualPreviousAbortSha256 -ne $PreviousAbortArtifactSha256 -or
                $previousAbort.datasetFingerprint -ne $pre.referenceDatasetFingerprint -or
                $previousAbort.preManifestSourceSha256 -ne $pre.legacySourceSha256 -or
                [long]$previousAbort.preManifestSourceCount -ne [long]$pre.legacySourceCount -or
                $previousFailedChecks.Count -ne [int]$previousAbort.failedCheckCount) {
            throw 'CHECKLIST_REHEARSAL_PREVIOUS_ABORT_ARTIFACT_INVALID'
        }
        if ([double]$previousAbort.thresholds.maxUnresolvedQuarantineCount -ne $MaxUnresolvedQuarantineCount -or
                [double]$previousAbort.thresholds.maxLegacyQuarantineRatePercent -ne $MaxLegacyQuarantineRatePercent -or
                [double]$previousAbort.thresholds.maxLockSeconds -ne $MaxLockSeconds -or
                [double]$previousAbort.thresholds.minBackfillRowsPerSecond -ne $MinBackfillRowsPerSecond -or
                [double]$previousAbort.thresholds.maxFullMigrationSeconds -ne $MaxFullMigrationSeconds) {
            throw 'CHECKLIST_REHEARSAL_ROLL_FORWARD_THRESHOLD_CHANGE_FORBIDDEN'
        }

        $correctionRecord = Read-JsonFile $CorrectionArtifactPath 'CHECKLIST_REHEARSAL_CORRECTION_ARTIFACT_REQUIRED'
        Assert-Sha256 $CorrectionArtifactSha256 'CHECKLIST_REHEARSAL_CORRECTION_ARTIFACT_HASH_REQUIRED'
        if ($correctionRecord.sha256 -ne $CorrectionArtifactSha256) {
            throw 'CHECKLIST_REHEARSAL_CORRECTION_ARTIFACT_HASH_MISMATCH'
        }
        $correction = $correctionRecord.value
        if ([int]$correction.schemaVersion -ne 1 -or
                $correction.datasetFingerprint -ne $ReferenceDatasetFingerprint -or
                $correction.previousFinalFlywayVersion -ne $previousAbort.expectedFinalFlywayVersion -or
                $correction.correctedFinalFlywayVersion -ne $ExpectedFinalFlywayVersion -or
                $correction.previousFlywayHistorySha256 -ne $previousAbort.expectedFlywayHistorySha256 -or
                $correction.correctedFlywayHistorySha256 -ne $ExpectedFlywayHistorySha256 -or
                "$($correction.correctionMigrationVersion)" -notmatch '^[0-9]{14}$' -or
                [long]$correction.correctionMigrationVersion -le [long]$correction.previousFinalFlywayVersion -or
                -not [bool]$correction.correctionApplied -or
                -not [bool]$correction.historyExtensionVerified -or
                [long]$correction.correctedHistoryRowCount -le [long]$correction.previousHistoryRowCount -or
                "$($correction.correctionSqlSha256)" -notmatch '^[0-9a-f]{64}$') {
            throw 'CHECKLIST_REHEARSAL_CORRECTION_ARTIFACT_INVALID'
        }
    }

    if ($Phase -eq 'ProveAbort') {
        $verdict = if ($failedChecks.Count -gt 0) { 'ABORT_PROVEN' } else { 'ABORT_NOT_PROVEN' }
    }
    elseif ($Phase -eq 'VerifyRollForward') {
        $verdict = if ($failedChecks.Count -eq 0) { 'ROLL_FORWARD_PASS' } else { 'ABORT' }
    }
    else {
        $verdict = if ($failedChecks.Count -eq 0) { 'PASS' } else { 'ABORT' }
    }

    $artifact = [ordered]@{
        schemaVersion = 1
        runId = $runId
        phase = $Phase
        verdict = $verdict
        capturedAtUtc = $post.capturedAtUtc
        datasetFingerprint = $pre.referenceDatasetFingerprint
        preManifestSourceSha256 = $pre.legacySourceSha256
        preManifestSourceCount = [long]$pre.legacySourceCount
        preManifestRunId = $pre.runId
        previousAbortRunId = if ($null -eq $previousAbort) { $null } else { $previousAbort.runId }
        correctionArtifactSha256 = if ($null -eq $correctionRecord) { $null } else { $correctionRecord.sha256 }
        disposableRunId = $disposableRecord.value.runId
        disposableAttestationSha256 = $disposableRecord.sha256
        datasetManifestSha256 = $datasetRecord.sha256
        cohortAttestationSha256 = $cohortRecord.sha256
        thresholds = [ordered]@{
            maxUnresolvedQuarantineCount = $MaxUnresolvedQuarantineCount
            maxLegacyQuarantineRatePercent = $MaxLegacyQuarantineRatePercent
            maxLockSeconds = $MaxLockSeconds
            minBackfillRowsPerSecond = $MinBackfillRowsPerSecond
            maxFullMigrationSeconds = $MaxFullMigrationSeconds
        }
        metrics = $sanitizedMetrics
        postManifest = $post
        checks = $checks
        failedCheckCount = $failedChecks.Count
        expectedFinalFlywayVersion = $ExpectedFinalFlywayVersion
        expectedFlywayHistorySha256 = $ExpectedFlywayHistorySha256
    }
    $artifactPath = Join-Path $evidenceRoot "$runId-$($Phase.ToLowerInvariant())-verdict.json"
    Write-SanitizedJson $artifactPath $artifact

    if ($verdict -eq 'PASS' -or $verdict -eq 'ROLL_FORWARD_PASS' -or $verdict -eq 'ABORT_PROVEN') {
        Write-Output "CHECKLIST_REHEARSAL_${verdict}:$artifactPath"
        exit 0
    }
    [Console]::Error.WriteLine("CHECKLIST_REHEARSAL_${verdict}:$artifactPath")
    exit 2
}
finally {
    if ($null -eq $previousPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
    else {
        $env:PGPASSWORD = $previousPassword
    }
}
