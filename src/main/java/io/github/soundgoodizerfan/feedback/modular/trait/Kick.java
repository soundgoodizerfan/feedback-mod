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

import java.util.UUID;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Greaves (spec 5.4, armour): a sprinting wearer who runs into a hostile mob in front of them
 * kicks it back, {@code strength} of a knockback, no damage. Costs {@code exhaustion} hunger,
 * {@code wear} on the piece, and a {@code cooldown_ticks} wait between kicks. Per-wearer cooldown
 * is dropped on death or logout. Worn: counts once.
 */
public final class Kick implements ModularTrait {

    private static final WielderState<Long> READY = new WielderState<>();

    @Override
    public void onWornTick(Player wearer, ItemStack piece, Params params) {
        if (!wearer.isSprinting())
            return;
        UUID id = wearer.getUUID();
        long now = wearer.level().getGameTime();
        Long ready = READY.get(id);
        if (ready != null && now < ready)
            return;
        Vec3 look = Vec3.directionFromRotation(0, wearer.getYRot());
        var box = wearer.getBoundingBox().inflate(0.2, 0, 0.2).move(look.scale(0.6));
        for (LivingEntity target : wearer.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != wearer && e instanceof Enemy)) {
            target.knockback(params.get("strength", 1.0f), wearer.getX() - target.getX(), wearer.getZ() - target.getZ());
            wearer.causeFoodExhaustion(params.get("exhaustion", 0.3f));
            piece.hurtAndBreak(ModularStacks.cap(piece, (int) params.get("wear", 1f)), wearer, ((ArmorItem) piece.getItem()).getEquipmentSlot());
            READY.put(id, now + (long) params.get("cooldown_ticks", 30f));
            return;
        }
    }
}
