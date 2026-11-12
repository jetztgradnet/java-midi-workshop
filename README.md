# Java MIDI Workshop

A hands-on 90-minute workshop that teaches Java developers how to make music with MIDI using the `javax.sound.midi` API built into the JDK.

Presented at Java conferences and available for self-study.

---

## What You Will Learn

- What MIDI is and how it works (messages, channels, notes, velocity)
- How Java's `javax.sound.midi` package models MIDI devices and playback
- How to play notes programmatically via the software synthesizer
- How to build a musical sequence with ticks, tempo, and tracks
- How to load and play standard MIDI files (`.mid`)
- How to program Channel 10 drum patterns
- How to switch instrument patches with Program Change
- How to connect and use a USB MIDI synthesizer (Seeed XIAO)
- How to play notes live using the computer keyboard (terminal and Swing)
- How to monitor and inspect incoming MIDI messages in real time

---

## Prerequisites

| Requirement | Version / Notes |
|-------------|----------------|
| JDK | 25 or higher |
| Git | any recent version |
| IDE / Editor | IntelliJ IDEA, VS Code, Eclipse, or any editor |
| OS | macOS 12+ or Windows 10/11 |

No external Java libraries required — only `javax.sound.midi` (bundled in the JDK).
(The optional [Ableton Push 2 apps](#ableton-push-2-optional) in `src/push2/` are a separate source set with their own libraries, fetched by Gradle only when you build or run them.)

---

## Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/jetztgradnet/java-midi-workshop.git
cd java-midi-workshop

# 2. Verify your MIDI setup (should list at least one device)
./gradlew listDevices

# 3. Play your first note
./gradlew playNote
```

> **macOS**: if `listDevices` shows nothing, enable the IAC Driver in Audio MIDI Setup — see `cheatsheets/os-setup.md`.
>
> **Windows**: install VirtualMIDISynth if needed — see `cheatsheets/os-setup.md`.

---

## Running the Examples

| Command | Description |
|---------|-------------|
| `./listDevices.sh` | Enumerate all available MIDI devices |
| `./playNote.sh` | Play a single note via the software synthesizer |
| `./simpleSequence.sh` | Play a programmatic C–E–G–C sequence |
| `./midiFilePlayer.sh` | Play the first MIDI file found in `midi/` |
| `./drumPattern.sh` | Play a 4/4 kick + snare + hi-hat pattern |
| `./programChange.sh` | Hear the same phrase in 4 different instrument patches |
| `./myMelody.sh` | Run the hands-on exercise (edit `exercise/MyMelody.java` first!) |
| `./liveKeyboard.sh` | Play notes live by typing in the terminal |
| `./liveWindow.sh` | Play notes live in a Swing window (true polyphony) |
| `./midiMonitor.sh` | Monitor incoming MIDI messages from an external device |
| `./midiPanic.sh` | Send All Notes Off on all channels (silence stuck notes) |
| `./pianoRoll.sh` | Play a piano roll text notation (built-in demo) |
| `./toRoll.sh` | Convert a MIDI file to piano roll text notation |
| `./push2Hello.sh` | *Ableton Push 2:* hello world — pads play notes, the display mirrors the grid |
| `./notesweeper.sh` | *Ableton Push 2:* Notesweeper — Minesweeper where you find a band instead of mines |
| `./push2Jam.sh` | *Ableton Push 2:* jam over the Notesweeper band, one pad column per instrument |

> All examples also run via `./gradlew <taskName>` — see `./gradlew tasks --group=workshop`
> (Push 2 apps: `./gradlew tasks --group=push2`).
> **Windows:** use the equivalent `.bat` files (e.g. `listDevices.bat`) instead of the `.sh` scripts.

### Common Parameters

Most scripts accept these optional flags:

| Flag | Applies to | Description |
|------|-----------|-------------|
| `--device=<name>` | all | Select MIDI device by partial name (case-insensitive). Default: built-in software synth. |
| `--patch=<n\|name>` | playback scripts | GM instrument — number (0–127) or partial name, e.g. `--patch=violin`. Default: `0` (Grand Piano). |
| `--spy` | playback scripts | Print every outgoing MIDI message to the console with timestamps. |
| `--channel=<1-16\|all>` | `midiMonitor` | Filter display to one MIDI channel. Default: all. |
| `--file=<path>` | `pianoRoll`, `toRoll` | Piano roll file to load / MIDI file to convert. |

---

## Ableton Push 2 (optional)

An [Ableton Push 2](https://www.ableton.com/push/) controller can be driven directly from Java:
pads, buttons and encoders are plain MIDI, and the 960×160 colour display is reached over USB
(protocol: [Ableton/push-interface](https://github.com/Ableton/push-interface)).

**You need:** a Push 2 connected via USB *before* starting the app, and **Ableton Live quit**
(it claims the Push's MIDI ports and display). Sound comes from the built-in Java synthesizer
or any MIDI device via `--device=<name>`.

| App | What it does |
|-----|--------------|
| `./push2Hello.sh` | Lights the pads, plays a note per pad, shows "Hello, Push 2!" on the display |
| `./notesweeper.sh` | **Notesweeper** — 10 band members hide on the 8×8 pads. *Tap* a pad to reveal it: its note tells you how many members are next to it. *Hold* a pad (or Shift + tap) to invite a member — if you're right, their part joins the looping groove; reveal one by accident and it goes *boom*. 3 lives; New starts a new round. Win, and the board becomes a jam session. |
| `./push2Jam.sh` | **Jam session** right away: the groove plays, and each pad column is one instrument (bottom to top: A minor scale, or 8 drum sounds). The encoder above a column sets how loud you play it, the master encoder the band; the button below a column switches its groove part on/off, Play starts/stops the band. |

Extra flags for the Push 2 apps (in addition to `--device`, `--patch` and `--spy`):

| Flag | Applies to | Description |
|------|-----------|-------------|
| `--latency=<ms>` | all three | Audio buffer of the built-in synth. Default: 20 ms (Java's own default, 120 ms, feels sluggish). Raise it if you hear crackles. |
| `--display=wayang\|ffm` | all three | How the display is driven: `wayang` (default, Wayang library) or `ffm` (plain Java calling libusb via the Foreign Function & Memory API; needs `brew install libusb` on macOS). |
| `--push=<name>` | all three | Push MIDI port by partial name. Default: `Live Port`. |
| `--boom=<file.mid>` | `notesweeper` | MIDI file to play when a band member blows up. |
| `--band=<names>` | `push2Jam` | Up to 8 instruments, left to right, e.g. `--band=Kick,Bass,Keys,Lead`. Choose from Kick, Snare, Hi-hat, Clap, Bass, Keys, Pad, Arp, Lead, Brass. |

The Push 2 code lives in its own Gradle source set (`src/push2/`), so the workshop examples
stay dependency-free. Its libraries — [Wayang](https://github.com/Deep-Symmetry/wayang) and
[usb4java](http://usb4java.org/), plus a native `libusb4java` build for Apple Silicon (GPL-3) —
are downloaded by Gradle only when you build or run a Push 2 app.

---

## Project Structure

```
java-midi-workshop/
├── src/main/java/net/jetztgrad/midi/workshop/
│   ├── ListDevices.java       — enumerate MIDI devices
│   ├── PlayNote.java          — single note on/off
│   ├── SimpleSequence.java    — programmatic sequence
│   ├── MidiFilePlayer.java    — load and play a .mid file
│   ├── DrumPattern.java       — channel 10 percussion
│   ├── ProgramChange.java     — switch GM instrument patches
│   ├── LiveTerminalKeyboard.java — live keyboard via terminal
│   ├── LiveWindowKeyboard.java   — live keyboard via Swing window
│   ├── MidiMonitor.java      — monitor incoming MIDI messages with timestamps
│   ├── MidiPanic.java        — send All Notes Off on all channels
│   ├── PianoRollPlayer.java  — play piano roll text notation
│   ├── MidiToRoll.java       — convert a MIDI file to piano roll notation
│   └── exercise/
│       └── MyMelody.java      — hands-on template with TODO stubs
├── src/push2/java/net/jetztgrad/midi/workshop/push2/   — optional Ableton Push 2 apps
│   ├── Push2.java             — pads, buttons, palette, display (USB)
│   ├── LibUsbDisplay.java     — display via libusb + Java FFM
│   ├── Push2Hello.java        — hello world
│   ├── Notesweeper.java       — the game
│   ├── SweeperBoard.java      — game rules (no MIDI)
│   ├── BandLoop.java          — the band's looping groove
│   ├── JamSession.java        — jam mode (pads as keyboard)
│   └── Push2Jam.java          — standalone jam app
├── midi/                      — place public-domain .mid files here
├── slides/
│   ├── slides.md              — Marp source (English)
│   ├── slides.html            — pre-built slides (open in browser)
│   ├── slides-de.md           — Marp source (German)
│   └── slides-de.html         — pre-built slides DE
├── cheatsheets/
│   ├── midi-commands.md / -de.md      — MIDI message reference
│   ├── java-midi-api.md / -de.md      — Java API quick reference
│   └── os-setup.md / -de.md           — macOS & Windows setup guide
└── handouts/
    ├── participant-checklist.md / -de.md
    ├── tutor-packing-list.md / -de.md
    └── tutor-timeline.md / -de.md
```

---

## Workshop Materials

| Material | English | German |
|----------|---------|--------|
| Slides | [slides/slides.html](slides/slides.html) | [slides/slides-de.html](slides/slides-de.html) |
| MIDI commands | [cheatsheets/midi-commands.md](cheatsheets/midi-commands.md) | [cheatsheets/midi-commands-de.md](cheatsheets/midi-commands-de.md) |
| Java MIDI API | [cheatsheets/java-midi-api.md](cheatsheets/java-midi-api.md) | [cheatsheets/java-midi-api-de.md](cheatsheets/java-midi-api-de.md) |
| OS Setup | [cheatsheets/os-setup.md](cheatsheets/os-setup.md) | [cheatsheets/os-setup-de.md](cheatsheets/os-setup-de.md) |
| Participant checklist | [handouts/participant-checklist.md](handouts/participant-checklist.md) | [handouts/participant-checklist-de.md](handouts/participant-checklist-de.md) |
| Tutor packing list | [handouts/tutor-packing-list.md](handouts/tutor-packing-list.md) | [handouts/tutor-packing-list-de.md](handouts/tutor-packing-list-de.md) |
| Tutor timeline | [handouts/tutor-timeline.md](handouts/tutor-timeline.md) | [handouts/tutor-timeline-de.md](handouts/tutor-timeline-de.md) |

---

## Rebuilding Slides

The pre-built HTML slides are committed to the repository and can be opened directly in any browser.
To rebuild them after editing the Markdown sources, you need [Node.js](https://nodejs.org/) installed:

```bash
# Via Gradle (recommended)
./gradlew buildSlides

# Or call npx directly
npx --yes @marp-team/marp-cli@latest slides/slides.md    --html -o slides/slides.html
npx --yes @marp-team/marp-cli@latest slides/slides-de.md --html -o slides/slides-de.html
```

---

## MIDI Files

The `midi/` directory is ready for royalty-free MIDI files. See [`midi/README.md`](midi/README.md) for recommended public-domain sources (Mutopia Project, Kunst der Fuge).

---

## License

The source code in this repository is licensed under the [Apache License 2.0](LICENSE).

Workshop materials (slides, cheat sheets, handouts) are licensed under [Creative Commons CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

MIDI files (if added to `midi/`) retain their original licenses — see `midi/README.md`.
