[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$runnerPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\Invoke-ChecklistDistributionE2E.ps1'))
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..\..'))
$testRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("carebridge-checklist-runner-test-{0}" -f [Guid]::NewGuid().ToString('N'))
$secretAccessToken = 'access-token-MUST-NOT-LEAK-97f1'
$secretRefreshToken = 'refresh-token-MUST-NOT-LEAK-47a2'
$secretUserId = 'user-id-MUST-NOT-LEAK-8d31'
$script:passed = 0
$script:failed = 0

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) {
        throw "ASSERT_TRUE_FAILED:$Message"
    }
}

function Assert-Equal($Expected, $Actual, [string]$Message) {
    if ($Expected -ne $Actual) {
        throw "ASSERT_EQUAL_FAILED:$Message expected=[$Expected] actual=[$Actual]"
    }
}

function Write-Utf8NoBom([string]$Path, [string]$Value) {
    $parent = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        [System.IO.Directory]::CreateDirectory($parent) | Out-Null
    }
    [System.IO.File]::WriteAllText($Path, $Value, (New-Object System.Text.UTF8Encoding($false)))
}

function New-ValidDefines([string]$Path, [string]$Environment = 'DISPOSABLE_NON_PRODUCTION') {
    $values = [ordered]@{
        API_BASE_URL = 'http://10.0.2.2:8080'
        CHK_API_E2E = 'true'
        CHK_E2E_ENVIRONMENT = $Environment
        CHK_E2E_SERVER_ENVIRONMENT_ID = 'e2e-runner-fixture'
        CHK_E2E_EXPECTED_API_BASE_URL = 'http://10.0.2.2:8080'
        CHK_E2E_DEVICE_ACK = 'DEDICATED_DEVICE_CONFIRMED'
        CHK_E2E_CREDENTIAL_ARTIFACT_ACK = 'COMPILED_CREDENTIAL_ARTIFACT_ACCEPTED'
        CHK_E2E_ALLOW_LOOPBACK_HTTP = 'LOOPBACK_ONLY_CONFIRMED'
    }
    foreach ($actor in @('CONTENT_ADMIN', 'ADMIN', 'MOTHER', 'FAMILY', 'ISOLATION_FAMILY')) {
        $values["CHK_${actor}_ACCESS_TOKEN"] = "$secretAccessToken-$actor"
        $values["CHK_${actor}_REFRESH_TOKEN"] = "$secretRefreshToken-$actor"
        $values["CHK_${actor}_USER_ID"] = "$secretUserId-$actor"
    }
    Write-Utf8NoBom $Path ($values | ConvertTo-Json -Depth 4)
}

