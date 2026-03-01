# java-midi-workshop — AI Agent Context

This is a hands-on Java MIDI workshop project used in live teaching sessions.
It contains standalone runnable examples, slides, cheatsheets, handouts, and
a Gradle build — all self-contained, no external dependencies.

---

## Quick facts

| Item | Value |
|------|-------|
| Package root | `net.jetztgrad.midi.workshop` |
| Exercise sub-package | `net.jetztgrad.midi.workshop.exercise` |
| Java version | 21 (JDK toolchain via Gradle) |
| Build tool | Gradle wrapper (`./gradlew`), targets 8.13 |
| Runtime deps | None — `javax.sound.midi` only |
| Language policy | All participant-facing content in **English + German** (parallel files) |

---

## Repository layout

```
src/main/java/net/jetztgrad/midi/workshop/
    DeviceUtil.java          — shared MIDI device + GM patch helpers (no main())
    ListDevices.java         — list all MIDI devices
    PlayNote.java            — send a single NOTE_ON / NOTE_OFF
    SimpleSequence.java      — build + play a sequence programmatically
    MidiFilePlayer.java      — load + play a .mid file
    DrumPattern.java         — 4/4 drum pattern on channel 9
    ProgramChange.java       — demonstrate GM instrument switching
    LiveTerminalKeyboard.java — play notes in real time from the terminal
    LiveWindowKeyboard.java  — play notes in real time via a Swing window
    exercise/
        MyMelody.java        — participant hands-on exercise skeleton

slides/          slides.md / slides-de.md  (Marp source) + pre-built .html
cheatsheets/     midi-commands, java-midi-api, os-setup  (en + de)
handouts/        participant-checklist, tutor-packing-list, tutor-timeline (en + de)
midi/            README.md; .mid files NOT in repo (download from Mutopia Project)
spec/            Java-MIDI-Workshop.md  (original spec)
```

---

## Running examples

Every example has a Gradle task and a launcher script:

```bash
./gradlew tasks --group=workshop      # show all tasks

# Gradle (compile + run in one step)
./gradlew listDevices
./gradlew playNote           [-Pdevice=<name>] [-Ppatch=<0-127|name>]
./gradlew simpleSequence     [-Pdevice=<name>] [-Ppatch=<0-127|name>]
./gradlew midiFilePlayer     [-PmidiFile=<path>] [-Pdevice=<name>] [-Ppatch=<0-127|name>]
./gradlew drumPattern        [-Pdevice=<name>]
./gradlew programChange      [-Pdevice=<name>]
./gradlew myMelody           [-Pdevice=<name>] [-Ppatch=<0-127|name>]
./gradlew liveTerminalKeyboard [-Pdevice=<name>] [-Ppatch=<0-127|name>]
./gradlew liveWindowKeyboard   [-Pdevice=<name>] [-Ppatch=<0-127|name>]

# Shell scripts (preferred for live keyboards — direct exec, no Gradle daemon)
./listDevices.sh
./playNote.sh          [--device=<name>] [--patch=<0-127|name>]
./simpleSequence.sh    [--device=<name>] [--patch=<0-127|name>]
./midiFilePlayer.sh    [<file.mid>] [--device=<name>] [--patch=<0-127|name>]
./drumPattern.sh       [--device=<name>]
./programChange.sh     [--device=<name>]
./myMelody.sh          [--device=<name>] [--patch=<0-127|name>]
./liveKeyboard.sh      [--device=<name>] [--patch=<0-127|name>]
./liveWindow.sh        [--device=<name>] [--patch=<0-127|name>]
```

---

## Key `DeviceUtil` helpers

All examples call these static methods; they live in `DeviceUtil.java`:

| Method | Purpose |
|--------|---------|
| `openReceiver(args)` | Open MIDI output; honours `--device=<partial-name>`, falls back to default synth |
| `deviceFilter(args)` | Extract the `--device=` value (or null) |
| `patchFilter(args)` | Extract `--patch=` value; accepts `0–127` or case-insensitive partial GM name |
| `sendProgramChange(receiver, channel, program)` | Send a PROGRAM_CHANGE message |
| `GM_PATCH_NAMES[128]` | Full GM instrument name list (index = program number) |
| `PRESET_PROGRAMS[10]` / `PRESET_NAMES[10]` | Curated 10-preset list for digit-key selection |
| `gmPatchList()` | 128-entry `String[]` for Swing combo boxes |

`patchFilter` resolves in this order: numeric → partial name match → 0 (Grand Piano).

---

## Architecture rules

- **Every example is a standalone `main()` class** — no shared base class, intentionally.
- `DeviceUtil` is the only shared utility; keep it lean.
- `MyMelody.java` is a participant exercise skeleton. It compiles and runs even with all
  TODOs unfilled (plays silence, exits cleanly). The EOT marker is at tick 32 — participants
  must move it if their melody is longer.
- `SimpleSequence.addNote()` is the canonical "add note to track" helper pattern;
  it is intentionally duplicated in `DrumPattern` and `MyMelody` so each file is
  self-contained and readable in isolation.

---

## `--patch` / `-Ppatch` support

Added to `PlayNote`, `SimpleSequence`, `MidiFilePlayer`, `MyMelody`,
`LiveTerminalKeyboard`, `LiveWindowKeyboard`.

**Not added to:** `listDevices` (no sound), `drumPattern` (channel 9 — PC selects drum
kit, different semantics), `programChange` (IS the PC demo).

Pattern used in each file:
```java
var receiver = DeviceUtil.openReceiver(args);
int patch = DeviceUtil.patchFilter(args);
DeviceUtil.sendProgramChange(receiver, 0, patch);
System.out.println("Instrument: " + DeviceUtil.GM_PATCH_NAMES[patch] + " (patch " + patch + ")");
// then pass receiver to sequencer / use directly
```

---

## Slides

Marp markdown source; pre-built HTML is committed.  Rebuild with:
```bash
npx @marp-team/marp-cli@latest slides/slides.md    --html -o slides/slides.html
npx @marp-team/marp-cli@latest slides/slides-de.md --html -o slides/slides-de.html
# or:
./gradlew buildSlides
```

---

## Known gaps / outstanding work

- No `.mid` files committed — participants download from Mutopia Project (see `midi/README.md`).
  `MidiFilePlayer` auto-detects the first `.mid` in `midi/` if no file is specified.
- `MyMelody.java` EOT tick is 32 (6 beats at 4 ticks/beat + 8 ticks slack).
  Participants who write longer melodies must move it.
