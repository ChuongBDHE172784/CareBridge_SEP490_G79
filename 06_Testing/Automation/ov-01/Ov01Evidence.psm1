Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-Ov01Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Cannot hash missing file: $Path"
    }
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Get-Ov01StringSha256 {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Value)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return ([System.BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
}

function Normalize-Ov01VersionOutput {
    param([AllowEmptyString()][string]$Text)

    if ($null -eq $Text) { return '' }
    # Version output is retained after secret redaction.  Canonicalize platform
    # line endings and terminal colour escapes before hashing/comparing it.
    $normalized = [regex]::Replace([string]$Text, '\x1B\[[0-?]*[ -/]*[@-~]', '')
    $normalized = $normalized -replace "`r`n?", "`n"
    # Flutter derives human-readable ages from the wall clock and may emit an
    # empty age when SDK Git metadata is unavailable in a sandbox.  Canonicalize
    # only that volatile parenthetical; keep versions, revisions, hashes, dates,
    # channel and Dart/DevTools identities byte-for-byte significant.
    # Keep the source ASCII-only: Windows PowerShell 5 loads UTF-8/no-BOM
    # modules using the active ANSI code page, so use the regex Unicode escape.
    $relativeAgePattern = '\s+\((?:(?:\d+|a|an)\s+(?:seconds?|minutes?|hours?|days?|weeks?|months?|years?)\s+ago)?\)(?=\s+\u2022)'
    $normalized = [regex]::Replace($normalized, $relativeAgePattern, ' (<VOLATILE_AGE>)', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    return $normalized.TrimEnd("`n")
}

function ConvertTo-Ov01RelativePath {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Path
    )

    $rootFull = [System.IO.Path]::GetFullPath($Root).TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
    $pathFull = [System.IO.Path]::GetFullPath($Path)
    if (-not $pathFull.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Path escapes repository root: $Path"
    }
    return $pathFull.Substring($rootFull.Length).Replace('\', '/')
}

function Resolve-Ov01RepoPath {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$RelativePath
    )

    if ([System.IO.Path]::IsPathRooted($RelativePath)) {
        throw "Registry path must be repository-relative: $RelativePath"
    }
    $candidate = [System.IO.Path]::GetFullPath((Join-Path $Root $RelativePath))
    [void](ConvertTo-Ov01RelativePath -Root $Root -Path $candidate)
    return $candidate
}

function Resolve-Ov01RepoGlob {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$RelativePattern
    )

    if ([System.IO.Path]::IsPathRooted($RelativePattern)) { throw "Registry path must be repository-relative: $RelativePattern" }
    $normalized = $RelativePattern.Replace('\', '/')
    $wildcardIndex = $normalized.IndexOfAny([char[]]@('*', '?', '['))
    $staticPrefix = if ($wildcardIndex -ge 0) { $normalized.Substring(0, $wildcardIndex) } else { $normalized }
    $staticParent = if ($staticPrefix.EndsWith('/')) { $staticPrefix.TrimEnd('/') } else { [System.IO.Path]::GetDirectoryName($staticPrefix.Replace('/', '\')) }
    if ([string]::IsNullOrWhiteSpace($staticParent)) { $staticParent = '.' }
    [void](Resolve-Ov01RepoPath -Root $Root -RelativePath $staticParent)
    return (Join-Path $Root $RelativePattern)
}

function Protect-Ov01Text {
    param([AllowEmptyString()][string]$Text)

    if ($null -eq $Text) { return '' }
    $value = $Text
    $patterns = @(
        @{ Pattern = '(?i)(authorization\s*:\s*bearer\s+)[^\s\"'']+'; Replacement = '$1[REDACTED]' },
        @{ Pattern = '(?i)\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b'; Replacement = '[REDACTED_JWT]' },
        @{ Pattern = '(?i)((?:password|passwd|pwd|client[_-]?secret|api[_-]?key|access[_-]?token|refresh[_-]?token|otp)\s*[=:]\s*)[^\s,;\"'']+'; Replacement = '$1[REDACTED]' },
        @{ Pattern = '(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b'; Replacement = '[REDACTED_EMAIL]' },
        @{ Pattern = '(?<![A-Fa-f0-9])(?:\+?84|0)[ .-]?(?:3|5|7|8|9)(?:[ .-]?\d){8}(?![A-Fa-f0-9])'; Replacement = '[REDACTED_PHONE]' }
    )
    foreach ($item in $patterns) {
        $value = [regex]::Replace($value, $item.Pattern, $item.Replacement)
    }
    return $value
}

function Test-Ov01Leak {
    param([AllowEmptyString()][string]$Text)

    $findings = New-Object System.Collections.Generic.List[string]
    $checks = @(
        @{ Code = 'BEARER_TOKEN'; Pattern = '(?i)authorization\s*:\s*bearer\s+(?!\[REDACTED\])\S+' },
        @{ Code = 'JWT'; Pattern = '(?i)\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b' },
        @{ Code = 'SECRET_ASSIGNMENT'; Pattern = '(?i)(?:password|passwd|pwd|client[_-]?secret|api[_-]?key|access[_-]?token|refresh[_-]?token|otp)\s*[=:]\s*(?!\[REDACTED\])[^\s,;\"'']+' },
        @{ Code = 'EMAIL'; Pattern = '(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b' },
        @{ Code = 'PHONE'; Pattern = '(?<![A-Fa-f0-9])(?:\+?84|0)[ .-]?(?:3|5|7|8|9)(?:[ .-]?\d){8}(?![A-Fa-f0-9])' }
    )
    foreach ($check in $checks) {
        if ([regex]::IsMatch($Text, $check.Pattern)) { $findings.Add($check.Code) }
    }
    return @($findings | Sort-Object -Unique)
}

function Read-Ov01Registry {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Registry not found: $Path" }
    return (Get-Content -Encoding UTF8 -Raw -LiteralPath $Path | ConvertFrom-Json)
}

function Add-Ov01ExactSetErrors {
    param(
        [AllowEmptyCollection()][object[]]$Actual,
        [Parameter(Mandatory = $true)][string[]]$Expected,
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)]$Errors
    )

    $values = @($Actual | ForEach-Object { [string]$_ })
    foreach ($empty in @($values | Where-Object { [string]::IsNullOrWhiteSpace($_) })) {
        $Errors.Add("empty $Label identity")
    }
    foreach ($duplicate in @($values | Group-Object | Where-Object Count -gt 1)) {
        $Errors.Add("duplicate $Label identity: $($duplicate.Name)")
    }
    foreach ($id in $Expected) {
        if ($values -notcontains $id) { $Errors.Add("missing canonical $Label identity: $id") }
    }
    foreach ($id in $values) {
        if (-not [string]::IsNullOrWhiteSpace($id) -and $Expected -notcontains $id) { $Errors.Add("unknown $Label identity: $id") }
    }
    if ($values.Count -ne $Expected.Count) { $Errors.Add("$Label cardinality must be exactly $($Expected.Count), actual=$($values.Count)") }
}

function Get-Ov01CanonicalReleaseGateIds {
    [string[]]$ids = @(
        'OV01-GATE-BE-LIFECYCLE', 'OV01-GATE-BE-SAFETY',
        'OV01-GATE-MOBILE-OV01', 'OV01-GATE-WEB-OV01', 'OV01-GATE-AI-OV01',
        'OV01-GATE-BE-FULL', 'OV01-GATE-BE-PACKAGE',
        'OV01-GATE-MOBILE-FULL', 'OV01-GATE-MOBILE-ANALYZE', 'OV01-GATE-MOBILE-FORMAT', 'OV01-GATE-MOBILE-APK',
        'OV01-GATE-WEB-FULL', 'OV01-GATE-WEB-LINT', 'OV01-GATE-WEB-BUILD',
        'OV01-GATE-AI-FULL', 'OV01-GATE-AI-COMPILE', 'OV01-GATE-EVALUATOR-FULL'
    )
    return $ids
}

function Test-Ov01Registry {
    param(
        [Parameter(Mandatory = $true)]$Registry,
        [Parameter(Mandatory = $true)][string]$RepoRoot
    )

    $errors = New-Object System.Collections.Generic.List[string]
    if ($Registry.runnerId -ne 'OV01-AUTO-002') { $errors.Add('runnerId must be OV01-AUTO-002') }

    $expectedScenarios = @(1..15 | ForEach-Object { 'OV01-E2E-{0:d3}' -f $_ })
    $expectedRequiredGates = @(
        'OV01-GATE-BE-FULL', 'OV01-GATE-BE-PACKAGE',
        'OV01-GATE-MOBILE-FULL', 'OV01-GATE-MOBILE-ANALYZE', 'OV01-GATE-MOBILE-FORMAT', 'OV01-GATE-MOBILE-APK',
        'OV01-GATE-WEB-FULL', 'OV01-GATE-WEB-LINT', 'OV01-GATE-WEB-BUILD',
        'OV01-GATE-AI-FULL', 'OV01-GATE-AI-COMPILE', 'OV01-GATE-EVALUATOR-FULL'
    )
    $expectedArtifacts = @(
        'OV01-ART-REGISTRY', 'OV01-ART-AI-RULES', 'OV01-ART-AI-SCHEMAS', 'OV01-ART-EVAL-BENCHMARK', 'OV01-ART-EVAL-MAPPING',
        'OV01-ART-BACKEND-JAR', 'OV01-ART-MOBILE-APK', 'OV01-ART-WEB-INDEX', 'OV01-ART-WEB-ASSETS'
    )

    $scenarioIds = @($Registry.scenarios | ForEach-Object { [string]$_.id })
    Add-Ov01ExactSetErrors -Actual $scenarioIds -Expected $expectedScenarios -Label 'scenario' -Errors $errors
    Add-Ov01ExactSetErrors -Actual @($Registry.requiredScenarioIds) -Expected $expectedScenarios -Label 'required scenario' -Errors $errors

    $gateIds = @($Registry.gates | ForEach-Object { [string]$_.id })
    Add-Ov01ExactSetErrors -Actual $gateIds -Expected (Get-Ov01CanonicalReleaseGateIds) -Label 'gate' -Errors $errors
    if (@(Compare-Object (Get-Ov01CanonicalReleaseGateIds) $gateIds -SyncWindow 0).Count -ne 0) { $errors.Add('gate catalog order is not canonical') }
    Add-Ov01ExactSetErrors -Actual @($Registry.requiredPlatformGateIds) -Expected $expectedRequiredGates -Label 'required platform gate' -Errors $errors
    foreach ($requiredGate in @($Registry.requiredPlatformGateIds)) {
        if ($gateIds -notcontains [string]$requiredGate) { $errors.Add("missing required platform gate: $requiredGate") }
    }

    $referencedGateIds = New-Object System.Collections.Generic.List[string]
    foreach ($scenario in @($Registry.scenarios)) {
        if (@($scenario.gateIds).Count -eq 0) { $errors.Add("scenario has empty gate selection: $($scenario.id)") }
        if (@($scenario.proofRefs).Count -eq 0) { $errors.Add("scenario has no proofRefs: $($scenario.id)") }
        if ($null -eq $scenario.PSObject.Properties['executableSelectors'] -or @($scenario.executableSelectors).Count -eq 0) {
            $errors.Add("scenario has no executable selectors: $($scenario.id)")
        } else {
            foreach ($selector in @($scenario.executableSelectors)) {
                if ([string]::IsNullOrWhiteSpace([string]$selector.selector)) { $errors.Add("scenario has empty executable selector: $($scenario.id)") }
                if ([string]::IsNullOrWhiteSpace([string]$selector.gateId)) { $errors.Add("scenario selector has empty gate: $($scenario.id)") }
                elseif (@($scenario.gateIds) -notcontains [string]$selector.gateId) { $errors.Add("scenario selector gate is outside scenario: $($scenario.id)/$($selector.gateId)") }
                elseif ($gateIds -notcontains [string]$selector.gateId) { $errors.Add("scenario selector references unknown gate: $($scenario.id)/$($selector.gateId)") }
            }
        }
        foreach ($gateId in @($scenario.gateIds)) {
            $referencedGateIds.Add([string]$gateId)
            if ($gateIds -notcontains [string]$gateId) { $errors.Add("scenario $($scenario.id) references unknown gate: $gateId") }
        }
        foreach ($proof in @($scenario.proofRefs)) {
            try {
                $proofPath = Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath ([string]$proof.path)
                if (-not (Test-Path -LiteralPath $proofPath -PathType Leaf)) {
                    $errors.Add("proof path does not exist for $($scenario.id): $($proof.path)")
                } elseif (-not [string]::IsNullOrWhiteSpace([string]$proof.contains)) {
                    $content = Get-Content -Raw -LiteralPath $proofPath
                    if ($content.IndexOf([string]$proof.contains, [System.StringComparison]::Ordinal) -lt 0) {
                        $errors.Add("proof selector not found for $($scenario.id): $($proof.contains)")
                    }
                }
            } catch { $errors.Add($_.Exception.Message) }
        }
    }
    foreach ($gateId in @($Registry.requiredPlatformGateIds)) { $referencedGateIds.Add([string]$gateId) }
    foreach ($gateId in $gateIds) {
        if (@($referencedGateIds) -notcontains $gateId) { $errors.Add("orphan gate: $gateId") }
    }

    $e2e014Definition = @($Registry.scenarios | Where-Object { [string]$_.id -eq 'OV01-E2E-014' } | Select-Object -First 1)
    if ($e2e014Definition.Count -eq 1) {
        $actualE2e014Selectors = @($e2e014Definition[0].executableSelectors | ForEach-Object { "$([string]$_.gateId)`t$([string]$_.selector)" })
        $expectedE2e014Selectors = @(
            "OV01-GATE-BE-SAFETY`tEmergencyTriageLinkPostgresIntegrationTest::ov01E2e014RestartReclaimsExpiredAttemptWithoutResendingSuccessfulDevice",
            "OV01-GATE-MOBILE-OV01`tauth landing keeps unresolved continuation and offers retry in place"
        )
        Add-Ov01ExactSetErrors -Actual $actualE2e014Selectors -Expected $expectedE2e014Selectors -Label 'OV01-E2E-014 executable selector' -Errors $errors
    }

    foreach ($gate in @($Registry.gates)) {
        if (@('test', 'analysis', 'build') -notcontains [string]$gate.kind) {
            $errors.Add("invalid gate kind for $($gate.id): $($gate.kind)")
        }
        if ([string]::IsNullOrWhiteSpace([string]$gate.executable)) { $errors.Add("gate executable is empty: $($gate.id)") }
        if ($null -ne $gate.PSObject.Properties['runnerTool']) {
            if ([string]$gate.runnerTool -ne 'flutter-dart-snapshot') { $errors.Add("unknown gate runner tool: $($gate.id)/$($gate.runnerTool)") }
            if ([string]$gate.platform -ne 'Mobile') { $errors.Add("Flutter Dart snapshot runner must be a Mobile gate: $($gate.id)") }
            if ([System.IO.Path]::GetFileName([string]$gate.executable) -ine 'flutter.bat') { $errors.Add("Flutter Dart snapshot runner must be configured from flutter.bat: $($gate.id)") }
        }
        $isWindowsBatchGate = @('.cmd', '.bat') -contains [System.IO.Path]::GetExtension([string]$gate.executable).ToLowerInvariant()
        foreach ($argument in @(@($gate.arguments) + @($gate.versionArguments))) {
            if ([regex]::IsMatch([string]$argument, "[\x00\r\n]")) { $errors.Add("gate argument contains a forbidden control character: $($gate.id)") }
            if ($isWindowsBatchGate -and [string]$argument -match '"' -and [regex]::IsMatch([string]$argument, '[&|<>()\^\s]')) {
                $errors.Add("gate argument combines an embedded quote with a CMD separator: $($gate.id)")
            }
        }
        $timeoutSeconds = 0
        if ($null -eq $gate.PSObject.Properties['timeoutSeconds'] -or
            -not [int]::TryParse([string]$gate.timeoutSeconds, [ref]$timeoutSeconds) -or
            $timeoutSeconds -lt 1 -or $timeoutSeconds -gt 3600) {
            $errors.Add("gate timeout must be an integer from 1 through 3600 seconds: $($gate.id)")
        }
        if ($null -ne $gate.PSObject.Properties['environment']) {
            foreach ($property in $gate.environment.PSObject.Properties) {
                if ([string]::IsNullOrWhiteSpace([string]$property.Name)) { $errors.Add("gate environment contains an empty key: $($gate.id)") }
                if ($null -eq $property.Value) { $errors.Add("gate environment contains a null value: $($gate.id)/$($property.Name)") }
                if (@('PYTHONPATH', 'DART_SUPPRESS_ANALYTICS') -notcontains [string]$property.Name) { $errors.Add("gate environment key is not evidence-safe: $($gate.id)/$($property.Name)") }
            }
        }
        if ($null -ne $gate.PSObject.Properties['evidenceEnvironmentKeys']) {
            foreach ($key in @($gate.evidenceEnvironmentKeys)) {
                if (@('PYTHONPATH', 'DART_SUPPRESS_ANALYTICS') -notcontains [string]$key) { $errors.Add("gate evidence environment key is not allowlisted: $($gate.id)/$key") }
                elseif ($null -eq $gate.PSObject.Properties['environment'] -or $null -eq $gate.environment.PSObject.Properties[[string]$key]) { $errors.Add("gate evidence environment key has no configured value: $($gate.id)/$key") }
            }
        }
        if ($null -ne $gate.PSObject.Properties['pytestIsolation']) {
            if ($gate.pytestIsolation -isnot [bool] -or -not [bool]$gate.pytestIsolation) { $errors.Add("gate pytest isolation must be the boolean true when present: $($gate.id)") }
            if ([string]$gate.parser -ne 'pytest' -or [string]$gate.kind -ne 'test') { $errors.Add("gate pytest isolation requires a pytest test gate: $($gate.id)") }
        }
        if ($null -ne $gate.PSObject.Properties['approvedSkippedTests']) {
            if ([string]$gate.parser -ne 'maven-surefire' -or [string]$gate.kind -ne 'test') { $errors.Add("approved skipped tests require a Maven Surefire test gate: $($gate.id)") }
            $approvedSelectors = @($gate.approvedSkippedTests | ForEach-Object { [string]$_.selector })
            foreach ($duplicate in @($approvedSelectors | Group-Object | Where-Object Count -gt 1)) { $errors.Add("duplicate approved skipped test selector: $($gate.id)/$($duplicate.Name)") }
            foreach ($approval in @($gate.approvedSkippedTests)) {
                if ([string]$approval.selector -notmatch '^[A-Za-z0-9_.$]+#[A-Za-z0-9_.$]+$') { $errors.Add("approved skipped test selector is invalid: $($gate.id)/$($approval.selector)") }
                foreach ($field in @('reasonContains', 'reasonCode', 'owner', 'expiresOn')) {
                    if ($null -eq $approval.PSObject.Properties[$field] -or [string]::IsNullOrWhiteSpace([string]$approval.$field)) { $errors.Add("approved skipped test metadata is missing: $($gate.id)/$($approval.selector)/$field") }
                }
                $expiry = [DateTime]::MinValue
                if (-not [DateTime]::TryParseExact([string]$approval.expiresOn, 'yyyy-MM-dd', [System.Globalization.CultureInfo]::InvariantCulture, [System.Globalization.DateTimeStyles]::None, [ref]$expiry)) {
                    $errors.Add("approved skipped test expiry is invalid: $($gate.id)/$($approval.selector)")
                } elseif ($expiry.Date -lt [DateTime]::UtcNow.Date) {
                    $errors.Add("approved skipped test waiver is expired: $($gate.id)/$($approval.selector)")
                }
                if ($null -eq $approval.PSObject.Properties['evidencePaths'] -or @($approval.evidencePaths).Count -eq 0) {
                    $errors.Add("approved skipped test evidence is missing: $($gate.id)/$($approval.selector)")
                } else {
                    foreach ($path in @($approval.evidencePaths)) {
                        try {
                            $evidencePath = Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath ([string]$path)
                            if (-not (Test-Path -LiteralPath $evidencePath -PathType Leaf)) { $errors.Add("approved skipped test evidence path does not exist: $($gate.id)/$path") }
                        } catch { $errors.Add($_.Exception.Message) }
                    }
                }
            }
        }
        if ($null -eq $gate.PSObject.Properties['expectedVersionRegex'] -or [string]::IsNullOrWhiteSpace([string]$gate.expectedVersionRegex)) {
            $errors.Add("gate version regex is missing: $($gate.id)")
        } else {
            try { [void][regex]::new([string]$gate.expectedVersionRegex) } catch { $errors.Add("gate version regex is invalid: $($gate.id)") }
            if ($null -eq $gate.PSObject.Properties['versionArguments'] -or @($gate.versionArguments).Count -eq 0) { $errors.Add("gate version arguments are missing: $($gate.id)") }
            elseif (@($gate.versionArguments | Where-Object { [string]::IsNullOrWhiteSpace([string]$_) }).Count -gt 0) { $errors.Add("gate version arguments contain an empty value: $($gate.id)") }
        }
        if ($null -ne $gate.PSObject.Properties['dynamicArguments']) {
            if ([string]::IsNullOrWhiteSpace([string]$gate.dynamicArguments.sourceIdentityPathRegex)) { $errors.Add("gate dynamic source regex is empty: $($gate.id)") }
            else { try { [void][regex]::new([string]$gate.dynamicArguments.sourceIdentityPathRegex) } catch { $errors.Add("gate dynamic source regex is invalid: $($gate.id)") } }
            if ([string]::IsNullOrWhiteSpace([string]$gate.dynamicArguments.stripPrefix)) { $errors.Add("gate dynamic strip prefix is empty: $($gate.id)") }
        }
        if ($null -ne $gate.PSObject.Properties['fallback']) {
            $isWindowsBatchFallback = @('.cmd', '.bat') -contains [System.IO.Path]::GetExtension([string]$gate.fallback.executable).ToLowerInvariant()
            $fallbackArgumentsForValidation = if ($null -ne $gate.fallback.PSObject.Properties['arguments']) { @($gate.fallback.arguments) } else { @() }
            $fallbackVersionArgumentsForValidation = if ($null -ne $gate.fallback.PSObject.Properties['versionArguments']) { @($gate.fallback.versionArguments) } else { @() }
            foreach ($argument in @($fallbackArgumentsForValidation + $fallbackVersionArgumentsForValidation)) {
                if ([regex]::IsMatch([string]$argument, "[\x00\r\n]")) { $errors.Add("gate fallback argument contains a forbidden control character: $($gate.id)") }
                if ($isWindowsBatchFallback -and [string]$argument -match '"' -and [regex]::IsMatch([string]$argument, '[&|<>()\^\s]')) {
                    $errors.Add("gate fallback argument combines an embedded quote with a CMD separator: $($gate.id)")
                }
            }
            if ([string]::IsNullOrWhiteSpace([string]$gate.fallback.executable)) { $errors.Add("gate fallback executable is empty: $($gate.id)") }
            if ([string]::IsNullOrWhiteSpace([string]$gate.fallback.whenOutputMatches)) { $errors.Add("gate fallback trigger is empty: $($gate.id)") }
            else {
                try { [void][regex]::new([string]$gate.fallback.whenOutputMatches) }
                catch { $errors.Add("gate fallback trigger is invalid regex: $($gate.id)") }
            }
            if ([string]$gate.fallback.executable -eq '${MAVEN_PINNED_3_9_16}' -and [string]$gate.fallback.expectedVersionRegex -ne 'Apache Maven 3\.9\.16') {
                $errors.Add("pinned Maven fallback must require Maven 3.9.16: $($gate.id)")
            }
            if ($null -eq $gate.fallback.PSObject.Properties['versionArguments'] -or @($gate.fallback.versionArguments).Count -eq 0) { $errors.Add("gate fallback version arguments are missing: $($gate.id)") }
            if ($null -eq $gate.fallback.PSObject.Properties['expectedVersionRegex'] -or [string]::IsNullOrWhiteSpace([string]$gate.fallback.expectedVersionRegex)) { $errors.Add("gate fallback version regex is missing: $($gate.id)") }
            else { try { [void][regex]::new([string]$gate.fallback.expectedVersionRegex) } catch { $errors.Add("gate fallback version regex is invalid: $($gate.id)") } }
        }
        try {
            $workingDirectory = Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath ([string]$gate.workingDirectory)
            if (-not (Test-Path -LiteralPath $workingDirectory -PathType Container)) {
                $errors.Add("gate working directory does not exist: $($gate.workingDirectory)")
            }
        } catch { $errors.Add($_.Exception.Message) }
    }

    $expectedFlutterDartGateIds = @('OV01-GATE-MOBILE-OV01', 'OV01-GATE-MOBILE-FULL', 'OV01-GATE-MOBILE-ANALYZE', 'OV01-GATE-MOBILE-APK')
    $actualFlutterDartGateIds = @($Registry.gates | Where-Object { $null -ne $_.PSObject.Properties['runnerTool'] -and [string]$_.runnerTool -eq 'flutter-dart-snapshot' } | ForEach-Object { [string]$_.id })
    Add-Ov01ExactSetErrors -Actual $actualFlutterDartGateIds -Expected $expectedFlutterDartGateIds -Label 'Flutter Dart snapshot gate' -Errors $errors

    $expectedPytestIsolationGateIds = @('OV01-GATE-AI-OV01', 'OV01-GATE-AI-FULL', 'OV01-GATE-EVALUATOR-FULL')
    $actualPytestIsolationGateIds = @($Registry.gates | Where-Object { $null -ne $_.PSObject.Properties['pytestIsolation'] -and [bool]$_.pytestIsolation } | ForEach-Object { [string]$_.id })
    Add-Ov01ExactSetErrors -Actual $actualPytestIsolationGateIds -Expected $expectedPytestIsolationGateIds -Label 'pytest-isolated gate' -Errors $errors

    $expectedApprovedSkipGateIds = @('OV01-GATE-BE-FULL', 'OV01-GATE-BE-PACKAGE')
    $actualApprovedSkipGateIds = @($Registry.gates | Where-Object { $null -ne $_.PSObject.Properties['approvedSkippedTests'] -and @($_.approvedSkippedTests).Count -gt 0 } | ForEach-Object { [string]$_.id })
    Add-Ov01ExactSetErrors -Actual $actualApprovedSkipGateIds -Expected $expectedApprovedSkipGateIds -Label 'approved-skip gate' -Errors $errors
    $expectedCloudinarySkipSelector = 'com.carebridge.backend.content.integration.ContentBodySanitizeIntegrationTest#uploadPublicContentImage_endToEnd_persistsPublicAccessModeAndPermanentUrl'
    $expectedCloudinarySkipReasonContains = 'Requires real CLOUDINARY_'
    $expectedCloudinarySkipReasonCode = 'EXTERNAL_CREDENTIAL_INTEGRATION_OUTSIDE_OV01'
    $expectedCloudinarySkipOwner = 'CareBridge content platform'
    $expectedCloudinarySkipExpiresOn = '2026-10-31'
    $expectedCloudinarySkipEvidencePaths = @(
        '04_Implement/ContentRichTextEditor/ContentRichTextEditor_Test-Spec.md',
        '05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/file/CloudinaryStorageServiceTest.java'
    )
    $expectedCloudinarySkipProperties = @('evidencePaths', 'expiresOn', 'owner', 'reasonCode', 'reasonContains', 'selector')
    foreach ($gateId in $expectedApprovedSkipGateIds) {
        $gate = @($Registry.gates | Where-Object { [string]$_.id -eq $gateId } | Select-Object -First 1)
        if ($gate.Count -eq 1) {
            $approvals = @($gate[0].approvedSkippedTests)
            $approval = if ($approvals.Count -eq 1) { $approvals[0] } else { $null }
            $actualEvidencePaths = @()
            if ($null -ne $approval -and $null -ne $approval.PSObject.Properties['evidencePaths']) {
                $actualEvidencePaths = @($approval.evidencePaths | ForEach-Object { [string]$_ } | Sort-Object)
            }
            $expectedEvidencePaths = @($expectedCloudinarySkipEvidencePaths | Sort-Object)
            $actualProperties = @()
            if ($null -ne $approval) {
                $actualProperties = @($approval.PSObject.Properties.Name | Sort-Object)
            }
            $metadataMatches = $null -ne $approval -and
                [string]$approval.selector -ceq $expectedCloudinarySkipSelector -and
                [string]$approval.reasonContains -ceq $expectedCloudinarySkipReasonContains -and
                [string]$approval.reasonCode -ceq $expectedCloudinarySkipReasonCode -and
                [string]$approval.owner -ceq $expectedCloudinarySkipOwner -and
                [string]$approval.expiresOn -ceq $expectedCloudinarySkipExpiresOn -and
                $actualEvidencePaths.Count -eq $expectedEvidencePaths.Count -and
                @(Compare-Object $expectedEvidencePaths $actualEvidencePaths -SyncWindow 0).Count -eq 0 -and
                $actualProperties.Count -eq $expectedCloudinarySkipProperties.Count -and
                @(Compare-Object $expectedCloudinarySkipProperties $actualProperties -SyncWindow 0).Count -eq 0
            if ($approvals.Count -ne 1 -or -not $metadataMatches) {
                $errors.Add("approved skip contract is not the exact Cloudinary external waiver: $gateId")
            }
        }
    }
    $expectedBackendReleaseArguments = @{
        'OV01-GATE-BE-FULL' = @('-Dgate0.enabled=true', 'test')
        'OV01-GATE-BE-PACKAGE' = @('-Dgate0.enabled=true', 'clean', 'package')
    }
    foreach ($gateId in $expectedBackendReleaseArguments.Keys) {
        $gate = @($Registry.gates | Where-Object { [string]$_.id -eq $gateId } | Select-Object -First 1)
        if ($gate.Count -eq 1) {
            $actualArguments = @($gate[0].arguments | ForEach-Object { [string]$_ })
            $expectedArguments = @($expectedBackendReleaseArguments[$gateId])
            if ($actualArguments.Count -ne $expectedArguments.Count -or @(Compare-Object $expectedArguments $actualArguments -SyncWindow 0).Count -ne 0) { $errors.Add("backend release gate does not execute exact Gate0 arguments: $gateId") }
        }
    }

    $formatGate = @($Registry.gates | Where-Object { [string]$_.id -eq 'OV01-GATE-MOBILE-FORMAT' } | Select-Object -First 1)
    if ($formatGate.Count -eq 1) {
        $formatArguments = @($formatGate[0].arguments | ForEach-Object { [string]$_ })
        $expectedFormatArguments = @('--suppress-analytics', 'format', '--output=none', '--set-exit-if-changed')
        if ($formatArguments.Count -ne $expectedFormatArguments.Count -or @(Compare-Object $expectedFormatArguments $formatArguments -SyncWindow 0).Count -ne 0) { $errors.Add('touched-Dart format gate arguments are not deterministic') }
        $formatEnvironmentValid = $null -ne $formatGate[0].PSObject.Properties['environment'] -and $null -ne $formatGate[0].environment.PSObject.Properties['DART_SUPPRESS_ANALYTICS'] -and [string]$formatGate[0].environment.DART_SUPPRESS_ANALYTICS -eq 'true'
        $formatEvidenceKeysValid = $null -ne $formatGate[0].PSObject.Properties['evidenceEnvironmentKeys'] -and @($formatGate[0].evidenceEnvironmentKeys) -contains 'DART_SUPPRESS_ANALYTICS'
        if (-not $formatEnvironmentValid -or -not $formatEvidenceKeysValid) { $errors.Add('touched-Dart format gate does not retain analytics suppression evidence') }
        $formatDynamicValid = $false
        if ($null -ne $formatGate[0].PSObject.Properties['dynamicArguments']) {
            $dynamic = $formatGate[0].dynamicArguments
            $formatDynamicValid = $null -ne $dynamic.PSObject.Properties['sourceIdentityPathRegex'] -and $null -ne $dynamic.PSObject.Properties['stripPrefix'] -and $null -ne $dynamic.PSObject.Properties['allowEmpty'] -and [string]$dynamic.sourceIdentityPathRegex -eq '^05_Development/CareBridgeMobileApp/.*\.dart$' -and [string]$dynamic.stripPrefix -eq '05_Development/CareBridgeMobileApp/' -and [bool]$dynamic.allowEmpty
        }
        if (-not $formatDynamicValid) { $errors.Add('touched-Dart format gate dynamic input contract is not canonical') }
    }

    $artifactIds = @($Registry.artifacts | ForEach-Object { [string]$_.id })
    Add-Ov01ExactSetErrors -Actual $artifactIds -Expected $expectedArtifacts -Label 'artifact' -Errors $errors
    foreach ($artifact in @($Registry.artifacts)) {
        try { [void](Resolve-Ov01RepoGlob -Root $RepoRoot -RelativePattern ([string]$artifact.path)) } catch { $errors.Add($_.Exception.Message) }
    }
    $backendJarArtifact = @($Registry.artifacts | Where-Object { [string]$_.id -eq 'OV01-ART-BACKEND-JAR' } | Select-Object -First 1)
    if ($backendJarArtifact.Count -eq 1 -and ([string]$backendJarArtifact[0].path -ne '05_Development/CareBridgeAPI/target/backend-0.0.1-SNAPSHOT.jar' -or [bool]$backendJarArtifact[0].allowMany)) {
        $errors.Add('backend JAR artifact contract must resolve the exact canonical package output once')
    }

    return [pscustomobject]@{
        valid = ($errors.Count -eq 0)
        errors = $errors.ToArray()
        scenarioCount = $scenarioIds.Count
        gateCount = $gateIds.Count
        artifactCount = $artifactIds.Count
    }
}

function Get-Ov01SourceIdentity {
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$BaselineHead,
        [Parameter(Mandatory = $true)][string]$TouchedPathsFile,
        [Parameter(Mandatory = $true)][string]$RegistryPath,
        [string[]]$ExcludedRootPaths = @()
    )

    $currentHeadLines = @(& git -C $RepoRoot rev-parse HEAD 2>$null)
    $headExitCode = $LASTEXITCODE
    $currentHead = if ($currentHeadLines.Count -gt 0) { ([string]$currentHeadLines[0]).Trim() } else { '' }
    if ($headExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($currentHead)) { throw "Unable to resolve current Git HEAD (repo=$RepoRoot exit=$headExitCode value=$currentHead)" }
    if ($currentHead -ne $BaselineHead) { throw "Current HEAD $currentHead does not match approved baseline $BaselineHead" }
    if (-not (Test-Path -LiteralPath $TouchedPathsFile -PathType Leaf)) { throw "Touched-path file not found: $TouchedPathsFile" }

    $excludedRelativeRoots = New-Object System.Collections.Generic.List[string]
    foreach ($excluded in @($ExcludedRootPaths)) {
        if ([string]::IsNullOrWhiteSpace([string]$excluded)) { continue }
        $excludedFull = if ([System.IO.Path]::IsPathRooted([string]$excluded)) { [System.IO.Path]::GetFullPath([string]$excluded) } else { Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath ([string]$excluded) }
        $excludedRelativeRoots.Add((ConvertTo-Ov01RelativePath -Root $RepoRoot -Path $excludedFull).TrimEnd('/'))
    }
    $isExcluded = {
        param([string]$Candidate)
        foreach ($excludedRelative in $excludedRelativeRoots) {
            if ($Candidate -ceq $excludedRelative -or $Candidate.StartsWith($excludedRelative + '/', [System.StringComparison]::Ordinal)) { return $true }
        }
        return $false
    }

    $paths = New-Object System.Collections.Generic.List[string]
    foreach ($line in @(Get-Content -LiteralPath $TouchedPathsFile)) {
        $candidate = ([string]$line).Trim().Replace('\', '/')
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and -not $candidate.StartsWith('#') -and -not (& $isExcluded $candidate)) { $paths.Add($candidate) }
    }
    $statusLines = @(& git -C $RepoRoot -c core.quotepath=false status --porcelain=v1 --untracked-files=all)
    if ($LASTEXITCODE -ne 0) { throw 'Unable to read Git dirty state' }
    foreach ($line in $statusLines) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.Length -lt 4) { continue }
        $candidate = $line.Substring(3).Trim()
        if ($candidate.Contains(' -> ')) { $candidate = $candidate.Substring($candidate.LastIndexOf(' -> ') + 4) }
        $candidate = $candidate.Trim('"').Replace('\', '/')
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and -not (& $isExcluded $candidate)) { $paths.Add($candidate) }
    }

    $entries = New-Object System.Collections.Generic.List[object]
    $orderedPaths = [string[]]@($paths | Select-Object -Unique)
    [System.Array]::Sort($orderedPaths, [System.StringComparer]::Ordinal)
    foreach ($relative in $orderedPaths) {
        $absolute = Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath $relative
        if (Test-Path -LiteralPath $absolute -PathType Leaf) {
            $entries.Add([pscustomobject]@{ path = $relative; state = 'present'; sha256 = Get-Ov01Sha256 -Path $absolute })
        } elseif (Test-Path -LiteralPath $absolute -PathType Container) {
            $errors = "Touched path must identify a file, not a directory: $relative"
            throw $errors
        } else {
            $entries.Add([pscustomobject]@{ path = $relative; state = 'missing'; sha256 = $null })
        }
    }
    if ($entries.Count -eq 0) { throw 'Source identity cannot be computed from an empty touched/dirty file set' }

    $registryFull = [System.IO.Path]::GetFullPath($RegistryPath)
    if (-not (Test-Path -LiteralPath $registryFull -PathType Leaf)) { throw "Registry identity file not found: $registryFull" }
    $registryRelativePath = ConvertTo-Ov01RelativePath -Root $RepoRoot -Path $registryFull
    $canonicalRegistryRelativePath = '06_Testing/Automation/ov-01/ov01-scenario-registry.json'
    $registrySha256 = Get-Ov01Sha256 -Path $registryFull
    $registryCanonical = $registryRelativePath -ceq $canonicalRegistryRelativePath

    $identityLines = New-Object System.Collections.Generic.List[string]
    $identityLines.Add("BASELINE_HEAD=$BaselineHead")
    $identityLines.Add("REGISTRY_PATH=$registryRelativePath")
    $identityLines.Add("REGISTRY_SHA256=$registrySha256")
    foreach ($entry in $entries) {
        $hash = if ($null -eq $entry.sha256) { '<MISSING>' } else { $entry.sha256 }
        $identityLines.Add("$($entry.path)`t$($entry.state)`t$hash")
    }
    $canonical = ($identityLines -join "`n") + "`n"
    return [pscustomobject]@{
        baselineHead = $BaselineHead
        currentHead = $currentHead
        compositeSha256 = Get-Ov01StringSha256 -Value $canonical
        canonicalFormat = 'BASELINE_HEAD, registry path/SHA-256, then ordinal-sorted path/state/SHA-256 lines, UTF-8 LF'
        registryRelativePath = $registryRelativePath
        registrySha256 = $registrySha256
        registryCanonical = $registryCanonical
        files = $entries.ToArray()
    }
}

