// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.geology.CrudeGrade;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
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
    /** Game time this tank was saved at, until the first {@link #relax} after a load spends it (-1 for none). */
    private long savedAt = -1L;
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
        if (savedAt >= 0L) {
            // What sat in a chunk nobody had loaded cooled (or warmed) all the while: close the whole gap at once, the
            // same law a tick applies, solved for the number of ticks away.
            long now = gameTime();
            long gap = now < 0L ? 0L : now - savedAt;
            savedAt = -1L;
            if (gap >= FTuning.CATCHUP_MIN_TICKS && !fluid.isEmpty())
                temperature = toward.value() + (temperature - toward.value())
                        * (float) Math.pow(1f - FTuning.FLUID_RELAX_PER_TICK, gap);
        }
        if (!fluid.isEmpty())
            temperature += (toward.value() - temperature) * FTuning.FLUID_RELAX_PER_TICK;
    }

    /** The server's game time, or -1 where there is no server (a remote client). */
    private static long gameTime() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? -1L : server.overworld().getGameTime();
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
        // Grades merge by mass (spec 2.4), so two crudes differing only in grade are the same fluid here.
        if (!FluidStack.isSameFluidSameComponents(ungraded(fluid), ungraded(bare)))
            return 0;
        int filled = Math.min(capacity - fluid.getAmount(), resource.getAmount());
        if (filled > 0 && action.execute()) {
            if (fluid.has(FDataComponents.CRUDE_GRADE.get()) || bare.has(FDataComponents.CRUDE_GRADE.get()))
                fluid.set(FDataComponents.CRUDE_GRADE.get(), CrudeGrade.merge(CrudeGrade.of(fluid), fluid.getAmount(),
                        CrudeGrade.of(bare), filled));
            temperature = FluidHeat.merge(temperature, fluid.getAmount(), incoming, filled);
            fluid.grow(filled);
            onContentsChanged();
        }
        return Math.max(0, filled);
    }

    private static FluidStack ungraded(FluidStack stack) {
        FluidStack out = stack.copy();
        out.remove(FDataComponents.CRUDE_GRADE.get());
        return out;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        // Tanks never filter: any crude of the same fluid comes out, at the tank's blended grade.
        return fluid.getFluid() == resource.getFluid() ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
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
        savedAt = nbt.contains("FluidAt") ? nbt.getLong("FluidAt") : -1L;
        return this;
    }

    @Override
    public CompoundTag writeToNBT(HolderLookup.Provider registries, CompoundTag nbt) {
        super.writeToNBT(registries, nbt);
        nbt.putFloat("FluidTu", temperature);
        long now = gameTime();
        if (now >= 0L)
            nbt.putLong("FluidAt", now);
        return nbt;
    }
}
