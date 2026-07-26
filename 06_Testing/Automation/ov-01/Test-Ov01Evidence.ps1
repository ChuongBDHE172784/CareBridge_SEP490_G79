[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('Registry', 'Report', 'Seal')][string]$Mode,
    [string]$RegistryPath,
    [string]$ReportPath,
    [string]$EvidenceDirectory,
    [string]$CheckpointKeyPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'Ov01Evidence.psm1') -Force
if ([string]::IsNullOrWhiteSpace($RegistryPath)) { $RegistryPath = Join-Path $PSScriptRoot 'ov01-scenario-registry.json' }
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
$registry = Read-Ov01Registry -Path $RegistryPath

switch ($Mode) {
    'Registry' { $result = Test-Ov01Registry -Registry $registry -RepoRoot $repoRoot }
    'Report' {
        if ([string]::IsNullOrWhiteSpace($ReportPath)) { throw '-ReportPath is required for Report mode' }
        $report = Get-Content -Encoding UTF8 -Raw -LiteralPath $ReportPath | ConvertFrom-Json
        $reportDirectory = [System.IO.Path]::GetDirectoryName([System.IO.Path]::GetFullPath($ReportPath))
        $result = Test-Ov01RunReport -Report $report -Registry $registry -EvidenceDirectory $reportDirectory -CheckpointKeyPath $CheckpointKeyPath
    }
    'Seal' {
        if ([string]::IsNullOrWhiteSpace($EvidenceDirectory)) { throw '-EvidenceDirectory is required for Seal mode' }
        $result = Test-Ov01ClosedSetManifest -EvidenceDirectory $EvidenceDirectory
    }
}
$result | ConvertTo-Json -Depth 20
if (-not $result.valid) { exit 1 }
exit 0
