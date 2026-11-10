// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.gantry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * A frame on a wire that charges the suit its wearer is standing in. The wearer walks into it: only the floor plate is solid, so the
 * posts and the beam above are a picture, not a wall. See {@link GantryBlockEntity}.
 */
public class GantryBlock extends Block implements EntityBlock {

    private static final VoxelShape PLATE = Block.box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape FRAME = Shapes.or(PLATE, Block.box(0, 2, 0, 2, 16, 2), Block.box(14, 2, 0, 16, 16, 2),
            Block.box(0, 2, 14, 2, 16, 16), Block.box(14, 2, 14, 16, 16, 16), Block.box(0, 14, 0, 16, 16, 2), Block.box(0, 14, 14, 16, 16, 16));

    public GantryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FRAME;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PLATE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GantryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((GantryBlockEntity) be).tickServer();
    }
}
