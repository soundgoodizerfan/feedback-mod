// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

import io.github.soundgoodizerfan.feedback.core.FTuning;

/**
 * Bearing friction: the load a component adds per RPM it turns at, in {@code Su/RPM}.
 *
 * <h2>Why friction is charged in Su and scales with speed</h2>
 * Loss is {@link Su} rather than {@link Rpm} because a rigid shaft turns at one speed along its
 * whole length -- a difference between its ends would be torsion, not loss -- and what friction
 * actually consumes is torque. Scaling with speed is what turns a tax into a decision, and three
 * things follow that nobody had to write as rules:
 * <ul>
 *   <li>A stopped shaft costs nothing, so an idle network is free rather than "overstressed".</li>
 *   <li>A network finds its own top speed, where {@code capacity = drag x rpm}. A long run does
 *       not hit a wall and refuse to turn; it just turns more slowly, because friction ate the
 *       torque. Unlike a hard per-shaft cap, which produces the absurdity of a generator being
 *       too good for its own shafting.</li>
 *   <li>Slow and wide becomes a real alternative to fast and narrow, because speed is now
 *       something bought by the block.</li>
 * </ul>
 *
 * <h2>Kept apart from {@link Inertia} on purpose</h2>
 * Both are per-component floats that live two lines apart in {@code FTuning}, both describe "how
 * much this part resists turning", and they are completely different quantities: drag is a
 * steady-state cost and inertia is a transient one. A network with high drag and no inertia is
 * slow and twitchy; one with the reverse is fast and ponderous. Swapping them would produce a
 * network that still ran, still looked plausible, and felt wrong -- the exact failure mode this
 * package exists to make impossible.
 */
public record Drag(float suPerRpm) implements Unit {

    @Override
    public float raw() {
        return suPerRpm;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.drag";
    }
}
