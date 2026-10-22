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
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A built gun as the physics reads it: the {@link ModularDesign} of a gun, with the receiver's
 * variant, the action and feed kinds, the barrel's real shape and the fitted options pulled out of
 * its parts ({@code docs/feedback_modular_items_spec.md} 8.3). Not stored and not a second design:
 * the design on the stack is the only record, and this is what {@link #of} reads off it.
 *
 * <p>It is also where the round-dependent physics lives, <em>moved</em> from the retired
 * {@code GunDesign} and not rewritten: {@link #bake(Map, Cartridge)} derives every figure of this
 * gun firing one round. The same parts in the same materials bake the same figures as they did
 * before the gun was a tool type. A pure function; nothing keys on an id.
 *
 * <p>What it models is interior ballistics cut down to three things a player can act on: the
 * chamber's rating (its metal and its wall), the pressure the charge makes, and how long the
 * barrel is that the pressure acts over (guns spec 3.2 and 3.3).
 *
 * @param material the barrel's metal.
 * @param barrel   the barrel's real shape: its part's {@code dims}.
 * @param sight    iron without a sight part, else the sight part's variant.
 * @param launcher a coil if a winding is on, else a chamber.
 * @param parts    the metals of the other parts.
 * @param layout   the receiver's variant, the action and the feed.
 * @param fittings the options fitted (suppressor, brake, vent, shroud, eddy sleeve), each to its metal.
 * @param traits   what the gun's parts give it (blowback, a bolt lockup, a belt feed...), which reshape the
 *                 figures it bakes ({@link ModularTraits#gunBaked}).
 */
public record GunSpec(ResourceLocation material, BarrelGeometry barrel, Sight sight, Launcher launcher, Parts parts,
                      GunLayout layout, Map<ResourceLocation, ResourceLocation> fittings, List<ModularTraits.Active> traits) {

    public static final int MIN_BORE_MM = 5, MAX_BORE_MM = 20;

    /** The roles of the gun type, by the letters {@code modular_type/gun.json} gives them. */
    private static final String BARREL = "B", SECOND = "D", RECEIVER = "R", SLIDE = "A", BOLT = "K", TRIGGER = "T", GRIP = "G",
            CYLINDER = "F", TUBE = "E", BELT = "Y", SIGHT = "S", WINDING = "W", WRAP = "X";
    private static final List<String> FITTED = List.of("M", "N", "Q", "V", "H");

    /** Whether the gun has the trait {@code id}. */
    public boolean has(String id) {
        return traits.stream().anyMatch(a -> a.id().equals(id));
    }

    /** Whether the option {@code f} is fitted. */
    public boolean has(ResourceLocation f) {
        return fittings.containsKey(f);
    }

    /** What throws the projectile: a gas in a sealed chamber, or a field along a coil. */
    public enum Launcher {
        CHAMBER, COIL
    }

    /** What the player looks through: how far in aiming zooms, and how long it takes to come up. */
    public enum Sight {
        IRON(FTuning.GUN_SIGHT_FOV_IRON, FTuning.GUN_SIGHT_MS_IRON),
        REFLEX(FTuning.GUN_SIGHT_FOV_REFLEX, FTuning.GUN_SIGHT_MS_REFLEX),
        SCOPE(FTuning.GUN_SIGHT_FOV_SCOPE, FTuning.GUN_SIGHT_MS_SCOPE);

        private final float fov;
        private final int raiseMs;

        Sight(float fov, int raiseMs) {
            this.fov = fov;
            this.raiseMs = raiseMs;
        }

        /** FOV multiplier when fully aimed. */
        public float fov() {
            return fov;
        }

        public int raiseMs() {
            return raiseMs;
        }
    }

    /** Barrel length, named so a designation can say "short" (read off the real length, never chosen). */
    public enum Band {
        SHORT(FTuning.GUN_BARREL_SHORT_M), MEDIUM(FTuning.GUN_BARREL_MEDIUM_M), LONG(FTuning.GUN_BARREL_LONG_M);

        private final float metres;

        Band(float metres) {
            this.metres = metres;
        }

        public float metres() {
            return metres;
        }
    }

    /**
     * The materials of the parts off the barrel. {@code slide} is whatever the action works -- a
     * slide or a bolt.
     *
     * @param feed   the metal of the feed's own part (a cylinder, a tube, a belt) when the gun has one.
     * @param second a double gun's other barrel, with its own metal and real shape: it weighs, and
     *               every other shot is fired down it ({@link #firing}).
     * @param wrap   what the grip is wrapped in, if anything: the hand holds the wrap, so its friction
     *               is the grip's.
     */
    public record Parts(ResourceLocation slide, ResourceLocation trigger, ResourceLocation grip, ResourceLocation receiver,
                        Optional<ResourceLocation> feed, Optional<Second> second, Optional<ResourceLocation> wrap) {
        Parts withSecond(Optional<Second> s) {
            return new Parts(slide, trigger, grip, receiver, feed, s, wrap);
        }
    }

    /** A double gun's other barrel: its metal, and its shape as machined. */
    public record Second(ResourceLocation material, BarrelGeometry built) {
    }

    /** Whether this gun has two barrels side by side. */
    public boolean doubled() {
        return parts.second().isPresent();
    }

    /**
     * The gun as the next shot sees it: a double fires its first barrel with two rounds in and its
     * second with one, so the round count says which barrel is loaded. Swapping the two keeps the
     * mass the same, because both tubes are always weighed.
     */
    public GunSpec firing(int roundsHeld) {
        if (roundsHeld != 1 || parts.second().isEmpty())
            return this;
        Second other = parts.second().get();
        return new GunSpec(other.material(), other.built(), sight, launcher,
                parts.withSecond(Optional.of(new Second(material, barrel))), layout, fittings, traits);
    }

    // --- reading a design -------------------------------------------------------------------------

    private static final Map<ModularDesign, Optional<GunSpec>> CACHE = new HashMap<>();
    private static int cachedFor = -1;

    /** The gun on {@code stack}, read off its design on whichever side this is; empty for anything else. */
    public static Optional<GunSpec> of(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return Optional.empty();
        int generation = MaterialTable.generation() * 31 + ModularTypes.generation();
        synchronized (CACHE) {
            if (cachedFor != generation || CACHE.size() > 64) {
                CACHE.clear();
                cachedFor = generation;
            }
            return CACHE.computeIfAbsent(design, d -> of(d, ModularTypes.known().get(d.type()), MaterialTable.get().materials()));
        }
    }

    /**
     * A gun from its parts, or empty if it is not one the physics can fire: a part names a material
     * the table lacks or that is no gun metal, the action and the feed do not work together, or a
     * second barrel sits on an action that is not a break.
     */
    public static Optional<GunSpec> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        if (type == null || type.family() != ModularType.Family.GUN)
            return Optional.empty();
        ModularDesign.Part barrel = design.part(BARREL), receiver = design.part(RECEIVER), trigger = design.part(TRIGGER),
                grip = design.part(GRIP), second = design.part(SECOND);
        ModularDesign.Part action = design.part(SLIDE) != null ? design.part(SLIDE) : design.part(BOLT);
        if (barrel == null || receiver == null || trigger == null || grip == null || action == null)
            return Optional.empty();
        String actionRole = design.part(SLIDE) != null ? SLIDE : BOLT;
        GunLayout.Action act = GunLayout.Action.of(type.roles().get(actionRole).variant(action).map(ModularType.Variant::name).orElse(""));
        GunLayout.Type kind = GunLayout.Type.of(type.roles().get(RECEIVER).variant(receiver).map(ModularType.Variant::name).orElse(""));
        ModularDesign.Part feedPart = firstOf(design, CYLINDER, TUBE, BELT);
        GunLayout.Feed feed = feedPart == null ? (act == GunLayout.Action.BREAK ? GunLayout.Feed.BARRELS
                : act == GunLayout.Action.BOLT || act == GunLayout.Action.LEVER ? GunLayout.Feed.SINGLE : GunLayout.Feed.BOX)
                : design.part(CYLINDER) != null ? GunLayout.Feed.CYLINDER : design.part(TUBE) != null ? GunLayout.Feed.TUBE
                : GunLayout.Feed.BELT;
        if (!feed.fits(act) || (second != null && (act != GunLayout.Action.BREAK || feed != GunLayout.Feed.BARRELS)))
            return Optional.empty();
        if (design.parts().stream().filter(p -> List.of(CYLINDER, TUBE, BELT).contains(p.role())).count() > 1)
            return Optional.empty();
        Material metal = materials.get(barrel.material());
        if (metal == null || !buildable(metal))
            return Optional.empty();
        for (ModularDesign.Part p : List.of(receiver, trigger, action))
            if (materials.get(p.material()) == null || !buildable(materials.get(p.material())))
                return Optional.empty();
        if (materials.get(grip.material()) == null || !gripMaterial(materials.get(grip.material())))
            return Optional.empty();
        if (feedPart != null && (materials.get(feedPart.material()) == null || !buildable(materials.get(feedPart.material()))))
            return Optional.empty();
        ModularDesign.Part wrap = design.part(WRAP);
        if (wrap != null && (materials.get(wrap.material()) == null || !wrapMaterial(materials.get(wrap.material()))))
            return Optional.empty();
        Optional<Second> other = Optional.empty();
        if (second != null) {
            Material sm = materials.get(second.material());
            if (sm == null || !buildable(sm))
                return Optional.empty();
            other = Optional.of(new Second(second.material(), geometry(type, SECOND, second)));
        }
        ModularDesign.Part winding = design.part(WINDING);
        Launcher launcher = Launcher.CHAMBER;
        if (winding != null) {
            Material wm = materials.get(winding.material());
            if (wm == null || wm.propellant().filter(p -> p.drive() == Material.Propellant.Drive.COIL).isEmpty())
                return Optional.empty();
            launcher = Launcher.COIL;
        }
        ModularDesign.Part sightPart = design.part(SIGHT);
        Sight sight = sightPart == null ? Sight.IRON
                : "scope".equals(type.roles().get(SIGHT).variant(sightPart).map(ModularType.Variant::name).orElse("")) ? Sight.SCOPE : Sight.REFLEX;
        Map<ResourceLocation, ResourceLocation> fittings = new HashMap<>();
        for (String letter : FITTED) {
            ModularDesign.Part p = design.part(letter);
            if (p == null)
                continue;
            Material fm = materials.get(p.material());
            Optional<GunOption> option = GunOptions.ofForm(p.kind());
            if (fm == null || !buildable(fm) || option.isEmpty())
                return Optional.empty();
            // One device to a slot: a suppressor and a brake cannot both be on the muzzle.
            String slot = option.get().slot();
            if (fittings.keySet().stream().anyMatch(o -> GunOptions.find(o).map(x -> x.slot().equals(slot)).orElse(false)))
                return Optional.empty();
            fittings.put(option.get().id(), p.material());
        }
        Parts parts = new Parts(action.material(), trigger.material(), grip.material(), receiver.material(),
                Optional.ofNullable(feedPart).map(ModularDesign.Part::material), other,
                Optional.ofNullable(wrap).map(ModularDesign.Part::material));
        return Optional.of(new GunSpec(barrel.material(), geometry(type, BARREL, barrel), sight, launcher, parts,
                new GunLayout(kind, act, feed), fittings, ModularTraits.allOf(design, type)));
    }

    private static ModularDesign.Part firstOf(ModularDesign design, String... roles) {
        for (String r : roles)
            if (design.part(r) != null)
                return design.part(r);
        return null;
    }

    /** The barrel part's real shape: its {@code dims}, else what its calibre class and the role default to. */
    private static BarrelGeometry geometry(ModularType type, String role, ModularDesign.Part barrel) {
        ModularType.Role r = type.roles().get(role);
        return new BarrelGeometry(r.dim(barrel, BarrelGeometry.BORE), r.dim(barrel, BarrelGeometry.OUTER), r.dim(barrel, BarrelGeometry.LENGTH));
    }

    // --- what the design is, in words the rest of the gun code uses ---------------------------------

    /** The bore a round must match, whole millimetres: the real bore rounded. */
    public int boreMm() {
        return Math.round(barrel.boreMm());
    }

    /** The bore the barrel really has. */
    public float boreMmReal() {
        return barrel.boreMm();
    }

    /** The length band the barrel is nearest: what a tube under it or a coil wound on it is sized by. */
    public Band barrelBand() {
        Band nearest = Band.SHORT;
        for (Band b : Band.values())
            if (Math.abs(b.metres() - barrel.lengthM()) < Math.abs(nearest.metres() - barrel.lengthM()))
                nearest = b;
        return nearest;
    }

    /** Every metal this gun names, barrel first, fittings last. */
    public List<ResourceLocation> materials() {
        List<ResourceLocation> out = new java.util.ArrayList<>(List.of(material, parts.slide(), parts.trigger(), parts.grip(),
                parts.receiver()));
        out.addAll(fittings.values());
        return out;
    }

    /**
     * Rounds the gun's own feed holds, chamber included, or empty when its rounds come in a
     * magazine item: a break action one per barrel, a lone chamber one, a cylinder its chambers, a tube as many as fit
     * under the barrel.
     */
    public java.util.OptionalInt internalCapacity() {
        return switch (layout.feed()) {
            case SINGLE -> java.util.OptionalInt.of(1);
            case BARRELS -> java.util.OptionalInt.of(doubled() ? 2 : 1);
            case CYLINDER -> java.util.OptionalInt.of(FTuning.GUN_CYLINDER_ROUNDS);
            case TUBE -> java.util.OptionalInt.of(switch (barrelBand()) {
                case SHORT -> FTuning.GUN_TUBE_ROUNDS_SHORT;
                case MEDIUM -> FTuning.GUN_TUBE_ROUNDS_MEDIUM;
                case LONG -> FTuning.GUN_TUBE_ROUNDS_LONG;
            });
            case BOX, BELT -> java.util.OptionalInt.empty();
        };
    }

    // --- the physics: moved from GunDesign ------------------------------------------------------------

    /**
     * Whether a gun can be made of {@code m}: a metal, weighed, with a yield strength to read the
     * chamber's rating from.
     */
    public static boolean buildable(Material m) {
        return m.kind().orElse(null) == Material.Kind.METAL && m.density().muPerMb() > 0
                && m.mechanical().yieldStrength().isPresent();
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

    /** What a barrel tube of this shape weighs in {@code m}: outer and inner diameter, a little over its length; 1 m3 = 1000 mB. */
    private static float tubeMu(BarrelGeometry g, Material m) {
        double bore = g.boreMm() / 1000.0, outer = g.outerMm() / 1000.0;
        return (float) (Math.PI / 4 * (outer * outer - bore * bore) * (g.lengthM() + 0.1) * 1000 * m.density().muPerMb());
    }

    /**
     * The parts off the barrel: what they weigh, how well the grip holds the hand, and how hard the
     * fitted sleeves drag a bullet that couples to them ({@link #eddyOf}, summed).
     */
    private record Frame(float mu, float gripFriction, float eddy) {
    }

    /**
     * A sleeve's drag: the option's own figure times how well its metal carries current
     * ({@code ampacity}, copper = 1), so a copper sleeve drags and a steel one barely does. Read by
     * property; nothing asks which metal it is.
     */
    private static float eddyOf(ResourceLocation option, Material metal) {
        return GunOptions.find(option).map(GunOption::eddy).orElse(0f) * metal.ampacity().orElse(0f);
    }

    /** A grip's friction; one never measured holds like the reference. */
    private static float gripFriction(Material grip) {
        return grip.mechanical().friction().orElse(FTuning.GUN_KICK_REFERENCE_FRICTION);
    }

    /** The feed's own part, in mB of metal; none for any other feed. */
    private float feedMb() {
        return switch (layout.feed()) {
            case CYLINDER -> FTuning.GUN_CYLINDER_METAL_MB;
            case TUBE -> FTuning.GUN_TUBE_METAL_MB;
            case BELT -> FTuning.GUN_BELT_METAL_MB;
            default -> 0f;
        };
    }

    /** What the parts off the barrel weigh, each in its own metal -- the feed's part and a double's other barrel included. */
    private Optional<Frame> frame(Map<ResourceLocation, Material> materials) {
        Material slide = materials.get(parts.slide()), trigger = materials.get(parts.trigger()), grip = materials.get(parts.grip()),
                receiver = materials.get(parts.receiver()), feed = materials.get(parts.feed().orElse(parts.receiver())),
                other = materials.get(parts.second().map(Second::material).orElse(material)),
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
            mu += tubeMu(parts.second().get().built(), other);
        float eddy = 0;
        for (var fitted : fittings.entrySet()) {
            Material fm = materials.get(fitted.getValue());
            if (fm == null || !buildable(fm))
                return Optional.empty();
            eddy += eddyOf(fitted.getKey(), fm);
            mu += GunOptions.find(fitted.getKey()).map(GunOption::metalMb).orElse(0f) * fm.density().muPerMb();
        }
        return Optional.of(new Frame(mu, gripFriction(wrap != null ? wrap : grip), eddy));
    }

    /** What the fitted options do to the bake, folded: every figure the gun owns and an option reaches. */
    private record Effects(float reliefAt, float bleed, float gasCut, float blast, float heat, float shed) {
    }

    private Effects effects() {
        float relief = Float.MAX_VALUE, keep = 1, gasKeep = 1, blast = 1, heat = 1, shed = 1;
        for (ResourceLocation id : fittings.keySet()) {
            var o = GunOptions.find(id);
            if (o.isEmpty())
                continue;
            GunOption f = o.get();
            relief = Math.min(relief, f.reliefAt().orElse(Float.MAX_VALUE));
            keep *= 1 - f.bleed();
            gasKeep *= 1 - f.gasCut();
            blast *= f.blast();
            heat *= f.heat();
            shed *= f.shed();
        }
        return new Effects(relief, 1 - keep, 1 - gasKeep, blast, heat, shed);
    }

    /**
     * Bake against the material table at the reference load: lead for a chamber, iron for a coil
     * (a slug has to couple to the field). Named once, here: this is what the stack's figures are
     * and what the spec sheet starts from; what is fired is baked per round.
     */
    public Optional<GunFigures> bake(Map<ResourceLocation, Material> materials) {
        Material metal = materials.get(material);
        Material bullet = materials.get(Feedback.id(launcher == Launcher.COIL ? "iron" : "lead"));
        Optional<Frame> frame = frame(materials);
        return metal == null || bullet == null || frame.isEmpty() ? Optional.empty()
                : bake(metal, frame.get(), bullet, launcher == Launcher.COIL ? Load.REFERENCE_COIL : Load.REFERENCE,
                        boreMmReal() - Math.round(boreMmReal()));
    }

    /**
     * The figures of this gun firing one particular round, derived now rather than stored: a gun
     * is a baked design, a round is what it is fed, and nothing keys on either's id.
     *
     * @return empty if the round is for another bore, or names a material that cannot fill its
     *         part.
     */
    public Optional<GunFigures> bake(Map<ResourceLocation, Material> materials, Cartridge round) {
        // Only a shotgun's open bore throws a pattern; it also takes a bullet of its bore, as a slug (spec 12).
        if (round.shot() && layout.type() != GunLayout.Type.SHOTGUN)
            return Optional.empty();
        // A built barrel takes what will chamber in the bore it really has, and how loose or tight
        // it sits is in the figures.
        float loose = boreMmReal() - round.boreMm();
        if (Math.abs(loose) > FTuning.GUN_BORE_FIT_MM)
            return Optional.empty();
        Material metal = materials.get(material), bullet = materials.get(round.projectile());
        Optional<Frame> frame = frame(materials);
        if (metal == null || bullet == null || frame.isEmpty())
            return Optional.empty();
        return round.load(materials).flatMap(load -> bake(metal, frame.get(), bullet, load, loose));
    }

    /** @param loose how much wider the bore is than the round, in mm; negative is a squeeze. */
    private Optional<GunFigures> bake(Material m, Frame frame, Material projectile, Load load, float loose) {
        if (!buildable(m) || projectile.density().muPerMb() <= 0 || load.coil() != (launcher == Launcher.COIL))
            return Optional.empty();
        double boreM = boreMmReal() / 1000.0, barrelM = barrel.lengthM();
        double area = Math.PI / 4 * boreM * boreM;

        float rating = m.mechanical().yieldStrength().get().value() * barrel.wallMm() / (boreMmReal() / 2f);
        // A round squeezed into too tight a bore makes more pressure; one rattling in too loose a
        // bore leaks it past (below). Precision is never a gate: a sloppy barrel is a worse gun.
        float peak = load.peakPressure() * (1 + Math.max(0, -loose) * FTuning.GUN_BORE_SQUEEZE_PER_MM);
        // A relief vent opens short of the rating: what the chamber sees never passes it, and the
        // charge's surplus goes out of the port and not into the gun or the hand.
        Effects fx = effects();
        if (fx.reliefAt() < Float.MAX_VALUE)
            peak = Math.min(peak, rating * fx.reliefAt());
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
        work *= 1 - fx.bleed(); // a port leaks a little of every shot

        // 1 m3 is 1000 mB. A bullet is its own bore's area over a length of some bores.
        double roundM = (boreMmReal() - loose) / 1000.0;
        double bulletMb = Math.PI / 4 * roundM * roundM * (roundM * FTuning.GUN_PROJECTILE_LENGTH_PER_BORE) * 1000;
        float bulletMu = (float) (bulletMb * projectile.density().muPerMb());
        float exitMs = (float) Math.sqrt(2 * work / bulletMu);
        // An eddy sleeve drags the bullet after it has left the barrel, so the recoil is the exit
        // speed's and the muzzle figures are the slowed one's: a bullet that does not couple to the
        // field (not magnetic) is dragged only a little. The energy goes into the sleeve as heat.
        float couple = projectile.separation().magnetic() ? 1f : FTuning.GUN_EDDY_NONMAGNETIC_COUPLING;
        float drained = Mth.clamp(frame.eddy() * couple, 0f, FTuning.GUN_EDDY_MAX_DRAIN);
        work *= 1 - drained;
        float speedMs = exitMs * (float) Math.sqrt(1 - drained);

        // A brake throws the gas sideways: most of its share of the recoil is gone.
        float gas = FTuning.GUN_GAS_IMPULSE_FRACTION * (1 - fx.gasCut());
        float impulse = bulletMu * exitMs * (1 + gas);

        // Barrel tube in its metal, the rest in theirs.
        float gunMu = tubeMu(new BarrelGeometry(boreMmReal(), boreMmReal() + 2 * barrel.wallMm(), (float) barrelM), m) + frame.mu();

        int cycleMs = Math.round(FTuning.GUN_CYCLE_BASE_MS + FTuning.GUN_CYCLE_MS_PER_IMPULSE * impulse)
                + layout.action().manualMs();
        float spread = Math.max(FTuning.GUN_SPREAD_FLOOR_DEG,
                FTuning.GUN_SPREAD_AT_MEDIUM_DEG * FTuning.GUN_BARREL_MEDIUM_M / (float) barrelM)
                + Math.max(0, loose) * FTuning.GUN_BORE_SPREAD_DEG_PER_MM;
        // Kick is recoil velocity, impulse over the gun's mass, and the grip's friction is how
        // much of it the hand lets through: a heavy gun with a grippy grip kicks least.
        float kick = FTuning.GUN_KICK_DEG_PER_IMPULSE * impulse * (FTuning.GUN_KICK_REFERENCE_MU / gunMu)
                * (FTuning.GUN_KICK_REFERENCE_FRICTION / frame.gripFriction());

        // Blast is the gas's, and a muzzle device works on it; the crack is the round's speed, which no
        // fitting sets (guns spec 13.3), so a suppressed supersonic round is still loud. Only a sleeve that
        // slows the bullet below sound quiets it, and it pays for that in muzzle energy and heat.
        float blast = fx.blast();
        float crack = Mth.clamp((speedMs / FTuning.SOUND_MS - (1 - FTuning.GUN_CRACK_RAMP)) / (2 * FTuning.GUN_CRACK_RAMP), 0f, 1f);
        float report = (1 - FTuning.GUN_CRACK_SHARE) * blast + FTuning.GUN_CRACK_SHARE * crack;
        float heatFactor = fx.heat() * (1 + drained * FTuning.GUN_EDDY_HEAT);
        float leakFactor = fx.shed();
        return Optional.of(ModularTraits.gunBaked(new GunFigures(new Pu(peak), new Pu(rating), speedMs / 20f, (float) work,
                new Mu(bulletMu), new Mu(gunMu), impulse, cycleMs, spread, kick, load.coil(), report, heatFactor, leakFactor), this));
    }
}
