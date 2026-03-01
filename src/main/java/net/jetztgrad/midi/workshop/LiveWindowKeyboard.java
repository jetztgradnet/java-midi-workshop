package net.jetztgrad.midi.workshop;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LiveWindowKeyboard — play notes in real time via a Swing window.
 *
 * Run with:  ./liveWindow.sh                      ← convenience launcher
 *            ./liveWindow.sh --device=XIAO
 *            ./gradlew liveWindowKeyboard [-Pdevice=XIAO]
 *
 * ──────────────────────────────────────────────────────────────────
 * Keyboard layout  (two-row piano, starting at C4):
 *
 *   Q  W     R  T  Y     I  O  [   ← sharps/flats (black keys)
 *   A  S  D  F  G  H  J  K  L  ;  '  ← white keys (C D E F G A B C D E F)
 *
 *   German QWERTZ equivalents:  Ö = ;   Ä = '   Ü = [
 *   (Swing uses physical key positions, so these work automatically.)
 *
 *   +/=  octave up    -  octave down
 *   1–9/0  select instrument preset (Piano, Organ, Guitar, …)
 *   SPACE  hold for sustain (CC64)    ESC  All Notes Off (panic)
 * ──────────────────────────────────────────────────────────────────
 *
 * Supports true polyphony: multiple keys can be held simultaneously.
 * The note number at key-press time is stored so octave changes while
 * a key is held down never produce stuck notes.
 *
 * A device drop-down at the top of the window lets you switch MIDI
 * output devices without restarting.  An instrument drop-down selects
 * any of the 128 GM patches.  Both combo boxes are mouse-only
 * (non-focusable) so they never intercept note keystrokes.  Pass
 * {@code -Pdevice=<name>} to pre-select a device by partial name.
 */
public class LiveWindowKeyboard {

    static final int DEFAULT_OCTAVE  = 4;
    static final int CHANNEL         = 0;
    static final int BASE_VELOCITY   = 80;
    static final int VELOCITY_JITTER = 15;

    static final String[] NOTE_NAMES = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};

    // VK keyCode → semitone offset from root C of the current octave
    // Bottom row: white keys  C D E F G A B C(+1) D(+1) E(+1) F(+1)
    // Top row:    black keys  C# D# F# G# A#  C#(+1) D#(+1) F#(+1)
    // VK codes are physical-key-position-based, so QWERTY and QWERTZ
    // map to the same VK constants (VK_Z / VK_Y both present for A#).
    // VK_SEMICOLON = ; on US = Ö on QWERTZ (same physical key).
    // VK_QUOTE     = ' on US = Ä on QWERTZ.
    // VK_OPEN_BRACKET = [ on US = Ü on QWERTZ.
    static final Map<Integer, Integer> KEYCODE_TO_OFFSET = Map.ofEntries(
        Map.entry(KeyEvent.VK_A,            0), Map.entry(KeyEvent.VK_S,           2),
        Map.entry(KeyEvent.VK_D,            4), Map.entry(KeyEvent.VK_F,           5),
        Map.entry(KeyEvent.VK_G,            7), Map.entry(KeyEvent.VK_H,           9),
        Map.entry(KeyEvent.VK_J,           11), Map.entry(KeyEvent.VK_K,          12),
        Map.entry(KeyEvent.VK_L,           14), Map.entry(KeyEvent.VK_SEMICOLON,  16),  // E(+1) ; / Ö
        Map.entry(KeyEvent.VK_QUOTE,       17),                                           // F(+1) ' / Ä
        Map.entry(KeyEvent.VK_Q,            1), Map.entry(KeyEvent.VK_W,           3),
        Map.entry(KeyEvent.VK_R,            6), Map.entry(KeyEvent.VK_T,           8),
        Map.entry(KeyEvent.VK_Y,           10), Map.entry(KeyEvent.VK_Z,          10),
        Map.entry(KeyEvent.VK_I,           13), Map.entry(KeyEvent.VK_O,          15),
        Map.entry(KeyEvent.VK_OPEN_BRACKET,18)                                            // F#(+1) [ / Ü
    );

    // State
    static volatile int     octave         = DEFAULT_OCTAVE;
    static volatile int     currentProgram = 0;
    static volatile boolean sustain        = false;
    // keyCode → note number at press time (ensures correct NOTE_OFF under octave changes)
    static final ConcurrentHashMap<Integer, Integer> pressedNotes = new ConcurrentHashMap<>();

    static volatile Receiver receiver;
    static JLabel octaveLabel;
    static JComboBox<String> patchCombo;  // static so handleKeyTyped can sync it

    public static void main(String[] args) {
        String[] finalArgs = args;
        SwingUtilities.invokeLater(() -> {
            try {
                buildAndShowGui(finalArgs);
            } catch (Exception e) {
                System.err.println("Error starting GUI: " + e.getMessage());
                System.exit(1);
            }
        });
    }

    // ── GUI setup ────────────────────────────────────────────────────────────

    static void buildAndShowGui(String[] args) throws Exception {
        // Collect all MIDI output devices
        List<MidiDevice.Info> outputInfos = new ArrayList<>();
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            MidiDevice dev = MidiSystem.getMidiDevice(info);
            if (dev.getMaxReceivers() != 0) {
                outputInfos.add(info);
            }
        }

        if (outputInfos.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "No MIDI output devices found.\nRun './gradlew listDevices' to check your setup.",
                    "No MIDI Devices", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Determine initial selection (honour --device= if supplied)
        String filter = DeviceUtil.deviceFilter(args);
        int selectedIndex = 0;
        if (filter != null) {
            for (int i = 0; i < outputInfos.size(); i++) {
                if (outputInfos.get(i).getName().toLowerCase().contains(filter.toLowerCase())) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        // Open initial receiver
        int initialPatch = DeviceUtil.patchFilter(args);
        currentProgram = initialPatch;  // set before listeners are registered
        receiver = openDevice(outputInfos.get(selectedIndex));
        DeviceUtil.sendProgramChange(receiver, CHANNEL, initialPatch);

        // Device selector combo — non-focusable so it never intercepts note keys.
        // Mouse interaction (click to open, click item to select) still works fully.
        String[] deviceNames = outputInfos.stream()
                .map(MidiDevice.Info::getName).toArray(String[]::new);
        JComboBox<String> deviceCombo = new JComboBox<>(deviceNames);
        deviceCombo.setSelectedIndex(selectedIndex);
        deviceCombo.setFocusable(false);
        deviceCombo.addActionListener(e -> {
            int idx = deviceCombo.getSelectedIndex();
            if (idx < 0) return;
            panicAllNotes();
            try {
                if (receiver != null) receiver.close();
                receiver = openDevice(outputInfos.get(idx));
                DeviceUtil.sendProgramChange(receiver, CHANNEL, currentProgram);
                System.out.println("Switched to: " + outputInfos.get(idx).getName());
            } catch (Exception ex) {
                System.err.println("Could not open device: " + ex.getMessage());
            }
        });

        // ── Frame layout ─────────────────────────────────────────────────────

        JFrame frame = new JFrame("Live MIDI Keyboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(620, 340);
        frame.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Top: device selector + instrument selector (two rows)
        JPanel topPanel  = new JPanel(new GridLayout(2, 1, 0, 4));
        JPanel deviceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        deviceRow.add(new JLabel("Output device:"));
        deviceCombo.setPreferredSize(new Dimension(320, deviceCombo.getPreferredSize().height));
        deviceRow.add(deviceCombo);

        patchCombo = new JComboBox<>(DeviceUtil.gmPatchList());
        patchCombo.setSelectedIndex(initialPatch);
        patchCombo.setFocusable(false);
        patchCombo.setPreferredSize(new Dimension(320, patchCombo.getPreferredSize().height));
        patchCombo.addActionListener(e -> {
            int idx = patchCombo.getSelectedIndex();
            if (idx < 0 || idx == currentProgram) return;  // skip programmatic updates
            panicAllNotes();
            currentProgram = idx;
            DeviceUtil.sendProgramChange(receiver, CHANNEL, idx);
            System.out.println("Instrument: " + DeviceUtil.GM_PATCH_NAMES[idx] + " (patch " + idx + ")");
        });

        JPanel patchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        patchRow.add(new JLabel("Instrument:"));
        patchRow.add(patchCombo);

        topPanel.add(deviceRow);
        topPanel.add(patchRow);
        content.add(topPanel, BorderLayout.NORTH);

        // Centre: keyboard layout reference
        String layoutText =
                "  Keyboard layout:\n\n" +
                "    Q  W     R  T  Y     I  O  [    ← sharps / flats\n" +
                "    A  S  D  F  G  H  J  K  L  ;  '  ← white keys\n" +
                "    C  D  E  F  G  A  B  C  D  E  F   (in current octave)\n" +
                "    German QWERTZ: Ö=;   Ä='   Ü=[\n\n" +
                "  +/=  octave up   |   −  octave down\n" +
                "  1–9/0  select instrument (Piano/Organ/Guitar/…)\n" +
                "  SPACE  hold for sustain (CC64)   |   ESC  All Notes Off";
        JTextArea layoutArea = new JTextArea(layoutText);
        layoutArea.setEditable(false);
        layoutArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        layoutArea.setBackground(content.getBackground());
        layoutArea.setFocusable(false);
        content.add(layoutArea, BorderLayout.CENTER);

        // Bottom: octave controls + quit button
        octaveLabel = new JLabel(octaveText(), SwingConstants.CENTER);
        octaveLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));

        JButton octDownBtn = new JButton("Oct −");
        octDownBtn.setFocusable(false);   // keeps key focus away while playing
        octDownBtn.addActionListener(e -> {
            if (octave > 0) { octave--; updateOctaveLabel(); }
        });

        JButton octUpBtn = new JButton("Oct +");
        octUpBtn.setFocusable(false);
        octUpBtn.addActionListener(e -> {
            if (octave < 8) { octave++; updateOctaveLabel(); }
        });

        JButton quitButton = new JButton("Quit");
        quitButton.setFocusable(false);
        quitButton.addActionListener(e ->
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));

        JPanel octCtrl = new JPanel(new BorderLayout(4, 0));
        octCtrl.add(octDownBtn, BorderLayout.WEST);
        octCtrl.add(octaveLabel, BorderLayout.CENTER);
        octCtrl.add(octUpBtn,   BorderLayout.EAST);

        JPanel southPanel = new JPanel(new BorderLayout(12, 0));
        southPanel.add(octCtrl,    BorderLayout.CENTER);
        southPanel.add(quitButton, BorderLayout.EAST);
        content.add(southPanel, BorderLayout.SOUTH);

        frame.setContentPane(content);

        // ── Global key dispatcher ─────────────────────────────────────────────
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(event -> {
                    int id = event.getID();
                    if (id == KeyEvent.KEY_PRESSED)       handleKeyPressed(event.getKeyCode());
                    else if (id == KeyEvent.KEY_RELEASED) handleKeyReleased(event.getKeyCode());
                    else if (id == KeyEvent.KEY_TYPED)    handleKeyTyped(event.getKeyChar());
                    return false; // let normal Swing processing continue
                });

        // ── Window listener ───────────────────────────────────────────────────
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panicAllNotes();
                if (receiver != null) receiver.close();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // Auto-panic when the window loses focus (prevents stuck notes)
                panicAllNotes();
            }
        });

        frame.setVisible(true);
        System.out.println("Live Keyboard ready — click the window and start playing!");
        System.out.println(octaveText());
    }

    // ── Key event handlers ───────────────────────────────────────────────────

    static void handleKeyPressed(int keyCode) {
        if (pressedNotes.containsKey(keyCode)) return; // suppress key-repeat

        switch (keyCode) {
            case KeyEvent.VK_ESCAPE -> { panicAllNotes(); return; }
            case KeyEvent.VK_ADD -> {           // numpad + (no KEY_TYPED on most systems)
                if (octave < 8) { octave++; updateOctaveLabel(); }
                return;
            }
            case KeyEvent.VK_SUBTRACT -> {      // numpad −
                if (octave > 0) { octave--; updateOctaveLabel(); }
                return;
            }
            case KeyEvent.VK_SPACE -> {
                if (!sustain) {
                    sustain = true;
                    sendCC(CHANNEL, 64, 127);
                    System.out.println("  [Sustain ON]");
                }
                return;
            }
        }

        Integer offset = KEYCODE_TO_OFFSET.get(keyCode);
        if (offset == null) return;

        int note = 12 * (octave + 1) + offset;
        if (note < 0 || note > 127) return;

        int velocity = BASE_VELOCITY + (int)(Math.random() * VELOCITY_JITTER * 2) - VELOCITY_JITTER;
        velocity = Math.max(1, Math.min(127, velocity));

        try {
            receiver.send(new ShortMessage(ShortMessage.NOTE_ON, CHANNEL, note, velocity), -1);
            pressedNotes.put(keyCode, note);
            System.out.printf("  NoteOn  %-3s%-2d  (note %3d, vel %3d)%n",
                    NOTE_NAMES[note % 12], note / 12 - 1, note, velocity);
        } catch (Exception e) {
            System.err.println("Error sending NoteOn: " + e.getMessage());
        }
    }

    static void handleKeyReleased(int keyCode) {
        if (keyCode == KeyEvent.VK_SPACE && sustain) {
            sustain = false;
            sendCC(CHANNEL, 64, 0);
            System.out.println("  [Sustain OFF]");
            return;
        }

        Integer note = pressedNotes.remove(keyCode);
        if (note == null) return;

        try {
            receiver.send(new ShortMessage(ShortMessage.NOTE_OFF, CHANNEL, note, 0), -1);
            System.out.printf("  NoteOff %-3s%-2d  (note %3d)%n",
                    NOTE_NAMES[note % 12], note / 12 - 1, note);
        } catch (Exception e) {
            System.err.println("Error sending NoteOff: " + e.getMessage());
        }
    }

    // KEY_TYPED carries the actual typed character regardless of keyboard layout.
    // Use it for octave shifts so + and − work on QWERTY, QWERTZ, and others.
    // (On QWERTZ the + key is at the ] position → VK_CLOSE_BRACKET, not VK_PLUS.)
    static void handleKeyTyped(char ch) {
        if (ch == '+' || ch == '=') {
            if (octave < 8) { octave++; updateOctaveLabel(); }
        } else if (ch == '-') {
            if (octave > 0) { octave--; updateOctaveLabel(); }
        } else if (ch >= '1' && ch <= '9') { selectPreset(ch - '1'); }
        else if (ch == '0')                { selectPreset(9); }
    }

    // ── Panic / helpers ──────────────────────────────────────────────────────

    static void selectPreset(int index) {
        int program = DeviceUtil.PRESET_PROGRAMS[index];
        panicAllNotes();
        currentProgram = program;
        DeviceUtil.sendProgramChange(receiver, CHANNEL, program);
        System.out.println("  Instrument: " + DeviceUtil.PRESET_NAMES[index] + " (patch " + program + ")");
        // Sync combo box; ActionListener sees currentProgram == idx and returns early
        if (patchCombo != null)
            SwingUtilities.invokeLater(() -> patchCombo.setSelectedIndex(program));
    }

    static void panicAllNotes() {
        if (!pressedNotes.isEmpty() || sustain) {
            System.out.println("  [Panic] All Notes Off");
        }
        DeviceUtil.sendPanic(receiver);
        pressedNotes.clear();
        sustain = false;
    }

    static void sendCC(int channel, int controller, int value) {
        try {
            if (receiver != null) {
                receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value), -1);
            }
        } catch (Exception ignored) {}
    }

    static void updateOctaveLabel() {
        String text = octaveText();
        if (octaveLabel != null) SwingUtilities.invokeLater(() -> octaveLabel.setText(text));
        System.out.println("  " + text);
    }

    static String octaveText() {
        return "Octave: " + octave + "   (C" + octave + " = note " + (12 * (octave + 1)) + ")";
    }

    static Receiver openDevice(MidiDevice.Info info) throws Exception {
        MidiDevice device = MidiSystem.getMidiDevice(info);
        device.open();
        System.out.println("Output device: " + info.getName());
        return device.getReceiver();
    }
}
