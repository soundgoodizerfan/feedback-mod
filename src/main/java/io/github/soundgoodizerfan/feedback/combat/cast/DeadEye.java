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
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;

/** Dead Eye: the next shot leaves exact. */
public final class DeadEye {

    private DeadEye() {
    }

    /** The game tick until which a Dead Eye shot is exact. */
    private static final java.util.Map<java.util.UUID, Long> DEAD_EYE = new java.util.HashMap<>();

    /** Dead eye: for the next five seconds, the next shot leaves exact and with a harder round. */
    static boolean act(ServerPlayer p) {
        if (!(p.getMainHandItem().getItem() instanceof GunItem))
            return false;
        DEAD_EYE.put(p.getUUID(), p.level().getGameTime() + 100);
        CombatCues.everyone(p, FSounds.FEEL_LIGHT.get(), FeelTuning.LIGHT_VOLUME, 0.6f);
        ServerLevel lv = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        // breath held: light gathers into the eye, then a glint and a line down the barrel
        CastFx.converge(p, eye.add(0, -0.1, 0), 1.8, 6, ParticleTypes.END_ROD);
        later(p, 6, () -> {
            Vec3 e = p.getEyePosition(), look = p.getLookAngle();
            lv.sendParticles(ParticleTypes.FLASH, e.x + look.x, e.y + look.y, e.z + look.z, 1, 0, 0, 0, 0);
            CastFx.line(lv, CastFx.dust(CastFx.ICE, 0.7f), e.add(look.scale(0.8)), e.add(look.scale(10)), 0.7);
            CastFx.sound(lv, e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 2f);
        });
        CastFx.sound(lv, eye, SoundEvents.BEACON_ACTIVATE, 0.4f, 2f);
        return true;
    }

    /** Makes {@code p}'s next shot exact for {@code ticks} (the same state Dead Eye sets; Mark and Fire arms it per round). */
    static void arm(ServerPlayer p, int ticks) {
        DEAD_EYE.put(p.getUUID(), p.level().getGameTime() + ticks);
    }

    /** Whether {@code p}'s next shot is a Dead Eye one (and if so, spends it). */
    public static boolean consume(ServerPlayer p) {
        Long until = DEAD_EYE.remove(p.getUUID());
        return until != null && p.level().getGameTime() <= until;
    }
}
