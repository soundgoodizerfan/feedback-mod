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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.MapCodec;

import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A placed explosive: a crate packed by hand with propellant, lit by anything hot enough
 * ({@link DemolitionChargeBlockEntity}). What it does is read off what is in it, never off a
 * recipe -- the same blend rule a cartridge's charge follows.
 */
public class DemolitionChargeBlock extends BaseEntityBlock {

    public static final MapCodec<DemolitionChargeBlock> CODEC = simpleCodec(DemolitionChargeBlock::new);

    public DemolitionChargeBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemolitionChargeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FWeapons.DEMOLITION_CHARGE_ENTITY.get() ? null
                : (l, pos, s, be) -> ((DemolitionChargeBlockEntity) be).tick();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DemolitionChargeBlockEntity charge))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        if (stack.is(FTags.IGNITERS)) {
            if (charge.light(player))
                return ItemInteractionResult.CONSUME;
            return ItemInteractionResult.FAIL;
        }
        Component said = charge.pack(stack, player);
        if (said == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        player.displayClientMessage(said, true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                                    BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DemolitionChargeBlockEntity charge) {
            if (!level.isClientSide)
                player.displayClientMessage(charge.describe(), true);
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    /** Broken before it goes up, it gives back what was packed in it. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild && level.getBlockEntity(pos) instanceof DemolitionChargeBlockEntity charge)
            charge.contents().forEach(stack -> net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Anything burning or molten beside it lights it, as a fuse is lit by any flame. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block,
                                   BlockPos from, boolean moved) {
        super.neighborChanged(state, level, pos, block, from, moved);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof DemolitionChargeBlockEntity charge))
            return;
        BlockState beside = level.getBlockState(from);
        if (beside.is(net.minecraft.tags.BlockTags.FIRE) || level.getFluidState(from).is(net.minecraft.tags.FluidTags.LAVA))
            charge.light(null);
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Explosion explosion) {
        return false;
    }

    /** A blast sets a charge off at once, as it does any packed explosive; it does not go off twice. */
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DemolitionChargeBlockEntity charge)
            charge.detonate();
        else
            super.onBlockExploded(state, level, pos, explosion);
    }
}
