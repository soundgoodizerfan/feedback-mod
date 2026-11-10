// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
 * A loaded round ({@code docs/specs/feedback_guns_spec.md} section 3.1): a case, a charge and a projectile,
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
 * @param shape        what the projectile is shaped like ({@link BulletShape}); a shot shell's is a shot shape.
 * @param core         a bullet's core, when the bullet is a jacket (the {@code projectile} material) round a harder
 *                     or denser metal: the core sets the mass's weight and the hardness armour meets, the jacket
 *                     keeps the bore from it. A polymer jacket round a core is a sabot.
 * @param vial         a potion carried in a glass vial in the bullet instead of a dust: it is let go where the round lands, as a thrown
 *                     flask lets it go, a smaller reach. Its strength fades as the potion's does, from its own stamp.
 * @param seed         the item id of a seed carried in the bullet: it is planted where the round lands on farmland. An item, not a material,
 *                     because the thing planted is the block that item places; the press takes any item that places a crop.
 */
public record Cartridge(int boreMm, ResourceLocation caseMaterial, List<Charge> charges, ResourceLocation projectile,
                        Optional<ResourceLocation> payload, boolean shot, BulletShape shape,
                        Optional<ResourceLocation> core, Optional<io.github.soundgoodizerfan.feedback.potion.PotionState> vial,
                        Optional<ResourceLocation> seed) {

    /** One component of a charge. */
    public record Charge(ResourceLocation material, int count) {
        public static final Codec<Charge> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("material").forGetter(Charge::material),
                Codec.intRange(1, FTuning.GUN_CHARGE_CEILING).fieldOf("count").forGetter(Charge::count)
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
            Codec.intRange(1, FTuning.GUN_CHARGE_CEILING).fieldOf("charge").forGetter(Cartridge::charge),
            ResourceLocation.CODEC.fieldOf("projectile").forGetter(Cartridge::projectile)
    ).apply(i, Cartridge::new));

    public static final Codec<Cartridge> CODEC = Codec.withAlternative(RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(GunSpec.MIN_BORE_MM, GunSpec.MAX_BORE_MM).fieldOf("bore").forGetter(Cartridge::boreMm),
            ResourceLocation.CODEC.fieldOf("case").forGetter(Cartridge::caseMaterial),
            Charge.CODEC.listOf().fieldOf("charges").forGetter(Cartridge::charges),
            ResourceLocation.CODEC.fieldOf("projectile").forGetter(Cartridge::projectile),
            ResourceLocation.CODEC.optionalFieldOf("payload").forGetter(Cartridge::payload),
            Codec.BOOL.optionalFieldOf("shot", false).forGetter(Cartridge::shot),
            BulletShape.CODEC.optionalFieldOf("shape", BulletShape.ROUND_NOSE).forGetter(Cartridge::shape),
            ResourceLocation.CODEC.optionalFieldOf("core").forGetter(Cartridge::core),
            io.github.soundgoodizerfan.feedback.potion.PotionState.CODEC.optionalFieldOf("vial").forGetter(Cartridge::vial),
            ResourceLocation.CODEC.optionalFieldOf("seed").forGetter(Cartridge::seed)
    ).apply(i, Cartridge::new)), LEGACY);

    /** Written out by hand: ten fields is past what {@code composite} takes. */
    public static final StreamCodec<RegistryFriendlyByteBuf, Cartridge> STREAM_CODEC = StreamCodec.of(
            (buf, c) -> {
                ByteBufCodecs.VAR_INT.encode(buf, c.boreMm());
                ResourceLocation.STREAM_CODEC.encode(buf, c.caseMaterial());
                Charge.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, c.charges());
                ResourceLocation.STREAM_CODEC.encode(buf, c.projectile());
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, c.payload());
                ByteBufCodecs.BOOL.encode(buf, c.shot());
                ByteBufCodecs.idMapper(i -> BulletShape.values()[i], BulletShape::ordinal).encode(buf, c.shape());
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, c.core());
                ByteBufCodecs.optional(io.github.soundgoodizerfan.feedback.potion.PotionState.STREAM_CODEC).encode(buf, c.vial());
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, c.seed());
            },
            buf -> new Cartridge(ByteBufCodecs.VAR_INT.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf),
                    Charge.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf), ResourceLocation.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf), ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.idMapper(i -> BulletShape.values()[i], BulletShape::ordinal).decode(buf),
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.optional(io.github.soundgoodizerfan.feedback.potion.PotionState.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf)));

    /** A shot shell always has a shot shape and a bullet never does; a missing one is the plain one. */
    public Cartridge {
        charges = List.copyOf(charges);
        if (shape == null || shape.shot() != shot)
            shape = shot ? BulletShape.BUCKSHOT : BulletShape.ROUND_NOSE;
        if (core == null || shot)
            core = Optional.empty();
        if (vial == null || shot)
            vial = Optional.empty();
        if (seed == null || shot)
            seed = Optional.empty();
    }

    /** A round with no seed. */
    public Cartridge(int boreMm, ResourceLocation caseMaterial, List<Charge> charges, ResourceLocation projectile,
                     Optional<ResourceLocation> payload, boolean shot, BulletShape shape, Optional<ResourceLocation> core,
                     Optional<io.github.soundgoodizerfan.feedback.potion.PotionState> vial) {
        this(boreMm, caseMaterial, charges, projectile, payload, shot, shape, core, vial, Optional.empty());
    }

    /** A round with no vial. */
    public Cartridge(int boreMm, ResourceLocation caseMaterial, List<Charge> charges, ResourceLocation projectile,
                     Optional<ResourceLocation> payload, boolean shot, BulletShape shape, Optional<ResourceLocation> core) {
        this(boreMm, caseMaterial, charges, projectile, payload, shot, shape, core, Optional.empty());
    }

    /** A round of a plain shape and no core. */
    public Cartridge(int boreMm, ResourceLocation caseMaterial, List<Charge> charges, ResourceLocation projectile,
                     Optional<ResourceLocation> payload, boolean shot) {
        this(boreMm, caseMaterial, charges, projectile, payload, shot, null, Optional.empty());
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
        double pellet = Math.PI / 6 * Math.pow(shape.pelletMm(), 3);
        return Math.max(1, Math.min(shape.maxPellets(), (int) Math.round(boreVolume / pellet)));
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

    /** The material most of the charge is: what a tooltip and a card name it by. */
    public ResourceLocation propellant() {
        return charges.stream().max(java.util.Comparator.comparingInt(Charge::count)).map(Charge::material)
                .orElse(ResourceLocation.fromNamespaceAndPath("feedback", "gunpowder"));
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

    /**
     * What this round asks of a chamber, from its charge's figures and how much of
     * each was seated.
     *
     * <p><b>Chemical blends.</b> Each item brings its propellant's pressure potential, and an
     * oxygen balance: gunpowder brings a surplus, blaze powder is fuel that needs some. The blend
     * burns completely when the sum is not negative; short of that the fraction burned falls
     * (never to nothing -- a fizzle floor). So blaze powder alone barely does anything, and a
     * blend near the balance is the hottest and fastest charge there is, with the lowest ignition
     * temperature of what is in it. Case room caps the total, so more fuel is less oxidizer.
     */
    /** How much room a case of this bore has, against the reference bore's: its area. */
    public static float caseScale(float boreMm) {
        return (float) Math.pow(boreMm / FTuning.GUN_CHARGE_REF_BORE_MM, FTuning.GUN_CASE_ROOM_EXPONENT);
    }

    /** The most charge a case of this bore takes. */
    public static int maxCharge(int boreMm) {
        return Math.max(1, Math.min(FTuning.GUN_CHARGE_CEILING, Math.round(FTuning.GUN_CHARGE_MAX * caseScale(boreMm))));
    }

    public Optional<GunSpec.Load> load(Map<ResourceLocation, Material> materials) {
        Optional<Blend> blend = Blend.of(materials, charges);
        if (blend.isEmpty())
            return Optional.empty();
        Blend b = blend.get();
        // Pressure is how full the case is: the same count in a wider case is thinner.
        return Optional.of(new GunSpec.Load(b.potential() / caseScale(boreMm) * b.burned(), b.burnRate(), b.ignitionTu(), b.total()));
    }

    /**
     * What a pile of propellant items is as one charge: each item's pressure potential, burn rate and
     * oxygen balance summed by count (the blend rule {@link #load} describes), shared by a round and by
     * a demolition charge, which burns the same stuff in a block instead of a case.
     *
     * @param potential  pressure potential, in nominal charges' worth of the reference.
     * @param burned     the fraction that burns: 1 at a balanced or oxidizer-rich blend, falling to a floor as it runs fuel-rich.
     * @param burnRate   the count-weighted burn rate.
     */
    public record Blend(float potential, float burned, float burnRate, float ignitionTu, int total) {
        /** Empty for no charge or an item with no propellant. */
        public static Optional<Blend> of(Map<ResourceLocation, Material> materials, List<Charge> charges) {
            if (charges.isEmpty())
                return Optional.empty();
            float potential = 0, burnWeighted = 0, balance = 0, ignition = Float.MAX_VALUE;
            int total = 0;
            for (Charge c : charges) {
                Material m = materials.get(c.material());
                if (m == null || m.propellant().isEmpty())
                    return Optional.empty();
                var p = m.propellant().get();
                potential += c.count() * p.peakPressure().value() / FTuning.GUN_CHARGE_NOMINAL;
                burnWeighted += c.count() * p.burnRate();
                balance += c.count() * p.oxygenBalance();
                ignition = Math.min(ignition, p.ignitionTemperature().value());
                total += c.count();
            }
            if (total <= 0)
                return Optional.empty();
            float burned = balance >= 0 ? 1f : Math.max(FTuning.GUN_FIZZLE_FLOOR, 1f + balance / total);
            return Optional.of(new Blend(potential, burned, burnWeighted / total, ignition, total));
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
                .filter(b -> b.burned() >= 1f)
                .map(b -> FTuning.DEMO_POWER_PER_SQRT * FTuning.DEMO_ROUND_SCALE * (float) Math.sqrt(b.potential()
                        * Math.pow(boreMm / 9.0, 3)))
                .orElse(0f);
        return Math.min(FTuning.DEMO_POWER_MAX, power);
    }

    /** Whether the bullet carries a flammable payload: it sets what it hits alight. */
    public boolean incendiary(Map<ResourceLocation, Material> materials) {
        return payload.map(materials::get).map(m -> m.traits().contains(Material.Trait.FLAMMABLE)).orElse(false);
    }

    /** The material the bullet's behaviour is read from: the core where there is one, else the projectile. */
    public ResourceLocation slugMaterial() {
        return core.orElse(projectile);
    }

    /**
     * The bullet's bulk figures in {@code materials}, or empty if a material is unknown: density
     * (core and jacket by their volume share), magnetism and hardness (the core's where there is one).
     */
    public Optional<Slug> slug(Map<ResourceLocation, Material> materials) {
        Material jacket = materials.get(projectile);
        if (jacket == null)
            return Optional.empty();
        if (core.isEmpty())
            return Optional.of(Slug.of(jacket));
        Material heart = materials.get(core.get());
        if (heart == null)
            return Optional.empty();
        float f = FTuning.GUN_CORE_FRACTION;
        return Optional.of(new Slug((float) (f * heart.density().muPerMb() + (1 - f) * jacket.density().muPerMb()),
                heart.separation().magnetic(), heart.hardness().value()));
    }

    /** What a bullet is as a mass and a hardness: see {@link #slug}. */
    public record Slug(float density, boolean magnetic, float hardness) {
        /** A bullet that is one material through. */
        public static Slug of(Material m) {
            return new Slug((float) m.density().muPerMb(), m.separation().magnetic(), m.hardness().value());
        }
    }

    /** A polymer jacket round a core: the carrier falls away and the core flies on, light and fast. */
    public boolean sabot(Map<ResourceLocation, Material> materials) {
        Material jacket = materials.get(projectile);
        return core.isPresent() && jacket != null && jacket.kind().filter(k -> k == Material.Kind.POLYMER).isPresent();
    }

    /** The hardness armour and stone meet: the bullet's, scaled by its shape (and a sabot's core, a little over). */
    public float hardness(Map<ResourceLocation, Material> materials) {
        float h = slug(materials).map(Slug::hardness).orElse(FTuning.GUN_DEFAULT_PROJECTILE_HARDNESS);
        return h * shape.pierce() * (sabot(materials) ? FTuning.GUN_SABOT_PIERCE : 1f);
    }

    /** Air drag against a round nose's: the shape's, and a sabot's is less. */
    public float drag(Map<ResourceLocation, Material> materials) {
        return shape.drag() * (sabot(materials) ? FTuning.GUN_SABOT_DRAG : 1f);
    }

    /**
     * Bore erosion against a round nose's: the shape's, and a jacket softer than its core shields the bore
     * from it in proportion.
     */
    public float boreWear(Map<ResourceLocation, Material> materials) {
        float w = shape.wear();
        Material jacket = materials.get(projectile);
        Material heart = core.map(materials::get).orElse(null);
        if (jacket != null && heart != null && heart.hardness().value() > 0)
            w *= Math.max(FTuning.GUN_JACKET_WEAR_FLOOR, Math.min(1f, jacket.hardness().value() / heart.hardness().value()));
        return w;
    }

    /** The traits of the payload's material: what it is, for the hooks that read what a payload is rather than what it does. */
    public java.util.Set<Material.Trait> payloadTraits(Map<ResourceLocation, Material> materials) {
        return payload.map(materials::get).map(m -> m.traits().isEmpty() ? java.util.Set.<Material.Trait>of()
                : java.util.EnumSet.copyOf(m.traits())).orElse(java.util.Set.of());
    }

    /** What the payload does to a living thing it strikes: the payload material's own perks (poison, chill, cling...), none for no payload. */
    public Map<Material.Perk, Integer> payloadPerks(Map<ResourceLocation, Material> materials) {
        return payload.map(materials::get).map(Material::perks).orElse(Map.of());
    }
}
