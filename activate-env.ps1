# Java Environment Activation Script for Transport Management System
Write-Host "Activating Java Development Environment" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Green

# Set Java Home
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "" }
$javaFxPath = if ($env:JAVAFX_PATH) { $env:JAVAFX_PATH } else { "C:\javafx-sdk-25.0.1\lib" }

# Check if Java exists
if ([string]::IsNullOrWhiteSpace($javaHome) -or -not (Test-Path $javaHome)) {
    Write-Host "ERROR: Set JAVA_HOME to a JDK installation before running this script" -ForegroundColor Red
    Write-Host "Please install Java SDK first" -ForegroundColor Yellow
    exit 1
}

# Check if JavaFX exists
if (-not (Test-Path $javaFxPath)) {
    Write-Host "ERROR: JavaFX not found at $javaFxPath" -ForegroundColor Red
    Write-Host "Please download JavaFX SDK and extract it to C:\" -ForegroundColor Yellow
    exit 1
}

# Set environment variables for current session
$env:JAVA_HOME = $javaHome
$env:JAVAFX_PATH = $javaFxPath
$env:PATH = "$javaHome\bin;" + $env:PATH

# Verify Java version
Write-Host "`nJava Version:" -ForegroundColor Cyan
java -version

Write-Host "`nEnvironment activated successfully!" -ForegroundColor Green
Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor White
Write-Host "JavaFX Path: $env:JAVAFX_PATH" -ForegroundColor White
