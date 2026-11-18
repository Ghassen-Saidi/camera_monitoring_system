# Java Environment Activation Script for Transport Management System
Write-Host "Activating Java Development Environment" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Green

# Set Java Home
$JAVA_HOME = "C:\Program Files (x86)\Java\jdk-23"
$JAVAFX_PATH = "C:\javafx-sdk-25.0.1\lib"

# Check if Java exists
if (-not (Test-Path $JAVA_HOME)) {
    Write-Host "ERROR: Java not found at $JAVA_HOME" -ForegroundColor Red
    Write-Host "Please install Java SDK first" -ForegroundColor Yellow
    exit 1
}

# Check if JavaFX exists
if (-not (Test-Path $JAVAFX_PATH)) {
    Write-Host "ERROR: JavaFX not found at $JAVAFX_PATH" -ForegroundColor Red
    Write-Host "Please download JavaFX SDK and extract it to C:\" -ForegroundColor Yellow
    exit 1
}

# Set environment variables for current session
$env:JAVA_HOME = $JAVA_HOME
$env:PATH = "$JAVA_HOME\bin;" + $env:PATH

# Verify Java version
Write-Host "`nJava Version:" -ForegroundColor Cyan
java -version

Write-Host "`nEnvironment activated successfully!" -ForegroundColor Green
Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor White
Write-Host "JavaFX Path: $JAVAFX_PATH" -ForegroundColor White