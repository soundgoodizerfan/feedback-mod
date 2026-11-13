// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramVariable;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;

import net.minecraft.network.chat.Component;

/** The Monitor app: the running program's variables and the last reading of each of its sensor cards. */
final class MonitorWindow extends DesktopWindow {

    private final ComputerContext ctx;
    private final ScrollArea scroll = new ScrollArea();

    MonitorWindow(ComputerContext ctx) {
        super(60, 14, 180, 140, 140, 90, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.monitor");
    }

    @Override
    public Glyph glyph() {
        return Glyph.MONITOR;
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ProgramGraph graph = ctx.running();
        if (graph.nodes().isEmpty()) {
            ui.wrapped(0, 0, ui.w(), Component.translatable("gui.feedback.computer.nothing_running"), skin.dim());
            return;
        }
        ComputerBlockEntity computer = ctx.computer();
        Map<String, Object> values = computer == null ? Map.of() : computer.core().variables();
        float[] seen = computer == null ? new float[0] : computer.readings();
        int w = ui.w();
        int y = scroll.begin(ui, 0, 0, w, ui.h());
        int inner = scroll.innerWidth();
        int top = y;
        ui.text(4, y, Component.translatable("gui.feedback.computer.variables"), skin.dim());
        y += 11;
        for (ProgramVariable v : graph.variables()) {
            Object value = values.get(v.name());
            String text = value instanceof Float f ? Readout.figure(Quantity.FIGURE, f) : Boolean.TRUE.equals(value) ? "on" : "off";
            ui.row(4, y, inner - 4, v.name(), text, skin.accent());
            y += Ui.LINE;
        }
        if (graph.variables().isEmpty()) {
            ui.text(4, y, Component.translatable("gui.feedback.computer.no_variables"), skin.dim());
            y += Ui.LINE;
        }
        y += 4;
        ui.text(4, y, Component.translatable("gui.feedback.computer.sensors"), skin.dim());
        y += 11;
        int index = 0;
        for (ProgramNode node : graph.nodes()) {
            if (!(node instanceof SensorNode sensor))
                continue;
            Quantity q = sensor.quantity() == null ? Quantity.FIGURE : sensor.quantity();
            float v = index < seen.length ? seen[index] : 0f;
            String text = q == Quantity.STATE ? (v > 0.5f ? "on" : "off") : Readout.figure(q, v);
            ui.row(4, y, inner - 4, q.getSerializedName(), text, skin.accent());
            y += Ui.LINE;
            index++;
        }
        scroll.end(ui, y - top + 4);
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        return scroll.scrolled(lx, ly, wheel);
    }
}
