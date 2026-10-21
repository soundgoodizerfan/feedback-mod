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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Bardiche (spec 5.4): the blow carries on through the target and hits the nearest other living
 * thing standing in line behind it, within {@code length} blocks, for {@code line_mult} of the
 * blow. The cost is that the main blow itself is only {@code main_mult}, and {@code wear} points of
 * durability when the second target is struck.
 */
public final class LineCut implements ModularTrait {

    private static boolean cutting;

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        if (cutting)
            return;
        LivingEntity target = event.getEntity();
        Vec3 look = attacker.getLookAngle();
        Vec3 eye = attacker.getEyePosition();
        double along = target.getBoundingBox().getCenter().subtract(eye).dot(look);
        float length = params.get("length", 3f);
        List<LivingEntity> behind = attacker.level().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(length),
                e -> e != attacker && e != target && !(e instanceof ArmorStand) && !attacker.isAlliedTo(e) && inLine(e, eye, look, along, length));
        event.setAmount(event.getAmount() * params.get("main_mult", 0.9f));
        if (behind.isEmpty())
            return;
        LivingEntity next = behind.stream().min((a, b) -> Double.compare(a.distanceToSqr(target), b.distanceToSqr(target))).get();
        DamageSource source = attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker);
        cutting = true;
        try {
            next.hurt(source, event.getAmount() * params.get("line_mult", 0.7f));
        } finally {
            cutting = false;
        }
        weapon.hurtAndBreak(ModularStacks.cap(weapon, Math.round(params.get("wear", 1f))), attacker, EquipmentSlot.MAINHAND);
    }

    /** True if {@code e} stands beyond the target along the look line, no more than {@code length} past it and about a block off the line. */
    private static boolean inLine(LivingEntity e, Vec3 eye, Vec3 look, double targetAlong, float length) {
        Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
        double along = to.dot(look);
        return along > targetAlong && along <= targetAlong + length && to.subtract(look.scale(along)).length() < 1.0;
    }
}
