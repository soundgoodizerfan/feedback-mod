// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Something that can hold a melt -- the crucible and the thermal vessel, so far.
 *
 * <h2>Why a tank rather than an item-capability exposure, for now</h2>
 * No external capability is registered against this yet. Nothing needs one: there is no pipe,
 * no hopper-for-fluids, and no other mod's tank sitting against ours. {@link
 * io.github.soundgoodizerfan.feedback.item.MoldItem} is the only thing that ever calls
 * {@link #getTank()}, by casting the clicked block entity the same way {@code CalipersItem} and
 * {@code ThermometerItem} already do for {@code Wearing} and {@link ThermalBody}. Exposing an
 * {@code IFluidHandler} capability costs one more line whenever a pipe mod actually needs it --
 * see the melting spec conversation for why that is a real, committed later step and not a
 * speculative one.
 */
public interface MoltenVessel extends ThermalBody {

    FluidTank getTank();

    /**
     * {@link #getTank()} hands out a real, mutable {@link FluidTank}, so a mold draining it
     * happens directly against that tank rather than through this interface. Most implementers
     * have nothing else to keep in sync and can ignore this; the crucible overrides it because
     * its tank is a computed view over a separate ledger (see its {@code AlloyMix}) that a bare
     * drain would otherwise leave stale.
     */
    default void onDrained(FluidStack drained) {
    }

    /**
     * Puts molten metal into the vessel (a dip that turns the bath into another fluid). Returns what
     * went in. The crucible overrides it to go through its alloy ledger.
     */
    default int addMolten(FluidStack stack) {
        return getTank().fill(stack, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
    }
}
