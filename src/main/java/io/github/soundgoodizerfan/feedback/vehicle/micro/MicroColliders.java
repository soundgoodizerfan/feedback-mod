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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderData;
import dev.ryanhcode.sable.util.LevelAccelerator;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A micro cell's physics collider at voxel resolution ({@code docs/specs/feedback_micro_spec.md},
 * <em>Voxel colliders</em>). Sable bakes a collider per block state, so a cell's state can only say
 * which half-block octants it fills; that put two bodies whose voxels never touch into contact
 * whenever they shared an octant, and froze a micro bearing (playtest 2026-11-02). Here each cell
 * hands Sable's Rapier scene its own collider, the exact merged boxes of its voxels, at its
 * position: a collider entry per voxel pattern (shared, made once), written with the same native
 * call and packing Sable uses for a block change.
 *
 * <p><b>This reaches into Sable's Rapier implementation</b> ({@code physics.impl.rapier}, not its
 * API): 2.0.6's {@code RapierPhysicsPipeline.handleBlockChange} writes
 * {@code neighbourhood byte | (entry handle + 1) << 16} through {@code Rapier3D.changeBlock}, and
 * nothing in Sable's API lets a block give a per-position collider ({@code
 * BlockSubLevelDynamicCollider} exists but nothing calls it). Sable rewrites a block's collider
 * whenever it or a neighbour changes, and when its chunk section is uploaded, so the cell re-applies
 * after its own commits, after a neighbour change, and every {@link #REFRESH} ticks. Any failure
 * (a Sable update) turns this off for the session and leaves the octant collider, with a log line.
 */
public final class MicroColliders {

    private static final Logger LOG = LoggerFactory.getLogger("feedback.microcollider");
    /** Ticks between re-applying, in case Sable re-uploaded a section without a block change. */
    public static final int REFRESH = 40;
    /** A collider's identity: the cube voxels' mask and a hash of the wedge boxes beside them (two hashes, so a clash is not a worry). */
    private record Key(long mask, long wedgeA, long wedgeB) {
    }

    private static final java.util.HashMap<Key, RapierVoxelColliderData> ENTRIES = new java.util.HashMap<>();
    private static final Map<ServerLevel, LevelAccelerator> ACCELERATORS = new WeakHashMap<>();
    private static boolean broken;

    private MicroColliders() {
    }

    /** Writes the collider of {@code pattern} (voxel bits, as MicroCellBlockEntity#filled) at {@code pos}. */
    public static void apply(ServerLevel level, BlockPos pos, BlockState state, long pattern) {
        apply(level, pos, state, pattern, java.util.List.of());
    }

    /** As {@link #apply(ServerLevel, BlockPos, BlockState, long)}, with extra boxes ({@code [x0, y0, z0, x1, y1, z1]} in blocks of the cell): wedge steps. */
    public static void apply(ServerLevel level, BlockPos pos, BlockState state, long pattern, java.util.List<double[]> extra) {
        if (broken)
            return;
        long ha = 1125899906842597L, hb = 7;
        for (double[] b : extra)
            for (double v : b) {
                long bits = Double.doubleToLongBits(v);
                ha = ha * 31 + bits;
                hb = (hb ^ bits) * 0x9E3779B97F4A7C15L;
            }
        Key key = new Key(pattern, ha, hb);
        try {
            RapierVoxelColliderData entry = ENTRIES.get(key);
            if (entry == null) {
                entry = Rapier3D.createVoxelColliderEntry(PhysicsBlockPropertyHelper.getFriction(state),
                        PhysicsBlockPropertyHelper.getVolume(state), PhysicsBlockPropertyHelper.getRestitution(state), false,
                        BlockWithSubLevelCollisionCallback.sable$getCallback(state));
                RapierVoxelColliderData e = entry;
                MicroCellBlockEntity.merge(pattern).forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                        e.addBox(new Vector3d(x0, y0, z0), new Vector3d(x1, y1, z1)));
                for (double[] b : extra)
                    e.addBox(new Vector3d(b[0], b[1], b[2]), new Vector3d(b[3], b[4], b[5]));
                ENTRIES.put(key, entry);
            }
            LevelAccelerator accelerator = ACCELERATORS.computeIfAbsent(level, LevelAccelerator::new);
            accelerator.clearCache();
            byte neighbourhood = VoxelNeighborhoodState.getState(accelerator, pos, null).byteRepresentation();
            // Sable's own packing (RapierPhysicsPipeline.packBlockState): the byte, sign and all, or the handle.
            Rapier3D.changeBlock(Rapier3D.getSceneHandle(level), pos.getX(), pos.getY(), pos.getZ(),
                    neighbourhood | (entry.handle() + 1) << 16);
        } catch (Throwable t) {
            broken = true;
            LOG.warn("[feedback] voxel colliders off for this session (Sable's internals changed?): {}", t.toString());
        }
    }
}
