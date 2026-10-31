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
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * What passing current through a liquid does to it: the voltage at which it comes apart, and the fluids that leave
 * at each electrode per millibucket of the liquid. The electrolyser knows only "run current through whatever liquid
 * has a row here"; the row, matched by fluid tag or id, says what it makes.
 *
 * @param feed     the liquid that is split, by tag or id.
 * @param volts    the voltage the liquid decomposes at.
 * @param products the fluids made, each per millibucket of feed, and the electrode each collects at.
 */
public record Electrolysis(FluidIngredient feed, float volts, List<Product> products) {

    public enum Electrode implements StringRepresentable {
        ANODE("anode"), CATHODE("cathode");

        public static final Codec<Electrode> CODEC = StringRepresentable.fromEnum(Electrode::values);
        private final String key;

        Electrode(String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    /** {@code mb} millibuckets of {@code fluid} per millibucket of feed, collected at {@code electrode}. */
    public record Product(ResourceLocation fluid, int mb, Electrode electrode) {
        public static final Codec<Product> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(Product::fluid),
                Codec.intRange(1, 1000).fieldOf("mb").forGetter(Product::mb),
                Electrode.CODEC.fieldOf("electrode").forGetter(Product::electrode)).apply(i, Product::new));
    }

    public static final Codec<Electrolysis> CODEC = RecordCodecBuilder.create(i -> i.group(
            FluidIngredient.CODEC.fieldOf("feed").forGetter(Electrolysis::feed),
            Codec.floatRange(0f, 1000f).fieldOf("volts").forGetter(Electrolysis::volts),
            Product.CODEC.listOf().fieldOf("products").forGetter(Electrolysis::products)).apply(i, Electrolysis::new));

    public boolean splits(FluidStack liquid) {
        return feed.test(liquid);
    }
}
