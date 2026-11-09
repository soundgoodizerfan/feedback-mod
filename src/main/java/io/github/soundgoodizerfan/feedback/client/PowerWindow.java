// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.client.desktop.DesktopWindow;
import io.github.soundgoodizerfan.feedback.client.desktop.Glyph;
import io.github.soundgoodizerfan.feedback.client.desktop.Ui;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The Power app: what the cable is giving this Computer. Voltage, current and what it asks for, as figures read through {@link Readout}
 * (so the carried instrument sets the precision), the band it is built for, a bar of how much of that band's voltage arrives, and the
 * last half minute of voltage as a trace.
 */
final class PowerWindow extends DesktopWindow {

    private static final int RING = 120;

    private final ComputerContext ctx;
    private final float[] volts = new float[RING];
    private int at, ticks;

    PowerWindow(ComputerContext ctx) {
        super(60, 14, 200, 140, 180, 120, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.power");
    }

    @Override
    public Glyph glyph() {
        return Glyph.POWER;
    }

    @Override
    protected void tick() {
        ComputerBlockEntity computer = ctx.computer();
        if (computer != null && ++ticks % 5 == 0) {
            volts[at] = computer.voltsSeen();
            at = (at + 1) % RING;
        }
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ComputerBlockEntity computer = ctx.computer();
        if (computer == null)
            return;
        int w = ui.w();
        boolean powered = computer.poweredSeen();
        ui.text(0, 0, Component.translatable(powered ? "gui.feedback.computer.powered" : "gui.feedback.computer.unpowered"), powered ? skin.accent() : skin.warn());
        ui.row(0, 14, w, Component.translatable("gui.feedback.computer.volts").getString(), Readout.figure(Quantity.VOLTAGE, computer.voltsSeen()), skin.accent());
        ui.row(0, 24, w, Component.translatable("gui.feedback.computer.amps").getString(), Readout.figure(Quantity.CURRENT, computer.ampsSeen()), skin.accent());
        ui.row(0, 34, w, Component.translatable("gui.feedback.computer.wants").getString(), Readout.figure(Quantity.CURRENT, computer.wantedSeen()), skin.ink());
        float rated = ElectricRules.bandVolts(computer.ratedBand());
        ui.row(0, 44, w, Component.translatable("gui.feedback.computer.band").getString(), computer.ratedBand().getSerializedName().toUpperCase(), skin.ink());
        ui.bar(0, 57, w, 5, rated <= 0 ? 0 : computer.voltsSeen() / rated);
        int py = 68, ph = ui.h() - py;
        if (ph < 20)
            return;
        ui.inset(0, py, w, ph);
        float hi = Math.max(rated, 1f);
        int prevX = -1, prevY = 0;
        GuiGraphics g = ui.graphics();
        for (int i = 0; i < RING; i++) {
            float v = volts[(at + i) % RING];
            int x = ui.ax(2 + i * (w - 4) / RING), y = ui.ay(py + ph - 3 - (int) (Math.min(1f, v / hi) * (ph - 6)));
            if (prevX >= 0)
                GuiStyle.line(g, prevX, prevY, x, y, skin.accent());
            prevX = x;
            prevY = y;
        }
    }
}
