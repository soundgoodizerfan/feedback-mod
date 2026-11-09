// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The small pictograms of the desktop: an 8 x 8 one-colour mask each, drawn as fills in whatever colour the skin gives, so they take
 * the phosphor. Shown on a window's title bar, in the launcher menu and by a desktop icon.
 */
public enum Glyph {
    /** A disk. */
    DRIVE(0b00111100, 0b01000010, 0b01011010, 0b01000010, 0b01111110, 0b01000010, 0b01000010, 0b00111100),
    /** A screen with a trace across it. */
    MONITOR(0b11111111, 0b10000001, 0b10010001, 0b10101001, 0b10000101, 0b10000001, 0b11111111, 0b00111100),
    /** Two linked rings. */
    LINKS(0b00111000, 0b01000100, 0b01000100, 0b00111110, 0b00010001, 0b00010001, 0b00001110, 0b00000000),
    /** A node with two wires. */
    EDITOR(0b11100000, 0b10100000, 0b11101110, 0b00001010, 0b00001010, 0b11101110, 0b10100000, 0b11100000),
    /** A slider panel. */
    SETTINGS(0b00100100, 0b00100100, 0b11111111, 0b00100100, 0b00100100, 0b11111111, 0b00100100, 0b00100100),
    /** A prompt. */
    CONSOLE(0xFF, 0x81, 0xA1, 0x91, 0xA1, 0x8F, 0x81, 0xFF),
    /** A trace in a graticule. */
    SCOPE(0xFF, 0x81, 0x85, 0x89, 0xB1, 0xA1, 0x81, 0xFF),
    /** A crosshair on a plot. */
    MAP(0x18, 0x18, 0x3C, 0xFF, 0xFF, 0x3C, 0x18, 0x18),
    /** A page of lines. */
    NOTES(0x7E, 0x42, 0x5A, 0x42, 0x5A, 0x42, 0x7E, 0x00),
    /** Two arrows. */
    CONVERT(0x08, 0x0C, 0xFE, 0x0C, 0x08, 0x20, 0x60, 0xFE),
    /** A shelf of media. */
    LIBRARY(0xEE, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA, 0xFF),
    /** A bolt. */
    POWER(0x0E, 0x1C, 0x38, 0x7E, 0x1C, 0x38, 0x70, 0x20),
    /** A game pad. */
    GAME(0x00, 0x7E, 0xDB, 0xFF, 0xE7, 0xFF, 0x7E, 0x00),
    /** A calculator. */
    CALC(0x7E, 0x42, 0x7E, 0x56, 0x7E, 0x56, 0x7E, 0x7E),
    /** A drafting square. */
    BLUEPRINT(0xFF, 0x81, 0xBD, 0xA5, 0xA5, 0xBD, 0x81, 0xFF),
    /** A folder. */
    FILES(0b01110000, 0b11111110, 0b10000010, 0b10000010, 0b10000010, 0b10000010, 0b11111110, 0b00000000),
    /** A bin. */
    TRASH(0b00111000, 0b11111110, 0b01111100, 0b01010100, 0b01010100, 0b01010100, 0b01111100, 0b00000000),
    /** A plain window. */
    APP(0b11111111, 0b10000001, 0b11111111, 0b10000001, 0b10000001, 0b10000001, 0b10000001, 0b11111111);

    private final int[] rows;

    Glyph(int... rows) {
        this.rows = rows;
    }

    /** Draws the mask with its top-left at (x, y), each lit cell one pixel. */
    public void draw(GuiGraphics g, int x, int y, int colour) {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                if ((rows[row] >> (7 - col) & 1) != 0)
                    g.fill(x + col, y + row, x + col + 1, y + row + 1, colour);
    }
}
