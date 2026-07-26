[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string[]]$ScenarioId,
    [string[]]$GateId = @(),
    [string]$RegistryPath,
    [ValidateSet('Release', 'Diagnostic')][string]$RunMode = 'Release',
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$BaselineHead,
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$TouchedPathsFile,
    [string]$ManualEvidenceSummaryPath,
    [string]$CheckpointKeyPath,
    [switch]$Resume,
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$OutputDirectory
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'Ov01Evidence.psm1') -Force

if ([string]::IsNullOrWhiteSpace($RegistryPath)) { $RegistryPath = Join-Path $PSScriptRoot 'ov01-scenario-registry.json' }
if ($Resume -and $RunMode -ne 'Release') { throw '-Resume is valid only with -RunMode Release; Diagnostic cannot consume a Release checkpoint' }
if ($RunMode -eq 'Release' -and [string]::IsNullOrWhiteSpace($CheckpointKeyPath)) { throw 'Release mode requires an explicit absolute -CheckpointKeyPath outside the repository and evidence directory' }

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
$registryFull = [System.IO.Path]::GetFullPath($RegistryPath)
$canonicalRegistryFull = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'ov01-scenario-registry.json'))
if ($RunMode -eq 'Release' -and -not $registryFull.Equals($canonicalRegistryFull, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Release mode requires canonical registry path: $canonicalRegistryFull"
}
$registry = Read-Ov01Registry -Path $RegistryPath
$registryValidation = Test-Ov01Registry -Registry $registry -RepoRoot $repoRoot
if (-not $registryValidation.valid) { throw "Registry validation failed:`n$($registryValidation.errors -join "`n")" }

$requestedScenarioIds = @($ScenarioId | ForEach-Object { ([string]$_).Trim().ToUpperInvariant() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
if ($requestedScenarioIds.Count -eq 0) { throw 'At least one scenario ID is required; empty selection is forbidden' }
if (@($requestedScenarioIds | Select-Object -Unique).Count -ne $requestedScenarioIds.Count) { throw 'Duplicate scenario selections are forbidden' }
if ($RunMode -eq 'Release' -and ($requestedScenarioIds.Count -ne 1 -or $requestedScenarioIds[0] -ne 'ALL')) {
    throw 'Release mode requires -ScenarioId ALL; focused or subset runs must use Diagnostic mode'
}
$allScenarioIds = @($registry.scenarios | ForEach-Object { [string]$_.id })
if ($requestedScenarioIds -contains 'ALL') {
    if ($requestedScenarioIds.Count -ne 1) { throw 'ALL cannot be combined with explicit scenario IDs' }
    $selectedScenarioIds = $allScenarioIds
    $includeAllPlatformGates = $true
} else {
    $selectedScenarioIds = @($requestedScenarioIds | Sort-Object -Unique)
    $includeAllPlatformGates = $false
    foreach ($id in $selectedScenarioIds) { if ($allScenarioIds -notcontains $id) { throw "Unknown scenario ID: $id" } }
}

$selectedScenarios = @($registry.scenarios | Where-Object { $selectedScenarioIds -contains [string]$_.id })
$selectedGateIds = New-Object System.Collections.Generic.List[string]
foreach ($scenario in $selectedScenarios) { foreach ($id in @($scenario.gateIds)) { $selectedGateIds.Add([string]$id) } }
foreach ($id in @($GateId | ForEach-Object { ([string]$_).Trim().ToUpperInvariant() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })) {
    if ($id -eq 'ALL') { foreach ($known in @($registry.gates.id)) { $selectedGateIds.Add([string]$known) } } else { $selectedGateIds.Add($id) }
}
if ($includeAllPlatformGates) { foreach ($id in @($registry.requiredPlatformGateIds)) { $selectedGateIds.Add([string]$id) } }
$selectedGateIds = @($selectedGateIds | Sort-Object -Unique)
if ($selectedGateIds.Count -eq 0) { throw 'Resolved gate selection is empty; refusing a false-green run' }
$knownGateIds = @($registry.gates | ForEach-Object { [string]$_.id })
foreach ($id in $selectedGateIds) { if ($knownGateIds -notcontains $id) { throw "Unknown gate ID: $id" } }
if ($RunMode -eq 'Release') {
    [string[]]$expectedReleaseGateIds = @(Get-Ov01CanonicalReleaseGateIds)
    if ($selectedScenarioIds.Count -ne $allScenarioIds.Count -or @(Compare-Object $allScenarioIds $selectedScenarioIds).Count -ne 0) {
        throw 'Release mode did not resolve the exact canonical all-scenario set'
    }
    if ($selectedGateIds.Count -ne $expectedReleaseGateIds.Count -or @(Compare-Object $expectedReleaseGateIds $selectedGateIds).Count -ne 0) {
        throw 'Release mode did not resolve the exact canonical release-gate set'
    }
    if ([string]::IsNullOrWhiteSpace($ManualEvidenceSummaryPath)) {
        throw 'Release mode requires -ManualEvidenceSummaryPath for the exact final OV01-MAN-001..034 bundle'
    }
    $manualEvidencePreflight = Test-Ov01ManualEvidence -SummaryPath $ManualEvidenceSummaryPath -RepoRoot $repoRoot
    if (-not $manualEvidencePreflight.valid) {
        throw "Manual evidence validation failed before release execution:`n$($manualEvidencePreflight.errors -join "`n")"
    }
}

