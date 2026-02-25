# MIDI Commands Cheat Sheet

## Message Anatomy

Every MIDI message is 1–3 bytes long:

```
Byte 1 (Status):  [ command (4 bits) | channel (4 bits) ]
Byte 2 (Data 1):  first data value  (0–127)
Byte 3 (Data 2):  second data value (0–127)  — omitted for some messages
```

**Channel** in the status byte is 0–15 (MIDI channels 1–16).

---

## Common Message Types

| Message | Status | Data 1 | Data 2 | Notes |
|---------|--------|--------|--------|-------|
| NOTE_ON | `0x9n` | note (0–127) | velocity (1–127) | velocity 0 = note off |
| NOTE_OFF | `0x8n` | note (0–127) | velocity (0–127) | usually velocity 0 |
| PROGRAM_CHANGE | `0xCn` | patch (0–127) | — | selects instrument |
| CONTROL_CHANGE | `0xBn` | controller (0–127) | value (0–127) | see CC table below |
| PITCH_BEND | `0xEn` | LSB (0–127) | MSB (0–127) | center = 0x40 0x00 |
| AFTERTOUCH | `0xAn` | note (0–127) | pressure (0–127) | per-note pressure |
| CHANNEL_PRESSURE | `0xDn` | pressure (0–127) | — | channel-wide pressure |
| SYSEX START | `0xF0` | manufacturer ID | … data … | ends with 0xF7 |

`n` = channel 0–15.  Example: `0x90` = NOTE_ON on channel 1.

---

## Note Number Table

```
      C     C#    D     D#    E     F     F#    G     G#    A     A#    B
C0:   0     1     2     3     4     5     6     7     8     9     10    11
C1:  12    13    14    15    16    17    18    19    20    21    22    23
C2:  24    25    26    27    28    29    30    31    32    33    34    35
C3:  36    37    38    39    40    41    42    43    44    45    46    47
C4:  60    61    62    63    64    65    66    67    68    69    70    71   ← middle C = 60
C5:  72    73    74    75    76    77    78    79    80    81    82    83
C6:  84    85    86    87    88    89    90    91    92    93    94    95
C7:  96    97    98    99   100   101   102   103   104   105   106   107
C8: 108   109   110   111   112   113   114   115   116   117   118   119
```

- Middle C = **C4 = MIDI note 60**
- One semitone = +1 or −1
- One octave = +12 or −12

---

## Velocity Reference

| Value | Dynamic | Description |
|-------|---------|-------------|
| 0 | — | Note off (when sent in NOTE_ON) |
| 1–15 | ppp | Very soft |
| 16–31 | pp | Soft |
| 32–47 | p | Soft |
| 48–63 | mp | Medium soft |
| 64–79 | mf | Medium |
| 80–95 | f | Loud |
| 96–111 | ff | Very loud |
| 112–127 | fff | Maximum |

---

## GM Channel 10 Percussion Map (selection)

Channel 10 (index 9) note numbers → drum sounds:

| Note | Sound | Note | Sound |
|------|-------|------|-------|
| 35 | Acoustic Bass Drum | 51 | Ride Cymbal 1 |
| **36** | **Bass Drum 1 (Kick)** | 52 | Chinese Cymbal |
| 37 | Side Stick | 53 | Ride Bell |
| **38** | **Acoustic Snare** | 54 | Tambourine |
| 39 | Hand Clap | 55 | Splash Cymbal |
| 40 | Electric Snare | 56 | Cowbell |
| 41 | Low Floor Tom | **57** | Crash Cymbal 2 |
| **42** | **Closed Hi-Hat** | 59 | Ride Cymbal 2 |
| 43 | High Floor Tom | 60 | High Bongo |
| 44 | Pedal Hi-Hat | 61 | Low Bongo |
| 45 | Low Tom | 62 | Mute Hi Conga |
| **46** | **Open Hi-Hat** | 63 | Open Hi Conga |
| 47 | Low-Mid Tom | 64 | Low Conga |
| 48 | Hi-Mid Tom | 65 | High Timbale |
| **49** | **Crash Cymbal 1** | 66 | Low Timbale |
| 50 | High Tom | | |

---

## GM Patch List (all 128)

### Piano (0–7)
0 Acoustic Grand Piano · 1 Bright Acoustic Piano · 2 Electric Grand Piano · 3 Honky-tonk Piano · 4 Electric Piano 1 · 5 Electric Piano 2 · 6 Harpsichord · 7 Clavinet

### Chromatic Percussion (8–15)
8 Celesta · 9 Glockenspiel · 10 Music Box · 11 Vibraphone · 12 Marimba · 13 Xylophone · 14 Tubular Bells · 15 Dulcimer

