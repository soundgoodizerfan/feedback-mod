// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.geology;

import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.neoforged.neoforge.fluids.FluidStack;

/** The sign of a Deep Dark basin: a seep on a cave floor a copper bucket scoops a little weathered echo crude from. */
public class EchoSeepBlock extends SeepBlock {

    /** What a seep gives: the volatile end has evaporated but the experience stays. Placeholder figures. */
    public static final CrudeGrade SEEP_GRADE = new CrudeGrade(0.99f, 0.01f, 0.03f, 0.5f);

    public EchoSeepBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected FluidStack scoop() {
        return CrudeGrade.stamp(new FluidStack(FFluids.ECHO_CRUDE.get(), 250), SEEP_GRADE);
    }
}
