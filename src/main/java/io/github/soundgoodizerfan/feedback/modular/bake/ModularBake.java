// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.weapon.GunTuning;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.combat.gear.GearTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.modular.attunement.Attunements;
import io.github.soundgoodizerfan.feedback.modular.bench.Edge;
import io.github.soundgoodizerfan.feedback.modular.bench.Filing;
import io.github.soundgoodizerfan.feedback.modular.bench.ForgeRhythm;
import io.github.soundgoodizerfan.feedback.modular.item.ModularGlove;
import io.github.soundgoodizerfan.feedback.modular.part.ModularBackpack;
import io.github.soundgoodizerfan.feedback.modular.part.ModularBed;
import io.github.soundgoodizerfan.feedback.modular.prep.DishBake;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularFaces;
import io.github.soundgoodizerfan.feedback.modular.type.ModularHeads;
import io.github.soundgoodizerfan.feedback.modular.type.ModularNest;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;

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
 * treatment is the material (hardened steel is its own file), so there is no
 * treatment state either.
 */
public final class ModularBake {

    private ModularBake() {
    }

    /**
     * The material in slot {@code name} of {@code part}: what it was filled with apart from the main slot, else the main material;
     * null when the part kind has no such slot. A slot never carries a trait: the bake reads its figures by name ({@code edge}, {@code lens}).
     */
    public static Material slotMaterial(ModularType.Role role, Part part, String name, Map<ResourceLocation, Material> materials) {
        ModularType.PartSlot slot = role.shapingOf(part).flatMap(sh -> sh.slot(name)).orElse(null);
        if (slot == null)
            return null;
        return part.measures().slot(name).map(s -> materials.get(s.material()))
                .orElseGet(() -> slot.standard().map(materials::get).orElse(materials.get(part.material())));
    }

    /**
     * {@code zone} with the part's {@code edge} slot inlaid, if it has one filled with another material than the body: the edge sets the
     * hardness it bites with, and its share of the part (the slot's {@code share}) of the toughness and wear, the body's strength
     * standing behind it -- a katana's trick, a hard edge on a tough spine.
     */
    /** A figure of a part read across its slots: each slot's material's, weighted by the slot's share of the part; the main material's alone for a part of one material. */
    static float blend(ModularType.Role role, Part part, Material main, Map<ResourceLocation, Material> materials,
                       java.util.function.ToDoubleFunction<Material> figure) {
        ModularType.Shaping sh = role.shapingOf(part).orElse(null);
        if (sh == null || sh.slots().size() < 2)
            return (float) figure.applyAsDouble(main);
        float sum = 0f;
        for (int i = 0; i < sh.slots().size(); i++) {
            Material m = i == 0 ? main : slotMaterial(role, part, sh.slots().get(i).name(), materials);
            sum += sh.shareOf(i) * (float) figure.applyAsDouble(m == null ? main : m);
        }
        return sum;
    }

