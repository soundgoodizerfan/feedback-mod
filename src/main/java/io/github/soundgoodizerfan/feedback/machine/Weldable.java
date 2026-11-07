// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
