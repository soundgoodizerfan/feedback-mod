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

import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Sunder: an overhead blow into the shield or weapon a body holds up. */
final class Sunder {

    private Sunder() {
    }

    /** A weapon this many times softer than your own is struck out of the hand; harder or near, it only wears. */
    private static final float KNOCK_RATIO = 1.5f;

    /** Sunder: a heavy blow at what you face. A raised shield is split (worn by your hardness against its, disabled); else the held weapon is worn, and if yours is {@value #KNOCK_RATIO} times harder it is struck out of the hand and lies on the ground. Your weapon is worn by theirs. */
    static boolean act(ServerPlayer p) {
        ItemStack mine = p.getMainHandItem();
        LivingEntity t = ahead(p, 3.2, 0.5);
        if (mine.isEmpty() || mine.getItem() instanceof GunItem || t == null)
            return false;
        boolean guard = t.isBlocking();
        ItemStack theirs = guard ? t.getUseItem() : t.getMainHandItem();
        if (theirs.isEmpty())
            return false;
        float h = Disorder.hardness(theirs), ratio = Disorder.hardness(mine) / h;
        EquipmentSlot slot = guard && t.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), 3f);
        if (theirs.isDamageableItem())
            theirs.hurtAndBreak(Disorder.wear(Disorder.hardness(mine), h, 2f), t, slot);
        if (guard) {
            if (t instanceof Player pl)
                pl.disableShield();
            else
                t.stopUsingItem();
        } else if (ratio >= KNOCK_RATIO) {
            ItemStack dropped = t.getMainHandItem().copy();
            t.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            t.spawnAtLocation(dropped);
        }
        Disorder.selfWear(p, h);
        Disorder.note(p, t, Disorder.Kind.STRIPPED);
        stagger(p, t, io.github.soundgoodizerfan.feedback.combat.tether.BodyMass.of(p) * 9.8f);
        p.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel lv = p.serverLevel();
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.7, 0);
        lv.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 18, 0.3, 0.3, 0.3, 0.4);
        CastFx.line(lv, CastFx.fade(0xFFFFFF, CastFx.STEEL, 1.3f), at.add(0, 0.9, 0), at.add(0, -0.3, 0), 0.12);
        CastFx.sound(lv, at, guard ? SoundEvents.SHIELD_BREAK : SoundEvents.ITEM_BREAK, 1f, 0.7f);
        CastFx.sound(lv, at, SoundEvents.ANVIL_LAND, 0.7f, 0.9f);
        return true;
    }
}
