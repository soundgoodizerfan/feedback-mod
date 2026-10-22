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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.registry.FCombat;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The roll (implementation plan 2.3). Direction comes from movement input at the press, forward if
 * there is none; the displacement is one impulse and the ground's friction does the rest, so the client
 * can run the same call and agree with the server. The i-frames cancel every damage event, which
 * is also how fire and other contact ticks are ignored (user ruling, plan 5.7) -- except what
 * bypasses invulnerability altogether (the void, /kill).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Rolling {

    private Rolling() {
    }

    /** Whether {@code player} may start a roll now. */
    public static boolean canRoll(Player player) {
        return CombatState.of(player).ready() && !player.isPassenger() && !player.isSpectator() && !player.isSleeping();
    }

    /**
     * Starts the roll: state, then the impulse. Run on the server for the verdict and on the client to
     * predict; the client's copy of the state is the server's synced one, so it only moves the body.
     */
    public static void begin(Player player, float forward, float strafe, boolean server) {
        if (!canRoll(player))
            return;
        if (server)
            CombatState.set(player, new CombatState(CombatPhase.ROLL, RollTuning.I_FRAMES, RollTuning.RECOVERY, RollTuning.I_FRAMES));
        Vec3 dir = direction(player.getYRot(), forward, strafe);
        player.setDeltaMovement(dir.x * RollTuning.SPEED, player.getDeltaMovement().y, dir.z * RollTuning.SPEED);
        if (server)
            player.hurtMarked = true;
    }

    /** The unit heading for input ({@code forward}, {@code strafe} left-positive) at {@code yawDeg}; forward if there is none. */
    public static Vec3 direction(float yawDeg, float forward, float strafe) {
        if (forward == 0 && strafe == 0)
            forward = 1;
        float yaw = yawDeg * Mth.DEG_TO_RAD;
        double x = -Mth.sin(yaw) * forward + Mth.cos(yaw) * strafe;
        double z = Mth.cos(yaw) * forward + Mth.sin(yaw) * strafe;
        return new Vec3(x, 0, z).normalize();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity who = event.getEntity();
        if (!who.level().isClientSide && who.hasData(FCombat.STATE) && CombatState.of(who).iFrames() > 0
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            event.setCanceled(true);
    }
}
