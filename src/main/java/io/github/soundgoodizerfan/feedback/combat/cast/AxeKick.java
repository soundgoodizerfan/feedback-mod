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

import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Axe Kick: an overhead heel that comes down through a raised guard. */
final class AxeKick {

    private AxeKick() {
    }

    /** Axe kick: the heel comes down on the nearest thing ahead; a raised guard is broken and its bearer staggered, and Rending Heel wears their armour. */
    static boolean act(ServerPlayer p) {
        LivingEntity t = ahead(p, 3.2, 0.5);
        if (t == null)
            return false;
        float mass = BodyMass.of(p) + BodyMass.armour(p);
        boolean guarded = t.isBlocking();
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), 3f + mass / 15f);
        if (guarded) {
            if (t instanceof Player pl)
                pl.disableShield();
            else
                t.stopUsingItem();
            stagger(p, t, mass * 9.8f * 1.5f);
        }
        if (Skills.unlocked(p, "rending_heel") && Disorder.wearArmour(t, Disorder.IRON_HEEL) > 0f)
            Disorder.note(p, t, Disorder.Kind.WORN);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        ServerLevel lv = p.serverLevel();
        Vec3 top = t.position().add(0, t.getBbHeight() + 0.8, 0), head = t.position().add(0, t.getBbHeight(), 0);
        CastFx.line(lv, CastFx.fade(CastFx.STEEL, 0xFFFFFF, 1.3f), top, head, 0.15);
        lv.sendParticles(ParticleTypes.CRIT, head.x, head.y, head.z, 12, 0.25, 0.1, 0.25, 0.3);
        CastFx.sound(lv, head, guarded ? SoundEvents.SHIELD_BREAK : SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.8f);
        return true;
    }
}
