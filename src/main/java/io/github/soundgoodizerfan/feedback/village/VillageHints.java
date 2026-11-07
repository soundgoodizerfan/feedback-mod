// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.DishProfile;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The hint button, for a dish order only: how each stated axis moves and which foods push it. A tool order names its parts outright. */
public final class VillageHints {

    private VillageHints() {
    }

    /** Most foods named per term. */
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
        return out; // a tool order names its parts outright; nothing to hint
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

    private static String name(Fig fig) {
        return fig.name().toLowerCase(Locale.ROOT);
    }

    private static Component noun(Fig fig) {
        return Component.translatable("village.feedback.noun." + name(fig));
    }
}
