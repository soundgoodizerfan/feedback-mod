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
package io.github.soundgoodizerfan.feedback.machine;

import net.minecraft.resources.ResourceLocation;

/**
 * A worn machine the hand arc welder ({@code item/ArcWelderItem}) can mend by right-clicking it.
 * <p>
 * The condition a weld restores is only ever the part's own current maximum, and each weld lowers that
 * maximum (a repaired part is never as good as new). Wear code needs no clamp: condition only falls
 * from where a weld left it.
 */
public interface Weldable extends Wearing {

    /** The most condition a weld can restore now, 1 as built. */
    float maxCondition();

    /** Set the condition and the new maximum a weld leaves. */
    void weld(float newCondition, float newMax);

    /** The material the housing is made of, which the filler metal must match; null when the machine carries none (iron). */
    default ResourceLocation fillerMaterial() {
        return null;
    }
}
