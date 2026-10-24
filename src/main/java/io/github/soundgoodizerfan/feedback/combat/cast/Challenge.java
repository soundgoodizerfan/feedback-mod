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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Challenge: a blow of the weapon on whatever you hold that every hostile nearby turns to answer. */
final class Challenge {

    private Challenge() {
    }

    /** Challenge: ring your weapon on your guard; every hostile within ten blocks turns on you. The weapon pays a point of wear for the noise. */
    static boolean act(ServerPlayer p) {
        Vec3 c = p.position().add(0, 1, 0);
        int drawn = 0;
        for (LivingEntity e : near(p, c, 10))
            if (e instanceof Mob m && e instanceof Enemy) {
                m.setTarget(p);
                drawn++;
            }
        if (drawn == 0)
            return false;
        ItemStack weapon = p.getMainHandItem();
        if (weapon.isDamageableItem())
            weapon.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.shockwave(p, p.position().add(0, 0.1, 0), 10, 6, CastFx.dust(CastFx.CRIMSON, 1.4f), 0);
        p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.5, c.z, 20, 0.4, 0.3, 0.4, 0.4);
        CastFx.sound(p.serverLevel(), c, SoundEvents.ANVIL_LAND, 1.2f, 1.2f);
        CastFx.sound(p.serverLevel(), c, SoundEvents.RAVAGER_ROAR, 0.7f, 0.9f);
        CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 1.1f);
        return true;
    }
}
