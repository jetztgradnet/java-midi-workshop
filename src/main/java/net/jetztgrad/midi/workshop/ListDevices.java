package net.jetztgrad.midi.workshop;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;

/**
 * ListDevices — enumerate all MIDI devices available on this machine.
 *
 * Run with:  ./gradlew listDevices
 *
 * A "Receiver" device can accept incoming MIDI data (e.g. a software synth, a hardware synth).
 * A "Transmitter" device sends MIDI data (e.g. a MIDI keyboard, controller).
 */
public class ListDevices {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Available MIDI Devices ===\n");

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        if (infos.length == 0) {
            System.out.println("No MIDI devices found.");
            System.out.println("  macOS  : Open 'Audio MIDI Setup' > Window > Show MIDI Studio,");
            System.out.println("           then enable the IAC Driver.");
            System.out.println("  Windows: Install VirtualMIDISynth (see cheatsheets/os-setup.md).");
            return;
        }

        for (MidiDevice.Info info : infos) {
            MidiDevice device = MidiSystem.getMidiDevice(info);

            System.out.println("Device : " + info.getName());
            System.out.println("  Vendor      : " + info.getVendor());
            System.out.println("  Description : " + info.getDescription());
            System.out.println("  Version     : " + info.getVersion());

            // getMaxReceivers() != 0  →  device can accept MIDI (output / synth side)
            // getMaxTransmitters() != 0 → device can send MIDI (input / controller side)
            // -1 means "unlimited"
            System.out.println("  Receiver    : " + (device.getMaxReceivers()    != 0
                    ? "yes (MIDI output — can play notes)"
                    : "no"));
            System.out.println("  Transmitter : " + (device.getMaxTransmitters() != 0
                    ? "yes (MIDI input  — sends note data)"
                    : "no"));
            System.out.println();
        }
    }
}
