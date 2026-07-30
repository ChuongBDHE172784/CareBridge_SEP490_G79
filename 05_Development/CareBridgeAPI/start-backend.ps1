$ErrorActionPreference='SilentlyContinue'
$jarPath = "D:\Do_aN\05_Development\CareBridgeAPI\target\backend-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jarPath)) { Write-Host "JAR_NOT_FOUND: $jarPath"; exit 1 }
$envVars = @{}
Get-Content "D:\Do_aN\05_Development\CareBridgeAPI\.env" | ForEach-Object {
  $line = $_.Trim()
  if ($line -and $line -notmatch '^\s*#') {
    $kv = $line.Split('=',2)
    if ($kv.Length -eq 2) {
      $key = $kv[0].Trim()
      $value = $kv[1].Trim()
      $value = $value -replace '^"(.*)"$','$1'
      $envVars[$key] = $value
    }
  }
}
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "java"
$psi.Arguments = "-jar `"$jarPath`""
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Hidden
foreach ($k in $envVars.Keys) {
    $psi.EnvironmentVariables[$k] = $envVars[$k]
}
$p = [System.Diagnostics.Process]::Start($psi)
Write-Host "PID: $($p.Id)"
Start-Sleep -Seconds 12
$listening = netstat -ano | Select-String ':8080.*LISTENING'
if ($listening) { Write-Host 'BACKEND_LISTENING_8080' } else { Write-Host 'BACKEND_NOT_LISTENING' }
$out = $p.StandardOutput.ReadToEnd()
$err = $p.StandardError.ReadToEnd()
if ($out) { Write-Host "STDOUT: $out" }
if ($err) { Write-Host "STDERR: $err" }