function Test-Ov01SourceIdentityStable {
    param(
        [Parameter(Mandatory = $true)]$Before,
        [Parameter(Mandatory = $true)]$After
    )

    $errors = New-Object System.Collections.Generic.List[string]
    foreach ($field in @('baselineHead', 'currentHead', 'registryRelativePath', 'registrySha256', 'registryCanonical', 'compositeSha256')) {
        if ([string]$Before.$field -cne [string]$After.$field) { $errors.Add("source identity drift: $field") }
    }
    $beforeFiles = @($Before.files | ForEach-Object { "$([string]$_.path)`t$([string]$_.state)`t$([string]$_.sha256)" })
    $afterFiles = @($After.files | ForEach-Object { "$([string]$_.path)`t$([string]$_.state)`t$([string]$_.sha256)" })
    if ($beforeFiles.Count -ne $afterFiles.Count -or @(Compare-Object $beforeFiles $afterFiles -SyncWindow 0).Count -ne 0) {
        $errors.Add('source identity drift: exact files/state/hashes')
    }
    return [pscustomobject]@{ valid = ($errors.Count -eq 0); errors = $errors.ToArray() }
}

function Get-Ov01ObjectSha256 {
    param([Parameter(Mandatory = $true)]$Value)
    return Get-Ov01StringSha256 -Value ($Value | ConvertTo-Json -Compress -Depth 50)
}

function Write-Ov01AtomicJson {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Value
    )
    $full = [System.IO.Path]::GetFullPath($Path)
    $temp = "$full.tmp-$([Guid]::NewGuid().ToString('N'))"
    $backup = "$full.bak-$([Guid]::NewGuid().ToString('N'))"
    [System.IO.File]::WriteAllText($temp, ($Value | ConvertTo-Json -Depth 50) + "`n", (New-Object System.Text.UTF8Encoding($false)))
    if (Test-Path -LiteralPath $full -PathType Leaf) {
        try { [System.IO.File]::Replace($temp, $full, $backup, $true) }
        finally { if (Test-Path -LiteralPath $backup -PathType Leaf) { [System.IO.File]::Delete($backup) } }
    } else {
        [System.IO.File]::Move($temp, $full)
    }
}

