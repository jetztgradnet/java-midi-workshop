package net.jetztgrad.midi.workshop;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MidiFilePlayer — load and play a standard MIDI file (.mid).
 *
 * Run with:  ./gradlew midiFilePlayer
 *            ./gradlew midiFilePlayer -PmidiFile=midi/bach_minuet_g.mid
 *            ./gradlew midiFilePlayer -Pdevice=XIAO
 *            ./gradlew midiFilePlayer -PmidiFile=midi/bach_minuet_g.mid -Pdevice=XIAO
 *
 * If no file is specified the player picks the first .mid file in the midi/ directory.
 */
public class MidiFilePlayer {

    public static void main(String[] args) throws Exception {
        File midiFile = resolveMidiFile(args);
        System.out.println("Loading: " + midiFile.getPath());

        // Load the MIDI file into a Sequence object
        Sequence sequence;
        try {
            sequence = MidiSystem.getSequence(midiFile);
        } catch (IOException | InvalidMidiDataException e) {
            System.err.println("Error loading MIDI file: " + e.getMessage());
            System.exit(1);
            return;
        }

        int trackCount = sequence.getTracks().length;
        double seconds  = sequence.getMicrosecondLength() / 1_000_000.0;
        System.out.printf("Tracks: %d  |  Duration: %.1f seconds%n", trackCount, seconds);

        // Open the sequencer and connect it to the output device
        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        sequencer.getTransmitter().setReceiver(DeviceUtil.openReceiver(args));
        sequencer.setSequence(sequence);

        System.out.println("Playing... (press Ctrl+C to stop early)");
        sequencer.start();

        // Poll until playback finishes, showing a simple progress indicator
        while (sequencer.isRunning()) {
            double pos = sequencer.getMicrosecondPosition() / 1_000_000.0;
            double len = sequencer.getMicrosecondLength()   / 1_000_000.0;
            System.out.printf("\r  %.1f / %.1f s  ", pos, len);
            System.out.flush();
            Thread.sleep(500);
        }

        System.out.println("\nPlayback complete.");
        sequencer.close();
    }

    /** Resolves which MIDI file to play. */
    private static File resolveMidiFile(String[] args) {
        // 1. First argument that is not a flag (--device=...) is the file path
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                return new File(arg);
            }
        }

        // 2. First .mid / .midi file found in the midi/ directory
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
                        .orElseGet(MidiFilePlayer::exitNoFile);
            } catch (IOException e) {
                return exitNoFile();
            }
        }

        return exitNoFile();
    }

    private static File exitNoFile() {
        System.err.println("No MIDI file found in the midi/ directory.");
        System.err.println("Usage: ./gradlew midiFilePlayer -PmidiFile=midi/filename.mid");
        System.exit(1);
        return null; // unreachable
    }
}
