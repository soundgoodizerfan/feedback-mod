// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Forge Slam: a hot held item slammed into the ground. */
final class ForgeSlam {

    private ForgeSlam() {
    }

    /** Slams a hot held item into the ground: a ring of searing around the feet, and the item gives up its heat to it. */
    static boolean act(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty())
            return false;
        Tu tu = ItemHeat.feltNoBarrel(held, p.level());
        if (tu == null)
            return false;
        ServerLevel level = p.serverLevel();
        int burned = 0;
        for (LivingEntity e : near(p, p.position(), 3.5)) {
            e.invulnerableTime = 0;
            if (HotContact.burn(e, tu))
                burned++;
        }
        float lost = 150f + burned * 40f; // the more it sears, the more the iron gives up
        if (!(held.getItem() instanceof GunItem))
            ItemHeat.drain(held, p.level(), lost);
        // heat reads in the colour: a molten ring spreading out, a second one behind it, sparks and a column rising
        Vec3 land = p.position().add(0, 0.1, 0);
        double heat = Math.min(1.0, tu.value() / 1200.0);
        CastFx.pulse(level, land, Vec3.ZERO, 0xFFE08A, 3.5f, 9);
        CastFx.pulse(level, land, Vec3.ZERO, CastFx.EMBER, 2.5f, 12);
        CastFx.shards(level, land.add(0, 0.3, 0), CastFx.EMBER, 1.5f + (float) heat, 8);
        CastFx.column(level, land, CastFx.EMBER, 2f + (float) heat, 10);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, land.x, land.y + 0.5, land.z, 10, 0.5, 0.3, 0.5, 0.05);
        CastFx.smash(level, land, 750, false);
        CastFx.sound(level, land, FSounds.CAST_SLAM.get(), 1f, 0.8f);
        CastFx.sound(level, land, FSounds.CAST_SURGE.get(), 0.6f, 0.7f);
        level.playSound(null, p.blockPosition(), FSounds.FEEL_HEAVY.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.5f);
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.clip(p, "one_handed_slam_instant", CastFx.MAIN, 14, 4);
        return true;
    }
}
