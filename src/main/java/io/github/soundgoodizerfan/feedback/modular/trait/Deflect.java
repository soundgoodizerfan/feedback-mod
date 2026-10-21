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
import io.github.soundgoodizerfan.feedback.modular.RangedDispatch;

import net.minecraft.server.TickTask;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * The kite shield (spec 5.4). A projectile it stops is sent back the way the wielder looks, at
 * {@code return_speed} of the speed it came with and now the wielder's, and wears the face by only
 * {@code projectile_wear} of what the blow was. Cost: the long face is narrow to a thrust, so a
 * melee blow is held to {@code melee_mult} of what the face could take. (The arrow's own bounce
 * runs after the event, so the return is set the next tick.)
 */
public final class Deflect implements ModularTrait {

    @Override
    public void onShieldBlock(LivingShieldBlockEvent event, LivingEntity blocker, ItemStack shield, Params params) {
        if (!event.getBlocked() || ModularStacks.broken(shield))
            return;
        if (event.getDamageSource().getDirectEntity() instanceof Projectile projectile) {
            event.setShieldDamage(event.getBlockedDamage() * params.get("projectile_wear", 0.5f));
            Vec3 came = projectile.getDeltaMovement();
            float back = params.get("return_speed", 0.8f);
            var server = blocker.getServer();
            server.tell(new TickTask(server.getTickCount() + 1, () -> {
                if (projectile.isRemoved())
                    return;
                projectile.setOwner(blocker);
                projectile.setDeltaMovement(blocker.getLookAngle().scale(Math.max(0.6, came.length() * back)));
                projectile.hurtMarked = true;
            }));
        } else
            event.setBlockedDamage(Math.min(event.getBlockedDamage(), RangedDispatch.capacity(shield) * params.get("melee_mult", 0.75f)));
    }
}
