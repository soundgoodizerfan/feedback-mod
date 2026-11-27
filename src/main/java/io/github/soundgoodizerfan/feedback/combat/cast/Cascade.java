// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.potion.PotionTuning;

import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Cascade: a bottle thrown so it breaks on one body and runs on to the next. */
final class Cascade {

    private Cascade() {
    }

    private static final double RANGE = 12, JUMP = 4;

    /** Cascade: a bottle thrown at the body you face breaks on it, and what runs off lands as a second dose on the nearest other body within a few blocks of it. */
    static boolean act(ServerPlayer p) {
        LivingEntity first = ahead(p, RANGE, 0.9);
        if (first == null)
            return false;
        FluidStack one = Vials.draw(p, PotionTuning.SPRAY_DOSE_MB);
        if (one.isEmpty())
            return false;
        Vials.dose(p, first, one);
        LivingEntity second = null;
        double best = JUMP * JUMP;
        for (LivingEntity e : near(p, first.position(), JUMP)) {
            double d = e.distanceToSqr(first);
            if (e != first && d < best) {
                best = d;
                second = e;
            }
        }
        FluidStack two = second == null ? FluidStack.EMPTY : Vials.draw(p, PotionTuning.SPRAY_DOSE_MB);
        if (second != null && !two.isEmpty()) {
            Vec3 a = first.position().add(0, first.getBbHeight() * 0.6, 0), b = second.position().add(0, second.getBbHeight() * 0.6, 0);
            int col = io.github.soundgoodizerfan.feedback.potion.PotionSpray.colour(one);
            CastFx.streak(p.serverLevel(), a, b, col, 0.04f, 6);
            CastFx.shards(p.serverLevel(), b, col, 0.8f, 6);
            CastFx.sound(p.serverLevel(), b, FSounds.CAST_SHATTER.get(), 0.8f, 1.2f);
            Vials.dose(p, second, two);
        }
        p.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }
}
