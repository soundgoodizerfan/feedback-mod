// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        // The opened body takes a bottle broken across it.
        Vials.dose(p, t, Vials.draw(p, io.github.soundgoodizerfan.feedback.potion.PotionTuning.SPRAY_DOSE_MB));
        stagger(p, t, new Work(io.github.soundgoodizerfan.feedback.combat.tether.BodyMass.of(p) * FTuning.GRAVITY_BLOCKS_PER_S2));
        p.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel lv = p.serverLevel();
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.7, 0);
        CastFx.shards(lv, at, CastFx.STEEL, 1.4f, 7);
        CastFx.streak(lv, at.add(0, 0.9, 0), at.add(0, -0.3, 0), 0xFFFFFF, 0.05f, 4);
        CastFx.sound(lv, at, guard ? FSounds.CAST_SHATTER.get() : FSounds.CAST_GUARD.get(), 1f, 0.8f);
        CastFx.sound(lv, at, FSounds.CAST_SLAM.get(), 0.7f, 1.2f);
        return true;
    }
}
