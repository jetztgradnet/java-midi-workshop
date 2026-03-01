package net.jetztgrad.midi.workshop;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * PianoRollPlayer — read text-based piano-roll notation and play it via the Java MIDI sequencer.
 *
 * Run with:  ./gradlew pianoRoll
 *            ./gradlew pianoRoll -Pfile=my_melody.roll
 *            ./pianoRoll.sh [--device=<name>] [--patch=<0-127|name>] [--file=<path>]
 *
 * ── Notation format ──────────────────────────────────────────────────────────────
 *
 *   Header (key=value, before first note row):
 *     BPM=120          # beats per minute (default 120)
 *     CHANNEL=0        # MIDI channel 0–15 (default 0)
 *     RESOLUTION=16    # grid columns per beat: 4=quarter, 8=8th, 16=16th (default 16)
 *     PGM=0            # GM program number 0–127 or partial name (e.g. PGM=Violin)
 *
 *   Note rows:
 *     <note>  <grid>
 *
 *     Note name: C4, D#3, Bb5 (flat = b or B), or raw MIDI number 60
 *     Grid chars:
 *       .  rest
 *       x  note start (one column duration)
 *       -  hold / extend previous x in this row
 *       |  bar-line separator (visual only, ignored by parser)
 *
 *   # lines are comments.  Blank lines separate stanzas; stanzas play sequentially.
 *
 * ── Tick arithmetic ──────────────────────────────────────────────────────────────
 *   PPQ = 4  (4 ticks per beat, same as all other workshop examples)
 *   ticksPerColumn = PPQ * 4 / RESOLUTION
 *   RESOLUTION=16  →  1 tick/column
 *   RESOLUTION=8   →  2 ticks/column
 *   RESOLUTION=4   →  4 ticks/column  (1 column = 1 quarter-note beat)
 */
public class PianoRollPlayer {

    /** Ticks per beat — matches all other workshop examples. */
    static final int PPQ = 4;

    /** Built-in demo: two stanzas, 16th-note grid. */
    static final String DEFAULT_NOTATION = """
            BPM=120
            CHANNEL=0
            RESOLUTION=16

            # Stanza 1 — descending phrase with some rhythm
            E5  |x..x....|x.......|
            D5  |....x...|..x.x...|
            C5  |x.......|x.......|
            G4  |........|...x....|

            # Stanza 2 — variation
            E5  |x.......|x.......|
            D5  |..x.x...|..x.x...|
            C5  |x.......|x.......|
            """;

    // ── Records ──────────────────────────────────────────────────────────────────

    record Settings(int bpm, int channel, int resolution, String pgm) {}

    record NoteEvent(int note, int startTick, int durationTicks, int velocity) {}

    record ParseResult(Settings settings, List<NoteEvent> events) {}

    // ── main ─────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {

        // ── 1. Open MIDI output receiver ─────────────────────────────────────────
        var receiver = DeviceUtil.openReceiver(args);

        // ── 2. Load notation: --file=<path> overrides the built-in default ────────
        String notation = DEFAULT_NOTATION;
        for (String arg : args) {
            if (arg.startsWith("--file=")) {
                String path = arg.substring("--file=".length()).trim();
                notation = Files.readString(Paths.get(path));
                System.out.println("Loaded notation: " + path);
                break;
            }
        }

        // ── 3. Parse ──────────────────────────────────────────────────────────────
        ParseResult result   = parseNotation(notation);
        Settings    settings = result.settings();

        // ── 4. Resolve patch: --patch= CLI arg wins over PGM= header ─────────────
        int patch = resolvePatch(args, settings.pgm());
        DeviceUtil.sendProgramChange(receiver, settings.channel(), patch);
        System.out.println("Instrument: " + DeviceUtil.GM_PATCH_NAMES[patch]
                + " (patch " + patch + ")");
        System.out.printf("BPM=%d  Channel=%d  Resolution=%d  Notes=%d%n",
                settings.bpm(), settings.channel() + 1,
                settings.resolution(), result.events().size());

        // ── 5. Build sequence ─────────────────────────────────────────────────────
        Sequence sequence = buildSequence(result);

        // ── 6. Open sequencer and connect to receiver ─────────────────────────────
        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.setSequence(sequence);
        sequencer.setTempoInBPM(settings.bpm());

        // ── 7. Play ───────────────────────────────────────────────────────────────
        System.out.println("Playing piano roll...");
        sequencer.start();

        while (sequencer.isRunning()) {
            Thread.sleep(50);
        }

