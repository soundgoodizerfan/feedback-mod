// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A crashed craft on the ground: a shell of iron with plates missing (the acid has had it a while) half sunk into the surface, and a locker
 * of salvage inside, the same locker the Gap's wrecks hold ({@code salvage/wreck}). The hull is the Gap wreck's shape ({@code space/Salvage}),
 * laid on the ground instead of floating. Placed by {@code placed_feature/acid_wreck}, which makes it rare.
 */
public class WreckFeature extends Feature<NoneFeatureConfiguration> {

    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Feedback.id("salvage/wreck"));
    private static final int LX = 7, LY = 4, LZ = 4;

    public WreckFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource r = context.random();
        BlockPos origin = context.origin().below();
        if (!level.getBlockState(origin).isFaceSturdy(level, origin, net.minecraft.core.Direction.UP))
            return false;
        for (int x = 0; x < LX; x++)
            for (int y = 0; y < LY; y++)
                for (int z = 0; z < LZ; z++) {
                    boolean shell = x == 0 || y == 0 || z == 0 || x == LX - 1 || y == LY - 1 || z == LZ - 1;
                    BlockPos p = origin.offset(x, y, z);
                    if (shell && r.nextFloat() > 0.35f)
                        level.setBlock(p, Blocks.IRON_BLOCK.defaultBlockState(), 2);
                    else
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
        BlockPos locker = origin.offset(LX / 2, 1, LZ / 2);
        level.setBlock(locker, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(locker) instanceof RandomizableContainerBlockEntity chest)
            chest.setLootTable(LOOT, r.nextLong());
        return true;
    }
}
