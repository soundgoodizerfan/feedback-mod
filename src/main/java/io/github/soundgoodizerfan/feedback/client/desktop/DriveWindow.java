// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerApps;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBays;
import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The Drive app: the medium in the drive icon last opened, what is on it, and Edit, Run or Stop, and Eject. */
final class DriveWindow extends DesktopWindow {

    private static final int RUN = ComputerBays.SIZE;

    private final ComputerContext ctx;
    private Button edit, run, eject;

    DriveWindow(ComputerContext ctx) {
        super(10, 6, 170, 112, 140, 100, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        int d = ctx.drive();
        return Component.translatable("gui.feedback.computer.drive", d == RUN ? "RUN" : (char) ('A' + d) + ":");
    }

    @Override
    public Glyph glyph() {
        return Glyph.DRIVE;
    }

    @Override
    protected void build(GuiSkin skin) {
        edit = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.edit"), b -> {
            ctx.send(ComputerActionPayload.EDIT, ctx.drive());
            desktop().open("editor");
        }, 0, 0, 48, 14), 0, 0);
        run = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.run"),
                b -> ctx.send(ctx.drive() == RUN ? ComputerActionPayload.STOP : ComputerActionPayload.RUN, ctx.drive()), 0, 0, 48, 14), 0, 0);
        eject = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.eject"),
                b -> ctx.send(ComputerActionPayload.EJECT, ctx.drive()), 0, 0, 48, 14), 0, 0);
    }

    @Override
    protected void layout(int cw, int ch) {
        place(edit, 0, ch - 16);
        place(run, 52, ch - 16);
        place(eject, 104, ch - 16);
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        int d = ctx.drive();
        ItemStack medium = ctx.menu().getSlot(d).getItem();
        boolean editing = ctx.computer() != null && ctx.computer().editing();
        boolean has = !medium.isEmpty();
        boolean app = ComputerApps.isApp(medium);
        edit.visible = has && d != RUN && !app;
        edit.active = has && d != RUN && !editing;
        run.visible = has && !app;
        run.active = !editing;
        run.setMessage(Component.translatable(d == RUN ? "gui.feedback.computer.stop" : "gui.feedback.computer.run"));
        eject.visible = has && d != RUN;
        eject.active = !editing;

        int w = ui.w();
        GuiSkin skin = ui.skin();
        if (!has) {
            ui.wrapped(0, 0, w, Component.translatable("gui.feedback.computer.empty"), skin.dim());
            return;
        }
        ui.item(medium, 0, 0);
        ui.clipped(22, 4, w - 22, medium.getHoverName().getString(), skin.ink());
        if (app) {
            ui.wrapped(0, 24, w, Component.translatable("gui.feedback.computer.app_hint"), skin.accent());
            return;
        }
        ProgramGraph graph = medium.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
        if (graph.nodes().isEmpty()) {
            ui.text(0, 24, Component.translatable("gui.feedback.computer.blank"), skin.dim());
            return;
        }
        ui.wrapped(0, 24, w, Component.translatable("gui.feedback.computer.program", graph.nodes().size(), graph.variables().size()), skin.ink());
        ui.text(0, 35, Component.translatable(d == RUN ? "gui.feedback.computer.is_running" : "gui.feedback.computer.stored"),
                d == RUN ? skin.accent() : skin.dim());
        ui.text(0, 47, Component.translatable("gui.feedback.computer.memory_use"), skin.dim());
        ui.bar(0, 58, w, 5, graph.nodes().size() / (float) Math.max(1, ControllerLimits.MAX.nodes()));
        if (editing)
            ui.text(0, 66, Component.translatable("gui.feedback.computer.editing"), skin.accent());
    }
}
