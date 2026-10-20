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
 * A gun as designed ({@code docs/feedback_guns_spec.md} sections 2 and 3): four settings, and
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
 * @param built    the real shape of the barrel it was assembled from, when it was (spec 12): the
 *                 gun bakes from this, and {@code boreMm}, {@code barrel} and {@code wall} are then
 *                 only what the blueprint aimed at. Empty on a design that is still paper.
 * @param parts    the metals of the slide, trigger group and grip, each its own (spec 2.1: a
 *                 design space with a material per part is the thing a row per gun cannot hold).
 */
public record GunDesign(int boreMm, Barrel barrel, Wall wall, ResourceLocation material, Sight sight,
                        Launcher launcher, Parts parts, GunLayout layout, Optional<BarrelGeometry> built,
                        Map<Fitting, ResourceLocation> fittings) {

    /**
     * What can be fitted to a gun besides its parts (spec 2: attachments are physical parts of the
     * design, never modifiers -- fitting one re-runs the bake). Each is a machined part of a metal
     * that weighs what it is made of. A suppressor and a brake both go on the muzzle, so a gun has
     * one or the other.
     *
     * <ul>
     *   <li><b>Suppressor</b> -- baffles trap the gas: a quieter report and a smaller vibration,
     *   but the gas's heat goes into the gun, so it heats faster.</li>
     *   <li><b>Muzzle brake</b> -- ports throw the gas sideways: the gas's share of the recoil
     *   is mostly cancelled, and the report is louder.</li>
     *   <li><b>Relief vent</b> -- the Ghast's pressure-release vent (compendium 4.4): a port that
     *   opens short of the chamber's rating, so overpressure goes out of it and not into the gun
     *   or the hand; it leaks a little of every shot.</li>
     *   <li><b>Barrel shroud</b> -- a finned jacket: the barrel sheds heat faster.</li>
     * </ul>
     */
    public enum Fitting implements StringRepresentable {
        SUPPRESSOR("suppressor", "suppressor", true, FTuning.GUN_SUPPRESSOR_METAL_MB),
        BRAKE("brake", "muzzle_brake", true, FTuning.GUN_BRAKE_METAL_MB),
        VENT("vent", "relief_vent", false, FTuning.GUN_VENT_METAL_MB),
        SHROUD("shroud", "barrel_shroud", false, FTuning.GUN_SHROUD_METAL_MB);

        public static final Codec<Fitting> CODEC = StringRepresentable.fromEnum(Fitting::values);
        public static final StreamCodec<ByteBuf, Fitting> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Fitting::ordinal);

        private final String id, form;
        private final boolean muzzle;
        private final float metalMb;

        Fitting(String id, String form, boolean muzzle, float metalMb) {
            this.id = id;
            this.form = form;
            this.muzzle = muzzle;
            this.metalMb = metalMb;
        }

        /** The part form it is machined as: what an assembly or a refit looks for. */
        public String form() {
            return form;
        }

        /** Whether it goes on the muzzle, where there is room for one. */
        public boolean muzzle() {
            return muzzle;
        }

        public float metalMb() {
            return metalMb;
        }

        public String langKey() {
            return "gun.feedback.fitting." + id;
        }

        /** The fitting machined as {@code form}, if any. */
        public static Optional<Fitting> ofForm(String form) {
            for (Fitting f : values())
                if (f.form.equals(form))
                    return Optional.of(f);
            return Optional.empty();
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** A design with nothing fitted: every design before fittings. */
    public GunDesign(int boreMm, Barrel barrel, Wall wall, ResourceLocation material, Sight sight,
                     Launcher launcher, Parts parts, GunLayout layout, Optional<BarrelGeometry> built) {
        this(boreMm, barrel, wall, material, sight, launcher, parts, layout, built, Map.of());
    }

    /** Whether {@code f} is fitted. */
    public boolean has(Fitting f) {
        return fittings.containsKey(f);
    }

    /** This design with {@code f} fitted in {@code metal}; a muzzle fitting takes the muzzle's place. */
    public GunDesign withFitting(Fitting f, ResourceLocation metal) {
        Map<Fitting, ResourceLocation> out = new java.util.EnumMap<>(Fitting.class);
        out.putAll(fittings);
        if (f.muzzle())
            for (Fitting other : Fitting.values())
                if (other.muzzle())
                    out.remove(other);
        out.put(f, metal);
        return new GunDesign(boreMm, barrel, wall, material, sight, launcher, parts, layout, built, out);
    }

    public GunDesign without(Fitting f) {
        Map<Fitting, ResourceLocation> out = new java.util.EnumMap<>(Fitting.class);
        out.putAll(fittings);
        out.remove(f);
        return new GunDesign(boreMm, barrel, wall, material, sight, launcher, parts, layout, built, out);
    }

    /**
     * The parts the Stand draws, each with a material. The barrel's is {@link #material}, read
     * for the chamber's rating; the other three are metal in the gun's mass and in its price.
     */
    public enum Part {
        BARREL, SLIDE, TRIGGER, GRIP, RECEIVER;

        public String langKey() {
            return "gun.feedback.part." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * The materials of the parts off the barrel. {@code slide} is whatever the action works -- a
     * slide on a pistol, a bolt otherwise; a break action has neither and it is the receiver's.
     *
     * @param feed   the metal of the feed's own part, a cylinder or a tube, when the gun has one:
     *               it weighs what it is made of. Empty reads as the receiver's.
     * @param second a double gun's other barrel, with its own metal and real shape (spec 12): it
     *               weighs, and every other shot is fired down it ({@link #firing}).
     * @param wrap   what the grip is wrapped in, if anything: the hand holds the wrap, so its
     *               friction is the grip's ({@link #wrapMaterial}). Leather, cloth, honey.
     */
    public record Parts(ResourceLocation slide, ResourceLocation trigger, ResourceLocation grip, ResourceLocation receiver,
                        Optional<ResourceLocation> feed, Optional<SecondBarrel> second, Optional<ResourceLocation> wrap) {
        public static final Codec<Parts> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("slide").forGetter(Parts::slide),
                ResourceLocation.CODEC.fieldOf("trigger").forGetter(Parts::trigger),
                ResourceLocation.CODEC.fieldOf("grip").forGetter(Parts::grip),
                ResourceLocation.CODEC.optionalFieldOf("receiver").forGetter(p -> Optional.of(p.receiver())),
                ResourceLocation.CODEC.optionalFieldOf("feed").forGetter(Parts::feed),
                SecondBarrel.CODEC.optionalFieldOf("second").forGetter(Parts::second),
                ResourceLocation.CODEC.optionalFieldOf("wrap").forGetter(Parts::wrap)
        ).apply(i, (slide, trigger, grip, receiver, feed, second, wrap) ->
                new Parts(slide, trigger, grip, receiver.orElse(slide), feed, second, wrap)));

        /** Written out by hand: seven fields is past what {@code composite} takes. */
        public static final StreamCodec<ByteBuf, Parts> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    ResourceLocation.STREAM_CODEC.encode(buf, p.slide());
                    ResourceLocation.STREAM_CODEC.encode(buf, p.trigger());
                    ResourceLocation.STREAM_CODEC.encode(buf, p.grip());
                    ResourceLocation.STREAM_CODEC.encode(buf, p.receiver());
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, p.feed());
                    ByteBufCodecs.optional(SecondBarrel.STREAM_CODEC).encode(buf, p.second());
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, p.wrap());
                },
                buf -> new Parts(ResourceLocation.STREAM_CODEC.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf),
                        ResourceLocation.STREAM_CODEC.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf),
                        ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf),
                        ByteBufCodecs.optional(SecondBarrel.STREAM_CODEC).decode(buf),
                        ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf)));

        public Parts(ResourceLocation slide, ResourceLocation trigger, ResourceLocation grip, ResourceLocation receiver) {
            this(slide, trigger, grip, receiver, Optional.empty(), Optional.empty(), Optional.empty());
        }

        public Parts(ResourceLocation slide, ResourceLocation trigger, ResourceLocation grip, ResourceLocation receiver,
                     Optional<ResourceLocation> feed, Optional<SecondBarrel> second) {
            this(slide, trigger, grip, receiver, feed, second, Optional.empty());
        }

        /** Every part in one metal: what every gun before parts existed was. */
        public static Parts of(ResourceLocation m) {
            return new Parts(m, m, m, m);
        }

        Parts withSecond(Optional<SecondBarrel> s) {
            return new Parts(slide, trigger, grip, receiver, feed, s, wrap);
        }

        Parts with(ResourceLocation slide, ResourceLocation trigger, ResourceLocation grip, ResourceLocation receiver) {
            return new Parts(slide, trigger, grip, receiver, feed, second, wrap);
        }
    }

    /** A double gun's other barrel: its metal, and its shape as machined. */
    public record SecondBarrel(ResourceLocation material, BarrelGeometry built) {
        public static final Codec<SecondBarrel> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("material").forGetter(SecondBarrel::material),
                BarrelGeometry.CODEC.fieldOf("built").forGetter(SecondBarrel::built)
        ).apply(i, SecondBarrel::new));

        public static final StreamCodec<ByteBuf, SecondBarrel> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, SecondBarrel::material,
                BarrelGeometry.STREAM_CODEC, SecondBarrel::built,
                SecondBarrel::new);
    }

    /** Whether this design has two barrels side by side: a break-action shotgun. */
    public boolean doubled() {
        return layout.feed() == GunLayout.Feed.BARRELS && layout.type() == GunLayout.Type.SHOTGUN;
    }

    /**
     * The gun as the next shot sees it: a double fires its first barrel with two rounds in and its
     * second with one, so the round count says which barrel is loaded. Swapping the two keeps the
     * mass the same, because both tubes are always weighed.
     */
    public GunDesign firing(int roundsHeld) {
        if (roundsHeld != 1 || parts.second().isEmpty() || built.isEmpty())
            return this;
        SecondBarrel other = parts.second().get();
        return new GunDesign(Math.round(other.built().boreMm()), barrel, wall, other.material(), sight, launcher,
                parts.withSecond(Optional.of(new SecondBarrel(material, built.get()))), layout, Optional.of(other.built()), fittings);
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
        IRON("iron", FTuning.GUN_SIGHT_FOV_IRON, FTuning.GUN_SIGHT_MS_IRON, FTuning.GUN_SIGHT_PLATES_IRON),
        REFLEX("reflex", FTuning.GUN_SIGHT_FOV_REFLEX, FTuning.GUN_SIGHT_MS_REFLEX, FTuning.GUN_SIGHT_PLATES_REFLEX),
        SCOPE("scope", FTuning.GUN_SIGHT_FOV_SCOPE, FTuning.GUN_SIGHT_MS_SCOPE, FTuning.GUN_SIGHT_PLATES_SCOPE);

        public static final Codec<Sight> CODEC = StringRepresentable.fromEnum(Sight::values);
        public static final StreamCodec<ByteBuf, Sight> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Sight::ordinal);

        private final String id;
        private final float fov;
        private final int raiseMs, plates;

        Sight(String id, float fov, int raiseMs, int plates) {
            this.id = id;
            this.fov = fov;
            this.raiseMs = raiseMs;
            this.plates = plates;
        }

        /** FOV multiplier when fully aimed. */
        public float fov() {
            return fov;
        }

        public int raiseMs() {
            return raiseMs;
        }

        /** Plates it is built from at assembly. */
        public int plates() {
            return plates;
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
        this(boreMm, barrel, wall, material, sight, Launcher.CHAMBER, Parts.of(material), GunLayout.DEFAULT, Optional.empty());
    }

    public static final int MIN_BORE_MM = 5, MAX_BORE_MM = 20;

    /** Barrel length, named options not a free scalar (philosophy 3). */
    public enum Barrel implements StringRepresentable {
        SHORT("short", FTuning.GUN_BARREL_SHORT_M),
        MEDIUM("medium", FTuning.GUN_BARREL_MEDIUM_M),
        LONG("long", FTuning.GUN_BARREL_LONG_M);

        public static final Codec<Barrel> CODEC = StringRepresentable.fromEnum(Barrel::values);
        public static final StreamCodec<ByteBuf, Barrel> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Barrel::ordinal);

        private final String id;
        private final float metres;

        Barrel(String id, float metres) {
            this.id = id;
            this.metres = metres;
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
        THIN("thin", FTuning.GUN_WALL_THIN_MM),
        STANDARD("standard", FTuning.GUN_WALL_STANDARD_MM),
        THICK("thick", FTuning.GUN_WALL_THICK_MM);

        public static final Codec<Wall> CODEC = StringRepresentable.fromEnum(Wall::values);
        public static final StreamCodec<ByteBuf, Wall> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Wall::ordinal);

        private final String id;
        private final float mm;

        Wall(String id, float mm) {
            this.id = id;
            this.mm = mm;
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
            GunLayout.CODEC.optionalFieldOf("layout", GunLayout.DEFAULT).forGetter(GunDesign::layout),
            BarrelGeometry.CODEC.optionalFieldOf("built").forGetter(GunDesign::built),
            Codec.unboundedMap(Fitting.CODEC, ResourceLocation.CODEC).optionalFieldOf("fittings", Map.of())
                    .forGetter(GunDesign::fittings)
    ).apply(i, (bore, barrel, wall, material, sight, launcher, parts, layout, built, fittings) ->
            new GunDesign(bore, barrel, wall, material, sight, launcher, parts.orElse(Parts.of(material)), layout, built,
                    fittings)));

    private static final StreamCodec<ByteBuf, Map<Fitting, ResourceLocation>> FITTINGS_STREAM =
            ByteBufCodecs.map(java.util.HashMap::new, Fitting.STREAM_CODEC, ResourceLocation.STREAM_CODEC);

    /** Written out by hand: ten fields is past what {@code composite} takes. */
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
                ByteBufCodecs.optional(BarrelGeometry.STREAM_CODEC).encode(buf, d.built());
                FITTINGS_STREAM.encode(buf, d.fittings());
            },
            buf -> new GunDesign(ByteBufCodecs.VAR_INT.decode(buf), Barrel.STREAM_CODEC.decode(buf),
                    Wall.STREAM_CODEC.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf), Sight.STREAM_CODEC.decode(buf),
                    Launcher.STREAM_CODEC.decode(buf), Parts.STREAM_CODEC.decode(buf), GunLayout.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(BarrelGeometry.STREAM_CODEC).decode(buf), FITTINGS_STREAM.decode(buf)));

    /** The bore pulled back into range, so a design is always buildable. */
    public GunDesign {
        boreMm = Mth.clamp(boreMm, MIN_BORE_MM, MAX_BORE_MM);
        fittings = Map.copyOf(fittings);
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
    public record Load(float peakPressure, float burnRatePerM, float ignitionTu, boolean coil, float charge) {
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
        // Only a shotgun's open bore throws a pattern; it also takes a bullet of its bore, as a slug (spec 12).
        if (round.shot() && layout.type() != GunLayout.Type.SHOTGUN)
            return Optional.empty();
        // A paper design takes its own bore exactly; a built one takes what will chamber in the
        // bore it really has, and how loose or tight it sits is in the figures.
        float loose = boreMmReal() - round.boreMm();
        if (built.isEmpty() ? round.boreMm() != boreMm : Math.abs(loose) > FTuning.GUN_BORE_FIT_MM)
            return Optional.empty();
        Material metal = materials.get(material), bullet = materials.get(round.projectile());
        Optional<Frame> frame = frame(materials);
        if (metal == null || bullet == null || frame.isEmpty())
            return Optional.empty();
        return round.load(materials).flatMap(load -> bake(metal, frame.get(), bullet, load, loose));
    }

    public Optional<GunFigures> bake(Material m, Material projectile, Load load) {
        float mu = frameMb() * m.density().muPerMb();
        for (Fitting f : fittings.keySet())
            mu += f.metalMb() * m.density().muPerMb();
        if (doubled())
            mu += tubeMu(built.orElse(target()), m);
        return bake(m, new Frame(mu, gripFriction(m)), projectile, load, 0f);
    }

    /** What a barrel tube of this shape weighs in {@code m}: outer and inner diameter, a little over its length; 1 m3 = 1000 mB. */
    private static float tubeMu(BarrelGeometry g, Material m) {
        double bore = g.boreMm() / 1000.0, outer = g.outerMm() / 1000.0;
        return (float) (Math.PI / 4 * (outer * outer - bore * bore) * (g.lengthM() + 0.1) * 1000 * m.density().muPerMb());
    }

    /** The bore the barrel really has: as machined, or the blueprint's on paper. */
    public float boreMmReal() {
        return built.map(BarrelGeometry::boreMm).orElse((float) boreMm);
    }

    private float wallMmReal() {
        return built.map(BarrelGeometry::wallMm).orElse(wall.mm());
    }

    private float barrelMReal() {
        return built.map(BarrelGeometry::lengthM).orElse(barrel.metres());
    }

    /** The length band the barrel is nearest: what a tube under it or a coil wound on it is sized by. */
    public Barrel barrelBand() {
        if (built.isEmpty())
            return barrel;
        Barrel nearest = Barrel.SHORT;
        for (Barrel b : Barrel.values())
            if (Math.abs(b.metres() - barrelMReal()) < Math.abs(nearest.metres() - barrelMReal()))
                nearest = b;
        return nearest;
    }

    /** The parts off the barrel: what they weigh, and how well the grip holds the hand. */
    private record Frame(float mu, float gripFriction) {
    }

    /** A grip's friction; one never measured holds like the reference. */
    private static float gripFriction(Material grip) {
        return grip.mechanical().friction().orElse(FTuning.GUN_KICK_REFERENCE_FRICTION);
    }

    /** Metal off the barrel, in mB: receiver, slide, trigger group, grip, and the feed's own part. */
    private float frameMb() {
        return FTuning.GUN_RECEIVER_METAL_MB + FTuning.GUN_SLIDE_METAL_MB + FTuning.GUN_TRIGGER_METAL_MB
                + FTuning.GUN_GRIP_METAL_MB + feedMb();
    }

    /** The cylinder's or the tube's metal, in mB; none for any other feed. */
    private float feedMb() {
        return switch (layout.feed()) {
            case CYLINDER -> FTuning.GUN_CYLINDER_METAL_MB;
            case TUBE -> FTuning.GUN_TUBE_METAL_MB;
            default -> 0f;
        };
    }

    /**
     * What the parts off the barrel weigh, each in its own metal -- the feed's part and a double's
     * other barrel included; empty if one is not a gun metal.
     */
    private Optional<Frame> frame(Map<ResourceLocation, Material> materials) {
        Material slide = materials.get(parts.slide()), trigger = materials.get(parts.trigger()), grip = materials.get(parts.grip()),
                receiver = materials.get(parts.receiver()), feed = materials.get(parts.feed().orElse(parts.receiver())),
                other = materials.get(parts.second().map(SecondBarrel::material).orElse(material)),
                wrap = parts.wrap().map(materials::get).orElse(null);
        if (parts.wrap().isPresent() && (wrap == null || !wrapMaterial(wrap)))
            return Optional.empty();
        if (slide == null || trigger == null || grip == null || receiver == null || feed == null || other == null
                || !buildable(slide) || !buildable(trigger) || !gripMaterial(grip) || !buildable(receiver) || !buildable(feed)
                || !buildable(other))
            return Optional.empty();
        float mu = FTuning.GUN_RECEIVER_METAL_MB * receiver.density().muPerMb()
                + FTuning.GUN_SLIDE_METAL_MB * slide.density().muPerMb()
                + FTuning.GUN_TRIGGER_METAL_MB * trigger.density().muPerMb()
                + FTuning.GUN_GRIP_METAL_MB * grip.density().muPerMb()
                + feedMb() * feed.density().muPerMb();
        if (doubled())
            mu += tubeMu(parts.second().map(SecondBarrel::built).orElse(built.orElse(target())), other);
        for (var fitted : fittings.entrySet()) {
            Material fm = materials.get(fitted.getValue());
            if (fm == null || !buildable(fm))
                return Optional.empty();
            mu += fitted.getKey().metalMb() * fm.density().muPerMb();
        }
        return Optional.of(new Frame(mu, gripFriction(wrap != null ? wrap : grip)));
    }

    /**
     * A grip is held, not stressed, so it needs no yield strength: a gun metal, or anything
     * organic that has been weighed -- wood, bone, bamboo, turned on the lathe like a metal one.
     */
    public static boolean gripMaterial(Material m) {
        return buildable(m) || (m.kind().orElse(null) == Material.Kind.ORGANIC && m.density().muPerMb() > 0);
    }

    /**
     * What a grip can be wrapped in: something organic that is fibrous (leather, cloth, paper) or
     * sticky (honey). Read by property, never by item; a plank counts as a poor wrap and is allowed.
     */
    public static boolean wrapMaterial(Material m) {
        return m.kind().orElse(null) == Material.Kind.ORGANIC
                && (m.traits().contains(Material.Trait.FIBROUS) || m.traits().contains(Material.Trait.STICKY));
    }

    /** @param loose how much wider the bore is than the round, in mm; negative is a squeeze. */
    private Optional<GunFigures> bake(Material m, Frame frame, Material projectile, Load load, float loose) {
        if (!buildable(m) || projectile.density().muPerMb() <= 0 || load.coil() != (launcher == Launcher.COIL))
            return Optional.empty();
        double boreM = boreMmReal() / 1000.0, barrelM = barrelMReal();
        double area = Math.PI / 4 * boreM * boreM;

        float rating = m.mechanical().yieldStrength().get().value() * wallMmReal() / (boreMmReal() / 2f);
        // A round squeezed into too tight a bore makes more pressure; one rattling in too loose a
        // bore leaks it past (below). Precision is never a gate: a sloppy barrel is a worse gun.
        float peak = load.peakPressure() * (1 + Math.max(0, -loose) * FTuning.GUN_BORE_SQUEEZE_PER_MM);
        // A relief vent opens short of the rating: what the chamber sees never passes it, and the
        // charge's surplus goes out of the port and not into the gun or the hand.
        if (has(Fitting.VENT))
            peak = Math.min(peak, rating * FTuning.GUN_VENT_OPENS);
        double leak = Math.max(FTuning.GUN_BORE_LEAK_FLOOR, 1 - Math.max(0, loose) * FTuning.GUN_BORE_LEAK_PER_MM);

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
        work *= leak;
        if (has(Fitting.VENT))
            work *= 1 - FTuning.GUN_VENT_LEAK; // the port leaks a little of every shot

        // 1 m3 is 1000 mB. A bullet is its own bore's area over a length of some bores.
        double roundM = (boreMmReal() - loose) / 1000.0;
        double bulletMb = Math.PI / 4 * roundM * roundM * (roundM * FTuning.GUN_PROJECTILE_LENGTH_PER_BORE) * 1000;
        float bulletMu = (float) (bulletMb * projectile.density().muPerMb());
        float speedMs = (float) Math.sqrt(2 * work / bulletMu);

        // A brake throws the gas sideways: most of its share of the recoil is gone.
        float gas = FTuning.GUN_GAS_IMPULSE_FRACTION * (has(Fitting.BRAKE) ? 1 - FTuning.GUN_BRAKE_GAS_CUT : 1f);
        float impulse = bulletMu * speedMs * (1 + gas);

        // Barrel tube in its metal, the rest in theirs.
        float gunMu = tubeMu(new BarrelGeometry(boreMmReal(), boreMmReal() + 2 * wallMmReal(), (float) barrelM), m) + frame.mu();

        int cycleMs = Math.round(FTuning.GUN_CYCLE_BASE_MS + FTuning.GUN_CYCLE_MS_PER_IMPULSE * impulse)
                + layout.action().manualMs();
        float spread = Math.max(FTuning.GUN_SPREAD_FLOOR_DEG,
                FTuning.GUN_SPREAD_AT_MEDIUM_DEG * FTuning.GUN_BARREL_MEDIUM_M / (float) barrelM)
                + Math.max(0, loose) * FTuning.GUN_BORE_SPREAD_DEG_PER_MM;
        // Kick is recoil velocity, impulse over the gun's mass, and the grip's friction is how
        // much of it the hand lets through: a heavy gun with a grippy grip kicks least.
        float kick = FTuning.GUN_KICK_DEG_PER_IMPULSE * impulse * (FTuning.GUN_KICK_REFERENCE_MU / gunMu)
                * (FTuning.GUN_KICK_REFERENCE_FRICTION / frame.gripFriction());

        float report = has(Fitting.SUPPRESSOR) ? FTuning.GUN_SUPPRESSOR_REPORT : has(Fitting.BRAKE) ? FTuning.GUN_BRAKE_REPORT : 1f;
        float heatFactor = has(Fitting.SUPPRESSOR) ? FTuning.GUN_SUPPRESSOR_HEAT : 1f;
        float leakFactor = has(Fitting.SHROUD) ? FTuning.GUN_SHROUD_LEAK : 1f;
        return Optional.of(new GunFigures(new Pu(peak), new Pu(rating), speedMs / 20f, (float) work,
                new Mu(bulletMu), new Mu(gunMu), impulse, cycleMs, spread, kick, load.coil(), report, heatFactor, leakFactor));
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

    /**
     * The material a sample names for {@code part}: a grip also takes anything a grip can be
     * turned from (it has a grip form), since a grip carries no load.
     */
    public static Optional<ResourceLocation> sampleMaterial(Map<ResourceLocation, Material> materials, ItemStack sample, Part part) {
        if (part != Part.GRIP || sample.isEmpty())
            return sampleMaterial(materials, sample);
        return Material.find(materials, sample.getItem())
                .filter(l -> printable(l.id(), l.material())
                        || (gripMaterial(l.material()) && l.material().item(l.id(), "grip").isPresent()))
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
        return switch (barrelBand()) {
            case SHORT -> FTuning.GUN_COIL_BLOCKS_SHORT;
            case MEDIUM -> FTuning.GUN_COIL_BLOCKS_MEDIUM;
            case LONG -> FTuning.GUN_COIL_BLOCKS_LONG;
        };
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
            case SINGLE -> java.util.OptionalInt.of(1);
            case TUBE -> java.util.OptionalInt.of(switch (barrelBand()) {
                case SHORT -> FTuning.GUN_TUBE_ROUNDS_SHORT;
                case MEDIUM -> FTuning.GUN_TUBE_ROUNDS_MEDIUM;
                case LONG -> FTuning.GUN_TUBE_ROUNDS_LONG;
            });
            case BOX, BELT -> java.util.OptionalInt.empty();
        };
    }

    /**
     * The machined parts it is assembled from, by form, with how many of each (spec 12): a gun
     * barrel (two on a break-action shotgun), a receiver, the part its action works -- a slide on
     * a self-loading or automatic pistol, a bolt on anything else that is not a break action --
     * a trigger group, a grip, the feed's own part if it has one, and plates for a sight. Each
     * part's material is read off the item that fills it.
     */
    public Map<String, Integer> assembly() {
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("gun_barrel", layout.feed() == GunLayout.Feed.BARRELS && layout.type() == GunLayout.Type.SHOTGUN ? 2 : 1);
        out.put("receiver", 1);
        actionPart().ifPresent(p -> out.put(p, 1));
        out.put("trigger_group", 1);
        out.put("grip", 1);
        switch (layout.feed()) {
            case CYLINDER -> out.put("cylinder", 1);
            case TUBE -> out.put("magazine_tube", 1);
            default -> {
            }
        }
        if (sight.plates() > 0)
            out.put("plate", sight.plates());
        for (Fitting f : fittings.keySet())
            out.put(f.form(), 1);
        return out;
    }

    /** The form of the part the action works, if it has one. */
    public Optional<String> actionPart() {
        GunLayout.Action a = layout.action();
        if (a == GunLayout.Action.BREAK)
            return Optional.empty();
        boolean pistolSlide = layout.type() == GunLayout.Type.PISTOL
                && (a == GunLayout.Action.SELF_LOADING || a == GunLayout.Action.AUTOMATIC);
        return Optional.of(pistolSlide ? "slide" : "bolt");
    }

    /** Every metal this design names, barrel first, fittings last. */
    public List<ResourceLocation> materials() {
        List<ResourceLocation> out = new java.util.ArrayList<>(List.of(material, parts.slide(), parts.trigger(), parts.grip(),
                parts.receiver()));
        out.addAll(fittings.values());
        return out;
    }

    /** Whether the Stand may print this design: every part it plans in a gun metal, the grip in anything a grip can be. */
    public boolean printable(Map<ResourceLocation, Material> materials) {
        return materials().stream().allMatch(id -> materials.get(id) != null
                && (id.equals(parts.grip()) ? gripMaterial(materials.get(id)) : buildable(materials.get(id))));
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
                : bake(metal, frame.get(), bullet, launcher == Launcher.COIL ? Load.REFERENCE_COIL : Load.REFERENCE,
                boreMmReal() - boreMm);
    }

    public GunDesign withBore(int mm) {
        return new GunDesign(mm, barrel, wall, material, sight, launcher, parts, layout, built, fittings);
    }

    public GunDesign withBarrel(Barrel b) {
        return new GunDesign(boreMm, b, wall, material, sight, launcher, parts, layout, built, fittings);
    }

    public GunDesign withWall(Wall w) {
        return new GunDesign(boreMm, barrel, w, material, sight, launcher, parts, layout, built, fittings);
    }

    public GunDesign withSight(Sight s) {
        return new GunDesign(boreMm, barrel, wall, material, s, launcher, parts, layout, built, fittings);
    }

    public GunDesign withLauncher(Launcher l) {
        return new GunDesign(boreMm, barrel, wall, material, sight, l, parts, layout, built, fittings);
    }

    public GunDesign withParts(Parts p) {
        return new GunDesign(boreMm, barrel, wall, material, sight, launcher, p, layout, built, fittings);
    }

    /** The barrel the blueprint aims at: its bore, its wall on each side, its band's length. */
    public BarrelGeometry target() {
        return new BarrelGeometry(boreMm, boreMm + 2 * wall.mm(), barrel.metres());
    }

    /** As assembled from a barrel of this real shape: its bore, rounded, is what rounds are matched by. */
    public GunDesign withBuilt(BarrelGeometry g) {
        return new GunDesign(Math.round(g.boreMm()), barrel, wall, material, sight, launcher, parts, layout, Optional.of(g), fittings);
    }

    public GunDesign withLayout(GunLayout l) {
        return new GunDesign(boreMm, barrel, wall, material, sight, launcher, parts, l, built, fittings);
    }

    /** The barrel's metal; the other parts keep theirs. */
    public GunDesign withMaterial(ResourceLocation m) {
        return new GunDesign(boreMm, barrel, wall, m, sight, launcher, parts, layout, built, fittings);
    }

    public ResourceLocation material(Part part) {
        return switch (part) {
            case BARREL -> material;
            case SLIDE -> parts.slide();
            case TRIGGER -> parts.trigger();
            case GRIP -> parts.grip();
            case RECEIVER -> parts.receiver();
        };
    }

    public GunDesign withMaterial(Part part, ResourceLocation m) {
        return switch (part) {
            case BARREL -> withMaterial(m);
            case SLIDE -> withParts(parts.with(m, parts.trigger(), parts.grip(), parts.receiver()));
            case TRIGGER -> withParts(parts.with(parts.slide(), m, parts.grip(), parts.receiver()));
            case GRIP -> withParts(parts.with(parts.slide(), parts.trigger(), m, parts.receiver()));
            case RECEIVER -> withParts(parts.with(parts.slide(), parts.trigger(), parts.grip(), m));
        };
    }

    /** A first design for the test pistol: a 9 mm, medium barrel, standard wall. */
    public static GunDesign starter(ResourceLocation material) {
        return new GunDesign(9, Barrel.MEDIUM, Wall.STANDARD, material);
    }
}
