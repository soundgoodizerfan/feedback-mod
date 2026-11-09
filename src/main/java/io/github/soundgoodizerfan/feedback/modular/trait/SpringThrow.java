// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

/**
 * Spring-launched blade: a spear head thrown from a spring. Like {@link Harpoon} it does nothing in the hand; its params are read
 * by {@link io.github.soundgoodizerfan.feedback.modular.Throwing} on release: {@code speed_mult} on the throw's speed (quick to let
 * go, so a short {@code full_charge_ticks}), and the spring's {@code rating}, which a heavy head at that speed can overload
 * ({@link io.github.soundgoodizerfan.feedback.modular.SpringSet}). A spring with a set throws less hard.
 */
public final class SpringThrow implements ModularTrait {
}
