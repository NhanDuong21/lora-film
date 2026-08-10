[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $env:TEMP 'lorafilm-backend-pids.json'
$logDirectory = Join-Path $env:TEMP 'lorafilm-backend-logs'
$envFile = Join-Path $projectRoot '.env'

function Test-TcpPort([int]$port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $asyncResult = $client.BeginConnect('127.0.0.1', $port, $null, $null)
        if (-not $asyncResult.AsyncWaitHandle.WaitOne(500)) {
            return $false
        }
        $client.EndConnect($asyncResult)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Start-ServiceProcess([hashtable]$service) {
    $serviceDirectory = Join-Path $projectRoot $service.Directory
    $pomPath = Join-Path $serviceDirectory 'pom.xml'
    $stdoutPath = Join-Path $logDirectory "$($service.Name).out.log"
    $stderrPath = Join-Path $logDirectory "$($service.Name).err.log"

    if (-not (Test-Path $pomPath)) {
        throw "Maven pom.xml was not found for $($service.Name): $pomPath"
    }

    $process = Start-Process -FilePath $mavenPath `
        -ArgumentList @('-f', $pomPath, '-DskipTests', 'spring-boot:run') `
        -WorkingDirectory $serviceDirectory `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -WindowStyle Hidden `
        -PassThru

    Write-Host ("Started {0} (PID {1})" -f $service.Name, $process.Id)
    return [PSCustomObject]@{
        Name = $service.Name
        Pid = $process.Id
        Port = $service.Port
    }
}

function Save-StartedProcesses {
    $started | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding UTF8
}

& (Join-Path $PSScriptRoot 'stop-backend.ps1') -KeepInfrastructure
& (Join-Path $PSScriptRoot 'start-dev.ps1')

if (-not (Test-Path $envFile)) {
    throw "Missing .env at $envFile. Copy .env.example to .env and fill in local values first."
}

# VS Code reads .env through envFile, but Maven does not. Load the same values
# into this process so every child Maven/Spring Boot process inherits them.
foreach ($line in Get-Content -LiteralPath $envFile) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
        $variableName = $Matches[1]
        $variableValue = $Matches[2].Trim()
        # Match dotenv behavior for unquoted values such as
        # MYSQL_PORT=3307 # local MySQL mapping.
        if (-not ($variableValue.StartsWith('"') -or $variableValue.StartsWith("'"))) {
            $variableValue = ($variableValue -replace '\s+#.*$', '').Trim()
        }
        if (($variableValue.StartsWith('"') -and $variableValue.EndsWith('"')) -or
            ($variableValue.StartsWith("'") -and $variableValue.EndsWith("'"))) {
            $variableValue = $variableValue.Substring(1, $variableValue.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($variableName, $variableValue, 'Process')
    }
}

$mavenCommand = Get-Command mvn -ErrorAction Stop
$mavenPath = $mavenCommand.Source
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

$services = @(
    @{ Name = 'eureka-server'; Directory = 'eureka-server'; Port = 8761 },
    @{ Name = 'api-gateway'; Directory = 'api-gateway'; Port = 8080 },
    @{ Name = 'auth-service'; Directory = 'server/auth-service'; Port = 8081 },
    @{ Name = 'movie-service'; Directory = 'server/movie-service'; Port = 8082 },
    @{ Name = 'booking-service'; Directory = 'server/booking-service'; Port = 8083 },
    @{ Name = 'payment-service'; Directory = 'server/payment-service'; Port = 8084 },
    @{ Name = 'notification-service'; Directory = 'server/notification-service'; Port = 8085 },
    @{ Name = 'user-service'; Directory = 'server/user-service'; Port = 8086 },
    @{ Name = 'promotion-service'; Directory = 'server/promotion-service'; Port = 8087 },
    @{ Name = 'score-service'; Directory = 'server/score-service'; Port = 8088 },
    @{ Name = 'analytics-service'; Directory = 'server/analytics-service'; Port = 8089 }
)

$started = @()
$eureka = Start-ServiceProcess $services[0]
$started += $eureka
Save-StartedProcesses

$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline -and -not (Test-TcpPort 8761)) {
    Start-Sleep -Seconds 1
}

if (-not (Test-TcpPort 8761)) {
    throw "Eureka did not start within 90 seconds. See $logDirectory\eureka-server.out.log and .err.log"
}

Write-Host 'Eureka is ready; starting the remaining backend services.' -ForegroundColor Green
foreach ($service in $services | Select-Object -Skip 1) {
    $started += Start-ServiceProcess $service
    Save-StartedProcesses
}

$realtimePort = 9093
if ($env:BOOKING_REALTIME_PORT -match '^\d+$') {
    $realtimePort = [int]$env:BOOKING_REALTIME_PORT
}
$requiredPorts = @($services.Port) + $realtimePort
$allPortsDeadline = (Get-Date).AddSeconds(180)
do {
    $missingPorts = @($requiredPorts | Where-Object { -not (Test-TcpPort $_) })
    if ($missingPorts.Count -eq 0) {
        break
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $allPortsDeadline)

Write-Host ''
if ($missingPorts.Count -gt 0) {
    Write-Host ("Backend did not become ready. Missing port(s): {0}" -f ($missingPorts -join ', ')) -ForegroundColor Red
    Write-Host "Logs: $logDirectory"
    exit 1
}

& (Join-Path $PSScriptRoot 'check-dev.ps1')
$checkExitCode = $LASTEXITCODE
if ($checkExitCode -ne 0) {
    Write-Host "Backend ports are open, but health/Eureka checks failed. Logs: $logDirectory" -ForegroundColor Red
    exit $checkExitCode
}

Write-Host 'Backend is fully ready.' -ForegroundColor Green
Write-Host "Logs: $logDirectory"
