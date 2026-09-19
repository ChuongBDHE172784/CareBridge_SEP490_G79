Get-Content .env | Where-Object { $_ -match '^\s*([^#=]+)\s*=\s*(.*)$' } | ForEach-Object {
    $k = $matches[1].Trim()
    $v = $matches[2].Trim()
    if ($v -match '^"(.*)"$' -or $v -match "^'(.*)'$") { $v = $matches[1] }
    [System.Environment]::SetEnvironmentVariable($k, $v, [System.EnvironmentVariableTarget]::Process)
}
cmd.exe /c "mvnw.cmd spring-boot:run"
