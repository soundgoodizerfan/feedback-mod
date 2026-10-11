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
package io.github.soundgoodizerfan.feedback.machine.crucible;

import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Two sizes, one class. The difference is thermal mass and nothing else -- see
 * {@link CrucibleBlockEntity}, which reads its own size off the block rather than storing it.
 * <p>
 * Right click to put something in, empty-handed to take the last thing back out. What comes out
 * is at the vessel's temperature, and starts cooling the moment it does.
 */
public class CrucibleBlock extends Block implements EntityBlock {

    private final ThermalMass thermalMass;

    public CrucibleBlock(Properties properties, ThermalMass thermalMass) {
        super(properties);
        this.thermalMass = thermalMass;
    }

    /** The one thing that differs between sizes -- a property of the block, not its id. */
    public ThermalMass getThermalMass() {
        return thermalMass;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // Server only: the client inserting too left ghost contents until the next sync.
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        if (!crucible.insert(stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack taken = crucible.removeFirst();
            if (!taken.isEmpty() && !player.getInventory().add(taken))
                player.drop(taken, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            // removeItem stamps each piece at the vessel's temperature on the way out.
            for (int slot = 0; slot < crucible.getContents().size(); slot++) {
                ItemStack held = crucible.removeItem(slot);
                if (!held.isEmpty())
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), held);
            }
            Fittable.dropAll(crucible, level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((CrucibleBlockEntity) be).tickServer();
    }
}
