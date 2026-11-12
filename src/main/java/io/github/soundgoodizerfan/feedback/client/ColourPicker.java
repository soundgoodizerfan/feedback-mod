// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A colour control: a swatch showing the colour and a hex entry ({@code #RRGGBB}) beside it, on an inset of the skin. The colour is read
 * from a supplier every frame, so the control shows what is set, not what was typed; the entry reports a colour only once six hex digits
 * parse, and goes warning-red while it does not. It is 14 high and any width from about 70. Used by the Blueprint board for a pulse gun's
 * beam colour; any screen that wants a free colour uses it the same way.
 *
 * <p>The entry holds the typing focus: a host that forwards keys to its own widgets sends {@link #keyPressed}, {@link #charTyped} while
 * {@link #typing}, and calls {@link #blur} when a click lands elsewhere.
 */
final class ColourPicker extends AbstractWidget {

    private final GuiSkin skin;
    private final EditBox box;
    private final IntSupplier colour;
    private int shown = -1;

    ColourPicker(GuiSkin skin, Font font, int x, int y, int w, Component label, IntSupplier colour, IntConsumer set) {
        super(x, y, w, 14, label);
        this.skin = skin;
        this.colour = colour;
        box = new EditBox(font, x + 17, y + 3, w - 20, 9, label);
        box.setBordered(false);
        box.setMaxLength(7);
        box.setFilter(s -> s.matches("#?[0-9a-fA-F]{0,6}"));
        box.setResponder(text -> {
            Integer rgb = parse(text);
            box.setTextColor(rgb == null ? skin.warn() : skin.ink());
            if (rgb != null) {
                shown = rgb;
                set.accept(rgb);
            }
        });
        box.setTextColor(skin.ink());
    }

    /** {@code text} as an RGB, or null if it is not six hex digits (a leading {@code #} is allowed). */
    static Integer parse(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6)
            return null;
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    boolean typing() {
        return box.isFocused();
    }

    void blur() {
        box.setFocused(false);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int now = colour.getAsInt() & 0xFFFFFF;
        if (!box.isFocused() && now != shown) {
            shown = now;
            box.setValue(hex(now));
        }
        skin.inset(g, getX(), getY(), width, height);
        g.fill(getX() + 2, getY() + 2, getX() + 14, getY() + 12, skin.dim());
        g.fill(getX() + 3, getY() + 3, getX() + 13, getY() + 11, 0xFF000000 | now);
        box.setX(getX() + 17);
        box.setY(getY() + 3);
        box.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible || !active || !isMouseOver(mx, my))
            return false;
        box.setFocused(true);
        return box.mouseClicked(mx, my, button) || true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return box.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return box.charTyped(c, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
