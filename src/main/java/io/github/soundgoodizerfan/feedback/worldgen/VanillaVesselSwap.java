// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

import java.util.ArrayDeque;
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
 * Turns every vanilla furnace, smoker, blast furnace, enchanting table, campfire, soul campfire, smithing table, stonecutter, anvil (any damage) and brewing stand and redstone lamp into Feedback's own block, since the
 * vanilla blocks are uncraftable here. No structure or pool is touched, so a village, another
 * mod's structure, an old world and a block placed by command all end the same way: a chunk that
 * loads (or generates) with one in it, or a block placed, queues the position, and the next server
 * tick swaps it. A block put there by a command raises neither event and stays vanilla until its chunk reloads. Facing carries over; the old block's contents drop as items when it is replaced.
 */
public final class VanillaVesselSwap {

    private static final Map<Block, Block> SWAP = swapTable(Map.ofEntries(
            Map.entry(Blocks.FURNACE, FBlocks.FURNACE.get()),
            Map.entry(Blocks.SMOKER, FBlocks.SMOKER.get()),
            Map.entry(Blocks.BLAST_FURNACE, FBlocks.BLAST_FURNACE.get()),
            Map.entry(Blocks.ENCHANTING_TABLE, FModular.ENCHANTING_TABLE.get()),
            Map.entry(Blocks.CAMPFIRE, FBlocks.CAMPFIRE.get()),
            Map.entry(Blocks.SOUL_CAMPFIRE, FBlocks.SOUL_CAMPFIRE.get()),
            Map.entry(Blocks.SMITHING_TABLE, FModular.SMITHING_TABLE.get()),
            Map.entry(Blocks.STONECUTTER, FModular.STONECUTTER.get()),
            Map.entry(Blocks.BREWING_STAND, FBlocks.BREWING_STAND.get()),
            Map.entry(Blocks.ANVIL, FModular.ANVIL.get()),
            Map.entry(Blocks.CHIPPED_ANVIL, FModular.ANVIL.get()),
            Map.entry(Blocks.DAMAGED_ANVIL, FModular.ANVIL.get()),
            Map.entry(Blocks.REDSTONE_LAMP, FBlocks.LAMP.get()),
            Map.entry(Blocks.HOPPER, FBlocks.HOPPER.get()),
            Map.entry(Blocks.DROPPER, FBlocks.DROPPER.get()),
            Map.entry(Blocks.NOTE_BLOCK, FBlocks.NOTE_BLOCK.get()),
            Map.entry(Blocks.DISPENSER, FBlocks.DISPENSER.get()),
            Map.entry(Blocks.PISTON, FBlocks.MODULAR_PISTON.get()),
            Map.entry(Blocks.STICKY_PISTON, FBlocks.MODULAR_PISTON.get()),
            Map.entry(Blocks.OBSERVER, io.github.soundgoodizerfan.feedback.registry.FAutomation.SENSORS.get(
                    io.github.soundgoodizerfan.feedback.machine.sensor.SensorKind.OBSERVER).get()),
            Map.entry(Blocks.COMPARATOR, io.github.soundgoodizerfan.feedback.registry.FAutomation.SENSORS.get(
                    io.github.soundgoodizerfan.feedback.machine.sensor.SensorKind.FILL).get())));

    /** Every replaced block to the one that takes its place. Only once the registries are filled. */
    public static Map<Block, Block> swaps() {
        return SWAP;
    }

    /** The table plus every vanilla bed: each of the sixteen colours becomes the one placed bed, dyed by the colour it was. */
    private static Map<Block, Block> swapTable(Map<Block, Block> base) {
        var all = new java.util.HashMap<>(base);
        for (var bed : net.minecraft.core.registries.BuiltInRegistries.BLOCK)
            if (bed instanceof net.minecraft.world.level.block.BedBlock && !(bed instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularBedBlock))
                all.put(bed, FModular.PLACED_BED.get());
        return Map.copyOf(all);
    }

