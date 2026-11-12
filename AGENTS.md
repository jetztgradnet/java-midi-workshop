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
| Java version | 25 (JDK toolchain via Gradle) |
| Build tool | Gradle wrapper (`./gradlew`), 9.2.1 |
| Runtime deps | None for workshop examples (`javax.sound.midi` only); the separate `push2` source set uses Wayang + usb4java |
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

src/push2/java/net/jetztgrad/midi/workshop/push2/   — own source set + deps (Wayang/usb4java)
        Push2.java           — pad/LED/port helpers + Display (50 ms refresh, two USB backends)
        LibUsbDisplay.java   — display backend "ffm": libusb via Java FFM, encodes frames itself
        Push2Hello.java      — hello world: pads → synth notes, LCD mirrors the grid
        Notesweeper.java     — the game: Minesweeper where you find a band (main, input, LEDs, LCD)
        JamSession.java      — jam mode: pads as keyboard over the groove (shared by both apps)
        Push2Jam.java        — standalone jam app: JamSession with a chosen lineup, no game
        SweeperBoard.java    — pure game rules (no MIDI): placement, flood fill, flags, win
        BandLoop.java        — 4-bar Am–F–C–G groove, one muted Sequencer track per band member

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

## Ableton Push 2 (`push2` source set)

```bash
./push2Hello.sh  [--device=<synth>] [--patch=<0-127|name>] [--push=<port>] [--display=ffm|wayang] [--latency=<ms>]
./notesweeper.sh [--device=<synth>] [--boom=<file.mid>] [--push=<port>] [--display=ffm|wayang] [--latency=<ms>]
./push2Jam.sh    [--band=Kick,Bass,…] [--device=<synth>] [--push=<port>] [--display=ffm|wayang] [--latency=<ms>]
./gradlew push2Hello  [-Pdevice=<synth>] [-Ppatch=...] [-Ppush=<port>] [-Pdisplay=ffm|wayang] [-Platency=<ms>]
./gradlew notesweeper [-Pdevice=<synth>] [-Pboom=<file.mid>] [-Ppush=<port>] [-Pdisplay=ffm|wayang] [-Platency=<ms>]
./gradlew push2Jam    [-Pband=Kick,Bass,…] [-Pdevice=<synth>] [-Ppush=<port>] [-Pdisplay=ffm|wayang] [-Platency=<ms>]
```

