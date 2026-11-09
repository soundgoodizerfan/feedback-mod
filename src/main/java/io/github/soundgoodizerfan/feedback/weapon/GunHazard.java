// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What a round that asks too much does ({@code docs/specs/feedback_guns_spec.md} section 3.2, first cut:
 * damage and a spent gun, never a removed one).
 *
 * <p>It is read entirely off the spec sheet. The ratio is the round's peak pressure over the
 * chamber's rating (and over its own case's: whichever it exceeds more), so a player who looked
 * at the tooltip was never surprised, and it is about the design, not uptime -- a sound gun is
 * never touched by it however often it is fired. Past the rating the gun takes damage in
 * proportion; past a further onset the shooter does, up to a cap that hurts and does not kill an
 * unhurt player. Spent is a state the tooltip names and the trigger honours, not an item that
 * disappears. Nothing here is a chance: the only randomness is the noise floor on the size.
 */
public final class GunHazard {

    private GunHazard() {
    }

    /** What one shot cost: the ratio it was over, damage to the gun, hp to the shooter. */
    public record Outcome(float ratio, float gunDamage, float shooterHp) {
        public boolean any() {
            return gunDamage > 0 || shooterHp > 0;
        }
    }

    /**
     * @param noise a factor near 1 (the floor); callers pass 1 to read the band exactly.
     * @param caseRating the round's own case rating, or {@link Float#MAX_VALUE} if unknown.
     */
    public static Outcome assess(float peak, float chamberRating, float caseRating, float noise) {
        float gunRatio = peak / chamberRating;
        float hazard = Math.max(gunRatio, peak / caseRating);
        float gunDamage = gunRatio > 1 ? (gunRatio - 1) * FTuning.GUN_OVERPRESSURE_DAMAGE_PER_RATIO * noise : 0;
        float hp = hazard > FTuning.GUN_HAZARD_SHOOTER_ONSET
                ? Math.min(FTuning.GUN_HAZARD_SHOOTER_MAX_HP,
                        (hazard - FTuning.GUN_HAZARD_SHOOTER_ONSET) * FTuning.GUN_HAZARD_SHOOTER_HP_PER_RATIO * noise)
                : 0;
        return new Outcome(hazard, gunDamage, hp);
    }

    /** hp a cooked-off round does to the one holding the gun: per nominal charge, capped, with the noise floor. */
    public static float cookOffHp(int charge, float noise) {
        return Math.min(FTuning.GUN_COOKOFF_HP_MAX, FTuning.GUN_COOKOFF_HP * charge / FTuning.GUN_CHARGE_NOMINAL * noise);
    }

    public static float damage(ItemStack gun) {
        return gun.getOrDefault(FWeapons.GUN_DAMAGE.get(), 0f);
    }

    public static boolean isSpent(ItemStack gun) {
        return damage(gun) >= 1f;
    }

    /** Book {@code outcome}: the gun's damage, and the blast the shooter and the room feel. */
    public static void apply(ServerLevel level, net.minecraft.world.entity.LivingEntity shooter, ItemStack gun, Outcome outcome) {
        if (!outcome.any())
            return;
        if (outcome.gunDamage() > 0)
            gun.set(FWeapons.GUN_DAMAGE.get(), damage(gun) + outcome.gunDamage());
        if (outcome.gunDamage() > 0)
            io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(shooter instanceof net.minecraft.world.entity.player.Player pl ? pl : null, "combat/overrun");
        float loudness = Mth.clamp(outcome.ratio() / 4f, 0.3f, 1.5f);
        level.playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, loudness, 1.6f);
        level.sendParticles(ParticleTypes.EXPLOSION, shooter.getX(), shooter.getEyeY(), shooter.getZ(),
                1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SMOKE, shooter.getX(), shooter.getEyeY(), shooter.getZ(),
                8, 0.2, 0.2, 0.2, 0.02);
        if (outcome.shooterHp() > 0)
            shooter.hurt(level.damageSources().explosion(null, null), outcome.shooterHp());
    }
}
