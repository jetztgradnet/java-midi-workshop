package net.jetztgrad.midi.workshop;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * DrumPattern — play a basic 4/4 rock beat using GM channel 10 percussion.
 *
 * Run with:  ./gradlew drumPattern
 *
 * Key facts:
 *   • MIDI channel 10 (index 9 in code) is reserved for percussion in General MIDI.
 *   • Each note number maps to a specific drum sound (the GM Drum Map).
 *   • Common GM drum note numbers:
 *       36 = Bass Drum 1 (Kick)
 *       38 = Acoustic Snare
 *       42 = Closed Hi-Hat
 *       46 = Open Hi-Hat
 *       49 = Crash Cymbal 1
 *       51 = Ride Cymbal 1
 *
 * Pattern (2 bars of 4/4, 4 ticks/beat, 8 ticks/bar):
 *
 *   Beat:     1  &  2  &  3  &  4  &   |  1  &  2  &  3  &  4  &
 *   Kick:     X        X               |  X        X
 *   Snare:       X        X            |     X        X
 *   Hi-Hat:   X  X  X  X  X  X  X  X  |  X  X  X  X  X  X  X  X
 */
public class DrumPattern {

    // Channel 9 in Java = MIDI channel 10 (GM percussion)
    static final int DRUM_CHANNEL = 9;

    // GM Drum Map note numbers
    static final int KICK    = 36;
    static final int SNARE   = 38;
    static final int HI_HAT  = 42;
    static final int CRASH   = 49;

    // Timing: 4 ticks/beat, 2 bars = 32 ticks total
    static final int TICKS_PER_BEAT = 4;
    static final int BARS = 2;
    static final int TOTAL_TICKS = BARS * 4 * TICKS_PER_BEAT;   // 32

    public static void main(String[] args) throws Exception {
        Sequence sequence = new Sequence(Sequence.PPQ, TICKS_PER_BEAT);
        Track track = sequence.createTrack();

        // Build the pattern across 2 bars (ticks 0–31)
        for (int bar = 0; bar < BARS; bar++) {
            int base = bar * 4 * TICKS_PER_BEAT;   // 0 or 16

            // Kick on beats 1 and 3
            addNote(track, DRUM_CHANNEL, KICK,   100, base,      2);
            addNote(track, DRUM_CHANNEL, KICK,   100, base + 8,  2);

            // Snare on beats 2 and 4
            addNote(track, DRUM_CHANNEL, SNARE,   90, base + 4,  2);
            addNote(track, DRUM_CHANNEL, SNARE,   90, base + 12, 2);

            // Closed hi-hat on every eighth note (every 2 ticks)
            for (int i = 0; i < 8; i++) {
                addNote(track, DRUM_CHANNEL, HI_HAT, 70, base + i * 2, 1);
            }
        }

        // Opening crash on bar 1 beat 1
        addNote(track, DRUM_CHANNEL, CRASH, 110, 0, TICKS_PER_BEAT);

        // End-of-track marker
        MetaMessage eot = new MetaMessage();
        eot.setMessage(0x2F, new byte[0], 0);
        track.add(new MidiEvent(eot, TOTAL_TICKS));

        // Set up and play
        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.setSequence(sequence);
        sequencer.setTempoInBPM(120);

        System.out.println("Playing 2-bar drum pattern at 120 BPM...");
        System.out.println("Kick (36) + Snare (38) + Hi-Hat (42) on channel 10");
        sequencer.start();

        while (sequencer.isRunning()) {
            Thread.sleep(50);
        }

        Thread.sleep(300);
        sequencer.close();
        System.out.println("Done.");
    }

    /** Adds a NOTE_ON / NOTE_OFF pair to the track. */
    static void addNote(Track track, int channel, int note, int velocity,
                        long startTick, long durationTicks) throws InvalidMidiDataException {
        track.add(new MidiEvent(
                new ShortMessage(ShortMessage.NOTE_ON,  channel, note, velocity), startTick));
        track.add(new MidiEvent(
                new ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0),        startTick + durationTicks));
    }
}
