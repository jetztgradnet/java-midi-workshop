@echo off
rem Usage:  listDevices.bat
call gradlew.bat classes -q
if errorlevel 1 exit /b 1
java -cp build\classes\java\main net.jetztgrad.midi.workshop.ListDevices %*
