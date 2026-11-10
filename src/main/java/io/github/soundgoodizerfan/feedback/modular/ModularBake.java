// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.combat.gear.GearTuning;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;

import net.minecraft.resources.ResourceLocation;

/**
 * A design and the material table in, figures out: a pure function, run once when an item is
 * assembled or changes and stored on the stack (the gun's {@code GunDesign.bake}; Silent Gear's
 * {@code GearData.recalculateGearData} and Tinkers' {@code ToolStack.rebuildStats} are the same
 * shape, taken). Nothing reads a stat table per material -- every figure is derived, and each
 * pulls on the others (spec 2.1):
 *
 * <ul>
 *   <li>mass is each part's volume times its material's density;</li>
 *   <li>how fast it swings falls with the inertia about the hand, which is mass and how far out
 *       it sits, and with the moment the arm has to hold out;</li>
 *   <li>a blow carries the striking mass at the tip's speed, so a heavier head hits harder and
 *       swings slower;</li>
 *   <li>bite is the edge's hardness against its strength, as it was ground;</li>
 *   <li>durability is the wearing edge's volume times that same competence;</li>
 *   <li>the joint carries the head's leverage on a handle and a binding of limited strength.</li>
 * </ul>
 *
 * <p>Two things from the spec are not here yet. Parts come out of the forge at their target size,
 * so there is no as-built geometry to read; and heat
 * treatment is the material (hardened steel is its own file, mechanics 5.3), so there is no
 * treatment state either.
 */
public final class ModularBake {

    private ModularBake() {
    }

    /** The edge competence of a material: how hard it is and how well it holds that hardness. */
    public static float competence(Material m) {
        return (float) Math.sqrt(m.hardness().value() * strength(m));
    }

    /** Yield strength, else a tenth of toughness (a crystal states only that), else a small floor. */
    public static float strength(Material m) {
        return m.mechanical().yieldStrength().map(s -> s.value())
                .or(() -> m.mechanical().toughness().map(t -> t.value() * 0.1f))
                .orElse(ModularTuning.STRENGTH_FALLBACK);
    }

    /** 1 for a material that gives under the hand, falling to 0 as it hardens. */
    private static float soft(Material m) {
        return 1f - GearTuning.saturate(m.hardness().value(), GearTuning.HARDNESS_HALF);
    }

    /** Toughness, else the strength it would have stood on. */
    private static float toughness(Material m) {
        return m.mechanical().toughness().map(t -> t.value()).orElse(strength(m));
    }

    /**
     * How much of its competence a piece made whole keeps against cracking: a material tougher than
     * a fifth of its hardness keeps all of it, a brittle one (diamond, quartz) keeps the square of
     * what it has. A whole-diamond blade is therefore possible, bites well and shatters in use --
     * bad, never forbidden (philosophy 7); a diamond set in tougher metal ({@link #zone}) is not
     * whole, so does not pay it.
     */
    static float fracture(Material m) {
        float r = Math.min(1f, toughness(m) / (m.hardness().value() * ModularTuning.FRACTURE_RATIO));
        return r * r;
    }

    /**
     * The wearing zone: what bites, what chips and what lasts.
     *
     * @param hardness gates what it can dig.
     * @param toughness decides what it chips on.
     * @param bite     the competence its bite reads, hardness against the strength behind it.
     * @param wear     the competence its durability reads.
     */
    record Zone(float hardness, float toughness, float bite, float wear) {}

    /**
     * The wearing zone of {@code body}, with {@code set} -- a tip or studs -- set into it if it is
     * harder. A hard piece on a tough body has the piece's hardness (as far as {@code hardnessShare}
     * of the zone is it) and the body's strength behind it, so it bites like the hard material
     * without being brittle like it: the diamond saw blade and the carbide insert. The piece
     * lends only {@code mixShare} of the zone's toughness and wear, because most of it is body.
     */
    static Zone zone(Material body, Material set, float hardnessShare, float mixShare) {
        return mix(new Zone(body.hardness().value(), toughness(body), competence(body), competence(body) * fracture(body)),
                body, set, hardnessShare, mixShare);
    }

    /** {@code zone} with a harder {@code set} mixed in, its bite still resting on {@code body}'s strength. */
    private static Zone mix(Zone zone, Material body, Material set, float hardnessShare, float mixShare) {
        if (set == null || set.hardness().value() <= zone.hardness())
            return zone;
        float h = zone.hardness() + (set.hardness().value() - zone.hardness()) * hardnessShare;
        return new Zone(h, zone.toughness() + (toughness(set) - zone.toughness()) * mixShare,
                (float) Math.sqrt(h * strength(body)), zone.wear() + (competence(set) - zone.wear()) * mixShare);
    }

    /**
     * How much of what a coat can do it does: it must have closed (thick enough), and a coat grown
     * burnt or mixed holds half as well. The same {@code Plating} the electrolytic cell grows, read
     * as properties of the metal that grew it -- no coating table of its own.
     */
    public static float coatHold(Plating coat) {
        float closed = Math.min(1f, coat.thickness() / FTuning.PLATING_POROUS_BELOW_QU) * coat.intact();
        return closed * (coat.quality() == Plating.Quality.BURNT || coat.quality() == Plating.Quality.MIXED
                ? ModularTuning.COAT_FAULTY_HOLD : 1f);
    }

    /**
     * Strain a coat of {@code m} takes before a hit chips it: the same {@link #fracture} test a whole part
     * is held to, so a brittle coat (glass) chips from the coat material's own toughness and a tough one
     * (zinc) never does.
     */
    public static float coatChipStrain(Material m) {
        return fracture(m) >= 1f ? Float.MAX_VALUE : toughness(m) * ModularTuning.CHIP_SLACK;
    }

    public static float bite(float competence, Edge edge) {
        float b = ModularTuning.BITE_BASE + ModularTuning.BITE_SLOPE * (float) Math.log(1 + competence);
        return Math.min(ModularTuning.BITE_MAX, Math.max(ModularTuning.BITE_MIN, b)) * edge.bite();
    }

    /**
     * The empty XP store a vessel design makes: its cells of stock times the material's {@code xp_store}
     * capacitance is the ceiling, read at the material's top level. Points {@code held} (a rebake of a
     * filled vessel) stay, clamped to the new ceiling. Empty if the material states no {@code xp_store}.
     */
    public static Optional<XpStore> xpStore(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials,
                                            XpStore held) {
        Part p = design.part(type.wearRole());
        Material m = p == null ? null : materials.get(p.material());
        if (m == null || m.xpStore().isEmpty())
            return Optional.empty();
        ModularType.Role role = type.roles().get(p.role());
        int cells = role.variant(p).map(ModularType.Variant::cells).filter(c -> c > 0).orElse(role.part().map(ModularType.Shaping::cells).orElse(1));
        Material.XpCapacity x = m.xpStore().get();
        XpStore empty = XpStore.empty(Math.max(1, Math.round(cells * x.capacitance())), x.topLevel());
        return Optional.of(held == null ? empty : empty.with(held.points()));
    }

