package net.jetztgrad.midi.workshop;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MidiToRoll — convert a standard MIDI file to piano roll text notation.
 *
 * Run with:  ./gradlew toRoll -PmidiFile=midi/song.mid
 *            ./gradlew toRoll -PmidiFile=midi/song.mid -Pout=rolls/song.roll
 *            ./toRoll.sh --file=midi/song.mid --out=rolls/song.roll
 *
 * Options:
 *   --file=<path>         MIDI file (auto-detects first .mid in midi/ if omitted)
 *   --out=<path>          Output .roll file (prints to stdout if omitted)
 *   --track=<n>           MIDI track index (default: auto-detect busiest track on channel)
 *   --channel=<n>         MIDI channel 0–15 (default: 0)
 *   --resolution=<n>      Grid columns per beat: 4, 8, or 16 (default: 8)
 *
 * Output format:
 *   The generated .roll file is compatible with PianoRollPlayer.
 *   Each stanza covers 2 bars (4/4).  Pipes mark beats.
 *
 * Tick arithmetic:
 *   ticksPerColumn = (filePPQ × 4) / resolution
 *   Example: filePPQ=480, resolution=8  →  240 ticks/column = 1 eighth note ✓
 */
public class MidiToRoll {

    record NoteSpan(int note, long startTick, long endTick) {}

    // ── main ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        String fileArg = null;
        String outArg  = null;
        int    trackArg   = -1;    // -1 = auto-detect
        int    channelArg = -1;    // -1 = auto-detect
        int    resolution = 8;

        for (String arg : args) {
            if      (arg.startsWith("--file="))       fileArg    =         arg.substring("--file=".length());
            else if (arg.startsWith("--out="))        outArg     =         arg.substring("--out=".length());
            else if (arg.startsWith("--track="))      trackArg   = Integer.parseInt(arg.substring("--track=".length()));
            else if (arg.startsWith("--channel="))    channelArg = Integer.parseInt(arg.substring("--channel=".length()));
            else if (arg.startsWith("--resolution=")) resolution = Integer.parseInt(arg.substring("--resolution=".length()));
        }

        File midiFile = resolveMidiFile(fileArg);
        System.err.println("Converting: " + midiFile.getPath());

        Sequence seq     = MidiSystem.getSequence(midiFile);
        int      filePPQ = seq.getResolution();
        int      bpm     = extractBpm(seq);

        // Auto-detect track and channel when not specified
        int[] best  = (trackArg >= 0 && channelArg >= 0)
                ? new int[]{trackArg, channelArg}
                : findBestTrackAndChannel(seq, trackArg, channelArg);
        int track   = best[0];
        int channel = best[1];

        System.err.printf("Track: %d  Channel: %d  BPM: %d  PPQ: %d%n",
                track, channel, bpm, filePPQ);

        List<NoteSpan> spans = extractNoteSpans(seq.getTracks()[track], channel);
        System.err.printf("Extracted %d note span(s)%n", spans.size());

        String output = convert(spans, filePPQ, resolution, bpm, channel, track, midiFile.getName());

