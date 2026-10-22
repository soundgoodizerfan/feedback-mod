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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.tether;

import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import net.minecraft.world.entity.Entity;

/**
 * The mass stand-in for a living thing: bounding-box volume times a density, for want of a mass on
 * mobs (plan 5.18). The parry's stagger computes the same figure inline in {@code Parrying#result};
 * this is the shared form, for that site to call too.
 */
public final class BodyMass {

    private BodyMass() {
    }

    /** Stand-in mass in Mu. */
    public static float of(Entity e) {
        return e.getBbWidth() * e.getBbWidth() * e.getBbHeight() * ParryTuning.BBOX_DENSITY;
    }
}
