package net.jetztgrad.midi.workshop.exercise;

import net.jetztgrad.midi.workshop.DeviceUtil;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * MyMelody — hands-on exercise.
 *
 * Run with:  ./gradlew myMelody
 *            ./gradlew myMelody -Pdevice=XIAO
 *
 * ─────────────────────────────────────────────────────────────────────────
 * YOUR TASK: Fill in the three TODOs below to compose your own melody.
 * The skeleton compiles and runs even with the TODOs unfilled (silent play).
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Quick reference — MIDI note numbers:
 *
 *   C4 =60   D4 =62   E4 =64   F4 =65   G4 =67   A4 =69   B4 =71   C5 =72
 *   C#4=61   D#4=63            F#4=66            G#4=68   A#4=70
 *
 *   One octave up = +12   |   One octave down = -12
 *
 * Velocity: 1 = whisper, 64 = medium, 100 = forte, 127 = maximum
 *
 * Ticks: resolution is 4 ticks per beat (quarter-note).
 *   beat 1 = tick  0,  beat 2 = tick  4,  beat 3 = tick  8,  beat 4 = tick 12
 *   beat 5 = tick 16,  beat 6 = tick 20,  …
 *
 * Use addNote(track, channel, noteNumber, velocity, startTick, durationTicks)
 */
public class MyMelody {

    public static void main(String[] args) throws Exception {
        // ── Setup (no need to change this) ───────────────────────────────
        Sequence sequence = new Sequence(Sequence.PPQ, 4);
        Track track = sequence.createTrack();

        // ════════════════════════════════════════════════════════════════
        // TODO 1: Add your notes to compose a melody.
        //
        // Uncomment and modify these examples, or write your own:
        //
        // addNote(track, 0, 60, 80,  0, 4);   // C4, beat 1, 1 beat long
        // addNote(track, 0, 62, 80,  4, 4);   // D4, beat 2
        // addNote(track, 0, 64, 80,  8, 4);   // E4, beat 3
        // addNote(track, 0, 65, 80, 12, 4);   // F4, beat 4
        // addNote(track, 0, 67, 80, 16, 8);   // G4, beats 5–6, held 2 beats
        // ════════════════════════════════════════════════════════════════

        // ════════════════════════════════════════════════════════════════
        // TODO 2: Set the tempo.
        //   Slow ballad: 60 BPM  |  Walk: 90 BPM  |  Default: 120 BPM  |  Fast: 160 BPM
        // ════════════════════════════════════════════════════════════════
        int bpm = 120; // ← change me!

        // ════════════════════════════════════════════════════════════════
        // TODO 3 (Bonus): Change the instrument.
        //   Uncomment and set the patch number (0–127):
        //
        // ShortMessage pc = new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 0, 0);
        //   Patch 0  = Acoustic Grand Piano   ← 3rd argument
        //   Patch 33 = Electric Bass
        //   Patch 40 = Violin
        //   Patch 56 = Trumpet
        //   Patch 73 = Flute
        //   Patch 80 = Square Synth Lead
        //
        // track.add(new MidiEvent(pc, 0));   // add at tick 0 (before first note)
        // ════════════════════════════════════════════════════════════════

        // ── End-of-track marker ──────────────────────────────────────────
        // Adjust tick 24 if your melody is longer than 6 beats
        MetaMessage eot = new MetaMessage();
        eot.setMessage(0x2F, new byte[0], 0);
        track.add(new MidiEvent(eot, 32));

        // ── Playback (no need to change this) ────────────────────────────
        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        var receiver = DeviceUtil.openReceiver(args);
        int patch = DeviceUtil.patchFilter(args);
        DeviceUtil.sendProgramChange(receiver, 0, patch);
        System.out.println("Instrument: " + DeviceUtil.GM_PATCH_NAMES[patch] + " (patch " + patch + ")");
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.setSequence(sequence);
        sequencer.setTempoInBPM(bpm);

        System.out.println("Playing your melody at " + bpm + " BPM ...");
        sequencer.start();

        while (sequencer.isRunning()) {
            Thread.sleep(50);
        }

        Thread.sleep(500);
        sequencer.stop();
        sequencer.close();
        System.out.println("Done! Great melody :)");
    }

    /**
     * Adds a NOTE_ON / NOTE_OFF pair to the track.
     *
     * @param track         track to add to
     * @param channel       MIDI channel 0–15 (9 = drums)
     * @param note          note number 0–127 (60 = middle C)
     * @param velocity      loudness 1–127
     * @param startTick     tick position of note start
     * @param durationTicks note length in ticks (4 ticks = 1 beat)
     */
    static void addNote(Track track, int channel, int note, int velocity,
                        long startTick, long durationTicks) throws InvalidMidiDataException {
        track.add(new MidiEvent(
                new ShortMessage(ShortMessage.NOTE_ON,  channel, note, velocity), startTick));
        track.add(new MidiEvent(
                new ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0),        startTick + durationTicks));
    }
}
