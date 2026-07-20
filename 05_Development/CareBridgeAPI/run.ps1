# run.ps1 — load .env then start Spring Boot
param()
$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) { Write-Error ".env not found at $envFile"; exit 1 }

# Parse KEY=VALUE lines (skip comments and blank lines)
foreach ($line in Get-Content $envFile) {
    $trimmed = $line.Trim()
    if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
    if ($trimmed -match '^\{') { continue }  # skip raw JSON block
    $kv = $trimmed -split '=', 2
    if ($kv.Length -eq 2) {
        $name  = $kv[0].Trim()
        $value = $kv[1].Trim().Trim('"')
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        Write-Host "[env] $name set"
    }
}

Set-Location $PSScriptRoot
& .\mvnw.cmd spring-boot:run "-Dmaven.test.skip=true"
