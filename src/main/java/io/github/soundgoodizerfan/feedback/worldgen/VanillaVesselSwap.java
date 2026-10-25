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
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;

import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselBlock;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Turns every vanilla furnace, smoker, blast furnace, enchanting table, campfire and soul campfire into Feedback's own block, since the
 * vanilla blocks are uncraftable here. No structure or pool is touched, so a village, another
 * mod's structure, an old world and a block placed by command all end the same way: a chunk that
 * loads (or generates) with one in it, or a block placed, queues the position, and the next server
 * tick swaps it. Facing carries over; the old block's contents drop as items when it is replaced.
 */
public final class VanillaVesselSwap {

    private static final Map<Block, Block> SWAP = Map.of(
            Blocks.FURNACE, FBlocks.FURNACE.get(),
            Blocks.SMOKER, FBlocks.SMOKER.get(),
            Blocks.BLAST_FURNACE, FBlocks.BLAST_FURNACE.get(),
            Blocks.ENCHANTING_TABLE, FModular.ENCHANTING_TABLE.get(),
            Blocks.CAMPFIRE, FBlocks.CAMPFIRE.get(),
            Blocks.SOUL_CAMPFIRE, FBlocks.SOUL_CAMPFIRE.get());

    private record Pending(ServerLevel level, BlockPos pos) {}

    private static final Queue<Pending> QUEUE = new ArrayDeque<>();

    private VanillaVesselSwap() {}

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || !(chunk.getLevel() instanceof ServerLevel level))
            return;
        for (BlockPos pos : new ArrayList<>(chunk.getBlockEntities().keySet())) {
            if (SWAP.containsKey(chunk.getBlockState(pos).getBlock()))
                QUEUE.add(new Pending(level, pos.immutable()));
        }
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && SWAP.containsKey(event.getPlacedBlock().getBlock()))
            QUEUE.add(new Pending(level, event.getPos().immutable()));
    }

    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState copy(
            net.minecraft.world.level.block.state.BlockState to, net.minecraft.world.level.block.state.BlockState from,
            net.minecraft.world.level.block.state.properties.Property<T> prop) {
        return to.hasProperty(prop) ? to.setValue(prop, from.getValue(prop)) : to;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        for (Pending p; (p = QUEUE.poll()) != null;) {
            var state = p.level().getBlockState(p.pos());
            Block ours = SWAP.get(state.getBlock());
            if (ours == null)
                continue;
            var swapped = ours.defaultBlockState();
            if (swapped.hasProperty(ThermalVesselBlock.FACING) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                swapped = swapped.setValue(ThermalVesselBlock.FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if (swapped.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock)
                for (var prop : state.getProperties())
                    swapped = copy(swapped, state, prop);
            p.level().setBlock(p.pos(), swapped, Block.UPDATE_ALL);
        }
    }
}
