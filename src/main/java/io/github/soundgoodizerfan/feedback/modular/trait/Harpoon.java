/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
