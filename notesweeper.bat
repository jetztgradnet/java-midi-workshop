@echo off
rem Notesweeper: Minesweeper on Push 2 - find the band instead of mines.
rem Usage:  notesweeper.bat [--device=^<name^>] [--boom=^<file.mid^>] [--push=^<port^>] [--display=ffm^|wayang]
call gradlew.bat classes push2Classes runtimeLibs -q
if errorlevel 1 exit /b 1
java --enable-native-access=ALL-UNNAMED --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED -cp "build\classes\java\main;build\classes\java\push2;build\runtime-libs\*" net.jetztgrad.midi.workshop.push2.Notesweeper %*
