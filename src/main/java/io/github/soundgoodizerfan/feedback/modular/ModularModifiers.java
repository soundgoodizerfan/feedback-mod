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
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Everything that makes this item behave or read differently from the plain one, whatever its
 * source, as one list (spec 5.4): the <b>adjectives</b> the figures earn (head-heavy, keen,
 * brittle), the <b>trait</b> the main part's variant or the type names (the katana's dash,
 * the spear's charge) and the <b>shape</b> that part was made in (claymore, rapier). The tooltip,
 * its Shift glossary, the blueprint paper and the Tool Table's part hover all read this list, so a
 * new modifier is one entry here and a lang key, not a change in four places.
 *
 * <p>Names are {@code modular.feedback.modifier.<kind>.<id>}; the gloss of an adjective is
 * {@code feedback.gloss.modular.<id>} (what figure decides it and where the line is), of a trait
 * {@code modular.feedback.desc.trait.<id>} with the numbers its data gives, of a shape
 * {@code modular.feedback.desc.variant.<name>}. Common code, no client class.
 */
public final class ModularModifiers {

    private ModularModifiers() {
    }

    public enum Kind {
        ADJECTIVE("adjective", ChatFormatting.GRAY, ChatFormatting.DARK_GRAY),
        TRAIT("trait", ChatFormatting.GOLD, ChatFormatting.GRAY),
        SHAPE("shape", ChatFormatting.YELLOW, ChatFormatting.GRAY);

        final String key;
        final ChatFormatting name, text;

        Kind(String key, ChatFormatting name, ChatFormatting text) {
            this.key = key;
            this.name = name;
            this.text = text;
        }
    }

    /** @param fig the figure an adjective is judged by, so a reader can say which part it is down to; null otherwise. */
    public record Modifier(Kind kind, String id, Component name, Component gloss, @Nullable Fig fig) {

        /** The name in its kind's colour. */
        public MutableComponent coloured() {
            return name.copy().withStyle(kind.name);
        }

        /** {@code Flurry: every hit on the same target...}, the Shift line. */
        public MutableComponent line() {
            return coloured().append(Component.literal(": ").withStyle(kind.text))
                    .append(gloss.copy().withStyle(kind.text));
        }
    }

    /** Every modifier of {@code stack}; empty when it is not a modular item. */
    public static List<Modifier> of(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        ModularFigures f = ModularStacks.figures(stack);
        return design == null || f == null ? List.of() : of(design, ModularTypes.known().get(design.type()), f);
    }

    /** Adjectives, then the trait; the shape stays off an item (see below). {@code type} may be null (unknown to this pack). */
    public static List<Modifier> of(ModularDesign design, @Nullable ModularType type, ModularFigures f) {
        List<Modifier> out = new ArrayList<>(adjectives(design, type, f));
        if (type == null)
            return out;
        // A gun has a trait for every mechanism its parts show (spec 8.4), not one signature: all of them, each once.
        if (type.family() == ModularType.Family.GUN) {
            for (ModularTraits.Active a : ModularTraits.allOf(design, type))
                trait(a.id(), a.params().values()).ifPresent(out::add);
            return out;
        }
        Optional<ModularType.Variant> variant = ModularTraits.traitVariant(design, type);
        if (variant.isEmpty()) {
            ModularDesign.Part main = design.part(type.primary());
            ModularType.Role role = type.roles().get(type.primary());
            variant = main == null || role == null ? Optional.empty() : role.variant(main);
        }
        if (variant.isPresent())
            // The shape is the part's name already (the item's name and its parts line), so a finished
            // item lists only the trait; the shape's description is read where the part is chosen.
            forVariant(type, variant.get()).stream().filter(m -> m.kind() != Kind.SHAPE).forEach(out::add);
        else
            trait(type.trait(), type.traitParams()).ifPresent(out::add);
        return out;
    }

