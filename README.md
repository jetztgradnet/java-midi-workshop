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

---

## Prerequisites

| Requirement | Version / Notes |
|-------------|----------------|
| JDK | 21 or higher |
| Git | any recent version |
| IDE / Editor | IntelliJ IDEA, VS Code, Eclipse, or any editor |
| OS | macOS 12+ or Windows 10/11 |

No external Java libraries required — only `javax.sound.midi` (bundled in the JDK).

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
| `./gradlew listDevices` | Enumerate all available MIDI devices |
| `./gradlew playNote` | Play a single note via the software synthesizer |
| `./gradlew simpleSequence` | Play a programmatic C–E–G–C sequence |
| `./gradlew midiFilePlayer` | Play the first MIDI file found in `midi/` |
| `./gradlew midiFilePlayer -PmidiFile=midi/song.mid` | Play a specific MIDI file |
| `./gradlew drumPattern` | Play a 4/4 kick + snare + hi-hat pattern |
| `./gradlew programChange` | Hear the same phrase in 4 different instrument patches |
| `./gradlew myMelody` | Run the hands-on exercise (edit `exercise/MyMelody.java` first!) |
| `./gradlew liveTerminalKeyboard` | Play notes live by typing in the terminal |
| `./gradlew liveWindowKeyboard` | Play notes live in a Swing window (true polyphony) |

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
