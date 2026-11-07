// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.geology;

import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.neoforged.neoforge.fluids.FluidStack;

/** The surface sign of a soul oil basin: a seep a copper bucket scoops soul oil from. */
public class SoulSeepBlock extends SeepBlock {
    public SoulSeepBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected FluidStack scoop() {
        return new FluidStack(FFluids.SOUL_OIL.get(), 250);
    }
}
