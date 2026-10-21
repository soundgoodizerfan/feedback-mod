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

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.modular.Smash;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Sledge (spec 5.4): the mace's smash, struck on the ground. A fully charged swing at the top of a
 * block sends the mace's ring of shove, sound and dust out {@code radius} blocks from the point
 * struck, {@code shove} times as hard as the mace's. Spending the charge is the cost -- the swing
 * cannot be hurried -- and {@code wear} points of durability each time.
 */
public final class Slam implements ModularTrait {

    @Override
    public void onGroundHit(PlayerInteractEvent.LeftClickBlock event, Player player, ItemStack weapon, Params params) {
        if (event.getFace() != Direction.UP || !player.onGround() || player.getAttackStrengthScale(0.5f) < 0.95f)
            return;
        Vec3 at = Vec3.atBottomCenterOf(event.getPos().above());
        Smash.shockwave(player.level(), player, at, event.getPos(), params.get("radius", 3.5f), params.get("shove", 1f), false);
        player.resetAttackStrengthTicker();
        weapon.hurtAndBreak(ModularStacks.cap(weapon, Math.round(params.get("wear", 3f))), player, EquipmentSlot.MAINHAND);
    }
}
