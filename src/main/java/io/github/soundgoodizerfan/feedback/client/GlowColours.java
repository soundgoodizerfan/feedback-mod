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

import io.github.soundgoodizerfan.feedback.core.thermal.Incandescence;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** The colour of a glowing body by light level (spec incandescence): dull red, orange, yellow, white. Client only. */
public final class GlowColours {

    private GlowColours() {
    }

    /** 0 to 1 along the ramp for a light level from 2 (faint red) to 15 (brilliant white). */
    static float along(float level) {
        return Mth.clamp((level - 2f) / 13f, 0f, 1f);
    }

    /** The glow colour at {@code t} along the ramp, as r, g, b in 0 to 1. */
    static float[] rgb(float t) {
        if (t < 0.5f) {
            float u = t / 0.5f;
            return new float[] {Mth.lerp(u, 0.55f, 1f), Mth.lerp(u, 0.05f, 0.45f), Mth.lerp(u, 0f, 0.05f)};
        }
        float u = (t - 0.5f) / 0.5f;
        return new float[] {1f, Mth.lerp(u, 0.45f, 1f), Mth.lerp(u, 0.05f, 0.9f)};
    }

    /**
     * {@code base} (0xRRGGBB) as the stack looks: drawn toward the glow colour by how hot it is, so an ingot called "white hot"
     * looks it wherever it is shown. A cool stack keeps its own colour.
     */
    static int heated(int base, ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null || !stack.has(FDataComponents.TEMPERATURE.get()))
            return base;
        return heated(base, io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(stack, level).value());
    }

    /** {@code base} (0xRRGGBB) drawn toward the glow colour of a body at {@code tu}. */
    public static int heated(int base, float tu) {
        float glow = Incandescence.light(tu);
        if (glow <= 0f)
            return base;
        float t = along(glow);
        float[] c = rgb(t);
        float mix = 0.4f + 0.5f * t;
        int r = Math.round(Mth.lerp(mix, (base >> 16) & 0xFF, c[0] * 255f));
        int g = Math.round(Mth.lerp(mix, (base >> 8) & 0xFF, c[1] * 255f));
        int b = Math.round(Mth.lerp(mix, base & 0xFF, c[2] * 255f));
        return r << 16 | g << 8 | b;
    }
}