    /** The trait a part made in {@code v} gives (its own, else {@code type}'s), then the shape's name. */
    public static List<Modifier> forVariant(ModularType type, ModularType.Variant v) {
        List<Modifier> out = new ArrayList<>();
        if (v.trait().isEmpty())
            trait(type.trait(), type.traitParams()).ifPresent(out::add);
        else
            for (String id : v.trait().split(","))
                trait(id.trim(), v.params()).ifPresent(out::add);
        if (!v.name().isEmpty())
            out.add(new Modifier(Kind.SHAPE, v.name(), name(Kind.SHAPE, v.name()),
                    Component.translatable("modular.feedback.desc.variant." + v.name()), null));
        return out;
    }

    /**
     * What a Tool Table option gives: for a standard part, the trait of the first type that uses
     * the part (the spear head shows the charge); for a variant, as {@link #forVariant}.
     */
    public static List<Modifier> forPart(ToolTableShaping.Option o) {
        ModularType type = ModularTypes.known().get(o.type());
        if (type == null)
            return List.of();
        if (o.variant().name().isEmpty() && !o.shaping().base().isEmpty())
            return o.shaping().variant(o.shaping().base()).map(v -> forVariant(type, v)).orElse(List.of());
        if (!o.variant().name().isEmpty() || !type.trait().isEmpty())
            return forVariant(type, o.variant());
        for (ModularType t : ModularTypes.known().values())
            if (!t.trait().isEmpty() && t.roles().values().stream().anyMatch(r -> r.part().filter(s -> s.item().equals(o.shaping().item())).isPresent()))
                return forVariant(t, o.variant());
        return List.of();
    }

    /** The compact tooltip line: the names, comma separated, each in its kind's colour; null when none. */
    public static @Nullable MutableComponent line(List<Modifier> mods) {
        if (mods.isEmpty())
            return null;
        MutableComponent line = Component.empty();
        for (Modifier m : mods) {
            if (!line.getSiblings().isEmpty())
                line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            line.append(m.coloured());
        }
        return line;
    }

    private static Component name(Kind kind, String id) {
        return Component.translatable("modular.feedback.modifier." + kind.key + "." + id);
    }

    // --- adjectives -----------------------------------------------------------------------------

    private static List<Modifier> adjectives(ModularDesign design, @Nullable ModularType type, ModularFigures f) {
        List<Modifier> out = new ArrayList<>();
        if (type != null && isMelee(type)) {
            if (f.get(Fig.BALANCE) > ModularTooltip.HEAD_HEAVY)
                out.add(adjective("head_heavy", Fig.BALANCE, n(ModularTooltip.HEAD_HEAVY)));
            else if (f.get(Fig.BALANCE) < ModularTooltip.BALANCED_BELOW)
                out.add(adjective("balanced", Fig.BALANCE, n(ModularTooltip.BALANCED_BELOW)));
            if (f.get(Fig.SWING) < ModularTooltip.HEAVY_SWING)
                out.add(adjective("heavy", Fig.SWING, n(ModularTooltip.HEAVY_SWING)));
            else if (f.get(Fig.SWING) > ModularTooltip.LIGHT_SWING)
                out.add(adjective("light", Fig.SWING, n(ModularTooltip.LIGHT_SWING)));
            if (design.edge() == Edge.KEEN)
                out.add(adjective("keen", null, n(Edge.KEEN.bite()), n(Edge.KEEN.chip())));
            else if (design.edge() == Edge.BLUNT)
                out.add(adjective("blunt", null, n(Edge.BLUNT.bite()), n(Edge.BLUNT.chip())));
        }
        if (type != null && type.family() == ModularType.Family.GUN) {
            // A gun has no edge to be brittle or soft; what its figures can say is that its chamber is asked too much.
            if (f.get(Fig.OVERPRESSURE) > 1)
                out.add(adjective("overstressed", Fig.OVERPRESSURE));
            return out;
        }
        if (f.get(Fig.EDGE_TOUGHNESS) < ModularTooltip.BRITTLE_SHARE * f.get(Fig.HARDNESS))
            out.add(adjective("brittle", Fig.EDGE_TOUGHNESS, n(ModularTooltip.BRITTLE_SHARE)));
        else if (f.get(Fig.HARDNESS) < ModularTooltip.SOFT_HARDNESS)
            out.add(adjective("soft", Fig.HARDNESS, n(ModularTooltip.SOFT_HARDNESS)));
        if (f.get(Fig.JOINT) > 1)
            out.add(adjective("loose", Fig.JOINT));
        return out;
    }

