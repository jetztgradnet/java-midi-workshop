package net.jetztgrad.midi.workshop;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;

/**
 * DeviceUtil — locate and open a MIDI output device by name.
 *
 * Pass {@code -Pdevice=<partial-name>} on the Gradle command line to select
 * a specific MIDI output device, for example:
 *
 *   ./gradlew playNote           -Pdevice=XIAO
 *   ./gradlew simpleSequence     -Pdevice="IAC Bus"
 *   ./gradlew midiFilePlayer     -Pdevice=XIAO  -PmidiFile=midi/bach_minuet_g.mid
 *
 * Without {@code -Pdevice} the platform's default software synthesizer is used.
 * Run {@code ./gradlew listDevices} to see all available device names.
 */
public class DeviceUtil {

    /**
     * Returns a Receiver for the requested MIDI output device.
     *
     * Looks for {@code --device=<name>} in {@code args} and performs a
     * case-insensitive partial match against all MIDI device names.
     * Falls back to the default software synthesizer when the argument is absent.
     */
    public static Receiver openReceiver(String[] args) throws MidiUnavailableException {
        String filter = deviceFilter(args);
        if (filter != null) {
            return openNamedReceiver(filter);
        }
        // Default: built-in platform software synthesizer
        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();
        System.out.println("Output device: " + synth.getDeviceInfo().getName() + " (default)");
        return synth.getReceiver();
    }

    /**
     * Extracts the {@code --device=<name>} value from {@code args}, or returns {@code null}.
     */
    static String deviceFilter(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--device=")) {
                return arg.substring("--device=".length()).trim();
            }
        }
        return null;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private static Receiver openNamedReceiver(String filter) throws MidiUnavailableException {
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            if (info.getName().toLowerCase().contains(filter.toLowerCase())) {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxReceivers() != 0) {   // it's an output device
                    device.open();
                    System.out.println("Output device: " + info.getName());
                    return device.getReceiver();
                }
            }
        }
        // Not found — print available names and exit
        System.err.println("No output device matching \"" + filter + "\" was found.");
        System.err.println("Run './gradlew listDevices' to see all available device names.");
        System.exit(1);
        return null; // unreachable
    }
}
