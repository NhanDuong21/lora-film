[CmdletBinding()]
param(
    [switch]$SkipDocker
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

# Stop only listeners on ports owned by this project. Docker infrastructure is
# intentionally kept running so the next start remains fast.
& (Join-Path $PSScriptRoot 'stop-dev.ps1') -KeepInfrastructure

if (-not $SkipDocker) {
    Push-Location $projectRoot
    try {
        if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
            throw 'Docker was not found in PATH. Start Docker Desktop and try again.'
        }

        docker compose up -d
        if ($LASTEXITCODE -ne 0) {
            throw 'Docker Compose could not start the local infrastructure.'
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host ''
Write-Host 'LoraFilm dev environment is ready.' -ForegroundColor Green
Write-Host 'Run: LoraFilm - backend (all) from VS Code Run and Debug.'
