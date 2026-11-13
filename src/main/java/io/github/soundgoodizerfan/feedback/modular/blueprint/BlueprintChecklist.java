// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.bench.ModularAssembly;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A blueprint read as a recipe card: one row per part to make, what it is (material, role or shape, number, axis values), and whether
 * something now held would be that part. A blueprint never blocks a missing material; the card only lists it. Reads the design the stack
 * already carries, so it asks nothing of the server.
 */
public final class BlueprintChecklist {

    private BlueprintChecklist() {
    }

    /** One part of the drawing and whether {@code held} has it. */
    public record Row(Part want, Component text, boolean held) {}

    /** The rows of {@code drawn}; {@code held} are the stacks the bench holds (empty for a card with nothing to tick). */
    public static List<Row> rows(ModularDesign drawn, List<ItemStack> held, Map<ResourceLocation, Material> materials,
                                 Map<ResourceLocation, ModularType> types) {
        ModularType type = types.get(drawn.type());
        List<Row> out = new ArrayList<>();
        if (type == null)
            return out;
        for (Part want : drawn.parts()) {
            if (!want.sub().isEmpty() || type.roles().get(want.role()) == null)
                continue;
            out.add(new Row(want, describe(want, type), has(want, held, materials)));
        }
        return out;
    }

    /** Whether some stack in {@code held} is the part {@code want}: the same predicate a blueprint's credit is matched by. */
    private static boolean has(Part want, List<ItemStack> held, Map<ResourceLocation, Material> materials) {
        for (ItemStack s : held) {
            if (s.isEmpty())
                continue;
            Optional<Material.Located> located = Material.find(materials, s);
            if (located.isEmpty())
                continue;
            Part p = ModularAssembly.part(want.role(), located.get(), s, s.getCount());
            if (p.sub().isEmpty() && p.material().equals(want.material()) && p.kind().equals(want.kind()) && p.count() == want.count())
                return true;
        }
        return false;
    }

    /** "Iron head x2, Width 0.80": the material and role (or shape), the number, and each axis value the drawing chose. */
    public static MutableComponent describe(Part p, ModularType type) {
        MutableComponent out = ModularTooltip.parts(List.of(p), type);
        if (p.count() > 1)
            out.append(" x" + p.count());
        p.measures().axes().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> out.append(", ")
                .append(Component.translatable("modular.feedback.axis." + e.getKey()))
                .append(" " + String.format(Locale.ROOT, "%.2f", e.getValue())));
        return out;
    }
}
