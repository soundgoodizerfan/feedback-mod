// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.registry.FVehicles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * One cell of an airship's envelope: it holds a lift gas, and adjacent cells share it. The skin
 * ({@link Skin}) is what it is wrapped in. A pipe or bucket fills it; a patch item seals its holes.
 */
public class GasCellBlock extends Block implements EntityBlock {

    /** Items that seal a hole in a skin (cloth, rubber, plate): a tag, never an id. */
    public static final TagKey<Item> PATCHES = TagKey.create(Registries.ITEM,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("feedback", "gas_patch"));

    private final Skin skin;

    public GasCellBlock(Properties properties, Skin skin) {
        super(properties);
        this.skin = skin;
    }

    public Skin skin() {
        return skin;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(PATCHES) && level.getBlockEntity(pos) instanceof GasCellBlockEntity cell) {
            if (!level.isClientSide && cell.patch() && !player.getAbilities().instabuild)
                stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && level.getBlockEntity(hit.getBlockPos()) instanceof GasCellBlockEntity cell)
            cell.puncture();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        super.neighborChanged(state, level, pos, block, from, moved);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GasCellBlockEntity cell)
            cell.invalidate();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!level.isClientSide && !state.is(next.getBlock()))
            for (Direction d : Direction.values())
                if (level.getBlockEntity(pos.relative(d)) instanceof GasCellBlockEntity cell)
                    cell.invalidate();
        super.onRemove(state, level, pos, next, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasCellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((GasCellBlockEntity) be).tickServer();
    }
}
