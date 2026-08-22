# Chay app tren dien thoai that, noi thang toi server AWS.
#
# Vi sao can script nay: mac dinh trong api_client.dart, Android dung
# http://10.0.2.2:8080 - do la loopback cua MAY AO, dien thoai that hieu
# thanh chinh no nen luon that bai. Phai truyen API_BASE_URL bang dart-define.
#
# TRACKASIA_API_KEY doc tu .env cua backend, khong viet thang vao day,
# de khoa khong loc vao git hay lich su terminal.
#
# Cach dung:
#   .\run_phone.ps1                 # tu chon dien thoai dau tien tim thay
#   .\run_phone.ps1 -DeviceId <id>  # chi dinh may cu the
#   .\run_phone.ps1 -Debug          # chay debug thay vi release (co hot reload)

param(
    [string]$DeviceId = "",
    [switch]$Debug
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$ApiBaseUrl = "https://api.carebridgevn.site"

# --- Lay khoa TrackAsia tu .env cua backend ---
$EnvFile = Join-Path $PSScriptRoot "..\CareBridgeAPI\.env"
$TrackAsiaKey = ""
if (Test-Path $EnvFile) {
    $line = Select-String -Path $EnvFile -Pattern '^TRACKASIA_API_KEY=' | Select-Object -First 1
    if ($line) { $TrackAsiaKey = ($line.Line -split '=', 2)[1].Trim() }
}
if ([string]::IsNullOrWhiteSpace($TrackAsiaKey)) {
    Write-Host "Canh bao: khong tim thay TRACKASIA_API_KEY - ban do se khong hien." -ForegroundColor Yellow
} else {
    Write-Host "TrackAsia key: da nap ($($TrackAsiaKey.Length) ky tu)" -ForegroundColor Green
}

# --- Chon thiet bi ---
if ([string]::IsNullOrWhiteSpace($DeviceId)) {
    Write-Host "Dang tim dien thoai..." -ForegroundColor Cyan
    $raw = flutter devices --machine 2>$null | Out-String
    $devices = @()
    try { $devices = $raw | ConvertFrom-Json } catch { }

    $phone = $devices | Where-Object {
        -not $_.emulator -and
        ($_.targetPlatform -like 'android*' -or $_.targetPlatform -like 'ios*')
    } | Select-Object -First 1

    if (-not $phone) {
        Write-Host ""
        Write-Host "Khong thay dien thoai that nao." -ForegroundColor Red
        Write-Host "Kiem tra lai:" -ForegroundColor Yellow
        Write-Host "  Android - bat Developer options, bat USB debugging, cam cap,"
        Write-Host "            chon 'Allow' tren man hinh dien thoai, roi chay: adb devices"
        Write-Host "  iPhone  - can macOS de build, Windows khong build duoc iOS"
        exit 1
    }
    $DeviceId = $phone.id
    Write-Host "Thiet bi: $($phone.name)  [$DeviceId]" -ForegroundColor Green
}

$Mode = if ($Debug) { "--debug" } else { "--release" }

Write-Host ""
Write-Host "API_BASE_URL = $ApiBaseUrl" -ForegroundColor Cyan
Write-Host "Che do       = $Mode" -ForegroundColor Cyan
Write-Host ""

flutter run $Mode -d $DeviceId `
    --dart-define=API_BASE_URL=$ApiBaseUrl `
    --dart-define=TRACKASIA_API_KEY=$TrackAsiaKey
