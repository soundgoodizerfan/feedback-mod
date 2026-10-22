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
import io.github.soundgoodizerfan.feedback.modular.RangedDispatch;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * The tower shield (spec 5.4). It guards <b>every angle</b> once raised (anything but a blow that
 * bypasses shields), and its face holds {@code capacity_mult} of what a plain one could. <b>Bash</b>:
 * use it while sprinting and it is not raised; it slams the hostile things within {@code range}
 * blocks ahead for {@code bash_damage}, knocking them back {@code bash_knock}, at a cost of
 * {@code bash_wear} wear, {@code bash_exhaust} exhaustion, the sprint and {@code bash_cooldown}
 * ticks before it can be raised again. <b>Cost</b>: raised, it slows the bearer by {@code slow}
 * and every blow it takes wears it {@code wear_mult} times as much.
 */
public final class Bulwark implements ModularTrait {

    @Override
    public void onShieldBlock(LivingShieldBlockEvent event, LivingEntity blocker, ItemStack shield, Params params) {
        if (!blocker.isBlocking() || ModularStacks.broken(shield))
            return;
        if (!event.getDamageSource().is(DamageTypeTags.BYPASSES_SHIELD))
            event.setBlocked(true);
        if (!event.getBlocked())
            return;
        event.setBlockedDamage(Math.min(event.getOriginalBlockedDamage(), RangedDispatch.capacity(shield) * params.get("capacity_mult", 1.5f)));
        event.setShieldDamage(event.getBlockedDamage() * params.get("wear_mult", 1.5f));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack shield, int remaining, Params params) {
        if (!level.isClientSide)
            Burden.apply(user, params.get("slow", 0.25f));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack shield, Params params) {
        if (!player.isSprinting() || player.getCooldowns().isOnCooldown(shield.getItem()) || ModularStacks.broken(shield))
            return InteractionResultHolder.pass(shield);
        if (!level.isClientSide) {
            Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
            float range = params.get("range", 2.5f);
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                    e -> e != player && e.isAlive() && !e.isAlliedTo(player)
                            && e.position().subtract(player.position()).multiply(1, 0, 1).normalize().dot(look) > 0.5);
            for (LivingEntity target : near) {
                target.hurt(player.damageSources().playerAttack(player), params.get("bash_damage", 2f));
                target.knockback(params.get("bash_knock", 1.4f), -look.x, -look.z);
                target.hurtMarked = true;
            }
            player.causeFoodExhaustion(params.get("bash_exhaust", 1.5f));
            player.setSprinting(false);
            player.getCooldowns().addCooldown(shield.getItem(), (int) io.github.soundgoodizerfan.feedback.core.Haste.time((long) params.get("bash_cooldown", 80f), player));
            shield.hurtAndBreak((int) params.get("bash_wear", 1f), player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResultHolder.consume(shield);
    }
}
