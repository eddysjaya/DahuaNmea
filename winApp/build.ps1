# Build and Run Script for Dahua NMEA Viewer

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Dahua NMEA Viewer - Build Script" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Check if .NET 7.0 is installed
Write-Host "Checking .NET installation..." -ForegroundColor Yellow
$dotnetVersion = dotnet --version 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: .NET SDK not found!" -ForegroundColor Red
    Write-Host "Please install .NET 7.0 SDK from: https://dotnet.microsoft.com/download/dotnet/7.0" -ForegroundColor Red
    exit 1
}
Write-Host "Found .NET version: $dotnetVersion" -ForegroundColor Green
Write-Host ""

# Restore packages
Write-Host "Restoring NuGet packages..." -ForegroundColor Yellow
dotnet restore
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Package restore failed!" -ForegroundColor Red
    exit 1
}
Write-Host "Packages restored successfully" -ForegroundColor Green
Write-Host ""

# Build Release
Write-Host "Building Release version..." -ForegroundColor Yellow
dotnet build -c Release
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "Build completed successfully" -ForegroundColor Green
Write-Host ""

# Publish self-contained
Write-Host "Publishing application..." -ForegroundColor Yellow
dotnet publish -c Release -r win-x64 --self-contained false -o .\publish
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Publish failed!" -ForegroundColor Red
    exit 1
}
Write-Host "Application published to: .\publish" -ForegroundColor Green
Write-Host ""

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Build Complete!" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "To run the application:" -ForegroundColor Yellow
Write-Host "  cd publish" -ForegroundColor White
Write-Host "  .\DahuaNmeaViewer.exe" -ForegroundColor White
Write-Host ""

# Ask if user wants to run now
$run = Read-Host "Do you want to run the application now? (Y/N)"
if ($run -eq "Y" -or $run -eq "y") {
    Write-Host "Starting application..." -ForegroundColor Green
    Start-Process ".\publish\DahuaNmeaViewer.exe"
}
