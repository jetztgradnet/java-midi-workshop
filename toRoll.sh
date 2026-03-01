#!/usr/bin/env bash
# Convert a MIDI file to piano roll notation.
# Usage:  ./toRoll.sh
#         ./toRoll.sh --file=midi/song.mid
#         ./toRoll.sh --file=midi/song.mid --out=rolls/song.roll
#         ./toRoll.sh --file=midi/song.mid --channel=0 --resolution=8
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.MidiToRoll "$@"
