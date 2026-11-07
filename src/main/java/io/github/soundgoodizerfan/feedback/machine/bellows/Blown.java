// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bellows;

import io.github.soundgoodizerfan.feedback.core.unit.Mb;

/**
 * Something a bellows can blow into.
 *
 * <h2>Air, not heat</h2>
 * The bellows moves air and knows nothing else. It does not raise a temperature, it does not
 * consult a thermometer, and it has no idea whether the thing in front of it is even alight --
 * philosophy 10's separation, arriving as a one-method interface. What air <em>does</em> is
 * entirely the fire's business, which is why a bellows pointed at an unlit firebox politely
 * achieves nothing rather than needing a rule saying so.
 */
public interface Blown {

    /** Take this much air. Anything over the buffer is simply lost to the room. */
    void addAir(Mb air);
}
