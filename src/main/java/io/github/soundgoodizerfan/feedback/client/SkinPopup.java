/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A second window that opens over a skinned screen to pick one thing from a list. It is drawn on the host's
 * skin and arrives the way that skin's material does ({@link Opening}: an unrolling sheet, a hatch, a panel
 * powering up), with the skin's sounds; the host dims a little behind it and nothing under it takes input
 * while it is up. A host owns one, calls {@link #open} to show it, and routes four things to it: {@link #render}
 * after its own drawing, and the click, scroll and key events, each of which returns whether the popup took it.
 *
 * @param <T> what an entry stands for, handed to the host when it is picked.
 */
final class SkinPopup<T> {

    /** One choice: what it stands for, its label, and a tooltip for what it is. */
    record Entry<T>(T value, Component label, List<Component> tip) {
    }

    private static final int W = 200, PAD = 10, TITLE_H = 16, ROW_H = 18, MAX_ROWS = 6, SEARCH_H = 16;

    private final GuiSkin skin;
    private final Component title;
    private Opening opening;
    private boolean shown;
    /** Every entry, and those whose label has the search text in it. */
    private List<Entry<T>> all = List.of(), entries = List.of();
    /** The search box, only for a list longer than a page. */
    private EditBox search;
    private Consumer<T> onPick = v -> {
    };
    private int scroll;
    /** Where the window was last drawn, which the input is tested against. */
    private int x, y, h;

    SkinPopup(GuiSkin skin, Component title) {
        this.skin = skin;
        this.title = title;
    }

    /** Whether the popup is up, opening or closing: the host gives it all input. */
    boolean active() {
        return shown;
    }

    void open(List<Entry<T>> entries, Consumer<T> onPick) {
        this.all = entries;
        this.entries = entries;
        var font = Minecraft.getInstance().font;
        search = entries.size() > MAX_ROWS ? new EditBox(font, 0, 0, W - 2 * PAD - 8, SEARCH_H - 8, Component.translatable("gui.feedback.popup.search")) : null;
        if (search != null) {
            search.setBordered(false);
            search.setTextColor(skin.ink());
            search.setHint(Component.translatable("gui.feedback.popup.search").withColor(skin.dim()));
            search.setResponder(this::filter);
            search.setFocused(true);
        }
        this.onPick = onPick;
        scroll = 0;
        shown = true;
        opening = new Opening(skin);
        opening.start();
    }

    void close() {
        if (shown)
            opening.close();
    }

    private void filter(String text) {
        String q = text.trim().toLowerCase(Locale.ROOT);
        entries = q.isEmpty() ? all : all.stream().filter(e -> e.label().getString().toLowerCase(Locale.ROOT).contains(q)).toList();
        scroll = 0;
    }

    private int searchH() {
        return search == null ? 0 : SEARCH_H + 2;
    }

    private int rows() {
        return Math.min(entries.size(), MAX_ROWS);
    }

    private int listY() {
        return y + PAD + TITLE_H + searchH();
    }

    /** The entry under the pointer, or -1. */
    private int at(double mx, double my) {
        if (mx < x + PAD || mx >= x + W - PAD || my < listY() || my >= listY() + rows() * ROW_H)
            return -1;
        int i = scroll + (int) (my - listY()) / ROW_H;
        return i < entries.size() ? i : -1;
    }

    /** Drawn over the host's panel at ({@code hx}, {@code hy}), {@code hw} by {@code hh}, centred on it. */
    void render(GuiGraphics g, int hx, int hy, int hw, int hh, int mx, int my) {
        if (!shown)
            return;
        if (opening.shut()) {
            shown = false;
            return;
        }
        h = 2 * PAD + TITLE_H + searchH() + rows() * ROW_H;
        x = hx + (hw - W) / 2;
        y = hy + (hh - h) / 2;
        var font = Minecraft.getInstance().font;
        boolean live = opening.settled();
        int hover = live ? at(mx, my) : -1;
        g.pose().pushPose();
        g.pose().translate(0, 0, 200); // above the host's widgets and slots
        opening.render(g, g.guiWidth(), g.guiHeight(), x, y, W, h, 0.5f, () -> {
            skin.panel(g, x, y, W, h);
            g.drawString(font, title, x + (W - font.width(title)) / 2, y + PAD + 3, skin.ink(), false);
            if (search != null) {
                int bx = x + PAD, by = y + PAD + TITLE_H;
                skin.inset(g, bx, by, W - 2 * PAD, SEARCH_H);
                search.setX(bx + 4);
                search.setY(by + 4);
                search.render(g, mx, my, 0);
            }
            if (entries.isEmpty())
                g.drawString(font, Component.translatable("gui.feedback.popup.none"), x + PAD + 5, listY() + 5, skin.dim(), false);
            for (int r = 0; r < rows(); r++) {
                int i = scroll + r, ry = listY() + r * ROW_H;
                skin.plaque(g, x + PAD, ry + 1, W - 2 * PAD, ROW_H - 2);
                skin.row(g, x + PAD, ry + 1, W - 2 * PAD, ROW_H - 2, false, i == hover);
                g.drawString(font, entries.get(i).label(), x + PAD + 5, ry + 5, i == hover ? skin.accent() : skin.ink(), false);
            }
            skin.scrollbar(g, x + W - PAD + 1, listY(), rows() * ROW_H, entries.size(), rows(), scroll);
        });
        if (hover >= 0 && !entries.get(hover).tip().isEmpty())
            g.renderComponentTooltip(font, entries.get(hover).tip(), mx, my);
        g.pose().popPose();
    }

    /** A click: picks the entry under it, or closes if it was outside the window. Takes every click while up. */
    boolean mouseClicked(double mx, double my, int button) {
        if (!shown)
            return false;
        if (!opening.settled())
            return true;
        if (search != null && mx >= x + PAD && mx < x + W - PAD && my >= y + PAD + TITLE_H && my < y + PAD + TITLE_H + SEARCH_H) {
            search.setFocused(true);
            return true;
        }
        int i = button == 0 ? at(mx, my) : -1;
        if (i >= 0) {
            GuiSkin.play(skin.click(), 1f);
            onPick.accept(entries.get(i).value());
            opening.close();
        } else if (mx < x || mx >= x + W || my < y || my >= y + h)
            opening.close();
        return true;
    }

    boolean mouseScrolled(double scrollY) {
        if (!shown)
            return false;
        if (entries.size() > MAX_ROWS)
            scroll = Math.max(0, Math.min(entries.size() - MAX_ROWS, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    /** Escape closes the popup; every other key goes to the search box, and is the popup's either way while it is up. */
    boolean keyPressed(int key, int scan, int modifiers) {
        if (!shown)
            return false;
        if (key == GLFW.GLFW_KEY_ESCAPE && opening.settled())
            opening.close();
        else if (search != null && opening.settled())
            search.keyPressed(key, scan, modifiers);
        return true;
    }

    boolean charTyped(char c, int modifiers) {
        if (!shown)
            return false;
        if (search != null && opening.settled())
            search.charTyped(c, modifiers);
        return true;
    }
}
