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
package io.github.soundgoodizerfan.feedback.combat.hit;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * The server half of the swept melee hit (plan 2.4). It <b>rides</b> vanilla's attack rather than
 * replacing it, because the pick, the crit, the cooldown reset and the enchantments all live in
 * {@code Player.attack} and the client picks the target before the server hears of it.
 *
 * <p>Two ways in. A vanilla attack on a picked target ({@link #onAttack}): the volume's other targets
 * are hurt too, before vanilla's own hit, while the cooldown is still charged. A swing the client's pick
 * found nothing for ({@code MeleeSweepPayload}, see {@link #miss}): the volume may still cover
 * something, and the server resets the cooldown itself as the client did.
 *
 * <p>An extra target takes vanilla's formula without the crit: the attack damage attribute, the
 * weapon's enchantments, scaled by the charge as {@code Player.attack} scales it. It goes through
 * {@code hurt} with the player's attack source, so {@code LivingIncomingDamageEvent} fires for it and
 * every trait and material perk resolves per target as for any hit. (A trait that sweeps by itself, the
 * claymore's overhead, will sweep again around each extra target; see the plan.)
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MeleeSweep {

    private MeleeSweep() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            SweptVolume.halfWidth(player).ifPresent(w -> hit(player, SweptVolume.targets(player, w), event.getTarget()));
    }

    /** A swing the client's pick missed everything on. */
    public static void miss(ServerPlayer player) {
        SweptVolume.halfWidth(player).ifPresent(w -> {
            List<LivingEntity> targets = SweptVolume.targets(player, w);
            if (!targets.isEmpty()) {
                hit(player, targets, null);
                player.resetAttackStrengthTicker();
            }
        });
    }

    private static void hit(ServerPlayer player, List<LivingEntity> targets, Entity picked) {
        ItemStack weapon = player.getMainHandItem();
        float charge = player.getAttackStrengthScale(0.5f);
        DamageSource source = player.damageSources().playerAttack(player);
        ServerLevel level = player.serverLevel();
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        for (LivingEntity target : targets) {
            if (target == picked)
                continue;
            float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float bonus = EnchantmentHelper.modifyDamage(level, weapon, target, source, base) - base;
            float amount = base * (0.2f + charge * charge * 0.8f) + bonus * charge;
            if (target.hurt(source, amount)) {
                target.knockback(ModularTuning.SWEEP_KNOCKBACK * charge, Mth.sin(yaw), -Mth.cos(yaw));
                weapon.hurtAndBreak(ModularStacks.cap(weapon, 1), player, EquipmentSlot.MAINHAND);
            }
        }
    }
}
