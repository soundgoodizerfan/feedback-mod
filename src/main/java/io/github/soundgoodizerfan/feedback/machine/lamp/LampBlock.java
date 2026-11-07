// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.lamp;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The glowstone lamp: an electroluminescent panel on a power line. Brightness is the {@link #GLOW}
 * block state, so block light follows the state and the client needs no block entity data.
 */
public class LampBlock extends Block implements EntityBlock {

    /** Off, three lit steps, and burnt out (dark for good, replaced by hand). */
    public enum Glow implements StringRepresentable {
        OFF, DIM, BRIGHT, FULL, BURNT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Block light of this step: a third and two thirds of the full step for dim and bright. */
        public int light() {
            int full = FTuning.LAMP_FULL_LIGHT;
            return switch (this) {
                case OFF, BURNT -> 0;
                case DIM -> Math.max(1, full / 3);
                case BRIGHT -> Math.max(1, full * 2 / 3);
                case FULL -> full;
            };
        }
    }

    public static final EnumProperty<Glow> GLOW = EnumProperty.create("glow", Glow.class);

    public LampBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(GLOW).light()));
        registerDefaultState(stateDefinition.any().setValue(GLOW, Glow.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLOW);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LampBlockEntity(pos, state);
    }
}
