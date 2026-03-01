package net.jetztgrad.midi.workshop;

/**
 * MidiPanic — send All Notes Off + Sustain Off on all 16 channels.
 *
 * Use this after Ctrl+C-ing an example to clear stuck notes.
 *
 * Run with:  ./gradlew midiPanic
 *            ./gradlew midiPanic -Pdevice=<name>
 *            ./midiPanic.sh [--device=<name>]
 */
public class MidiPanic {
    public static void main(String[] args) throws Exception {
        var receiver = DeviceUtil.openReceiver(args);
        System.out.println("Sending panic: All Notes Off + Sustain Off on all 16 channels...");
        DeviceUtil.sendPanic(receiver);
        System.out.println("Done.");
    }
}
