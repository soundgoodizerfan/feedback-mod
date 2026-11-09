// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

import io.github.soundgoodizerfan.feedback.client.desktop.DesktopWindow;
import io.github.soundgoodizerfan.feedback.client.desktop.Glyph;
import io.github.soundgoodizerfan.feedback.client.desktop.Ui;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerDataPayload;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Arcade app: Snake (arrows or WASD, space to start) and Minesweeper (click to dig, right-click to flag). Both are played on the
 * client; only a finished game's score goes to the block, which keeps the best (longest snake, fastest cleared field). Nothing here touches
 * a program: it is a desktop toy.
 */
final class ArcadeWindow extends DesktopWindow {

    private static final int SCOLS = 20, SROWS = 12, SCELL = 8;
    private static final int MCOLS = 14, MROWS = 8, MCELL = 11, MINES = 14;
    private static final int TOP = 20;

    private final ComputerContext ctx;
    private final Random random = new Random();
    private int game;

    // snake
    private final ArrayDeque<int[]> snake = new ArrayDeque<>();
    private int dx = 1, dy = 0, nx = 1, ny = 0, foodX, foodY, stepTimer;
    private boolean started, alive = true;

    // mines
    private final boolean[] mine = new boolean[MCOLS * MROWS], open = new boolean[MCOLS * MROWS], flag = new boolean[MCOLS * MROWS];
    private final int[] near = new int[MCOLS * MROWS];
    private boolean laid, lost, won;
    private int clock, opened, flags;

    ArcadeWindow(ComputerContext ctx) {
        super(50, 8, 200, 154, 200, 154, false);
        this.ctx = ctx;
        newSnake();
        newMines();
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.arcade");
    }

    @Override
    public Glyph glyph() {
        return Glyph.GAME;
    }

