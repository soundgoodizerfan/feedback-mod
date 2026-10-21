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
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The bearded axe's hook (spec 5.4). A blow the target blocks with a shield disables that shield
 * for {@code disable_ticks} and drags the target {@code pull} toward the wielder. Cost: with no
 * shield to catch, the hook only snags and the blow lands at {@code no_shield_mult}.
 */
public final class Hook implements ModularTrait {

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        LivingEntity target = event.getEntity();
        if (!target.isBlocking()) {
            event.setAmount(event.getAmount() * params.get("no_shield_mult", 0.9f));
            return;
        }
        // Vanilla's own test for the shield facing the blow.
        Vec3 from = event.getSource().getSourcePosition();
        if (from != null && from.vectorTo(target.position()).normalize().multiply(1, 0, 1).dot(target.getViewVector(1f)) >= 0)
            return;
        Vec3 toward = target.position().vectorTo(attacker.position()).multiply(1, 0, 1).normalize().scale(params.get("pull", 0.4f));
        target.setDeltaMovement(target.getDeltaMovement().add(toward));
        target.hurtMarked = true;
        if (target instanceof Player victim) {
            // Vanilla's own shield disable lands after this event; set ours a tick later so the longer one stands.
            int ticks = (int) params.get("disable_ticks", 160);
            var server = victim.getServer();
            server.tell(new TickTask(server.getTickCount() + 1, () -> {
                victim.getCooldowns().addCooldown(victim.getUseItem().isEmpty() ? victim.getOffhandItem().getItem() : victim.getUseItem().getItem(), ticks);
                victim.stopUsingItem();
                victim.level().broadcastEntityEvent(victim, (byte) 30);
            }));
        }
    }
}
