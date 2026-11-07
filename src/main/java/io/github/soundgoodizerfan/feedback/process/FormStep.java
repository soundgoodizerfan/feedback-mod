// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One way a form becomes another: {@code [input form, fussiness, output form]}.
 *
 * <h2>The function is stored, not its results</h2>
 * What this step costs a material is {@code base + slope * hardness}, evaluated per material when
 * {@link FormTable} compiles. Adding a material is one number; adding a step is one file and every
 * material that has both forms picks it up. The shape is affine on purpose. Per blow, a material
 * absorbs {@code St / hardness}, so blows needed go as {@code work * hardness}; a step that was
 * simply {@code k * hardness} would make that go as hardness squared and steel would be
 * unworkable. {@code base} is what keeps hard materials from paying twice.
 *
 * @param from      form consumed. A material without it gets no entry from this step.
 * @param to        form produced. Likewise.
 * @param operation how force must be delivered; also decides whether this compiles to a
 *                  {@link Deformation} or a {@link Removal} ({@link Operation#removesMaterial}).
 * @param finer     the result is the input moved one {@link Cut} finer instead of a fresh item
 *                  (a removal only; {@code to} names the same form). The step stops applying at the finest cut.
 * @param base      work at zero hardness.
 * @param slope     extra work per point of hardness.
 * @param kinds     which {@link Material.Kind}s the step applies to; empty is every kind. Grinding to dust is for brittle kinds only:
 *                  a ductile metal flattens under the rolls, and gets its dust by atomising a melt instead.
 * @param into      the material the result is of, when it is not the material that was worked (a bloom of {@code iron_bloom} becomes an
 *                  ingot of {@code wrought_iron}); absent means the same material.
 * @param byproduct what the work drives out of the piece (slag out of a bloom), thrown clear by the machine or handed to the player;
 *                  empty for most steps.
 */
public record FormStep(String from, String to, Operation operation, boolean finer, float base, float slope, java.util.List<Material.Kind> kinds,
                       java.util.Optional<net.minecraft.resources.ResourceLocation> into, net.minecraft.world.item.ItemStack byproduct) {

    public static final Codec<FormStep> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("from").forGetter(FormStep::from),
            Codec.STRING.fieldOf("to").forGetter(FormStep::to),
            Operation.CODEC.fieldOf("operation").forGetter(FormStep::operation),
            Codec.BOOL.optionalFieldOf("finer", false).forGetter(FormStep::finer),
            Codec.FLOAT.optionalFieldOf("base", 0f).forGetter(FormStep::base),
            Codec.FLOAT.fieldOf("slope").forGetter(FormStep::slope),
            Material.Kind.CODEC.listOf().optionalFieldOf("kinds", java.util.List.of()).forGetter(FormStep::kinds),
            net.minecraft.resources.ResourceLocation.CODEC.optionalFieldOf("into").forGetter(FormStep::into),
            net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.optionalFieldOf("byproduct", net.minecraft.world.item.ItemStack.EMPTY).forGetter(FormStep::byproduct)
    ).apply(instance, FormStep::new));

    /** Whether the step applies to a material of this kind. */
    public boolean appliesTo(Material material) {
        return kinds.isEmpty() || material.kind().map(kinds::contains).orElse(false);
    }

    /** Cumulative Fu this step costs a material of the given hardness. Never below 1. */
    public int work(float hardness) {
        return Math.max(1, Math.round(base + slope * hardness));
    }
}
