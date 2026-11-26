// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;

/**
 * A craft that touches down hard hurts the people aboard (docs/specs/feedback_space_spec.md 6). Every physics step a body's velocity is
 * compared with the last step's; a drop in one step that no engine makes is a hit. Its full-size speed change is turned into the fall
 * that would stop a person as fast ({@code dv^2 / 2g}) and every living thing aboard takes fall damage for the part over
 * {@link SpaceTuning#IMPACT_FREE_FALL_BLOCKS}. The physics thread only notes the hit; the damage is done on the level's own tick.
 * <p>
 * shortcut: blocks at the contact are not singled out (Sable gives no contact point); a very hard hit shatters the brittle blocks anywhere aboard instead.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Impact {
    private static final double G = 9.81;

    private record Hit(ResourceKey<Level> level, UUID body, double fall) {
    }

    /** A body's velocity and scale as of the last step: a crossing rewrites both, which is not a hit. */
    private record Step(Vector3d velocity, double scale) {
    }

    private static final Map<UUID, Step> LAST = new ConcurrentHashMap<>();
    private static final Queue<Hit> HITS = new ConcurrentLinkedQueue<>();

    private Impact() {
    }

    @SubscribeEvent
    public static void onPhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        ServerLevel level = event.getPhysicsSystem().getLevel();
        var container = ServerSubLevelContainer.getContainer(level);
        if (container == null)
            return;
        for (ServerSubLevel body : container.getAllSubLevels()) {
            if (body.isRemoved())
                continue;
            Vector3d v = event.getPhysicsSystem().getPhysicsHandle(body).getLinearVelocity(new Vector3d());
            double scale = BodyScale.of(body);
            Step was = LAST.put(body.getUniqueId(), new Step(v, scale));
            if (was == null || was.scale() != scale)
                continue;
            double dv = v.distance(was.velocity()) / scale;
            if (dv < SpaceTuning.IMPACT_MIN_SPEED_CHANGE)
                continue;
            HITS.add(new Hit(level.dimension(), body.getUniqueId(), dv * dv / (2 * G)));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || HITS.isEmpty())
            return;
        for (Hit hit : HITS) {
            if (!hit.level().equals(level.dimension()))
                continue;
            HITS.remove(hit);
            var container = ServerSubLevelContainer.getContainer(level);
            if (container == null || !(container.getSubLevel(hit.body()) instanceof ServerSubLevel body))
                continue;
            float damage = (float) Math.ceil(hit.fall() - SpaceTuning.IMPACT_FREE_FALL_BLOCKS);
            if (hit.fall() > SpaceTuning.IMPACT_PUNCTURE_FALL_BLOCKS)
                puncture(level, body, hit.fall());
            if (hit.fall() > SpaceTuning.IMPACT_BREAK_FALL_BLOCKS)
                shatter(level, body, hit.fall());
            if (damage <= 0)
                continue;
            var b = body.boundingBox();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()).inflate(1)))
                e.hurt(level.damageSources().fall(), damage);
        }
    }

    /**
     * A hard hit tears gas cells aboard: each cell in the body's plot is punctured with a chance that grows from nothing at
     * {@link SpaceTuning#IMPACT_PUNCTURE_FALL_BLOCKS} to certain at twice it. A skin that took the blow no better than an arrow's point
     * leaks as an arrow hole does (and is patched the same way).
     */
    private static void puncture(ServerLevel level, ServerSubLevel body, double fall) {
        double chance = Math.min(1.0, (fall - SpaceTuning.IMPACT_PUNCTURE_FALL_BLOCKS) / SpaceTuning.IMPACT_PUNCTURE_FALL_BLOCKS);
        var box = body.getPlot().getBoundingBox();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null)
                    continue;
                for (var be : chunk.getBlockEntities().values())
                    if (be instanceof io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlockEntity cell && level.random.nextDouble() < chance)
                        cell.puncture();
            }
    }

    /**
     * A very hard hit shatters what is brittle aboard: every block in the body's plot at or under {@link SpaceTuning#IMPACT_BRITTLE_HARDNESS}
     * (glass, panes) breaks, with a chance that grows from nothing at {@link SpaceTuning#IMPACT_BREAK_FALL_BLOCKS} to certain at twice it. The
     * blocks at the contact are not singled out: Sable gives no contact point, so the brittle ones anywhere in the body stand for them.
     */
    private static void shatter(ServerLevel level, ServerSubLevel body, double fall) {
        double chance = Math.min(1.0, (fall - SpaceTuning.IMPACT_BREAK_FALL_BLOCKS) / SpaceTuning.IMPACT_BREAK_FALL_BLOCKS);
        var box = body.getPlot().getBoundingBox();
        var at = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++)
            for (int y = box.minY(); y <= box.maxY(); y++)
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    at.set(x, y, z);
                    var state = level.getBlockState(at);
                    if (state.isAir())
                        continue;
                    float hardness = state.getDestroySpeed(level, at);
                    if (hardness >= 0f && hardness <= SpaceTuning.IMPACT_BRITTLE_HARDNESS && level.random.nextDouble() < chance)
                        level.destroyBlock(at, false);
                }
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        LAST.clear();
        HITS.clear();
    }
}
