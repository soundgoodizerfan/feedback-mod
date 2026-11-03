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
package io.github.soundgoodizerfan.feedback.machine.refrigeration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.Refrigerable;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The room a Refrigeration Unit faces: a bounded flood fill out from the unit through everything
 * that is not a wall, found by shape and never by block. Each wall face the fill meets leaks its
 * material's thermal conductivity; an opening (a cell that sees the sky, or the edge of the box)
 * leaks {@link FTuning#ROOM_OPENING_LEAK}. A gap never un-forms the room, it only raises the leak.
 * The fill is recomputed, never stored.
 *
 * <p>Taken from Firmalife's Climate Station (MIT, design only, no code): a bounded 6-direction flood
 * fill against a wall predicate, an interior set the owner keeps, a block count shown to the player.
 * Not taken: its wall block tag, its fixed decay multipliers and its biome-set cold; here the walls
 * are physical and the room is a temperature.
 */
final class ColdRoom {

    /** What a fill found. {@code interior} is block positions as longs; {@code min}/{@code max} bound them. */
    record Scan(LongOpenHashSet interior, int openings, float leak, List<RefrigerationUnitBlockEntity> units,
                List<Refrigerable> boxes, List<BlockPos> openingAt, BlockPos min, BlockPos max) {

        int volume() {
            return interior.size();
        }
    }

    private ColdRoom() {}

    /** A full block, or a closed door, trapdoor or gate: what holds air. */
    static boolean wall(Level level, BlockPos pos, BlockState state) {
        if (state.isAir())
            return false;
        if (state.isCollisionShapeFullBlock(level, pos))
            return true;
        return (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.FENCE_GATES))
                && state.hasProperty(BlockStateProperties.OPEN) && !state.getValue(BlockStateProperties.OPEN);
    }

    static Scan scan(Level level, BlockPos origin) {
        int r = FTuning.ROOM_RADIUS;
        var materials = MaterialTable.get().materials();
        Map<Block, Float> leaks = new HashMap<>();
        LongOpenHashSet interior = new LongOpenHashSet(), seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<RefrigerationUnitBlockEntity> units = new ArrayList<>();
        List<Refrigerable> boxes = new ArrayList<>();
        List<BlockPos> openingAt = new ArrayList<>();
        float leak = 0f;
        int openings = 0;
        int[] lo = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, hi = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        queue.add(origin);
        for (BlockPos cell = queue.poll(); cell != null; cell = queue.poll()) {
            // A wall beside the unit itself is not a wall of the room.
            boolean fromUnit = cell.equals(origin);
            for (Direction d : Direction.values()) {
                BlockPos n = cell.relative(d);
                if (n.equals(origin))
                    continue;
                if (Math.abs(n.getX() - origin.getX()) > r || Math.abs(n.getY() - origin.getY()) > r || Math.abs(n.getZ() - origin.getZ()) > r) {
                    openings++;
                    leak += FTuning.ROOM_OPENING_LEAK;
                    if (openingAt.size() < 64)
                        openingAt.add(n);
                    continue;
                }
                // An unloaded neighbour is unknown air: counted as a gap, never as a seal.
                if (!level.isLoaded(n)) {
                    openings++;
                    leak += FTuning.ROOM_OPENING_LEAK;
                    continue;
                }
                BlockState state = level.getBlockState(n);
                if (wall(level, n, state)) {
                    if (!fromUnit)
                        leak += leaks.computeIfAbsent(state.getBlock(), b -> Material.find(materials, b.asItem())
                                .flatMap(l -> l.material().thermal().conductivity()).map(Conductance::workPerTickPerTu)
                                .orElse(FTuning.ROOM_WALL_DEFAULT_LEAK));
                    if (state.hasBlockEntity()) {
                        BlockEntity be = level.getBlockEntity(n);
                        if (be instanceof RefrigerationUnitBlockEntity u && !units.contains(u))
                            units.add(u);
                        else if (be instanceof Refrigerable box && !boxes.contains(box))
                            boxes.add(box);
                    }
                    continue;
                }
                if (!seen.add(n.asLong()))
                    continue;
                if (level.canSeeSky(n)) {
                    openings++;
                    leak += FTuning.ROOM_OPENING_LEAK;
                    if (openingAt.size() < 64)
                        openingAt.add(n);
                    continue;
                }
                interior.add(n.asLong());
                lo[0] = Math.min(lo[0], n.getX());
                lo[1] = Math.min(lo[1], n.getY());
                lo[2] = Math.min(lo[2], n.getZ());
                hi[0] = Math.max(hi[0], n.getX());
                hi[1] = Math.max(hi[1], n.getY());
                hi[2] = Math.max(hi[2], n.getZ());
                queue.add(n);
            }
        }
        if (interior.isEmpty())
            return new Scan(interior, openings, leak, units, boxes, openingAt, origin, origin);
        return new Scan(interior, openings, leak, units, boxes, openingAt, new BlockPos(lo[0], lo[1], lo[2]), new BlockPos(hi[0], hi[1], hi[2]));
    }
}
