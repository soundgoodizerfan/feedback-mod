// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * One age ({@code data/<ns>/age/<id>.json}): the materials whose first tool or part enters it (by id, or by
 * {@link Material.Kind} name) and the treatise volumes ({@code "weapons:1"}) whose specimen is that age's work. An age with no materials is
 * entered by an act instead (the machine age: a part cut on a shop machine). Display only: nothing is gated on an age.
 */
public record Age(ResourceLocation id, List<ResourceLocation> materials, List<String> kinds, List<String> volumes) {

    public static final Codec<Age> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(Age::id),
            ResourceLocation.CODEC.listOf().optionalFieldOf("materials", List.of()).forGetter(Age::materials),
            Codec.STRING.listOf().optionalFieldOf("kinds", List.of()).forGetter(Age::kinds),
            Codec.STRING.listOf().optionalFieldOf("volumes", List.of()).forGetter(Age::volumes)
    ).apply(i, Age::new));

    /** The advancement event that names entering this age. */
    public String event() {
        return "progress/age/" + id.getPath();
    }

    public boolean has(ResourceLocation material, Material m) {
        return materials.contains(material) || m.kind().filter(k -> kinds.contains(k.getSerializedName())).isPresent();
    }
}
