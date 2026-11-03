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
 */
public record Want(ResourceLocation profession, Item item, boolean food, int price, int max) {

    public static final ResourceLocation ANY = ResourceLocation.fromNamespaceAndPath("feedback", "any");

    /** A line as a file writes it. */
    public record Line(Item item, boolean food, int price, int max) {

        public static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item", Items.AIR).forGetter(Line::item),
                Codec.BOOL.optionalFieldOf("food", false).forGetter(Line::food),
                Codec.intRange(1, 64).fieldOf("price").forGetter(Line::price),
                Codec.intRange(1, 64).optionalFieldOf("max", 8).forGetter(Line::max)).apply(i, Line::new));
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
            Codec.INT.fieldOf("max").forGetter(Want::max)).apply(i, Want::new));

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && (food ? stack.has(DataComponents.FOOD) : stack.is(item));
    }

    /** What to draw for the line: the item, or an apple standing for any food. */
    public ItemStack icon() {
        return new ItemStack(food ? Items.APPLE : item);
    }
}
