package net.jetztgrad.midi.workshop.push2;

import net.jetztgrad.midi.workshop.DeviceUtil;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Push2Hello — "hello world" for the Ableton Push 2.
 *
 * Run with:  ./gradlew push2Hello
 *            ./gradlew push2Hello -Pdevice="IAC Bus" -Ppatch=synth
 *            ./push2Hello.sh [--device=<synth>] [--patch=<0-127|name>] [--push=<port>] [--display=ffm|wayang]
 *                            [--latency=<ms>]   (built-in synth buffer, default 20)
 *
 * • Lights the 8×8 pads in a blue/green checkerboard (MIDI out to Push).
 * • Pressing a pad lights it red and plays a note on the synth (MIDI in from Push,
 *   MIDI out to the synth). Rows are a fourth apart, like Push's own layout.
 * • The LCD says hello and mirrors the grid (USB display via Wayang).
 *
 * --device / --patch choose the *synth*, as in all other examples; without
 * --device the built-in Java synthesizer plays. Quit Ableton Live first.
 */
public class Push2Hello {

    static final int LOWEST_NOTE = 36;   // C2 on the bottom-left pad
    static final int ROW_STEP    = 5;    // a fourth per row

    static final boolean[][] held = new boolean[Push2.GRID][Push2.GRID];
    static String lastNote = "press a pad";

    public static void main(String[] args) throws Exception {
        String port = Push2.portFilter(args);
        Receiver push = DeviceUtil.openNamedReceiver(port);        // LEDs
        var pads = DeviceUtil.openNamedTransmitter(port);          // pad presses

        var synth = Push2.openSynth(args);
        int patch = DeviceUtil.patchFilter(args);
        DeviceUtil.sendProgramChange(synth, 0, patch);
        System.out.println("Instrument: " + DeviceUtil.GM_PATCH_NAMES[patch] + " (patch " + patch + ")");

        Push2.Display display = Push2.Display.open(args);   // null → MIDI only

        for (int y = 0; y < Push2.GRID; y++)
            for (int x = 0; x < Push2.GRID; x++)
                Push2.setPad(push, x, y, baseColor(x, y));
        redraw(display);

        pads.setReceiver(new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (!(message instanceof ShortMessage sm) || !Push2.isPad(sm.getData1())) return;
                int cmd = sm.getCommand();
                if (cmd != ShortMessage.NOTE_ON && cmd != ShortMessage.NOTE_OFF) return;  // skip aftertouch

                int x = Push2.padX(sm.getData1());
                int y = Push2.padY(sm.getData1());
                int note = LOWEST_NOTE + x + ROW_STEP * y;
                boolean down = cmd == ShortMessage.NOTE_ON && sm.getData2() > 0;
                try {
                    if (down) {
                        synth.send(new ShortMessage(ShortMessage.NOTE_ON, 0, note, sm.getData2()), -1);
                        Push2.setPad(push, x, y, Push2.RED);
                        lastNote = DeviceUtil.noteName(note) + "  velocity " + sm.getData2();
                    } else {
                        synth.send(new ShortMessage(ShortMessage.NOTE_OFF, 0, note, 0), -1);
                        Push2.setPad(push, x, y, baseColor(x, y));
                    }
                } catch (Exception e) {
                    System.err.println("MIDI error: " + e.getMessage());
                }
                held[x][y] = down;
                redraw(display);
            }
            @Override public void close() {}
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Push2.clearPads(push);
            DeviceUtil.sendPanic(synth);
            if (display != null) display.close();
            pads.close();
            System.out.println("\nBye.");
        }));
        System.out.println("Play the pads — press Ctrl+C to stop.");
        Thread.currentThread().join();  // block until Ctrl+C
    }

    static int baseColor(int x, int y) {
        return (x + y) % 2 == 0 ? Push2.BLUE : Push2.GREEN;
    }

    // ── LCD ──────────────────────────────────────────────────────────────────

    static final Color LCD_BLUE  = new Color(0x2060FF);
    static final Color LCD_GREEN = new Color(0x20C040);
    static final Color LCD_RED   = new Color(0xFF3030);

    static void redraw(Push2.Display display) {
        if (display == null) return;
        display.draw(g -> {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, Push2.Display.WIDTH, Push2.Display.HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
            g.drawString("Hello, Push 2!", 40, 85);
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
            g.drawString(lastNote, 40, 130);

            drawGrid(g, Push2.Display.WIDTH - 8 * 18 - 24, 8, 18);
        });
    }

    /** Mirrors the pads; y = 0 is the bottom row on Push, so flip vertically. */
    static void drawGrid(Graphics2D g, int left, int top, int cell) {
        for (int y = 0; y < Push2.GRID; y++) {
            for (int x = 0; x < Push2.GRID; x++) {
                g.setColor(held[x][y] ? LCD_RED : baseColor(x, y) == Push2.BLUE ? LCD_BLUE : LCD_GREEN);
                g.fillRect(left + x * cell, top + (Push2.GRID - 1 - y) * cell, cell - 2, cell - 2);
            }
        }
    }
}
