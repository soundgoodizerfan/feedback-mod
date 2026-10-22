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
package io.github.soundgoodizerfan.feedback.combat.parry;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.util.Mth;

/**
 * Every figure of the parry, and the one curve that turns a weapon into a window (implementation plan
 * 2.2, section 5). Ticks are server ticks.
 *
 * <p>The curve. A weapon's <i>heft</i> is its baked mass, lengthened by its reach:
 * {@code heft = MASS * (1 + HEFT_REACH * REACH)}. Heft runs from {@link #HEFT_LIGHT} (a dagger) to
 * {@link #HEFT_HEAVY} (a greatsword) as a fraction {@code u}, and both figures are linear in it. The
 * ruling is that heavy and long is a <i>wide window and a huge lockout</i>: a long heavy blade sweeps
 * across a lot of angle, so it meets a blow for longer, and is slow to bring back when it misses. A
 * dagger is a sliver of a chance and cheap to miss with. All of this is placeholder until played.
 */
public final class ParryTuning {

    /** How much a block of extra reach adds to heft, as a fraction of mass. */
    public static final float HEFT_REACH = 0.5f;
    /** Heft at or below which a weapon gets the light figures (a dagger), and at or above the heavy ones (a greatsword). */
    public static final float HEFT_LIGHT = 0.5f, HEFT_HEAVY = 6f;

    /** Active window, ticks: light end and heavy end (dagger about 4, greatsword about 12). */
    public static final int WINDOW_LIGHT = 4, WINDOW_HEAVY = 12;
    /** Recovery lockout after the window, ticks (dagger about 10, greatsword about 35). */
    public static final int RECOVERY_LIGHT = 10, RECOVERY_HEAVY = 35;
    /** Most ticks the server adds to the window for the presser's latency. Never more, so ping cannot buy a parry. */
    public static final int PING_CAP = 4;

    /** Half-angle of the frontal cone, degrees: a point (near 0 balance) and a head-heavy weapon (near 1). */
    public static final float CONE_MIN_DEG = 35f, CONE_BALANCE_DEG = 55f;
    /** A shield's face is wide whatever its balance. */
    public static final float CONE_SHIELD_DEG = 75f;

    /**
     * STAND-IN: a living thing's mass in Mu, its bounding-box volume (blocks cubed) times this. A player's
     * box is 0.6 x 1.8 x 0.6 = 0.65 and weighs about 80, which is where 120 comes from. Real mobs have no
     * mass figure yet; this is the flagged exception for mobs (plan section 5, item 18).
     */
    public static final float BBOX_DENSITY = 120f;
    /** Joules an unarmed or unfigured blow carries per point of damage, for an attacker with no baked IMPACT. */
    public static final float JOULES_PER_DAMAGE = 10f;
    /** Stagger ticks per joule of impact over the attacker's mass (J/Mu), within the bounds below. */
    public static final float STAGGER_PER_SPECIFIC_IMPACT = 16f;
    public static final int STAGGER_MIN = 4, STAGGER_MAX = 40;
    /** Fraction of walking speed lost while staggered. */
    public static final double STAGGER_SLOW = 0.6;

    /** Joules of blow the parrying weapon takes per Mu of its own mass before it is over its build. */
    public static final float RATING_PER_MU = 60f;
    /** Wear points a clean parry costs, and extra per whole multiple of its rating a heavier blow is over by. */
    public static final int WEAR_CLEAN = 1, WEAR_PER_OVER = 4;

    /** A redirected projectile's speed over its incoming speed, and its inaccuracy (vanilla bow is 1). */
    public static final float REDIRECT_SPEED = 1.5f, REDIRECT_INACCURACY = 2f;

    /** Technique id the Feedbacker grants: a parry that punches any projectile back. */
    public static final net.minecraft.resources.ResourceLocation PUNCH_PROJECTILES = Feedback.id("punch_projectiles");
    /** The Feedbacker's window and recovery, ticks, and the half-angle of its cone (a bare fist: no weapon heft to read). */
    public static final int FEEDBACKER_WINDOW = 12, FEEDBACKER_RECOVERY = 20;
    public static final float FEEDBACKER_CONE_DEG = 75f;
    /** A punched projectile's speed over its incoming speed (a plain parry turns it at {@link #REDIRECT_SPEED}). */
    public static final float FEEDBACKER_SPEED = 2.5f;
    /**
     * Impulse (Mu blocks/tick; STAND-IN projectile mass as for {@link #BBOX_DENSITY}) the gauntlet's joints take
     * before they are over their build, times 1 plus the worn gear's joint share. An arrow is about 45, a ghast
     * fireball about 120 to 150, a heavier body far over.
     */
    public static final float FEEDBACKER_RATING = 150f;
    /** Wear per punch: extra per whole rating of impulse (on top of {@link #WEAR_CLEAN}), and the wreck when over the rating. */
    public static final int FEEDBACKER_WEAR_PER_RATING = 12, FEEDBACKER_WRECK_WEAR = 150;

    private ParryTuning() {
    }

    /** STAND-IN mass of a living thing, Mu: bounding-box volume times {@link #BBOX_DENSITY}. Shared by the parry and the gun bludgeon. A worn back piece adds its mass (a heavier body staggers shorter). */
    public static float standInMass(net.minecraft.world.entity.LivingEntity who) {
        return who.getBbWidth() * who.getBbWidth() * who.getBbHeight() * BBOX_DENSITY
                + io.github.soundgoodizerfan.feedback.combat.gear.CombatGear.of(who).mass();
    }

    /** Riposte (skill unlock): ticks after a landed parry in which the parrier's next strike is boosted, and the damage multiplier. */
    public static final int RIPOSTE_TICKS = 30;
    public static final float RIPOSTE_MULT = 1.5f;

    /** Unclamped stagger ticks of a blow of {@code impact} joules on a body of {@code mass} Mu. */
    public static float rawStaggerTicks(float impact, float mass) {
        return impact / mass * STAGGER_PER_SPECIFIC_IMPACT;
    }

    /** Stagger ticks the parry delivers: the raw figure within the bounds. */
    public static int staggerTicks(float impact, float mass) {
        return Mth.clamp(Math.round(rawStaggerTicks(impact, mass)), STAGGER_MIN, STAGGER_MAX);
    }

    /** Where in the light-to-heavy range a weapon's heft sits, 0 to 1. */
    public static float heaviness(float mass, float reach) {
        return Mth.clamp((mass * (1 + HEFT_REACH * Math.max(0f, reach)) - HEFT_LIGHT) / (HEFT_HEAVY - HEFT_LIGHT), 0f, 1f);
    }

    public static int window(float heaviness) {
        return Math.round(Mth.lerp(heaviness, WINDOW_LIGHT, WINDOW_HEAVY));
    }

    public static int recovery(float heaviness) {
        return Math.round(Mth.lerp(heaviness, RECOVERY_LIGHT, RECOVERY_HEAVY));
    }

    public static float coneHalfDegrees(float balance) {
        return CONE_MIN_DEG + CONE_BALANCE_DEG * Mth.clamp(balance, 0f, 1f);
    }
}
