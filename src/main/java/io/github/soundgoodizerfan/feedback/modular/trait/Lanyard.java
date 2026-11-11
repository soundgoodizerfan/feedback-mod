// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

/**
 * A line from the weapon to the hand. It has no hook of its own: {@link io.github.soundgoodizerfan.feedback.modular.Throwing} reads its params on a throw (a cost in speed, a reel, a weak return), and a disarming shot cannot strip a weapon that has one.
 */
public final class Lanyard implements ModularTrait {
}
