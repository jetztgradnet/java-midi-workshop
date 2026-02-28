---
marp: true
theme: default
paginate: true
style: |
  section {
    font-family: 'Segoe UI', Arial, sans-serif;
  }
  section.title {
    background: #1a1a2e;
    color: #eee;
    text-align: center;
  }
  section.title h1 { color: #e94560; font-size: 2.2em; }
  section.title p  { color: #aaa; }
  code { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; }
  pre  { background: #1e1e1e; color: #d4d4d4; }
  .hands-on { background: #fff3cd; border-left: 5px solid #f0ad4e; padding: 10px; }
---

<!-- _class: title -->

# Java MIDI Workshop

## Playing Music with Java

**90 minutes · javax.sound.midi · Hands-on**

---

## Agenda

1. What is MIDI?
2. MIDI messages, channels & files
3. Java's MIDI API — key classes
4. Live demos: ListDevices · PlayNote · SimpleSequence
5. **Hands-on: compose your own melody**
6. MIDI files, drums, program change
7. Hardware: Seeed XIAO synth
8. Software synths: macOS & Windows
9. Beyond & Resources

---

## What is MIDI?

**Musical Instrument Digital Interface** — standard since 1983

- A **communication protocol**, not audio
- Sends *instructions*: "play note 60 at velocity 80"
- Works over USB, traditional 5-pin DIN, Bluetooth (BLE MIDI)

> Think of MIDI as sheet music your computer can read — the synthesizer is the orchestra.

---

## MIDI Messages

Every MIDI message is 1–3 bytes:

| Byte | Name | Meaning |
|------|------|---------|
| Status | `0x90` | NOTE_ON on channel 1 |
| Data 1 | `60` | Note number (middle C) |
| Data 2 | `80` | Velocity (loudness) |

**Status byte** = `command (upper nibble)` + `channel (lower nibble)`

`0x90` = NOTE_ON (9) on channel 1 (0)

---

## Note Numbers

```
Octave:   C    D    E    F    G    A    B
  C3 :   48   50   52   53   55   57   59
  C4 :   60   62   64   65   67   69   71   ← middle C = 60
  C5 :   72   74   76   77   79   81   83
```

- **+1** = one semitone up (e.g. C → C#)
- **+12** = one octave up
- Velocity: `1` = whisper · `64` = medium · `127` = maximum

---

## MIDI Channels

- **16 channels** per MIDI port (1–16, 0–15 in code)
- Each channel can have its own instrument (patch)
- **Channel 10 = drums** (General MIDI standard)

```java
// Channel 9 in Java = MIDI channel 10 = GM percussion
int DRUM_CHANNEL = 9;

// Note 36 = Bass Drum,  38 = Snare,  42 = Closed Hi-Hat
```

---

## Program Change: 128 Instruments

General MIDI defines **128 standard patches** (0–127):

| Range | Family | Examples |
|-------|--------|---------|
| 0–7 | Piano | Grand Piano, E.Piano |
| 8–15 | Chromatic Perc. | Vibraphone, Marimba |
| 40–47 | Strings | Violin, Cello |
| 56–63 | Brass | Trumpet, Trombone |
| 72–79 | Pipe | Flute, Piccolo |
| 80–87 | Synth Lead | Square, Sawtooth |

---

## MIDI Devices

| Type | Examples |
|------|---------|
| **Hardware synth** | Seeed XIAO MIDI-Synth, Yamaha, Roland |
| **Controller** | MIDI keyboard, drum pads |
| **Software synth** | macOS DLS, Windows GS Wavetable |
| **Virtual port** | macOS IAC Driver, Windows loopMIDI |

A device is a **Receiver** (accepts MIDI input) or a **Transmitter** (sends MIDI).

---

## MIDI Files

- Binary format, extension `.mid`
- Contains one or more **tracks** of time-stamped MIDI events
- **SMF Type 0** — all events on one track
- **SMF Type 1** — multiple tracks (most common)

Java can read and write MIDI files with `MidiSystem.getSequence(file)`

---

## Java's `javax.sound.midi`

Built into the JDK — no external libraries needed!

| Class | Purpose |
|-------|---------|
| `MidiSystem` | Factory — get devices, load files |
| `Synthesizer` | Software synth built into Java |
| `Sequencer` | "Tape deck" — plays/records Sequences |
| `MidiDevice` | Any MIDI device (hardware or software) |
| `Sequence` | Container for tracks + timing info |
| `Track` | Ordered list of MidiEvents |
| `MidiEvent` | A MidiMessage + tick timestamp |
| `ShortMessage` | NOTE_ON/OFF, PROGRAM_CHANGE, CC, … |

---

## Getting Started

```bash
# Clone the repository
git clone https://github.com/jetztgradnet/java-midi-workshop.git
cd java-midi-workshop

# List available MIDI devices (verify your setup works)
./gradlew listDevices
```

You should see at least one device with **Receiver: yes**.

> macOS: enable the IAC Driver in Audio MIDI Setup
> Windows: install VirtualMIDISynth (see `cheatsheets/os-setup.md`)

---

## Demo: ListDevices

```java
MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

for (MidiDevice.Info info : infos) {
    MidiDevice device = MidiSystem.getMidiDevice(info);
    System.out.println("Device: " + info.getName());
    System.out.println("  Receiver    : " + (device.getMaxReceivers()    != 0));
    System.out.println("  Transmitter : " + (device.getMaxTransmitters() != 0));
}
```

```bash
./gradlew listDevices
```

---

## Demo: PlayNote

```java
Synthesizer synth = MidiSystem.getSynthesizer();
synth.open();
Receiver receiver = synth.getReceiver();

// NOTE_ON: channel 0, middle C (60), velocity 80
ShortMessage on = new ShortMessage();
on.setMessage(ShortMessage.NOTE_ON, 0, 60, 80);
receiver.send(on, -1);   // -1 = send immediately

Thread.sleep(1000);

ShortMessage off = new ShortMessage();
off.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);
receiver.send(off, -1);
```

```bash
./gradlew playNote
```

---

## Sequences: Ticks and Tempo

```
Sequence(Sequence.PPQ, 4)
                         └─ resolution: 4 ticks per beat
```

| Concept | Meaning |
|---------|---------|
| **PPQ** | Pulses Per Quarter-note (ticks per beat) |
| **Tick** | Smallest time unit in a sequence |
| **BPM** | Beats per minute — 120 BPM → 1 beat = 500 ms |

**Example (4 ticks/beat, 120 BPM):**
- Beat 1 = tick 0
- Beat 2 = tick 4 (= 500 ms)
- Beat 3 = tick 8 (= 1000 ms)

---

## Demo: SimpleSequence

```java
Sequence sequence = new Sequence(Sequence.PPQ, 4);
Track    track    = sequence.createTrack();

// addNote(track, channel, note, velocity, startTick, durationTicks)
addNote(track, 0, 60, 80,  0, 4);  // C4 beat 1
addNote(track, 0, 64, 80,  4, 4);  // E4 beat 2
addNote(track, 0, 67, 80,  8, 4);  // G4 beat 3
addNote(track, 0, 72, 80, 12, 4);  // C5 beat 4

Sequencer sequencer = MidiSystem.getSequencer();
sequencer.open();
sequencer.setSequence(sequence);
sequencer.setTempoInBPM(120);
sequencer.start();
```

```bash
./gradlew simpleSequence
```

---

<!-- _class: -->
<!-- backgroundColor: #fff8e1 -->

## Hands-On: MyMelody

**Your turn! Open `src/main/java/.../exercise/MyMelody.java`**

Three TODOs:

1. **Add notes** — use `addNote(track, 0, noteNumber, velocity, startTick, durationTicks)`
2. **Change the tempo** — set `int bpm = ...`
3. **(Bonus)** Change the instrument with `PROGRAM_CHANGE`

Run your melody:
```bash
./gradlew myMelody
```

> Quick note reference: C4=60, D4=62, E4=64, F4=65, G4=67, A4=69, B4=71, C5=72

---

## MIDI Files Demo

```java
// Load a MIDI file
Sequence sequence = MidiSystem.getSequence(new File("midi/bach_minuet.mid"));

// Play it
Sequencer sequencer = MidiSystem.getSequencer();
sequencer.open();
sequencer.setSequence(sequence);
sequencer.start();

while (sequencer.isRunning()) {
    Thread.sleep(500);
}
```

```bash
./gradlew midiFilePlayer
./gradlew midiFilePlayer -PmidiFile=midi/another.mid
```

---

## Drums: Channel 10

```java
int DRUM_CHANNEL = 9;   // Java index 9 = MIDI channel 10

// Kick on beat 1, Snare on beat 2
addNote(track, DRUM_CHANNEL, 36, 100,  0, 2);  // kick
addNote(track, DRUM_CHANNEL, 38,  90,  4, 2);  // snare
addNote(track, DRUM_CHANNEL, 42,  70,  0, 1);  // hi-hat (every 8th)
```

| Note | Sound | Note | Sound |
|------|-------|------|-------|
| 36 | Bass Drum | 42 | Closed Hi-Hat |
| 38 | Snare | 46 | Open Hi-Hat |
| 49 | Crash | 51 | Ride Cymbal |

```bash
./gradlew drumPattern
```

---

## Program Change Demo

```java
// Switch to patch 56 = Trumpet on channel 0
ShortMessage pc = new ShortMessage();
pc.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 56, 0);
receiver.send(pc, -1);

// Now play notes — they will sound like a trumpet
```

```bash
./gradlew programChange
```

Hear the same C–E–G phrase as Piano (0), Violin (40), Trumpet (56), Flute (73)

---

## Hardware: Seeed XIAO Synth

1. Connect via **USB cable** (USB-C)
2. The device appears automatically as a MIDI device
3. Run `./gradlew listDevices` — look for the XIAO in the list
4. In `PlayNote.java`, route to the XIAO's Receiver instead of the default synth

```java
// Find a specific device by name
for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
    if (info.getName().contains("XIAO")) {
        MidiDevice device = MidiSystem.getMidiDevice(info);
        device.open();
        Receiver receiver = device.getReceiver();
        // ... send MIDI messages ...
    }
}
```

---

## Software Synths: macOS

**Built-in (DLS MusicDevice) — available automatically**

To expose it as a virtual MIDI port for other apps:
1. Open **Audio MIDI Setup** (`⌘Space` → "Audio MIDI Setup")
2. `Window` → `Show MIDI Studio`
3. Double-click **IAC Driver** → tick **"Device is online"**
4. Run `./gradlew listDevices` → IAC Bus appears

**GarageBand** can also act as a software synth via IAC.

---

## Software Synths: Windows

**Windows 11** — Windows MIDI Services included by default

**Windows 10** — install a soundfont synth:
1. Download [VirtualMIDISynth](https://coolsoft.altervista.org/en/virtualmidisynth)
2. Download a soundfont, e.g. **GeneralUser GS** (free)
3. Load the soundfont in VirtualMIDISynth
4. Run `./gradlew listDevices` — synth appears as a MIDI device

---

## Live Terminal Keyboard

Play notes by typing — no extra hardware needed!

```bash
./gradlew liveTerminalKeyboard            # default synth
./gradlew liveTerminalKeyboard -Pdevice=XIAO
```

```
  Q  W     R  T  Y     I  O    ← sharps / flats
  A  S  D  F  G  H  J  K  L  ;  ← white keys
  C  D  E  F  G  A  B  C  D  E   (current octave)

  +/=  octave up   |   -  octave down   |   ESC  quit
```

> Each key press fires `NOTE_ON`; after a fixed duration a virtual thread
> sends `NOTE_OFF` — so chords and fast runs both work.

---

## Live Window Keyboard

A Swing window gives true polyphony — hold multiple keys at once.

```bash
./gradlew liveWindowKeyboard              # default synth
./gradlew liveWindowKeyboard -Pdevice=XIAO
```

- **Device drop-down** — switch MIDI output without restarting
- **SPACE** — hold for sustain pedal (CC 64)
- **ESC** — All Notes Off (panic)
- Key-press stores note number → correct `NOTE_OFF` even after octave change
- Window losing focus triggers auto-panic (no stuck notes)

> Uses `KeyboardFocusManager.addKeyEventDispatcher` so key events arrive
> regardless of which Swing component has focus.

---

## Beyond This Workshop

| Topic | Where to look |
|-------|--------------|
| **BLE MIDI** | JSyn library, or OS Bluetooth MIDI setup |
| **JFugue** | High-level MIDI composition for Java |
| **Alda** | Music programming language (JVM) |
| **JACK** | Low-latency audio/MIDI routing (Linux/macOS) |
| **OGG/WAV playback** | `javax.sound.sampled` |
| **Real-time input** | `Transmitter` + `Receiver` chain |

> BLE MIDI: pair via OS Bluetooth settings — Java sees it as a regular MIDI device!

---

## Resources

- **JavaDoc** — [`javax.sound.midi`](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/sound/midi/package-summary.html)
- **General MIDI spec** — midi.org/specifications
- **MIDI note chart** — `cheatsheets/midi-commands.md`
- **OS setup guide** — `cheatsheets/os-setup.md`
- **Java API cheat sheet** — `cheatsheets/java-midi-api.md`
- **Mutopia Project** — free public-domain sheet music + MIDI

---

<!-- _class: title -->

# Thank You!

## Questions?

```bash
./gradlew listDevices   # start here
./gradlew myMelody      # play your creation
```

Slides & code: **github.com/jetztgradnet/java-midi-workshop**

---

*End of slides*
