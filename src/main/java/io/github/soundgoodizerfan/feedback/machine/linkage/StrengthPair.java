// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.linkage;
import io.github.soundgoodizerfan.feedback.core.unit.Unit;

/**
 * A machine that delivers a different figure per stroke depending on how it is geared.
 * <p>
 * The unit is the machine's own: {@code St} for a hammer, which delivers force, {@code mB} for a
 * bellows, which delivers displaced air. One crank drives both, and the pair is where the two
 * meanings of one stroke are written down.
 * <p>
 * Philosophy 21: a machine has no single strength. It publishes both figures -- printed on the
 * block, as {@code 12 / 3 St} -- and which one the player gets depends on the crank they bolted
 * to it. Satisfying a minimum-force requirement means re-gearing the drive, not buying a
 * stronger machine.
 */
public interface StrengthPair<U extends Unit> {

    U getStrength(Throw installed);

    /**
     * The most this machine can deliver per stroke, however hard it is driven.
     * <p>
     * A ceiling set by construction, not by the drive. Gearing down multiplies force without any
     * natural limit, so without this one long gear train would make every machine of a kind
     * equivalent and the force axis would collapse into how many cogs somebody was willing to
     * place. A paper blade at a million RPM still will not cut steel.
     * <p>
     * It is also what keeps a better machine worth buying once gearing exists: a gear train
     * reaches the ceiling, and only a better machine raises it.
     */
    U getMaxStrength();
}
