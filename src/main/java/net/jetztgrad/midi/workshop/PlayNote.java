package net.jetztgrad.midi.workshop;

import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 * PlayNote — send a single NOTE_ON / NOTE_OFF via a MIDI output device.
 *
 * Run with:  ./gradlew playNote
 *            ./gradlew playNote -Pdevice=XIAO
 *
 * This shows the raw MIDI message model:
 *   • NOTE_ON  : status byte 0x90 | channel, data1 = note number, data2 = velocity
 *   • NOTE_OFF : status byte 0x80 | channel, data1 = note number, data2 = 0
 *
 * Middle C = MIDI note 60.  Velocity 80 = medium-loud.
 */
public class PlayNote {

    public static void main(String[] args) throws Exception {
        // Open the requested output device (or the default synth if -Pdevice is absent).
        Receiver receiver = DeviceUtil.openReceiver(args);
        int patch = DeviceUtil.patchFilter(args);
        DeviceUtil.sendProgramChange(receiver, 0, patch);
        System.out.println("Instrument: " + DeviceUtil.GM_PATCH_NAMES[patch] + " (patch " + patch + ")");

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
        System.out.println("Done.");
    }
}
