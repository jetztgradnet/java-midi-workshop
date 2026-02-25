package net.jetztgrad.midi.workshop;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

/**
 * ProgramChange — play the same short phrase with different GM instrument patches.
 *
 * Run with:  ./gradlew programChange
 *
 * General MIDI defines 128 instrument patches (0–127).
 * A PROGRAM_CHANGE message selects the current patch on a channel.
 *
 * Patch families (every 8 patches):
 *   0–7    Pianos          64–71  Reed instruments
 *   8–15   Chromatic perc  72–79  Pipe (flutes, recorders)
 *   16–23  Organs          80–87  Synth lead
 *   24–31  Guitars         88–95  Synth pad
 *   32–39  Bass            96–103 Synth effects
 *   40–47  Strings         104–111 Ethnic
 *   48–55  Ensemble        112–119 Percussive
 *   56–63  Brass           120–127 Sound effects
 *
 * This demo plays:  C4 – E4 – G4   (a C major triad broken arpeggio)
 */
public class ProgramChange {

    // GM patches to demonstrate
    static final int[]    PATCH_NUMBERS = {  0,        40,       56,         73    };
    static final String[] PATCH_NAMES   = {
            "Acoustic Grand Piano",
            "Violin",
            "Trumpet",
            "Flute"
    };

    // Short phrase: C4 – E4 – G4
    static final int[] NOTES    = { 60, 64, 67 };
    static final int   VELOCITY = 80;

    public static void main(String[] args) throws Exception {
        System.out.println("Opening software synthesizer...");

        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();
        Receiver receiver = synth.getReceiver();

        for (int i = 0; i < PATCH_NUMBERS.length; i++) {
            int    patch = PATCH_NUMBERS[i];
            String name  = PATCH_NAMES[i];

            // Send PROGRAM_CHANGE on channel 0
            // ShortMessage.PROGRAM_CHANGE = 0xC0; data1 = patch number; data2 ignored
            ShortMessage pc = new ShortMessage();
            pc.setMessage(ShortMessage.PROGRAM_CHANGE, 0, patch, 0);
            receiver.send(pc, -1);

            System.out.printf("%nPatch %3d — %s%n", patch, name);

            // Play the phrase
            for (int note : NOTES) {
                ShortMessage on = new ShortMessage();
                on.setMessage(ShortMessage.NOTE_ON, 0, note, VELOCITY);
                receiver.send(on, -1);

                Thread.sleep(400);  // hold note

                ShortMessage off = new ShortMessage();
                off.setMessage(ShortMessage.NOTE_OFF, 0, note, 0);
                receiver.send(off, -1);

                Thread.sleep(80);   // short gap between notes
            }

            Thread.sleep(600);      // pause before next patch
        }

        receiver.close();
        synth.close();
        System.out.println("\nDone.");
    }
}
