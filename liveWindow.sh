#!/usr/bin/env bash
# Convenience launcher for LiveWindowKeyboard.
# Usage:  ./liveWindow.sh
#         ./liveWindow.sh --device=XIAO
#         ./liveWindow.sh --device="IAC Bus 1"
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.LiveWindowKeyboard "$@"
