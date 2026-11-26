// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.bearing.BlockMoves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Salvage in the Gap: sparse asteroids (stone with meteoric ore) and derelict wrecks (an iron hull and a locker), each a Sable body at the
 * Gap's scale like everything else there, so they are the right size beside a craft and a person. The Gap is cut into cells of
 * {@link #CELL} blocks; the first time a player is near a cell it is rolled once (recorded in {@link Seen}, so a cell is never rolled twice,
 * not even after the thing in it is mined away) and may hold one asteroid or wreck. The blocks are laid at full size where the body will
 * float, assembled into a body ({@code SubLevelAssemblyHelper}, as the Lift does) and scaled by {@link SpaceTuning#GAP_SCALE}. Nothing near the
 * Gap's origin point, where crafts arrive.
 *
 * <p>Meteoric ore is the Gap's own set: iron, pentlandite (nickel), sperrylite and osmiridium (platinum-group traces) and, rarely, beryl, a
 * crystal that nothing on the ground makes in these quantities ("fallen from beyond the Gap").
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Salvage {

    /** Blocks (at the Gap's raw coordinates) a cell is wide. */
    private static final int CELL = 64;
    private static final int CHECK_TICKS = 100;
    /** Cells whose centre lies within this of the Gap's origin point are never rolled. */
    private static final double SAFE_BLOCKS = 160;
    private static final float THING_CHANCE = 0.2f, WRECK_SHARE = 0.15f;
    private static final ResourceKey<LootTable> WRECK_LOOT = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            Feedback.id("salvage/wreck"));

    private Salvage() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(SpaceTravel.GAP) || level.getGameTime() % CHECK_TICKS != 0)
            return;
        Seen seen = Seen.of(level);
        for (ServerPlayer p : level.players()) {
            int cx = Math.floorDiv(p.blockPosition().getX(), CELL), cy = Math.floorDiv(p.blockPosition().getY(), CELL), cz = Math.floorDiv(p.blockPosition().getZ(), CELL);
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = cx + dx, y = cy + dy, z = cz + dz;
                        if (y * CELL < level.getMinBuildHeight() + 16 || (y + 1) * CELL > level.getMaxBuildHeight() - 16)
                            continue;
                        if (seen.add(pack(x, y, z)))
                            roll(level, x, y, z);
                    }
        }
    }

    private static long pack(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    private static void roll(ServerLevel level, int cx, int cy, int cz) {
        RandomSource r = RandomSource.create(Mth.getSeed(cx, cy, cz) ^ level.getSeed());
        BlockPos at = new BlockPos(cx * CELL + 12 + r.nextInt(CELL - 24), cy * CELL + 12 + r.nextInt(CELL - 24), cz * CELL + 12 + r.nextInt(CELL - 24));
        double dx = at.getX() + 0.5, dy = at.getY() - SpaceTuning.ENTRY_Y, dz = at.getZ() + 0.5;
        if (dx * dx + dy * dy + dz * dz < SAFE_BLOCKS * SAFE_BLOCKS || r.nextFloat() >= THING_CHANCE)
            return;
        List<BlockPos> blocks = new ArrayList<>();
        if (r.nextFloat() < WRECK_SHARE)
            wreck(level, at, r, blocks);
        else
            asteroid(level, at, r, blocks);
        if (blocks.isEmpty())
            return;
        ServerSubLevel body;
        BlockMoves.enter();
        try {
            body = SubLevelAssemblyHelper.assembleBlocks(level, at, blocks, BoundingBox3i.from(blocks));
        } finally {
            BlockMoves.leave();
        }
        if (body != null)
            SpaceTravel.scale(body, SpaceTuning.GAP_SCALE);
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state, List<BlockPos> blocks) {
        level.getChunkAt(pos);
        level.setBlock(pos, state, 2);
        blocks.add(pos.immutable());
    }

    /** A rough ellipsoid of rock with ore through it. */
    private static void asteroid(ServerLevel level, BlockPos centre, RandomSource r, List<BlockPos> blocks) {
        float rx = 3 + r.nextFloat() * 3, ry = 3 + r.nextFloat() * 3, rz = 3 + r.nextFloat() * 3;
        int reach = 7;
        for (int x = -reach; x <= reach; x++)
            for (int y = -reach; y <= reach; y++)
                for (int z = -reach; z <= reach; z++) {
                    float d = (x * x) / (rx * rx) + (y * y) / (ry * ry) + (z * z) / (rz * rz);
                    if (d > 1f + (r.nextFloat() - 0.5f) * 0.5f)
                        continue;
                    put(level, centre.offset(x, y, z), rock(r), blocks);
                }
    }

    /** An ore block by id (the ore blocks are generated from material files, not named constants). */
    private static BlockState ore(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(Feedback.id(id)).defaultBlockState();
    }

    /** The rock and, by chance, an ore in it. */
    private static BlockState rock(RandomSource r) {
        float roll = r.nextFloat();
        if (roll < 0.004f)
            return ore("beryl_ore");
        if (roll < 0.012f)
            return ore("osmiridium_ore");
        if (roll < 0.025f)
            return ore("sperrylite_ore");
        if (roll < 0.07f)
            return ore("pentlandite_ore");
        if (roll < 0.16f)
            return ore("iron_ore");
        return Blocks.TUFF.defaultBlockState();
    }

    /** A hull: an iron shell with a few plates missing, and a locker of salvage inside. */
    private static void wreck(ServerLevel level, BlockPos origin, RandomSource r, List<BlockPos> blocks) {
        int lx = 7, ly = 4, lz = 4;
        for (int x = 0; x < lx; x++)
            for (int y = 0; y < ly; y++)
                for (int z = 0; z < lz; z++) {
                    boolean shell = x == 0 || y == 0 || z == 0 || x == lx - 1 || y == ly - 1 || z == lz - 1;
                    BlockPos p = origin.offset(x, y, z);
                    if (shell && r.nextFloat() > 0.2f)
                        put(level, p, Blocks.IRON_BLOCK.defaultBlockState(), blocks);
                }
        BlockPos locker = origin.offset(lx / 2, 1, lz / 2);
        put(level, locker, Blocks.CHEST.defaultBlockState(), blocks);
        if (level.getBlockEntity(locker) instanceof RandomizableContainerBlockEntity chest)
            chest.setLootTable(WRECK_LOOT, r.nextLong());
    }

    /** The cells already rolled, saved with the Gap. */
    static final class Seen extends SavedData {
        private static final SavedData.Factory<Seen> FACTORY = new SavedData.Factory<>(Seen::new, Seen::load, null);
        private final Set<Long> rolled = new HashSet<>();

        static Seen of(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(FACTORY, "feedback_salvage");
        }

        boolean add(long key) {
            if (!rolled.add(key))
                return false;
            setDirty();
            return true;
        }

        static Seen load(CompoundTag tag, HolderLookup.Provider registries) {
            Seen out = new Seen();
            for (long k : tag.getLongArray("Rolled"))
                out.rolled.add(k);
            return out;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.put("Rolled", new LongArrayTag(rolled.stream().mapToLong(Long::longValue).toArray()));
            return tag;
        }
    }
}
