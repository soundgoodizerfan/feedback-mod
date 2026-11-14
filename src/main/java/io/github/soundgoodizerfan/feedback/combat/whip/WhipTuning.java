// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.whip;

import io.github.soundgoodizerfan.feedback.Feedback;

/**
 * The Whiplash's figures. Every number is a placeholder to be felt in play. Ticks are server ticks (20 a second). The line itself
 * (length, reel, rating, bites) is {@code GrappleHook}'s and {@code ModularTuning}'s {@code GRAPPLE_*}: the arm throws a grapple hook.
 */
public final class WhipTuning {

    /** Technique id the Whiplash grants: throw a barbed hook on its own key, pull on release. */
    public static final net.minecraft.resources.ResourceLocation WHIPLASH = Feedback.id("whiplash");

    /** Ticks before the next throw, and the stamina a throw spends. */
    public static final int COOLDOWN = 10;
    public static final float STAMINA = 6f;
    /** Wear points a throw costs the arm (tools wear with use). */
    public static final int WEAR_THROW = 1;
    /** Damage of a bite on a creature: the barb, not a weapon. */
    public static final float BITE_DAMAGE = 1f;
    /** The speed a carried projectile leaves the arm at, over the speed it was caught at, and the blocks a coin in the spear's path is touched from. */
    public static final float RETURN_SPEED = 1.5f;
    public static final double COIN_TOUCH_RADIUS = 1.2;

    private WhipTuning() {
    }
}
