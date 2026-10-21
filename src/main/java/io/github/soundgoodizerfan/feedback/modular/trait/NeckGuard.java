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

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Sallet (spec 5.4, armour): the tail guards the neck, so a blow from behind (more than
 * {@code back_angle} degrees off the wearer's facing, as the dagger reckons it) does only
 * {@code behind_mult}. The cost is the extra cells of the tail: a heavier shell. Worn: counts once.
 */
public final class NeckGuard implements ModularTrait {

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        Entity source = event.getSource().getEntity();
        if (source == null)
            return;
        Vec3 facing = Vec3.directionFromRotation(0, wearer.yBodyRot);
        Vec3 toSource = new Vec3(source.getX() - wearer.getX(), 0, source.getZ() - wearer.getZ()).normalize();
        if (facing.dot(toSource) < Math.cos(Math.toRadians(params.get("back_angle", 120f))))
            event.setAmount(event.getAmount() * params.get("behind_mult", 0.8f));
    }
}