    private static boolean isMelee(ModularType type) {
        return switch (type.family()) {
            case DIG, BLADE, MACE, SPEAR -> true;
            default -> false;
        };
    }

    private static Modifier adjective(String id, @Nullable Fig fig, Object... args) {
        return new Modifier(Kind.ADJECTIVE, id, name(Kind.ADJECTIVE, id),
                Component.translatable("feedback.gloss.modular." + id, args), fig);
    }

    // --- traits ---------------------------------------------------------------------------------

    /** A trait by id with the numbers its data gives; empty for none. Defaults are the trait classes' own. */
    private static Optional<Modifier> trait(String id, Map<String, Float> v) {
        if (id.isEmpty())
            return Optional.empty();
        Object[] args = switch (id) {
            case "flurry" -> new Object[]{n(p(v, "window", 25) / 20), n(p(v, "bonus", 0.12f) * 100), n(p(v, "max_stacks", 5))};
            case "dash" -> new Object[]{n(p(v, "distance", 6)), n(p(v, "dash_mult", 1.2f)), n(p(v, "width", 1)),
                    n(p(v, "cooldown_ticks", 80) / 20)};
            case "hack" -> new Object[]{n(p(v, "unarmoured_mult", 1.4f)), n(p(v, "armoured_mult", 0.8f)), n(p(v, "sweep", 2)),
                    n(p(v, "wear_per_block", 1f / 3f))};
            case "overhead" -> new Object[]{n(p(v, "radius", 2.5f)), n(p(v, "sweep_mult", 0.6f) * 100), n(p(v, "wear", 2)),
                    n(p(v, "weak_mult", 0.6f) * 100)};
            case "backstab" -> new Object[]{n(p(v, "back_mult", 2)), n(p(v, "back_angle", 120)), n(p(v, "sneak_mult", 1.3f)),
                    n(p(v, "face_mult", 0.9f))};
            case "flourish" -> new Object[]{n(p(v, "boost_ticks", 40) / 20), n(p(v, "crowd_mult", 1.2f)), n(p(v, "crowd", 2))};
            case "timber" -> new Object[]{n(p(v, "cap", 24)), n(p(v, "max_radius", 12)), n(p(v, "wear_per_log", 1))};
            case "throw" -> new Object[]{n(p(v, "min_charge_ticks", 10) / 20), n(p(v, "damage_per_block", 0.35f) * 100),
                    n(p(v, "max_mult", 2.5f)), n(p(v, "wear", 2))};
            case "hook" -> new Object[]{n(p(v, "disable_ticks", 160) / 20), n(p(v, "pull", 0.4f)), n(p(v, "no_shield_mult", 0.9f))};
            case "sounding" -> new Object[]{n(p(v, "range", 12)), n(p(v, "seconds", 6)), n(p(v, "cooldown_ticks", 200) / 20),
                    n(p(v, "wear", 4)), n(p(v, "max_found", 64))};
            case "vein" -> new Object[]{n(p(v, "cap", 8)), n(p(v, "wear_per_block", 1))};
            case "spear_charge" -> new Object[]{n(p(v, "min_range", 2)), n(p(v, "charge_reach", 4.5f)), n(p(v, "min_speed", 4.6f)),
                    n(p(v, "knockback_speed", 5.1f)),
                    n((p(v, "engaged", 40) + p(v, "tired", 30) + p(v, "disengaged", 30)) / 20), n(p(v, "wear_speed", 6))};
            case "slam" -> new Object[]{n(p(v, "radius", 3.5f)), n(p(v, "shove", 1f) * 100), n(p(v, "wear", 3))};
            case "pierce" -> new Object[]{n(p(v, "share", 0.5f) * 100), n(p(v, "dent_seconds", 4))};
            case "butcher" -> new Object[]{n(p(v, "animal_mult", 1.5f)), n(p(v, "extra_chance", 0.5f) * 100), n(p(v, "monster_mult", 0.85f))};
            case "line_cut" -> new Object[]{n(p(v, "length", 3)), n(p(v, "line_mult", 0.7f) * 100), n(p(v, "main_mult", 0.9f) * 100), n(p(v, "wear", 1))};
            case "lunge" -> new Object[]{n(p(v, "sprint_mult", 1.4f)), n(p(v, "still_mult", 0.85f))};
            case "harpoon" -> new Object[]{n(p(v, "rope", 14)), n(p(v, "pull", 0.5f) * 20), n(p(v, "water_mult", 1.8f))};
            case "fork" -> new Object[]{n(p(v, "extra", 2)), n(p(v, "reach", 3.5f)), n(p(v, "side_mult", 0.7f) * 100), n(p(v, "main_mult", 0.7f) * 100), n(p(v, "wear", 1))};
            case "snap_shot" -> new Object[]{n(p(v, "draw_mult", 0.6f) * 100), n(p(v, "velocity_mult", 0.8f) * 100)};
            case "heavy_draw" -> new Object[]{n(p(v, "draw_mult", 1.5f) * 100), n(p(v, "velocity_mult", 1.3f) * 100), n(p(v, "slow", 0.3f) * 100)};
            case "heavy_bolt" -> new Object[]{n(p(v, "draw_mult", 2.2f) * 100), n(p(v, "damage_mult", 1.6f) * 100), n(p(v, "pierce", 2)),
                    n(p(v, "slow", 0.25f) * 100)};
            case "snap_bolt" -> new Object[]{n(p(v, "draw_mult", 0.45f) * 100), n(p(v, "velocity_mult", 0.7f) * 100), n(p(v, "damage_mult", 0.8f) * 100)};
            case "recurve" -> new Object[]{n(p(v, "bonus", 0.25f) * 100), n(p(v, "pierce", 1)), n(p(v, "draw_mult", 1.15f) * 100)};
            case "stalker" -> new Object[]{n(p(v, "head_mult", 1.5f)), n(p(v, "velocity_mult", 0.85f) * 100), n(p(v, "draw_mult", 1.15f) * 100)};
            case "repeater" -> new Object[]{n(p(v, "bolts", 3)), n(p(v, "draw_mult", 1.3f) * 100), n(p(v, "damage_mult", 0.85f) * 100),
                    n(p(v, "spread_mult", 5))};
            case "blocking" -> new Object[]{n(p(v, "block_mult", 0.5f) * 100), n(p(v, "wear", 1))};
            case "parry" -> new Object[]{n(p(v, "parry_ticks", 6) / 20), n(p(v, "cone", 110)), n(p(v, "capacity_mult", 0.65f) * 100)};
            case "deflect" -> new Object[]{n(p(v, "return_speed", 0.8f) * 100), n(p(v, "melee_mult", 0.75f) * 100)};
            case "bulwark" -> new Object[]{n(p(v, "capacity_mult", 1.5f) * 100), n(p(v, "slow", 0.25f) * 100), n(p(v, "wear_mult", 1.5f) * 100),
                    n(p(v, "range", 2.5f)), n(p(v, "bash_damage", 2)), n(p(v, "bash_cooldown", 80) / 20)};
            case "scoop" -> new Object[]{n(p(v, "side", 1) * 2 + 1), n(p(v, "wear_per_block", 1))};
            case "trench" -> new Object[]{n(p(v, "depth", 1)), n(p(v, "wear_per_block", 2))};
            case "kama" -> new Object[]{n(p(v, "length", 2)), n(p(v, "cooldown_ticks", 20) / 20)};
            case "rake" -> new Object[]{n(p(v, "radius", 4)), n(p(v, "wear_per_stack", 1))};
            case "scythe" -> new Object[]{n(p(v, "per_wear", 3)), n(p(v, "mob_mult", 0.7f) * 100)};
            case "billhook" -> new Object[]{n(p(v, "bleed_damage", 1)), n(p(v, "bleed_seconds", 5)), n(p(v, "hit_mult", 0.8f) * 100)};
            case "reaping" -> new Object[]{n(p(v, "wear", 1))};
            case "open_face" -> new Object[]{n(p(v, "projectile_mult", 1.3f))};
            case "closed_visor" -> new Object[]{n(p(v, "projectile_mult", 0.6f)), n(p(v, "visibility_mult", 1.5f))};
            case "neck_guard" -> new Object[]{n(p(v, "behind_mult", 0.8f)), n(p(v, "back_angle", 120))};
            case "studded" -> new Object[]{n(p(v, "per_stud", 1)), n(p(v, "max", 3)), n(p(v, "wear", 1))};
            case "rigid" -> new Object[]{n(p(v, "absorb_mult", 0.7f))};
            case "mail" -> new Object[]{n(p(v, "slash_mult", 0.75f)), n(p(v, "blunt_mult", 1.3f))};
            case "kick" -> new Object[]{n(p(v, "strength", 1)), n(p(v, "exhaustion", 0.3f)), n(p(v, "wear", 1)),
                    n(p(v, "cooldown_ticks", 30) / 20)};
            case "braced" -> new Object[]{n(p(v, "knock_mult", 0.6f)), n(p(v, "swim_slow", 0.15f) * 100)};
            case "blowback" -> new Object[]{n(p(v, "pressure_limit", 1.2f) * FTuning.GUN_PROPELLANT_PEAK_PU), n(p(v, "wear_mult", 2f))};
            case "bolt_lockup" -> new Object[]{n((p(v, "rating_mult", 1.25f) - 1) * 100), n((1 - p(v, "spread_mult", 0.8f)) * 100)};
            case "gas_operated" -> new Object[]{n((1 - p(v, "cycle_mult", 0.9f)) * 100), n((p(v, "heat_mult", 1.15f) - 1) * 100), n(p(v, "mass", 0.15f))};
            case "gun_braced" -> new Object[]{n((1 - p(v, "kick_mult", 0.7f)) * 100), n((1 - p(v, "spread_mult", 0.6f)) * 100)};
            case "open_bolt" -> new Object[]{n(p(v, "idle_ms", 1500) / 1000), n(p(v, "first_shot_spread", 2f))};
            case "belt_fed" -> new Object[]{n(p(v, "reload_mult", 2f))};
            case "trample" -> new Object[]{n(p(v, "min_fall", 3)), n(p(v, "radius", 1.5f)), n(p(v, "per_block", 1)),
                    n(p(v, "max", 12)), n(p(v, "wear", 1))};
            case "spurs" -> new Object[]{n(p(v, "boost_ticks", 60) / 20), n(p(v, "level", 1)), n(p(v, "cooldown_ticks", 200) / 20)};
            default -> new Object[0];
        };
        return Optional.of(new Modifier(Kind.TRAIT, id, name(Kind.TRAIT, id),
                Component.translatable("modular.feedback.desc.trait." + id, args), null));
    }

    private static float p(Map<String, Float> values, String name, float fallback) {
        return values.getOrDefault(name, fallback);
    }

    private static String n(float value) {
        return Instruments.format(value, Instrument.TRUTH);
    }
}
