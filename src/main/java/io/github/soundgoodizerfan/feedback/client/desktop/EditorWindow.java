// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkedPayload;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.screen.ProgramEditor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Editor app: the same {@link ProgramEditor} the Programmer block's screen shows, in a window. It edits whatever medium the Computer
 * has put in its embedded programmer (the Drive window's Edit does), and closing the window commits the edit and sends the medium back to
 * its drive. The editor draws in screen coordinates, so this window hands it the content rectangle each frame and turns the pointer back
 * from content coordinates.
 */
final class EditorWindow extends DesktopWindow {

    private final ComputerContext ctx;
    private ProgramEditor editor;
    private GuiSkin skin;
    private int originX, originY;

    EditorWindow(ComputerContext ctx) {
        super(0, 0, 380, 180, 300, 140, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.editor");
    }

    @Override
    public Glyph glyph() {
        return Glyph.EDITOR;
    }

    private ProgramEditor.Source source() {
        return new ProgramEditor.Source() {
            @Override
            public BlockPos pos() {
                return ctx.menu().getPos();
            }

            @Override
            public ProgrammerBlockEntity programmer() {
                return ctx.computer() == null ? null : ctx.computer().programmer();
            }

            @Override
            public ItemStack medium() {
                ProgrammerBlockEntity p = programmer();
                return p == null ? ItemStack.EMPTY : p.getItem(0);
            }
        };
    }

    @Override
    protected void build(GuiSkin skin) {
        if (editor != null) {
            editor.flush();
            editor = new ProgramEditor(source(), skin);
        }
        this.skin = skin;
        if (editor != null)
            editor.build();
    }

    @Override
    protected void opened() {
        editor = new ProgramEditor(source(), skin);
        editor.build();
    }

    @Override
    protected void closed() {
        if (editor != null)
            editor.flush();
        editor = null;
        ctx.send(ComputerActionPayload.CLOSE_EDITOR, 0);
    }

    /** Sends an unsent edit before the desktop closes, while the server still has the menu open. */
    void flush() {
        if (editor != null)
            editor.flush();
    }

    void applyLink(ProgrammerLinkedPayload payload) {
        if (editor != null)
            editor.applyLink(payload);
    }

    @Override
    protected void tick() {
        if (editor != null)
            editor.tick();
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        originX = ui.ax(0);
        originY = ui.ay(0);
        if (editor == null)
            return;
        ProgrammerBlockEntity programmer = ctx.computer() == null ? null : ctx.computer().programmer();
        if (programmer == null || programmer.getItem(0).isEmpty()) {
            ui.wrapped(4, 4, ui.w() - 8, Component.translatable("gui.feedback.computer.editor_empty"), ui.skin().dim());
            return;
        }
        editor.viewport(originX, originY, ui.w(), ui.h());
        editor.render(ui.graphics(), mx + originX, my + originY, 0f);
    }

    @Override
    protected boolean mouseClicked(double lx, double ly, int button) {
        return editor != null && editor.mouseClicked(lx + originX, ly + originY, button);
    }

    @Override
    protected boolean mouseDragged(double lx, double ly, int button, double dx, double dy) {
        return editor != null && editor.mouseDragged(lx + originX, ly + originY, button, dx, dy);
    }

    @Override
    protected boolean mouseReleased(double lx, double ly, int button) {
        return editor != null && editor.mouseReleased(lx + originX, ly + originY, button);
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        return editor != null && editor.mouseScrolled(lx + originX, ly + originY, wheel);
    }

    @Override
    protected boolean keyPressed(int key, int scan, int modifiers) {
        return editor != null && editor.keyPressed(key, scan, modifiers);
    }

    @Override
    protected boolean charTyped(char c, int modifiers) {
        return editor != null && editor.charTyped(c, modifiers);
    }
}
