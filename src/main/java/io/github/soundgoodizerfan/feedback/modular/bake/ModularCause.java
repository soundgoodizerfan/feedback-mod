// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.modular.item.ModularGlove;
import io.github.soundgoodizerfan.feedback.modular.item.ModularRespirator;
import io.github.soundgoodizerfan.feedback.modular.item.ModularTelescope;
import io.github.soundgoodizerfan.feedback.modular.part.ModularBackpack;
import io.github.soundgoodizerfan.feedback.modular.part.ModularBed;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;

import net.minecraft.resources.ResourceLocation;

/**
 * Which parts a figure comes from, for the tooltip's "why" (philosophy 10: a figure shown with its
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
        boolean air = type.family() == ModularType.Family.AIR_GUN || type.family() == ModularType.Family.ELECTRIC_GUN;
        for (Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            if (role == null)
                continue;
            boolean wearing = p.role().equals(type.wearRole()) || role.set();
            boolean named = switch (fig) {
                case HARDNESS, EDGE_TOUGHNESS, BITE, DURABILITY, ARMOUR_TOUGHNESS, CAPACITY -> wearing || (fig == Fig.DURABILITY && role.striker())
                        // fittings whose own metal moves it: a pauldron, a poleyn, a shield's rim, strap and boss, a pack's liner, bracket and bay
                        || (fig == Fig.ARMOUR_TOUGHNESS && role.name().equals("pauldron"))
                        || (fig == Fig.DURABILITY && "poleyn boss hard_liner lamp_bracket battery_fitting".contains(role.name()))
                        || (fig == Fig.CAPACITY && (role.name().equals("rim") || role.name().equals("strap")));
                case HARDNESS_OFF, EDGE_TOUGHNESS_OFF, DIG_SPEED_OFF -> p.role().equals("J");
                case HARDNESS_THIRD, EDGE_TOUGHNESS_THIRD, DIG_SPEED_THIRD -> p.role().equals("K");
                case DURABILITY_B -> p.role().equals("J");
                case DURABILITY_C -> p.role().equals("K");
                case ARMOUR -> type.family() == ModularType.Family.GLOVE ? p.role().equals(ModularGlove.PLATE) || role.name().equals("vambrace") || role.name().equals("lining")
                        : wearing || role.name().equals("underlayer") || role.name().equals("battery_bay");
                case PIERCE_RESIST -> p.role().equals(type.wearRole()) || role.name().equals("collar");
                case FIRE_CONDUCT -> p.role().equals(type.wearRole()) || role.name().equals("fire_patch");
                case KNOCKBACK_RESISTANCE -> p.role().equals(type.wearRole()) || role.name().equals("spurs") || role.name().equals("hobnails");
                case CORROSION -> p.role().equals(type.wearRole());
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
                case FITTING_WEAR, FITTING_LIMIT -> "MNQVHZ".contains(p.role());
                case BRACE_KICK, BRACE_SPREAD, FOLDED_BRACE_KICK, FOLDED_BRACE_SPREAD -> p.role().equals("i") || p.role().equals("I");
                case PEAK_PRESSURE, BULLET_MASS, IMPULSE, OVERPRESSURE -> false;
                // a charge's: the filler's blend sets the burn, the casing holds it in
                case BLAST -> p.role().equals(type.wearRole()) || "FG".contains(p.role());
                case BURNED, PACKED -> "FG".contains(p.role());
                // a head's: its one part
                case STRIKE_ST -> true;
                // a glove's: the metacarpal plate punches, guards and weighs; the body and lining only pad
                // a backpack's: the strap carries it, the shell holds it
                case CARRY -> ModularBackpack.CARRY_ROLES.contains(p.role()) || p.role().equals(ModularBackpack.PAD) || p.role().equals(ModularBackpack.BUCKLE);
                case SLOTS, POINTS -> p.role().equals(ModularBackpack.SHELL) || p.role().equals(ModularBackpack.FRAME);
                case STORES -> ModularBackpack.STORE_ROLES.contains(p.role());
                case SUIT_DRIVE, SUIT_LOSS -> role.name().equals("servo");
                case SUIT_GAUGE -> role.name().equals("gauge");
                case SUIT_SENSE -> role.name().equals("sensor_head");
                case SUIT_BAY -> role.name().equals("battery_bay");
                case SUIT_ATT_1, SUIT_ATT_2, SUIT_ATT_3, SUIT_ATT_4, SUIT_POW_1, SUIT_POW_2, SUIT_POW_3, SUIT_POW_4,
                        SUIT_ARG_1, SUIT_ARG_2, SUIT_ARG_3, SUIT_ARG_4, SUIT_SUP_1, SUIT_SUP_2, SUIT_SUP_3, SUIT_SUP_4 ->
                        role.name().equals("upgrade") || role.name().startsWith("module_");
                case SUIT_WIRING -> role.name().equals("wiring") || role.name().equals("upgrade");
                case CLIP, CLIP_FINE -> p.role().equals("B");
                case COMFORT, WARMTH -> p.role().equals(ModularBed.MATTRESS) || p.role().equals(ModularBed.FRAME);
                case FIST, SWING_DRAG -> p.role().equals(ModularGlove.PLATE);
                case BREATH_FLOOR, FILTER_LIFE, BREATH_DRAG -> p.role().equals(ModularRespirator.FILTER);
                case AIR_CAPACITY -> p.role().equals(ModularRespirator.CYLINDER);
                case MAGNIFICATION, TELESCOPE_BLUR -> !p.role().equals(ModularTelescope.TUBE);
                // a dish's: the foods make the meal and carry its hazards, the container sets how much it holds
                case HUNGER, SATURATION, PATHOGEN, TOXIN, CHARRED, FAT, FIBRE -> !p.role().equals(type.primary()) || p.kind().isEmpty();
                case LITRES -> p.role().equals(type.primary());
                // a dish's profile is read live off the whole dish, no one part is down to it
                case DISH_COOK, DISH_MOISTURE, DISH_SALT, DISH_SMOKE, DISH_FRESH, DISH_WARMTH, SHARE_MEAT, SHARE_SEAFOOD, SHARE_PLANT, SHARE_STARCH,
                        CUISINE_ITALIAN, CUISINE_EAST_ASIAN, CUISINE_MEXICAN, CUISINE_EASTERN_EUROPEAN -> false;
                // a spring gun's: the spring stores it, the barrel and bore hand it on, the receiver is what the spring is rated against
                case SPRING_ENERGY -> role.name().equals(LauncherSpec.SPRING);
                case AIR_EFFICIENCY -> role.name().equals(LauncherSpec.BARREL);
                case SHOT_ENERGY -> role.name().equals(ElectricGunSpec.SEAT);
                // a caster's: the tank's walls hold the pressure and the room, the nozzle's bore (and a pump's head) set the stream
                case CASTER_CAPACITY, CASTER_RATING -> role.name().equals(CasterSpec.TANK);
                case CASTER_RESIST -> role.name().equals(CasterSpec.TANK) || role.name().equals(CasterSpec.VALVE) || role.name().equals(CasterSpec.NOZZLE);
                case CASTER_SERVICE -> role.name().equals(CasterSpec.TANK);
                case CASTER_PUMP_LOAD, CASTER_DRIVE -> role.name().equals(CasterSpec.DRIVE) || role.name().equals(CasterSpec.TANK);
                case CASTER_HEAD, CASTER_REACH, CASTER_FLOW -> role.name().equals(CasterSpec.NOZZLE) || role.name().equals(CasterSpec.DRIVE);
                case JOINT, DRAW, VELOCITY -> air
                        ? (fig == Fig.JOINT ? role.name().equals(LauncherSpec.RECEIVER) || role.name().equals(LauncherSpec.SPRING)
                        : fig == Fig.DRAW ? role.name().equals(LauncherSpec.SPRING)
                        : !role.name().equals(LauncherSpec.RECEIVER))
                        : bow
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
