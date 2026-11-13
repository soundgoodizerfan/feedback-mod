// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;


import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** The Calculator app: {@code + - * / ^} and brackets, buttons or the keyboard, Enter or = to evaluate, ans for the last result. */
final class CalculatorWindow extends DesktopWindow {

    private static final String[][] KEYS = {
            {"C", "(", ")", "<", "/"},
            {"7", "8", "9", "^", "*"},
            {"4", "5", "6", "-", "+"},
            {"1", "2", "3", ".", "ans"},
    };

    private final ComputerContext ctx;
    private StringBuilder entry = new StringBuilder();
    private String last = "", shown = "0";
    private double answer;

    CalculatorWindow(ComputerContext ctx) {
        super(80, 10, 128, 152, 128, 152, false);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.calculator");
    }

    @Override
    public Glyph glyph() {
        return Glyph.CALC;
    }

    @Override
    protected void build(GuiSkin skin) {
        int cell = 24;
        for (int r = 0; r < KEYS.length; r++)
            for (int c = 0; c < KEYS[r].length; c++) {
                final String key = KEYS[r][c];
                add(GuiStyle.button(skin, Component.literal(key), b -> press(key), 0, 0, cell, 18), c * (cell + 1), 34 + r * 20);
            }
        add(GuiStyle.button(skin, Component.literal("0"), b -> press("0"), 0, 0, cell, 18), 0, 34 + 4 * 20);
        add(GuiStyle.button(skin, Component.literal("00"), b -> press("00"), 0, 0, cell, 18), cell + 1, 34 + 4 * 20);
        add(GuiStyle.primary(skin, Component.literal("="), b -> evaluate(), 0, 0, cell * 3 + 2, 18, null), 2 * (cell + 1), 34 + 4 * 20);
    }

    private void press(String key) {
        switch (key) {
            case "C" -> {
                entry.setLength(0);
                last = "";
                shown = "0";
            }
            case "<" -> {
                if (!entry.isEmpty())
                    entry.setLength(entry.length() - 1);
            }
            case "ans" -> entry.append(CalcEval.show(answer));
            default -> {
                if (entry.length() < 40)
                    entry.append(key);
            }
        }
        shown = entry.isEmpty() ? "0" : entry.toString();
    }

    private void evaluate() {
        if (entry.isEmpty())
            return;
        double value = CalcEval.eval(entry.toString());
        last = entry + " =";
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            shown = "error";
            entry.setLength(0);
            return;
        }
        answer = value;
        shown = CalcEval.show(value);
        entry = new StringBuilder(shown);
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ui.inset(0, 0, ui.w(), 30);
        ui.clipped(4, 3, ui.w() - 8, last, skin.dim());
        String text = shown.length() > 18 ? shown.substring(shown.length() - 18) : shown;
        ui.right(ui.w() - 4, 15, text, skin.accent());
    }

    @Override
    protected boolean charTyped(char c, int modifiers) {
        if ("0123456789.+-*/^()".indexOf(c) >= 0) {
            press(String.valueOf(c));
            return true;
        }
        if (c == '=') {
            evaluate();
            return true;
        }
        return false;
    }

    @Override
    protected boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 257 || key == 335) {
            evaluate();
            return true;
        }
        if (key == 259) {
            press("<");
            return true;
        }
        // digits are the window's, not the hotbar's
        return key >= 48 && key <= 57 || key >= 320 && key <= 329;
    }
}
