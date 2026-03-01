@echo off
rem Monitor incoming MIDI messages on an input device.
rem Usage:  midiMonitor.bat
rem         midiMonitor.bat --device=XIAO
rem         midiMonitor.bat --device=XIAO --channel=1
call gradlew.bat classes -q
if errorlevel 1 exit /b 1
java -cp build\classes\java\main net.jetztgrad.midi.workshop.MidiMonitor %*
