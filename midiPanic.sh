#!/usr/bin/env bash
# Send All Notes Off to clear stuck notes after Ctrl+C.
# Usage:  ./midiPanic.sh
#         ./midiPanic.sh --device=XIAO
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.MidiPanic "$@"
