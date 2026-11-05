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
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.DishProfile;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The hint button: for each term the villager states, which material in which part of the type's plain design meets it. Worked out by
 * baking the starter design once per (part, material) and testing every term against the result, so it is the real bake and nothing
 * about the order is kept. It only says what one swap can do; a term no single swap reaches says so. A dish order gets how each axis moves,
 * since a dish is composed from food and not from one part.
 */
public final class VillageHints {

    private VillageHints() {
    }

    /** Most materials named per term. */
    private static final int MAX_NAMED = 4;

    public static List<Component> of(Order order) {
        List<Component> out = new ArrayList<>();
        ModularType type = ModularTypes.known().get(order.type());
        if (type == null)
            return out;
        if (type.family() == ModularType.Family.DISH) {
            for (Order.Term t : order.terms()) {
                out.add(Component.translatable("village.feedback.hint.line", noun(t.fig()), Component.translatable("village.feedback.hint.fig." + name(t.fig()))));
                List<Component> foods = foodsFor(t);
                if (!foods.isEmpty()) {
                    Component list = Component.empty();
                    for (int i = 0; i < foods.size(); i++)
                        list = Component.empty().append(list).append(i == 0 ? Component.empty() : Component.literal(", ")).append(foods.get(i));
                    out.add(Component.translatable("village.feedback.hint.eg", list));
                }
            }
            out.add(Component.translatable("village.feedback.hint.dish"));
            return out;
        }
        Map<ResourceLocation, ModularType> types = ModularTypes.known();
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        Optional<ModularDesign> starter = ToolDesigns.starter(order.type(), types, materials);
        if (starter.isEmpty())
            return out;
        // Every swap of one part to one material, baked once: role, material, figures.
        record Swap(String role, ResourceLocation material, ModularFigures figures) {
        }
        List<Swap> swaps = new ArrayList<>();
        for (Map.Entry<String, ModularType.Role> r : type.roles().entrySet()) {
            ModularDesign.Part current = starter.get().part(r.getKey());
            if (current == null)
                continue;
            var shape = ToolDesigns.standard(r.getValue(), types);
            for (Material.Located l : ToolDesigns.materials(type, r.getKey(), shape, materials)) {
                ModularDesign swapped = starter.get().withPart(ToolDesigns.part(r.getKey(), l, shape, current.count()));
                ToolDesigns.predict(swapped, types, materials).ifPresent(f -> swaps.add(new Swap(r.getKey(), l.id(), f)));
            }
        }
        for (Order.Term t : order.terms()) {
            List<Swap> fits = swaps.stream().filter(s -> meets(t, s.figures().get(t.fig()))).toList();
            if (fits.isEmpty()) {
                out.add(Component.translatable("village.feedback.hint.none", noun(t.fig())));
                continue;
            }
            // The part whose swap meets it most often is the part the figure comes from.
            String role = fits.stream().collect(java.util.stream.Collectors.groupingBy(Swap::role, java.util.stream.Collectors.counting()))
                    .entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
            List<Component> names = fits.stream().filter(s -> s.role().equals(role))
                    .sorted(Comparator.comparingDouble(s -> Math.abs(s.figures().get(t.fig()) - t.target())))
                    .limit(MAX_NAMED).map(s -> (Component) Component.translatable("material." + s.material().getNamespace() + "." + s.material().getPath())).toList();
            Component list = Component.empty();
            for (int i = 0; i < names.size(); i++)
                list = Component.empty().append(list).append(i == 0 ? Component.empty() : Component.literal(", ")).append(names.get(i));
            out.add(Component.translatable("village.feedback.hint.try", noun(t.fig()), type.roles().get(role).name(), list));
        }
        out.add(Component.translatable("village.feedback.hint.note"));
        return out;
    }

    /** Foods worth building the dish from for a term that wants a high figure (or a low fat, fibre or moisture): by kind, trait or the food's own figure. Empty when no food says it. */
    private static List<Component> foodsFor(Order.Term t) {
        boolean high = t.must() ? t.above() : t.frac() >= 0.5f;
        Fig f = t.fig();
        java.util.function.Predicate<Material> pick = switch (f) {
            case SHARE_MEAT -> m -> m.kind().orElse(null) == Material.Kind.ANIMAL;
            case SHARE_SEAFOOD -> m -> m.kind().orElse(null) == Material.Kind.SEAFOOD;
            case SHARE_PLANT -> m -> m.kind().orElse(null) == Material.Kind.VEGETABLE || m.kind().orElse(null) == Material.Kind.FRUIT || m.kind().orElse(null) == Material.Kind.FUNGUS;
            case SHARE_STARCH -> m -> m.kind().orElse(null) == Material.Kind.GRAIN || m.kind().orElse(null) == Material.Kind.DOUGH;
            case CUISINE_ITALIAN -> m -> m.traits().contains(Material.Trait.ITALIAN);
            case CUISINE_EAST_ASIAN -> m -> m.traits().contains(Material.Trait.EAST_ASIAN);
            case CUISINE_MEXICAN -> m -> m.traits().contains(Material.Trait.MEXICAN);
            case CUISINE_EASTERN_EUROPEAN -> m -> m.traits().contains(Material.Trait.EASTERN_EUROPEAN);
            case FAT, FIBRE, DISH_MOISTURE -> m -> true;
            default -> null;
        };
        if (pick == null)
            return List.of();
        boolean share = f.name().startsWith("SHARE_") || f.name().startsWith("CUISINE_");
        if (share && !high)
            return List.of();
        java.util.function.ToDoubleFunction<Material> key = m -> switch (f) {
            case FAT -> m.food().get().fat();
            case FIBRE -> m.food().get().fibre();
            case DISH_MOISTURE -> m.food().get().moisture();
            default -> 0d;
        };
        return MaterialTable.get().materials().entrySet().stream()
                .filter(e -> e.getValue().food().isPresent() && e.getValue().kind().orElse(null) != Material.Kind.DISH && pick.test(e.getValue()))
                .sorted(Comparator.<Map.Entry<ResourceLocation, Material>>comparingDouble(e -> high ? -key.applyAsDouble(e.getValue()) : key.applyAsDouble(e.getValue()))
                        .thenComparing(e -> e.getKey().toString()))
                .limit(MAX_NAMED)
                .map(e -> (Component) Component.translatable("material." + e.getKey().getNamespace() + "." + e.getKey().getPath())).toList();
    }

    /** Whether {@code v} would satisfy the term at full score: a must's side of its target, a band's inner half. */
    private static boolean meets(Order.Term t, float v) {
        if (t.must())
            return t.above() ? v >= t.target() : v <= t.target();
        return Math.abs(v - t.target()) / t.range() <= t.tol() * 0.5f;
    }

    private static String name(Fig fig) {
        return fig.name().toLowerCase(Locale.ROOT);
    }

    private static Component noun(Fig fig) {
        return Component.translatable("village.feedback.noun." + name(fig));
    }
}
