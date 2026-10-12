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
package io.github.soundgoodizerfan.feedback.machine.bellows;

import io.github.soundgoodizerfan.feedback.core.unit.Mb;

/**
 * Something a bellows can blow into.
 *
 * <h2>Air, not heat</h2>
 * The bellows moves air and knows nothing else. It does not raise a temperature, it does not
 * consult a thermometer, and it has no idea whether the thing in front of it is even alight --
 * philosophy 10's separation, arriving as a one-method interface. What air <em>does</em> is
 * entirely the fire's business, which is why a bellows pointed at an unlit firebox politely
 * achieves nothing rather than needing a rule saying so.
 */
public interface Blown {

    /** Take this much air. Anything over the buffer is simply lost to the room. */
    void addAir(Mb air);
}