function Get-Ov01BytesSha256 {
    param([Parameter(Mandatory = $true)][byte[]]$Bytes)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { return ([System.BitConverter]::ToString($sha.ComputeHash($Bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
}

function Get-Ov01CheckpointKeyPathFingerprint {
    param([Parameter(Mandatory = $true)][string]$Path)
    $canonical = [System.IO.Path]::GetFullPath($Path).TrimEnd('\', '/').Replace('\', '/').ToLowerInvariant()
    return Get-Ov01StringSha256 -Value $canonical
}

function Test-Ov01PathContainedBy {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)][string]$Root)
    $pathFull = [System.IO.Path]::GetFullPath($Path)
    $rootFull = [System.IO.Path]::GetFullPath($Root).TrimEnd('\', '/')
    return $pathFull.Equals($rootFull, [System.StringComparison]::OrdinalIgnoreCase) -or $pathFull.StartsWith($rootFull + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)
}

function Get-Ov01CheckpointAuthenticationContext {
    param(
        [Parameter(Mandatory = $true)][string]$CheckpointKeyPath,
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [switch]$Create
    )
    if (-not [System.IO.Path]::IsPathRooted($CheckpointKeyPath)) { throw 'Checkpoint key path must be absolute' }
    $keyFull = [System.IO.Path]::GetFullPath($CheckpointKeyPath)
    $repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
    $evidenceFull = [System.IO.Path]::GetFullPath($EvidenceDirectory)
    if ((Test-Ov01PathContainedBy -Path $keyFull -Root $repoFull) -or (Test-Ov01PathContainedBy -Path $keyFull -Root $evidenceFull)) { throw 'Checkpoint key path must be outside the repository and evidence directory' }
    if (-not [System.IO.Path]::GetPathRoot($keyFull).Equals([System.IO.Path]::GetPathRoot($evidenceFull), [System.StringComparison]::OrdinalIgnoreCase)) { throw 'Checkpoint key state and evidence directory must share one volume for atomic replacement' }
    $stateDirectory = [System.IO.Path]::GetDirectoryName($keyFull)
    if (-not (Test-Path -LiteralPath $stateDirectory -PathType Container)) { throw 'Checkpoint key state directory does not exist' }
    if (((Get-Item -LiteralPath $stateDirectory).Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) { throw 'Checkpoint key state directory cannot be a reparse point' }
    try { [void][System.Security.Cryptography.ProtectedData] } catch { Add-Type -AssemblyName System.Security }
    if ($Create) {
        if (Test-Path -LiteralPath $keyFull) { throw 'Fresh Release checkpoint key path must not already exist' }
        $keyBytes = New-Object byte[] 32
        $protectedBytes = $null
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($keyBytes) } finally { $rng.Dispose() }
        try {
            $protectedBytes = [System.Security.Cryptography.ProtectedData]::Protect($keyBytes, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
            $keyDocument = [pscustomobject]@{
                schemaVersion = 1
                keyId = [Guid]::NewGuid().ToString('D')
                createdUtc = [DateTimeOffset]::UtcNow.ToString('o')
                protection = 'DPAPI-CurrentUser'
                protectedKeyBase64 = [Convert]::ToBase64String($protectedBytes)
                keyFingerprintSha256 = Get-Ov01BytesSha256 -Bytes $keyBytes
            }
            $keyTemp = Join-Path $stateDirectory ('.ov01-key-create-' + [Guid]::NewGuid().ToString('N') + '.tmp')
            $currentSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
            $security = New-Object System.Security.AccessControl.FileSecurity
            $security.SetAccessRuleProtection($true, $false)
            $rule = New-Object System.Security.AccessControl.FileSystemAccessRule($currentSid, [System.Security.AccessControl.FileSystemRights]::FullControl, [System.Security.AccessControl.AccessControlType]::Allow)
            [void]$security.AddAccessRule($rule)
            $keyStream = New-Object System.IO.FileStream($keyTemp, [System.IO.FileMode]::CreateNew, [System.Security.AccessControl.FileSystemRights]::FullControl, [System.IO.FileShare]::None, 4096, [System.IO.FileOptions]::WriteThrough, $security)
            try {
                $writer = New-Object System.IO.StreamWriter($keyStream, (New-Object System.Text.UTF8Encoding($false)))
                try { $writer.Write(($keyDocument | ConvertTo-Json -Depth 10) + "`n"); $writer.Flush() }
                finally { $writer.Dispose() }
            } finally { if ($null -ne $keyStream) { $keyStream.Dispose() } }
            [System.IO.File]::Move($keyTemp, $keyFull)
        } finally {
            if ($null -ne $protectedBytes) { [Array]::Clear($protectedBytes, 0, $protectedBytes.Length) }
            [Array]::Clear($keyBytes, 0, $keyBytes.Length)
        }
    }
    if (-not (Test-Path -LiteralPath $keyFull -PathType Leaf)) { throw 'Release checkpoint key file is missing' }
    if (((Get-Item -LiteralPath $keyFull).Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) { throw 'Checkpoint key file cannot be a reparse point' }
    $keyDocument = Get-Content -Encoding UTF8 -Raw -LiteralPath $keyFull | ConvertFrom-Json
    $expectedKeyProps = @('schemaVersion', 'keyId', 'createdUtc', 'protection', 'protectedKeyBase64', 'keyFingerprintSha256') | Sort-Object
    if (@(Compare-Object $expectedKeyProps @($keyDocument.PSObject.Properties.Name | Sort-Object) -SyncWindow 0).Count -ne 0) { throw 'Checkpoint key file fields are not exact' }
    if ([int]$keyDocument.schemaVersion -ne 1 -or [string]$keyDocument.protection -cne 'DPAPI-CurrentUser' -or [string]$keyDocument.keyId -notmatch '^[0-9a-fA-F-]{36}$') { throw 'Checkpoint key file metadata is invalid' }
    $currentSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl = [System.IO.File]::GetAccessControl($keyFull)
    if (-not $acl.AreAccessRulesProtected) { throw 'Checkpoint key ACL inheritance is not disabled' }
    $accessRules = @($acl.GetAccessRules($true, $true, [System.Security.Principal.SecurityIdentifier]))
    $foreignAllows = @($accessRules | Where-Object { $_.AccessControlType -eq [System.Security.AccessControl.AccessControlType]::Allow -and -not $_.IdentityReference.Equals($currentSid) })
    $currentAllows = @($accessRules | Where-Object { $_.AccessControlType -eq [System.Security.AccessControl.AccessControlType]::Allow -and $_.IdentityReference.Equals($currentSid) })
    if ($foreignAllows.Count -ne 0 -or $currentAllows.Count -eq 0) { throw 'Checkpoint key ACL is not restricted to the current Windows user' }
    try {
        $protectedBytes = [Convert]::FromBase64String([string]$keyDocument.protectedKeyBase64)
        $keyBytes = [System.Security.Cryptography.ProtectedData]::Unprotect($protectedBytes, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    } catch { throw 'Checkpoint key cannot be unprotected by the current Windows user' }
    if ($keyBytes.Length -ne 32 -or (Get-Ov01BytesSha256 -Bytes $keyBytes) -cne [string]$keyDocument.keyFingerprintSha256) { [Array]::Clear($keyBytes, 0, $keyBytes.Length); throw 'Checkpoint key fingerprint is invalid' }
    $attestation = [pscustomobject]@{
        scheme = 'HMAC-SHA256'
        protection = 'DPAPI-CurrentUser'
        keyId = [string]$keyDocument.keyId
        keyFingerprintSha256 = [string]$keyDocument.keyFingerprintSha256
        keyPathFingerprintSha256 = Get-Ov01CheckpointKeyPathFingerprint -Path $keyFull
        keyFileSha256 = Get-Ov01Sha256 -Path $keyFull
    }
    return [pscustomobject]@{ keyPath = $keyFull; stateDirectory = $stateDirectory; keyBytes = $keyBytes; attestation = $attestation }
}

function Get-Ov01CheckpointHmacSha256 {
    param([Parameter(Mandatory = $true)]$Checkpoint, [Parameter(Mandatory = $true)][byte[]]$KeyBytes)
    $payload = [ordered]@{}
    foreach ($property in $Checkpoint.PSObject.Properties) { if ([string]$property.Name -cne 'checkpointHmacSha256') { $payload[[string]$property.Name] = $property.Value } }
    $payloadBytes = [System.Text.Encoding]::UTF8.GetBytes(([pscustomobject]$payload | ConvertTo-Json -Compress -Depth 50))
    $hmac = New-Object System.Security.Cryptography.HMACSHA256(,$KeyBytes)
    try { return ([System.BitConverter]::ToString($hmac.ComputeHash($payloadBytes))).Replace('-', '').ToLowerInvariant() }
    finally { $hmac.Dispose() }
}

function Test-Ov01FixedTimeHexEqual {
    param([AllowEmptyString()][string]$Left, [AllowEmptyString()][string]$Right)
    if ($Left -notmatch '^[0-9a-fA-F]{64}$' -or $Right -notmatch '^[0-9a-fA-F]{64}$') { return $false }
    $difference = 0
    for ($i = 0; $i -lt 64; $i++) { $difference = $difference -bor ([int][char]$Left[$i] -bxor [int][char]$Right[$i]) }
    return $difference -eq 0
}

function Write-Ov01AuthenticatedCheckpoint {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Value,
        [Parameter(Mandatory = $true)]$AuthenticationContext
    )
    if ($null -eq $Value.PSObject.Properties['authentication']) { $Value | Add-Member -NotePropertyName authentication -NotePropertyValue $AuthenticationContext.attestation }
    else { $Value.authentication = $AuthenticationContext.attestation }
    if ($null -eq $Value.PSObject.Properties['checkpointHmacSha256']) { $Value | Add-Member -NotePropertyName checkpointHmacSha256 -NotePropertyValue '' }
    $Value.checkpointHmacSha256 = Get-Ov01CheckpointHmacSha256 -Checkpoint $Value -KeyBytes $AuthenticationContext.keyBytes
    $full = [System.IO.Path]::GetFullPath($Path)
    $prefix = '.ov01-checkpoint-' + [string]$AuthenticationContext.attestation.keyId
    $temp = Join-Path $AuthenticationContext.stateDirectory ($prefix + '.tmp-' + [Guid]::NewGuid().ToString('N'))
    $currentSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
    $security = New-Object System.Security.AccessControl.FileSecurity
    $security.SetAccessRuleProtection($true, $false)
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule($currentSid, [System.Security.AccessControl.FileSystemRights]::FullControl, [System.Security.AccessControl.AccessControlType]::Allow)
    [void]$security.AddAccessRule($rule)
    $tempStream = New-Object System.IO.FileStream($temp, [System.IO.FileMode]::CreateNew, [System.Security.AccessControl.FileSystemRights]::FullControl, [System.IO.FileShare]::None, 4096, [System.IO.FileOptions]::WriteThrough, $security)
    try {
        $writer = New-Object System.IO.StreamWriter($tempStream, (New-Object System.Text.UTF8Encoding($false)))
        try { $writer.Write(($Value | ConvertTo-Json -Depth 50) + "`n"); $writer.Flush(); $tempStream.Flush($true) }
        finally { $writer.Dispose() }
    } finally { if ($null -ne $tempStream) { $tempStream.Dispose() } }
    if ($null -eq ('Ov01NativeAtomicFile' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class Ov01NativeAtomicFile {
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern bool MoveFileEx(string existingFileName, string newFileName, int flags);
}
'@
    }
    $moveFileReplaceExisting = 0x1
    $moveFileWriteThrough = 0x8
    if (-not [Ov01NativeAtomicFile]::MoveFileEx($temp, $full, ($moveFileReplaceExisting -bor $moveFileWriteThrough))) {
        $nativeError = [Runtime.InteropServices.Marshal]::GetLastWin32Error()
        throw "Authenticated checkpoint atomic replacement failed: Win32=$nativeError"
    }
}

function Get-Ov01AuthenticatedCheckpointArtifactInventory {
    param([Parameter(Mandatory = $true)]$AuthenticationContext)
    $prefix = '.ov01-checkpoint-' + [string]$AuthenticationContext.attestation.keyId
    $exactPattern = '^' + [regex]::Escape($prefix) + '\.(?:tmp|bak)-[0-9a-f]{32}$'
    $records = foreach ($file in @(Get-ChildItem -LiteralPath $AuthenticationContext.stateDirectory -File | Where-Object { $_.Name -match $exactPattern } | Sort-Object Name)) {
        [pscustomobject]@{ name = [string]$file.Name; kind = if ($file.Name -match '\.tmp-') { 'tmp' } else { 'bak' }; bytes = [int64]$file.Length; sha256 = Get-Ov01Sha256 -Path $file.FullName }
    }
    return [pscustomobject]@{ valid = $true; orphanCount = @($records).Count; orphanArtifacts = @($records); errors = @() }
}

function Get-Ov01CheckpointPropertyNames {
    return @('schemaVersion', 'runnerId', 'runMode', 'outputDirectory', 'createdUtc', 'updatedUtc', 'preRunSourceIdentity', 'gateOrder', 'manualEvidence', 'completedGateResults', 'activeGate', 'artifactRecords', 'evidenceFiles', 'authentication', 'checkpointHmacSha256')
}

function Get-Ov01CheckpointGatePropertyNames {
    return @('id', 'resultSha256', 'result')
}

function Get-Ov01GateResultPropertyNames {
    return @('id', 'platform', 'kind', 'status', 'exactCommand', 'effectiveArguments', 'workingDirectory', 'startedUtc', 'durationMs', 'timeoutSeconds', 'timedOut', 'childTreeTerminated', 'exitCode', 'primaryExitCode', 'fallbackUsed', 'notApplicable', 'notApplicableReason', 'dynamicInputs', 'totals', 'surefireFreshReportCount', 'selectorResults', 'skipDisposition', 'forbiddenOutcomeDetected', 'toolchain', 'environmentEvidence', 'runtimeIsolation', 'logPath', 'logBytes', 'logSha256')
}

function Get-Ov01ActiveGatePropertyNames {
    return @('id', 'stage', 'attempt', 'activatedUtc', 'logPath', 'runtimeIsolationPrefix', 'buildArtifactIds')
}

function New-Ov01ActiveGateState {
    param(
        [Parameter(Mandatory = $true)]$Gate,
        [Parameter(Mandatory = $true)]$Registry,
        [Parameter(Mandatory = $true)][ValidateRange(1, 1000)][int]$Attempt
    )
    $gateId = [string]$Gate.id
    $runtimePrefix = if ($null -ne $Gate.PSObject.Properties['pytestIsolation'] -and [bool]$Gate.pytestIsolation) { "runtime/pytest/$gateId/attempt-$Attempt/" } else { '' }
    [string[]]$buildArtifactIds = @($Registry.artifacts | Where-Object { [string]$_.kind -eq 'build' -and @($_.requiredForGateIds) -contains $gateId } | ForEach-Object { [string]$_.id })
    return [pscustomobject]@{
        id = $gateId
        stage = 'executing'
        attempt = $Attempt
        activatedUtc = [DateTimeOffset]::UtcNow.ToString('o')
        logPath = "logs/$gateId.log"
        runtimeIsolationPrefix = $runtimePrefix
        buildArtifactIds = $buildArtifactIds
    }
}

function Get-Ov01EvidenceFileRecords {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [string[]]$ExcludedRelativePaths = @('release-checkpoint.json'),
        [string[]]$ExcludedRelativePrefixes = @()
    )
    $root = [System.IO.Path]::GetFullPath($EvidenceDirectory)
    $records = foreach ($file in @(Get-ChildItem -LiteralPath $root -Recurse -File | Sort-Object FullName)) {
        $relative = ConvertTo-Ov01RelativePath -Root $root -Path $file.FullName
        if (@($ExcludedRelativePaths) -contains $relative) { continue }
        if (@($ExcludedRelativePrefixes | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and $relative.StartsWith([string]$_, [System.StringComparison]::OrdinalIgnoreCase) }).Count -gt 0) { continue }
        [pscustomobject]@{ path = $relative; bytes = [int64]$file.Length; sha256 = Get-Ov01Sha256 -Path $file.FullName }
    }
    return @($records)
}

function Test-Ov01GateSemanticReplay {
    param(
        [Parameter(Mandatory = $true)]$Gate,
        [Parameter(Mandatory = $true)]$Result,
        [Parameter(Mandatory = $true)]$Registry,
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)]$SourceIdentity,
        [Parameter(Mandatory = $true)][string[]]$SelectedScenarioIds
    )
    $errors = New-Object System.Collections.Generic.List[string]
    $gateId = [string]$Gate.id
    $workingDirectory = Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath ([string]$Gate.workingDirectory)
    $configuredExecutable = Resolve-Ov01GateExecutable -RepoRoot $RepoRoot -WorkingDirectory $workingDirectory -Executable ([string]$Gate.executable)
    $effectiveExecutable = $configuredExecutable
    [string[]]$expectedArguments = @($Gate.arguments | ForEach-Object { [string]$_ })
    $flutterTool = $null
    if ($null -ne $Gate.PSObject.Properties['runnerTool'] -and [string]$Gate.runnerTool -eq 'flutter-dart-snapshot') {
        $flutterTool = Resolve-Ov01FlutterDartTool -ConfiguredFlutterExecutable $configuredExecutable
        $effectiveExecutable = [string]$flutterTool.dartExecutable
        $expectedArguments = @([string]$flutterTool.flutterSnapshot) + $expectedArguments
    }

    [string[]]$expectedDynamicInputs = @()
    $expectedNotApplicable = $false
    $expectedNotApplicableReason = $null
    if ($null -ne $Gate.PSObject.Properties['dynamicArguments']) {
        $sourceRegex = [string]$Gate.dynamicArguments.sourceIdentityPathRegex
        $stripPrefix = [string]$Gate.dynamicArguments.stripPrefix
        $expectedDynamicInputs = @($SourceIdentity.files | Where-Object { [string]$_.state -eq 'present' -and [regex]::IsMatch([string]$_.path, $sourceRegex) } | ForEach-Object {
            $relative = [string]$_.path
            if (-not $relative.StartsWith($stripPrefix, [System.StringComparison]::Ordinal)) { throw "Semantic replay dynamic input prefix mismatch: $relative" }
            $relative.Substring($stripPrefix.Length).Replace('\', '/')
        } | Select-Object -Unique)
        [System.Array]::Sort($expectedDynamicInputs, [System.StringComparer]::Ordinal)
        if ($expectedDynamicInputs.Count -eq 0 -and [bool]$Gate.dynamicArguments.allowEmpty) {
            $expectedNotApplicable = $true
            $expectedNotApplicableReason = 'NO_TOUCHED_DART_FILES'
        } else { $expectedArguments += $expectedDynamicInputs }
    }

    $expectedRuntimeIsolation = $null
    if ($null -ne $Gate.PSObject.Properties['pytestIsolation'] -and [bool]$Gate.pytestIsolation) {
        if ($null -eq $Result.runtimeIsolation) {
            $errors.Add("semantic replay runtime isolation is missing: $gateId")
        } else {
            $relativeRoot = [string]$Result.runtimeIsolation.relativeRoot
            $escapedGateId = [regex]::Escape($gateId)
            if ($relativeRoot -notmatch "^runtime/pytest/$escapedGateId(?:/attempt-[1-9][0-9]*)?$") {
                $errors.Add("semantic replay runtime isolation path is not canonical: $gateId")
            } else {
                $isolationRoot = [System.IO.Path]::GetFullPath((Join-Path $EvidenceDirectory $relativeRoot))
                $expectedRuntimeIsolation = [pscustomobject]@{
                    kind = 'pytest'
                    root = $isolationRoot
                    relativeRoot = $relativeRoot
                    osTemp = Join-Path $isolationRoot 'os-temp'
                    baseTemp = Join-Path $isolationRoot 'basetemp'
                    cacheDir = Join-Path $isolationRoot 'cache'
                    environmentKeys = @('TMPDIR', 'TEMP', 'TMP')
                }
                $expectedArguments += @('--basetemp', $expectedRuntimeIsolation.baseTemp, '-o', "cache_dir=$($expectedRuntimeIsolation.cacheDir)")
            }
        }
    } elseif ($null -ne $Result.runtimeIsolation) { $errors.Add("semantic replay unexpected runtime isolation: $gateId") }

    foreach ($binding in @(
        [pscustomobject]@{ name = 'platform'; expected = [string]$Gate.platform; actual = [string]$Result.platform },
        [pscustomobject]@{ name = 'kind'; expected = [string]$Gate.kind; actual = [string]$Result.kind },
        [pscustomobject]@{ name = 'workingDirectory'; expected = [string]$Gate.workingDirectory; actual = [string]$Result.workingDirectory },
        [pscustomobject]@{ name = 'timeoutSeconds'; expected = [string]$Gate.timeoutSeconds; actual = [string]$Result.timeoutSeconds }
    )) { if ($binding.actual -cne $binding.expected) { $errors.Add("semantic replay registry binding mismatch: $gateId/$($binding.name)") } }
    if ((@($expectedArguments) | ConvertTo-Json -Compress) -cne (@($Result.effectiveArguments) | ConvertTo-Json -Compress)) { $errors.Add("semantic replay effective arguments mismatch: $gateId") }
    if ((@($expectedDynamicInputs) | ConvertTo-Json -Compress) -cne (@($Result.dynamicInputs) | ConvertTo-Json -Compress)) { $errors.Add("semantic replay dynamic inputs mismatch: $gateId") }
    if ([bool]$Result.notApplicable -ne $expectedNotApplicable -or [string]$Result.notApplicableReason -cne [string]$expectedNotApplicableReason) { $errors.Add("semantic replay not-applicable binding mismatch: $gateId") }
    if (($expectedRuntimeIsolation | ConvertTo-Json -Compress -Depth 10) -cne ($Result.runtimeIsolation | ConvertTo-Json -Compress -Depth 10)) { $errors.Add("semantic replay runtime isolation mismatch: $gateId") }

    $expectedCommand = Format-Ov01Command -Executable $effectiveExecutable -Arguments $expectedArguments
    $expectedToolExecutable = $effectiveExecutable
    $expectedToolConfiguration = $Gate
    if ([bool]$Result.fallbackUsed) {
        if ($null -eq $Gate.PSObject.Properties['fallback']) { $errors.Add("semantic replay unexpected fallback: $gateId") }
        else {
            $fallbackExecutable = Resolve-Ov01GateExecutable -RepoRoot $RepoRoot -WorkingDirectory $workingDirectory -Executable ([string]$Gate.fallback.executable)
            [string[]]$fallbackArguments = if ($null -ne $Gate.fallback.PSObject.Properties['arguments']) { @($Gate.fallback.arguments | ForEach-Object { [string]$_ }) } else { @($expectedArguments) }
            $expectedCommand += ' || FALLBACK ' + (Format-Ov01Command -Executable $fallbackExecutable -Arguments $fallbackArguments)
            $expectedToolExecutable = $fallbackExecutable
            $expectedToolConfiguration = $Gate.fallback
        }
    } elseif ([int]$Result.primaryExitCode -ne [int]$Result.exitCode) { $errors.Add("semantic replay primary exit code mismatch without fallback: $gateId") }
    if ([string]$Result.exactCommand -cne $expectedCommand) { $errors.Add("semantic replay exact command mismatch: $gateId") }

    if ($null -eq $Result.toolchain) { $errors.Add("semantic replay toolchain is missing: $gateId") }
    else {
        [string[]]$expectedVersionArguments = @($expectedToolConfiguration.versionArguments | ForEach-Object { [string]$_ })
        if ($null -ne $flutterTool) { $expectedVersionArguments = @([string]$flutterTool.flutterSnapshot) + $expectedVersionArguments }
        if ([string]$Result.toolchain.resolvedExecutable -cne [string]$expectedToolExecutable) { $errors.Add("semantic replay toolchain executable mismatch: $gateId") }
        elseif (-not (Test-Path -LiteralPath $expectedToolExecutable -PathType Leaf) -or [string]$Result.toolchain.executableSha256 -cne (Get-Ov01Sha256 -Path $expectedToolExecutable)) { $errors.Add("semantic replay toolchain executable SHA mismatch: $gateId") }
        if ((@($Result.toolchain.versionArguments) | ConvertTo-Json -Compress) -cne (@($expectedVersionArguments) | ConvertTo-Json -Compress)) { $errors.Add("semantic replay toolchain version arguments mismatch: $gateId") }
        if ([string]$Result.toolchain.expectedVersionRegex -cne [string]$expectedToolConfiguration.expectedVersionRegex) { $errors.Add("semantic replay toolchain version regex mismatch: $gateId") }
        $normalizedVersionOutput = Normalize-Ov01VersionOutput -Text ([string]$Result.toolchain.versionOutput)
        if ([string]$Result.toolchain.versionOutputSha256 -cne (Get-Ov01StringSha256 -Value $normalizedVersionOutput)) { $errors.Add("semantic replay toolchain version output digest mismatch: $gateId") }
        $expectedVersionMatched = [int]$Result.toolchain.versionExitCode -eq 0 -and [regex]::IsMatch($normalizedVersionOutput, [string]$expectedToolConfiguration.expectedVersionRegex)
        if ([bool]$Result.toolchain.versionMatched -ne $expectedVersionMatched) { $errors.Add("semantic replay toolchain version verdict mismatch: $gateId") }
    }

    $expectedEnvironment = Get-Ov01GateEnvironment -Gate $Gate
    $expectedEnvironmentEvidence = [ordered]@{}
    if ($null -ne $Gate.PSObject.Properties['evidenceEnvironmentKeys']) {
        foreach ($key in @($Gate.evidenceEnvironmentKeys)) { if ($expectedEnvironment.ContainsKey([string]$key)) { $expectedEnvironmentEvidence[[string]$key] = [string]$expectedEnvironment[[string]$key] } }
    }
    if (([pscustomobject]$expectedEnvironmentEvidence | ConvertTo-Json -Compress) -cne ($Result.environmentEvidence | ConvertTo-Json -Compress)) { $errors.Add("semantic replay environment evidence mismatch: $gateId") }

    $logFull = Resolve-Ov01RepoPath -Root $EvidenceDirectory -RelativePath ([string]$Result.logPath)
    $retainedLog = Get-Content -Encoding UTF8 -Raw -LiteralPath $logFull
    $commandOutput = $retainedLog
    $surefireFreshReportCount = 0
    $surefireSelectors = @()
    $actualSkippedTests = @()
    if ([string]$Gate.parser -eq 'maven-surefire') {
        $markerMatches = [regex]::Matches($retainedLog, '(?m)^OV01_SUREFIRE_EVIDENCE\s+(\{[^\r\n]+\})\r?$')
        if ($markerMatches.Count -ne 1) { $errors.Add("semantic replay retained Surefire evidence marker count mismatch: $gateId") }
        else {
            try {
                $retainedSurefire = $markerMatches[0].Groups[1].Value | ConvertFrom-Json
                $surefireFreshReportCount = [int]$retainedSurefire.freshReportCount
                $surefireSelectors = @($retainedSurefire.selectorIdentities | ForEach-Object { [string]$_ })
                $actualSkippedTests = @($retainedSurefire.skippedTests)
            } catch { $errors.Add("semantic replay retained Surefire evidence is invalid: $gateId") }
        }
        $commandOutput = [regex]::Replace($retainedLog, '(?m)^OV01_SUREFIRE_EVIDENCE\s+\{[^\r\n]+\}\r?\n?', '')
    }
    $replayedTotals = Get-Ov01CommandTotals -Parser ([string]$Gate.parser) -Output $commandOutput
    if (($replayedTotals | ConvertTo-Json -Compress) -cne ($Result.totals | ConvertTo-Json -Compress)) { $errors.Add("semantic replay totals mismatch: $gateId") }
    if ([int]$Result.surefireFreshReportCount -ne $surefireFreshReportCount) { $errors.Add("semantic replay Surefire report count mismatch: $gateId") }
    $replayedSkipDisposition = Get-Ov01SkipDisposition -Gate $Gate -ActualSkippedTests $actualSkippedTests -ReportedSkipped ([int]$replayedTotals.skipped) -RepoRoot $RepoRoot
    if (($replayedSkipDisposition | ConvertTo-Json -Compress -Depth 20) -cne ($Result.skipDisposition | ConvertTo-Json -Compress -Depth 20)) { $errors.Add("semantic replay skipped-test disposition mismatch: $gateId") }

    $selectorCorpus = $commandOutput + "`n" + ($surefireSelectors -join "`n")
    $expectedSelectorResults = New-Object System.Collections.Generic.List[object]
    foreach ($scenario in @($Registry.scenarios | Where-Object { $SelectedScenarioIds -contains [string]$_.id })) {
        foreach ($selector in @($scenario.executableSelectors | Where-Object { [string]$_.gateId -eq $gateId })) {
            $selectorText = [string]$selector.selector
            $expectedSelectorResults.Add([pscustomobject]@{ scenarioId = [string]$scenario.id; gateId = $gateId; selector = $selectorText; matched = (-not [string]::IsNullOrWhiteSpace($selectorText) -and $selectorCorpus.IndexOf($selectorText, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) })
        }
    }
    if (($expectedSelectorResults.ToArray() | ConvertTo-Json -Compress -Depth 10) -cne (@($Result.selectorResults) | ConvertTo-Json -Compress -Depth 10)) { $errors.Add("semantic replay selector results mismatch: $gateId") }
    $allSelectorsMatched = @($expectedSelectorResults | Where-Object { -not [bool]$_.matched }).Count -eq 0
    $nonSkipNonPass = [int]$replayedTotals.failures + [int]$replayedTotals.errors + [int]$replayedTotals.xfailed + [int]$replayedTotals.xpassed + [int]$replayedTotals.deselected
    $replayedForbidden = ($nonSkipNonPass -gt 0) -or -not [bool]$replayedSkipDisposition.valid -or [regex]::IsMatch($commandOutput, '(?im)^\s*(?:FIXME|PENDING|DISABLED|XFAIL|XPASS)\b|\b(?:status|outcome)\s*[:=]\s*(?:FIXME|PENDING|DISABLED|XFAIL|XPASS)\b')
    if ([bool]$Result.forbiddenOutcomeDetected -ne $replayedForbidden) { $errors.Add("semantic replay forbidden outcome mismatch: $gateId") }
    $testContractMet = if ([string]$Gate.kind -eq 'test') { [bool]$replayedTotals.parsed -and [int]$replayedTotals.tests -ge [int]$Gate.expectedMinTests } else { [bool]$replayedTotals.parsed }
    $surefireValid = [string]$Gate.parser -ne 'maven-surefire' -or $surefireFreshReportCount -gt 0
    $replayedStatus = if (-not [bool]$Result.timedOut -and [int]$Result.exitCode -eq 0 -and [int]$replayedTotals.failures -eq 0 -and [int]$replayedTotals.errors -eq 0 -and -not $replayedForbidden -and $testContractMet -and $surefireValid -and $allSelectorsMatched -and [bool]$Result.toolchain.versionMatched) { 'PASS' } else { 'FAIL' }
    if ([string]$Result.status -cne $replayedStatus) { $errors.Add("semantic replay gate status mismatch: $gateId") }
    return [pscustomobject]@{ valid = ($errors.Count -eq 0); errors = $errors.ToArray() }
}

function Test-Ov01ReleaseCheckpoint {
    param(
        [Parameter(Mandatory = $true)][string]$CheckpointPath,
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)]$ExpectedSourceIdentity,
        [Parameter(Mandatory = $true)]$ExpectedManualEvidence,
        [Parameter(Mandatory = $true)]$Registry,
        [Parameter(Mandatory = $true)][string]$CheckpointKeyPath,
        [switch]$AllowFinalArtifacts
    )
    $errors = New-Object System.Collections.Generic.List[string]
    if (-not (Test-Path -LiteralPath $CheckpointPath -PathType Leaf)) { return [pscustomobject]@{ valid = $false; errors = @('release checkpoint is missing'); checkpoint = $null } }
    try { $checkpoint = Get-Content -Encoding UTF8 -Raw -LiteralPath $CheckpointPath | ConvertFrom-Json }
    catch { return [pscustomobject]@{ valid = $false; errors = @("release checkpoint JSON is invalid: $($_.Exception.Message)"); checkpoint = $null } }
    try {
        $authenticationContext = Get-Ov01CheckpointAuthenticationContext -CheckpointKeyPath $CheckpointKeyPath -RepoRoot $RepoRoot -EvidenceDirectory $EvidenceDirectory
        $reportedAttestationJson = $checkpoint.authentication | ConvertTo-Json -Compress -Depth 10
        $expectedAttestationJson = $authenticationContext.attestation | ConvertTo-Json -Compress -Depth 10
        $expectedHmac = Get-Ov01CheckpointHmacSha256 -Checkpoint $checkpoint -KeyBytes $authenticationContext.keyBytes
        $authenticationValid = $reportedAttestationJson -ceq $expectedAttestationJson -and (Test-Ov01FixedTimeHexEqual -Left ([string]$checkpoint.checkpointHmacSha256) -Right $expectedHmac)
        [Array]::Clear($authenticationContext.keyBytes, 0, $authenticationContext.keyBytes.Length)
        if (-not $authenticationValid) { return [pscustomobject]@{ valid = $false; errors = @('release checkpoint external authentication failed'); checkpoint = $null } }
    } catch { return [pscustomobject]@{ valid = $false; errors = @("release checkpoint external authentication failed: $($_.Exception.Message)"); checkpoint = $null } }
    $actualRootProps = @($checkpoint.PSObject.Properties.Name | Sort-Object)
    $expectedRootProps = @(Get-Ov01CheckpointPropertyNames | Sort-Object)
    $rootPropertyDiff = @(Compare-Object $expectedRootProps $actualRootProps -SyncWindow 0)
    if ($rootPropertyDiff.Count -ne 0) {
        $errors.Add('release checkpoint root fields are not exact')
        if (@($rootPropertyDiff | Where-Object SideIndicator -eq '<=').Count -gt 0) {
            return [pscustomobject]@{ valid = $false; errors = $errors.ToArray(); checkpoint = $checkpoint }
        }
    }
    if ([int]$checkpoint.schemaVersion -ne 1) { $errors.Add('release checkpoint schemaVersion must be 1') }
    if ([string]$checkpoint.runnerId -ne 'OV01-AUTO-002' -or [string]$checkpoint.runMode -ne 'Release') { $errors.Add('release checkpoint runner or mode is invalid') }
    $evidenceFull = [System.IO.Path]::GetFullPath($EvidenceDirectory)
    $expectedOutputRelative = ConvertTo-Ov01RelativePath -Root $RepoRoot -Path $evidenceFull
    if ([string]$checkpoint.outputDirectory -cne $expectedOutputRelative) { $errors.Add('release checkpoint output directory binding mismatch') }
    foreach ($field in @('createdUtc', 'updatedUtc')) { try { [void][DateTimeOffset]::Parse([string]$checkpoint.$field) } catch { $errors.Add("release checkpoint timestamp is invalid: $field") } }
    $expectedGateOrder = @(Get-Ov01CanonicalReleaseGateIds)
    [string[]]$actualGateOrder = @($checkpoint.gateOrder | ForEach-Object { [string]$_ })
    if ($actualGateOrder.Count -ne 17 -or @(Compare-Object $expectedGateOrder $actualGateOrder -SyncWindow 0).Count -ne 0) { $errors.Add('release checkpoint gate order is not the exact 17-gate catalog') }
    $sourceProps = @($checkpoint.preRunSourceIdentity.PSObject.Properties.Name | Sort-Object)
    $expectedSourceProps = @($ExpectedSourceIdentity.PSObject.Properties.Name | Sort-Object)
    if (@(Compare-Object $expectedSourceProps $sourceProps -SyncWindow 0).Count -ne 0) { $errors.Add('release checkpoint source identity fields are not exact') }
    $sourceCheck = Test-Ov01SourceIdentityStable -Before $ExpectedSourceIdentity -After $checkpoint.preRunSourceIdentity
    foreach ($sourceError in @($sourceCheck.errors)) { $errors.Add("release checkpoint source identity mismatch: $sourceError") }
    $manualJson = $checkpoint.manualEvidence | ConvertTo-Json -Compress -Depth 30
    $expectedManualJson = $ExpectedManualEvidence.record | ConvertTo-Json -Compress -Depth 30
    if ($manualJson -cne $expectedManualJson) { $errors.Add('release checkpoint manual evidence binding mismatch') }
    $completed = @($checkpoint.completedGateResults)
    if ($completed.Count -gt 17) { $errors.Add('release checkpoint has too many completed gates') }
    for ($i = 0; $i -lt $completed.Count; $i++) {
        $wrapper = $completed[$i]
        $wrapperProps = @($wrapper.PSObject.Properties.Name | Sort-Object)
        if (@(Compare-Object (Get-Ov01CheckpointGatePropertyNames | Sort-Object) $wrapperProps -SyncWindow 0).Count -ne 0) { $errors.Add("release checkpoint gate wrapper fields are not exact: index=$i"); continue }
        if ([string]$wrapper.id -cne $expectedGateOrder[$i]) { $errors.Add("release checkpoint completed gate order mismatch: index=$i") }
        $resultProps = @($wrapper.result.PSObject.Properties.Name | Sort-Object)
        if (@(Compare-Object (Get-Ov01GateResultPropertyNames | Sort-Object) $resultProps -SyncWindow 0).Count -ne 0) { $errors.Add("release checkpoint gate result fields are not exact: $($wrapper.id)") }
        if ([string]$wrapper.result.id -cne [string]$wrapper.id) { $errors.Add("release checkpoint gate result ID mismatch: $($wrapper.id)") }
        if ([string]$wrapper.resultSha256 -cne (Get-Ov01ObjectSha256 -Value $wrapper.result)) { $errors.Add("release checkpoint gate result digest mismatch: $($wrapper.id)") }
        $expectedLogPath = "logs/$([string]$wrapper.id).log"
        if ([string]$wrapper.result.logPath -cne $expectedLogPath) { $errors.Add("release checkpoint gate log path is not canonical: $($wrapper.id)") }
        $logReplayable = $false
        try {
            $logFull = Resolve-Ov01RepoPath -Root $evidenceFull -RelativePath ([string]$wrapper.result.logPath)
            if (-not (Test-Path -LiteralPath $logFull -PathType Leaf)) { $errors.Add("release checkpoint gate log is missing: $($wrapper.id)") }
            else {
                $logReplayable = $true
                if ([int64]$wrapper.result.logBytes -ne [int64](Get-Item -LiteralPath $logFull).Length) { $errors.Add("release checkpoint gate log bytes mismatch: $($wrapper.id)") }
                if ([string]$wrapper.result.logSha256 -cne (Get-Ov01Sha256 -Path $logFull)) { $errors.Add("release checkpoint gate log SHA-256 mismatch: $($wrapper.id)") }
            }
        } catch { $errors.Add("release checkpoint gate log escapes output: $($wrapper.id)") }
        if ($logReplayable) {
            try {
                $gateDefinition = @($Registry.gates | Where-Object { [string]$_.id -eq [string]$wrapper.id })
                if ($gateDefinition.Count -ne 1) { $errors.Add("release checkpoint semantic replay gate definition mismatch: $($wrapper.id)") }
                else {
                    $semanticReplay = Test-Ov01GateSemanticReplay -Gate $gateDefinition[0] -Result $wrapper.result -Registry $Registry -RepoRoot $RepoRoot -EvidenceDirectory $evidenceFull -SourceIdentity $ExpectedSourceIdentity -SelectedScenarioIds @($Registry.scenarios | ForEach-Object { [string]$_.id })
                    foreach ($semanticError in @($semanticReplay.errors)) { $errors.Add("release checkpoint $semanticError") }
                }
            } catch { $errors.Add("release checkpoint semantic replay failed: $($wrapper.id)/$($_.Exception.Message)") }
        }
    }
    $completedGateIds = @($completed | ForEach-Object { [string]$_.id })
    $activeGate = $checkpoint.activeGate
    $activeExcludedPaths = @()
    $activeExcludedPrefixes = @()
    if ($null -ne $activeGate) {
        $activeProps = @($activeGate.PSObject.Properties.Name | Sort-Object)
        if (@(Compare-Object (Get-Ov01ActiveGatePropertyNames | Sort-Object) $activeProps -SyncWindow 0).Count -ne 0) {
            $errors.Add('release checkpoint active gate fields are not exact')
        } else {
            $expectedActiveId = if ($completed.Count -lt $expectedGateOrder.Count) { $expectedGateOrder[$completed.Count] } else { '' }
            if ([string]$activeGate.id -cne $expectedActiveId) { $errors.Add('release checkpoint active gate is not the canonical incomplete next gate') }
            if ([string]$activeGate.stage -cne 'executing') { $errors.Add('release checkpoint active gate stage is invalid') }
            if ([int]$activeGate.attempt -lt 1) { $errors.Add('release checkpoint active gate attempt is invalid') }
            try {
                $activeTimestamp = [DateTimeOffset]::Parse([string]$activeGate.activatedUtc)
                if ($activeTimestamp.Offset -ne [TimeSpan]::Zero) { throw 'not UTC' }
            } catch { $errors.Add('release checkpoint active gate timestamp is invalid') }
            if ([string]$activeGate.logPath -cne "logs/$expectedActiveId.log") { $errors.Add('release checkpoint active gate log path is invalid') }
            $activeDefinition = @($Registry.gates | Where-Object { [string]$_.id -eq $expectedActiveId })
            if ($activeDefinition.Count -eq 1) {
                $expectedRuntimePrefix = if ($null -ne $activeDefinition[0].PSObject.Properties['pytestIsolation'] -and [bool]$activeDefinition[0].pytestIsolation) { "runtime/pytest/$expectedActiveId/attempt-$([int]$activeGate.attempt)/" } else { '' }
                if ([string]$activeGate.runtimeIsolationPrefix -cne $expectedRuntimePrefix) { $errors.Add('release checkpoint active gate runtime isolation prefix is invalid') }
                [string[]]$expectedBuildIds = @($Registry.artifacts | Where-Object { [string]$_.kind -eq 'build' -and @($_.requiredForGateIds) -contains $expectedActiveId } | ForEach-Object { [string]$_.id })
                [string[]]$actualBuildIds = @($activeGate.buildArtifactIds | ForEach-Object { [string]$_ })
                if ($actualBuildIds.Count -ne $expectedBuildIds.Count -or @(Compare-Object $expectedBuildIds $actualBuildIds -SyncWindow 0).Count -ne 0) { $errors.Add('release checkpoint active gate build artifact binding is invalid') }
            }
            $activeExcludedPaths = @([string]$activeGate.logPath)
            if (-not [string]::IsNullOrWhiteSpace([string]$activeGate.runtimeIsolationPrefix)) { $activeExcludedPrefixes = @([string]$activeGate.runtimeIsolationPrefix) }
        }
    }
    $actualArtifactResult = Get-Ov01ArtifactRecords -Registry $Registry -RepoRoot $RepoRoot -SelectedGateIds $completedGateIds -SourceIdentity $ExpectedSourceIdentity
    foreach ($artifactError in @($actualArtifactResult.errors)) { $errors.Add("release checkpoint artifact invalid: $artifactError") }
    $checkpointArtifactJson = @($checkpoint.artifactRecords) | ConvertTo-Json -Compress -Depth 20
    $actualArtifactJson = @($actualArtifactResult.records) | ConvertTo-Json -Compress -Depth 20
    if ($checkpointArtifactJson -cne $actualArtifactJson) { $errors.Add('release checkpoint artifact records/filesystem mismatch') }
    $excludedEvidencePaths = @('release-checkpoint.json')
    if ($AllowFinalArtifacts) { $excludedEvidencePaths += @('run-report.json', 'validation.json', 'evidence-manifest.json') }
    $excludedEvidencePaths += $activeExcludedPaths
    $actualEvidenceFiles = @(Get-Ov01EvidenceFileRecords -EvidenceDirectory $evidenceFull -ExcludedRelativePaths $excludedEvidencePaths -ExcludedRelativePrefixes $activeExcludedPrefixes)
    $checkpointEvidenceJson = @($checkpoint.evidenceFiles) | ConvertTo-Json -Compress -Depth 20
    $actualEvidenceJson = $actualEvidenceFiles | ConvertTo-Json -Compress -Depth 20
    if ($checkpointEvidenceJson -cne $actualEvidenceJson) { $errors.Add('release checkpoint evidence file closed set mismatch') }
    return [pscustomobject]@{ valid = ($errors.Count -eq 0); errors = $errors.ToArray(); checkpoint = $checkpoint }
}

function Get-Ov01CommandTotals {
    param(
        [Parameter(Mandatory = $true)][string]$Parser,
        [AllowEmptyString()][string]$Output
    )

    $plain = [regex]::Replace($Output, "`e\[[\d;]*[A-Za-z]", '')
    $tests = 0; $failures = 0; $errors = 0; $skipped = 0
    $xfailed = 0; $xpassed = 0; $deselected = 0; $parsed = $false
    switch ($Parser) {
        'maven-surefire' {
            $matches = [regex]::Matches($plain, 'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)', 'IgnoreCase')
            if ($matches.Count -gt 0) {
                $m = $matches[$matches.Count - 1]; $tests = [int]$m.Groups[1].Value; $failures = [int]$m.Groups[2].Value; $errors = [int]$m.Groups[3].Value; $skipped = [int]$m.Groups[4].Value; $parsed = $true
            }
        }
        'flutter' {
            $matches = [regex]::Matches($plain, '\+(\d+)(?:\s+~(\d+))?:\s+All tests passed!', 'IgnoreCase')
            if ($matches.Count -gt 0) { $m = $matches[$matches.Count - 1]; $tests = [int]$m.Groups[1].Value; if ($m.Groups[2].Success) { $skipped = [int]$m.Groups[2].Value }; $parsed = $true }
        }
        'vitest' {
            $m = [regex]::Match($plain, 'Tests\s+(\d+)\s+passed(?:\s*\|\s*(\d+)\s+skipped)?', 'IgnoreCase')
            if ($m.Success) { $tests = [int]$m.Groups[1].Value; if ($m.Groups[2].Success) { $skipped = [int]$m.Groups[2].Value }; $parsed = $true }
        }
        'pytest' {
            $outcomes = [ordered]@{
                passed = 0; failed = 0; errors = 0; skipped = 0; xfailed = 0; xpassed = 0; deselected = 0
            }
            foreach ($outcome in @('passed', 'failed', 'errors', 'skipped', 'xfailed', 'xpassed', 'deselected')) {
                $word = if ($outcome -eq 'errors') { 'errors?' } else { [regex]::Escape($outcome) }
                $matches = [regex]::Matches($plain, "(\d+)\s+$word", 'IgnoreCase')
                if ($matches.Count -gt 0) {
                    $outcomes[$outcome] = [int]$matches[$matches.Count - 1].Groups[1].Value
                    $parsed = $true
                }
            }
            $failures = [int]$outcomes.failed
            $errors = [int]$outcomes.errors
            $skipped = [int]$outcomes.skipped
            $xfailed = [int]$outcomes.xfailed
            $xpassed = [int]$outcomes.xpassed
            $deselected = [int]$outcomes.deselected
            $tests = [int]$outcomes.passed + $failures + $errors + $skipped + $xfailed + $xpassed
        }
        'ov01-json-line' {
            $m = [regex]::Match($plain, 'OV01_RESULT\s+(\{[^\r\n]+\})')
            if ($m.Success) { $result = $m.Groups[1].Value | ConvertFrom-Json; $tests = [int]$result.tests; $failures = [int]$result.failures; $errors = [int]$result.errors; $skipped = [int]$result.skipped; $parsed = $true }
        }
        'none' { $parsed = $true }
        default { throw "Unknown output parser: $Parser" }
    }
    $nonPass = $failures + $errors + $skipped + $xfailed + $xpassed + $deselected
    return [pscustomobject]@{
        parsed = $parsed
        tests = $tests
        failures = $failures
        errors = $errors
        skipped = $skipped
        xfailed = $xfailed
        xpassed = $xpassed
        deselected = $deselected
        nonPass = $nonPass
    }
}

function Get-Ov01SurefireXmlEvidence {
    param([Parameter(Mandatory = $true)][string]$Path)

    $selectors = New-Object System.Collections.Generic.List[string]
    $skippedTests = New-Object System.Collections.Generic.List[object]
    try {
        $xmlText = Protect-Ov01Text -Text (Get-Content -Raw -LiteralPath $Path)
        [xml]$surefireXml = $xmlText
        foreach ($testcase in @($surefireXml.testsuite.testcase)) {
            $className = if ($null -ne $testcase.PSObject.Properties['classname']) { [string]$testcase.classname } else { '' }
            $methodName = if ($null -ne $testcase.PSObject.Properties['name']) { [string]$testcase.name } else { '' }
            $skippedProperty = $testcase.PSObject.Properties['skipped']
            if ($null -ne $skippedProperty -and $null -ne $skippedProperty.Value) {
                $skippedNode = $skippedProperty.Value
                $messageProperty = $skippedNode.PSObject.Properties['message']
                $skipReason = if ($null -ne $messageProperty) { [string]$messageProperty.Value } else { [string]$skippedNode.InnerText }
                $skippedTests.Add([pscustomobject]@{ selector = "$className#$methodName"; reason = $skipReason })
            }
            if (-not [string]::IsNullOrWhiteSpace($className) -and -not [string]::IsNullOrWhiteSpace($methodName)) {
                $simpleClassName = $className.Substring($className.LastIndexOf('.') + 1)
                $selectors.Add("${className}::$methodName")
                $selectors.Add("${simpleClassName}::$methodName")
            }
        }
        return [pscustomobject]@{
            parsed = $true
            selectorIdentities = $selectors.ToArray()
            skippedTests = $skippedTests.ToArray()
            error = $null
        }
    } catch {
        return [pscustomobject]@{
            parsed = $false
            selectorIdentities = @()
            skippedTests = @()
            error = $_.Exception.GetType().Name
        }
    }
}

function Resolve-Ov01GateExecutable {
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory,
        [Parameter(Mandatory = $true)][string]$Executable
    )

    if ($Executable -eq '${MAVEN_PINNED_3_9_16}') {
        $candidatePaths = New-Object System.Collections.Generic.List[string]
        $pathCommand = Get-Command 'mvn.cmd' -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($null -ne $pathCommand -and -not [string]::IsNullOrWhiteSpace([string]$pathCommand.Source)) {
            $candidatePaths.Add([System.IO.Path]::GetFullPath([string]$pathCommand.Source))
        }
        $userProfile = [System.Environment]::GetEnvironmentVariable('USERPROFILE')
        if (-not [string]::IsNullOrWhiteSpace($userProfile)) {
            $distributionRoot = Join-Path $userProfile '.m2\wrapper\dists\apache-maven-3.9.16'
            if (Test-Path -LiteralPath $distributionRoot -PathType Container) {
                foreach ($candidate in @(Get-ChildItem -LiteralPath $distributionRoot -Filter 'mvn.cmd' -File -Recurse -ErrorAction SilentlyContinue)) {
                    $candidatePaths.Add([System.IO.Path]::GetFullPath($candidate.FullName))
                }
            }
        }
        [string[]]$orderedCandidates = @($candidatePaths | Select-Object -Unique)
        [System.Array]::Sort($orderedCandidates, [System.StringComparer]::OrdinalIgnoreCase)
        foreach ($candidate in $orderedCandidates) {
            if ($candidate -match '(?i)CodexSandboxOffline' -or -not (Test-Path -LiteralPath $candidate -PathType Leaf)) { continue }
            try {
                $version = Invoke-Ov01Process -Executable $candidate -Arguments @('-v') -WorkingDirectory $WorkingDirectory
                if ([int]$version.exitCode -eq 0 -and [regex]::IsMatch([string]$version.output, '(?m)^Apache Maven 3\.9\.16\b')) { return $candidate }
            } catch { continue }
        }
        throw 'Unable to locate and execute the pinned Apache Maven 3.9.16 binary'
    }
    if ([System.IO.Path]::IsPathRooted($Executable)) { return [System.IO.Path]::GetFullPath($Executable) }
    if ($Executable.StartsWith('./') -or $Executable.StartsWith('.\')) {
        return [System.IO.Path]::GetFullPath((Join-Path $WorkingDirectory $Executable.Substring(2)))
    }
    if ($Executable.Contains('/') -or $Executable.Contains('\')) {
        return Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath $Executable
    }
    $workingDirectoryCandidate = Join-Path $WorkingDirectory $Executable
    if (Test-Path -LiteralPath $workingDirectoryCandidate -PathType Leaf) {
        return [System.IO.Path]::GetFullPath($workingDirectoryCandidate)
    }
    $command = Get-Command $Executable -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -ne $command -and -not [string]::IsNullOrWhiteSpace([string]$command.Source)) { return [System.IO.Path]::GetFullPath([string]$command.Source) }
    return $Executable
}

function Resolve-Ov01FlutterDartTool {
    param([Parameter(Mandatory = $true)][string]$ConfiguredFlutterExecutable)

    $configuredFull = [System.IO.Path]::GetFullPath($ConfiguredFlutterExecutable)
    if (-not (Test-Path -LiteralPath $configuredFull -PathType Leaf)) { throw "Configured Flutter launcher does not exist: $configuredFull" }
    if ([System.IO.Path]::GetFileName($configuredFull) -ine 'flutter.bat') { throw "Configured Flutter launcher must be flutter.bat: $configuredFull" }
    $binDirectory = [System.IO.Path]::GetDirectoryName($configuredFull)
    $flutterRoot = [System.IO.Path]::GetFullPath((Join-Path $binDirectory '..'))
    $dartExecutable = [System.IO.Path]::GetFullPath((Join-Path $flutterRoot 'bin\cache\dart-sdk\bin\dart.exe'))
    $flutterSnapshot = [System.IO.Path]::GetFullPath((Join-Path $flutterRoot 'bin\cache\flutter_tools.snapshot'))
    if (-not (Test-Path -LiteralPath $dartExecutable -PathType Leaf)) { throw "Flutter Dart executable does not exist: $dartExecutable" }
    if (-not (Test-Path -LiteralPath $flutterSnapshot -PathType Leaf)) { throw "Flutter tool snapshot does not exist: $flutterSnapshot" }
    return [pscustomobject]@{
        runnerTool = 'flutter-dart-snapshot'
        configuredFlutterExecutable = $configuredFull
        configuredFlutterExecutableSha256 = Get-Ov01Sha256 -Path $configuredFull
        flutterRoot = $flutterRoot
        dartExecutable = $dartExecutable
        dartExecutableSha256 = Get-Ov01Sha256 -Path $dartExecutable
        flutterSnapshot = $flutterSnapshot
        flutterSnapshotSha256 = Get-Ov01Sha256 -Path $flutterSnapshot
    }
}

function Get-Ov01ExecutableIdentity {
    param(
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $true)]$Configuration,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory,
        [hashtable]$Environment = @{},
        [ValidateRange(1, 3600)][int]$TimeoutSeconds = 120
    )

    [string[]]$versionArguments = @(if ($null -ne $Configuration.PSObject.Properties['versionArguments']) { $Configuration.versionArguments | ForEach-Object { [string]$_ } })
    $expectedRegex = if ($null -ne $Configuration.PSObject.Properties['expectedVersionRegex']) { [string]$Configuration.expectedVersionRegex } else { '' }
    $versionExitCode = $null
    $versionOutput = ''
    $versionMatched = $true
    if ($versionArguments.Count -gt 0) {
        try {
            $version = Invoke-Ov01Process -Executable $Executable -Arguments $versionArguments -WorkingDirectory $WorkingDirectory -Environment $Environment -TimeoutSeconds $TimeoutSeconds
            $versionExitCode = [int]$version.exitCode
            $versionOutput = Protect-Ov01Text -Text ([string]$version.output)
            if (-not [string]::IsNullOrWhiteSpace($expectedRegex)) { $versionMatched = $versionExitCode -eq 0 -and [regex]::IsMatch($versionOutput, $expectedRegex) }
        } catch {
            $versionExitCode = 9009
            $versionOutput = Protect-Ov01Text -Text $_.Exception.Message
            $versionMatched = $false
        }
    }
    $sha = if (Test-Path -LiteralPath $Executable -PathType Leaf) { Get-Ov01Sha256 -Path $Executable } else { $null }
    return [pscustomobject]@{
        resolvedExecutable = $Executable
        executableSha256 = $sha
        versionArguments = $versionArguments
        versionExitCode = $versionExitCode
        versionOutput = $versionOutput
        versionOutputSha256 = Get-Ov01StringSha256 -Value (Normalize-Ov01VersionOutput -Text $versionOutput)
        expectedVersionRegex = $expectedRegex
        versionMatched = $versionMatched
    }
}

