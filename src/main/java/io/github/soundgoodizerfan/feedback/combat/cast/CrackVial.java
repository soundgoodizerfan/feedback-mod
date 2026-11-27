// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;
import io.github.soundgoodizerfan.feedback.potion.PotionTuning;

import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.fluids.FluidStack;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Crack Vial: a bottle broken on the one body in front of you. */
final class CrackVial {

    private CrackVial() {
    }

    /** Crack vial: a bottle from your pack is smashed on the body at arm's length, so its whole dose lands on that one body and none of it spreads. The bottle is the cost: no live potion, no cast. */
    static boolean act(ServerPlayer p) {
        LivingEntity t = ahead(p, 3.2, 0.5);
        if (t == null)
            return false;
        FluidStack drawn = Vials.draw(p, PotionTuning.SPRAY_DOSE_MB);
        if (drawn.isEmpty())
            return false;
        Vials.dose(p, t, drawn);
        stagger(p, t, new Work(30f));
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.7, 0);
        CastFx.shards(p.serverLevel(), at, 0xFFFFFF, 0.9f, 6);
        CastFx.sound(p.serverLevel(), at, FSounds.CAST_SHATTER.get(), 1f, 1f);
        p.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }
}
