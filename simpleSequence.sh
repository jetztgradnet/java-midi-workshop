#!/usr/bin/env bash
# Usage:  ./simpleSequence.sh [--device=<name>] [--patch=<0-127>]
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.SimpleSequence "$@"
