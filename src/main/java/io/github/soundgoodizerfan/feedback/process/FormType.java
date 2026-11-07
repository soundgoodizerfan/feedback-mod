// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

/**
 * A form that is generated: {@code data/feedback/form_type/<form>.json}. Any other form name a
 * material lists ({@code horse_armor}, {@code sensor}) is an association only -- it names an item
 * that already exists and generates nothing.
 *
 * @param kinds which {@link Material.Kind}s get this form generated. A plate is a metal thing; a
 *              stone plate would need its own form type, not a wider one.
 * @param block whether the form is a placed block (a storage block) rather than an item.
 * @param mb    volume of one item of this form in millibuckets (an ingot is 144, a nugget 16, a block 1296).
 *              Stock is counted by volume, so nine ingots and one block fill the same shape. Forms that
 *              name no volume are one ingot's worth.
 * @param bearing whether the form is an ore's: generated for every material that {@link Material#bears}
 *              something, whatever its kind, and for no other ({@code kinds} is then ignored).
 * @param ored  with {@code bearing}: also generated for a material that lists an {@code ore} form of its own
 *              (iron's hand-registered ore), so every ore, bearing or not, has its loose rock.
 * @param pressed whether the form is a pressed and sintered slug: generated for a material that lists a {@code green_compact} form
 *              and has no ingot (it cannot be cast, so powder metallurgy is its only road), whatever its kind ({@code kinds} is then ignored).
 * @param grain the form's characteristic size relative to a reference ingot (grain 1): surface per volume goes as {@code 1/grain}, so a
 *              finer form runs a form-agnostic row sooner ({@link #holdFactor}). One number per form type, because a form is shaped by how it
 *              was made. Absent means 1, behaving as before. Invented placeholders, retunable in the files.
 * @param roasted whether the form is a roasted ore (calcine): generated for a non-metal bearer that states an oxidation temperature, a sulfide that burns in air
 *              ({@code bearing} and {@code kinds} are then ignored).
 * @param part  whether the form is a shaped part ({@link io.github.soundgoodizerfan.feedback.modular.ModularPartItem}):
 *              one shared item, the material on the stack ({@link MadeOf}), instead of an item per material.
 */
public record FormType(List<Material.Kind> kinds, boolean block, boolean part, int mb, boolean bearing, boolean ored, boolean pressed, float grain, boolean roasted) {

    /** Volume of an item whose form names none: one ingot of stock, a cell of a shaped part. */
    public static final int DEFAULT_MB = 144;

    public static final Codec<FormType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Material.Kind.CODEC.listOf().fieldOf("kinds").forGetter(FormType::kinds),
            Codec.BOOL.optionalFieldOf("block", false).forGetter(FormType::block),
            Codec.BOOL.optionalFieldOf("part", false).forGetter(FormType::part),
            Codec.INT.optionalFieldOf("mb", DEFAULT_MB).forGetter(FormType::mb),
            Codec.BOOL.optionalFieldOf("bearing", false).forGetter(FormType::bearing),
            Codec.BOOL.optionalFieldOf("ored", false).forGetter(FormType::ored),
            Codec.BOOL.optionalFieldOf("pressed", false).forGetter(FormType::pressed),
            Codec.floatRange(0.001f, 100f).optionalFieldOf("grain", 1f).forGetter(FormType::grain),
            Codec.BOOL.optionalFieldOf("roasted", false).forGetter(FormType::roasted)
    ).apply(instance, FormType::new));

    /** Share of its authored hold a form-agnostic row asks of a form of this {@code grain}: {@code clamp(grain^k, floor, 1)}, never above 1. */
    public static float holdFactor(float grain) {
        return Math.min(1f, Math.max(FTuning.FORM_HOLD_FLOOR, (float) Math.pow(grain, FTuning.FORM_HOLD_EXPONENT)));
    }

    /** The form an item is and that form's grain; {@code form} is empty where the item is no material's. */
    public record Shown(String form, float grain) {
        static final Shown NONE = new Shown("", 1f);
    }

    /**
     * The form {@code stack} is and its grain (1 where it is no material's item or its form names no type). Memoised per item, since a
     * vessel asks every tick and a recipe card every frame; the memo is dropped when the material table is replaced.
     */
    public static Shown shown(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty())
            return Shown.NONE;
        var located = MaterialIngredient.locate(stack.getItem());
        if (MaterialIngredient.materialsNow() != memoFor) {
            MEMO.clear();
            memoFor = MaterialIngredient.materialsNow();
        }
        return MEMO.computeIfAbsent(stack.getItem(), item -> {
            if (located.isEmpty())
                return Shown.NONE;
            FormType type = FormItems.types().get(located.get().form());
            return new Shown(located.get().form(), type == null ? 1f : type.grain());
        });
    }

    /** The grain of the form {@code stack} is; see {@link #shown}. */
    public static float grainOf(net.minecraft.world.item.ItemStack stack) {
        return shown(stack).grain();
    }

    private static final java.util.Map<net.minecraft.world.item.Item, Shown> MEMO = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile java.util.Map<net.minecraft.resources.ResourceLocation, Material> memoFor = java.util.Map.of();

    /** Forms drawn as grey overlay on stone and tinted by the material: an ore, and the loose rock that shows it. */
    public static boolean overlaid(String form) {
        return form.endsWith("ore") || form.equals("loose_rock");
    }

    public boolean appliesTo(Material material) {
        if (pressed)
            return material.forms().containsKey("green_compact") && material.exclude().contains("ingot");
        // A roasted form (calcine) is for the ore that burns in air: a non-metal bearer with an oxidation temperature (a sulfide).
        if (roasted)
            return !material.bears().isEmpty() && material.thermal().oxidationTemperature().isPresent()
                    && material.kind().map(k -> k != Material.Kind.METAL).orElse(false);
        if (bearing)
            return !material.bears().isEmpty() || ored && material.forms().containsKey("ore");
        return material.kind().map(kinds::contains).orElse(false);
    }
}