    @Override
    protected void build(GuiSkin skin) {
        add(GuiStyle.tabs(skin, 0, 0, 110, 14, List.of(Component.translatable("gui.feedback.computer.snake"), Component.translatable("gui.feedback.computer.mines")),
                () -> game, i -> game = i), 0, 0);
        add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.new_game"), b -> {
            if (game == 0)
                newSnake();
            else
                newMines();
        }, 0, 0, 44, 14), 116, 0);
    }

    // --- snake ------------------------------------------------------------------------------

    private void newSnake() {
        snake.clear();
        for (int i = 0; i < 3; i++)
            snake.addFirst(new int[] {5 + i, SROWS / 2});
        dx = nx = 1;
        dy = ny = 0;
        alive = true;
        started = false;
        stepTimer = 0;
        food();
    }

    private void food() {
        do {
            foodX = random.nextInt(SCOLS);
            foodY = random.nextInt(SROWS);
        } while (onSnake(foodX, foodY));
    }

    private boolean onSnake(int x, int y) {
        for (int[] s : snake)
            if (s[0] == x && s[1] == y)
                return true;
        return false;
    }

    private void step() {
        dx = nx;
        dy = ny;
        int[] head = snake.peekFirst();
        int x = head[0] + dx, y = head[1] + dy;
        boolean eat = x == foodX && y == foodY;
        if (x < 0 || y < 0 || x >= SCOLS || y >= SROWS || onSnake(x, y) && !(snake.peekLast()[0] == x && snake.peekLast()[1] == y && !eat)) {
            alive = false;
            int score = snake.size() - 3;
            if (score > 0 && ctx.computer() != null && score > ctx.computer().snakeBest())
                PacketDistributor.sendToServer(new ComputerDataPayload(ctx.menu().getPos(), ComputerDataPayload.SNAKE, score, ""));
            return;
        }
        snake.addFirst(new int[] {x, y});
        if (eat)
            food();
        else
            snake.removeLast();
    }

    @Override
    protected void tick() {
        if (game == 0 && started && alive) {
            int period = Math.max(2, 5 - (snake.size() - 3) / 6);
            if (++stepTimer >= period) {
                stepTimer = 0;
                step();
            }
        }
        if (game == 1 && laid && !lost && !won)
            clock++;
    }

    // --- mines ------------------------------------------------------------------------------

    private void newMines() {
        java.util.Arrays.fill(mine, false);
        java.util.Arrays.fill(open, false);
        java.util.Arrays.fill(flag, false);
        laid = lost = won = false;
        clock = opened = flags = 0;
    }

    private void lay(int safe) {
        int placed = 0;
        while (placed < MINES) {
            int i = random.nextInt(mine.length);
            if (mine[i] || Math.abs(i % MCOLS - safe % MCOLS) <= 1 && Math.abs(i / MCOLS - safe / MCOLS) <= 1)
                continue;
            mine[i] = true;
            placed++;
        }
        for (int i = 0; i < mine.length; i++) {
            int n = 0;
            for (int ox = -1; ox <= 1; ox++)
                for (int oy = -1; oy <= 1; oy++) {
                    int cx = i % MCOLS + ox, cy = i / MCOLS + oy;
                    if (cx >= 0 && cy >= 0 && cx < MCOLS && cy < MROWS && mine[cy * MCOLS + cx])
                        n++;
                }
            near[i] = n;
        }
        laid = true;
    }

    private void dig(int i) {
        if (open[i] || flag[i])
            return;
        open[i] = true;
        opened++;
        if (mine[i]) {
            lost = true;
            return;
        }
        if (near[i] == 0)
            for (int ox = -1; ox <= 1; ox++)
                for (int oy = -1; oy <= 1; oy++) {
                    int cx = i % MCOLS + ox, cy = i / MCOLS + oy;
                    if (cx >= 0 && cy >= 0 && cx < MCOLS && cy < MROWS)
                        dig(cy * MCOLS + cx);
                }
    }

    @Override
    protected boolean mouseClicked(double lx, double ly, int button) {
        if (game != 1 || lost || won)
            return false;
        int ox = (contentW() - MCOLS * MCELL) / 2;
        int cx = (int) Math.floor((lx - ox) / MCELL), cy = (int) Math.floor((ly - TOP) / MCELL);
        if (cx < 0 || cy < 0 || cx >= MCOLS || cy >= MROWS)
            return false;
        int i = cy * MCOLS + cx;
        if (button == 1) {
            if (!open[i]) {
                flag[i] = !flag[i];
                flags += flag[i] ? 1 : -1;
            }
            return true;
        }
        if (!laid)
            lay(i);
        dig(i);
        if (!lost && opened == mine.length - MINES) {
            won = true;
            int seconds = Math.max(1, clock / 20);
            if (ctx.computer() != null && (ctx.computer().minesBest() == 0 || seconds < ctx.computer().minesBest()))
                PacketDistributor.sendToServer(new ComputerDataPayload(ctx.menu().getPos(), ComputerDataPayload.MINES, seconds, ""));
        }
        return true;
    }

    // --- input and drawing ------------------------------------------------------------------

    @Override
    protected boolean keyPressed(int key, int scan, int modifiers) {
        if (game != 0)
            return false;
        int ddx = nx, ddy = ny;
        switch (key) {
            case 262, 68 -> { ddx = 1; ddy = 0; }
            case 263, 65 -> { ddx = -1; ddy = 0; }
            case 264, 83 -> { ddx = 0; ddy = 1; }
            case 265, 87 -> { ddx = 0; ddy = -1; }
            case 32 -> {
                if (!alive)
                    newSnake();
                started = true;
                return true;
            }
            default -> {
                return false;
            }
        }
        if (ddx != -dx || ddy != -dy) {
            nx = ddx;
            ny = ddy;
        }
        started = true;
        return true;
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ComputerBlockEntity computer = ctx.computer();
        int w = ui.w();
        if (game == 0) {
            int ox = (w - SCOLS * SCELL) / 2;
            ui.inset(ox - 1, TOP - 1, SCOLS * SCELL + 2, SROWS * SCELL + 2);
            for (int[] s : snake)
                ui.fill(ox + s[0] * SCELL + 1, TOP + s[1] * SCELL + 1, SCELL - 2, SCELL - 2, alive ? skin.ink() : skin.dim());
            ui.fill(ox + foodX * SCELL + 2, TOP + foodY * SCELL + 2, SCELL - 4, SCELL - 4, skin.accent());
            ui.text(0, TOP + SROWS * SCELL + 6, Component.translatable("gui.feedback.computer.score", snake.size() - 3), skin.ink());
            ui.right(w, TOP + SROWS * SCELL + 6, Component.translatable("gui.feedback.computer.best", computer == null ? 0 : computer.snakeBest()).getString(), skin.dim());
            Component hint = !alive ? Component.translatable("gui.feedback.computer.game_over") : !started ? Component.translatable("gui.feedback.computer.snake_hint") : null;
            if (hint != null)
                ui.text((w - ui.font().width(hint)) / 2, TOP + SROWS * SCELL / 2 - 4, hint, alive ? skin.dim() : skin.warn());
            return;
        }
        int ox = (w - MCOLS * MCELL) / 2;
        ui.inset(ox - 1, TOP - 1, MCOLS * MCELL + 2, MROWS * MCELL + 2);
        for (int i = 0; i < mine.length; i++) {
            int x = ox + i % MCOLS * MCELL, y = TOP + i / MCOLS * MCELL;
            boolean reveal = open[i] || lost && mine[i];
            if (!reveal) {
                ui.plaque(x, y, MCELL, MCELL);
                if (flag[i])
                    ui.text(x + 3, y + 2, "F", skin.warn());
            } else if (mine[i]) {
                ui.fill(x, y, MCELL, MCELL, (skin.warn() & 0xFFFFFF) | 0x50000000);
                ui.text(x + 3, y + 2, "*", skin.warn());
            } else if (near[i] > 0) {
                ui.text(x + 3, y + 2, String.valueOf(near[i]), near[i] >= 3 ? skin.accent() : skin.ink());
            }
        }
        ui.text(0, TOP + MROWS * MCELL + 6, Component.translatable("gui.feedback.computer.mines_left", MINES - flags), skin.ink());
        ui.right(w, TOP + MROWS * MCELL + 6, (clock / 20) + "s" + (computer != null && computer.minesBest() > 0 ? "  " + Component.translatable("gui.feedback.computer.best", computer.minesBest() + "s").getString() : ""), skin.dim());
        if (lost || won)
            ui.text(0, TOP + MROWS * MCELL + 18, Component.translatable(lost ? "gui.feedback.computer.game_over" : "gui.feedback.computer.cleared"), lost ? skin.warn() : skin.accent());
    }
}