function Get-Ov01GateEnvironment {
    param($Gate, [hashtable]$Base = @{})

    $environment = @{}
    foreach ($entry in $Base.GetEnumerator()) { $environment[$entry.Key] = [string]$entry.Value }
    if ($null -ne $Gate -and $null -ne $Gate.PSObject.Properties['environment']) {
        foreach ($property in $Gate.environment.PSObject.Properties) {
            $environment[[string]$property.Name] = [string]$property.Value
        }
    }
    return $environment
}

function Get-Ov01FlutterVersionEnvironment {
    param(
        [hashtable]$Base = @{},
        [Parameter(Mandatory = $true)][string]$StateDirectory
    )

    $environment = @{}
    foreach ($entry in $Base.GetEnumerator()) { $environment[[string]$entry.Key] = [string]$entry.Value }
    $environment.APPDATA = $StateDirectory
    $environment.CI = 'true'
    $environment.BOT = 'true'
    $environment.FLUTTER_SUPPRESS_ANALYTICS = 'true'
    $environment.FLUTTER_NO_VERSION_CHECK = 'true'
    # Direct snapshot invocation intentionally bypasses flutter.bat; this flag
    # prevents a lockfile write while retaining the exact SDK/tool identity.
    $environment.FLUTTER_ALREADY_LOCKED = 'true'
    return $environment
}

function Get-Ov01FlutterVersionEnvironmentEvidence {
    return [pscustomobject][ordered]@{
        APPDATA = '<ISOLATED_TEMP>'
        CI = 'true'
        BOT = 'true'
        FLUTTER_SUPPRESS_ANALYTICS = 'true'
        FLUTTER_NO_VERSION_CHECK = 'true'
        FLUTTER_ALREADY_LOCKED = 'true'
    }
}

