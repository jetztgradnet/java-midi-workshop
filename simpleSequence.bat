@echo off
rem Usage:  simpleSequence.bat [--device=<name>] [--patch=<0-127>]
call gradlew.bat classes -q
if errorlevel 1 exit /b 1
java -cp build\classes\java\main net.jetztgrad.midi.workshop.SimpleSequence %*
