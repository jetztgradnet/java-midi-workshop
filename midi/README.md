# MIDI Files

This directory contains royalty-free, public-domain MIDI files for use in the workshop.

---

## How to Add Files

Download MIDI files from the sources listed below and place them in this directory.
The `MidiFilePlayer` example automatically picks up the first `.mid` file it finds here.

```bash
./gradlew midiFilePlayer                          # plays first .mid in this dir
./gradlew midiFilePlayer -PmidiFile=midi/file.mid # plays a specific file
```

---

## Recommended Sources

### Mutopia Project — https://www.mutopiaproject.org/
Free sheet music and MIDI files in the public domain or under Creative Commons licenses.
Curated suggestions:

| File to look for | Description | License |
|-----------------|-------------|---------|
| Bach — Minuet in G (BWV Anh. 114) | Short, recognizable melody | Public domain |
| Bach — Prelude in C major (WTC I) | Arpeggiated chords, single track | Public domain |
| Beethoven — Ode to Joy theme | Simple melody, widely known | Public domain |
| Handel — Hallelujah (excerpt) | Multi-track with chords | Public domain |

Search on Mutopia: https://www.mutopiaproject.org/cgibin/make-table.cgi?searchingfor=midi

### Kunst der Fuge — https://www.kunstderfuge.com/
Large collection of public-domain classical MIDI files.

### FreeMidi.org — https://freemidi.org/
Mixed licensing — verify each file is public domain or CC-licensed before distributing.

---

## Files in This Directory

| Filename | Composer | Title | Source | License |
|----------|---------|-------|--------|---------|
| bach_minuet_g.mid | J.S. Bach | Minuet in G, BWV Anh. 114 | Mutopia | Public Domain |
| bach_prelude_c.mid | J.S. Bach | Prelude in C, WTC I | Mutopia | Public Domain |
| ode_to_joy.mid | L.v. Beethoven | Ode to Joy (theme) | Mutopia | Public Domain |

---

## License Note

All MIDI files committed to this repository must be either:
- **Public domain** (composer died > 70 years ago and the arrangement/encoding is original or also public domain), or
- **Creative Commons** with a license compatible with workshop redistribution (CC0, CC BY, CC BY-SA)

Do **not** commit files from sites that do not clearly state the license.
