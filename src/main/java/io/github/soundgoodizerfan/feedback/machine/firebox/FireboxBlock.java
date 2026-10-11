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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.machine.firebox;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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
 * Right click with fuel to feed it, empty-handed to take the unburnt fuel back.
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
        if (stack.isEmpty() || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // Server only: the client inserting too left ghost contents until the next sync.
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        if (!firebox.insertFuel(stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FireboxBlockEntity firebox))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack taken = firebox.removeFuel();
            if (!taken.isEmpty() && !player.getInventory().add(taken))
                player.drop(taken, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Smoke and flame while lit.
     * <p>
     * Free under philosophy 8 -- it is an adjective, not a figure. It says "this is alight" and
     * nothing about how hot, which is precisely the distinction the thermometer is sold on.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;
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