$outputFull = [System.IO.Path]::GetFullPath($OutputDirectory)
if ($Resume) {
    if (-not (Test-Path -LiteralPath $outputFull -PathType Container)) { throw "Resume output directory does not exist: $outputFull" }
} else {
    if (Test-Path -LiteralPath $outputFull) {
        if (@(Get-ChildItem -Force -LiteralPath $outputFull).Count -gt 0) { throw "Output directory must be new or empty: $outputFull" }
    } else { [void](New-Item -ItemType Directory -Path $outputFull) }
}
$logDirectory = Join-Path $outputFull 'logs'
[void](New-Item -ItemType Directory -Path $logDirectory -Force)
$checkpointPath = Join-Path $outputFull 'release-checkpoint.json'
$checkpointAuthenticationContext = $null
$externalAtomicArtifactInventory = $null
if ($RunMode -eq 'Release') {
    $checkpointAuthenticationContext = Get-Ov01CheckpointAuthenticationContext -CheckpointKeyPath $CheckpointKeyPath -RepoRoot $repoRoot -EvidenceDirectory $outputFull -Create:(-not $Resume)
    $externalAtomicArtifactInventory = Get-Ov01AuthenticatedCheckpointArtifactInventory -AuthenticationContext $checkpointAuthenticationContext
}

