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

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.combat.cast.CastState;
import io.github.soundgoodizerfan.feedback.combat.cast.Casts;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The cast radial, opened by holding {@link CombatKeys#RADIAL}: the five cast slots round a ring. It only SELECTS (the
 * choice is client-side state, {@link #selected()}); casting is a tap of the radial key, which sends the ordinary
 * {@code Fire} the server checks. Letting the key go, or a click, takes the slot the pointer is over.
 */
final class CastRadialScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;
    private static final int RADIUS = 64, CHIP_W = 84, CHIP_H = 14, DEAD = 16;

    private static int selected = 0;

    CastRadialScreen() {
        super(Component.translatable("key.feedback.cast_radial"));
    }

    /** The slot a tap of the radial key casts: the chosen one if it still holds a technique, else the first that does, else -1. */
    static int selected() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null)
            return -1;
        var slots = CastState.of(mc.player).slots();
        if (selected < slots.size() && !slots.get(selected).isEmpty())
            return selected;
        for (int i = 0; i < slots.size(); i++)
            if (!slots.get(i).isEmpty())
                return i;
        return -1;
    }

    private static double angle(int i) {
        return -Math.PI / 2 + i * 2 * Math.PI / CastState.SLOTS;
    }

    /** The slot whose direction the pointer is nearest, or -1 while it is still near the middle. */
    private int hovered(double mx, double my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        if (dx * dx + dy * dy < DEAD * DEAD)
            return -1;
        double at = Math.atan2(dy, dx);
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < CastState.SLOTS; i++) {
            double d = Math.abs(Math.atan2(Math.sin(at - angle(i)), Math.cos(at - angle(i))));
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private void choose(double mx, double my) {
        int h = hovered(mx, my);
        if (h >= 0 && !CastState.of(minecraft.player).slots().get(h).isEmpty())
            selected = h;
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // The world stays in view: no blur, no dimming.
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        var slots = CastState.of(minecraft.player).slots();
        int hov = hovered(mx, my), sel = selected(), cx = width / 2, cy = height / 2;
        GuiStyle.hudCard(g, cx - DEAD, cy - DEAD, cx + DEAD, cy + DEAD);
        for (int i = 0; i < slots.size(); i++) {
            int x = cx + (int) Math.round(Math.cos(angle(i)) * RADIUS) - CHIP_W / 2, y = cy + (int) Math.round(Math.sin(angle(i)) * RADIUS) - CHIP_H / 2;
            String tech = slots.get(i);
            var cast = Casts.find(tech);
            SKIN.plaque(g, x, y, CHIP_W, CHIP_H);
            SKIN.row(g, x, y, CHIP_W, CHIP_H, i == sel, i == hov && cast != null);
            Component name = cast == null ? Component.translatable("gui.feedback.cast_empty")
                    : Component.translatableWithFallback("skill.feedback.move." + tech, tech);
            g.drawCenteredString(font, font.substrByWidth(name, CHIP_W - 4).getString(), x + CHIP_W / 2, y + 3, cast == null ? SKIN.dim() : i == sel ? SKIN.accent() : SKIN.ink());
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        choose(mx, my);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (CombatKeys.RADIAL.matchesMouse(button)) {
            choose(mx, my);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (CombatKeys.RADIAL.matches(key, scan)) {
            var mouse = minecraft.mouseHandler;
            choose(mouse.xpos() * width / minecraft.getWindow().getScreenWidth(), mouse.ypos() * height / minecraft.getWindow().getScreenHeight());
            return true;
        }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public void tick() {
        // A release that landed before the screen existed would never reach keyReleased.
        var k = CombatKeys.RADIAL.getKey();
        if (k.getType() == InputConstants.Type.KEYSYM && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), k.getValue()))
            onClose();
    }
}
