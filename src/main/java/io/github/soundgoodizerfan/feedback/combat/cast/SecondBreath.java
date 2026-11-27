// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Second breath (Proficiency): a deep breath that refills stamina to sixty and pauses its regen, as a rest does.
 * Costs no stamina itself, so it is the one cast that can be slotted to answer an empty bar. Worn armour presses on the
 * chest: every {@link #MU_PER_POINT} Mu of it ({@link BodyMass#armour}) takes a point off the refill, down to
 * {@link #FLOOR}. It rearms on a landed blow, not the clock: a free refill on a timer was a cost paid by waiting.
 */
final class SecondBreath {

    /** STAND-IN: full diamond (about 50 Mu) breathes to 40, leather to about 53. */
    static final float TO = 60f, FLOOR = 30f, MU_PER_POINT = 2.5f;

    private SecondBreath() {
    }

    static boolean act(ServerPlayer p) {
        float to = Math.max(FLOOR, TO - BodyMass.armour(p) / MU_PER_POINT);
        if (Stamina.get(p) >= to)
            return false;
        Stamina.restore(p, to);
        Vec3 at = p.position().add(0, 1.6, 0);
        CastFx.gather(p.serverLevel(), p.position().add(0, 1, 0), CastFx.GOLD, 1.2f, 6);
        CastFx.sound(p.serverLevel(), at, FSounds.CAST_MEND.get(), 0.8f, 1.1f);
        return true;
    }
}
