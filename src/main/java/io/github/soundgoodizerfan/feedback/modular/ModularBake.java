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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Material;

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
 * so there is no as-built geometry to read ({@code feedback_modular_items_spec.md} 4); and heat
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

    public static float bite(Material m, Edge edge) {
        float b = ModularTuning.BITE_BASE + ModularTuning.BITE_SLOPE * (float) Math.log(1 + competence(m));
        return Math.min(ModularTuning.BITE_MAX, Math.max(ModularTuning.BITE_MIN, b)) * edge.bite();
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
        Map<String, Material> by = new java.util.HashMap<>();
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m == null || !type.roles().containsKey(p.role()))
                return Optional.empty();
            by.put(p.role(), m);
        }
        Part wearPart = design.part(type.wearRole());
        if (wearPart == null)
            return Optional.empty();
        Map<Fig, Float> f = new EnumMap<>(Fig.class);
        float worn = ModularTuning.WEAR_FACTOR[Math.min(step, ModularTuning.WEAR_FACTOR.length - 1)];
        Material edge = by.get(type.wearRole());
        float comp = competence(edge);
        f.put(Fig.DURABILITY, (float) Math.max(1, Math.round(ModularTuning.DURABILITY_K * comp * type.wearLitres())));
        f.put(Fig.ENCHANTABILITY, (float) enchantability(by.get(type.primary()) != null ? by.get(type.primary()) : edge));
        f.put(Fig.HARDNESS, edge.hardness().value());
        f.put(Fig.EDGE_TOUGHNESS, edge.mechanical().toughness().map(t -> t.value()).orElse(strength(edge)));
        switch (type.family()) {
            case DIG, BLADE, MACE, SPEAR, SHEARS -> melee(design, type, by, edge, f, worn);
            case SHIELD -> {
                float total = 0;
                for (Part p : design.parts())
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                f.put(Fig.MASS, total);
                f.put(Fig.CAPACITY, ModularTuning.SHIELD_BASE_CAPACITY + comp * worn);
            }
            default -> { }
        }
        return Optional.of(ModularFigures.of(f, step));
    }

    private static float mass(ModularType.Role role, Part part, Material m) {
        return role.litres() * part.count() * m.density().muPerMb() * role.factor(part.form());
    }

    /** Mass, balance, swing, blow, bite, damage, dig speed, reach and joint for a hand-swung head on a handle. */
    private static void melee(ModularDesign design, ModularType type, Map<String, Material> by, Material edge,
                              Map<Fig, Float> f, float worn) {
        float rodLength = 0;
        for (Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            rodLength += r.length() * p.count();
        }
        float total = 0;
        float inertia = 0;
        float moment = 0;
        float struck = 0;
        float rodMass = 0;
        float reachLength = rodLength;
        for (Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            float m = mass(r, p, by.get(p.role()));
            total += m;
            if (r.rod()) {
                inertia += m * rodLength * rodLength / 3;
                moment += m * rodLength / 2;
                rodMass += m;
                continue;
            }
            float d = r.at() + (r.rides() ? rodLength : 0);
            inertia += m * d * d;
            moment += m * d;
            reachLength = Math.max(reachLength, d);
            if (p.role().equals(type.wearRole()))
                struck += m;
        }
        if (struck == 0)
            struck = rodMass;
        float swing = clamp(ModularTuning.SWING_K / (float) Math.pow(inertia + ModularTuning.BALANCE_K * moment,
                ModularTuning.SWING_EXPONENT), ModularTuning.SWING_MIN, ModularTuning.SWING_MAX);
        float tip = reachLength * swing * (float) Math.PI;
        float impact = 0.5f * (struck + ModularTuning.ROD_STRIKE_SHARE * rodMass) * tip * tip;
        float bite = bite(edge, design.edge());
        f.put(Fig.MASS, total);
        f.put(Fig.BALANCE, total > 0 && reachLength > 0 ? Math.min(1f, moment / (total * reachLength)) : 0f);
        f.put(Fig.SWING, swing);
        f.put(Fig.IMPACT, impact);
        f.put(Fig.BITE, bite);
        f.put(Fig.ATTACK_SPEED, swing * worn);
        f.put(Fig.DAMAGE, 1 + ModularTuning.DAMAGE_K * type.strike() * bite * (float) Math.sqrt(impact) * worn);
        f.put(Fig.DIG_SPEED, ModularTuning.DIG_K * bite * (float) Math.sqrt(impact) * worn);
        f.put(Fig.REACH, Math.max(0, reachLength - ModularTuning.REACH_REFERENCE) * ModularTuning.REACH_PER_METRE);
        f.put(Fig.JOINT, joint(design, type, by, struck, reachLength, rodLength));
    }

    /** Leverage on the joint over what the handle, head and any binding carry together. */
    private static float joint(ModularDesign design, ModularType type, Map<String, Material> by, float struck,
                               float reachLength, float rodLength) {
        if (rodLength == 0)
            return 0;
        float weakest = Float.MAX_VALUE;
        float binding = 0;
        for (Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            Material m = by.get(p.role());
            if (r.name().equals("binding"))
                binding = (m.kind().filter(k -> k == Material.Kind.METAL).isPresent()
                        ? ModularTuning.PINNED_SHARE : ModularTuning.WRAPPED_SHARE) * strength(m);
            else if (r.rod() || p.role().equals(type.wearRole()))
                weakest = Math.min(weakest, strength(m));
        }
        float capacity = ModularTuning.JOINT_K * (weakest + binding);
        return capacity > 0 ? struck * reachLength / capacity : 0;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.min(hi, Math.max(lo, v));
    }
}
