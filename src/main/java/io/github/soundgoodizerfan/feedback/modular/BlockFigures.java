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
    public static boolean suits(ModularType type, Material material) {
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
        if (m == null || !suits(type, m))
            return Optional.empty();
        ModularType.Role role = type.roles().get(ROLE);
        CompoundTag out = new CompoundTag();
        switch (design.type().getPath()) {
            // Tubes carry heat as well as their metal conducts it, in proportion to the area they present. Copper at the
            // default area is the old fixed exchanger's 0.1.
            case "heat_exchanger" -> {
                float area = role.dim(part, "area") / ModularTuning.EXCHANGER_REF_AREA;
                out.putFloat("Conductance", m.thermal().conductivity().get().workPerTickPerTu() * area * ModularBake.param(type, part, "fin", 1f));
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
            }
            // A bigger wheel catches more water (capacity) and turns slower for the same flow; a heavier rim has more
            // momentum. The buckets are the blade count: more of them take more of the flow's push.
            case "water_wheel" -> {
                float d = role.dim(part, "diameter") / ModularTuning.WHEEL_REF_CM;
                float buckets = role.dim(part, "buckets") / ModularTuning.WHEEL_REF_BUCKETS;
                out.putFloat("Power", ModularTuning.WHEEL_CAPACITY_SU * d * buckets);
                out.putFloat("RpmPerFlow", ModularTuning.WHEEL_RPM_PER_FLOW / d);
                out.putFloat("Inertia", ModularTuning.WHEEL_INERTIA * d * d * d * (float) m.density().muPerMb() / ModularTuning.WHEEL_REF_DENSITY);
            }
            // A thicker wall has more to heat (mass) and loses less (leak); the wall's own conductivity sets the loss.
            case "thermal_store" -> {
                float wall = role.dim(part, "wall") / ModularTuning.STORE_REF_CM;
                out.putFloat("Mass", ModularTuning.STORE_MASS * wall * (float) m.density().muPerMb() / ModularTuning.STORE_REF_DENSITY);
                out.putFloat("Leak", ModularTuning.STORE_LEAK / wall * m.thermal().conductivity().get().workPerTickPerTu() / ModularTuning.STORE_REF_CONDUCTIVITY);
            }
            // Charge held is plate area over glass thickness; thin glass leaks it away much faster (as the square).
            case "leyden_jar" -> {
                float area = role.dim(part, "area") / ModularTuning.JAR_REF_AREA;
                float thin = ModularTuning.JAR_REF_WALL_MM / role.dim(part, "wall");
                out.putFloat("Capacitance", ModularTuning.JAR_CAPACITANCE * area * thin);
                out.putFloat("Leak", ModularTuning.JAR_LEAK * thin * thin);
                out.putFloat("BurstAmps", ModularTuning.JAR_BURST_AMPS * area);
            }
            default -> {
            }
        }
        return Optional.of(out);
    }
}
