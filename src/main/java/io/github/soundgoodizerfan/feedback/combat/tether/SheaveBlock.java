// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.tether;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A pulley wheel set in the air. It has no collision, so a line is never blocked by it; {@link RopeWrap} turns a line that runs through
 * it into a bend at its centre, and a bend there does not saw the line the way a block edge does. It is the one fixed point the player can
 * put where the world has none.
 */
public class SheaveBlock extends Block {

    public static final MapCodec<SheaveBlock> CODEC = simpleCodec(SheaveBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);

    public SheaveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
