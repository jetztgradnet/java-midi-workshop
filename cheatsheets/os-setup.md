# OS Setup Guide — MIDI on macOS and Windows

## macOS

### Built-in Software Synth (DLS MusicDevice)

macOS ships with a General MIDI software synthesizer. Java uses it automatically — no configuration required. Run `./gradlew listDevices` and you should see it listed.

### Enable the IAC Driver (Virtual MIDI Port)

The IAC (Inter-Application Communication) Driver creates virtual MIDI ports so apps can talk to each other (e.g. Java → GarageBand).

1. Open **Audio MIDI Setup**
   - Press `⌘ Space`, type "Audio MIDI Setup", press Enter
   - Or: `Applications → Utilities → Audio MIDI Setup`
2. Open the **MIDI Studio** window
   - Menu: `Window → Show MIDI Studio`
3. Double-click **IAC Driver**
4. Check **"Device is online"**
5. Click **Apply**

After this step, `./gradlew listDevices` shows an **IAC Bus** device.

### GarageBand as a Synth

1. Open GarageBand
2. Create a new project with a Software Instrument track
3. Make sure the IAC Driver is online (see above)
4. Java apps send MIDI to the IAC Bus → GarageBand receives and plays it

### Useful Terminal Commands

```bash
# List CoreMIDI devices (macOS built-in tool — limited output)
system_profiler SPMIDIDataType

# Run workshop list devices
./gradlew listDevices
```

---

## Windows

### Windows 11 — Windows MIDI Services

Windows 11 includes **Windows MIDI Services** with an improved MIDI stack. The classic `Microsoft GS Wavetable Synth` is also still present.

Run `./gradlew listDevices` — you should see at least one MIDI output device.

### Windows 10 — VirtualMIDISynth (Recommended)

The built-in Microsoft GS Wavetable Synth sounds dated. Install VirtualMIDISynth for better quality:

1. **Download VirtualMIDISynth**
   - https://coolsoft.altervista.org/en/virtualmidisynth
   - Free, open source

2. **Download a Soundfont (SF2 file)**
   - **GeneralUser GS** — free, high-quality: https://schristiancollins.com/generaluser.php
   - **FluidR3 GM** — alternative: search "FluidR3_GM.sf2"

3. **Install VirtualMIDISynth** and launch it

4. **Load the soundfont**
   - Click the `+` button in the Soundfonts tab
   - Browse to your downloaded `.sf2` file

5. **Verify**
   - Run `./gradlew listDevices` — "CoolSoft VirtualMIDISynth" appears

### loopMIDI (Virtual MIDI Cable)

To route MIDI between Java and another application (e.g. a DAW):

1. Download **loopMIDI**: https://www.tobias-erichsen.de/software/loopmidi.html
2. Install and run it
3. Click `+` to create a virtual port
4. `./gradlew listDevices` shows the new port

---

## Seeed XIAO MIDI Synthesizer

### Connecting via USB

1. Use a **USB-C cable** to connect the Seeed XIAO to your computer
2. The device is USB class-compliant — **no driver installation needed**
3. macOS and Windows detect it automatically as a MIDI device
4. Run `./gradlew listDevices` — look for "XIAO" in the device list

### Selecting the XIAO in Code

```java
// Find the XIAO receiver by name
for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
    if (info.getName().contains("XIAO")) {
        MidiDevice device = MidiSystem.getMidiDevice(info);
        device.open();
        Receiver receiver = device.getReceiver();
        // Send your MIDI messages here
        receiver.close();
        device.close();
    }
}
```

### Troubleshooting

| Problem | Solution |
|---------|---------|
| Device not showing up | Try a different USB cable; use USB-C port directly |
| Sound is distorted | Check volume level on device |
| Multiple XIAO devices | Names will have index suffix, e.g. "XIAO 1", "XIAO 2" |

---

## Troubleshooting

### "No MIDI devices found"

- **macOS**: Enable IAC Driver (see above) or check that DLS MusicDevice is loaded
- **Windows**: Install VirtualMIDISynth or check Device Manager for MIDI devices
- **All**: Run `./gradlew listDevices` as administrator / with sudo if needed

### Sound is not playing

1. Check system volume is not muted
2. Verify at least one device with `Receiver: yes` is listed
3. On macOS: check that the application is allowed to access audio (`System Settings → Privacy → Microphone` may gate audio output)
4. Try headphones to rule out speaker issues

### Gradle issues

```bash
# Force re-download if wrapper is corrupt
rm -rf ~/.gradle/wrapper/dists/gradle-8*
./gradlew --version

# Run with debug output
./gradlew listDevices --info
```

### Java not found

```bash
java -version      # should show 25.x
which java         # should return a path

# macOS (Homebrew/SDKMAN): reload shell or set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```
