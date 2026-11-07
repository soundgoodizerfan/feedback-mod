// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The anchor a chassis is welded around. The vertices, edges and plates live in its
 * {@link FrameBlockEntity}; the block itself is only where they are measured from. It becomes a
 * vehicle with the blocks around it when a {@link io.github.soundgoodizerfan.feedback.vehicle.LiftBlock}
 * under them is used.
 */
public class FrameBlock extends BaseEntityBlock {

    public static final MapCodec<FrameBlock> CODEC = simpleCodec(FrameBlock::new);

    public FrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof FrameBlockEntity be ? be.shape() : Shapes.block();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, io.github.soundgoodizerfan.feedback.registry.FVehicles.FRAME_ENTITY.get(), FrameBlockEntity::serverTick);
    }

    /**
     * Breaking the frame takes its cells and swinging parts with it. Hooked on a player's break and
     * on an explosion, not on every removal: Sable moves this block when a vehicle is lifted or
     * lowered, and that must not tear the vehicle apart.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && level.getBlockEntity(pos) instanceof FrameBlockEntity be)
            be.breakCells(server);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && level.getBlockEntity(pos) instanceof FrameBlockEntity be)
            be.breakCells(server);
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FrameBlockEntity(pos, state);
    }
}
