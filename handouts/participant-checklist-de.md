# Teilnehmer-Checkliste (vor dem Workshop)

Bitte erledige diese Schritte **vor** dem Workshop. Die Einrichtung dauert ca. 10–15 Minuten.

---

## 1. JDK 25 installieren

```bash
java -version
```

Erwartete Ausgabe: `openjdk version "25.x.x" ...`

**JDK 25 installieren (falls noch nicht vorhanden):**
- **macOS** (Homebrew): `brew install temurin@25`
- **macOS** (manuell): https://adoptium.net
- **Windows**: https://adoptium.net → JDK 25 auswählen

Nach der Installation sicherstellen, dass `java` im PATH ist.

---

## 2. Git installieren

```bash
git --version
```

- **macOS**: Bestandteil der Xcode Command Line Tools (`xcode-select --install`)
- **Windows**: https://git-scm.com/download/win

---

## 3. IDE oder Editor wählen

Folgende Optionen funktionieren gut:
- [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/) (empfohlen)
- [VS Code](https://code.visualstudio.com/) mit dem Java Extension Pack
- Eclipse, NetBeans oder ein beliebiger Editor

---

## 4. Repository klonen

```bash
git clone https://github.com/jetztgradnet/java-midi-workshop.git
cd java-midi-workshop
```

---

## 5. MIDI-Einrichtung prüfen

```bash
./gradlew listDevices
```

**Es sollte mindestens ein Gerät mit `Receiver: yes` erscheinen.**

Falls „Keine MIDI-Geräte gefunden" angezeigt wird:

### macOS — IAC-Treiber aktivieren

1. **Audio MIDI Setup** öffnen (`⌘ Leertaste` → „Audio MIDI Setup")
2. `Fenster → MIDI-Studio einblenden`
3. Doppelklick auf **IAC-Treiber**
4. Häkchen bei **„Gerät ist online"** → Übernehmen
5. `./gradlew listDevices` erneut ausführen

### Windows — VirtualMIDISynth installieren

1. Herunterladen: https://coolsoft.altervista.org/de/virtualmidisynth
2. Soundfont herunterladen: https://schristiancollins.com/generaluser.php
3. VirtualMIDISynth installieren und Soundfont laden
4. `./gradlew listDevices` erneut ausführen

---

## 6. Klangausgabe testen

```bash
./gradlew playNote
```

Es sollte eine kurze Klaviernote (mittleres C) zu hören sein. Falls nicht, Lautstärke prüfen und die Fehlerbehebung in `cheatsheets/os-setup-de.md` befolgen.

---

## 7. Empfohlen: Kopfhörer mitbringen

Der Workshop-Raum kann laut sein. Mit Kopfhörern ist die MIDI-Ausgabe besser zu hören.

---

## Schnelle Notenreferenz (für die Übung)

```
C4=60  D4=62  E4=64  F4=65  G4=67  A4=69  H4=71  C5=72
```

Eine Oktave höher = +12 | Ein Halbton höher = +1

Velocity: 1 (leise) → 64 (mittel) → 127 (laut)

---

## Fragen vor dem Workshop?

Issue öffnen bei: **github.com/jetztgradnet/java-midi-workshop/issues**
