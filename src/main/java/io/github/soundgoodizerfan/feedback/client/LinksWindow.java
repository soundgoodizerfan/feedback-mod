// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.client.desktop.DesktopWindow;
import io.github.soundgoodizerfan.feedback.client.desktop.Glyph;
import io.github.soundgoodizerfan.feedback.client.desktop.ScrollArea;
import io.github.soundgoodizerfan.feedback.client.desktop.Ui;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** The Links app: the blocks the running program's sensor and actuator cards are linked to, with their positions. */
final class LinksWindow extends DesktopWindow {

    private final ComputerContext ctx;
    private final ScrollArea scroll = new ScrollArea();

    LinksWindow(ComputerContext ctx) {
        super(100, 30, 200, 120, 140, 80, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.links");
    }

    @Override
    public Glyph glyph() {
        return Glyph.LINKS;
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ProgramGraph graph = ctx.running();
        boolean any = false;
        for (ProgramNode node : graph.nodes())
            any |= node instanceof SensorNode || node instanceof ActuatorNode;
        if (!any) {
            ui.wrapped(0, 0, ui.w(), Component.translatable("gui.feedback.computer.no_links"), skin.dim());
            return;
        }
        int y = scroll.begin(ui, 0, 0, ui.w(), ui.h());
        int top = y;
        int width = scroll.innerWidth() - 4;
        for (ProgramNode node : graph.nodes()) {
            DataNodeRef ref;
            String kind;
            if (node instanceof SensorNode s) {
                ref = s.target();
                kind = "in";
            } else if (node instanceof ActuatorNode a) {
                ref = a.target();
                kind = "out";
            } else {
                continue;
            }
            String where = "unlinked";
            var level = Minecraft.getInstance().level;
            if (ref != null && level != null) {
                var pos = ref.linkedAt();
                where = level.getBlockState(pos).getBlock().getName().getString() + " " + pos.getX() + "," + pos.getY() + "," + pos.getZ();
            }
            ui.text(4, y, kind, skin.dim());
            ui.clipped(24, y, width - 20, where, ref == null ? skin.warn() : skin.ink());
            y += Ui.LINE;
        }
        scroll.end(ui, y - top + 4);
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        return scroll.scrolled(lx, ly, wheel);
    }
}
