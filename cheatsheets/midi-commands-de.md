# MIDI-Befehle Spickzettel

## Nachrichtenaufbau

Jede MIDI-Nachricht besteht aus 1–3 Bytes:

```
Byte 1 (Status):  [ Befehl (4 Bit) | Kanal (4 Bit) ]
Byte 2 (Data 1):  erster Datenwert  (0–127)
Byte 3 (Data 2):  zweiter Datenwert (0–127)  — bei manchen Nachrichten weggelassen
```

**Kanal** im Status-Byte: 0–15 (entspricht MIDI-Kanäle 1–16).

---

## Wichtige Nachrichtentypen

| Nachricht | Status | Data 1 | Data 2 | Hinweis |
|-----------|--------|--------|--------|---------|
| NOTE_ON | `0x9n` | Note (0–127) | Velocity (1–127) | Velocity 0 = Note off |
| NOTE_OFF | `0x8n` | Note (0–127) | Velocity (0–127) | üblicherweise Velocity 0 |
| PROGRAM_CHANGE | `0xCn` | Patch (0–127) | — | Instrument wählen |
| CONTROL_CHANGE | `0xBn` | Controller (0–127) | Wert (0–127) | siehe CC-Tabelle |
| PITCH_BEND | `0xEn` | LSB (0–127) | MSB (0–127) | Mitte = 0x40 0x00 |
| AFTERTOUCH | `0xAn` | Note (0–127) | Druck (0–127) | pro Note |
| CHANNEL_PRESSURE | `0xDn` | Druck (0–127) | — | kanalweit |
| SYSEX START | `0xF0` | Hersteller-ID | … Daten … | endet mit 0xF7 |

`n` = Kanal 0–15.  Beispiel: `0x90` = NOTE_ON auf Kanal 1.

---

## Notenummerntabelle

```
      C     C#    D     D#    E     F     F#    G     G#    A     A#    H
C0:   0     1     2     3     4     5     6     7     8     9     10    11
C1:  12    13    14    15    16    17    18    19    20    21    22    23
C2:  24    25    26    27    28    29    30    31    32    33    34    35
C3:  36    37    38    39    40    41    42    43    44    45    46    47
C4:  60    61    62    63    64    65    66    67    68    69    70    71   ← mittleres C = 60
C5:  72    73    74    75    76    77    78    79    80    81    82    83
C6:  84    85    86    87    88    89    90    91    92    93    94    95
C7:  96    97    98    99   100   101   102   103   104   105   106   107
C8: 108   109   110   111   112   113   114   115   116   117   118   119
```

- Mittleres C = **C4 = MIDI-Notenummer 60**
- Ein Halbton = +1 oder −1
- Eine Oktave = +12 oder −12

---

## Velocity-Referenz

| Wert | Dynamik | Beschreibung |
|------|---------|-------------|
| 0 | — | Note aus (wenn in NOTE_ON gesendet) |
| 1–15 | ppp | Sehr leise |
| 16–31 | pp | Leise |
| 32–47 | p | Leise |
| 48–63 | mp | Halbleise |
| 64–79 | mf | Mittel |
| 80–95 | f | Laut |
| 96–111 | ff | Sehr laut |
| 112–127 | fff | Maximum |

---

## GM-Kanal-10-Schlagzeugbelegung (Auswahl)

Kanal 10 (Index 9) — Notenummern → Schlagzeugklänge:

| Note | Klang | Note | Klang |
|------|-------|------|-------|
| 35 | Acoustic Bass Drum | 51 | Ride Cymbal 1 |
| **36** | **Bass Drum 1 (Kick)** | 52 | Chinese Cymbal |
| 37 | Side Stick | 53 | Ride Bell |
| **38** | **Acoustic Snare** | 54 | Tamburin |
| 39 | Hand Clap | 55 | Splash Cymbal |
| 40 | Electric Snare | 56 | Cowbell |
| **42** | **Closed Hi-Hat** | **57** | Crash Cymbal 2 |
| 43 | High Floor Tom | 59 | Ride Cymbal 2 |
| **46** | **Open Hi-Hat** | 60 | High Bongo |
| **49** | **Crash Cymbal 1** | 64 | Low Conga |

---

## GM-Patch-Liste (alle 128)

### Klavier (0–7)
0 Acoustic Grand Piano · 1 Bright Acoustic Piano · 2 Electric Grand Piano · 3 Honky-tonk Piano · 4 Electric Piano 1 · 5 Electric Piano 2 · 6 Cembalo · 7 Clavinet

### Chromatisches Schlagwerk (8–15)
8 Celesta · 9 Glockenspiel · 10 Music Box · 11 Vibraphon · 12 Marimba · 13 Xylophon · 14 Röhrenglocken · 15 Dulcimer

