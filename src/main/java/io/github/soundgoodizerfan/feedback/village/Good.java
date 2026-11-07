// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One line of a profession's shop: what is sold, from which trade level, for how many emeralds,
 * how many a day, and how much standing the villager wants before selling it.
 *
 * @param enchantedBook the stack is a book carrying one enchantment the villager picks for the day
 * @param blueprint     the stack is a blueprint of this design: the id of a gun blueprint pool entry, else of a modular type (its starter design)
 * @param guildTier     the least guild tier the villager's guild must have for this line to be sold (0: always)
 * @param types         villager types (biome: {@code desert}, {@code taiga}) that sell the line; empty means every type
 * @param calibrated    an instrument sold as a reference standard: it reads at the truth tier
 * @param weekly        sold at most once in {@link VillageTuning#WEEK_DAYS} days to a villager, whatever its day stock
 */
public record Good(ResourceLocation profession, int level, Item item, int count, int price, int stock, int standing,
                   boolean enchantedBook, java.util.Optional<ResourceLocation> blueprint, int guildTier, java.util.List<String> types,
                   boolean calibrated, boolean weekly) {

    /** A line as a file writes it, under its profession. */
    public record Line(int level, Item item, int count, int price, int stock, int standing, boolean enchantedBook,
                       java.util.Optional<ResourceLocation> blueprint, int guildTier, java.util.List<String> types,
                       boolean calibrated, boolean weekly) {

        public static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 5).optionalFieldOf("level", 1).forGetter(Line::level),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Line::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Line::count),
                Codec.intRange(1, 64).fieldOf("price").forGetter(Line::price),
                Codec.intRange(1, 64).optionalFieldOf("stock", 4).forGetter(Line::stock),
                Codec.INT.optionalFieldOf("standing", 0).forGetter(Line::standing),
                Codec.BOOL.optionalFieldOf("enchanted_book", false).forGetter(Line::enchantedBook),
                ResourceLocation.CODEC.optionalFieldOf("blueprint").forGetter(Line::blueprint),
                Codec.intRange(0, 3).optionalFieldOf("guild_tier", 0).forGetter(Line::guildTier),
                Codec.STRING.listOf().optionalFieldOf("types", java.util.List.of()).forGetter(Line::types),
                Codec.BOOL.optionalFieldOf("calibrated", false).forGetter(Line::calibrated),
                Codec.BOOL.optionalFieldOf("weekly", false).forGetter(Line::weekly)).apply(i, Line::new));
    }

    /** A whole shop file. */
    public record File(ResourceLocation profession, java.util.List<Line> goods) {

        public static final Codec<File> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("profession").forGetter(File::profession),
                Line.CODEC.listOf().fieldOf("goods").forGetter(File::goods)).apply(i, File::new));
    }

    public static final Codec<Good> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("profession").forGetter(Good::profession),
            Codec.INT.fieldOf("level").forGetter(Good::level),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Good::item),
            Codec.INT.fieldOf("count").forGetter(Good::count),
            Codec.INT.fieldOf("price").forGetter(Good::price),
            Codec.INT.fieldOf("stock").forGetter(Good::stock),
            Codec.INT.fieldOf("standing").forGetter(Good::standing),
            Codec.BOOL.fieldOf("enchanted_book").forGetter(Good::enchantedBook),
            ResourceLocation.CODEC.optionalFieldOf("blueprint").forGetter(Good::blueprint),
            Codec.INT.optionalFieldOf("guild_tier", 0).forGetter(Good::guildTier),
            Codec.STRING.listOf().optionalFieldOf("types", java.util.List.of()).forGetter(Good::types),
            Codec.BOOL.optionalFieldOf("calibrated", false).forGetter(Good::calibrated),
            Codec.BOOL.optionalFieldOf("weekly", false).forGetter(Good::weekly)).apply(i, Good::new));

    public ItemStack stack() {
        ItemStack s = new ItemStack(item, count);
        if (calibrated)
            s.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CALIBRATED.get(), true);
        return s;
    }
}
