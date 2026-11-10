// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.client.desktop.DesktopWindow;
import io.github.soundgoodizerfan.feedback.client.desktop.Glyph;
import io.github.soundgoodizerfan.feedback.client.desktop.Ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Blueprint app: the Blueprint Table's drafting board ({@link BlueprintBoard}) in a window, in the Computer's skin. Everything the
 * table lets you draw, compare and pin is here; what it cannot do is write the drawing onto a sheet of paper, and it has no sample slot
 * (a material item carried on the cursor from the dock paints a part instead). The board lays itself out to the window's content size
 * ({@link BlueprintBoard#resize}), so a bigger window gives the canvas and panels more room; below the board's smallest size the window
 * scrolls: the wheel with Ctrl held, or a drag on the strip down its right edge.
 */
final class BlueprintWindow extends DesktopWindow implements BlueprintBoard.Host {

    private static final int GUTTER = 12;

    private final ComputerContext ctx;
    private BlueprintBoard board;
    private GuiSkin skin;
    private int scroll, originX, originY, viewH;
    private boolean draggingBar;

    BlueprintWindow(ComputerContext ctx) {
        super(0, 0, 380, 180, 372, 120, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.blueprint");
    }

    @Override
    public Glyph glyph() {
        return Glyph.BLUEPRINT;
    }

    @Override
    protected void layout(int cw, int ch) {
        if (board != null)
            board.resize(cw - GUTTER, ch);
    }

    @Override
    protected void build(GuiSkin skin) {
        // a new skin is a new board; the drawing is not kept across it
        this.skin = skin;
        board = new BlueprintBoard(this, skin);
        board.build();
        board.resize(contentW() - GUTTER, contentH());
    }

    // --- BlueprintBoard.Host ------------------------------------------------------------------

    @Override
    public ItemStack carried() {
        return ctx.menu().getCarried();
    }

    @Override
    public void selectionChanged() {
    }

    @Override
    public void sampleWanted(boolean wanted) {
    }

    // --- window ------------------------------------------------------------------------------

    private int maxScroll() {
        return Math.max(0, board.height() - viewH);
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        originX = ui.ax(0);
        originY = ui.ay(0);
        viewH = ui.h();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        board.origin(originX, originY - scroll);
        int ax = mx + originX, ay = my + originY;
        board.drawBoard(ui.graphics(), ax, ay, 0f);
        board.drawOverlay(ui.graphics(), ax, ay);
        board.tooltip(ui.graphics(), ax, ay);
        ui.scrollbar(ui.w() - 3, 0, ui.h(), board.height(), ui.h(), scroll);
    }

    private boolean onStrip(double lx) {
        return lx >= contentW() - GUTTER && maxScroll() > 0;
    }

    private void dragBar(double ly) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), ly / Math.max(1, viewH) * board.height() - viewH / 2.0));
    }

    @Override
    protected boolean mouseClicked(double lx, double ly, int button) {
        if (button == 0 && onStrip(lx)) {
            draggingBar = true;
            dragBar(ly);
            return true;
        }
        return board.mouseClicked(lx + originX, ly + originY, button);
    }

    @Override
    protected boolean mouseDragged(double lx, double ly, int button, double dx, double dy) {
        if (draggingBar) {
            dragBar(ly);
            return true;
        }
        return board.mouseDragged(lx + originX, ly + originY, button, dx, dy);
    }

    @Override
    protected boolean mouseReleased(double lx, double ly, int button) {
        boolean bar = draggingBar;
        draggingBar = false;
        return board.mouseReleased(lx + originX, ly + originY, button) || bar;
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        if (net.minecraft.client.gui.screens.Screen.hasControlDown() || onStrip(lx)) {
            scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - Math.signum(wheel) * 24));
            return true;
        }
        return board.mouseScrolled(lx + originX, ly + originY, wheel);
    }

    @Override
    protected boolean keyPressed(int key, int scan, int modifiers) {
        return board.keyPressed(key, scan, modifiers);
    }

    @Override
    protected boolean charTyped(char c, int modifiers) {
        return board.charTyped(c, modifiers);
    }
}
