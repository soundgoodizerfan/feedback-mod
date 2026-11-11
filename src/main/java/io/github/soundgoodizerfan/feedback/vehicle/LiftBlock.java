// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import com.mojang.serialization.MapCodec;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
 * The stand a vehicle is built on ({@code docs/specs/feedback_vehicles_spec.md} section 4), after Scrap
 * Mechanic's lift: build on top of it, use it, and everything resting on it becomes one Sable
 * body held still where it stands. Use again to raise it a step, sneak-use to lower it, and
 * sneak-use at the bottom to turn it back into blocks; break the lift and the vehicle drops into
 * physics.
 *
 * <p>The lift is never part of the vehicle. It is what bounds Sable's floodfill, which otherwise
 * takes every block touching the build -- the ground included.
 */
public class LiftBlock extends BaseEntityBlock {

    public static final MapCodec<LiftBlock> CODEC = simpleCodec(LiftBlock::new);

    public LiftBlock(Properties properties) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof LiftBlockEntity lift))
            return InteractionResult.SUCCESS;
        // Sneak-use at the bottom puts the vehicle back into the world as blocks.
        String message = !lift.holding() ? lift.lift(server)
                : player.isShiftKeyDown() && lift.height() == 0 ? lift.lower(server)
                : lift.step(server, player.isShiftKeyDown() ? -1 : 1);
        Component balance = lift.holding() ? lift.balance(server) : null;
        Component said = Component.translatable("message.feedback.lift." + message, lift.height());
        if (message.equals("lifted") && lift.jointLoops() > 0)
            said = said.copy().append(" ").append(Component.translatable("message.feedback.lift.loops", lift.jointLoops()));
        player.displayClientMessage(balance == null ? said : said.copy().append(" ").append(balance), true);
        return InteractionResult.CONSUME;
    }

    /** Breaking the lift lets go of the vehicle; unloading it does not. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof LiftBlockEntity lift)
            lift.release(server);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FVehicles.LIFT_ENTITY.get(), LiftBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LiftBlockEntity(pos, state);
    }
}
