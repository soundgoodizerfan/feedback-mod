// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.MapCodec;

import io.github.soundgoodizerfan.feedback.modular.ToolTableBlock;
import io.github.soundgoodizerfan.feedback.modular.ToolTableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Where guns are made: the Tool Table with a type filter.
 * It holds what the Tool Table holds, takes only guns, and has a slot for a round that the screen reads the
 * gun's spec sheet against (guns spec 13.6). See {@link GunTableMenu}.
 */
public class GunTableBlock extends ToolTableBlock {

    public static final MapCodec<GunTableBlock> CODEC = simpleCodec(GunTableBlock::new);

    public GunTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GunTableBlockEntity(pos, state);
    }
}
