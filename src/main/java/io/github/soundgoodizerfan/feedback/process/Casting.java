// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * What a filled mold becomes once it has cooled enough to solidify.
 *
 * <p>Never authored: {@link CastingTable} derives one for every melt and every mold shape from the material table, and the client sees each as
 * a freeze row of the Thermal Process card ({@link ThermalProcess#freezing}). It fires on <em>cooling below</em> its threshold, the one
 * direction every melting row runs backwards, and nothing asks why a mold cooled, only what {@link io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat} reads.
 *
 * @param fluid               what the mold must be holding, and how much.
 * @param solidifyTemperature at or below this the mold's contents are solid and may be extracted.
 * @param result              what comes out.
 * @param mold                the shape of mold it is cast in: the same melt makes an ingot in an
 *                            ingot mold and bullets in a bullet mold.
 */
public record Casting(SizedFluidIngredient fluid, Tu solidifyTemperature, ItemStack result, String mold) {

    /** The mold shape a casting written without one is for: the ingot mold. */
    public static final String INGOT_MOLD = "ingot";

    public static final Codec<Casting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SizedFluidIngredient.FLAT_CODEC.fieldOf("fluid").forGetter(Casting::fluid),
            Units.codec(Tu::new).fieldOf("solidify_temperature").forGetter(Casting::solidifyTemperature),
            ItemStack.CODEC.fieldOf("result").forGetter(Casting::result),
            Codec.STRING.optionalFieldOf("mold", INGOT_MOLD).forGetter(Casting::mold)
    ).apply(instance, Casting::new));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Casting> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                    SizedFluidIngredient.STREAM_CODEC, Casting::fluid,
                    Units.streamCodec(Tu::new), Casting::solidifyTemperature,
                    ItemStack.STREAM_CODEC, Casting::result,
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, Casting::mold,
                    Casting::new);

    public boolean matches(FluidStack contents) {
        return fluid.test(contents);
    }

    public boolean isSolidAt(Tu tu) {
        return tu.value() <= solidifyTemperature.value();
    }
}
