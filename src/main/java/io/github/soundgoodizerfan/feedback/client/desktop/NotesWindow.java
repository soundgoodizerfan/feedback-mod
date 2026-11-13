// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerDataPayload;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Notes app: a text pad saved on the block, for coordinates, pinouts and reminders. Sent a moment after typing stops, and on close. */
final class NotesWindow extends DesktopWindow {

    private final ComputerContext ctx;
    private MultiLineEditBox box;
    private String text = "";
    private boolean dirty, loaded;
    private int idle, originX, originY;

    NotesWindow(ComputerContext ctx) {
        super(30, 12, 220, 150, 140, 80, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.notes");
    }

    @Override
    public Glyph glyph() {
        return Glyph.NOTES;
    }

    @Override
    protected void build(GuiSkin skin) {
        box = add(new MultiLineEditBox(Minecraft.getInstance().font, 0, 0, contentW(), contentH(),
                Component.translatable("gui.feedback.computer.notes_hint"), Component.empty()), 0, 0);
        box.setCharacterLimit(ComputerBlockEntity.NOTES_MAX);
        box.setValue(text);
        box.setValueListener(value -> {
            text = value;
            dirty = true;
            idle = 0;
        });
    }

    @Override
    protected void layout(int cw, int ch) {
        box.setWidth(cw);
        box.setHeight(ch);
    }

    @Override
    protected void opened() {
        if (!loaded && ctx.computer() != null) {
            loaded = true;
            text = ctx.computer().notes();
            box.setValue(text);
            dirty = false;
        }
    }

    /** Sends the text if it changed since it was last sent. */
    void flush() {
        if (!dirty)
            return;
        dirty = false;
        PacketDistributor.sendToServer(new ComputerDataPayload(ctx.menu().getPos(), ComputerDataPayload.NOTES, 0, text));
    }

    @Override
    protected void closed() {
        flush();
    }

    @Override
    protected void tick() {
        if (dirty && ++idle > 20)
            flush();
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        originX = ui.ax(0);
        originY = ui.ay(0);
    }

    @Override
    protected boolean mouseDragged(double lx, double ly, int button, double dx, double dy) {
        return box.mouseDragged(lx + originX, ly + originY, button, dx, dy);
    }

    @Override
    protected boolean mouseReleased(double lx, double ly, int button) {
        return box.mouseReleased(lx + originX, ly + originY, button);
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        return box.mouseScrolled(lx + originX, ly + originY, 0, wheel);
    }
}
