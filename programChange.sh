#!/usr/bin/env bash
# Usage:  ./programChange.sh [--device=<name>]
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.ProgramChange "$@"