    private static Zone withEdge(Zone zone, Material body, ModularType.Role role, Part part, Map<ResourceLocation, Material> materials) {
        Material edge = slotMaterial(role, part, "edge", materials);
        if (edge == null || edge == body)
            return zone;
        ModularType.Shaping shaping = role.shapingOf(part).orElseThrow();
        float share = shaping.shareOf(shaping.slots().indexOf(shaping.slot("edge").orElseThrow()));
        float h = zone.hardness() + (edge.hardness().value() - zone.hardness()) * ModularTuning.EDGE_SLOT_HARDNESS_SHARE;
        return new Zone(h, zone.toughness() + (toughness(edge) - zone.toughness()) * share,
                (float) Math.sqrt(h * strength(body)), zone.wear() + (competence(edge) - zone.wear()) * share);
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
     * bad, never forbidden (philosophy 8); a diamond set in tougher metal ({@link #zone}) is not
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
        float cells = role.cells(p) * role.volumeScale(p);
        Material.XpCapacity x = m.xpStore().get();
        XpStore empty = XpStore.empty(Math.max(1, Math.round(cells * x.capacitance())), x.topLevel());
        return Optional.of(held == null ? empty : empty.with(held.points()));
    }

    /**
     * Whether {@code material} can fill {@code roleKey} of a {@code type} and still bake: a battery's plates must state a
     * {@code secondary_cell} and its case must insulate, a machine's winding must conduct and its housing state a conductivity, a vessel's body must hold experience, a charge's filler must be a chemical propellant, a spring gun's barrel, piston and spring must be metal (the spring with a yield strength). Other families take any material their role accepts.
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
            case ENGINE -> engineMetal(material) && (!(roleKey.equals(ENGINE_PISTONS) || roleKey.equals(ENGINE_CRANK)) || material.mechanical().yieldStrength().isPresent())
                    && (!roleKey.equals(ENGINE_PLUG) || material.thermal().meltingPoint().isPresent());
            case GEARBOX -> engineMetal(material);
            // a module's wiring must conduct; its casing and core take any material their shapes accept
            case SUIT_MODULE -> switch (type.roles().get(roleKey).name()) {
                case "wiring" -> material.ampacity().isPresent();
                // a spring is rated by what it stores: a material that states no yield strength cannot be one
                case "module_spring" -> material.mechanical().yieldStrength().isPresent();
                default -> true;
            };
            // a shell holds pressure by its yield strength: a metal that states none cannot be rated
            case BLOCK -> BlockFigures.suits(type, roleKey, material);
            case PRESSURE -> engineMetal(material) && material.mechanical().yieldStrength().isPresent();
            case PROPELLER -> engineMetal(material) && (!roleKey.equals(PROP_BLADES) || material.mechanical().yieldStrength().isPresent());
            // a spring gun's barrel, piston and spring are metal (the spring states a yield strength); its receiver takes any material
            // a pulse gun's seat must conduct (its conductor sets the ceiling of a pulse); its barrel, receiver and grip take any material
            case ELECTRIC_GUN -> !type.roles().get(roleKey).name().equals(ElectricGunSpec.SEAT) || material.ampacity().isPresent();
            case AIR_GUN -> {
                String name = type.roles().get(roleKey).name();
                yield name.equals(SpringGunSpec.RECEIVER) || (engineMetal(material)
                        && (!name.equals(SpringGunSpec.SPRING) || material.mechanical().yieldStrength().isPresent()));
            }
            case CHARGE -> wear || !io.github.soundgoodizerfan.feedback.weapon.Blast.isFiller(type, roleKey) || material.propellant().isPresent();
            default -> true;
        };
    }

    /** What a battery design is: its cell count, one cell's capacity in Eu, the plates' figures and the band the case rates the battery. */
    public record CellSpec(int cells, float capacity, Material.SecondaryCell plates, VoltageBand caseBand) {
    }

    /**
     * The battery a design makes: the plate part's cells of stock (two plates to a cell, {@link FTuning#BATTERY_INGOTS_PER_CELL}, unless its variant's {@code ingots_per_cell} param says 1 or 4)
     * give the cell count, each plate its material's {@code eu_per_ingot}, and the case material's {@code insulation} is the
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
        // Size and count are axes: a cell is this many ingots of plate, and there are this many cells.
        int perCell = Math.max(1, Math.round(role.axis(plates, "cell_size")));
        int cells = Math.max(1, Math.round(role.axis(plates, "cell_count")));
        Material.SecondaryCell sc = pm.electrical().secondaryCell().get();
        return Optional.of(new CellSpec(cells, sc.euPerIngot() * perCell, sc, cm.electrical().insulation().get()));
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
    public static final String ENGINE_BLOCK = "K", ENGINE_PISTONS = "P", ENGINE_CRANK = "C", ENGINE_HEAD = "H", ENGINE_PLUG = "S";
    /** The role letters of the propeller's two parts. */
    public static final String PROP_HUB = "H", PROP_BLADES = "B";
    /** The role letter of the pressure vessel's shell. */
    public static final String TANK_SHELL = "S", TANK_BANDS = "B", TANK_VALVE = "V";
    public static final String GEAR_SET = "G", GEAR_SYNCHRO = "S", GEAR_CASE = "H";

    /** A weighed metal: what an engine or gearbox part is cast from. */
    private static boolean engineMetal(Material m) {
        return m.kind().orElse(null) == Material.Kind.METAL && m.density().muPerMb() > 0;
    }

    /** A shape param of {@code part}'s variant, else {@code fallback}. */
    static float param(ModularType type, Part part, String name, float fallback) {
        return type.roles().get(part.role()).paramOf(part, name, fallback);
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
     * own {@code torque}; the plug's metal its life and fouling; the block's layout a {@code torque}, {@code rev} and {@code mass}; the crank's build a {@code rev}. The
     * pistons' metal is the engine's (it sets the rev limit); a crank of a weaker metal than the pistons caps the revs by the
     * square root of its specific strength against theirs, and the other parts' metals weigh the engine. Empty if a part is
     * missing or is not a weighed metal (the pistons and crank need a yield strength too).
     */
    public static Optional<io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign> engine(ModularDesign design, ModularType type,
                                                                                                     Map<ResourceLocation, Material> materials) {
        Part block = design.part(ENGINE_BLOCK), pistons = design.part(ENGINE_PISTONS), crank = design.part(ENGINE_CRANK), head = design.part(ENGINE_HEAD), plug = design.part(ENGINE_PLUG);
        if (block == null || pistons == null || crank == null || head == null || plug == null)
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
        float torque = param(type, block, "torque", 1f)
                * (ModularTuning.ENGINE_HEAD_TORQUE_LOW + ModularTuning.ENGINE_HEAD_TORQUE_PER_COMPRESSION * param(type, head, "compression", 1f));
        float rev = param(type, block, "rev", 1f) * param(type, crank, "rev", 1f);
        float pSpec = (float) Math.sqrt(pm.mechanical().yieldStrength().get().value() / pm.density().muPerMb());
        float cSpec = (float) Math.sqrt(cm.mechanical().yieldStrength().get().value() / cm.density().muPerMb());
        rev *= Math.min(1f, cSpec / pSpec);
        float mass = param(type, block, "mass", 1f) * massBlend(design, type, materials, pm, ENGINE_BLOCK, ENGINE_CRANK, ENGINE_HEAD);
        // The plug's electrode metal: a higher melting point resists spark erosion (life), a conductive one runs cold and fouls.
        Material em = materials.get(plug.material());
        float life = Math.clamp(em.thermal().meltingPoint().get().value() / VehicleTuning.PLUG_REFERENCE_MELT_TU,
                VehicleTuning.PLUG_LIFE_RATIO[0], VehicleTuning.PLUG_LIFE_RATIO[1]) * VehicleTuning.PLUG_LIFE_TICKS;
        float foul = Math.clamp(em.thermal().conductivity().map(c -> c.workPerTickPerTu() / VehicleTuning.PLUG_REFERENCE_CONDUCTIVITY).orElse(1f),
                VehicleTuning.PLUG_FOUL_RATIO[0], VehicleTuning.PLUG_FOUL_RATIO[1]);
        return Optional.of(new io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign(cylinders, bore, stroke, pistons.material(),
                compression, torque, rev, mass, life, foul));
    }

    /**
     * What a pressure vessel design is, read by the placed block.
     *
     * @param volumeMb   the room inside, in mB of air: the block's metre cube less the walls, times the shape's fill.
     * @param dangerPu   the pressure past which it starts to risk letting go.
     * @param criticalPu the pressure at which it lets go outright.
     * @param mass       the shell's weight against a plain one (the wall and the metal's density).
     * @param reliefMb   mB of air a relief valve lets out each tick once the pressure nears the danger line; 0 with no valve.
     */
    public record TankSpec(float volumeMb, float dangerPu, float criticalPu, float mass, float reliefMb) {
    }

    /**
     * The pressure vessel a design makes. The shell's variant is its shape ({@code rating}: how well it carries a load for its wall,
     * a flat box poorly, a sphere twice a cylinder; {@code fill}: how much of the block's cube the room takes), its {@code wall}
     * dim the thickness in cm. A thick wall holds more and leaves less room: the rating is the metal's yield strength times the
     * wall over the inner radius ({@code (50 - wall)} cm), the room is the cube left inside. Empty if the metal states no yield.
     */
    public static Optional<TankSpec> tank(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part shell = design.part(TANK_SHELL);
        if (shell == null)
            return Optional.empty();
        Material m = materials.get(shell.material());
        if (m == null || !suits(type, TANK_SHELL, m))
            return Optional.empty();
        float wall = type.roles().get(TANK_SHELL).axis(shell, "wall");
        float inner = Math.max(1f, ModularTuning.TANK_HALF_CM - wall);
        float rating = param(type, shell, "rating", 1f);
        float fill = param(type, shell, "fill", 1f);
        float danger = ModularTuning.TANK_PU_PER_STRESS * m.mechanical().yieldStrength().get().value() * rating * wall / inner;
        float room = 2f * inner / (2f * ModularTuning.TANK_HALF_CM);
        float volume = ModularTuning.TANK_CUBE_MB * fill * room * room * room;
        float steel = m.density().muPerMb() * wall / ModularTuning.TANK_REF_WALL_CM;
        // Bands (an addon): hoop bands of their own metal share the load, the more the stronger they are against the shell.
        Part bands = design.part(TANK_BANDS);
        Material bm = bands == null ? null : materials.get(bands.material());
        if (bm != null && bm.mechanical().yieldStrength().isPresent()) {
            float shellYield = m.mechanical().yieldStrength().get().value(), bandYield = bm.mechanical().yieldStrength().get().value();
            danger *= 1f + ModularTuning.TANK_BANDS_GAIN * bandYield / (bandYield + shellYield);
        }
        // A relief valve (an addon) vents a share of the room a tick near the danger line, so a pump that outruns the leak still cannot burst it.
        float relief = design.part(TANK_VALVE) == null ? 0f : volume * ModularTuning.TANK_RELIEF_PER_ROOM;
        return Optional.of(new TankSpec(volume, danger, danger * ModularTuning.TANK_CRITICAL_OVER_DANGER, steel, relief));
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
        float mass = (1f + ModularTuning.PROP_HUB_MASS_PER_BLADE * (count - ModularTuning.PROP_HUB_MASS_REF_BLADES))
                * massBlend(design, type, materials, bm, PROP_HUB, PROP_BLADES);
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
    public static final String MACHINE_FRAME = "F", MACHINE_WINDING = "W", MACHINE_CORE = "C", MACHINE_HOUSING = "H", MACHINE_BEARING = "B", MACHINE_COOLING = "K";

    /**
     * What a dynamotor design is, read by the placed block.
     *
     * @param frameSu    the frame's torque ceiling, Su.
     * @param ratedRpm   the speed the winding is built for, with its band: the Su each amp costs the shaft there is {@code packet / (EU_PER_SU_RPM x ratedRpm)}.
     * @param ratedAmps  the current it carries: the frame's ceiling over the winding's figure, or the winding conductor's, whichever is less.
     * @param band       the band the winding puts on a line and is built for (a discrete choice of the winding).
     * @param mass       thermal mass of the frame, Work per Tu.
     * @param leak       the housing's shed to air, Work per tick per Tu (a finned one).
     * @param serviceTu  the hottest the housing runs before the machine wears.
     * @param windingDrop volts lost per amp over the whole winding (the conductor's drop times its length).
     * @param coreLoss   the share of throughput the core loses as heat.
     * @param jacketed   whether the housing is a water jacket.
     */
    public record MachineSpec(float frameSu, float ratedRpm, float ratedAmps, VoltageBand band, float mass, float leak,
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
                // The servo is a motor held whole (a nested item): its drive is how well rated it is, its loss what it falls short by.
                float grade = part.sub().isEmpty() ? DRAWN_MOTOR_GRADE : supportGrade("motor", part.sub());
                f.merge(Fig.SUIT_DRIVE, grade * ModularTuning.SUIT_DRIVE_PER_MOTOR_GRADE, Float::sum);
                f.put(Fig.SUIT_LOSS, clamp(ModularTuning.SUIT_LOSS_FLOOR + (1f - grade) * ModularTuning.SUIT_LOSS_PER_GRADE_SHORT, 0f, ModularTuning.SUIT_LOSS_MAX));
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
                        f.put(Fig.arg(i), held.get(Fig.arg(i)));
                        f.put(Fig.sup(i), held.get(Fig.sup(i)));
                    }
                    f.put(Fig.SUIT_WIRING, held.get(Fig.SUIT_WIRING));
                }
            }
            default -> { }
        }
    }

