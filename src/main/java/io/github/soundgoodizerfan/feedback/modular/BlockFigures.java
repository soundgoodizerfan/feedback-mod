// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.Material;

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
                case "water_wheel/R", "pump/V", "compressor/I", "burner/F", "hand_crank/G", "electromagnet/Y", "boiler/B", "large_boiler/B", "flywheel/B", "flywheel/G", "galvanic_cell/C", "lamp/B" -> material.density().muPerMb() > 0;
                case "thermal_store/I" -> true;
                case "thermal_store/F" -> material.density().muPerMb() > 0;
                case "leyden_jar/F" -> material.ampacity().isPresent();
                case "leyden_jar/W" -> material.electrical().insulation().isPresent();
                default -> true;
            };
        return switch (idOf(type)) {
            case "heat_exchanger" -> material.thermal().conductivity().isPresent();
            case "flywheel" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0 && material.mechanical().yieldStrength().isPresent();
            case "gas_tank", "boiler", "large_boiler" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0
                    && material.mechanical().yieldStrength().isPresent();
            case "water_wheel", "pump", "compressor", "burner", "hand_crank" -> material.density().muPerMb() > 0;
            case "galvanic_cell" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0;
            case "lamp" -> material.ampacity().isPresent();
            case "electromagnet" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0;
            case "thermal_store" -> material.thermal().conductivity().isPresent() && material.density().muPerMb() > 0;
            case "leyden_jar" -> material.electrical().insulation().isPresent();
            default -> true;
        };
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
                    mass += ModularTuning.STORE_SALT_MASS * (float) sm.density().muPerMb() / ModularTuning.STORE_SALT_DENSITY;
                if (lm != null)
                    leak *= Math.min(1f, Math.max(ModularTuning.STORE_LAG_FLOOR, lm.thermal().conductivity()
                            .map(c -> c.workPerTickPerTu() / ModularTuning.STORE_REF_CONDUCTIVITY).orElse(0.5f)));
                out.putFloat("Mass", mass);
                out.putFloat("Leak", leak);
            }
            // Charge held is plate area over glass thickness; thin glass leaks it away much faster (as the square).
            case "leyden_jar" -> {
                float area = role.axis(part, "area") / ModularTuning.JAR_REF_AREA * ModularBake.param(type, part, "area", 1f);
                float thin = ModularTuning.JAR_REF_WALL_MM / role.axis(part, "wall");
                out.putFloat("Capacitance", ModularTuning.JAR_CAPACITANCE * area * thin);
float leak = ModularBake.param(type, part, "leak", 1f) * ModularTuning.JAR_LEAK * thin * thin;
                float burst = ModularTuning.JAR_BURST_AMPS * area;
                // foil (an addon) carries the discharge, as much as its ampacity allows; an outer sleeve (an addon) is a second insulator
                Part foil = design.part("F"), sleeve = design.part("W");
                Material fm = foil == null ? null : materials.get(foil.material());
                if (fm != null && fm.ampacity().isPresent())
                    burst *= Math.max(0.5f, Math.min(3f, fm.ampacity().get() / ModularTuning.JAR_REF_AMPACITY));
                if (sleeve != null)
                    leak *= ModularTuning.JAR_SLEEVE_LEAK;
                out.putFloat("Leak", leak);
                out.putFloat("BurstAmps", burst);
            }
            // Impeller: flow goes with the square of the diameter; the variant trades flow against lift (an axial wheel moves a lot and cannot lift).
            // A volute (an addon) is a casing that turns speed into flow.
            case "pump" -> {
                float d = role.axis(part, "diameter") / 30f, flow = ModularBake.param(type, part, "flow", 1f);
                float volute = design.part("V") == null ? 1f : 1.15f;
                out.putFloat("MbPerRpm", 0.5f * d * d * flow * volute);
                out.putFloat("Load", 8f * d * d * flow * volute);
                out.putFloat("Lift", 6f / ModularBake.param(type, part, "lift", 1f));
            }
            // Rotor: a bigger rotor moves more and takes more; the variant trades flow against heat (staging cools the charge); an intercooler (an addon) takes heat out between stages.
            case "compressor" -> {
                float d = role.axis(part, "diameter") / 40f;
                out.putFloat("MbPerRpm", 0.4f * d * d * ModularBake.param(type, part, "flow", 1f));
                out.putFloat("Load", 30f * d * d * ModularBake.param(type, part, "load", 1f));
                out.putFloat("Heat", 60f * ModularBake.param(type, part, "heat", 1f) * (design.part("I") == null ? 1f : 0.6f));
            }
            // Nozzle: fuel burned goes with the bore area; the variant trades burn against flame temperature; a flame holder (an addon) steadies the flame a little hotter.
            case "burner" -> {
                float b = role.axis(part, "bore") / 10f;
                out.putFloat("Burn", 0.5f * b * b * ModularBake.param(type, part, "burn", 1f));
                out.putFloat("Flame", ModularBake.param(type, part, "flame", 1f) * (design.part("F") == null ? 1f : 1.03f));
            }
            // Crank: a longer arm is more lever and a slower hand; a flywheel (an addon) carries it through the dead spot.
            case "hand_crank" -> {
                out.putFloat("Power", 12f * ModularBake.param(type, part, "lever", 1f));
                out.putFloat("Rpm", 32f * ModularBake.param(type, part, "rpm", 1f));
                out.putFloat("Inertia", design.part("G") == null ? 4f : 12f);
            }
            // Core: turns raise the pull and the current together; the shape trades pull against reach; a yoke (an addon) closes the magnetic path.
            case "electromagnet" -> {
                float n = role.axis(part, "turns") / 100f;
                out.putFloat("Amps", n);
                out.putFloat("Pull", 0.08f * n * ModularBake.param(type, part, "pull", 1f) * (design.part("Y") == null ? 1f : 1.2f));
                out.putFloat("Range", 4f * ModularBake.param(type, part, "range", 1f));
            }
            // Anode: a bigger anode feeds more current and lasts in proportion to its bulk; the shape trades current against life; a copper cathode (an addon) draws a little more.
            case "galvanic_cell" -> {
                float a = role.axis(part, "anode") / 50f, amps = ModularBake.param(type, part, "amps", 1f) * a * (design.part("C") == null ? 1f : 1.25f);
                out.putFloat("Amps", amps);
                out.putFloat("AnodeTicks", 24000f * a * ModularBake.param(type, part, "life", 1f) / Math.max(0.1f, ModularBake.param(type, part, "amps", 1f) * a));
            }
            // Filament: a thicker one draws more and lasts; the shape trades draw against toughness; a ballast (an addon) lets it ride a little overvoltage.
            case "lamp" -> {
                float g = role.axis(part, "gauge") / 50f;
                out.putFloat("Amps", 0.5f * g * g * ModularBake.param(type, part, "amps", 1f));
                out.putFloat("Burnout", 1.5f * ModularBake.param(type, part, "tough", 1f) * (0.6f + 0.4f * g) * (design.part("B") == null ? 1f : 1.2f));
            }
            // A boiler is a shell like the vessel's: the shell sets the pressure it risks and gives way at, and the pressure its seams weep and its valve opens at follow it.
            case "boiler", "large_boiler" -> {
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
                float inertia = 400f * r * r * r * r * th * ModularBake.param(type, part, "mass", 1f) * (float) m.density().muPerMb() / 7.2f;
                float yield = m.mechanical().yieldStrength().get().value();
                Part bands = design.part("B");
                Material bm = bands == null ? null : materials.get(bands.material());
                float rim = 44f * (float) Math.sqrt(yield / m.density().muPerMb()) / r * ModularBake.param(type, part, "rim", 1f);
                if (bm != null && bm.mechanical().yieldStrength().isPresent())
                    rim *= 1f + 0.25f * bm.mechanical().yieldStrength().get().value() / (bm.mechanical().yieldStrength().get().value() + yield);
                out.putFloat("Inertia", inertia);
                out.putFloat("RimRpm", rim);
                out.putFloat("Drag", 0.2f * r * (design.part("G") == null ? 1f : 0.7f));
            }
            default -> {
            }
        }
        return Optional.of(out);
    }
}
