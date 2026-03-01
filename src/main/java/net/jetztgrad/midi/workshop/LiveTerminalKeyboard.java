package net.jetztgrad.midi.workshop;

import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LiveTerminalKeyboard — play notes in real time by typing in the terminal.
 *
 * Run with: ./liveKeyboard.sh ← recommended (raw mode works)
 * ./liveKeyboard.sh --device=XIAO
 *
 * ./gradlew --no-daemon liveTerminalKeyboard ← also works
 * ./gradlew --no-daemon liveTerminalKeyboard -Pdevice=XIAO
 *
 * (Plain ./gradlew liveTerminalKeyboard uses the Gradle daemon,
 * which has no controlling terminal — raw mode will not work.)
 *
 * ──────────────────────────────────────────────────────────────────
 * Keyboard layout (two-row piano, starting at C4):
 *
 * Q W R T Y I O [ ← sharps/flats (black keys)
 * A S D F G H J K L ; ' ← white keys (C D E F G A B C D E F)
 *
 * German QWERTZ equivalents: Ö = ; Ä = ' Ü = [
 *
 * +/= octave up - octave down ESC quit
 * 1-9/0 select instrument (Piano / Organ / Guitar / …)
 * ──────────────────────────────────────────────────────────────────
 *
 * Because terminals do not deliver key-release events, key-hold is
 * simulated: each key-repeat event extends the note-off deadline by
 * DEFAULT_DURATION_MS. The note sounds until repeats stop (key released),
 * then fades after one more DEFAULT_DURATION_MS tail. Chords work because
 * each character gets its own deadline tracked independently.
 *
 * macOS / Linux: raw terminal mode is activated via {@code stty raw -echo}.
 * Input is read from /dev/tty directly, bypassing any stdin pipe, so this
 * works correctly even when launched via
 * {@code ./gradlew liveTerminalKeyboard}.
 * Windows: falls back to buffered mode (type letters then Enter to play).
 */
public class LiveTerminalKeyboard {
    static final int DEFAULT_OCTAVE  = 4;
    static final int CHANNEL         = 0;

    // Initial note duration — must outlast the OS key-repeat initial delay
    // (typically ~500 ms on macOS/Linux) so the first repeat extends the
    // deadline rather than being mistaken for a new keypress.
    static final int INITIAL_HOLD_MS = 700;
    // How far each key-repeat event pushes the note-off deadline forward.
    // Needs to be comfortably longer than the key-repeat interval (~30 ms).
    static final int REPEAT_TAIL_MS = 200;

    static final String[] NOTE_NAMES = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};

    // Character → semitone offset from root C of the current octave
    // Bottom row: white keys C D E F G A B C(+1) D(+1) E(+1) F(+1)
    // Top row: black keys C# D# F# G# A# C#(+1) D#(+1) F#(+1)
    // Both 'y' and 'z' map to A# to support QWERTY and QWERTZ keyboards.
    // German QWERTZ: 'ö' is the same physical key as ';' on US layout;
    // 'ä' and '\'' are the same physical key; 'ü' and '[' are the same.
    static final Map<Character, Integer> KEY_TO_OFFSET = Map.ofEntries(
        Map.entry('a',  0), Map.entry('s',  2), Map.entry('d',  4), Map.entry('f',  5),
        Map.entry('g',  7), Map.entry('h',  9), Map.entry('j', 11), Map.entry('k', 12),
            Map.entry('l', 14), Map.entry(';', 16), Map.entry('ö', 16), // E(+1)
            Map.entry('\'', 17), Map.entry('ä', 17), // F(+1)
        Map.entry('q',  1), Map.entry('w',  3), Map.entry('r',  6), Map.entry('t',  8),
            Map.entry('y', 10), Map.entry('z', 10), Map.entry('i', 13), Map.entry('o', 15),
            Map.entry('[', 18), Map.entry('ü', 18) // F#(+1)
    );

    static volatile int octave          = DEFAULT_OCTAVE;
    static volatile int currentProgram  = 0;  // active GM patch number (0–127)
    static Receiver receiver;

    // Per-character note tracking for key-hold simulation.
    // heldNotes: char → MIDI note number currently sounding for that key
    // noteDeadlines: char → note-off deadline (ms epoch), pushed forward on repeats
    static final ConcurrentHashMap<Character, Integer> heldNotes = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<Character, AtomicLong> noteDeadlines = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        // Force UTF-8 on stdout so box-drawing characters and umlauts render
        // correctly even when Gradle (or another launcher) pipes the output.
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));

        receiver = DeviceUtil.openReceiver(args);
        int initialPatch = DeviceUtil.patchFilter(args);
        currentProgram = initialPatch;
        DeviceUtil.sendProgramChange(receiver, CHANNEL, initialPatch);
        if (initialPatch != 0)
            System.out.printf("  Instrument: %s  (GM patch %d)\r\n",
                    DeviceUtil.GM_PATCH_NAMES[initialPatch], initialPatch);

        System.out.println("┌─────────────────────────────────────────────────────┐");
        System.out.println("│  Live Terminal Keyboard                             │");
        System.out.println("│                                                     │");
        System.out.println("│   Q  W     R  T  Y     I  O  [  ← sharps/flats      │");
        System.out.println("│   A  S  D  F  G  H  J  K  L  ;  '  ← white keys     │");
        System.out.println("│   QWERTZ: Ö=;   Ä='   Ü=[                           │");
        System.out.println("│                                                     │");
        System.out.println("│   +/=  octave up   |   -  octave down               │");
        System.out.println("│   1-9/0  select instrument                          │");
        System.out.println("│   ESC  quit (All Notes Off)                         │");
        System.out.println("└─────────────────────────────────────────────────────┘");
        printOctave();

        if (isUnixLike()) {
            runRawMode();
        } else {
            runBufferedMode();
        }
    }

    // ── Raw mode (macOS / Linux) ─────────────────────────────────────────────

    static void runRawMode() throws Exception {
        boolean rawEnabled = setRawMode(true);
        if (!rawEnabled) {
            System.out.println("(Could not enable raw mode — the Gradle daemon has no controlling terminal.)");
            System.out
                    .println("(Use instead:  ./liveKeyboard.sh [--device=<name>]          ← compiles & runs directly)");
            System.out.println("(          or: ./gradlew --no-daemon liveTerminalKeyboard [-Pdevice=<name>])");
            System.out.println("(Falling back to buffered mode: type key(s) and press Enter to play.)");
            runBufferedMode();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            silentRestoreTerminal();
            silentPanic();
        }));

        // Read from /dev/tty directly so raw mode works even when stdin is
        // piped (e.g. launched via ./gradlew). InputStreamReader decodes
        // UTF-8 so multi-byte chars like Ö, Ä, Ü arrive as single chars.
        var reader = new java.io.InputStreamReader(openTty(), java.nio.charset.StandardCharsets.UTF_8);
        try {
            while (true) {
                int c = reader.read();
                if (c == -1)
                    break; // EOF
                if (c == 3)
                    break; // Ctrl+C
                if (c == 27) { // ESC
                    panic();
                    break;
                }
                handleChar((char) c);
            }
        } finally {
            setRawMode(false);
            receiver.close();
            System.out.print("\r\nBye!\r\n");
        }
    }

    static java.io.InputStream openTty() {
        try {
            return new java.io.FileInputStream("/dev/tty");
        } catch (Exception e) {
            return System.in; // fallback (e.g. no /dev/tty on this system)
        }
    }

    static boolean setRawMode(boolean enable) {
        try {
            String[] cmd = enable
                    ? new String[]{"stty", "raw", "-echo"}
                    : new String[]{"stty", "sane"};
            Process p = new ProcessBuilder(cmd)
                    .redirectInput(new File("/dev/tty"))
                    .start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    static void silentRestoreTerminal() {
        try { setRawMode(false); } catch (Exception ignored) {}
    }

    // ── Buffered mode (Windows / fallback) ──────────────────────────────────

    static void runBufferedMode() throws Exception {
        System.out.println("(Buffered mode: type key(s) and press Enter to play)");
        try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                for (char ch : line.toCharArray()) {
                    handleChar(ch);
                }
            }
        } finally {
            receiver.close();
        }
    }

    // ── Key handling ─────────────────────────────────────────────────────────

    static void handleChar(char ch) {
        ch = Character.toLowerCase(ch);

        if (ch == '+' || ch == '=') {
            if (octave < 8) { octave++; printOctave(); }
            return;
        }
        if (ch == '-') {
            if (octave > 0) { octave--; printOctave(); }
            return;
        }

        if (ch >= '1' && ch <= '9') { selectPreset(ch - '1'); return; }
        if (ch == '0')               { selectPreset(9);        return; }

        Integer offset = KEY_TO_OFFSET.get(ch);
        if (offset == null) return;

        int note = 12 * (octave + 1) + offset;
        if (note < 0 || note > 127) return;

        // Key-repeat: same note already sounding → just extend the note-off deadline.
        AtomicLong existing = noteDeadlines.get(ch);
        if (existing != null && heldNotes.getOrDefault(ch, -1) == note) {
            existing.set(System.currentTimeMillis() + REPEAT_TAIL_MS);
            return;
        }

        // New note: use INITIAL_HOLD_MS so the deadline outlasts the OS
        // key-repeat initial delay and the first repeat extends it cleanly.
        int velocity = 50 + (int)(Math.random() * 31);
        playNote(ch, note, velocity, System.currentTimeMillis() + INITIAL_HOLD_MS);
    }

    static void playNote(char key, int note, int velocity, long initialDeadline) {
        try {
            receiver.send(new ShortMessage(ShortMessage.NOTE_ON, 0, note, velocity), -1);
            System.out.printf("  %-3s%-2d  (note %3d, vel %3d)\r\n",
                    NOTE_NAMES[note % 12], note / 12 - 1, note, velocity);
        } catch (Exception e) {
            System.err.println("Error sending note on: " + e.getMessage());
            return;
        }

        AtomicLong deadline = new AtomicLong(initialDeadline);
        noteDeadlines.put(key, deadline);
        heldNotes.put(key, note);

        Thread.ofVirtual().start(() -> {
            try {
                while (true) {
                    long remaining = deadline.get() - System.currentTimeMillis();
                    if (remaining <= 0)
                        break;
                    Thread.sleep(remaining + 1); // +1 avoids sub-ms spin on rounding
                    // Loop: deadline may have been pushed forward while we slept.
                }
            } catch (InterruptedException ignored) {
            }
            heldNotes.remove(key, note);
            noteDeadlines.remove(key, deadline);
            try {
                receiver.send(new ShortMessage(ShortMessage.NOTE_OFF, 0, note, 0), -1);
            } catch (Exception ignored) {}
        });
    }

    // ── Panic / helpers ──────────────────────────────────────────────────────

    static void selectPreset(int index) {
        int patch = DeviceUtil.PRESET_PROGRAMS[index];
        currentProgram = patch;
        silentPanic();
        DeviceUtil.sendProgramChange(receiver, CHANNEL, patch);
        System.out.printf("  Instrument %d: %s  (GM patch %d)\r\n",
                index == 9 ? 0 : index + 1, DeviceUtil.PRESET_NAMES[index], patch);
    }

    static void panic() {
        System.out.print("\r\n  [Panic] All Notes Off\r\n");
        silentPanic();
    }

    static void silentPanic() {
        if (receiver == null) return;
        for (int ch = 0; ch < 16; ch++) {
            try {
                receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, ch, 123, 0), -1);
            } catch (Exception ignored) {}
        }
    }

    static void printOctave() {
        int rootNote = 12 * (octave + 1);
        System.out.printf("  Octave: %d   (C%d = note %d)\r\n", octave, octave, rootNote);
    }

    static boolean isUnixLike() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") || os.contains("nix") || os.contains("nux");
    }
}
