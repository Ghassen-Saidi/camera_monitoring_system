@echo off
echo Compiling Camera Surveillance Client...

set JAVAFX_PATH=C:\javafx-sdk-25.0.1\lib

if exist out rmdir /s /q out
mkdir out

javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml ^
      -d out ^
      src/camera/*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

copy src\camera\camera.fxml out\camera\
echo Compilation successful!
pause