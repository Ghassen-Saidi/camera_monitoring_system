# Build and Run Script for Camera Surveillance Client
Write-Host "Building Camera Surveillance Client..." -ForegroundColor Green

$JAVAFX_PATH = "C:\javafx-sdk-25.0.1\lib"
$SRC_DIR = "src"
$OUT_DIR = "out"

if (Test-Path $OUT_DIR) { Remove-Item -Recurse -Force $OUT_DIR }
New-Item -ItemType Directory -Force $OUT_DIR | Out-Null

$javaFiles = Get-ChildItem -Path "$SRC_DIR/camera" -Filter "*.java" | ForEach-Object { $_.FullName }

Write-Host "Compiling Java files..." -ForegroundColor Cyan
javac --module-path "$JAVAFX_PATH" `
      --add-modules javafx.controls,javafx.fxml,javafx.media `
      -d $OUT_DIR `
      $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}

Copy-Item -Path "$SRC_DIR/camera/camera.fxml" -Destination "$OUT_DIR/camera/" -Force
Write-Host "Build successful!" -ForegroundColor Green
Write-Host "`nStarting application..." -ForegroundColor Yellow

java --module-path "$JAVAFX_PATH" `
     --add-modules javafx.controls,javafx.fxml,javafx.media `
     -cp "$OUT_DIR" `
     camera.CameraClientFX
