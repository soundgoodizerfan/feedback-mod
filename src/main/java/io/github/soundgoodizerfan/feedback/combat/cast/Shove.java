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
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.vehicle.BodyPush;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.joml.Vector3d;

/**
 * Shove and Carry: hands on a vehicle body, held while the cast key is down. The ratio of the body's mass to yours
 * (plus worn armour) decides the act: within your carry ratio you hold it at a point in front of the camera like a
 * prop; past it you shove it along the ground, or drag it back, by impulse at the contact point. A shove delivers
 * your momentum (mass times how fast you move) and the body gives an equal reaction back to you; the tyres' grip and
 * the brake pads resist through the physics, so a braked car simply does not move. The server decides everything:
 * the client only says the key is down. Blocks tagged {@code feedback:unshovable} cannot be taken hold of.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Shove {

    static final TagKey<Block> UNSHOVABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "unshovable"));

    /** A held body is found again each tick by the contact point in its own plot space. */
    private static final Map<UUID, Vec3> GRIPS = new HashMap<>();
    /** Work delivered since the last Brute point, per player. */
    private static final Map<UUID, Double> WORK = new HashMap<>();

    static final Casts.Hold HOLD = new Casts.Hold(ShoveTuning.HOLD_DRAIN, Shove::tick, Shove::drop);

    private Shove() {
    }

    static boolean act(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        if (p.isPassenger())
            return false;
        var hit = p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(ShoveTuning.REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK || p.level().getBlockState(hit.getBlockPos()).is(UNSHOVABLE))
            return false;
        Vec3 local = hit.getLocation();
        if (!(SableCompanion.INSTANCE.getContaining(p.level(), local) instanceof ServerSubLevel body)
                || body.getMassTracker().getMass() < ShoveTuning.MIN_MASS || standingOn(p, body))
            return false;
        GRIPS.put(p.getUUID(), local);
        CastFx.sound(p.serverLevel(), body.logicalPose().transformPosition(local), SoundEvents.ANVIL_PLACE, 0.5f, 0.7f);
        return true;
    }

    private static boolean standingOn(ServerPlayer p, ServerSubLevel body) {
        var under = SableCompanion.INSTANCE.getTrackingSubLevel(p);
        return under != null && under.getUniqueId().equals(body.getUniqueId());
    }

    /** One tick of the hold; false once it has ended itself. */
    static boolean tick(ServerPlayer p) {
        Vec3 local = GRIPS.get(p.getUUID());
        if (local == null)
            return false;
        if (p.isPassenger() || !p.isAlive()
                || !(SableCompanion.INSTANCE.getContaining(p.level(), local) instanceof ServerSubLevel body) || standingOn(p, body)) {
            drop(p);
            return false;
        }
        Vec3 world = body.logicalPose().transformPosition(local), eye = p.getEyePosition();
        Vector3d at = new Vector3d(world.x, world.y, world.z);
        float mine = BodyMass.of(p) + BodyMass.armour(p);
        double ratio = body.getMassTracker().getMass() / mine;
        boolean ok = world.distanceTo(eye) <= ShoveTuning.REACH + ShoveTuning.STRETCH
                && (ratio <= ShoveTuning.CARRY_RATIO + Skills.modifier(p, SkillTree.Figure.CARRY_RATIO)
                ? carry(p, body, at, eye, ratio) : shove(p, body, at, mine));
        if (!ok)
            drop(p);
        return ok;
    }

    /** Holds the contact point at a spot in front of the camera: it is driven toward it at a speed proportional to the error, by the impulse the point's own mass needs to reach that speed. */
    private static boolean carry(ServerPlayer p, ServerSubLevel body, Vector3d at, Vec3 eye, double ratio) {
        if (!Stamina.spend(p, (float) (ratio * ShoveTuning.CARRY_STAMINA_PER_RATIO)))
            return false;
        Vec3 want = eye.add(p.getLookAngle().scale(ShoveTuning.HOLD_DISTANCE));
        Vector3d dv = new Vector3d(want.x - at.x, want.y - at.y, want.z - at.z);
        if (dv.length() > ShoveTuning.SNAG_ERROR)
            return false; // snagged on something: the hold lets go rather than tear
        dv.mul(ShoveTuning.CARRY_GAIN);
        if (dv.length() > ShoveTuning.CARRY_MAX_SPEED)
            dv.normalize(ShoveTuning.CARRY_MAX_SPEED);
        dv.sub(BodyPush.pointVelocity(body, at));
        double len = dv.length();
        if (len < 1e-4)
            return true;
        dv.div(len);
        BodyPush.impulseAt(body, at, dv.mul(Math.min(BodyPush.effectiveMass(body, at, dv) * len, ShoveTuning.MAX_IMPULSE)));
        return true;
    }

    /** Pushes along the horizontal look (forward input) or drags back along it (backward input), at your momentum, with the equal reaction on you. */
    private static boolean shove(ServerPlayer p, ServerSubLevel body, Vector3d at, float mine) {
        Vec3 look = p.getLookAngle();
        double flat = Math.hypot(look.x, look.z);
        if (p.zza == 0 || flat < 1e-3)
            return true; // leaning on it: only the hold's own drain
        double sign = p.zza > 0 ? 1 : -1;
        Vector3d dir = new Vector3d(look.x / flat * sign, 0, look.z / flat * sign);
        double impulse = Math.min(mine * p.getSpeed() * ShoveTuning.MS_PER_SPEED * ShoveTuning.TICK_SECONDS, ShoveTuning.MAX_IMPULSE);
        // Work this tick is force times distance, which is impulse times the hand's speed along the push.
        double work = impulse * Math.max(0, BodyPush.pointVelocity(body, at).dot(dir));
        if (!Stamina.spend(p, (float) Math.min(work * ShoveTuning.STAMINA_PER_WORK, ShoveTuning.MAX_STAMINA_PER_TICK)))
            return false;
        BodyPush.impulseAt(body, at, new Vector3d(dir).mul(impulse));
        double back = impulse / mine / 20; // m/s of the reaction, as blocks per tick
        p.setDeltaMovement(p.getDeltaMovement().add(-dir.x * back, 0, -dir.z * back));
        p.hurtMarked = true;
        double sum = WORK.merge(p.getUUID(), work, Double::sum);
        if (sum >= ShoveTuning.WORK_PER_POINT) {
            WORK.put(p.getUUID(), sum - ShoveTuning.WORK_PER_POINT);
            SkillEarning.pushed(p, body.getUniqueId());
        }
        return true;
    }

    static void drop(ServerPlayer p) {
        GRIPS.remove(p.getUUID());
        WORK.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GRIPS.remove(event.getEntity().getUUID());
        WORK.remove(event.getEntity().getUUID());
    }
}
