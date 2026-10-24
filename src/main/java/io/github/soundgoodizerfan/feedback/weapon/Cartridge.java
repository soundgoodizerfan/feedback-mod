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
 * A loaded round ({@code docs/feedback_guns_spec.md} section 3.1): a case, a charge and a projectile,
 * each a <em>material</em> and none an item id. A round is never "a 9 mm"; it is this record, and
 * a chamber accepts it by fit (the bore), so a pack's round fits a gun it was never listed in.
 *
 * <p>The charge is a <em>list</em> of materials with counts: a blend. The press seats the ratio it
 * is given, or a batch the Mixer made ({@code machine/mixer}). What a blend does is read off the materials' own figures in {@link #load}.
 *
 * @param boreMm       the die set the press seated it in; a gun takes only its own bore.
 * @param caseMaterial the metal it was drawn from.
 * @param charges      what was seated as the charge, with how many of each: at most two kinds.
 * @param projectile   the metal of the bullet.
 * @param payload      something carried in the bullet, read by what it is: a luminous material
 *                     makes it a tracer, a flammable one makes it incendiary.
 * @param shot         seated as shot, not a bullet: a shell, for a shotgun (spec 12). The press
 *                     makes shot of a nugget and a bullet of a bullet; it is what was put in.
 * @param stored       a round seated with no charge is a small jar (mechanics 6.6): its drive is
 *                     the volts it was charged to, read through {@link #at}, and it leaks wherever
 *                     it is. Present only on such a round. A coil fires it; a chamber does not.
 */
public record Cartridge(int boreMm, ResourceLocation caseMaterial, List<Charge> charges, ResourceLocation projectile,
                        Optional<ResourceLocation> payload, boolean shot, Optional<Stored> stored) {

    /**
     * Volts on a charged round as of {@code tick}: a stamp and a tick, the shape of a workpiece's
     * heat, so a round goes flat in a chest because nothing has to remember to drain it.
     */
    public record Stored(float volts, long tick) {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("volts").forGetter(Stored::volts),
                Codec.LONG.fieldOf("tick").forGetter(Stored::tick)
        ).apply(i, Stored::new));

        static final StreamCodec<RegistryFriendlyByteBuf, Stored> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Stored::volts,
                ByteBufCodecs.VAR_LONG, Stored::tick,
                Stored::new);

        /** Volts left at {@code now}, leaked linearly ({@link FTuning#GUN_ROUND_LEAK_VOLTS_PER_TICK}). */
        public float voltsAt(long now) {
            return Math.max(0, volts - Math.max(0, now - tick) * FTuning.GUN_ROUND_LEAK_VOLTS_PER_TICK);
        }
    }

    /** One component of a charge. */
    public record Charge(ResourceLocation material, int count) {
        public static final Codec<Charge> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("material").forGetter(Charge::material),
                Codec.intRange(1, FTuning.GUN_CHARGE_MAX).fieldOf("count").forGetter(Charge::count)
        ).apply(i, Charge::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Charge> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Charge::material,
                ByteBufCodecs.VAR_INT, Charge::count,
                Charge::new);
    }

    /** The shape a round had before blends: one propellant and a count. Still read, never written. */
    private static final Codec<Cartridge> LEGACY = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(GunSpec.MIN_BORE_MM, GunSpec.MAX_BORE_MM).fieldOf("bore").forGetter(Cartridge::boreMm),
            ResourceLocation.CODEC.fieldOf("case").forGetter(Cartridge::caseMaterial),
            ResourceLocation.CODEC.fieldOf("propellant").forGetter(Cartridge::propellant),
            Codec.intRange(1, FTuning.GUN_CHARGE_MAX).fieldOf("charge").forGetter(Cartridge::charge),
            ResourceLocation.CODEC.fieldOf("projectile").forGetter(Cartridge::projectile)
    ).apply(i, Cartridge::new));

    public static final Codec<Cartridge> CODEC = Codec.withAlternative(RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(GunSpec.MIN_BORE_MM, GunSpec.MAX_BORE_MM).fieldOf("bore").forGetter(Cartridge::boreMm),
            ResourceLocation.CODEC.fieldOf("case").forGetter(Cartridge::caseMaterial),
            Charge.CODEC.listOf().fieldOf("charges").forGetter(Cartridge::charges),
            ResourceLocation.CODEC.fieldOf("projectile").forGetter(Cartridge::projectile),
            ResourceLocation.CODEC.optionalFieldOf("payload").forGetter(Cartridge::payload),
            Codec.BOOL.optionalFieldOf("shot", false).forGetter(Cartridge::shot),
            Stored.CODEC.optionalFieldOf("stored").forGetter(Cartridge::stored)
    ).apply(i, Cartridge::new)), LEGACY);

    /** Written out by hand: seven fields is past what {@code composite} takes. */
    public static final StreamCodec<RegistryFriendlyByteBuf, Cartridge> STREAM_CODEC = StreamCodec.of(
            (buf, c) -> {
                ByteBufCodecs.VAR_INT.encode(buf, c.boreMm());
                ResourceLocation.STREAM_CODEC.encode(buf, c.caseMaterial());
                Charge.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, c.charges());
                ResourceLocation.STREAM_CODEC.encode(buf, c.projectile());
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, c.payload());
                ByteBufCodecs.BOOL.encode(buf, c.shot());
                ByteBufCodecs.optional(Stored.STREAM_CODEC).encode(buf, c.stored());
            },
            buf -> new Cartridge(ByteBufCodecs.VAR_INT.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf),
                    Charge.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf), ResourceLocation.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf), ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.optional(Stored.STREAM_CODEC).decode(buf)));

    public Cartridge {
        charges = List.copyOf(charges);
    }

    /** A round with no stored charge: every round before charged ones. */
    public Cartridge(int boreMm, ResourceLocation caseMaterial, List<Charge> charges, ResourceLocation projectile,
                     Optional<ResourceLocation> payload, boolean shot) {
        this(boreMm, caseMaterial, charges, projectile, payload, shot, Optional.empty());
    }

    /** Whether a coil drives this round (a jar round or a redstone charge); false for a chemical one or one that loads nowhere. */
    public boolean coilDriven(Map<ResourceLocation, Material> materials) {
        return load(materials).map(GunSpec.Load::coil).orElse(false);
    }

    /** Whether this is a jar round: seated dry, driven by what it was charged to. */
    public boolean charged() {
        return stored.isPresent();
    }

    /** This round as it is at {@code now}: a charged one's volts leaked down and restamped. */
    public Cartridge at(long now) {
        return stored.map(s -> withVolts(s.voltsAt(now), now)).orElse(this);
    }

    /** This jar round holding {@code volts} as of {@code now}. */
    public Cartridge withVolts(float volts, long now) {
        return new Cartridge(boreMm, caseMaterial, charges, projectile, payload, shot,
                Optional.of(new Stored(Math.max(0, volts), now)));
    }

    /** Volts a jar round holds as of its own stamp; zero for any other. Read {@link #at} first. */
    public float volts() {
        return stored.map(Stored::volts).orElse(0f);
    }

    /** A bullet: what every round was before shot. */
    public Cartridge(int boreMm, ResourceLocation caseMaterial, List<Charge> charges, ResourceLocation projectile,
                     Optional<ResourceLocation> payload) {
        this(boreMm, caseMaterial, charges, projectile, payload, false);
    }

    /**
     * Pellets of shot the shell throws: the bore's projectile volume cut into buckshot of a fixed
     * size, so a bigger bore throws more, never fewer than one nor more than a cap. One for a bullet.
     */
    public int pellets() {
        if (!shot)
            return 1;
        double boreVolume = Math.PI / 4 * boreMm * boreMm * boreMm * FTuning.GUN_PROJECTILE_LENGTH_PER_BORE;
        double pellet = Math.PI / 6 * Math.pow(FTuning.GUN_SHOT_PELLET_MM, 3);
        return Math.max(1, Math.min(FTuning.GUN_SHOT_MAX_PELLETS, (int) Math.round(boreVolume / pellet)));
    }

    /** A shell of reference shot in {@code boreMm}: brass, nominal gunpowder, lead shot. */
    public static Cartridge referenceShot(int boreMm) {
        Cartridge r = reference(boreMm);
        return new Cartridge(boreMm, r.caseMaterial(), r.charges(), r.projectile(), Optional.empty(), true);
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

    /** The material most of the charge is: what a tooltip and a card name it by. A jar round's is its winding's drive, redstone. */
    public ResourceLocation propellant() {
        return charges.stream().max(java.util.Comparator.comparingInt(Charge::count)).map(Charge::material)
                .orElse(ResourceLocation.fromNamespaceAndPath("feedback", "redstone"));
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
     *
     * <p><b>Charged.</b> A jar round drives a coil by its volts against {@link
     * FTuning#GUN_COIL_NOMINAL_VOLTS}, the same square law as dust: a round charged past nominal
     * presses the winding past what nominal dust does, and one left to go flat barely pushes. The
     * quench is the winding's, which is redstone whatever charged it. Call on a round {@link #at}
     * the moment it is fired; this reads the volts as stamped.
     */
    public Optional<GunSpec.Load> load(Map<ResourceLocation, Material> materials) {
        if (stored.isPresent()) {
            if (!charges.isEmpty())
                return Optional.empty();
            float drive = volts() / FTuning.GUN_COIL_NOMINAL_VOLTS;
            GunSpec.Load ref = GunSpec.Load.REFERENCE_COIL;
            return Optional.of(new GunSpec.Load(ref.peakPressure() * drive * drive, 1f, ref.ignitionTu(), GunSpec.Launcher.COIL,
                    drive * FTuning.GUN_CHARGE_NOMINAL));
        }
        Optional<Blend> blend = Blend.of(materials, charges);
        if (blend.isEmpty())
            return Optional.empty();
        Blend b = blend.get();
        if (b.coil()) {
            float drive = b.total() / (float) FTuning.GUN_CHARGE_NOMINAL;
            return Optional.of(new GunSpec.Load(b.potential() * drive, 1f, b.ignitionTu(), GunSpec.Launcher.COIL, b.total()));
        }
        return Optional.of(new GunSpec.Load(b.potential() * b.burned(), b.burnRate(), b.ignitionTu(), GunSpec.Launcher.CHAMBER, b.total()));
    }

    /**
     * What a pile of propellant items is as one charge: each item's pressure potential, burn rate and
     * oxygen balance summed by count (the blend rule {@link #load} describes), shared by a round and by
     * a demolition charge, which burns the same stuff in a block instead of a case.
     *
     * @param potential  pressure potential, in nominal charges' worth of the reference.
     * @param burned     the fraction that burns: 1 at a balanced or oxidizer-rich blend, falling to a floor as it runs fuel-rich.
     * @param burnRate   the count-weighted burn rate.
     * @param coil       whether the charge is a field's (redstone) and not a gas's.
     * @param chemical   whether any of it is a gas's.
     */
    public record Blend(float potential, float burned, float burnRate, float ignitionTu, int total, boolean coil, boolean chemical) {
        /** Empty for no charge, an item with no propellant, or a coil mixed with a chemical one. */
        public static Optional<Blend> of(Map<ResourceLocation, Material> materials, List<Charge> charges) {
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
            if (coil && (chemical || charges.size() > 1) || total <= 0)
                return Optional.empty();
            float burned = balance >= 0 ? 1f : Math.max(FTuning.GUN_FIZZLE_FLOOR, 1f + balance / total);
            return Optional.of(new Blend(potential, burned, burnWeighted / total, ignition, total, coil, chemical));
        }
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

    /**
     * The explosion this round makes where it stops, or 0: a payload of a propellant that carries its
     * own oxidizer (a non-negative oxygen balance -- explosive filler, guns spec 13.4a) goes up on impact.
     * The same blend rule a demolition charge uses, of one item's worth scaled to the bore's volume,
     * so a bigger round carries more.
     */
    public float blast(Map<ResourceLocation, Material> materials) {
        float power = payload.flatMap(p -> Blend.of(materials, List.of(new Charge(p, 1))))
                .filter(b -> !b.coil() && b.burned() >= 1f)
                .map(b -> FTuning.DEMO_POWER_PER_SQRT * FTuning.DEMO_ROUND_SCALE * (float) Math.sqrt(b.potential()
                        * Math.pow(boreMm / 9.0, 3)))
                .orElse(0f);
        return Math.min(FTuning.DEMO_POWER_MAX, power);
    }

    /** Whether the bullet carries a flammable payload: it sets what it hits alight. */
    public boolean incendiary(Map<ResourceLocation, Material> materials) {
        return payload.map(materials::get).map(m -> m.traits().contains(Material.Trait.FLAMMABLE)).orElse(false);
    }
}