    /**
     * Whether {@code material} can fill {@code roleKey} of a {@code type} and still bake: a battery's plates must state a
     * {@code secondary_cell} and its case must insulate, a machine's winding must conduct and its housing state a conductivity, a vessel's body must hold experience, a charge's filler must be a chemical propellant. Other families take any material their role accepts.
     */
    public static boolean suits(ModularType type, String roleKey, Material material) {
        if (material.traits().contains(Material.Trait.ASSEMBLY))
            return false;
        boolean wear = roleKey.equals(type.wearRole());
        return switch (type.family()) {
            case CELL -> wear ? material.electrical().secondaryCell().isPresent() : material.electrical().insulation().isPresent();
            case MACHINE -> switch (roleKey) {
                case MACHINE_WINDING -> material.ampacity().isPresent();
                case MACHINE_HOUSING -> material.thermal().conductivity().isPresent();
                default -> true;
            };
            case VESSEL -> !wear || material.xpStore().isPresent();
            case BELT -> !wear || (material.mechanical().friction().isPresent() && material.thermal().serviceTemperature().isPresent());
            case ENGINE -> engineMetal(material) && (!(roleKey.equals(ENGINE_PISTONS) || roleKey.equals(ENGINE_CRANK)) || material.mechanical().yieldStrength().isPresent());
            case GEARBOX -> engineMetal(material);
            // a module's wiring must conduct; its casing and core take any material their shapes accept
            case SUIT_MODULE -> !roleKey.equals("W") || material.ampacity().isPresent();
            case PROPELLER -> engineMetal(material) && (!roleKey.equals(PROP_BLADES) || material.mechanical().yieldStrength().isPresent());
            // A coilgun's winding is a conductor that holds a field and has a quench point: the rest cannot wind a gun.
            case GUN -> !roleKey.equals("W") || io.github.soundgoodizerfan.feedback.weapon.GunSpec.windable(material);
            case CHARGE -> wear || !io.github.soundgoodizerfan.feedback.weapon.Blast.isFiller(type, roleKey) || material.propellant().filter(p -> p.drive() != Material.Propellant.Drive.COIL).isPresent();
            default -> true;
        };
    }

    /** What a battery design is: its cell count, one cell's capacity in amp-ticks, the plates' volts and the case's band. */
    public record CellSpec(int cells, float capacity, Material.SecondaryCell plates, VoltageBand caseBand) {
    }

    /**
     * The battery a design makes: the plate part's cells of stock (two plates to a cell, {@link FTuning#BATTERY_INGOTS_PER_CELL}, unless its variant's {@code ingots_per_cell} param says 1 or 4)
     * give the cell count, each plate its material's {@code amp_ticks_per_ingot}, and the case material's {@code insulation} is the
     * highest voltage it holds off. Empty if the plates state no {@code secondary_cell} or the case does not insulate.
     */
    public static Optional<CellSpec> cell(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part plates = design.part(type.wearRole());
        Part shell = design.parts().stream().filter(p -> !p.role().equals(type.wearRole())).findFirst().orElse(null);
        Material pm = plates == null ? null : materials.get(plates.material());
        Material cm = shell == null ? null : materials.get(shell.material());
        if (pm == null || cm == null || pm.electrical().secondaryCell().isEmpty() || cm.electrical().insulation().isEmpty())
            return Optional.empty();
        ModularType.Role role = type.roles().get(plates.role());
        int stock = role.variant(plates).map(ModularType.Variant::cells).filter(c -> c > 0).orElse(role.part().map(ModularType.Shaping::cells).orElse(1));
        int perCell = role.variant(plates).map(v -> v.params().get("ingots_per_cell")).map(Math::round).orElse(FTuning.BATTERY_INGOTS_PER_CELL);
        int cells = Math.max(1, stock / perCell);
        Material.SecondaryCell sc = pm.electrical().secondaryCell().get();
        return Optional.of(new CellSpec(cells, sc.ampTicksPerIngot() * perCell, sc, cm.electrical().insulation().get()));
    }

    /**
     * The band a belt design makes: the band material's {@code mechanical.friction} times its construction's {@code grip} (a
     * textured face grips more, a link band less), plus a cleat's; and the hottest cargo it carries unharmed, the material's service
     * temperature times the construction's {@code heat}. Empty if the material states no friction or service temperature.
     */
    public static Optional<io.github.soundgoodizerfan.feedback.logistics.belt.BeltSpec> belt(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part band = design.part(type.wearRole());
        Material m = band == null ? null : materials.get(band.material());
        if (m == null || m.mechanical().friction().isEmpty() || m.thermal().serviceTemperature().isEmpty())
            return Optional.empty();
        var variant = type.roles().get(band.role()).variant(band);
        float grip = variant.map(v -> v.params().getOrDefault("grip", 1f)).orElse(1f);
        float heat = variant.map(v -> v.params().getOrDefault("heat", 1f)).orElse(1f);
        boolean cleat = design.parts().stream().anyMatch(p -> !p.role().equals(type.wearRole()));
        float friction = m.mechanical().friction().get() * grip + (cleat ? io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning.CLEAT_GRIP : 0f);
        return Optional.of(new io.github.soundgoodizerfan.feedback.logistics.belt.BeltSpec(band.material(), friction, m.thermal().serviceTemperature().get().value() * heat, cleat));
    }

    /** The role letters of the engine's four parts and the gearbox's three. */
    public static final String ENGINE_BLOCK = "K", ENGINE_PISTONS = "P", ENGINE_CRANK = "C", ENGINE_HEAD = "H";
    /** The role letters of the propeller's two parts. */
    public static final String PROP_HUB = "H", PROP_BLADES = "B";
    public static final String GEAR_SET = "G", GEAR_SYNCHRO = "S", GEAR_CASE = "H";

    /** A weighed metal: what an engine or gearbox part is cast from. */
    private static boolean engineMetal(Material m) {
        return m.kind().orElse(null) == Material.Kind.METAL && m.density().muPerMb() > 0;
    }

    /** A shape param of {@code part}'s variant, else {@code fallback}. */
    private static float param(ModularType type, Part part, String name, float fallback) {
        return type.roles().get(part.role()).variant(part).map(v -> v.params().getOrDefault(name, fallback)).orElse(fallback);
    }

    /**
     * How heavy the parts of {@code roles} are against the same cells of {@code ref}: each part's cells of stock times its
     * metal's density, over the cells times the reference density. A block cast of a light alloy round steel pistons weighs less.
     */
    private static float massBlend(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials, Material ref, String... roles) {
        float have = 0, cells = 0;
        for (String r : roles) {
            Part p = design.part(r);
            Material m = p == null ? null : materials.get(p.material());
            if (m == null)
                continue;
            int c = stockCells(type.roles().get(r), p);
            have += c * m.density().muPerMb();
            cells += c;
        }
        return cells == 0 || ref.density().muPerMb() <= 0 ? 1f : have / (cells * ref.density().muPerMb());
    }

