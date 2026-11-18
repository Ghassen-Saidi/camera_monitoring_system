@echo off
echo Running Camera Surveillance Client...

set JAVAFX_PATH=C:\javafx-sdk-25.0.1\lib

java --module-path "%JAVAFX_PATH%" ^
     --add-modules javafx.controls,javafx.fxml,javafx.media ^
     -cp "out;lib/*" ^
     camera.CameraClientFX

pause
