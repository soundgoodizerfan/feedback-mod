// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.phosphor;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The phosphor screen. Set it on a fire; empty hand pinches the envelope off at its present heat
 * (hotter is a better vacuum). Glows only when sealed, evacuated and driven by an oscilloscope
 * beside it. See {@link PhosphorScreenBlockEntity}.
 */
public class PhosphorScreenBlock extends BaseEntityBlock {

    public static final MapCodec<PhosphorScreenBlock> CODEC = simpleCodec(PhosphorScreenBlock::new);
    /** Four discrete brightness levels, 0 dark to 3 full. */
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 3);

    public PhosphorScreenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GLOW, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(GLOW);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PhosphorScreenBlockEntity screen))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (screen.isSealed())
                player.displayClientMessage(Component.translatable("feedback.screen.sealed"), true);
            else {
                screen.seal();
                player.displayClientMessage(Component.translatable("feedback.screen.pinched"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhosphorScreenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((PhosphorScreenBlockEntity) be).tickServer();
    }
}
