// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.List;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * What a window draws with: its content rectangle, in window-local coordinates (0, 0 is the content's top-left), through the desktop's
 * skin. A window never sees screen coordinates, never sets a scissor of its own that could leak past its edge (the desktop clips the
 * content), and wraps text to its own width. One instance is reused for every window, every frame, so nothing here allocates.
 *
 * <p>The primitives are the skin's ({@code inset}, {@code plaque}, {@code bar}, {@code scrollbar}); this adds the text layout the
 * windows share: wrapped paragraphs, label and value rows, clipped lines.
 */
public final class Ui {

    public static final int LINE = 10;

    private final Font font;
    private GuiGraphics g;
    private GuiSkin skin;
    private int ox, oy, w, h;

    Ui(Font font) {
        this.font = font;
    }

    void begin(GuiGraphics g, GuiSkin skin, int ox, int oy, int w, int h) {
        this.g = g;
        this.skin = skin;
        this.ox = ox;
        this.oy = oy;
        this.w = w;
        this.h = h;
    }

    /** The content's size. */
    public int w() {
        return w;
    }

    public int h() {
        return h;
    }

    public GuiSkin skin() {
        return skin;
    }

    public Font font() {
        return font;
    }

    /** The raw graphics, for something the helpers do not cover; coordinates are screen ones, so add {@link #ax} and {@link #ay}. */
    public GuiGraphics graphics() {
        return g;
    }

    public int ax(int x) {
        return ox + x;
    }

    public int ay(int y) {
        return oy + y;
    }

    // --- text -------------------------------------------------------------------------------

    public void text(int x, int y, Component text, int colour) {
        g.drawString(font, text, ox + x, oy + y, colour, false);
    }

    public void text(int x, int y, String text, int colour) {
        g.drawString(font, text, ox + x, oy + y, colour, false);
    }

    /** A line cut to {@code width} pixels, with no ellipsis. */
    public void clipped(int x, int y, int width, String text, int colour) {
        g.drawString(font, font.plainSubstrByWidth(text, width), ox + x, oy + y, colour, false);
    }

    /** A line ending at {@code right}. */
    public void right(int right, int y, String text, int colour) {
        g.drawString(font, text, ox + right - font.width(text), oy + y, colour, false);
    }

    /**
     * A paragraph wrapped to {@code width}, one {@link #LINE} per line, from the line {@code skip} (for a scrolled block); returns how
     * many lines it takes in all, which is how a caller sizes a scroll area. Lines above {@code y < -LINE} or below {@code limit} are
     * counted but not drawn.
     */
    public int wrapped(int x, int y, int width, FormattedText text, int colour, int limit) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(8, width));
        for (int i = 0; i < lines.size(); i++) {
            int ly = y + i * LINE;
            if (ly > -LINE && ly < limit)
                g.drawString(font, lines.get(i), ox + x, oy + ly, colour, false);
        }
        return lines.size();
    }

    /** {@link #wrapped} with no clipping rows. */
    public int wrapped(int x, int y, int width, FormattedText text, int colour) {
        return wrapped(x, y, width, text, colour, Integer.MAX_VALUE);
    }

    /** A label at the left and its value at the right of {@code width}, the value in the accent. */
    public void row(int x, int y, int width, String label, String value, int valueColour) {
        int valueW = font.width(value);
        clipped(x, y, Math.max(8, width - valueW - 6), label, skin.ink());
        text(x + width - valueW, y, value, valueColour);
    }

    // --- shapes -----------------------------------------------------------------------------

    public void fill(int x, int y, int w, int h, int colour) {
        g.fill(ox + x, oy + y, ox + x + w, oy + y + h, colour);
    }

    public void inset(int x, int y, int w, int h) {
        skin.inset(g, ox + x, oy + y, w, h);
    }

    public void plaque(int x, int y, int w, int h) {
        skin.plaque(g, ox + x, oy + y, w, h);
    }

    public void bar(int x, int y, int w, int h, float fill) {
        skin.bar(g, ox + x, oy + y, w, h, Math.max(0f, Math.min(1f, fill)), -1f);
    }

    public void row(int x, int y, int w, int h, boolean picked, boolean hovered) {
        skin.row(g, ox + x, oy + y, w, h, picked, hovered);
    }

    public void scrollbar(int x, int y, int h, int total, int visible, int at) {
        skin.scrollbar(g, ox + x, oy + y, h, total, visible, at);
    }

    public void glyph(Glyph glyph, int x, int y, int colour) {
        glyph.draw(g, ox + x, oy + y, colour);
    }

    public void item(ItemStack stack, int x, int y) {
        g.renderItem(stack, ox + x, oy + y);
    }

    /** Clips what follows to a rectangle inside the content; pair with {@link #unclip}. Nests with the desktop's own clip. */
    public void clip(int x, int y, int w, int h) {
        g.enableScissor(ox + x, oy + y, ox + x + w, oy + y + h);
    }

    public void unclip() {
        g.disableScissor();
    }
}
