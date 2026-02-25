# Java MIDI API Cheat Sheet

Package: `javax.sound.midi`
Available in every standard JDK — no external dependencies.

---

## Class Overview

| Class / Interface | Purpose | Key Methods |
|------------------|---------|-------------|
| `MidiSystem` | Static factory | `getMidiDeviceInfo()`, `getMidiDevice()`, `getSynthesizer()`, `getSequencer()`, `getSequence(File)` |
| `MidiDevice` | Any MIDI device | `open()`, `close()`, `getReceiver()`, `getTransmitter()`, `getMaxReceivers()` |
| `Synthesizer` | Software synth | `open()`, `close()`, `getReceiver()`, `loadInstruments()` |
| `Sequencer` | Playback engine | `open()`, `setSequence()`, `start()`, `stop()`, `isRunning()`, `setTempoInBPM()`, `getMicrosecondPosition()` |
| `Sequence` | Container | `createTrack()`, `getTracks()`, `getMicrosecondLength()`, `getTickLength()` |
| `Track` | Event list | `add(MidiEvent)`, `remove(MidiEvent)`, `size()` |
| `MidiEvent` | Message + tick | `getMessage()`, `getTick()` |
| `ShortMessage` | Note/CC/PC/etc. | `setMessage(command, channel, data1, data2)`, `getCommand()`, `getChannel()`, `getData1()`, `getData2()` |
| `MetaMessage` | Tempo/EOT/etc. | `setMessage(type, data, length)`, `getType()` |
| `SysexMessage` | SysEx | `setMessage(data, length)` |
| `MidiDevice.Info` | Device metadata | `getName()`, `getVendor()`, `getDescription()` |
| `Receiver` | Accepts messages | `send(MidiMessage, timestamp)`, `close()` |
| `Transmitter` | Sends messages | `setReceiver(Receiver)`, `close()` |

---

## Pattern: Open Synth and Play a Note

```java
import javax.sound.midi.*;

// 1. Get and open the default software synthesizer
Synthesizer synth = MidiSystem.getSynthesizer();
synth.open();

// 2. Get a Receiver to send messages to it
Receiver receiver = synth.getReceiver();

// 3. Build and send NOTE_ON
ShortMessage on = new ShortMessage();
on.setMessage(ShortMessage.NOTE_ON, 0, 60, 80);   // ch0, C4, vel 80
receiver.send(on, -1);   // -1 = immediate

Thread.sleep(1000);

// 4. Send NOTE_OFF
ShortMessage off = new ShortMessage();
off.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);
receiver.send(off, -1);

// 5. Clean up
receiver.close();
synth.close();
```

---

## Pattern: Create and Play a Programmatic Sequence

```java
import javax.sound.midi.*;

// 1. Create sequence: PPQ mode, 4 ticks per beat
Sequence sequence = new Sequence(Sequence.PPQ, 4);
Track track = sequence.createTrack();

// 2. Add notes (helper method below)
addNote(track, 0, 60, 80,  0, 4);  // C4, beat 1, duration 1 beat
addNote(track, 0, 64, 80,  4, 4);  // E4, beat 2
addNote(track, 0, 67, 80,  8, 4);  // G4, beat 3

// 3. End-of-track meta message (required)
MetaMessage eot = new MetaMessage();
eot.setMessage(0x2F, new byte[0], 0);
track.add(new MidiEvent(eot, 12));  // tick after last note

// 4. Set up sequencer
Sequencer sequencer = MidiSystem.getSequencer();
sequencer.open();
sequencer.setSequence(sequence);
sequencer.setTempoInBPM(120);

// 5. Play and wait
sequencer.start();
while (sequencer.isRunning()) Thread.sleep(50);
sequencer.close();

// Helper
static void addNote(Track track, int ch, int note, int vel,
                    long startTick, long durationTicks) throws InvalidMidiDataException {
    track.add(new MidiEvent(
        new ShortMessage(ShortMessage.NOTE_ON,  ch, note, vel), startTick));
    track.add(new MidiEvent(
        new ShortMessage(ShortMessage.NOTE_OFF, ch, note, 0),   startTick + durationTicks));
}
```

