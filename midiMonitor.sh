#!/usr/bin/env bash
# Monitor incoming MIDI messages on an input device.
# Usage:  ./midiMonitor.sh
#         ./midiMonitor.sh --device=XIAO
#         ./midiMonitor.sh --device=XIAO --channel=1
set -e
./gradlew classes -q
exec java -cp build/classes/java/main \
    net.jetztgrad.midi.workshop.MidiMonitor "$@"
