// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.firebox;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.item.HandTool;
import io.github.soundgoodizerfan.feedback.item.TongsItem;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A hole with a fire in it. Put fuel in, set a vessel on top.
 * <p>
 * Right click with fuel to feed it, empty-handed to take the unburnt fuel back. A piece of material (a bar to reheat) lies in the flame
 * and warms toward it; empty-handed takes it out. Tongs lay a held piece in (sneak), take one out into the jaws, or reheat it in place (hold use).
 */
public class FireboxBlock extends Block implements EntityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FireboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Tongs work the fire through TongsItem (lay a piece in, take one out, reheat in the jaws), so the click passes on to the item.
        if (stack.isEmpty() || player.isShiftKeyDown() || stack.getItem() instanceof TongsItem)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // Server only: the client inserting too left ghost contents until the next sync.
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        if (!firebox.insertFuel(stack)) {
            if (!layable(stack) || !firebox.getPiece().isEmpty())
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            firebox.putPiece(stack.split(1));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Anything made of a material that is not a block to set down, a tool or tongs may lie in the flame. */
    private static boolean layable(ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem) && !(stack.getItem() instanceof HandTool)
                && !(stack.getItem() instanceof TongsItem) && MaterialIngredient.locate(stack).isPresent();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox))
            return InteractionResult.PASS;
        // Tongs in either hand work the fire themselves, so a bare click does not take the fuel out from under them.
        if (player.getMainHandItem().getItem() instanceof TongsItem || player.getOffhandItem().getItem() instanceof TongsItem)
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack taken = firebox.getPiece().isEmpty() ? firebox.removeFuel() : firebox.takePiece();
            if (!taken.isEmpty() && !player.getInventory().add(taken))
                player.drop(taken, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Smoke and flame while lit.
     * <p>
     * Free under philosophy 10 -- it is an adjective, not a figure. It says "this is alight" and
     * nothing about how hot, which is precisely the distinction the thermometer is sold on.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;
        if (random.nextDouble() < 0.1)
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE, net.minecraft.sounds.SoundSource.BLOCKS,
                    1f, 1f, false);
        level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + 0.5 + random.nextDouble() * 0.4 - 0.2, pos.getY() + 1.0,
                pos.getZ() + 0.5 + random.nextDouble() * 0.4 - 0.2, 0, 0.02, 0);
        if (random.nextInt(3) == 0)
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5 + random.nextDouble() * 0.3 - 0.15, pos.getY() + 0.9,
                    pos.getZ() + 0.5 + random.nextDouble() * 0.3 - 0.15, 0, 0.01, 0);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox) {
            ItemStack held = firebox.getFuel();
            if (!held.isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), held);
            if (!firebox.getPiece().isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), firebox.getPiece());
            if (!firebox.getOxidiser().isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), firebox.getOxidiser());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FireboxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((FireboxBlockEntity) be).tickServer();
    }
}
