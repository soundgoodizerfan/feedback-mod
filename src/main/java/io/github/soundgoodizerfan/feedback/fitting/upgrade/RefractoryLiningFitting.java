// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.fitting.FittingTuning;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * The mod's first {@link UpgradeFitting}: a thicker refractory wall fitted inside a vessel. It
 * adds thermal mass and nothing else, so the vessel climbs and falls more slowly and settles
 * exactly where it did -- see {@link FittingTuning#REFRACTORY_LINING_MASS} for why mass rather than
 * leak, and for the trade.
 * <p>
 * Stateless: whether one is fitted is the whole of it, so the holder persists a flag and
 * rebuilds this on load.
 */
public class RefractoryLiningFitting implements UpgradeFitting {

    @Override
    public ThermalMass addedThermalMass() {
        return FittingTuning.REFRACTORY_LINING_MASS;
    }

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.REFRACTORY_LINING.get());
    }
}
