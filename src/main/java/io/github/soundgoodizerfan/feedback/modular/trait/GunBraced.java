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

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Braced (spec 8.4): supported fire. A shoulder stock or a heavy frame takes the kick into the body when the
 * shooter is crouched and still: kick falls by {@code kick_mult} and spread by {@code spread_mult}. Named
 * {@code gun_braced} because armour's cuisses already hold {@code braced}.
 */
public class GunBraced implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        if (!shooter.isCrouching() || shooter.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3)
            return f;
        return f.withKick(p.get("kick_mult", 0.7f)).withSpread(p.get("spread_mult", 0.6f));
    }
}
