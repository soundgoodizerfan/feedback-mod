// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.detector;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A redstone coherer. Empty-hand click is the tap. See {@link CohererBlockEntity}. */
public class CohererBlock extends Block implements EntityBlock {

    public static final BooleanProperty WELDED = BooleanProperty.create("welded");

    /** Mirrors models/block/coherer.json (scripts/coherer_model.py): base, low cap, tube, high cap, terminal, post, clapper. */
    private static final int[][] BOXES = {
            {2, 0, 2, 14, 2, 14}, {3, 2, 4, 11, 4, 12}, {4, 4, 5, 10, 12, 11}, {3, 12, 4, 11, 14, 12}, {6, 14, 7, 8, 16, 9},
            {12, 2, 6, 14, 10, 8}, {10, 8, 6, 12, 10, 8}};
    private static final VoxelShape SHAPE = shape();

    private static VoxelShape shape() {
        VoxelShape s = Shapes.empty();
        for (int[] b : BOXES) {
            s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return s;
    }

    public CohererBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WELDED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WELDED);
    }

    /** The clapper's tick ({@link CohererBlockEntity#clap}). */
    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof CohererBlockEntity coherer)
            coherer.clap();
    }

    /** A clapper in hand is seated; with the coherer already carrying one, nothing happens. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                                  Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.is(io.github.soundgoodizerfan.feedback.registry.FItems.COHERER_CLAPPER.get()) && level.getBlockEntity(pos) instanceof CohererBlockEntity coherer) {
            if (!level.isClientSide && !coherer.hasClapper()) {
                coherer.seatClapper(true);
                if (!player.hasInfiniteMaterials())
                    stack.shrink(1);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CohererBlockEntity coherer) {
            boolean welded = coherer.isEngaged();
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable(!welded ? "feedback.coherer.loose"
                    : coherer.tap() ? "feedback.coherer.tapped" : "feedback.coherer.stuck"));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CohererBlockEntity(pos, state);
    }
}