function ConvertTo-Ov01ProcessArgument {
    param([AllowEmptyString()][string]$Value)

    if ($null -eq $Value -or $Value.Length -eq 0) { return '""' }
    if ($Value -notmatch '[\s"]') { return $Value }
    $builder = New-Object System.Text.StringBuilder
    [void]$builder.Append('"')
    $backslashes = 0
    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') {
            $backslashes++
        } elseif ($character -eq '"') {
            [void]$builder.Append(('\' * (($backslashes * 2) + 1)))
            [void]$builder.Append('"')
            $backslashes = 0
        } else {
            if ($backslashes -gt 0) { [void]$builder.Append(('\' * $backslashes)); $backslashes = 0 }
            [void]$builder.Append($character)
        }
    }
    if ($backslashes -gt 0) { [void]$builder.Append(('\' * ($backslashes * 2))) }
    [void]$builder.Append('"')
    return $builder.ToString()
}

function Stop-Ov01WindowsProcessTree {
    param([Parameter(Mandatory = $true)][int]$RootProcessId)

    if ($null -eq ('Ov01ProcessTreeTerminator' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;

public static class Ov01ProcessTreeTerminator
{
    private const uint TH32CS_SNAPPROCESS = 0x00000002;
    private static readonly IntPtr INVALID_HANDLE_VALUE = new IntPtr(-1);

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
    private struct PROCESSENTRY32
    {
        public uint dwSize;
        public uint cntUsage;
        public uint th32ProcessID;
        public IntPtr th32DefaultHeapID;
        public uint th32ModuleID;
        public uint cntThreads;
        public uint th32ParentProcessID;
        public int pcPriClassBase;
        public uint dwFlags;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 260)]
        public string szExeFile;
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern IntPtr CreateToolhelp32Snapshot(uint flags, uint processId);

    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Auto)]
    private static extern bool Process32First(IntPtr snapshot, ref PROCESSENTRY32 entry);

    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Auto)]
    private static extern bool Process32Next(IntPtr snapshot, ref PROCESSENTRY32 entry);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool CloseHandle(IntPtr handle);

    private static List<int> SnapshotDescendants(int rootProcessId)
    {
        var children = new Dictionary<int, List<int>>();
        IntPtr snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
        if (snapshot == INVALID_HANDLE_VALUE) return new List<int>();
        try
        {
            var entry = new PROCESSENTRY32();
            entry.dwSize = (uint)Marshal.SizeOf(typeof(PROCESSENTRY32));
            if (Process32First(snapshot, ref entry))
            {
                do
                {
                    int parent = unchecked((int)entry.th32ParentProcessID);
                    int child = unchecked((int)entry.th32ProcessID);
                    List<int> values;
                    if (!children.TryGetValue(parent, out values))
                    {
                        values = new List<int>();
                        children[parent] = values;
                    }
                    values.Add(child);
                    entry.dwSize = (uint)Marshal.SizeOf(typeof(PROCESSENTRY32));
                }
                while (Process32Next(snapshot, ref entry));
            }
        }
        finally
        {
            CloseHandle(snapshot);
        }

        var result = new List<int>();
        var queue = new Queue<int>();
        queue.Enqueue(rootProcessId);
        while (queue.Count > 0)
        {
            int parent = queue.Dequeue();
            List<int> values;
            if (!children.TryGetValue(parent, out values)) continue;
            foreach (int child in values)
            {
                if (child == rootProcessId || result.Contains(child)) continue;
                result.Add(child);
                queue.Enqueue(child);
            }
        }
        return result;
    }

    private static bool IsRunning(int processId)
    {
        try
        {
            using (Process process = Process.GetProcessById(processId))
            {
                return !process.HasExited;
            }
        }
        catch (ArgumentException) { return false; }
        catch (InvalidOperationException) { return false; }
    }

    public static bool KillTree(int rootProcessId, int waitMilliseconds)
    {
        var processIds = SnapshotDescendants(rootProcessId);
        var orderedProcessIds = processIds.AsEnumerable().Reverse().ToList();
        orderedProcessIds.Add(rootProcessId);
        foreach (int processId in orderedProcessIds)
        {
            try
            {
                using (Process process = Process.GetProcessById(processId))
                {
                    if (!process.HasExited) process.Kill();
                }
            }
            catch (ArgumentException) { }
            catch (InvalidOperationException) { }
        }

        var deadline = DateTime.UtcNow.AddMilliseconds(waitMilliseconds);
        while (DateTime.UtcNow < deadline)
        {
            if (!orderedProcessIds.Any(IsRunning)) return true;
            Thread.Sleep(50);
        }
        return !orderedProcessIds.Any(IsRunning);
    }
}
'@
    }
    return [Ov01ProcessTreeTerminator]::KillTree($RootProcessId, 5000)
}

function ConvertTo-Ov01NormalizedEnvironmentEntries {
    param([Parameter(Mandatory = $true)][object[]]$Entries)
    $normalizedInput = @($Entries | ForEach-Object {
        [pscustomobject]@{ name = [string]$_.Key; value = [string]$_.Value; folded = ([string]$_.Key).ToUpperInvariant() }
    })
    $result = New-Object System.Collections.Generic.List[object]
    foreach ($group in @($normalizedInput | Group-Object folded)) {
        $ordered = @($group.Group | Sort-Object @{ Expression = {
            if ([string]$_.folded -eq 'PATH' -and [string]$_.name -ceq 'Path') { 0 }
            elseif ([string]$_.folded -eq 'PATH' -and [string]$_.name -ceq 'PATH') { 1 }
            else { 2 }
        } }, @{ Expression = { [string]$_.name } })
        $canonicalName = if ([string]$group.Name -eq 'PATH') { 'Path' } else { [string]$ordered[0].name }
        $result.Add([pscustomobject]@{ name = $canonicalName; value = [string]$ordered[0].value; folded = [string]$group.Name })
    }
    return @($result.ToArray() | Sort-Object folded)
}

function Repair-Ov01ProcessEnvironmentCaseCollisions {
    $rawEntries = @([System.Environment]::GetEnvironmentVariables([System.EnvironmentVariableTarget]::Process).GetEnumerator())
    $normalized = @(ConvertTo-Ov01NormalizedEnvironmentEntries -Entries $rawEntries)
    foreach ($entry in $rawEntries) { [System.Environment]::SetEnvironmentVariable([string]$entry.Key, $null, [System.EnvironmentVariableTarget]::Process) }
    foreach ($entry in $normalized) { [System.Environment]::SetEnvironmentVariable([string]$entry.name, [string]$entry.value, [System.EnvironmentVariableTarget]::Process) }
}

function Invoke-Ov01Process {
    param(
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory,
        [hashtable]$Environment = @{},
        [ValidateRange(1, 3600)][int]$TimeoutSeconds = 120
    )

    Repair-Ov01ProcessEnvironmentCaseCollisions
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.WorkingDirectory = $WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $isWindowsPlatform = [System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT
    $internalEnvironment = @{}
    $usesReservedBatchEnvironment = $false
    if ($isWindowsPlatform -and @('.cmd', '.bat') -contains [System.IO.Path]::GetExtension($Executable).ToLowerInvariant()) {
        if ([regex]::IsMatch($Executable, "[\x00\r\n]")) { throw 'Windows batch executable path contains a forbidden control character' }
        for ($argumentIndex = 0; $argumentIndex -lt $Arguments.Count; $argumentIndex++) {
            $argumentValue = [string]$Arguments[$argumentIndex]
            if ([regex]::IsMatch($argumentValue, "[\x00\r\n]")) {
                throw "Windows batch argument contains a forbidden control character at index $argumentIndex"
            }
            if ($argumentValue -match '"' -and [regex]::IsMatch($argumentValue, '[&|<>()\^\s]')) {
                throw "Windows batch argument combines an embedded quote with a CMD separator at index $argumentIndex"
            }
            $internalEnvironment['OV01_CMD_ARG_{0:d4}' -f $argumentIndex] = $argumentValue
        }
        $internalEnvironment.OV01_CMD_TARGET = $Executable
        $usesReservedBatchEnvironment = $true
        $startInfo.FileName = $env:ComSpec
        $argumentReferences = @()
        if ($Arguments.Count -gt 0) {
            $argumentReferences = @(0..($Arguments.Count - 1) | ForEach-Object { '"%OV01_CMD_ARG_{0:d4}%"' -f $_ })
        }
        $batchCommand = @('"%OV01_CMD_TARGET%"') + $argumentReferences
        # The target batch is the final command, so CALL's second expansion pass is deliberately avoided.
        # Values arrive through one-pass environment expansion inside quotes; metacharacters introduced by
        # expansion are data, while /v:off keeps literal exclamation marks from delayed expansion.
        $startInfo.Arguments = '/d /v:off /s /c "' + ($batchCommand -join ' ') + '"'
    } else {
        $startInfo.FileName = $Executable
        $startInfo.Arguments = (@($Arguments | ForEach-Object { ConvertTo-Ov01ProcessArgument -Value ([string]$_) }) -join ' ')
    }
    foreach ($entry in $Environment.GetEnumerator()) { $startInfo.EnvironmentVariables[[string]$entry.Key] = [string]$entry.Value }
    if ($usesReservedBatchEnvironment) {
        foreach ($entry in @($startInfo.EnvironmentVariables.GetEnumerator())) {
            $key = [string]$entry.Key
            if ($key -eq 'OV01_CMD_TARGET' -or $key -match '^OV01_CMD_ARG_\d{4,}$') { $startInfo.EnvironmentVariables.Remove($key) }
        }
    }
    foreach ($entry in $internalEnvironment.GetEnumerator()) { $startInfo.EnvironmentVariables[[string]$entry.Key] = [string]$entry.Value }

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    try {
        if (-not $process.Start()) { throw "Unable to start executable: $Executable" }
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        $timeoutMilliseconds = [int][Math]::Min([int64]$TimeoutSeconds * 1000L, [int]::MaxValue)
        $timedOut = -not $process.WaitForExit($timeoutMilliseconds)
        $terminationSucceeded = $true
        if ($timedOut) {
            $terminationSucceeded = $false
            if ($isWindowsPlatform -and -not $process.HasExited) {
                try { $terminationSucceeded = Stop-Ov01WindowsProcessTree -RootProcessId $process.Id } catch { $terminationSucceeded = $false }
            }
            if ($isWindowsPlatform -and -not $terminationSucceeded -and -not $process.HasExited) {
                $taskkill = New-Object System.Diagnostics.Process
                $taskkill.StartInfo = New-Object System.Diagnostics.ProcessStartInfo
                $taskkill.StartInfo.FileName = Join-Path $env:SystemRoot 'System32\taskkill.exe'
                $taskkill.StartInfo.Arguments = "/PID $($process.Id) /T /F"
                $taskkill.StartInfo.UseShellExecute = $false
                $taskkill.StartInfo.CreateNoWindow = $true
                try {
                    if ($taskkill.Start()) {
                        if ($taskkill.WaitForExit(15000)) {
                            $terminationSucceeded = [int]$taskkill.ExitCode -eq 0
                        } elseif (-not $taskkill.HasExited) {
                            $taskkill.Kill()
                        }
                    }
                } finally {
                    $taskkill.Dispose()
                }
            }
            if (-not $process.HasExited) {
                try { $process.Kill() } catch { }
            }
            $parentExited = $process.WaitForExit(5000)
            $terminationSucceeded = $terminationSucceeded -and $parentExited
        }
        try {
            [void][System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]@($stdout, $stderr), 2000)
        } catch {
            # A killed process may close redirected handles abruptly; timeout evidence remains authoritative.
        }
        $stdoutText = if ($stdout.IsCompleted -and -not $stdout.IsFaulted) { $stdout.GetAwaiter().GetResult().TrimEnd("`r", "`n") } else { '' }
        $stderrText = if ($stderr.IsCompleted -and -not $stderr.IsFaulted) { $stderr.GetAwaiter().GetResult().TrimEnd("`r", "`n") } else { '' }
        $parts = @($stdoutText, $stderrText | Where-Object { -not [string]::IsNullOrEmpty($_) })
        if ($timedOut) {
            $parts += "OV01_PROCESS_TIMEOUT timeoutSeconds=$TimeoutSeconds childTreeTerminated=$terminationSucceeded"
        }
        return [pscustomobject]@{
            exitCode = if ($timedOut) { 124 } else { [int]$process.ExitCode }
            output = ($parts | Where-Object { -not [string]::IsNullOrEmpty($_) }) -join "`n"
            timedOut = $timedOut
            timeoutSeconds = $TimeoutSeconds
            childTreeTerminated = $terminationSucceeded
        }
    } finally {
        $process.Dispose()
    }
}

function Format-Ov01Command {
    param([string]$Executable, [string[]]$Arguments)
    $displayParts = @($Executable) + @($Arguments | ForEach-Object { if ($_ -match '\s') { '"' + $_.Replace('"', '\"') + '"' } else { $_ } })
    return $displayParts -join ' '
}

function Invoke-Ov01Gate {
    param(
        [Parameter(Mandatory = $true)]$Gate,
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$LogDirectory,
        [AllowEmptyCollection()][object[]]$ExecutableSelectors = @(),
        $SourceIdentity,
        [ValidateRange(0, 1000)][int]$RuntimeIsolationAttempt = 0
    )

    $workingDirectory = Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath ([string]$Gate.workingDirectory)
    $executable = Resolve-Ov01GateExecutable -RepoRoot $RepoRoot -WorkingDirectory $workingDirectory -Executable ([string]$Gate.executable)
    $arguments = @($Gate.arguments | ForEach-Object { [string]$_ })
    $flutterDartTool = $null
    if ($null -ne $Gate.PSObject.Properties['runnerTool'] -and [string]$Gate.runnerTool -eq 'flutter-dart-snapshot') {
        $flutterDartTool = Resolve-Ov01FlutterDartTool -ConfiguredFlutterExecutable $executable
        $executable = [string]$flutterDartTool.dartExecutable
        $arguments = @([string]$flutterDartTool.flutterSnapshot) + $arguments
    }
    $dynamicInputs = @()
    $notApplicable = $false
    $notApplicableReason = $null
    if ($null -ne $Gate.PSObject.Properties['dynamicArguments']) {
        if ($null -eq $SourceIdentity) { throw "Source identity is required for dynamic gate: $($Gate.id)" }
        $sourceRegex = [string]$Gate.dynamicArguments.sourceIdentityPathRegex
        $stripPrefix = [string]$Gate.dynamicArguments.stripPrefix
        [string[]]$dynamicInputs = @($SourceIdentity.files | Where-Object { [string]$_.state -eq 'present' -and [regex]::IsMatch([string]$_.path, $sourceRegex) } | ForEach-Object {
            $relative = [string]$_.path
            if (-not $relative.StartsWith($stripPrefix, [System.StringComparison]::Ordinal)) { throw "Dynamic input does not have required prefix: $relative" }
            $gateRelative = $relative.Substring($stripPrefix.Length)
            $absolute = Resolve-Ov01RepoPath -Root $workingDirectory -RelativePath $gateRelative
            if (-not (Test-Path -LiteralPath $absolute -PathType Leaf)) { throw "Dynamic input is not a present file: $relative" }
            $gateRelative.Replace('\', '/')
        } | Select-Object -Unique)
        [System.Array]::Sort($dynamicInputs, [System.StringComparer]::Ordinal)
        if ($dynamicInputs.Count -eq 0 -and [bool]$Gate.dynamicArguments.allowEmpty) {
            $notApplicable = $true
            $notApplicableReason = 'NO_TOUCHED_DART_FILES'
        } else {
            $arguments += $dynamicInputs
        }
    }
    $environment = Get-Ov01GateEnvironment -Gate $Gate
    $runtimeIsolation = $null
    if ($null -ne $Gate.PSObject.Properties['pytestIsolation'] -and [bool]$Gate.pytestIsolation) {
        $evidenceRoot = [System.IO.Path]::GetFullPath((Join-Path $LogDirectory '..'))
        $isolationRelativeRoot = if ($RuntimeIsolationAttempt -gt 0) {
            'runtime/pytest/' + [string]$Gate.id + '/attempt-' + $RuntimeIsolationAttempt
        } else { 'runtime/pytest/' + [string]$Gate.id }
        $isolationRoot = [System.IO.Path]::GetFullPath((Join-Path $evidenceRoot $isolationRelativeRoot))
        $expectedIsolationPrefix = $evidenceRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
        if (-not $isolationRoot.StartsWith($expectedIsolationPrefix, [System.StringComparison]::OrdinalIgnoreCase)) { throw "Pytest isolation escaped the evidence root: $($Gate.id)" }
        if (Test-Path -LiteralPath $isolationRoot) { throw "Pytest isolation root already exists: $isolationRoot" }
        $osTemp = Join-Path $isolationRoot 'os-temp'
        $baseTemp = Join-Path $isolationRoot 'basetemp'
        $cacheDirectory = Join-Path $isolationRoot 'cache'
        [void](New-Item -ItemType Directory -Path $osTemp -Force)
        foreach ($key in @('TMPDIR', 'TEMP', 'TMP')) { $environment[$key] = $osTemp }
        $arguments += @('--basetemp', $baseTemp, '-o', "cache_dir=$cacheDirectory")
        $runtimeIsolation = [pscustomobject]@{
            kind = 'pytest'
            root = $isolationRoot
            relativeRoot = $isolationRelativeRoot
            osTemp = $osTemp
            baseTemp = $baseTemp
            cacheDir = $cacheDirectory
            environmentKeys = @('TMPDIR', 'TEMP', 'TMP')
        }
    }
    $exactCommand = Format-Ov01Command -Executable $executable -Arguments $arguments
    $startedUtc = [DateTime]::UtcNow
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    $exitCode = 9009
    $primaryExitCode = 9009
    $fallbackUsed = $false
    $rawOutput = ''
    $actualExecutable = $executable
    $actualConfiguration = $Gate
    $timeoutSeconds = if ($null -ne $Gate.PSObject.Properties['timeoutSeconds']) { [int]$Gate.timeoutSeconds } else { 120 }
    $timedOut = $false
    $childTreeTerminated = $true
    try {
        if ($notApplicable) {
            $primaryExitCode = 0
            $exitCode = 0
            $rawOutput = $notApplicableReason
        } else {
            $primary = Invoke-Ov01Process -Executable $executable -Arguments $arguments -WorkingDirectory $workingDirectory -Environment $environment -TimeoutSeconds $timeoutSeconds
            $primaryExitCode = [int]$primary.exitCode
            $exitCode = $primaryExitCode
            $rawOutput = [string]$primary.output
            $timedOut = [bool]$primary.timedOut
            $childTreeTerminated = [bool]$primary.childTreeTerminated
        }

        if (-not $notApplicable -and -not $timedOut -and $exitCode -ne 0 -and $null -ne $Gate.PSObject.Properties['fallback']) {
            $trigger = [string]$Gate.fallback.whenOutputMatches
            if (-not [string]::IsNullOrWhiteSpace($trigger) -and [regex]::IsMatch($rawOutput, $trigger)) {
                $fallbackExecutable = Resolve-Ov01GateExecutable -RepoRoot $RepoRoot -WorkingDirectory $workingDirectory -Executable ([string]$Gate.fallback.executable)
                $fallbackArguments = if ($null -ne $Gate.fallback.PSObject.Properties['arguments']) {
                    @($Gate.fallback.arguments | ForEach-Object { [string]$_ })
                } else {
                    @($arguments)
                }
                $fallbackEnvironment = Get-Ov01GateEnvironment -Gate $Gate.fallback -Base $environment
                $fallback = Invoke-Ov01Process -Executable $fallbackExecutable -Arguments $fallbackArguments -WorkingDirectory $workingDirectory -Environment $fallbackEnvironment -TimeoutSeconds $timeoutSeconds
                $fallbackUsed = $true
                $actualExecutable = $fallbackExecutable
                $actualConfiguration = $Gate.fallback
                $exitCode = [int]$fallback.exitCode
                $timedOut = [bool]$fallback.timedOut
                $childTreeTerminated = [bool]$fallback.childTreeTerminated
                $rawOutput = "PRIMARY_WRAPPER_EXIT=$primaryExitCode`n$rawOutput`nFALLBACK_COMMAND=$(Format-Ov01Command -Executable $fallbackExecutable -Arguments $fallbackArguments)`n$($fallback.output)"
                $exactCommand += ' || FALLBACK ' + (Format-Ov01Command -Executable $fallbackExecutable -Arguments $fallbackArguments)
            }
        }
    } catch {
        $rawOutput = $_.Exception.Message
        $exitCode = 9009
    } finally {
        $watch.Stop()
    }
    $sanitized = Protect-Ov01Text -Text $rawOutput
    $totals = Get-Ov01CommandTotals -Parser ([string]$Gate.parser) -Output $sanitized
    $selectorCorpus = $sanitized
    $actualSkippedTests = New-Object System.Collections.Generic.List[object]
    $surefireSelectorIdentities = New-Object System.Collections.Generic.List[string]
    $surefireFreshReportCount = 0
    if ([string]$Gate.parser -eq 'maven-surefire') {
        $surefireDirectory = Join-Path $workingDirectory 'target\surefire-reports'
        if (Test-Path -LiteralPath $surefireDirectory -PathType Container) {
            foreach ($xml in @(Get-ChildItem -LiteralPath $surefireDirectory -Filter 'TEST-*.xml' -File)) {
                if ($xml.LastWriteTimeUtc -lt $startedUtc.AddSeconds(-5)) { continue }
                $surefireFreshReportCount++
                $surefireEvidence = Get-Ov01SurefireXmlEvidence -Path $xml.FullName
                if ([bool]$surefireEvidence.parsed) {
                    foreach ($identity in @($surefireEvidence.selectorIdentities)) { $selectorCorpus += "`n$identity"; $surefireSelectorIdentities.Add([string]$identity) }
                    foreach ($skippedTest in @($surefireEvidence.skippedTests)) { $actualSkippedTests.Add($skippedTest) }
                } else {
                    $selectorCorpus += "`nSUREFIRE_SELECTOR_PARSE_ERROR=$([System.IO.Path]::GetFileName($xml.FullName)):$($surefireEvidence.error)"
                }
            }
        }
    }
    if ([string]$Gate.parser -eq 'maven-surefire') {
        $surefireRetainedEvidence = [pscustomobject]@{
            freshReportCount = $surefireFreshReportCount
            selectorIdentities = @($surefireSelectorIdentities.ToArray())
            skippedTests = @($actualSkippedTests.ToArray())
        }
        $sanitized = $sanitized.TrimEnd("`r", "`n") + "`nOV01_SUREFIRE_EVIDENCE " + ($surefireRetainedEvidence | ConvertTo-Json -Compress -Depth 10)
    }
    $logPath = Join-Path $LogDirectory (([string]$Gate.id) + '.log')
    [System.IO.File]::WriteAllText($logPath, $sanitized + "`n", (New-Object System.Text.UTF8Encoding($false)))
    $skipDisposition = Get-Ov01SkipDisposition -Gate $Gate -ActualSkippedTests $actualSkippedTests.ToArray() -ReportedSkipped ([int]$totals.skipped) -RepoRoot $RepoRoot
    $selectorResults = @($ExecutableSelectors | ForEach-Object {
        $selectorText = [string]$_.selector
        [pscustomobject]@{
            scenarioId = [string]$_.scenarioId
            gateId = [string]$Gate.id
            selector = $selectorText
            matched = (-not [string]::IsNullOrWhiteSpace($selectorText) -and $selectorCorpus.IndexOf($selectorText, [System.StringComparison]::OrdinalIgnoreCase) -ge 0)
        }
    })
    $allSelectorsMatched = @($selectorResults | Where-Object { -not [bool]$_.matched }).Count -eq 0
    $nonSkipNonPass = [int]$totals.failures + [int]$totals.errors + [int]$totals.xfailed + [int]$totals.xpassed + [int]$totals.deselected
    $forbiddenOutcome = ($nonSkipNonPass -gt 0) -or -not [bool]$skipDisposition.valid -or [regex]::IsMatch($sanitized, '(?im)^\s*(?:FIXME|PENDING|DISABLED|XFAIL|XPASS)\b|\b(?:status|outcome)\s*[:=]\s*(?:FIXME|PENDING|DISABLED|XFAIL|XPASS)\b')
    $minimum = [int]$Gate.expectedMinTests
    $testContractMet = if ([string]$Gate.kind -eq 'test') { $totals.parsed -and $totals.tests -ge $minimum } else { $totals.parsed }
    $surefireEvidenceValid = [string]$Gate.parser -ne 'maven-surefire' -or $surefireFreshReportCount -gt 0
    $versionTimeoutSeconds = [Math]::Min($timeoutSeconds, 120)
    $toolchainConfiguration = $actualConfiguration
    if ($null -ne $flutterDartTool) {
        $toolchainConfiguration = [pscustomobject]@{
            versionArguments = @([string]$flutterDartTool.flutterSnapshot) + @($actualConfiguration.versionArguments | ForEach-Object { [string]$_ })
            expectedVersionRegex = [string]$actualConfiguration.expectedVersionRegex
        }
    }
    $versionEnvironment = $environment
    $versionStateDirectory = ''
    try {
        if ($null -ne $flutterDartTool) {
            $versionStateDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ('ov01-flutter-capture-' + [Guid]::NewGuid().ToString('N'))
            [void](New-Item -ItemType Directory -Path $versionStateDirectory)
            $versionEnvironment = Get-Ov01FlutterVersionEnvironment -Base $environment -StateDirectory $versionStateDirectory
        }
        $toolchain = Get-Ov01ExecutableIdentity -Executable $actualExecutable -Configuration $toolchainConfiguration -WorkingDirectory $workingDirectory -Environment $versionEnvironment -TimeoutSeconds $versionTimeoutSeconds
    } finally {
        if (-not [string]::IsNullOrWhiteSpace($versionStateDirectory) -and (Test-Path -LiteralPath $versionStateDirectory -PathType Container)) {
            Remove-Item -LiteralPath $versionStateDirectory -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    if ($null -ne $flutterDartTool) {
        $toolchain | Add-Member -NotePropertyName runnerTool -NotePropertyValue ([string]$flutterDartTool.runnerTool)
        $toolchain | Add-Member -NotePropertyName configuredFlutterExecutable -NotePropertyValue ([string]$flutterDartTool.configuredFlutterExecutable)
        $toolchain | Add-Member -NotePropertyName configuredFlutterExecutableSha256 -NotePropertyValue ([string]$flutterDartTool.configuredFlutterExecutableSha256)
        $toolchain | Add-Member -NotePropertyName flutterRoot -NotePropertyValue ([string]$flutterDartTool.flutterRoot)
        $toolchain | Add-Member -NotePropertyName flutterSnapshot -NotePropertyValue ([string]$flutterDartTool.flutterSnapshot)
        $toolchain | Add-Member -NotePropertyName flutterSnapshotSha256 -NotePropertyValue ([string]$flutterDartTool.flutterSnapshotSha256)
        $toolchain | Add-Member -NotePropertyName versionEnvironmentEvidence -NotePropertyValue (Get-Ov01FlutterVersionEnvironmentEvidence)
    }
    $environmentEvidence = [ordered]@{}
    if ($null -ne $Gate.PSObject.Properties['evidenceEnvironmentKeys']) {
        foreach ($key in @($Gate.evidenceEnvironmentKeys)) { if ($environment.ContainsKey([string]$key)) { $environmentEvidence[[string]$key] = [string]$environment[[string]$key] } }
    }
    $status = if (-not $timedOut -and $exitCode -eq 0 -and $totals.failures -eq 0 -and $totals.errors -eq 0 -and -not $forbiddenOutcome -and $testContractMet -and $surefireEvidenceValid -and $allSelectorsMatched -and [bool]$toolchain.versionMatched) { 'PASS' } else { 'FAIL' }
    return [pscustomobject]@{
        id = [string]$Gate.id
        platform = [string]$Gate.platform
        kind = [string]$Gate.kind
        status = $status
        exactCommand = $exactCommand
        effectiveArguments = @($arguments)
        workingDirectory = [string]$Gate.workingDirectory
        startedUtc = $startedUtc.ToString('o')
        durationMs = [int64]$watch.ElapsedMilliseconds
        timeoutSeconds = $timeoutSeconds
        timedOut = $timedOut
        childTreeTerminated = $childTreeTerminated
        exitCode = [int]$exitCode
        primaryExitCode = [int]$primaryExitCode
        fallbackUsed = $fallbackUsed
        notApplicable = $notApplicable
        notApplicableReason = $notApplicableReason
        dynamicInputs = @($dynamicInputs)
        totals = $totals
        surefireFreshReportCount = $surefireFreshReportCount
        selectorResults = @($selectorResults)
        skipDisposition = $skipDisposition
        forbiddenOutcomeDetected = $forbiddenOutcome
        toolchain = $toolchain
        environmentEvidence = [pscustomobject]$environmentEvidence
        runtimeIsolation = $runtimeIsolation
        logPath = 'logs/' + [System.IO.Path]::GetFileName($logPath)
        logBytes = [int64](Get-Item -LiteralPath $logPath).Length
        logSha256 = Get-Ov01Sha256 -Path $logPath
    }
}

function Get-Ov01ArtifactRecords {
    param(
        [Parameter(Mandatory = $true)]$Registry,
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string[]]$SelectedGateIds,
        [Parameter(Mandatory = $true)]$SourceIdentity
    )

    $records = New-Object System.Collections.Generic.List[object]
    $errors = New-Object System.Collections.Generic.List[string]
    foreach ($artifact in @($Registry.artifacts)) {
        $requiredBySelectedGate = @($artifact.requiredForGateIds | Where-Object { $SelectedGateIds -contains [string]$_ }).Count -gt 0
        $alwaysRequired = ([string]$artifact.kind -eq 'input')
        if (-not $requiredBySelectedGate -and -not $alwaysRequired) { continue }
        if ([string]$artifact.id -eq 'OV01-ART-REGISTRY') {
            $actualRegistryPath = Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath ([string]$SourceIdentity.registryRelativePath)
            $matches = @(Get-Item -LiteralPath $actualRegistryPath -ErrorAction SilentlyContinue | Where-Object { -not $_.PSIsContainer })
        } else {
            $absolutePattern = Resolve-Ov01RepoGlob -Root $RepoRoot -RelativePattern ([string]$artifact.path)
            $matches = @(Get-ChildItem -Path $absolutePattern -File -ErrorAction SilentlyContinue | Sort-Object FullName)
        }
        if ($matches.Count -eq 0) {
            if ([bool]$artifact.required) { $errors.Add("required artifact missing: $($artifact.id) [$($artifact.path)]") }
            continue
        }
        if (-not [bool]$artifact.allowMany -and $matches.Count -ne 1) { $errors.Add("artifact must resolve exactly once: $($artifact.id)"); continue }
        foreach ($file in $matches) {
            $records.Add([pscustomobject]@{
                id = [string]$artifact.id
                kind = [string]$artifact.kind
                path = ConvertTo-Ov01RelativePath -Root $RepoRoot -Path $file.FullName
                bytes = [int64]$file.Length
                sha256 = Get-Ov01Sha256 -Path $file.FullName
                sourceIdentity = [string]$SourceIdentity.compositeSha256
                registryRelativePath = [string]$SourceIdentity.registryRelativePath
                registrySha256 = [string]$SourceIdentity.registrySha256
            })
        }
    }
    return [pscustomobject]@{ records = $records.ToArray(); errors = $errors.ToArray() }
}

function Get-Ov01SkipDisposition {
    param(
        [Parameter(Mandatory = $true)]$Gate,
        [AllowEmptyCollection()][object[]]$ActualSkippedTests = @(),
        [Parameter(Mandatory = $true)][int]$ReportedSkipped,
        [Parameter(Mandatory = $true)][string]$RepoRoot
    )

    $errors = New-Object System.Collections.Generic.List[string]
    $actualList = New-Object System.Collections.Generic.List[object]
    foreach ($item in @($ActualSkippedTests)) {
        if ($null -ne $item) { $actualList.Add([pscustomobject]@{ selector = [string]$item.selector; reason = [string]$item.reason }) }
    }
    $actual = @($actualList.ToArray() | Sort-Object selector)
    $approvals = @(if ($null -ne $Gate.PSObject.Properties['approvedSkippedTests']) { $Gate.approvedSkippedTests })
    $approved = New-Object System.Collections.Generic.List[object]
    $unapproved = New-Object System.Collections.Generic.List[object]
    $matchedApprovalSelectors = New-Object System.Collections.Generic.List[string]

    if ($actual.Count -ne $ReportedSkipped) { $errors.Add("Surefire skipped count does not match XML identities: reported=$ReportedSkipped actual=$($actual.Count)") }
    foreach ($duplicate in @($actual | ForEach-Object { [string]$_.selector } | Group-Object | Where-Object Count -gt 1)) { $errors.Add("duplicate actual skipped test selector: $($duplicate.Name)") }
    foreach ($skipped in $actual) {
        $matches = @($approvals | Where-Object {
            [string]$_.selector -ceq [string]$skipped.selector -and
            ([string]$skipped.reason).IndexOf([string]$_.reasonContains, [System.StringComparison]::OrdinalIgnoreCase) -ge 0
        })
        if ($matches.Count -ne 1) {
            $unapproved.Add($skipped)
            $errors.Add("unapproved skipped test: $($skipped.selector)")
            continue
        }
        $approval = $matches[0]
        $matchedApprovalSelectors.Add([string]$approval.selector)
        $evidence = New-Object System.Collections.Generic.List[object]
        foreach ($path in @($approval.evidencePaths)) {
            $absolute = Resolve-Ov01RepoPath -Root $RepoRoot -RelativePath ([string]$path)
            if (-not (Test-Path -LiteralPath $absolute -PathType Leaf)) { $errors.Add("approved skip evidence path does not exist: $path"); continue }
            $evidence.Add([pscustomobject]@{ path = ([string]$path).Replace('\', '/'); sha256 = Get-Ov01Sha256 -Path $absolute })
        }
        $approved.Add([pscustomobject]@{
            selector = [string]$skipped.selector
            reason = [string]$skipped.reason
            reasonCode = [string]$approval.reasonCode
            owner = [string]$approval.owner
            expiresOn = [string]$approval.expiresOn
            evidence = $evidence.ToArray()
        })
    }
    $missingExpected = @($approvals | Where-Object { @($matchedApprovalSelectors) -notcontains [string]$_.selector } | ForEach-Object { [string]$_.selector } | Sort-Object)
    foreach ($selector in $missingExpected) { $errors.Add("approved skipped test is no longer observed: $selector") }
    return [pscustomobject]@{
        actual = $actual
        approved = @($approved.ToArray() | Sort-Object selector)
        unapproved = @($unapproved.ToArray() | Sort-Object selector)
        missingExpected = $missingExpected
        countMatches = ($actual.Count -eq $ReportedSkipped)
        valid = ($errors.Count -eq 0)
        errors = $errors.ToArray()
    }
}

function Test-Ov01PytestRuntimeIsolation {
    param(
        [Parameter(Mandatory = $true)]$Result,
        [Parameter(Mandatory = $true)]$GateDefinition,
        [AllowEmptyString()][string]$EvidenceDirectory = ''
    )

    $errors = New-Object System.Collections.Generic.List[string]
    $requiresIsolation = $null -ne $GateDefinition.PSObject.Properties['pytestIsolation'] -and [bool]$GateDefinition.pytestIsolation
    $hasIsolation = $null -ne $Result.PSObject.Properties['runtimeIsolation'] -and $null -ne $Result.runtimeIsolation
    if (-not $requiresIsolation) {
        if ($hasIsolation) { $errors.Add("unexpected pytest runtime isolation evidence: $($Result.id)") }
        return [pscustomobject]@{ valid = ($errors.Count -eq 0); errors = $errors.ToArray() }
    }
    if (-not $hasIsolation) {
        $errors.Add("missing pytest runtime isolation evidence: $($Result.id)")
        return [pscustomobject]@{ valid = $false; errors = $errors.ToArray() }
    }
    if ([string]::IsNullOrWhiteSpace($EvidenceDirectory)) {
        $errors.Add("pytest runtime isolation evidence directory is missing: $($Result.id)")
        return [pscustomobject]@{ valid = $false; errors = $errors.ToArray() }
    }

    $isolation = $Result.runtimeIsolation
    if ([string]$isolation.kind -ne 'pytest') { $errors.Add("invalid pytest runtime isolation kind: $($Result.id)") }
    $expectedRelativeRoot = 'runtime/pytest/' + [string]$Result.id
    if ([string]$isolation.relativeRoot -cne $expectedRelativeRoot) { $errors.Add("pytest runtime isolation relative root mismatch: $($Result.id)") }
    $evidenceRoot = [System.IO.Path]::GetFullPath($EvidenceDirectory)
    $expectedRoot = [System.IO.Path]::GetFullPath((Join-Path $evidenceRoot $expectedRelativeRoot))
    if ([string]$isolation.root -cne $expectedRoot) { $errors.Add("pytest runtime isolation root is not bound to evidence directory: $($Result.id)") }
    if (-not (Test-Path -LiteralPath $expectedRoot -PathType Container)) { $errors.Add("pytest runtime isolation root is missing: $($Result.id)") }
    $expectedChildren = [ordered]@{
        osTemp = Join-Path $expectedRoot 'os-temp'
        baseTemp = Join-Path $expectedRoot 'basetemp'
        cacheDir = Join-Path $expectedRoot 'cache'
    }
    foreach ($pathProperty in $expectedChildren.Keys) {
        $expectedPath = [System.IO.Path]::GetFullPath([string]$expectedChildren[$pathProperty])
        if ([string]$isolation.$pathProperty -cne $expectedPath) { $errors.Add("pytest runtime isolation child path mismatch: $($Result.id)/$pathProperty") }
        elseif ([string]$Result.status -eq 'PASS' -and -not (Test-Path -LiteralPath $expectedPath -PathType Container)) { $errors.Add("pytest runtime isolation path is missing on PASS: $($Result.id)/$pathProperty") }
    }
    $expectedEnvironmentKeys = @('TMPDIR', 'TEMP', 'TMP')
    $actualEnvironmentKeys = @($isolation.environmentKeys | ForEach-Object { [string]$_ })
    if ($actualEnvironmentKeys.Count -ne $expectedEnvironmentKeys.Count -or @(Compare-Object $expectedEnvironmentKeys $actualEnvironmentKeys -SyncWindow 0).Count -ne 0) { $errors.Add("pytest runtime isolation environment keys mismatch: $($Result.id)") }
    $effectiveArguments = @(if ($null -ne $Result.PSObject.Properties['effectiveArguments']) { $Result.effectiveArguments | ForEach-Object { [string]$_ } })
    $expectedIsolationArguments = @('--basetemp', [string]$expectedChildren.baseTemp, '-o', "cache_dir=$([string]$expectedChildren.cacheDir)")
    $actualIsolationArguments = if ($effectiveArguments.Count -ge 4) { @($effectiveArguments[($effectiveArguments.Count - 4)..($effectiveArguments.Count - 1)]) } else { @() }
    if ($actualIsolationArguments.Count -ne $expectedIsolationArguments.Count -or @(Compare-Object $expectedIsolationArguments $actualIsolationArguments -SyncWindow 0).Count -ne 0) { $errors.Add("pytest runtime isolation effective arguments mismatch: $($Result.id)") }
    $resolvedExecutable = if ($null -ne $Result.PSObject.Properties['toolchain'] -and $null -ne $Result.toolchain.PSObject.Properties['resolvedExecutable']) { [string]$Result.toolchain.resolvedExecutable } else { '' }
    $expectedExactCommand = if ([string]::IsNullOrWhiteSpace($resolvedExecutable)) { '' } else { Format-Ov01Command -Executable $resolvedExecutable -Arguments $effectiveArguments }
    if ([string]::IsNullOrWhiteSpace($expectedExactCommand) -or [string]$Result.exactCommand -cne $expectedExactCommand) { $errors.Add("pytest runtime isolation exact command mismatch: $($Result.id)") }
    return [pscustomobject]@{ valid = ($errors.Count -eq 0); errors = $errors.ToArray() }
}

function Test-Ov01ManualMetadataArtifactName {
    param([Parameter(Mandatory = $true)][string]$Path)

    $name = [System.IO.Path]::GetFileName($Path).ToLowerInvariant()
    return [regex]::IsMatch($name, '(^|[-_.])(index|summary|identity|manifest)([-_.]|$)')
}

function Test-Ov01ManualScenarioArtifactName {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ScenarioId
    )

    if ($ScenarioId -notmatch '^OV01-MAN-(\d{3})$') { return $false }
    $number = $Matches[1]
    $name = [System.IO.Path]::GetFileName($Path)
    $pattern = '(?i)(?:^|[^a-z0-9])(?:ov01[-_.]?)?man[-_.]?' + [regex]::Escape($number) + '(?:[^0-9]|$)'
    return [regex]::IsMatch($name, $pattern)
}

function Test-Ov01ManualEvidence {
    param(
        [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$SummaryPath,
        [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$RepoRoot,
        [AllowEmptyString()][string]$ExpectedApkSha256 = ''
    )

    $errors = New-Object System.Collections.Generic.List[string]
    $repoFull = [System.IO.Path]::GetFullPath($RepoRoot).TrimEnd('\', '/')
    $summaryFull = if ([System.IO.Path]::IsPathRooted($SummaryPath)) {
        [System.IO.Path]::GetFullPath($SummaryPath)
    } else {
        Resolve-Ov01RepoPath -Root $repoFull -RelativePath $SummaryPath
    }
    $repoPrefix = $repoFull + [System.IO.Path]::DirectorySeparatorChar
    if (-not $summaryFull.StartsWith($repoPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        $errors.Add('manual evidence summary escapes repository root')
    }
    if (-not (Test-Path -LiteralPath $summaryFull -PathType Leaf)) {
        $errors.Add('manual evidence summary is missing')
        return [pscustomobject]@{ valid = $false; errors = $errors.ToArray(); record = $null }
    }

    $summary = $null
    try { $summary = Get-Content -Encoding UTF8 -Raw -LiteralPath $summaryFull | ConvertFrom-Json }
    catch {
        $errors.Add("manual evidence summary is invalid JSON: $($_.Exception.Message)")
        return [pscustomobject]@{ valid = $false; errors = $errors.ToArray(); record = $null }
    }

    if ($null -eq $summary.PSObject.Properties['schemaVersion'] -or [int]$summary.schemaVersion -ne 2) { $errors.Add('manual evidence schemaVersion must be 2') }
    if ($null -eq $summary.PSObject.Properties['status'] -or [string]$summary.status -ne 'PASS') { $errors.Add('manual evidence status must be PASS') }
    foreach ($field in @('candidateBuiltUtc', 'installedUtc', 'completedUtc', 'candidateApkSha256', 'installedApkSha256', 'evidenceManifestPath')) {
        if ($null -eq $summary.PSObject.Properties[$field] -or [string]::IsNullOrWhiteSpace([string]$summary.$field)) {
            $errors.Add("manual evidence field is missing: $field")
        }
    }
    $completedUtc = [DateTimeOffset]::MinValue
    try { $completedUtc = [DateTimeOffset]::Parse([string]$summary.completedUtc) } catch { $errors.Add('manual evidence completedUtc is invalid') }
    $candidateBuiltUtc = [DateTimeOffset]::MinValue
    $installedUtc = [DateTimeOffset]::MinValue
    try { $candidateBuiltUtc = [DateTimeOffset]::Parse([string]$summary.candidateBuiltUtc) } catch { $errors.Add('manual evidence candidateBuiltUtc is invalid') }
    try { $installedUtc = [DateTimeOffset]::Parse([string]$summary.installedUtc) } catch { $errors.Add('manual evidence installedUtc is invalid') }
    foreach ($timestamp in @([pscustomobject]@{ name='candidateBuiltUtc'; value=$candidateBuiltUtc }, [pscustomobject]@{ name='installedUtc'; value=$installedUtc }, [pscustomobject]@{ name='completedUtc'; value=$completedUtc })) {
        if ($timestamp.value -ne [DateTimeOffset]::MinValue -and $timestamp.value.Offset -ne [TimeSpan]::Zero) { $errors.Add("manual evidence timestamp must be UTC: $($timestamp.name)") }
    }
    if ($candidateBuiltUtc -ne [DateTimeOffset]::MinValue -and $installedUtc -ne [DateTimeOffset]::MinValue -and $installedUtc -lt $candidateBuiltUtc) { $errors.Add('manual evidence installedUtc predates candidateBuiltUtc') }
    if ($installedUtc -ne [DateTimeOffset]::MinValue -and $completedUtc -ne [DateTimeOffset]::MinValue -and $completedUtc -lt $installedUtc) { $errors.Add('manual evidence completedUtc predates installedUtc') }

    $candidateApkSha = ([string]$summary.candidateApkSha256).ToLowerInvariant()
    $installedApkSha = ([string]$summary.installedApkSha256).ToLowerInvariant()
    if ($candidateApkSha -notmatch '^[0-9a-f]{64}$') { $errors.Add('manual candidate APK SHA-256 is invalid') }
    if ($installedApkSha -notmatch '^[0-9a-f]{64}$') { $errors.Add('manual installed APK SHA-256 is invalid') }
    if ($candidateApkSha -ne $installedApkSha) { $errors.Add('manual candidate and installed APK SHA-256 differ') }
    if (-not [string]::IsNullOrWhiteSpace($ExpectedApkSha256) -and $candidateApkSha -ne $ExpectedApkSha256.ToLowerInvariant()) {
        $errors.Add('manual APK SHA-256 does not match official built APK')
    }

    if ($null -eq $summary.PSObject.Properties['device']) {
        $errors.Add('manual device identity is missing')
    } else {
        foreach ($field in @('serial', 'androidVersion', 'apiLevel', 'buildFingerprint')) {
            if ($null -eq $summary.device.PSObject.Properties[$field] -or [string]::IsNullOrWhiteSpace([string]$summary.device.$field)) {
                $errors.Add("manual device identity field is missing: $field")
            }
        }
    }
    if ($null -eq $summary.PSObject.Properties['leakScan']) {
        $errors.Add('manual leak scan evidence is missing')
    } elseif ([string]$summary.leakScan.status -ne 'PASS' -or [int]$summary.leakScan.findingsCount -ne 0) {
        $errors.Add('manual leak scan must PASS with zero findings')
    }

    [string[]]$expectedScenarioIds = @(1..34 | ForEach-Object { 'OV01-MAN-{0:d3}' -f $_ })
    [string[]]$declaredScenarioIds = @(if ($null -ne $summary.PSObject.Properties['requiredScenarioIds']) { $summary.requiredScenarioIds | ForEach-Object { [string]$_ } })
    $scenarioResults = @(if ($null -ne $summary.PSObject.Properties['scenarioResults']) { $summary.scenarioResults })
    [string[]]$resultIds = @($scenarioResults | ForEach-Object { [string]$_.id })
    if ($declaredScenarioIds.Count -ne 34 -or @(Compare-Object $expectedScenarioIds $declaredScenarioIds).Count -ne 0) {
        $errors.Add('manual requiredScenarioIds must be the exact OV01-MAN-001..034 set')
    }
    if ($scenarioResults.Count -ne 34 -or $resultIds.Count -ne @($resultIds | Select-Object -Unique).Count -or @(Compare-Object $expectedScenarioIds $resultIds).Count -ne 0) {
        $errors.Add('manual scenarioResults must contain each OV01-MAN-001..034 exactly once')
    }

    $bundleFull = [System.IO.Path]::GetDirectoryName($summaryFull)
    $bundlePrefix = $bundleFull.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
    $multiCaseByPath = @{}
    $multiCaseArtifacts = @(if ($null -ne $summary.PSObject.Properties['multiCaseArtifacts']) { $summary.multiCaseArtifacts })
    foreach ($declaration in $multiCaseArtifacts) {
        $declaredPath = if ($null -ne $declaration.PSObject.Properties['path']) { [string]$declaration.path } else { '' }
        $declaredName = [System.IO.Path]::GetFileName($declaredPath)
        if ([string]::IsNullOrWhiteSpace($declaredPath)) {
            $errors.Add('manual multi-case artifact path is missing')
            continue
        }
        $declaredIds = @(if ($null -ne $declaration.PSObject.Properties['scenarioIds']) { $declaration.scenarioIds | ForEach-Object { [string]$_ } })
        if ($declaredIds.Count -lt 2) { $errors.Add("manual multi-case artifact must declare at least two scenarios: $declaredName") }
        if ($declaredIds.Count -ne @($declaredIds | Select-Object -Unique).Count) { $errors.Add("manual multi-case artifact has duplicate scenarios: $declaredName") }
        foreach ($declaredId in $declaredIds) {
            if ($expectedScenarioIds -notcontains $declaredId) { $errors.Add("manual multi-case artifact declares unknown scenario: $declaredName/$declaredId") }
        }
        try {
            $declaredFull = if ([System.IO.Path]::IsPathRooted($declaredPath)) { [System.IO.Path]::GetFullPath($declaredPath) } else { Resolve-Ov01RepoPath -Root $repoFull -RelativePath $declaredPath }
            if (-not $declaredFull.StartsWith($bundlePrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
                $errors.Add("manual multi-case artifact escapes bundle: $declaredPath")
                continue
            }
            if (-not (Test-Path -LiteralPath $declaredFull -PathType Leaf)) { $errors.Add("manual multi-case artifact is missing: $declaredPath") }
            $declarationKey = (ConvertTo-Ov01RelativePath -Root $bundleFull -Path $declaredFull).ToLowerInvariant()
            if ($multiCaseByPath.ContainsKey($declarationKey)) {
                $errors.Add("duplicate manual multi-case artifact declaration: $declaredName")
            } else {
                $multiCaseByPath[$declarationKey] = [pscustomobject]@{ path = $declaredFull; name = $declaredName; scenarioIds = $declaredIds }
            }
        } catch { $errors.Add("manual multi-case artifact path is invalid: $declaredPath") }
    }

    $artifactUsers = @{}
    foreach ($scenario in $scenarioResults) {
        $scenarioId = [string]$scenario.id
        if ([string]$scenario.status -ne 'PASS') { $errors.Add("manual scenario is not PASS: $scenarioId") }
        foreach ($field in @('actualResult', 'executedUtc', 'apkSha256')) {
            if ($null -eq $scenario.PSObject.Properties[$field] -or [string]::IsNullOrWhiteSpace([string]$scenario.$field)) {
                $errors.Add("manual scenario field is missing: $scenarioId/$field")
            }
        }
        $executedUtc = [DateTimeOffset]::MinValue
        $executedUtcValid = $false
        if ($null -ne $scenario.PSObject.Properties['executedUtc'] -and -not [string]::IsNullOrWhiteSpace([string]$scenario.executedUtc)) {
            try { $executedUtc = [DateTimeOffset]::Parse([string]$scenario.executedUtc); $executedUtcValid = $true }
            catch { $errors.Add("manual scenario executedUtc is invalid: $scenarioId") }
        }
        if ($executedUtcValid -and $executedUtc.Offset -ne [TimeSpan]::Zero) { $errors.Add("manual scenario executedUtc must be UTC: $scenarioId") }
        if ($executedUtcValid -and $installedUtc -ne [DateTimeOffset]::MinValue -and $executedUtc -lt $installedUtc) { $errors.Add("manual scenario executedUtc predates installed candidate: $scenarioId") }
        if ($executedUtcValid -and $completedUtc -ne [DateTimeOffset]::MinValue -and $executedUtc -gt $completedUtc) { $errors.Add("manual scenario executedUtc is after completion: $scenarioId") }
        if ($null -ne $scenario.PSObject.Properties['apkSha256'] -and -not [string]::IsNullOrWhiteSpace([string]$scenario.apkSha256)) {
            $scenarioApkSha = ([string]$scenario.apkSha256).ToLowerInvariant()
            if ($scenarioApkSha -notmatch '^[0-9a-f]{64}$') { $errors.Add("manual scenario APK SHA-256 is invalid: $scenarioId") }
            elseif ($scenarioApkSha -ne $candidateApkSha) { $errors.Add("manual scenario APK SHA-256 does not match candidate: $scenarioId") }
        }
        if ($null -eq $scenario.PSObject.Properties['device'] -or $null -eq $scenario.device) {
            $errors.Add("manual scenario device identity is missing: $scenarioId")
        } else {
            foreach ($field in @('serial', 'androidVersion', 'apiLevel', 'buildFingerprint')) {
                if ($null -eq $scenario.device.PSObject.Properties[$field] -or [string]::IsNullOrWhiteSpace([string]$scenario.device.$field)) {
                    $errors.Add("manual scenario device identity field is missing: $scenarioId/$field")
                } elseif ($null -ne $summary.PSObject.Properties['device'] -and [string]$scenario.device.$field -cne [string]$summary.device.$field) {
                    $errors.Add("manual scenario device identity mismatch: $scenarioId/$field")
                }
            }
        }
        if ($null -eq $scenario.PSObject.Properties['oracle'] -or $null -eq $scenario.oracle) {
            $errors.Add("manual scenario oracle is missing: $scenarioId")
        } else {
            foreach ($field in @('type', 'expected', 'observed', 'verdict')) {
                if ($null -eq $scenario.oracle.PSObject.Properties[$field] -or [string]::IsNullOrWhiteSpace([string]$scenario.oracle.$field)) {
                    $errors.Add("manual scenario oracle field is missing: $scenarioId/$field")
                }
            }
            if ($null -eq $scenario.oracle.PSObject.Properties['requiresApiDb'] -or $scenario.oracle.requiresApiDb -isnot [bool]) { $errors.Add("manual scenario oracle requiresApiDb must be boolean: $scenarioId") }
            if ($null -ne $scenario.oracle.PSObject.Properties['type'] -and @('visual', 'state', 'api', 'database', 'accessibility', 'security', 'composite') -notcontains [string]$scenario.oracle.type) {
                $errors.Add("manual scenario oracle type is invalid: $scenarioId")
            }
            if ($null -ne $scenario.oracle.PSObject.Properties['verdict'] -and [string]$scenario.oracle.verdict -ne 'PASS') {
                $errors.Add("manual scenario oracle verdict must be PASS: $scenarioId")
            }
        }
        if ($null -eq $scenario.PSObject.Properties['defectRefs']) {
            $errors.Add("manual scenario defectRefs is missing: $scenarioId")
        } else {
            $defectRefs = @($scenario.defectRefs | ForEach-Object { [string]$_ })
            if (@($defectRefs | Where-Object { [string]::IsNullOrWhiteSpace($_) }).Count -gt 0) { $errors.Add("manual scenario defectRefs contains an empty value: $scenarioId") }
            if ($defectRefs.Count -ne @($defectRefs | Select-Object -Unique).Count) { $errors.Add("manual scenario defectRefs contains a duplicate: $scenarioId") }
        }
        $paths = @(if ($null -ne $scenario.PSObject.Properties['evidencePaths']) { $scenario.evidencePaths | ForEach-Object { [string]$_ } })
        if ($paths.Count -eq 0) { $errors.Add("manual scenario has no evidence path: $scenarioId") }
        if ($paths.Count -ne @($paths | Select-Object -Unique).Count) { $errors.Add("manual scenario has duplicate evidence paths: $scenarioId") }
        $adbTranscriptPaths = @(if ($null -ne $scenario.PSObject.Properties['adbTranscriptPaths']) { $scenario.adbTranscriptPaths | ForEach-Object { [string]$_ } })
        if ($null -eq $scenario.PSObject.Properties['adbTranscriptPaths'] -or $adbTranscriptPaths.Count -eq 0) { $errors.Add("manual scenario ADB transcript is missing: $scenarioId") }
        if ($adbTranscriptPaths.Count -ne @($adbTranscriptPaths | Select-Object -Unique).Count) { $errors.Add("manual scenario ADB transcript paths contain duplicates: $scenarioId") }
        foreach ($adbPath in $adbTranscriptPaths) {
            if ($paths -notcontains $adbPath) { $errors.Add("manual scenario ADB transcript is not in evidencePaths: $scenarioId/$adbPath") }
            if (@('.txt', '.log') -notcontains [System.IO.Path]::GetExtension($adbPath).ToLowerInvariant()) { $errors.Add("manual scenario ADB transcript extension is invalid: $scenarioId/$adbPath") }
            if (Test-Ov01ManualMetadataArtifactName -Path $adbPath) { $errors.Add("manual scenario ADB transcript is metadata-only: $scenarioId/$adbPath") }
            try {
                $adbFull = if ([System.IO.Path]::IsPathRooted($adbPath)) { [System.IO.Path]::GetFullPath($adbPath) } else { Resolve-Ov01RepoPath -Root $repoFull -RelativePath $adbPath }
                if ($adbFull.StartsWith($bundlePrefix, [System.StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $adbFull -PathType Leaf)) {
                    $adbText = Get-Content -Encoding UTF8 -Raw -LiteralPath $adbFull
                    if ([string]::IsNullOrWhiteSpace($adbText)) { $errors.Add("manual scenario ADB transcript is empty: $scenarioId/$adbPath") }
                }
            } catch { $errors.Add("manual scenario ADB transcript path is invalid: $scenarioId/$adbPath") }
        }
        $apiDbEvidencePaths = @(if ($null -ne $scenario.PSObject.Properties['apiDbEvidencePaths']) { $scenario.apiDbEvidencePaths | ForEach-Object { [string]$_ } })
        if ($null -eq $scenario.PSObject.Properties['apiDbEvidencePaths']) { $errors.Add("manual scenario apiDbEvidencePaths is missing: $scenarioId") }
        if ($apiDbEvidencePaths.Count -ne @($apiDbEvidencePaths | Select-Object -Unique).Count) { $errors.Add("manual scenario API/DB evidence paths contain duplicates: $scenarioId") }
        $requiresApiDb = $null -ne $scenario.PSObject.Properties['oracle'] -and $null -ne $scenario.oracle -and $null -ne $scenario.oracle.PSObject.Properties['requiresApiDb'] -and $scenario.oracle.requiresApiDb -is [bool] -and [bool]$scenario.oracle.requiresApiDb
        if ($requiresApiDb -and $apiDbEvidencePaths.Count -eq 0) { $errors.Add("manual scenario requires API/DB evidence but has none: $scenarioId") }
        foreach ($apiDbPath in $apiDbEvidencePaths) {
            if ($paths -notcontains $apiDbPath) { $errors.Add("manual scenario API/DB evidence is not in evidencePaths: $scenarioId/$apiDbPath") }
            if (Test-Ov01ManualMetadataArtifactName -Path $apiDbPath) { $errors.Add("manual scenario API/DB evidence is metadata-only: $scenarioId/$apiDbPath") }
        }
        $substantiveCount = 0
        foreach ($path in $paths) {
            try {
                $evidenceFull = if ([System.IO.Path]::IsPathRooted($path)) { [System.IO.Path]::GetFullPath($path) } else { Resolve-Ov01RepoPath -Root $repoFull -RelativePath $path }
                if (-not $evidenceFull.StartsWith($bundlePrefix, [System.StringComparison]::OrdinalIgnoreCase)) { $errors.Add("manual scenario evidence escapes bundle: $scenarioId/$path") }
                elseif (-not (Test-Path -LiteralPath $evidenceFull -PathType Leaf)) { $errors.Add("manual scenario evidence is missing: $scenarioId/$path") }
                else {
                    $artifactKey = (ConvertTo-Ov01RelativePath -Root $bundleFull -Path $evidenceFull).ToLowerInvariant()
                    $artifactName = [System.IO.Path]::GetFileName($evidenceFull)
                    if ([int64](Get-Item -LiteralPath $evidenceFull).Length -eq 0) { $errors.Add("manual scenario evidence is empty: $scenarioId/$path") }
                    $isMetadata = Test-Ov01ManualMetadataArtifactName -Path $evidenceFull
                    if (-not $isMetadata) {
                        $substantiveCount++
                        $isScenarioSpecific = Test-Ov01ManualScenarioArtifactName -Path $evidenceFull -ScenarioId $scenarioId
                        $isDeclaredForScenario = $multiCaseByPath.ContainsKey($artifactKey) -and @($multiCaseByPath[$artifactKey].scenarioIds) -contains $scenarioId
                        if (-not $isScenarioSpecific -and -not $isDeclaredForScenario) {
                            $errors.Add("manual evidence artifact is neither scenario-specific nor declared multi-case: $scenarioId/$artifactName")
                        }
                        if (-not $artifactUsers.ContainsKey($artifactKey)) { $artifactUsers[$artifactKey] = New-Object System.Collections.Generic.List[string] }
                        $artifactUsers[$artifactKey].Add($scenarioId)
                    }
                }
            } catch { $errors.Add("manual scenario evidence path is invalid: $scenarioId/$path") }
        }
        if ($substantiveCount -eq 0) { $errors.Add("manual scenario has no substantive evidence artifact: $scenarioId") }
    }

    foreach ($artifactKey in @($artifactUsers.Keys)) {
        $users = @($artifactUsers[$artifactKey] | Sort-Object -Unique)
        if ($users.Count -gt 1) {
            $artifactName = [System.IO.Path]::GetFileName($artifactKey)
            $hasExactDeclaration = $multiCaseByPath.ContainsKey($artifactKey) -and
                @($multiCaseByPath[$artifactKey].scenarioIds).Count -eq $users.Count -and
                @(Compare-Object @($multiCaseByPath[$artifactKey].scenarioIds | Sort-Object -Unique) $users).Count -eq 0
            if (-not $hasExactDeclaration) { $errors.Add("manual evidence artifact is reused without an exact multi-case declaration: $artifactName") }
        }
    }
    foreach ($artifactKey in @($multiCaseByPath.Keys)) {
        $declared = @($multiCaseByPath[$artifactKey].scenarioIds | Sort-Object -Unique)
        $users = @(if ($artifactUsers.ContainsKey($artifactKey)) { $artifactUsers[$artifactKey] | Sort-Object -Unique })
        if ($declared.Count -ne $users.Count -or @(Compare-Object $declared $users).Count -ne 0) {
            $errors.Add("manual multi-case artifact scenario binding mismatch: $($multiCaseByPath[$artifactKey].name)")
        }
    }

    $manifestFull = ''
    try {
        $manifestFull = if ([System.IO.Path]::IsPathRooted([string]$summary.evidenceManifestPath)) {
            [System.IO.Path]::GetFullPath([string]$summary.evidenceManifestPath)
        } else {
            Resolve-Ov01RepoPath -Root $repoFull -RelativePath ([string]$summary.evidenceManifestPath)
        }
        if ([System.IO.Path]::GetDirectoryName($manifestFull) -ne $bundleFull -or [System.IO.Path]::GetFileName($manifestFull) -ne 'evidence-manifest.json') {
            $errors.Add('manual evidence manifest must be evidence-manifest.json in the summary bundle')
        } else {
            $seal = Test-Ov01ClosedSetManifest -EvidenceDirectory $bundleFull
            foreach ($sealError in @($seal.errors)) { $errors.Add("manual evidence seal invalid: $sealError") }
        }
    } catch { $errors.Add("manual evidence manifest path is invalid: $($_.Exception.Message)") }

    $textExtensions = @('.txt', '.log', '.json', '.xml', '.md', '.csv', '.yaml', '.yml')
    foreach ($file in @(Get-ChildItem -LiteralPath $bundleFull -Recurse -File | Where-Object { $textExtensions -contains $_.Extension.ToLowerInvariant() })) {
        try {
            $content = Get-Content -Encoding UTF8 -Raw -LiteralPath $file.FullName
            foreach ($finding in @(Test-Ov01Leak -Text $content)) {
                $relativeLeakPath = ConvertTo-Ov01RelativePath -Root $bundleFull -Path $file.FullName
                $errors.Add("manual evidence leak finding: $finding/$relativeLeakPath")
            }
        } catch { $errors.Add("manual evidence text scan failed: $($file.Name)") }
    }

    $record = [pscustomobject]@{
        summaryPath = ConvertTo-Ov01RelativePath -Root $repoFull -Path $summaryFull
        summarySha256 = Get-Ov01Sha256 -Path $summaryFull
        status = [string]$summary.status
        completedUtc = [string]$summary.completedUtc
        scenarioCount = $scenarioResults.Count
        candidateApkSha256 = $candidateApkSha
        installedApkSha256 = $installedApkSha
        evidenceManifestPath = if ([string]::IsNullOrWhiteSpace($manifestFull)) { [string]$summary.evidenceManifestPath } else { ConvertTo-Ov01RelativePath -Root $repoFull -Path $manifestFull }
        evidenceManifestSha256 = if (-not [string]::IsNullOrWhiteSpace($manifestFull) -and (Test-Path -LiteralPath $manifestFull -PathType Leaf)) { Get-Ov01Sha256 -Path $manifestFull } else { '' }
        device = $summary.device
        leakScan = $summary.leakScan
        valid = ($errors.Count -eq 0)
    }
    return [pscustomobject]@{ valid = ($errors.Count -eq 0); errors = $errors.ToArray(); record = $record }
}

function Test-Ov01RunReport {
    param(
        [Parameter(Mandatory = $true)]$Report,
        [Parameter(Mandatory = $true)]$Registry,
        [AllowEmptyString()][string]$EvidenceDirectory = '',
        [AllowEmptyString()][string]$CheckpointKeyPath = ''
    )

    $errors = New-Object System.Collections.Generic.List[string]
    $validationRepoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
    $evidenceFull = ''
    $declaredEvidenceFull = ''
    if ($null -ne $Report.PSObject.Properties['evidenceDirectory'] -and -not [string]::IsNullOrWhiteSpace([string]$Report.evidenceDirectory)) {
        try { $declaredEvidenceFull = Resolve-Ov01RepoPath -Root $validationRepoRoot -RelativePath ([string]$Report.evidenceDirectory) }
        catch { $errors.Add("report evidence directory is invalid: $($_.Exception.Message)") }
    } else {
        $errors.Add('report evidence directory is missing')
    }
    if (-not [string]::IsNullOrWhiteSpace($EvidenceDirectory)) {
        $evidenceFull = [System.IO.Path]::GetFullPath($EvidenceDirectory)
        if (-not [string]::IsNullOrWhiteSpace($declaredEvidenceFull) -and -not $evidenceFull.Equals($declaredEvidenceFull, [System.StringComparison]::OrdinalIgnoreCase)) {
            $errors.Add('report evidence directory binding mismatch')
        }
    } else { $evidenceFull = $declaredEvidenceFull }
    if (-not [string]::IsNullOrWhiteSpace($evidenceFull) -and -not (Test-Path -LiteralPath $evidenceFull -PathType Container)) {
        $errors.Add('report evidence directory does not exist')
    }
    $allowed = @('PASS', 'FAIL', 'BLOCKED', 'DECISION_REQUIRED', 'NOT_APPLICABLE')
    $knownScenarioIds = @($Registry.scenarios | ForEach-Object { [string]$_.id })
    $knownGateIds = @($Registry.gates | ForEach-Object { [string]$_.id })
    $knownArtifactIds = @($Registry.artifacts | ForEach-Object { [string]$_.id })
    $selectedScenarioIds = @($Report.selection.scenarioIds | ForEach-Object { [string]$_ })
    $selectedGateIds = @($Report.selection.gateIds | ForEach-Object { [string]$_ })
    $scenarioResultIds = @($Report.scenarioResults | ForEach-Object { [string]$_.id })
    $gateResultIds = @($Report.gateResults | ForEach-Object { [string]$_.id })
    if ($selectedScenarioIds.Count -eq 0) { $errors.Add('empty scenario selection') }
    foreach ($collection in @(
        [pscustomobject]@{ label = 'selected scenario'; values = $selectedScenarioIds; known = $knownScenarioIds },
        [pscustomobject]@{ label = 'scenario result'; values = $scenarioResultIds; known = $knownScenarioIds },
        [pscustomobject]@{ label = 'selected gate'; values = $selectedGateIds; known = $knownGateIds },
        [pscustomobject]@{ label = 'gate result'; values = $gateResultIds; known = $knownGateIds }
    )) {
        foreach ($empty in @($collection.values | Where-Object { [string]::IsNullOrWhiteSpace([string]$_) })) { $errors.Add("empty $($collection.label) identity") }
        foreach ($duplicate in @($collection.values | Group-Object | Where-Object Count -gt 1)) { $errors.Add("duplicate $($collection.label): $($duplicate.Name)") }
        foreach ($id in @($collection.values)) { if (@($collection.known) -notcontains [string]$id) { $errors.Add("unknown $($collection.label): $id") } }
    }
    if (@(Compare-Object $selectedScenarioIds $scenarioResultIds).Count -ne 0 -or $selectedScenarioIds.Count -ne $scenarioResultIds.Count) { $errors.Add('selected scenario identities do not exactly match scenario results') }
    if (@(Compare-Object $selectedGateIds $gateResultIds).Count -ne 0 -or $selectedGateIds.Count -ne $gateResultIds.Count) { $errors.Add('selected gate identities do not exactly match gate results') }

    if ([string]::IsNullOrWhiteSpace([string]$Report.sourceIdentity.compositeSha256)) { $errors.Add('missing composite source identity') }
    if ($Report.sourceIdentity.baselineHead -ne $Report.sourceIdentity.currentHead) { $errors.Add('build binding HEAD mismatch') }
    if ([string]::IsNullOrWhiteSpace([string]$Report.sourceIdentity.registryRelativePath)) { $errors.Add('missing source registry relative path') }
    if ([string]::IsNullOrWhiteSpace([string]$Report.sourceIdentity.registrySha256)) { $errors.Add('missing source registry SHA-256') }
    if (@('Release', 'Diagnostic') -notcontains [string]$Report.runMode) { $errors.Add('invalid or missing run mode') }
    if ([string]$Report.runMode -eq 'Release' -and (-not [bool]$Report.releaseEligible -or -not [bool]$Report.sourceIdentity.registryCanonical)) { $errors.Add('release run is not bound to the canonical registry') }
    if ([string]$Report.runMode -eq 'Diagnostic' -and [bool]$Report.releaseEligible) { $errors.Add('diagnostic run cannot be release eligible') }
    if ([string]$Report.runMode -eq 'Release') {
        if ([string]::IsNullOrWhiteSpace($CheckpointKeyPath)) { $errors.Add('release checkpoint key path is required for report validation') }
        [string[]]$expectedReleaseGateIds = @(Get-Ov01CanonicalReleaseGateIds)
        if ($selectedScenarioIds.Count -ne $knownScenarioIds.Count -or @(Compare-Object $knownScenarioIds $selectedScenarioIds).Count -ne 0) {
            $errors.Add('release scenario selection must be the exact canonical all-scenario set')
        }
        if ($selectedGateIds.Count -ne $expectedReleaseGateIds.Count -or @(Compare-Object $expectedReleaseGateIds $selectedGateIds).Count -ne 0) {
            $errors.Add('release gate selection must be the exact canonical release-gate set')
        }
        if ([string]$Report.sourceIdentity.registryRelativePath -cne '06_Testing/Automation/ov-01/ov01-scenario-registry.json') {
            $errors.Add('release source registry path is not canonical')
        }
        if ($null -eq $Report.PSObject.Properties['postRunSourceIdentity'] -or $null -eq $Report.PSObject.Properties['sourceIdentityDriftErrors']) {
            $errors.Add('release post-run source identity evidence is missing')
        } else {
            $stability = Test-Ov01SourceIdentityStable -Before $Report.sourceIdentity -After $Report.postRunSourceIdentity
            foreach ($driftError in @($stability.errors)) { $errors.Add($driftError) }
            if (@($Report.sourceIdentityDriftErrors).Count -ne 0) { $errors.Add('release source identity drift errors are not empty') }
        }
        if ($null -eq $Report.PSObject.Properties['manualEvidence'] -or $null -eq $Report.manualEvidence) {
            $errors.Add('release manual evidence is missing')
        } else {
            $apkRecords = @($Report.artifacts | Where-Object id -eq 'OV01-ART-MOBILE-APK')
            $expectedApkSha = if ($apkRecords.Count -eq 1) { [string]$apkRecords[0].sha256 } else { '' }
            $manualCheck = Test-Ov01ManualEvidence -SummaryPath ([string]$Report.manualEvidence.summaryPath) -RepoRoot $validationRepoRoot -ExpectedApkSha256 $expectedApkSha
            foreach ($manualError in @($manualCheck.errors)) { $errors.Add("release manual evidence invalid: $manualError") }
            if ($null -ne $manualCheck.record) {
                $reportedManualJson = $Report.manualEvidence | ConvertTo-Json -Compress -Depth 20
                $validatedManualJson = $manualCheck.record | ConvertTo-Json -Compress -Depth 20
                if ($reportedManualJson -cne $validatedManualJson) { $errors.Add('release manual evidence record mismatch') }
            }
        }
        if ($null -eq $Report.PSObject.Properties['manualEvidenceErrors'] -or @($Report.manualEvidenceErrors).Count -ne 0) {
            $errors.Add('release manual evidence errors must be empty')
        }
        if (-not [string]::IsNullOrWhiteSpace($evidenceFull) -and $null -ne $Report.manualEvidence) {
            $checkpointPath = Join-Path $evidenceFull 'release-checkpoint.json'
            $expectedManualWrapper = [pscustomobject]@{ record = $Report.manualEvidence }
            if ([string]::IsNullOrWhiteSpace($CheckpointKeyPath)) {
                $checkpointCheck = [pscustomobject]@{ valid = $false; errors = @('release checkpoint key path is missing'); checkpoint = $null }
            } else {
                $checkpointCheck = Test-Ov01ReleaseCheckpoint -CheckpointPath $checkpointPath -RepoRoot $validationRepoRoot -EvidenceDirectory $evidenceFull -ExpectedSourceIdentity $Report.sourceIdentity -ExpectedManualEvidence $expectedManualWrapper -Registry $Registry -CheckpointKeyPath $CheckpointKeyPath -AllowFinalArtifacts
            }
            foreach ($checkpointError in @($checkpointCheck.errors)) { $errors.Add("release checkpoint invalid: $checkpointError") }
            if ($checkpointCheck.valid) {
                if ($null -eq $Report.PSObject.Properties['checkpointAuthentication'] -or ($Report.checkpointAuthentication | ConvertTo-Json -Compress -Depth 10) -cne ($checkpointCheck.checkpoint.authentication | ConvertTo-Json -Compress -Depth 10)) { $errors.Add('release report checkpoint authentication attestation mismatch') }
                $checkpointResults = @($checkpointCheck.checkpoint.completedGateResults)
                if ($checkpointResults.Count -ne 17) { $errors.Add('release checkpoint is not complete at final report') }
                elseif (@($Report.gateResults).Count -eq 17) {
                    for ($i = 0; $i -lt 17; $i++) {
                        if ([string]$checkpointResults[$i].resultSha256 -cne (Get-Ov01ObjectSha256 -Value $Report.gateResults[$i])) { $errors.Add("release checkpoint/report gate result mismatch: index=$i") }
                    }
                }
                if ((@($checkpointCheck.checkpoint.artifactRecords) | ConvertTo-Json -Compress -Depth 20) -cne (@($Report.artifacts) | ConvertTo-Json -Compress -Depth 20)) {
                    $errors.Add('release checkpoint/report artifact records mismatch')
                }
            }
        }
    }

    if ($null -eq $Report.PSObject.Properties['runnerEnvironment']) {
        $errors.Add('missing runner environment identity')
    } else {
        foreach ($field in @('osVersion', 'architecture', 'powerShellVersion')) {
            if ($null -eq $Report.runnerEnvironment.PSObject.Properties[$field] -or [string]::IsNullOrWhiteSpace([string]$Report.runnerEnvironment.$field)) {
                $errors.Add("missing runner environment field: $field")
            }
        }
    }

    foreach ($selectedScenarioId in $selectedScenarioIds) {
        $definition = @($Registry.scenarios | Where-Object { [string]$_.id -eq $selectedScenarioId } | Select-Object -First 1)
        if ($definition.Count -eq 1) {
            foreach ($requiredGateId in @($definition[0].gateIds)) { if ($selectedGateIds -notcontains [string]$requiredGateId) { $errors.Add("selected scenario is missing required selected gate: $selectedScenarioId/$requiredGateId") } }
        }
    }
    foreach ($result in @($Report.gateResults)) {
        if ($allowed -notcontains [string]$result.status) { $errors.Add("invalid gate status: $($result.id)") }
        $gateDefinition = @($Registry.gates | Where-Object { [string]$_.id -eq [string]$result.id } | Select-Object -First 1)
        if ($null -eq $result.PSObject.Properties['timeoutSeconds'] -or [int]$result.timeoutSeconds -lt 1) {
            $errors.Add("missing or invalid gate timeout evidence: $($result.id)")
        } elseif ($gateDefinition.Count -eq 1 -and [int]$result.timeoutSeconds -ne [int]$gateDefinition[0].timeoutSeconds) {
            $errors.Add("gate timeout evidence does not match registry: $($result.id)")
        }
        if ($null -eq $result.PSObject.Properties['timedOut']) { $errors.Add("missing gate timeout outcome: $($result.id)") }
        if ($null -eq $result.PSObject.Properties['childTreeTerminated']) { $errors.Add("missing child-tree termination outcome: $($result.id)") }
        $expectedLogPath = "logs/$([string]$result.id).log"
        if ($null -eq $result.PSObject.Properties['logPath'] -or [string]$result.logPath -cne $expectedLogPath) {
            $errors.Add("gate log path is not canonical: $($result.id)")
        } elseif (-not [string]::IsNullOrWhiteSpace($evidenceFull)) {
            try {
                $logFull = Resolve-Ov01RepoPath -Root $evidenceFull -RelativePath ([string]$result.logPath)
                if (-not (Test-Path -LiteralPath $logFull -PathType Leaf)) {
                    $errors.Add("gate log file is missing: $($result.id)")
                } else {
                    $actualLogBytes = [int64](Get-Item -LiteralPath $logFull).Length
                    $actualLogSha = Get-Ov01Sha256 -Path $logFull
                    if ($null -eq $result.PSObject.Properties['logBytes'] -or [int64]$result.logBytes -ne $actualLogBytes) { $errors.Add("gate log byte length mismatch: $($result.id)") }
                    if ($null -eq $result.PSObject.Properties['logSha256'] -or [string]$result.logSha256 -cne $actualLogSha) { $errors.Add("gate log SHA-256 mismatch: $($result.id)") }
                }
            } catch { $errors.Add("gate log path escapes evidence directory: $($result.id)") }
        }
        $reportedNonPass = 0
        foreach ($field in @('failures', 'errors', 'xfailed', 'xpassed', 'deselected')) {
            if ($null -ne $result.totals.PSObject.Properties[$field]) { $reportedNonPass += [int]$result.totals.$field }
        }
        $skipEvidenceValid = $false
        if ($null -eq $result.PSObject.Properties['skipDisposition']) {
            $requiresSkipDisposition = [int]$result.totals.skipped -gt 0 -or ($gateDefinition.Count -eq 1 -and $null -ne $gateDefinition[0].PSObject.Properties['approvedSkippedTests'] -and @($gateDefinition[0].approvedSkippedTests).Count -gt 0)
            if ($requiresSkipDisposition) { $errors.Add("missing skipped-test disposition: $($result.id)") } else { $skipEvidenceValid = $true }
        } elseif ($gateDefinition.Count -eq 1) {
            $expectedSkipDisposition = Get-Ov01SkipDisposition -Gate $gateDefinition[0] -ActualSkippedTests @($result.skipDisposition.actual) -ReportedSkipped ([int]$result.totals.skipped) -RepoRoot $validationRepoRoot
            $reportedSkipJson = $result.skipDisposition | ConvertTo-Json -Compress -Depth 20
            $expectedSkipJson = $expectedSkipDisposition | ConvertTo-Json -Compress -Depth 20
            if ($reportedSkipJson -cne $expectedSkipJson) { $errors.Add("skipped-test disposition mismatch: $($result.id)") }
            $skipEvidenceValid = [bool]$expectedSkipDisposition.valid -and $reportedSkipJson -ceq $expectedSkipJson
        }
        $surefireEvidenceValid = $true
        if ($gateDefinition.Count -eq 1 -and [string]$gateDefinition[0].parser -eq 'maven-surefire') {
            $surefireEvidenceValid = $null -ne $result.PSObject.Properties['surefireFreshReportCount'] -and [int]$result.surefireFreshReportCount -gt 0
            if (-not $surefireEvidenceValid) { $errors.Add("missing fresh Surefire report evidence: $($result.id)") }
        }
        if ([string]$result.status -eq 'PASS' -and ([int]$result.exitCode -ne 0 -or $reportedNonPass -ne 0 -or -not $skipEvidenceValid -or -not $surefireEvidenceValid -or [bool]$result.forbiddenOutcomeDetected -or [bool]$result.timedOut)) {
            $errors.Add("false-green gate: $($result.id)")
        }
        if ([string]$result.kind -eq 'test' -and [string]$result.status -eq 'PASS' -and [int]$result.totals.tests -lt 1) { $errors.Add("zero-test green gate: $($result.id)") }
        if ($null -eq $result.PSObject.Properties['toolchain']) {
            $errors.Add("missing gate toolchain identity: $($result.id)")
        } else {
            $toolchain = $result.toolchain
            $resolvedExecutable = if ($null -ne $toolchain.PSObject.Properties['resolvedExecutable']) { [string]$toolchain.resolvedExecutable } else { '' }
            $reportedExecutableSha = if ($null -ne $toolchain.PSObject.Properties['executableSha256']) { [string]$toolchain.executableSha256 } else { '' }
            if ([string]::IsNullOrWhiteSpace($resolvedExecutable)) {
                $errors.Add("gate toolchain executable path is missing: $($result.id)")
            } elseif (-not [System.IO.Path]::IsPathRooted($resolvedExecutable)) {
                $errors.Add("gate toolchain executable is not an absolute path: $($result.id)")
            } elseif (-not (Test-Path -LiteralPath $resolvedExecutable -PathType Leaf)) {
                $errors.Add("gate toolchain executable does not exist: $($result.id)")
            } else {
                $actualExecutableSha = Get-Ov01Sha256 -Path $resolvedExecutable
                if ($reportedExecutableSha -ne $actualExecutableSha) { $errors.Add("gate toolchain executable SHA mismatch: $($result.id)") }
            }
            if ($reportedExecutableSha -notmatch '^[0-9a-fA-F]{64}$') { $errors.Add("gate toolchain executable SHA is invalid: $($result.id)") }
            $versionExitCodeValid = $null -ne $toolchain.PSObject.Properties['versionExitCode'] -and $null -ne $toolchain.versionExitCode -and [int]$toolchain.versionExitCode -eq 0
            if (-not $versionExitCodeValid) { $errors.Add("gate toolchain version command failed: $($result.id)") }
            $versionOutput = if ($null -ne $toolchain.PSObject.Properties['versionOutput']) { [string]$toolchain.versionOutput } else { '' }
            $versionOutputSha256 = if ($null -ne $toolchain.PSObject.Properties['versionOutputSha256']) { [string]$toolchain.versionOutputSha256 } else { '' }
            $normalizedVersionOutput = Normalize-Ov01VersionOutput -Text $versionOutput
            $actualVersionOutputSha256 = Get-Ov01StringSha256 -Value $normalizedVersionOutput
            $expectedVersionRegex = if ($null -ne $toolchain.PSObject.Properties['expectedVersionRegex']) { [string]$toolchain.expectedVersionRegex } else { '' }
            if ([string]::IsNullOrWhiteSpace($versionOutput)) { $errors.Add("gate toolchain version output is empty: $($result.id)") }
            if ($versionOutputSha256 -notmatch '^[0-9a-fA-F]{64}$') {
                $errors.Add("gate toolchain version output SHA is invalid: $($result.id)")
            } elseif ($versionOutputSha256 -cne $actualVersionOutputSha256) {
                $errors.Add("gate toolchain version output SHA mismatch: $($result.id)")
            }
            if ([string]::IsNullOrWhiteSpace($expectedVersionRegex)) {
                $errors.Add("gate toolchain expected version regex is empty: $($result.id)")
            } else {
                try {
                    if (-not [regex]::IsMatch($versionOutput, $expectedVersionRegex)) { $errors.Add("gate toolchain version output mismatch: $($result.id)") }
                } catch { $errors.Add("gate toolchain expected version regex is invalid: $($result.id)") }
            }
            if ($null -eq $toolchain.PSObject.Properties['versionMatched'] -or -not [bool]$toolchain.versionMatched) { $errors.Add("toolchain version mismatch on gate: $($result.id)") }
            if ($gateDefinition.Count -eq 1) {
                $expectedConfiguration = if ([bool]$result.fallbackUsed -and $null -ne $gateDefinition[0].PSObject.Properties['fallback']) { $gateDefinition[0].fallback } else { $gateDefinition[0] }
                if ($expectedVersionRegex -ne [string]$expectedConfiguration.expectedVersionRegex) { $errors.Add("gate toolchain version contract mismatch: $($result.id)") }
                [string[]]$reportedVersionArguments = @(if ($null -ne $toolchain.PSObject.Properties['versionArguments']) { $toolchain.versionArguments | ForEach-Object { [string]$_ } })
                [string[]]$expectedVersionArguments = @($expectedConfiguration.versionArguments | ForEach-Object { [string]$_ })
                $expectedFlutterTool = $null
                if ($null -ne $gateDefinition[0].PSObject.Properties['runnerTool'] -and [string]$gateDefinition[0].runnerTool -eq 'flutter-dart-snapshot') {
                    try {
                        if (-not [System.IO.Path]::IsPathRooted([string]$gateDefinition[0].executable)) { throw 'configured Flutter launcher is not absolute' }
                        $expectedFlutterTool = Resolve-Ov01FlutterDartTool -ConfiguredFlutterExecutable ([string]$gateDefinition[0].executable)
                        $expectedVersionArguments = @([string]$expectedFlutterTool.flutterSnapshot) + $expectedVersionArguments
                        foreach ($field in @('runnerTool', 'configuredFlutterExecutable', 'configuredFlutterExecutableSha256', 'flutterRoot', 'flutterSnapshot', 'flutterSnapshotSha256')) {
                            if ($null -eq $toolchain.PSObject.Properties[$field] -or [string]$toolchain.$field -ne [string]$expectedFlutterTool.$field) {
                                $errors.Add("Flutter Dart snapshot identity mismatch: $($result.id)/$field")
                            }
                        }
                        if ([string]$toolchain.resolvedExecutable -ne [string]$expectedFlutterTool.dartExecutable -or [string]$toolchain.executableSha256 -ne [string]$expectedFlutterTool.dartExecutableSha256) {
                            $errors.Add("Flutter Dart executable identity mismatch: $($result.id)")
                        }
                        $expectedVersionEnvironmentEvidence = Get-Ov01FlutterVersionEnvironmentEvidence
                        if ($null -eq $toolchain.PSObject.Properties['versionEnvironmentEvidence']) {
                            $errors.Add("missing Flutter version environment evidence: $($result.id)")
                        } else {
                            $actualVersionEnvironmentKeys = @($toolchain.versionEnvironmentEvidence.PSObject.Properties | ForEach-Object { [string]$_.Name })
                            $expectedVersionEnvironmentKeys = @($expectedVersionEnvironmentEvidence.PSObject.Properties | ForEach-Object { [string]$_.Name })
                            if ($actualVersionEnvironmentKeys.Count -ne $expectedVersionEnvironmentKeys.Count -or @(Compare-Object $expectedVersionEnvironmentKeys $actualVersionEnvironmentKeys).Count -ne 0) {
                                $errors.Add("Flutter version environment evidence keys mismatch: $($result.id)")
                            }
                            foreach ($key in $expectedVersionEnvironmentKeys) {
                                if ($null -eq $toolchain.versionEnvironmentEvidence.PSObject.Properties[$key] -or [string]$toolchain.versionEnvironmentEvidence.$key -cne [string]$expectedVersionEnvironmentEvidence.$key) {
                                    $errors.Add("Flutter version environment evidence value mismatch: $($result.id)/$key")
                                }
                            }
                        }
                    } catch { $errors.Add("Flutter Dart snapshot identity validation failed: $($result.id)/$($_.Exception.Message)") }
                }
                $versionArgumentsMatch = $reportedVersionArguments.Count -eq $expectedVersionArguments.Count -and @(Compare-Object $expectedVersionArguments $reportedVersionArguments -SyncWindow 0).Count -eq 0
                if (-not $versionArgumentsMatch) {
                    $errors.Add("gate toolchain version arguments mismatch: $($result.id)")
                }
                if ($null -ne $expectedFlutterTool -and $versionArgumentsMatch) {
                    $replayStateDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ('ov01-flutter-version-' + [Guid]::NewGuid().ToString('N'))
                    try {
                        [void](New-Item -ItemType Directory -Path $replayStateDirectory)
                        $replayEnvironment = Get-Ov01FlutterVersionEnvironment -Base (Get-Ov01GateEnvironment -Gate $expectedConfiguration) -StateDirectory $replayStateDirectory
                        $replayWorkingDirectory = if ($null -ne $result.PSObject.Properties['workingDirectory']) {
                            Resolve-Ov01RepoPath -Root $validationRepoRoot -RelativePath ([string]$result.workingDirectory)
                        } else {
                            [string]$expectedFlutterTool.flutterRoot
                        }
                        $replay = Invoke-Ov01Process -Executable ([string]$expectedFlutterTool.dartExecutable) -Arguments $reportedVersionArguments -WorkingDirectory $replayWorkingDirectory -Environment $replayEnvironment -TimeoutSeconds 120
                        if ([bool]$replay.timedOut) {
                            $errors.Add("Flutter version replay timed out: $($result.id)")
                        } elseif ([int]$replay.exitCode -ne 0) {
                            $errors.Add("Flutter version replay failed: $($result.id)/exit=$($replay.exitCode)")
                        } else {
                            $normalizedReplayOutput = Normalize-Ov01VersionOutput -Text (Protect-Ov01Text -Text ([string]$replay.output))
                            $replayOutputSha256 = Get-Ov01StringSha256 -Value $normalizedReplayOutput
                            if ($normalizedReplayOutput -cne $normalizedVersionOutput -or $replayOutputSha256 -cne $versionOutputSha256) {
                                $errors.Add("Flutter version output replay mismatch: $($result.id)")
                            }
                        }
                    } catch {
                        $errors.Add("Flutter version replay validation failed: $($result.id)/$($_.Exception.Message)")
                    } finally {
                        if (Test-Path -LiteralPath $replayStateDirectory -PathType Container) {
                            Remove-Item -LiteralPath $replayStateDirectory -Recurse -Force -ErrorAction SilentlyContinue
                        }
                    }
                }
            }
        }

        if ($null -eq $result.PSObject.Properties['environmentEvidence']) {
            $errors.Add("missing gate environment evidence: $($result.id)")
        } elseif ($gateDefinition.Count -eq 1) {
            $actualEnvironmentKeys = @($result.environmentEvidence.PSObject.Properties | ForEach-Object { [string]$_.Name })
            [string[]]$expectedEnvironmentKeys = @(if ($null -ne $gateDefinition[0].PSObject.Properties['evidenceEnvironmentKeys']) { $gateDefinition[0].evidenceEnvironmentKeys | ForEach-Object { [string]$_ } })
            if ($actualEnvironmentKeys.Count -ne $expectedEnvironmentKeys.Count -or @(Compare-Object $expectedEnvironmentKeys $actualEnvironmentKeys).Count -ne 0) {
                $errors.Add("gate environment evidence keys mismatch: $($result.id)")
            }
            foreach ($key in $expectedEnvironmentKeys) {
                if ($null -eq $result.environmentEvidence.PSObject.Properties[$key] -or [string]$result.environmentEvidence.$key -ne [string]$gateDefinition[0].environment.$key) {
                    $errors.Add("gate environment evidence value mismatch: $($result.id)/$key")
                }
            }
        }
        if ($gateDefinition.Count -eq 1) {
            $isolationCheck = Test-Ov01PytestRuntimeIsolation -Result $result -GateDefinition $gateDefinition[0] -EvidenceDirectory $EvidenceDirectory
            foreach ($isolationError in @($isolationCheck.errors)) { $errors.Add([string]$isolationError) }
            if (-not [string]::IsNullOrWhiteSpace($evidenceFull)) {
                try {
                    $semanticReplay = Test-Ov01GateSemanticReplay -Gate $gateDefinition[0] -Result $result -Registry $Registry -RepoRoot $validationRepoRoot -EvidenceDirectory $evidenceFull -SourceIdentity $Report.sourceIdentity -SelectedScenarioIds $selectedScenarioIds
                    foreach ($semanticError in @($semanticReplay.errors)) { $errors.Add([string]$semanticError) }
                } catch { $errors.Add("semantic replay failed: $($result.id)/$($_.Exception.Message)") }
            }
        }
    }
    foreach ($scenario in @($Report.scenarioResults)) {
        if ($allowed -notcontains [string]$scenario.status) { $errors.Add("invalid scenario status: $($scenario.id)") }
        $definition = @($Registry.scenarios | Where-Object { [string]$_.id -eq [string]$scenario.id } | Select-Object -First 1)
        $required = if ($definition.Count -eq 1) { @($definition[0].gateIds) } else { @() }
        $gateResults = @($Report.gateResults | Where-Object { $required -contains [string]$_.id })
        $allRequiredPassed = ($gateResults.Count -eq @($required).Count) -and (@($gateResults | Where-Object status -ne 'PASS').Count -eq 0)
        if ([string]$scenario.status -eq 'PASS' -and -not $allRequiredPassed) { $errors.Add("false-green scenario: $($scenario.id)") }
        if ($definition.Count -eq 1) {
            foreach ($selector in @($definition[0].executableSelectors)) {
                $matches = @($Report.gateResults | Where-Object { [string]$_.id -eq [string]$selector.gateId } | ForEach-Object { @($_.selectorResults) } | Where-Object {
                    [string]$_.scenarioId -eq [string]$scenario.id -and [string]$_.gateId -eq [string]$selector.gateId -and [string]$_.selector -eq [string]$selector.selector -and [bool]$_.matched
                })
                if ([string]$scenario.status -eq 'PASS' -and $matches.Count -ne 1) { $errors.Add("scenario executable selector is not proven exactly once: $($scenario.id)/$($selector.gateId)/$($selector.selector)") }
            }
        }
    }
    $artifactPairs = @($Report.artifacts | ForEach-Object { "$( [string]$_.id)`t$( [string]$_.path)" })
    foreach ($duplicate in @($artifactPairs | Group-Object | Where-Object Count -gt 1)) { $errors.Add("duplicate artifact identity/path: $($duplicate.Name)") }
    $expectedArtifactIds = @($Registry.artifacts | Where-Object { [string]$_.kind -eq 'input' -or @($_.requiredForGateIds | Where-Object { $selectedGateIds -contains [string]$_ }).Count -gt 0 } | ForEach-Object { [string]$_.id })
    $actualArtifactIds = @($Report.artifacts | ForEach-Object { [string]$_.id } | Select-Object -Unique)
    foreach ($id in $expectedArtifactIds) {
        if ($actualArtifactIds -notcontains $id) { $errors.Add("required artifact identity missing from report: $id") }
        $definition = @($Registry.artifacts | Where-Object { [string]$_.id -eq $id } | Select-Object -First 1)
        $records = @($Report.artifacts | Where-Object { [string]$_.id -eq $id })
        if ($definition.Count -eq 1 -and -not [bool]$definition[0].allowMany -and $records.Count -ne 1) { $errors.Add("artifact identity must occur exactly once in report: $id") }
    }
    foreach ($id in $actualArtifactIds) { if ($knownArtifactIds -notcontains $id -or $expectedArtifactIds -notcontains $id) { $errors.Add("unexpected artifact identity in report: $id") } }
    foreach ($artifact in @($Report.artifacts)) {
        $definition = @($Registry.artifacts | Where-Object { [string]$_.id -eq [string]$artifact.id } | Select-Object -First 1)
        if ([string]::IsNullOrWhiteSpace([string]$artifact.path)) { $errors.Add("artifact path is empty: $($artifact.id)") }
        elseif ([string]$artifact.id -eq 'OV01-ART-REGISTRY' -and [string]$artifact.path -ne [string]$Report.sourceIdentity.registryRelativePath) { $errors.Add("registry artifact path does not match actual registry identity: $($artifact.path)") }
        elseif ($definition.Count -eq 1 -and [string]$artifact.id -ne 'OV01-ART-REGISTRY' -and [string]$artifact.path -notlike [string]$definition[0].path) { $errors.Add("artifact path does not match registry contract: $($artifact.id)/$($artifact.path)") }
        if ([string]$artifact.sourceIdentity -ne [string]$Report.sourceIdentity.compositeSha256) { $errors.Add("artifact build binding mismatch: $($artifact.path)") }
        if ([string]$artifact.registryRelativePath -ne [string]$Report.sourceIdentity.registryRelativePath) { $errors.Add("artifact registry path binding mismatch: $($artifact.path)") }
        if ([string]$artifact.registrySha256 -ne [string]$Report.sourceIdentity.registrySha256) { $errors.Add("artifact registry SHA binding mismatch: $($artifact.path)") }
        if ([string]$artifact.id -eq 'OV01-ART-REGISTRY' -and [string]$artifact.sha256 -ne [string]$Report.sourceIdentity.registrySha256) { $errors.Add("registry artifact content SHA does not match actual registry identity: $($artifact.path)") }
        try {
            $artifactFull = Resolve-Ov01RepoPath -Root $validationRepoRoot -RelativePath ([string]$artifact.path)
            if (-not (Test-Path -LiteralPath $artifactFull -PathType Leaf)) {
                $errors.Add("artifact file is missing: $($artifact.id)/$($artifact.path)")
            } else {
                $actualArtifactBytes = [int64](Get-Item -LiteralPath $artifactFull).Length
                $actualArtifactSha = Get-Ov01Sha256 -Path $artifactFull
                if ($null -eq $artifact.PSObject.Properties['bytes'] -or [int64]$artifact.bytes -ne $actualArtifactBytes) { $errors.Add("artifact byte length mismatch: $($artifact.id)/$($artifact.path)") }
                if ($null -eq $artifact.PSObject.Properties['sha256'] -or [string]$artifact.sha256 -cne $actualArtifactSha) { $errors.Add("artifact SHA-256 mismatch: $($artifact.id)/$($artifact.path)") }
            }
        } catch { $errors.Add("artifact path escapes repository: $($artifact.id)/$($artifact.path)") }
    }
    if (@($Report.artifactErrors).Count -gt 0) { foreach ($errorMessage in @($Report.artifactErrors)) { $errors.Add([string]$errorMessage) } }
    $expectedExecutionPass = @($Report.scenarioResults | Where-Object status -ne 'PASS').Count -eq 0 -and @($Report.gateResults | Where-Object status -ne 'PASS').Count -eq 0 -and $errors.Count -eq 0
    if ([string]$Report.runMode -eq 'Diagnostic') {
        if ([string]$Report.overallStatus -ne 'NOT_APPLICABLE') { $errors.Add('diagnostic overall status must be NOT_APPLICABLE') }
        if (@('PASS', 'FAIL') -notcontains [string]$Report.diagnosticStatus) { $errors.Add('diagnostic status must be PASS or FAIL') }
        if ([string]$Report.diagnosticStatus -eq 'PASS' -and -not $expectedExecutionPass) { $errors.Add('false-green diagnostic status') }
        if ([string]$Report.diagnosticStatus -ne 'PASS' -and $expectedExecutionPass) { $errors.Add('diagnostic status is not PASS although all diagnostic contracts passed') }
    } else {
        if ([string]$Report.overallStatus -eq 'PASS' -and -not $expectedExecutionPass) { $errors.Add('false-green overall status') }
        if ([string]$Report.overallStatus -ne 'PASS' -and $expectedExecutionPass) { $errors.Add('overall status is not PASS although all selected contracts passed') }
    }

    $serialized = $Report | ConvertTo-Json -Depth 30
    foreach ($finding in @(Test-Ov01Leak -Text $serialized)) { $errors.Add("leak finding in report: $finding") }
    return [pscustomobject]@{ valid = ($errors.Count -eq 0); errors = $errors.ToArray() }
}

function New-Ov01ClosedSetManifest {
    param([Parameter(Mandatory = $true)][string]$EvidenceDirectory)

    $evidenceFull = [System.IO.Path]::GetFullPath($EvidenceDirectory)
    $manifestPath = Join-Path $evidenceFull 'evidence-manifest.json'
    $files = @(Get-ChildItem -LiteralPath $evidenceFull -Recurse -File | Where-Object FullName -ne $manifestPath | Sort-Object FullName)
    if ($files.Count -eq 0) { throw 'Cannot seal an empty evidence directory' }
    $entries = foreach ($file in $files) {
        [pscustomobject]@{
            path = ConvertTo-Ov01RelativePath -Root $evidenceFull -Path $file.FullName
            bytes = [int64]$file.Length
            sha256 = Get-Ov01Sha256 -Path $file.FullName
        }
    }
    $manifest = [pscustomobject]@{
        schemaVersion = 1
        algorithm = 'SHA-256'
        closedSet = $true
        excludedSelf = 'evidence-manifest.json'
        files = @($entries)
    }
    [System.IO.File]::WriteAllText($manifestPath, ($manifest | ConvertTo-Json -Depth 10) + "`n", (New-Object System.Text.UTF8Encoding($false)))
    return $manifest
}

function Test-Ov01ClosedSetManifest {
    param([Parameter(Mandatory = $true)][string]$EvidenceDirectory)

    $evidenceFull = [System.IO.Path]::GetFullPath($EvidenceDirectory)
    $manifestPath = Join-Path $evidenceFull 'evidence-manifest.json'
    $errors = New-Object System.Collections.Generic.List[string]
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) { return [pscustomobject]@{ valid = $false; errors = @('evidence-manifest.json is missing') } }
    $manifest = Get-Content -Encoding UTF8 -Raw -LiteralPath $manifestPath | ConvertFrom-Json
    $expected = @($manifest.files | ForEach-Object { [string]$_.path } | Sort-Object)
    $actualFiles = @(Get-ChildItem -LiteralPath $evidenceFull -Recurse -File | Where-Object FullName -ne $manifestPath | Sort-Object FullName)
    $actual = @($actualFiles | ForEach-Object { ConvertTo-Ov01RelativePath -Root $evidenceFull -Path $_.FullName } | Sort-Object)
    foreach ($path in $expected) { if ($actual -notcontains $path) { $errors.Add("closed-set removal detected: $path") } }
    foreach ($path in $actual) { if ($expected -notcontains $path) { $errors.Add("closed-set addition detected: $path") } }
    foreach ($entry in @($manifest.files)) {
        $filePath = Resolve-Ov01RepoPath -Root $evidenceFull -RelativePath ([string]$entry.path)
        if (Test-Path -LiteralPath $filePath -PathType Leaf) {
            $hash = Get-Ov01Sha256 -Path $filePath
            if ($hash -ne [string]$entry.sha256) { $errors.Add("closed-set mutation detected: $($entry.path)") }
            if ((Get-Item -LiteralPath $filePath).Length -ne [int64]$entry.bytes) { $errors.Add("closed-set length mismatch: $($entry.path)") }
        }
    }
    return [pscustomobject]@{ valid = ($errors.Count -eq 0); errors = $errors.ToArray() }
}

Export-ModuleMember -Function Get-Ov01Sha256, Get-Ov01StringSha256, ConvertTo-Ov01RelativePath, Resolve-Ov01RepoPath, Resolve-Ov01RepoGlob, Protect-Ov01Text, Test-Ov01Leak, Read-Ov01Registry, Get-Ov01CanonicalReleaseGateIds, Test-Ov01Registry, Get-Ov01SourceIdentity, Test-Ov01SourceIdentityStable, Get-Ov01ObjectSha256, Write-Ov01AtomicJson, Get-Ov01CheckpointKeyPathFingerprint, Get-Ov01CheckpointAuthenticationContext, Get-Ov01CheckpointHmacSha256, Write-Ov01AuthenticatedCheckpoint, Get-Ov01AuthenticatedCheckpointArtifactInventory, New-Ov01ActiveGateState, Get-Ov01EvidenceFileRecords, Test-Ov01GateSemanticReplay, Test-Ov01ReleaseCheckpoint, Get-Ov01CommandTotals, Get-Ov01SurefireXmlEvidence, Resolve-Ov01GateExecutable, Resolve-Ov01FlutterDartTool, Get-Ov01GateEnvironment, ConvertTo-Ov01ProcessArgument, ConvertTo-Ov01NormalizedEnvironmentEntries, Invoke-Ov01Process, Invoke-Ov01Gate, Get-Ov01ArtifactRecords, Get-Ov01SkipDisposition, Test-Ov01PytestRuntimeIsolation, Test-Ov01ManualEvidence, Test-Ov01RunReport, New-Ov01ClosedSetManifest, Test-Ov01ClosedSetManifest
