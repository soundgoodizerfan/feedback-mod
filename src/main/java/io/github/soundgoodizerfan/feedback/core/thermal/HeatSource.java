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
package io.github.soundgoodizerfan.feedback.core.thermal;

import net.minecraft.tags.FluidTags;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A fire. Something that holds a flame temperature, which is the ceiling on anything it heats.
 *
 * <h2>Why a source is not a {@link ThermalBody}</h2>
 * A body has a mass and a temperature that other things change. A fire has a temperature and no
 * mass worth modelling -- nothing the player does cools a fire, they either feed it or they do
 * not. Keeping them apart is what stops the crucible needing to know whether it is over charcoal,
 * over lava, or over something a later slice invents: it asks the block below how hot it burns and
 * has no further opinion.
 */
public interface HeatSource {

    /** How hot this burns right now. Ambient when it is not lit. */
    Tu getFireTu();

    /**
     * How hot the block below this position burns.
     *
     * <h3>Lava is handled here and nowhere else</h3>
     * It is a plain vanilla block with no block entity, so it cannot implement anything -- and it
     * should not have to. A special case for the one vanilla fluid that is a heat source is a
     * cheaper honesty than wrapping lava in machinery, and it keeps the slice's best joke intact:
     * the most stable thermal environment in the game is a puddle the player has been ignoring
     * since their first night.
     */
    static Tu below(Level level, BlockPos pos) {
        BlockPos under = pos.below();

        BlockEntity be = level.getBlockEntity(under);
        if (be instanceof HeatSource source)
            return source.getFireTu();

        // By tag, so any mod's lava counts. A lava block's fluid state is lava, so no block check.
        if (level.getFluidState(under).is(FluidTags.LAVA))
            return FTuning.FIRE_TU_LAVA;

        return AmbientTable.at(level);
    }
}
