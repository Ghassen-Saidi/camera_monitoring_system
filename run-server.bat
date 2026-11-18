@echo off
echo Starting Camera Surveillance Server...

set LIB_DIR=lib
set OUT_DIR=server\out

set CLASSPATH=%OUT_DIR%;%LIB_DIR%\jackson-core-2.18.4.1.jar;%LIB_DIR%\jackson-annotations-2.18.4.jar;%LIB_DIR%\jackson-databind-2.18.4.jar

java -cp "%CLASSPATH%" server.Main

pause