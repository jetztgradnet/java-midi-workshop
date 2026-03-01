#!/usr/bin/env bash
# Usage:  ./listDevices.sh
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.ListDevices "$@"
