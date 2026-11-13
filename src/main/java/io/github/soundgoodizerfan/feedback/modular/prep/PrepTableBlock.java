// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.prep;

import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchBlock;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Where dishes are made: the Tool Table with the dish filter and a knife slot where the hammer is
 *. See {@link PrepTableMenu}.
 */
public class PrepTableBlock extends PartBenchBlock {

    public static final MapCodec<PrepTableBlock> CODEC = simpleCodec(PrepTableBlock::new);

    public PrepTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrepTableBlockEntity(pos, state);
    }
}
