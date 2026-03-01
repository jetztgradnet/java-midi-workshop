@echo off
rem Play a piano roll text notation file (or the built-in demo).
rem Usage:  pianoRoll.bat
rem         pianoRoll.bat --file=my_melody.roll
rem         pianoRoll.bat --device=XIAO --patch=violin
call gradlew.bat classes -q
if errorlevel 1 exit /b 1
java -cp build\classes\java\main net.jetztgrad.midi.workshop.PianoRollPlayer %*
