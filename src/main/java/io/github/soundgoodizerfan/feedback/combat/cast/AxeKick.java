// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
            stagger(p, t, new Work(mass * FTuning.GRAVITY_BLOCKS_PER_S2 * 1.5f));
        }
        if (Skills.unlocked(p, "rending_heel") && Disorder.wearArmour(t, Disorder.IRON_HEEL) > 0f)
            Disorder.note(p, t, Disorder.Kind.WORN);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        ServerLevel lv = p.serverLevel();
        Vec3 top = t.position().add(0, t.getBbHeight() + 0.8, 0), head = t.position().add(0, t.getBbHeight(), 0);
        CastFx.streak(lv, top, head, CastFx.STEEL, 0.04f, 5);
        CastFx.shards(lv, head, 0xFFFFFF, 1f, 6);
        CastFx.sound(lv, head, guarded ? FSounds.CAST_GUARD.get() : FSounds.CAST_PUNCH.get(), 1f, 0.8f);
        return true;
    }
}
