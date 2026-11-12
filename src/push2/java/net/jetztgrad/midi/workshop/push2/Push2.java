package net.jetztgrad.midi.workshop.push2;

import org.deepsymmetry.Wayang;

import net.jetztgrad.midi.workshop.DeviceUtil;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.sampled.SourceDataLine;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Push2 — constants and helpers for the Ableton Push 2 controller.
 *
 * Push 2 shows up as two separate USB interfaces:
 *   • MIDI    — class-compliant ports "Ableton Push 2 Live Port" and "… User Port".
 *               Pads, buttons and encoders send notes / CCs; sending notes / CCs
 *               back to Push sets the LED colours.
 *   • Display — a 960×160 colour LCD on a vendor-specific USB bulk endpoint,
 *               driven via libusb (here: the Wayang library).
 *
 * Quit Ableton Live first — it grabs both the MIDI ports and the display.
 *
 * Protocol reference: https://github.com/Ableton/push-interface
 */
public final class Push2 {

    private Push2() {}

    /**
     * Partial name of the MIDI port to use. Without Ableton Live running, Push
     * stays in "Live" mode: pads arrive on, and LEDs are set via, the Live port.
     * (Java on macOS may report just "Live Port", so we match on that.)
     */
    public static final String DEFAULT_PORT = "Live Port";

    // ── Pads ─────────────────────────────────────────────────────────────────
    // 8×8 grid, notes 36–99. x = 0 (left) … 7, y = 0 (bottom) … 7.

    public static final int GRID      = 8;
    public static final int PAD_FIRST = 36;   // bottom-left pad
    public static final int PAD_LAST  = 99;   // top-right pad

    public static boolean isPad(int note) { return note >= PAD_FIRST && note <= PAD_LAST; }
    public static int padNote(int x, int y) { return PAD_FIRST + x + GRID * y; }
    public static int padX(int note)        { return (note - PAD_FIRST) % GRID; }
    public static int padY(int note)        { return (note - PAD_FIRST) / GRID; }

    // ── Pad LED colours ──────────────────────────────────────────────────────
    // A pad's colour is set by sending NOTE_ON to its note: velocity = palette
    // index, MIDI channel = animation. Only these indices are documented for
    // the default palette.

    public static final int BLACK = 0;
    public static final int BLUE  = 125;
    public static final int GREEN = 126;
    public static final int RED   = 127;

    // Animation channels: 0 = static, 1–5 one-shot, 6–10 pulsing, 11–15 blinking
    // (each group: 1/24, 1/16, 1/8, 1/4, 1/2 note; 120 bpm unless clocked).
    public static final int ANIM_NONE  = 0;
    public static final int ANIM_FADE  = 4;    // one-shot, quarter note
    public static final int ANIM_PULSE = 9;    // pulse, quarter notes
    public static final int ANIM_BLINK = 14;   // blink, quarter notes

    public static void setPad(Receiver push, int x, int y, int color) {
        setPad(push, x, y, color, ANIM_NONE);
    }

