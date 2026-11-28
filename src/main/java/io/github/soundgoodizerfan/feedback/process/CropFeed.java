// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.Feedback;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongIterator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Fertilised crops: a dose feeds one crop block and the feeding lasts until that crop is harvested. A fed crop gets extra
 * random ticks, the same call the world makes when it picks the block at random, so light, moisture and {@link CropClimate}
 * still decide whether each one grows it; and a fed crop broken ripe drops more than it would. A second dose on a crop
 * that is still fed is an overdose and scorches it ({@link io.github.soundgoodizerfan.feedback.item.FertiliserItem}).
 * <p>
 * The fed positions are kept per level in saved data, one long per block; a position whose block is no longer a crop is
 * dropped on the next pass. Cost: one pass every {@link ProcessTuning#FEED_EVERY} ticks over only the fed, loaded crops.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class CropFeed extends SavedData {

    private static final String NAME = "feedback_crop_feed", FED = "Fed";

    private final LongOpenHashSet fed = new LongOpenHashSet();

    private static CropFeed of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(CropFeed::new, CropFeed::load), NAME);
    }

    private static CropFeed load(CompoundTag tag, HolderLookup.Provider registries) {
        CropFeed out = new CropFeed();
        for (long p : tag.getLongArray(FED))
            out.fed.add(p);
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray(FED, fed.toLongArray());
        return tag;
    }

    /** Whether the crop at {@code pos} has been fed and not yet harvested. */
    public static boolean isFed(ServerLevel level, BlockPos pos) {
        return of(level).fed.contains(pos.asLong());
    }

    /** Feeds the crop at {@code pos}. */
    public static void feed(ServerLevel level, BlockPos pos) {
        CropFeed store = of(level);
        if (store.fed.add(pos.asLong()))
            store.setDirty();
    }

    /** Ends the feeding of the crop at {@code pos}. */
    public static void forget(ServerLevel level, BlockPos pos) {
        CropFeed store = of(level);
        if (store.fed.remove(pos.asLong()))
            store.setDirty();
    }

    /** Extra random ticks a fed crop is owed each pass: the world's own rate, times the speed-up beyond 1, over one pass. */
    private static float owed(ServerLevel level) {
        return (ProcessTuning.FEED_SPEEDUP - 1f) * level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) / 4096f * ProcessTuning.FEED_EVERY;
    }

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % ProcessTuning.FEED_EVERY != 0)
            return;
        CropFeed store = of(level);
        if (store.fed.isEmpty())
            return;
        float owed = owed(level);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;
        for (LongIterator it = store.fed.iterator(); it.hasNext();) {
            long p = it.nextLong();
            pos.set(p);
            if (!level.shouldTickBlocksAt(pos))
                continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop)) {
                it.remove();
                changed = true;
                continue;
            }
            if (crop.isMaxAge(state))
                continue;
            int n = (int) owed + (level.random.nextFloat() < owed - (int) owed ? 1 : 0);
            for (int i = 0; i < n && state.getBlock() instanceof CropBlock c && !c.isMaxAge(state); i++) {
                state.randomTick(level, pos.immutable(), level.random);
                state = level.getBlockState(pos);
            }
            if (level.random.nextInt(ProcessTuning.FEED_SPARKLE_ONE_IN) == 0)
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 1, 0.25, 0.2, 0.25, 0);
        }
        if (changed)
            store.setDirty();
    }

    /** A fed crop broken: if it was ripe, each drop is topped up by {@link ProcessTuning#FEED_YIELD} of itself; the feeding ends either way. */
    @SubscribeEvent
    public static void onBreak(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!isFed(level, pos))
            return;
        forget(level, pos);
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state))
            return;
        for (ItemEntity drop : java.util.List.copyOf(event.getDrops())) {
            float extra = drop.getItem().getCount() * ProcessTuning.FEED_YIELD;
            int n = (int) extra + (level.random.nextFloat() < extra - (int) extra ? 1 : 0);
            if (n <= 0)
                continue;
            ItemStack more = drop.getItem().copyWithCount(n);
            event.getDrops().add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), more));
        }
    }
}
