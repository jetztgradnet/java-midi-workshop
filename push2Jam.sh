#!/usr/bin/env bash
# Push2Jam: jam over the Notesweeper band on Push 2 — no game first.
# The push2 source set needs the Wayang / usb4java jars, so they are copied
# to build/runtime-libs first; then java is exec'd directly so Ctrl+C runs the
# shutdown hook (pads off, notes off, display closed).
#
# Usage:  ./push2Jam.sh
#         ./push2Jam.sh --band=Kick,Bass,Keys,Lead
#         ./push2Jam.sh --display=ffm        (LCD via libusb + Java FFM instead of Wayang)
set -e
./gradlew classes push2Classes runtimeLibs -q
exec java --enable-native-access=ALL-UNNAMED \
    --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED \
    -cp "build/classes/java/main:build/classes/java/push2:build/runtime-libs/*" \
    net.jetztgrad.midi.workshop.push2.Push2Jam "$@"
