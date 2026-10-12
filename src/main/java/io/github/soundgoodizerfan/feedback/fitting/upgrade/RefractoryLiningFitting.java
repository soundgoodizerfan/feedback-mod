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
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * The mod's first {@link UpgradeFitting}: a thicker refractory wall fitted inside a vessel. It
 * adds thermal mass and nothing else, so the vessel climbs and falls more slowly and settles
 * exactly where it did -- see {@link FTuning#REFRACTORY_LINING_MASS} for why mass rather than
 * leak, and for the trade.
 * <p>
 * Stateless: whether one is fitted is the whole of it, so the holder persists a flag and
 * rebuilds this on load.
 */
public class RefractoryLiningFitting implements UpgradeFitting {

    @Override
    public ThermalMass addedThermalMass() {
        return FTuning.REFRACTORY_LINING_MASS;
    }

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.REFRACTORY_LINING.get());
    }
}
