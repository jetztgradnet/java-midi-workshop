#!/usr/bin/env bash
# Play a piano roll text notation file (or the built-in demo).
# Usage:  ./pianoRoll.sh
#         ./pianoRoll.sh --file=my_melody.roll
#         ./pianoRoll.sh --device=XIAO --patch=violin
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.PianoRollPlayer "$@"
