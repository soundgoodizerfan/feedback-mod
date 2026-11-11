// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.engineer;

import io.github.soundgoodizerfan.feedback.item.DataStickItem;
import io.github.soundgoodizerfan.feedback.modular.engineering.Fasteners;
import io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Auto Engineer. Right click with a recipe data stick to seat it (the one it replaces comes back), with a hand tool to seat it in a tool
 * slot, with a fastener to add it to the stock, with anything else to feed the parts bay by hand as a hopper would. Empty-handed takes the
 * output, else the stick; sneaking empty-handed takes back the last tool, then the last fastener stack, then the last parts stack.
 */
public class AutoEngineerBlock extends Block implements EntityBlock {

    public AutoEngineerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AutoEngineerBlockEntity engineer))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            if (stack.getItem() instanceof DataStickItem)
                give(player, engineer.seatStick(stack));
            else if (ToolAbilities.isTool(stack)) {
                if (!engineer.seatTool(stack))
                    stackInto(engineer, stack, player);
            } else if (Fasteners.is(stack)) {
                for (int i = 0; i < AutoEngineerBlockEntity.FASTENERS && !stack.isEmpty(); i++) {
                    ItemStack left = engineer.insertFastener(i, stack.copy(), false);
                    if (!player.hasInfiniteMaterials())
                        stack.setCount(left.getCount());
                    else
                        break;
                }
            } else if (!stack.isEmpty())
                stackInto(engineer, stack, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void stackInto(AutoEngineerBlockEntity engineer, ItemStack stack, Player player) {
        ItemStack left = engineer.insertPart(stack.copy());
        if (!player.hasInfiniteMaterials())
            stack.setCount(left.getCount());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AutoEngineerBlockEntity engineer))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack out = player.isShiftKeyDown() ? engineer.takeLoose() : engineer.takeOutput(64, false);
            give(player, !out.isEmpty() ? out : player.isShiftKeyDown() ? ItemStack.EMPTY : engineer.removeStick());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void give(Player player, ItemStack stack) {
        if (stack.isEmpty())
            return;
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AutoEngineerBlockEntity engineer) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), engineer.getStick());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), engineer.getOutput());
            for (var list : java.util.List.of(engineer.getBay(), engineer.getTools(), engineer.getFasteners()))
                for (ItemStack s : list)
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoEngineerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FBlockEntities.AUTO_ENGINEER.get() ? null : (l, p, s, be) -> ((AutoEngineerBlockEntity) be).tickServer();
    }
}
