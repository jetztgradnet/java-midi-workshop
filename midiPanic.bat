@echo off
rem Send All Notes Off to clear stuck notes after Ctrl+C.
rem Usage:  midiPanic.bat
rem         midiPanic.bat --device=XIAO
call gradlew.bat classes -q
if errorlevel 1 exit /b 1
java -cp build\classes\java\main net.jetztgrad.midi.workshop.MidiPanic %*
