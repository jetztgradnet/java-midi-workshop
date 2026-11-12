package net.jetztgrad.midi.workshop.push2;

import net.jetztgrad.midi.workshop.DeviceUtil;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Push2Jam — jam over the Notesweeper band, no game first.
 *
 * Run with:  ./push2Jam.sh [--band=Lead,Bass,…] [--device=<synth>] [--latency=<ms>]
 *                          [--push=<port>] [--display=ffm|wayang]
 *            ./gradlew push2Jam -Pband=Kick,Bass,Keys,Lead
 *
 * The groove starts with every lineup member playing; the pads are a keyboard,
 * one column per member — see {@link JamSession} for the controls.
 * --band picks up to 8 members (left to right) from: Kick, Snare, Hi-hat, Clap,
 * Bass, Keys, Pad, Arp, Lead, Brass. Default: Kick Snare Hi-hat Bass Keys Pad Arp Lead.
 */
public class Push2Jam {

    static final List<String> DEFAULT_BAND = List.of("Kick", "Snare", "Hi-hat", "Bass", "Keys", "Pad", "Arp", "Lead");

    public static void main(String[] args) throws Exception {
        List<Integer> lineup = bandFilter(args);
        String port = Push2.portFilter(args);
        Receiver push = DeviceUtil.openNamedReceiver(port);
        var pads = DeviceUtil.openNamedTransmitter(port);
        Receiver synth = Push2.openSynth(args);
        Push2.Display display = Push2.Display.open(args);

        BandLoop band = new BandLoop(synth);
        JamSession jam = new JamSession(push, synth, display, band, lineup, null);
        ExecutorService game = Executors.newSingleThreadExecutor(r -> {   // one thread: no locking needed
            Thread t = new Thread(r, "push2jam");
            t.setDaemon(true);
            return t;
        });
        game.execute(() -> {
            band.start();
            for (int m : lineup) band.join(m);
            jam.start();
        });
        pads.setReceiver(new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof ShortMessage sm) game.execute(() -> jam.onMidi(sm));
            }
            @Override public void close() {}
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            game.shutdownNow();
            band.close();
            Push2.clearPads(push);
            JamSession.buttonsOff(push);
            DeviceUtil.sendPanic(synth);
            if (display != null) display.close();
            pads.close();
            System.out.println("\nBye.");
        }));
        System.out.println("Band: " + lineup.stream().map(m -> BandLoop.MEMBERS.get(m).name()).toList());
        System.out.println("Jam away — press Ctrl+C to quit.");
        Thread.currentThread().join();
    }

    /** Extracts {@code --band=<name,name,…>} (case-insensitive, max 8), or the default lineup. */
    static List<Integer> bandFilter(String[] args) {
        List<String> names = DEFAULT_BAND;
        for (String arg : args)
            if (arg.startsWith("--band=")) names = List.of(arg.substring("--band=".length()).split(","));
        List<Integer> lineup = new ArrayList<>();
        for (String name : names) {
            int m = memberIndex(name.trim());
            if (m < 0) System.err.println("No band member \"" + name.trim() + "\" — skipped.");
            else if (lineup.contains(m)) System.err.println(name.trim() + " is already in the band — skipped.");
            else if (lineup.size() == Push2.GRID) System.err.println("Only " + Push2.GRID + " columns — " + name.trim() + " skipped.");
            else lineup.add(m);
        }
        return lineup;
    }

    static int memberIndex(String name) {
        for (int m = 0; m < BandLoop.MEMBERS.size(); m++)
            if (BandLoop.MEMBERS.get(m).name().equalsIgnoreCase(name)) return m;
        return -1;
    }
}
