// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.net.ThermalFeedPayload;
import io.github.soundgoodizerfan.feedback.net.ThermalFeedPayload.Entry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Gathers what a thermal-vision wearer can see and sends it, only when it changed. In priority order: the real temperature of what holds
 * one (a block body, an item on the floor), then mobs by the body-heat table ({@link MobHeat}), then lava on a coarse grid. A scan reads loaded
 * chunks' block-entity lists, the entities in a box, and one fluid lookup per grid cell; it runs a few times a second, only for a wearer who has
 * the view on, and sends nothing when the picture is the same as the last one.
 */
final class ThermalScan {

    private ThermalScan() {
    }

    /** A body only shows when it is this far off ambient. */
    private static final float MIN_DELTA = 4f;
    /** Lava is sampled every this many blocks on a world-aligned grid, so the picture does not crawl as the wearer walks. */
    private static final int LAVA_GRID = 4;
    private static final float LAVA_TU = 1200f;

    private static final Map<UUID, Integer> LAST = new HashMap<>();

    static void forget(UUID id) {
        LAST.remove(id);
    }

    static void scan(ServerPlayer p, float range) {
        ServerLevel level = p.serverLevel();
        float amb = AmbientTable.at(level).value();
        List<Entry> out = new ArrayList<>();
        double rr = (double) range * range;
        double px = p.getX(), py = p.getY(), pz = p.getZ();
        int cap = SuitTuning.THERMAL_MAX_ENTRIES;

        // 1. what holds a real temperature
        int r = (int) Math.ceil(range);
        for (int cx = (int) Math.floor((px - r) / 16); cx <= (int) Math.floor((px + r) / 16) && out.size() < cap; cx++)
            for (int cz = (int) Math.floor((pz - r) / 16); cz <= (int) Math.floor((pz + r) / 16) && out.size() < cap; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null)
                    continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof ThermalBody body) || out.size() >= cap)
                        continue;
                    BlockPos pos = be.getBlockPos();
                    double dx = pos.getX() + 0.5 - px, dy = pos.getY() + 0.5 - py, dz = pos.getZ() + 0.5 - pz;
                    if (dx * dx + dy * dy + dz * dz > rr)
                        continue;
                    float tu = body.getTemperature().value();
                    if (Math.abs(tu - amb) >= MIN_DELTA)
                        out.add(new Entry((float) (pos.getX() + 0.5), (float) (pos.getY() + 0.5), (float) (pos.getZ() + 0.5), tu, ThermalFeedPayload.BLOCK, 1f));
                }
            }
        var box = p.getBoundingBox().inflate(range);
        List<Entity> near = level.getEntities(p, box, e -> e instanceof ItemEntity || e instanceof LivingEntity);
        for (Entity e : near) {
            if (out.size() >= cap || e.distanceToSqr(p) > rr || !(e instanceof ItemEntity item))
                continue;
            float tu = ItemHeat.get(item.getItem(), level).value();
            if (Math.abs(tu - amb) >= MIN_DELTA)
                out.add(new Entry((float) e.getX(), (float) (e.getY() + 0.15), (float) e.getZ(), tu, ThermalFeedPayload.ITEM, 0.3f));
        }
        // 2. living things, by body-heat class
        for (Entity e : near) {
            if (out.size() >= cap || e.distanceToSqr(p) > rr || !(e instanceof LivingEntity living) || living.isSpectator() || living.isInvisible() && !living.isCurrentlyGlowing())
                continue;
            out.add(new Entry((float) e.getX(), (float) (e.getY() + e.getBbHeight() * 0.5), (float) e.getZ(), MobHeat.get().tu(e), ThermalFeedPayload.MOB,
                    Math.max(e.getBbWidth(), e.getBbHeight() * 0.6f)));
        }
        // 3. lava, on a coarse grid
        if (out.size() < cap) {
            int minX = Math.floorDiv((int) (px - r), LAVA_GRID) * LAVA_GRID, maxX = (int) (px + r);
            int minZ = Math.floorDiv((int) (pz - r), LAVA_GRID) * LAVA_GRID, maxZ = (int) (pz + r);
            int minY = Math.floorDiv((int) (py - r / 2), LAVA_GRID) * LAVA_GRID, maxY = (int) (py + r / 2);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = minX; x <= maxX && out.size() < cap; x += LAVA_GRID)
                for (int z = minZ; z <= maxZ && out.size() < cap; z += LAVA_GRID)
                    for (int y = Math.max(level.getMinBuildHeight(), minY); y <= Math.min(level.getMaxBuildHeight() - 1, maxY) && out.size() < cap; y += LAVA_GRID) {
                        pos.set(x, y, z);
                        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
                        if (dx * dx + dy * dy + dz * dz > rr || !level.hasChunkAt(pos) || !level.getFluidState(pos).is(FluidTags.LAVA))
                            continue;
                        out.add(new Entry(x + 0.5f, y + 0.5f, z + 0.5f, LAVA_TU, ThermalFeedPayload.FLUID, LAVA_GRID * 0.5f));
                    }
        }

        int hash = 1;
        for (Entry e : out)
            hash = hash * 31 + java.util.Objects.hash(Math.round(e.x() * 8), Math.round(e.y() * 8), Math.round(e.z() * 8), Math.round(e.tu()), e.kind());
        Integer last = LAST.put(p.getUUID(), hash);
        if (last == null || last != hash)
            PacketDistributor.sendToPlayer(p, new ThermalFeedPayload(out));
    }
}
