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

import io.github.soundgoodizerfan.feedback.Feedback;
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

/** Swing: a line fast to a far block, and an arc across the gap beneath it. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Swing {

    private Swing() {
    }

    private record Line(Vec3 anchor, double rope, long born, long until) {
    }

    private static final Map<UUID, Line> LINES = new HashMap<>();

    /** Swing: fix a line to the block you look at (4-24 blocks off); you pendulum on it, the rope as long as the throw, until you land, crouch to let go or two and a half seconds pass. */
    static boolean act(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        var hit = p.level().clip(new ClipContext(eye, eye.add(look.scale(24)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getLocation().distanceTo(eye) < 4 || p.isPassenger())
            return false;
        long now = p.level().getGameTime();
        LINES.put(p.getUUID(), new Line(hit.getLocation(), hit.getLocation().distanceTo(eye), now, now + 50));
        p.setDeltaMovement(p.getDeltaMovement().add(look.scale(0.4)).add(0, 0.2, 0));
        p.hurtMarked = true;
        CastFx.sound(p.serverLevel(), hit.getLocation(), SoundEvents.CHAIN_PLACE, 1f, 0.8f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !LINES.containsKey(p.getUUID()))
            return;
        Line l = LINES.get(p.getUUID());
        long now = p.level().getGameTime();
        if (now >= l.until() || p.isShiftKeyDown() || p.onGround() && now - l.born() > 6 || p.isPassenger()) {
            LINES.remove(p.getUUID());
            p.setDeltaMovement(p.getDeltaMovement().scale(1.1)); // let go with the swing still in you
            p.hurtMarked = true;
            return;
        }
        Vec3 from = p.getEyePosition(), toAnchor = l.anchor().subtract(from);
        double d = toAnchor.length();
        Vec3 dm = p.getDeltaMovement();
        if (d > l.rope()) {
            Vec3 radial = toAnchor.normalize();
            double outward = -dm.dot(radial); // speed running away from the anchor is what the rope takes out
            if (outward > 0)
                dm = dm.add(radial.scale(outward));
            dm = dm.add(radial.scale((d - l.rope()) * 0.3));
            p.setDeltaMovement(dm);
            p.hurtMarked = true;
        }
        p.fallDistance = 0f;
        CastFx.line(p.serverLevel(), CastFx.dust(CastFx.STEEL, 0.7f), from.add(0, -0.4, 0), l.anchor(), 0.6);
    }
}
