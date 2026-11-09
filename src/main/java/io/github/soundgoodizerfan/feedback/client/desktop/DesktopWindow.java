// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.GuiSkin;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * One window of the desktop: an app's content in a frame the {@link Desktop} owns (title bar, minimise, maximise, close, drag and edge
 * resize, focus and stacking). A subclass is an app: it names itself, draws into a {@link Ui} in its own coordinates, and puts widgets
 * where it wants them, relative to its content; the desktop moves, clips and hides all of it with the window.
 *
 * <p>Everything arrives in window-local coordinates: (0, 0) is the content's top-left, whatever the window's place on screen. Input a
 * window does not take returns false, which lets the screen under it have it (Escape closes the desktop).
 *
 * <p>Lifecycle: {@link #build} (make widgets; again after a skin change or a window resize of the whole screen), {@link #layout} (the
 * content size is known or has changed), {@link #opened} when shown, {@link #tick} every client tick while open, {@link #closed} when its
 * close button is pressed or {@link Desktop#close} is called.
 */
public abstract class DesktopWindow {

    /** The title bar's height, and the frame the content sits inside. */
    public static final int TITLE = 14, FRAME = 4;

    private record Placed(AbstractWidget widget, int x, int y) {
    }

    private final List<Placed> widgets = new ArrayList<>();
    private AbstractWidget focus;
    Desktop desktop;
    int x, y, w, h;
    int restoreX, restoreY, restoreW, restoreH;
    boolean open, minimised, maximised, placed;
    final int startX, startY;
    final int minW, minH;
    final boolean resizable;

    /** {@code w} x {@code h} is the first size; {@code startX}, {@code startY} the first place, relative to the work area's top-left. */
    protected DesktopWindow(int startX, int startY, int w, int h, int minW, int minH, boolean resizable) {
        this.startX = startX;
        this.startY = startY;
        this.w = w;
        this.h = h;
        this.minW = minW;
        this.minH = minH;
        this.resizable = resizable;
    }

    public abstract Component title();

    public Glyph glyph() {
        return Glyph.APP;
    }

    /** The content's width and height now. */
    public final int contentW() {
        return w - 2 * FRAME;
    }

    public final int contentH() {
        return h - TITLE - FRAME - 3;
    }

    public final boolean isOpen() {
        return open;
    }

    protected final Desktop desktop() {
        return desktop;
    }

    // --- widgets ----------------------------------------------------------------------------

    /** Adds a widget at ({@code lx}, {@code ly}) in the content. The desktop positions, clips, renders and clicks it. */
    protected final <T extends AbstractWidget> T add(T widget, int lx, int ly) {
        widgets.add(new Placed(widget, lx, ly));
        return widget;
    }

    /** Moves a widget this window added; used from {@link #layout} to follow a resize. */
    protected final void place(AbstractWidget widget, int lx, int ly) {
        for (int i = 0; i < widgets.size(); i++)
            if (widgets.get(i).widget == widget)
                widgets.set(i, new Placed(widget, lx, ly));
    }

    protected final void remove(AbstractWidget widget) {
        widgets.removeIf(p -> p.widget == widget);
        if (focus == widget)
            focus = null;
    }

    final void clearWidgets() {
        widgets.clear();
        focus = null;
    }

    final void positionWidgets(int cx, int cy) {
        for (Placed p : widgets)
            p.widget.setPosition(cx + p.x, cy + p.y);
    }

    final void renderWidgets(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float partial) {
        for (Placed p : widgets)
            if (p.widget.visible)
                p.widget.render(g, mx, my, partial);
    }

    /** Hands a click to the topmost widget under (mx, my), in screen coordinates; the clicked EditBox takes the keys. */
    final boolean clickWidgets(double mx, double my, int button) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            AbstractWidget wd = widgets.get(i).widget;
            if (wd.visible && wd.active && wd.isMouseOver(mx, my) && wd.mouseClicked(mx, my, button)) {
                focusOn(wd instanceof EditBox || wd instanceof net.minecraft.client.gui.components.MultiLineEditBox ? wd : null);
                return true;
            }
        }
        focusOn(null);
        return false;
    }

    final boolean releaseWidgets(double mx, double my, int button) {
        boolean any = false;
        for (Placed p : widgets)
            if (p.widget.visible)
                any |= p.widget.mouseReleased(mx, my, button);
        return any;
    }

    protected final void focusOn(AbstractWidget widget) {
        if (focus != null && focus != widget)
            focus.setFocused(false);
        focus = widget;
        if (widget != null)
            widget.setFocused(true);
    }

    /** The EditBox (or other widget) taking the keys, if any. */
    protected final AbstractWidget focused() {
        return focus;
    }

    // --- lifecycle (override) ---------------------------------------------------------------

    /** Make the widgets, through {@link #add}, in {@code skin}. Runs again, from a clean slate, after anything that changes the skin or the screen. */
    protected void build(GuiSkin skin) {
    }

    /** The content is now {@code cw} x {@code ch}: put widgets where that size wants them ({@link #place}). */
    protected void layout(int cw, int ch) {
    }

    protected void opened() {
    }

    protected void closed() {
    }

    protected void tick() {
    }

    /** Draws the content. {@code mx}, {@code my} is the pointer in content coordinates. Widgets are drawn after this returns. */
    protected abstract void render(Ui ui, int mx, int my);

    // --- input (override; coordinates are content-local) ------------------------------------

    protected boolean mouseClicked(double lx, double ly, int button) {
        return false;
    }

    protected boolean mouseDragged(double lx, double ly, int button, double dx, double dy) {
        return false;
    }

    protected boolean mouseReleased(double lx, double ly, int button) {
        return false;
    }

    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        return false;
    }

    /** A focused text box takes every key, Escape unfocusing it; otherwise nothing is taken. */
    protected boolean keyPressed(int key, int scan, int modifiers) {
        if (focus == null)
            return false;
        if (key == 256) {
            focusOn(null);
            return true;
        }
        focus.keyPressed(key, scan, modifiers);
        return true;
    }

    protected boolean charTyped(char c, int modifiers) {
        return focus != null && focus.charTyped(c, modifiers);
    }
}
