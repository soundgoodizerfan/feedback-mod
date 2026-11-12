// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only settings (feedback-client.toml). Registered from {@code Feedback} on the client dist only. */
public final class ClientSettings {

    private ClientSettings() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue REDUCE_MOTION = B
            .comment("Drops screen shake, shutters and idle life; keeps sounds and the stage lamp.")
            .define("reduceMotion", false);

    private static final ModConfigSpec.BooleanValue ITEM_INFO_HUD = B
            .comment("Held-item info HUD shown (also toggled in game by its key).")
            .define("itemInfoHud", true);

    private static final ModConfigSpec.BooleanValue GUN_ARMS = B
            .comment("Draw your arms on a gun held in first person.")
            .define("gunArms", true);

    private static final ModConfigSpec.BooleanValue GAIN_WISP = B
            .comment("A killing blow that earns skill points sends one small wisp in the tree's colour from the fallen creature to you. Applies at once, no restart.")
            .define("gainWisp", true);

    /** How big the crosshair HUD ({@link AimHud}) draws: its dial, needle, mode strip and link chip. */
    public enum AimSize {
        SMALL(1f), MEDIUM(1.5f), LARGE(2f);

        final float scale;

        AimSize(float scale) {
            this.scale = scale;
        }
    }

    private static final ModConfigSpec.EnumValue<AimSize> AIM_SIZE = B
            .comment("Size of the readings, needles, mode strips and link chips drawn by the crosshair.")
            .defineEnum("aimHudSize", AimSize.SMALL);

    public static final ModConfigSpec SPEC = B.build();

    public static float aimScale() {
        return SPEC.isLoaded() ? AIM_SIZE.get().scale : 1f;
    }

    /** True when the player asked for less motion. False before the config has loaded. */
    public static boolean reduceMotion() {
        return SPEC.isLoaded() && REDUCE_MOTION.get();
    }

    public static boolean gainWisp() {
        return !SPEC.isLoaded() || GAIN_WISP.get();
    }

    public static boolean gunArms() {
        return !SPEC.isLoaded() || GUN_ARMS.get();
    }

    public static boolean itemInfoHud() {
        return !SPEC.isLoaded() || ITEM_INFO_HUD.get();
    }

    /** Sets and writes the file, so the choice survives a restart. */
    public static void setItemInfoHud(boolean v) {
        if (!SPEC.isLoaded()) return;
        ITEM_INFO_HUD.set(v);
        ITEM_INFO_HUD.save();
    }
}