### Organ (16–23)
16 Drawbar Organ · 17 Percussive Organ · 18 Rock Organ · 19 Church Organ · 20 Reed Organ · 21 Accordion · 22 Harmonica · 23 Tango Accordion

### Guitar (24–31)
24 Nylon Guitar · 25 Steel Guitar · 26 Jazz Guitar · 27 Clean Guitar · 28 Muted Guitar · 29 Overdriven Guitar · 30 Distortion Guitar · 31 Guitar Harmonics

### Bass (32–39)
32 Acoustic Bass · 33 Electric Bass (finger) · 34 Electric Bass (pick) · 35 Fretless Bass · 36 Slap Bass 1 · 37 Slap Bass 2 · 38 Synth Bass 1 · 39 Synth Bass 2

### Strings (40–47)
**40 Violin** · 41 Viola · 42 Cello · 43 Contrabass · 44 Tremolo Strings · 45 Pizzicato Strings · 46 Orchestral Harp · 47 Timpani

### Ensemble (48–55)
48 String Ensemble 1 · 49 String Ensemble 2 · 50 Synth Strings 1 · 51 Synth Strings 2 · 52 Choir Aahs · 53 Voice Oohs · 54 Synth Voice · 55 Orchestra Hit

### Brass (56–63)
**56 Trumpet** · 57 Trombone · 58 Tuba · 59 Muted Trumpet · 60 French Horn · 61 Brass Section · 62 Synth Brass 1 · 63 Synth Brass 2

### Reed (64–71)
64 Soprano Sax · 65 Alto Sax · 66 Tenor Sax · 67 Baritone Sax · 68 Oboe · 69 English Horn · 70 Bassoon · 71 Clarinet

### Pipe (72–79)
72 Piccolo · **73 Flute** · 74 Recorder · 75 Pan Flute · 76 Blown Bottle · 77 Shakuhachi · 78 Whistle · 79 Ocarina

### Synth Lead (80–87)
80 Square Lead · 81 Sawtooth Lead · 82 Calliope Lead · 83 Chiff Lead · 84 Charang Lead · 85 Voice Lead · 86 Fifth Lead · 87 Bass+Lead

### Synth Pad (88–95)
88 New Age Pad · 89 Warm Pad · 90 Polysynth Pad · 91 Choir Pad · 92 Bowed Pad · 93 Metallic Pad · 94 Halo Pad · 95 Sweep Pad

### Synth Effects (96–103)
96 Rain · 97 Soundtrack · 98 Crystal · 99 Atmosphere · 100 Brightness · 101 Goblins · 102 Echoes · 103 Sci-fi

### Ethnic (104–111)
104 Sitar · 105 Banjo · 106 Shamisen · 107 Koto · 108 Kalimba · 109 Bag Pipe · 110 Fiddle · 111 Shanai

### Percussive (112–119)
112 Tinkle Bell · 113 Agogo · 114 Steel Drums · 115 Woodblock · 116 Taiko Drum · 117 Melodic Tom · 118 Synth Drum · 119 Reverse Cymbal

### Sound Effects (120–127)
120 Guitar Fret Noise · 121 Breath Noise · 122 Seashore · 123 Bird Tweet · 124 Telephone Ring · 125 Helicopter · 126 Applause · 127 Gunshot

---

## Common Control Change (CC) Numbers

| CC# | Name | Range | Default |
|-----|------|-------|---------|
| 0 | Bank Select MSB | 0–127 | — |
| 1 | Modulation Wheel | 0–127 | 0 |
| 7 | Channel Volume | 0–127 | 100 |
| 10 | Pan | 0=left, 64=center, 127=right | 64 |
| 11 | Expression | 0–127 | 127 |
| 64 | Sustain Pedal | 0–63=off, 64–127=on | 0 |
| 65 | Portamento | 0–63=off, 64–127=on | 0 |
| 91 | Reverb Send | 0–127 | — |
| 93 | Chorus Send | 0–127 | — |
| 121 | Reset All Controllers | 0 | — |
| 123 | All Notes Off | 0 | — |

---

## Tempo

Tempo is stored in MIDI files as **microseconds per beat** (not BPM).

| BPM | µs per beat |
|-----|------------|
| 60 | 1,000,000 |
| 90 | 666,667 |
| 120 | 500,000 |
| 140 | 428,571 |
| 160 | 375,000 |
| 180 | 333,333 |

Formula: `µs_per_beat = 60,000,000 / BPM`

In Java: `sequencer.setTempoInBPM(120)` handles the conversion automatically.
