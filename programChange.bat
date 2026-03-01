@echo off
rem Usage:  programChange.bat [--device=<name>]
call gradlew.bat classes -q
if errorlevel 1 exit /b 1
java -cp build\classes\java\main net.jetztgrad.midi.workshop.ProgramChange %*
