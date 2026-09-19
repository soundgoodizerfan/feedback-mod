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
package io.github.soundgoodizerfan.feedback.machine.piston;

/**
 * Something a Piston can push against -- the Drill Press's automatic axial feed, and whatever
 * else eventually wants a discrete linear nudge rather than a rotating drive.
 * <p>
 * One method, taking nothing, the same shape {@link io.github.soundgoodizerfan.feedback.machine.bellows.Blown}
 * already uses for the Bellows: the Piston knows nothing about what the push accomplishes, only
 * that one landed. What a push <em>does</em> is entirely the far end's business.
 */
public interface Pushable {

    /** One discrete push landed. */
    void push();
}
