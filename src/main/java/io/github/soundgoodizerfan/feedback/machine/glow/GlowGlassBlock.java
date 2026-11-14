// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.glow;

import io.github.soundgoodizerfan.feedback.item.FormBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A block of glass that fluoresces in radiation (uranium glass): its {@code glow} state, 0 to {@link GlowTuning#MAX_GLOW}, follows the
 * dose around it ({@link RadiationField}) in steps ({@link GlowTuning#BANDS_RU}) and is its block light. It counts its own faint activity
 * through the source table, so a heap of it glows a little by itself. It looks every {@link GlowTuning#SAMPLE_INTERVAL_TICKS} ticks
 * with a scheduled tick (no block entity), and writes its state only when the level changes.
 */
public class GlowGlassBlock extends FormBlock {

    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, GlowTuning.MAX_GLOW);

    public GlowGlassBlock(ResourceLocation material, String form) {
        super(material, form, glassProperties().lightLevel(state -> state.getValue(GLOW)));
        registerDefaultState(defaultBlockState().setValue(GLOW, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLOW);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide && !old.is(this))
            level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int glow = GlowTuning.levelFor(RadiationField.at(level, pos));
        if (state.getValue(GLOW) != glow)
            level.setBlock(pos, state.setValue(GLOW, glow), 3);
        level.scheduleTick(pos, this, GlowTuning.SAMPLE_INTERVAL_TICKS);
    }
}
