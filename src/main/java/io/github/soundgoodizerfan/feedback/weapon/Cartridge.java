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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.process.Material;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A loaded round ({@code feedback_guns_spec.md} section 3.1): a case, a charge and a projectile,
 * each a <em>material</em> and none an item id. A round is never "a 9 mm"; it is this record, and
 * a chamber accepts it by fit (the bore), so a pack's round fits a gun it was never listed in.
 *
 * <p>The charge is a <em>list</em> of materials with counts: a blend. Nothing mixes them but the
 * press -- there is no mixer recipe, because the mixing machine does not exist yet (see {@code
 * TODO.md}: a mixer recipe type should take over what the press's two charge slots do by
 * hand). What a blend does is read off the materials' own figures in {@link #load}.
 *
 * @param boreMm       the die set the press seated it in; a gun takes only its own bore.
 * @param caseMaterial the metal it was drawn from.
 * @param charges      what was seated as the charge, with how many of each: at most two kinds.
 * @param projectile   the metal of the bullet.
 * @param payload      something carried in the bullet, read by what it is: a luminous material
 *                     makes it a tracer, a flammable one makes it incendiary.
 */
public record Cartridge(int boreMm, ResourceLocation caseMaterial, List<Charge> charges, ResourceLocation projectile,
                        Optional<ResourceLocation> payload) {

    /** One component of a charge. */
    public record Charge(ResourceLocation material, int count) {
        static final Codec<Charge> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("material").forGetter(Charge::material),
                Codec.intRange(1, FTuning.GUN_CHARGE_MAX).fieldOf("count").forGetter(Charge::count)
        ).apply(i, Charge::new));

        static final StreamCodec<RegistryFriendlyByteBuf, Charge> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Charge::material,
                ByteBufCodecs.VAR_INT, Charge::count,
                Charge::new);
    }

    /** The shape a round had before blends: one propellant and a count. Still read, never written. */
    private static final Codec<Cartridge> LEGACY = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(GunDesign.MIN_BORE_MM, GunDesign.MAX_BORE_MM).fieldOf("bore").forGetter(Cartridge::boreMm),
            ResourceLocation.CODEC.fieldOf("case").forGetter(Cartridge::caseMaterial),
            ResourceLocation.CODEC.fieldOf("propellant").forGetter(Cartridge::propellant),
            Codec.intRange(1, FTuning.GUN_CHARGE_MAX).fieldOf("charge").forGetter(Cartridge::charge),
            ResourceLocation.CODEC.fieldOf("projectile").forGetter(Cartridge::projectile)
    ).apply(i, Cartridge::new));

    public static final Codec<Cartridge> CODEC = Codec.withAlternative(RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(GunDesign.MIN_BORE_MM, GunDesign.MAX_BORE_MM).fieldOf("bore").forGetter(Cartridge::boreMm),
            ResourceLocation.CODEC.fieldOf("case").forGetter(Cartridge::caseMaterial),
            Charge.CODEC.listOf().fieldOf("charges").forGetter(Cartridge::charges),
            ResourceLocation.CODEC.fieldOf("projectile").forGetter(Cartridge::projectile),
            ResourceLocation.CODEC.optionalFieldOf("payload").forGetter(Cartridge::payload)
    ).apply(i, Cartridge::new)), LEGACY);

    public static final StreamCodec<RegistryFriendlyByteBuf, Cartridge> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Cartridge::boreMm,
            ResourceLocation.STREAM_CODEC, Cartridge::caseMaterial,
            Charge.STREAM_CODEC.apply(ByteBufCodecs.list()), Cartridge::charges,
            ResourceLocation.STREAM_CODEC, Cartridge::projectile,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Cartridge::payload,
            Cartridge::new);

    public Cartridge {
        charges = List.copyOf(charges);
    }

    /** A single-propellant round with no payload: what every round was before blends. */
    public Cartridge(int boreMm, ResourceLocation caseMaterial, ResourceLocation propellant, int charge,
                     ResourceLocation projectile) {
        this(boreMm, caseMaterial, List.of(new Charge(propellant, charge)), projectile, Optional.empty());
    }

    /** Items seated as the charge, of every kind. */
    public int charge() {
        return charges.stream().mapToInt(Charge::count).sum();
    }

    /** The material most of the charge is: what a tooltip and a card name it by. */
    public ResourceLocation propellant() {
        return charges.stream().max(java.util.Comparator.comparingInt(Charge::count)).orElseThrow().material();
    }

    /**
     * A round for a creative tab and tests: brass case, nominal gunpowder, lead bullet, in
     * {@code boreMm}. Names three materials, which is what a round is; no item is named.
     */
    public static Cartridge reference(int boreMm) {
        return new Cartridge(boreMm, ResourceLocation.fromNamespaceAndPath("feedback", "brass"),
                ResourceLocation.fromNamespaceAndPath("feedback", "gunpowder"), FTuning.GUN_CHARGE_NOMINAL,
                ResourceLocation.fromNamespaceAndPath("feedback", "lead"));
    }

    /** A slug for a coil: an iron bullet (it must couple to the field) on a nominal charge of redstone. */
    public static Cartridge referenceCoil(int boreMm) {
        return new Cartridge(boreMm, ResourceLocation.fromNamespaceAndPath("feedback", "brass"),
                ResourceLocation.fromNamespaceAndPath("feedback", "redstone"), FTuning.GUN_CHARGE_NOMINAL,
                ResourceLocation.fromNamespaceAndPath("feedback", "iron"));
    }

    /**
     * What this round asks of a chamber (or a winding), from its charge's figures and how much of
     * each was seated.
     *
     * <p><b>Chemical blends.</b> Each item brings its propellant's pressure potential, and an
     * oxygen balance: gunpowder brings a surplus, blaze powder is fuel that needs some. The blend
     * burns completely when the sum is not negative; short of that the fraction burned falls
     * (never to nothing -- a fizzle floor). So blaze powder alone barely does anything, and a
     * blend near the balance is the hottest and fastest charge there is, with the lowest ignition
     * temperature of what is in it. Case room caps the total, so more fuel is less oxidizer.
     *
     * <p><b>Coil.</b> A redstone charge is a field, not a gas: the pressure it makes goes as the
     * square of the drive (magnetic pressure is a pressure), and it cannot be blended with a
     * chemical charge.
     */
    public Optional<GunDesign.Load> load(Map<ResourceLocation, Material> materials) {
        if (charges.isEmpty())
            return Optional.empty();
        float potential = 0, burnWeighted = 0, balance = 0, ignition = Float.MAX_VALUE;
        int total = 0;
        boolean coil = false, chemical = false;
        for (Charge c : charges) {
            Material m = materials.get(c.material());
            if (m == null || m.propellant().isEmpty())
                return Optional.empty();
            var p = m.propellant().get();
            if (p.drive() == Material.Propellant.Drive.COIL)
                coil = true;
            else
                chemical = true;
            potential += c.count() * p.peakPressure().value() / FTuning.GUN_CHARGE_NOMINAL;
            burnWeighted += c.count() * p.burnRate();
            balance += c.count() * p.oxygenBalance();
            ignition = Math.min(ignition, p.ignitionTemperature().value());
            total += c.count();
        }
        if (coil && (chemical || charges.size() > 1))
            return Optional.empty();
        if (coil) {
            float drive = total / (float) FTuning.GUN_CHARGE_NOMINAL;
            return Optional.of(new GunDesign.Load(potential * drive, 1f, ignition, true, total));
        }
        float burned = balance >= 0 ? 1f : Math.max(FTuning.GUN_FIZZLE_FLOOR, 1f + balance / total);
        return Optional.of(new GunDesign.Load(potential * burned, burnWeighted / total, ignition, false, total));
    }

    /**
     * What the case itself holds: the chamber's hoop-stress rule with the case drawn to scale
     * with its bore, so the bore cancels ({@link FTuning#GUN_CASE_WALL_PER_RADIUS}).
     */
    public Optional<Pu> caseRating(Map<ResourceLocation, Material> materials) {
        Material c = materials.get(caseMaterial);
        if (c == null || c.mechanical().yieldStrength().isEmpty())
            return Optional.empty();
        return Optional.of(new Pu(c.mechanical().yieldStrength().get().value() * FTuning.GUN_CASE_WALL_PER_RADIUS));
    }

    /** Whether the bullet carries a luminous payload: a tracer. By trait, never by item. */
    public boolean tracer(Map<ResourceLocation, Material> materials) {
        return payload.map(materials::get).map(m -> m.traits().contains(Material.Trait.LUMINOUS)).orElse(false);
    }

    /** Whether the bullet carries a flammable payload: it sets what it hits alight. */
    public boolean incendiary(Map<ResourceLocation, Material> materials) {
        return payload.map(materials::get).map(m -> m.traits().contains(Material.Trait.FLAMMABLE)).orElse(false);
    }
}
