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
}
