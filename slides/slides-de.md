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

## Musik machen mit Java

**90 Minuten · javax.sound.midi · Hands-on**

---

## Agenda

1. Was ist MIDI?
2. MIDI-Nachrichten, Kanäle & Dateien
3. Java MIDI API — die wichtigsten Klassen
4. Live-Demos: ListDevices · PlayNote · SimpleSequence
5. **Hands-on: eigene Melodie komponieren**
6. MIDI-Dateien, Drums, Program Change
7. Hardware: Seeed XIAO Synthesizer
8. Software-Synthesizer: macOS & Windows
9. Ausblick & Ressourcen

---

## Was ist MIDI?

**Musical Instrument Digital Interface** — Standard seit 1983

- Ein **Kommunikationsprotokoll**, kein Audio
- Sendet *Anweisungen*: „Spiele Note 60 mit Velocity 80"
- Übertragung per USB, 5-pol. DIN-Kabel oder Bluetooth (BLE MIDI)

> MIDI ist wie Notenblätter, die dein Computer lesen kann — der Synthesizer ist das Orchester.

---

## MIDI-Nachrichten

Jede MIDI-Nachricht besteht aus 1–3 Bytes:

| Byte | Name | Bedeutung |
|------|------|-----------|
| Status | `0x90` | NOTE_ON auf Kanal 1 |
| Data 1 | `60` | Notenummer (mittleres C) |
| Data 2 | `80` | Velocity (Lautstärke) |

**Status-Byte** = `Befehl (oberes Nibble)` + `Kanal (unteres Nibble)`

`0x90` = NOTE_ON (9) auf Kanal 1 (0)

---

## Notenummern

```
Oktave:   C    D    E    F    G    A    H
  C3 :   48   50   52   53   55   57   59
  C4 :   60   62   64   65   67   69   71   ← Mittleres C = 60
  C5 :   72   74   76   77   79   81   83
```

- **+1** = ein Halbton höher (z. B. C → Cis)
- **+12** = eine Oktave höher
- Velocity: `1` = Pianissimo · `64` = Mezzoforte · `127` = Fortissimo

---

## MIDI-Kanäle

- **16 Kanäle** pro MIDI-Port (1–16, im Code 0–15)
- Jeder Kanal kann ein eigenes Instrument (Patch) haben
- **Kanal 10 = Drums** (General-MIDI-Standard)

```java
// Channel 9 in Java = MIDI-Kanal 10 = GM Percussion
int DRUM_CHANNEL = 9;

// Note 36 = Bass Drum,  38 = Snare,  42 = Closed Hi-Hat
```

---

## Program Change: 128 Instrumente

General MIDI definiert **128 Standard-Patches** (0–127):

| Bereich | Familie | Beispiele |
|---------|---------|---------|
| 0–7 | Klavier | Grand Piano, E-Piano |
| 8–15 | Chrom. Perc. | Vibraphon, Marimba |
| 40–47 | Streicher | Violine, Cello |
| 56–63 | Blechbläser | Trompete, Posaune |
| 72–79 | Holzbläser | Flöte, Piccolo |
| 80–87 | Synth Lead | Square, Säge |

---

## MIDI-Geräte

| Typ | Beispiele |
|-----|---------|
| **Hardware-Synthesizer** | Seeed XIAO MIDI-Synth, Yamaha, Roland |
| **Controller** | MIDI-Keyboard, Drum-Pads |
| **Software-Synthesizer** | macOS DLS, Windows GS Wavetable |
| **Virtueller Port** | macOS IAC Driver, Windows loopMIDI |

Ein Gerät ist entweder ein **Receiver** (empfängt MIDI) oder ein **Transmitter** (sendet MIDI).

---

## MIDI-Dateien

- Binärformat, Endung `.mid`
- Enthält eine oder mehrere **Tracks** mit zeitgestempelten MIDI-Events
- **SMF Typ 0** — alle Events in einem Track
- **SMF Typ 1** — mehrere Tracks (häufigste Variante)

Java liest und schreibt MIDI-Dateien mit `MidiSystem.getSequence(file)`

---

## Javas `javax.sound.midi`

Im JDK integriert — keine externen Bibliotheken nötig!

| Klasse | Zweck |
|--------|-------|
| `MidiSystem` | Factory — Geräte holen, Dateien laden |
| `Synthesizer` | Software-Synthesizer im JDK |
| `Sequencer` | „Bandmaschine" — spielt Sequences ab |
| `MidiDevice` | Jedes MIDI-Gerät (Hardware oder Software) |
| `Sequence` | Container für Tracks + Zeitinformation |
| `Track` | Geordnete Liste von MidiEvents |
| `MidiEvent` | MidiMessage + Tick-Zeitstempel |
| `ShortMessage` | NOTE_ON/OFF, PROGRAM_CHANGE, CC, … |

---

## Loslegen

