# Tutor-Zeitplan & Sprechernotizen

90-Minuten-Workshop-Leitfaden mit Stichpunkten, Zeitplanung und Fallback-Optionen.

---

## Vor dem Workshop (T−30 Min.)

- Früh ankommen, Projektor aufbauen, Bildschirmverbindung testen
- `slides/slides-de.html` im Browser öffnen (Vollbildmodus: F11 / Cmd+Ctrl+F)
- Terminal im Workshop-Verzeichnis öffnen
- `./gradlew listDevices` und `./gradlew playNote` ausführen — Audio prüfen
- Ein XIAO am Laptop anschließen, Erkennung in `listDevices` bestätigen
- WLAN-Passwort an die Tafel schreiben
- Gedruckte Spickzettel auf Tische oder Ausgabetisch legen
- Haftnotizen und Marker verteilen

---

## Minuten-für-Minuten-Plan

| Uhr | Dauer | Thema | Hinweise |
|-----|-------|-------|----------|
| 0:00 | **10 Min.** | Was ist MIDI? | Den physischen Seeed-XIAO-Synth zeigen. Live vom Terminal eine Note spielen, um die Gruppe sofort zu begeistern. Fragen: „Wer hat schon mal mit MIDI gearbeitet?" |
| 0:10 | **10 Min.** | Java MIDI API | Schlüsselklassen auf den Folien durchgehen. Überblick statt Details — das Ziel ist Orientierung, nicht Auswendiglernen. Erwähnen: javax.sound.midi ist im JDK enthalten (kein Maven nötig). |
| 0:20 | **10 Min.** | ListDevices + PlayNote | Beide Befehle live vorführen. Teilnehmer auffordern, sie auf ihren Laptops auszuführen. **Checkpoint**: Wer sieht keine Geräte? IAC aktivieren (macOS) oder os-setup-de.md (Windows). |
| 0:30 | **15 Min.** | SimpleSequence | Code auf dem Bildschirm durchgehen. PPQ, Ticks, Tempo erklären. Ausführen. Tempo-Zahl live verändern. Fragen: „Welche Note ist 60? Was ist 72?" |
| 0:45 | **15 Min.** | **HANDS-ON: MyMelody** | Auf die Übungsdatei hinweisen. Beim Herumgehen unterstützen. XIAOs verteilen (bis zu 6 Einheiten). Empfehlung: erst TODO 1, dann TODO 2 (Tempo), Bonus als letztes. |
| 1:00 | **5 Min.** | MIDI-Dateien | `midiFilePlayer` ausführen. `midi/`-Verzeichnis zeigen. SMF Typ 0 vs. 1 kurz erwähnen. |
| 1:05 | **5 Min.** | Drums + ProgramChange | `drumPattern` und `programChange` ausführen. Teilnehmer den Unterschied zwischen Patches hören lassen. |
| 1:10 | **10 Min.** | Hardware-Demo | XIAO-Verbindung zeigen. Java-Ausgabe an den XIAO routen demonstrieren. macOS-IAC und Windows-loopMIDI erwähnen (falls noch nicht geschehen). |
| 1:20 | **10 Min.** | Abschluss + Q&A | Ressourcen-Folie zeigen. BLE MIDI als Ausblick erwähnen. Fragen beantworten. Auf Repo und Issues-Seite hinweisen. |

**Gesamt: 90 Minuten**

---

## Wichtige Stichpunkte

### Was ist MIDI? (0:00–0:10)
- MIDI ist ein *Protokoll*, kein Audio — Anweisungen, kein Klang
- Wie Noten vs. eine Aufnahme
- Dieselbe MIDI-Datei klingt auf verschiedenen Synthesizern völlig unterschiedlich
- „Seit 1983 Standard in der Musikproduktion — und immer noch aktuell"

### Java MIDI API (0:10–0:20)
- In jedem JDK eingebaut — `javax.sound.midi` — kein Maven, keine Downloads
- Mentales Modell: Sequence → Track → Events; Sequencer spielt ab
- Receiver = nimmt MIDI an; Transmitter = sendet MIDI
- Der Plattform-Synth funktioniert auf macOS und modernem Windows sofort

### ListDevices + PlayNote (0:20–0:30)
- `listDevices` ist das „Hello World" von MIDI — bestätigt die Umgebung
- `getMaxReceivers() != 0` = Gerät empfängt MIDI (Ausgang/Synthesizer)
- `getMaxTransmitters() != 0` = Gerät sendet MIDI (Eingang/Controller)
- NOTE_ON mit Velocity 0 entspricht NOTE_OFF — Besonderheit der Spezifikation

### SimpleSequence (0:30–0:45)
- PPQ = Pulses Per Quarter-note — die Auflösung der Zeitachse
- 4 Ticks/Beat ist leicht zu berechnen: Beat 1 = 0, Beat 2 = 4, Beat 3 = 8
- Höhere Auflösung (48, 96, 480) für feineres Timing — für heute nicht nötig
- `setTempoInBPM(120)` = 500 ms pro Beat
- Zeigen: BPM-Änderung ändert Geschwindigkeit, nicht Tonhöhe

### Hands-On (0:45–1:00)
- Experimentieren ermutigen — falsche Noten sind OK, so lernt man
- Häufige Stolpersteine:
  - Notenummern eine Oktave daneben (vergessen: +12 für eine Oktave höher)
  - EOT-Tick zu früh (weiter nach hinten verschieben, wenn Melodie abgehackt wird)
  - Vergessen, Zeilen auszukommentieren

### Abschluss (1:20–1:30)
- BLE MIDI: OS koppelt Bluetooth-MIDI-Gerät, Java sieht es als normales MIDI — nur erwähnen
- JFugue: höherwertige API für `"C D E F G"`-Strings
- Die echte Stärke: MIDI mit Java-Logik kombinieren — generative Musik, Spiele, IoT

---

## Falls die Zeit knapp wird (diese Punkte kürzen)

1. **ProgramChange** — erwähnen, nicht vorführen
2. **MidiFilePlayer** — überspringen; Teilnehmer können selbst erkunden
3. **Hardware-Demo** — XIAO kurz zeigen, ohne Code-Walkthrough
4. **BLE MIDI** — immer nur Erwähnung, kein Problem

## Falls zu viel Zeit übrig bleibt (hier beschleunigen)

- ListDevices und PlayNote auf 5 Min. kürzen (keine Zeile-für-Zeile-Erklärung)
- SimpleSequence-Walkthrough auf 8 Min. (Tempo nicht live verändern)

---

## Häufige Probleme

| Symptom | Wahrscheinliche Ursache | Lösung |
|---------|------------------------|--------|
| `Keine MIDI-Geräte gefunden` | macOS: IAC deaktiviert | IAC-Treiber in Audio MIDI Setup aktivieren |
| `Keine MIDI-Geräte gefunden` | Windows: kein Synthesizer | VirtualMIDISynth installieren |
| Kein Ton bei `playNote` | Lautstärke stummgeschaltet | OS-Lautstärke prüfen, nicht nur App |
| `./gradlew: Keine Berechtigung` | Fehlendes Execute-Bit | `chmod +x gradlew` |
| Gradle-Download hängt | Kein Internet | Mobilen Hotspot verwenden oder vorab herunterladen |
| Kompilierfehler | Falsches JDK (< 25) | `java -version` muss 25+ zeigen |
| Note bricht zu früh ab | EOT-Tick zu früh | EOT-Tick hinter letzte Note verschieben |
| XIAO nicht erkannt | Defektes USB-Kabel | Anderes Kabel ausprobieren |
