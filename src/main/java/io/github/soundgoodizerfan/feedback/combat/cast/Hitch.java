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

import dev.ryanhcode.sable.companion.SableCompanion;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Hitch: a line fast to a block of a vehicle body, towed behind it; the anchor rides the body's pose. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Hitch {

    private Hitch() {
    }

    /** {@code local} is the hit point in the body's own (plot) space; the world anchor is rebuilt from the body's pose every tick. */
    private record Line(Vec3 local, double rope, long until) {
    }

    private static final Map<UUID, Line> LINES = new HashMap<>();

    /** [OPEN] stand-ins: throw range, tow time, and the stretch (blocks past the rope) one Mu of your body holds before the line parts. */
    private static final double REACH = 24;
    private static final int TOW_TICKS = 80;
    private static final double SNAP_STRETCH_PER_MU = 0.05;

    /** Hitch: fix a line to the vehicle block you look at (within 24 blocks) and be towed. Crouch to let go; the line parts when the stretch passes what your mass holds, or after four seconds. A plain block, or no look at one, does nothing and costs nothing. The body takes no reaction. */
    static boolean act(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        var hit = p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK || p.isPassenger())
            return false;
        Vec3 local = hit.getLocation();
        var body = SableCompanion.INSTANCE.getContaining(p.level(), local);
        if (body == null)
            return false;
        Vec3 anchor = body.logicalPose().transformPosition(local);
        LINES.put(p.getUUID(), new Line(local, Math.max(3, anchor.distanceTo(eye)), p.level().getGameTime() + TOW_TICKS));
        CastFx.sound(p.serverLevel(), anchor, SoundEvents.CHAIN_PLACE, 1f, 0.6f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !LINES.containsKey(p.getUUID()))
            return;
        Line l = LINES.get(p.getUUID());
        var body = SableCompanion.INSTANCE.getContaining(p.level(), l.local());
        if (body == null || p.level().getGameTime() >= l.until() || p.isShiftKeyDown() || p.isPassenger()) {
            LINES.remove(p.getUUID());
            return;
        }
        Vec3 anchor = body.logicalPose().transformPosition(l.local());
        Vec3 from = p.getEyePosition(), toAnchor = anchor.subtract(from);
        double d = toAnchor.length(), stretch = d - l.rope();
        if (stretch > SNAP_STRETCH_PER_MU * BodyMass.of(p)) {
            LINES.remove(p.getUUID());
            CastFx.sound(p.serverLevel(), from, SoundEvents.CHAIN_BREAK, 1f, 0.8f);
            return;
        }
        if (stretch > 0) {
            Vec3 radial = toAnchor.normalize(), dm = p.getDeltaMovement();
            dm = dm.add(radial.scale(Math.max(0, -dm.dot(radial)) + stretch * 0.3));
            p.setDeltaMovement(dm);
            p.hurtMarked = true;
        }
        p.fallDistance = 0f;
        CastFx.line(p.serverLevel(), CastFx.dust(CastFx.STEEL, 0.7f), from.add(0, -0.4, 0), anchor, 0.6);
    }
}
