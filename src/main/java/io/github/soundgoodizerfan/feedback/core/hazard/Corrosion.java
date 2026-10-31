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
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What an acid or a base does to the vessel that holds it. A fluid whose material carries the {@code corrosive}
 * trait bites a vessel whose corrosion resistance is under {@link FTuning#CORROSIVE_BITE}, a little condition at a
 * time and never at once; a resistant lining takes no harm at all. Wear from the wrong vessel is misuse, not uptime.
 */
public final class Corrosion {

    private Corrosion() {
    }

    /** Whether {@code stack} is a corrosive liquid. */
    public static boolean corrosive(FluidStack stack) {
        if (stack.isEmpty())
            return false;
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        return m != null && m.traits().contains(Material.Trait.CORROSIVE);
    }

    /** Condition lost over one second holding a corrosive liquid in a vessel of the given resistance, 0 if it resists. */
    public static float wearPerSecond(float resistance) {
        return resistance >= FTuning.CORROSIVE_BITE ? 0f
                : FTuning.CORROSION_WEAR_PER_SECOND * (FTuning.CORROSIVE_BITE - resistance) / FTuning.CORROSIVE_BITE;
    }
}