    /** A vanilla job site's block states live in its point-of-interest type; ours join it, so a villager still claims the swapped block (toolsmith, mason, armorer, butcher, cleric). */
    public static void onExtendPoi(net.neoforged.neoforge.common.world.poi.ExtendPoiTypesEvent event) {
        event.addBlockToPoi(net.minecraft.world.entity.ai.village.poi.PoiTypes.TOOLSMITH, FModular.SMITHING_TABLE.get());
        event.addBlockToPoi(net.minecraft.world.entity.ai.village.poi.PoiTypes.MASON, FModular.STONECUTTER.get());
        event.addBlockToPoi(net.minecraft.world.entity.ai.village.poi.PoiTypes.ARMORER, FBlocks.BLAST_FURNACE.get());
        event.addBlockToPoi(net.minecraft.world.entity.ai.village.poi.PoiTypes.BUTCHER, FBlocks.SMOKER.get());
        event.addBlockToPoi(net.minecraft.world.entity.ai.village.poi.PoiTypes.CLERIC, FBlocks.BREWING_STAND.get());
        // A villager's home is the head of a bed, as for vanilla's; the foot is not claimed.
        event.addStatesToPoi(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME, FModular.PLACED_BED.get().getStateDefinition().getPossibleStates().stream()
                .filter(st -> st.getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD)
                .collect(java.util.stream.Collectors.toSet()));
    }

    /** Our dropper and dispenser keep vanilla's block entities. */
    public static void onAddBlocks(net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent event) {
        event.modify(net.minecraft.world.level.block.entity.BlockEntityType.DROPPER, FBlocks.DROPPER.get());
        event.modify(net.minecraft.world.level.block.entity.BlockEntityType.DISPENSER, FBlocks.DISPENSER.get());
    }

    private record Pending(ServerLevel level, BlockPos pos) {}

    private static final Queue<Pending> QUEUE = new ArrayDeque<>();

