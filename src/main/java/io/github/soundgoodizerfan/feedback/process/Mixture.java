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
 * What a generic mixture is once a machine or a player acts on it: the window each material's
 * fraction of the whole must sit in, and what it becomes. A mixture that matches no row stays a
 * mixture; the table names materials, never an item the machine looks for. One table serves two
 * evaluators: the Mixer and Reloading Press ask it for a drawn mixture (with a temperature), and a
 * crucible's melt ledger asks it for a fluid row ({@link #matchesAmounts}).
 *
 * @param parts          material id to the window its fraction of the whole must sit in.
 * @param stray          the most any material not listed may make up. Zero means nothing else in it.
 * @param result         what it resolves into; empty for a fluid result.
 * @param resultFluid    what it resolves into as a melt, per part (per mB for a crucible melt); empty for an item result.
 * @param minTemperature the mixture must be at or above this. Unset means no floor.
 * @param maxTemperature the mixture must be at or below this. Unset means no ceiling.
 */
public record Mixture(Map<ResourceLocation, Range> parts, float stray, ItemStack result, FluidStack resultFluid,
                      Tu minTemperature, Tu maxTemperature) {

    /** Float slack on a window's edges, so a mix sitting exactly on a boundary is not lost to rounding. */
    private static final float EPSILON = 1e-4f;

    /**
     * One material's acceptable fraction of the whole, as a fraction (0.08-0.12, not 8-12). The
     * ratio-window shape -- a composition defined by acceptable ranges rather than a fixed recipe --
     * is adapted from TerraFirmaCraft's {@code AlloyRange}/{@code AlloyRecipe} (EUPL-1.2, cleared for
     * reading per {@code THIRD-PARTY-LICENSES.md}); the shape of the idea, not the code.
     */
    public record Range(float min, float max) {
        public static final Codec<Range> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0f, 1f).fieldOf("min").forGetter(Range::min),
                Codec.floatRange(0f, 1f).fieldOf("max").forGetter(Range::max)
        ).apply(i, Range::new));

        public boolean isIn(float fraction) {
            return fraction >= min - EPSILON && fraction <= max + EPSILON;
        }
    }

    public static final Codec<Mixture> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Range.CODEC).fieldOf("parts").forGetter(Mixture::parts),
            Codec.floatRange(0f, 1f).optionalFieldOf("stray", 0f).forGetter(Mixture::stray),
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

    /** Whether {@code parts} (counts by material) are this row's windows, at {@code temperature}. */
    public boolean matches(List<Cartridge.Charge> parts, Tu temperature) {
        if (temperature.value() < minTemperature.value() || temperature.value() > maxTemperature.value())
            return false;
        Map<ResourceLocation, Integer> amounts = new java.util.HashMap<>();
        for (var part : parts)
            amounts.merge(part.material(), part.count(), Integer::sum);
        return matchesAmounts(amounts);
    }

    /** Whether {@code amounts} (any common unit, by material) fall in this row's windows; temperature is not asked. */
    public boolean matchesAmounts(Map<ResourceLocation, Integer> amounts) {
        long total = 0;
        for (int n : amounts.values())
            total += n;
        if (total <= 0)
            return false;
        for (var e : amounts.entrySet())
            if (!parts.containsKey(e.getKey()) && e.getValue() / (float) total > stray + EPSILON)
                return false;
        for (var e : parts.entrySet())
            if (!e.getValue().isIn(amounts.getOrDefault(e.getKey(), 0) / (float) total))
                return false;
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
