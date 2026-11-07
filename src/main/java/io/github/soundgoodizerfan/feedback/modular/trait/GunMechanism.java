// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
