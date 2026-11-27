// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * The bake of the {@code BLOCK} family: a fixed block's constants, worked out from the parts and metals it was built from. Each
 * type is one {@code case} that reads the material's properties and the part's measures and writes named figures (a float each)
 * the placed block's {@link FigureStore} hands back. Pure: no world, no screen. A type whose metal cannot bake returns empty, and
 * the bench offers no part of it ({@link #suits}).
 */
public final class BlockFigures {

    private BlockFigures() {
    }

    /** The role letter every {@code BLOCK} type's one part is laid on. */
    public static final String ROLE = "S";

    /** Whether {@code material} can fill the role of a {@code BLOCK} type and still bake. */
    public static boolean suits(ModularType type, String roleKey, Material material) {
        if (!roleKey.equals(ROLE))
            return switch (idOf(type) + "/" + roleKey) {
                case "heat_exchanger/F" -> material.thermal().conductivity().isPresent();
                case "water_wheel/R", "pump/V", "compressor/I", "burner/F", "thruster/J", "hand_crank/G", "electromagnet/Y", "boiler/B", "flywheel/B", "flywheel/G", "lamp/B" -> material.density().muPerMb() > 0;
                case "jet/B", "jet/T", "jet/A" -> material.thermal().serviceTemperature().isPresent() && material.density().muPerMb() > 0;
                case "note_block/N", "note_block/O" -> material.density().muPerMb() > 0;
                case "piston/R" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0;
                case "piston/L" -> true;
                case "piston/F" -> material.traits().contains(Material.Trait.STICKY);
                case "thermal_store/I" -> true;
                case "thermal_store/F" -> material.density().muPerMb() > 0;
                case "leyden_jar/F" -> material.ampacity().isPresent();
                case "leyden_jar/W" -> material.electrical().insulation().isPresent();
                case "pallet_rack/D" -> material.density().muPerMb() > 0 && material.mechanical().yieldStrength().isPresent() && serviceTu(material) > 0f;
                default -> true;
            };
        return switch (idOf(type)) {
            case "heat_exchanger" -> material.thermal().conductivity().isPresent();
            case "flywheel" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0 && material.mechanical().yieldStrength().isPresent();
            case "gas_tank", "boiler" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0
                    && material.mechanical().yieldStrength().isPresent();
            case "thruster" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0
                    && material.mechanical().yieldStrength().isPresent();
            case "jet" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0
                    && material.mechanical().yieldStrength().isPresent();
            case "water_wheel", "pump", "compressor", "burner", "hand_crank" -> material.density().muPerMb() > 0;
            case "galvanic_cell" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0;
            case "thermoelectric_generator", "thermocouple_probe" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0
                    && material.thermal().serviceTemperature().or(material::failureTemperature).isPresent();
            case "lamp" -> material.ampacity().isPresent();
            case "linear_motor" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0 && material.ampacity().isPresent();
            case "heating_coil" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0 && material.thermal().serviceTemperature().isPresent()
                    && material.ampacity().isPresent();
            case "tray" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0;
            case "note_block" -> material.density().muPerMb() > 0;
            case "piston" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0 && material.mechanical().yieldStrength().isPresent();
            case "gas_centrifuge" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0 && material.mechanical().yieldStrength().isPresent();
            case "electromagnet" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0;
            case "thermal_store" -> material.thermal().conductivity().isPresent() && material.density().muPerMb() > 0;
            case "crucible" -> material.density().muPerMb() > 0 && material.failureTemperature().isPresent() && material.thermal().specificHeat().isPresent();
            case "leyden_jar" -> material.electrical().insulation().isPresent();
            case "pallet_rack" -> material.density().muPerMb() > 0 && material.mechanical().yieldStrength().isPresent();
            default -> true;
        };
    }

    /**
     * The current a winding of {@code m} carries before it is past its rating: its ampacity (copper's one) times {@code
     * ElectricMachineTuning#WIRE_AMPS_PER_AMPACITY} times its section (1 for the reference wire, gauge squared for a thicker or thinner one).
     * What every wound or filament part is rated for at most, the same figure a cable of the metal carries.
     */
    private static float carries(Material m, float section) {
        return m.ampacity().orElse(0f) * ElectricMachineTuning.WIRE_AMPS_PER_AMPACITY * section;
    }

    /** {@code v} over {@code ref}, held to 0..1: how much of a reference property a part's metal has. */
    private static float share(float v, float ref) {
        return Math.max(0f, Math.min(1f, v / ref));
    }

    /** The hottest a part of {@code m} runs: its service temperature, else the temperature it fails at. */
    private static float serviceTu(Material m) {
        return m.thermal().serviceTemperature().or(m::failureTemperature).map(t -> t.value()).orElse(0f);
    }

    private static float conduction(Material m) {
        return m.thermal().conductivity().map(c -> c.workPerTickPerTu()).orElse(0f);
    }

    /** The id {@code type} is known by (the item and file name), or empty. */
    private static String idOf(ModularType type) {
        for (Map.Entry<ResourceLocation, ModularType> e : ModularTypes.known().entrySet())
            if (e.getValue() == type)
                return e.getKey().getPath();
        return "";
    }

    /** The figures a design of a {@code BLOCK} type makes, by name, or empty if it cannot be built. */
    public static Optional<CompoundTag> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part part = design.part(ROLE);
        if (part == null)
            return Optional.empty();
        Material m = materials.get(part.material());
        if (m == null || !suits(type, ROLE, m))
            return Optional.empty();
        ModularType.Role role = type.roles().get(ROLE);
        CompoundTag out = new CompoundTag();
        switch (design.type().getPath()) {
            // A bay holds what the weaker of its frame and its shelf bears, against the reference metal; the shelf's own
            // service temperature is what hot cargo wears it past.
            case "pallet_rack" -> {
                Part shelf = design.part("D");
                Material sm = shelf == null ? null : materials.get(shelf.material());
                if (sm == null || !suits(type, "D", sm))
                    return Optional.empty();
                float frame = m.mechanical().yieldStrength().get().value() / StoreTuning.REF_FRAME_YIELD;
                float deck = sm.mechanical().yieldStrength().get().value() / StoreTuning.REF_SHELF_YIELD;
                out.putFloat("Rating", Math.max(StoreTuning.RATING_MIN, Math.min(StoreTuning.RATING_MAX, Math.min(frame, deck))));
                out.putFloat("ServiceTu", serviceTu(sm));            }
            // Tubes carry heat as well as their metal conducts it, in proportion to the area they present. Copper at the
            // default area is the old fixed exchanger's 0.1.
            case "heat_exchanger" -> {
                float area = role.axis(part, "area") / ModularTuning.EXCHANGER_REF_AREA;
                float tube = m.thermal().conductivity().get().workPerTickPerTu();
                // fins (an addon) are a second metal bonded on: its conductivity counts for a share, as aluminium on copper
                Part fins = design.part("F");
                Material fm = fins == null ? null : materials.get(fins.material());
                if (fm != null && fm.thermal().conductivity().isPresent())
                    tube += ModularTuning.FIN_SHARE * fm.thermal().conductivity().get().workPerTickPerTu();
                out.putFloat("Conductance", tube * area * ModularBake.param(type, part, "fin", 1f));
            }
            // A gas bottle is a pressure shell (the vessel's bake) many times the size: it holds GAS_TANK_ROOMS cubes of gas
            // at the pressure that cube would, so the same wall carries the same line.
            case "gas_tank" -> {
                Optional<ModularBake.TankSpec> t = ModularBake.tank(design, type, materials);
                if (t.isEmpty())
                    return Optional.empty();
                out.putFloat("Capacity", t.get().volumeMb() * ModularTuning.GAS_TANK_ROOMS);
                out.putFloat("MbPerPu", t.get().volumeMb() * ModularTuning.GAS_TANK_MB_PER_ROOM_PU);
                out.putFloat("Danger", t.get().dangerPu());
                out.putFloat("Critical", t.get().criticalPu());
                out.putFloat("Relief", t.get().reliefMb() * ModularTuning.GAS_TANK_ROOMS);
            }
            // A bigger wheel catches more water (capacity) and turns slower for the same flow; a heavier rim has more
            // momentum. The buckets are the blade count: more of them take more of the flow's push.
            case "water_wheel" -> {
                float d = role.axis(part, "diameter") / ModularTuning.WHEEL_REF_CM;
                float buckets = role.axis(part, "buckets") / ModularTuning.WHEEL_REF_BUCKETS;
                out.putFloat("Power", ModularTuning.WHEEL_CAPACITY_SU * d * buckets * ModularBake.param(type, part, "power", 1f));
                out.putFloat("RpmPerFlow", ModularTuning.WHEEL_RPM_PER_FLOW / d * ModularBake.param(type, part, "rpm", 1f));
float inertia = ModularTuning.WHEEL_INERTIA * d * d * d * (float) m.density().muPerMb() / ModularTuning.WHEEL_REF_DENSITY;
                // a ring (an addon) of dense metal round the rim: more momentum, and slower to spin up
                Part ring = design.part("R");
                Material rm = ring == null ? null : materials.get(ring.material());
                if (rm != null)
                    inertia += ModularTuning.RING_INERTIA * d * (float) rm.density().muPerMb();
                out.putFloat("Inertia", inertia);
            }
            // A thicker wall has more to heat (mass) and loses less (leak); the wall's own conductivity sets the loss.
            case "thermal_store" -> {
                float wall = role.axis(part, "wall") / ModularTuning.STORE_REF_CM;
float mass = ModularBake.param(type, part, "mass", 1f) * ModularTuning.STORE_MASS * wall * (float) m.density().muPerMb() / ModularTuning.STORE_REF_DENSITY;
                float leak = ModularBake.param(type, part, "leak", 1f) * ModularTuning.STORE_LEAK / wall * m.thermal().conductivity().get().workPerTickPerTu() / ModularTuning.STORE_REF_CONDUCTIVITY;
                // a salt fill (an addon) is more stuff to heat; a lagging (an addon) is a poor conductor round the outside
                Part salt = design.part("F"), lag = design.part("I");
                Material sm = salt == null ? null : materials.get(salt.material()), lm = lag == null ? null : materials.get(lag.material());
                if (sm != null)
                    mass += ModularTuning.STORE_SALT_MASS * (float) sm.density().muPerMb() * sm.thermal().specificHeat().map(c -> c.workPerMuPerTu()).orElse(ModularTuning.STORE_SALT_SPECIFIC_HEAT) / ModularTuning.STORE_SALT_VOLUMETRIC_HEAT;
                if (lm != null)
                    leak *= Math.min(1f, Math.max(ModularTuning.STORE_LAG_FLOOR, lm.thermal().conductivity()
                            .map(c -> c.workPerTickPerTu() / ModularTuning.STORE_REF_CONDUCTIVITY).orElse(0.5f)));
                out.putFloat("Mass", mass);
                out.putFloat("Leak", leak);
            }
            // A crucible is its wall: more slots is a bigger vessel (heat mass with the square), a thicker wall has more to heat, and the
            // wall's own stuff (density times specific heat, against the brick the first one was built of) sets how much heat that is.
            // The hottest the wall holds is its material's failure temperature; past it the wall wears.
            case "crucible" -> {
                float slots = Math.round(role.axis(part, "slots"));
                float wall = role.axis(part, "wall") / ModularTuning.CRUCIBLE_REF_WALL_MM;
                float heat = (float) m.density().muPerMb() * m.thermal().specificHeat().get().workPerMuPerTu() / ModularTuning.CRUCIBLE_REF_VOLUMETRIC_HEAT;
                out.putFloat("Slots", slots);
                out.putFloat("Tank", ModularTuning.CRUCIBLE_TANK_MB_PER_SLOT * slots);
                out.putFloat("Mass", ModularTuning.CRUCIBLE_MASS * (slots / 2f) * (slots / 2f) * wall * heat);
                out.putFloat("ServiceTu", m.failureTemperature().get().value());
                // The sudden jump the wall takes without crazing (glass, ceramic): a hot pour into a cold wall past it wears the wall. 0 for a wall that is not rated.
                out.putFloat("ShockTu", m.thermal().shockLimit().map(t -> t.value()).orElse(0f));
            }
            // A rotor separates by centripetal pull, which grows with radius and the square of the speed, so a wider rotor starts at a lower speed
            // (a = w^2 r); it weighs, and so loads the shaft, with its radius squared and its metal's density; and its rim holds only as fast
            // as its metal's strength over density allows, falling with radius (the flywheel's rim law). Past that the rotor tears.
            case "gas_centrifuge" -> {
                float r = role.axis(part, "radius") / ModularTuning.CENTRIFUGE_REF_CM;
                out.putFloat("MinRpm", io.github.soundgoodizerfan.feedback.machine.MachineTuning.CENTRIFUGE_MIN_RPM / (float) Math.sqrt(r));
                out.putFloat("Load", io.github.soundgoodizerfan.feedback.machine.MachineTuning.CENTRIFUGE_LOAD_SU.value() * r * r * (float) m.density().muPerMb() / 7.9f);
                out.putFloat("TearRpm", ModularTuning.ROTOR_RIM_CONSTANT * (float) Math.sqrt(m.mechanical().yieldStrength().get().value() / m.density().muPerMb()) / (r * ModularTuning.CENTRIFUGE_REF_CM / 50f));
            }
            // A deck's three shapes are the old bubble-cap, sieve and packed trays; the hole and pitch drilled in it tune them: a finer hole holds back more
            // of the vapour and cokes faster, the open area (hole over pitch, squared) is how much it takes a tick.
            case "tray" -> {
                float hole = role.axis(part, "hole") / ModularTuning.TRAY_REF_HOLE_MM, pitch = role.axis(part, "pitch") / ModularTuning.TRAY_REF_PITCH_MM;
                out.putFloat("Efficiency", Math.min(0.98f, ModularBake.param(type, part, "efficiency", 0.8f) * (float) Math.pow(1f / hole, 0.15)));
                out.putFloat("Flow", ModularBake.param(type, part, "flow", 40f) * hole * hole / (pitch * pitch));
                out.putFloat("Foul", ModularBake.param(type, part, "foul", 1.5f) / (float) Math.sqrt(hole));
            }
            // A thicker wire passes more current (with the square of its gauge) and a longer one (more turns) less; the heat each Eu makes is the share of
            // the power the wire's own drop turns to heat, so nichrome heats well and copper hardly; the mass to heat is the wire's, and it holds as hot as its metal.
            case "heating_coil" -> {
                float g = role.axis(part, "gauge") / ModularTuning.COIL_REF_GAUGE, turns = role.axis(part, "turns") / ModularTuning.COIL_REF_TURNS;
                float drop = m.electrical().drop().orElse(0.5f);
                float heat = drop / (drop + ModularTuning.COIL_DROP_HALF);
                // The wire carries what its ampacity allows over its section (gauge squared); the coil is rated for no more than that.
                float carries = carries(m, g * g);
                out.putFloat("Ampacity", carries);
                out.putFloat("Amps", Math.min(ElectricMachineTuning.COIL_AMPS * g * g / turns, carries));
                out.putFloat("Work", ElectricMachineTuning.COIL_WORK_PER_EU * heat / 0.5f);
                out.putFloat("Mass", ElectricMachineTuning.COIL_MASS.workPerTu() * g * g * turns * (float) m.density().muPerMb() / 7.9f);
                out.putFloat("ServiceTu", m.thermal().serviceTemperature().get().value());
            }
            // A ring of winding round the tube: more turns pull a magnetic capsule harder and draw more current, and a thicker wire draws more
            // (gauge squared); a push of 1 would be no ring, the reference ring pushes as the old fixed motor did.
            case "linear_motor" -> {
                float turns = role.axis(part, "turns") / ModularTuning.RING_REF_TURNS, g = role.axis(part, "gauge") / ModularTuning.RING_REF_GAUGE;
                // The ring draws with turns and gauge squared, the wire carries with gauge squared: the rated draw is the lesser. A thicker wire pushes
                // harder (ampere-turns) but draws far more for it; a ring held down by its wire pushes by the share of its draw it is allowed.
                float draw = io.github.soundgoodizerfan.feedback.core.tube.TubeTuning.LINEAR_MOTOR_AMPS * turns * g * g;
                float carries = carries(m, g * g);
                out.putFloat("Ampacity", carries);
                out.putFloat("Amps", Math.min(draw, carries));
                out.putFloat("Push", 1f + (io.github.soundgoodizerfan.feedback.core.tube.TubeTuning.TUBE_LINEAR_FACTOR - 1f) * turns
                        * (float) Math.pow(g, ModularTuning.RING_PUSH_GAUGE_EXP) * Math.min(1f, carries / draw));
            }
            // Charge held is plate area over glass thickness; thin glass leaks it away much faster (as the square).
            case "leyden_jar" -> {
                float area = role.axis(part, "area") / ModularTuning.JAR_REF_AREA * ModularBake.param(type, part, "area", 1f);
                float thin = ModularTuning.JAR_REF_WALL_MM / role.axis(part, "wall");
                // The glass sets the band the jar is rated: it charges when fed that band, and holds its packets' worth of Eu.
                VoltageBand band = m.electrical().insulation().orElse(ElectricMachineTuning.LEYDEN_JAR_BAND);
                out.putFloat("Band", band.ordinal());
                out.putFloat("StoresEu", ModularTuning.JAR_CAPACITY_PACKETS * area * thin * ElectricRules.packetEu(band));
float leak = ModularBake.param(type, part, "leak", 1f) * ModularTuning.JAR_LEAK * thin * thin;
                float burst = ModularTuning.JAR_BURST_AMPS * area;
                // foil (an addon) carries the discharge, as much as its ampacity allows; an outer sleeve (an addon) is a second insulator
                Part foil = design.part("F"), sleeve = design.part("W");
                Material fm = foil == null ? null : materials.get(foil.material());
                if (fm != null && fm.ampacity().isPresent())
                    burst *= Math.max(0.5f, Math.min(3f, fm.ampacity().get() / ModularTuning.JAR_REF_AMPACITY));
                // the sleeve cuts the leak the more, the higher the band its own insulation holds off
                Material sl = sleeve == null ? null : materials.get(sleeve.material());
                if (sl != null)
                    leak *= net.minecraft.util.Mth.lerp((float) sl.electrical().insulation().map(Enum::ordinal).orElse(0) / (VoltageBand.values().length - 1),
                            ModularTuning.JAR_SLEEVE_LEAK_POOR, ModularTuning.JAR_SLEEVE_LEAK);
                // a radioactive glass ionises the gas in the jar, which carries charge across it
                leak *= 1f + ModularTuning.JAR_IONISATION_LEAK * m.radioactivity().map(r -> r.value()).orElse(0f);
                out.putFloat("Seep", leak);
                out.putFloat("BurstAmps", burst);
            }
            // Impeller: flow goes with the square of the diameter; the variant trades flow against lift (an axial wheel moves a lot and cannot lift).
            // A volute (an addon) is a casing that turns speed into flow.
            case "pump" -> {
                float d = role.axis(part, "diameter") / 30f, flow = ModularBake.param(type, part, "flow", 1f);
                // a smooth wall (low friction) turns more of the speed into flow
                Part vp = design.part("V");
                Material vm = vp == null ? null : materials.get(vp.material());
                float volute = vm == null ? 1f : 1f + ModularTuning.VOLUTE_GAIN * (1f - share(vm.mechanical().friction().orElse(ModularTuning.VOLUTE_REF_FRICTION), ModularTuning.VOLUTE_REF_FRICTION));
                out.putFloat("MbPerRpm", 0.5f * d * d * flow * volute);
                out.putFloat("Load", 8f * d * d * flow * volute);
                out.putFloat("Lift", 6f / ModularBake.param(type, part, "lift", 1f));
            }
            // Note block: the body is cut to a pitch; each noisemaker is an instrument (its shape) whose material sets how loud it rings (harder is louder)
            // and which way it is pitched (a heavy one plays lower). The per-voice figures start with an underscore, so the item's tooltip lists the count only.
            case "note_block" -> {
                out.putFloat("Pitch", role.axis(part, "pitch"));
                int voices = 0;
                String[] letters = {"N", "O"}, inst = {"_i0", "_i1"}, loud = {"_v0", "_v1"}, shift = {"_s0", "_s1"};
                for (int i = 0; i < letters.length; i++) {
                    Part voice = design.part(letters[i]);
                    Material vm = voice == null ? null : materials.get(voice.material());
                    if (vm == null) {
                        out.putFloat(inst[i], -1f);
                        continue;
                    }
                    voices++;
                    out.putFloat(inst[i], ModularBake.param(type, voice, "instrument", 0f));
                    out.putFloat(loud[i], Math.max(io.github.soundgoodizerfan.feedback.machine.note.NoteTuning.MIN_VOLUME, Math.min(io.github.soundgoodizerfan.feedback.machine.note.NoteTuning.MAX_VOLUME,
                            io.github.soundgoodizerfan.feedback.machine.note.NoteTuning.LOUD_BASE + io.github.soundgoodizerfan.feedback.machine.note.NoteTuning.LOUD_PER_ST * vm.hardness().value())));
                    float octaves = -io.github.soundgoodizerfan.feedback.machine.note.NoteTuning.SHIFT_OCTAVES_PER_DOUBLING
                            * (float) (Math.log((double) vm.density().muPerMb() / io.github.soundgoodizerfan.feedback.machine.note.NoteTuning.REF_DENSITY) / Math.log(2));
                    out.putFloat(shift[i], Math.max(-io.github.soundgoodizerfan.feedback.machine.note.NoteTuning.MAX_SHIFT_OCTAVES,
                            Math.min(io.github.soundgoodizerfan.feedback.machine.note.NoteTuning.MAX_SHIFT_OCTAVES, octaves)));
                }
                if (voices == 0)
                    return Optional.empty();
                out.putFloat("Voices", voices);
            }
            // Cylinder: the bore sets the area the gas pushes on; the shell's yield over its wall against the bore sets the pressure it bursts at; the rod's
            // length is the reach; the seal's hardness is how much gas slips by (a soft leather seals better than a hard plastic); a sticky face pulls back.
            case "piston" -> {
                float bore = role.axis(part, "bore"), wall = role.axis(part, "wall");
                float r = bore / 20f;
                out.putFloat("Area", (float) Math.PI * r * r);
                float danger = io.github.soundgoodizerfan.feedback.machine.piston.PistonTuning.CYLINDER_PU_PER_STRESS * m.mechanical().yieldStrength().get().value() * (wall / 10f) / r;
                out.putFloat("Danger", danger);
                Part rod = design.part("R");
                out.putFloat("Reach", rod == null ? 1f : type.roles().get("R").axis(rod, "reach"));
                Material seal = addon(design, materials, "L");
                out.putFloat("Slip", io.github.soundgoodizerfan.feedback.machine.piston.PistonTuning.LEAK_BASE * (seal == null ? io.github.soundgoodizerfan.feedback.machine.piston.PistonTuning.LEAK_REF_HARDNESS : seal.hardness().value()) / io.github.soundgoodizerfan.feedback.machine.piston.PistonTuning.LEAK_REF_HARDNESS);
                out.putFloat("Sticky", design.part("F") == null ? 0f : 1f);
            }
            // Rotor: a bigger rotor moves more and takes more; the variant trades flow against heat (staging cools the charge); an intercooler (an addon) takes heat out between stages.
            case "compressor" -> {
                float d = role.axis(part, "diameter") / 40f;
                out.putFloat("MbPerRpm", 0.4f * d * d * ModularBake.param(type, part, "flow", 1f));
                out.putFloat("Load", 30f * d * d * ModularBake.param(type, part, "load", 1f));
                out.putFloat("Heat", 60f * ModularBake.param(type, part, "heat", 1f) * intercooler(design, materials));
            }
            // Nozzle: fuel burned goes with the bore area; the variant trades burn against flame temperature; a flame holder (an addon) steadies the flame a little hotter.
            case "burner" -> {
                float b = role.axis(part, "bore") / 10f;
                out.putFloat("Burn", 0.5f * b * b * ModularBake.param(type, part, "burn", 1f));
                // A wide bore keeps a hotter flame (the narrow one quenches against its walls) and pays for it in fuel with the bore squared.
                float boreFlame = Math.max(ModularTuning.BURNER_BORE_FLAME_RANGE[0], Math.min(ModularTuning.BURNER_BORE_FLAME_RANGE[1],
                        (float) Math.pow(b, ModularTuning.BURNER_BORE_FLAME_EXP)));
                out.putFloat("Flame", ModularBake.param(type, part, "flame", 1f) * flameHolder(design, materials) * boreFlame);
            }
            // Nozzle: push and fuel burned both go with the bore area; the variant trades push against fuel; a regen jacket (an addon) warms the fuel first, so less of it burns.
            case "thruster" -> {
                float b = role.axis(part, "bore") / 20f;
                out.putFloat("Force", b * b * ModularBake.param(type, part, "thrust", 1f));
                out.putFloat("Burn", io.github.soundgoodizerfan.feedback.core.FTuning.THRUSTER_FUEL_MB_PER_TICK * b * b * ModularBake.param(type, part, "burn", 1f) * regenJacket(design, materials));
            }
            // Jet: the compressor sets the intake (its diameter) and the stages; the nozzle is converging or converging-diverging; the combustor
            // liner and the turbine blades each state the heat they stand; an afterburner (an addon) adds a reheat stage behind the turbine.
            case "jet" -> {
                Part nozzle = design.part("N"), liner = design.part("B"), blades = design.part("T"), reheat = design.part("A");
                Material lm = liner == null ? null : materials.get(liner.material()), bm = blades == null ? null : materials.get(blades.material()),
                        am = reheat == null ? null : materials.get(reheat.material());
                float d = role.axis(part, "diameter") / 100f;
                out.putFloat("Intake", (float) (Math.PI / 4 * d * d));
                out.putFloat("Stages", ModularBake.param(type, part, "stages", 3f));
                if (nozzle != null && ModularBake.param(type, nozzle, "divergent", 0f) > 0f)
                    out.putFloat("Divergent", 1f);
                out.putFloat("LinerTu", lm == null ? 0f : lm.thermal().serviceTemperature().get().value());
                out.putFloat("BladeTu", bm == null ? 0f : bm.thermal().serviceTemperature().get().value());
                out.putFloat("ReheatTu", am == null ? 0f : am.thermal().serviceTemperature().get().value());
                if (m.mechanical().yieldStrength().isPresent() && m.density().muPerMb() > 0)
                    out.putFloat("TipLimit", Math.min(io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning.PROP_TIP_SPEED_CAP,
                            io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning.PROP_TIP_SPEED_PER_SQRT_SPECIFIC_YIELD
                                    * (float) Math.sqrt(m.mechanical().yieldStrength().get().value() / m.density().muPerMb())));
            }
            // Crank: a longer arm is more lever and a slower hand; a flywheel (an addon) carries it through the dead spot.
            case "hand_crank" -> {
                // The arm's length is the lever and the inverse of the hand's speed, against the standard arm.
                float arm = role.axesFor(part).containsKey("arm_length") ? role.axis(part, "arm_length") / role.axesFor(part).get("arm_length").def() : 1f;
                out.putFloat("Power", 12f * ModularBake.param(type, part, "lever", 1f) * arm);
                out.putFloat("Rpm", 32f * ModularBake.param(type, part, "rpm", 1f) / arm);
                // the flywheel plate's momentum goes with its density (the heavier plate carries the hand through the dead spot)
                Part fly = design.part("G");
                Material fm = fly == null ? null : materials.get(fly.material());
                out.putFloat("Inertia", fm == null ? ModularTuning.CRANK_BARE_INERTIA
                        : ModularTuning.CRANK_BARE_INERTIA + ModularTuning.CRANK_PLATE_INERTIA * Math.min(ModularTuning.CRANK_PLATE_DENSITY_CAP, (float) fm.density().muPerMb() / ModularTuning.CRANK_REF_DENSITY));
            }
            // Core: turns raise the pull and the current together; the shape trades pull against reach; a yoke (an addon) closes the magnetic path.
            case "electromagnet" -> {
                float n = role.axis(part, "turns") / 100f;
                // The winding is the part's own metal: it draws with its turns, and carries what its ampacity allows; past that the magnet is held to it.
                float drawn = n;
                if (m.ampacity().isPresent()) {
                    float carries = carries(m, 1f);
                    out.putFloat("Ampacity", carries);
                    drawn = Math.min(n, carries);
                }
                out.putFloat("Amps", drawn);
                // the core is what is magnetised: a core that is not magnetic pulls only NONMAGNETIC_CORE of what iron does, and one that loses
                // much to eddies and hysteresis keeps less of it
                float core = m.separation().magnetic() ? 1f - 0.5f * share(m.electrical().coreLoss().orElse(ModularTuning.YOKE_REF_CORE_LOSS / 2f), ModularTuning.YOKE_REF_CORE_LOSS)
                        : ModularTuning.NONMAGNETIC_CORE;
                out.putFloat("Pull", 0.08f * drawn * ModularBake.param(type, part, "pull", 1f) * yoke(design, materials) * core / 0.85f);
                out.putFloat("Range", 4f * ModularBake.param(type, part, "range", 1f));
            }
            // Anode: a bigger anode feeds more current and lasts in proportion to its bulk; the shape trades current against life; a cathode metal further above the anode draws more.
            case "galvanic_cell" -> {
                float a = role.axis(part, "anode") / 50f, amps = ModularBake.param(type, part, "amps", 1f) * a * cathode(design, role, materials, m);
                out.putFloat("Supply", amps);
                out.putFloat("AnodeEu", ElectricMachineTuning.GALVANIC_ANODE_EU * a * ModularBake.param(type, part, "life", 1f));
            }
            // Two legs of different metals: the voltage a degree of difference drives is the gap between their Seebeck figures (same metal, no gap, no source);
            // the legs' section sets the current, stubby legs carry more and hold less of the temperature difference than slender ones, and the weaker leg's
            // service temperature is the hottest the pair stands.
            case "thermoelectric_generator" -> {
                Material pos = ModularBake.slotMaterial(role, part, "positive", materials), neg = ModularBake.slotMaterial(role, part, "negative", materials);
                if (pos == null || neg == null)
                    return Optional.empty();
                out.putFloat("Gap", Math.abs(pos.electrical().seebeckOrEstimate() - neg.electrical().seebeckOrEstimate()));
                out.putFloat("Supply", ModularTuning.THERMO_LEG_AMPS * (role.axis(part, "section") / 50f) * ModularBake.param(type, part, "amps", 1f));
                out.putFloat("Hold", ModularBake.param(type, part, "hold", 1f));
                out.putFloat("ServiceTu", Math.min(serviceTu(pos), serviceTu(neg)));
            }
            // A probe's pair: the Seebeck gap is how finely it reads, the weaker wire's service temperature is the top of its scale.
            case "thermocouple_probe" -> {
                Material pos = ModularBake.slotMaterial(role, part, "positive", materials), neg = ModularBake.slotMaterial(role, part, "negative", materials);
                if (pos == null || neg == null)
                    return Optional.empty();
                out.putFloat("Gap", Math.abs(pos.electrical().seebeckOrEstimate() - neg.electrical().seebeckOrEstimate()));
                out.putFloat("ServiceTu", Math.min(serviceTu(pos), serviceTu(neg)));
            }
            // Filament: a thicker one draws more and lasts; the shape trades draw against toughness; a ballast (an addon) lets it ride a little overvoltage.
            case "lamp" -> {
                float g = role.axis(part, "gauge") / 50f;
                // The filament carries what its ampacity allows over its section; it is rated for the lesser of that and its draw. A thicker one glows
                // brighter, and tougher, but draws with the gauge squared.
                float carries = carries(m, g * g);
                out.putFloat("Ampacity", carries);
                out.putFloat("Amps", Math.min(0.5f * g * g * ModularBake.param(type, part, "amps", 1f), carries));
                out.putFloat("Light", (float) Math.pow(g, ModularTuning.LAMP_LIGHT_EXP));
                out.putFloat("Burnout", 1.5f * ModularBake.param(type, part, "tough", 1f) * (0.6f + 0.4f * g) * ballast(design, materials));
            }
            // A boiler is a shell like the vessel's: the shell sets the pressure it risks and gives way at, and the pressure its seams weep and its valve opens at follow it.
            case "boiler" -> {
                // The size is read from the design, not a second block: a boiler of scale n is n plain boilers in one.
                out.putFloat("Scale", role.axis(part, "scale"));
                Optional<ModularBake.TankSpec> t = ModularBake.tank(design, type, materials);
                if (t.isEmpty())
                    return Optional.empty();
                out.putFloat("Danger", t.get().dangerPu());
                out.putFloat("Critical", t.get().criticalPu());
            }
            // Disc: momentum goes with the fourth power of the radius and the thickness; the rim speed it survives falls as it grows (the rim moves faster at the same RPM)
            // and rises with the metal's strength over density; the variant trades mass against rim strength. Bands (an addon) hold the rim; a bearing (an addon) cuts the drag.
            case "flywheel" -> {
                float r = role.axis(part, "radius") / 50f, th = role.axis(part, "thickness") / 10f;
                float massAt = ModularBake.interp(role.axis(part, "rim_mass"), ModularTuning.FLYWHEEL_RIM_MASS_AT, ModularTuning.FLYWHEEL_MASS);
                float inertia = 400f * r * r * r * r * th * massAt * (float) m.density().muPerMb() / 7.2f;
                float yield = m.mechanical().yieldStrength().get().value();
                Part bands = design.part("B");
                Material bm = bands == null ? null : materials.get(bands.material());
                float rim = 44f * (float) Math.sqrt(yield / m.density().muPerMb()) / r * ModularBake.interp(role.axis(part, "rim_mass"), ModularTuning.FLYWHEEL_RIM_MASS_AT, ModularTuning.FLYWHEEL_RIM);
                if (bm != null && bm.mechanical().yieldStrength().isPresent())
                    rim *= 1f + 0.25f * bm.mechanical().yieldStrength().get().value() / (bm.mechanical().yieldStrength().get().value() + yield);
                out.putFloat("Inertia", inertia);
                out.putFloat("RimRpm", rim);
                out.putFloat("Drag", 0.2f * r * bearing(design, materials));
            }
            default -> {
            }
        }
        // A lining (the inside of a shell) or a surface (the skin over a core) is what a corrosive liquid meets: the figure is how well it resists.
        for (String skin : new String[] {"lining", "surface"}) {
            Material sm = ModularBake.slotMaterial(role, part, skin, materials);
            if (sm != null && role.shapingOf(part).flatMap(sh -> sh.slot(skin)).isPresent()) {
                float own = m.corrosionResistance().orElse(0f), cover = sm.corrosionResistance().orElse(0f);
                out.putFloat("Resists", skin.equals("lining") ? cover : 1f - (1f - own) * (1f - cover));
            }
        }
        return Optional.of(out);
    }

    /**
     * The band a single galvanic cell of {@code design} supplies: a close pair of electrodes is the lowest band, a pair whose potentials
     * stand {@code GALVANIC_WIDE_GAP} apart the next one (series cells raise it further, but that is the placed block's). Empty when
     * either electrode states no potential or the pair has no gap, which supplies nothing.
     */
    public static Optional<VoltageBand> galvanicBand(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part part = design.part(ROLE);
        Material anode = part == null ? null : materials.get(part.material());
        Material cathode = part == null ? null : ModularBake.slotMaterial(type.roles().get(ROLE), part, "cathode", materials);
        if (anode == null || cathode == null || anode.electrical().electrodePotential().isEmpty() || cathode.electrical().electrodePotential().isEmpty())
            return Optional.empty();
        float gap = Math.abs(cathode.electrical().electrodePotential().get() - anode.electrical().electrodePotential().get());
        if (gap <= 0f)
            return Optional.empty();
        return Optional.of(VoltageBand.values()[VoltageBand.LV.ordinal() + (gap >= ElectricMachineTuning.GALVANIC_WIDE_GAP ? 1 : 0)]);
    }

    /** The material of addon part {@code role}, or null if the design has none (or it is unknown). */
    private static Material addon(ModularDesign design, Map<ResourceLocation, Material> materials, String role) {
        Part p = design.part(role);
        return p == null ? null : materials.get(p.material());
    }

    /** An intercooler takes heat out between stages by how well its plate conducts: copper takes {@code INTERCOOLER_TAKE} of it. */
    private static float intercooler(ModularDesign design, Map<ResourceLocation, Material> materials) {
        Material c = addon(design, materials, "I");
        return c == null ? 1f : 1f - ModularTuning.INTERCOOLER_TAKE * share(conduction(c), ModularTuning.ADDON_REF_CONDUCTIVITY);
    }

    /** A flame holder lives in the flame: the hotter its metal holds, the hotter a flame it steadies. */
    private static float flameHolder(ModularDesign design, Map<ResourceLocation, Material> materials) {
        Material h = addon(design, materials, "F");
        return h == null ? 1f : 1f + ModularTuning.FLAME_HOLDER_GAIN * share(serviceTu(h), ModularTuning.FLAME_HOLDER_REF_TU);
    }

    /** A regen jacket hands the nozzle's heat to the fuel: the better its plate conducts, the more heat the fuel carries back and the less of it burns. */
    private static float regenJacket(ModularDesign design, Map<ResourceLocation, Material> materials) {
        Material j = addon(design, materials, "J");
        return j == null ? 1f : 1f - ModularTuning.REGEN_SAVING * share(conduction(j), ModularTuning.ADDON_REF_CONDUCTIVITY);
    }

    /** A yoke closes the magnetic path only if it is magnetic, and the less it loses to eddies and hysteresis the more of the pull it keeps. */
    private static float yoke(ModularDesign design, Map<ResourceLocation, Material> materials) {
        Material y = addon(design, materials, "Y");
        if (y == null)
            return 1f;
        if (!y.separation().magnetic())
            return 1f;
        return 1f + ModularTuning.YOKE_GAIN * (1f - share(y.electrical().coreLoss().orElse(ModularTuning.YOKE_REF_CORE_LOSS / 2f), ModularTuning.YOKE_REF_CORE_LOSS));
    }

    /** A cell's voltage is the gap between its electrodes' potentials: a cathode further above the anode draws more current. */
    private static float cathode(ModularDesign design, ModularType.Role role, Map<ResourceLocation, Material> materials, Material anode) {
        Part part = design.part(ROLE);
        Material c = part == null ? null : ModularBake.slotMaterial(role, part, "cathode", materials);
        if (c == null)
            return 1f;
        float gap = c.electrical().electrodePotential().orElse(0f) - anode.electrical().electrodePotential().orElse(0f);
        return 1f + ModularTuning.CATHODE_GAIN * Math.max(0f, Math.min(ModularTuning.CATHODE_GAP_CAP, gap / ModularTuning.CATHODE_REF_GAP));
    }

    /** A ballast soaks an overvoltage as heat: the more heat a mB of its metal takes (density times specific heat), the bigger the surge it carries. */
    private static float ballast(ModularDesign design, Map<ResourceLocation, Material> materials) {
        Material b = addon(design, materials, "B");
        if (b == null)
            return 1f;
        float cap = (float) b.density().muPerMb() * b.thermal().specificHeat().map(c -> c.workPerMuPerTu()).orElse(0f);
        return 1f + ModularTuning.BALLAST_GAIN * share(cap, ModularTuning.BALLAST_REF_HEAT_CAPACITY);
    }

    /** A bearing's sliding friction is the drag it leaves on the spinning wheel. */
    private static float bearing(ModularDesign design, Map<ResourceLocation, Material> materials) {
        Material b = addon(design, materials, "G");
        if (b == null)
            return 1f;
        return Math.min(1f, ModularTuning.BEARING_DRAG_FLOOR + ModularTuning.BEARING_DRAG_PER_FRICTION * b.mechanical().friction().orElse(ModularTuning.BEARING_DEFAULT_FRICTION));
    }
}
