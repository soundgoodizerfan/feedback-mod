// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.belt;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * What a belt block is made of, baked at the Tool Table from the band's material and its construction.
 *
 * @param material the band's material (shown, tinted, and the repair stuff)
 * @param friction belt-to-cargo grip: the material's {@code mechanical.friction} times the band's texture, plus a cleat's
 * @param maxTu    the hottest cargo it carries without wear: the material's service temperature times the band's heat tolerance
 * @param cleat    whether a cleat rides on the band (it roughens the face)
 */
public record BeltSpec(ResourceLocation material, float friction, float maxTu, boolean cleat) {

    public static final BeltSpec PLAIN = new BeltSpec(Feedback.id("leather"), 0.6f, 100f, false);

    public CompoundTag write(CompoundTag tag) {
        tag.putString("Material", material.toString());
        tag.putFloat("Friction", friction);
        tag.putFloat("MaxTu", maxTu);
        tag.putBoolean("Cleat", cleat);
        return tag;
    }

    public static BeltSpec read(CompoundTag tag) {
        if (!tag.contains("Material"))
            return PLAIN;
        return new BeltSpec(ResourceLocation.parse(tag.getString("Material")), tag.getFloat("Friction"), tag.getFloat("MaxTu"), tag.getBoolean("Cleat"));
    }
}
