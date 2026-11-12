package net.jetztgrad.midi.workshop.push2;

import net.jetztgrad.midi.workshop.DeviceUtil;
import net.jetztgrad.midi.workshop.push2.BandLoop.Member;

import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

/**
 * JamSession — the pads become a keyboard over the running {@link BandLoop} groove.
 * Used by {@link Push2Jam} (standalone) and by {@link Notesweeper} (bonus after a win).
 *
 *   • Each column is one band member from the lineup (left to right, at most 8),
 *     right under its slot on the display. Bottom to top: the A minor scale
 *     (A B C D E F G A); drum columns get 8 drum / percussion sounds.
 *   • The encoder above a column sets how loud you play that member (middle =
 *     your pad velocity, plus a boost of {@link #BOOST}). The master encoder sets
 *     the background groove for the whole band (60 % when the session starts).
 *   • The button below a column switches that member's groove part on / off —
 *     drop the lead and play it yourself. PLAY starts / stops the groove.
 *
 * Not thread-safe: call everything from the owner's single game thread.
 */
final class JamSession {

    static final int MASTER     = 76;     // band level when the session starts: 60 % of 127
    static final int LIVE_UNITY = 64;     // live level where pad velocity passes unscaled (encoder middle)
    static final int BOOST      = 25;     // added to every live note, so you cut through the groove

    private final Receiver push, synth;
    private final Push2.Display display;  // null → no LCD
    private final BandLoop band;
    private final List<Integer> lineup;   // member per column, left to right
    private final String newButtonHint;   // what NEW does in the owning app, or null

    final int[][] sounding = new int[Push2.GRID][Push2.GRID];   // note held per pad (0 = none)
    final int[] liveLevel = new int[BandLoop.MEMBERS.size()];    // 0–127 per member, LIVE_UNITY = 1×
    String lastPlayed = "";
    String status = "";

    JamSession(Receiver push, Receiver synth, Push2.Display display, BandLoop band,
               List<Integer> lineup, String newButtonHint) {
        this.push = push;
        this.synth = synth;
        this.display = display;
        this.band = band;
        this.lineup = List.copyOf(lineup);
        this.newButtonHint = newButtonHint;
    }

    /** Column x plays this member, or -1 if unused (at most 8 members). */
    int columnMember(int x) {
        return x < Math.min(lineup.size(), Push2.GRID) ? lineup.get(x) : -1;
    }

    /** Lights pads and buttons, drops the groove to {@link #MASTER} and draws the screen. */
    void start() {
        for (int[] column : sounding) Arrays.fill(column, 0);
        lastPlayed = "";
        Arrays.fill(liveLevel, LIVE_UNITY);
        band.setMaster(MASTER);
        for (int y = 0; y < Push2.GRID; y++)
            for (int x = 0; x < Push2.GRID; x++)
                showKey(x, y);
        for (int x = 0; x < Push2.GRID; x++) showTrackButton(x);
        showPlayButton();
        status = "Jam! One column per band member, low A at the bottom.";
        redraw();
    }

    // ── Input ────────────────────────────────────────────────────────────────

    /** Pads, encoders, PLAY and the buttons below the display; ignores everything else. */
    void onMidi(ShortMessage sm) {
        int cmd = sm.getCommand();
        if (cmd == ShortMessage.CONTROL_CHANGE) {
            int cc = sm.getData1();
            if (cc >= Push2.CC_ENCODER_FIRST && cc < Push2.CC_ENCODER_FIRST + Push2.GRID) {
                turnLevel(cc - Push2.CC_ENCODER_FIRST, Push2.encoderDelta(sm.getData2()));
            } else if (cc == Push2.CC_MASTER_ENCODER) {
                band.setMaster(band.master() + Push2.encoderDelta(sm.getData2()));
                redraw();
            } else if (sm.getData2() > 0) {
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
        int m = columnMember(x);
        if (m < 0) return;
        Member member = BandLoop.MEMBERS.get(m);
        int note = member.keys()[y];
        int out = liveVelocity(velocity, liveLevel[m]);
        if (out == 0) return;                                    // turned all the way down
        send(ShortMessage.NOTE_ON, member.liveChannel(), note, out);
        sounding[x][y] = note;
        Push2.setPad(push, x, y, Push2.LIGHT_GREY);
        lastPlayed = member.name() + (member.channel() == BandLoop.DRUMS ? "" : "  " + DeviceUtil.noteName(note))
                + "  vel " + velocity + " → " + out;
        redraw();
    }

    void padUp(int x, int y) {
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

    /** Pad velocity × level (LIVE_UNITY = 1×, up to ~2×) + BOOST, capped at 127; level 0 = silent. */
    static int liveVelocity(int padVelocity, int level) {
        if (level == 0) return 0;
        return Math.min(127, padVelocity * level / LIVE_UNITY + BOOST);
    }

    // ── Pads & buttons ───────────────────────────────────────────────────────

    /** Every pad in a column shows that member's colour; unused columns stay dark. */
    void showKey(int x, int y) {
        int m = columnMember(x);
        Push2.setPad(push, x, y, m < 0 ? Push2.BLACK : BandLoop.MEMBER_PAD[m]);
    }

    /** Button below a column: lit while the member's groove part plays, off when muted. */
    void showTrackButton(int x) {
        int m = columnMember(x);
        Push2.setButton(push, Push2.CC_LOWER_FIRST + x, m < 0 || band.isMuted(m) ? 0 : BandLoop.MEMBER_PAD[m]);
    }

    void showPlayButton() {
        Push2.setButton(push, Push2.CC_PLAY, band.isRunning() ? Push2.GREEN : 0);
    }

    /** Turns off the buttons a session lights (pads are left to the owner). */
    static void buttonsOff(Receiver push) {
        for (int x = 0; x < Push2.GRID; x++) Push2.setButton(push, Push2.CC_LOWER_FIRST + x, 0);
        Push2.setButton(push, Push2.CC_PLAY, 0);
    }

    private void send(int command, int channel, int note, int velocity) {
        try {
            synth.send(new ShortMessage(command, channel, note, velocity), -1);
        } catch (Exception e) {
            System.err.println("MIDI error: " + e.getMessage());
        }
    }

    // ── LCD ──────────────────────────────────────────────────────────────────

    void redraw() {
        if (display != null) display.draw(this::draw);
    }

    /** Jam screen; the bottom strip lines up with the 8 buttons below the display. */
    void draw(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, Push2.Display.WIDTH, Push2.Display.HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.drawString("JAM SESSION", 16, 36);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(band.isRunning() ? new Color(0, 200, 0) : Color.GRAY);
        g.drawString((band.isRunning() ? "▶ band " : "■ band ") + band.master() * 100 / BandLoop.FULL + " %", 260, 34);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.drawString(lastPlayed, Push2.Display.WIDTH - 16 - g.getFontMetrics().stringWidth(lastPlayed), 36);

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        g.drawString(status, 16, 70);
        g.setColor(Color.GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString("encoders: how loud you play · master: band level · buttons below: band part on/off · PLAY: band"
                + (newButtonHint == null ? "" : " · NEW: " + newButtonHint), 16, 96);

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
                g.setColor(BandLoop.memberColor(m));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(bx, by, bw, bh, 10, 10);
            } else {
                g.setColor(BandLoop.memberColor(m));
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
}
