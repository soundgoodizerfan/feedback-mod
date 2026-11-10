// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Modular items held in modular items. A family that declares a {@link ModularType.Family#nestForm} (a suit module) makes finished
 * items that a role listing that form takes whole: the Tool Table seats the stack, {@link ModularDesign.Part#sub} keeps it, and the
 * holder's bake reads the held item's own baked figures, so a held item's own held items count in turn. Nothing is flattened: the held
 * item keeps its design, wear and attunements and comes out whole when the holder is taken apart.
 *
 * <p>A held item is found like any other stack, as a {@link Material.Located} in the nest form and the material of its wearing part,
 * which is how every slot, role and audit already reads a stack.
 */
public final class ModularNest {

    private ModularNest() {
    }

    /** The nest form of {@code stack}, or empty if it is not a finished item that can be held. */
    public static String form(ItemStack stack) {
        if (stack.isEmpty())
            return "";
        // a live circuit is a held component too, read through Circuit.of
        if (stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT.get())
                && io.github.soundgoodizerfan.feedback.process.Circuit.of(stack).live())
            return "circuit";
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return "";
        ModularType type = ModularTypes.known(design.type());
        return type == null ? "" : type.family().nestForm();
    }

    public static boolean holdable(ItemStack stack) {
        return !form(stack).isEmpty();
    }

    /** {@code stack} as a located thing: its nest form and its wearing part's material. */
    public static Optional<Material.Located> locate(Map<ResourceLocation, Material> materials, ItemStack stack) {
        String form = form(stack);
        if (form.isEmpty())
            return Optional.empty();
        if (form.equals("circuit"))
            return placeholder(materials).map(e -> new Material.Located(e.getKey(), e.getValue(), form));
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = ModularTypes.known(design.type());
        ModularDesign.Part wear = design.part(type.wearRole());
        Material m = wear == null ? null : materials.get(wear.material());
        return m == null ? Optional.empty() : Optional.of(new Material.Located(wear.material(), m, form));
    }

    /**
     * A circuit is made of no one material, but a located thing carries one: the table's first, by id. Nothing reads it; what a held
     * circuit is comes off the stack ({@code Circuit.of}).
     */
    private static Optional<Map.Entry<ResourceLocation, Material>> placeholder(Map<ResourceLocation, Material> materials) {
        return materials.entrySet().stream().min(Map.Entry.comparingByKey());
    }

    /** The baked figures of a held item, or null if it is empty or spent. */
    public static ModularFigures figures(ModularDesign.Part part) {
        return part.sub().isEmpty() || ModularStacks.broken(part.sub()) ? null : ModularStacks.figures(part.sub());
    }
}
