[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$toolRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $toolRoot '..\..\..'))
Import-Module (Join-Path $toolRoot 'Ov01Evidence.psm1') -Force

$passed = 0
$failed = 0
function Assert-True {
    param([bool]$Condition, [string]$Name)
    if ($Condition) {
        $script:passed++
        Write-Output "PASS $Name"
    } else {
        $script:failed++
        Write-Output "FAIL $Name"
    }
}

$tempRoot = Join-Path $repoRoot ('_bmad-output/test-artifacts/story-6-10/runner-selftest-' + [Guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $tempRoot)
$baselineHead = (& git -C $repoRoot rev-parse HEAD | Select-Object -First 1).Trim()
$touchedPath = Join-Path $tempRoot 'touched.txt'
[System.IO.File]::WriteAllText($touchedPath, "06_Testing/Automation/ov-01/ov01-scenario-registry.json`n", (New-Object System.Text.UTF8Encoding($false)))

$manualBundle = Join-Path $tempRoot 'manual-evidence'
[void](New-Item -ItemType Directory -Path $manualBundle)
$manualProofPath = Join-Path $manualBundle 'man001-002-shared-proof.txt'
[System.IO.File]::WriteAllText($manualProofPath, 'synthetic sanitized multi-case manual proof', (New-Object System.Text.UTF8Encoding($false)))
$manualSummaryPath = Join-Path $manualBundle 'manual-run-summary.json'
$manualSummaryRelative = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $manualSummaryPath
$manualProofRelative = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $manualProofPath
$manualManifestRelative = ConvertTo-Ov01RelativePath -Root $repoRoot -Path (Join-Path $manualBundle 'evidence-manifest.json')
$manualApkSha = 'a' * 64
$manualScenarioIds = @(1..34 | ForEach-Object { 'OV01-MAN-{0:d3}' -f $_ })
$manualDevice = [pscustomobject]@{ serial = 'synthetic-device'; androidVersion = '15'; apiLevel = '35'; buildFingerprint = 'synthetic/fingerprint' }
$manualCandidateBuiltUtc = [DateTimeOffset]::UtcNow.AddMinutes(-4).ToString('o')
$manualInstalledUtc = [DateTimeOffset]::UtcNow.AddMinutes(-3).ToString('o')
$manualExecutedUtc = [DateTimeOffset]::UtcNow.AddMinutes(-2).ToString('o')
$manualCompletedUtc = [DateTimeOffset]::UtcNow.AddMinutes(-1).ToString('o')
$manualScenarioResults = New-Object System.Collections.Generic.List[object]
foreach ($manualScenarioId in $manualScenarioIds) {
    $manualScenarioNumber = [int]$manualScenarioId.Substring($manualScenarioId.Length - 3)
    $manualAdbPath = Join-Path $manualBundle ('man{0:d3}-adb.txt' -f $manualScenarioNumber)
    [System.IO.File]::WriteAllText($manualAdbPath, "synthetic sanitized ADB transcript for $manualScenarioId", (New-Object System.Text.UTF8Encoding($false)))
    $manualAdbRelative = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $manualAdbPath
    if ($manualScenarioNumber -le 2) {
        $manualEvidencePaths = @($manualProofRelative, $manualAdbRelative)
    } else {
        $manualScenarioProofPath = Join-Path $manualBundle ('man{0:d3}-proof.txt' -f $manualScenarioNumber)
        [System.IO.File]::WriteAllText($manualScenarioProofPath, "synthetic sanitized proof for $manualScenarioId", (New-Object System.Text.UTF8Encoding($false)))
        $manualEvidencePaths = @((ConvertTo-Ov01RelativePath -Root $repoRoot -Path $manualScenarioProofPath), $manualAdbRelative)
    }
    $manualScenarioResults.Add([pscustomobject]@{
        id = $manualScenarioId
        status = 'PASS'
        actualResult = "Observed the expected result for $manualScenarioId."
        executedUtc = $manualExecutedUtc
        apkSha256 = $manualApkSha
        device = ($manualDevice | ConvertTo-Json -Compress | ConvertFrom-Json)
        oracle = [pscustomobject]@{
            type = 'state'
            expected = "Expected outcome for $manualScenarioId."
            observed = "Observed outcome for $manualScenarioId."
            verdict = 'PASS'
            requiresApiDb = $false
        }
        defectRefs = @()
        evidencePaths = $manualEvidencePaths
        adbTranscriptPaths = @($manualAdbRelative)
        apiDbEvidencePaths = @()
    })
}
$manualSummary = [pscustomobject]@{
    schemaVersion = 2
    status = 'PASS'
    candidateBuiltUtc = $manualCandidateBuiltUtc
    installedUtc = $manualInstalledUtc
    completedUtc = $manualCompletedUtc
    candidateApkSha256 = $manualApkSha
    installedApkSha256 = $manualApkSha
    evidenceManifestPath = $manualManifestRelative
    requiredScenarioIds = $manualScenarioIds
    scenarioResults = $manualScenarioResults.ToArray()
    multiCaseArtifacts = @([pscustomobject]@{ path = $manualProofRelative; scenarioIds = @('OV01-MAN-001', 'OV01-MAN-002') })
    device = $manualDevice
    leakScan = [pscustomobject]@{ status = 'PASS'; findingsCount = 0 }
}
[System.IO.File]::WriteAllText($manualSummaryPath, ($manualSummary | ConvertTo-Json -Depth 10) + "`n", (New-Object System.Text.UTF8Encoding($false)))
[void](New-Ov01ClosedSetManifest -EvidenceDirectory $manualBundle)
$manualCheck = Test-Ov01ManualEvidence -SummaryPath $manualSummaryRelative -RepoRoot $repoRoot -ExpectedApkSha256 $manualApkSha
Assert-True ($manualCheck.valid -and $manualCheck.record.scenarioCount -eq 34 -and $manualCheck.record.candidateApkSha256 -eq $manualApkSha) 'sealed exact-build MAN-001..034 schema with scenario-specific and declared multi-case evidence validates'
$wrongManualApkCheck = Test-Ov01ManualEvidence -SummaryPath $manualSummaryRelative -RepoRoot $repoRoot -ExpectedApkSha256 ('b' * 64)
Assert-True (-not $wrongManualApkCheck.valid -and @($wrongManualApkCheck.errors | Where-Object { $_ -eq 'manual APK SHA-256 does not match official built APK' }).Count -eq 1) 'manual APK that differs from official build is rejected'
$incompleteManualSummary = $manualSummary | ConvertTo-Json -Depth 10 | ConvertFrom-Json
$incompleteManualSummary.scenarioResults = @($incompleteManualSummary.scenarioResults | Select-Object -First 33)
$incompleteManualPath = Join-Path $tempRoot 'incomplete-manual.json'
[System.IO.File]::WriteAllText($incompleteManualPath, ($incompleteManualSummary | ConvertTo-Json -Depth 10) + "`n", (New-Object System.Text.UTF8Encoding($false)))
$incompleteManualCheck = Test-Ov01ManualEvidence -SummaryPath $incompleteManualPath -RepoRoot $repoRoot -ExpectedApkSha256 $manualApkSha
Assert-True (-not $incompleteManualCheck.valid -and @($incompleteManualCheck.errors | Where-Object { $_ -eq 'manual scenarioResults must contain each OV01-MAN-001..034 exactly once' }).Count -eq 1) 'manual summary missing one required row is rejected'

function Invoke-ManualEvidenceMutation {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Mutation
    )

    $mutationBundle = Join-Path $tempRoot $Name
    Copy-Item -LiteralPath $manualBundle -Destination $mutationBundle -Recurse
    $mutationSummaryPath = Join-Path $mutationBundle 'manual-run-summary.json'
    $sourceBundleRelative = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $manualBundle
    $mutationBundleRelative = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $mutationBundle
    $mutationSummaryText = (Get-Content -Encoding UTF8 -Raw -LiteralPath $mutationSummaryPath).Replace($sourceBundleRelative, $mutationBundleRelative)
    $mutationSummary = $mutationSummaryText | ConvertFrom-Json
    & $Mutation $mutationSummary $mutationBundle $mutationBundleRelative
    [System.IO.File]::WriteAllText($mutationSummaryPath, ($mutationSummary | ConvertTo-Json -Depth 20) + "`n", (New-Object System.Text.UTF8Encoding($false)))
    [void](New-Ov01ClosedSetManifest -EvidenceDirectory $mutationBundle)
    return Test-Ov01ManualEvidence -SummaryPath $mutationSummaryPath -RepoRoot $repoRoot -ExpectedApkSha256 $manualApkSha
}

$missingActualResultCheck = Invoke-ManualEvidenceMutation -Name 'manual-missing-actual-result' -Mutation {
    param($summary) $summary.scenarioResults[0].PSObject.Properties.Remove('actualResult')
}
Assert-True (-not $missingActualResultCheck.valid -and @($missingActualResultCheck.errors | Where-Object { $_ -eq 'manual scenario field is missing: OV01-MAN-001/actualResult' }).Count -eq 1) 'manual PASS row without actualResult is rejected'

$invalidExecutedUtcCheck = Invoke-ManualEvidenceMutation -Name 'manual-invalid-executed-utc' -Mutation {
    param($summary) $summary.scenarioResults[0].executedUtc = 'not-a-timestamp'
}
Assert-True (-not $invalidExecutedUtcCheck.valid -and @($invalidExecutedUtcCheck.errors | Where-Object { $_ -eq 'manual scenario executedUtc is invalid: OV01-MAN-001' }).Count -eq 1) 'manual PASS row without a valid execution timestamp is rejected'

$wrongScenarioApkCheck = Invoke-ManualEvidenceMutation -Name 'manual-wrong-scenario-apk' -Mutation {
    param($summary) $summary.scenarioResults[0].apkSha256 = 'b' * 64
}
Assert-True (-not $wrongScenarioApkCheck.valid -and @($wrongScenarioApkCheck.errors | Where-Object { $_ -eq 'manual scenario APK SHA-256 does not match candidate: OV01-MAN-001' }).Count -eq 1) 'manual PASS row bound to a different APK is rejected'

$wrongScenarioDeviceCheck = Invoke-ManualEvidenceMutation -Name 'manual-wrong-scenario-device' -Mutation {
    param($summary) $summary.scenarioResults[0].device.serial = 'other-device'
}
Assert-True (-not $wrongScenarioDeviceCheck.valid -and @($wrongScenarioDeviceCheck.errors | Where-Object { $_ -eq 'manual scenario device identity mismatch: OV01-MAN-001/serial' }).Count -eq 1) 'manual PASS row bound to a different device is rejected'

$missingOracleCheck = Invoke-ManualEvidenceMutation -Name 'manual-missing-oracle' -Mutation {
    param($summary) $summary.scenarioResults[0].PSObject.Properties.Remove('oracle')
}
Assert-True (-not $missingOracleCheck.valid -and @($missingOracleCheck.errors | Where-Object { $_ -eq 'manual scenario oracle is missing: OV01-MAN-001' }).Count -eq 1) 'manual PASS row without an oracle is rejected'

$missingDefectRefsCheck = Invoke-ManualEvidenceMutation -Name 'manual-missing-defect-refs' -Mutation {
    param($summary) $summary.scenarioResults[0].PSObject.Properties.Remove('defectRefs')
}
Assert-True (-not $missingDefectRefsCheck.valid -and @($missingDefectRefsCheck.errors | Where-Object { $_ -eq 'manual scenario defectRefs is missing: OV01-MAN-001' }).Count -eq 1) 'manual PASS row without an explicit defect reference set is rejected'

$metadataOnlyCheck = Invoke-ManualEvidenceMutation -Name 'manual-metadata-only' -Mutation {
    param($summary, $bundle, $bundleRelative)
    $indexPath = Join-Path $bundle 'scenario-evidence-index.md'
    [System.IO.File]::WriteAllText($indexPath, '# Synthetic index only', (New-Object System.Text.UTF8Encoding($false)))
    $summary.scenarioResults[2].evidencePaths = @("$bundleRelative/scenario-evidence-index.md")
}
Assert-True (-not $metadataOnlyCheck.valid -and @($metadataOnlyCheck.errors | Where-Object { $_ -eq 'manual scenario has no substantive evidence artifact: OV01-MAN-003' }).Count -eq 1) 'index/summary/identity/manifest artifacts cannot be the sole manual proof'

$undeclaredGenericReuseCheck = Invoke-ManualEvidenceMutation -Name 'manual-undeclared-generic-reuse' -Mutation {
    param($summary, $bundle, $bundleRelative)
    $genericPath = Join-Path $bundle 'generic-proof.txt'
    [System.IO.File]::WriteAllText($genericPath, 'generic proof reused without a declaration', (New-Object System.Text.UTF8Encoding($false)))
    $genericRelative = "$bundleRelative/generic-proof.txt"
    $summary.scenarioResults[2].evidencePaths = @($genericRelative)
    $summary.scenarioResults[3].evidencePaths = @($genericRelative)
}
Assert-True (-not $undeclaredGenericReuseCheck.valid -and @($undeclaredGenericReuseCheck.errors | Where-Object { $_ -eq 'manual evidence artifact is neither scenario-specific nor declared multi-case: OV01-MAN-003/generic-proof.txt' }).Count -eq 1 -and @($undeclaredGenericReuseCheck.errors | Where-Object { $_ -eq 'manual evidence artifact is reused without an exact multi-case declaration: generic-proof.txt' }).Count -eq 1) 'reused generic artifact without an exact multi-case declaration is rejected'

$incompleteMultiCaseCheck = Invoke-ManualEvidenceMutation -Name 'manual-incomplete-multi-case' -Mutation {
    param($summary) $summary.multiCaseArtifacts[0].scenarioIds = @('OV01-MAN-001')
}
Assert-True (-not $incompleteMultiCaseCheck.valid -and @($incompleteMultiCaseCheck.errors | Where-Object { $_ -eq 'manual multi-case artifact must declare at least two scenarios: man001-002-shared-proof.txt' }).Count -eq 1) 'multi-case evidence declaration with fewer than two scenarios is rejected'

$missingAdbTranscriptCheck = Invoke-ManualEvidenceMutation -Name 'manual-missing-adb-transcript' -Mutation {
    param($summary) $summary.scenarioResults[0].PSObject.Properties.Remove('adbTranscriptPaths')
}
Assert-True (-not $missingAdbTranscriptCheck.valid -and @($missingAdbTranscriptCheck.errors | Where-Object { $_ -eq 'manual scenario ADB transcript is missing: OV01-MAN-001' }).Count -eq 1) 'manual scenario without an ADB transcript declaration is rejected'

$emptyAdbTranscriptCheck = Invoke-ManualEvidenceMutation -Name 'manual-empty-adb-transcript' -Mutation {
    param($summary, $bundle)
    [System.IO.File]::WriteAllText((Join-Path $bundle 'man001-adb.txt'), '', (New-Object System.Text.UTF8Encoding($false)))
}
Assert-True (-not $emptyAdbTranscriptCheck.valid -and @($emptyAdbTranscriptCheck.errors | Where-Object { $_ -like 'manual scenario ADB transcript is empty: OV01-MAN-001/*man001-adb.txt' }).Count -eq 1) 'empty ADB transcript is rejected'

$metadataAdbTranscriptCheck = Invoke-ManualEvidenceMutation -Name 'manual-metadata-adb-transcript' -Mutation {
    param($summary, $bundle, $bundleRelative)
    $metadataPath = Join-Path $bundle 'man001-summary.txt'
    [System.IO.File]::WriteAllText($metadataPath, 'metadata-only ADB index', (New-Object System.Text.UTF8Encoding($false)))
    $metadataRelative = "$bundleRelative/man001-summary.txt"
    $summary.scenarioResults[0].evidencePaths += $metadataRelative
    $summary.scenarioResults[0].adbTranscriptPaths = @($metadataRelative)
}
Assert-True (-not $metadataAdbTranscriptCheck.valid -and @($metadataAdbTranscriptCheck.errors | Where-Object { $_ -like 'manual scenario ADB transcript is metadata-only: OV01-MAN-001/*man001-summary.txt' }).Count -eq 1) 'metadata-only ADB transcript is rejected'

$wrongAdbExtensionCheck = Invoke-ManualEvidenceMutation -Name 'manual-wrong-adb-extension' -Mutation {
    param($summary, $bundle, $bundleRelative)
    $wrongPath = Join-Path $bundle 'man001-adb.json'
    [System.IO.File]::WriteAllText($wrongPath, '{"synthetic":"adb"}', (New-Object System.Text.UTF8Encoding($false)))
    $wrongRelative = "$bundleRelative/man001-adb.json"
    $summary.scenarioResults[0].evidencePaths += $wrongRelative
    $summary.scenarioResults[0].adbTranscriptPaths = @($wrongRelative)
}
Assert-True (-not $wrongAdbExtensionCheck.valid -and @($wrongAdbExtensionCheck.errors | Where-Object { $_ -like 'manual scenario ADB transcript extension is invalid: OV01-MAN-001/*man001-adb.json' }).Count -eq 1) 'ADB transcript with a non-text/log extension is rejected'

$missingApiDbEvidenceCheck = Invoke-ManualEvidenceMutation -Name 'manual-missing-api-db-evidence' -Mutation {
    param($summary) $summary.scenarioResults[0].oracle.requiresApiDb = $true
}
Assert-True (-not $missingApiDbEvidenceCheck.valid -and @($missingApiDbEvidenceCheck.errors | Where-Object { $_ -eq 'manual scenario requires API/DB evidence but has none: OV01-MAN-001' }).Count -eq 1) 'oracle requiring API/DB proof cannot pass with an empty path set'

