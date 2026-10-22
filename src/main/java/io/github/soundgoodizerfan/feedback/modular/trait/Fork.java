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

import java.util.Comparator;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Pitchfork (spec 5.4): three tines. A blow also hits up to {@code extra} other living things in a
 * cone ahead of the wielder, within {@code reach} blocks, each for {@code side_mult} of the blow;
 * the blow on the target is only {@code main_mult}. Good against a crowd, poor against one.
 * Costs {@code wear} points when the tines catch more than the target.
 */
public final class Fork implements ModularTrait {

    private static boolean forking;

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        if (forking)
            return;
        LivingEntity target = event.getEntity();
        Vec3 look = attacker.getLookAngle();
        Vec3 eye = attacker.getEyePosition();
        float reach = params.get("reach", 3.5f);
        List<LivingEntity> near = attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(reach),
                e -> e != attacker && e != target && !(e instanceof ArmorStand) && !attacker.isAlliedTo(e)
                        && e.getBoundingBox().getCenter().distanceTo(eye) <= reach
                        && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > params.get("cone", 0.8f));
        event.setAmount(event.getAmount() * params.get("main_mult", 0.7f));
        if (near.isEmpty())
            return;
        DamageSource source = io.github.soundgoodizerfan.feedback.modular.StrikeSource.of(attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker), attacker, weapon);
        float amount = event.getAmount() * params.get("side_mult", 0.7f);
        forking = true;
        try {
            near.stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(attacker))).limit(Math.round(params.get("extra", 2f)))
                    .forEach(e -> e.hurt(source, amount));
        } finally {
            forking = false;
        }
        weapon.hurtAndBreak(ModularStacks.cap(weapon, Math.round(params.get("wear", 1f))), attacker, io.github.soundgoodizerfan.feedback.modular.StrikeSource.slot(attacker, weapon));
    }
}