    public static void setPad(Receiver push, int x, int y, int color, int animation) {
        try {
            push.send(new ShortMessage(ShortMessage.NOTE_ON, animation, padNote(x, y), color), -1);
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Animates a pad between two colours: {@code from} is set statically first,
     * then {@code to} on an animation channel (one-shot fades from → to,
     * pulse/blink alternate between them).
     */
    public static void animatePad(Receiver push, int x, int y, int from, int to, int animation) {
        setPad(push, x, y, from);
        setPad(push, x, y, to, animation);
    }

    /** Turns all 64 pad LEDs off. */
    public static void clearPads(Receiver push) {
        for (int y = 0; y < GRID; y++)
            for (int x = 0; x < GRID; x++)
                setPad(push, x, y, BLACK);
    }

    /**
     * Push 2's default RGB palette (0xRRGGBB per index), read from a Push 2 via
     * SysEx 0x04 (with a native CoreMIDI tool — Java's built-in macOS MIDI cannot
     * send or receive SysEx). Index 0 is black; 122–127 match Ableton's docs.
     * Use it to pick LED colours that exist on Push without SysEx, and to draw
     * the same colours on the LCD.
     */
    static final int[] DEFAULT_PALETTE = {
            0x000000, 0xFF4032, 0x800400, 0xC93C00, 0xAC1F00, 0x8C5018, 0x491804, 0xFADC3B,   //   0–  7
            0xFFC516, 0xB6FF0E, 0x79FF18, 0x34C216, 0x4F8A04, 0x62FF55, 0x297D53, 0x269E72,   //   8– 15
            0x31ADFF, 0x3663FC, 0x1A34FF, 0x1C0CE6, 0x153999, 0x3937FF, 0x5722FF, 0x972BFF,   //  16– 23
            0x852178, 0xFF1032, 0xFF2BD4, 0xA63421, 0x995628, 0x876700, 0x90821F, 0x4A8700,   //  24– 31
            0x007F12, 0x1853B2, 0x624BAD, 0x733A67, 0xF8BCAF, 0xFF9B76, 0xFFBF5F, 0xD9AF71,   //  32– 39
            0xFFF480, 0xBFBA69, 0xBCCC88, 0xAEFF99, 0x7CDD9F, 0x89B47D, 0x80F3FF, 0x7ACEFC,   //  40– 47
            0x68A1D3, 0x858FC2, 0xBBAAF2, 0xCDBBE4, 0xEF8BB0, 0x859D8C, 0x6B756E, 0x84909B,   //  48– 55
            0x6A7075, 0x88859D, 0x6C6A75, 0x9D859C, 0x746A74, 0x9C9D85, 0x74756A, 0x9D8484,   //  56– 63
            0x756A6A, 0x661914, 0x210806, 0x460300, 0x280000, 0x5D1700, 0x200D00, 0x470C00,   //  64– 71
            0x1C0800, 0x3B2B14, 0x1C130A, 0x250E05, 0x0D0602, 0x645817, 0x201C07, 0x664E08,   //  72– 79
            0x211902, 0x486605, 0x172101, 0x306609, 0x0F2103, 0x144D08, 0x061902, 0x1F3701,   //  80– 87
            0x0A1100, 0x276622, 0x0C210B, 0x143E29, 0x081910, 0x004D36, 0x00180E, 0x134566,   //  88– 95
            0x061621, 0x152764, 0x070C20, 0x0A1466, 0x030621, 0x0B045C, 0x03011D, 0x0A1C4C,   //  96–103
            0x040B1E, 0x161666, 0x070721, 0x220D66, 0x0B0421, 0x3C1166, 0x130521, 0x350D30,   // 104–111
            0x11040F, 0x660614, 0x210206, 0x661154, 0x21051B, 0x000000, 0x595959, 0x1A1A1A,   // 112–119
            0xFFFFFF, 0x595959, 0xCCCCCC, 0x404040, 0x141414, 0x0000FF, 0x00FF00, 0xFF0000,   // 120–127
    };

    /** The RGB colour Push shows for default palette {@code index}. */
    public static Color paletteColor(int index) {
        return new Color(DEFAULT_PALETTE[index]);
    }

    /**
     * The default palette index closest to {@code c} (redmean-weighted RGB
     * distance), skipping black (0) and any index in {@code taken}.
     */
    public static int nearestPaletteIndex(Color c, java.util.Set<Integer> taken) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 1; i < DEFAULT_PALETTE.length; i++) {
            if (taken.contains(i)) continue;
            Color p = paletteColor(i);
            double rMean = (c.getRed() + p.getRed()) / 2.0;
            int dr = c.getRed() - p.getRed(), dg = c.getGreen() - p.getGreen(), db = c.getBlue() - p.getBlue();
            double d = (2 + rMean / 256) * dr * dr + 4 * dg * dg + (2 + (255 - rMean) / 256) * db * db;
            if (d < bestDistance) { bestDistance = d; best = i; }
        }
        return best;
    }

