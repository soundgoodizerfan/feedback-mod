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
 * A mechanism a gun has whose effect is already in its figures or its fire loop, named so a tooltip can say it
 * and a designation can read it (spec 8.4): the hammer (it lets a cylinder be fanned and a cylinder be fitted),
 * the manual cycle (the action's own time between shots, in the cycle figure), select fire (the fire mode key),
 * the double barrel (both barrels at once when sneaking, in the shot logic) and the closed bolt (a round in the
 * hot chamber can cook off, in the shot logic). One class, many ids: the registry names each.
 */
public class GunMechanism implements ModularTrait {
}
