// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

/**
 * Trident (spec 5.4): the plain thrown spear. Like {@link Harpoon} it does nothing itself: it is the
 * trait that is not {@link SpearCharge}, which is how a spear tells that it is thrown on release
 * ({@code ModularSpear}) rather than charged by key. Riptide and Loyalty apply as in vanilla.
 */
public final class TridentThrow implements ModularTrait {
}
