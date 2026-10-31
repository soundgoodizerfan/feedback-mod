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
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * What passing current through a liquid does to it: the voltage at which it comes apart, and the fluids that leave
 * at each electrode per millibucket of the liquid. The electrolyser knows only "run current through whatever liquid
 * has a row here"; the row, matched by fluid tag or id, says what it makes.
 *
 * <p>
 * A row may also name an {@code item} that is the thing actually split (a solute in a solvent): each item gives
 * {@code unitsPerItem} units of work, a unit being what a millibucket of feed is in a row with no item, and a
 * {@code solvent} feed is only the bath and is not used up. A row may need a {@code minTemperature}, and may leave a
 * {@code residue} in the cell (lye behind a splitting brine) per unit of work.
 *
 * @param feed         the liquid that is split (or the bath, for a solvent row), by tag or id.
 * @param volts        the voltage the liquid decomposes at.
 * @param products     the fluids made, each per unit of work, and the electrode each collects at.
 * @param item         the solute that is consumed, if the row has one.
 * @param unitsPerItem units of work one item provides.
 * @param solvent      whether the feed is a bath that is not consumed.
 * @param minTemperature the bath temperature below which the row does not run, Tu.
 * @param residue      the fluid left in the cell per unit of work, if any.
 */
public record Electrolysis(FluidIngredient feed, float volts, List<Product> products, Optional<Ingredient> item,
                           int unitsPerItem, boolean solvent, float minTemperature, Optional<Residue> residue) {

    /** {@code mb} millibuckets of {@code fluid} left in the cell per unit of work. */
    public record Residue(ResourceLocation fluid, int mb) {
        public static final Codec<Residue> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(Residue::fluid),
                Codec.intRange(1, 1000).fieldOf("mb").forGetter(Residue::mb)).apply(i, Residue::new));
    }

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
            Product.CODEC.listOf().fieldOf("products").forGetter(Electrolysis::products),
            Ingredient.CODEC_NONEMPTY.optionalFieldOf("item").forGetter(Electrolysis::item),
            Codec.intRange(1, 100000).optionalFieldOf("units_per_item", 1).forGetter(Electrolysis::unitsPerItem),
            Codec.BOOL.optionalFieldOf("solvent", false).forGetter(Electrolysis::solvent),
            Codec.floatRange(0f, 5000f).optionalFieldOf("min_temperature", 0f).forGetter(Electrolysis::minTemperature),
            Residue.CODEC.optionalFieldOf("residue").forGetter(Electrolysis::residue)).apply(i, Electrolysis::new));

    /** Whether {@code stack} is the solute this row consumes. */
    public boolean takes(net.minecraft.world.item.ItemStack stack) {
        return item.isPresent() && item.get().test(stack);
    }

    public boolean splits(FluidStack liquid) {
        return feed.test(liquid);
    }
}
