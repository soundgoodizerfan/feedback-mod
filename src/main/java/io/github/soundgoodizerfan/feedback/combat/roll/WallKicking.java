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
package io.github.soundgoodizerfan.feedback.combat.roll;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The wall kick (skill unlock {@code wall_kick}): a jump pressed in the air against a wall kicks the
 * fighter off it, up and back. The client sends the press ({@link WallKickPayload}); the server decides.
 * It needs momentum: the fighter was sprinting or rolling on the ground (or is mid-roll) within
 * {@link WallKickTuning#MOMENTUM_TICKS}, and a kick spends it, so a wall cannot be climbed by kicking
 * again and again without touching the ground to build speed anew. Direction assumes the fighter faces
 * the wall: straight back from the horizontal look.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class WallKicking {

    /** Game tick each player last had momentum (sprinting on the ground, or rolling). */
    private static final Map<UUID, Long> MOMENTUM = new HashMap<>();

    private WallKicking() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        CombatPhase phase = CombatState.of(p).phase();
        if (p.isSprinting() && p.onGround() || phase == CombatPhase.ROLL || phase == CombatPhase.ROLL_RECOVERY)
            MOMENTUM.put(p.getUUID(), p.level().getGameTime());
    }

    public static void kick(ServerPlayer player) {
        Long last = MOMENTUM.get(player.getUUID());
        if (last == null || player.level().getGameTime() - last > WallKickTuning.MOMENTUM_TICKS
                || player.onGround() || !player.horizontalCollision || player.isSpectator() || player.isPassenger()
                || !Skills.unlocked(player, "wall_kick"))
            return;
        MOMENTUM.remove(player.getUUID());
        Vec3 back = player.getLookAngle().multiply(-1, 0, -1);
        back = back.lengthSqr() < 1e-6 ? Vec3.ZERO : back.normalize();
        player.setDeltaMovement(back.x * WallKickTuning.PUSH, WallKickTuning.UP, back.z * WallKickTuning.PUSH);
        player.hurtMarked = true;
        player.fallDistance = 0f;
        player.causeFoodExhaustion(WallKickTuning.EXHAUSTION);
        CombatCues.everyone(player, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 1.2f);
    }
}
