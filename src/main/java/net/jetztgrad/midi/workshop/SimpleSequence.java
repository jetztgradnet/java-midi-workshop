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
 * SimpleSequence — build and play a short melody programmatically.
 *
 * Run with:  ./gradlew simpleSequence
 *            ./gradlew simpleSequence -Pdevice=XIAO
 *
 * Concepts introduced:
 *   Sequence  — a container for one or more tracks and timing information
 *   Track     — a stream of MidiEvents (notes, control changes, …)
 *   MidiEvent — a MidiMessage plus a tick timestamp
 *   Sequencer — a MIDI "tape deck" that plays back a Sequence
 *
 * Timing:
 *   PPQ = Pulses Per Quarter-note (i.e. ticks per beat).
 *   Here we use 4 ticks/beat for simplicity: beat 1 = tick 0, beat 2 = tick 4, …
 *   Tempo (120 BPM) converts ticks to real time: 1 beat = 500 ms.
 *
 * The melody: C4 – E4 – G4 – C5  (a C major arpeggio)
 *   MIDI note numbers: C4=60, E4=64, G4=67, C5=72
 */
public class SimpleSequence {

    public static void main(String[] args) throws Exception {

        // ── 1. Create a Sequence ──────────────────────────────────────────
        // Sequence.PPQ = Pulse Per Quarter-note timing mode
        // 4 = resolution: 4 ticks per quarter-note (beat)
        Sequence sequence = new Sequence(Sequence.PPQ, 4);

        // ── 2. Create a Track ─────────────────────────────────────────────
        Track track = sequence.createTrack();

        // ── 3. Add notes (channel 0 = MIDI channel 1) ────────────────────
        //        note,  vel, startTick, durationTicks
        addNote(track, 0, 60, 80,  0, 4);   // C4 — beat 1
        addNote(track, 0, 64, 80,  4, 4);   // E4 — beat 2
        addNote(track, 0, 67, 80,  8, 4);   // G4 — beat 3
        addNote(track, 0, 72, 80, 12, 4);   // C5 — beat 4

        // ── 4. End-of-Track meta message (required) ───────────────────────
        MetaMessage endOfTrack = new MetaMessage();
        endOfTrack.setMessage(0x2F, new byte[0], 0);   // type 0x2F = End of Track
        track.add(new MidiEvent(endOfTrack, 16));        // after the last note

        // ── 5. Open a Sequencer and connect it to the output device ──────
        // getSequencer(false) = don't auto-connect; we set the receiver explicitly.
        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        var receiver = DeviceUtil.openReceiver(args);
        int patch = DeviceUtil.patchFilter(args);
        DeviceUtil.sendProgramChange(receiver, 0, patch);
        System.out.println("Instrument: " + DeviceUtil.GM_PATCH_NAMES[patch] + " (patch " + patch + ")");
        sequencer.getTransmitter().setReceiver(receiver);

        // ── 6. Load the sequence and set the tempo ────────────────────────
        sequencer.setSequence(sequence);
        sequencer.setTempoInBPM(120);          // 120 BPM = 500 ms per beat

        // ── 7. Play ───────────────────────────────────────────────────────
        System.out.println("Playing C major arpeggio: C4 – E4 – G4 – C5");
        System.out.println("Tempo: 120 BPM | Resolution: 4 ticks/beat");
        sequencer.start();

        while (sequencer.isRunning()) {
            Thread.sleep(50);
        }

        Thread.sleep(500);   // let the synth finish the last note's release
        sequencer.stop();
        sequencer.close();
        System.out.println("Done.");
    }

    /**
     * Adds a NOTE_ON / NOTE_OFF pair to a track.
     *
     * @param track         the track to add to
     * @param channel       MIDI channel (0–15; channel 9 = drums)
     * @param note          MIDI note number (0–127; 60 = middle C)
     * @param velocity      note velocity / loudness (1–127)
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
}
