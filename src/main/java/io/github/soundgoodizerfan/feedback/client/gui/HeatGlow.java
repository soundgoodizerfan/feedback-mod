// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.bench.BenchFire;
import io.github.soundgoodizerfan.feedback.core.thermal.Incandescence;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Light a screen throws from the heat of the thing opened: a bloom behind a slot holding glowing stock, and the fire's flicker.
 * Stateless draw helpers; colour is the bench fire's ({@link BenchFire#colour}), so a bar glows here as it does on the anvil.
 * Nothing draws below the faint red band.
 */
public final class HeatGlow {

    private static final RandomSource RNG = RandomSource.create();

    private HeatGlow() {
    }

    /** The fire's brightness multiplier, about 0.85 to 1.15, two summed sines; 1 under reduce motion. */
    public static float pulse() {
        if (ClientSettings.reduceMotion())
            return 1f;
        float t = GuiStyle.time();
        return 1f + 0.09f * Mth.sin(t * 0.55f) + 0.06f * Mth.sin(t * 1.17f + 1.3f);
    }

    /** A bloom behind the 16x16 item at (x, y), in the glow colour of a body at {@code tu}; nothing while it is too cool to glow. */
    public static void slot(GuiGraphics g, int x, int y, float tu) {
        if (Incandescence.light(tu) <= 0)
            return;
        float heat = Mth.clamp(tu / ThermalTuning.MAX_VISIBLE_TU.value(), 0.15f, 1f) * pulse();
        int rgb = BenchFire.colour(tu);
        for (int k = 0; k < 3; k++) { // three rings growing outward, fainter each
            int a = (int) Mth.clamp(0x58 * heat / (k + 1), 0, 0xFF);
            g.fill(x - 1 - k, y - 1 - k, x + 17 + k, y + 17 + k, a << 24 | rgb);
        }
    }

    /** Called once a tick per glowing slot at (x, y), panel-relative: sparks lift off, more of them the hotter it is. Silent under reduce motion or below the faint red band. */
    public static void embers(Specks specks, int x, int y, net.minecraft.world.item.ItemStack stack) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null)
            embers(specks, x, y, io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(stack, level).value());
    }

    /** The bloom of the stack in a slot, at its own heat. */
    public static void slot(GuiGraphics g, int x, int y, net.minecraft.world.item.ItemStack stack) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null)
            slot(g, x, y, io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(stack, level).value());
    }

    public static void embers(Specks specks, int x, int y, float tu) {
        if (ClientSettings.reduceMotion() || Incandescence.light(tu) <= 0)
            return;
        float heat = Mth.clamp(tu / ThermalTuning.MAX_VISIBLE_TU.value(), 0.1f, 1f);
        if (RNG.nextFloat() < 0.05f + 0.3f * heat)
            specks.spawn(x + RNG.nextInt(16), y + 10, (RNG.nextFloat() - 0.5f) * 0.4f, -0.3f - 0.5f * RNG.nextFloat(),
                    BenchFire.colour(tu) | 0x606060, 14 + RNG.nextInt(20), 1);
    }

    /** Called once a tick per food slot at (x, y): a pale puff drifting up. */
    public static void steam(Specks specks, int x, int y) {
        if (!ClientSettings.reduceMotion() && RNG.nextFloat() < 0.12f)
            specks.spawn(x + 3 + RNG.nextInt(10), y + 4, (RNG.nextFloat() - 0.5f) * 0.3f, -0.25f - 0.2f * RNG.nextFloat(),
                    0xE8E8E8, 24 + RNG.nextInt(12), 2);
    }

    /** Called once a tick for a hot melt filling (x, y, w, h), panel-relative: a bubble rises from inside it. */
    public static void bubble(Specks specks, int x, int y, int w, int h, int rgb) {
        if (!ClientSettings.reduceMotion() && h > 2 && RNG.nextFloat() < 0.2f)
            specks.spawn(x + 1 + RNG.nextInt(Math.max(1, w - 3)), y + 2 + RNG.nextInt(h - 2), 0f, -0.45f,
                    rgb | 0x404040, 8 + RNG.nextInt(10), 2);
    }
}
