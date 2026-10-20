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
package io.github.soundgoodizerfan.feedback.instrument;

import net.minecraft.network.chat.Component;

/**
 * An item that converts one of the player's adjectives into a figure.
 *
 * <h2>The point of this interface</h2>
 * Before it, every instrument meant its own display code: which tooltip it changed, how the line
 * was worded, where the reading came from. That is the shape where a thermometer costs a day and
 * a fourth instrument costs a week.
 * <p>
 * An instrument now declares three things and nothing else — what it can read, how finely, and
 * what to call it — and every display in the mod already knows what to do with that. A new
 * instrument touches no tooltip, no HUD and no compat code.
 *
 * <h2>Resolution is here from the start on purpose</h2>
 * Philosophy 8 says each tier of instrument buys <em>significant figures</em>, not access: a
 * crude thermometer reads to 25 Tu and a good one to 0.1, and both are reading the same world.
 * Putting resolution in the interface before any instrument needs it means a tier is one number,
 * rather than a reason to reopen every display.
 */
public interface Instrument {

    /**
     * The truth tier: a resolution of zero, the exact figure the simulation holds. Every
     * {@link Quantity} has it, whether or not any instrument a player can make reaches it, and
     * every display must be able to print it -- so the debug helmet, which answers at this tier
     * for everything, shows the truth rather than whatever the weakest instrument's display was
     * written for (philosophy 8, <em>Every quantity has a truth tier</em>).
     */
    float TRUTH = 0f;

    /** Whether this device measures that quantity at all. */
    boolean canRead(Quantity quantity);

    /**
     * The smallest step this device can distinguish, in the quantity's own units. Zero means it
     * reports the world exactly, which no purchasable instrument should ever do (§8).
     */
    float resolution(Quantity quantity);

    /** What to sign a reading with. Every figure the mod shows names the device that took it. */
    Component label();
}
