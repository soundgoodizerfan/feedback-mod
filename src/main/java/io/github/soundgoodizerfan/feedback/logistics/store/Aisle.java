// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FStore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A dock's read of its aisle: the shuttle head behind it, the rail running on from the head, and the rack cells standing
 * against either side of the rail, stacked up. Cells in unloaded chunks are left out until they load.
 *
 * @param head   the shuttle head, or null for a dock with none
 * @param along  the way the rail runs from the dock
 * @param length rail cells
 * @param racks  rack cells, nearest the dock first
 * @param partial whether some of it was in unloaded chunks, so the read is worth taking again
 */
public record Aisle(BlockPos head, Direction along, int length, List<Cell> racks, boolean partial) {

    /** A rack cell: its position, the rail cell it stands against (1 = next to the head) and its height above the rail. */
    public record Cell(BlockPos pos, int rail, int height, int side) {
        /** Cells the carriage runs, one way, to reach it. */
        public float distance() {
            return rail + 1 + height * StoreTuning.HEIGHT_COST;
        }
    }

    public static Aisle read(Level level, BlockPos dock, Direction along) {
        BlockPos headPos = dock.relative(along);
        if (!level.isLoaded(headPos))
            return new Aisle(null, along, 0, List.of(), true);
        BlockState head = level.getBlockState(headPos);
        if (!head.is(FStore.HEAD.get()) || head.getValue(ShuttleHeadBlock.FACING) != along)
            return new Aisle(null, along, 0, List.of(), false);
        List<Cell> racks = new ArrayList<>();
        boolean partial = false;
        int length = 0;
        Direction right = along.getClockWise();
        for (int i = 1; i <= StoreTuning.AISLE_MAX_LENGTH; i++) {
            BlockPos rail = headPos.relative(along, i);
            if (!level.isLoaded(rail)) {
                partial = true;
                break;
            }
            BlockState s = level.getBlockState(rail);
            if (!s.is(FStore.RAIL.get()) || s.getValue(RailBlock.AXIS) != along.getAxis())
                break;
            length = i;
            for (int side = 0; side < 2; side++) {
                BlockPos beside = rail.relative(side == 0 ? right : right.getOpposite());
                if (!level.isLoaded(beside)) {
                    partial = true;
                    continue;
                }
                for (int h = 0; h < StoreTuning.RACK_MAX_HEIGHT; h++) {
                    BlockPos at = beside.above(h);
                    if (!level.getBlockState(at).is(FStore.RACK.get()))
                        break;
                    racks.add(new Cell(at.immutable(), i, h, side));
                }
            }
        }
        racks.sort(Comparator.comparingDouble(Cell::distance));
        return new Aisle(headPos.immutable(), along, length, racks, partial);
    }
}
