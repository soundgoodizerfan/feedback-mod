// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * One thing a villager will buy: an item, or any food, for emeralds each, up to a daily cap. A
 * villager wants four to six of the lines in its pool each day (the base pool every villager
 * shares plus its profession's), never everything.
 *
 * @param profession the profession id, or {@link #ANY} for the pool every villager shares
 * @param item       the item bought; {@link Items#AIR} when {@code food} is set
 * @param food       any item carrying a food component
 * @param price      emeralds paid for each
 * @param max        most bought in a day
 * @param regional   price multiplier by villager type (the biome type path); a type absent pays the line's own price
 */
public record Want(ResourceLocation profession, Item item, boolean food, int price, int max, java.util.Map<String, Float> regional) {

    public static final ResourceLocation ANY = ResourceLocation.fromNamespaceAndPath("feedback", "any");

    /** A line as a file writes it. */
    public record Line(Item item, boolean food, int price, int max, java.util.Map<String, Float> regional) {

        public static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item", Items.AIR).forGetter(Line::item),
                Codec.BOOL.optionalFieldOf("food", false).forGetter(Line::food),
                Codec.intRange(1, 64).fieldOf("price").forGetter(Line::price),
                Codec.intRange(1, 64).optionalFieldOf("max", 8).forGetter(Line::max),
                Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("regional", java.util.Map.of()).forGetter(Line::regional)).apply(i, Line::new));
    }

    /** A whole file: one profession's pool. */
    public record File(ResourceLocation profession, List<Line> wants) {

        public static final Codec<File> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("profession").forGetter(File::profession),
                Line.CODEC.listOf().fieldOf("wants").forGetter(File::wants)).apply(i, File::new));
    }

    public static final Codec<Want> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("profession").forGetter(Want::profession),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Want::item),
            Codec.BOOL.fieldOf("food").forGetter(Want::food),
            Codec.INT.fieldOf("price").forGetter(Want::price),
            Codec.INT.fieldOf("max").forGetter(Want::max),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("regional", java.util.Map.of()).forGetter(Want::regional)).apply(i, Want::new));

    /** The price a villager of {@code villagerType} pays: the line's, times its regional lean (kept within the allowed range), at least 1. */
    public int priceFor(String villagerType) {
        Float lean = regional.get(villagerType);
        if (lean == null)
            return price;
        float m = Math.max(VillageTuning.REGIONAL_MIN, Math.min(VillageTuning.REGIONAL_MAX, lean));
        return Math.max(1, Math.round(price * m));
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && (food ? stack.has(DataComponents.FOOD) : stack.is(item));
    }

    /** What to draw for the line: the item, or an apple standing for any food. */
    public ItemStack icon() {
        return new ItemStack(food ? Items.APPLE : item);
    }
}
