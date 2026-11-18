@echo off
echo Compiling Camera Surveillance Server...

set LIB_DIR=lib
set SRC_DIR=server\src
set OUT_DIR=server\out

if not exist %LIB_DIR% mkdir %LIB_DIR%
if exist %OUT_DIR% rmdir /s /q %OUT_DIR%
mkdir %OUT_DIR%

:: Build classpath with Jackson JARs
set CLASSPATH=%LIB_DIR%\jackson-core-2.18.4.1.jar;%LIB_DIR%\jackson-annotations-2.18.4.jar;%LIB_DIR%\jackson-databind-2.18.4.jar

:: Compile all Java files in server/src/server/
javac -cp "%CLASSPATH%" -d %OUT_DIR% %SRC_DIR%\server\*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo Compilation successful!
pause