function Invoke-RunnerCase(
    [string]$Name,
    [string]$Scenario,
    [string]$DefinesPath,
    [string]$DeviceId = 'emulator-5554',
    [string]$AcknowledgedApiOrigin = 'http://10.0.2.2:8080'
) {
    $caseRoot = Join-Path $testRoot $Name
    $evidenceRoot = Join-Path $caseRoot 'evidence'
    $argumentLog = Join-Path $caseRoot 'fake-flutter-arguments.jsonl'
    [System.IO.Directory]::CreateDirectory($caseRoot) | Out-Null
    $previousScenario = $env:FAKE_FLUTTER_SCENARIO
    $previousArgumentLog = $env:FAKE_FLUTTER_ARGUMENT_LOG
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $env:FAKE_FLUTTER_SCENARIO = $Scenario
        $env:FAKE_FLUTTER_ARGUMENT_LOG = $argumentLog
        $ErrorActionPreference = 'Continue'
        $output = @(& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $runnerPath `
                -FlutterExecutable $script:fakeFlutterPath `
                -DeviceId $DeviceId `
                -DefinesFile $DefinesPath `
                -AcknowledgedApiOrigin $AcknowledgedApiOrigin `
                -EvidenceDirectory $evidenceRoot 2>&1 | ForEach-Object { "$_" })
        $exitCode = $LASTEXITCODE
        return [pscustomobject]@{
            Name = $Name
            ExitCode = $exitCode
            Output = ($output -join [Environment]::NewLine)
            EvidenceRoot = $evidenceRoot
            ArgumentLog = $argumentLog
        }
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
        if ($null -eq $previousScenario) { Remove-Item Env:FAKE_FLUTTER_SCENARIO -ErrorAction SilentlyContinue }
        else { $env:FAKE_FLUTTER_SCENARIO = $previousScenario }
        if ($null -eq $previousArgumentLog) { Remove-Item Env:FAKE_FLUTTER_ARGUMENT_LOG -ErrorAction SilentlyContinue }
        else { $env:FAKE_FLUTTER_ARGUMENT_LOG = $previousArgumentLog }
    }
}

function Assert-NoSecrets([string]$Text, [string]$Label) {
    Assert-True (-not $Text.Contains($secretAccessToken)) "$Label contains an access token"
    Assert-True (-not $Text.Contains($secretRefreshToken)) "$Label contains a refresh token"
    Assert-True (-not $Text.Contains($secretUserId)) "$Label contains a user id"
}

function Invoke-Test([string]$Name, [scriptblock]$Body) {
    try {
        & $Body
        $script:passed++
        Write-Output "PASS:$Name"
    }
    catch {
        $script:failed++
        [Console]::Error.WriteLine("FAIL:${Name}:$($_.Exception.Message)")
    }
}

try {
    [System.IO.Directory]::CreateDirectory($testRoot) | Out-Null
    $fakeFlutterDriver = Join-Path $testRoot 'fake-flutter.ps1'
    $script:fakeFlutterPath = Join-Path $testRoot 'flutter-fake.cmd'
    Write-Utf8NoBom $script:fakeFlutterPath '@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0fake-flutter.ps1" %*
exit /b %ERRORLEVEL%
'
    Write-Utf8NoBom $fakeFlutterDriver @'
$ErrorActionPreference = 'Stop'
$allArguments = @($args | ForEach-Object { "$_" })
if (-not [string]::IsNullOrWhiteSpace($env:FAKE_FLUTTER_ARGUMENT_LOG)) {
    [System.IO.File]::AppendAllText(
        $env:FAKE_FLUTTER_ARGUMENT_LOG,
        (($allArguments | ConvertTo-Json -Compress) + [Environment]::NewLine),
        (New-Object System.Text.UTF8Encoding($false)))
}
if ($allArguments.Count -gt 0 -and $allArguments[0] -eq 'devices') {
    if ($env:FAKE_FLUTTER_SCENARIO -eq 'desktop-device') {
        @([ordered]@{ name='Windows'; id='windows'; isSupported=$true; targetPlatform='windows-x64'; emulator=$false }) |
            ConvertTo-Json -Compress | Write-Output
    }
    elseif ($env:FAKE_FLUTTER_SCENARIO -eq 'multi-device') {
        @(
            [ordered]@{ name='Dedicated Android'; id='emulator-5554'; isSupported=$true; targetPlatform='android-x64'; emulator=$true }
            [ordered]@{ name='Windows'; id='windows'; isSupported=$true; targetPlatform='windows-x64'; emulator=$false }
        ) | ConvertTo-Json -Compress | Write-Output
    }
    else {
        @([ordered]@{ name='Dedicated Android'; id='emulator-5554'; isSupported=$true; targetPlatform='android-x64'; emulator=$true }) |
            ConvertTo-Json -Compress | Write-Output
    }
    exit 0
}

$definesArgument = @($allArguments | Where-Object { $_ -like '--dart-define-from-file=*' }) | Select-Object -First 1
$definesPath = if ($null -eq $definesArgument) { '' } else { $definesArgument.Substring('--dart-define-from-file='.Length) }
$defines = if ([string]::IsNullOrWhiteSpace($definesPath)) { $null } else { Get-Content -Raw -LiteralPath $definesPath | ConvertFrom-Json }
$accessToken = if ($null -eq $defines) { 'missing-token' } else { "$($defines.CHK_MOTHER_ACCESS_TOKEN)" }
$refreshToken = if ($null -eq $defines) { 'missing-refresh' } else { "$($defines.CHK_MOTHER_REFRESH_TOKEN)" }
$userId = if ($null -eq $defines) { 'missing-user' } else { "$($defines.CHK_MOTHER_USER_ID)" }
Write-Output "fixture values: $accessToken / $refreshToken / $userId"
[Console]::Error.WriteLine("fixture stderr: $accessToken / $refreshToken / $userId")
if ($env:FAKE_FLUTTER_SCENARIO -eq 'base64-secret-output') {
    $encodedDefine = [Convert]::ToBase64String(
        [Text.Encoding]::UTF8.GetBytes("CHK_MOTHER_ACCESS_TOKEN=$accessToken"))
    Write-Output "gradle -Pdart-defines=$encodedDefine"
}

$testName = switch ($env:FAKE_FLUTTER_SCENARIO) {
    'missing-target' { 'A different integration test' }
    'prefix-collision' { 'CHK-042/043 a different integration test' }
    default { 'CHK-042/043 author, approve, distribute and complete isolated Mother/Family tasks via live API' }
}
$testFile = if ($env:FAKE_FLUTTER_SCENARIO -eq 'wrong-target-url') {
    Join-Path $PWD.Path 'integration_test/different_test.dart'
} else {
    Join-Path $PWD.Path 'integration_test/checklist_distribution_e2e_test.dart'
}
$testUrl = (New-Object Uri ([System.IO.Path]::GetFullPath($testFile))).AbsoluteUri
$differentTestFile = Join-Path $PWD.Path 'integration_test/different_test.dart'
$differentTestUrl = (New-Object Uri ([System.IO.Path]::GetFullPath($differentTestFile))).AbsoluteUri
Write-Output (([ordered]@{ protocolVersion='0.1.1'; runnerVersion='fake'; pid=1; type='start'; time=0 }) | ConvertTo-Json -Compress)
if ($env:FAKE_FLUTTER_SCENARIO -eq 'suite-path-native-url') {
    Write-Output (([ordered]@{ suite=[ordered]@{ id=0; platform='vm'; path=[System.IO.Path]::GetFullPath($testFile) }; type='suite'; time=0 }) | ConvertTo-Json -Compress)
    Write-Output (([ordered]@{ test=[ordered]@{ id=1; name=$testName; suiteID=0; url='package:flutter_test/src/widget_tester.dart' }; type='testStart'; time=1 }) | ConvertTo-Json -Compress)
}
elseif ($env:FAKE_FLUTTER_SCENARIO -eq 'suite-wrong-path-canonical-url') {
    Write-Output (([ordered]@{ suite=[ordered]@{ id=0; platform='vm'; path=[System.IO.Path]::GetFullPath($differentTestFile) }; type='suite'; time=0 }) | ConvertTo-Json -Compress)
    Write-Output (([ordered]@{ test=[ordered]@{ id=1; name=$testName; suiteID=0; url=$testUrl }; type='testStart'; time=1 }) | ConvertTo-Json -Compress)
}
elseif ($env:FAKE_FLUTTER_SCENARIO -eq 'suite-dangling-canonical-url') {
    Write-Output (([ordered]@{ test=[ordered]@{ id=1; name=$testName; suiteID=99; url=$testUrl }; type='testStart'; time=1 }) | ConvertTo-Json -Compress)
}
elseif ($env:FAKE_FLUTTER_SCENARIO -eq 'suite-duplicate-id') {
    Write-Output (([ordered]@{ suite=[ordered]@{ id=0; platform='vm'; path=[System.IO.Path]::GetFullPath($differentTestFile) }; type='suite'; time=0 }) | ConvertTo-Json -Compress)
    Write-Output (([ordered]@{ suite=[ordered]@{ id=0; platform='vm'; path=[System.IO.Path]::GetFullPath($testFile) }; type='suite'; time=0 }) | ConvertTo-Json -Compress)
    Write-Output (([ordered]@{ test=[ordered]@{ id=1; name=$testName; suiteID=0; url='package:flutter_test/src/widget_tester.dart' }; type='testStart'; time=1 }) | ConvertTo-Json -Compress)
}
elseif ($env:FAKE_FLUTTER_SCENARIO -eq 'suite-conflicting-file-url') {
    Write-Output (([ordered]@{ suite=[ordered]@{ id=0; platform='vm'; path=[System.IO.Path]::GetFullPath($testFile) }; type='suite'; time=0 }) | ConvertTo-Json -Compress)
    Write-Output (([ordered]@{ test=[ordered]@{ id=1; name=$testName; suiteID=0; url=$differentTestUrl }; type='testStart'; time=1 }) | ConvertTo-Json -Compress)
}
else {
    Write-Output (([ordered]@{ test=[ordered]@{ id=1; name=$testName; url=$testUrl }; type='testStart'; time=1 }) | ConvertTo-Json -Compress)
}

switch ($env:FAKE_FLUTTER_SCENARIO) {
    'child-failure' {
        Write-Output (([ordered]@{ testID=1; result='error'; skipped=$false; hidden=$false; type='testDone'; time=2 }) | ConvertTo-Json -Compress)
        Write-Output (([ordered]@{ success=$false; type='done'; time=3 }) | ConvertTo-Json -Compress)
        exit 23
    }
    'skipped' {
        Write-Output (([ordered]@{ testID=1; result='success'; skipped=$true; hidden=$false; type='testDone'; time=2 }) | ConvertTo-Json -Compress)
        Write-Output (([ordered]@{ success=$true; type='done'; time=3 }) | ConvertTo-Json -Compress)
        exit 0
    }
    'duplicate-target' {
        Write-Output (([ordered]@{ testID=1; result='success'; skipped=$false; hidden=$false; type='testDone'; time=2 }) | ConvertTo-Json -Compress)
        Write-Output (([ordered]@{ test=[ordered]@{ id=2; name=$testName; url='integration_test/checklist_distribution_e2e_test.dart' }; type='testStart'; time=3 }) | ConvertTo-Json -Compress)
        Write-Output (([ordered]@{ testID=2; result='success'; skipped=$false; hidden=$false; type='testDone'; time=4 }) | ConvertTo-Json -Compress)
        Write-Output (([ordered]@{ success=$true; type='done'; time=5 }) | ConvertTo-Json -Compress)
        exit 0
    }
    'extra-unrelated-test' {
        Write-Output (([ordered]@{ testID=1; result='success'; skipped=$false; hidden=$false; type='testDone'; time=2 }) | ConvertTo-Json -Compress)
        Write-Output (([ordered]@{ test=[ordered]@{ id=2; name='An unrelated test'; url='integration_test/checklist_distribution_e2e_test.dart' }; type='testStart'; time=3 }) | ConvertTo-Json -Compress)
        Write-Output (([ordered]@{ testID=2; result='success'; skipped=$false; hidden=$false; type='testDone'; time=4 }) | ConvertTo-Json -Compress)
        Write-Output (([ordered]@{ success=$true; type='done'; time=5 }) | ConvertTo-Json -Compress)
        exit 0
    }
    default {
        Write-Output (([ordered]@{ testID=1; result='success'; skipped=$false; hidden=$false; type='testDone'; time=2 }) | ConvertTo-Json -Compress)
        Write-Output (([ordered]@{ success=$true; type='done'; time=3 }) | ConvertTo-Json -Compress)
        exit 0
    }
}
'@

    $externalDefines = Join-Path $testRoot 'carebridge-checklist-live-e2e.json'
    New-ValidDefines $externalDefines

    Invoke-Test 'valid run writes sanitized PASS evidence and exact command' {
        $result = Invoke-RunnerCase 'valid' 'success' $externalDefines
        Assert-Equal 0 $result.ExitCode 'valid runner exit'
        Assert-NoSecrets $result.Output 'console output'
        $evidenceFiles = @(Get-ChildItem -LiteralPath $result.EvidenceRoot -File)
        Assert-Equal 2 $evidenceFiles.Count 'evidence file count'
        $allEvidence = ($evidenceFiles | ForEach-Object { Get-Content -Raw -LiteralPath $_.FullName }) -join [Environment]::NewLine
        Assert-NoSecrets $allEvidence 'evidence'
        $jsonFile = $evidenceFiles | Where-Object Extension -eq '.json' | Select-Object -First 1
        $evidence = Get-Content -Raw -LiteralPath $jsonFile.FullName | ConvertFrom-Json
        Assert-Equal 'PASS' $evidence.verdict 'evidence verdict'
        Assert-Equal 1 $evidence.targetTestCount 'target count'
        Assert-Equal 1 $evidence.successfulTargetTestCount 'successful target count'
        Assert-Equal 1 $evidence.visibleTestDoneCount 'visible test count'
        Assert-Equal 'e2e-runner-fixture' $evidence.serverEnvironmentId 'server environment identity'
        Assert-Equal 0 $evidence.skippedTestCount 'skipped count'
        $calls = @(Get-Content -LiteralPath $result.ArgumentLog | ForEach-Object { $_ | ConvertFrom-Json })
        Assert-Equal 2 $calls.Count 'fake Flutter call count'
        Assert-Equal 'devices' $calls[0][0] 'first call'
        Assert-True ($calls[0] -contains '--machine') 'device discovery must use machine output'
        Assert-Equal 'test' $calls[1][0] 'second call'
        Assert-True ($calls[1] -contains '--machine') 'test must use machine output'
        Assert-True ($calls[1] -contains '-d') 'test must set device flag'
        Assert-True ($calls[1] -contains 'emulator-5554') 'test must set exact device id'
        Assert-True ($calls[1] -contains 'integration_test/checklist_distribution_e2e_test.dart') 'test target must be exact'
        $defineArguments = @($calls[1] | Where-Object { $_ -like '--dart-define-from-file=*' })
        Assert-Equal 1 $defineArguments.Count 'define file argument count'
        Assert-True (-not (($calls[1] -join ' ').Contains($secretAccessToken))) 'secrets must not be CLI arguments'
    }

    Invoke-Test 'native integration suite path identifies the exact canonical test' {
        $result = Invoke-RunnerCase 'suite-path-native-url' 'suite-path-native-url' $externalDefines
        Assert-Equal 0 $result.ExitCode 'native suite-path runner exit'
        $evidenceFiles = @(Get-ChildItem -LiteralPath $result.EvidenceRoot -File)
        $jsonFile = $evidenceFiles | Where-Object Extension -eq '.json' | Select-Object -First 1
        $evidence = Get-Content -Raw -LiteralPath $jsonFile.FullName | ConvertFrom-Json
        Assert-Equal 'PASS' $evidence.verdict 'native suite-path evidence verdict'
        Assert-Equal 1 $evidence.targetTestCount 'native suite-path target count'
        Assert-Equal 1 $evidence.successfulTargetTestCount 'native suite-path successful target count'
        Assert-Equal 1 $evidence.visibleTestDoneCount 'native suite-path visible test count'
    }

    Invoke-Test 'wrong suite path cannot be rescued by a canonical test URL' {
        $result = Invoke-RunnerCase 'suite-wrong-path-canonical-url' 'suite-wrong-path-canonical-url' $externalDefines
        Assert-True ($result.ExitCode -ne 0) 'wrong suite path must fail'
        $jsonFile = Get-ChildItem -LiteralPath $result.EvidenceRoot -Filter '*.json' | Select-Object -First 1
        $evidence = Get-Content -Raw -LiteralPath $jsonFile.FullName | ConvertFrom-Json
        Assert-Equal 'FAIL' $evidence.verdict 'wrong suite path verdict'
        Assert-Equal 0 $evidence.targetTestCount 'wrong suite path target count'
    }

    Invoke-Test 'dangling suite identity cannot fall back to canonical test URL' {
        $result = Invoke-RunnerCase 'suite-dangling-canonical-url' 'suite-dangling-canonical-url' $externalDefines
        Assert-True ($result.ExitCode -ne 0) 'dangling suite identity must fail'
        $jsonFile = Get-ChildItem -LiteralPath $result.EvidenceRoot -Filter '*.json' | Select-Object -First 1
        $evidence = Get-Content -Raw -LiteralPath $jsonFile.FullName | ConvertFrom-Json
        Assert-Equal 'FAIL' $evidence.verdict 'dangling suite verdict'
        Assert-Equal 0 $evidence.targetTestCount 'dangling suite target count'
    }

    Invoke-Test 'duplicate suite identities cannot overwrite each other' {
        $result = Invoke-RunnerCase 'suite-duplicate-id' 'suite-duplicate-id' $externalDefines
        Assert-True ($result.ExitCode -ne 0) 'duplicate suite identity must fail'
        $jsonFile = Get-ChildItem -LiteralPath $result.EvidenceRoot -Filter '*.json' | Select-Object -First 1
        $evidence = Get-Content -Raw -LiteralPath $jsonFile.FullName | ConvertFrom-Json
        Assert-Equal 'FAIL' $evidence.verdict 'duplicate suite verdict'
        Assert-Equal 0 $evidence.targetTestCount 'duplicate suite target count'
    }

    Invoke-Test 'canonical suite rejects an explicit conflicting file URL' {
        $result = Invoke-RunnerCase 'suite-conflicting-file-url' 'suite-conflicting-file-url' $externalDefines
        Assert-True ($result.ExitCode -ne 0) 'conflicting suite file URL must fail'
        $jsonFile = Get-ChildItem -LiteralPath $result.EvidenceRoot -Filter '*.json' | Select-Object -First 1
        $evidence = Get-Content -Raw -LiteralPath $jsonFile.FullName | ConvertFrom-Json
        Assert-Equal 'FAIL' $evidence.verdict 'conflicting suite file URL verdict'
        Assert-Equal 0 $evidence.targetTestCount 'conflicting suite file URL target count'
    }

    Invoke-Test 'child nonzero exit is preserved and evidence stays sanitized' {
        $result = Invoke-RunnerCase 'child-failure' 'child-failure' $externalDefines
        Assert-Equal 23 $result.ExitCode 'child exit propagation'
        Assert-NoSecrets $result.Output 'failure console output'
        $evidenceFiles = @(Get-ChildItem -LiteralPath $result.EvidenceRoot -File)
        Assert-Equal 2 $evidenceFiles.Count 'failure evidence file count'
        $allEvidence = ($evidenceFiles | ForEach-Object { Get-Content -Raw -LiteralPath $_.FullName }) -join [Environment]::NewLine
        Assert-NoSecrets $allEvidence 'failure evidence'
        $evidence = Get-Content -Raw -LiteralPath (($evidenceFiles | Where-Object Extension -eq '.json')[0].FullName) | ConvertFrom-Json
        Assert-Equal 'FAIL' $evidence.verdict 'failure evidence verdict'
        Assert-Equal 23 $evidence.flutterExitCode 'failure evidence child exit'
    }

    Invoke-Test 'base64 dart defines are removed from sanitized evidence' {
        $result = Invoke-RunnerCase 'base64-secret-output' 'base64-secret-output' $externalDefines
        $encodedSecret = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(
            "CHK_MOTHER_ACCESS_TOKEN=$secretAccessToken-MOTHER"))
        $evidenceFiles = @(Get-ChildItem -LiteralPath $result.EvidenceRoot -File)
        $allEvidence = ($evidenceFiles | ForEach-Object { Get-Content -Raw -LiteralPath $_.FullName }) -join [Environment]::NewLine
        Assert-True (-not $allEvidence.Contains($encodedSecret)) 'base64 dart define must not survive redaction'
    }

    Invoke-Test 'base64 redaction precedes cross-secret raw substring redaction' {
        $collisionDefines = Join-Path $testRoot 'carebridge-base64-collision.json'
        New-ValidDefines $collisionDefines
        $collisionValues = Get-Content -Raw -LiteralPath $collisionDefines | ConvertFrom-Json
        $collisionValues.CHK_CONTENT_ADMIN_ACCESS_TOKEN = 'Q0hLX01P'
        $collisionValues.CHK_MOTHER_ACCESS_TOKEN = 'x'
        Write-Utf8NoBom $collisionDefines ($collisionValues | ConvertTo-Json -Depth 4)
        $result = Invoke-RunnerCase 'base64-collision' 'base64-secret-output' $collisionDefines
        $logFile = Get-ChildItem -LiteralPath $result.EvidenceRoot -Filter '*.log' | Select-Object -First 1
        $log = Get-Content -Raw -LiteralPath $logFile.FullName
        Assert-True $log.Contains('[REDACTED_BASE64:CHK_MOTHER_ACCESS_TOKEN]') 'encoded marker must survive collision'
        Assert-True (-not $log.Contains('[REDACTED:CHK_CONTENT_ADMIN_ACCESS_TOKEN]VEhFUl9BQ0NFU1NfVE9LRU49eA==')) 'reconstructable encoded tail must not survive'
    }

    Invoke-Test 'multi-device discovery selects the exact dedicated device' {
        $result = Invoke-RunnerCase 'multi-device' 'multi-device' $externalDefines
        Assert-Equal 0 $result.ExitCode 'multi-device runner exit code'
        Assert-True $result.Output.Contains('CHECKLIST_E2E_PASS') 'multi-device runner verdict'
        $calls = @(Get-Content -LiteralPath $result.ArgumentLog | ForEach-Object { $_ | ConvertFrom-Json })
        Assert-Equal 2 $calls.Count 'multi-device fake Flutter call count'
        Assert-True ($calls[1] -contains 'emulator-5554') 'multi-device exact target selection'
    }

    Invoke-Test 'skipped target cannot pass' {
        $result = Invoke-RunnerCase 'skipped' 'skipped' $externalDefines
        Assert-True ($result.ExitCode -ne 0) 'skipped test must fail runner'
        Assert-True (-not $result.Output.Contains('CHECKLIST_E2E_PASS')) 'skipped test must not print PASS'
    }

    Invoke-Test 'duplicate target cannot pass exactly-once gate' {
        $result = Invoke-RunnerCase 'duplicate-target' 'duplicate-target' $externalDefines
        Assert-True ($result.ExitCode -ne 0) 'duplicate target must fail runner'
        Assert-True (-not $result.Output.Contains('CHECKLIST_E2E_PASS')) 'duplicate target must not print PASS'
    }

    Invoke-Test 'missing target cannot pass exactly-once gate' {
        $result = Invoke-RunnerCase 'missing-target' 'missing-target' $externalDefines
        Assert-True ($result.ExitCode -ne 0) 'missing target must fail runner'
        Assert-True (-not $result.Output.Contains('CHECKLIST_E2E_PASS')) 'missing target must not print PASS'
    }

    Invoke-Test 'an extra unrelated visible test cannot pass exactly-once gate' {
        $result = Invoke-RunnerCase 'extra-unrelated-test' 'extra-unrelated-test' $externalDefines
        Assert-True ($result.ExitCode -ne 0) 'extra unrelated test must fail runner'
        Assert-True (-not $result.Output.Contains('CHECKLIST_E2E_PASS')) 'extra unrelated test must not print PASS'
    }

    Invoke-Test 'prefix collision and wrong target URL cannot satisfy target identity' {
        foreach ($scenario in @('prefix-collision', 'wrong-target-url')) {
            $result = Invoke-RunnerCase $scenario $scenario $externalDefines
            Assert-True ($result.ExitCode -ne 0) "$scenario must fail runner"
            Assert-True (-not $result.Output.Contains('CHECKLIST_E2E_PASS')) "$scenario must not print PASS"
        }
    }

    Invoke-Test 'production marker is refused before Flutter invocation' {
        $productionDefines = Join-Path $testRoot 'carebridge-production-refused.json'
        New-ValidDefines $productionDefines 'PRODUCTION'
        $result = Invoke-RunnerCase 'production-refused' 'success' $productionDefines
        Assert-True ($result.ExitCode -ne 0) 'production environment must be refused'
        Assert-True (-not (Test-Path -LiteralPath $result.ArgumentLog)) 'Flutter must not run for production marker'
        Assert-NoSecrets $result.Output 'production refusal output'
    }

    Invoke-Test 'unacknowledged origin is refused before Flutter invocation' {
        $result = Invoke-RunnerCase 'origin-refused' 'success' $externalDefines 'emulator-5554' 'http://127.0.0.1:8080'
        Assert-True ($result.ExitCode -ne 0) 'origin mismatch must be refused'
        Assert-True (-not (Test-Path -LiteralPath $result.ArgumentLog)) 'Flutter must not run for origin mismatch'
    }

    Invoke-Test 'desktop device is refused before test invocation' {
        $result = Invoke-RunnerCase 'desktop-refused' 'desktop-device' $externalDefines 'windows'
        Assert-True ($result.ExitCode -ne 0) 'desktop target must be refused'
        $calls = @(Get-Content -LiteralPath $result.ArgumentLog | ForEach-Object { $_ | ConvertFrom-Json })
        Assert-Equal 1 $calls.Count 'only device discovery may run'
        Assert-Equal 'devices' $calls[0][0] 'desktop refusal discovery call'
    }

    Invoke-Test 'missing required define is refused and redacted' {
        $missingDefines = Join-Path $testRoot 'carebridge-missing-key.json'
        New-ValidDefines $missingDefines
        $values = Get-Content -Raw -LiteralPath $missingDefines | ConvertFrom-Json
        $values.PSObject.Properties.Remove('CHK_ADMIN_REFRESH_TOKEN')
        Write-Utf8NoBom $missingDefines ($values | ConvertTo-Json -Depth 4)
        $result = Invoke-RunnerCase 'missing-key' 'success' $missingDefines
        Assert-True ($result.ExitCode -ne 0) 'missing key must fail runner'
        Assert-True (-not (Test-Path -LiteralPath $result.ArgumentLog)) 'Flutter must not run with missing defines'
        Assert-NoSecrets $result.Output 'missing-key refusal output'
    }

    Invoke-Test 'server environment identity is mandatory and production-safe' {
        $missingEnvironmentId = Join-Path $testRoot 'carebridge-missing-server-environment-id.json'
        New-ValidDefines $missingEnvironmentId
        $missingValues = Get-Content -Raw -LiteralPath $missingEnvironmentId | ConvertFrom-Json
        $missingValues.PSObject.Properties.Remove('CHK_E2E_SERVER_ENVIRONMENT_ID')
        Write-Utf8NoBom $missingEnvironmentId ($missingValues | ConvertTo-Json -Depth 4)
        $missingResult = Invoke-RunnerCase 'missing-server-environment-id' 'success' $missingEnvironmentId
        Assert-True ($missingResult.ExitCode -ne 0) 'missing server environment id must fail runner'
        Assert-True (-not (Test-Path -LiteralPath $missingResult.ArgumentLog)) 'Flutter must not run without server environment id'

        $productionEnvironmentId = Join-Path $testRoot 'carebridge-production-server-environment-id.json'
        New-ValidDefines $productionEnvironmentId
        $productionValues = Get-Content -Raw -LiteralPath $productionEnvironmentId | ConvertFrom-Json
        $productionValues.CHK_E2E_SERVER_ENVIRONMENT_ID = 'e2e-prod-primary'
        Write-Utf8NoBom $productionEnvironmentId ($productionValues | ConvertTo-Json -Depth 4)
        $productionResult = Invoke-RunnerCase 'production-server-environment-id' 'success' $productionEnvironmentId
        Assert-True ($productionResult.ExitCode -ne 0) 'production-like server environment id must fail runner'
        Assert-True (-not (Test-Path -LiteralPath $productionResult.ArgumentLog)) 'Flutter must not run with production-like server environment id'

        foreach ($unsafeId in @('e2e-prod1-primary', 'e2e-production1', 'e2e-live2')) {
            $unsafePath = Join-Path $testRoot ("unsafe-server-environment-{0}.json" -f ([Guid]::NewGuid().ToString('N')))
            New-ValidDefines $unsafePath
            $unsafeValues = Get-Content -Raw -LiteralPath $unsafePath | ConvertFrom-Json
            $unsafeValues.CHK_E2E_SERVER_ENVIRONMENT_ID = $unsafeId
            Write-Utf8NoBom $unsafePath ($unsafeValues | ConvertTo-Json -Depth 4)
            $unsafeResult = Invoke-RunnerCase ("unsafe-server-environment-$unsafeId") 'success' $unsafePath
            Assert-True ($unsafeResult.ExitCode -ne 0) "$unsafeId must fail runner"
            Assert-True (-not (Test-Path -LiteralPath $unsafeResult.ArgumentLog)) 'Flutter must not run with suffixed production marker'
        }
    }

    Invoke-Test 'production-like origin host suffixes are refused before Flutter invocation' {
        foreach ($unsafeOrigin in @('https://api-prod1.example.com', 'https://production1.example.com', 'https://live2.example.com')) {
            $unsafePath = Join-Path $testRoot ("unsafe-origin-{0}.json" -f ([Guid]::NewGuid().ToString('N')))
            New-ValidDefines $unsafePath
            $unsafeValues = Get-Content -Raw -LiteralPath $unsafePath | ConvertFrom-Json
            $unsafeValues.API_BASE_URL = $unsafeOrigin
            $unsafeValues.CHK_E2E_EXPECTED_API_BASE_URL = $unsafeOrigin
            Write-Utf8NoBom $unsafePath ($unsafeValues | ConvertTo-Json -Depth 4)
            $unsafeResult = Invoke-RunnerCase ("unsafe-origin-$($unsafeValues.GetHashCode())") 'success' $unsafePath 'emulator-5554' $unsafeOrigin
            Assert-True ($unsafeResult.ExitCode -ne 0) "$unsafeOrigin must fail runner"
            Assert-True (-not (Test-Path -LiteralPath $unsafeResult.ArgumentLog)) 'Flutter must not run for production-like origin host'
        }
    }

    Invoke-Test 'unignored defines inside repository are refused' {
        $unignoredDefines = Join-Path $PSScriptRoot 'runner-defines-unignored.json'
        try {
            New-ValidDefines $unignoredDefines
            $result = Invoke-RunnerCase 'unignored-defines' 'success' $unignoredDefines
            Assert-True ($result.ExitCode -ne 0) 'unignored repository defines must fail runner'
            Assert-True (-not (Test-Path -LiteralPath $result.ArgumentLog)) 'Flutter must not run with unignored repository defines'
            Assert-NoSecrets $result.Output 'unignored defines refusal output'
        }
        finally {
            if (Test-Path -LiteralPath $unignoredDefines -PathType Leaf) {
                Remove-Item -LiteralPath $unignoredDefines -Force
            }
        }
    }

    Invoke-Test 'relative Flutter executable is refused' {
        $caseRoot = Join-Path $testRoot 'relative-flutter'
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        $output = @(& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $runnerPath `
                -FlutterExecutable 'flutter.bat' `
                -DeviceId 'emulator-5554' `
                -DefinesFile $externalDefines `
                -AcknowledgedApiOrigin 'http://10.0.2.2:8080' `
                -EvidenceDirectory (Join-Path $caseRoot 'evidence') 2>&1 | ForEach-Object { "$_" })
        $exitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorActionPreference
        Assert-True ($exitCode -ne 0) 'relative Flutter executable must fail'
        Assert-NoSecrets ($output -join [Environment]::NewLine) 'relative executable refusal output'
    }
}
finally {
    if (Test-Path -LiteralPath $testRoot) {
        $resolvedTestRoot = [System.IO.Path]::GetFullPath($testRoot)
        $resolvedTempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
        if ($resolvedTestRoot.StartsWith($resolvedTempRoot, [System.StringComparison]::OrdinalIgnoreCase) -and
                $resolvedTestRoot -ne $resolvedTempRoot) {
            Remove-Item -LiteralPath $resolvedTestRoot -Recurse -Force
        }
    }
}

Write-Output "RESULT:passed=$script:passed failed=$script:failed"
if ($script:failed -ne 0) {
    exit 1
}
exit 0
