[CmdletBinding()]
param()

$ErrorActionPreference = 'Continue'
$failed = 0
$projectRoot = Split-Path -Parent $PSScriptRoot
$bookingRealtimePort = 9093
$envPath = Join-Path $projectRoot '.env'

if (Test-Path $envPath) {
    $realtimeLine = Get-Content $envPath | Where-Object { $_ -match '^\s*BOOKING_REALTIME_PORT\s*=\s*\d+\s*$' } | Select-Object -First 1
    if ($realtimeLine -match '=\s*(\d+)') {
        $bookingRealtimePort = [int]$Matches[1]
    }
}

$checks = @(
    @{ Name = 'Eureka Server'; Port = 8761; Url = 'http://localhost:8761/' },
    @{ Name = 'API Gateway'; Port = 8080; Url = 'http://localhost:8080/health' },
    @{ Name = 'Auth Service'; Port = 8081 },
    @{ Name = 'Movie Service'; Port = 8082; Url = 'http://localhost:8082/actuator/health' },
    @{ Name = 'Booking Service'; Port = 8083; Url = 'http://localhost:8083/actuator/health' },
    @{ Name = 'Payment Service'; Port = 8084; Url = 'http://localhost:8084/health' },
    @{ Name = 'Notification Service'; Port = 8085; Url = 'http://localhost:8085/actuator/health' },
    @{ Name = 'User Service'; Port = 8086; Url = 'http://localhost:8086/health' },
    @{ Name = 'Promotion Service'; Port = 8087; Url = 'http://localhost:8087/actuator/health' },
    # Score actuator is protected by Spring Security, so a listening port is
    # the reliable startup check for this service in local development.
    @{ Name = 'Score Service'; Port = 8088 },
    @{ Name = 'Analytics Service'; Port = 8089; Url = 'http://localhost:8089/actuator/health' },
    @{ Name = 'Booking Realtime'; Port = $bookingRealtimePort }
)

function Write-Result([string]$name, [bool]$ok, [string]$detail) {
    if ($ok) {
        Write-Host ("[ OK ] {0} - {1}" -f $name, $detail) -ForegroundColor Green
    }
    else {
        Write-Host ("[FAIL] {0} - {1}" -f $name, $detail) -ForegroundColor Red
        $script:failed++
    }
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    Push-Location (Split-Path -Parent $PSScriptRoot)
    try {
        Write-Host '--- Docker infrastructure ---' -ForegroundColor Cyan
        docker compose ps
        if ($LASTEXITCODE -ne 0) {
            Write-Result 'Docker Compose' $false 'docker compose ps failed'
        }
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Result 'Docker' $false 'Docker was not found in PATH'
}

Write-Host ''
Write-Host '--- Java services ---' -ForegroundColor Cyan
foreach ($check in $checks) {
    $tcpOk = Test-NetConnection -ComputerName localhost -Port $check.Port -InformationLevel Quiet -WarningAction SilentlyContinue
    if (-not $tcpOk) {
        Write-Result $check.Name $false ("port {0} is not listening" -f $check.Port)
        continue
    }

    if ($check.Url) {
        try {
            $response = Invoke-WebRequest -Uri $check.Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                Write-Result $check.Name $true ("port {0}, HTTP {1}" -f $check.Port, $response.StatusCode)
            }
            else {
                Write-Result $check.Name $false ("port {0}, HTTP {1}" -f $check.Port, $response.StatusCode)
            }
        }
        catch {
            Write-Result $check.Name $false ("port {0} is open but health endpoint failed: {1}" -f $check.Port, $_.Exception.Message)
        }
    }
    else {
        Write-Result $check.Name $true ("port {0} is listening" -f $check.Port)
    }
}

Write-Host ''
Write-Host '--- Eureka registrations ---' -ForegroundColor Cyan
$expectedApps = @(
    'API-GATEWAY', 'AUTH-SERVICE', 'MOVIE-SERVICE', 'BOOKING-SERVICE',
    'PAYMENT-SERVICE', 'NOTIFICATION-SERVICE', 'USER-SERVICE',
    'PROMOTION-SERVICE', 'SCORE-SERVICE', 'ANALYTICS-SERVICE'
)

try {
    $eurekaResponse = Invoke-WebRequest -Uri 'http://localhost:8761/eureka/apps' -Headers @{ Accept = 'application/xml' } -UseBasicParsing -TimeoutSec 5
    foreach ($app in $expectedApps) {
        $registered = $eurekaResponse.Content -match ("<name>{0}</name>" -f [regex]::Escape($app))
        Write-Result "Eureka: $app" $registered ($(if ($registered) { 'registered' } else { 'not registered yet' }))
    }
}
catch {
    Write-Result 'Eureka registry' $false $_.Exception.Message
}

Write-Host ''
if ($failed -eq 0) {
    Write-Host 'All checks passed.' -ForegroundColor Green
}
else {
    Write-Host ("{0} check(s) failed. Check the corresponding VS Code terminal for the startup error." -f $failed) -ForegroundColor Red
}

exit $failed
