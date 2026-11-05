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
package io.github.soundgoodizerfan.feedback.geology;

import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.neoforged.neoforge.fluids.FluidStack;

/** The surface sign of a soul oil basin: a seep a copper bucket scoops soul oil from. */
public class SoulSeepBlock extends SeepBlock {
    public SoulSeepBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected FluidStack scoop() {
        return new FluidStack(FFluids.SOUL_OIL.get(), 250);
    }
}
