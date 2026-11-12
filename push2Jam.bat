@echo off
rem Push2Jam: jam over the Notesweeper band on Push 2 - no game first.
rem Usage:  push2Jam.bat [--band=^<Kick,Bass,...^>] [--device=^<name^>] [--push=^<port^>] [--display=ffm^|wayang]
call gradlew.bat classes push2Classes runtimeLibs -q
if errorlevel 1 exit /b 1
java --enable-native-access=ALL-UNNAMED --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED -cp "build\classes\java\main;build\classes\java\push2;build\runtime-libs\*" net.jetztgrad.midi.workshop.push2.Push2Jam %*
