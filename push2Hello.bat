@echo off
rem Push 2 hello world: pads play notes, the LCD says hello.
rem Usage:  push2Hello.bat [--device=^<name^>] [--patch=^<0-127^|name^>] [--push=^<port^>] [--display=ffm^|wayang]
call gradlew.bat classes push2Classes runtimeLibs -q
if errorlevel 1 exit /b 1
java --enable-native-access=ALL-UNNAMED --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED -cp "build\classes\java\main;build\classes\java\push2;build\runtime-libs\*" net.jetztgrad.midi.workshop.push2.Push2Hello %*
