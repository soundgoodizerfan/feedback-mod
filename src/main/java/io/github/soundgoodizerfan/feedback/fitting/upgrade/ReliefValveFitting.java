// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * A spring-loaded relief valve fitted to a boiler: it lifts at {@link FTuning#BOILER_RELIEF_PU}
 * and vents steam until the pressure is back under it. Relief only -- it never regulates and
 * never vents below its set pressure (ruling 2026-10-13). The holder does the venting; this part
 * is the fact that one is fitted, so it is stateless and the holder persists a flag. It does not
 * wear: it is spent only in steam and a scald, and only when the boiler is run over the line.
 */
public class ReliefValveFitting implements UpgradeFitting {

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.RELIEF_VALVE.get());
    }
}
