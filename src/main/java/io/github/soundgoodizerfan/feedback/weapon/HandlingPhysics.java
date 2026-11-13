// Copyright (C) 2026 soundgoodizerfan/feedback
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.combat.gear.GearTuning;
import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * What the fittings that hold a gun steady do, from the metal of each: a bipod's leg or a stock's core is as stiff as its yield
 * strength lets it be, a stock's pad cushions by being soft and tough, a foregrip holds by friction over a stiff frame, and a
 * counterweight runs against the bolt by its share of the gun's mass. Every method returns multipliers (1 changes nothing).
 */
public final class HandlingPhysics {

    private HandlingPhysics() {
    }

    /** Kick and spread multipliers of a fitting. */
    public record Brace(float kick, float spread) {
    }

    /** How stiff {@code m} holds, 0 to 1: its yield strength against the half point. */
    public static float rigidity(Material m) {
        return GearTuning.saturate(m == null ? 0f : m.mechanical().yieldStrength().map(y -> y.value()).orElse(0f), GunTuning.HAND_YIELD_HALF);
    }

    /** How well {@code m} cushions a blow, 0 to 1: soft, and tough enough not to tear. */
    public static float cushion(Material m) {
        if (m == null)
            return 0f;
        float soft = 1f - GearTuning.saturate(m.hardness().value(), GunTuning.HAND_PAD_SOFT_HALF);
        return soft * GearTuning.saturate(m.mechanical().toughness().map(t -> t.value()).orElse(0f), GunTuning.HAND_PAD_TOUGH_HALF);
    }

    /** A bipod's {@code leg} metal, braced on the ground. */
    public static Brace bipod(Material leg) {
        float r = rigidity(leg);
        return new Brace(1f - GunTuning.BIPOD_MAX_KICK_CUT * r, 1f - GunTuning.BIPOD_MAX_SPREAD_CUT * r);
    }

    /** A stock of {@code length} (1 fixed, less for a skeleton or a folding one): its pad takes the kick, its core holds the aim. */
    public static Brace stock(float length, Material core, Material pad) {
        return new Brace(1f - GunTuning.STOCK_MAX_KICK_CUT * length * cushion(pad), 1f - GunTuning.STOCK_MAX_SPREAD_CUT * length * rigidity(core));
    }

    /** A foregrip of {@code leverage} in {@code m}: the off hand grips it by friction, and a stiff frame keeps the muzzle down. */
    public static Brace foregrip(float leverage, Material m) {
        float grip = GearTuning.saturate(m == null ? 0f : m.mechanical().friction().orElse(0f), GunTuning.HAND_FRICTION_HALF);
        return new Brace(1f - GunTuning.FOREGRIP_MAX_KICK_CUT * leverage * grip, 1f - GunTuning.FOREGRIP_MAX_SPREAD_CUT * leverage * rigidity(m));
    }

    /** The counterweight's own mass over the gun's. */
    public static float share(Material m, GunOption o, float gunMu) {
        return gunMu <= 0f ? 0f : o.metalMb() * m.density().muPerMb() / gunMu;
    }

    /** Kick multiplier of a counterweight of {@code share} of the gun's mass, as built. */
    public static float counterKick(float share) {
        return 1f / (1f + GunTuning.CW_KICK_GAIN * share);
    }

    /** Cycle multiplier: the moving mass slows the bolt. */
    public static float counterCycle(float share) {
        return 1f + GunTuning.CW_CYCLE_GAIN * share;
    }

    /** Kick multiplier at full wear: the stroke is cut short and the mass slaps the receiver, worse the more brittle it is. */
    public static float counterSlap(Material m) {
        float brittle = 1f - GearTuning.saturate(m.mechanical().toughness().map(t -> t.value()).orElse(0f), GunTuning.CW_BRITTLE_TOUGH_HALF);
        return 1f + GunTuning.CW_SLAP_BRITTLE * (0.5f + brittle);
    }

    /** A belt's reload multiple: a rougher feed way drags the chain. */
    public static float beltReload(Material m) {
        return 1f + m.mechanical().friction().orElse(GunTuning.FEED_REF_FRICTION) / GunTuning.FEED_REF_FRICTION;
    }

    /**
     * The chance a pull on a box or belt feed jams: a lip of tough metal springs back, a brittle or soft one is bent by the recoil, and
     * a harder-kicking gun bends it sooner. 0 for a tough lip on a mild gun.
     */
    public static float jamChance(Material feed, float impulse) {
        if (feed == null)
            return 0f;
        float tough = GearTuning.saturate(feed.mechanical().toughness().map(t -> t.value()).orElse(0f), GunTuning.FEED_JAM_TOUGH_HALF);
        return GunTuning.FEED_JAM_MAX * (1f - tough) * GearTuning.saturate(impulse, GunTuning.FEED_JAM_IMPULSE_HALF);
    }
}