**Notesweeper rules**: 10 band members hide on the 8×8 pads (placed after the first reveal,
never next to it). Tap = reveal; the celesta note encodes the neighbour count (A minor
pentatonic, `COUNT_NOTES[n]`), empty areas ripple open. Hold ≥400 ms or Shift+tap = invite:
correct → member's track un-mutes in `BandLoop`, pad pulses in its colour; wrong → honk,
−1 life. Revealing a member = boom (crash + taiko + instrument burst + optional `--boom`
MIDI file), member lost, −1 life. 3 lives; New button restarts. All game logic runs on one
`ScheduledExecutorService` thread (input + timers), so no locks.
**Jam mode** = `JamSession`, used twice: as Notesweeper's bonus (`State.JAM`, ~2.5 s after a
win; lineup = members in join order, auto-joined at win last, exploded excluded; NEW → next
round) and standalone by `Push2Jam` (lineup from `--band=`, default Kick Snare Hi-hat Bass Keys
Pad Arp Lead; all lineup parts start playing). The owner keeps the single game thread and
forwards MIDI to `JamSession.onMidi`. Column x = `columnMember(x)` = `lineup.get(x)`, max 8 —
aligned with the display slot, the encoder above and the button below. Rows bottom → top = `Member.keys()`: A natural minor for melodic members,
8 GM drum/percussion notes for drum members. Encoders CC 71–78 set each member's *live* level
(`liveLevel[m]`, starts at `LIVE_UNITY` = 64 = middle); live velocity =
`min(127, pad × level / 64 + BOOST)` (boost 25; level 0 = silent) — see `liveVelocity`.
The LCD shows `vel <pad> → <sent>`. Master encoder CC 79 = background groove level for the
whole band (60 % on entering jam). Buttons below the display (CC 20–27) switch that member's
groove part on/off (`BandLoop.setMuted`, sends all-notes-off on that channel). Play (CC 85)
toggles the groove. Live notes go to `Member.liveChannel()` (11–16, drums 10), bypassing the
mixer, so the loop never cuts them off. NEW leaves jam mode.
**Mixer**: the sequencer's transmitter feeds `BandLoop.Mixer`, which scales the groove's
NOTE_ON velocities by `master / 127` and drops notes that scale to 0. `start()` resets it.
Channels: 1 Bass, 2 Keys, 3 Pad, 4 Arp, 5 Lead, 6 Brass, 7 count notes, 8 boom, 9 honk, 10 drums,
11–16 live (jam) Bass…Brass.
**Pad colours use Push's default palette — no SysEx.** Java's built-in macOS MIDI neither
delivers SysEx nor receives it (Push replies arrive in 3-byte USB-MIDI chunks it never
reassembles); a native CoreMIDI probe works both ways. So `Push2.DEFAULT_PALETTE` holds the
128 default entries (dumped once via CoreMIDI, SysEx 0x04). `Notesweeper` maps each intended
colour to the nearest entry (`Push2.nearestPaletteIndex`, members kept unique) into
`BandLoop.MEMBER_PAD` / `NUMBER_PAD` / `HIDDEN_PAD` / `HELD_PAD`, and the LCD draws
`Push2.paletteColor(index)` — so pads, buttons and screen match. The palette has no true
orange or cyan, so Kick / Keys / number 5 use colours that exist on Push (indices 3, 15, 3).
If Live has loaded its own palette since Push powered up, colours may differ until a power cycle.
`Push2.setPaletteEntry` / `reapplyPalette` exist but are unused (they'd work where Java SysEx does).

- Quit Ableton Live first (it grabs ports + display). Plug Push in before starting the JVM.
- **Latency**: Java's built-in synth (Gervill) buffers 120 ms by default — far too laggy for
  playing pads. `Push2.openSynth(args)` (used instead of `DeviceUtil.openReceiver` when no
  `--device=`) opens Gervill via its internal `AudioSynthesizer.open(line, {"latency": µs})`
  with `--latency=<ms>` (default 20). Needs
  `--add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED` (launchers + Gradle tasks pass
  it); without it, falls back to 120 ms with a warning. Measured: pad → synth `send` through
  the game thread is < 1 ms median, ≤ 2 ms while spinning an encoder — the rest is audio.
- **MIDI**: Java on macOS names the ports `Live Port` / `User Port` (vendor "Ableton AG",
  full name only in the description). Without Live, Push stays in Live mode → use `Live Port`
  both ways. `--device` keeps selecting the *synth*; `--push` overrides the Push port.
- **Pads**: notes 36–99, `note = 36 + x + 8*y` (y = 0 bottom row). Press = NOTE_ON, release =
  NOTE_OFF; polyphonic aftertouch is also sent while held.
- **Pad LEDs**: NOTE_ON to the pad note, velocity = palette index (0 black, 125 blue,
  126 green, 127 red), channel = animation (0 static, 6–10 pulse, 11–15 blink).
- **Buttons** are CCs (Shift 49, Select 48, New 87, Play 85, User 59, …), encoders CC 71–79
  relative. Sending the CC back sets the button LED (channel = animation).
  SysEx header `F0 00 21 1D 01 01 <cmd> … F7` (mode switch cmd `0A`).
- **Display**: 960×160 via USB bulk (VID 2982 / PID 1967, interface 0, EP 0x01), drawn via
  Wayang's `BufferedImage`. Push blanks it after 2 s without a frame → `Push2.Display`
  re-sends every 50 ms. Never send undocumented headers (can harm the device).
- **Display backends** (`--display=`, `Push2.Display.Backend`):
  - `wayang` (default): Wayang → usb4java (JNI), libusb bundled in the native jar.
  - `ffm`: `LibUsbDisplay`, no libraries — binds 8 libusb functions via `java.lang.foreign`
    (no structs needed), draws into a `TYPE_USHORT_565_RGB` image and encodes it itself
    (RGB565 → BGR565, little-endian, XOR `E7 F3 E7 FF`; 16-byte header + 160 × 2048 bytes,
    sent in 16 KB bulk transfers). Needs system libusb (`brew install libusb`); path
    override `-Dpush2.libusb=…`. Byte encoding verified identical to Wayang's `maskPixels`;
    Java2D's 24→16-bit rounding differs by ≤ 1 LSB between the two image types.
  - Both need `--enable-native-access=ALL-UNNAMED` (launchers and Gradle tasks set it).
- **Deps**: `org.deepsymmetry:wayang:0.1.8` → `usb4java:1.3.0`; Apple Silicon native from
  `io.github.dsheirer:libusb4java-darwin-aarch64:1.3.1` (GPL-3, libusb statically linked).
- Separate Gradle source set `src/push2` (`push2Implementation` deps, sees `main` output),
  so `src/main` stays dependency-free and builds offline. Never add deps to `main`.
- Launchers exec `java` directly with
  `-cp build/classes/java/main:build/classes/java/push2:build/runtime-libs/*`;
  the `runtimeLibs` task copies the push2 jars there.
- Spec/reference: https://github.com/Ableton/push-interface

---

## Known gaps / outstanding work

- No `.mid` files committed — participants download from Mutopia Project (see `midi/README.md`).
  `MidiFilePlayer` auto-detects the first `.mid` in `midi/` if no file is specified.
- `MyMelody.java` EOT tick is 32 (6 beats at 4 ticks/beat + 8 ticks slack).
  Participants who write longer melodies must move it.
