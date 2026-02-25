# Tutor-Packliste

Alles, was du für einen reibungslosen Workshop mitbringen oder vorbereiten solltest.

---

## Hardware

- [ ] **6× Seeed XIAO MIDI-Synthesizer** — die handlichen Synthesizer für Teilnehmer
- [ ] **6× USB-C-Kabel** — eines pro XIAO-Einheit (jedes Kabel vor der Veranstaltung testen!)
- [ ] **USB-Hub(s)** — mindestens ein 4-Port-Hub; zwei mitbringen, wenn der Raum wenige USB-Ports hat
- [ ] **USB-A → USB-C-Adapter** — 3–4 Stück für Teilnehmer mit älteren Laptops
- [ ] **Kopfhörer** — mindestens 1× für eigene Tests; 3–4 mitbringen, wenn Teilnehmer keine haben
- [ ] **Kleiner portabler Lautsprecher** — für Live-Demos, die der ganze Raum hören kann
- [ ] **Laptop** — mit geklontem Repository und getesteten `./gradlew *`-Tasks
- [ ] **Mehrfachsteckdose / Verlängerungskabel** — unverzichtbar für Geräte mit Strom

---

## Gedruckte Materialien

- [ ] **Spickzettel: MIDI-Befehle** (`cheatsheets/midi-commands-de.md`)
- [ ] **Spickzettel: Java-MIDI-API** (`cheatsheets/java-midi-api-de.md`)
- [ ] **Spickzettel: OS-Einrichtung** (`cheatsheets/os-setup-de.md`)
- [ ] **Teilnehmer-Checkliste** (`handouts/participant-checklist-de.md`) — für Teilnehmer ohne Vorbereitung

Doppelseitig drucken, um Papier zu sparen. 5–10 % Kopien extra einplanen.

---

## Logistik

- [ ] **Haftnotizen + Marker** — für schnelles Hilfe-Signaling (grün = OK, rot = Hilfe nötig)
- [ ] **Projektor / Leinwandzugang** — vorab mit Veranstaltungsort klären
- [ ] **HDMI / Display-Adapter** — zum Laptop-Ausgang passend (USB-C, Mini DP, etc.)
- [ ] **WLAN-Zugangsdaten** — zu Beginn an die Tafel schreiben

---

## Digitale Vorbereitung (Tag zuvor)

- [ ] Repository aktualisieren: `git pull origin main`
- [ ] Alle Workshop-Tasks testen und Audioausgabe prüfen:
  ```bash
  ./gradlew listDevices
  ./gradlew playNote
  ./gradlew simpleSequence
  ./gradlew midiFilePlayer
  ./gradlew drumPattern
  ./gradlew programChange
  ./gradlew myMelody
  ```
- [ ] `slides/slides.html` im Browser öffnen — Folien prüfen
- [ ] Alle 6 XIAO-Einheiten per USB testen — jede muss in `listDevices` erscheinen
- [ ] XIAOs laden, falls sie interne Akkus haben

---

## Checkliste am Veranstaltungstag (30 Min. früher ankommen)

- [ ] Projektor einrichten und Folien öffnen
- [ ] Ein XIAO am Laptop anschließen und Funktion bestätigen
- [ ] WLAN-Passwort an die Tafel schreiben
- [ ] Haftnotizen und Marker auf die Tische legen
- [ ] Gedruckte Spickzettel auf Plätze oder Ausgabetisch legen
- [ ] Terminal mit geöffnetem Workshop-Verzeichnis bereithalten
