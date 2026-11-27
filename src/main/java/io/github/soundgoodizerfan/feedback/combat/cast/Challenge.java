// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerPlayer;
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
        CastFx.pulse(p.serverLevel(), p.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.CRIMSON, 10f, 12);
        CastFx.pulse(p.serverLevel(), p.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.CRIMSON, 6f, 9);
        CastFx.shards(p.serverLevel(), c.add(0, 0.5, 0), CastFx.CRIMSON, 2.5f, 8);
        CastFx.sound(p.serverLevel(), c, FSounds.CAST_SURGE.get(), 1.2f, 0.8f);
        CastFx.sound(p.serverLevel(), c, FSounds.CAST_SLAM.get(), 0.7f, 1.2f);
        CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 1.1f);
        return true;
    }
}
