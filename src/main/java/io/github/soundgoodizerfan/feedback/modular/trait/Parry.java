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

import io.github.soundgoodizerfan.feedback.client.HudReadouts;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.modular.RangedDispatch;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * The buckler (spec 5.4). <b>Parry</b>: a blow that lands in the {@code parry_ticks} after the
 * shield first comes up to guard (vanilla's raise delay is five ticks, so the window opens there)
 * is stopped whole, costs the shield nothing, and throws a living attacker back {@code knock}.
 * <b>Cost</b>: the face is small, so it guards only a {@code cone}-degree arc ahead (vanilla's is
 * 180), and outside the parry window it holds {@code capacity_mult} of what its face could.
 */
public final class Parry implements ModularTrait {

    @Override
    public void onShieldBlock(LivingShieldBlockEvent event, LivingEntity blocker, ItemStack shield, Params params) {
        if (!event.getBlocked() || ModularStacks.broken(shield))
            return;
        Vec3 from = event.getDamageSource().getSourcePosition();
        if (from != null && !inCone(blocker, from, params.get("cone", 110f))) {
            event.setBlocked(false);
            return;
        }
        if (blocker.getTicksUsingItem() < 5 + params.get("parry_ticks", 6f)) {
            event.setBlockedDamage(event.getOriginalBlockedDamage());
            event.setShieldDamage(0);
            if (event.getDamageSource().getEntity() instanceof LivingEntity attacker)
                attacker.knockback(params.get("knock", 0.8f), blocker.getX() - attacker.getX(), blocker.getZ() - attacker.getZ());
            if (blocker instanceof ServerPlayer player)
                HudReadouts.send(player, Component.translatable("trait.feedback.parry"), 20);
        } else
            event.setBlockedDamage(Math.min(event.getBlockedDamage(), RangedDispatch.capacity(shield) * params.get("capacity_mult", 0.65f)));
    }

    /** Whether {@code from} lies within {@code degrees} (the full arc) of where {@code blocker} is looking, flat. */
    static boolean inCone(LivingEntity blocker, Vec3 from, float degrees) {
        Vec3 to = from.subtract(blocker.getEyePosition()).multiply(1, 0, 1);
        Vec3 look = blocker.getViewVector(1f).multiply(1, 0, 1);
        if (to.lengthSqr() < 1.0E-6 || look.lengthSqr() < 1.0E-6)
            return true;
        return to.normalize().dot(look.normalize()) >= Math.cos(Math.toRadians(degrees / 2));
    }
}
