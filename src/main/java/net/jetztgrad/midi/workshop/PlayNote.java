package net.jetztgrad.midi.workshop;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

/**
 * PlayNote — send a single NOTE_ON / NOTE_OFF via the built-in software synthesizer.
 *
 * Run with:  ./gradlew playNote
 *
 * This shows the raw MIDI message model:
 *   • NOTE_ON  : status byte 0x90 | channel, data1 = note number, data2 = velocity
 *   • NOTE_OFF : status byte 0x80 | channel, data1 = note number, data2 = 0
 *
 * Middle C = MIDI note 60.  Velocity 80 = medium-loud.
 */
public class PlayNote {

    public static void main(String[] args) throws Exception {
        System.out.println("Opening software synthesizer...");

        // Obtain and open the platform's default software synthesizer.
        // On macOS this is Apple's built-in DLS MusicDevice.
        // On Windows it is the default Microsoft GS Wavetable Synth.
        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();

        // A Receiver accepts MidiMessage objects.
        Receiver receiver = synth.getReceiver();

        // --- Build NOTE_ON message ---
        // channel 0 (MIDI channel 1), note 60 (middle C), velocity 80
        ShortMessage noteOn = new ShortMessage();
        noteOn.setMessage(ShortMessage.NOTE_ON, 0, 60, 80);

        System.out.println("NOTE_ON  — channel 1, note 60 (middle C), velocity 80");
        receiver.send(noteOn, -1);   // -1 = send immediately (no timestamp)

        Thread.sleep(1000);          // hold the note for 1 second

        // --- Build NOTE_OFF message ---
        ShortMessage noteOff = new ShortMessage();
        noteOff.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);

        System.out.println("NOTE_OFF — channel 1, note 60, velocity 0");
        receiver.send(noteOff, -1);

        Thread.sleep(300);           // give the synth time to release

        receiver.close();
        synth.close();
        System.out.println("Done.");
    }
}
