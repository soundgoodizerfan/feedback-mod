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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * An engine's fuel as a blend (petrochemistry spec 9.1: a diesel-naphtha blend has an in-between
 * ignition point; a blend is a fact, not a fight). It never refuses and never filters: whatever is put
 * in mixes by mass, the way a tank's temperature does, and what the engine reads is the mixture's
 * flame temperature and ignition point. A fluid that does not burn dilutes the flame and adds no
 * ignition point; there is no drain, because the engine only burns what it holds.
 */
public final class FuelBlend implements IFluidHandler {

    private final Runnable changed;
    private float mb, mass, flameMass, ignitionMass, ignitionWeight, oxygenMass;
    private FluidStack shown = FluidStack.EMPTY;

    public FuelBlend(Runnable changed) {
        this.changed = changed;
    }

    public boolean isEmpty() {
        return mb < 1f;
    }

    /** Mass-weighted flame temperature of everything held, non-fuel counting as cold. */
    public float flame() {
        return mass <= 0f ? 0f : flameMass / mass;
    }

    /** Mass-weighted ignition point of the fuel held; far above any compression if there is none. */
    public float ignition() {
        return ignitionWeight <= 0f ? Float.MAX_VALUE : ignitionMass / ignitionWeight;
    }

    /** Mass-weighted oxygen balance of everything held: positive, the blend carries oxidiser of its own. */
    public float oxygen() {
        return mass <= 0f ? 0f : oxygenMass / mass;
    }

    /** Burns off {@code amount} mB, taking the same share of the mixture's mass. */
    public void burn(int amount) {
        if (mb <= 0f)
            return;
        float keep = Math.max(0f, (mb - amount) / mb);
        mb *= keep;
        mass *= keep;
        flameMass *= keep;
        ignitionMass *= keep;
        ignitionWeight *= keep;
        oxygenMass *= keep;
        if (mb < 1f)
            mb = mass = flameMass = ignitionMass = ignitionWeight = oxygenMass = 0f;
        changed.run();
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return shown.isEmpty() ? FluidStack.EMPTY : shown.copyWithAmount(Math.round(mb));
    }

    @Override
    public int getTankCapacity(int tank) {
        return FTuning.ENGINE_FUEL_TANK_MB;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        int amount = Math.min(resource.getAmount(), FTuning.ENGINE_FUEL_TANK_MB - Math.round(mb));
        if (amount <= 0)
            return 0;
        if (action.execute()) {
            Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(resource.getFluid()));
            float density = m == null || m.density().muPerMb() <= 0f ? 1f : m.density().muPerMb();
            float w = amount * density;
            float flame = 0f;
            if (m != null && m.combustion().isPresent())
                flame = m.combustion().get().flameTemperature().map(t -> t.value()).orElse(0f);
            mb += amount;
            mass += w;
            flameMass += w * flame;
            if (m != null && m.combustion().isPresent())
                oxygenMass += w * m.combustion().get().oxygenBalance();
            if (flame > 0f && m.thermal().oxidationTemperature().isPresent()) {
                ignitionMass += w * m.thermal().oxidationTemperature().get().value();
                ignitionWeight += w;
            }
            if (flame > 0f || shown.isEmpty())
                shown = resource.copyWithAmount(1);
            changed.run();
        }
        return amount;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }

    public void save(CompoundTag tag) {
        tag.putFloat("Mb", mb);
        tag.putFloat("Mass", mass);
        tag.putFloat("FlameMass", flameMass);
        tag.putFloat("IgnitionMass", ignitionMass);
        tag.putFloat("IgnitionWeight", ignitionWeight);
        tag.putFloat("OxygenMass", oxygenMass);
    }

    public void load(CompoundTag tag) {
        mb = tag.getFloat("Mb");
        mass = tag.getFloat("Mass");
        flameMass = tag.getFloat("FlameMass");
        ignitionMass = tag.getFloat("IgnitionMass");
        ignitionWeight = tag.getFloat("IgnitionWeight");
        oxygenMass = tag.getFloat("OxygenMass");
    }
}
