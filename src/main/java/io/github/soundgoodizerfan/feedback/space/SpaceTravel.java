// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.egg.dimensionalsable.SubLevelWarper;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.sablescale.scale.SubLevelScale;
import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import javax.annotation.Nullable;

/**
 * The Gap prototype (docs/specs/feedback_space_spec.md): an operator moves a Sable body and
 * everything joined to it between the Overworld and {@code feedback:gap}, and scales one.
 * The move is Dimensional Sable's (MIT; serialises the body and loads it in the other level, keeping velocity),
 * the scale is Sable Scaling's (MIT; resamples the colliders and scales mass). Both are required mods and
 * called through their public API; nothing of theirs is copied.
 */
public final class SpaceTravel {
    /** The acid-cloud world: a dimension of its own, reached from the Gap along {@link AcidRoute}'s bearing. */
    public static final ResourceKey<Level> ACID = ResourceKey.create(Registries.DIMENSION,
            io.github.soundgoodizerfan.feedback.Feedback.id("acid_cloud"));
    public static final ResourceKey<Level> GAP = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "gap"));
    private static final double LOOK_REACH = 16;

    private SpaceTravel() {
    }

    /** The body the player stands on, else the one they look at; null if neither is a Sable body. */
    @Nullable
    public static ServerSubLevel bodyOf(ServerPlayer player) {
        if (SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(player) instanceof ServerSubLevel s) return s;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(LOOK_REACH));
        var hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return SableCompanion.INSTANCE.getContaining(player.level(), hit.getBlockPos()) instanceof ServerSubLevel s ? s : null;
    }

    /** Bodies moved (the chain), or -1 when {@code body} is already in the Gap. */
    public static int send(ServerLevel from, ServerSubLevel body) {
        if (from.dimension().equals(GAP)) return -1;
        ServerLevel gap = from.getServer().getLevel(GAP);
        if (gap == null) return -2;
        return cross(from, gap, body, flat(body, SpaceTuning.ENTRY_Y), true, SpaceTuning.GAP_SCALE, v -> v);
    }

    /** Bodies moved, or -1 when {@code body} is not in the Gap. Arrives where the Overworld's air is thin but not zero. */
    public static int back(ServerLevel from, ServerSubLevel body) {
        if (!from.dimension().equals(GAP)) return -1;
        return cross(from, from.getServer().overworld(), body, flat(body, SpaceTuning.RETURN_Y), false, 1.0, v -> v);
    }

    /**
     * People in the Gap below its floor who are on no Sable body fall out of it: they arrive in the Overworld at the same x and z, at
     * {@link SpaceTuning#RETURN_Y}, keeping speed and fall distance, and burn from the way in. A person on a body goes with the body.
     */
    public static void dropStrays(ServerLevel gap) {
        ServerLevel overworld = gap.getServer().overworld();
        for (ServerPlayer p : new java.util.ArrayList<>(gap.players())) {
            if (p.isSpectator() || p.getY() >= SpaceTuning.FLOOR_Y)
                continue;
            if (SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(p) != null
                    || SableCompanion.INSTANCE.getContaining(gap, p.blockPosition()) != null)
                continue;
            Vec3 v = p.getDeltaMovement();
            float fall = p.fallDistance;
            p.stopRiding();
            p.teleportTo(overworld, p.getX(), SpaceTuning.RETURN_Y, p.getZ(), java.util.Set.of(), p.getYRot(), p.getXRot());
            p.setDeltaMovement(v);
            p.fallDistance = fall;
            p.hurtMarked = true;
            p.setRemainingFireTicks(SpaceTuning.REENTRY_FIRE_TICKS);
            ScaleAboard.set(p, 1.0);
            fade(java.util.List.of(p), false);
        }
    }

    /** Where a player last stood on a body (their feet, in the body's frame) and when. */
    private record Stand(java.util.UUID body, Vec3 local, long tick) {
    }

    private static final java.util.Map<java.util.UUID, Stand> STANDING = new java.util.HashMap<>();

    /** Ticks a remembered standing place is still trusted. */
    private static final long STAND_KEEP_TICKS = 200;

    /**
     * Remembers where each player on the ground stands on their body. A craft at speed leaves its rider trailing behind the deck
     * by the time it crosses; where they last stood on it is where they belong on the other side.
     */
    public static void noteStanding(ServerLevel level) {
        for (ServerPlayer p : level.players())
            if (p.onGround() && SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(p) instanceof ServerSubLevel s)
                STANDING.put(p.getUUID(), new Stand(s.getUniqueId(), s.logicalPose().transformPositionInverse(p.position()), level.getGameTime()));
    }

    public static void forgetStanding() {
        STANDING.clear();
    }

    /** A player Sable has tracked on the chain: where they stood on their body, so they can follow it across. */
    private record Rider(ServerPlayer player, java.util.UUID body, Vec3 local, Vec3 velocity) {
    }

    private static Rider rider(dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem physics, ServerPlayer p, ServerSubLevel on) {
        Vector3d v = physics.getPhysicsHandle(on).getLinearVelocity(new Vector3d());
        Stand stood = STANDING.get(p.getUUID());
        Vec3 local = stood != null && stood.body().equals(on.getUniqueId()) && on.getLevel().getGameTime() - stood.tick() <= STAND_KEEP_TICKS
                ? stood.local() : on.logicalPose().transformPositionInverse(p.position());
        return new Rider(p, on.getUniqueId(), local, new Vec3(v.x / 20, v.y / 20, v.z / 20));
    }

    /** The body's own x and z at height {@code y}. */
    private static Vector3d flat(ServerSubLevel body, double y) {
        var p = body.logicalPose().position();
        return new Vector3d(p.x(), y, p.z());
    }

    /**
     * Moves {@code body} and its chain to {@code target} at {@code at}, to scale {@code scale}; its velocity is converted by the scale
     * ratio and then passed through {@code bend} (blocks a second in and out, the same for everyone carried). Dimensional Sable carries what stands inside each
     * body's box; a craft moving several blocks a tick leaves a rider trailing outside it, so everyone Sable has tracked on the chain
     * is also brought across to the same place on the body afterwards, if the move left them behind.
     */
    static int cross(ServerLevel from, ServerLevel target, ServerSubLevel body, Vector3d at, boolean up, double scale,
            java.util.function.UnaryOperator<Vector3d> bend) {
        double ratio = scale / BodyScale.of(body);
        var chain = SubLevelWarper.getSubLevelGroup(body);
        var ids = new java.util.HashSet<java.util.UUID>();
        for (ServerSubLevel b : chain)
            ids.add(b.getUniqueId());
        var riders = new java.util.ArrayList<Rider>();
        var physics = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(from).physicsSystem();
        for (ServerPlayer p : from.players())
            if (SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(p) instanceof ServerSubLevel s && ids.contains(s.getUniqueId()))
                riders.add(rider(physics, p, s));
        var seeing = new java.util.LinkedHashSet<>(aboard(from, body));
        // Whoever is beside the craft is carried too, even if Sable does not have them standing on it.
        for (ServerPlayer p : seeing)
            if (riders.stream().noneMatch(r -> r.player() == p))
                riders.add(rider(physics, p, body));
        for (Rider r : riders)
            seeing.add(r.player());
        // Sable tells everyone following a body that it is gone, by where its plot is, once the body is removed. Plots are the same in
        // both dimensions, so a rider already across would be told to forget the body they arrived on, and stop riding it. They stop
        // following the bodies here now instead (Genesis's order; Sable has them follow the new ones on its next tick).
        var fromContainer = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(from);
        if (fromContainer != null)
            for (ServerSubLevel b : fromContainer.getAllSubLevels())
                for (ServerPlayer p : seeing)
                    b.getTrackingPlayers().remove(p.getUUID());
        int n = SubLevelWarper.warpConnectedSubLevels(body, target, at);
        if (n <= 0)
            return n;
        var container = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(target);
        // The craft is scaled on the far side: lengths scale by k and time does not, so every speed scales by k with them.
        if (container.getSubLevel(body.getUniqueId()) instanceof ServerSubLevel moved) {
            SubLevelScale.apply(moved, scale);
            for (ServerSubLevel b : SubLevelWarper.getSubLevelGroup(moved)) {
                var handle = dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(b);
                Vector3d v = handle.getLinearVelocity(new Vector3d());
                handle.addLinearAndAngularVelocity(bend.apply(new Vector3d(v).mul(ratio)).sub(v), new Vector3d());
                var bb = b.boundingBox();
                for (net.minecraft.world.entity.LivingEntity e : target.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        new net.minecraft.world.phys.AABB(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.maxY(), bb.maxZ()).inflate(2)))
                    ScaleAboard.set(e, scale);
            }
        }
        for (Rider r : riders) {
            if (container.getSubLevel(r.body()) instanceof ServerSubLevel moved) {
                Vec3 w = moved.logicalPose().transformPosition(r.local());
                r.player().teleportTo(target, w.x, w.y, w.z, java.util.Set.of(), r.player().getYRot(), r.player().getXRot());
            }
            // The craft keeps its speed across, so its rider must, or it leaves them behind.
            if (r.player().level() == target) {
                Vector3d v = bend.apply(new Vector3d(r.velocity().x * 20 * ratio, r.velocity().y * 20 * ratio, r.velocity().z * 20 * ratio));
                r.player().setDeltaMovement(v.x / 20, v.y / 20, v.z / 20);
                r.player().hurtMarked = true;
            }
        }
        for (ServerPlayer p : seeing)
            ScaleAboard.set(p, scale);
        fade(seeing, up);
        return n;
    }

    /** Players inside or beside the body: they see the crossing. */
    private static java.util.List<ServerPlayer> aboard(ServerLevel level, ServerSubLevel body) {
        var b = body.boundingBox();
        return level.getEntitiesOfClass(ServerPlayer.class,
                new net.minecraft.world.phys.AABB(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()).inflate(2));
    }

    private static void fade(java.util.Collection<ServerPlayer> players, boolean up) {
        for (ServerPlayer p : players)
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new io.github.soundgoodizerfan.feedback.net.SpaceFadePayload(up));
    }

    /** Sets the scale of {@code body} and every body joined to it (Sable Scaling walks the chain). */
    public static void scale(ServerSubLevel body, double k) {
        SubLevelScale.apply(body, k);
    }

    public static double minScale() {
        return SubLevelScale.MIN_SCALE;
    }

    public static double maxScale() {
        return SubLevelScale.MAX_SCALE;
    }
}
