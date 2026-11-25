// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.hammer;

import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The machine that does not know when to stop.
 * <p>
 * Right click with something to put it in, empty-handed to take it back out. Taking it out in
 * time is the entire game here. A machine head right-clicked on seats as the head (the one it
 * replaces comes back to the hand); sneaking with an empty hand pulls the head.
 */
public class MechanicalHammerBlock extends Block implements EntityBlock {

    public MechanicalHammerBlock(Properties properties) {
        super(properties);
    }

    /**
     * Mirrors models/block/mechanical_hammer.json and the head at rest in mechanical_hammer_head.json (scripts/hammer_model.py): the
     * cobblestone anvil slab, two posts, the top beam, and the head with its rod. The dropping head stays inside this outline.
     */
    private static final int[][] BOXES = {{0, 0, 0, 16, 3, 16}, {1, 3, 6, 3, 16, 10}, {13, 3, 6, 15, 16, 10}, {3, 13, 5, 13, 16, 11},
            {3, 6, 4, 13, 12, 12}, {7, 12, 7, 9, 16, 9}};
    private static final VoxelShape SHAPE = shape();

    private static VoxelShape shape() {
        VoxelShape s = Shapes.empty();
        for (int[] b : BOXES)
            s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        return s;
    }

    /** A pattern's last note is due: hand over the part ({@link MechanicalHammerBlockEntity#queue}). */
    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof MechanicalHammerBlockEntity hammer)
            hammer.finish(level);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** A block event for the stroke animation: Block does not hand these to its block entity the way BaseEntityBlock does. */
    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        return level.getBlockEntity(pos) instanceof MechanicalHammerBlockEntity hammer && hammer.triggerEvent(id, param);
    }

    /**
     * Vanilla calls this even when the hand is empty, and only falls through to
     * {@link #useWithoutItem} when told to with PASS_TO_DEFAULT_BLOCK_INTERACTION. Returning
     * success for an empty hand therefore swallows the interaction -- which is exactly how
     * extraction came to be unreachable without breaking the block.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof MechanicalHammerBlockEntity hammer))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (stack.getItem() instanceof io.github.soundgoodizerfan.feedback.item.HammerCamItem) {
            if (!level.isClientSide)
                give(player, hammer.seatCam(stack));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (io.github.soundgoodizerfan.feedback.modular.type.ModularFaces.has(stack, "head_blank")) {
            if (!level.isClientSide)
                give(player, hammer.seatHead(stack));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // An occupied hammer gives its workpiece back whatever you are holding. Requiring an
        // empty hand reads as "there is no way to get it out" the first time you try it with a
        // stack of ingots in hand, which is the only thing you would plausibly be holding.
        if (!level.isClientSide) {
            if (hammer.getWorkpiece().isEmpty()) {
                hammer.insert(stack);
                if (hammer.getHead().isEmpty() && player instanceof ServerPlayer sp)
                    HudReadoutPayload.send(sp, Component.translatable("message.feedback.machine.no_head"), 60);
            } else
                give(player, hammer.removeWorkpiece());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MechanicalHammerBlockEntity hammer))
            return InteractionResult.PASS;
        if (!level.isClientSide)
            give(player, player.isShiftKeyDown() ? hammer.removeHead() : !hammer.getWorkpiece().isEmpty() ? hammer.removeWorkpiece() : hammer.removeCam());
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
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MechanicalHammerBlockEntity hammer) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), hammer.getWorkpiece());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), hammer.getHead().get());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), hammer.getCam());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MechanicalHammerBlockEntity(pos, state);
    }
}
