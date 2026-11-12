// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Text a JEI card shares: a label and its value on one row, whatever the words are. */
final class CardText {

    private static final int GAP = 6;

    private CardText() {
    }

    /**
     * A label at {@code labelX} and its value right-aligned to the card's edge ({@code width}), so neither runs into the other however long the
     * label is. If both cannot fit side by side the value goes on a second line under the label. Returns the lines used (1 or 2); a card whose
     * rows are stacked adds that many row heights.
     */
    static int row(GuiGraphics graphics, Font font, int width, int labelX, int y, Component label, String value, int labelColour, int valueColour) {
        return row(graphics, font, width, labelX, y, label, value, labelColour, valueColour, true);
    }

    /**
     * As above; with {@code wrap} false the row stays one line whatever the words are: the value is cut with an ellipsis to the room the label leaves
     * (for a card whose rows sit at fixed heights beside slots, where a slot's tooltip carries the full name).
     */
    static int row(GuiGraphics graphics, Font font, int width, int labelX, int y, Component label, String value, int labelColour, int valueColour, boolean wrap) {
        if (!wrap) {
            graphics.drawString(font, label.getString(), labelX, y, labelColour, false);
            int room = width - 1 - labelX - font.width(label.getString()) - GAP;
            String shown = font.width(value) <= room ? value : font.plainSubstrByWidth(value, Math.max(0, room - font.width("..."))) + "...";
            graphics.drawString(font, shown, width - 1 - font.width(shown), y, valueColour, false);
            return 1;
        }
        int valueWidth = font.width(value), labelWidth = font.width(label.getString());
        boolean sideBySide = labelX + labelWidth + GAP + valueWidth <= width - 1;
        String text = label.getString();
        int room = width - 1 - labelX;
        if (labelWidth > room)
            text = font.plainSubstrByWidth(text, Math.max(0, room - font.width("..."))) + "...";
        graphics.drawString(font, text, labelX, y, labelColour, false);
        String shown = value;
        if (valueWidth > width - 1 - labelX)
            shown = font.plainSubstrByWidth(value, width - 1 - labelX - font.width("...")) + "...";
        graphics.drawString(font, shown, width - 1 - font.width(shown), sideBySide ? y : y + 9, valueColour, false);
        return sideBySide ? 1 : 2;
    }
}
