// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.damper;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A vent, stacked against a vessel the way {@code InsulationBlock} is. It is the Bellows' opposite:
 * the Bellows only ever pushes air in, one-way (see its own class doc), and the Damper only ever
 * lets heat out. Where the Bellows is a {@code Reciprocating} linkage part driven by a crank, the
 * Damper needs no force input at all -- a vent flap is binary, open or shut, so it is a plain
 * {@code Switchable}, exactly philosophy 14's actuator vocabulary.
 * <p>
 * Crucible-only, same scope as {@code InsulationBlock} -- the Furnace/Smoker/Blast Furnace family
 * never grew a neighbour-scan for insulation either, and this follows that existing line rather
 * than drawing a new one.
 */
public class DamperBlock extends Block implements EntityBlock {

    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public DamperBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    /** A player is a perfectly good controller (§13) -- same as throwing a clutch by hand. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DamperBlockEntity damper)
            damper.setEngaged(!damper.isEngaged());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DamperBlockEntity(pos, state);
    }
}
