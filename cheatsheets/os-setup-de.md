# OS-Einrichtungsanleitung — MIDI unter macOS und Windows

## macOS

### Eingebauter Software-Synthesizer (DLS MusicDevice)

macOS bringt einen General-MIDI-Software-Synthesizer mit. Java nutzt ihn automatisch — keine Konfiguration erforderlich. `./gradlew listDevices` sollte ihn in der Geräteliste zeigen.

### IAC-Treiber aktivieren (Virtueller MIDI-Port)

Der IAC-Treiber (Inter-Application Communication) erstellt virtuelle MIDI-Ports, damit Anwendungen miteinander kommunizieren können (z. B. Java → GarageBand).

1. **Audio MIDI Setup** öffnen
   - `⌘ Leertaste`, „Audio MIDI Setup" tippen, Enter drücken
   - Oder: `Programme → Dienstprogramme → Audio MIDI Setup`
2. **MIDI-Studio** öffnen
   - Menü: `Fenster → MIDI-Studio einblenden`
3. Doppelklick auf **IAC-Treiber**
4. Häkchen bei **„Gerät ist online"** setzen
5. **Übernehmen** klicken

Nach diesem Schritt zeigt `./gradlew listDevices` einen **IAC-Bus**.

### GarageBand als Synthesizer

1. GarageBand öffnen
2. Neues Projekt mit einem Software-Instrument-Track erstellen
3. IAC-Treiber online stellen (siehe oben)
4. Java sendet MIDI an den IAC-Bus → GarageBand empfängt und spielt

### Nützliche Terminal-Befehle

```bash
# MIDI-Geräte mit macOS-Systemwerkzeug auflisten
system_profiler SPMIDIDataType

# Workshop-Geräteliste
./gradlew listDevices
```

---

## Windows

### Windows 11 — Windows MIDI Services

Windows 11 enthält standardmäßig **Windows MIDI Services** mit einem modernen MIDI-Stack. Der klassische `Microsoft GS Wavetable Synth` ist ebenfalls noch vorhanden.

`./gradlew listDevices` ausführen — es sollte mindestens ein MIDI-Ausgabegerät erscheinen.

### Windows 10 — VirtualMIDISynth (Empfohlen)

Der eingebaute Microsoft GS Wavetable Synth klingt veraltet. VirtualMIDISynth bietet bessere Qualität:

1. **VirtualMIDISynth herunterladen**
   - https://coolsoft.altervista.org/de/virtualmidisynth
   - Kostenlos, Open Source

2. **Soundfont (SF2-Datei) herunterladen**
   - **GeneralUser GS** — kostenlos, hohe Qualität: https://schristiancollins.com/generaluser.php
   - **FluidR3 GM** — Alternative: nach „FluidR3_GM.sf2" suchen

3. **VirtualMIDISynth installieren** und starten

4. **Soundfont laden**
   - Im Tab „Soundfonts" auf `+` klicken
   - Die heruntergeladene `.sf2`-Datei auswählen

5. **Prüfen**
   - `./gradlew listDevices` — „CoolSoft VirtualMIDISynth" erscheint

### loopMIDI (Virtuelles MIDI-Kabel)

MIDI zwischen Java und einer anderen Anwendung (z. B. DAW) routen:

1. **loopMIDI** herunterladen: https://www.tobias-erichsen.de/software/loopmidi.html
2. Installieren und starten
3. Auf `+` klicken, um einen virtuellen Port zu erstellen
4. `./gradlew listDevices` zeigt den neuen Port

---

## Seeed XIAO MIDI-Synthesizer

### Per USB verbinden

1. **USB-C-Kabel** verwenden, um den Seeed XIAO mit dem Computer zu verbinden
2. Das Gerät ist USB-Class-Compliant — **kein Treiber nötig**
3. macOS und Windows erkennen es automatisch als MIDI-Gerät
4. `./gradlew listDevices` — nach „XIAO" in der Liste suchen

### XIAO im Code ansprechen

```java
// XIAO-Receiver anhand des Namens finden
for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
    if (info.getName().contains("XIAO")) {
        MidiDevice device = MidiSystem.getMidiDevice(info);
        device.open();
        Receiver receiver = device.getReceiver();
        // MIDI-Nachrichten hier senden
        receiver.close();
        device.close();
    }
}
```

### Fehlerbehebung

| Problem | Lösung |
|---------|--------|
| Gerät erscheint nicht | Anderes USB-Kabel probieren; USB-C-Port direkt verwenden |
| Verzerrter Klang | Lautstärke am Gerät prüfen |
| Mehrere XIAO-Geräte | Namen haben Index-Suffix, z. B. „XIAO 1", „XIAO 2" |

---

## Allgemeine Fehlerbehebung

### „Keine MIDI-Geräte gefunden"

- **macOS**: IAC-Treiber aktivieren (siehe oben) oder prüfen, ob DLS MusicDevice geladen ist
- **Windows**: VirtualMIDISynth installieren oder Geräte-Manager nach MIDI-Geräten durchsuchen
- **Alle**: `./gradlew listDevices` ggf. als Administrator ausführen

### Kein Klang

1. Systemlautstärke prüfen (nicht stummgeschaltet)
2. Sicherstellen, dass mindestens ein Gerät mit `Receiver: yes` gelistet ist
3. Kopfhörer verwenden, um Lautsprecher-Probleme auszuschließen

### Gradle-Probleme

```bash
# Wrapper neu herunterladen (bei defektem Cache)
rm -rf ~/.gradle/wrapper/dists/gradle-8*
./gradlew --version

# Mit Debug-Ausgabe ausführen
./gradlew listDevices --info
```

### Java nicht gefunden

```bash
java -version      # sollte 21.x anzeigen
where java         # (Windows) Pfad anzeigen
which java         # (macOS) Pfad anzeigen

# macOS: JAVA_HOME setzen
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```
