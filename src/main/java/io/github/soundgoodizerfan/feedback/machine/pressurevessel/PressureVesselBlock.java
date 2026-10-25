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
package io.github.soundgoodizerfan.feedback.machine.pressurevessel;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A plain sealed block -- see {@link PressureVesselBlockEntity} for the whole mechanism. Right
 * click reads its own gauge, the same standing as a {@code ThermalVesselBlockEntity} showing its
 * own temperature on its own screen (§8's "equipment reading its own state is not the same
 * purchase as an instrument scanning someone else's"): no tool required, because this is the
 * machine's own dial, not a sensor pointed at it. A jarred food in hand goes in (up to four); an empty
 * hand while sneaking takes the last one out.
 */
public class PressureVesselBlock extends Block implements EntityBlock {

    public PressureVesselBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PressureVesselBlockEntity vessel) {
            if (player.isShiftKeyDown() && vessel.jarCount() > 0) {
                ItemStack out = vessel.takeJar();
                if (!player.getInventory().add(out))
                    player.drop(out, false);
                player.displayClientMessage(status(vessel), true);
            } else
                player.displayClientMessage(Units.figure(vessel.getPressure()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!held.has(FDataComponents.CANNED.get()) || !(level.getBlockEntity(pos) instanceof PressureVesselBlockEntity vessel))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            if (vessel.addJar(held.copyWithCount(1)) && !player.getAbilities().instabuild)
                held.shrink(1);
            player.displayClientMessage(status(vessel), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Component status(PressureVesselBlockEntity vessel) {
        return Component.translatable("feedback.canning.status", vessel.jarCount(), FTuning.CANNING_SLOTS);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof PressureVesselBlockEntity vessel)
            vessel.dropJars();
        super.onRemove(state, level, pos, next, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressureVesselBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((PressureVesselBlockEntity) be).tickServer();
    }
}
