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
package io.github.soundgoodizerfan.feedback.machine.sculk;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The death-capture organ: a node on the XP wire that takes the XP of anything dying within reach into its run.
 * It holds nothing and ticks nothing; {@link SculkSites} finds it when a death happens.
 */
public class CatalystTapBlockEntity extends SculkNodeBlockEntity {

    public CatalystTapBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CATALYST_TAP.get(), pos, state);
    }

    @Override
    protected void joined() {
        SculkSites.addTap(level, worldPosition);
    }
}
