// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.modular.attunement.RecallKinds;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait.Params;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * Turns a held, released modular weapon into a {@link ThrownWeapon}: the one place a throw's
 * figures are worked out, so the hatchet's trait and a trident's release say the same thing.
 *
 * <p>Nothing here is a per-weapon table. How fast it leaves is one arm's throw over the baked mass
 * (a heavy weapon is slower), how hard it hits is the baked damage scaled by how long the wind-up was
 * held, and how quickly it comes back is lighter-is-quicker. What a variant chooses, in its own
 * {@code params}, is only what it is: {@code tumble} (end over end, rather than point first),
 * {@code return} (a loyalty-style return rate), and {@code rope}, {@code pull} and
 * {@code water_mult} (a line and a reel).
 */
public final class Throwing {

    private Throwing() {
    }

    /**
     * Throws {@code weapon} from {@code player} if the wind-up was long enough.
     *
     * @param held ticks use was held
     * @return whether it was thrown
     */
    public static boolean release(ItemStack weapon, Level level, Player player, int held, Params params,
                                  float defaultPerBlock, float defaultMaxMult, int defaultWear) {
        if (level.isClientSide || held < params.get("min_charge_ticks", 10))
            return false;
        ModularFigures figures = ModularStacks.figures(weapon);
        if (figures == null || ModularStacks.broken(weapon))
            return false;
        float effort = effort(held, params);
        float mass = Math.max(0.1f, figures.get(Fig.MASS));
        float speed = Mth.clamp(ModularTuning.THROW_SPEED_K / (float) Math.sqrt(mass), ModularTuning.THROW_SPEED_MIN, ModularTuning.THROW_SPEED_MAX)
                * effort;

        // A spring-launched head: the spring adds to the throw, a set takes some of that back, and a heavy head at that speed overloads it.
        float springMult = params.get("speed_mult", 1f);
        if (springMult != 1f) {
            float set = SpringSet.set(weapon);
            speed *= 1f + (springMult - 1f) * (1f - set);
            float rating = params.get("rating", 0f);
            if (rating > 0f)
                SpringSet.overload(weapon, mass * springMult * springMult / rating, player);
        }

        float returns = params.get("return", 0f);
        if (level instanceof ServerLevel server)
            returns = Math.max(returns, EnchantmentHelper.getTridentReturnToOwnerAcceleration(server, weapon, player));
        returns = Math.max(returns, RecallKinds.returns(weapon, level));
        returns *= Mth.clamp(1f / (float) Math.sqrt(mass), ModularTuning.RETURN_MASS_MIN, ModularTuning.RETURN_MASS_MAX);

        ThrownWeapon.Flight flight = new ThrownWeapon.Flight(figures.get(Fig.DAMAGE) * effort,
                params.get("damage_per_block", defaultPerBlock) * effort, params.get("max_mult", defaultMaxMult), returns,
                params.get("rope", 0f), params.get("pull", 0f), params.get("water_mult", 1f), params.get("tumble", 0f) > 0f);

        weapon.hurtAndBreak(ModularStacks.cap(weapon, (int) params.get("wear", defaultWear)), player,
                LivingEntity.getSlotForHand(player.getUsedItemHand()));
        ItemStack thrown = weapon.copy();
        thrown.setCount(1);
        ThrownWeapon entity = new ThrownWeapon(level, player, thrown, flight);
        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, speed, 1.0f);
        if (player.hasInfiniteMaterials())
            entity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(entity);
        level.playSound(null, entity, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(weapon.getItem()));
        if (!player.hasInfiniteMaterials())
            weapon.shrink(1);
        return true;
    }

    /** How hard the throw was: the floor at the first tick it is allowed, 1 at a full wind-up. */
    static float effort(int held, Params params) {
        float full = Math.max(1f, params.get("full_charge_ticks", ModularTuning.THROW_FULL_CHARGE_TICKS));
        return Mth.lerp(Mth.clamp(held / full, 0f, 1f), ModularTuning.THROW_EFFORT_FLOOR, 1f);
    }
}
