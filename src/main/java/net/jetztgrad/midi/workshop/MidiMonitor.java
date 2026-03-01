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
        String formatted = DeviceUtil.formatShortMessage(sm);
        if (formatted != null)
            System.out.println(DeviceUtil.elapsedTimestamp(startTime) + "  " + formatted);
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
