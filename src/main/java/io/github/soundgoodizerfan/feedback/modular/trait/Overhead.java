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

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Claymore (spec 5.4): a fully charged swing also hits everything within {@code radius} of the
 * target for {@code sweep_mult} of the blow and knocks it back; a half-charged swing does only
 * {@code weak_mult} and no sweep. The cost is timing, and {@code wear} points of durability each
 * time the sweep is used.
 */
public final class Overhead implements ModularTrait {

    private static boolean sweeping;

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        if (sweeping)
            return;
        if (attacker instanceof Player player && player.getAttackStrengthScale(0.5f) < 0.95f) {
            event.setAmount(event.getAmount() * params.get("weak_mult", 0.6f));
            return;
        }
        LivingEntity target = event.getEntity();
        float radius = params.get("radius", 2.5f);
        List<LivingEntity> others = attacker.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius),
                e -> e != attacker && e != target && !(e instanceof ArmorStand) && !attacker.isAlliedTo(e)
                        && e.distanceToSqr(target) <= radius * radius);
        if (others.isEmpty())
            return;
        DamageSource source = attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker);
        float amount = event.getAmount() * params.get("sweep_mult", 0.6f);
        sweeping = true;
        try {
            for (LivingEntity e : others) {
                e.hurt(source, amount);
                e.knockback(0.4, attacker.getX() - e.getX(), attacker.getZ() - e.getZ());
            }
        } finally {
            sweeping = false;
        }
        weapon.hurtAndBreak(ModularStacks.cap(weapon, Math.round(params.get("wear", 2f))), attacker, EquipmentSlot.MAINHAND);
    }
}
