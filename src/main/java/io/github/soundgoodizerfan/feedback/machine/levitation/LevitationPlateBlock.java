// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.levitation;

import io.github.soundgoodizerfan.feedback.machine.electromagnet.MagnetField;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A slab of a diamagnetic material. Over a powered electromagnet it is slick: its surface friction rises from a block's
 * ordinary 0.6 to ice's as the field below it takes hold, scaled by the material's diamagnetism, so dropped items and
 * walkers glide. Inside a vehicle body (a Sable body from the Lift) it is also lifted ({@link LevitationPlateBlockEntity}).
 * One block serves every material: it rides on the item as {@code MadeOf}.
 */
public class LevitationPlateBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    /** How far a powered magnet below holds this plate off, in thirds (0 resting, 3 fully). A block state, so the client knows it as the server does. */
    public static final IntegerProperty LIFT = IntegerProperty.create("lift", 0, 3);

    public LevitationPlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIFT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIFT);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide)
            refresh(level, pos);
    }

    /** Writes the lift state for the field below this plate now: written only when it changes. */
    public static void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LevitationPlateBlock) || !(level.getBlockEntity(pos) instanceof LevitationPlateBlockEntity plate))
            return;
        int lift = Math.round(3f * Math.min(1f, plate.diamagnetism()
                * MagnetField.below(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
        if (state.getValue(LIFT) != lift)
            level.setBlock(pos, state.setValue(LIFT, lift), 2);
    }

    /** Refreshes every plate a magnet at {@code magnet} can reach (the 3 by 3 columns above it, up to its reach). */
    public static void refreshAbove(Level level, BlockPos magnet) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                for (int dy = 1; dy <= (int) MagnetField.REACH; dy++)
                    refresh(level, p.set(magnet.getX() + dx, magnet.getY() + dy, magnet.getZ() + dz));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Read only when something touches the surface: no tick. */
    @Override
    public float getFriction(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        float resting = LevitationTuning.SLAB_FRICTION_RESTING;
        return resting + (LevitationTuning.SLAB_FRICTION_LIFTED - resting) * (state.getValue(LIFT) / 3f);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LevitationPlateBlockEntity(pos, state);
    }
}
