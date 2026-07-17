$ErrorActionPreference = 'Stop'

$webRoot = Split-Path -Parent $PSScriptRoot
$apiRoot = Resolve-Path (Join-Path $webRoot '..\CareBridgeAPI')
$envFile = Join-Path $apiRoot '.env'
$backendProcess = $null
$importedEnvironmentNames = [System.Collections.Generic.HashSet[string]]::new()
$databaseContainer = "carebridge-federated-e2e-$PID"
$databasePort = 55432
$databaseContainerStarted = $false

function Import-DotEnv([string] $path) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Backend environment file not found: $path"
    }

    Get-Content -LiteralPath $path | ForEach-Object {
        if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $name = $matches[1]
            $value = $matches[2].Trim()
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            Set-Item -Path "Env:$name" -Value $value
            $null = $script:importedEnvironmentNames.Add($name)
        }
    }
}

function Stop-ProcessTree([int] $processId) {
    Get-CimInstance Win32_Process -Filter "ParentProcessId = $processId" -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-ProcessTree $_.ProcessId }
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

try {
    Import-DotEnv $envFile

    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker CLI was not found. Start Docker Desktop and ensure docker is on PATH.'
    }
    if (Get-NetTCPConnection -State Listen -LocalPort $databasePort -ErrorAction SilentlyContinue) {
        throw "Port $databasePort is already in use; cannot start the isolated E2E database."
    }

    docker run --name $databaseContainer `
        --env POSTGRES_DB=carebridge_e2e `
        --env POSTGRES_USER=carebridge `
        --env POSTGRES_PASSWORD=carebridge_e2e `
        --publish "127.0.0.1:${databasePort}:5432" `
        --detach postgres:16-alpine | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to start the isolated PostgreSQL container.'
    }
    $databaseContainerStarted = $true

    $databaseReady = $false
    for ($attempt = 0; $attempt -lt 120; $attempt += 1) {
        docker exec $databaseContainer pg_isready --username carebridge --dbname carebridge_e2e *> $null
        if ($LASTEXITCODE -eq 0) {
            $databaseReady = $true
            break
        }
        Start-Sleep -Milliseconds 500
    }
    if (-not $databaseReady) {
        throw 'The isolated PostgreSQL container did not become ready.'
    }

    $env:SERVER_PORT = '8081'
    $env:SUPABASE_DB_URL = "jdbc:postgresql://127.0.0.1:$databasePort/carebridge_e2e"
    $env:SUPABASE_DB_USERNAME = 'carebridge'
    $env:SUPABASE_DB_PASSWORD = 'carebridge_e2e'
    $env:CAREBRIDGE_FCM_ENABLED = 'false'
    $env:CAREBRIDGE_FIREBASE_AUTH_EMULATOR_ENABLED = 'true'
    $env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
    $env:FIREBASE_PROJECT_ID = 'demo-carebridge'
    $env:CAREBRIDGE_DEV_SEED_ENABLED = 'false'

    $targetDirectory = Join-Path $apiRoot 'target'
    New-Item -ItemType Directory -Path $targetDirectory -Force | Out-Null
    $stdout = Join-Path $targetDirectory 'federated-emulator-e2e.out.log'
    $stderr = Join-Path $targetDirectory 'federated-emulator-e2e.err.log'

    $backendProcess = Start-Process -FilePath (Join-Path $apiRoot 'mvnw.cmd') `
        -ArgumentList @('spring-boot:run', '-Dspring-boot.run.arguments=--server.port=8081') `
        -WorkingDirectory $apiRoot `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden `
        -PassThru

    $ready = $false
    for ($attempt = 0; $attempt -lt 180; $attempt += 1) {
        if (Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue) {
            $ready = $true
            break
        }
        if ($backendProcess.HasExited) {
            throw "Backend exited before port 8081 became ready. See $stdout"
        }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) {
        throw "Backend did not become ready on port 8081. See $stdout"
    }

    # The backend process already inherited its configuration. Do not pass API/database
    # secrets to Firebase CLI or Playwright, whose debug logs may print child environments.
    foreach ($name in $importedEnvironmentNames) {
        Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
    }
    Remove-Item -Path 'Env:DEBUG' -ErrorAction SilentlyContinue
    Remove-Item -Path 'Env:SERVER_PORT' -ErrorAction SilentlyContinue
    Remove-Item -Path 'Env:CAREBRIDGE_FIREBASE_AUTH_EMULATOR_ENABLED' -ErrorAction SilentlyContinue
    Remove-Item -Path 'Env:FIREBASE_PROJECT_ID' -ErrorAction SilentlyContinue

    Push-Location $webRoot
    try {
        npm run test:e2e:federated-emulator
        if ($LASTEXITCODE -ne 0) {
            throw "Federated emulator E2E failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
} finally {
    $listener = Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue
    if ($listener) {
        Stop-ProcessTree $listener.OwningProcess
    }
    if ($backendProcess -and -not $backendProcess.HasExited) {
        Stop-ProcessTree $backendProcess.Id
    }
    if ($databaseContainerStarted) {
        docker rm --force $databaseContainer *> $null
    }
}