```bash
# Repository klonen
git clone https://github.com/jetztgradnet/java-midi-workshop.git
cd java-midi-workshop

# Verfügbare MIDI-Geräte auflisten (Setup prüfen)
./gradlew listDevices
```

Es sollte mindestens ein Gerät mit **Receiver: yes** erscheinen.

> macOS: IAC Driver in Audio MIDI Setup aktivieren
> Windows: VirtualMIDISynth installieren (siehe `cheatsheets/os-setup-de.md`)

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

// NOTE_ON: Kanal 0, mittleres C (60), Velocity 80
ShortMessage on = new ShortMessage();
on.setMessage(ShortMessage.NOTE_ON, 0, 60, 80);
receiver.send(on, -1);   // -1 = sofort senden

Thread.sleep(1000);

ShortMessage off = new ShortMessage();
off.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);
receiver.send(off, -1);
```

```bash
./gradlew playNote
```

---

## Sequences: Ticks und Tempo

```
Sequence(Sequence.PPQ, 4)
                         └─ Auflösung: 4 Ticks pro Beat
```

| Begriff | Bedeutung |
|---------|-----------|
| **PPQ** | Pulses Per Quarter-note (Ticks pro Viertelnote) |
| **Tick** | Kleinste Zeiteinheit in einer Sequence |
| **BPM** | Beats per Minute — 120 BPM → 1 Beat = 500 ms |

**Beispiel (4 Ticks/Beat, 120 BPM):**
- Beat 1 = Tick 0
- Beat 2 = Tick 4 (= 500 ms)
- Beat 3 = Tick 8 (= 1000 ms)

---

## Demo: SimpleSequence

```java
Sequence sequence = new Sequence(Sequence.PPQ, 4);
Track    track    = sequence.createTrack();

// addNote(track, channel, note, velocity, startTick, durationTicks)
addNote(track, 0, 60, 80,  0, 4);  // C4 Beat 1
addNote(track, 0, 64, 80,  4, 4);  // E4 Beat 2
addNote(track, 0, 67, 80,  8, 4);  // G4 Beat 3
addNote(track, 0, 72, 80, 12, 4);  // C5 Beat 4

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

**Dran! Öffne `src/main/java/.../exercise/MyMelody.java`**

Drei TODOs:

1. **Noten hinzufügen** — `addNote(track, 0, notenNummer, velocity, startTick, durationTicks)`
2. **Tempo ändern** — `int bpm = ...` setzen
3. **(Bonus)** Instrument mit `PROGRAM_CHANGE` wechseln

Melodie abspielen:
```bash
./gradlew myMelody
```

> Schnelle Notenreferenz: C4=60, D4=62, E4=64, F4=65, G4=67, A4=69, H4=71, C5=72

---

## MIDI-Dateien Demo

```java
// MIDI-Datei laden
Sequence sequence = MidiSystem.getSequence(new File("midi/bach_minuet.mid"));

// Abspielen
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
./gradlew midiFilePlayer -PmidiFile=midi/andere.mid
```

---

## Drums: Kanal 10

```java
int DRUM_CHANNEL = 9;   // Java-Index 9 = MIDI-Kanal 10

// Kick auf Beat 1, Snare auf Beat 2
addNote(track, DRUM_CHANNEL, 36, 100,  0, 2);  // Kick
addNote(track, DRUM_CHANNEL, 38,  90,  4, 2);  // Snare
addNote(track, DRUM_CHANNEL, 42,  70,  0, 1);  // Hi-Hat (jede Achtelnote)
```

| Note | Klang | Note | Klang |
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
// Auf Patch 56 = Trompete auf Kanal 0 wechseln
ShortMessage pc = new ShortMessage();
pc.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 56, 0);
receiver.send(pc, -1);

// Noten klingen jetzt wie eine Trompete
```

```bash
./gradlew programChange
```

Dieselbe C–E–G-Phrase als Piano (0), Violine (40), Trompete (56), Flöte (73)

---

## `--patch`: Instrument wechseln ohne Quellcode-Änderung

Alle Wiedergabe-Beispiele akzeptieren `--patch` — als Nummer oder Name:

```bash
./playNote.sh        --patch=40       # Violine  (Nummer)
./playNote.sh        --patch=violin   # Violine  (Teilname, Groß-/Kleinschreibung egal)
./simpleSequence.sh  --patch=flute    # Flöte
./midiFilePlayer.sh  --patch=trumpet  # Trompete als Startinstrument auf Kanal 0
./myMelody.sh        --patch=24       # Acoustic Guitar (Nylon)

