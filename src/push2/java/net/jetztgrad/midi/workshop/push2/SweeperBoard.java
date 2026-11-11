package net.jetztgrad.midi.workshop.push2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * SweeperBoard — the Minesweeper rules, with no MIDI or graphics.
 *
 * Instead of mines, band members hide on the board: member i (0 … members-1)
 * sits on exactly one cell. Coordinates match the Push pads: x = 0 (left) … 7,
 * y = 0 (bottom) … 7.
 *
 * A cell is in one of four states:
 *   hidden   — not touched yet
 *   revealed — opened, safe (shows its neighbour count)
 *   flagged  — a member found by flagging (flags are only kept when correct)
 *   exploded — a member hit by revealing it
 */
final class SweeperBoard {

    record Cell(int x, int y) {}

    static final int SIZE = Push2.GRID;
    static final int EMPTY = -1;

    private final int members;
    private final Random random;
    private final int[][] member        = new int[SIZE][SIZE];
    private final boolean[][] revealed  = new boolean[SIZE][SIZE];
    private final boolean[][] flagged   = new boolean[SIZE][SIZE];
    private final boolean[][] exploded  = new boolean[SIZE][SIZE];
    private boolean placed;

    SweeperBoard(int members, Random random) {
        this.members = members;
        this.random = random;
        for (int[] column : member) Arrays.fill(column, EMPTY);
    }

    /** Members are placed on the first reveal, never on or next to that cell. */
    private void place(int safeX, int safeY) {
        List<Cell> spots = new ArrayList<>();
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                if (Math.abs(x - safeX) > 1 || Math.abs(y - safeY) > 1)
                    spots.add(new Cell(x, y));
        Collections.shuffle(spots, random);
        for (int i = 0; i < members; i++)
            member[spots.get(i).x()][spots.get(i).y()] = i;
        placed = true;
    }

    boolean isPlaced()                 { return placed; }
    int memberAt(int x, int y)         { return member[x][y]; }
    boolean isRevealed(int x, int y)   { return revealed[x][y]; }
    boolean isFlagged(int x, int y)    { return flagged[x][y]; }
    boolean isExploded(int x, int y)   { return exploded[x][y]; }
    boolean isHidden(int x, int y)     { return !revealed[x][y] && !flagged[x][y] && !exploded[x][y]; }

    /** Number of members on the (up to 8) neighbouring cells. */
    int neighbours(int x, int y) {
        int n = 0;
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                if ((dx != 0 || dy != 0) && inside(x + dx, y + dy) && member[x + dx][y + dy] != EMPTY)
                    n++;
        return n;
    }

    /**
     * Opens a hidden cell. A safe cell with no neighbours opens its neighbours
     * too (flood fill). Returns the newly revealed cells grouped by distance
     * from (x, y) — handy for a ripple animation. If a member hides there, the
     * cell explodes and the result is empty; check {@link #isExploded}.
     */
    List<List<Cell>> reveal(int x, int y) {
        if (!placed) place(x, y);
        if (!isHidden(x, y)) return List.of();
        if (member[x][y] != EMPTY) {
            exploded[x][y] = true;
            return List.of();
        }
        List<List<Cell>> layers = new ArrayList<>();
        List<Cell> current = List.of(new Cell(x, y));
        revealed[x][y] = true;
        while (!current.isEmpty()) {
            layers.add(current);
            List<Cell> next = new ArrayList<>();
            for (Cell c : current) {
                if (neighbours(c.x(), c.y()) != 0) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = c.x() + dx, ny = c.y() + dy;
                        if (inside(nx, ny) && isHidden(nx, ny) && member[nx][ny] == EMPTY) {
                            revealed[nx][ny] = true;
                            next.add(new Cell(nx, ny));
                        }
                    }
                }
            }
            current = next;
        }
        return layers;
    }

    /** Flags a hidden cell. Returns true (and keeps the flag) only if a member is there. */
    boolean flag(int x, int y) {
        if (!placed || !isHidden(x, y) || member[x][y] == EMPTY) return false;
        flagged[x][y] = true;
        return true;
    }

    /** True when every safe cell is revealed. */
    boolean isCleared() {
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                if (member[x][y] == EMPTY && !revealed[x][y]) return false;
        return placed;
    }

    /** True when every member was either flagged or exploded. */
    boolean allMembersFound() {
        int found = 0;
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                if (flagged[x][y] || exploded[x][y]) found++;
        return found == members;
    }

    /** Where member {@code m} hides (only valid once placed). */
    Cell find(int m) {
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                if (member[x][y] == m) return new Cell(x, y);
        throw new IllegalStateException("member " + m + " not placed");
    }

    private static boolean inside(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }
}
