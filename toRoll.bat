@echo off
rem Convert a MIDI file to piano roll notation.
rem Usage:  toRoll.bat
rem         toRoll.bat --file=midi/song.mid
rem         toRoll.bat --file=midi/song.mid --out=rolls/song.roll
rem         toRoll.bat --file=midi/song.mid --channel=0 --resolution=8
call gradlew.bat classes -q
if errorlevel 1 exit /b 1
java -cp build\classes\java\main net.jetztgrad.midi.workshop.MidiToRoll %*
