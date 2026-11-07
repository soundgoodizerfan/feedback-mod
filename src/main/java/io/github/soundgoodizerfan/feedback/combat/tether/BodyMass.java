// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    /**
     * STAND-IN: Mu of armour worn, the armour attribute (what the real plates add up to) times this. No armour
     * item carries a mass yet; when one does, this is the one place to replace.
     */
    public static final float ARMOUR_MU_PER_POINT = 2.5f;

    /** Stand-in mass of the armour {@code e} wears, in Mu. */
    public static float armour(net.minecraft.world.entity.LivingEntity e) {
        return (float) e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) * ARMOUR_MU_PER_POINT;
    }

    /** Stand-in mass in Mu. */
    public static float of(Entity e) {
        return e.getBbWidth() * e.getBbWidth() * e.getBbHeight() * ParryTuning.BBOX_DENSITY;
    }
}
