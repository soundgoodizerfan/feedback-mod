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