    private VanillaVesselSwap() {}

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getChunk() instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level)
            scan(level, chunk);
    }

    /** The smithing table, stonecutter and brewing stand have no block entity we can find from outside, so look in the palettes; a section with none is skipped whole. */
    private static void scan(ServerLevel level, LevelChunk chunk) {
        var at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < chunk.getSectionsCount(); i++) {
            var section = chunk.getSection(i);
            if (section.hasOnlyAir() || !section.maybeHas(state -> SWAP.containsKey(state.getBlock())))
                continue;
            int y0 = net.minecraft.core.SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++)
                    for (int z = 0; z < 16; z++)
                        if (SWAP.containsKey(section.getBlockState(x, y, z).getBlock()))
                            QUEUE.add(new Pending(level, at.set(chunk.getPos().getMinBlockX() + x, y0 + y, chunk.getPos().getMinBlockZ() + z).immutable()));
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

    /** Time one tick may spend swapping; the rest of the queue waits for the next tick. */
    private static final long BUDGET_NANOS = 1_500_000L;

    /**
     * A swap writes a block, and the physics mod reads the blocks around it on every write; a neighbour in a chunk that is not loaded
     * makes the server load or generate it on the spot and stall the tick. So a position waits until every chunk within two blocks of it is loaded.
     */
    private static boolean surroundingsLoaded(ServerLevel level, BlockPos pos) {
        for (int dx = -2; dx <= 2; dx += 2)
            for (int dz = -2; dz <= 2; dz += 2)
                if (!level.hasChunk((pos.getX() + dx) >> 4, (pos.getZ() + dz) >> 4))
                    return false;
        return true;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        // Only the positions queued before this tick are looked at, so a deferred one cannot loop forever.
        for (int left = QUEUE.size(); left-- > 0 && System.nanoTime() < deadline;) {
            Pending p = QUEUE.poll();
            // Its own chunk is gone: it queues again when the chunk loads.
            if (!p.level().hasChunk(p.pos().getX() >> 4, p.pos().getZ() >> 4))
                continue;
            if (!surroundingsLoaded(p.level(), p.pos())) {
                QUEUE.add(p);
                continue;
            }
            var state = p.level().getBlockState(p.pos());
            Block ours = SWAP.get(state.getBlock());
            if (ours == null)
                continue;
            if (ours instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularBedBlock && state.getBlock() instanceof net.minecraft.world.level.block.BedBlock vanilla) {
                swapBed(p.level(), p.pos(), state, vanilla, ours);
                continue;
            }
            if (ours instanceof io.github.soundgoodizerfan.feedback.machine.note.FeedbackNoteBlock) {
                swapNote(p.level(), p.pos(), state);
                continue;
            }
            if (ours instanceof io.github.soundgoodizerfan.feedback.machine.piston.ModularPistonBlock) {
                swapPiston(p.level(), p.pos(), state);
                continue;
            }
            var swapped = ours.defaultBlockState();
            if (swapped.hasProperty(ThermalVesselBlock.FACING) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                swapped = swapped.setValue(ThermalVesselBlock.FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            // Every property the two blocks share carries over (a campfire's lit and waterlogged, an observer's or piston's facing).
            for (var prop : state.getProperties())
                swapped = copy(swapped, state, prop);
            // A comparator faces the player and reads the container behind it; the fill gauge faces the face it reads.
            if (state.is(Blocks.COMPARATOR))
                swapped = swapped.setValue(io.github.soundgoodizerfan.feedback.machine.sensor.SensorBlock.FACING,
                        state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
            p.level().setBlock(p.pos(), swapped, Block.UPDATE_ALL);
        }
    }

    /** A vanilla note block keeps its note and plays its instrument through one noisemaker; its figures are the fixed block's (no design). */
    private static void swapNote(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        level.setBlock(pos, FBlocks.NOTE_BLOCK.get().defaultBlockState()
                .setValue(io.github.soundgoodizerfan.feedback.machine.note.FeedbackNoteBlock.NOTE, state.getValue(BlockStateProperties.NOTE))
                .setValue(io.github.soundgoodizerfan.feedback.machine.note.FeedbackNoteBlock.POWERED, state.getValue(BlockStateProperties.POWERED)), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.machine.note.NoteBlockEntity note) {
            var instrument = state.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT);
            note.figures().put("_i0", instrument.isTunable() ? instrument.ordinal() : net.minecraft.world.level.block.state.properties.NoteBlockInstrument.HARP.ordinal());
        }
    }

    /**
     * A vanilla piston becomes the modular one retracted: an extended one loses its vanilla head (the blocks it pushed stay), and a sticky one
     * gets a sticky face. Its figures are the fixed block's, an iron cylinder with a leather seal and one block of reach.
     */
    private static void swapPiston(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        var facing = state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING);
        if (state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED)
                && level.getBlockState(pos.relative(facing)).getBlock() instanceof net.minecraft.world.level.block.piston.PistonHeadBlock)
            level.setBlock(pos.relative(facing), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos, FBlocks.MODULAR_PISTON.get().defaultBlockState()
                .setValue(io.github.soundgoodizerfan.feedback.machine.piston.ModularPistonBlock.FACING, facing), Block.UPDATE_ALL);
        if (state.is(Blocks.STICKY_PISTON) && level.getBlockEntity(pos) instanceof io.github.soundgoodizerfan.feedback.machine.piston.ModularPistonBlockEntity piston)
            piston.figures().put("Sticky", 1f);
    }

    /**
     * Swaps both halves of a vanilla bed at once into the placed modular bed, so neither half sees a foreign neighbour and breaks. The
     * bed's frame and mattress are drawn from {@link io.github.soundgoodizerfan.feedback.modular.part.BedPool} by the head's position (both halves
     * and a reload agree), its cover dyed the colour the vanilla bed was; facing, part and occupancy carry over.
     */
    private static void swapBed(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                                net.minecraft.world.level.block.BedBlock vanilla, Block ours) {
        var facing = state.getValue(net.minecraft.world.level.block.BedBlock.FACING);
        boolean head = state.getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD;
        BlockPos headPos = head ? pos : pos.relative(facing);
        var bed = io.github.soundgoodizerfan.feedback.modular.part.BedPool.draw(headPos.asLong() * 31L + level.dimension().location().hashCode(), vanilla.getColor());
        if (bed.isEmpty())
            return;
        for (BlockPos half : new BlockPos[] {headPos, headPos.relative(facing.getOpposite())}) {
            var old = level.getBlockState(half);
            if (!(old.getBlock() instanceof net.minecraft.world.level.block.BedBlock) || old.getValue(net.minecraft.world.level.block.BedBlock.FACING) != facing)
                continue;
            var swapped = ours.defaultBlockState();
            for (var prop : old.getProperties())
                swapped = copy(swapped, old, prop);
            // No neighbour or shape update: the other half is still vanilla for one more line.
            level.setBlock(half, swapped, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (level.getBlockEntity(half) instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularBedBlockEntity be)
                be.setBed(bed);
        }
    }
}
