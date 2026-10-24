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

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.Predicate;

/**
 * A tank whose contents have a temperature ({@code feedback_mechanics.md} 5.9). The stack inside is
 * kept bare and the temperature is a float beside it, so cooling every tick never makes the stack
 * unequal to itself. What goes in is merged by {@link FluidHeat#merge}; what comes out is stamped.
 * An empty tank has no temperature. Pipes and valves add no loss (user ruling 2026-10-23): only a
 * tank cools what it holds, by {@link #relax}.
 */
public class HeatedTank extends FluidTank {

    private float temperature = FTuning.AMBIENT_TU.value();
    private final Runnable onChange;

    public HeatedTank(int capacity, Predicate<FluidStack> validator, Runnable onChange) {
        super(capacity, validator);
        this.onChange = onChange;
    }

    public HeatedTank(int capacity, Runnable onChange) {
        this(capacity, stack -> true, onChange);
    }

    public Tu temperature() {
        return new Tu(temperature);
    }

    /** Pulls the contents toward {@code toward} by a fixed share per tick (a tank in a hot body relaxes to the body, not the room). */
    public void relax(Tu toward) {
        if (!fluid.isEmpty())
            temperature += (toward.value() - temperature) * FTuning.FLUID_RELAX_PER_TICK;
    }

    @Override
    protected void onContentsChanged() {
        onChange.run();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(resource))
            return 0;
        FluidStack bare = FluidHeat.bare(resource);
        float incoming = FluidHeat.of(resource).value();
        if (fluid.isEmpty()) {
            int filled = Math.min(capacity, resource.getAmount());
            if (action.execute()) {
                fluid = bare.copyWithAmount(filled);
                temperature = incoming;
                onContentsChanged();
            }
            return filled;
        }
        if (!FluidStack.isSameFluidSameComponents(fluid, bare))
            return 0;
        int filled = Math.min(capacity - fluid.getAmount(), resource.getAmount());
        if (filled > 0 && action.execute()) {
            temperature = FluidHeat.merge(temperature, fluid.getAmount(), incoming, filled);
            fluid.grow(filled);
            onContentsChanged();
        }
        return Math.max(0, filled);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return stamped(super.drain(FluidHeat.bare(resource), action));
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return stamped(super.drain(maxDrain, action));
    }

    private FluidStack stamped(FluidStack out) {
        return out.isEmpty() ? out : FluidHeat.stamp(out, temperature);
    }

    @Override
    public FluidTank readFromNBT(HolderLookup.Provider registries, CompoundTag nbt) {
        super.readFromNBT(registries, nbt);
        temperature = nbt.contains("FluidTu") ? nbt.getFloat("FluidTu") : FTuning.AMBIENT_TU.value();
        return this;
    }

    @Override
    public CompoundTag writeToNBT(HolderLookup.Provider registries, CompoundTag nbt) {
        super.writeToNBT(registries, nbt);
        nbt.putFloat("FluidTu", temperature);
        return nbt;
    }
}