        Thread.sleep(500);   // let the synth finish the last note's release
        sequencer.stop();
        sequencer.close();
        System.out.println("Done.");
    }

    // ── Patch resolution ──────────────────────────────────────────────────────────

    /**
     * Resolves the GM program number.
     * Priority: --patch= CLI arg > PGM= header > 0 (Grand Piano).
     */
    static int resolvePatch(String[] args, String pgmHeader) {
        for (String arg : args) {
            if (arg.startsWith("--patch=")) return DeviceUtil.patchFilter(args);
        }
        if (!pgmHeader.isEmpty()) {
            // Try numeric first
            try { return Math.max(0, Math.min(127, Integer.parseInt(pgmHeader))); }
            catch (NumberFormatException ignored) {}
            // Fall back to partial GM name match (same logic as DeviceUtil.patchFilter)
            String lower = pgmHeader.toLowerCase();
            for (int i = 0; i < DeviceUtil.GM_PATCH_NAMES.length; i++) {
                if (DeviceUtil.GM_PATCH_NAMES[i].toLowerCase().contains(lower)) return i;
            }
        }
        return 0;
    }

    // ── Parser ────────────────────────────────────────────────────────────────────

    /**
     * Parses the full notation text into a {@link ParseResult}.
     *
     * <p>Header key=value lines are consumed before the first note row.
     * Blank lines (after header) separate stanzas; each stanza plays sequentially
     * after the previous one.
     */
    static ParseResult parseNotation(String text) {

        // Header defaults
        int    bpm        = 120;
        int    channel    = 0;
        int    resolution = 16;
        String pgm        = "";

        boolean            headerDone    = false;
        List<NoteEvent>    events        = new ArrayList<>();
        List<List<String>> stanzas       = new ArrayList<>();
        List<String>       currentStanza = new ArrayList<>();

        for (String rawLine : text.lines().toList()) {
            String line = rawLine.trim();

            // Blank lines flush the current stanza
            if (line.isEmpty()) {
                if (!currentStanza.isEmpty()) {
                    stanzas.add(List.copyOf(currentStanza));
                    currentStanza.clear();
                }
                continue;
            }

            // Comment lines are ignored everywhere
            if (line.startsWith("#")) continue;

            // Header entries: KEY=VALUE where KEY is all letters (no digits/spaces)
            if (!headerDone && isHeaderLine(line)) {
                int    eq    = line.indexOf('=');
                String key   = line.substring(0, eq).trim().toUpperCase();
                String value = line.substring(eq + 1).trim();
                switch (key) {
                    case "BPM"        -> { try { bpm        = Integer.parseInt(value);                               } catch (NumberFormatException ignored) {} }
                    case "CHANNEL"    -> { try { channel    = Math.max(0, Math.min(15, Integer.parseInt(value)));    } catch (NumberFormatException ignored) {} }
                    case "RESOLUTION" -> { try { resolution = Integer.parseInt(value);                               } catch (NumberFormatException ignored) {} }
                    case "PGM"        -> pgm = value;
                }
                continue;
            }

            // Note row
            headerDone = true;
            currentStanza.add(line);
        }
        if (!currentStanza.isEmpty()) stanzas.add(List.copyOf(currentStanza));

        Settings settings       = new Settings(bpm, channel, resolution, pgm);
        int      ticksPerColumn = Math.max(1, PPQ * 4 / resolution);

        int stanzaStartTick = 0;
        for (List<String> stanza : stanzas) {
            int stanzaLen = processStanza(stanza, settings.channel(),
                    stanzaStartTick, ticksPerColumn, events);
            stanzaStartTick += stanzaLen;
        }
        System.out.printf("Parsed %d stanza(s), %d note(s).%n",
                stanzas.size(), events.size());

        return new ParseResult(settings, events);
    }

    /** A header line has the form KEY=VALUE where KEY consists only of letters. */
    private static boolean isHeaderLine(String line) {
        int eq = line.indexOf('=');
        if (eq <= 0) return false;
        return line.substring(0, eq).trim().matches("[A-Za-z]+");
    }

    /**
     * Parses one stanza, emits {@link NoteEvent}s into {@code events}.
     *
     * <p>Each row: {@code <noteName>  <grid>}
     * Pipe characters ({@code |}) are stripped (visual bar-line separators).
     *
     * @return total ticks consumed by this stanza (maxColumns × ticksPerColumn)
     */
    private static int processStanza(List<String> rows, int channel,
                                     int stanzaStartTick, int ticksPerColumn,
                                     List<NoteEvent> events) {
        // Local record — Java 16+; we are on Java 21
        record Row(int midi, char[] grid) {}

        List<Row> parsed  = new ArrayList<>();
        int       maxCols = 0;

        for (String line : rows) {
            int sp = firstSpaceIndex(line);
            if (sp < 0) continue;
            String noteName = line.substring(0, sp);
            String gridStr  = line.substring(sp).stripLeading().replace("|", "");
            int    midi     = noteNameToMidi(noteName);
            if (midi < 0) continue;   // skip unparseable note names
            parsed.add(new Row(midi, gridStr.toCharArray()));
            if (gridStr.length() > maxCols) maxCols = gridStr.length();
        }

        for (Row row : parsed) {
            int pendingStart    = -1;
            int pendingDuration =  0;

            for (int c = 0; c < row.grid().length; c++) {
                switch (row.grid()[c]) {
                    case 'x' -> {
                        if (pendingStart >= 0)
                            flushNote(events, row.midi(), pendingStart, pendingDuration, ticksPerColumn);
                        pendingStart    = stanzaStartTick + c * ticksPerColumn;
                        pendingDuration = 1;
                    }
                    case '-' -> {
                        if (pendingStart >= 0) pendingDuration++;   // extend previous note
                    }
                    default -> {
                        // '.' or anything else = rest; flush any pending note
                        if (pendingStart >= 0) {
                            flushNote(events, row.midi(), pendingStart, pendingDuration, ticksPerColumn);
                            pendingStart = -1;
                        }
                    }
                }
            }
            // Flush a note held to the end of the row
            if (pendingStart >= 0)
                flushNote(events, row.midi(), pendingStart, pendingDuration, ticksPerColumn);
        }

        return maxCols * ticksPerColumn;
    }

    private static void flushNote(List<NoteEvent> events, int midi,
                                  int startTick, int duration, int ticksPerColumn) {
        events.add(new NoteEvent(midi, startTick, duration * ticksPerColumn, randomVelocity()));
    }

    private static int firstSpaceIndex(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t') return i;
        }
        return -1;
    }

    // ── Sequence builder ──────────────────────────────────────────────────────────

    /**
     * Builds a {@link Sequence} from the parsed events.
     * Uses {@link #PPQ} as the tick resolution (consistent with all other examples).
     */
    static Sequence buildSequence(ParseResult result) throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        Track    track    = sequence.createTrack();

        long maxTick = 0;
        for (NoteEvent e : result.events()) {
            addNote(track, result.settings().channel(),
                    e.note(), e.velocity(), e.startTick(), e.durationTicks());
            long end = (long) e.startTick() + e.durationTicks();
            if (end > maxTick) maxTick = end;
        }

        // End-of-Track meta message — required by the MIDI spec
        MetaMessage eot = new MetaMessage();
        eot.setMessage(0x2F, new byte[0], 0);
        track.add(new MidiEvent(eot, maxTick + PPQ));   // one beat after last note

        return sequence;
    }

    /**
     * Adds a NOTE_ON / NOTE_OFF pair to a track.
     *
     * @param track         the track to add to
     * @param channel       MIDI channel (0–15)
     * @param note          MIDI note number (0–127; 60 = middle C)
     * @param velocity      note velocity (1–127)
     * @param startTick     tick at which the note starts
     * @param durationTicks how long the note lasts in ticks
     */
    static void addNote(Track track, int channel, int note, int velocity,
                        long startTick, long durationTicks) throws InvalidMidiDataException {
        ShortMessage on  = new ShortMessage(ShortMessage.NOTE_ON,  channel, note, velocity);
        ShortMessage off = new ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0);
        track.add(new MidiEvent(on,  startTick));
        track.add(new MidiEvent(off, startTick + durationTicks));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /**
     * Converts a note name ("C4", "D#3", "Bb5") or raw MIDI number ("60")
     * to a MIDI note number.  Middle C (C4) = 60.
     *
     * <p>Accidentals: {@code #} = sharp, {@code b} or {@code B} = flat.
     *
     * @return MIDI note number (0–127), or {@code -1} if the input is not parseable
     */
    static int noteNameToMidi(String name) {
        if (name == null || name.isEmpty()) return -1;

        // Raw MIDI number (e.g. "60")
        try { return Math.max(0, Math.min(127, Integer.parseInt(name))); }
        catch (NumberFormatException ignored) {}

        // Note letter
        char letter = Character.toUpperCase(name.charAt(0));
        int base = switch (letter) {
            case 'C' -> 0; case 'D' -> 2; case 'E' -> 4; case 'F' -> 5;
            case 'G' -> 7; case 'A' -> 9; case 'B' -> 11; default -> -99;
        };
        if (base == -99) return -1;

        int idx = 1;
        int acc = 0;
        if (idx < name.length()) {
            char c = name.charAt(idx);
            if      (c == '#')           { acc =  1; idx++; }  // sharp
            else if (c == 'b' || c == 'B') { acc = -1; idx++; }  // flat
        }

        if (idx >= name.length()) return -1;   // no octave digit
        int octave;
        try { octave = Integer.parseInt(name.substring(idx)); }
        catch (NumberFormatException e) { return -1; }

        // MIDI number: middle C (C4) = 60  →  (octave + 1) * 12 + base
        return Math.max(0, Math.min(127, (octave + 1) * 12 + base + acc));
    }

    /**
     * Returns a velocity near 72 with subtle random variation (±12).
     * Keeps the piano roll sounding slightly humanised without extra notation.
     *
     * TODO: support explicit velocity annotations in the grid, e.g. "x80"
     */
    static int randomVelocity() {
        return 72 + ThreadLocalRandom.current().nextInt(-12, 13);
    }
}
