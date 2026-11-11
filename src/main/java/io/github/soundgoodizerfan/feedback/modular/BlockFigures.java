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
                case "water_wheel/R" -> material.density().muPerMb() > 0;
                case "thermal_store/I" -> true;
                case "thermal_store/F" -> material.density().muPerMb() > 0;
                case "leyden_jar/F" -> material.ampacity().isPresent();
                case "leyden_jar/W" -> material.electrical().insulation().isPresent();
                default -> true;
            };
        return switch (idOf(type)) {
            case "heat_exchanger" -> material.thermal().conductivity().isPresent();
            case "gas_tank" -> material.kind().orElse(null) == Material.Kind.METAL && material.density().muPerMb() > 0
                    && material.mechanical().yieldStrength().isPresent();
            case "water_wheel" -> material.density().muPerMb() > 0;
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
                float area = role.dim(part, "area") / ModularTuning.EXCHANGER_REF_AREA;
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
                float d = role.dim(part, "diameter") / ModularTuning.WHEEL_REF_CM;
                float buckets = role.dim(part, "buckets") / ModularTuning.WHEEL_REF_BUCKETS;
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
                float wall = role.dim(part, "wall") / ModularTuning.STORE_REF_CM;
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
                float area = role.dim(part, "area") / ModularTuning.JAR_REF_AREA * ModularBake.param(type, part, "area", 1f);
                float thin = ModularTuning.JAR_REF_WALL_MM / role.dim(part, "wall");
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
            default -> {
            }
        }
        return Optional.of(out);
    }
}