### Orgel (16–23)
16 Drawbar-Orgel · 17 Perkussive Orgel · 18 Rock-Orgel · 19 Kirchenorgel · 20 Reed-Orgel · 21 Akkordeon · 22 Mundharmonika · 23 Tango-Akkordeon

### Gitarre (24–31)
24 Nylonsaiten-Gitarre · 25 Stahlsaiten-Gitarre · 26 Jazz-Gitarre · 27 Clean E-Gitarre · 28 Muted E-Gitarre · 29 Overdriven Gitarre · 30 Distortion Gitarre · 31 Gitarren-Harmonics

### Bass (32–39)
32 Akustischer Bass · 33 E-Bass (fingergespielt) · 34 E-Bass (Pick) · 35 Fretless Bass · 36 Slap Bass 1 · 37 Slap Bass 2 · 38 Synth Bass 1 · 39 Synth Bass 2

### Streicher (40–47)
**40 Violine** · 41 Viola · 42 Violoncello · 43 Kontrabass · 44 Tremolo-Streicher · 45 Pizzicato-Streicher · 46 Konzertharfe · 47 Pauke

### Ensemble (48–55)
48 Streichquartett 1 · 49 Streichquartett 2 · 50 Synth-Streicher 1 · 51 Synth-Streicher 2 · 52 Chor Aah · 53 Chor Ooh · 54 Synth-Stimme · 55 Orchestra Hit

### Blechbläser (56–63)
**56 Trompete** · 57 Posaune · 58 Tuba · 59 Gedämpfte Trompete · 60 Waldhorn · 61 Blechbläsersatz · 62 Synth-Brass 1 · 63 Synth-Brass 2

### Holzbläser — Reed (64–71)
64 Sopransaxophon · 65 Altsaxophon · 66 Tenorsaxophon · 67 Baritonsaxophon · 68 Oboe · 69 Englischhorn · 70 Fagott · 71 Klarinette

### Holzbläser — Pipe (72–79)
72 Piccolo · **73 Flöte** · 74 Blockflöte · 75 Panflöte · 76 Blown Bottle · 77 Shakuhachi · 78 Pfeife · 79 Okarina

### Synth Lead (80–87)
80 Square Lead · 81 Sägezahn Lead · 82 Calliope Lead · 83 Chiff Lead · 84 Charang Lead · 85 Stimme Lead · 86 Fifth Lead · 87 Bass+Lead

### Synth Pad (88–95)
88 New Age Pad · 89 Warm Pad · 90 Polysynth Pad · 91 Chor Pad · 92 Bowed Pad · 93 Metallic Pad · 94 Halo Pad · 95 Sweep Pad

### Synth-Effekte (96–103)
96 Regen · 97 Soundtrack · 98 Kristall · 99 Atmosphäre · 100 Helligkeit · 101 Goblins · 102 Echos · 103 Sci-Fi

### Ethnisch (104–111)
104 Sitar · 105 Banjo · 106 Shamisen · 107 Koto · 108 Kalimba · 109 Dudelsack · 110 Fiddle · 111 Shanai

### Perkussion (112–119)
112 Tinkle Bell · 113 Agogo · 114 Steel Drums · 115 Holzblock · 116 Taiko-Trommel · 117 Melodic Tom · 118 Synth-Drum · 119 Reverse Cymbal

### Soundeffekte (120–127)
120 Gitarren-Bundgeräusch · 121 Atemgeräusch · 122 Meeresrauschen · 123 Vogelzwitschern · 124 Telefon · 125 Hubschrauber · 126 Applaus · 127 Schuss

---

## Wichtige Control-Change-Nummern (CC)

| CC# | Name | Bereich | Standard |
|-----|------|---------|---------|
| 1 | Modulationsrad | 0–127 | 0 |
| 7 | Kanallautstärke | 0–127 | 100 |
| 10 | Pan | 0=links, 64=Mitte, 127=rechts | 64 |
| 11 | Expression | 0–127 | 127 |
| 64 | Haltepedal | 0–63=aus, 64–127=ein | 0 |
| 91 | Hall (Reverb) | 0–127 | — |
| 93 | Chorus | 0–127 | — |
| 123 | Alle Noten aus | 0 | — |

---

## Tempo

Das Tempo wird in MIDI-Dateien als **Mikrosekunden pro Schlag** gespeichert (nicht BPM).

| BPM | µs pro Schlag |
|-----|--------------|
| 60 | 1.000.000 |
| 90 | 666.667 |
| 120 | 500.000 |
| 140 | 428.571 |
| 160 | 375.000 |

Formel: `µs_pro_Schlag = 60.000.000 / BPM`

In Java: `sequencer.setTempoInBPM(120)` übernimmt die Umrechnung automatisch.
