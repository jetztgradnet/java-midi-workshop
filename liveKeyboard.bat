@echo off
REM Run LiveTerminalKeyboard on Windows.
REM Usage: liveKeyboard.bat [--device=<name>]
REM Note: raw terminal mode is not supported on Windows; the program
REM       falls back to buffered mode (type key(s) and press Enter).
call gradlew.bat classes -q
if errorlevel 1 exit /b 1
java -cp build\classes\java\main net.jetztgrad.midi.workshop.LiveTerminalKeyboard %*
