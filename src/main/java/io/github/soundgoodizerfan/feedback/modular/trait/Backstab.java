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

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Dagger (spec 5.4): {@code back_mult} when the attacker is behind the target (more than
 * {@code back_angle} degrees off its facing) or the target is a mob with nothing targeted, and
 * {@code sneak_mult} more when sneaking. The cost is the open fight: face to face it deals only
 * {@code face_mult}.
 */
public final class Backstab implements ModularTrait {

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        LivingEntity target = event.getEntity();
        Vec3 facing = Vec3.directionFromRotation(0, target.yBodyRot);
        Vec3 toAttacker = new Vec3(attacker.getX() - target.getX(), 0, attacker.getZ() - target.getZ()).normalize();
        boolean behind = facing.dot(toAttacker) < Math.cos(Math.toRadians(params.get("back_angle", 120f)));
        boolean unaware = target instanceof Mob mob && mob.getTarget() == null;
        float mult = params.get("face_mult", 0.9f);
        if (behind || unaware) {
            mult = params.get("back_mult", 2f);
            if (attacker.isShiftKeyDown())
                mult *= params.get("sneak_mult", 1.3f);
        }
        event.setAmount(event.getAmount() * mult);
    }
}
