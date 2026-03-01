#!/usr/bin/env bash
# Usage:  ./midiFilePlayer.sh [<file.mid>] [--device=<name>] [--patch=<0-127>]
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.MidiFilePlayer "$@"
