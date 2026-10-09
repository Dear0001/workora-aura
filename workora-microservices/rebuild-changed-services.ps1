[CmdletBinding()]
param(
    [switch]$NoCache
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$composeProject = if ($env:COMPOSE_PROJECT_NAME) {
    $env:COMPOSE_PROJECT_NAME
} else {
    "workora-microservices-2"
}

$services = @(
    "bug-service",
    "notification-service",
    "organization-service",
    "payment-service",
    "project-service",
    "recruitment-service",
    "work-service"
)

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Docker is not running or is not available in PATH."
}

$buildArgs = @(
    "compose",
    "--project-name", $composeProject,
    "build"
)
if ($NoCache) {
    $buildArgs += "--no-cache"
}
$buildArgs += $services

Write-Host "Building changed Workora services: $($services -join ', ')"
& docker @buildArgs
if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose build failed."
}

Write-Host "Restarting changed Workora services..."
& docker compose --project-name $composeProject up -d --no-build --no-deps $services
if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose restart failed."
}

Write-Host "Changed services are rebuilding/restarting."
Write-Host "Payment Flyway migrations will run when payment-service starts."
Write-Host "Check status with: docker compose --project-name $composeProject ps"
