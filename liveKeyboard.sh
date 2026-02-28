#!/usr/bin/env bash
# Run LiveTerminalKeyboard directly so /dev/tty raw mode works.
# The Gradle daemon runs detached (no controlling terminal), so raw mode
# fails when using  ./gradlew liveTerminalKeyboard.  This script compiles
# via Gradle and then exec's java directly, replacing the shell process —
# the Java process inherits the shell's controlling terminal.
#
# Usage:  ./liveKeyboard.sh
#         ./liveKeyboard.sh --device=XIAO
#         ./liveKeyboard.sh --device="IAC Bus 1"
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.LiveTerminalKeyboard "$@"
