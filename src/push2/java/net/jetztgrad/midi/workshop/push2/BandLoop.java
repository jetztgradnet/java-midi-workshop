package net.jetztgrad.midi.workshop.push2;

import net.jetztgrad.midi.workshop.DeviceUtil;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.awt.Color;
import java.util.List;

/**
 * BandLoop — a 4-bar groove (Am – F – C – G) with one track per band member.
 *
 * All tracks start muted and the loop runs silently from the start of a game.
 * Finding a member un-mutes its track, so it joins exactly in time and the
 * song builds up while the board is being solved.
 *
 * MIDI channels (1-based, for routing to an external synth):
 *   1 Bass · 2 Keys · 3 Pad · 4 Arp · 5 Lead · 6 Brass · 10 Drums
 */
final class BandLoop {

    /** A band member: name, MIDI channel (0-based), GM program (-1 = drums), signature note, colour. */
    record Member(String name, int channel, int program, int note, Color color) {}

    static final int DRUMS = 9;

    static final List<Member> MEMBERS = List.of(
            new Member("Kick",   DRUMS, -1, 36, new Color(201,  60,   0)),
            new Member("Snare",  DRUMS, -1, 38, new Color(255, 220,   0)),
            new Member("Hi-hat", DRUMS, -1, 42, new Color(160, 255,  40)),
            new Member("Clap",   DRUMS, -1, 39, new Color(255,  60, 200)),
            new Member("Bass",   0,     38, 33, new Color( 30,  90, 255)),   // Synth Bass 1
            new Member("Keys",   1,      4, 60, new Color( 38, 158, 114)),   // Electric Piano 1
            new Member("Pad",    2,     89, 69, new Color(150,  90, 255)),   // Pad 2 (warm)
            new Member("Arp",    3,     46, 76, new Color( 40, 200,  80)),   // Orchestral Harp
            new Member("Lead",   4,     80, 76, new Color(255,  40,  50)),   // Lead 1 (square)
            new Member("Brass",  5,     61, 64, new Color(255, 170, 120)));  // Brass Section

    static final int BPM         = 108;
    static final int TICKS_16TH  = 6;               // resolution 24 PPQ → 6 ticks per 16th
    static final int BARS        = 4;
    static final int LOOP_TICKS  = BARS * 16 * TICKS_16TH;

    // Am – F – C – G
    static final int[]   ROOTS  = { 33, 29, 36, 31 };     // bass roots: A1, F1, C2, G1
    static final int[][] TRIADS = {
            { 57, 60, 64 }, { 57, 60, 65 }, { 55, 60, 64 }, { 55, 59, 62 } };

    private final Sequencer sequencer;

    BandLoop(Receiver out) throws MidiUnavailableException, InvalidMidiDataException {
        for (Member m : MEMBERS)
            if (m.program() >= 0) DeviceUtil.sendProgramChange(out, m.channel(), m.program());

        Sequence sequence = new Sequence(Sequence.PPQ, 4 * TICKS_16TH);
        for (int i = 0; i < MEMBERS.size(); i++) {
            Track track = sequence.createTrack();
            write(i, track, MEMBERS.get(i).channel());
        }
        // A marker at the loop end makes the sequence exactly LOOP_TICKS long.
        sequence.getTracks()[0].add(new MidiEvent(new MetaMessage(0x06, new byte[0], 0), LOOP_TICKS));

        sequencer = MidiSystem.getSequencer(false);   // false = not wired to the default synth
        sequencer.open();
        sequencer.getTransmitter().setReceiver(out);
        sequencer.setSequence(sequence);
        sequencer.setLoopStartPoint(0);
        sequencer.setLoopEndPoint(LOOP_TICKS);
        sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
    }

    /** Starts the loop from bar 1 with every member muted. */
    void start() {
        sequencer.stop();
        for (int i = 0; i < MEMBERS.size(); i++) sequencer.setTrackMute(i, true);
        sequencer.setTickPosition(0);
        sequencer.start();
        sequencer.setTempoInBPM(BPM);
    }

