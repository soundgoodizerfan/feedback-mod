// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.combat.RangedDispatch;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * How a ranged weapon handles, as numbers alone (spec 5.4): the shortbow's quick draw
 * ({@code snap_shot}), the longbow's heavy one ({@code heavy_draw}), the arbalest ({@code
 * heavy_bolt}) and the pistol crossbow ({@code snap_bolt}) are this class under four ids and four
 * sets of params. {@code draw_mult} (draw or reload time), {@code velocity_mult} and
 * {@code spread_mult} are read for every ranged trait by {@code RangedDispatch}; here are the
 * rest: {@code damage_mult} of each projectile, {@code pierce} (mobs a projectile passes through)
 * and {@code slow}, the fraction of walking speed lost while the weapon is drawn or being loaded.
 * Each archetype pays for what it gains in the other numbers.
 */
public final class Handling implements ModularTrait {

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack weapon, int remaining, Params params) {
        if (!level.isClientSide)
            Burden.apply(user, params.get("slow", 0f));
    }

    @Override
    public void onArrow(AbstractArrow arrow, ItemStack weapon, Params params) {
        arrow.setBaseDamage(arrow.getBaseDamage() * params.get("damage_mult", 1f));
        RangedDispatch.pierce(arrow, params.get("pierce", 0f));
    }
}
