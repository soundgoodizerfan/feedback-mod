// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.GuiStyle;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The right-click menu of the desktop: a small raised list of actions at the pointer, one open at a time, owned by the {@link Desktop}.
 * Each entry is a {@link GuiStyle} button, so it reads and sounds like every other control on the skin. A click on an entry closes the
 * menu and then runs the entry's action (so an action may open the next menu); a click anywhere else, or Escape, only closes it.
 */
public final class ContextMenu {

    /** One entry. A disabled one is drawn grey and does nothing, which also makes a label of it. */
    public record Item(Component label, Runnable action, boolean enabled) {

        public Item(Component label, Runnable action) {
            this(label, action, true);
        }
    }

    private static final int ROW = 13, PAD = 3, MIN_W = 64;

    private final List<Button> buttons = new ArrayList<>();
    private int x, y, w, h;
    private boolean open;

    public boolean isOpen() {
        return open;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public void close() {
        open = false;
        buttons.clear();
    }

    /** Opens the menu with its top-left at ({@code mx}, {@code my}), moved back inside the bounds if it would overhang them. */
    void open(GuiSkin skin, Font font, int mx, int my, List<Item> items, int bx, int by, int bw, int bh) {
        close();
        if (items.isEmpty())
            return;
        int widest = MIN_W;
        for (Item item : items)
            widest = Math.max(widest, font.width(item.label()) + 12);
        w = widest + 2 * PAD;
        h = items.size() * ROW + 2 * PAD;
        x = Math.max(bx, Math.min(mx, bx + bw - w));
        y = Math.max(by, Math.min(my, by + bh - h));
        int row = 0;
        for (Item item : items) {
            Button button = GuiStyle.button(skin, item.label(), b -> {
                close();
                item.action().run();
            }, x + PAD, y + PAD + row * ROW, widest, ROW - 1);
            button.active = item.enabled();
            buttons.add(button);
            row++;
        }
        open = true;
    }

    /** Whether the menu, if open, overlaps the rectangle. */
    public boolean overlaps(int rx, int ry, int rw, int rh) {
        return open && rx < x + w && rx + rw > x && ry < y + h && ry + rh > y;
    }

    public boolean covers(double mx, double my) {
        return open && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    void render(GuiGraphics g, GuiSkin skin, int mx, int my, float partial) {
        if (!open)
            return;
        skin.panel(g, x, y, w, h);
        for (Button button : buttons)
            button.render(g, mx, my, partial);
    }

    /** A click on the menu: an entry takes it, the rest of the menu swallows it. Returns whether the menu took it. */
    boolean mouseClicked(double mx, double my, int button) {
        if (!covers(mx, my))
            return false;
        for (Button b : new ArrayList<>(buttons))
            if (b.active && b.isMouseOver(mx, my) && b.mouseClicked(mx, my, button))
                return true;
        return true;
    }
}
