// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

/**
 * A rest on a bow or crossbow: it has no hook of its own; its {@code spread_mult} and {@code draw_mult} are read by {@link io.github.soundgoodizerfan.feedback.modular.RangedDispatch}, which multiplies them over every trait the weapon has.
 */
public final class Steady implements ModularTrait {
}