        if (outArg != null) {
            Files.writeString(Path.of(outArg), output);
            System.err.println("Written to: " + outArg);
        } else {
            System.out.print(output);
        }
    }

    // ── File resolution ───────────────────────────────────────────────────────

    static File resolveMidiFile(String fileArg) {
        if (fileArg != null) return new File(fileArg);

        Path midiDir = Paths.get("midi");
        if (Files.isDirectory(midiDir)) {
            try {
                return Files.list(midiDir)
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.endsWith(".mid") || name.endsWith(".midi");
                        })
                        .sorted()
                        .map(Path::toFile)
                        .findFirst()
                        .orElseGet(MidiToRoll::exitNoFile);
            } catch (IOException e) {
                return exitNoFile();
            }
        }
        return exitNoFile();
    }

    private static File exitNoFile() {
        System.err.println("No MIDI file found. Use --file=<path> or place a .mid file in midi/");
        System.err.println("Usage: ./gradlew toRoll -PmidiFile=midi/filename.mid");
        System.exit(1);
        return null; // unreachable
    }

    // ── BPM extraction ────────────────────────────────────────────────────────

    /**
     * Scans all tracks for the first SET_TEMPO meta event (type 0x51).
     * Returns the corresponding BPM, or 120 if none is found.
     */
    static int extractBpm(Sequence seq) {
        for (Track t : seq.getTracks()) {
            for (int i = 0; i < t.size(); i++) {
                MidiEvent ev = t.get(i);
                if (ev.getMessage() instanceof MetaMessage meta && meta.getType() == 0x51) {
                    byte[] data = meta.getData();
                    if (data.length >= 3) {
                        int usPerBeat = ((data[0] & 0xFF) << 16)
                                      | ((data[1] & 0xFF) << 8)
                                      |  (data[2] & 0xFF);
                        return (int) Math.round(60_000_000.0 / usPerBeat);
                    }
                }
            }
        }
        return 120;
    }

    // ── Track + channel selection ─────────────────────────────────────────────

    /**
     * Returns {trackIndex, channel} for the track/channel combination that has
     * the most NOTE_ON events (vel &gt; 0).
     *
     * <p>If {@code fixedTrack} or {@code fixedChannel} is &ge; 0, that dimension
     * is locked and only the other is searched.  Falls back to {0, 0} if the
     * sequence contains no notes.
     */
    static int[] findBestTrackAndChannel(Sequence seq, int fixedTrack, int fixedChannel) {
        Track[] tracks    = seq.getTracks();
        int     bestTrack = 0;
        int     bestCh    = 0;
        int     bestCount = 0;

        int tStart = (fixedTrack   >= 0) ? fixedTrack   : 0;
        int tEnd   = (fixedTrack   >= 0) ? fixedTrack   : tracks.length - 1;
        int cStart = (fixedChannel >= 0) ? fixedChannel : 0;
        int cEnd   = (fixedChannel >= 0) ? fixedChannel : 15;

        for (int t = tStart; t <= tEnd; t++) {
            int[] counts = new int[16];
            for (int i = 0; i < tracks[t].size(); i++) {
                MidiEvent ev = tracks[t].get(i);
                if (ev.getMessage() instanceof ShortMessage sm
                        && sm.getCommand() == ShortMessage.NOTE_ON
                        && sm.getData2() > 0
                        && sm.getChannel() >= cStart
                        && sm.getChannel() <= cEnd) {
                    counts[sm.getChannel()]++;
                }
            }
            for (int c = cStart; c <= cEnd; c++) {
                if (counts[c] > bestCount) {
                    bestCount = counts[c];
                    bestTrack = t;
                    bestCh    = c;
                }
            }
        }
        return new int[]{bestTrack, bestCh};
    }

    // ── Note span extraction ──────────────────────────────────────────────────

    /**
     * Pairs NOTE_ON / NOTE_OFF events into {@link NoteSpan} records.
     * Handles NOTE_ON with velocity=0 as NOTE_OFF.
     * Notes that never receive a NOTE_OFF are flushed with a 1-tick duration.
     */
    static List<NoteSpan> extractNoteSpans(Track track, int channel) {
        List<NoteSpan>     spans       = new ArrayList<>();
        Map<Integer, Long> activeNotes = new HashMap<>();

        for (int i = 0; i < track.size(); i++) {
            MidiEvent ev = track.get(i);
            if (!(ev.getMessage() instanceof ShortMessage sm)) continue;
            if (sm.getChannel() != channel) continue;

            int  note = sm.getData1();
            long tick = ev.getTick();

            if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                activeNotes.put(note, tick);
            } else if (sm.getCommand() == ShortMessage.NOTE_OFF
                    || (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0)) {
                Long startTick = activeNotes.remove(note);
                if (startTick != null) spans.add(new NoteSpan(note, startTick, tick));
            }
        }

        // Flush notes that never received NOTE_OFF (minimal 1-tick duration)
        for (Map.Entry<Integer, Long> entry : activeNotes.entrySet()) {
            long start = entry.getValue();
            spans.add(new NoteSpan(entry.getKey(), start, start + 1));
        }

        return spans;
    }

    // ── Conversion ────────────────────────────────────────────────────────────

    /**
     * Converts a list of {@link NoteSpan}s to piano roll text notation.
     *
     * <p>Each stanza covers 2 bars of 4/4 time ({@code stanzaLen = resolution * 2}).
     * Pipes are inserted every {@code colsPerBeat = resolution / 4} columns.
     * Empty stanzas (no notes) are skipped for compact output.
     */
    static String convert(List<NoteSpan> spans, int filePPQ, int resolution,
                          int bpm, int channel, int track, String sourceName) {
        if (spans.isEmpty()) return "# No notes found on channel " + channel + "\n";

        double tpc       = (double)(filePPQ * 4) / resolution;  // ticks per column
        int    stanzaLen = resolution * 2;                       // 2 bars of 4/4
        int    pipeEvery = Math.max(1, resolution / 4);          // columns per beat

        long lastEndTick = spans.stream().mapToLong(NoteSpan::endTick).max().orElse(0);
        int  totalCols   = (int) Math.ceil(lastEndTick / tpc);
        int  numStanzas  = Math.max(1, (int) Math.ceil((double) totalCols / stanzaLen));

        StringBuilder sb = new StringBuilder();
        sb.append("# Converted from: ").append(sourceName)
          .append("  (track ").append(track).append(", channel ").append(channel).append(")\n");
        sb.append("BPM=").append(bpm).append("\n");
        sb.append("CHANNEL=").append(channel).append("\n");
        sb.append("RESOLUTION=").append(resolution).append("\n");

        for (int s = 0; s < numStanzas; s++) {
            int stanzaStart = s * stanzaLen;
            int stanzaEnd   = stanzaStart + stanzaLen;

            // Collect spans that overlap this stanza's column range
            List<NoteSpan> stanzaSpans = new ArrayList<>();
            for (NoteSpan span : spans) {
                int sc = (int) Math.round(span.startTick() / tpc);
                int ec = Math.max(sc + 1, (int) Math.round(span.endTick() / tpc));
                if (ec > stanzaStart && sc < stanzaEnd) stanzaSpans.add(span);
            }
            if (stanzaSpans.isEmpty()) continue;

            // Unique pitches, highest first
            List<Integer> pitches = stanzaSpans.stream()
                    .map(NoteSpan::note)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .toList();

            int barStart = s * 2 + 1;
            sb.append("\n# Bars ").append(barStart).append("–").append(barStart + 1).append("\n");

            for (int pitch : pitches) {
                List<NoteSpan> pitchSpans = stanzaSpans.stream()
                        .filter(sp -> sp.note() == pitch).toList();
                String grid = renderGridRow(pitchSpans, stanzaStart, stanzaLen, pipeEvery, tpc);
                sb.append(String.format("%-4s%s%n", midiToNoteName(pitch), grid));
            }
        }

        return sb.toString();
    }

    // ── Grid rendering ────────────────────────────────────────────────────────

    /**
     * Renders a single note-row grid string for one pitch within one stanza.
     *
     * <p>Column characters:
     * <ul>
     *   <li>{@code x} — note start column
     *   <li>{@code -} — held column (within same note span)
     *   <li>{@code .} — rest
     * </ul>
     * A {@code |} pipe is inserted before every {@code pipeEvery}-th column
     * and a trailing {@code |} closes the row.
     */
    static String renderGridRow(List<NoteSpan> spans, int stanzaStart, int stanzaLen,
                                int pipeEvery, double tpc) {
        char[] grid = new char[stanzaLen];
        Arrays.fill(grid, '.');

        for (NoteSpan span : spans) {
            int sc = (int) Math.round(span.startTick() / tpc);
            int ec = Math.max(sc + 1, (int) Math.round(span.endTick() / tpc));

            for (int absCol = Math.max(stanzaStart, sc);
                     absCol < Math.min(stanzaStart + stanzaLen, ec); absCol++) {
                int offset = absCol - stanzaStart;
                if (absCol == sc) {
                    grid[offset] = 'x';
                } else if (grid[offset] != 'x') {
                    grid[offset] = '-';
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stanzaLen; i++) {
            if (i % pipeEvery == 0) sb.append('|');
            sb.append(grid[i]);
        }
        sb.append('|');   // trailing pipe
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts a MIDI note number to a note name string (e.g. 60 → "C4", 69 → "A4").
     * Middle C (MIDI 60) = C4.
     */
    static String midiToNoteName(int midi) {
        String[] names = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
        int octave  = midi / 12 - 1;
        int noteIdx = midi % 12;
        return names[noteIdx] + octave;
    }
}