$predatingScenarioCheck = Invoke-ManualEvidenceMutation -Name 'manual-predating-scenario' -Mutation {
    param($summary) $summary.scenarioResults[0].executedUtc = ([DateTimeOffset]::Parse([string]$summary.candidateBuiltUtc).AddSeconds(-1)).ToString('o')
}
Assert-True (-not $predatingScenarioCheck.valid -and @($predatingScenarioCheck.errors | Where-Object { $_ -eq 'manual scenario executedUtc predates installed candidate: OV01-MAN-001' }).Count -eq 1) 'scenario timestamp predating the built and installed candidate is rejected'

$duplicateEvidencePathCheck = Invoke-ManualEvidenceMutation -Name 'manual-duplicate-evidence-path' -Mutation {
    param($summary) $summary.scenarioResults[0].evidencePaths += [string]$summary.scenarioResults[0].evidencePaths[0]
}
Assert-True (-not $duplicateEvidencePathCheck.valid -and @($duplicateEvidencePathCheck.errors | Where-Object { $_ -eq 'manual scenario has duplicate evidence paths: OV01-MAN-001' }).Count -eq 1) 'duplicate per-scenario evidence path is rejected'

[System.IO.File]::WriteAllText($manualProofPath, 'access_token=unredacted-test-secret', (New-Object System.Text.UTF8Encoding($false)))
[void](New-Ov01ClosedSetManifest -EvidenceDirectory $manualBundle)
$leakyManualCheck = Test-Ov01ManualEvidence -SummaryPath $manualSummaryRelative -RepoRoot $repoRoot -ExpectedApkSha256 $manualApkSha
Assert-True (-not $leakyManualCheck.valid -and @($leakyManualCheck.errors | Where-Object { $_ -eq 'manual evidence leak finding: SECRET_ASSIGNMENT/man001-002-shared-proof.txt' }).Count -eq 1) 'sealed manual bundle with an unredacted secret is rejected'
[System.IO.File]::WriteAllText($manualProofPath, 'synthetic sanitized multi-case manual proof', (New-Object System.Text.UTF8Encoding($false)))
[void](New-Ov01ClosedSetManifest -EvidenceDirectory $manualBundle)
$logoutIntegrationPath = Join-Path $repoRoot '05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/LogoutIntegrationTest.java'
$sessionServiceTestPath = Join-Path $repoRoot '05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/service/impl/SessionServiceImplTest.java'
$logoutIntegrationText = Get-Content -Raw $logoutIntegrationPath
$sessionServiceTestText = Get-Content -Raw $sessionServiceTestPath
Assert-True ($logoutIntegrationText -match 'void logout_noAccessToken_returns401\(\)' -and $logoutIntegrationText -match 'post\("/api/v1/auth/logout"' -and $sessionServiceTestText -notmatch 'logout_noAccessToken_returns401') 'controller-level logout no-access-token contract executes in the canonical integration test'

