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

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Katana (spec 5.4): right click dashes the wielder up to {@code distance} blocks along the look
 * line, stopping short of a solid block, and cuts every living thing the path passes within
 * {@code width} blocks for {@code dash_mult} of the wielder's attack damage (once each). The cost
 * is {@code cooldown_ticks} between dashes, {@code wear} per dash, and the wielder ends the dash
 * where the cut ended, not where they chose.
 */
public final class Dash implements ModularTrait {

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack weapon, Params params) {
        if (!(player instanceof ServerPlayer sp) || player.getCooldowns().isOnCooldown(weapon.getItem()))
            return InteractionResultHolder.pass(weapon);
        Vec3 from = player.position();
        Vec3 eye = player.getEyePosition();
        Vec3 aim = player.getLookAngle();
        double reach = params.get("distance", 6f);
        // Clip the eye line, then drop back by the body's half width so the wielder is not left inside the wall.
        HitResult wall = level.clip(new ClipContext(eye, eye.add(aim.scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double run = Math.max(0, eye.distanceTo(wall.getLocation()) - (wall.getType() == HitResult.Type.MISS ? 0 : player.getBbWidth()));
        Vec3 to = from.add(aim.scale(run));

        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * params.get("dash_mult", 1.2f);
        double width = params.get("width", 1f);
        var swept = player.getBoundingBox().expandTowards(to.subtract(from)).inflate(width);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, swept, e -> e != player && e.isAlive()))
            if (distToSegment(victim.position().add(0, victim.getBbHeight() / 2, 0), eye, eye.add(aim.scale(run))) <= width + victim.getBbWidth() / 2)
                victim.hurt(player.damageSources().playerAttack(player), damage);

        sp.teleportTo(to.x, to.y, to.z);
        player.fallDistance = 0;
        level.playSound(null, to.x, to.y, to.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.4f);
        player.getCooldowns().addCooldown(weapon.getItem(), (int) params.get("cooldown_ticks", 80));
        weapon.hurtAndBreak(ModularStacks.cap(weapon, (int) params.get("wear", 2)), sp, EquipmentSlot.MAINHAND);
        return InteractionResultHolder.sidedSuccess(weapon, level.isClientSide);
    }

    private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }
}
