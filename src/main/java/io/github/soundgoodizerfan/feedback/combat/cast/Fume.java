// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.potion.PotionSpray;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fume: a bottle dashed to the floor so it hangs as a cloud. */
final class Fume {

    private Fume() {
    }

    /** Fume: two doses of potion burst at your feet and hang as a mist that doses whatever stands in it, you too. Denies the ground; the price is that you are on it. */
    static boolean act(ServerPlayer p) {
        FluidStack drawn = Vials.draw(p, io.github.soundgoodizerfan.feedback.potion.PotionTuning.SPRAY_DOSE_MB * 4);
        if (drawn.isEmpty())
            return false;
        Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5)).add(0, 0.6, 0);
        PotionSpray.mist(p.serverLevel(), at, drawn, drawn.getAmount() * 2);
        Vials.splash(p.serverLevel(), at, drawn);
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.pulse(p.serverLevel(), at.add(0, -0.5, 0), Vec3.ZERO, CastFx.ICE, 1.8f, 9);
        CastFx.shards(p.serverLevel(), at, CastFx.ICE, 1f, 6);
        CastFx.sound(p.serverLevel(), at, io.github.soundgoodizerfan.feedback.registry.FSounds.CAST_SHATTER.get(), 0.8f, 1.1f);
        return true;
    }
}
