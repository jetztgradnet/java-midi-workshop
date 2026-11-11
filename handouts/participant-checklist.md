# Participant Pre-Workshop Checklist

Please complete these steps **before** the workshop begins. The whole setup takes about 10–15 minutes.

---

## 1. Install JDK 25

```bash
java -version
```

Expected output: `openjdk version "25.x.x" ...`

**Install JDK 25 if needed:**
- **macOS** (Homebrew): `brew install temurin@25`
- **macOS** (manual): https://adoptium.net
- **Windows**: https://adoptium.net → select JDK 25

Make sure `java` is on your PATH after installation.

---

## 2. Install Git

```bash
git --version
```

- **macOS**: comes with Xcode Command Line Tools (`xcode-select --install`)
- **Windows**: https://git-scm.com/download/win

---

## 3. Choose an IDE or Editor

Any of these work well:
- [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/) (recommended)
- [VS Code](https://code.visualstudio.com/) with the Java Extension Pack
- Eclipse, NetBeans, or any editor you're comfortable with

---

## 4. Clone the Repository

```bash
git clone https://github.com/jetztgradnet/java-midi-workshop.git
cd java-midi-workshop
```

---

## 5. Verify Your MIDI Setup

```bash
./gradlew listDevices
```

**You should see at least one device listed with `Receiver: yes`.**

If you see `No MIDI devices found`, follow the steps for your OS:

### macOS — Enable the IAC Driver

1. Open **Audio MIDI Setup** (`⌘ Space` → type "Audio MIDI Setup")
2. `Window → Show MIDI Studio`
3. Double-click **IAC Driver**
4. Check **"Device is online"** → Apply
5. Re-run `./gradlew listDevices`

### Windows — Install VirtualMIDISynth

1. Download from https://coolsoft.altervista.org/en/virtualmidisynth
2. Also download a soundfont: https://schristiancollins.com/generaluser.php
3. Install VirtualMIDISynth and load the soundfont
4. Re-run `./gradlew listDevices`

---

## 6. Test Sound Output

```bash
./gradlew playNote
```

You should hear a short piano note (middle C). If not, check volume and follow the troubleshooting section in `cheatsheets/os-setup.md`.

---

## 7. Recommended: Bring Headphones

The workshop room may be noisy. Headphones ensure you can hear your MIDI output clearly.

---

## Quick Note Reference (for the exercise)

```
C4=60  D4=62  E4=64  F4=65  G4=67  A4=69  B4=71  C5=72
```

One octave up = +12 | One semitone up = +1

Velocity: 1 (quiet) → 64 (medium) → 127 (loud)

---

## Questions Before the Workshop?

Open an issue at: **github.com/jetztgradnet/java-midi-workshop/issues**
