[CmdletBinding()]
param(
    [switch]$KeepInfrastructure
)

$ErrorActionPreference = 'Continue'
$pidFile = Join-Path $env:TEMP 'lorafilm-backend-pids.json'

if (Test-Path $pidFile) {
    $records = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
    foreach ($record in $records) {
        $process = Get-Process -Id ([int]$record.Pid) -ErrorAction SilentlyContinue
        if ($null -ne $process) {
            Write-Host "Stopping $($record.Name) (PID $($record.Pid))" -ForegroundColor Yellow
            & taskkill.exe /PID ([int]$record.Pid) /T /F *> $null
        }
    }
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

$stopScript = Join-Path $PSScriptRoot 'stop-dev.ps1'
if ($KeepInfrastructure) {
    & $stopScript -KeepInfrastructure
}
else {
    & $stopScript
}

Write-Host 'Backend processes stopped.' -ForegroundColor Green
