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
