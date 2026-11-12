// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.dropper;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The spring behind a dropper or dispenser. Redstone is signal only, so the signal does not push the item: it releases a spring, and the
 * spring does the work. A released spring stays spent until it is cocked again, by hand (sneak and use with an empty hand) or by a turning
 * shaft touching the block, which winds it. A signal on a spent spring is the dry click of vanilla's empty dispenser.
 */
final class SpringFeed {

    static final BooleanProperty COCKED = BooleanProperty.create("cocked");
    /** Ticks between looks for a turning shaft while the spring is spent. */
    static final int POLL = 20;

    private SpringFeed() {
    }

    static void release(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(COCKED)) {
            level.setBlock(pos, state.setValue(COCKED, false), Block.UPDATE_ALL);
            level.scheduleTick(pos, state.getBlock(), POLL);
        }
    }

    static boolean cockByHand(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(COCKED) || state.getValue(COCKED))
            return false;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(COCKED, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 0.8f, 0.9f);
        }
        return true;
    }

    /** A spent spring winds up while a turning shaft touches the block; otherwise look again soon. */
    static void poll(ServerLevel level, BlockPos pos, Block block) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(block) || state.getValue(COCKED))
            return;
        for (Direction d : Direction.values())
            if (level.getBlockEntity(pos.relative(d)) instanceof RotationNode node && node.getRpm().value() != 0f) {
                level.setBlock(pos, state.setValue(COCKED, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 0.6f, 1.1f);
                return;
            }
        level.scheduleTick(pos, block, POLL);
    }
}
