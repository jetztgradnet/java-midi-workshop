package net.jetztgrad.midi.workshop.push2;

import net.jetztgrad.midi.workshop.DeviceUtil;
import net.jetztgrad.midi.workshop.push2.BandLoop.Member;
import net.jetztgrad.midi.workshop.push2.SweeperBoard.Cell;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Notesweeper — Minesweeper on the Ableton Push 2, where you find a band
 * instead of mines.
 *
 * Run with:  ./notesweeper.sh [--device=<synth>] [--boom=<file.mid>] [--push=<port>] [--display=ffm|wayang]
 *                             [--latency=<ms>]   (built-in synth buffer, default 20)
 *            ./gradlew notesweeper -Pdevice="IAC Bus" -Pboom=midi/boom.mid
 *
 * Ten band members hide on the 8×8 pads. A groove loops (muted) all game long.
 *   • Tap a pad      → reveal it. It plays a note whose pitch is its neighbour
 *                      count (A minor pentatonic: 0 = A, 1 = C, 2 = D …), so
 *                      you can *hear* the numbers. Empty areas ripple open.
 *   • Hold a pad     → invite the member you think hides there (or Shift + tap).
 *     Right: the pad pulses in their colour and they join the groove.
 *     Wrong: a sour honk, and you lose a life.
 *   • Reveal a member by accident → BOOM. They are gone for this round, and
 *     you lose a life.
 *   • Clear the board (or find everyone) to win; lose all 3 lives and the band
 *     breaks up. Press NEW for another round.
 *
 * Jam mode (after a win): the pads become a keyboard over the running groove.
 *   • Each column is a band member you found, in the order you found them (left
 *     to right, at most 8; blown-up members are out) — right under its slot on
 *     the display. Bottom to top: the A minor scale (A B C D E F G A); drum
 *     columns get 8 drum / percussion sounds.
 *   • The encoder above a column sets how loud you play that member (middle =
 *     your pad velocity, plus a boost of JAM_BOOST). The master encoder sets the
 *     background groove for the whole band (it drops to 60 % when the jam starts).
 *   • The button below a column mutes / un-mutes that member's groove part —
 *     drop the lead and play it yourself. PLAY starts / stops the groove.
 *
 * Game logic runs on a single thread: MIDI input and timers are posted to
 * {@link #game}, so no locking is needed.
 */
public class Notesweeper {

    enum State { PLAYING, WON, LOST, JAM }

    static final int  LIVES         = 3;
    static final long LONG_PRESS_MS = 400;
    static final long JAM_DELAY_MS  = 2500;   // after the win fanfare
    static final int  JAM_MASTER    = 76;     // band level in jam mode: 60 % of 127
    static final int  LIVE_UNITY    = 64;     // live level where pad velocity passes unscaled (encoder middle)
    static final int  JAM_BOOST     = 25;     // added to every live note, so you cut through the groove
    static final int  CC_ENCODER_FIRST = 71;  // encoders above the display: CC 71–78
    static final int  CC_MASTER     = 79;     // master encoder
    static final int  PRESSED       = 122;    // default palette light grey — no SysEx needed

    // Game-sound channels (the band uses 0–5 and 9)
    static final int CH_COUNT = 6;     // Celesta — neighbour-count notes
    static final int CH_BOOM  = 7;     // Taiko Drum
    static final int CH_HONK  = 8;     // Baritone Sax
    static final int CRASH    = 49;    // GM drum note

    /** A minor pentatonic; index = neighbour count. */
    static final int[] COUNT_NOTES = { 69, 72, 74, 76, 79, 81, 84, 86, 88 };

    static final Color[] NUMBER_COLORS = {
            Color.BLACK,
            new Color(  0,  80, 255), new Color(  0, 200,   0), new Color(255,   0,   0),
            new Color(130,   0, 255), new Color(201,  60,   0), new Color(  0, 200, 200),
            new Color(255,   0, 150), new Color(255, 255, 255) };
    static final Color HIDDEN_COLOR = new Color(89, 89, 89);    // neutral grey: no hue shared with a number
    static final Color HELD_COLOR   = new Color(255, 230, 120);

    // Pad colours are indices into Push's *default* palette (no SysEx needed):
    // the entry closest to each intended colour. The LCD draws the palette's own
    // RGB for these indices, so screen and pads show the same colour.
    static final int[] NUMBER_PAD = new int[NUMBER_COLORS.length];   // [0] = black
    static final int[] MEMBER_PAD = new int[BandLoop.MEMBERS.size()];
    static final int   HIDDEN_PAD, HELD_PAD;
    static {
        Set<Integer> taken = new HashSet<>();
        for (int n = 1; n < NUMBER_COLORS.length; n++)
            taken.add(NUMBER_PAD[n] = Push2.nearestPaletteIndex(NUMBER_COLORS[n], taken));
        HIDDEN_PAD = Push2.nearestPaletteIndex(HIDDEN_COLOR, taken);
        HELD_PAD   = Push2.nearestPaletteIndex(HELD_COLOR, taken);
        taken.clear();                                               // members: unique among themselves
        for (int m = 0; m < MEMBER_PAD.length; m++)
            taken.add(MEMBER_PAD[m] = Push2.nearestPaletteIndex(BandLoop.MEMBERS.get(m).color(), taken));
    }

    static Color memberColor(int m) { return Push2.paletteColor(MEMBER_PAD[m]); }
    static Color numberColor(int n) { return Push2.paletteColor(NUMBER_PAD[n]); }
    static final Color HIDDEN_LCD = Push2.paletteColor(HIDDEN_PAD);

    final Receiver push, synth;
    final Push2.Display display;          // null → play without the LCD
    final BandLoop band;
    final Sequencer boomPlayer;           // null unless --boom=<file.mid>
    final ScheduledExecutorService game = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "notesweeper");
        t.setDaemon(true);
        return t;
    });
    final Random random = new Random();

    SweeperBoard board;
    State state;
    int lives;
    String status;
    boolean shift;
    int generation;                       // bumped per game, so stale timers do nothing
    final ScheduledFuture<?>[][] held = new ScheduledFuture<?>[Push2.GRID][Push2.GRID];
    final int[][] heldVelocity = new int[Push2.GRID][Push2.GRID];
    final List<Integer> lineup = new ArrayList<>();   // members in the order they joined

    Notesweeper(Receiver push, Receiver synth, Push2.Display display, File boomFile) throws Exception {
        this.push = push;
        this.synth = synth;
        this.display = display;
        this.band = new BandLoop(synth);
        DeviceUtil.sendProgramChange(synth, CH_COUNT, 8);     // Celesta
        DeviceUtil.sendProgramChange(synth, CH_BOOM, 116);    // Taiko Drum
        DeviceUtil.sendProgramChange(synth, CH_HONK, 67);     // Baritone Sax
        boomPlayer = boomFile == null ? null : openBoom(boomFile);
    }

    public static void main(String[] args) throws Exception {
        String port = Push2.portFilter(args);
        Receiver push = DeviceUtil.openNamedReceiver(port);
        var pads = DeviceUtil.openNamedTransmitter(port);
        Receiver synth = Push2.openSynth(args);
        Push2.Display display = Push2.Display.open(args);

        Notesweeper sweeper = new Notesweeper(push, synth, display, boomFilter(args));
        pads.setReceiver(new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof ShortMessage sm) sweeper.game.execute(() -> sweeper.onMidi(sm));
            }
            @Override public void close() {}
        });
        sweeper.game.execute(sweeper::newGame);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sweeper.game.shutdownNow();
            sweeper.band.close();
            if (sweeper.boomPlayer != null) sweeper.boomPlayer.close();
            Push2.clearPads(push);
            Push2.setButton(push, Push2.CC_NEW, 0);
            Push2.setButton(push, Push2.CC_SHIFT, 0);
            sweeper.jamButtonsOff();
            DeviceUtil.sendPanic(synth);
            if (display != null) display.close();
            pads.close();
            System.out.println("\nBye.");
        }));
        System.out.println("Notesweeper — find the band! Press Ctrl+C to quit.");
        Thread.currentThread().join();
    }

    // ── Input ────────────────────────────────────────────────────────────────

    void onMidi(ShortMessage sm) {
        int cmd = sm.getCommand();
        if (cmd == ShortMessage.CONTROL_CHANGE) {
            int cc = sm.getData1();
            boolean pressed = sm.getData2() > 0;
            if (cc == Push2.CC_SHIFT) shift = pressed;
            if (cc == Push2.CC_NEW && pressed) newGame();
            if (state != State.JAM) return;
            if (cc >= CC_ENCODER_FIRST && cc < CC_ENCODER_FIRST + Push2.GRID) {
                turnLevel(cc - CC_ENCODER_FIRST, encoderDelta(sm.getData2()));
            } else if (cc == CC_MASTER) {
                band.setMaster(band.master() + encoderDelta(sm.getData2()));
                redraw();
            } else if (pressed) {
                if (cc == Push2.CC_PLAY) toggleBand();
                if (cc >= Push2.CC_LOWER_FIRST && cc < Push2.CC_LOWER_FIRST + Push2.GRID)
                    toggleTrack(cc - Push2.CC_LOWER_FIRST);
            }
            return;
        }
        if ((cmd != ShortMessage.NOTE_ON && cmd != ShortMessage.NOTE_OFF) || !Push2.isPad(sm.getData1())) return;
        int x = Push2.padX(sm.getData1()), y = Push2.padY(sm.getData1());
        if (cmd == ShortMessage.NOTE_ON && sm.getData2() > 0) padDown(x, y, sm.getData2());
        else padUp(x, y);
    }

    void padDown(int x, int y, int velocity) {
        if (state == State.JAM) { jamDown(x, y, velocity); return; }
        if (state != State.PLAYING) return;
        if (board.isFlagged(x, y)) {                         // say hello to a band member
            Member m = BandLoop.MEMBERS.get(board.memberAt(x, y));
            playNote(m.channel(), m.note(), velocity, 300);
        } else if (board.isRevealed(x, y)) {                 // listen to a number again
            playNote(CH_COUNT, COUNT_NOTES[board.neighbours(x, y)], velocity, 300);
        } else if (board.isHidden(x, y)) {
            if (shift) {
                invite(x, y);
            } else {                                         // tap or hold? decided on release / timeout
                Push2.setPad(push, x, y, HELD_PAD);
                heldVelocity[x][y] = velocity;
                held[x][y] = game.schedule(() -> {
                    held[x][y] = null;
                    invite(x, y);
                }, LONG_PRESS_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    void padUp(int x, int y) {
        if (state == State.JAM) { jamUp(x, y); return; }
        ScheduledFuture<?> pending = held[x][y];
        if (pending == null) return;                         // long press already fired
        pending.cancel(false);
        held[x][y] = null;
        reveal(x, y, heldVelocity[x][y]);
    }

    // ── Moves ────────────────────────────────────────────────────────────────

    void reveal(int x, int y, int velocity) {
        List<List<Cell>> layers = board.reveal(x, y);
        if (board.isExploded(x, y)) {
            explode(x, y, board.memberAt(x, y));
            if (state == State.PLAYING && board.allMembersFound()) win();
        } else {
            ripple(layers, velocity);
            if (board.isCleared()) win();
        }
        redraw();
    }

    void invite(int x, int y) {
        if (!board.isPlaced()) {
            Push2.setPad(push, x, y, HIDDEN_PAD);
            status = "Reveal a pad first — the band is still hiding.";
        } else if (board.flag(x, y)) {
            int m = board.memberAt(x, y);
            Member member = BandLoop.MEMBERS.get(m);
            showMember(x, y, m);
            band.join(m);
            lineup.add(m);
            playNote(member.channel(), member.note(), 110, 300);
            status = member.name() + " joined the band!";
            if (board.allMembersFound()) win();
        } else {
            Push2.animatePad(push, x, y, Push2.RED, HIDDEN_PAD, Push2.ANIM_FADE);
            playNote(CH_HONK, 40, 120, 350);
            playNote(CH_HONK, 41, 120, 350);
            status = "Nobody there — sour note!";
            loseLife();
        }
        redraw();
    }

    void explode(int x, int y, int m) {
        Member member = BandLoop.MEMBERS.get(m);
        Push2.animatePad(push, x, y, Push2.BLACK, Push2.RED, Push2.ANIM_BLINK);
        playNote(BandLoop.DRUMS, CRASH, 127, 1500);
        playNote(CH_BOOM, 36, 127, 1200);
        playNote(CH_BOOM, 31, 127, 1200);
        int gen = generation;
        for (int i = 0; i < 8; i++) {                        // the instrument goes haywire
            int note = member.channel() == BandLoop.DRUMS ? 35 + random.nextInt(22) : member.note() - 12 + random.nextInt(25);
            game.schedule(() -> { if (gen == generation) playNote(member.channel(), note, 100, 80); },
                    60L * i, TimeUnit.MILLISECONDS);
        }
        if (boomPlayer != null) {
            boomPlayer.setTickPosition(0);
            boomPlayer.start();
        }
        status = "BOOM! You blew up the " + member.name() + ".";
        loseLife();
    }

    /** Lights newly revealed cells layer by layer, with a rising pentatonic run. */
    void ripple(List<List<Cell>> layers, int velocity) {
        int gen = generation;
        for (int i = 0; i < layers.size(); i++) {
            List<Cell> layer = layers.get(i);
            int note = layers.size() == 1
                    ? COUNT_NOTES[board.neighbours(layer.get(0).x(), layer.get(0).y())]
                    : COUNT_NOTES[i % COUNT_NOTES.length] + 12 * (i / COUNT_NOTES.length);
            game.schedule(() -> {
                if (gen != generation) return;
                for (Cell c : layer) showCell(c.x(), c.y());
                playNote(CH_COUNT, note, velocity, 250);
            }, 45L * i, TimeUnit.MILLISECONDS);
        }
    }

    void loseLife() {
        if (--lives > 0) return;
        state = State.LOST;
        band.stop();
        for (int m = 0; m < BandLoop.MEMBERS.size(); m++) {  // show who was hiding where
            Cell c = board.find(m);
            if (board.isHidden(c.x(), c.y()))
                Push2.animatePad(push, c.x(), c.y(), Push2.BLACK, MEMBER_PAD[m], Push2.ANIM_BLINK);
        }
        Push2.setButton(push, Push2.CC_NEW, 127, Push2.ANIM_BLINK);
        status = "The band broke up. Press NEW to try again.";
    }

    void win() {
        if (state != State.PLAYING) return;
        state = State.WON;
        for (int m = 0; m < BandLoop.MEMBERS.size(); m++) {  // everyone still hiding comes out
            Cell c = board.find(m);
            if (board.flag(c.x(), c.y())) {
                showMember(c.x(), c.y(), m);
                band.join(m);
                lineup.add(m);
            }
        }
        for (int y = 0; y < Push2.GRID; y++)
            for (int x = 0; x < Push2.GRID; x++)
                if (board.isHidden(x, y)) { board.reveal(x, y); showCell(x, y); }
        int[] fanfare = { 69, 72, 76, 81, 84, 88, 93 };
        int gen = generation;
        for (int i = 0; i < fanfare.length; i++) {
            int note = fanfare[i];
            game.schedule(() -> { if (gen == generation) playNote(CH_COUNT, note, 110, 400); },
                    90L * i, TimeUnit.MILLISECONDS);
        }
        Push2.setButton(push, Push2.CC_NEW, 127, Push2.ANIM_BLINK);
        status = "You built the band!  Get ready to jam …";
        game.schedule(() -> { if (gen == generation) enterJam(); }, JAM_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    void newGame() {
        generation++;
        for (ScheduledFuture<?>[] column : held)
            for (int y = 0; y < column.length; y++)
                if (column[y] != null) { column[y].cancel(false); column[y] = null; }
        DeviceUtil.sendPanic(synth);
        board = new SweeperBoard(BandLoop.MEMBERS.size(), random);
        lineup.clear();
        jamButtonsOff();
        state = State.PLAYING;
        lives = LIVES;
        status = "Tap to reveal · hold (or Shift+tap) to invite";
        for (int y = 0; y < Push2.GRID; y++)
            for (int x = 0; x < Push2.GRID; x++)
                Push2.setPad(push, x, y, HIDDEN_PAD);
        Push2.setButton(push, Push2.CC_NEW, 127);
        Push2.setButton(push, Push2.CC_SHIFT, 127);
        band.start();
        redraw();
    }

    // ── Jam mode ─────────────────────────────────────────────────────────────

    final int[][] sounding = new int[Push2.GRID][Push2.GRID];   // note held per pad (0 = none)
    final int[] liveLevel = new int[BandLoop.MEMBERS.size()];    // 0–127 per member, LIVE_UNITY = 1×
    String lastPlayed = "";

    /** Column x plays this member, or -1 if unused (at most 8 members, in join order). */
    int columnMember(int x) {
        return x < Math.min(lineup.size(), Push2.GRID) ? lineup.get(x) : -1;
    }

    void enterJam() {
        state = State.JAM;
        for (int[] column : sounding) Arrays.fill(column, 0);
        lastPlayed = "";
        Arrays.fill(liveLevel, LIVE_UNITY);
        band.setMaster(JAM_MASTER);
        for (int y = 0; y < Push2.GRID; y++)
            for (int x = 0; x < Push2.GRID; x++)
                showKey(x, y);
        for (int x = 0; x < Push2.GRID; x++) showTrackButton(x);
        showPlayButton();
        Push2.setButton(push, Push2.CC_NEW, 127);
        Push2.setButton(push, Push2.CC_SHIFT, 0);
        status = "Jam! One column per band member, low A at the bottom.";
        redraw();
    }

    void jamDown(int x, int y, int velocity) {
        int m = columnMember(x);
        if (m < 0) return;
        Member member = BandLoop.MEMBERS.get(m);
        int note = member.keys()[y];
        int out = liveVelocity(velocity, liveLevel[m]);
        if (out == 0) return;                                    // turned all the way down
        send(ShortMessage.NOTE_ON, member.liveChannel(), note, out);
        sounding[x][y] = note;
        Push2.setPad(push, x, y, PRESSED);
        lastPlayed = member.name() + (member.channel() == BandLoop.DRUMS ? "" : "  " + DeviceUtil.noteName(note))
                + "  vel " + velocity + " \u2192 " + out;
        redraw();
    }

    void jamUp(int x, int y) {
        int m = columnMember(x);
        if (m < 0 || sounding[x][y] == 0) return;
        send(ShortMessage.NOTE_OFF, BandLoop.MEMBERS.get(m).liveChannel(), sounding[x][y], 0);
        sounding[x][y] = 0;
        showKey(x, y);
    }

    void toggleBand() {
        band.toggle();
        showPlayButton();
        status = band.isRunning() ? "Band is back in." : "Band stopped — you're solo. PLAY to bring them back.";
        redraw();
    }

    void toggleTrack(int x) {
        int m = columnMember(x);
        if (m < 0) return;
        band.setMuted(m, !band.isMuted(m), synth);
        showTrackButton(x);
        status = BandLoop.MEMBERS.get(m).name() + (band.isMuted(m) ? " sits out — your turn." : " plays along again.");
        redraw();
    }

    void turnLevel(int x, int delta) {
        int m = columnMember(x);
        if (m < 0) return;
        liveLevel[m] = Math.max(0, Math.min(127, liveLevel[m] + delta));
        redraw();
    }

    /** Pad velocity × level (LIVE_UNITY = 1×, up to ~2×) + JAM_BOOST, capped at 127; level 0 = silent. */
    static int liveVelocity(int padVelocity, int level) {
        if (level == 0) return 0;
        return Math.min(127, padVelocity * level / LIVE_UNITY + JAM_BOOST);
    }

    /** Encoders send relative steps: 1–63 = right, 127–64 = left (two's complement). */
    static int encoderDelta(int value) {
        return value < 64 ? value : value - 128;
    }

    /** Every pad in a column shows that member's palette entry; unused columns stay dark. */
    void showKey(int x, int y) {
        int m = columnMember(x);
        Push2.setPad(push, x, y, m < 0 ? Push2.BLACK : MEMBER_PAD[m]);
    }

    /** Button below a column: lit while the member's groove part plays, off when muted. */
    void showTrackButton(int x) {
        int m = columnMember(x);
        Push2.setButton(push, Push2.CC_LOWER_FIRST + x, m < 0 || band.isMuted(m) ? 0 : MEMBER_PAD[m]);
    }

    void showPlayButton() {
        Push2.setButton(push, Push2.CC_PLAY, band.isRunning() ? Push2.GREEN : 0);
    }

    void jamButtonsOff() {
        for (int x = 0; x < Push2.GRID; x++) Push2.setButton(push, Push2.CC_LOWER_FIRST + x, 0);
        Push2.setButton(push, Push2.CC_PLAY, 0);
    }

    // ── Pads & sound ─────────────────────────────────────────────────────────

    /** Revealed cell: dark when empty, otherwise the colour of its number. */
    void showCell(int x, int y) {
        Push2.setPad(push, x, y, NUMBER_PAD[board.neighbours(x, y)]);
    }

    /** Found members pulse, so they stand out from the static number colours. */
    void showMember(int x, int y, int m) {
        Push2.animatePad(push, x, y, Push2.BLACK, MEMBER_PAD[m], Push2.ANIM_PULSE);
    }

    void send(int command, int channel, int note, int velocity) {
        try {
            synth.send(new ShortMessage(command, channel, note, velocity), -1);
        } catch (Exception e) {
            System.err.println("MIDI error: " + e.getMessage());
        }
    }

    void playNote(int channel, int note, int velocity, long millis) {
        try {
            synth.send(new ShortMessage(ShortMessage.NOTE_ON, channel, note, velocity), -1);
            game.schedule(() -> {
                try {
                    synth.send(new ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0), -1);
                } catch (Exception ignored) {}
            }, millis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            System.err.println("MIDI error: " + e.getMessage());
        }
    }

    Sequencer openBoom(File file) throws Exception {
        Sequence sequence = MidiSystem.getSequence(file);
        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
        sequencer.getTransmitter().setReceiver(synth);
        sequencer.setSequence(sequence);
        System.out.println("Boom: " + file);
        return sequencer;
    }

    /** Extracts {@code --boom=<file.mid>}, or returns null. */
    static File boomFilter(String[] args) {
        for (String arg : args)
            if (arg.startsWith("--boom=")) return new File(arg.substring("--boom=".length()).trim());
        return null;
    }

    // ── LCD ──────────────────────────────────────────────────────────────────

    static final int CELL = 19, BOARD_X = 8, BOARD_Y = 4, INFO_X = 180;

    void redraw() {
        if (display == null) return;
        if (state == State.JAM) { display.draw(this::drawJam); return; }
        display.draw(g -> {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, Push2.Display.WIDTH, Push2.Display.HEIGHT);
            drawBoard(g);

            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            g.drawString("NOTESWEEPER", INFO_X, 36);
            for (int i = 0; i < LIVES; i++) drawHeart(g, 850 + i * 34, 14, i < lives);

            g.setColor(state == State.LOST ? NUMBER_COLORS[3] : state == State.WON ? NUMBER_COLORS[2] : Color.LIGHT_GRAY);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
            g.drawString(status, INFO_X, 72);

            drawRoster(g);
        });
    }

    void drawBoard(Graphics2D g) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        for (int y = 0; y < Push2.GRID; y++) {
            for (int x = 0; x < Push2.GRID; x++) {
                int px = BOARD_X + x * CELL, py = BOARD_Y + (Push2.GRID - 1 - y) * CELL;   // y = 0 is the bottom row
                int m = board.memberAt(x, y);
                if (board.isFlagged(x, y)) {
                    fillCell(g, px, py, memberColor(m), BandLoop.MEMBERS.get(m).name().substring(0, 1), Color.BLACK);
                } else if (board.isExploded(x, y)) {
                    fillCell(g, px, py, NUMBER_COLORS[3], "X", Color.WHITE);
                } else if (board.isRevealed(x, y)) {
                    int n = board.neighbours(x, y);
                    fillCell(g, px, py, new Color(20, 20, 20), n == 0 ? "" : String.valueOf(n), numberColor(n));
                } else if (state == State.LOST && m != SweeperBoard.EMPTY) {
                    fillCell(g, px, py, HIDDEN_LCD, BandLoop.MEMBERS.get(m).name().substring(0, 1), memberColor(m));
                } else {
                    fillCell(g, px, py, HIDDEN_LCD, "", Color.WHITE);
                }
            }
        }
    }

    static void fillCell(Graphics2D g, int px, int py, Color fill, String label, Color text) {
        g.setColor(fill);
        g.fillRect(px, py, CELL - 2, CELL - 2);
        if (label.isEmpty()) return;
        g.setColor(text);
        int w = g.getFontMetrics().stringWidth(label);
        g.drawString(label, px + (CELL - 2 - w) / 2, py + CELL - 5);
    }

    /** The band: lit when joined, crossed out when blown up, outlined while hiding. */
    void drawRoster(Graphics2D g) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        boolean[] joined = new boolean[BandLoop.MEMBERS.size()], lost = new boolean[BandLoop.MEMBERS.size()];
        if (board.isPlaced()) {
            for (int m = 0; m < joined.length; m++) {
                Cell c = board.find(m);
                joined[m] = board.isFlagged(c.x(), c.y());
                lost[m] = board.isExploded(c.x(), c.y());
            }
        }
        for (int m = 0; m < joined.length; m++) {
            Member member = BandLoop.MEMBERS.get(m);
            int bx = INFO_X + m * 77, by = 96, bw = 70, bh = 50;
            if (joined[m]) {
                g.setColor(memberColor(m));
                g.fillRoundRect(bx, by, bw, bh, 10, 10);
                g.setColor(Color.BLACK);
            } else {
                g.setColor(lost[m] ? new Color(120, 0, 0) : new Color(90, 90, 90));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(bx, by, bw, bh, 10, 10);
                g.setColor(lost[m] ? new Color(200, 60, 60) : new Color(140, 140, 140));
            }
            int w = g.getFontMetrics().stringWidth(member.name());
            g.drawString(member.name(), bx + (bw - w) / 2, by + 30);
            if (lost[m]) {
                g.setColor(NUMBER_COLORS[3]);
                g.drawLine(bx + 6, by + 6, bx + bw - 6, by + bh - 6);
                g.drawLine(bx + bw - 6, by + 6, bx + 6, by + bh - 6);
            }
        }
    }

    /** Jam screen; the bottom strip lines up with the 8 buttons below the display. */
    void drawJam(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, Push2.Display.WIDTH, Push2.Display.HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.drawString("JAM SESSION", 16, 36);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(band.isRunning() ? NUMBER_COLORS[2] : Color.GRAY);
        g.drawString((band.isRunning() ? "\u25B6 band " : "\u25A0 band ") + band.master() * 100 / BandLoop.FULL + " %", 260, 34);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.drawString(lastPlayed, Push2.Display.WIDTH - 16 - g.getFontMetrics().stringWidth(lastPlayed), 36);

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        g.drawString(status, 16, 70);
        g.setColor(Color.GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString("encoders: how loud you play \u00B7 master: band level \u00B7 buttons below: band part on/off \u00B7 PLAY: band \u00B7 NEW: next round",
                16, 96);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        int slot = Push2.Display.WIDTH / Push2.GRID;                 // 120 px per button
        for (int x = 0; x < Push2.GRID; x++) {
            int bx = x * slot + 6, by = 110, bw = slot - 12, bh = 44;
            int m = columnMember(x);
            if (m < 0) {
                g.setColor(new Color(40, 40, 40));
                g.setStroke(new BasicStroke(1));
                g.drawRoundRect(bx, by, bw, bh, 10, 10);
                continue;
            }
            Member member = BandLoop.MEMBERS.get(m);
            boolean muted = band.isMuted(m);
            if (muted) {
                g.setColor(memberColor(m));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(bx, by, bw, bh, 10, 10);
            } else {
                g.setColor(memberColor(m));
                g.fillRoundRect(bx, by, bw, bh, 10, 10);
                g.setColor(Color.BLACK);
            }
            String label = member.name() + (muted ? " (off)" : "");
            int w = g.getFontMetrics().stringWidth(label);
            g.drawString(label, bx + (bw - w) / 2, by + 24);
            // live level bar along the bottom of the slot, with a tick at the middle (1×)
            g.fillRect(bx + 8, by + bh - 10, (bw - 16) * liveLevel[m] / 127, 4);
            g.fillRect(bx + 8 + (bw - 16) / 2, by + bh - 13, 1, 10);
        }
    }

    static void drawHeart(Graphics2D g, int x, int y, boolean full) {
        g.setColor(full ? NUMBER_COLORS[3] : new Color(70, 70, 70));
        g.fillOval(x, y, 14, 14);
        g.fillOval(x + 12, y, 14, 14);
        g.fillPolygon(new Polygon(new int[] { x + 1, x + 25, x + 13 }, new int[] { y + 10, y + 10, y + 24 }, 3));
    }
}
