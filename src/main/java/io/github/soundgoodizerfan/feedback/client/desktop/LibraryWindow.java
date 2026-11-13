// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerApps;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBays;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Library app: everything in the drives at a glance, with Open (the Drive window for it) and Copy. Copy writes the program onto a
 * blank Floppy or USB drive sitting alone in another bay; the Computer makes no program from nothing, so the blank medium is the cost.
 */
final class LibraryWindow extends DesktopWindow {

    private static final int RUN = ComputerBays.SIZE, ROW = 20;

    private final ComputerContext ctx;
    private final Button[] open = new Button[RUN + 1], copy = new Button[RUN];

    LibraryWindow(ComputerContext ctx) {
        super(30, 10, 250, 140, 230, 130, false);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.library");
    }

    @Override
    public Glyph glyph() {
        return Glyph.LIBRARY;
    }

    @Override
    protected void build(GuiSkin skin) {
        int w = contentW();
        for (int i = 0; i <= RUN; i++) {
            final int drive = i;
            open[i] = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.open"), b -> {
                ctx.showDrive(drive);
                desktop().open("drive");
            }, 0, 0, 40, 14), w - 86, i * ROW + 2);
            if (i < RUN)
                copy[i] = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.copy"),
                        b -> ctx.send(ComputerActionPayload.COPY, drive), 0, 0, 40, 14), w - 43, i * ROW + 2);
        }
    }

    private static ProgramGraph graphOf(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
    }

    private boolean blankTargetFor(int drive) {
        for (int i = 0; i < RUN; i++) {
            ItemStack t = ctx.menu().getSlot(i).getItem();
            if (i != drive && t.getCount() == 1 && t.is(FTags.SOFT_CARDS) && graphOf(t).nodes().isEmpty())
                return true;
        }
        return false;
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        boolean editing = ctx.computer() != null && ctx.computer().editing();
        for (int i = 0; i <= RUN; i++) {
            ItemStack stack = ctx.menu().getSlot(i).getItem();
            int y = i * ROW;
            ui.plaque(0, y, ui.w() - 90, ROW - 2);
            ui.text(4, y + 6, i == RUN ? "R" : String.valueOf((char) ('A' + i)), skin.accent());
            open[i].visible = !stack.isEmpty();
            if (i < RUN) {
                copy[i].visible = !stack.isEmpty();
                copy[i].active = !graphOf(stack).nodes().isEmpty() && blankTargetFor(i) && !editing;
            }
            if (stack.isEmpty()) {
                ui.text(18, y + 6, Component.translatable("gui.feedback.computer.empty_short"), skin.dim());
                continue;
            }
            ui.item(stack, 16, y + 1);
            if (ComputerApps.isApp(stack)) {
                ui.clipped(36, y + 6, ui.w() - 36 - 94, Component.translatable("gui.feedback.computer.app_disk", stack.getHoverName()).getString(), skin.accent());
                continue;
            }
            int size = graphOf(stack).nodes().size();
            ui.clipped(36, y + 6, ui.w() - 36 - 94, size == 0 ? Component.translatable("gui.feedback.computer.blank").getString()
                    : Component.translatable("gui.feedback.computer.cards", size).getString(), size == 0 ? skin.dim() : skin.ink());
        }
        ui.wrapped(0, (RUN + 1) * ROW + 4, ui.w(), Component.translatable("gui.feedback.computer.copy_note"), skin.dim());
    }
}
