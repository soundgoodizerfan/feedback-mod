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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A fluid's temperature, carried on the stack ({@code feedback_mechanics.md} 5.9). The fluid id says
 * only what phase it is (water, steam); how hot it is rides with it through any handler, so a line
 * of steam is as hot as the boiler made it and a tank cools its own contents.
 *
 * <p>Stored rounded to the nearest Tu on the way out of a {@link HeatedTank}, so streams from one
 * source stay equal enough for a foreign tank to merge them. Absent means ambient.
 */
public final class FluidHeat {

    private FluidHeat() {
    }

    /** The stamp on {@code stack}, or the default ambient if it has none. */
    public static Tu of(FluidStack stack) {
        return new Tu(stack.getOrDefault(FDataComponents.FLUID_TEMPERATURE.get(), FTuning.AMBIENT_TU.value()));
    }

    /** {@code stack} stamped at {@code tu} (a copy; rounded to a whole Tu). */
    public static FluidStack stamp(FluidStack stack, float tu) {
        FluidStack out = stack.copy();
        if (!out.isEmpty())
            out.set(FDataComponents.FLUID_TEMPERATURE.get(), (float) Math.round(tu));
        return out;
    }

    /** {@code stack} with no stamp: what a tank compares and stores. */
    public static FluidStack bare(FluidStack stack) {
        FluidStack out = stack.copy();
        out.remove(FDataComponents.FLUID_TEMPERATURE.get());
        return out;
    }

    /** The one merge rule, used by every tank: mass-weighted average (volume stands in for mass of one phase). */
    public static float merge(float tuA, int mbA, float tuB, int mbB) {
        int total = mbA + mbB;
        return total <= 0 ? tuA : (tuA * mbA + tuB * mbB) / total;
    }
}
