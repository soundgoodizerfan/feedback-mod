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
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A gun as designed ({@code feedback_guns_spec.md} sections 2 and 3): four settings, and
 * {@link #bake} derives every figure the gun has from them and its material, once.
 *
 * <p>The derivation is the whole point and is deliberately a pure function: the same design in
 * the same materials is the same {@link GunFigures}, so a gun crafted from any metal needs no
 * registry row (spec 2.1 -- the thing TacZ's rows-by-id could not do). Nothing keys on an id.
 *
 * <p>What it models is interior ballistics cut down to three things a player can act on: the
 * chamber's rating (its metal and its wall), the pressure the charge makes, and how long the
 * barrel is that the pressure acts over (spec 3.2 and 3.3).
 *
 * @param boreMm   bore, in mm; a design parameter, not a quantity any system trades in.
 * @param barrel   barrel length, a band (spec 3.3).
 * @param wall     chamber wall, a band.
 * @param material the metal of the barrel and chamber, a material id (never an item id).
 * @param layout   the gun's type and action (spec 12).
 * @param parts    the metals of the slide, trigger group and grip, each its own (spec 2.1: a
 *                 design space with a material per part is the thing a row per gun cannot hold).
 */
public record GunDesign(int boreMm, Barrel barrel, Wall wall, ResourceLocation material, Sight sight,
                        Launcher launcher, Parts parts, GunLayout layout) {

    /**
     * The parts the Stand draws, each with a material. The barrel's is {@link #material}, read
     * for the chamber's rating; the other three are metal in the gun's mass and in its price.
     */
    public enum Part {
        BARREL, SLIDE, TRIGGER, GRIP;

        public String langKey() {
            return "gun.feedback.part." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** The materials of the parts off the barrel. */
    public record Parts(ResourceLocation slide, ResourceLocation trigger, ResourceLocation grip) {
        public static final Codec<Parts> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("slide").forGetter(Parts::slide),
                ResourceLocation.CODEC.fieldOf("trigger").forGetter(Parts::trigger),
                ResourceLocation.CODEC.fieldOf("grip").forGetter(Parts::grip)
        ).apply(i, Parts::new));

        public static final StreamCodec<ByteBuf, Parts> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Parts::slide,
                ResourceLocation.STREAM_CODEC, Parts::trigger,
                ResourceLocation.STREAM_CODEC, Parts::grip,
                Parts::new);

        /** Every part in one metal: what every gun before parts existed was. */
        public static Parts of(ResourceLocation m) {
            return new Parts(m, m, m);
        }
    }

    /**
     * What throws the projectile: a gas in a sealed chamber, or a field along a coil. The Stand
     * makes both, and a later pass makes shapes of the first (carbine, revolver, shotgun); this
     * is the one fork that changes what the gun *is* rather than how it is built. A coil has no
     * propellant to burn, so it has no cook-off and no barrel-length burn trade; it has a winding
     * that quenches when it gets too hot and bursts under too much field.
     */
    public enum Launcher implements StringRepresentable {
        CHAMBER("chamber"), COIL("coil");

        public static final Codec<Launcher> CODEC = StringRepresentable.fromEnum(Launcher::values);
        public static final StreamCodec<ByteBuf, Launcher> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Launcher::ordinal);

        private final String id;

        Launcher(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /**
     * What the player looks through, a band like the others (philosophy 3): how far in aiming
     * zooms, and how long it takes to come up. A scope is a lot of zoom and a slow raise; iron
     * sights are free and quick. It does not change what the gun does, only what the player can do.
     */
    public enum Sight implements StringRepresentable {
        IRON("iron", FTuning.GUN_SIGHT_FOV_IRON, FTuning.GUN_SIGHT_MS_IRON, FTuning.GUN_INGOTS_SIGHT_IRON),
        REFLEX("reflex", FTuning.GUN_SIGHT_FOV_REFLEX, FTuning.GUN_SIGHT_MS_REFLEX, FTuning.GUN_INGOTS_SIGHT_REFLEX),
        SCOPE("scope", FTuning.GUN_SIGHT_FOV_SCOPE, FTuning.GUN_SIGHT_MS_SCOPE, FTuning.GUN_INGOTS_SIGHT_SCOPE);

        public static final Codec<Sight> CODEC = StringRepresentable.fromEnum(Sight::values);
        public static final StreamCodec<ByteBuf, Sight> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Sight::ordinal);

        private final String id;
        private final float fov;
        private final int raiseMs, ingots;

        Sight(String id, float fov, int raiseMs, int ingots) {
            this.id = id;
            this.fov = fov;
            this.raiseMs = raiseMs;
            this.ingots = ingots;
        }

        /** FOV multiplier when fully aimed. */
        public float fov() {
            return fov;
        }

        public int raiseMs() {
            return raiseMs;
        }

        public int ingots() {
            return ingots;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** A design with iron sights: what every gun before sights existed was. */
    public GunDesign(int boreMm, Barrel barrel, Wall wall, ResourceLocation material) {
        this(boreMm, barrel, wall, material, Sight.IRON);
    }

    public GunDesign(int boreMm, Barrel barrel, Wall wall, ResourceLocation material, Sight sight) {
        this(boreMm, barrel, wall, material, sight, Launcher.CHAMBER, Parts.of(material), GunLayout.DEFAULT);
    }

    public static final int MIN_BORE_MM = 5, MAX_BORE_MM = 20;

    /** Barrel length, named options not a free scalar (philosophy 3). */
    public enum Barrel implements StringRepresentable {
        SHORT("short", FTuning.GUN_BARREL_SHORT_M, FTuning.GUN_INGOTS_BARREL_SHORT),
        MEDIUM("medium", FTuning.GUN_BARREL_MEDIUM_M, FTuning.GUN_INGOTS_BARREL_MEDIUM),
        LONG("long", FTuning.GUN_BARREL_LONG_M, FTuning.GUN_INGOTS_BARREL_LONG);

        public static final Codec<Barrel> CODEC = StringRepresentable.fromEnum(Barrel::values);
        public static final StreamCodec<ByteBuf, Barrel> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Barrel::ordinal);

        private final String id;
        private final float metres;
        private final int ingots;

        Barrel(String id, float metres, int ingots) {
            this.id = id;
            this.metres = metres;
            this.ingots = ingots;
        }

        public int ingots() {
            return ingots;
        }

        public float metres() {
            return metres;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** Chamber wall thickness, a band. */
    public enum Wall implements StringRepresentable {
        THIN("thin", FTuning.GUN_WALL_THIN_MM, FTuning.GUN_INGOTS_WALL_THIN),
        STANDARD("standard", FTuning.GUN_WALL_STANDARD_MM, FTuning.GUN_INGOTS_WALL_STANDARD),
        THICK("thick", FTuning.GUN_WALL_THICK_MM, FTuning.GUN_INGOTS_WALL_THICK);

        public static final Codec<Wall> CODEC = StringRepresentable.fromEnum(Wall::values);
        public static final StreamCodec<ByteBuf, Wall> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Wall::ordinal);

        private final String id;
        private final float mm;
        private final int ingots;

        Wall(String id, float mm, int ingots) {
            this.id = id;
            this.mm = mm;
            this.ingots = ingots;
        }

        public int ingots() {
            return ingots;
        }

        public float mm() {
            return mm;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static final Codec<GunDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(MIN_BORE_MM, MAX_BORE_MM).fieldOf("bore").forGetter(GunDesign::boreMm),
            Barrel.CODEC.fieldOf("barrel").forGetter(GunDesign::barrel),
            Wall.CODEC.fieldOf("wall").forGetter(GunDesign::wall),
            ResourceLocation.CODEC.fieldOf("material").forGetter(GunDesign::material),
            Sight.CODEC.optionalFieldOf("sight", Sight.IRON).forGetter(GunDesign::sight),
            Launcher.CODEC.optionalFieldOf("launcher", Launcher.CHAMBER).forGetter(GunDesign::launcher),
            // A design saved before parts had a material is all one metal.
            Parts.CODEC.optionalFieldOf("parts").forGetter(d -> Optional.of(d.parts())),
            GunLayout.CODEC.optionalFieldOf("layout", GunLayout.DEFAULT).forGetter(GunDesign::layout)
    ).apply(i, (bore, barrel, wall, material, sight, launcher, parts, layout) ->
            new GunDesign(bore, barrel, wall, material, sight, launcher, parts.orElse(Parts.of(material)), layout)));

    /** Written out by hand: eight fields is past what {@code composite} takes. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GunDesign> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> {
                ByteBufCodecs.VAR_INT.encode(buf, d.boreMm());
                Barrel.STREAM_CODEC.encode(buf, d.barrel());
                Wall.STREAM_CODEC.encode(buf, d.wall());
                ResourceLocation.STREAM_CODEC.encode(buf, d.material());
                Sight.STREAM_CODEC.encode(buf, d.sight());
                Launcher.STREAM_CODEC.encode(buf, d.launcher());
                Parts.STREAM_CODEC.encode(buf, d.parts());
                GunLayout.STREAM_CODEC.encode(buf, d.layout());
            },
            buf -> new GunDesign(ByteBufCodecs.VAR_INT.decode(buf), Barrel.STREAM_CODEC.decode(buf),
                    Wall.STREAM_CODEC.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf), Sight.STREAM_CODEC.decode(buf),
                    Launcher.STREAM_CODEC.decode(buf), Parts.STREAM_CODEC.decode(buf), GunLayout.STREAM_CODEC.decode(buf)));

    /** The bore pulled back into range, so a design is always buildable. */
    public GunDesign {
        boreMm = Mth.clamp(boreMm, MIN_BORE_MM, MAX_BORE_MM);
    }

    /**
     * Whether a gun can be made of {@code m}: a metal, weighed, with a yield strength to read the
     * chamber's rating from. The engine's rule ({@code EngineDesign.buildable}), one figure
     * short of it: a gun needs no ingot until the Stand pays for one.
     */
    public static boolean buildable(Material m) {
        return m.kind().orElse(null) == Material.Kind.METAL && m.density().muPerMb() > 0
                && m.mechanical().yieldStrength().isPresent();
    }

    /**
     * Every figure of this gun, derived once.
     *
     * <p>The rating is thin-wall hoop stress turned round: a vessel of radius r and wall t holds
     * {@code yield x t / r}. Peak pressure is the propellant's, and muzzle energy is the mean
     * pressure acting on the bore's area over the barrel's length -- so a longer barrel is more
     * speed and more weight, and a bigger bore more energy and more kick, with neither
     * dominating (spec 3.3).
     *
     * <p>This and the other overloads taking a {@link Material} read every part as {@code m};
     * the ones taking the table read each part's own.
     *
     * @param m          the barrel and chamber metal.
     * @param projectile the projectile's material; its density is what the bullet weighs.
     * @return empty if {@code m} cannot make a gun or {@code projectile} has not been weighed.
     */
    public Optional<GunFigures> bake(Material m, Material projectile) {
        return bake(m, projectile, launcher == Launcher.COIL ? Load.REFERENCE_COIL : Load.REFERENCE);
    }

    /**
     * What a round asks of a chamber and a barrel: the gas pressure it peaks at, and how much
     * barrel it takes to burn ({@link Material.Propellant}).
     */
    public record Load(float peakPressure, float burnRatePerM, float ignitionTu, boolean coil, int charge) {
        /** Gunpowder at its nominal charge: the figures a gun's spec sheet is read against. */
        public static final Load REFERENCE = new Load(FTuning.GUN_PROPELLANT_PEAK_PU, FTuning.GUN_PROPELLANT_BURN_RATE_PER_M,
                FTuning.GUN_PROPELLANT_IGNITION_TU, false, FTuning.GUN_CHARGE_NOMINAL);
        /** A nominal redstone drive: a coil's spec sheet is read against this. Ignition is the quench temperature. */
        public static final Load REFERENCE_COIL = new Load(FTuning.GUN_PROPELLANT_PEAK_PU, 1f, 400f, true,
                FTuning.GUN_CHARGE_NOMINAL);
    }

    /**
     * The figures of this gun firing one particular round, derived now rather than stored: a gun
     * is a baked design, a round is what it is fed (spec 2), and nothing keys on either's id.
     *
     * @return empty if the round is for another bore, or names a material that cannot fill its
     *         part.
     */
    public Optional<GunFigures> bake(Map<ResourceLocation, Material> materials, Cartridge round) {
        if (round.boreMm() != boreMm)
            return Optional.empty();
        Material metal = materials.get(material), bullet = materials.get(round.projectile());
        Optional<Frame> frame = frame(materials);
        if (metal == null || bullet == null || frame.isEmpty())
            return Optional.empty();
        return round.load(materials).flatMap(load -> bake(metal, frame.get(), bullet, load));
    }

    public Optional<GunFigures> bake(Material m, Material projectile, Load load) {
        return bake(m, new Frame((float) (frameMb() * m.density().muPerMb()), gripFriction(m)), projectile, load);
    }

    /** The parts off the barrel: what they weigh, and how well the grip holds the hand. */
    private record Frame(float mu, float gripFriction) {
    }

    /** A grip's friction; one never measured holds like the reference. */
    private static float gripFriction(Material grip) {
        return grip.mechanical().friction().orElse(FTuning.GUN_KICK_REFERENCE_FRICTION);
    }

    /** Metal off the barrel, in mB: slide, trigger group and grip. */
    private static float frameMb() {
        return FTuning.GUN_SLIDE_METAL_MB + FTuning.GUN_TRIGGER_METAL_MB + FTuning.GUN_GRIP_METAL_MB;
    }

    /** What the parts off the barrel weigh, each in its own metal; empty if one is not a gun metal. */
    private Optional<Frame> frame(Map<ResourceLocation, Material> materials) {
        Material slide = materials.get(parts.slide()), trigger = materials.get(parts.trigger()), grip = materials.get(parts.grip());
        if (slide == null || trigger == null || grip == null || !buildable(slide) || !buildable(trigger) || !buildable(grip))
            return Optional.empty();
        return Optional.of(new Frame(FTuning.GUN_SLIDE_METAL_MB * slide.density().muPerMb()
                + FTuning.GUN_TRIGGER_METAL_MB * trigger.density().muPerMb()
                + FTuning.GUN_GRIP_METAL_MB * grip.density().muPerMb(), gripFriction(grip)));
    }

    private Optional<GunFigures> bake(Material m, Frame frame, Material projectile, Load load) {
        if (!buildable(m) || projectile.density().muPerMb() <= 0 || load.coil() != (launcher == Launcher.COIL))
            return Optional.empty();
        double boreM = boreMm / 1000.0, barrelM = barrel.metres(), wallM = wall.mm() / 1000.0;
        double area = Math.PI / 4 * boreM * boreM;

        float rating = m.mechanical().yieldStrength().get().value() * wall.mm() / (boreMm / 2f);
        float peak = load.peakPressure();

        double work;
        if (load.coil()) {
            // Stages add up along the coil, saturating; a slug that is not magnetic barely couples;
            // a winding past its rating (the field's pressure against its hoop strength) delivers
            // only what it held, the same venting rule as a chamber.
            double stages = Math.min(FTuning.GUN_COIL_STAGE_MAX, barrelM / FTuning.GUN_BARREL_MEDIUM_M);
            double coupling = projectile.separation().magnetic() ? 1.0 : FTuning.GUN_COIL_NONMAGNETIC_COUPLING;
            double held = peak <= 0 ? 0 : Math.min(1.0, rating / peak);
            work = load.charge() * FTuning.GUN_COIL_WORK_PER_DUST * stages * coupling * held;
        } else {
            // Only the part of the charge burned by the muzzle does work: a slow powder wants a long barrel.
            double burned = Math.min(1.0, load.burnRatePerM() * barrelM);
            // A chamber past its rating vents rather than delivers: the push is the pressure it held.
            work = Math.min(peak, rating) * FTuning.GUN_PRESSURE_FILL * area * barrelM * burned * FTuning.GUN_WORK_PER_PU_M3;
        }

        // 1 m3 is 1000 mB. A bullet is its bore's area over a length of some bores.
        double bulletMb = area * (boreM * FTuning.GUN_PROJECTILE_LENGTH_PER_BORE) * 1000;
        float bulletMu = (float) (bulletMb * projectile.density().muPerMb());
        float speedMs = (float) Math.sqrt(2 * work / bulletMu);

        float impulse = bulletMu * speedMs * (1 + FTuning.GUN_GAS_IMPULSE_FRACTION);

        // Barrel tube in its metal, the rest in theirs. A tube of outer and inner diameter, 1 m3 = 1000 mB.
        double outer = boreM + 2 * wallM;
        double tubeMb = Math.PI / 4 * (outer * outer - boreM * boreM) * (barrelM + 0.1) * 1000;
        float gunMu = (float) (tubeMb * m.density().muPerMb()) + frame.mu();

        int cycleMs = Math.round(FTuning.GUN_CYCLE_BASE_MS + FTuning.GUN_CYCLE_MS_PER_IMPULSE * impulse)
                + layout.action().manualMs();
        float spread = Math.max(FTuning.GUN_SPREAD_FLOOR_DEG,
                FTuning.GUN_SPREAD_AT_MEDIUM_DEG * FTuning.GUN_BARREL_MEDIUM_M / barrel.metres());
        // Kick is recoil velocity, impulse over the gun's mass, and the grip's friction is how
        // much of it the hand lets through: a heavy gun with a grippy grip kicks least.
        float kick = FTuning.GUN_KICK_DEG_PER_IMPULSE * impulse * (FTuning.GUN_KICK_REFERENCE_MU / gunMu)
                * (FTuning.GUN_KICK_REFERENCE_FRICTION / frame.gripFriction());

        return Optional.of(new GunFigures(new Pu(peak), new Pu(rating), speedMs / 20f, (float) work,
                new Mu(bulletMu), new Mu(gunMu), impulse, cycleMs, spread, kick, load.coil()));
    }

    /** Whether the Stand may print a gun of {@code m}: buildable, and with an ingot to pay in. */
    public static boolean printable(ResourceLocation id, Material m) {
        return buildable(m) && m.item(id, "ingot").isPresent();
    }

    /** The material a sample item names for a gun -- any form of a printable metal -- if any. */
    public static Optional<ResourceLocation> sampleMaterial(Map<ResourceLocation, Material> materials, ItemStack sample) {
        if (sample.isEmpty())
            return Optional.empty();
        return Material.find(materials, sample.getItem()).filter(l -> printable(l.id(), l.material()))
                .map(Material.Located::id);
    }

    /** The item a winding is wound from: consolidated redstone, the bulk phase (a block), if the table has one. */
    public static Optional<net.minecraft.world.item.Item> windingItem(Map<ResourceLocation, Material> materials) {
        ResourceLocation id = Feedback.id("redstone");
        Material m = materials.get(id);
        return m == null ? Optional.empty() : m.item(id, "block");
    }

    /** Redstone blocks a coil's winding takes, by its length. None for a chamber. */
    public int windingBlocks() {
        if (launcher != Launcher.COIL)
            return 0;
        return switch (barrel) {
            case SHORT -> FTuning.GUN_COIL_BLOCKS_SHORT;
            case MEDIUM -> FTuning.GUN_COIL_BLOCKS_MEDIUM;
            case LONG -> FTuning.GUN_COIL_BLOCKS_LONG;
        };
    }

    /** Ingots it takes to build, all metals together. */
    public int ingots() {
        return ingotsByMaterial().values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Ingots of each metal: the barrel's pays for its band and the wall's, the slide's for itself
     * and the sight it carries, the trigger's and the grip's for themselves.
     */
    public Map<ResourceLocation, Integer> ingotsByMaterial() {
        Map<ResourceLocation, Integer> out = new LinkedHashMap<>();
        out.merge(material, barrel.ingots() + wall.ingots(), Integer::sum);
        out.merge(parts.slide(), FTuning.GUN_INGOTS_SLIDE + sight.ingots(), Integer::sum);
        out.merge(parts.trigger(), FTuning.GUN_INGOTS_TRIGGER, Integer::sum);
        out.merge(parts.grip(), FTuning.GUN_INGOTS_GRIP, Integer::sum);
        return out;
    }

    /**
     * Rounds the gun's own feed holds, chamber included, or empty when its rounds come in a
     * magazine item: a break action one per barrel (a shotgun has two), a cylinder its chambers,
     * a tube as many as fit under the barrel.
     */
    public java.util.OptionalInt internalCapacity() {
        return switch (layout.feed()) {
            case BARRELS -> java.util.OptionalInt.of(layout.type() == GunLayout.Type.SHOTGUN ? 2 : 1);
            case CYLINDER -> java.util.OptionalInt.of(FTuning.GUN_CYLINDER_ROUNDS);
            case TUBE -> java.util.OptionalInt.of(switch (barrel) {
                case SHORT -> FTuning.GUN_TUBE_ROUNDS_SHORT;
                case MEDIUM -> FTuning.GUN_TUBE_ROUNDS_MEDIUM;
                case LONG -> FTuning.GUN_TUBE_ROUNDS_LONG;
            });
            case BOX, BELT -> java.util.OptionalInt.empty();
        };
    }

    /** Every metal this design names, barrel first. */
    public List<ResourceLocation> materials() {
        return List.of(material, parts.slide(), parts.trigger(), parts.grip());
    }

    /** Whether the Stand may print this design: every part a printable metal. */
    public boolean printable(Map<ResourceLocation, Material> materials) {
        return materials().stream().allMatch(id -> materials.get(id) != null && printable(id, materials.get(id)));
    }

    /**
     * Bake against the material table at the reference load: lead for a chamber, iron for a coil
     * (a slug has to couple to the field). Named once, here, and only the spec sheet reads it --
     * what is fired is baked per round.
     */
    public Optional<GunFigures> bake(Map<ResourceLocation, Material> materials) {
        Material metal = materials.get(material);
        Material bullet = materials.get(Feedback.id(launcher == Launcher.COIL ? "iron" : "lead"));
        Optional<Frame> frame = frame(materials);
        return metal == null || bullet == null || frame.isEmpty() ? Optional.empty()
                : bake(metal, frame.get(), bullet, launcher == Launcher.COIL ? Load.REFERENCE_COIL : Load.REFERENCE);
    }

    public GunDesign withBore(int mm) {
        return new GunDesign(mm, barrel, wall, material, sight, launcher, parts, layout);
    }

    public GunDesign withBarrel(Barrel b) {
        return new GunDesign(boreMm, b, wall, material, sight, launcher, parts, layout);
    }

    public GunDesign withWall(Wall w) {
        return new GunDesign(boreMm, barrel, w, material, sight, launcher, parts, layout);
    }

    public GunDesign withSight(Sight s) {
        return new GunDesign(boreMm, barrel, wall, material, s, launcher, parts, layout);
    }

    public GunDesign withLauncher(Launcher l) {
        return new GunDesign(boreMm, barrel, wall, material, sight, l, parts, layout);
    }

    public GunDesign withLayout(GunLayout l) {
        return new GunDesign(boreMm, barrel, wall, material, sight, launcher, parts, l);
    }

    /** The barrel's metal; the other parts keep theirs. */
    public GunDesign withMaterial(ResourceLocation m) {
        return new GunDesign(boreMm, barrel, wall, m, sight, launcher, parts, layout);
    }

    public ResourceLocation material(Part part) {
        return switch (part) {
            case BARREL -> material;
            case SLIDE -> parts.slide();
            case TRIGGER -> parts.trigger();
            case GRIP -> parts.grip();
        };
    }

    public GunDesign withMaterial(Part part, ResourceLocation m) {
        return switch (part) {
            case BARREL -> withMaterial(m);
            case SLIDE -> new GunDesign(boreMm, barrel, wall, material, sight, launcher, new Parts(m, parts.trigger(), parts.grip()), layout);
            case TRIGGER -> new GunDesign(boreMm, barrel, wall, material, sight, launcher, new Parts(parts.slide(), m, parts.grip()), layout);
            case GRIP -> new GunDesign(boreMm, barrel, wall, material, sight, launcher, new Parts(parts.slide(), parts.trigger(), m), layout);
        };
    }

    /** A first design for the test pistol: a 9 mm, medium barrel, standard wall. */
    public static GunDesign starter(ResourceLocation material) {
        return new GunDesign(9, Barrel.MEDIUM, Wall.STANDARD, material);
    }
}