    /**
     * The engine a design makes. The pistons' variant is the cylinder count and their {@code bore} dim the bore; the crank's
     * {@code stroke} dim the stroke; the head's {@code compression} param (0 low, 1 medium, 2 high) the compression, with its
     * own {@code torque}; the block's layout a {@code torque}, {@code rev} and {@code mass}; the crank's build a {@code rev}. The
     * pistons' metal is the engine's (it sets the rev limit); a crank of a weaker metal than the pistons caps the revs by the
     * square root of its specific strength against theirs, and the other parts' metals weigh the engine. Empty if a part is
     * missing or is not a weighed metal (the pistons and crank need a yield strength too).
     */
    public static Optional<io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign> engine(ModularDesign design, ModularType type,
                                                                                                     Map<ResourceLocation, Material> materials) {
        Part block = design.part(ENGINE_BLOCK), pistons = design.part(ENGINE_PISTONS), crank = design.part(ENGINE_CRANK), head = design.part(ENGINE_HEAD);
        if (block == null || pistons == null || crank == null || head == null)
            return Optional.empty();
        Material pm = materials.get(pistons.material()), cm = materials.get(crank.material());
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m == null || !suits(type, p.role(), m))
                return Optional.empty();
        }
        int cylinders = Math.round(param(type, pistons, "cylinders", 4f));
        int bore = Math.round(type.roles().get(ENGINE_PISTONS).axis(pistons, "bore"));
        int stroke = Math.round(type.roles().get(ENGINE_CRANK).axis(crank, "stroke"));
        var compression = io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign.Compression.values()[
                Math.max(0, Math.min(2, Math.round(param(type, head, "compression", 1f))))];
        float torque = param(type, block, "torque", 1f) * param(type, head, "torque", 1f);
        float rev = param(type, block, "rev", 1f) * param(type, crank, "rev", 1f);
        float pSpec = (float) Math.sqrt(pm.mechanical().yieldStrength().get().value() / pm.density().muPerMb());
        float cSpec = (float) Math.sqrt(cm.mechanical().yieldStrength().get().value() / cm.density().muPerMb());
        rev *= Math.min(1f, cSpec / pSpec);
        float mass = param(type, block, "mass", 1f) * massBlend(design, type, materials, pm, ENGINE_BLOCK, ENGINE_CRANK, ENGINE_HEAD);
        return Optional.of(new io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign(cylinders, bore, stroke, pistons.material(),
                compression, torque, rev, mass));
    }

    /**
     * The propeller a design makes. The hub's variant is the blade count ({@code blades}) and its {@code mass}; the blades' variant
     * is the pitch (0 fine, 1 medium, 2 coarse) and their {@code diameter} dim the sweep in cm. The blades' metal is the
     * propeller's (it sets the tip speed it survives); the hub's metal weighs it. Empty if a part is missing or a metal does not suit.
     */
    public static Optional<io.github.soundgoodizerfan.feedback.vehicle.air.PropDesign> propeller(ModularDesign design, ModularType type,
                                                                                                  Map<ResourceLocation, Material> materials) {
        Part hub = design.part(PROP_HUB), blades = design.part(PROP_BLADES);
        if (hub == null || blades == null)
            return Optional.empty();
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m == null || !suits(type, p.role(), m))
                return Optional.empty();
        }
        Material bm = materials.get(blades.material());
        int count = Math.round(param(type, hub, "blades", 3f));
        int diameter = Math.round(type.roles().get(PROP_BLADES).axis(blades, "diameter"));
        int pitch = Math.round(param(type, blades, "pitch", 1f));
        float mass = param(type, hub, "mass", 1f) * massBlend(design, type, materials, bm, PROP_HUB, PROP_BLADES);
        return Optional.of(new io.github.soundgoodizerfan.feedback.vehicle.air.PropDesign(count, diameter, pitch, blades.material(), mass));
    }

    /**
     * The gearbox a design makes. The gear set's variant is the gear count and its axes {@code g1} to {@code g6} the ratios (in
     * steps of 0.05, the grain of a transmission); the gear set's metal is the box's, which sets how fast a shift at speed wears it;
     * the synchronisers and the case each multiply the wear ({@code wear}) and the weight ({@code mass}), the other parts' metals
     * weighing it too. Empty if a part is missing or is not a weighed metal.
     */
    public static Optional<io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign> gearbox(ModularDesign design, ModularType type,
                                                                                                             Map<ResourceLocation, Material> materials) {
        Part gears = design.part(GEAR_SET), synchro = design.part(GEAR_SYNCHRO), box = design.part(GEAR_CASE);
        if (gears == null || synchro == null || box == null)
            return Optional.empty();
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m == null || !suits(type, p.role(), m))
                return Optional.empty();
        }
        ModularType.Role role = type.roles().get(GEAR_SET);
        int count = Math.round(param(type, gears, "gears", 4f));
        java.util.List<Integer> steps = new java.util.ArrayList<>();
        for (int g = 1; g <= count; g++)
            steps.add(Math.round(role.axis(gears, "g" + g) * io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign.PER_UNIT));
        float wear = param(type, synchro, "wear", 1f) * param(type, box, "wear", 1f);
        float mass = param(type, synchro, "mass", 1f) * param(type, box, "mass", 1f)
                * massBlend(design, type, materials, materials.get(gears.material()), GEAR_SET, GEAR_SYNCHRO, GEAR_CASE);
        return Optional.of(new io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign(steps, gears.material(), wear, mass));
    }

    /** The role letters of the dynamotor's four parts. */
    public static final String MACHINE_FRAME = "F", MACHINE_WINDING = "W", MACHINE_CORE = "C", MACHINE_HOUSING = "H", MACHINE_BEARING = "B";

    /**
     * What a dynamotor design is, read by the placed block.
     *
     * @param frameSu    the frame's torque ceiling, Su.
     * @param voltsPerRpm the winding's, which is also the Su each amp costs the shaft.
     * @param ratedAmps  the current it carries: the frame's ceiling over the winding's figure, or the winding conductor's, whichever is less.
     * @param band       the band it is built for: its volts per RPM at {@link FTuning#MACHINE_REFERENCE_RPM}.
     * @param mass       thermal mass of the frame, Work per Tu.
     * @param leak       the housing's shed to air, Work per tick per Tu (a finned one).
     * @param serviceTu  the hottest the housing runs before the machine wears.
     * @param windingDrop volts lost per amp over the whole winding (the conductor's drop times its length).
     * @param coreLoss   the share of throughput the core loses as heat.
     * @param jacketed   whether the housing is a water jacket.
     */
    public record MachineSpec(float frameSu, float voltsPerRpm, float ratedAmps, VoltageBand band, float mass, float leak,
                              float serviceTu, float windingDrop, float coreLoss, boolean jacketed) {
    }

    /** The cells of stock a part of {@code role} was shaped from: its variant's, else the standard shape's. */
    /**
     * What a powered-suit fitting gives the piece it is on: the servo's drive and loss come off its winding conductor (ampacity and
     * drop, as a machine's winding does), the gauge's step off its lens glass (the clearer, the finer, as a carried lens does), the sensor
     * head's reach off its cells of fibre, and the bay is the plate that holds the battery. A piece with none bakes zero.
     */
    private static void suitFitting(Map<Fig, Float> f, ModularType type, ModularType.Role role, Part part, Material m) {
        switch (role.name()) {
            case "servo" -> {
                int cells = stockCells(role, part);
                f.merge(Fig.SUIT_DRIVE, m.ampacity().orElse(0f) * cells * ModularTuning.SUIT_DRIVE_PER_AMPACITY, Float::sum);
                f.put(Fig.SUIT_LOSS, clamp(ModularTuning.SUIT_LOSS_FLOOR + m.electrical().drop().orElse(0f) * cells
                        * ModularTuning.SUIT_LOSS_PER_DROP, 0f, ModularTuning.SUIT_LOSS_MAX));
            }
            case "gauge" -> f.put(Fig.SUIT_GAUGE, Math.min(1f, 0.5f + 5f * m.separation().reflectivity().orElse(0.2f)));
            case "sensor_head" -> f.merge(Fig.SUIT_SENSE, (float) part.count(), Float::sum);
            case "battery_bay" -> f.put(Fig.SUIT_BAY, 1f);
            // the held module's own baked figures: what it is and how strongly it works, and how well it is wired
            case "upgrade" -> {
                ModularFigures held = ModularNest.figures(part);
                if (held != null && held.get(Fig.SUIT_ATT_1) > 0f) {
                    for (int i = 0; i < Fig.ATTACHMENTS; i++) {
                        f.put(Fig.att(i), held.get(Fig.att(i)));
                        f.put(Fig.pow(i), held.get(Fig.pow(i)));
                    }
                    f.put(Fig.SUIT_WIRING, held.get(Fig.SUIT_WIRING));
                }
            }
            default -> { }
        }
    }

    private static final ThreadLocal<Boolean> DRAWING = ThreadLocal.withInitial(() -> false);

    /** Sets whether this thread is baking a drawing (a blueprint's prediction), which has no components fitted. */
    static void drawing(boolean on) {
        DRAWING.set(on);
    }

    /**
     * What a suit module is: its attachments (each a part of one mechanism, cut in a variant that is the upgrade) and how hard each
     * works. A variant names the upgrade ({@code module} param) and the property of the attachment's material that makes it work,
     * with the figure that is full strength; the material's share of that figure is its power ({@code SUIT_POWER_MIN} to
     * {@code SUIT_POWER_MAX}). The wiring's conductor, over its cells, sets how much of the module's draw becomes waste and how many
     * attachments it can feed ({@code WIRING_AMPS_PER_ATTACHMENT}). Empty with no attachment, one that is not cut in an upgrade
     * shape, or more than the wiring feeds.
     */
    private static Optional<Map<Fig, Float>> suitModule(ModularType type, Map<String, Material> by, ModularDesign design) {
        Part wiring = design.part("W");
        if (wiring == null || by.get("W") == null)
            return Optional.empty();
        float amps = by.get("W").ampacity().orElse(0f) * stockCells(type.roles().get("W"), wiring);
        int budget = Math.min(Fig.ATTACHMENTS, Math.max(1, (int) (amps / ModularTuning.WIRING_AMPS_PER_ATTACHMENT)));
        // the support parts the module carries, each as a grade 0 to 1 read off the held item; an upgrade that needs one the module lacks is refused
        Map<String, Float> supports = new java.util.HashMap<>();
        for (Part p : design.parts()) {
            String name = type.roles().get(p.role()).name();
            if (name.equals("controller") || name.equals("cell") || name.equals("motor"))
                supports.put(name, supportGrade(name, p.sub()));
        }
        Map<Fig, Float> out = new EnumMap<>(Fig.class);
        int n = 0;
        for (Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            if (!role.name().startsWith("module_") || by.get(p.role()) == null)
                continue;
            Optional<ModularType.Variant> v = role.variant(p);
            if (v.isEmpty() || v.get().params().getOrDefault("module", 0f) <= 0f || n >= budget)
                return Optional.empty();
            Map<String, Float> k = v.get().params();
            float supported = 1f;
            for (String need : new String[] {"controller", "cell", "motor"})
                if (k.containsKey("needs_" + need)) {
                    Float grade = supports.get(need);
                    if (grade == null && DRAWING.get())
                        grade = ModularTuning.DRAWN_SUPPORT_GRADE;
                    if (grade == null)
                        return Optional.empty();
                    supported *= ModularTuning.SUPPORT_FLOOR + (1f - ModularTuning.SUPPORT_FLOOR) * grade;
                }
            Material m = by.get(p.role());
            float power = 1f;
            if (k.containsKey("thermal"))
                power = m.thermal().conductivity().map(c -> c.workPerTickPerTu()).orElse(0f) / k.get("thermal");
            else if (k.containsKey("electric"))
                power = m.ampacity().orElse(0f) / k.get("electric");
            else if (k.containsKey("competence"))
                power = competence(m) / k.get("competence");
            else if (k.containsKey("optic"))
                power = m.separation().reflectivity().orElse(0.2f) / k.get("optic");
            // A bored part (a cylinder, a nozzle) works as well as its bore is cut to the size the upgrade wants: uncut or overbored, it still
            // works, only worse. The cut comes off the part (a drill, per Fu), never a gate.
            if (k.containsKey("bore_target") && role.axes().containsKey("bore")) {
                float want = k.get("bore_target"), cut = p.axis("bore", role.axes().get("bore").def());
                float closeness = Math.max(0f, 1f - Math.abs(cut - want) / want);
                power *= ModularTuning.BORE_FLOOR + (1f - ModularTuning.BORE_FLOOR) * closeness;
            }
            out.put(Fig.att(n), k.get("module"));
            out.put(Fig.pow(n), clamp(power * supported, ModularTuning.SUIT_POWER_MIN, ModularTuning.SUIT_POWER_MAX));
            n++;
        }
        if (n == 0)
            return Optional.empty();
        out.put(Fig.SUIT_WIRING, ModularTuning.WIRING_WORST + (ModularTuning.WIRING_BEST - ModularTuning.WIRING_WORST) * (amps / (amps + ModularTuning.WIRING_HALF_AMPS)));
        return Optional.of(out);
    }

    /**
     * How good the held support {@code name} is, 0 to 1, from the item's own state: a controller is its circuit's speed times its quiet, a
     * cell is the capacity its plates hold against {@code SUPPORT_CELL_HALF}, a motor the amps it is rated for against {@code SUPPORT_MOTOR_HALF}.
     * The item is read, never named, so any circuit, cell or motor works.
     */
    private static float supportGrade(String name, net.minecraft.world.item.ItemStack held) {
        switch (name) {
            case "controller" -> {
                io.github.soundgoodizerfan.feedback.process.Circuit c = io.github.soundgoodizerfan.feedback.process.Circuit.of(held);
                return c.live() ? clamp(c.speed() * (1f - c.noise()), 0f, 1f) : 0f;
            }
            case "cell" -> {
                net.minecraft.nbt.CompoundTag t = io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack.state(held);
                float cap = t == null ? 0f : io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack.effectiveCapacity(t);
                return cap / (cap + ModularTuning.SUPPORT_CELL_HALF);
            }
            default -> {
                net.minecraft.nbt.CompoundTag t = held.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.DYNAMOTOR.get());
                float amps = t == null ? 0f : t.getFloat("RatedAmps");
                return amps / (amps + ModularTuning.SUPPORT_MOTOR_HALF);
            }
        }
    }

    private static int stockCells(ModularType.Role role, Part part) {
        return role.variant(part).map(ModularType.Variant::cells).filter(c -> c > 0).orElse(role.part().map(ModularType.Shaping::cells).orElse(1));
    }

    /** The name of the shape {@code part} was made in, the standard shape's base name when it has no variant. */
    private static String shapeName(ModularType.Role role, Part part) {
        return role.variant(part).map(ModularType.Variant::name).orElse("");
    }

    /**
     * The machine a design makes. The frame sets the torque ceiling, the thermal mass and the wall area; the winding sets volts
     * per RPM (its shape) and the current its conductor carries (the {@code ampacity} times its cells of wire); the core's
     * {@code electrical.core_loss} and the winding's {@code drop} are heat; the housing's {@code thermal.conductivity} over the
     * frame's area is the shed to air, and a jacketed housing carries water. Empty if the winding does not conduct or the housing
     * states no conductivity.
     */
    public static Optional<MachineSpec> machine(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part frame = design.part(MACHINE_FRAME), winding = design.part(MACHINE_WINDING), core = design.part(MACHINE_CORE), housing = design.part(MACHINE_HOUSING);
        if (frame == null || winding == null || core == null || housing == null)
            return Optional.empty();
        Material wm = materials.get(winding.material()), cm = materials.get(core.material()), hm = materials.get(housing.material());
        if (wm == null || cm == null || hm == null || wm.ampacity().isEmpty() || hm.thermal().conductivity().isEmpty())
            return Optional.empty();
        String size = shapeName(type.roles().get(MACHINE_FRAME), frame);
        float su = switch (size) {
            case "medium" -> FTuning.FRAME_SU_MEDIUM;
            case "large" -> FTuning.FRAME_SU_LARGE;
            default -> FTuning.FRAME_SU_SMALL;
        };
        float mass = switch (size) {
            case "medium" -> FTuning.FRAME_MASS_MEDIUM.workPerTu();
            case "large" -> FTuning.FRAME_MASS_LARGE.workPerTu();
            default -> FTuning.FRAME_MASS_SMALL.workPerTu();
        };
        float area = switch (size) {
            case "medium" -> FTuning.FRAME_AREA_MEDIUM;
            case "large" -> FTuning.FRAME_AREA_LARGE;
            default -> FTuning.FRAME_AREA_SMALL;
        };
        float vpr = switch (shapeName(type.roles().get(MACHINE_WINDING), winding)) {
            case "fine" -> FTuning.WINDING_VOLTS_PER_RPM_FINE;
            case "dense" -> FTuning.WINDING_VOLTS_PER_RPM_DENSE;
            case "heavy_dense" -> FTuning.WINDING_VOLTS_PER_RPM_HEAVY_DENSE;
            default -> FTuning.WINDING_VOLTS_PER_RPM_COARSE;
        };
        // An optional bearing sets how much torque the rotor carries and how much friction heat it makes (a sleeve is cheap and warm,
        // a ball bearing runs cool, a roller bearing carries the most).
        Part bearing = design.part(MACHINE_BEARING);
        var bv = bearing == null ? Optional.<ModularType.Variant>empty() : type.roles().get(MACHINE_BEARING).variant(bearing);
        su *= bv.map(v -> v.params().getOrDefault("su_mult", 1f)).orElse(1f);
        float friction = bv.map(v -> v.params().getOrDefault("friction", 0f)).orElse(0f);
        int wireCells = stockCells(type.roles().get(MACHINE_WINDING), winding);
        float amps = Math.min(su / vpr, wm.ampacity().get() * wireCells * FTuning.WINDING_AMPS_PER_AMPACITY);
        float drop = wm.electrical().drop().orElse(0f) * wireCells * FTuning.WINDING_BLOCKS_PER_CELL;
        float leak = hm.thermal().conductivity().get().workPerTickPerTu() * area * FTuning.MACHINE_SHED_PER_CONDUCTIVITY;
        float service = hm.thermal().serviceTemperature().map(Tu::value).orElse(FTuning.MACHINE_SERVICE_TU);
        return Optional.of(new MachineSpec(su, vpr, amps, ElectricRules.bandOf(vpr * FTuning.MACHINE_REFERENCE_RPM), mass, leak, service, drop,
                cm.electrical().coreLoss().orElse(FTuning.CORE_LOSS_DEFAULT) + friction, shapeName(type.roles().get(MACHINE_HOUSING), housing).equals("jacketed")));
    }

    public static int enchantability(Material m) {
        return m.enchantability().orElse(ModularTuning.ENCHANTABILITY_FALLBACK);
    }

    /** Which step of the wear ladder {@code worn} (0 new to 1 spent) is on. */
    public static int wearStep(float worn) {
        int step = 0;
        for (float edge : ModularTuning.WEAR_STEPS)
            if (worn >= edge)
                step++;
        return step;
    }

    /**
     * The figures {@code design} makes at {@code step} of wear, or empty if a part names a
     * material the table does not have (a datapack removed it).
     */
    public static Optional<ModularFigures> bake(ModularDesign design, ModularType type,
                                                Map<ResourceLocation, Material> materials, int step) {
        return bake(design, type, materials, step, null);
    }

    /** As {@link #bake(ModularDesign, ModularType, Map, int)}, for an item carrying {@code coat} (or none). */
    public static Optional<ModularFigures> bake(ModularDesign design, ModularType type,
                                                Map<ResourceLocation, Material> materials, int step, Plating coat) {
        Map<String, Material> by = new java.util.HashMap<>();
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m == null || !type.roles().containsKey(p.role()))
                return Optional.empty();
            by.put(p.role(), m);
        }
        if (type.family() == ModularType.Family.DISH)
            return dish(design, type, materials, step);
        Part wearPart = design.part(type.wearRole());
        if (wearPart == null)
            return Optional.empty();
        if (type.family() == ModularType.Family.GUN)
            return gun(design, type, by, materials, step);
        if (type.family() == ModularType.Family.CHARGE)
            return charge(design, type, by, materials, step);
        Map<Fig, Float> f = new EnumMap<>(Fig.class);
        float worn = ModularTuning.WEAR_FACTOR[Math.min(step, ModularTuning.WEAR_FACTOR.length - 1)];
        Material edge = by.get(type.wearRole());
        boolean armour = type.family() == ModularType.Family.ARMOUR;
        Part setPart = design.parts().stream().filter(p -> type.roles().get(p.role()).set()).findFirst().orElse(null);
        // Armour studs cover a share of the plate by how many there are; a tip is the whole edge.
        float studs = setPart == null ? 0 : Math.min(ModularTuning.STUD_MAX, setPart.count() * ModularTuning.STUD_SHARE);
        Zone zone = section(zone(edge, setPart == null ? null : by.get(setPart.role()), armour ? studs : 1f,
                armour ? studs : ModularTuning.TIP_SHARE), type.roles().get(type.wearRole()).section(wearPart)
                * (setPart == null ? 1f : type.roles().get(setPart.role()).section(setPart)),
                // An attunement can cost the part toughness (keen) or buy it back (ward on a plate): flaws, as forging leaves them.
                Math.max(0, Math.min(ForgeRhythm.MAX_FLAW, wearPart.flaw() + Attunements.flawShift(wearPart, type.roles().get(type.wearRole()).name()))));
        Material coatMetal = coat == null ? null : materials.get(coat.metal());
        float hold = coatMetal == null ? 0f : coatHold(coat);
        // A coat on armour is a skin of its metal over the whole plate; on a tool the edge is ground
        // bare, so a coat protects the flats from water and leaves the bite alone.
        if (armour)
            zone = mix(zone, edge, coatMetal, hold * ModularTuning.COAT_SURFACE_SHARE, hold * ModularTuning.COAT_SURFACE_SHARE);
        float comp = zone.bite();
        f.put(Fig.CORROSION, 1f - (1f - edge.corrosionResistance().orElse(0f))
                * (1f - hold * (coatMetal == null ? 0f : coatMetal.corrosionResistance().orElse(0f))));
        float durabilityComp = armour ? zone.wear() + ModularTuning.ARMOUR_DURABILITY_FLOOR : zone.wear();
        f.put(Fig.DURABILITY, (float) Math.max(1, Math.round(ModularTuning.DURABILITY_K * durabilityComp * type.wearLitres())));
        f.put(Fig.ENCHANTABILITY, (float) enchantability(by.get(type.primary()) != null ? by.get(type.primary()) : edge));
        f.put(Fig.HARDNESS, zone.hardness());
        f.put(Fig.EDGE_TOUGHNESS, zone.toughness());
        switch (type.family()) {
            case DIG, BLADE, MACE, SPEAR, SHEARS -> melee(design, type, by, zone, f, worn);
            case BOW, CROSSBOW -> {
                float drive = 0;
                float string = 0;
                float total = 0;
                for (Part p : design.parts()) {
                    ModularType.Role r = type.roles().get(p.role());
                    total += mass(r, p, by.get(p.role()));
                    if (r.name().equals("string"))
                        string = strength(by.get(p.role()));
                    else if (r.name().equals("limbs") || r.name().equals("prod"))
                        drive = strength(by.get(p.role()));
                }
                float held = Math.min(drive, ModularTuning.STRING_SHARE * string);
                float ratio = held / ModularTuning.DRAW_REFERENCE_STRENGTH;
                f.put(Fig.MASS, total);
                f.put(Fig.VELOCITY, clamp((float) Math.pow(ratio, ModularTuning.VELOCITY_EXPONENT) * worn,
                        ModularTuning.VELOCITY_MIN, ModularTuning.VELOCITY_MAX));
                f.put(Fig.DRAW, clamp(ModularTuning.DRAW_REFERENCE_TICKS * (float) Math.sqrt(ratio),
                        ModularTuning.DRAW_MIN_TICKS, ModularTuning.DRAW_MAX_TICKS));
                f.put(Fig.JOINT, string > 0 ? drive / (ModularTuning.STRING_SHARE * string) : 0f);
            }
            case ARMOUR -> {
                Part plate = wearPart;
                float share = comp / (comp + ModularTuning.ARMOUR_HALF_COMPETENCE);
                float cover = Math.min(1f, plate.count() / (float) type.canonicalCounts().getOrDefault(type.wearRole(), plate.count()));
                float points = type.armourMax() * share * type.roles().get(type.wearRole()).factor(plate.form()) * cover;
                float total = 0;
                for (Part p : design.parts()) {
                    ModularType.Role r = type.roles().get(p.role());
                    total += mass(r, p, by.get(p.role()));
                    if (r.name().equals("lining")) {
                        float c = competence(by.get(p.role()));
                        points += type.armourMax() * ModularTuning.LINING_SHARE * c / (c + ModularTuning.ARMOUR_HALF_COMPETENCE);
                    }
                    suitFitting(f, type, r, p, by.get(p.role()));
                }
                f.put(Fig.MASS, total);
                f.put(Fig.ARMOUR, points * worn);
                f.put(Fig.ARMOUR_TOUGHNESS, ModularTuning.TOUGHNESS_MAX * Math.max(0f,
                        (share - ModularTuning.TOUGHNESS_FROM_SHARE) / (1 - ModularTuning.TOUGHNESS_FROM_SHARE)));
                // Boots meet the ground: a coat grippier than the plate under it plants the foot, a slicker one loses it.
                float grip = type.slot() != ModularType.Slot.FEET || coatMetal == null ? 0f
                        : hold * ModularTuning.COAT_GRIP_PER_FRICTION * (coatMetal.mechanical().friction().orElse(0f)
                        - edge.mechanical().friction().orElse(0f));
                f.put(Fig.KNOCKBACK_RESISTANCE, clamp((edge.density().muPerMb() - ModularTuning.KNOCKBACK_DENSITY_FLOOR)
                        * ModularTuning.KNOCKBACK_PER_DENSITY + grip, 0f, ModularTuning.KNOCKBACK_MAX));
                f.put(Fig.SLOW, clamp(Math.max(0f, total - ModularTuning.SLOW_FREE_MASS) * ModularTuning.SLOW_PER_KG,
                        0f, ModularTuning.SLOW_MAX));
            }
            case SHIELD -> {
                float total = 0;
                float held = 1f;
                for (Part p : design.parts()) {
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                    held *= param(type.roles().get(p.role()), p, "capacity_mult");
                }
                f.put(Fig.MASS, total);
                f.put(Fig.CAPACITY, (ModularTuning.SHIELD_BASE_CAPACITY + comp * worn) * held);
            }
            case HAND -> {
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
                // A heavier, harder, tougher head lands a harder blow; a worn one has lost some of it.
                f.put(Fig.STRIKE_ST, FTuning.HAND_ST_PER_MU_COMPETENCE * total * zone.wear() * worn * type.strike()
                        * type.roles().get(type.primary()).strike(design.part(type.primary())));
            }
            case GLOVE -> {
                float total = 0;
                float plateMass = 0;
                for (Part p : design.parts()) {
                    float m = mass(type.roles().get(p.role()), p, by.get(p.role()));
                    total += m;
                    if (p.role().equals(ModularGlove.PLATE))
                        plateMass += m;
                }
                f.put(Fig.MASS, total);
                // The metacarpal plate does it all; a bare glove has none. Hard and dense both hit harder: a light hard plate or a heavy soft one is half as good.
                Material plate = by.get(ModularGlove.PLATE);
                if (plate != null) {
                    ModularType.Role plateRole = type.roles().get(ModularGlove.PLATE);
                    ModularDesign.Part plateMade = design.part(ModularGlove.PLATE);
                    float strike = plateRole.strike(plateMade);
                    float cover = plateRole.variant(plateMade).map(v -> v.params().get("armour")).orElse(1f);
                    float hard = GearTuning.saturate(plate.hardness().value(), GearTuning.HARDNESS_HALF);
                    f.put(Fig.FIST, GearTuning.GLOVE_FIST * worn * strike * (hard + GearTuning.saturate(plate.density().muPerMb(), GearTuning.DENSITY_HALF)) / 2f);
                    f.put(Fig.ARMOUR, GearTuning.GLOVE_ARMOUR * worn * cover * hard);
                    f.put(Fig.SWING_DRAG, Math.min(GearTuning.GLOVE_DRAG_MAX, plateMass * GearTuning.GLOVE_DRAG_PER_MU));
                }
            }
            case BACKPACK -> {
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
                Part shell = design.part(ModularBackpack.SHELL);
                Material strap = by.get(ModularBackpack.STRAP);
                f.put(Fig.SLOTS, shell == null ? ModularTuning.PACK_DEFAULT_SLOTS : type.roles().get(ModularBackpack.SHELL).variant(shell)
                        .map(v -> v.params().get("slots")).orElse(ModularTuning.PACK_DEFAULT_SLOTS));
                // Fittings take mounting points from the shell (and the frame, which gives more); a hip belt hangs from a frame.
                int budget = (shell == null ? ModularTuning.PACK_DEFAULT_POINTS : type.roles().get(ModularBackpack.SHELL).variant(shell)
                        .map(v -> Math.round(v.params().get("points"))).orElse(ModularTuning.PACK_DEFAULT_POINTS))
                        + (design.part(ModularBackpack.FRAME) == null ? 0 : ModularTuning.PACK_FRAME_POINTS);
                int used = 0;
                int stores = 0;
                for (Part p : design.parts()) {
                    used += ModularBackpack.cost(p.role());
                    stores += ModularBackpack.STORES.getOrDefault(p.role(), 0);
                }
                if (used > budget || (design.part(ModularBackpack.BELT) != null && design.part(ModularBackpack.FRAME) == null))
                    return Optional.empty();
                f.put(Fig.POINTS, (float) (budget - used));
                f.put(Fig.STORES, (float) stores);
                float base = strap == null ? 0f : strength(strap) * ModularTuning.PACK_CARRY_PER_STRENGTH * worn;
                float carry = base;
                if (by.get(ModularBackpack.PANEL) != null)
                    carry += base * ModularTuning.PACK_PANEL_GAIN
                            * (1f - GearTuning.saturate(by.get(ModularBackpack.PANEL).hardness().value(), GearTuning.HARDNESS_HALF));
                if (by.get(ModularBackpack.FRAME) != null)
                    carry += strength(by.get(ModularBackpack.FRAME)) * ModularTuning.PACK_FRAME_CARRY;
                if (by.get(ModularBackpack.BELT) != null)
                    carry += strength(by.get(ModularBackpack.BELT)) * ModularTuning.PACK_BELT_CARRY * worn;
                f.put(Fig.CARRY, carry);
            }
            case BEDROLL -> {
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
                // Softness of what you lie on and what fills it: the fill counts most.
                Material fill = by.get(ModularBedroll.FILL);
                Material sheet = by.get(ModularBedroll.SHEET);
                f.put(Fig.COMFORT, worn * (0.7f * (fill == null ? 0f : soft(fill)) + 0.3f * (sheet == null ? 0f : soft(sheet))));
            }
            case HEAD -> {
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
                // Heavier, harder and tougher lifts the ceiling; brittleness has already cost the competence its square.
                f.put(Fig.STRIKE_ST, FTuning.HEAD_ST_PER_MU_COMPETENCE * total * zone.wear()
                        * type.roles().get(type.wearRole()).strike(wearPart));
            }
            case SUIT_MODULE -> {
                Optional<Map<Fig, Float>> module = suitModule(type, by, design);
                if (module.isEmpty())
                    return Optional.empty();
                f.putAll(module.get());
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
            }
            case CELL -> {
                if (cell(design, type, materials).isEmpty())
                    return Optional.empty();
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
            }
            case MACHINE -> {
                if (machine(design, type, materials).isEmpty())
                    return Optional.empty();
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
            }
            case ENGINE, GEARBOX, PROPELLER -> {
                if (switch (type.family()) {
                    case ENGINE -> engine(design, type, materials).isEmpty();
                    case PROPELLER -> propeller(design, type, materials).isEmpty();
                    default -> gearbox(design, type, materials).isEmpty();
                })
                    return Optional.empty();
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
            }
            case BELT -> {
                if (belt(design, type, materials).isEmpty())
                    return Optional.empty();
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
            }
            case VESSEL -> {
                if (xpStore(design, type, materials, null).isEmpty())
                    return Optional.empty();
                f.put(Fig.MASS, mass(type.roles().get(wearPart.role()), wearPart, by.get(wearPart.role())));
            }
            default -> { }
        }
        return Optional.of(ModularFigures.of(f, step));
    }

    /**
     * A gun: the physics of {@link GunSpec} at the reference load, written as figures. The round-dependent half
     * is {@link GunSpec#bake(Map, io.github.soundgoodizerfan.feedback.weapon.Cartridge)}, run at the shot and on
     * the spec sheet. Empty if the parts are not a gun the physics can fire (an action the feed does not suit).
     */
    private static Optional<ModularFigures> gun(ModularDesign design, ModularType type, Map<String, Material> by,
                                                Map<ResourceLocation, Material> materials, int step) {
        Optional<GunFigures> baked = GunSpec.of(design, type, materials).flatMap(spec -> spec.bake(materials));
        if (baked.isEmpty())
            return Optional.empty();
        Map<Fig, Float> f = new EnumMap<>(Fig.class);
        f.putAll(baked.get().figs());
        f.put(Fig.ENCHANTABILITY, (float) enchantability(by.get(type.primary())));
        return Optional.of(ModularFigures.of(f, step));
    }

    /**
     * A charge: its filler is a blend of propellants by count and its casing holds the gas, and
     * {@link io.github.soundgoodizerfan.feedback.weapon.Blast} reads both. Empty if the filler is not a propellant,
     * so the Tool Table refuses a charge of sugar.
     */
    private static Optional<ModularFigures> charge(ModularDesign design, ModularType type, Map<String, Material> by,
                                                   Map<ResourceLocation, Material> materials, int step) {
        Optional<io.github.soundgoodizerfan.feedback.weapon.Blast.Packed> read = io.github.soundgoodizerfan.feedback.weapon.Blast.read(design, type, materials);
        List<io.github.soundgoodizerfan.feedback.weapon.Cartridge.Charge> filler = new java.util.ArrayList<>();
        for (Part p : design.parts())
            if (io.github.soundgoodizerfan.feedback.weapon.Blast.isFiller(type, p.role()))
                filler.add(new io.github.soundgoodizerfan.feedback.weapon.Cartridge.Charge(p.material(), p.count()));
        Optional<io.github.soundgoodizerfan.feedback.weapon.Cartridge.Blend> blend =
                io.github.soundgoodizerfan.feedback.weapon.Cartridge.Blend.of(materials, filler).filter(b -> !b.coil());
        if (read.isEmpty() || blend.isEmpty() || blend.get().total() > FTuning.DEMO_FILLER_MAX)
            return Optional.empty();
        Map<Fig, Float> f = new EnumMap<>(Fig.class);
        f.put(Fig.BLAST, read.get().power());
        f.put(Fig.BURNED, blend.get().burned());
        f.put(Fig.PACKED, (float) blend.get().total());
        float mass = 0;
        for (Part p : design.parts())
            mass += mass(type.roles().get(p.role()), p, materials.get(p.material()));
        f.put(Fig.MASS, mass);
        return Optional.of(ModularFigures.of(f, step));
    }

    /** A dish: its hunger, saturation and hazards from {@link DishBake}. Empty with no food in it, so the table refuses an empty bowl. */
    private static Optional<ModularFigures> dish(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials, int step) {
        return DishBake.of(design, type, materials).map(d -> {
            Map<Fig, Float> f = new EnumMap<>(Fig.class);
            f.put(Fig.HUNGER, d.hunger());
            f.put(Fig.SATURATION, d.saturation());
            f.put(Fig.PATHOGEN, d.pathogen());
            f.put(Fig.TOXIN, d.toxin());
            f.put(Fig.CHARRED, d.charred());
            f.put(Fig.FAT, d.fat());
            f.put(Fig.FIBRE, d.fibre());
            f.put(Fig.LITRES, d.litres());
            return ModularFigures.of(f, step);
        });
    }

    /** The number a part's shape names under {@code name} (a binding's joint, a rim's capacity), as a multiple: 1 for none. */
    static float param(ModularType.Role role, Part part, String name) {
        return role.variant(part).map(v -> v.params().getOrDefault(name, 1f)).orElse(1f);
    }

    static float mass(ModularType.Role role, Part part, Material m) {
        if (!part.sub().isEmpty()) {
            ModularFigures held = ModularNest.figures(part);
            return held == null ? 0f : held.get(Fig.MASS);
        }
        return role.litres() * part.count() * m.density().muPerMb() * role.factor(part.form()) * role.kept(part);
    }

    /**
     * {@code zone} as the part was made: a section {@code thickness} times the standard (thin bites,
     * thick resists chipping), and a forging flaw, each step of which costs toughness.
     */
    private static Zone section(Zone zone, float thickness, int flaw) {
        if (thickness == 1f && flaw == 0)
            return zone;
        return new Zone(zone.hardness(), zone.toughness() * thickness * (1f - ModularTuning.FLAW_TOUGHNESS_LOSS * flaw),
                zone.bite() * (float) Math.pow(thickness, -ModularTuning.SECTION_BITE_EXPONENT),
                zone.wear() * (1f - ModularTuning.FLAW_DURABILITY_LOSS * flaw));
    }

    /**
     * Mass, balance, swing, blow, bite, damage, dig speed, reach and joint for a hand-swung head on a
     * handle. A twin-headed tool has two heads ({@link ModularType#strikers}); each is struck, dug
     * and worn as its own kind says ({@link ModularHeads}), the swing is the whole tool's, so a
     * second head costs speed and impact with no figure written for it (spec 5.3), and the blow
     * that lands is the better of the two.
     */
    private static void melee(ModularDesign design, ModularType type, Map<String, Material> by, Zone zone,
                              Map<Fig, Float> f, float worn) {
        List<String> heads = type.strikers().isEmpty() ? List.of(type.wearRole()) : type.strikers();
        float rodLength = 0;
        for (Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            rodLength += r.length() * p.count();
        }
        float total = 0;
        float feltTotal = 0;
        float inertia = 0;
        float moment = 0;
        float struck = 0;
        float rodMass = 0;
        float reachLength = rodLength;
        Map<String, Float> headMass = new java.util.HashMap<>();
        for (Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            float m = mass(r, p, by.get(p.role()));
            total += m;
            // What the hand feels: a featherlight part counts for less (spec 2.1a); the blow and MASS stay real.
            float felt = m * MaterialPerks.heft(by.get(p.role()));
            feltTotal += felt;
            if (r.rod()) {
                inertia += felt * rodLength * rodLength / 3;
                moment += felt * rodLength / 2;
                rodMass += m;
                continue;
            }
            float d = r.at() + r.centre(p) + (r.rides() ? rodLength : 0);
            inertia += felt * d * d;
            moment += felt * d;
            reachLength = Math.max(reachLength, r.at() + r.reach(p) + (r.rides() ? rodLength : 0));
            if (heads.contains(p.role())) {
                struck += m;
                headMass.put(p.role(), m);
            }
        }
        if (struck == 0)
            struck = rodMass;
        float swing = clamp(ModularTuning.SWING_K / (float) Math.pow(inertia + ModularTuning.BALANCE_K * moment,
                ModularTuning.SWING_EXPONENT), ModularTuning.SWING_MIN, ModularTuning.SWING_MAX);
        float tip = reachLength * swing * (float) Math.PI;
        float bestDamage = -1;
        float wear = 0;
        for (int i = 0; i < heads.size(); i++) {
            String role = heads.get(i);
            Part head = design.part(role);
            Zone z = i == 0 ? zone : head == null || by.get(role) == null ? zone
                    : section(zone(by.get(role), null, 1f, 1f), type.roles().get(role).section(head), head.flaw());
            float mass = headMass.getOrDefault(role, rodMass);
            float impact = 0.5f * (mass + ModularTuning.ROD_STRIKE_SHARE * rodMass) * tip * tip;
            float bite = bite(z.bite(), design.edge());
            float strike = type.strike();
            if (heads.size() > 1 && head != null)
                strike = ModularHeads.of(ModularHeads.kindOf(head, type.roles().get(role))).map(ModularHeads::strike).orElse(1f);
            if (head != null)
                strike *= type.roles().get(role).strike(head);
            float damage = 1 + ModularTuning.DAMAGE_K * strike * bite * (float) Math.sqrt(impact) * worn;
            float dig = ModularTuning.DIG_K * bite * (float) Math.sqrt(impact) * worn;
            wear += z.wear();
            if (i > 0) {
                f.put(Fig.DIG_SPEED_OFF, dig);
                f.put(Fig.HARDNESS_OFF, z.hardness());
                f.put(Fig.EDGE_TOUGHNESS_OFF, z.toughness());
            } else {
                f.put(Fig.DIG_SPEED, dig);
            }
            if (damage > bestDamage) {
                bestDamage = damage;
                f.put(Fig.IMPACT, impact);
                f.put(Fig.BITE, bite);
                f.put(Fig.DAMAGE, damage);
            }
        }
        if (heads.size() > 1)
            f.put(Fig.DURABILITY, (float) Math.max(1, Math.round(ModularTuning.DURABILITY_K * wear * type.wearLitres())));
        f.put(Fig.MASS, total);
        f.put(Fig.BALANCE, feltTotal > 0 && reachLength > 0 ? Math.min(1f, moment / (feltTotal * reachLength)) : 0f);
        f.put(Fig.SWING, swing);
        f.put(Fig.ATTACK_SPEED, swing * worn);
        f.put(Fig.REACH, Math.max(0, reachLength - ModularTuning.REACH_REFERENCE) * ModularTuning.REACH_PER_METRE);
        f.put(Fig.JOINT, joint(design, type, by, struck, reachLength, rodLength, heads));
    }

    /** The strength a joint can lean on in {@code m}: a binding counts for its share of its own strength, any other part for all of it. */
    private static float jointStrength(ModularType.Role r, Part p, Material m) {
        if (r.name().equals("binding"))
            return (m.kind().filter(k -> k == Material.Kind.METAL).isPresent()
                    ? ModularTuning.PINNED_SHARE : ModularTuning.WRAPPED_SHARE) * strength(m) * param(r, p, "joint_mult");
        return strength(m);
    }

    /** The weakest part a swung head hangs on, and what the binding adds. */
    private record JointCapacity(float weakest, float binding) {
    }

    private static JointCapacity jointCapacity(ModularDesign design, ModularType type, Map<String, Material> by, List<String> heads) {
        float weakest = Float.MAX_VALUE;
        float binding = 0;
        for (Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            if (r.name().equals("binding"))
                binding = jointStrength(r, p, by.get(p.role()));
            else if (r.rod() || heads.contains(p.role()))
                weakest = Math.min(weakest, strength(by.get(p.role())));
        }
        return new JointCapacity(weakest, binding);
    }

    /** Leverage on the joint over what the handle, head and any binding carry together. */
    private static float joint(ModularDesign design, ModularType type, Map<String, Material> by, float struck,
                               float reachLength, float rodLength, List<String> heads) {
        if (rodLength == 0)
            return 0;
        JointCapacity c = jointCapacity(design, type, by, heads);
        float capacity = ModularTuning.JOINT_K * (c.weakest() + c.binding());
        return capacity > 0 ? struck * reachLength / capacity : 0;
    }

    /**
     * The joint figure of each pair of roles ({@code pairs}, role letters), the load a pair carries over what it can
     * carry, 1 being its limit, for the Blueprint Table to colour each link by its own value. A swung tool's load is the
     * whole item's leverage (the {@code JOINT} figure times what the whole joint carries), so a head on its handle reads as
     * the item's {@code JOINT} and a guard on a grip, held by only its own strength, can read differently; a bow's pair of
     * limbs and string is its {@code JOINT}; any other pair carries the lighter part's mass at
     * {@link ModularTuning#JOINT_STATIC_LEVER} against the weaker part's strength. A pair with a part not drawn, or a
     * material missing, reads 0.
     */
    public static float[] joints(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials, List<String[]> pairs) {
        float[] out = new float[pairs.size()];
        Map<String, Material> by = new java.util.HashMap<>();
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m == null || !type.roles().containsKey(p.role()))
                return out;
            by.put(p.role(), m);
        }
        ModularType.Family family = type.family();
        boolean swung = type.roles().values().stream().anyMatch(ModularType.Role::rod);
        List<String> heads = type.strikers().isEmpty() ? List.of(type.wearRole()) : type.strikers();
        float load = 0;
        float binding = 0;
        if (swung) {
            Optional<ModularFigures> whole = bake(design, type, materials, 0);
            JointCapacity c = jointCapacity(design, type, by, heads);
            binding = c.binding();
            load = whole.map(f -> f.get(Fig.JOINT)).orElse(0f) * ModularTuning.JOINT_K * (c.weakest() + c.binding());
        }
        for (int i = 0; i < out.length; i++) {
            Part pa = design.part(pairs.get(i)[0]), pb = design.part(pairs.get(i)[1]);
            if (pa == null || pb == null)
                continue;
            ModularType.Role ra = type.roles().get(pa.role()), rb = type.roles().get(pb.role());
            Material ma = by.get(pa.role()), mb = by.get(pb.role());
            if (family == ModularType.Family.BOW || family == ModularType.Family.CROSSBOW) {
                boolean aString = ra.name().equals("string"), bString = rb.name().equals("string");
                if (aString != bString) {
                    float drive = strength(aString ? mb : ma), string = strength(aString ? ma : mb);
                    out[i] = string > 0 ? drive / (ModularTuning.STRING_SHARE * string) : 0f;
                    continue;
                }
            }
            float held = Math.min(jointStrength(ra, pa, ma), jointStrength(rb, pb, mb));
            if (swung) {
                boolean headOnHandle = (ra.rod() && heads.contains(pb.role())) || (rb.rod() && heads.contains(pa.role()));
                float capacity = ModularTuning.JOINT_K * (held + (headOnHandle ? binding : 0f));
                out[i] = capacity > 0 ? load / capacity : 0f;
            } else {
                float hung = Math.min(mass(ra, pa, ma), mass(rb, pb, mb));
                float capacity = ModularTuning.JOINT_K * held;
                out[i] = capacity > 0 ? hung * ModularTuning.JOINT_STATIC_LEVER / capacity : 0f;
            }
        }
        return out;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.min(hi, Math.max(lo, v));
    }
}
