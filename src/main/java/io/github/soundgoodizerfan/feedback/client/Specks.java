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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;

/**
 * A fixed pool of screen-space specks (squirt, splash, sparkle), advanced in the screen's {@code tick} and drawn as fills:
 * vanilla's particle engine only draws in the world, so a screen fakes it (the Skillet's pool, made general). Nothing is
 * allocated after construction; a full pool overwrites its oldest speck.
 */
final class Specks {

    private final float[] x, y, vx, vy;
    private final int[] life, span, rgb, size;
    private final RandomSource rng = RandomSource.create();
    private int next;

    Specks(int n) {
        x = new float[n];
        y = new float[n];
        vx = new float[n];
        vy = new float[n];
        life = new int[n];
        span = new int[n];
        rgb = new int[n];
        size = new int[n];
    }

    void spawn(float px, float py, float sx, float sy, int colour, int ticks, int pixels) {
        int i = next;
        next = (next + 1) % life.length;
        x[i] = px;
        y[i] = py;
        vx[i] = sx;
        vy[i] = sy;
        life[i] = span[i] = Math.max(1, ticks);
        rgb[i] = colour & 0xFFFFFF;
        size[i] = pixels;
    }

    /** {@code n} specks thrown out from a point: {@code speed} sideways at most, {@code up} upward at most. */
    void burst(float px, float py, int n, float speed, float up, int colour, int ticks, int pixels) {
        for (int k = 0; k < n; k++)
            spawn(px, py, (rng.nextFloat() * 2f - 1f) * speed, -rng.nextFloat() * up, colour, ticks / 2 + rng.nextInt(ticks / 2 + 1), pixels);
    }

    void tick(float gravity) {
        for (int i = 0; i < life.length; i++) {
            if (life[i] <= 0)
                continue;
            life[i]--;
            x[i] += vx[i];
            y[i] += vy[i];
            vy[i] += gravity;
        }
    }

    void draw(GuiGraphics g, float partial) {
        for (int i = 0; i < life.length; i++) {
            if (life[i] <= 0)
                continue;
            int a = Math.min(255, 60 + 195 * life[i] / span[i]);
            int px = Math.round(x[i] + vx[i] * partial), py = Math.round(y[i] + vy[i] * partial);
            g.fill(px, py, px + size[i], py + size[i], a << 24 | rgb[i]);
        }
    }
}
