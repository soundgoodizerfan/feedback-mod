// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.List;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The Scope app: every sensor card of the running program as its own trace, each scaled to its own range, with a pause and a time scale.
 * The readings the server pushes arrive a few times a second, so a trace is stepped, as the real thing sampled that slowly would be.
 */
final class ScopeWindow extends DesktopWindow {

    private static final int RING = 240, CHANNELS = 4;

    private final ComputerContext ctx;
    private final float[][] ring = new float[CHANNELS][RING];
    private int at, ticks, stride = 1, count;
    private boolean paused;

    ScopeWindow(ComputerContext ctx) {
        super(20, 10, 300, 170, 220, 120, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.scope_app");
    }

    @Override
    public Glyph glyph() {
        return Glyph.SCOPE;
    }

    @Override
    protected void build(GuiSkin skin) {
        add(GuiStyle.checkbox(skin, Component.translatable("gui.feedback.computer.pause"), () -> paused, b -> paused = !paused, 0, 0, 64), 0, 0);
        add(GuiStyle.tabs(skin, 0, 0, 96, 14, List.of(Component.literal("1x"), Component.literal("2x"), Component.literal("4x")),
                () -> stride == 1 ? 0 : stride == 2 ? 1 : 2, i -> stride = 1 << i), 72, 0);
        add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.clear"), b -> {
            for (float[] r : ring)
                java.util.Arrays.fill(r, 0f);
        }, 0, 0, 44, 14), 176, 0);
    }

    private List<Quantity> channels() {
        java.util.ArrayList<Quantity> out = new java.util.ArrayList<>();
        for (ProgramNode node : ctx.running().nodes())
            if (node instanceof SensorNode s && out.size() < CHANNELS)
                out.add(s.quantity() == null ? Quantity.FIGURE : s.quantity());
        return out;
    }

    @Override
    protected void tick() {
        if (paused || ++ticks % stride != 0)
            return;
        ComputerBlockEntity computer = ctx.computer();
        float[] seen = computer == null ? new float[0] : computer.readings();
        count = Math.min(channels().size(), CHANNELS);
        for (int c = 0; c < CHANNELS; c++)
            ring[c][at] = c < seen.length ? seen[c] : 0f;
        at = (at + 1) % RING;
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        List<Quantity> channels = channels();
        int legend = Math.max(1, channels.size()) * Ui.LINE + 2;
        int px = 0, py = 18, pw = ui.w(), ph = ui.h() - 18 - legend;
        ui.inset(px, py, pw, ph);
        int grid = (skin.ink() & 0xFFFFFF) | 0x28000000;
        for (int i = 1; i < 4; i++)
            ui.fill(px + 1, py + ph * i / 4, pw - 2, 1, grid);
        for (int i = 1; i < 8; i++)
            ui.fill(px + pw * i / 8, py + 1, 1, ph - 2, grid);
        if (channels.isEmpty()) {
            ui.text(px + 6, py + 6, Component.translatable("gui.feedback.computer.no_signal"), skin.dim());
            return;
        }
        int[] colours = {skin.accent(), skin.ink(), skin.warn(), skin.dim()};
        GuiGraphics g = ui.graphics();
        for (int c = 0; c < channels.size(); c++) {
            float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
            for (float v : ring[c]) {
                lo = Math.min(lo, v);
                hi = Math.max(hi, v);
            }
            float span = Math.max(hi - lo, 1e-3f);
            int prevX = -1, prevY = 0;
            for (int i = 0; i < RING; i++) {
                float v = ring[c][(at + i) % RING];
                int x = ui.ax(px + 2 + i * (pw - 4) / RING), y = ui.ay(py + ph - 3 - (int) ((v - lo) / span * (ph - 6)));
                if (prevX >= 0)
                    GuiStyle.line(g, prevX, prevY, x, y, colours[c]);
                prevX = x;
                prevY = y;
            }
            Quantity q = channels.get(c);
            float now = ring[c][(at + RING - 1) % RING];
            int ly = py + ph + 2 + c * Ui.LINE;
            ui.fill(0, ly + 2, 6, 5, colours[c]);
            ui.text(10, ly, q.getSerializedName(), skin.ink());
            ui.right(pw, ly, q == Quantity.STATE ? (now > 0.5f ? "on" : "off") : Readout.figure(q, now), colours[c]);
        }
    }
}
