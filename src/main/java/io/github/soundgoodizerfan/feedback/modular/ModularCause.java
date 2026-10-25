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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;

/**
 * Which parts a figure comes from, for the tooltip's "why" (philosophy 8: a figure shown with its
 * cause). It mirrors {@link ModularBake}, which stays the one place the arithmetic lives: where the
 * bake reads the wearing edge, this names it; where the bake sums every part's mass, this names the
 * heaviest. Silent Gear's tooltip shows a stat with its reason the same way (idea only, spec 9).
 * Nothing here reads an item id: a part is found by its role, a weakest or heaviest by the
 * material's own figures.
 */
public final class ModularCause {

    private ModularCause() {
    }

    /** The parts {@code fig} is down to, in the design's order; empty if it is not down to any one. */
    public static List<Part> of(Fig fig, ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        List<Part> out = new ArrayList<>();
        boolean bow = type.family() == ModularType.Family.BOW || type.family() == ModularType.Family.CROSSBOW;
        for (Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            if (role == null)
                continue;
            boolean wearing = p.role().equals(type.wearRole()) || role.set();
            boolean named = switch (fig) {
                case HARDNESS, EDGE_TOUGHNESS, BITE, DURABILITY, ARMOUR_TOUGHNESS, CAPACITY -> wearing || (fig == Fig.DURABILITY && role.striker());
                case HARDNESS_OFF, EDGE_TOUGHNESS_OFF, DIG_SPEED_OFF -> role.striker() && !p.role().equals(type.wearRole());
                case ARMOUR -> wearing || role.name().equals("lining");
                case KNOCKBACK_RESISTANCE, CORROSION -> p.role().equals(type.wearRole());
                case ENCHANTABILITY -> p.role().equals(type.primary());
                case REACH -> role.rod();
                case DAMAGE, DIG_SPEED -> wearing || p.equals(heaviest(design, type, materials));
                case MASS, SLOW, BALANCE, SWING, ATTACK_SPEED, IMPACT -> p.equals(heaviest(design, type, materials));
                // a gun's: the barrel's metal and bore set what the chamber holds and how the bullet leaves; the round and the
                // charge set the rest, and no part is down to them
                case CHAMBER_RATING, MUZZLE_SPEED, MUZZLE_ENERGY, SPREAD -> p.role().equals("B");
                case KICK -> p.role().equals("G") || p.role().equals("R");
                case CYCLE -> p.role().equals("A") || p.role().equals("K");
                case REPORT, HEAT_FACTOR, SHED_FACTOR -> "MNQVH".contains(p.role());
                case PEAK_PRESSURE, BULLET_MASS, IMPULSE, LAUNCHER, OVERPRESSURE -> false;
                // a charge's: the filler's blend sets the burn, the casing holds it in
                case BLAST -> p.role().equals(type.wearRole()) || "FG".contains(p.role());
                case BURNED, PACKED -> "FG".contains(p.role());
                // a head's: its one part
                case STRIKE_ST -> true;
                // a gauntlet's: the plate punches, the lining only pads
                case FIST -> p.role().equals(type.primary());
                // a dish's: the foods make the meal and carry its hazards, the container sets how much it holds
                case HUNGER, SATURATION, PATHOGEN, TOXIN, CHARRED, FAT, FIBRE -> !p.role().equals(type.primary()) || p.kind().isEmpty();
                case LITRES -> p.role().equals(type.primary());
                case JOINT, DRAW, VELOCITY -> bow
                        ? role.name().equals("string") || role.name().equals("limbs") || role.name().equals("prod")
                        : role.name().equals("binding") || p.equals(weakest(design, type, materials));
            };
            if (named)
                out.add(p);
        }
        return out;
    }

    /** The part with the most mass: what sets how the piece swings. */
    private static Part heaviest(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part best = null;
        float most = -1;
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            ModularType.Role role = type.roles().get(p.role());
            if (m == null || role == null)
                continue;
            float mass = ModularBake.mass(role, p, m);
            if (mass > most) {
                most = mass;
                best = p;
            }
        }
        return best;
    }

    /** Of the handle and the wearing part, the one with the least strength: what the joint gives at. */
    private static Part weakest(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part best = null;
        float least = Float.MAX_VALUE;
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            ModularType.Role role = type.roles().get(p.role());
            if (m == null || role == null || !(role.rod() || p.role().equals(type.wearRole())))
                continue;
            float strength = ModularBake.strength(m);
            if (strength < least) {
                least = strength;
                best = p;
            }
        }
        return best;
    }
}
