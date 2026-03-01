package net.jetztgrad.midi.workshop;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import java.util.Map;

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
     * Extracts the {@code --patch=<value>} argument from {@code args}.
     * {@code value} may be a program number (0–127) or a partial patch name
     * (case-insensitive substring match against {@link #GM_PATCH_NAMES}).
     * Returns 0 (Acoustic Grand Piano) if the argument is absent or unresolvable.
     */
    public static int patchFilter(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--patch=")) {
                String value = arg.substring("--patch=".length()).trim();
                // Try numeric first
                try {
                    int p = Integer.parseInt(value);
                    return Math.max(0, Math.min(127, p));
                } catch (NumberFormatException ignored) {}
                // Fall back to case-insensitive partial name match
                String lower = value.toLowerCase();
                for (int i = 0; i < GM_PATCH_NAMES.length; i++) {
                    if (GM_PATCH_NAMES[i].toLowerCase().contains(lower))
                        return i;
                }
                System.err.println("No GM patch matching \"" + value + "\" — using patch 0.");
            }
        }
        return 0;
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

    // ── GM patch data ─────────────────────────────────────────────────────────

    /** Full 128-entry GM patch name list (index = program number). */
    public static final String[] GM_PATCH_NAMES = {
        // 0–7 Pianos
        "Acoustic Grand Piano", "Bright Acoustic Piano", "Electric Grand Piano",
        "Honky-tonk Piano", "Electric Piano 1", "Electric Piano 2", "Harpsichord", "Clavinet",
        // 8–15 Chromatic Percussion
        "Celesta", "Glockenspiel", "Music Box", "Vibraphone", "Marimba",
        "Xylophone", "Tubular Bells", "Dulcimer",
        // 16–23 Organs
        "Drawbar Organ", "Percussive Organ", "Rock Organ", "Church Organ", "Reed Organ",
        "Accordion", "Harmonica", "Tango Accordion",
        // 24–31 Guitars
        "Acoustic Guitar (nylon)", "Acoustic Guitar (steel)", "Electric Guitar (jazz)",
        "Electric Guitar (clean)", "Electric Guitar (muted)", "Overdriven Guitar",
        "Distortion Guitar", "Guitar Harmonics",
        // 32–39 Bass
        "Acoustic Bass", "Electric Bass (finger)", "Electric Bass (pick)", "Fretless Bass",
        "Slap Bass 1", "Slap Bass 2", "Synth Bass 1", "Synth Bass 2",
        // 40–47 Strings
        "Violin", "Viola", "Cello", "Contrabass", "Tremolo Strings",
        "Pizzicato Strings", "Orchestral Harp", "Timpani",
        // 48–55 Ensemble
        "String Ensemble 1", "String Ensemble 2", "Synth Strings 1", "Synth Strings 2",
        "Choir Aahs", "Voice Oohs", "Synth Choir", "Orchestra Hit",
        // 56–63 Brass
        "Trumpet", "Trombone", "Tuba", "Muted Trumpet", "French Horn",
        "Brass Section", "Synth Brass 1", "Synth Brass 2",
        // 64–71 Reed
        "Soprano Sax", "Alto Sax", "Tenor Sax", "Baritone Sax",
        "Oboe", "English Horn", "Bassoon", "Clarinet",
        // 72–79 Pipe
        "Piccolo", "Flute", "Recorder", "Pan Flute",
        "Blown Bottle", "Shakuhachi", "Whistle", "Ocarina",
        // 80–87 Synth Lead
        "Lead 1 (square)", "Lead 2 (sawtooth)", "Lead 3 (calliope)", "Lead 4 (chiff)",
        "Lead 5 (charang)", "Lead 6 (voice)", "Lead 7 (fifths)", "Lead 8 (bass+lead)",
        // 88–95 Synth Pad
        "Pad 1 (new age)", "Pad 2 (warm)", "Pad 3 (polysynth)", "Pad 4 (choir)",
        "Pad 5 (bowed)", "Pad 6 (metallic)", "Pad 7 (halo)", "Pad 8 (sweep)",
        // 96–103 Synth Effects
        "FX 1 (rain)", "FX 2 (soundtrack)", "FX 3 (crystal)", "FX 4 (atmosphere)",
        "FX 5 (brightness)", "FX 6 (goblins)", "FX 7 (echoes)", "FX 8 (sci-fi)",
        // 104–111 Ethnic
        "Sitar", "Banjo", "Shamisen", "Koto", "Kalimba", "Bag Pipe", "Fiddle", "Shanai",
        // 112–119 Percussive
        "Tinkle Bell", "Agogo", "Steel Drums", "Woodblock", "Taiko Drum",
        "Melodic Tom", "Synth Drum", "Reverse Cymbal",
        // 120–127 Sound Effects
        "Guitar Fret Noise", "Breath Noise", "Seashore", "Bird Tweet",
        "Telephone Ring", "Helicopter", "Applause", "Gunshot"
    };

    /** Note name array shared across all examples. */
    public static final String[] NOTE_NAMES =
        {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};

    /** Returns note name + octave, e.g. {@code "C4"} or {@code "D#5"}. */
    public static String noteName(int note) {
        return NOTE_NAMES[note % 12] + (note / 12 - 1);
    }

    /** Common MIDI CC controller names (most frequently encountered). */
    static final Map<Integer, String> CC_NAMES = Map.ofEntries(
        Map.entry(0,   "BankSel"),
        Map.entry(1,   "ModWheel"),
        Map.entry(7,   "Volume"),
        Map.entry(10,  "Pan"),
        Map.entry(11,  "Expression"),
        Map.entry(64,  "Sustain"),
        Map.entry(65,  "Portamento"),
        Map.entry(66,  "Sostenuto"),
        Map.entry(67,  "SoftPedal"),
        Map.entry(120, "AllSndOff"),
        Map.entry(121, "ResetCtrl"),
        Map.entry(123, "AllNoteOff")
    );

    /** Returns the common name of a MIDI CC controller, or {@code ""} if unknown. */
    public static String ccName(int cc) {
        return CC_NAMES.getOrDefault(cc, "");
    }

    /** Curated 10-preset list for digit-key selection (keys 1–9, 0). */
    static final int[]    PRESET_PROGRAMS = {  0, 19, 24, 40, 48, 56, 65, 73, 80, 88 };
    static final String[] PRESET_NAMES    = {
        "Piano", "Organ", "Guitar", "Violin", "Strings",
        "Trumpet", "Alto Sax", "Flute", "Synth Lead", "Synth Pad"
    };

    /** Returns combo-box-ready entries: "  0  Acoustic Grand Piano", etc. */
    static String[] gmPatchList() {
        String[] result = new String[128];
        for (int i = 0; i < 128; i++)
            result[i] = String.format("%3d  %s", i, GM_PATCH_NAMES[i]);
        return result;
    }

    /** Sends a PROGRAM_CHANGE; silently ignores send errors. */
    public static void sendProgramChange(Receiver receiver, int channel, int program) {
        try {
            receiver.send(new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0), -1);
        } catch (Exception ignored) {}
    }

    /** Sends All Notes Off (CC 123) and Sustain Off (CC 64) on all 16 channels. */
    public static void sendPanic(Receiver receiver) {
        for (int ch = 0; ch < 16; ch++) {
            try {
                receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, ch, 123, 0), -1);
                receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, ch,  64, 0), -1);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Returns a Transmitter for the requested MIDI input device.
     *
     * Looks for {@code --device=<name>} in {@code args} and performs a
     * case-insensitive partial match.  Without {@code --device=}, opens the
     * first device that has a Transmitter (i.e. can send MIDI data).
     * Run {@code ./gradlew listDevices} to see all available device names.
     */
    public static Transmitter openTransmitter(String[] args) throws MidiUnavailableException {
        String filter = deviceFilter(args);
        if (filter != null) {
            return openNamedTransmitter(filter);
        }
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            MidiDevice device = MidiSystem.getMidiDevice(info);
            if (device.getMaxTransmitters() != 0) {
                device.open();
                System.out.println("Input device: " + info.getName());
                return device.getTransmitter();
            }
        }
        System.err.println("No MIDI input device found.");
        System.err.println("Run './gradlew listDevices' to see available devices.");
        System.exit(1);
        return null; // unreachable
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private static Transmitter openNamedTransmitter(String filter) throws MidiUnavailableException {
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            if (info.getName().toLowerCase().contains(filter.toLowerCase())) {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxTransmitters() != 0) {
                    device.open();
                    System.out.println("Input device: " + info.getName());
                    return device.getTransmitter();
                }
            }
        }
        System.err.println("No input device matching \"" + filter + "\" was found.");
        System.err.println("Run './gradlew listDevices' to see all available device names.");
        System.exit(1);
        return null; // unreachable
    }

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
