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
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.hazard.Shock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Conduct: a line to a live bare conductor, the target hauled along it and shocked on arrival; you take the shock too unless your hands are proof. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Conduct {

    private Conduct() {
    }

    private record Haul(UUID target, BlockPos wire, long until) {
    }

    private static final Map<UUID, Haul> HAULS = new HashMap<>();

    /** [OPEN] stand-ins: wire search reach, and how far around the look point a wire counts as the one looked at. */
    private static final double WIRE_REACH = 12, WIRE_SLACK = 2;
    private static final int SPARK = 0xFFF0A0;

    /** Conduct: fix the line to the live bare conductor nearest your look (12 blocks), then haul the nearest thing ahead to you along it. No live wire, no target, or a body over 2.5 times your mass: nothing happens and nothing is spent. */
    static boolean act(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        LivingEntity t = ahead(p, WIRE_REACH, 0.9);
        BlockPos wire = liveWire(p);
        if (t == null || wire == null || BodyMass.of(t) > BodyMass.of(p) * 2.5f)
            return false;
        HAULS.put(p.getUUID(), new Haul(t.getUUID(), wire, lv.getGameTime() + 24));
        CastFx.sound(lv, Vec3.atCenterOf(wire), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4f, 2f);
        return true;
    }

    /** Volts a wire at {@code pos} puts on whatever touches it right now: zero for insulated, idle or not a conductor. */
    private static float live(ServerLevel lv, BlockPos pos) {
        return lv.getBlockEntity(pos) instanceof ElectricNode n ? n.touchVolts() : 0f;
    }

    /** The live bare conductor nearest the point your look reaches, or null. */
    private static BlockPos liveWire(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(WIRE_REACH));
        Vec3 at = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getLocation();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(BlockPos.containing(at).offset(-2, -2, -2), BlockPos.containing(at).offset(2, 2, 2))) {
            double d = Vec3.atCenterOf(q).distanceToSqr(at);
            if (d < bestD && d <= (WIRE_SLACK + 1) * (WIRE_SLACK + 1) && live(lv, q) > 0f) {
                bestD = d;
                best = q.immutable();
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !HAULS.containsKey(p.getUUID()))
            return;
        Haul h = HAULS.get(p.getUUID());
        ServerLevel lv = p.serverLevel();
        if (!(lv.getEntity(h.target()) instanceof LivingEntity t) || !t.isAlive() || lv.getGameTime() >= h.until()) {
            HAULS.remove(p.getUUID());
            return;
        }
        Vec3 hand = p.getEyePosition().add(0, -0.4, 0), mid = t.getBoundingBox().getCenter(), to = hand.subtract(mid);
        double dist = to.length();
        if (dist < 2.0) {
            HAULS.remove(p.getUUID());
            float volts = live(lv, h.wire()); // the wire as it is now: a line cut mid-haul shocks nobody
            t.setDeltaMovement(Vec3.ZERO);
            Shock.apply(t, volts);
            Shock.apply(p, volts); // refused by Shock when the hands are proof against these volts
            if (volts > 0f)
                stagger(p, t, (BodyMass.of(p) + BodyMass.armour(p)) * 9.8f * Skills.reelScale(p));
            CastFx.spray(lv, CastFx.dust(SPARK, 1.1f), mid, 18, 0.25);
            CastFx.sound(lv, t.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);
            return;
        }
        t.setDeltaMovement(to.normalize().scale(Math.min(1.0, dist * 0.25)).add(0, 0.05, 0));
        t.hurtMarked = true;
        CastFx.line(lv, CastFx.dust(SPARK, 0.8f), Vec3.atCenterOf(h.wire()), hand, 0.4);
        CastFx.line(lv, CastFx.dust(SPARK, 0.8f), hand, mid, 0.5);
    }
}
