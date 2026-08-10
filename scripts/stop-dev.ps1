[CmdletBinding()]
param(
    [switch]$KeepInfrastructure
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$projectPorts = @(8761, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 9093, 9094)

$connections = @(
    Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalPort -in $projectPorts }
)

$processIds = @(
    $connections |
        Select-Object -ExpandProperty OwningProcess -Unique |
        Where-Object { $_ -notin @(0, 4) }
)

foreach ($processId in $processIds) {
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($null -ne $process) {
        $ownedPorts = @(
            $connections |
                Where-Object { $_.OwningProcess -eq $processId } |
                Select-Object -ExpandProperty LocalPort -Unique
        ) -join ', '

        Write-Host "Stopping $($process.ProcessName) (PID $processId) on port(s): $ownedPorts" -ForegroundColor Yellow
        Stop-Process -Id $processId -Force -ErrorAction Stop
    }
}

Start-Sleep -Milliseconds 500
$remaining = @(
    Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalPort -in $projectPorts }
)

if ($remaining.Count -gt 0) {
    $remainingPorts = ($remaining | Select-Object -ExpandProperty LocalPort -Unique) -join ', '
    throw "Could not free project port(s): $remainingPorts. Run PowerShell as Administrator or inspect the owning process."
}

if (-not $KeepInfrastructure) {
    Push-Location $projectRoot
    try {
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            docker compose down
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host 'Project Java ports are free.' -ForegroundColor Green
