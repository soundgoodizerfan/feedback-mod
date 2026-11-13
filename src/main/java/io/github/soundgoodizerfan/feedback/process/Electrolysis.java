// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * What passing current through a liquid does to it: the band at which it comes apart, and the fluids that leave
 * at each electrode per millibucket of the liquid. The electrolyser knows only "run current through whatever liquid
 * has a row here"; the row, matched by fluid tag or id, says what it makes.
 *
 * <p>
 * A row may also name an {@code item} that is the thing actually split (a solute in a solvent): each item gives
 * {@code unitsPerItem} units of work, a unit being what a millibucket of feed is in a row with no item, and a
 * {@code solvent} feed is only the bath and is not used up. A row may need a {@code minTemperature}, and may leave a
 * {@code residue} in the cell (lye behind a splitting brine) per unit of work, or per several units.
 *
 * @param feed         the liquid that is split (or the bath, for a solvent row), by tag or id.
 * @param band         the least band the liquid decomposes at.
 * @param products     the fluids made, each per unit of work, and the electrode each collects at.
 * @param item         the solute that is consumed, if the row has one.
 * @param unitsPerItem units of work one item provides.
 * @param solvent      whether the feed is a bath that is not consumed.
 * @param minTemperature the bath temperature below which the row does not run, Tu.
 * @param residue      the fluid left in the cell per unit of work, if any.
 * @param deposit      the solid that plates out on the cathode, if any: winning a metal from its liquor grows metal on the
 *                     plate, it does not run off it as a melt.
 */
public record Electrolysis(FluidIngredient feed, VoltageBand band, List<Product> products, Optional<Ingredient> item,
                           int unitsPerItem, boolean solvent, float minTemperature, Optional<Residue> residue,
                           Optional<Deposit> deposit) {

    /** One {@code item} plated on the cathode per {@code units} units of work (a nugget is 16 mB of the metal). */
    public record Deposit(ResourceLocation item, int units) {
        public static final Codec<Deposit> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Deposit::item),
                Codec.intRange(1, 100000).optionalFieldOf("units", 16).forGetter(Deposit::units)).apply(i, Deposit::new));

        public net.minecraft.world.item.ItemStack stack() {
            return new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(item));
        }
    }

    /**
     * {@code mb} millibuckets of {@code fluid} left in the cell per {@code per} units of work (1 by default: a brine row leaves its lye
     * unit for unit). A {@code per} above 1 is a small fraction that stays behind while the rest leaves as gas, the way the light
     * hydrogen leaves heavy water behind in a long electrolysis; the cell carries the fractional remainder from tick to tick.
     */
    public record Residue(ResourceLocation fluid, int mb, int per) {
        public static final Codec<Residue> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(Residue::fluid),
                Codec.intRange(1, 1000).fieldOf("mb").forGetter(Residue::mb),
                Codec.intRange(1, 100000).optionalFieldOf("per", 1).forGetter(Residue::per)).apply(i, Residue::new));
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
            VoltageBand.CODEC.fieldOf("band").forGetter(Electrolysis::band),
            Product.CODEC.listOf().optionalFieldOf("products", List.of()).forGetter(Electrolysis::products),
            Ingredient.CODEC_NONEMPTY.optionalFieldOf("item").forGetter(Electrolysis::item),
            Codec.intRange(1, 100000).optionalFieldOf("units_per_item", 1).forGetter(Electrolysis::unitsPerItem),
            Codec.BOOL.optionalFieldOf("solvent", false).forGetter(Electrolysis::solvent),
            Codec.floatRange(0f, 5000f).optionalFieldOf("min_temperature", 0f).forGetter(Electrolysis::minTemperature),
            Residue.CODEC.optionalFieldOf("residue").forGetter(Electrolysis::residue),
            Deposit.CODEC.optionalFieldOf("deposit").forGetter(Electrolysis::deposit)).apply(i, Electrolysis::new));

    /** Whether {@code stack} is the solute this row consumes. */
    public boolean takes(net.minecraft.world.item.ItemStack stack) {
        return item.isPresent() && item.get().test(stack);
    }

    public boolean splits(FluidStack liquid) {
        return feed.test(liquid);
    }
}
