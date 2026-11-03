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
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a generic mixture is once a machine or a player acts on it: the shares of each material
 * that make it, how far off those shares it may be, and what it becomes. A mixture that matches
 * no row stays a mixture; the table names materials, never an item the machine looks for.
 *
 * @param shares         material id to fraction of the whole; they sum to 1.
 * @param tolerance      how far each share (and any extra material) may stray from the row.
 * @param result         what it resolves into; empty for a fluid result.
 * @param resultFluid    what it resolves into as a melt; empty for an item result.
 * @param minTemperature the mixture must be at or above this. Unset means no floor.
 * @param maxTemperature the mixture must be at or below this. Unset means no ceiling.
 */
public record Mixture(Map<ResourceLocation, Float> shares, float tolerance, ItemStack result, FluidStack resultFluid,
                      Tu minTemperature, Tu maxTemperature) {

    public static final Codec<Mixture> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.floatRange(0f, 1f)).fieldOf("shares").forGetter(Mixture::shares),
            Codec.floatRange(0f, 1f).fieldOf("tolerance").forGetter(Mixture::tolerance),
            ItemStack.CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(Mixture::result),
            FluidStack.OPTIONAL_CODEC.optionalFieldOf("result_fluid", FluidStack.EMPTY).forGetter(Mixture::resultFluid),
            Units.codec(Tu::new).optionalFieldOf("min_temperature", new Tu(-Float.MAX_VALUE)).forGetter(Mixture::minTemperature),
            Units.codec(Tu::new).optionalFieldOf("max_temperature", new Tu(Float.MAX_VALUE)).forGetter(Mixture::maxTemperature)
    ).apply(i, Mixture::new));

    /** A fluid part of a mixture: the material the fluid is and how much of it, mB. */
    public record FluidPart(ResourceLocation material, int mb) {
        public static final Codec<FluidPart> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("material").forGetter(FluidPart::material),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("mb").forGetter(FluidPart::mb)
        ).apply(i, FluidPart::new));

        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, FluidPart> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, FluidPart::material,
                        net.minecraft.network.codec.ByteBufCodecs.VAR_INT, FluidPart::mb,
                        FluidPart::new);

        /** This part counted as whole parts beside the item parts (at least one). */
        public Cartridge.Charge asCharge() {
            return new Cartridge.Charge(material, Math.max(1, Math.min(FTuning.GUN_CHARGE_MAX, Math.round(mb / (float) FTuning.MIXER_MB_PER_PART))));
        }
    }

    /**
     * The material a fluid is: the material filed under the fluid's id, else under its id without a
     * {@code molten_} prefix (molten copper is copper). Empty means the fluid cannot be mixed.
     */
    public static Optional<ResourceLocation> fluidMaterial(Map<ResourceLocation, Material> materials, net.minecraft.world.level.material.Fluid fluid) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
        if (materials.containsKey(id))
            return Optional.of(id);
        if (id.getPath().startsWith("molten_")) {
            ResourceLocation base = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring("molten_".length()));
            if (materials.containsKey(base))
                return Optional.of(base);
        }
        return Optional.empty();
    }

    public boolean hasFluidResult() {
        return !resultFluid.isEmpty();
    }

    /** Whether {@code parts} (counts by material) are this row's shares within tolerance, at {@code temperature}. */
    public boolean matches(List<Cartridge.Charge> parts, Tu temperature) {
        if (temperature.value() < minTemperature.value() || temperature.value() > maxTemperature.value())
            return false;
        int total = parts.stream().mapToInt(Cartridge.Charge::count).sum();
        if (total <= 0)
            return false;
        for (var part : parts)
            if (!shares.containsKey(part.material()) && part.count() / (float) total > tolerance)
                return false;
        for (var share : shares.entrySet()) {
            int count = parts.stream().filter(p -> p.material().equals(share.getKey())).mapToInt(Cartridge.Charge::count).sum();
            if (Math.abs(count / (float) total - share.getValue()) > tolerance)
                return false;
        }
        return true;
    }

    /** The table a machine asks on this side of the wire: the server's, or the browser's copy. */
    public static List<Mixture> table(Level level) {
        return level != null && level.isClientSide ? ClientTable.MIXTURES.get() : MixtureTable.get().entries();
    }

    /** The first row {@code parts} fall under; empty leaves the mixture generic. */
    public static Optional<Mixture> resolve(List<Mixture> table, List<Cartridge.Charge> parts, Tu temperature) {
        return table.stream().filter(m -> m.matches(parts, temperature)).findFirst();
    }
}