$currentSourceIdentity = Get-Ov01SourceIdentity -RepoRoot $repoRoot -BaselineHead $BaselineHead -TouchedPathsFile $TouchedPathsFile -RegistryPath $registryFull -ExcludedRootPaths @($outputFull)
$checkpoint = $null
if ($Resume) {
    $checkpointCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $outputFull -ExpectedSourceIdentity $currentSourceIdentity -ExpectedManualEvidence $manualEvidencePreflight -Registry $registry -CheckpointKeyPath $CheckpointKeyPath
    if (-not $checkpointCheck.valid) { throw "Release checkpoint validation failed:`n$($checkpointCheck.errors -join "`n")" }
    $checkpoint = $checkpointCheck.checkpoint
    $sourceIdentity = $checkpoint.preRunSourceIdentity
} else { $sourceIdentity = $currentSourceIdentity }
if ($RunMode -eq 'Release' -and -not [bool]$sourceIdentity.registryCanonical) { throw 'Release mode source identity is not bound to the canonical registry' }
$sourcePath = Join-Path $outputFull 'source-identity.json'
if ($Resume) {
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) { throw 'Resume source-identity.json is missing' }
    $retainedSourceIdentity = Get-Content -Encoding UTF8 -Raw -LiteralPath $sourcePath | ConvertFrom-Json
    $retainedSourceCheck = Test-Ov01SourceIdentityStable -Before $sourceIdentity -After $retainedSourceIdentity
    if (-not $retainedSourceCheck.valid) { throw "Resume source-identity.json mismatch:`n$($retainedSourceCheck.errors -join "`n")" }
} else {
    [System.IO.File]::WriteAllText($sourcePath, ($sourceIdentity | ConvertTo-Json -Depth 20) + "`n", (New-Object System.Text.UTF8Encoding($false)))
}

$runStarted = [DateTime]::UtcNow
$gateResults = New-Object System.Collections.Generic.List[object]
if ($Resume) { foreach ($completed in @($checkpoint.completedGateResults)) { $gateResults.Add($completed.result) } }
if ($RunMode -eq 'Release' -and -not $Resume) {
    $now = [DateTime]::UtcNow.ToString('o')
    $initialCheckpointArtifacts = Get-Ov01ArtifactRecords -Registry $registry -RepoRoot $repoRoot -SelectedGateIds @() -SourceIdentity $sourceIdentity
    if (@($initialCheckpointArtifacts.errors).Count -ne 0) { throw "Initial checkpoint artifact validation failed:`n$($initialCheckpointArtifacts.errors -join "`n")" }
    $checkpoint = [pscustomobject]@{
        schemaVersion = 1
        runnerId = 'OV01-AUTO-002'
        runMode = 'Release'
        outputDirectory = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $outputFull
        createdUtc = $now
        updatedUtc = $now
        preRunSourceIdentity = $sourceIdentity
        gateOrder = @(Get-Ov01CanonicalReleaseGateIds)
        manualEvidence = $manualEvidencePreflight.record
        completedGateResults = @()
        activeGate = $null
        artifactRecords = @($initialCheckpointArtifacts.records)
        evidenceFiles = @((Get-Ov01EvidenceFileRecords -EvidenceDirectory $outputFull -ExcludedRelativePaths @('release-checkpoint.json')))
    }
    Write-Ov01AuthenticatedCheckpoint -Path $checkpointPath -Value $checkpoint -AuthenticationContext $checkpointAuthenticationContext
}
$orderedGates = if ($RunMode -eq 'Release') {
    @(Get-Ov01CanonicalReleaseGateIds | ForEach-Object { $id = $_; @($registry.gates | Where-Object { [string]$_.id -eq $id })[0] })
} else { @($registry.gates | Where-Object { $selectedGateIds -contains [string]$_.id }) }
foreach ($gate in $orderedGates) {
    if (@($gateResults | Where-Object { [string]$_.id -eq [string]$gate.id }).Count -eq 1) { continue }
    $runtimeIsolationAttempt = 0
    if ($RunMode -eq 'Release') {
        $runtimeIsolationAttempt = if ($null -ne $checkpoint.activeGate -and [string]$checkpoint.activeGate.id -eq [string]$gate.id) { [int]$checkpoint.activeGate.attempt + 1 } else { 1 }
        $checkpoint.activeGate = New-Ov01ActiveGateState -Gate $gate -Registry $registry -Attempt $runtimeIsolationAttempt
        $checkpoint.updatedUtc = [DateTime]::UtcNow.ToString('o')
        $activeExcludedPrefixes = if ([string]::IsNullOrWhiteSpace([string]$checkpoint.activeGate.runtimeIsolationPrefix)) { @() } else { @([string]$checkpoint.activeGate.runtimeIsolationPrefix) }
        $checkpoint.evidenceFiles = @(Get-Ov01EvidenceFileRecords -EvidenceDirectory $outputFull -ExcludedRelativePaths @('release-checkpoint.json', [string]$checkpoint.activeGate.logPath) -ExcludedRelativePrefixes $activeExcludedPrefixes)
        Write-Ov01AuthenticatedCheckpoint -Path $checkpointPath -Value $checkpoint -AuthenticationContext $checkpointAuthenticationContext
    }
    $selectors = New-Object System.Collections.Generic.List[object]
    foreach ($scenario in $selectedScenarios) {
        foreach ($selector in @($scenario.executableSelectors | Where-Object { [string]$_.gateId -eq [string]$gate.id })) {
            $selectors.Add([pscustomobject]@{ scenarioId = [string]$scenario.id; selector = [string]$selector.selector })
        }
    }
    $gateResult = Invoke-Ov01Gate -Gate $gate -RepoRoot $repoRoot -LogDirectory $logDirectory -ExecutableSelectors $selectors.ToArray() -SourceIdentity $sourceIdentity -RuntimeIsolationAttempt $runtimeIsolationAttempt
    $gateResults.Add($gateResult)
    if ($RunMode -eq 'Release') {
        $completedWrappers = New-Object System.Collections.Generic.List[object]
        foreach ($result in $gateResults) { $completedWrappers.Add([pscustomobject]@{ id = [string]$result.id; resultSha256 = Get-Ov01ObjectSha256 -Value $result; result = $result }) }
        $checkpoint.completedGateResults = $completedWrappers.ToArray()
        $checkpoint.activeGate = $null
        $completedGateIdsForCheckpoint = @($gateResults | ForEach-Object { [string]$_.id })
        $checkpointArtifacts = Get-Ov01ArtifactRecords -Registry $registry -RepoRoot $repoRoot -SelectedGateIds $completedGateIdsForCheckpoint -SourceIdentity $sourceIdentity
        if (@($checkpointArtifacts.errors).Count -ne 0) { throw "Checkpoint artifact validation failed:`n$($checkpointArtifacts.errors -join "`n")" }
        $checkpoint.artifactRecords = @($checkpointArtifacts.records)
        $checkpoint.evidenceFiles = @(Get-Ov01EvidenceFileRecords -EvidenceDirectory $outputFull -ExcludedRelativePaths @('release-checkpoint.json'))
        $checkpoint.updatedUtc = [DateTime]::UtcNow.ToString('o')
        Write-Ov01AuthenticatedCheckpoint -Path $checkpointPath -Value $checkpoint -AuthenticationContext $checkpointAuthenticationContext
    }
}

$scenarioResults = New-Object System.Collections.Generic.List[object]
foreach ($scenario in $selectedScenarios) {
    $requiredGateIds = @($scenario.gateIds | ForEach-Object { [string]$_ })
    $results = @($gateResults | Where-Object { $requiredGateIds -contains [string]$_.id })
    $selectorEvidence = @($results | ForEach-Object { @($_.selectorResults) } | Where-Object { [string]$_.scenarioId -eq [string]$scenario.id })
    $requiredSelectors = @($scenario.executableSelectors)
    $selectorsPassed = $selectorEvidence.Count -eq $requiredSelectors.Count -and @($selectorEvidence | Where-Object { -not [bool]$_.matched }).Count -eq 0
    $passed = ($results.Count -eq $requiredGateIds.Count) -and (@($results | Where-Object status -ne 'PASS').Count -eq 0) -and $selectorsPassed
    $scenarioResults.Add([pscustomobject]@{
        id = [string]$scenario.id
        title = [string]$scenario.title
        priority = [string]$scenario.priority
        status = if ($passed) { 'PASS' } else { 'FAIL' }
        gateIds = $requiredGateIds
        executableSelectors = @($requiredSelectors)
        proofRefs = @($scenario.proofRefs)
    })
}

$artifactResult = Get-Ov01ArtifactRecords -Registry $registry -RepoRoot $repoRoot -SelectedGateIds $selectedGateIds -SourceIdentity $sourceIdentity
$postRunSourceIdentity = Get-Ov01SourceIdentity -RepoRoot $repoRoot -BaselineHead $BaselineHead -TouchedPathsFile $TouchedPathsFile -RegistryPath $registryFull -ExcludedRootPaths @($outputFull)
$sourceIdentityStability = Test-Ov01SourceIdentityStable -Before $sourceIdentity -After $postRunSourceIdentity
$manualEvidence = if ($RunMode -eq 'Release') {
    $apkRecords = @($artifactResult.records | Where-Object id -eq 'OV01-ART-MOBILE-APK')
    $expectedApkSha = if ($apkRecords.Count -eq 1) { [string]$apkRecords[0].sha256 } else { '' }
    Test-Ov01ManualEvidence -SummaryPath $ManualEvidenceSummaryPath -RepoRoot $repoRoot -ExpectedApkSha256 $expectedApkSha
} else {
    [pscustomobject]@{ valid = $true; errors = @(); record = $null }
}
$allSelectedPassed = @($gateResults | Where-Object status -ne 'PASS').Count -eq 0 -and @($scenarioResults | Where-Object status -ne 'PASS').Count -eq 0 -and @($artifactResult.errors).Count -eq 0 -and [bool]$manualEvidence.valid -and [bool]$sourceIdentityStability.valid
$report = [pscustomobject]@{
    schemaVersion = 1
    runnerId = 'OV01-AUTO-002'
    generatedUtc = [DateTime]::UtcNow.ToString('o')
    durationMs = [int64]([DateTime]::UtcNow - $runStarted).TotalMilliseconds
    runMode = $RunMode
    evidenceDirectory = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $outputFull
    releaseEligible = ($RunMode -eq 'Release' -and [bool]$sourceIdentity.registryCanonical -and [bool]$manualEvidence.valid -and [bool]$sourceIdentityStability.valid)
    runnerEnvironment = [pscustomobject]@{
        osVersion = [System.Environment]::OSVersion.VersionString
        architecture = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString()
        powerShellVersion = $PSVersionTable.PSVersion.ToString()
        checkpointAuthentication = if ($RunMode -eq 'Release') { $checkpointAuthenticationContext.attestation } else { $null }
        checkpointExternalAtomicState = if ($RunMode -eq 'Release') { $externalAtomicArtifactInventory } else { $null }
    }
    sourceIdentity = $sourceIdentity
    postRunSourceIdentity = $postRunSourceIdentity
    sourceIdentityDriftErrors = @($sourceIdentityStability.errors)
    selection = [pscustomobject]@{ scenarioIds = @($selectedScenarioIds); gateIds = @($selectedGateIds) }
    gateResults = $gateResults.ToArray()
    scenarioResults = $scenarioResults.ToArray()
    artifacts = @($artifactResult.records)
    artifactErrors = @($artifactResult.errors)
    manualEvidence = $manualEvidence.record
    manualEvidenceErrors = @($manualEvidence.errors)
    checkpointAuthentication = if ($RunMode -eq 'Release') { $checkpointAuthenticationContext.attestation } else { $null }
    overallStatus = if ($RunMode -eq 'Diagnostic') { 'NOT_APPLICABLE' } elseif ($allSelectedPassed) { 'PASS' } else { 'FAIL' }
    diagnosticStatus = if ($RunMode -eq 'Diagnostic') { if ($allSelectedPassed) { 'PASS' } else { 'FAIL' } } else { $null }
}

$reportValidation = Test-Ov01RunReport -Report $report -Registry $registry -EvidenceDirectory $outputFull -CheckpointKeyPath $CheckpointKeyPath
if (-not $reportValidation.valid) {
    if ($RunMode -eq 'Diagnostic') { $report.diagnosticStatus = 'FAIL' } else { $report.overallStatus = 'FAIL' }
    $report | Add-Member -NotePropertyName validationErrors -NotePropertyValue @($reportValidation.errors)
}
$reportPath = Join-Path $outputFull 'run-report.json'
[System.IO.File]::WriteAllText($reportPath, ($report | ConvertTo-Json -Depth 30) + "`n", (New-Object System.Text.UTF8Encoding($false)))

$leakFindings = New-Object System.Collections.Generic.List[string]
foreach ($file in @(Get-ChildItem -LiteralPath $outputFull -Recurse -File)) {
    $content = Get-Content -Raw -LiteralPath $file.FullName
    foreach ($finding in @(Test-Ov01Leak -Text $content)) { $leakFindings.Add("${finding}:$($file.Name)") }
}
$validation = [pscustomobject]@{
    registry = $registryValidation
    report = $reportValidation
    leakFindings = @($leakFindings | Sort-Object -Unique)
    valid = ($registryValidation.valid -and $reportValidation.valid -and $leakFindings.Count -eq 0)
}
$validationPath = Join-Path $outputFull 'validation.json'
[System.IO.File]::WriteAllText($validationPath, ($validation | ConvertTo-Json -Depth 20) + "`n", (New-Object System.Text.UTF8Encoding($false)))
[void](New-Ov01ClosedSetManifest -EvidenceDirectory $outputFull)
$sealValidation = Test-Ov01ClosedSetManifest -EvidenceDirectory $outputFull

Write-Output "OV01-AUTO-002 mode=$RunMode status=$($report.overallStatus) diagnostic=$($report.diagnosticStatus) scenarios=$($selectedScenarioIds.Count) gates=$($selectedGateIds.Count) source=$($sourceIdentity.compositeSha256) registry=$($sourceIdentity.registrySha256)"
$executionPassed = if ($RunMode -eq 'Diagnostic') { [string]$report.diagnosticStatus -eq 'PASS' } else { [string]$report.overallStatus -eq 'PASS' }
if (-not $executionPassed -or -not $validation.valid -or -not $sealValidation.valid) { exit 1 }
exit 0
