// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularCrossbow;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The repeater (spec 5.4): loading takes {@code bolts} bolts out of the quiver at once, and one
 * shot looses them all, scattered ({@code spread_mult} on the shot's inaccuracy, read by
 * {@code RangedDispatch}). Cost: the load is {@code draw_mult} slower, each bolt does only
 * {@code damage_mult}, the scatter is wide, and every bolt is a real one out of the quiver and
 * a point of the crossbow's wear.
 */
public final class Repeater implements ModularTrait {

    @Override
    public void onLoaded(ServerPlayer player, ItemStack weapon, Params params) {
        ModularCrossbow.chamber(weapon, player, Math.max(0, Math.round(params.get("bolts", 3f)) - 1));
    }

    @Override
    public void onArrow(net.minecraft.world.entity.projectile.AbstractArrow arrow, ItemStack weapon, Params params) {
        arrow.setBaseDamage(arrow.getBaseDamage() * params.get("damage_mult", 1f));
    }
}