$registryPath = Join-Path $toolRoot 'ov01-scenario-registry.json'
$registry = Read-Ov01Registry -Path $registryPath
$registryCheck = Test-Ov01Registry -Registry $registry -RepoRoot $repoRoot
Assert-True $registryCheck.valid 'canonical registry passes path/status/orphan/duplicate validation'
Assert-True ($registryCheck.scenarioCount -eq 15) 'registry contains exactly 15 stable scenarios'
$expectedScenarioIds = @(1..15 | ForEach-Object { 'OV01-E2E-{0:d3}' -f $_ })
$expectedRequiredGateIds = @(
    'OV01-GATE-BE-FULL', 'OV01-GATE-BE-PACKAGE',
    'OV01-GATE-MOBILE-FULL', 'OV01-GATE-MOBILE-ANALYZE', 'OV01-GATE-MOBILE-FORMAT', 'OV01-GATE-MOBILE-APK',
    'OV01-GATE-WEB-FULL', 'OV01-GATE-WEB-LINT', 'OV01-GATE-WEB-BUILD',
    'OV01-GATE-AI-FULL', 'OV01-GATE-AI-COMPILE', 'OV01-GATE-EVALUATOR-FULL'
)
$expectedReleaseGateIds = @(Get-Ov01CanonicalReleaseGateIds)
Assert-True (@($registry.requiredScenarioIds).Count -eq 15 -and @(Compare-Object $expectedScenarioIds @($registry.requiredScenarioIds)).Count -eq 0) 'required scenario IDs are the exact canonical closed set'
Assert-True (@($registry.requiredPlatformGateIds).Count -eq 12 -and @(Compare-Object $expectedRequiredGateIds @($registry.requiredPlatformGateIds)).Count -eq 0) 'required platform gates are the exact canonical closed set including touched-Dart format'
Assert-True (@($registry.gates).Count -eq 17 -and @(Compare-Object $expectedReleaseGateIds @($registry.gates.id)).Count -eq 0) 'registry gate catalog is the immutable exact 17-gate release set'
Assert-True (@($registry.artifacts).Count -eq 9) 'artifact registry is the exact nine-identity closed set'
$selectorlessScenarios = @($registry.scenarios | Where-Object { $null -eq $_.PSObject.Properties['executableSelectors'] -or @($_.executableSelectors).Count -eq 0 })
Assert-True ($selectorlessScenarios.Count -eq 0) 'all 15 scenarios have executable selector contracts'
$mobileFormatGate = @($registry.gates | Where-Object id -eq 'OV01-GATE-MOBILE-FORMAT')
Assert-True ($mobileFormatGate.Count -eq 1) 'touched-Dart format gate is registered exactly once'
$expectedFlutterDartGateIds = @('OV01-GATE-MOBILE-OV01', 'OV01-GATE-MOBILE-FULL', 'OV01-GATE-MOBILE-ANALYZE', 'OV01-GATE-MOBILE-APK')
$actualFlutterDartGateIds = @($registry.gates | Where-Object { $null -ne $_.PSObject.Properties['runnerTool'] -and [string]$_.runnerTool -eq 'flutter-dart-snapshot' } | ForEach-Object { [string]$_.id })
Assert-True ($actualFlutterDartGateIds.Count -eq 4 -and @(Compare-Object $expectedFlutterDartGateIds $actualFlutterDartGateIds).Count -eq 0) 'all and only Flutter CLI gates use the direct Dart snapshot runner'
$configuredFlutterLauncher = [string](@($registry.gates | Where-Object id -eq 'OV01-GATE-MOBILE-OV01')[0].executable)
$resolvedFlutterDartTool = Resolve-Ov01FlutterDartTool -ConfiguredFlutterExecutable $configuredFlutterLauncher
Assert-True (
    [System.IO.Path]::IsPathRooted([string]$resolvedFlutterDartTool.dartExecutable) -and
    (Test-Path -LiteralPath $resolvedFlutterDartTool.dartExecutable -PathType Leaf) -and
    (Test-Path -LiteralPath $resolvedFlutterDartTool.flutterSnapshot -PathType Leaf) -and
    [string]$resolvedFlutterDartTool.dartExecutableSha256 -match '^[0-9a-f]{64}$' -and
    [string]$resolvedFlutterDartTool.flutterSnapshotSha256 -match '^[0-9a-f]{64}$'
) 'Flutter resolver derives existing hashed Dart executable and tool snapshot from configured flutter.bat root'
$e2e014 = @($registry.scenarios | Where-Object id -eq 'OV01-E2E-014')[0]
$e2e014Selectors = @($e2e014.executableSelectors)
Assert-True (
    $e2e014Selectors.Count -eq 2 -and
    @($e2e014Selectors | Where-Object { [string]$_.gateId -eq 'OV01-GATE-BE-SAFETY' -and [string]$_.selector -eq 'EmergencyTriageLinkPostgresIntegrationTest::ov01E2e014RestartReclaimsExpiredAttemptWithoutResendingSuccessfulDevice' }).Count -eq 1 -and
    @($e2e014Selectors | Where-Object { [string]$_.gateId -eq 'OV01-GATE-MOBILE-OV01' -and [string]$_.selector -eq 'auth landing keeps unresolved continuation and offers retry in place' }).Count -eq 1
) 'OV01-E2E-014 requires exact PostgreSQL restart/reclaim and Mobile continuation/retry selectors'
$surefireFixturePath = Join-Path $tempRoot 'TEST-SurefireMixedFixture.xml'
[System.IO.File]::WriteAllText($surefireFixturePath, @'
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.carebridge.backend.ExampleTest" tests="2" failures="0" errors="0" skipped="1">
  <testcase name="passingMethod" classname="com.carebridge.backend.ExampleTest" time="0.1" />
  <testcase name="skippedMethod" classname="com.carebridge.backend.ExampleTest" time="0.0">
    <skipped message="Requires real CLOUDINARY_* credentials" />
  </testcase>
</testsuite>
'@, (New-Object System.Text.UTF8Encoding($false)))
$surefireFixture = Get-Ov01SurefireXmlEvidence -Path $surefireFixturePath
Assert-True (
    [bool]$surefireFixture.parsed -and
    @($surefireFixture.selectorIdentities) -contains 'ExampleTest::passingMethod' -and
    @($surefireFixture.selectorIdentities) -contains 'ExampleTest::skippedMethod' -and
    @($surefireFixture.skippedTests).Count -eq 1 -and
    [string]$surefireFixture.skippedTests[0].selector -eq 'com.carebridge.backend.ExampleTest#skippedMethod' -and
    [string]$surefireFixture.skippedTests[0].reason -eq 'Requires real CLOUDINARY_* credentials'
) 'mixed passing/skipped Surefire XML yields method selectors and exact skip identity under strict mode'
$backendJarArtifact = @($registry.artifacts | Where-Object id -eq 'OV01-ART-BACKEND-JAR')[0]
Assert-True ([string]$backendJarArtifact.path -eq '05_Development/CareBridgeAPI/target/backend-0.0.1-SNAPSHOT.jar' -and -not [bool]$backendJarArtifact.allowMany) 'backend JAR artifact is one exact canonical build output'
Assert-True (
    @($mobileFormatGate[0].arguments)[0] -eq '--suppress-analytics' -and
    [string]$mobileFormatGate[0].environment.DART_SUPPRESS_ANALYTICS -eq 'true' -and
    @($mobileFormatGate[0].evidenceEnvironmentKeys) -contains 'DART_SUPPRESS_ANALYTICS'
) 'touched-Dart format gate suppresses analytics/config writes with retained safe environment evidence'
$backendGates = @($registry.gates | Where-Object { [string]$_.platform -eq 'Backend/PostgreSQL' })
Assert-True ($backendGates.Count -eq 4 -and @($backendGates | Where-Object { [string]$_.workingDirectory -ne '05_Development/CareBridgeAPI' }).Count -eq 0) 'all backend gates execute from the API working directory'
Assert-True (@($backendGates | Where-Object { $null -eq $_.PSObject.Properties['fallback'] -or [string]$_.fallback.executable -ne '${MAVEN_PINNED_3_9_16}' }).Count -eq 0) 'all backend gates retain pinned Maven 3.9.16 fallback after wrapper pre-execution failure'
Assert-True (@($backendGates | Where-Object { $null -ne $_.fallback.PSObject.Properties['arguments'] }).Count -eq 0) 'backend fallback reuses primary arguments without double-add'
$backendFullGate = @($registry.gates | Where-Object id -eq 'OV01-GATE-BE-FULL')[0]
$backendPackageGate = @($registry.gates | Where-Object id -eq 'OV01-GATE-BE-PACKAGE')[0]
Assert-True (@($backendFullGate.arguments)[0] -eq '-Dgate0.enabled=true' -and @($backendPackageGate.arguments)[0] -eq '-Dgate0.enabled=true') 'backend full and package gates execute the opt-in Gate0 DATA/SEC/OPS suite'
$approvedSkipGateIds = @($registry.gates | Where-Object { $null -ne $_.PSObject.Properties['approvedSkippedTests'] -and @($_.approvedSkippedTests).Count -gt 0 } | ForEach-Object { [string]$_.id })
Assert-True ($approvedSkipGateIds.Count -eq 2 -and @(Compare-Object @('OV01-GATE-BE-FULL', 'OV01-GATE-BE-PACKAGE') $approvedSkipGateIds -SyncWindow 0).Count -eq 0 -and @($backendFullGate.approvedSkippedTests).Count -eq 1) 'only backend full and package carry the exact single Cloudinary external waiver'
$cloudinarySkip = [pscustomobject]@{
    selector = 'com.carebridge.backend.content.integration.ContentBodySanitizeIntegrationTest#uploadPublicContentImage_endToEnd_persistsPublicAccessModeAndPermanentUrl'
    reason = 'Requires real CLOUDINARY_* credentials wired into the Testcontainers test context'
}
$approvedSkipDisposition = Get-Ov01SkipDisposition -Gate $backendFullGate -ActualSkippedTests @($cloudinarySkip) -ReportedSkipped 1 -RepoRoot $repoRoot
Assert-True ($approvedSkipDisposition.valid -and @($approvedSkipDisposition.approved).Count -eq 1 -and @($approvedSkipDisposition.unapproved).Count -eq 0) 'exact Cloudinary external skip is qualified with hashed compensating evidence'
$unknownSkipDisposition = Get-Ov01SkipDisposition -Gate $backendFullGate -ActualSkippedTests @([pscustomobject]@{ selector = 'example.UnknownTest#skipped'; reason = 'unknown' }) -ReportedSkipped 1 -RepoRoot $repoRoot
Assert-True (-not $unknownSkipDisposition.valid -and @($unknownSkipDisposition.unapproved).Count -eq 1) 'unknown skipped test fails closed'
$countMismatchSkipDisposition = Get-Ov01SkipDisposition -Gate $backendFullGate -ActualSkippedTests @($cloudinarySkip) -ReportedSkipped 2 -RepoRoot $repoRoot
Assert-True (-not $countMismatchSkipDisposition.valid -and -not $countMismatchSkipDisposition.countMatches) 'Surefire skipped count without matching XML identity fails closed'
$staleSkipDisposition = Get-Ov01SkipDisposition -Gate $backendFullGate -ActualSkippedTests @() -ReportedSkipped 0 -RepoRoot $repoRoot
Assert-True (-not $staleSkipDisposition.valid -and @($staleSkipDisposition.missingExpected).Count -eq 1) 'stale approved-skip contract fails after the expected skip disappears'
Assert-True (@($registry.gates | Where-Object { $null -eq $_.PSObject.Properties['timeoutSeconds'] -or [int]$_.timeoutSeconds -lt 1 }).Count -eq 0) 'every canonical gate declares a positive bounded timeout'
Assert-True ([int](@($registry.gates | Where-Object id -eq 'OV01-GATE-BE-FULL')[0].timeoutSeconds) -eq 1800 -and [int](@($registry.gates | Where-Object id -eq 'OV01-GATE-MOBILE-FULL')[0].timeoutSeconds) -eq 1800) 'backend and Flutter full gates retain realistic long-running timeout budgets'
Assert-True ([int](@($registry.gates | Where-Object id -eq 'OV01-GATE-BE-PACKAGE')[0].timeoutSeconds) -eq 2400) 'backend clean-package gate retains the measured release-host budget without exceeding the 3600-second runner cap'
$evaluatorGate = @($registry.gates | Where-Object id -eq 'OV01-GATE-EVALUATOR-FULL')[0]
Assert-True ([string]$evaluatorGate.workingDirectory -eq '06_Testing/CareBridgeAIEvaluation' -and [string]$evaluatorGate.environment.PYTHONPATH -eq 'src') 'evaluator gate applies package-correct PYTHONPATH from its module working directory'
$expectedEvaluatorArguments = @('-m', 'pytest')
$actualEvaluatorArguments = @($evaluatorGate.arguments | ForEach-Object { [string]$_ })
Assert-True ($actualEvaluatorArguments.Count -eq $expectedEvaluatorArguments.Count -and @(Compare-Object $expectedEvaluatorArguments $actualEvaluatorArguments -SyncWindow 0).Count -eq 0 -and [int]$evaluatorGate.expectedMinTests -eq 42) 'evaluator gate emits deterministic totals and enforces the approved 42-test minimum'
$pytestIsolationGateIds = @($registry.gates | Where-Object { $null -ne $_.PSObject.Properties['pytestIsolation'] -and [bool]$_.pytestIsolation } | ForEach-Object { [string]$_.id })
Assert-True ($pytestIsolationGateIds.Count -eq 3 -and @(Compare-Object @('OV01-GATE-AI-OV01', 'OV01-GATE-AI-FULL', 'OV01-GATE-EVALUATOR-FULL') $pytestIsolationGateIds -SyncWindow 0).Count -eq 0) 'all three pytest gates use the exact pytest isolation contract'
$evaluatorLogDirectory = Join-Path $tempRoot 'logs'
[void](New-Item -ItemType Directory -Path $evaluatorLogDirectory)
$evaluatorRun = Invoke-Ov01Gate -Gate $evaluatorGate -RepoRoot $repoRoot -LogDirectory $evaluatorLogDirectory
$evaluatorLog = Get-Content -Raw (Join-Path $evaluatorLogDirectory 'OV01-GATE-EVALUATOR-FULL.log')
Assert-True ($evaluatorRun.status -eq 'PASS' -and $evaluatorRun.exitCode -eq 0 -and $evaluatorRun.totals.parsed -and [int]$evaluatorRun.totals.tests -ge 42 -and [int]$evaluatorRun.totals.nonPass -eq 0 -and $evaluatorLog -match '(?im)\b42 passed\b' -and $null -ne $evaluatorRun.runtimeIsolation -and [string]$evaluatorRun.runtimeIsolation.kind -eq 'pytest' -and (Test-Path -LiteralPath ([string]$evaluatorRun.runtimeIsolation.root) -PathType Container)) 'evaluator gate runtime output and isolated temp/cache evidence are green'
$evaluatorIsolationCheck = Test-Ov01PytestRuntimeIsolation -Result $evaluatorRun -GateDefinition $evaluatorGate -EvidenceDirectory $tempRoot
Assert-True $evaluatorIsolationCheck.valid 'pytest isolation evidence is bound to its exact evidence directory and canonical child paths'

$reboundIsolationRun = ($evaluatorRun | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$alternateIsolationRoot = Join-Path $tempRoot 'alternate-isolation-root'
$alternateOsTemp = Join-Path $alternateIsolationRoot 'os-temp'
$alternateBaseTemp = Join-Path $alternateIsolationRoot 'basetemp'
$alternateCache = Join-Path $alternateIsolationRoot 'cache'
foreach ($path in @($alternateOsTemp, $alternateBaseTemp, $alternateCache)) { [void](New-Item -ItemType Directory -Path $path -Force) }
$reboundIsolationRun.exactCommand = ([string]$reboundIsolationRun.exactCommand).Replace([string]$reboundIsolationRun.runtimeIsolation.baseTemp, $alternateBaseTemp).Replace([string]$reboundIsolationRun.runtimeIsolation.cacheDir, $alternateCache)
$reboundIsolationRun.runtimeIsolation.root = $alternateIsolationRoot
$reboundIsolationRun.runtimeIsolation.osTemp = $alternateOsTemp
$reboundIsolationRun.runtimeIsolation.baseTemp = $alternateBaseTemp
$reboundIsolationRun.runtimeIsolation.cacheDir = $alternateCache
$reboundIsolationCheck = Test-Ov01PytestRuntimeIsolation -Result $reboundIsolationRun -GateDefinition $evaluatorGate -EvidenceDirectory $tempRoot
Assert-True (-not $reboundIsolationCheck.valid -and @($reboundIsolationCheck.errors | Where-Object { $_ -like 'pytest runtime isolation root is not bound*' }).Count -eq 1) 'pytest isolation evidence cannot be rebound to another existing directory tree'

$childSubstitutionRun = ($evaluatorRun | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$substituteBaseTemp = Join-Path ([string]$childSubstitutionRun.runtimeIsolation.root) 'substitute-basetemp'
[void](New-Item -ItemType Directory -Path $substituteBaseTemp)
$childSubstitutionRun.exactCommand = ([string]$childSubstitutionRun.exactCommand).Replace([string]$childSubstitutionRun.runtimeIsolation.baseTemp, $substituteBaseTemp)
$childSubstitutionRun.runtimeIsolation.baseTemp = $substituteBaseTemp
$childSubstitutionCheck = Test-Ov01PytestRuntimeIsolation -Result $childSubstitutionRun -GateDefinition $evaluatorGate -EvidenceDirectory $tempRoot
Assert-True (-not $childSubstitutionCheck.valid -and @($childSubstitutionCheck.errors | Where-Object { $_ -like 'pytest runtime isolation child path mismatch:*baseTemp' }).Count -eq 1) 'pytest isolation evidence rejects canonical child-path substitution'

$commandPathSubstitutionRun = ($evaluatorRun | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$argumentCount = @($commandPathSubstitutionRun.effectiveArguments).Count
$originalBaseTempArgument = [string]$commandPathSubstitutionRun.effectiveArguments[$argumentCount - 3]
$originalCacheArgument = [string]$commandPathSubstitutionRun.effectiveArguments[$argumentCount - 1]
$substitutedBaseTempArgument = $originalBaseTempArgument + '-substituted'
$substitutedCacheArgument = $originalCacheArgument + '-substituted'
$commandPathSubstitutionRun.effectiveArguments[$argumentCount - 3] = $substitutedBaseTempArgument
$commandPathSubstitutionRun.effectiveArguments[$argumentCount - 1] = $substitutedCacheArgument
$commandPathSubstitutionRun.exactCommand = ([string]$commandPathSubstitutionRun.exactCommand).Replace($originalBaseTempArgument, $substitutedBaseTempArgument).Replace($originalCacheArgument, $substitutedCacheArgument)
$commandPathSubstitutionCheck = Test-Ov01PytestRuntimeIsolation -Result $commandPathSubstitutionRun -GateDefinition $evaluatorGate -EvidenceDirectory $tempRoot
Assert-True (-not $commandPathSubstitutionCheck.valid -and @($commandPathSubstitutionCheck.errors | Where-Object { $_ -like 'pytest runtime isolation effective arguments mismatch:*' }).Count -eq 1) 'pytest isolation rejects prefix-substituted command arguments even when the display command is changed consistently'

$replacedRequiredScenarioRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$replacedRequiredScenarioRegistry.requiredScenarioIds[0] = 'OV01-E2E-999'
$replacedRequiredScenarioCheck = Test-Ov01Registry -Registry $replacedRequiredScenarioRegistry -RepoRoot $repoRoot
Assert-True (-not $replacedRequiredScenarioCheck.valid) 'replaced required scenario identity is rejected even when scenarios remain canonical'

$extraRequiredGateRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$extraRequiredGateRegistry.requiredPlatformGateIds += 'OV01-GATE-BE-LIFECYCLE'
$extraRequiredGateCheck = Test-Ov01Registry -Registry $extraRequiredGateRegistry -RepoRoot $repoRoot
Assert-True (-not $extraRequiredGateCheck.valid) 'unknown extra required platform gate identity is rejected'

$extraReferencedGateRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$extraReferencedGate = ($extraReferencedGateRegistry.gates[0] | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$extraReferencedGate.id = 'OV01-GATE-EXTRA'
$extraReferencedGateRegistry.gates += $extraReferencedGate
$extraReferencedGateRegistry.scenarios[0].gateIds += 'OV01-GATE-EXTRA'
$extraReferencedGateCheck = Test-Ov01Registry -Registry $extraReferencedGateRegistry -RepoRoot $repoRoot
Assert-True (-not $extraReferencedGateCheck.valid -and @($extraReferencedGateCheck.errors | Where-Object { $_ -eq 'unknown gate identity: OV01-GATE-EXTRA' }).Count -eq 1) 'extra gate is rejected even when a scenario references it'

$missingArtifactRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$missingArtifactRegistry.artifacts = @($missingArtifactRegistry.artifacts | Select-Object -First 8)
$missingArtifactCheck = Test-Ov01Registry -Registry $missingArtifactRegistry -RepoRoot $repoRoot
Assert-True (-not $missingArtifactCheck.valid) 'missing canonical artifact identity is rejected'

$duplicateRequiredScenarioRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$duplicateRequiredScenarioRegistry.requiredScenarioIds[1] = $duplicateRequiredScenarioRegistry.requiredScenarioIds[0]
$duplicateRequiredScenarioCheck = Test-Ov01Registry -Registry $duplicateRequiredScenarioRegistry -RepoRoot $repoRoot
Assert-True (-not $duplicateRequiredScenarioCheck.valid) 'duplicate required scenario identity is rejected'

$emptyRequiredGateRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$emptyRequiredGateRegistry.requiredPlatformGateIds[0] = ''
$emptyRequiredGateCheck = Test-Ov01Registry -Registry $emptyRequiredGateRegistry -RepoRoot $repoRoot
Assert-True (-not $emptyRequiredGateCheck.valid) 'empty required platform gate identity is rejected'

$unknownArtifactRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$unknownArtifactRegistry.artifacts[8].id = 'OV01-ART-UNKNOWN'
$unknownArtifactRegistryCheck = Test-Ov01Registry -Registry $unknownArtifactRegistry -RepoRoot $repoRoot
Assert-True (-not $unknownArtifactRegistryCheck.valid) 'replaced and unknown artifact registry identity is rejected'

$wrongSelectorRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$wrongSelectorRegistry.scenarios[0] | Add-Member -Force -NotePropertyName executableSelectors -NotePropertyValue @([pscustomobject]@{ gateId = 'OV01-GATE-WEB-OV01'; selector = 'JourneyOnboardingIntegrationTest' })
$wrongSelectorCheck = Test-Ov01Registry -Registry $wrongSelectorRegistry -RepoRoot $repoRoot
Assert-True (-not $wrongSelectorCheck.valid) 'scenario selector mapped to a gate outside its scenario is rejected'

$emptyVersionRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$emptyVersionRegistry.gates[0].versionArguments = @()
$emptyVersionCheck = Test-Ov01Registry -Registry $emptyVersionRegistry -RepoRoot $repoRoot
Assert-True (-not $emptyVersionCheck.valid -and @($emptyVersionCheck.errors | Where-Object { $_ -like 'gate version arguments are missing:*' }).Count -eq 1) 'gate without an executable version probe is rejected'

$missingTimeoutRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$missingTimeoutRegistry.gates[0].PSObject.Properties.Remove('timeoutSeconds')
$missingTimeoutCheck = Test-Ov01Registry -Registry $missingTimeoutRegistry -RepoRoot $repoRoot
Assert-True (-not $missingTimeoutCheck.valid -and @($missingTimeoutCheck.errors | Where-Object { $_ -like 'gate timeout must be an integer*' }).Count -eq 1) 'gate without an explicit bounded timeout is rejected'

$nonPytestIsolationRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
@($nonPytestIsolationRegistry.gates | Where-Object id -eq 'OV01-GATE-MOBILE-FORMAT')[0] | Add-Member -NotePropertyName pytestIsolation -NotePropertyValue $true
$nonPytestIsolationCheck = Test-Ov01Registry -Registry $nonPytestIsolationRegistry -RepoRoot $repoRoot
Assert-True (-not $nonPytestIsolationCheck.valid) 'pytest isolation on a non-pytest gate is rejected'

$nonBooleanIsolationRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
@($nonBooleanIsolationRegistry.gates | Where-Object id -eq 'OV01-GATE-EVALUATOR-FULL')[0].pytestIsolation = 'true'
$nonBooleanIsolationCheck = Test-Ov01Registry -Registry $nonBooleanIsolationRegistry -RepoRoot $repoRoot
Assert-True (-not $nonBooleanIsolationCheck.valid) 'non-boolean pytest isolation configuration is rejected'

$expiredSkipRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
@($expiredSkipRegistry.gates | Where-Object id -eq 'OV01-GATE-BE-FULL')[0].approvedSkippedTests[0].expiresOn = '2020-01-01'
$expiredSkipCheck = Test-Ov01Registry -Registry $expiredSkipRegistry -RepoRoot $repoRoot
Assert-True (-not $expiredSkipCheck.valid -and @($expiredSkipCheck.errors | Where-Object { $_ -like 'approved skipped test waiver is expired:*' }).Count -eq 1) 'expired approved-skip waiver is rejected'

foreach ($fieldMutation in @(
    @{ Name = 'reasonContains'; Value = 'e' },
    @{ Name = 'reasonCode'; Value = 'OTHER_REASON' },
    @{ Name = 'owner'; Value = 'arbitrary' },
    @{ Name = 'expiresOn'; Value = '2099-12-31' },
    @{ Name = 'evidencePaths'; Value = @('06_Testing/Automation/ov-01/ov01-scenario-registry.json') }
)) {
    $waiverMutationRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
    $waiver = @($waiverMutationRegistry.gates | Where-Object id -eq 'OV01-GATE-BE-FULL')[0].approvedSkippedTests[0]
    $waiver.($fieldMutation.Name) = $fieldMutation.Value
    $waiverMutationCheck = Test-Ov01Registry -Registry $waiverMutationRegistry -RepoRoot $repoRoot
    Assert-True (-not $waiverMutationCheck.valid -and @($waiverMutationCheck.errors | Where-Object { $_ -like 'approved skip contract is not the exact Cloudinary external waiver:*' }).Count -eq 1) "mutated approved-skip $($fieldMutation.Name) is rejected"
}

$controlCharacterRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$controlCharacterRegistry.gates[0].arguments[0] = "unsafe`r`nargument"
$controlCharacterRegistryCheck = Test-Ov01Registry -Registry $controlCharacterRegistry -RepoRoot $repoRoot
Assert-True (-not $controlCharacterRegistryCheck.valid -and @($controlCharacterRegistryCheck.errors | Where-Object { $_ -like 'gate argument contains a forbidden control character:*' }).Count -eq 1) 'registry rejects CR/LF gate arguments before execution'

$ambiguousQuoteRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$ambiguousQuoteRegistry.gates[0].arguments[0] = 'quote"&whoami'
$ambiguousQuoteRegistryCheck = Test-Ov01Registry -Registry $ambiguousQuoteRegistry -RepoRoot $repoRoot
Assert-True (-not $ambiguousQuoteRegistryCheck.valid -and @($ambiguousQuoteRegistryCheck.errors | Where-Object { $_ -like 'gate argument combines an embedded quote with a CMD separator:*' }).Count -eq 1) 'registry rejects ambiguous quote-plus-separator gate arguments'

$resolvedNpm = Resolve-Ov01GateExecutable -RepoRoot $repoRoot -WorkingDirectory (Join-Path $repoRoot '05_Development/CareBridgeWebApp') -Executable 'npm.cmd'
Assert-True ([System.IO.Path]::IsPathRooted($resolvedNpm)) 'bare tool executable resolves to an absolute retained identity'
$resolvedPinnedMaven = ''
try { $resolvedPinnedMaven = Resolve-Ov01GateExecutable -RepoRoot $repoRoot -WorkingDirectory (Join-Path $repoRoot '05_Development/CareBridgeAPI') -Executable '${MAVEN_PINNED_3_9_16}' } catch { $resolvedPinnedMaven = '' }
$pinnedMavenVersion = if (-not [string]::IsNullOrWhiteSpace($resolvedPinnedMaven) -and (Test-Path -LiteralPath $resolvedPinnedMaven -PathType Leaf)) { Invoke-Ov01Process -Executable $resolvedPinnedMaven -Arguments @('-v') -WorkingDirectory (Join-Path $repoRoot '05_Development/CareBridgeAPI') } else { $null }
Assert-True (
    -not [string]::IsNullOrWhiteSpace($resolvedPinnedMaven) -and
    [System.IO.Path]::IsPathRooted($resolvedPinnedMaven) -and
    $resolvedPinnedMaven -notmatch '(?i)CodexSandboxOffline' -and
    $null -ne $pinnedMavenVersion -and [int]$pinnedMavenVersion.exitCode -eq 0 -and [string]$pinnedMavenVersion.output -match 'Apache Maven 3\.9\.16'
) 'pinned Maven resolver locates and executes an actual Maven 3.9.16 binary'

$duplicateRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$duplicateRegistry.scenarios[1].id = $duplicateRegistry.scenarios[0].id
$duplicateCheck = Test-Ov01Registry -Registry $duplicateRegistry -RepoRoot $repoRoot
Assert-True (-not $duplicateCheck.valid -and @($duplicateCheck.errors | Where-Object { $_ -like 'duplicate scenario identity:*' }).Count -gt 0) 'duplicate scenario is rejected'

$orphanRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$orphanRegistry.scenarios[14].id = 'OV01-E2E-999'
$orphanCheck = Test-Ov01Registry -Registry $orphanRegistry -RepoRoot $repoRoot
Assert-True (-not $orphanCheck.valid -and @($orphanCheck.errors | Where-Object { $_ -like 'missing canonical scenario identity:*' }).Count -gt 0) 'orphan required scenario is rejected'

$pathRegistry = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$pathRegistry.scenarios[0].proofRefs[0].path = '../outside.txt'
$pathCheck = Test-Ov01Registry -Registry $pathRegistry -RepoRoot $repoRoot
Assert-True (-not $pathCheck.valid -and @($pathCheck.errors | Where-Object { $_ -like 'Path escapes repository root:*' }).Count -gt 0) 'path traversal is rejected'

$sanitized = Protect-Ov01Text -Text 'Authorization: Bearer abc.def.ghi password=secret person@example.com 0901234567'
Assert-True (@(Test-Ov01Leak -Text $sanitized).Count -eq 0) 'redaction removes high-confidence token/contact leaks'
Assert-True (@(Test-Ov01Leak -Text 'password=not-redacted').Count -gt 0) 'leak validator rejects raw secrets'
Assert-True (@(Test-Ov01Leak -Text 'sha256=0266a4405c94e143e5a635d189e86bc585d9be55a6547a41711cbb1c301f0e5d').Count -eq 0) 'hexadecimal SHA-256 identities do not trigger phone leak false positives'
$ansiVersionProbe = ([char]27) + '[31mFlutter 3.44.4' + ([char]27) + '[0m'
$normalizedAnsiVersionProbe = & (Get-Module Ov01Evidence) { param([string]$Value) Normalize-Ov01VersionOutput -Text $Value } $ansiVersionProbe
Assert-True (
    $normalizedAnsiVersionProbe -ceq 'Flutter 3.44.4' -and
    (Get-Ov01StringSha256 -Value $normalizedAnsiVersionProbe) -ceq (Get-Ov01StringSha256 -Value 'Flutter 3.44.4')
) 'Windows PowerShell 5 strips actual ESC ANSI sequences before version-output digesting'

$environmentProbeExecutable = (Get-Command powershell.exe -ErrorAction Stop).Source
$syntheticEnvironmentCollision = @(
    [System.Collections.DictionaryEntry]::new('PATH', 'C:\ov01-shadow-path-must-not-win'),
    [System.Collections.DictionaryEntry]::new('Path', [string]$env:Path),
    [System.Collections.DictionaryEntry]::new('TEMP', 'C:\tmp')
)
$normalizedSyntheticEnvironment = @(ConvertTo-Ov01NormalizedEnvironmentEntries -Entries $syntheticEnvironmentCollision)
$normalizedSyntheticPath = @($normalizedSyntheticEnvironment | Where-Object folded -eq 'PATH')
Assert-True ($normalizedSyntheticPath.Count -eq 1 -and [string]$normalizedSyntheticPath[0].name -ceq 'Path' -and [string]$normalizedSyntheticPath[0].value -ceq [string]$env:Path) 'duplicate Path/PATH normalization rejects the shadow value and deterministically preserves canonical Path'
$expectedCanonicalPath = [string]$env:Path
$environmentCollisionProbe = Invoke-Ov01Process -Executable $environmentProbeExecutable -Arguments @('-NoProfile', '-Command', '[Console]::Write($env:Path)') -WorkingDirectory $repoRoot -TimeoutSeconds 30
$pathEntriesAfter = @([System.Environment]::GetEnvironmentVariables([System.EnvironmentVariableTarget]::Process).GetEnumerator() | Where-Object { ([string]$_.Key).Equals('Path', [System.StringComparison]::OrdinalIgnoreCase) } | ForEach-Object { [pscustomobject]@{ name = [string]$_.Key; value = [string]$_.Value } })
Assert-True ($environmentCollisionProbe.exitCode -eq 0 -and [string]$environmentCollisionProbe.output -ceq $expectedCanonicalPath -and $pathEntriesAfter.Count -eq 1 -and [string]$pathEntriesAfter[0].name -ceq 'Path' -and [string]$pathEntriesAfter[0].value -ceq $expectedCanonicalPath) 'process launch normalizes Path keys case-insensitively with deterministic canonical-Path precedence'

function New-SyntheticCmd {
    param([string]$Name, [string[]]$Lines)
    $workingDirectory = Join-Path $tempRoot 'command-fixtures'
    if (-not (Test-Path -LiteralPath $workingDirectory)) {
        [void](New-Item -ItemType Directory -Path $workingDirectory)
    }
    $path = Join-Path $workingDirectory $Name
    [System.IO.File]::WriteAllText($path, (($Lines -join "`r`n") + "`r`n"), (New-Object System.Text.ASCIIEncoding))
    return $path
}

function New-SyntheticGate {
    param([string]$Id, [string]$Executable, [string[]]$Arguments = @())
    $workingDirectory = Join-Path $tempRoot 'command-fixtures'
    if (-not (Test-Path -LiteralPath $workingDirectory)) {
        [void](New-Item -ItemType Directory -Path $workingDirectory)
    }
    return [pscustomobject]@{
        id = $Id
        platform = 'Runner self-test'
        kind = 'test'
        workingDirectory = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $workingDirectory
        executable = $Executable
        arguments = $Arguments
        parser = 'ov01-json-line'
        expectedMinTests = 1
    }
}

$spacedFixtureDirectory = Join-Path $tempRoot 'command fixtures with spaces'
[void](New-Item -ItemType Directory -Path $spacedFixtureDirectory)
$spacedBatchPath = Join-Path $spacedFixtureDirectory 'flutter fixture with spaces.bat'
[System.IO.File]::WriteAllText($spacedBatchPath, (@(
    '@echo off',
    'if not "%~1"=="test" exit /b 31',
    'if not "%~2"=="test/features/journey/story_6_1_mobile_gap_test.dart" exit /b 32',
    'if not "%~3"=="test/features/journey/postpartum_recovery_setup_screen_test.dart" exit /b 33',
    'if not "%~4"=="test/features/consultation/triage_expert_handoff_mobile_test.dart" exit /b 34',
    'if not "%~5"=="--reporter" exit /b 35',
    'if not "%~6"=="compact" exit /b 36',
    'if not "%~7"=="--no-color" exit /b 37',
    'if not "%~8"=="" exit /b 38',
    'echo fixture stderr retained 1>&2',
    'echo OV01_RESULT {"tests":7,"failures":0,"errors":0,"skipped":0}',
    'exit /b 0'
) -join "`r`n") + "`r`n", (New-Object System.Text.ASCIIEncoding))
$spacedBatchArguments = @(
    'test',
    'test/features/journey/story_6_1_mobile_gap_test.dart',
    'test/features/journey/postpartum_recovery_setup_screen_test.dart',
    'test/features/consultation/triage_expert_handoff_mobile_test.dart',
    '--reporter', 'compact', '--no-color'
)
$spacedBatchGate = New-SyntheticGate -Id 'SELF-SPACED-BAT' -Executable $spacedBatchPath -Arguments $spacedBatchArguments
$spacedBatchGate.expectedMinTests = 7
$spacedBatchGate | Add-Member -NotePropertyName timeoutSeconds -NotePropertyValue 5
$spacedBatchResult = Invoke-Ov01Gate -Gate $spacedBatchGate -RepoRoot $repoRoot -LogDirectory $tempRoot
$spacedBatchLog = Get-Content -Raw -LiteralPath (Join-Path $tempRoot 'SELF-SPACED-BAT.log')
Assert-True (
    $spacedBatchResult.status -eq 'PASS' -and
    $spacedBatchResult.exitCode -eq 0 -and
    [int]$spacedBatchResult.totals.tests -eq 7 -and
    $spacedBatchLog -match 'fixture stderr retained'
) 'Windows .bat under a spaced path preserves long arguments, stderr, exit code, and parsed totals within its gate timeout'

$nestedChildPath = Join-Path $spacedFixtureDirectory 'nested dart child.bat'
[System.IO.File]::WriteAllText($nestedChildPath, (@(
    '@echo off',
    'if not "%~1"=="test" exit /b 51',
    'if not "%~2"=="test/features/journey/story_6_1_mobile_gap_test.dart" exit /b 52',
    '> "%OV01_NESTED_CHILD_MARKER%" echo nested-child-launched',
    'echo NESTED_CHILD_PROCESS_LAUNCHED',
    'echo OV01_RESULT {"tests":2,"failures":0,"errors":0,"skipped":0}',
    'exit /b 0'
) -join "`r`n") + "`r`n", (New-Object System.Text.ASCIIEncoding))
$nestedLauncherPath = Join-Path $spacedFixtureDirectory 'flutter nested launcher.bat'
[System.IO.File]::WriteAllText($nestedLauncherPath, (@(
    '@echo off',
    'setlocal',
    'for %%i in ("%~dp0.") do set "FIXTURE_ROOT=%%~fi"',
    'call "%FIXTURE_ROOT%\nested dart child.bat" %*',
    'exit /b %ERRORLEVEL%'
) -join "`r`n") + "`r`n", (New-Object System.Text.ASCIIEncoding))
$nestedMarkerPath = Join-Path $tempRoot 'nested-child.marker'
$nestedLauncherGate = New-SyntheticGate -Id 'SELF-NESTED-FLUTTER-BAT' -Executable $nestedLauncherPath -Arguments @('test', 'test/features/journey/story_6_1_mobile_gap_test.dart')
$nestedLauncherGate.expectedMinTests = 2
$nestedLauncherGate | Add-Member -NotePropertyName timeoutSeconds -NotePropertyValue 5
$nestedLauncherGate | Add-Member -NotePropertyName environment -NotePropertyValue ([pscustomobject]@{ OV01_NESTED_CHILD_MARKER = $nestedMarkerPath })
$nestedLauncherResult = Invoke-Ov01Gate -Gate $nestedLauncherGate -RepoRoot $repoRoot -LogDirectory $tempRoot
$nestedLauncherLog = Get-Content -Raw -LiteralPath (Join-Path $tempRoot 'SELF-NESTED-FLUTTER-BAT.log')
Assert-True (
    $nestedLauncherResult.status -eq 'PASS' -and $nestedLauncherResult.exitCode -eq 0 -and
    [int]$nestedLauncherResult.totals.tests -eq 2 -and $nestedLauncherLog -match 'NESTED_CHILD_PROCESS_LAUNCHED' -and
    (Test-Path -LiteralPath $nestedMarkerPath) -and (Get-Content -Raw -LiteralPath $nestedMarkerPath).Trim() -eq 'nested-child-launched'
) 'Flutter-like nested batch launcher starts its child batch and retains child output, exit code, and totals'

$metacharBatchPath = Join-Path $spacedFixtureDirectory 'cmd metachar round trip fixture.bat'
[System.IO.File]::WriteAllText($metacharBatchPath, (@(
    '@echo off',
    'setlocal DisableDelayedExpansion',
    'set "ARG=%~1"',
    'set ARG',
    'echo OV01_RESULT {"tests":1,"failures":0,"errors":0,"skipped":0}',
    'exit /b 0'
) -join "`r`n") + "`r`n", (New-Object System.Text.ASCIIEncoding))
$injectionMarkerPath = Join-Path $tempRoot 'cmd-injection-marker.txt'
$metacharCases = @(
    'literal&whoami',
    'pipe|ver',
    'caret^value',
    'percent%PATH%value',
    'bang!value!',
    'less<input.txt',
    'greater>output.txt',
    '(parenthesized)',
    'quote"inside',
    'space value',
    'trailing\',
    ('literal&echo.OV01_INJECTED>' + $injectionMarkerPath)
)
$metacharFailures = New-Object System.Collections.Generic.List[string]
for ($caseIndex = 0; $caseIndex -lt $metacharCases.Count; $caseIndex++) {
    $caseValue = [string]$metacharCases[$caseIndex]
    $gateId = 'SELF-CMD-ARG-{0:d2}' -f ($caseIndex + 1)
    $caseGate = New-SyntheticGate -Id $gateId -Executable $metacharBatchPath -Arguments @($caseValue)
    $caseGate | Add-Member -NotePropertyName timeoutSeconds -NotePropertyValue 5
    $caseResult = Invoke-Ov01Gate -Gate $caseGate -RepoRoot $repoRoot -LogDirectory $tempRoot
    $caseLines = @(Get-Content -LiteralPath (Join-Path $tempRoot ($gateId + '.log')) | Where-Object { -not [string]::IsNullOrEmpty($_) })
    $expectedArgumentLine = 'ARG=' + $caseValue
    $expectedResultLine = 'OV01_RESULT {"tests":1,"failures":0,"errors":0,"skipped":0}'
    if ($caseResult.status -ne 'PASS' -or $caseResult.exitCode -ne 0 -or [int]$caseResult.totals.tests -ne 1 -or
        $caseLines.Count -ne 2 -or $caseLines[0] -cne $expectedArgumentLine -or $caseLines[1] -cne $expectedResultLine) {
        $metacharFailures.Add("$gateId value=$caseValue exit=$($caseResult.exitCode) status=$($caseResult.status) lines=$($caseLines -join '<NL>')")
    }
}
if ($metacharFailures.Count -gt 0) { $metacharFailures | ForEach-Object { Write-Output "CMD_ARG_MISMATCH $_" } }
Assert-True ($metacharFailures.Count -eq 0 -and -not (Test-Path -LiteralPath $injectionMarkerPath)) 'Windows batch arguments round-trip CMD metacharacters exactly without command injection or side effects'
$controlCharacterRejected = $false
try {
    [void](Invoke-Ov01Process -Executable $metacharBatchPath -Arguments @("safe`r`nunsafe") -WorkingDirectory (Join-Path $tempRoot 'command-fixtures') -TimeoutSeconds 5)
} catch { $controlCharacterRejected = $_.Exception.Message -like '*forbidden control character*' }
Assert-True $controlCharacterRejected 'Windows batch invocation rejects CR/LF argument injection before process creation'
$ambiguousQuotePayloadRejected = $false
try {
    [void](Invoke-Ov01Process -Executable $metacharBatchPath -Arguments @(('quote"&echo.OV01_QUOTE_INJECTED>' + $injectionMarkerPath)) -WorkingDirectory (Join-Path $tempRoot 'command-fixtures') -TimeoutSeconds 5)
} catch { $ambiguousQuotePayloadRejected = $_.Exception.Message -like '*embedded quote with a CMD separator*' }
Assert-True ($ambiguousQuotePayloadRejected -and -not (Test-Path -LiteralPath $injectionMarkerPath)) 'Windows batch invocation rejects ambiguous quote-plus-separator payloads before process creation'

$timeoutBatchPath = Join-Path $spacedFixtureDirectory 'timeout fixture with spaces.bat'
[System.IO.File]::WriteAllText($timeoutBatchPath, (@(
    '@echo off',
    'powershell.exe -NoProfile -Command "[System.IO.File]::WriteAllText($env:OV01_TIMEOUT_PID_FILE, [string]$PID); Start-Sleep -Seconds 30"',
    'exit /b 0'
) -join "`r`n") + "`r`n", (New-Object System.Text.ASCIIEncoding))
$timeoutBatchGate = New-SyntheticGate -Id 'SELF-TIMEOUT' -Executable $timeoutBatchPath
$timeoutBatchGate | Add-Member -NotePropertyName timeoutSeconds -NotePropertyValue 1
$timeoutPidPath = Join-Path $tempRoot 'timeout-child.pid'
$timeoutBatchGate | Add-Member -NotePropertyName environment -NotePropertyValue ([pscustomobject]@{ OV01_TIMEOUT_PID_FILE = $timeoutPidPath })
$timeoutBatchResult = Invoke-Ov01Gate -Gate $timeoutBatchGate -RepoRoot $repoRoot -LogDirectory $tempRoot
$timeoutBatchLog = Get-Content -Raw -LiteralPath (Join-Path $tempRoot 'SELF-TIMEOUT.log')
$timeoutChildPid = if (Test-Path -LiteralPath $timeoutPidPath) { [int](Get-Content -Raw -LiteralPath $timeoutPidPath) } else { 0 }
$timeoutChildStopped = $timeoutChildPid -gt 0 -and $null -eq (Get-Process -Id $timeoutChildPid -ErrorAction SilentlyContinue)
Assert-True (
    $timeoutBatchResult.status -eq 'FAIL' -and
    $timeoutBatchResult.timedOut -and
    $timeoutBatchResult.exitCode -eq 124 -and
    $timeoutBatchResult.timeoutSeconds -eq 1 -and
    $timeoutBatchLog -match 'OV01_PROCESS_TIMEOUT timeoutSeconds=1' -and
    $timeoutBatchResult.durationMs -lt 15000
) 'hung batch gate is bounded, returns timeout failure, and seals timeout evidence'
Assert-True ($timeoutChildStopped -and [bool]$timeoutBatchResult.childTreeTerminated) 'timeout termination kills the spawned child process tree'

$workingDirectoryCmd = New-SyntheticCmd -Name 'working-directory.cmd' -Lines @(
    '@echo off',
    'echo OV01_RESULT {"tests":1,"failures":0,"errors":0,"skipped":0}'
)
$workingDirectoryGate = New-SyntheticGate -Id 'SELF-WORKDIR' -Executable './working-directory.cmd'
$workingDirectoryResult = Invoke-Ov01Gate -Gate $workingDirectoryGate -RepoRoot $repoRoot -LogDirectory $tempRoot
Assert-True ($workingDirectoryResult.status -eq 'PASS' -and $workingDirectoryResult.exitCode -eq 0) 'dot-relative executable resolves against gate working directory'

$stderrCmd = New-SyntheticCmd -Name 'stderr-success.cmd' -Lines @(
    '@echo off',
    'echo synthetic build warning 1>&2',
    'echo OV01_RESULT {"tests":1,"failures":0,"errors":0,"skipped":0}',
    'exit /b 0'
)
$stderrGate = New-SyntheticGate -Id 'SELF-STDERR' -Executable 'stderr-success.cmd'
$stderrResult = Invoke-Ov01Gate -Gate $stderrGate -RepoRoot $repoRoot -LogDirectory $tempRoot
Assert-True ($stderrResult.status -eq 'PASS' -and $stderrResult.exitCode -eq 0) 'successful Windows cmd may emit stderr without becoming exit 9009'

$environmentCmd = New-SyntheticCmd -Name 'environment.cmd' -Lines @(
    '@echo off',
    'if not "%PYTHONPATH%"=="src" exit /b 17',
    'echo OV01_RESULT {"tests":1,"failures":0,"errors":0,"skipped":0}'
)
$environmentGate = New-SyntheticGate -Id 'SELF-ENV' -Executable 'environment.cmd'
$environmentGate | Add-Member -NotePropertyName environment -NotePropertyValue ([pscustomobject]@{ PYTHONPATH = 'src' })
$environmentGate | Add-Member -NotePropertyName evidenceEnvironmentKeys -NotePropertyValue @('PYTHONPATH')
$environmentResult = Invoke-Ov01Gate -Gate $environmentGate -RepoRoot $repoRoot -LogDirectory $tempRoot
Assert-True ($environmentResult.status -eq 'PASS' -and $environmentResult.exitCode -eq 0 -and [string]$environmentResult.environmentEvidence.PYTHONPATH -eq 'src') 'gate-specific allowlisted environment is applied and retained as evidence'

$wrapperCmd = New-SyntheticCmd -Name 'wrapper-failure.cmd' -Lines @(
    '@echo off',
    'if not "%~2"=="" exit /b 19',
    'echo Cannot start maven from wrapper',
    'exit /b 1'
)
$fallbackCmd = New-SyntheticCmd -Name 'fallback-success.cmd' -Lines @(
    '@echo off',
    'if not "%~1"=="marker" exit /b 20',
    'if not "%~2"=="" exit /b 21',
    'echo OV01_RESULT {"tests":1,"failures":0,"errors":0,"skipped":0}'
)
$fallbackGate = New-SyntheticGate -Id 'SELF-FALLBACK' -Executable './wrapper-failure.cmd' -Arguments @('marker')
$fallbackGate | Add-Member -NotePropertyName fallback -NotePropertyValue ([pscustomobject]@{
    executable = './fallback-success.cmd'
    whenOutputMatches = 'Cannot start maven from wrapper'
})
$fallbackResult = Invoke-Ov01Gate -Gate $fallbackGate -RepoRoot $repoRoot -LogDirectory $tempRoot
Assert-True ($fallbackResult.status -eq 'PASS' -and $fallbackResult.fallbackUsed -and $fallbackResult.primaryExitCode -eq 1) 'wrapper pre-execution defect falls back once without duplicating arguments'

$pytestNonPassCmd = New-SyntheticCmd -Name 'pytest-nonpass.cmd' -Lines @(
    '@echo off',
    'echo safety-selector',
    'echo 1 passed, 1 xfailed, 1 xpassed, 2 deselected in 0.10s',
    'exit /b 0'
)
$pytestNonPassGate = New-SyntheticGate -Id 'SELF-PYTEST-NONPASS' -Executable 'pytest-nonpass.cmd'
$pytestNonPassGate.parser = 'pytest'
$pytestNonPassResult = Invoke-Ov01Gate -Gate $pytestNonPassGate -RepoRoot $repoRoot -LogDirectory $tempRoot -ExecutableSelectors @([pscustomobject]@{ scenarioId = 'OV01-E2E-011'; selector = 'safety-selector' })
$pytestTotalsExtended = $null -ne $pytestNonPassResult.totals.PSObject.Properties['xfailed'] -and $null -ne $pytestNonPassResult.totals.PSObject.Properties['xpassed'] -and $null -ne $pytestNonPassResult.totals.PSObject.Properties['deselected']
Assert-True (
    $pytestTotalsExtended -and
    [int]$pytestNonPassResult.totals.xfailed -eq 1 -and [int]$pytestNonPassResult.totals.xpassed -eq 1 -and [int]$pytestNonPassResult.totals.deselected -eq 2 -and
    $pytestNonPassResult.selectorResults[0].matched -and $pytestNonPassResult.status -eq 'FAIL' -and $pytestNonPassResult.forbiddenOutcomeDetected
) 'pytest xfailed/xpassed/deselected outcomes fail even when the safety selector is observed and exit is zero'

function New-SyntheticRegistryFile {
    param([int]$Tests, [int]$Failures, [int]$Errors, [int]$Skipped, [string]$Suffix = '', [string]$GateCommandPrefix = '')
    $synthetic = ($registry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
    $gate = @($synthetic.gates | Where-Object id -eq 'OV01-GATE-BE-LIFECYCLE')[0]
    $gate.executable = 'powershell.exe'
    $gate.PSObject.Properties.Remove('versionArguments')
    $gate.PSObject.Properties.Remove('expectedVersionRegex')
    $gate.PSObject.Properties.Remove('fallback')
    $gate | Add-Member -Force -NotePropertyName versionArguments -NotePropertyValue @('-NoProfile', '-Command', "Write-Output 'PowerShellSynthetic 1.0'")
    $gate | Add-Member -Force -NotePropertyName expectedVersionRegex -NotePropertyValue 'PowerShellSynthetic 1\.0'
    $payload = @{ tests = $Tests; failures = $Failures; errors = $Errors; skipped = $Skipped } | ConvertTo-Json -Compress
    $gateCommand = if ([string]::IsNullOrWhiteSpace($GateCommandPrefix)) {
        "Write-Output 'JourneyOnboardingIntegrationTest'; Write-Output 'OV01_RESULT $payload$Suffix'"
    } else {
        "$GateCommandPrefix; Write-Output 'JourneyOnboardingIntegrationTest'; Write-Output 'OV01_RESULT $payload$Suffix'"
    }
    $gate.arguments = @('-NoProfile', '-Command', $gateCommand)
    $gate.parser = 'ov01-json-line'
    $gate.expectedMinTests = 1
    $file = Join-Path $tempRoot ('registry-' + [Guid]::NewGuid().ToString('N') + '.json')
    [System.IO.File]::WriteAllText($file, ($synthetic | ConvertTo-Json -Depth 30) + "`n", (New-Object System.Text.UTF8Encoding($false)))
    return $file
}

function Invoke-SyntheticRun {
    param([string]$SyntheticRegistry, [string]$OutputDirectory, [string[]]$Scenarios = @('OV01-E2E-001'), [ValidateSet('Release', 'Diagnostic')][string]$RunMode = 'Diagnostic', [string]$TouchedPathFile = $touchedPath)
    $arguments = @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $toolRoot 'Invoke-Ov01QualityGate.ps1'),
        '-ScenarioId'
    ) + $Scenarios + @(
        '-RegistryPath', $SyntheticRegistry,
        '-RunMode', $RunMode,
        '-BaselineHead', $baselineHead,
        '-TouchedPathsFile', $TouchedPathFile,
        '-OutputDirectory', $OutputDirectory
    )
    & powershell.exe @arguments | Out-Host
    return $LASTEXITCODE
}

$greenRegistryPath = New-SyntheticRegistryFile -Tests 3 -Failures 0 -Errors 0 -Skipped 0
$greenOutput = Join-Path $tempRoot 'green'
$greenExit = Invoke-SyntheticRun -SyntheticRegistry $greenRegistryPath -OutputDirectory $greenOutput
Assert-True ($greenExit -eq 0) 'green runner exits zero'
$greenReport = Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $greenOutput 'run-report.json') | ConvertFrom-Json
$greenRegistry = Read-Ov01Registry -Path $greenRegistryPath
Assert-True ($greenReport.overallStatus -eq 'NOT_APPLICABLE' -and $greenReport.diagnosticStatus -eq 'PASS' -and -not $greenReport.releaseEligible -and $greenReport.gateResults[0].totals.tests -eq 3) 'green diagnostic runner records exact non-zero totals without release PASS'
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$greenReport.sourceIdentity.compositeSha256)) 'green runner records composite source identity'
Assert-True (@($greenReport.artifacts | Where-Object sourceIdentity -ne $greenReport.sourceIdentity.compositeSha256).Count -eq 0) 'all artifacts bind to the composite source identity'
Assert-True (@($greenReport.artifacts | Where-Object { [string]$_.registryRelativePath -ne [string]$greenReport.sourceIdentity.registryRelativePath -or [string]$_.registrySha256 -ne [string]$greenReport.sourceIdentity.registrySha256 }).Count -eq 0) 'all artifacts bind to the actual registry path and SHA-256'
$sealCheck = Test-Ov01ClosedSetManifest -EvidenceDirectory $greenOutput
Assert-True $sealCheck.valid 'fresh closed-set evidence seal validates'
$greenReportReplayCheck = Test-Ov01RunReport -Report $greenReport -Registry $greenRegistry -EvidenceDirectory $greenOutput
Assert-True $greenReportReplayCheck.valid 'green report replays gate logs and artifacts against filesystem bytes and SHA-256'

$checkpointFixture = Join-Path $tempRoot 'checkpoint-fixture'
$checkpointLogs = Join-Path $checkpointFixture 'logs'
[void](New-Item -ItemType Directory -Path $checkpointLogs -Force)
$externalKeyRoot = 'D:\tmp\ov01-runner-selftest-keys'
[void](New-Item -ItemType Directory -Path $externalKeyRoot -Force)
$externalKeyRunRoot = Join-Path $externalKeyRoot ([Guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $externalKeyRunRoot)
$checkpointKeyPath = Join-Path $externalKeyRunRoot 'checkpoint-key.json'
$checkpointAuthenticationContext = Get-Ov01CheckpointAuthenticationContext -CheckpointKeyPath $checkpointKeyPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -Create
$checkpointKeyAttestation = $checkpointAuthenticationContext.attestation
$checkpointSourcePath = Join-Path $checkpointFixture 'source-identity.json'
[System.IO.File]::WriteAllText($checkpointSourcePath, ($greenReport.sourceIdentity | ConvertTo-Json -Depth 30) + "`n", (New-Object System.Text.UTF8Encoding($false)))
$firstCheckpointResult = ($greenReport.gateResults[0] | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$greenLogPath = Resolve-Ov01RepoPath -Root $greenOutput -RelativePath ([string]$firstCheckpointResult.logPath)
$checkpointLogCorpus = Get-Content -Encoding UTF8 -Raw -LiteralPath $greenLogPath
$checkpointReleaseSelectors = New-Object System.Collections.Generic.List[object]
foreach ($scenario in @($greenRegistry.scenarios)) {
    foreach ($selector in @($scenario.executableSelectors | Where-Object { [string]$_.gateId -eq 'OV01-GATE-BE-LIFECYCLE' })) {
        $selectorText = [string]$selector.selector
        $checkpointReleaseSelectors.Add([pscustomobject]@{ scenarioId = [string]$scenario.id; gateId = 'OV01-GATE-BE-LIFECYCLE'; selector = $selectorText; matched = ($checkpointLogCorpus.IndexOf($selectorText, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) })
    }
}
$firstCheckpointResult.selectorResults = $checkpointReleaseSelectors.ToArray()
$firstCheckpointResult.status = if (@($checkpointReleaseSelectors | Where-Object { -not [bool]$_.matched }).Count -eq 0) { 'PASS' } else { 'FAIL' }
$checkpointLogPath = Join-Path $checkpointFixture ([string]$firstCheckpointResult.logPath)
Copy-Item -LiteralPath $greenLogPath -Destination $checkpointLogPath
$firstCheckpointResult.logBytes = [int64](Get-Item -LiteralPath $checkpointLogPath).Length
$firstCheckpointResult.logSha256 = Get-Ov01Sha256 -Path $checkpointLogPath
$checkpointRegistry = ($greenRegistry | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$checkpointArtifactDirectory = Join-Path $checkpointFixture 'artifacts'
[void](New-Item -ItemType Directory -Path $checkpointArtifactDirectory -Force)
foreach ($artifactDefinition in @($checkpointRegistry.artifacts | Where-Object {
    [string]$_.id -ne 'OV01-ART-REGISTRY' -and ([string]$_.kind -eq 'input' -or @($_.requiredForGateIds) -contains 'OV01-GATE-BE-LIFECYCLE')
})) {
    $isolatedArtifactPath = Join-Path $checkpointArtifactDirectory (([string]$artifactDefinition.id).ToLowerInvariant() + '.txt')
    [System.IO.File]::WriteAllText($isolatedArtifactPath, "isolated checkpoint artifact $($artifactDefinition.id)", (New-Object System.Text.UTF8Encoding($false)))
    $artifactDefinition.path = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $isolatedArtifactPath
    $artifactDefinition.allowMany = $false
}
$checkpointRegistryPath = Join-Path $tempRoot 'checkpoint-registry.json'
[System.IO.File]::WriteAllText($checkpointRegistryPath, ($checkpointRegistry | ConvertTo-Json -Depth 30) + "`n", (New-Object System.Text.UTF8Encoding($false)))
$checkpointArtifactResult = Get-Ov01ArtifactRecords -Registry $checkpointRegistry -RepoRoot $repoRoot -SelectedGateIds @('OV01-GATE-BE-LIFECYCLE') -SourceIdentity $greenReport.sourceIdentity
$checkpointNow = [DateTimeOffset]::UtcNow.ToString('o')
$checkpointObject = [pscustomobject]@{
    schemaVersion = 1
    runnerId = 'OV01-AUTO-002'
    runMode = 'Release'
    outputDirectory = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $checkpointFixture
    createdUtc = $checkpointNow
    updatedUtc = $checkpointNow
    preRunSourceIdentity = $greenReport.sourceIdentity
    gateOrder = @(Get-Ov01CanonicalReleaseGateIds)
    manualEvidence = $manualCheck.record
    completedGateResults = @([pscustomobject]@{ id = 'OV01-GATE-BE-LIFECYCLE'; resultSha256 = Get-Ov01ObjectSha256 -Value $firstCheckpointResult; result = $firstCheckpointResult })
    activeGate = $null
    artifactRecords = @($checkpointArtifactResult.records)
    evidenceFiles = @(Get-Ov01EvidenceFileRecords -EvidenceDirectory $checkpointFixture -ExcludedRelativePaths @('release-checkpoint.json'))
}
$checkpointSeedPath = Join-Path $tempRoot 'checkpoint-seed.json'
$checkpointExpectedSourcePath = Join-Path $tempRoot 'checkpoint-expected-source.json'
$checkpointExpectedManualPath = Join-Path $tempRoot 'checkpoint-expected-manual.json'
$checkpointPath = Join-Path $checkpointFixture 'release-checkpoint.json'
[System.IO.File]::WriteAllText($checkpointSeedPath, ($checkpointObject | ConvertTo-Json -Depth 50) + "`n", (New-Object System.Text.UTF8Encoding($false)))
[System.IO.File]::WriteAllText($checkpointExpectedSourcePath, ($greenReport.sourceIdentity | ConvertTo-Json -Depth 30) + "`n", (New-Object System.Text.UTF8Encoding($false)))
[System.IO.File]::WriteAllText($checkpointExpectedManualPath, ([pscustomobject]@{ record = $manualCheck.record } | ConvertTo-Json -Depth 30) + "`n", (New-Object System.Text.UTF8Encoding($false)))
$checkpointWriterScript = Join-Path $tempRoot 'checkpoint-process-1.ps1'
$checkpointResumeScript = Join-Path $tempRoot 'checkpoint-process-2.ps1'
$modulePathForScript = (Join-Path $toolRoot 'Ov01Evidence.psm1').Replace("'", "''")
[System.IO.File]::WriteAllText($checkpointWriterScript, "Import-Module '$modulePathForScript' -Force`n`$value=Get-Content -Raw '$($checkpointSeedPath.Replace("'", "''"))'|ConvertFrom-Json`n`$auth=Get-Ov01CheckpointAuthenticationContext -CheckpointKeyPath '$($checkpointKeyPath.Replace("'", "''"))' -RepoRoot '$($repoRoot.Replace("'", "''"))' -EvidenceDirectory '$($checkpointFixture.Replace("'", "''"))'`nWrite-Ov01AuthenticatedCheckpoint -Path '$($checkpointPath.Replace("'", "''"))' -Value `$value -AuthenticationContext `$auth`n", (New-Object System.Text.UTF8Encoding($false)))
[System.IO.File]::WriteAllText($checkpointResumeScript, "Import-Module '$modulePathForScript' -Force`n`$source=Get-Content -Raw '$($checkpointExpectedSourcePath.Replace("'", "''"))'|ConvertFrom-Json`n`$manual=Get-Content -Raw '$($checkpointExpectedManualPath.Replace("'", "''"))'|ConvertFrom-Json`n`$registry=Get-Content -Raw '$($checkpointRegistryPath.Replace("'", "''"))'|ConvertFrom-Json`n`$check=Test-Ov01ReleaseCheckpoint -CheckpointPath '$($checkpointPath.Replace("'", "''"))' -RepoRoot '$($repoRoot.Replace("'", "''"))' -EvidenceDirectory '$($checkpointFixture.Replace("'", "''"))' -ExpectedSourceIdentity `$source -ExpectedManualEvidence `$manual -Registry `$registry -CheckpointKeyPath '$($checkpointKeyPath.Replace("'", "''"))'`nif(-not `$check.valid){`$check.errors|Write-Error;exit 1}`n", (New-Object System.Text.UTF8Encoding($false)))
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $checkpointWriterScript | Out-Null
$checkpointWriterExit = $LASTEXITCODE
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $checkpointResumeScript | Out-Null
$checkpointResumeExit = $LASTEXITCODE
Assert-True ($checkpointWriterExit -eq 0 -and $checkpointResumeExit -eq 0) 'valid Release checkpoint survives atomic two-process handoff and resume validation'

$checkpointValid = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
Assert-True $checkpointValid.valid 'valid ordered-prefix checkpoint replays source, manual, logs, artifacts, and evidence closed set'
$checkpointTextWithoutSecrets = Get-Content -Encoding UTF8 -Raw -LiteralPath $checkpointPath
$protectedKeyValue = [string]((Get-Content -Encoding UTF8 -Raw -LiteralPath $checkpointKeyPath | ConvertFrom-Json).protectedKeyBase64)
Assert-True ($checkpointTextWithoutSecrets -notmatch 'protectedKeyBase64' -and -not $checkpointTextWithoutSecrets.Contains($protectedKeyValue) -and [string]$checkpointValid.checkpoint.authentication.protection -eq 'DPAPI-CurrentUser') 'checkpoint retains DPAPI/HMAC attestation but never protected or raw key material'

$missingExternalKeyCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath ($checkpointKeyPath + '.missing')
Assert-True (-not $missingExternalKeyCheck.valid -and @($missingExternalKeyCheck.errors | Where-Object { $_ -like 'release checkpoint external authentication failed:*missing*' }).Count -eq 1) 'resume rejects a missing external checkpoint key without generating a replacement'
$crossVolumeRejected = $false
try { [void](Get-Ov01CheckpointAuthenticationContext -CheckpointKeyPath 'C:\tmp\ov01-cross-volume-key.json' -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture) }
catch { $crossVolumeRejected = $_.Exception.Message -like '*must share one volume*' }
Assert-True $crossVolumeRejected 'checkpoint state on a different volume is rejected because atomic replacement cannot be guaranteed'

$wrongCheckpointKeyPath = Join-Path $externalKeyRunRoot 'wrong-checkpoint-key.json'
$wrongCheckpointKeyContext = Get-Ov01CheckpointAuthenticationContext -CheckpointKeyPath $wrongCheckpointKeyPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -Create
$wrongExternalKeyCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $wrongCheckpointKeyPath
Assert-True (-not $wrongExternalKeyCheck.valid -and @($wrongExternalKeyCheck.errors | Where-Object { $_ -eq 'release checkpoint external authentication failed' }).Count -eq 1) 'resume rejects a different valid DPAPI-protected checkpoint key'

$relocatedCheckpointKeyPath = Join-Path $externalKeyRunRoot 'relocated-checkpoint-key.json'
Copy-Item -LiteralPath $checkpointKeyPath -Destination $relocatedCheckpointKeyPath
$relocatedSecurity = [System.IO.File]::GetAccessControl($checkpointKeyPath)
[System.IO.File]::SetAccessControl($relocatedCheckpointKeyPath, $relocatedSecurity)
$relocatedExternalKeyCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $relocatedCheckpointKeyPath
Assert-True (-not $relocatedExternalKeyCheck.valid -and @($relocatedExternalKeyCheck.errors | Where-Object { $_ -like 'release checkpoint external authentication failed*' }).Count -eq 1) 'resume rejects a byte-identical key file relocated to a different absolute path'

$originalCheckpointKeyText = Get-Content -Encoding UTF8 -Raw -LiteralPath $checkpointKeyPath
[System.IO.File]::AppendAllText($checkpointKeyPath, " `n", (New-Object System.Text.UTF8Encoding($false)))
try {
    $changedExternalKeyCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
    Assert-True (-not $changedExternalKeyCheck.valid -and @($changedExternalKeyCheck.errors | Where-Object { $_ -eq 'release checkpoint external authentication failed' }).Count -eq 1) 'resume rejects changed external key-file bytes even when DPAPI payload still parses'
} finally { [System.IO.File]::WriteAllText($checkpointKeyPath, $originalCheckpointKeyText, (New-Object System.Text.UTF8Encoding($false))) }

$externalAtomicPrefix = '.ov01-checkpoint-' + [string]$checkpointKeyAttestation.keyId
$simulatedTempPath = Join-Path $checkpointAuthenticationContext.stateDirectory ($externalAtomicPrefix + '.tmp-' + [Guid]::NewGuid().ToString('N'))
[System.IO.File]::WriteAllText($simulatedTempPath, '{"truncated":', (New-Object System.Text.UTF8Encoding($false)))
$simulatedBackupPath = Join-Path $checkpointAuthenticationContext.stateDirectory ($externalAtomicPrefix + '.bak-' + [Guid]::NewGuid().ToString('N'))
Copy-Item -LiteralPath $checkpointPath -Destination $simulatedBackupPath
$simulatedTempShaBefore = Get-Ov01Sha256 -Path $simulatedTempPath
$simulatedBackupShaBefore = Get-Ov01Sha256 -Path $simulatedBackupPath
$externalOrphanInventory = Get-Ov01AuthenticatedCheckpointArtifactInventory -AuthenticationContext $checkpointAuthenticationContext
$checkpointWithExternalOrphans = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
Assert-True (
    $externalOrphanInventory.valid -and $externalOrphanInventory.orphanCount -eq 2 -and
    (Test-Path -LiteralPath $simulatedTempPath) -and (Get-Ov01Sha256 -Path $simulatedTempPath) -eq $simulatedTempShaBefore -and
    (Test-Path -LiteralPath $simulatedBackupPath) -and (Get-Ov01Sha256 -Path $simulatedBackupPath) -eq $simulatedBackupShaBefore -and
    $checkpointWithExternalOrphans.valid -and
    @(Get-ChildItem -LiteralPath $checkpointFixture -Recurse -File | Where-Object { $_.Name -match '\.(?:tmp|bak)(?:-|$)' }).Count -eq 0
) 'exact same-keyId temp/backup crash orphans remain byte-identical outside evidence and do not affect resume HMAC validation'

function Invoke-CheckpointMutation {
    param([Parameter(Mandatory = $true)][scriptblock]$Mutation)
    $originalText = Get-Content -Encoding UTF8 -Raw -LiteralPath $checkpointPath
    $value = $originalText | ConvertFrom-Json
    & $Mutation $value
    Write-Ov01AuthenticatedCheckpoint -Path $checkpointPath -Value $value -AuthenticationContext $checkpointAuthenticationContext
    try { return Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath }
    finally { [System.IO.File]::WriteAllText($checkpointPath, $originalText, (New-Object System.Text.UTF8Encoding($false))) }
}

$checkpointUnknownField = Invoke-CheckpointMutation { param($value) $value | Add-Member -NotePropertyName unexpected -NotePropertyValue $true }
Assert-True (-not $checkpointUnknownField.valid -and @($checkpointUnknownField.errors | Where-Object { $_ -eq 'release checkpoint root fields are not exact' }).Count -eq 1) 'checkpoint with unknown root field is rejected'
$checkpointMissingField = Invoke-CheckpointMutation { param($value) $value.PSObject.Properties.Remove('updatedUtc') }
Assert-True (-not $checkpointMissingField.valid -and @($checkpointMissingField.errors | Where-Object { $_ -eq 'release checkpoint root fields are not exact' }).Count -eq 1) 'checkpoint with a missing root field is rejected'
$checkpointReordered = Invoke-CheckpointMutation { param($value) $tmp=$value.gateOrder[0];$value.gateOrder[0]=$value.gateOrder[1];$value.gateOrder[1]=$tmp }
Assert-True (-not $checkpointReordered.valid -and @($checkpointReordered.errors | Where-Object { $_ -eq 'release checkpoint gate order is not the exact 17-gate catalog' }).Count -eq 1) 'checkpoint with reordered gate catalog is rejected'
$checkpointForeignSource = Invoke-CheckpointMutation { param($value) $value.preRunSourceIdentity.compositeSha256='f'*64 }
Assert-True (-not $checkpointForeignSource.valid -and @($checkpointForeignSource.errors | Where-Object { $_ -like 'release checkpoint source identity mismatch:*' }).Count -ge 1) 'checkpoint bound to foreign source is rejected'
$checkpointForeignManual = Invoke-CheckpointMutation { param($value) $value.manualEvidence.summarySha256='f'*64 }
Assert-True (-not $checkpointForeignManual.valid -and @($checkpointForeignManual.errors | Where-Object { $_ -eq 'release checkpoint manual evidence binding mismatch' }).Count -eq 1) 'checkpoint bound to foreign manual evidence is rejected'
$checkpointExtraGate = Invoke-CheckpointMutation { param($value) $extra=($value.completedGateResults[0]|ConvertTo-Json -Depth 50|ConvertFrom-Json);$extra.id='OV01-GATE-EXTRA';$extra.result.id='OV01-GATE-EXTRA';$extra.resultSha256=Get-Ov01ObjectSha256 -Value $extra.result;$value.completedGateResults+= $extra }
Assert-True (-not $checkpointExtraGate.valid -and @($checkpointExtraGate.errors | Where-Object { $_ -like 'release checkpoint completed gate order mismatch:*' }).Count -ge 1) 'checkpoint with extra completed gate is rejected'
$checkpointTamperedResult = Invoke-CheckpointMutation { param($value) $value.completedGateResults[0].result.durationMs = [int64]$value.completedGateResults[0].result.durationMs + 1 }
Assert-True (-not $checkpointTamperedResult.valid -and @($checkpointTamperedResult.errors | Where-Object { $_ -eq 'release checkpoint gate result digest mismatch: OV01-GATE-BE-LIFECYCLE' }).Count -eq 1) 'checkpoint gate result tampering without a matching digest is rejected'
$checkpointForgedDigest = Invoke-CheckpointMutation { param($value) $value.completedGateResults[0].resultSha256 = '0' * 64 }
Assert-True (-not $checkpointForgedDigest.valid -and @($checkpointForgedDigest.errors | Where-Object { $_ -eq 'release checkpoint gate result digest mismatch: OV01-GATE-BE-LIFECYCLE' }).Count -eq 1) 'checkpoint gate result with a forged digest is rejected'
$checkpointDuplicateCompleted = Invoke-CheckpointMutation { param($value) $value.completedGateResults += ($value.completedGateResults[0] | ConvertTo-Json -Depth 50 | ConvertFrom-Json) }
Assert-True (-not $checkpointDuplicateCompleted.valid -and @($checkpointDuplicateCompleted.errors | Where-Object { $_ -eq 'release checkpoint completed gate order mismatch: index=1' }).Count -eq 1) 'checkpoint duplicate completed gate IDs are rejected as a non-prefix'
$checkpointNonPrefix = Invoke-CheckpointMutation {
    param($value)
    $value.completedGateResults[0].id = 'OV01-GATE-BE-SAFETY'
    $value.completedGateResults[0].result.id = 'OV01-GATE-BE-SAFETY'
    $value.completedGateResults[0].result.logPath = 'logs/OV01-GATE-BE-SAFETY.log'
    $value.completedGateResults[0].resultSha256 = Get-Ov01ObjectSha256 -Value $value.completedGateResults[0].result
}
Assert-True (-not $checkpointNonPrefix.valid -and @($checkpointNonPrefix.errors | Where-Object { $_ -eq 'release checkpoint completed gate order mismatch: index=0' }).Count -eq 1) 'checkpoint completed gates must be the ordered catalog prefix'

$checkpointLogBackup = $checkpointLogPath + '.outside-fixture'
Move-Item -LiteralPath $checkpointLogPath -Destination $checkpointLogBackup
try {
    $checkpointMissingLog = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
    Assert-True (-not $checkpointMissingLog.valid -and @($checkpointMissingLog.errors | Where-Object { $_ -like 'release checkpoint gate log is missing:*' }).Count -eq 1) 'checkpoint with missing completed-gate log is rejected'
} finally { Move-Item -LiteralPath $checkpointLogBackup -Destination $checkpointLogPath }

$checkpointOriginalLogText = Get-Content -Encoding UTF8 -Raw -LiteralPath $checkpointLogPath
[System.IO.File]::AppendAllText($checkpointLogPath, "tampered`n", (New-Object System.Text.UTF8Encoding($false)))
try {
    $checkpointMutatedLog = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
    Assert-True (-not $checkpointMutatedLog.valid -and @($checkpointMutatedLog.errors | Where-Object { $_ -like 'release checkpoint gate log bytes mismatch:*' -or $_ -like 'release checkpoint gate log SHA-256 mismatch:*' }).Count -eq 2) 'checkpoint with mutated completed-gate log bytes and SHA-256 is rejected'
} finally { [System.IO.File]::WriteAllText($checkpointLogPath, $checkpointOriginalLogText, (New-Object System.Text.UTF8Encoding($false))) }

$checkpointBeforeSemanticForgery = Get-Content -Encoding UTF8 -Raw -LiteralPath $checkpointPath
$logBeforeSemanticForgery = Get-Content -Encoding UTF8 -Raw -LiteralPath $checkpointLogPath
try {
    $forgedLog = $logBeforeSemanticForgery.Replace('"tests":3', '"tests":4')
    [System.IO.File]::WriteAllText($checkpointLogPath, $forgedLog, (New-Object System.Text.UTF8Encoding($false)))
    $forgedCheckpoint = $checkpointBeforeSemanticForgery | ConvertFrom-Json
    $forgedCheckpoint.completedGateResults[0].result.logBytes = [int64](Get-Item -LiteralPath $checkpointLogPath).Length
    $forgedCheckpoint.completedGateResults[0].result.logSha256 = Get-Ov01Sha256 -Path $checkpointLogPath
    $forgedCheckpoint.completedGateResults[0].resultSha256 = Get-Ov01ObjectSha256 -Value $forgedCheckpoint.completedGateResults[0].result
    $forgedLogRecord = @($forgedCheckpoint.evidenceFiles | Where-Object { [string]$_.path -eq 'logs/OV01-GATE-BE-LIFECYCLE.log' })[0]
    $forgedLogRecord.bytes = [int64](Get-Item -LiteralPath $checkpointLogPath).Length
    $forgedLogRecord.sha256 = Get-Ov01Sha256 -Path $checkpointLogPath
    Write-Ov01AtomicJson -Path $checkpointPath -Value $forgedCheckpoint
    $semanticForgeryCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
    Assert-True (-not $semanticForgeryCheck.valid -and @($semanticForgeryCheck.errors | Where-Object { $_ -eq 'release checkpoint external authentication failed' }).Count -eq 1) 'coherent forgery recomputing every unkeyed log, evidence, and result digest is rejected without the external key'
    Write-Ov01AuthenticatedCheckpoint -Path $checkpointPath -Value $forgedCheckpoint -AuthenticationContext $checkpointAuthenticationContext
    $signedSemanticForgeryCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
    Assert-True (-not $signedSemanticForgeryCheck.valid -and @($signedSemanticForgeryCheck.errors | Where-Object { $_ -eq 'release checkpoint semantic replay totals mismatch: OV01-GATE-BE-LIFECYCLE' }).Count -eq 1) 'authorized checkpoint rewrite still cannot bypass retained-log semantic replay'
} finally {
    [System.IO.File]::WriteAllText($checkpointLogPath, $logBeforeSemanticForgery, (New-Object System.Text.UTF8Encoding($false)))
    [System.IO.File]::WriteAllText($checkpointPath, $checkpointBeforeSemanticForgery, (New-Object System.Text.UTF8Encoding($false)))
}

$checkpointArtifactRecord = @($checkpointObject.artifactRecords | Where-Object { [string]$_.path -like "$((ConvertTo-Ov01RelativePath -Root $repoRoot -Path $checkpointArtifactDirectory))/*" } | Select-Object -First 1)[0]
$checkpointArtifactFull = Resolve-Ov01RepoPath -Root $repoRoot -RelativePath ([string]$checkpointArtifactRecord.path)
$checkpointArtifactBackup = $checkpointArtifactFull + '.checkpoint-selftest-backup'
Move-Item -LiteralPath $checkpointArtifactFull -Destination $checkpointArtifactBackup
try {
    $checkpointMissingArtifact = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
    Assert-True (-not $checkpointMissingArtifact.valid -and @($checkpointMissingArtifact.errors | Where-Object { $_ -like 'release checkpoint artifact invalid:*' -or $_ -eq 'release checkpoint artifact records/filesystem mismatch' }).Count -ge 1) 'checkpoint with missing completed-gate artifact is rejected'
} finally { Move-Item -LiteralPath $checkpointArtifactBackup -Destination $checkpointArtifactFull }

$checkpointOriginalArtifactText = Get-Content -Encoding UTF8 -Raw -LiteralPath $checkpointArtifactFull
[System.IO.File]::AppendAllText($checkpointArtifactFull, 'tampered', (New-Object System.Text.UTF8Encoding($false)))
try {
    $checkpointMutatedArtifact = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
    Assert-True (-not $checkpointMutatedArtifact.valid -and @($checkpointMutatedArtifact.errors | Where-Object { $_ -eq 'release checkpoint artifact records/filesystem mismatch' }).Count -eq 1) 'checkpoint with mutated artifact bytes and SHA-256 is rejected'
} finally { [System.IO.File]::WriteAllText($checkpointArtifactFull, $checkpointOriginalArtifactText, (New-Object System.Text.UTF8Encoding($false))) }

$checkpointExtraFile = Join-Path $checkpointFixture 'stale-extra.txt'
[System.IO.File]::WriteAllText($checkpointExtraFile, 'stale', (New-Object System.Text.UTF8Encoding($false)))
$checkpointExtraFileCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
Assert-True (-not $checkpointExtraFileCheck.valid -and @($checkpointExtraFileCheck.errors | Where-Object { $_ -eq 'release checkpoint evidence file closed set mismatch' }).Count -eq 1) 'checkpoint resume rejects stale extra output file'
Move-Item -LiteralPath $checkpointExtraFile -Destination (Join-Path $tempRoot 'stale-extra-outside-fixture.txt')
$missingCheckpointCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath (Join-Path $checkpointFixture 'missing-checkpoint.json') -RepoRoot $repoRoot -EvidenceDirectory $checkpointFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $checkpointRegistry -CheckpointKeyPath $checkpointKeyPath
Assert-True (-not $missingCheckpointCheck.valid -and @($missingCheckpointCheck.errors | Where-Object { $_ -eq 'release checkpoint is missing' }).Count -eq 1) 'resume validation fails closed when the checkpoint is absent'

$activeRegistry = ($checkpointRegistry | ConvertTo-Json -Depth 40 | ConvertFrom-Json)
$activeFixture = Join-Path $tempRoot 'active-gate-interruption-fixture'
$activeLogs = Join-Path $activeFixture 'logs'
[void](New-Item -ItemType Directory -Path $activeLogs -Force)
[System.IO.File]::WriteAllText((Join-Path $activeFixture 'source-identity.json'), ($greenReport.sourceIdentity | ConvertTo-Json -Depth 30) + "`n", (New-Object System.Text.UTF8Encoding($false)))
$activeCompletedResults = New-Object System.Collections.Generic.List[object]
$activePrefixGateIds = @(Get-Ov01CanonicalReleaseGateIds | Select-Object -First 4)
foreach ($activePrefixGateId in $activePrefixGateIds) {
    $activeGateDefinition = @($activeRegistry.gates | Where-Object { [string]$_.id -eq $activePrefixGateId })[0]
    foreach ($optionalField in @('runnerTool', 'fallback', 'dynamicArguments', 'pytestIsolation', 'environment', 'evidenceEnvironmentKeys', 'approvedSkippedTests')) { $activeGateDefinition.PSObject.Properties.Remove($optionalField) }
    $activeSelectors = @($activeRegistry.scenarios | ForEach-Object { $scenario = $_; @($scenario.executableSelectors | Where-Object { [string]$_.gateId -eq $activePrefixGateId } | ForEach-Object { [pscustomobject]@{ scenarioId = [string]$scenario.id; selector = [string]$_.selector } }) })
    $selectorCommands = @($activeSelectors | ForEach-Object { "Write-Output '$(([string]$_.selector).Replace("'", "''"))'" })
    $selectorCommands += "Write-Output 'OV01_RESULT {`"tests`":1,`"failures`":0,`"errors`":0,`"skipped`":0}'"
    $activeGateDefinition.executable = 'powershell.exe'
    $activeGateDefinition.arguments = @('-NoProfile', '-Command', ($selectorCommands -join '; '))
    $activeGateDefinition.versionArguments = @('-NoProfile', '-Command', "Write-Output 'PowerShellSynthetic 1.0'")
    $activeGateDefinition.expectedVersionRegex = 'PowerShellSynthetic 1\.0'
    $activeGateDefinition.parser = 'ov01-json-line'
    $activeGateDefinition.expectedMinTests = 1
    $activeCompletedResults.Add((Invoke-Ov01Gate -Gate $activeGateDefinition -RepoRoot $repoRoot -LogDirectory $activeLogs -ExecutableSelectors $activeSelectors -SourceIdentity $greenReport.sourceIdentity))
}
$activeNextGateId = @(Get-Ov01CanonicalReleaseGateIds)[4]
$activeNextGateDefinition = @($activeRegistry.gates | Where-Object { [string]$_.id -eq $activeNextGateId })[0]
$activeState = New-Ov01ActiveGateState -Gate $activeNextGateDefinition -Registry $activeRegistry -Attempt 1
$activeArtifacts = Get-Ov01ArtifactRecords -Registry $activeRegistry -RepoRoot $repoRoot -SelectedGateIds $activePrefixGateIds -SourceIdentity $greenReport.sourceIdentity
$activeNow = [DateTimeOffset]::UtcNow.ToString('o')
$activeCheckpoint = [pscustomobject]@{
    schemaVersion = 1; runnerId = 'OV01-AUTO-002'; runMode = 'Release'
    outputDirectory = ConvertTo-Ov01RelativePath -Root $repoRoot -Path $activeFixture
    createdUtc = $activeNow; updatedUtc = $activeNow
    preRunSourceIdentity = $greenReport.sourceIdentity
    gateOrder = @(Get-Ov01CanonicalReleaseGateIds)
    manualEvidence = $manualCheck.record
    completedGateResults = @($activeCompletedResults | ForEach-Object { [pscustomobject]@{ id = [string]$_.id; resultSha256 = Get-Ov01ObjectSha256 -Value $_; result = $_ } })
    activeGate = $activeState
    artifactRecords = @($activeArtifacts.records)
    evidenceFiles = @(Get-Ov01EvidenceFileRecords -EvidenceDirectory $activeFixture -ExcludedRelativePaths @('release-checkpoint.json', [string]$activeState.logPath) -ExcludedRelativePrefixes @([string]$activeState.runtimeIsolationPrefix))
}
$activeCheckpointPath = Join-Path $activeFixture 'release-checkpoint.json'
Write-Ov01AuthenticatedCheckpoint -Path $activeCheckpointPath -Value $activeCheckpoint -AuthenticationContext $checkpointAuthenticationContext
$activeBeforeOutputCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $activeCheckpointPath -RepoRoot $repoRoot -EvidenceDirectory $activeFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $activeRegistry -CheckpointKeyPath $checkpointKeyPath
Assert-True $activeBeforeOutputCheck.valid 'interruption immediately after active-gate marker and before log/runtime creation is resumable'

$activeLogFull = Join-Path $activeFixture ([string]$activeState.logPath)
[System.IO.File]::WriteAllText($activeLogFull, 'partial active-gate log', (New-Object System.Text.UTF8Encoding($false)))
$activeRuntimePartial = Join-Path $activeFixture ([string]$activeState.runtimeIsolationPrefix + 'os-temp/partial.tmp')
[void](New-Item -ItemType Directory -Path ([System.IO.Path]::GetDirectoryName($activeRuntimePartial)) -Force)
[System.IO.File]::WriteAllText($activeRuntimePartial, 'partial isolated runtime', (New-Object System.Text.UTF8Encoding($false)))
$activeAfterOutputCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $activeCheckpointPath -RepoRoot $repoRoot -EvidenceDirectory $activeFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $activeRegistry -CheckpointKeyPath $checkpointKeyPath
Assert-True $activeAfterOutputCheck.valid 'interruption after canonical active-gate log and runtime creation is resumable'

$activeArbitraryExtra = Join-Path $activeFixture 'arbitrary-extra.txt'
[System.IO.File]::WriteAllText($activeArbitraryExtra, 'not active-gate evidence', (New-Object System.Text.UTF8Encoding($false)))
$activeExtraCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $activeCheckpointPath -RepoRoot $repoRoot -EvidenceDirectory $activeFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $activeRegistry -CheckpointKeyPath $checkpointKeyPath
Assert-True (-not $activeExtraCheck.valid -and @($activeExtraCheck.errors | Where-Object { $_ -eq 'release checkpoint evidence file closed set mismatch' }).Count -eq 1) 'active-gate marker never permits arbitrary extra evidence files'
Move-Item -LiteralPath $activeArbitraryExtra -Destination (Join-Path $tempRoot 'active-arbitrary-extra-outside-fixture.txt')

$activeCheckpoint.activeGate = New-Ov01ActiveGateState -Gate $activeNextGateDefinition -Registry $activeRegistry -Attempt 2
$activeCheckpoint.updatedUtc = [DateTimeOffset]::UtcNow.ToString('o')
$activeCheckpoint.evidenceFiles = @(Get-Ov01EvidenceFileRecords -EvidenceDirectory $activeFixture -ExcludedRelativePaths @('release-checkpoint.json', [string]$activeCheckpoint.activeGate.logPath) -ExcludedRelativePrefixes @([string]$activeCheckpoint.activeGate.runtimeIsolationPrefix))
Write-Ov01AuthenticatedCheckpoint -Path $activeCheckpointPath -Value $activeCheckpoint -AuthenticationContext $checkpointAuthenticationContext
$activeSecondRuntimePartial = Join-Path $activeFixture ([string]$activeCheckpoint.activeGate.runtimeIsolationPrefix + 'os-temp/partial.tmp')
[void](New-Item -ItemType Directory -Path ([System.IO.Path]::GetDirectoryName($activeSecondRuntimePartial)) -Force)
[System.IO.File]::WriteAllText($activeSecondRuntimePartial, 'second isolated attempt', (New-Object System.Text.UTF8Encoding($false)))
$activeSecondAttemptCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $activeCheckpointPath -RepoRoot $repoRoot -EvidenceDirectory $activeFixture -ExpectedSourceIdentity $greenReport.sourceIdentity -ExpectedManualEvidence ([pscustomobject]@{ record = $manualCheck.record }) -Registry $activeRegistry -CheckpointKeyPath $checkpointKeyPath
Assert-True $activeSecondAttemptCheck.valid 'resume advances to a new canonical runtime attempt while retaining prior interrupted evidence in the closed set'
$apkActiveState = New-Ov01ActiveGateState -Gate (@($activeRegistry.gates | Where-Object id -eq 'OV01-GATE-MOBILE-APK')[0]) -Registry $activeRegistry -Attempt 1
Assert-True (@($apkActiveState.buildArtifactIds).Count -eq 1 -and [string]$apkActiveState.buildArtifactIds[0] -eq 'OV01-ART-MOBILE-APK') 'active build gate marker binds only its canonical incomplete build artifact identity'

$diagnosticResumeOutput = Join-Path $tempRoot 'diagnostic-resume-rejected'
[void](New-Item -ItemType Directory -Path $diagnosticResumeOutput)
$diagnosticResumeArguments = @(
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $toolRoot 'Invoke-Ov01QualityGate.ps1'),
    '-ScenarioId', 'OV01-E2E-001', '-RegistryPath', $greenRegistryPath, '-RunMode', 'Diagnostic', '-Resume',
    '-BaselineHead', $baselineHead, '-TouchedPathsFile', $touchedPath, '-OutputDirectory', $diagnosticResumeOutput
)
$savedPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& powershell.exe @diagnosticResumeArguments 2>&1 | Out-Null
$diagnosticResumeExit = $LASTEXITCODE
$ErrorActionPreference = $savedPreference
Assert-True ($diagnosticResumeExit -ne 0 -and -not (Test-Path -LiteralPath (Join-Path $diagnosticResumeOutput 'release-checkpoint.json'))) 'Diagnostic mode cannot consume or create a Release checkpoint'
$alternateEvidenceDirectory = Join-Path $tempRoot 'alternate-evidence-root'
[void](New-Item -ItemType Directory -Path $alternateEvidenceDirectory)
$alternateEvidenceBindingCheck = Test-Ov01RunReport -Report $greenReport -Registry $greenRegistry -EvidenceDirectory $alternateEvidenceDirectory
Assert-True (-not $alternateEvidenceBindingCheck.valid -and @($alternateEvidenceBindingCheck.errors | Where-Object { $_ -eq 'report evidence directory binding mismatch' }).Count -eq 1) 'report cannot be rebound to a different evidence directory'

$logShaMutationReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$logShaMutationReport.gateResults[0].logSha256 = '0' * 64
$logShaMutationCheck = Test-Ov01RunReport -Report $logShaMutationReport -Registry $greenRegistry -EvidenceDirectory $greenOutput
Assert-True (-not $logShaMutationCheck.valid -and @($logShaMutationCheck.errors | Where-Object { $_ -like 'gate log SHA-256 mismatch:*' }).Count -eq 1) 'forged gate log SHA-256 is rejected'

$logBytesMutationReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$logBytesMutationReport.gateResults[0].logBytes = [int64]$logBytesMutationReport.gateResults[0].logBytes + 1
$logBytesMutationCheck = Test-Ov01RunReport -Report $logBytesMutationReport -Registry $greenRegistry -EvidenceDirectory $greenOutput
Assert-True (-not $logBytesMutationCheck.valid -and @($logBytesMutationCheck.errors | Where-Object { $_ -like 'gate log byte length mismatch:*' }).Count -eq 1) 'forged gate log byte length is rejected'

$escapingLogReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$escapingLogReport.gateResults[0].logPath = '../outside.log'
$escapingLogCheck = Test-Ov01RunReport -Report $escapingLogReport -Registry $greenRegistry -EvidenceDirectory $greenOutput
Assert-True (-not $escapingLogCheck.valid -and @($escapingLogCheck.errors | Where-Object { $_ -like 'gate log path is not canonical:*' }).Count -eq 1) 'gate log path traversal is rejected'

$greenLogFull = Resolve-Ov01RepoPath -Root $greenOutput -RelativePath ([string]$greenReport.gateResults[0].logPath)
$greenLogBackup = $greenLogFull + '.selftest-backup'
Move-Item -LiteralPath $greenLogFull -Destination $greenLogBackup
try {
    $missingLogCheck = Test-Ov01RunReport -Report $greenReport -Registry $greenRegistry -EvidenceDirectory $greenOutput
    Assert-True (-not $missingLogCheck.valid -and @($missingLogCheck.errors | Where-Object { $_ -like 'gate log file is missing:*' }).Count -eq 1) 'missing gate log file is rejected'
} finally { Move-Item -LiteralPath $greenLogBackup -Destination $greenLogFull }

$artifactShaMutationReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$artifactShaMutationReport.artifacts[0].sha256 = '0' * 64
$artifactShaMutationCheck = Test-Ov01RunReport -Report $artifactShaMutationReport -Registry $greenRegistry -EvidenceDirectory $greenOutput
Assert-True (-not $artifactShaMutationCheck.valid -and @($artifactShaMutationCheck.errors | Where-Object { $_ -like 'artifact SHA-256 mismatch:*' }).Count -eq 1) 'forged artifact SHA-256 is rejected'

$artifactBytesMutationReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$artifactBytesMutationReport.artifacts[0].bytes = [int64]$artifactBytesMutationReport.artifacts[0].bytes + 1
$artifactBytesMutationCheck = Test-Ov01RunReport -Report $artifactBytesMutationReport -Registry $greenRegistry -EvidenceDirectory $greenOutput
Assert-True (-not $artifactBytesMutationCheck.valid -and @($artifactBytesMutationCheck.errors | Where-Object { $_ -like 'artifact byte length mismatch:*' }).Count -eq 1) 'forged artifact byte length is rejected'

$greenArtifactFull = Resolve-Ov01RepoPath -Root $repoRoot -RelativePath ([string]$greenReport.artifacts[0].path)
$greenArtifactBackup = $greenArtifactFull + '.selftest-backup'
Move-Item -LiteralPath $greenArtifactFull -Destination $greenArtifactBackup
try {
    $missingArtifactFileCheck = Test-Ov01RunReport -Report $greenReport -Registry $greenRegistry -EvidenceDirectory $greenOutput
    Assert-True (-not $missingArtifactFileCheck.valid -and @($missingArtifactFileCheck.errors | Where-Object { $_ -like 'artifact file is missing:*' }).Count -eq 1) 'missing artifact file is rejected'
} finally { Move-Item -LiteralPath $greenArtifactBackup -Destination $greenArtifactFull }

$sourceDriftMarker = Join-Path $tempRoot 'source-drift-marker.txt'
[System.IO.File]::WriteAllText($sourceDriftMarker, 'before', (New-Object System.Text.UTF8Encoding($false)))
$sourceDriftCommand = "[System.IO.File]::WriteAllText('$($sourceDriftMarker.Replace("'", "''"))', 'after')"
$sourceDriftRegistryPath = New-SyntheticRegistryFile -Tests 3 -Failures 0 -Errors 0 -Skipped 0 -GateCommandPrefix $sourceDriftCommand
$sourceDriftTouchedPath = Join-Path $tempRoot 'source-drift-touched.txt'
[System.IO.File]::WriteAllText($sourceDriftTouchedPath, "06_Testing/Automation/ov-01/ov01-scenario-registry.json`n$($sourceDriftMarker.Substring($repoRoot.Length + 1).Replace('\', '/'))`n", (New-Object System.Text.UTF8Encoding($false)))
$sourceDriftOutput = Join-Path $tempRoot 'source-drift-run'
$sourceDriftExit = Invoke-SyntheticRun -SyntheticRegistry $sourceDriftRegistryPath -OutputDirectory $sourceDriftOutput -TouchedPathFile $sourceDriftTouchedPath
$sourceDriftReport = Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $sourceDriftOutput 'run-report.json') | ConvertFrom-Json
Assert-True ($sourceDriftExit -ne 0 -and $sourceDriftReport.diagnosticStatus -eq 'FAIL' -and @($sourceDriftReport.sourceIdentityDriftErrors | Where-Object { $_ -eq 'source identity drift: compositeSha256' }).Count -eq 1 -and @($sourceDriftReport.sourceIdentityDriftErrors | Where-Object { $_ -eq 'source identity drift: exact files/state/hashes' }).Count -eq 1) 'runner rejects source mutation performed during gate execution'

$releaseOutput = Join-Path $tempRoot 'noncanonical-release'
$releaseExit = Invoke-SyntheticRun -SyntheticRegistry $greenRegistryPath -OutputDirectory $releaseOutput -RunMode Release
Assert-True ($releaseExit -ne 0 -and -not (Test-Path -LiteralPath (Join-Path $releaseOutput 'run-report.json'))) 'noncanonical registry is rejected before a release report can be emitted'

$canonicalSubsetReleaseOutput = Join-Path $tempRoot 'canonical-subset-release'
$canonicalSubsetArguments = @(
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $toolRoot 'Invoke-Ov01QualityGate.ps1'),
    '-ScenarioId', 'OV01-E2E-001', '-RegistryPath', $registryPath, '-RunMode', 'Release',
    '-BaselineHead', $baselineHead, '-TouchedPathsFile', $touchedPath, '-OutputDirectory', $canonicalSubsetReleaseOutput
)
$savedPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& powershell.exe @canonicalSubsetArguments 2>&1 | Out-Null
$canonicalSubsetReleaseExit = $LASTEXITCODE
$ErrorActionPreference = $savedPreference
Assert-True ($canonicalSubsetReleaseExit -ne 0 -and -not (Test-Path -LiteralPath (Join-Path $canonicalSubsetReleaseOutput 'run-report.json'))) 'canonical one-scenario Release invocation is rejected before any platform gate executes'

$identityRegistryPath = Join-Path $tempRoot 'identity-registry.json'
Copy-Item -LiteralPath $greenRegistryPath -Destination $identityRegistryPath
$identityBefore = Get-Ov01SourceIdentity -RepoRoot $repoRoot -BaselineHead $baselineHead -TouchedPathsFile $touchedPath -RegistryPath $identityRegistryPath
[System.IO.File]::AppendAllText($identityRegistryPath, " `n")
$identityAfter = Get-Ov01SourceIdentity -RepoRoot $repoRoot -BaselineHead $baselineHead -TouchedPathsFile $touchedPath -RegistryPath $identityRegistryPath
Assert-True ($identityBefore.registrySha256 -ne $identityAfter.registrySha256 -and $identityBefore.compositeSha256 -ne $identityAfter.compositeSha256) 'registry mutation changes registry and composite source identities'
$stableIdentityCheck = Test-Ov01SourceIdentityStable -Before $identityBefore -After $identityBefore
Assert-True $stableIdentityCheck.valid 'identical pre/post source identities are stable'
$driftedIdentity = ($identityBefore | ConvertTo-Json -Depth 20 | ConvertFrom-Json)
$driftedIdentity.files[0].sha256 = 'f' * 64
$driftedIdentity.compositeSha256 = 'e' * 64
$driftedIdentityCheck = Test-Ov01SourceIdentityStable -Before $identityBefore -After $driftedIdentity
Assert-True (-not $driftedIdentityCheck.valid -and @($driftedIdentityCheck.errors | Where-Object { $_ -eq 'source identity drift: compositeSha256' }).Count -eq 1 -and @($driftedIdentityCheck.errors | Where-Object { $_ -eq 'source identity drift: exact files/state/hashes' }).Count -eq 1) 'post-run source hash/file drift is rejected'

$missingRegistryBindingReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$missingRegistryBindingReport.sourceIdentity.registrySha256 = ''
$missingRegistryBindingCheck = Test-Ov01RunReport -Report $missingRegistryBindingReport -Registry $greenRegistry
Assert-True (-not $missingRegistryBindingCheck.valid -and @($missingRegistryBindingCheck.errors | Where-Object { $_ -eq 'missing source registry SHA-256' }).Count -eq 1) 'missing registry SHA metadata is rejected'

$subsetReleaseReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$subsetReleaseReport.runMode = 'Release'
$subsetReleaseReport.releaseEligible = $true
$subsetReleaseReport.sourceIdentity.registryCanonical = $true
$subsetReleaseReport.overallStatus = 'PASS'
$subsetReleaseReport.diagnosticStatus = $null
$subsetReleaseCheck = Test-Ov01RunReport -Report $subsetReleaseReport -Registry $greenRegistry
Assert-True (-not $subsetReleaseCheck.valid -and @($subsetReleaseCheck.errors | Where-Object { $_ -eq 'release scenario selection must be the exact canonical all-scenario set' }).Count -eq 1) 'one-scenario Release report is rejected even when every selected contract passes'

$missingReleaseGatesReport = ($subsetReleaseReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$missingReleaseGatesReport.selection.scenarioIds = @($greenRegistry.requiredScenarioIds)
$missingReleaseGatesCheck = Test-Ov01RunReport -Report $missingReleaseGatesReport -Registry $greenRegistry
Assert-True (-not $missingReleaseGatesCheck.valid -and @($missingReleaseGatesCheck.errors | Where-Object { $_ -eq 'release gate selection must be the exact canonical release-gate set' }).Count -eq 1) 'Release report missing required platform/focused gates is rejected'

$missingRunnerEnvironmentReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$missingRunnerEnvironmentReport.PSObject.Properties.Remove('runnerEnvironment')
$missingRunnerEnvironmentCheck = Test-Ov01RunReport -Report $missingRunnerEnvironmentReport -Registry $greenRegistry
Assert-True (-not $missingRunnerEnvironmentCheck.valid -and @($missingRunnerEnvironmentCheck.errors | Where-Object { $_ -eq 'missing runner environment identity' }).Count -eq 1) 'report without runner environment identity is rejected'

$missingToolchainReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$missingToolchainReport.gateResults[0].PSObject.Properties.Remove('toolchain')
$missingToolchainCheck = Test-Ov01RunReport -Report $missingToolchainReport -Registry $greenRegistry
Assert-True (-not $missingToolchainCheck.valid -and @($missingToolchainCheck.errors | Where-Object { $_ -like 'missing gate toolchain identity:*' }).Count -eq 1) 'green gate without toolchain identity is rejected'

$relativeToolchainReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$relativeToolchainReport.gateResults[0].toolchain.resolvedExecutable = 'powershell.exe'
$relativeToolchainCheck = Test-Ov01RunReport -Report $relativeToolchainReport -Registry $greenRegistry
Assert-True (-not $relativeToolchainCheck.valid -and @($relativeToolchainCheck.errors | Where-Object { $_ -like 'gate toolchain executable is not an absolute path:*' }).Count -eq 1) 'green gate with non-absolute toolchain executable is rejected'

$wrongToolVersionReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$wrongToolVersionReport.gateResults[0].toolchain.versionOutput = 'PowerShellSynthetic 0.9'
$wrongToolVersionCheck = Test-Ov01RunReport -Report $wrongToolVersionReport -Registry $greenRegistry
Assert-True (-not $wrongToolVersionCheck.valid -and @($wrongToolVersionCheck.errors | Where-Object { $_ -like 'gate toolchain version output mismatch:*' }).Count -eq 1) 'green gate with mismatched retained tool version is rejected'

$officialFlutterReportPath = Get-ChildItem -LiteralPath (Join-Path $repoRoot '_bmad-output/test-artifacts/story-6-10') -Directory |
    Where-Object { $_.Name -like 'official-narrow-direct-dart*' -and (Test-Path -LiteralPath (Join-Path $_.FullName 'run-report.json') -PathType Leaf) } |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1 |
    ForEach-Object { Join-Path $_.FullName 'run-report.json' }
if ([string]::IsNullOrWhiteSpace([string]$officialFlutterReportPath)) {
    Assert-True $false 'real Flutter diagnostic report rejects retained version-output mutation by exact replay'
} else {
    $officialFlutterReport = Get-Content -Encoding UTF8 -Raw -LiteralPath $officialFlutterReportPath | ConvertFrom-Json
    $officialFlutterGateResults = @($officialFlutterReport.gateResults | Where-Object { $null -ne $_.toolchain.PSObject.Properties['runnerTool'] -and [string]$_.toolchain.runnerTool -eq 'flutter-dart-snapshot' })
    $officialFlutterEvidenceDirectory = Split-Path -Parent $officialFlutterReportPath
    $officialFlutterUnmodifiedCheck = Test-Ov01RunReport -Report $officialFlutterReport -Registry $registry -EvidenceDirectory $officialFlutterEvidenceDirectory
    Assert-True (
        -not $officialFlutterUnmodifiedCheck.valid -and
        @($officialFlutterUnmodifiedCheck.errors | Where-Object { $_ -like 'gate log byte length mismatch:*' }).Count -ge 1
    ) 'legacy Flutter report without retained log byte lengths is rejected by the strengthened replay contract'
    $officialFlutterMutationReport = ($officialFlutterReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
    $officialFlutterMutationResult = @($officialFlutterMutationReport.gateResults | Where-Object { $null -ne $_.toolchain.PSObject.Properties['runnerTool'] -and [string]$_.toolchain.runnerTool -eq 'flutter-dart-snapshot' })[0]
    $officialFlutterMutationResult.toolchain.versionOutput = 'Flutter 0.0.0'
    $officialFlutterMutationCheck = Test-Ov01RunReport -Report $officialFlutterMutationReport -Registry $registry -EvidenceDirectory $officialFlutterEvidenceDirectory
    $officialFlutterVersionOutputSha256 = if ($officialFlutterGateResults.Count -eq 1 -and $null -ne $officialFlutterGateResults[0].toolchain.PSObject.Properties['versionOutputSha256']) { [string]$officialFlutterGateResults[0].toolchain.versionOutputSha256 } else { '' }
    Assert-True (
        $officialFlutterGateResults.Count -eq 1 -and
        $officialFlutterVersionOutputSha256 -match '^[0-9a-f]{64}$' -and
        -not $officialFlutterMutationCheck.valid -and
        @($officialFlutterMutationCheck.errors | Where-Object { $_ -eq "gate toolchain version output SHA mismatch: $($officialFlutterMutationResult.id)" }).Count -eq 1 -and
        @($officialFlutterMutationCheck.errors | Where-Object { $_ -eq "Flutter version output replay mismatch: $($officialFlutterMutationResult.id)" }).Count -eq 1
    ) 'real Flutter diagnostic report rejects retained version-output mutation by exact replay'
}

$missingEnvironmentEvidenceReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$missingEnvironmentEvidenceReport.gateResults[0].PSObject.Properties.Remove('environmentEvidence')
$missingEnvironmentEvidenceCheck = Test-Ov01RunReport -Report $missingEnvironmentEvidenceReport -Registry $greenRegistry
Assert-True (-not $missingEnvironmentEvidenceCheck.valid -and @($missingEnvironmentEvidenceCheck.errors | Where-Object { $_ -like 'missing gate environment evidence:*' }).Count -eq 1) 'green gate without explicit environment evidence is rejected'

$wrongEnvironmentEvidenceReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$wrongEnvironmentEvidenceReport.gateResults[0].environmentEvidence | Add-Member -Force -NotePropertyName PYTHONPATH -NotePropertyValue 'wrong'
$wrongEnvironmentEvidenceCheck = Test-Ov01RunReport -Report $wrongEnvironmentEvidenceReport -Registry $greenRegistry
Assert-True (-not $wrongEnvironmentEvidenceCheck.valid -and @($wrongEnvironmentEvidenceCheck.errors | Where-Object { $_ -like 'gate environment evidence keys mismatch:*' }).Count -eq 1) 'green gate with unexpected environment evidence is rejected'

$duplicateResultReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$duplicateResultReport.gateResults += $duplicateResultReport.gateResults[0]
$duplicateResultCheck = Test-Ov01RunReport -Report $duplicateResultReport -Registry $greenRegistry
Assert-True (-not $duplicateResultCheck.valid -and @($duplicateResultCheck.errors | Where-Object { $_ -like 'duplicate gate result:*' -or $_ -eq 'selected gate identities do not exactly match gate results' }).Count -ge 2) 'duplicate and cardinality-mismatched gate results are rejected'

$unknownResultReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$unknownResultReport.gateResults[0].id = 'OV01-GATE-UNKNOWN'
$unknownResultCheck = Test-Ov01RunReport -Report $unknownResultReport -Registry $greenRegistry
Assert-True (-not $unknownResultCheck.valid -and @($unknownResultCheck.errors | Where-Object { $_ -like 'unknown gate result:*' }).Count -eq 1) 'unknown gate result identity is rejected'

$unknownScenarioResultReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$unknownScenarioResultReport.scenarioResults[0].id = 'OV01-E2E-999'
$unknownScenarioResultCheck = Test-Ov01RunReport -Report $unknownScenarioResultReport -Registry $greenRegistry
Assert-True (-not $unknownScenarioResultCheck.valid -and @($unknownScenarioResultCheck.errors | Where-Object { $_ -like 'unknown scenario result:*' }).Count -eq 1) 'unknown scenario result identity is rejected'

$missingSelectorReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$missingSelectorReport.gateResults[0].selectorResults = @()
$missingSelectorCheck = Test-Ov01RunReport -Report $missingSelectorReport -Registry $greenRegistry
Assert-True (-not $missingSelectorCheck.valid -and @($missingSelectorCheck.errors | Where-Object { $_ -like 'scenario executable selector is not proven exactly once:*' }).Count -eq 1) 'scenario PASS without parsed executable selector evidence is rejected'

$duplicateArtifactReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$duplicateArtifactReport.artifacts += $duplicateArtifactReport.artifacts[0]
$duplicateArtifactCheck = Test-Ov01RunReport -Report $duplicateArtifactReport -Registry $greenRegistry
Assert-True (-not $duplicateArtifactCheck.valid -and @($duplicateArtifactCheck.errors | Where-Object { $_ -like 'duplicate artifact identity/path:*' }).Count -eq 1) 'duplicate artifact identity/path is rejected'

$unknownArtifactReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$unknownArtifactReport.artifacts[0].id = 'OV01-ART-UNKNOWN'
$unknownArtifactCheck = Test-Ov01RunReport -Report $unknownArtifactReport -Registry $greenRegistry
Assert-True (-not $unknownArtifactCheck.valid -and @($unknownArtifactCheck.errors | Where-Object { $_ -like 'unexpected artifact identity in report:*' }).Count -eq 1) 'unknown artifact identity is rejected'

$wrongArtifactPathReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$wrongArtifactPathReport.artifacts[0].path = '06_Testing/Automation/ov-01/replaced-registry.json'
$wrongArtifactPathCheck = Test-Ov01RunReport -Report $wrongArtifactPathReport -Registry $greenRegistry
Assert-True (-not $wrongArtifactPathCheck.valid -and @($wrongArtifactPathCheck.errors | Where-Object { $_ -like 'registry artifact path does not match actual registry identity:*' }).Count -eq 1) 'artifact path replacement is rejected'

$formatSourceIdentity = [pscustomobject]@{ files = @() }
$formatResult = Invoke-Ov01Gate -Gate $mobileFormatGate[0] -RepoRoot $repoRoot -LogDirectory $tempRoot -SourceIdentity $formatSourceIdentity
Assert-True ($formatResult.status -eq 'PASS' -and $formatResult.notApplicable -and $formatResult.notApplicableReason -eq 'NO_TOUCHED_DART_FILES' -and @($formatResult.dynamicInputs).Count -eq 0) 'touched-Dart format gate records explicit safe not-applicable evidence when no Dart file changed'

$formatOrderCmd = New-SyntheticCmd -Name 'format-order.cmd' -Lines @('@echo off', 'echo OV01_RESULT {"tests":1,"failures":0,"errors":0,"skipped":0}')
$formatOrderWorkingDirectory = Split-Path -Parent $formatOrderCmd
$formatOrderPrefix = (ConvertTo-Ov01RelativePath -Root $repoRoot -Path $formatOrderWorkingDirectory) + '/'
[System.IO.File]::WriteAllText((Join-Path $formatOrderWorkingDirectory 'b.dart'), 'void b() {}')
[System.IO.File]::WriteAllText((Join-Path $formatOrderWorkingDirectory 'a.dart'), 'void a() {}')
$formatOrderGate = New-SyntheticGate -Id 'SELF-FORMAT-ORDER' -Executable 'format-order.cmd'
$formatOrderGate | Add-Member -NotePropertyName dynamicArguments -NotePropertyValue ([pscustomobject]@{
    sourceIdentityPathRegex = '^' + [regex]::Escape($formatOrderPrefix) + '.*\.dart$'
    stripPrefix = $formatOrderPrefix
    allowEmpty = $false
})
$formatOrderSource = [pscustomobject]@{ files = @(
    [pscustomobject]@{ path = $formatOrderPrefix + 'b.dart'; state = 'present' },
    [pscustomobject]@{ path = $formatOrderPrefix + 'a.dart'; state = 'present' }
) }
$formatOrderResult = Invoke-Ov01Gate -Gate $formatOrderGate -RepoRoot $repoRoot -LogDirectory $tempRoot -SourceIdentity $formatOrderSource
Assert-True ($formatOrderResult.status -eq 'PASS' -and ($formatOrderResult.dynamicInputs -join ',') -eq 'a.dart,b.dart') 'dynamic touched-file inputs use deterministic ordinal ordering'

$falseGreenReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$falseGreenReport.gateResults[0].exitCode = 1
$falseGreenCheck = Test-Ov01RunReport -Report $falseGreenReport -Registry $greenRegistry
Assert-True (-not $falseGreenCheck.valid -and @($falseGreenCheck.errors | Where-Object { $_ -like 'false-green gate:*' }).Count -gt 0) 'false-green gate status is rejected'

$timeoutFalseGreenReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$timeoutFalseGreenReport.gateResults[0].timedOut = $true
$timeoutFalseGreenCheck = Test-Ov01RunReport -Report $timeoutFalseGreenReport -Registry $greenRegistry
Assert-True (-not $timeoutFalseGreenCheck.valid -and @($timeoutFalseGreenCheck.errors | Where-Object { $_ -like 'false-green gate:*' }).Count -gt 0) 'timed-out gate cannot be changed to PASS in a report'

$bindingReport = ($greenReport | ConvertTo-Json -Depth 30 | ConvertFrom-Json)
$bindingReport.artifacts[0].sourceIdentity = 'wrong-source'
$bindingCheck = Test-Ov01RunReport -Report $bindingReport -Registry $greenRegistry
Assert-True (-not $bindingCheck.valid -and @($bindingCheck.errors | Where-Object { $_ -like 'artifact build binding mismatch:*' }).Count -gt 0) 'artifact build-binding mismatch is rejected'

$skipRegistryPath = New-SyntheticRegistryFile -Tests 3 -Failures 0 -Errors 0 -Skipped 1
$skipOutput = Join-Path $tempRoot 'skip'
$skipExit = Invoke-SyntheticRun -SyntheticRegistry $skipRegistryPath -OutputDirectory $skipOutput
$skipReport = Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $skipOutput 'run-report.json') | ConvertFrom-Json
Assert-True ($skipExit -ne 0 -and $skipReport.gateResults[0].status -eq 'FAIL') 'skipped outcome fails the runner'

$pendingRegistryPath = New-SyntheticRegistryFile -Tests 3 -Failures 0 -Errors 0 -Skipped 0 -Suffix ' outcome=PENDING'
$pendingOutput = Join-Path $tempRoot 'pending'
$pendingExit = Invoke-SyntheticRun -SyntheticRegistry $pendingRegistryPath -OutputDirectory $pendingOutput
$pendingReport = Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $pendingOutput 'run-report.json') | ConvertFrom-Json
Assert-True ($pendingExit -ne 0 -and $pendingReport.gateResults[0].forbiddenOutcomeDetected) 'pending/fixme/disabled marker fails the runner'

$zeroRegistryPath = New-SyntheticRegistryFile -Tests 0 -Failures 0 -Errors 0 -Skipped 0
$zeroOutput = Join-Path $tempRoot 'zero'
$zeroExit = Invoke-SyntheticRun -SyntheticRegistry $zeroRegistryPath -OutputDirectory $zeroOutput
Assert-True ($zeroExit -ne 0) 'zero-test false green is rejected'

$emptyOutput = Join-Path $tempRoot 'empty-selection'
$emptyArguments = @(
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $toolRoot 'Invoke-Ov01QualityGate.ps1'),
    '-ScenarioId', '', '-RegistryPath', $greenRegistryPath, '-BaselineHead', $baselineHead,
    '-TouchedPathsFile', $touchedPath, '-OutputDirectory', $emptyOutput
)
$savedPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& powershell.exe @emptyArguments 2>&1 | Out-Null
$emptyExit = $LASTEXITCODE
$ErrorActionPreference = $savedPreference
Assert-True ($emptyExit -ne 0) 'empty scenario selection is rejected'

function Copy-EvidenceDirectory {
    param([string]$Name)
    $destination = Join-Path $tempRoot $Name
    [void](New-Item -ItemType Directory -Path $destination)
    Copy-Item -Path (Join-Path $greenOutput '*') -Destination $destination -Recurse
    return $destination
}

$mutationCopy = Copy-EvidenceDirectory -Name 'mutation'
[System.IO.File]::AppendAllText((Join-Path $mutationCopy 'validation.json'), " `n")
$mutationCheck = Test-Ov01ClosedSetManifest -EvidenceDirectory $mutationCopy
Assert-True (-not $mutationCheck.valid -and @($mutationCheck.errors | Where-Object { $_ -like 'closed-set mutation detected:*' }).Count -gt 0) 'closed-set mutation is detected'

$additionCopy = Copy-EvidenceDirectory -Name 'addition'
[System.IO.File]::WriteAllText((Join-Path $additionCopy 'unexpected.txt'), 'unexpected')
$additionCheck = Test-Ov01ClosedSetManifest -EvidenceDirectory $additionCopy
Assert-True (-not $additionCheck.valid -and @($additionCheck.errors | Where-Object { $_ -like 'closed-set addition detected:*' }).Count -gt 0) 'closed-set addition is detected'

$removalCopy = Copy-EvidenceDirectory -Name 'removal'
Remove-Item -LiteralPath (Join-Path $removalCopy 'validation.json')
$removalCheck = Test-Ov01ClosedSetManifest -EvidenceDirectory $removalCopy
Assert-True (-not $removalCheck.valid -and @($removalCheck.errors | Where-Object { $_ -like 'closed-set removal detected:*' }).Count -gt 0) 'closed-set removal is detected'

Write-Output "OV01 runner self-test: passed=$passed failed=$failed temp=$tempRoot"
if ($failed -gt 0) { exit 1 }
exit 0