    // ── Buttons ──────────────────────────────────────────────────────────────
    // Buttons send CC (127 = pressed, 0 = released). Sending the same CC back
    // sets the button LED: white buttons 0 (off) … 127 (full), RGB buttons use
    // the palette. The channel selects an animation, as for pads.

    public static final int CC_SELECT = 48;
    public static final int CC_SHIFT  = 49;
    public static final int CC_PLAY   = 85;
    public static final int CC_NEW    = 87;
    /** The 8 RGB buttons below the display: CC 20 (left) … 27 (right). */
    public static final int CC_LOWER_FIRST = 20;

    public static void setButton(Receiver push, int cc, int value) {
        setButton(push, cc, value, ANIM_NONE);
    }

    public static void setButton(Receiver push, int cc, int value, int animation) {
        try {
            push.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, animation, cc, value), -1);
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // ── Palette (SysEx) ──────────────────────────────────────────────────────
    // F0 00 21 1D 01 01 <cmd> <args…> F7. Palette entries hold 8-bit R, G, B and
    // white, each split into two 7-bit bytes (bits 0–6, then bit 7).

    static final int SYSEX_SET_PALETTE_ENTRY = 0x03;
    static final int SYSEX_REAPPLY_PALETTE   = 0x05;

    /**
     * Redefines palette entry {@code index} (0–127). Takes effect for newly
     * set pads, or for all pads after {@link #reapplyPalette}. Push keeps the
     * change until it is power-cycled (or Live resets the palette).
     */
    public static void setPaletteEntry(Receiver push, int index, int r, int g, int b) {
        int w = (r + g + b) / 3;   // only used by white-LED buttons
        sendSysex(push, SYSEX_SET_PALETTE_ENTRY, index,
                r & 0x7F, r >> 7, g & 0x7F, g >> 7, b & 0x7F, b >> 7, w & 0x7F, w >> 7);
    }

    public static void reapplyPalette(Receiver push) {
        sendSysex(push, SYSEX_REAPPLY_PALETTE);
    }