---

## Pattern: Load and Play a MIDI File

```java
import javax.sound.midi.*;
import java.io.File;

// 1. Load
Sequence sequence = MidiSystem.getSequence(new File("midi/song.mid"));

// 2. Play
Sequencer sequencer = MidiSystem.getSequencer();
sequencer.open();
sequencer.setSequence(sequence);
sequencer.start();

// 3. Wait until done
while (sequencer.isRunning()) Thread.sleep(100);

sequencer.close();
```

---

## Pattern: Send Program Change

```java
// Select patch 56 (Trumpet) on channel 0
ShortMessage pc = new ShortMessage();
pc.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 56, 0);
//                                         ^ch ^patch
receiver.send(pc, -1);
```

---

## Pattern: Enumerate MIDI Devices

```java
MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
for (MidiDevice.Info info : infos) {
    MidiDevice device = MidiSystem.getMidiDevice(info);
    System.out.println(info.getName()
        + " | receivers=" + device.getMaxReceivers()
        + " | transmitters=" + device.getMaxTransmitters());
}
```

---

## Pattern: Route to a Specific Device

```java
for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
    if (info.getName().contains("XIAO")) {          // match by name
        MidiDevice device = MidiSystem.getMidiDevice(info);
        if (device.getMaxReceivers() != 0) {        // is it an output?
            device.open();
            Receiver receiver = device.getReceiver();
            // send messages …
            receiver.close();
            device.close();
        }
    }
}
```

---

## Tick Math

```
ticksPerBeat = resolution  (e.g. 4)
beatsPerMin  = BPM         (e.g. 120)

msPerBeat    = 60_000 / BPM                    → 500 ms/beat at 120 BPM
msPerTick    = msPerBeat / ticksPerBeat        → 125 ms/tick
ticksPerSec  = 1000 / msPerTick               → 8 ticks/sec

ticks = beats × ticksPerBeat
beats = ticks / ticksPerBeat
```

**Example** — place a note at 1.5 seconds (120 BPM, 4 ticks/beat):
```
beats    = 1.5 s × (120 / 60) = 3 beats
ticks    = 3 × 4 = 12
```

---

## ShortMessage Command Constants

| Constant | Hex | Description |
|----------|-----|-------------|
| `ShortMessage.NOTE_OFF` | `0x80` | Stop note |
| `ShortMessage.NOTE_ON` | `0x90` | Start note (vel 0 = off) |
| `ShortMessage.POLY_PRESSURE` | `0xA0` | Per-note aftertouch |
| `ShortMessage.CONTROL_CHANGE` | `0xB0` | Control change |
| `ShortMessage.PROGRAM_CHANGE` | `0xC0` | Instrument select |
| `ShortMessage.CHANNEL_PRESSURE` | `0xD0` | Channel aftertouch |
| `ShortMessage.PITCH_BEND` | `0xE0` | Pitch wheel |

---

## MetaMessage Types

| Type | Hex | Data |
|------|-----|------|
| Sequence Number | `0x00` | 2 bytes |
| Text Event | `0x01` | ASCII string |
| Track Name | `0x03` | ASCII string |
| Tempo | `0x51` | 3 bytes: µs/beat (big-endian) |
| Time Signature | `0x58` | 4 bytes |
| Key Signature | `0x59` | 2 bytes |
| **End of Track** | **`0x2F`** | **0 bytes — required!** |

---

## Common Exceptions

| Exception | Cause |
|-----------|-------|
| `MidiUnavailableException` | No MIDI subsystem / device busy |
| `InvalidMidiDataException` | Bad message bytes / out-of-range values |
| `IOException` | MIDI file not found / unreadable |
