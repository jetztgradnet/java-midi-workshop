#!/usr/bin/env bash
# Usage:  ./drumPattern.sh [--device=<name>]
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.DrumPattern "$@"