    static void sendSysex(Receiver push, int command, int... args) {
        byte[] data = new byte[8 + args.length];
        byte[] header = { (byte) 0xF0, 0x00, 0x21, 0x1D, 0x01, 0x01, (byte) command };
        System.arraycopy(header, 0, data, 0, header.length);
        for (int i = 0; i < args.length; i++) data[7 + i] = (byte) args[i];
        data[data.length - 1] = (byte) 0xF7;
        try {
            push.send(new SysexMessage(data, data.length), -1);
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Extracts {@code --push=<partial-port-name>}, or returns {@link #DEFAULT_PORT}. */
    public static String portFilter(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--push=")) return arg.substring("--push=".length()).trim();
        }
        return DEFAULT_PORT;
    }

    // ── Synth ────────────────────────────────────────────────────────────────

    /** Default audio buffer for the built-in synth, in ms (Gervill's own default is 120). */
    public static final int DEFAULT_SYNTH_LATENCY_MS = 20;

    /**
     * Like {@link DeviceUtil#openReceiver}, but when no {@code --device=} is given,
     * opens Java's built-in synthesizer (Gervill) with a small audio buffer:
     * {@code --latency=<ms>} (default 20) instead of Gervill's 120 ms, so pads
     * feel immediate. Raise it if you hear crackles.
     *
     * Gervill only takes a latency through its internal {@code AudioSynthesizer}
     * interface, so the JVM needs
     * {@code --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED}
     * (launchers and Gradle tasks pass it); without it we fall back to 120 ms.
     */
    public static Receiver openSynth(String[] args) throws MidiUnavailableException {
        for (String arg : args)
            if (arg.startsWith("--device=")) return DeviceUtil.openReceiver(args);

        int latencyMs = DEFAULT_SYNTH_LATENCY_MS;
        for (String arg : args)
            if (arg.startsWith("--latency=")) latencyMs = Integer.parseInt(arg.substring("--latency=".length()).trim());

        Synthesizer synth = MidiSystem.getSynthesizer();
        try {
            Class.forName("com.sun.media.sound.AudioSynthesizer")
                 .getMethod("open", SourceDataLine.class, Map.class)
                 .invoke(synth, null, Map.of("latency", latencyMs * 1000L));
        } catch (ReflectiveOperationException | RuntimeException e) {
            System.err.println("Low-latency synth not available (" + e.getClass().getSimpleName()
                    + ") — using the default 120 ms. Run with --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED");
            if (!synth.isOpen()) synth.open();
        }
        System.out.println("Output device: " + synth.getDeviceInfo().getName()
                + " (default, latency " + synth.getLatency() / 1000 + " ms)");
        return DeviceUtil.withSpy(synth.getReceiver(), args);
    }

    /** Extracts {@code --display=ffm|wayang} (default: wayang). */
    public static String displayFilter(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--display=")) return arg.substring("--display=".length()).trim().toLowerCase();
        }
        return "wayang";
    }

    // ── Display ──────────────────────────────────────────────────────────────

    /**
     * The 960×160 LCD. Draw into it with {@link #draw}; a background thread
     * re-sends the image every 50 ms, because Push blanks the screen when no
     * frame arrives for 2 seconds.
     *
     * Two interchangeable backends talk USB:
     *   • "wayang" — the Wayang library (usb4java / JNI, libusb bundled)
     *   • "ffm"    — {@link LibUsbDisplay}: plain Java calling libusb via FFM
     */
    public static final class Display implements AutoCloseable {

        public static final int WIDTH  = 960;
        public static final int HEIGHT = 160;

        /** Owns the image to draw into and knows how to send it to Push. */
        interface Backend extends AutoCloseable {
            BufferedImage image();
            void sendFrame();
            @Override void close();
        }

        private final Backend backend;
        private final BufferedImage image;
        private final ScheduledExecutorService refresher;

        private Display(Backend backend) {
            this.backend = backend;
            this.image = backend.image();
            refresher = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "push2-display");
                t.setDaemon(true);
                return t;
            });
            refresher.scheduleAtFixedRate(this::sendFrame, 0, 50, TimeUnit.MILLISECONDS);
        }

        /** Opens the display with the backend chosen by {@code --display=} in {@code args}. */
        public static Display open(String[] args) {
            return open(displayFilter(args));
        }

        /**
         * Opens the display with backend "wayang" or "ffm", or returns
         * {@code null} (after printing why) when no Push 2 display is found.
         */
        public static Display open(String backendName) {
            try {
                Backend backend = switch (backendName) {
                    case "wayang" -> new WayangBackend();
                    case "ffm"    -> new LibUsbDisplay();
                    default -> throw new IllegalArgumentException(
                            "unknown display backend \"" + backendName + "\" (use wayang or ffm)");
                };
                System.out.println("Display: Push 2 LCD via " + backendName);
                return new Display(backend);
            } catch (RuntimeException | LinkageError e) {
                System.err.println("Push 2 display not available (" + backendName + "): " + e.getMessage());
                System.err.println("  (Is Push 2 connected and powered on, and Ableton Live quit?"
                        + ("ffm".equals(backendName) ? " Is libusb installed: brew install libusb?)" : ")"));
                return null;
            }
        }

        /** Paints into the display image; the next refresh sends it to Push. */
        public void draw(Consumer<Graphics2D> painter) {
            synchronized (image) {
                Graphics2D g = image.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    painter.accept(g);
                } finally {
                    g.dispose();
                }
            }
        }

        private void sendFrame() {
            synchronized (image) {
                try {
                    backend.sendFrame();
                } catch (RuntimeException e) {
                    // A failed USB transfer must not cancel the schedule — retry next tick.
                }
            }
        }

        @Override
        public void close() {
            refresher.shutdownNow();
            synchronized (image) {
                try { backend.close(); } catch (RuntimeException ignored) {}
            }
        }

        /** Wayang does the USB work (and the pixel encoding) for us. */
        private static final class WayangBackend implements Backend {
            private final BufferedImage image = Wayang.open();
            @Override public BufferedImage image() { return image; }
            @Override public void sendFrame()      { Wayang.sendFrame(); }
            @Override public void close()          { Wayang.close(); }
        }
    }
}