    private static final ThreadLocal<Boolean> DRAWING = ThreadLocal.withInitial(() -> false);

    /** Sets whether this thread is baking a drawing (a blueprint's prediction), which has no components fitted. */
    public static void drawing(boolean on) {
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
    private static Optional<Map<Fig, Float>> suitModule(ModularType type, Map<String, Material> by, ModularDesign design,
                                                         Map<ResourceLocation, Material> materials) {
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
            // An attachment's figures come off its role's params, which its variant's params override: a mechanism part (a lens, a spring)
            // states its class on the role and is tuned by its axes; a part cut in a named shape states it on the variant.
            Map<String, Float> k = new java.util.HashMap<>(role.params());
            role.variant(p).ifPresent(v -> k.putAll(v.params()));
            if (k.getOrDefault("module", 0f) <= 0f || n >= budget)
                return Optional.empty();
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
            else if (k.containsKey("nozzle"))
                // a nozzle holds the flame it is lit with: its melting point against the reference flame the thruster burns at
                power = m.thermal().meltingPoint().map(t -> t.value()).orElse(0f) / k.get("nozzle");
            else if (k.containsKey("competence"))
                power = competence(m) / k.get("competence");
            else if (k.containsKey("optic")) {
                // The glass reads: the lens set in the casing when there is one, else the part's own material. What it does not reflect it
                // passes, so clarity is one minus its reflectivity (and what it turns back is the glare it keeps out).
                Material lens = slotMaterial(role, p, "lens", materials);
                power = (1f - (lens == null ? m : lens).separation().reflectivity().orElse(0.2f)) / k.get("optic");
            } else if (k.containsKey("porous")) {
                // A bed of loose, open media: the lighter it packs, the more surface the air crosses.
                Material media = slotMaterial(role, p, "media", materials);
                power = k.get("porous") / Math.max(1e-3f, (media == null ? m : media).density().muPerMb());
            } else if (k.containsKey("stiff"))
                // a spring stores what its stock's stiffness lets it, the same strength a weapon's spring reads (SpringPart)
                power = strength(m) / k.get("stiff");
            else if (k.containsKey("grip"))
                power = m.mechanical().friction().orElse(0.5f) / k.get("grip");
            // A bored part (a cylinder, a nozzle) works as well as its bore is cut to the size the upgrade wants: uncut or overbored, it still
            // works, only worse. The cut comes off the part (a drill, per Fu), never a gate.
            if (k.containsKey("bore_target") && role.axes().containsKey("bore")) {
                float want = k.get("bore_target"), cut = p.axis("bore", role.axes().get("bore").def());
                float closeness = Math.max(0f, 1f - Math.abs(cut - want) / want);
                power *= ModularTuning.BORE_FLOOR + (1f - ModularTuning.BORE_FLOOR) * closeness;
            }
            float module = k.get("module");
            // The part's tuned axis (a lens's focal length, a spring's stroke) as a share of its range: the suit reads what it trades.
            String axis = io.github.soundgoodizerfan.feedback.suit.SuitModules.argAxis(Math.round(module));
            ModularType.Axis tuned = axis.isEmpty() ? null : role.axesFor(p).get(axis);
            if (tuned != null && tuned.max() > tuned.min())
                out.put(Fig.arg(n), clamp((role.axis(p, axis) - tuned.min()) / (tuned.max() - tuned.min()), 0f, 1f));
            int held = (supports.getOrDefault("controller", 0f) > 0f ? Fig.SUP_CONTROLLER : 0) | (supports.getOrDefault("cell", 0f) > 0f ? Fig.SUP_CELL : 0)
                    | (supports.getOrDefault("motor", 0f) > 0f ? Fig.SUP_MOTOR : 0);
            out.put(Fig.sup(n), (float) held);
            out.put(Fig.att(n), module);
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
    /** The grade a drawing's motor is baked at: a middling one. */
    private static final float DRAWN_MOTOR_GRADE = 0.5f;

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
        return Math.max(1, Math.round(role.cells(part) * role.volumeScale(part)));
    }

    /** The index of the reference size in {@code xs} nearest {@code x}: a discrete choice, never a blend. */
    private static int nearest(float x, float[] xs) {
        int best = 0;
        for (int i = 1; i < xs.length; i++)
            if (Math.abs(xs[i] - x) < Math.abs(xs[best] - x))
                best = i;
        return best;
    }

    /** {@code x} on the piecewise-linear curve through ({@code xs}, {@code ys}), held flat past either end. */
    public static float interp(float x, float[] xs, float[] ys) {
        if (x <= xs[0])
            return ys[0];
        for (int i = 1; i < xs.length; i++)
            if (x <= xs[i])
                return ys[i - 1] + (ys[i] - ys[i - 1]) * (x - xs[i - 1]) / (xs[i] - xs[i - 1]);
        return ys[ys.length - 1];
    }

    /** The name of the shape {@code part} was made in, the standard shape's base name when it has no variant. */
    private static String shapeName(ModularType.Role role, Part part) {
        return role.variant(part).map(ModularType.Variant::name).orElse("");
    }

    /**
     * The machine a design makes. The frame sets the torque ceiling, the thermal mass and the wall area; the winding sets its band
     * and rated speed (its shape) and the current its conductor carries (the {@code ampacity} times its cells of wire); the core's
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
        // The frame's size is an axis in cells of stock; its ceiling, thermal mass and wall area are read between the three reference frames.
        float size = stockCells(type.roles().get(MACHINE_FRAME), frame);
        float su = interp(size, ModularTuning.FRAME_CELLS, ModularTuning.FRAME_SU);
        float mass = interp(size, ModularTuning.FRAME_CELLS, ModularTuning.FRAME_MASS);
        float area = interp(size, ModularTuning.FRAME_CELLS, ModularTuning.FRAME_AREA);
        // The winding's cells of wire pick its band and the speed it is built for: a discrete choice, the nearest reference winding.
        int windingStep = nearest(stockCells(type.roles().get(MACHINE_WINDING), winding), ModularTuning.WINDING_CELLS);
        VoltageBand windingBand = ModularTuning.WINDING_BAND[windingStep];
        float ratedRpm = ModularTuning.WINDING_RATED_RPM[windingStep];
        // Su each amp costs the shaft at the rated speed (what the old volts per RPM was).
        float suPerAmp = ElectricRules.packetEu(windingBand) / (ElectricTuning.EU_PER_SU_RPM * ratedRpm);
        // An optional bearing sets how much torque the rotor carries and how much friction heat it makes (a sleeve is cheap and warm,
        // a ball bearing runs cool, a roller bearing carries the most).
        Part bearing = design.part(MACHINE_BEARING);
        var bv = bearing == null ? Optional.<ModularType.Variant>empty() : type.roles().get(MACHINE_BEARING).variant(bearing);
        su *= bv.map(v -> v.params().getOrDefault("su_mult", 1f)).orElse(1f);
        float friction = bv.map(v -> v.params().getOrDefault("friction", 0f)).orElse(0f);
        // The balls (or rollers) set how much a bearing drags: their material's sliding friction against the reference.
        Material balls = bearing == null || bearing.measures().slot("balls").isEmpty() ? null : slotMaterial(type.roles().get(MACHINE_BEARING), bearing, "balls", materials);
        if (balls != null)
            friction *= balls.mechanical().friction().orElse(ModularTuning.BEARING_REF_FRICTION) / ModularTuning.BEARING_REF_FRICTION;
        int wireCells = stockCells(type.roles().get(MACHINE_WINDING), winding);
        float amps = Math.min(su / suPerAmp, wm.ampacity().get() * wireCells * FTuning.WINDING_AMPS_PER_AMPACITY);
        float drop = wm.electrical().drop().orElse(0f) * wireCells * ModularTuning.WINDING_BLOCKS_PER_CELL;
        float leak = hm.thermal().conductivity().get().workPerTickPerTu() * area * ModularTuning.MACHINE_SHED_PER_CONDUCTIVITY;
        // A cooling addon (plate fins, or pipe for a water jacket) sheds more: its material's conductivity over its volume.
        Part cooling = design.part(MACHINE_COOLING);
        Material km = cooling == null ? null : materials.get(cooling.material());
        if (km != null && km.thermal().conductivity().isPresent())
            leak += km.thermal().conductivity().get().workPerTickPerTu() * type.roles().get(MACHINE_COOLING).volume(cooling)
                    * ModularTuning.COOLING_SHED_PER_LS;
        float service = hm.thermal().serviceTemperature().map(Tu::value).orElse(FTuning.MACHINE_SERVICE_TU);
        return Optional.of(new MachineSpec(su, ratedRpm, amps, windingBand, mass, leak, service, drop,
                cm.electrical().coreLoss().orElse(FTuning.CORE_LOSS_DEFAULT) + friction, cooling != null && cooling.form().equals("pipe")));
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

    /**
     * A hafted tool with no haft: only a hammer head stands alone (the head a machine takes); any other head with no
     * haft is a required part missing, not a tool.
     */
    public static boolean needsHaft(ModularDesign design, ModularType planned) {
        return planned.family() == ModularType.Family.HAFTED && design.part("S") == null
                && design.parts().stream().anyMatch(p -> (p.role().equals("H") || p.role().equals("J") || p.role().equals("K"))
                        && !p.kind().equals("head_blank"));
    }

    /** As {@link #bake(ModularDesign, ModularType, Map, int)}, for an item carrying {@code coat} (or none). */
    public static Optional<ModularFigures> bake(ModularDesign design, ModularType planned,
                                                Map<ResourceLocation, Material> materials, int step, Plating coat) {
        Map<ResourceLocation, Material> outer = TABLE.get();
        TABLE.set(materials);
        try {
            return bakeIn(design, planned, materials, step, coat);
        } finally {
            if (outer == null)
                TABLE.remove();
            else
                TABLE.set(outer);
        }
    }

    private static Optional<ModularFigures> bakeIn(ModularDesign design, ModularType planned,
                                                   Map<ResourceLocation, Material> materials, int step, Plating coat) {
        ModularType type = ModularFaces.view(planned, design);
        Map<String, Material> by = new java.util.HashMap<>();
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m == null || !type.roles().containsKey(p.role()))
                return Optional.empty();
            by.put(p.role(), m);
        }
        if (type.family() == ModularType.Family.DISH)
            return dish(design, type, materials, step);
        if (needsHaft(design, planned) || !planned.missing(design.parts().stream().map(Part::role).toList()).isEmpty())
            return Optional.empty();
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
        Zone zone = section(mix(withEdge(zone(edge, null, 1f, 1f), edge, type.roles().get(type.wearRole()), wearPart, materials), edge,
                setPart == null ? null : by.get(setPart.role()), armour ? studs : 1f, armour ? studs : ModularTuning.TIP_SHARE),
                type.roles().get(type.wearRole()).section(wearPart)
                * (setPart == null ? 1f : type.roles().get(setPart.role()).section(setPart)),
                // An attunement can cost the part toughness (keen) or buy it back (ward on a plate): flaws, as forging leaves them.
                Math.max(0, Math.min(ForgeRhythm.MAX_FLAW, wearPart.flaw() + Attunements.flawShift(wearPart, type.roles().get(type.wearRole()).name(), type.roles().get(type.wearRole()).cls())
                        // Coarse file marks left unpolished are stress risers: one flaw worse.
                        + (wearPart.measures().state(Filing.MARKS, 0f) >= ModularTuning.FILE_MARKS_FLAW_AT ? 1 : 0))));
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
            case DIG, BLADE, MACE, SPEAR -> melee(design, type, by, materials, zone, f, worn);
            case SHEARS -> {
                melee(design, type, by, materials, zone, f, worn);
                shears(design, type, by, f, worn);
            }
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
                        // A laminated limb (a bone back on a wood core) is as stiff as its layers together, each by its share.
                        drive = blend(r, p, by.get(p.role()), materials, m -> strength(m));
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
            case AIR_GUN -> {
                Optional<SpringGunSpec> spec = SpringGunSpec.of(design, type, materials);
                if (spec.isEmpty())
                    return Optional.empty();
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                SpringGunSpec air = spec.get();
                f.put(Fig.MASS, total);
                f.put(Fig.SPRING_ENERGY, air.energy() * worn);
                f.put(Fig.AIR_EFFICIENCY, air.efficiency());
                f.put(Fig.VELOCITY, air.shot(ModularTuning.AIR_REF_DENSITY, 1f - worn).speed() / ModularTuning.AIR_BOW_ARROW_SPEED);
                f.put(Fig.DRAW, air.cockTicks());
                f.put(Fig.JOINT, air.load(0f));
            }
            case ELECTRIC_GUN -> {
                Optional<ElectricGunSpec> spec = ElectricGunSpec.of(design, type, materials);
                if (spec.isEmpty())
                    return Optional.empty();
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                ElectricGunSpec egun = spec.get();
                f.put(Fig.MASS, total);
                f.put(Fig.SHOT_ENERGY, egun.pulseJ() * egun.seatMult() * worn);
                // The shared gun figures at an ordinary pulse: what the gun pipeline reads (cycle, kick, sound, the HUD).
                f.putAll(egun.figures(egun.pulseJ() * egun.seatMult(), total, egun.cycleTicks(false), 0f, 1f).figs());
                f.put(Fig.MASS, total);
            }
            case ARMOUR -> {
                Part plate = wearPart;
                float share = comp / (comp + ModularTuning.ARMOUR_HALF_COMPETENCE);
                ModularType.Role plateRole = type.roles().get(type.wearRole());
                float canonical = type.canonicalCounts().getOrDefault(type.wearRole(), plate.count());
                float cover = clamp(plateRole.cells(plate) * plateRole.volumeScale(plate) / Math.max(1f, canonical), 0f, ModularTuning.ARMOUR_COVER_MAX);
                float points = type.armourMax() * share * type.roles().get(type.wearRole()).factor(plate.form()) * cover;
                float total = 0;
                for (Part p : design.parts()) {
                    ModularType.Role r = type.roles().get(p.role());
                    total += mass(r, p, by.get(p.role()));
                    if (r.name().equals("underlayer")) {
                        float c = competence(by.get(p.role()));
                        points += type.armourMax() * ModularTuning.LINING_SHARE * c / (c + ModularTuning.ARMOUR_HALF_COMPETENCE);
                    }
                    suitFitting(f, type, r, p, by.get(p.role()));
                }
                f.put(Fig.MASS, total);
                f.put(Fig.PIERCE_RESIST, clamp(share * cover * param(plateRole, plate, "pierce"), 0f, ModularTuning.PIERCE_RESIST_MAX));
                f.put(Fig.FIRE_CONDUCT, edge.thermal().conductivity().map(c -> c.workPerTickPerTu()).orElse(0f) * plateRole.volume(plate));
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
                    held *= type.roles().get(p.role()).axesFor(p).containsKey("rim_width")
                            ? 1f + ModularTuning.RIM_CAPACITY_PER_WIDTH * (type.roles().get(p.role()).axis(p, "rim_width") - 1f)
                            : param(type.roles().get(p.role()), p, "capacity_mult");
                    // An addon role may state its own multiple (a boss is a weak spot, a strap carries the weight).
                    held *= type.roles().get(p.role()).params().getOrDefault("capacity_mult", 1f);
                }
                f.put(Fig.MASS, total);
                f.put(Fig.CAPACITY, (ModularTuning.SHIELD_BASE_CAPACITY + comp * worn) * held);
            }
            case HAND -> {
                // A face of the hafted plan swings and digs as any other, and a hammer face also lands the hand's blow.
                if (!type.strikers().isEmpty()) {
                    melee(design, type, by, materials, zone, f, worn);
                    break;
                }
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
                // A heavier, harder, tougher head lands a harder blow; a worn one has lost some of it.
                f.put(Fig.STRIKE_ST, ModularTuning.HAND_ST_PER_MU_COMPETENCE * total * zone.wear() * worn * type.strike()
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
                float capacity = shell == null ? ModularTuning.PACK_REF_CELLS[1] : stockCells(type.roles().get(ModularBackpack.SHELL), shell);
                f.put(Fig.SLOTS, (float) Math.round(interp(capacity, ModularTuning.PACK_REF_CELLS, ModularTuning.PACK_REF_SLOTS)));
                // Fittings take mounting points from the shell (and the frame, which gives more); a hip belt hangs from a frame.
                int budget = Math.round(interp(capacity, ModularTuning.PACK_REF_CELLS, ModularTuning.PACK_REF_POINTS))
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
            case BED -> {
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
                // Softness of what you lie on and of the padding under it (the padding counts most); the mattress's own thickness and the
                // frame's lift off the ground raise it. Warmth is how little the padding passes the sleeper's heat, thicker the better.
                Part mattress = design.part(ModularBed.MATTRESS);
                ModularType.Role mattressRole = type.roles().get(ModularBed.MATTRESS);
                Material padding = mattress == null ? null : slotMaterial(mattressRole, mattress, "padding", materials);
                Material cover = by.get(ModularBed.MATTRESS);
                float comfort = mattress == null ? 0f : mattressRole.paramOf(mattress, "comfort", 1f);
                float thick = mattress == null ? 1f : mattressRole.paramOf(mattress, "thick", 1f);
                Part frame = design.part(ModularBed.FRAME);
                float lift = frame == null ? 0f : type.roles().get(ModularBed.FRAME).paramOf(frame, "lift", 1f);
                float soft = 0.7f * (padding == null ? 0f : soft(padding)) + 0.3f * (cover == null ? 0f : soft(cover));
                f.put(Fig.COMFORT, worn * soft * comfort * (1f + ModularTuning.BED_LIFT_COMFORT * lift));
                float pass = padding == null ? ModularTuning.BED_WARM_HALF_CONDUCTIVITY * 10f
                        : padding.thermal().conductivity().map(c -> c.workPerTickPerTu()).orElse(ModularTuning.BED_WARM_HALF_CONDUCTIVITY * 10f);
                float keeps = ModularTuning.BED_WARM_HALF_CONDUCTIVITY / (ModularTuning.BED_WARM_HALF_CONDUCTIVITY + pass);
                f.put(Fig.WARMTH, clamp(worn * keeps * (float) Math.sqrt(thick) * (1f + ModularTuning.BED_LIFT_COMFORT * lift), 0f, 1f));
            }
            case HEAD -> {
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
                // Heavier, harder and tougher lifts the ceiling; brittleness has already cost the competence its square.
                f.put(Fig.STRIKE_ST, ModularTuning.HEAD_ST_PER_MU_COMPETENCE * total * zone.wear()
                        * type.roles().get(type.wearRole()).strike(wearPart));
            }
            case SUIT_MODULE -> {
                Optional<Map<Fig, Float>> module = suitModule(type, by, design, materials);
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
            case ENGINE, GEARBOX, PROPELLER, PRESSURE, BLOCK -> {
                if (switch (type.family()) {
                    case BLOCK -> BlockFigures.of(design, type, materials).isEmpty();
                    case PRESSURE -> tank(design, type, materials).isEmpty();
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
                io.github.soundgoodizerfan.feedback.weapon.Cartridge.Blend.of(materials, filler);
        if (read.isEmpty() || blend.isEmpty() || blend.get().total() > GunTuning.DEMO_FILLER_MAX)
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
        return role.paramOf(part, name, 1f);
    }

    static float mass(ModularType.Role role, Part part, Material m) {
        if (!part.sub().isEmpty()) {
            ModularFigures held = ModularNest.figures(part);
            return held == null ? 0f : held.get(Fig.MASS);
        }
        return role.volume(part) * density(role, part, m) * role.factor(part.form()) * role.kept(part);
    }

    /** The materials of the bake in progress, so a part's slots can be weighed by their own density (set by {@link #bake}, per thread). */
    private static final ThreadLocal<Map<ResourceLocation, Material>> TABLE = new ThreadLocal<>();

    /** Density of a part: the main material's, or for a part with slots filled apart the share-weighted mean of its slots'. */
    private static float density(ModularType.Role role, Part part, Material main) {
        Map<ResourceLocation, Material> table = TABLE.get();
        ModularType.Shaping sh = table == null ? null : role.shapingOf(part).orElse(null);
        if (sh == null || sh.slots().size() < 2)
            return main.density().muPerMb();
        return blend(role, part, main, table, m -> m.density().muPerMb());
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
    /**
     * Shears: the blade pair's shape sets how quickly and how big a snap is. A spring bow (one bent bar) snaps fast and bears little; a
     * pivot scissor bears what its pin does, the weaker of pin and blade, and with no pin through it hardly at all. A longer blade takes a bigger
     * bite of wool and leaves, reaches further, and loses the control that cobweb and vines want.
     */
    private static void shears(ModularDesign design, ModularType type, Map<String, Material> by, Map<Fig, Float> f, float worn) {
        Part blade = design.part("B");
        ModularType.Role role = type.roles().get("B");
        float snap = role.paramOf(blade, "snap", 1f);
        ModularType.Axis length = role.axesFor(blade).get("length");
        float ratio = length == null || length.def() <= 0f ? 1f : role.axis(blade, "length") / length.def();
        float hold = role.paramOf(blade, "hold", 1f);
        if (role.paramOf(blade, "pivoted", 0f) > 0f) {
            Material pin = by.get("P");
            hold *= pin == null ? ModularTuning.SHEARS_NO_PIN_HOLD : Math.min(1f, strength(pin) / strength(by.get("B")));
        }
        f.put(Fig.DURABILITY, (float) Math.max(1, Math.round(f.get(Fig.DURABILITY) * hold)));
        f.put(Fig.CLIP, worn * snap * (float) Math.pow(ratio, ModularTuning.SHEARS_CLIP_EXPONENT));
        f.put(Fig.CLIP_FINE, worn * snap * (float) Math.pow(ratio, -ModularTuning.SHEARS_CLIP_EXPONENT));
        f.put(Fig.REACH, Math.max(0f, (ratio - 1f) * ModularTuning.SHEARS_REACH_PER_RATIO));
    }

    private static void melee(ModularDesign design, ModularType type, Map<String, Material> by, Map<ResourceLocation, Material> materials,
                              Zone zone, Map<Fig, Float> f, float worn) {
        boolean faced = !type.strikers().isEmpty();
        List<String> heads = !faced ? List.of(type.wearRole()) : type.strikers().stream().filter(r -> design.part(r) != null).toList();
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
                    : section(withEdge(zone(by.get(role), null, 1f, 1f), by.get(role), type.roles().get(role), head, materials),
                            type.roles().get(role).section(head), head.flaw());
            float mass = headMass.getOrDefault(role, rodMass);
            float impact = 0.5f * (mass + ModularTuning.ROD_STRIKE_SHARE * rodMass) * tip * tip;
            float bite = bite(z.bite(), design.edge());
            float strike = type.strike();
            Optional<ModularHeads> kind = faced && head != null ? ModularHeads.of(ModularHeads.kindOf(head, type.roles().get(role))) : Optional.empty();
            if (faced && head != null)
                strike = kind.map(ModularHeads::strike).orElse(1f);
            if (head != null)
                strike *= type.roles().get(role).strike(head);
            boolean spent = faced && ModularFaces.spent(head);
            float damage = spent ? 1f : 1 + ModularTuning.DAMAGE_K * strike * bite * (float) Math.sqrt(impact) * worn;
            float dig = spent ? 0f : ModularTuning.DIG_K * bite * (float) Math.sqrt(impact) * worn;
            wear += z.wear();
            if (i == 2) {
                f.put(Fig.DIG_SPEED_THIRD, dig);
                f.put(Fig.HARDNESS_THIRD, z.hardness());
                f.put(Fig.EDGE_TOUGHNESS_THIRD, z.toughness());
            } else if (i > 0) {
                f.put(Fig.DIG_SPEED_OFF, dig);
                f.put(Fig.HARDNESS_OFF, z.hardness());
                f.put(Fig.EDGE_TOUGHNESS_OFF, z.toughness());
            } else {
                f.put(Fig.DIG_SPEED, dig);
            }
            if (faced) {
                // Each face lasts as long as its own edge and stock do; the bar follows the most worn one (ModularFaces#wear).
                float litres = i == 0 ? type.wearLitres() : kind.map(ModularHeads::wearLitres).orElse(type.wearLitres());
                f.put(i == 0 ? Fig.DURABILITY : i == 1 ? Fig.DURABILITY_B : Fig.DURABILITY_C,
                        (float) Math.max(1, Math.round(ModularTuning.DURABILITY_K * z.wear() * litres)));
                // A hammer face lands the hand's blow (what a hand-forging recipe and a machine head ask of the tool).
                if (kind.map(h -> h.combat() == ModularType.Family.HAND && h.strike() > 0f).orElse(false) && !spent)
                    f.put(Fig.STRIKE_ST, Math.max(f.getOrDefault(Fig.STRIKE_ST, 0f), ModularTuning.HAND_ST_PER_MU_COMPETENCE * total * z.wear() * worn * strike));
            }
            if (!spent && damage > bestDamage) {
                bestDamage = damage;
                f.put(Fig.IMPACT, impact);
                f.put(Fig.BITE, bite);
                f.put(Fig.DAMAGE, damage);
            }
        }
        if (bestDamage < 0) {
            f.put(Fig.DAMAGE, 1f);
        }
        f.put(Fig.MASS, total);
        f.put(Fig.BALANCE, feltTotal > 0 && reachLength > 0 ? Math.min(1f, moment / (feltTotal * reachLength)) : 0f);
        f.put(Fig.SWING, swing);
        f.put(Fig.ATTACK_SPEED, swing * worn);
        f.put(Fig.REACH, Math.max(0, reachLength - ModularTuning.REACH_REFERENCE) * ModularTuning.REACH_PER_METRE);
        f.put(Fig.JOINT, joint(design, type, by, struck, reachLength, rodLength, heads));
    }

    /** What a binding's wraps multiply its joint by: one wrap is 1, each more adds {@link ModularTuning#BINDING_JOINT_PER_WRAP}; an addon with no wraps axis states {@code joint_mult}. */
    private static float jointMult(ModularType.Role r, Part p) {
        return r.axesFor(p).containsKey("wraps") ? 1f + ModularTuning.BINDING_JOINT_PER_WRAP * (r.axis(p, "wraps") - 1f) : param(r, p, "joint_mult");
    }

    /** The strength a joint can lean on in {@code m}: a binding or an edge guard counts for its share of its own strength, any other part for all of it. */
    private static float jointStrength(ModularType.Role r, Part p, Material m) {
        if (r.name().equals("binding") || r.name().equals("edge_guard"))
            return (m.kind().filter(k -> k == Material.Kind.METAL).isPresent()
                    ? ModularTuning.PINNED_SHARE : ModularTuning.WRAPPED_SHARE) * strength(m) * jointMult(r, p);
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
            if (r.name().equals("binding") || r.name().equals("edge_guard"))
                binding += jointStrength(r, p, by.get(p.role()));
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
