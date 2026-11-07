// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

/**
 * Harpoon (spec 5.4). A harpoon is a weapon that is thrown on a line: the trait itself does
 * nothing in the hand, and its params ({@code rope}, {@code pull}, {@code water_mult},
 * {@code return}) are read by {@link io.github.soundgoodizerfan.feedback.modular.Throwing} when
 * a trident-shaped item is thrown. In flight it is a {@code ThrownWeapon} on a rope no longer than
 * {@code rope} blocks; when it strikes a living thing it stays in it and reels the target in at
 * {@code pull} blocks a tick ({@code water_mult} times that in water, less for what resists
 * knockback) until it is close, then lets go and flies back. It always comes back, slowly. The
 * cost is a poor melee blow ({@code strike} of the stock).
 *
 * <p>This was a one-off shove (a velocity added once on impact), which is what a knockback already
 * is, so it pulled no harder than a hit pushed.
 */
public final class Harpoon implements ModularTrait {
}
