// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

/**
 * A machine that absorbs the force it fails to put into the work, and is worse for it.
 *
 * <h2>Two methods, and the second is the surprising one</h2>
 * Reading condition needs the figure, obviously. It also needs to know whether the machine is
 * <em>running</em>, because philosophy 10's rule is that a reading is passive when taking it is
 * free and an action when it costs something -- and what this reading costs is a stopped line.
 * Without the second method the price is unenforceable and the instrument becomes a keystroke
 * with no decision in it, which is the thing 3 cuts.
 *
 * <h2>What is deliberately not here</h2>
 * There is no {@code wear(float)}. Absorbing force is still private to the one machine that does
 * it, because the <em>consequence</em> of a wasted blow is the machine's own business -- a bellows
 * takes the same surplus as nothing at all. This interface is the read side only, which is why an
 * instrument can depend on it without depending on any machine.
 */
public interface Wearing {

    /** How sound this machine is, 1 as built, falling to its own floor. */
    float getCondition();

    /**
     * Whether it is working right now, and so cannot be measured.
     * <p>
     * "Running" is drawing Su, which is the intuitive answer and the one 4b settled on. What
     * stands in for it today is the drive: a machine with a linkage turning in front of it is
     * working, and one with a stopped linkage is not. Equivalent in practice, and it works without
     * first making machine load fall to zero at rest -- which is a rotation-network change with
     * consequences for the Su ledger and wants deciding on its own merits.
     */
    boolean isRunning();
}
