// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.mixer;

import com.mojang.serialization.MapCodec;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Mixer: a drum on a vertical shaft. Right click with a powder to pour it in, empty-handed to
 * open the drum's screen (slots, tank, readings, draw). See {@link MixerBlockEntity}.
 */
public class MixerBlock extends BaseEntityBlock implements Rotatable {

    public static final MapCodec<MixerBlock> CODEC = simpleCodec(MixerBlock::new);

    public MixerBlock(Properties properties) {
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MixerBlockEntity mixer) || stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // A water bucket (or any container of it) fills the drum's wash water.
        if (net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent()
                && net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        if (!level.isClientSide) {
            boolean poured = MixerBlockEntity.pourable(stack, io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials())
                    && mixer.pour(stack);
            if (!poured)
                player.displayClientMessage(Component.translatable("message.feedback.mixer.refused"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MixerBlockEntity mixer))
            return InteractionResult.PASS;
        if (!level.isClientSide)
            player.openMenu(mixer, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == Direction.Axis.Y;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((MixerBlockEntity) be).tickServer();
    }
}
