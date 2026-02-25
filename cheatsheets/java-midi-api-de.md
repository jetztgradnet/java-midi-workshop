# Java-MIDI-API Spickzettel

Paket: `javax.sound.midi`
Im JDK enthalten — keine externen Abhängigkeiten nötig.

---

## Klassenübersicht

| Klasse / Interface | Zweck | Wichtige Methoden |
|-------------------|-------|------------------|
| `MidiSystem` | Statische Factory | `getMidiDeviceInfo()`, `getMidiDevice()`, `getSynthesizer()`, `getSequencer()`, `getSequence(File)` |
| `MidiDevice` | Beliebiges MIDI-Gerät | `open()`, `close()`, `getReceiver()`, `getTransmitter()`, `getMaxReceivers()` |
| `Synthesizer` | Software-Synthesizer | `open()`, `close()`, `getReceiver()`, `loadInstruments()` |
| `Sequencer` | Wiedergabe-Engine | `open()`, `setSequence()`, `start()`, `stop()`, `isRunning()`, `setTempoInBPM()`, `getMicrosecondPosition()` |
| `Sequence` | Container | `createTrack()`, `getTracks()`, `getMicrosecondLength()`, `getTickLength()` |
| `Track` | Event-Liste | `add(MidiEvent)`, `remove(MidiEvent)`, `size()` |
| `MidiEvent` | Nachricht + Tick | `getMessage()`, `getTick()` |
| `ShortMessage` | Note/CC/PC/etc. | `setMessage(command, channel, data1, data2)`, `getCommand()`, `getChannel()`, `getData1()`, `getData2()` |
| `MetaMessage` | Tempo/EOT/etc. | `setMessage(type, data, length)`, `getType()` |
| `MidiDevice.Info` | Gerätemetadaten | `getName()`, `getVendor()`, `getDescription()` |
| `Receiver` | Nimmt Nachrichten an | `send(MidiMessage, timestamp)`, `close()` |
| `Transmitter` | Sendet Nachrichten | `setReceiver(Receiver)`, `close()` |

---

## Muster: Synthesizer öffnen und Note spielen

```java
import javax.sound.midi.*;

// 1. Standard-Software-Synthesizer öffnen
Synthesizer synth = MidiSystem.getSynthesizer();
synth.open();

// 2. Receiver holen
Receiver receiver = synth.getReceiver();

// 3. NOTE_ON senden
ShortMessage on = new ShortMessage();
on.setMessage(ShortMessage.NOTE_ON, 0, 60, 80);   // Kanal 0, C4, Vel 80
receiver.send(on, -1);   // -1 = sofort

Thread.sleep(1000);

// 4. NOTE_OFF senden
ShortMessage off = new ShortMessage();
off.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);
receiver.send(off, -1);

// 5. Aufräumen
receiver.close();
synth.close();
```

---

## Muster: Programmatische Sequence erstellen und abspielen

```java
import javax.sound.midi.*;

// 1. Sequence erstellen: PPQ-Modus, 4 Ticks pro Beat
Sequence sequence = new Sequence(Sequence.PPQ, 4);
Track track = sequence.createTrack();

// 2. Noten hinzufügen (Hilfsmethode siehe unten)
addNote(track, 0, 60, 80,  0, 4);  // C4, Beat 1, 1 Beat Dauer
addNote(track, 0, 64, 80,  4, 4);  // E4, Beat 2
addNote(track, 0, 67, 80,  8, 4);  // G4, Beat 3

// 3. End-of-Track-Meta-Nachricht (erforderlich!)
MetaMessage eot = new MetaMessage();
eot.setMessage(0x2F, new byte[0], 0);
track.add(new MidiEvent(eot, 12));

// 4. Sequencer einrichten
Sequencer sequencer = MidiSystem.getSequencer();
sequencer.open();
sequencer.setSequence(sequence);
sequencer.setTempoInBPM(120);

// 5. Abspielen und warten
sequencer.start();
while (sequencer.isRunning()) Thread.sleep(50);
sequencer.close();

// Hilfsmethode
static void addNote(Track track, int ch, int note, int vel,
                    long startTick, long durationTicks) throws InvalidMidiDataException {
    track.add(new MidiEvent(
        new ShortMessage(ShortMessage.NOTE_ON,  ch, note, vel), startTick));
    track.add(new MidiEvent(
        new ShortMessage(ShortMessage.NOTE_OFF, ch, note, 0),   startTick + durationTicks));
}
```

---

## Muster: MIDI-Datei laden und abspielen

```java
import javax.sound.midi.*;
import java.io.File;

// 1. Laden
Sequence sequence = MidiSystem.getSequence(new File("midi/lied.mid"));

// 2. Abspielen
Sequencer sequencer = MidiSystem.getSequencer();
sequencer.open();
sequencer.setSequence(sequence);
sequencer.start();

// 3. Warten bis fertig
while (sequencer.isRunning()) Thread.sleep(100);

sequencer.close();
```

---

## Muster: Program Change senden

```java
// Patch 56 (Trompete) auf Kanal 0 wählen
ShortMessage pc = new ShortMessage();
pc.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 56, 0);
//                                         ^Kanal ^Patch
receiver.send(pc, -1);
```

---

## Muster: MIDI-Geräte auflisten

```java
MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
for (MidiDevice.Info info : infos) {
    MidiDevice device = MidiSystem.getMidiDevice(info);
    System.out.println(info.getName()
        + " | Receiver=" + device.getMaxReceivers()
        + " | Transmitter=" + device.getMaxTransmitters());
}
```

---

## Muster: Bestimmtes Gerät ansprechen

```java
for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
    if (info.getName().contains("XIAO")) {          // nach Name suchen
        MidiDevice device = MidiSystem.getMidiDevice(info);
        if (device.getMaxReceivers() != 0) {        // ist es ein Ausgang?
            device.open();
            Receiver receiver = device.getReceiver();
            // Nachrichten senden …
            receiver.close();
            device.close();
        }
    }
}
```

---

## Tick-Rechnung

```
TicksProBeat = Auflösung  (z. B. 4)
BeatsProMin  = BPM         (z. B. 120)

msProBeat    = 60.000 / BPM                    → 500 ms/Beat bei 120 BPM
msProTick    = msProBeat / TicksProBeat        → 125 ms/Tick
TicksProSek  = 1000 / msProTick               → 8 Ticks/Sek

Ticks = Beats × TicksProBeat
Beats = Ticks / TicksProBeat
```

**Beispiel** — Note bei 1,5 Sekunden platzieren (120 BPM, 4 Ticks/Beat):
```
Beats = 1,5 s × (120 / 60) = 3 Beats
Ticks = 3 × 4 = 12
```

---

## ShortMessage-Befehls-Konstanten

| Konstante | Hex | Beschreibung |
|-----------|-----|-------------|
| `ShortMessage.NOTE_OFF` | `0x80` | Note stoppen |
| `ShortMessage.NOTE_ON` | `0x90` | Note starten (Vel 0 = aus) |
| `ShortMessage.CONTROL_CHANGE` | `0xB0` | Controller-Änderung |
| `ShortMessage.PROGRAM_CHANGE` | `0xC0` | Instrument wählen |
| `ShortMessage.PITCH_BEND` | `0xE0` | Tonhöhenrad |

---

## Häufige Ausnahmen

| Ausnahme | Ursache |
|----------|---------|
| `MidiUnavailableException` | Kein MIDI-Subsystem / Gerät belegt |
| `InvalidMidiDataException` | Fehlerhafte Nachrichtenbytes / ungültige Werte |
| `IOException` | MIDI-Datei nicht gefunden / nicht lesbar |
