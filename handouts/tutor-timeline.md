# Tutor Timeline & Speaker Notes

90-minute workshop guide with talking points, timing, and fallback options.

---

## Pre-Workshop (T−30 min)

- Arrive early, set up projector, test display connection
- Open `slides/slides.html` in a browser (full-screen mode: F11 / Cmd+Ctrl+F)
- Open a terminal in the workshop directory
- Run `./gradlew listDevices` and `./gradlew playNote` — confirm audio works
- Connect one XIAO to your laptop, confirm it appears in `listDevices`
- Write Wi-Fi password on the board
- Place printed cheat sheets on tables or at the front
- Distribute sticky notes and markers

---

## Minute-by-Minute Guide

| Clock | Duration | Topic | Notes |
|-------|----------|-------|-------|
| 0:00 | **10 min** | What is MIDI? | Show the physical Seeed XIAO synth. Connect it and play a note live from the command line to hook the audience immediately. Ask "who has used MIDI before?" |
| 0:10 | **10 min** | Java MIDI API | Walk through the key classes slide. Keep it high-level — the goal is orientation, not memorization. Mention javax.sound.midi is built in (no Maven needed). |
| 0:20 | **10 min** | ListDevices + PlayNote | Run both commands live. Ask participants to run on their machines. **Checkpoint**: anyone seeing zero devices? Help them enable IAC (macOS) or direct them to os-setup.md (Windows). |
| 0:30 | **15 min** | SimpleSequence | Walk through the code on screen. Explain PPQ, ticks, tempo. Run it. Play with the tempo number live. Ask "what note is 60? what is 72?". |
| 0:45 | **15 min** | **HANDS-ON: MyMelody** | Point to the exercise file. Circulate while participants work. Hand out XIAOs (up to 6 units). Suggest: finish TODO 1 first, then try TODO 2 (tempo), bonus last. |
| 1:00 | **5 min** | MIDI Files | Run `midiFilePlayer`. Show the `midi/` directory. Briefly mention SMF Type 0 vs 1. |
| 1:05 | **5 min** | Drums + ProgramChange | Run `drumPattern` and `programChange`. Let participants hear the difference between patches. |
| 1:10 | **10 min** | Hardware demo | Show the XIAO connection. Demo routing Java output to the XIAO. Mention macOS IAC and Windows loopMIDI if not done already. |
| 1:20 | **10 min** | Wrap-up + Q&A | Show the resources slide. Mention BLE MIDI as a teaser. Take questions. Point to the repo and issues page. |

**Total: 90 minutes**

---

## Talking Points

### What is MIDI? (0:00–0:10)
- MIDI is a *protocol*, not audio — it's instructions, not sound
- Like sheet music vs. a recording
- The same MIDI file sounds completely different on different synths
- "It's been around since 1983 and is still the standard in music production"

### Java MIDI API (0:10–0:20)
- Built into every JDK — `javax.sound.midi` — no Maven, no downloads
- Key mental model: Sequence → Track → Events; Sequencer plays it back
- Receiver = thing that accepts MIDI; Transmitter = thing that sends it
- The platform synth "just works" on macOS and modern Windows

### ListDevices + PlayNote (0:20–0:30)
- `listDevices` is the "hello world" of MIDI — confirms your environment
- `getMaxReceivers() != 0` means it can receive MIDI (i.e. it's an output/synth)
- `getMaxTransmitters() != 0` means it sends MIDI (i.e. it's a controller/input)
- `ShortMessage.NOTE_ON` with velocity 0 is equivalent to NOTE_OFF — quirk of the spec

### SimpleSequence (0:30–0:45)
- PPQ = Pulses Per Quarter-note — the resolution of your timeline
- 4 ticks/beat is simple to reason about: beat 1 = 0, beat 2 = 4, beat 3 = 8
- Higher resolution (48, 96, 480) gives smoother swing/timing — not needed today
- `setTempoInBPM(120)` = 500 ms per beat
- Show that changing the BPM number changes the speed, not the pitch

### Hands-On (0:45–1:00)
- Encourage experimentation — wrong notes are fine, that's how you learn
- Common stumbling blocks:
  - Note numbers off by octave (forgot +12 for octave up)
  - End-of-track tick too early (move `eot` tick further right if melody is cut off)
  - Forgetting to uncomment lines

### Wrap-Up (1:20–1:30)
- BLE MIDI: the OS pairs a Bluetooth MIDI device, Java sees it as regular MIDI — mention only
- JFugue: higher-level API if you want to write `"C D E F G"` strings
- The real power: combine MIDI with Java logic — generative music, games, IoT

---

## If Running Short on Time (skip these)

1. **ProgramChange** — can be mentioned but not demoed
2. **MidiFilePlayer** — can be skipped; participants can explore on their own
3. **Hardware demo** — briefly show XIAO plugged in without detailed code walkthrough
4. **BLE MIDI** — always mention-only anyway

## If Running Long (speed up here)

- Keep ListDevices and PlayNote to 5 min total (skip line-by-line explanation)
- Keep SimpleSequence walkthrough to 8 min (don't play with tempo live)

---

## Common Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `No MIDI devices found` | macOS: IAC disabled | Enable IAC Driver in Audio MIDI Setup |
| `No MIDI devices found` | Windows: no synth | Install VirtualMIDISynth |
| No sound on `playNote` | Volume muted | Check OS volume, not just app |
| `./gradlew: Permission denied` | Missing execute bit | `chmod +x gradlew` |
| Gradle download stuck | No internet | Use mobile hotspot or pre-download |
| Compile error | Wrong JDK (< 21) | `java -version` must show 21+ |
| Note cuts off early | EOT tick too soon | Move `eot` tick past last note |
| XIAO not showing | Bad USB cable | Try different cable |
