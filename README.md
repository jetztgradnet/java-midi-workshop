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
(The optional Ableton Push 2 code in `src/push2/` is a separate source set with its own libraries — Wayang / usb4java — fetched by Gradle only when you build or run it.)

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

> All examples also run via `./gradlew <taskName>` — see `./gradlew tasks --group=workshop`.
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

The source code in this repository is licensed under the [MIT License](LICENSE).

Workshop materials (slides, cheat sheets, handouts) are licensed under [Creative Commons CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

MIDI files (if added to `midi/`) retain their original licenses — see `midi/README.md`.
