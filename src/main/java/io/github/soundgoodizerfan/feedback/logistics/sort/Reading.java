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
package io.github.soundgoodizerfan.feedback.logistics.sort;

/**
 * What the world holds for one unit of cargo, before any detector's weakness is applied. Unknown
 * means no material could place the item (a vanilla item with no entry): detectors then fall to
 * the unexpected-cargo ladder, never to an id.
 *
 * @param magnetic     the material is magnetic at all
 * @param curie        Tu above which it is not (infinity when none stated)
 * @param reflectivity 0..1
 * @param density      Mu per mB
 * @param hardness     St
 * @param friction     sliding coefficient
 * @param kind         the band of its material kind
 * @param temperature  Tu now
 * @param mass         Mu of the whole unit (stack)
 * @param unitMass     Mu of one item
 * @param brittle      a long drop breaks it
 * @param sticky       clings to a nozzle's air (a trait of the material)
 */
public record Reading(boolean unknown, boolean magnetic, float curie, float reflectivity, float density, float hardness,
                      float friction, Band kind, float temperature, float mass, float unitMass, boolean brittle, boolean sticky) {

    static final Reading UNKNOWN = new Reading(true, false, Float.MAX_VALUE, 0.5f, 1f, 1f, 0.5f, Band.OTHER, 0f, 1f, 1f, false, false);
}
