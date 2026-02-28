@echo off
REM Run LiveWindowKeyboard on Windows.
REM Usage: liveWindow.bat [--device=<name>]
call gradlew.bat classes -q
if errorlevel 1 exit /b 1
java -cp build\classes\java\main net.jetztgrad.midi.workshop.LiveWindowKeyboard %*
