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
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A plate that burns what stands on it, as hot as the heat pointed at it. A mob that does not burn is untouched; the drops come out cooked, or charred if they wait. */
public class FirePlateBlock extends Block implements KillMethodBlock {

    /** Invented. Below this a plate is only warm. */
    private static final float WORKING_TU = 350f;

    public FirePlateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public KillMethod method(Level level, BlockPos pos, BlockState state) {
        float tu = HeatSource.at(level, pos).value();
        return new KillMethod() {
            @Override
            public KillKind kind() {
                return KillKind.HEAT;
            }

            @Override
            public boolean ready() {
                return tu >= WORKING_TU;
            }

            @Override
            public float damage(MobFacts facts) {
                return facts.fireImmune() ? 0f : (tu - 250f) / 20f;
            }
        };
    }
}
