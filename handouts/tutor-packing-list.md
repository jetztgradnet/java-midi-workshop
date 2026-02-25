# Tutor Packing List

Everything you need to bring or prepare for a smooth workshop.

---

## Hardware

- [ ] **6× Seeed XIAO MIDI Synthesizer** — the handheld synths for participants
- [ ] **6× USB-C cables** — one per XIAO unit (test each cable before the event!)
- [ ] **USB hub(s)** — at least one 4-port hub; bring two if the room has limited USB ports
- [ ] **USB-A → USB-C adapters** — 3–4 pieces for participants with older laptops
- [ ] **USB-A → USB-B adapter** (if you have older MIDI gear)
- [ ] **Headphones** — at least 1× for your own testing; bring 3–4 if participants don't have them
- [ ] **Small portable speaker** — for live demos the whole room can hear
- [ ] **Laptop** — with repo cloned and all `./gradlew *` tasks verified working
- [ ] **Power strip / extension cord** — essential for keeping devices charged

---

## Printed Materials

- [ ] **Cheat sheet: MIDI commands** (`cheatsheets/midi-commands.md`)
- [ ] **Cheat sheet: Java MIDI API** (`cheatsheets/java-midi-api.md`)
- [ ] **Cheat sheet: OS setup** (`cheatsheets/os-setup.md`)
- [ ] **Participant checklist** (`handouts/participant-checklist.md`) — in case some participants arrive without prep done

Print double-sided to save paper. Bring 5–10% extra copies.

---

## Logistics

- [ ] **Sticky notes + markers** — for quick help signaling (green = OK, red = stuck)
- [ ] **Projector / screen access** — confirm with venue in advance
- [ ] **HDMI / display adapter** — match your laptop's output (USB-C, Mini DP, etc.)
- [ ] **Wi-Fi credentials** — write them on the whiteboard at the start

---

## Digital Prep (Day Before)

- [ ] Clone / pull latest repo: `git pull origin main`
- [ ] Run all workshop tasks and verify audio output:
  ```bash
  ./gradlew listDevices
  ./gradlew playNote
  ./gradlew simpleSequence
  ./gradlew midiFilePlayer
  ./gradlew drumPattern
  ./gradlew programChange
  ./gradlew myMelody
  ```
- [ ] Open `slides/slides.html` in a browser — verify slides render
- [ ] Test all 6 XIAO units over USB — confirm each appears in `listDevices`
- [ ] Charge all XIAOs if they have internal batteries

---

## Day-of Checklist (Arrive 30 Min Early)

- [ ] Set up projector and open slides
- [ ] Connect one XIAO to your laptop and confirm it works
- [ ] Write Wi-Fi password on the board
- [ ] Place sticky notes and markers on tables
- [ ] Lay out printed cheat sheets at seats or on a table at the front
- [ ] Open a terminal with the workshop directory ready
