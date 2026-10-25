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

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;

/** Dead Eye, always on once owned (a rider on firing, not a cast): a shot taken crouched and standing still leaves exact, with a harder round. */
public final class DeadEye {

    private DeadEye() {
    }

    /** The game tick until which a Dead Eye shot is exact. */
    private static final java.util.Map<java.util.UUID, Long> DEAD_EYE = new java.util.HashMap<>();

    /** Makes {@code p}'s next shot exact for {@code ticks} (the same state Dead Eye sets; Mark and Fire arms it per round). */
    static void arm(ServerPlayer p, int ticks) {
        DEAD_EYE.put(p.getUUID(), p.level().getGameTime() + ticks);
    }

    /** Whether {@code p}'s next shot is a Dead Eye one (and if so, spends it). */
    public static boolean consume(ServerPlayer p) {
        Long until = DEAD_EYE.remove(p.getUUID());
        if (until != null && p.level().getGameTime() <= until)
            return true;
        // Innate: a shot taken crouched and still is exact.
        return Skills.unlocked(p, "dead_eye") && p.isShiftKeyDown() && p.getDeltaMovement().horizontalDistanceSqr() < 1e-3;
    }
}