./gradlew playNote -Ppatch=40
./gradlew simpleSequence -Ppatch=flute
```

> `drumPattern` nutzt Kanal 10 (Percussion — anderer PC-Kontext).
> `programChange` *ist* die PC-Demo, daher dort kein `--patch`.

---

## Hardware: Seeed XIAO Synth

1. Per **USB-Kabel** (USB-C) verbinden
2. Das Gerät erscheint automatisch als MIDI-Gerät
3. `./gradlew listDevices` — XIAO in der Liste suchen
4. In `PlayNote.java` den Receiver des XIAO verwenden

```java
// Gerät anhand des Namens finden
for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
    if (info.getName().contains("XIAO")) {
        MidiDevice device = MidiSystem.getMidiDevice(info);
        device.open();
        Receiver receiver = device.getReceiver();
        // ... MIDI-Nachrichten senden ...
    }
}
```

---

## Software-Synthesizer: macOS

**Integriert (DLS MusicDevice) — automatisch verfügbar**

Als virtuellen MIDI-Port für andere Apps freischalten:
1. **Audio MIDI Setup** öffnen (`⌘Space` → „Audio MIDI Setup")
2. `Fenster` → `MIDI-Studio einblenden`
3. Doppelklick auf **IAC-Treiber** → „Gerät ist online" aktivieren
4. `./gradlew listDevices` → IAC Bus erscheint

**GarageBand** kann ebenfalls als Software-Synth über IAC genutzt werden.

---

## Software-Synthesizer: Windows

**Windows 11** — Windows MIDI Services standardmäßig enthalten

**Windows 10** — Soundfont-Synthesizer installieren:
1. [VirtualMIDISynth](https://coolsoft.altervista.org/de/virtualmidisynth) herunterladen
2. Soundfont herunterladen, z. B. **GeneralUser GS** (kostenlos)
3. Soundfont in VirtualMIDISynth laden
4. `./gradlew listDevices` — Synthesizer erscheint als MIDI-Gerät

---

## Live Terminal-Keyboard

Noten spielen durch Tippen — keine extra Hardware nötig!

```bash
./gradlew liveTerminalKeyboard            # Standard-Synthesizer
./gradlew liveTerminalKeyboard -Pdevice=XIAO
```

```
  Q  W     R  T  Y     I  O    ← Halbtöne / schwarze Tasten
  A  S  D  F  G  H  J  K  L  ;  ← weiße Tasten
  C  D  E  F  G  A  H  C  D  E   (aktuelle Oktave)

  +/=  Oktave hoch   |   -  Oktave runter   |   ESC  Beenden
```

> Jeder Tastendruck sendet `NOTE_ON`; nach fester Dauer sendet
> ein virtueller Thread `NOTE_OFF` — auch Akkorde funktionieren.

---

## Live-Fenster-Keyboard

Ein Swing-Fenster ermöglicht echte Polyphonie — mehrere Tasten gleichzeitig.

```bash
./gradlew liveWindowKeyboard              # Standard-Synthesizer
./gradlew liveWindowKeyboard -Pdevice=XIAO
```

- **Geräte-Dropdown** — MIDI-Ausgang ohne Neustart wechseln
- **LEERTASTE** — Haltepedal (Sustain, CC 64)
- **ESC** — All Notes Off (Panic)
- Tastendruck speichert Notennummer → korrektes `NOTE_OFF` auch nach Oktavwechsel
- Fokusverlust löst automatisch Panic aus (keine hängenden Noten)

> Verwendet `KeyboardFocusManager.addKeyEventDispatcher` damit Tasten-Events
> unabhängig vom fokussierten Swing-Element ankommen.

---

## Ausblick

| Thema | Wo nachschauen |
|-------|---------------|
| **BLE MIDI** | JSyn-Bibliothek oder OS-Bluetooth-MIDI-Einstellungen |
| **JFugue** | High-Level MIDI-Komposition für Java |
| **Alda** | Musik-Programmiersprache (JVM) |
| **JACK** | Low-Latency Audio/MIDI-Routing (Linux/macOS) |
| **OGG/WAV** | `javax.sound.sampled` |
| **Echtzeit-Input** | `Transmitter` + `Receiver` Kette |

> BLE MIDI: per OS-Bluetooth koppeln — Java sieht es als normales MIDI-Gerät!

---

## Ressourcen

- **JavaDoc** — [`javax.sound.midi`](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/sound/midi/package-summary.html)
- **General-MIDI-Spezifikation** — midi.org/specifications
- **MIDI-Notentabelle** — `cheatsheets/midi-commands-de.md`
- **OS-Einrichtung** — `cheatsheets/os-setup-de.md`
- **Java-API-Spickzettel** — `cheatsheets/java-midi-api-de.md`
- **Mutopia-Projekt** — kostenlose gemeinfreie Noten + MIDI-Dateien

---

<!-- _class: title -->

# Danke!

## Fragen?

```bash
./gradlew listDevices   # hier starten
./gradlew myMelody      # eigene Kreation abspielen
```

Folien & Code: **github.com/jetztgradnet/java-midi-workshop**

---

*Ende der Folien*
