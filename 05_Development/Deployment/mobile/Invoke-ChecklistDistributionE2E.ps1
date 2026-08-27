[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$FlutterExecutable,
    [Parameter(Mandatory = $true)]
    [string]$DeviceId,
    [Parameter(Mandatory = $true)]
    [string]$DefinesFile,
    [Parameter(Mandatory = $true)]
    [string]$AcknowledgedApiOrigin,
    [Parameter(Mandatory = $true)]
    [string]$EvidenceDirectory
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:FlutterTestExitCode = $null

$targetTestPath = 'integration_test/checklist_distribution_e2e_test.dart'
$targetTestName = 'CHK-042/043 author, approve, distribute and complete isolated Mother/Family tasks via live API'
$requiredActors = @('CONTENT_ADMIN', 'ADMIN', 'MOTHER', 'FAMILY', 'ISOLATION_FAMILY')
$requiredKeys = @(
    'API_BASE_URL'
    'CHK_API_E2E'
    'CHK_E2E_ENVIRONMENT'
    'CHK_E2E_SERVER_ENVIRONMENT_ID'
    'CHK_E2E_EXPECTED_API_BASE_URL'
    'CHK_E2E_DEVICE_ACK'
    'CHK_E2E_CREDENTIAL_ARTIFACT_ACK'
)
foreach ($actor in $requiredActors) {
    $requiredKeys += "CHK_${actor}_ACCESS_TOKEN"
    $requiredKeys += "CHK_${actor}_REFRESH_TOKEN"
    $requiredKeys += "CHK_${actor}_USER_ID"
}

function Write-Utf8NoBom([string]$Path, [string]$Value) {
    [System.IO.File]::WriteAllText($Path, $Value, (New-Object System.Text.UTF8Encoding($false)))
}

function Invoke-BufferedNative([string]$Executable, [string[]]$Arguments) {
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        # Windows PowerShell materializes redirected native stderr as ErrorRecord.
        # Continue is required here so stderr remains buffered instead of escaping
        # before it can be redacted.
        $ErrorActionPreference = 'Continue'
        $lines = @(& $Executable @Arguments 2>&1 | ForEach-Object { "$_" })
        $exitCode = $LASTEXITCODE
        return [pscustomobject]@{ Lines = $lines; ExitCode = $exitCode }
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Resolve-RequiredAbsoluteFile([string]$Path, [string]$ErrorCode) {
    if ([string]::IsNullOrWhiteSpace($Path) -or -not [System.IO.Path]::IsPathRooted($Path)) {
        throw "${ErrorCode}_ABSOLUTE_PATH_REQUIRED"
    }
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "${ErrorCode}_NOT_FOUND"
    }
    return (Resolve-Path -LiteralPath $Path).Path
}

function Test-PathWithin([string]$Candidate, [string]$Parent) {
    $candidatePath = [System.IO.Path]::GetFullPath($Candidate)
    $parentPath = [System.IO.Path]::GetFullPath($Parent).TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    return $candidatePath.StartsWith($parentPath, [System.StringComparison]::OrdinalIgnoreCase)
}

function Assert-DefinesArtifactBoundary([string]$ResolvedDefines, [string]$RepositoryRoot) {
    if (-not (Test-PathWithin $ResolvedDefines $RepositoryRoot)) {
        return
    }
    $repositoryPrefix = [System.IO.Path]::GetFullPath($RepositoryRoot).TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    $relativePath = $ResolvedDefines.Substring($repositoryPrefix.Length).Replace('\', '/')
    $ignoreCheck = Invoke-BufferedNative 'git' @('-C', $RepositoryRoot, 'check-ignore', '--quiet', '--', $relativePath)
    if ($ignoreCheck.ExitCode -ne 0) {
        throw 'CHECKLIST_E2E_DEFINES_MUST_BE_EXTERNAL_OR_GITIGNORED'
    }
}

function Read-ValidatedDefines([string]$Path) {
    try {
        $document = Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
    }
    catch {
        throw 'CHECKLIST_E2E_DEFINES_JSON_INVALID'
    }
    if ($null -eq $document -or $document -isnot [psobject]) {
        throw 'CHECKLIST_E2E_DEFINES_JSON_OBJECT_REQUIRED'
    }

    $properties = @($document.PSObject.Properties)
    $actualNames = @($properties | ForEach-Object { $_.Name })
    foreach ($requiredKey in $requiredKeys) {
        if ($actualNames -cnotcontains $requiredKey) {
            throw "CHECKLIST_E2E_DEFINE_REQUIRED:$requiredKey"
        }
    }
    $allowedKeys = @($requiredKeys) + 'CHK_E2E_ALLOW_LOOPBACK_HTTP'
    foreach ($actualName in $actualNames) {
        if ($allowedKeys -cnotcontains $actualName) {
            throw "CHECKLIST_E2E_DEFINE_UNEXPECTED:$actualName"
        }
    }

    $values = New-Object 'System.Collections.Generic.Dictionary[string,string]' ([System.StringComparer]::Ordinal)
    foreach ($property in $properties) {
        if ($property.Value -isnot [string] -or [string]::IsNullOrWhiteSpace([string]$property.Value)) {
            throw "CHECKLIST_E2E_DEFINE_NONEMPTY_STRING_REQUIRED:$($property.Name)"
        }
        $values.Add($property.Name, [string]$property.Value)
    }
    return $values
}

function Get-NormalizedOrigin([string]$Value, [string]$ErrorCode) {
    $origin = $null
    if ([string]::IsNullOrWhiteSpace($Value) -or
            -not [Uri]::TryCreate($Value, [UriKind]::Absolute, [ref]$origin) -or
            ($origin.Scheme -ine 'http' -and $origin.Scheme -ine 'https') -or
            [string]::IsNullOrWhiteSpace($origin.Host) -or
            -not [string]::IsNullOrWhiteSpace($origin.UserInfo) -or
            -not [string]::IsNullOrWhiteSpace($origin.Query) -or
            -not [string]::IsNullOrWhiteSpace($origin.Fragment) -or
            ($origin.AbsolutePath -ne '/' -and -not [string]::IsNullOrWhiteSpace($origin.AbsolutePath))) {
        throw $ErrorCode
    }
    return $origin.GetLeftPart([UriPartial]::Authority).TrimEnd('/').ToLowerInvariant()
}

function Get-SecretValues([System.Collections.Generic.Dictionary[string,string]]$Defines) {
    $secrets = @()
    foreach ($key in $Defines.Keys) {
        if ($key -match '_(?:ACCESS_TOKEN|REFRESH_TOKEN|USER_ID)$') {
            $value = $Defines[$key]
            $encodedDefine = [Convert]::ToBase64String(
                [Text.Encoding]::UTF8.GetBytes("$key=$value"))
            $secrets += [pscustomobject]@{
                Key = $key
                Value = $value
                EncodedDefine = $encodedDefine
            }
        }
    }
    return @($secrets | Sort-Object { $_.Value.Length } -Descending)
}

function Protect-Text([string]$Value, [object[]]$Secrets) {
    $protected = if ($null -eq $Value) { '' } else { $Value }
    foreach ($secret in $Secrets) {
        if (-not [string]::IsNullOrEmpty($secret.EncodedDefine)) {
            $protected = $protected.Replace(
                $secret.EncodedDefine,
                "[REDACTED_BASE64:$($secret.Key)]")
        }
    }
    foreach ($secret in $Secrets) {
        if (-not [string]::IsNullOrEmpty($secret.Value)) {
            $protected = $protected.Replace($secret.Value, "[REDACTED:$($secret.Key)]")
        }
    }
    return $protected
}

function Test-ExpectedTargetUrl([string]$Url, [string]$MobileAppRoot) {
    if ([string]::IsNullOrWhiteSpace($Url)) {
        return $false
    }
    try {
        $expected = [System.IO.Path]::GetFullPath((Join-Path $MobileAppRoot $targetTestPath))
        $parsed = $null
        if ([Uri]::TryCreate($Url, [UriKind]::Absolute, [ref]$parsed) -and $parsed.IsFile) {
            $candidate = [System.IO.Path]::GetFullPath($parsed.LocalPath)
        }
        elseif ([System.IO.Path]::IsPathRooted($Url)) {
            $candidate = [System.IO.Path]::GetFullPath($Url)
        }
        else {
            $candidate = [System.IO.Path]::GetFullPath((Join-Path $MobileAppRoot $Url))
        }
        return $candidate.Equals($expected, [System.StringComparison]::OrdinalIgnoreCase)
    }
    catch {
        return $false
    }
}

function Test-ExplicitFileReference([string]$Url) {
    if ([string]::IsNullOrWhiteSpace($Url)) {
        return $false
    }
    $parsed = $null
    if ([Uri]::TryCreate($Url, [UriKind]::Absolute, [ref]$parsed)) {
        return $parsed.IsFile
    }
    return [System.IO.Path]::IsPathRooted($Url) -or $Url.EndsWith('.dart', [System.StringComparison]::OrdinalIgnoreCase)
}

function Convert-MachineEvidence([string[]]$RawLines, [string]$MobileAppRoot) {
    $events = @()
    $malformedEventCount = 0
    foreach ($line in $RawLines) {
        $trimmed = "$line".Trim()
        if (-not $trimmed.StartsWith('{')) {
            continue
        }
        try {
            $event = $trimmed | ConvertFrom-Json
            if ($null -ne $event.type) {
                $events += $event
            }
        }
        catch {
            $malformedEventCount++
        }
    }

    $suitePathsById = @{}
    $suiteCountsById = @{}
    foreach ($event in @($events | Where-Object { $_.type -eq 'suite' })) {
        $suiteIdProperty = if ($null -eq $event.suite) { $null } else {
            $event.suite.PSObject.Properties['id']
        }
        $suitePathProperty = if ($null -eq $event.suite) { $null } else {
            $event.suite.PSObject.Properties['path']
        }
        if ($null -eq $suiteIdProperty -or $null -eq $suiteIdProperty.Value -or
                $null -eq $suitePathProperty -or
                [string]::IsNullOrWhiteSpace("$($suitePathProperty.Value)")) {
            $malformedEventCount++
            continue
        }
        $suiteId = "$($suiteIdProperty.Value)"
        if ($suiteCountsById.ContainsKey($suiteId)) {
            $suiteCountsById[$suiteId]++
            $malformedEventCount++
        }
        else {
            $suiteCountsById[$suiteId] = 1
            $suitePathsById[$suiteId] = "$($suitePathProperty.Value)"
        }
    }

    $testNamesById = @{}
    $targetIds = @()
    foreach ($event in @($events | Where-Object { $_.type -eq 'testStart' })) {
        if ($null -eq $event.test -or $null -eq $event.test.id -or $null -eq $event.test.name) {
            $malformedEventCount++
            continue
        }
        $id = "$($event.test.id)"
        $name = "$($event.test.name)"
        $testNamesById[$id] = $name
        $url = if ($null -eq $event.test.url) { '' } else { "$($event.test.url)" }
        $suiteIdProperty = $event.test.PSObject.Properties['suiteID']
        if ($null -ne $suiteIdProperty -and $null -ne $suiteIdProperty.Value) {
            $suiteId = "$($suiteIdProperty.Value)"
            if (-not $suiteCountsById.ContainsKey($suiteId) -or
                    $suiteCountsById[$suiteId] -ne 1) {
                $malformedEventCount++
                $matchesTargetFile = $false
            }
            else {
                $suitePath = "$($suitePathsById[$suiteId])"
                $matchesTargetFile = Test-ExpectedTargetUrl $suitePath $MobileAppRoot
                if ($matchesTargetFile -and (Test-ExplicitFileReference $url)) {
                    $matchesTargetFile = Test-ExpectedTargetUrl $url $MobileAppRoot
                }
            }
        }
        else {
            $matchesTargetFile = Test-ExpectedTargetUrl $url $MobileAppRoot
        }
        if ($name -ceq $targetTestName -and $matchesTargetFile) {
            $targetIds += $id
        }
    }

    $testDoneEvents = @($events | Where-Object { $_.type -eq 'testDone' })
    $visibleTestDoneCount = @($testDoneEvents | Where-Object { $_.hidden -ne $true }).Count
    $skippedTestCount = @($testDoneEvents | Where-Object { $_.skipped -eq $true }).Count
    $successfulTargetTestCount = 0
    foreach ($targetId in $targetIds) {
        $matchingDone = @($testDoneEvents | Where-Object { "$($_.testID)" -eq $targetId })
        if ($matchingDone.Count -eq 1 -and
                $matchingDone[0].hidden -ne $true -and
                $matchingDone[0].skipped -eq $false -and
                $matchingDone[0].result -eq 'success') {
            $successfulTargetTestCount++
        }
    }
    $doneEvents = @($events | Where-Object { $_.type -eq 'done' })
    $overallSuccess = $doneEvents.Count -eq 1 -and $doneEvents[0].success -eq $true

    return [ordered]@{
        targetTestCount = $targetIds.Count
        successfulTargetTestCount = $successfulTargetTestCount
        visibleTestDoneCount = $visibleTestDoneCount
        skippedTestCount = $skippedTestCount
        malformedMachineEventCount = $malformedEventCount
        overallDoneEventCount = $doneEvents.Count
        overallSuccess = $overallSuccess
    }
}

try {
    $repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
    $mobileAppRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\CareBridgeMobileApp'))
    if (-not (Test-Path -LiteralPath (Join-Path $mobileAppRoot 'pubspec.yaml') -PathType Leaf)) {
        throw 'CHECKLIST_E2E_MOBILE_APP_ROOT_INVALID'
    }
    $resolvedFlutter = Resolve-RequiredAbsoluteFile $FlutterExecutable 'CHECKLIST_E2E_FLUTTER'
    $resolvedDefines = Resolve-RequiredAbsoluteFile $DefinesFile 'CHECKLIST_E2E_DEFINES'
    if ([string]::IsNullOrWhiteSpace($EvidenceDirectory)) {
        throw 'CHECKLIST_E2E_EVIDENCE_DIRECTORY_REQUIRED'
    }
    if ([string]::IsNullOrWhiteSpace($DeviceId) -or $DeviceId -notmatch '^[A-Za-z0-9][A-Za-z0-9._:-]{1,199}$') {
        throw 'CHECKLIST_E2E_DEDICATED_DEVICE_ID_INVALID'
    }
    Assert-DefinesArtifactBoundary $resolvedDefines $repositoryRoot
    $defines = Read-ValidatedDefines $resolvedDefines

    if ($defines['CHK_API_E2E'] -cne 'true') {
        throw 'CHECKLIST_E2E_ENABLE_ACK_REQUIRED'
    }
    if ($defines['CHK_E2E_ENVIRONMENT'] -cne 'DISPOSABLE_NON_PRODUCTION') {
        throw 'CHECKLIST_E2E_PRODUCTION_ENVIRONMENT_REFUSED'
    }
    if ($defines['CHK_E2E_SERVER_ENVIRONMENT_ID'] -notmatch '^e2e-[A-Za-z0-9][A-Za-z0-9._:-]{2,79}$' -or
            $defines['CHK_E2E_SERVER_ENVIRONMENT_ID'] -match '(?i)(?:^|[-_.])(?:prod(?:uction)?|live)[A-Za-z0-9]*(?:[-_.]|$)') {
        throw 'CHECKLIST_E2E_SERVER_ENVIRONMENT_ID_UNSAFE'
    }
    if ($defines['CHK_E2E_DEVICE_ACK'] -cne 'DEDICATED_DEVICE_CONFIRMED') {
        throw 'CHECKLIST_E2E_DEDICATED_DEVICE_ACK_REQUIRED'
    }
    if ($defines['CHK_E2E_CREDENTIAL_ARTIFACT_ACK'] -cne 'COMPILED_CREDENTIAL_ARTIFACT_ACCEPTED') {
        throw 'CHECKLIST_E2E_CREDENTIAL_ARTIFACT_ACK_REQUIRED'
    }

    $compiledOrigin = Get-NormalizedOrigin $defines['API_BASE_URL'] 'CHECKLIST_E2E_API_ORIGIN_INVALID'
    $definesAcknowledgedOrigin = Get-NormalizedOrigin $defines['CHK_E2E_EXPECTED_API_BASE_URL'] 'CHECKLIST_E2E_EXPECTED_ORIGIN_INVALID'
    $operatorAcknowledgedOrigin = Get-NormalizedOrigin $AcknowledgedApiOrigin 'CHECKLIST_E2E_OPERATOR_ORIGIN_INVALID'
    if ($compiledOrigin -cne $definesAcknowledgedOrigin -or $compiledOrigin -cne $operatorAcknowledgedOrigin) {
        throw 'CHECKLIST_E2E_UNACKNOWLEDGED_ORIGIN_REFUSED'
    }
    $compiledUri = [Uri]$compiledOrigin
    if ($compiledUri.Host -match '(?i)(?:^|[-_.])(?:prod(?:uction)?|live|www)[A-Za-z0-9]*(?:[-_.]|$)') {
        throw 'CHECKLIST_E2E_PRODUCTION_ORIGIN_REFUSED'
    }
    if ($compiledUri.Scheme -ieq 'http') {
        $loopbackHosts = @('localhost', '127.0.0.1', '::1', '10.0.2.2')
        if ($loopbackHosts -notcontains $compiledUri.Host.ToLowerInvariant() -or
                -not $defines.ContainsKey('CHK_E2E_ALLOW_LOOPBACK_HTTP') -or
                $defines['CHK_E2E_ALLOW_LOOPBACK_HTTP'] -cne 'LOOPBACK_ONLY_CONFIRMED') {
            throw 'CHECKLIST_E2E_INSECURE_ORIGIN_REFUSED'
        }
    }

    $secretValues = Get-SecretValues $defines
    $deviceArguments = @('devices', '--machine')
    $deviceInvocation = Invoke-BufferedNative $resolvedFlutter $deviceArguments
    $rawDeviceOutput = @($deviceInvocation.Lines)
    $deviceExitCode = $deviceInvocation.ExitCode
    if ($deviceExitCode -ne 0) {
        throw 'CHECKLIST_E2E_DEVICE_DISCOVERY_FAILED'
    }
    try {
        $parsedDevices = ($rawDeviceOutput -join [Environment]::NewLine) | ConvertFrom-Json
        # Windows PowerShell 5.1 keeps a JSON array as one nested Object[] value.
        # Re-entering the pipeline enumerates it so exact device matching works
        # both for a single attached target and for real multi-device hosts.
        $devices = @($parsedDevices | ForEach-Object { $_ })
    }
    catch {
        throw 'CHECKLIST_E2E_DEVICE_DISCOVERY_JSON_INVALID'
    }
    $matchingDevices = @($devices | Where-Object { "$($_.id)" -ceq $DeviceId })
    if ($matchingDevices.Count -ne 1) {
        throw 'CHECKLIST_E2E_DEDICATED_DEVICE_NOT_FOUND'
    }
    $targetDevice = $matchingDevices[0]
    if ($targetDevice.isSupported -ne $true -or "$($targetDevice.targetPlatform)" -notmatch '^(android|ios)(-|$)') {
        throw 'CHECKLIST_E2E_NATIVE_DEVICE_REQUIRED'
    }

    $evidenceRoot = [System.IO.Path]::GetFullPath($EvidenceDirectory)
    [System.IO.Directory]::CreateDirectory($evidenceRoot) | Out-Null
    $runId = '{0}-{1}' -f (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssfffZ'), ([Guid]::NewGuid().ToString('N'))
    $startedAtUtc = (Get-Date).ToUniversalTime()
    $testArguments = @(
        'test'
        $targetTestPath
        '-d'
        $DeviceId
        '--machine'
        "--dart-define-from-file=$resolvedDefines"
    )
    Push-Location $mobileAppRoot
    try {
        $testInvocation = Invoke-BufferedNative $resolvedFlutter $testArguments
    }
    finally {
        Pop-Location
    }
    $rawTestOutput = @($testInvocation.Lines)
    $script:FlutterTestExitCode = $testInvocation.ExitCode
    $completedAtUtc = (Get-Date).ToUniversalTime()

    $machineEvidence = Convert-MachineEvidence $rawTestOutput $mobileAppRoot
    $gatePassed = $script:FlutterTestExitCode -eq 0 -and
        $machineEvidence.targetTestCount -eq 1 -and
        $machineEvidence.successfulTargetTestCount -eq 1 -and
        $machineEvidence.visibleTestDoneCount -eq 1 -and
        $machineEvidence.skippedTestCount -eq 0 -and
        $machineEvidence.malformedMachineEventCount -eq 0 -and
        $machineEvidence.overallDoneEventCount -eq 1 -and
        $machineEvidence.overallSuccess
    $verdict = if ($gatePassed) { 'PASS' } else { 'FAIL' }

    $sanitizedLines = @('[device-discovery]')
    $sanitizedLines += @($rawDeviceOutput | ForEach-Object { Protect-Text "$_" $secretValues })
    $sanitizedLines += '[flutter-test]'
    $sanitizedLines += @($rawTestOutput | ForEach-Object { Protect-Text "$_" $secretValues })
    $logPath = Join-Path $evidenceRoot "$runId-checklist-e2e.log"
    Write-Utf8NoBom $logPath (($sanitizedLines -join [Environment]::NewLine) + [Environment]::NewLine)
    $logSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $logPath).Hash.ToLowerInvariant()

    $evidence = [ordered]@{
        schemaVersion = 1
        runId = $runId
        verdict = $verdict
        startedAtUtc = $startedAtUtc.ToString('o')
        completedAtUtc = $completedAtUtc.ToString('o')
        target = $targetTestPath
        deviceId = $DeviceId
        targetPlatform = "$($targetDevice.targetPlatform)"
        apiOrigin = $compiledOrigin
        serverEnvironmentId = $defines['CHK_E2E_SERVER_ENVIRONMENT_ID']
        flutterExitCode = $script:FlutterTestExitCode
        targetTestCount = $machineEvidence.targetTestCount
        successfulTargetTestCount = $machineEvidence.successfulTargetTestCount
        visibleTestDoneCount = $machineEvidence.visibleTestDoneCount
        skippedTestCount = $machineEvidence.skippedTestCount
        malformedMachineEventCount = $machineEvidence.malformedMachineEventCount
        overallDoneEventCount = $machineEvidence.overallDoneEventCount
        overallSuccess = $machineEvidence.overallSuccess
        sanitizedLogFile = [System.IO.Path]::GetFileName($logPath)
        sanitizedLogSha256 = $logSha256
    }
    $evidencePath = Join-Path $evidenceRoot "$runId-checklist-e2e-evidence.json"
    Write-Utf8NoBom $evidencePath ($evidence | ConvertTo-Json -Depth 5)

    if ($gatePassed) {
        Write-Output "CHECKLIST_E2E_PASS:$evidencePath"
        exit 0
    }
    [Console]::Error.WriteLine("CHECKLIST_E2E_FAIL:$evidencePath")
    if ($script:FlutterTestExitCode -ne 0) {
        exit $script:FlutterTestExitCode
    }
    exit 3
}
catch {
    [Console]::Error.WriteLine("CHECKLIST_E2E_REFUSED:$($_.Exception.Message)")
    if ($null -ne $script:FlutterTestExitCode -and $script:FlutterTestExitCode -ne 0) {
        exit $script:FlutterTestExitCode
    }
    exit 2
}
