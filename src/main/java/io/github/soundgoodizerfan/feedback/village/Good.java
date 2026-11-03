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
 */
public record Good(ResourceLocation profession, int level, Item item, int count, int price, int stock, int standing,
                   boolean enchantedBook, java.util.Optional<ResourceLocation> blueprint) {

    /** A line as a file writes it, under its profession. */
    public record Line(int level, Item item, int count, int price, int stock, int standing, boolean enchantedBook,
                       java.util.Optional<ResourceLocation> blueprint) {

        public static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 5).optionalFieldOf("level", 1).forGetter(Line::level),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Line::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Line::count),
                Codec.intRange(1, 64).fieldOf("price").forGetter(Line::price),
                Codec.intRange(1, 64).optionalFieldOf("stock", 4).forGetter(Line::stock),
                Codec.INT.optionalFieldOf("standing", 0).forGetter(Line::standing),
                Codec.BOOL.optionalFieldOf("enchanted_book", false).forGetter(Line::enchantedBook),
                ResourceLocation.CODEC.optionalFieldOf("blueprint").forGetter(Line::blueprint)).apply(i, Line::new));
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
            ResourceLocation.CODEC.optionalFieldOf("blueprint").forGetter(Good::blueprint)).apply(i, Good::new));

    public ItemStack stack() {
        return new ItemStack(item, count);
    }
}
