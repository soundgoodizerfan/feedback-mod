// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.dropper;

import net.minecraft.world.level.block.DispenserBlock;

/**
 * The dispenser, driven by a spring ({@link SpringFeed}): a signal releases a cocked spring that pushes the item out, and the spring stays
 * spent until it is cocked by hand or by a turning shaft. Redstone alone moves nothing. Vanilla's block is swapped for it
 * ({@code VanillaVesselSwap}); it keeps vanilla's inventory and dispense behaviours.
 */
public class SpringDispenserBlock extends DispenserBlock {

    public static final net.minecraft.world.level.block.state.properties.BooleanProperty COCKED = SpringFeed.COCKED;

    public SpringDispenserBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(COCKED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,
            net.minecraft.world.level.block.state.BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COCKED);
    }

    /** Releases only a cocked spring; the spring is then spent. */
    @Override
    protected void dispenseFrom(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.block.state.BlockState state,
                                net.minecraft.core.BlockPos pos) {
        if (!state.getValue(COCKED)) {
            level.levelEvent(1001, pos, 0); // the dry click of a dispenser with nothing to push
            return;
        }
        super.dispenseFrom(level, state, pos);
        SpringFeed.release(level, pos);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (player.isShiftKeyDown() && SpringFeed.cockByHand(level, pos, state))
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onPlace(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                           net.minecraft.world.level.block.state.BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!old.is(this) && !level.isClientSide)
            level.scheduleTick(pos, this, SpringFeed.POLL);
    }

    /** A tick is either a signal's release (the state is triggered) or a poll for a turning shaft that recocks the spring. */
    @Override
    protected void tick(net.minecraft.world.level.block.state.BlockState state, net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                        net.minecraft.util.RandomSource random) {
        if (state.getValue(TRIGGERED))
            super.tick(state, level, pos, random);
        SpringFeed.poll(level, pos, this);
    }
}
