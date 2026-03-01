package net.jetztgrad.midi.workshop;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 * MidiMonitor — print incoming MIDI messages to the console.
 *
 * Run with:  ./gradlew midiMonitor
 *            ./gradlew midiMonitor -Pdevice=&lt;name&gt;
 *            ./gradlew midiMonitor -Pdevice=&lt;name&gt; -Pchannel=&lt;1-16|all&gt;
 *            ./midiMonitor.sh [--device=&lt;name&gt;] [--channel=&lt;1-16|all&gt;]
 *
 * Displays: NoteOn, NoteOff, Control Change (with named controllers),
 *           Program Change (with GM name), and Pitch Bend.
 * Channel numbers are shown as 1–16 (musician convention).
 * Timestamps are relative to when the monitor started.
 */
public class MidiMonitor {

    static long startTime;

    public static void main(String[] args) throws Exception {
        int filterChannel = channelFilter(args);  // -1 = all; otherwise 0-based internal
        startTime = System.currentTimeMillis();

        var transmitter = DeviceUtil.openTransmitter(args);

        String chDesc = (filterChannel == -1)
                ? "all channels"
                : "channel " + (filterChannel + 1);
        System.out.println("Monitoring " + chDesc + " — press Ctrl+C to stop.");
        System.out.println();

        transmitter.setReceiver(new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof ShortMessage sm) {
                    int ch = sm.getChannel();
                    if (filterChannel != -1 && ch != filterChannel) return;
                    printShortMessage(sm);
                }
            }
            @Override public void close() {}
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nStopped.");
            transmitter.close();
        }));
        Thread.currentThread().join();  // block until Ctrl+C
    }

    static void printShortMessage(ShortMessage sm) {
        int cmd = sm.getCommand();
        int ch  = sm.getChannel() + 1;  // display as 1-based
        int d1  = sm.getData1();
        int d2  = sm.getData2();
        String ts = timestamp();

        switch (cmd) {
            case ShortMessage.NOTE_ON -> {
                if (d2 == 0) {
                    // MIDI convention: NOTE_ON with velocity 0 = NoteOff
                    System.out.printf("%s  NoteOff  %-5s  (note %3d, ch %2d)%n",
                            ts, DeviceUtil.noteName(d1), d1, ch);
                } else {
                    System.out.printf("%s  NoteOn   %-5s  (note %3d, vel %3d, ch %2d)%n",
                            ts, DeviceUtil.noteName(d1), d1, d2, ch);
                }
            }
            case ShortMessage.NOTE_OFF ->
                System.out.printf("%s  NoteOff  %-5s  (note %3d, ch %2d)%n",
                        ts, DeviceUtil.noteName(d1), d1, ch);
            case ShortMessage.CONTROL_CHANGE -> {
                String name = DeviceUtil.ccName(d1);
                String nameStr = name.isEmpty() ? "" : " [" + name + "]";
                System.out.printf("%s  CC  %3d%-14s  val %3d  (ch %2d)%n",
                        ts, d1, nameStr, d2, ch);
            }
            case ShortMessage.PROGRAM_CHANGE ->
                System.out.printf("%s  ProgramChange %3d  %-30s (ch %2d)%n",
                        ts, d1, DeviceUtil.GM_PATCH_NAMES[d1], ch);
            case ShortMessage.PITCH_BEND -> {
                // 14-bit value: LSB in d1, MSB in d2; 8192 = centre
                int raw    = (d2 << 7) | d1;
                int scaled = raw - 8192;
                System.out.printf("%s  PitchBend  %+6d  (raw %5d, ch %2d)%n",
                        ts, scaled, raw, ch);
            }
        }
    }

    /** Elapsed time since start as [MM:SS.mmm]. */
    static String timestamp() {
        long elapsed = System.currentTimeMillis() - startTime;
        long mm  = elapsed / 60000;
        long ss  = (elapsed % 60000) / 1000;
        long ms  = elapsed % 1000;
        return String.format("[%02d:%02d.%03d]", mm, ss, ms);
    }

    /**
     * Extracts {@code --channel=&lt;value&gt;} from args.
     * Accepts 1–16 (musician convention) or "all".
     * Returns internal 0-based channel, or -1 for all.
     */
    static int channelFilter(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--channel=")) {
                String value = arg.substring("--channel=".length()).trim();
                if (value.equalsIgnoreCase("all")) return -1;
                try {
                    int ch = Integer.parseInt(value);
                    if (ch >= 1 && ch <= 16) return ch - 1;  // musician 1-based
                    if (ch == 0)             return 0;        // tolerate 0-based
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1; // default: all channels
    }
}
