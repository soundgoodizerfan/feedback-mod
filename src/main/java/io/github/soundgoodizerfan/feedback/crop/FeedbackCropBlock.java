// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.crop;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Every Feedback crop is this one class (modular foods spec 2.3): four stages, a seed, and nothing else that differs.
 * What it drops is its loot table, what it wants of the weather is its block tag's {@code crop_climate} line, and what
 * the plant is called is a lang key, so a crop is an id plus data. The crop's id lives in {@code registry/FCrops}.
 */
public class FeedbackCropBlock extends CropBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    private final Supplier<? extends Item> seed;

    public FeedbackCropBlock(Properties properties, Supplier<? extends Item> seed) {
        super(properties);
        this.seed = seed;
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }
}