    void join(int member) { sequencer.setTrackMute(member, false); }
    void stop()           { sequencer.stop(); }
    void close()          { sequencer.close(); }

    // ── The song ─────────────────────────────────────────────────────────────

    private static void write(int member, Track t, int ch) throws InvalidMidiDataException {
        for (int bar = 0; bar < BARS; bar++) {
            int s = bar * 16;                          // first 16th of this bar
            int root = ROOTS[bar];
            int[] chord = TRIADS[bar];
            switch (MEMBERS.get(member).name()) {
                case "Kick" -> {
                    for (int beat = 0; beat < 16; beat += 4) note(t, ch, 36, 110, s + beat, 1);
                }
                case "Snare" -> {
                    note(t, ch, 38, 100, s + 4, 1);
                    note(t, ch, 38, 100, s + 12, 1);
                }
                case "Hi-hat" -> {
                    for (int i = 0; i < 16; i += 2) note(t, ch, 42, i % 4 == 2 ? 85 : 55, s + i, 1);
                }
                case "Clap" -> {
                    note(t, ch, 39, 90, s + 12, 1);
                    if (bar == BARS - 1) {                    // little fill into the loop
                        note(t, ch, 39, 70, s + 14, 1);
                        note(t, ch, 39, 80, s + 15, 1);
                    }
                }
                case "Bass" -> {
                    note(t, ch, root,      100, s,      3);
                    note(t, ch, root,       80, s + 3,  2);
                    note(t, ch, root + 12,  90, s + 6,  2);
                    note(t, ch, root,       90, s + 8,  2);
                    note(t, ch, root + 12,  80, s + 10, 2);
                    note(t, ch, root + 7,   85, s + 14, 2);
                }
                case "Keys" -> {
                    for (int n : chord) {
                        note(t, ch, n, 75, s + 4,  3);
                        note(t, ch, n, 60, s + 10, 1);
                        note(t, ch, n, 75, s + 12, 3);
                    }
                }
                case "Pad" -> {
                    for (int n : chord) note(t, ch, n + 12, 50, s, 16);
                }
                case "Arp" -> {
                    int[] tones = { chord[0] + 12, chord[1] + 12, chord[2] + 12, chord[0] + 24 };
                    for (int i = 0; i < 16; i++) note(t, ch, tones[i % 4], i % 4 == 0 ? 72 : 55, s + i, 1);
                }
                case "Lead" -> {
                    for (int[] n : LEAD[bar]) note(t, ch, n[1], 85, s + n[0], n[2]);
                }
                case "Brass" -> {
                    for (int n : chord) note(t, ch, n + 12, 90, s, 2);
                    if (bar == BARS - 1)
                        for (int n : TRIADS[0]) note(t, ch, n + 12, 80, s + 14, 2);   // pickup to Am
                }
                default -> throw new IllegalStateException(MEMBERS.get(member).name());
            }
        }
    }

    // Lead melody, A minor pentatonic: { 16th position, note, length in 16ths }
    private static final int[][][] LEAD = {
            { { 0, 76, 3 }, { 3, 74, 1 }, { 4, 72, 2 }, { 6, 69, 2 }, { 8, 72, 6 }, { 14, 74, 2 } },
            { { 0, 72, 6 }, { 6, 69, 2 }, { 8, 67, 4 }, { 12, 69, 4 } },
            { { 0, 76, 3 }, { 3, 79, 1 }, { 4, 76, 2 }, { 6, 74, 2 }, { 8, 72, 4 }, { 12, 74, 4 } },
            { { 0, 74, 4 }, { 4, 71, 2 }, { 6, 74, 2 }, { 8, 79, 6 }, { 14, 76, 2 } },
    };

    /** Adds a note; it ends one tick early so no note-off falls on the loop point. */
    private static void note(Track t, int ch, int note, int velocity, int start16th, int length16ths)
            throws InvalidMidiDataException {
        t.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, ch, note, velocity),
                (long) start16th * TICKS_16TH));
        t.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, ch, note, 0),
                (long) (start16th + length16ths) * TICKS_16TH - 1));
    }
}
