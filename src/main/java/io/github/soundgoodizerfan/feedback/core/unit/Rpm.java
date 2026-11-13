// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

import io.github.soundgoodizerfan.feedback.core.FTuning;

/**
 * Rotational speed, in revolutions per minute.
 *
 * <h2>Independent of {@link Su}, and that independence is load-bearing</h2>
 * A shaft has a speed and a network has a load, and neither implies the other. Keeping them
 * apart is what lets gearing be an honest trade: a geared-up branch turns faster and therefore
 * charges the ledger more, because friction and referred mass both scale, but nothing about
 * being fast makes it strong.
 *
 * <h2>Speed is a real number and a coarse readout</h2>
 * The simulation holds an exact RPM; a player standing next to a shaft gets four words for it
 * (§8, and {@code FTuning}'s speed bands). Both are true at once, which is the ordinary state of
 * affairs in this mod -- the world resolves on the figure and the player resolves on the
 * adjective until they buy something better.
 *
 * <h2>Signed, deliberately</h2>
 * The sign is the direction of rotation, so a reversed shaft is a negative RPM rather than a
 * separate flag. Anything comparing magnitudes has to say {@code Math.abs} and mean it.
 */
public record Rpm(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.rpm";
    }
}
