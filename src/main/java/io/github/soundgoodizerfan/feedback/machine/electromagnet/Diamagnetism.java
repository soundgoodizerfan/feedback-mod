// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.electromagnet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * How hard a field pushes a thing away: the material's {@code separation.diamagnetism}, read for a shaped part by its
 * {@link MadeOf} and for any other item by the material the item is a form of. The one reader the electromagnet's
 * repulsion, the levitation plate and the levitation sleeve share, so none of them knows which material is which.
 */
public final class Diamagnetism {

    // Every item whose material is diamagnetic, rebuilt when the material table reloads.
    private static int generation = -1;
    private static Map<Item, Float> byItem = Map.of();

    private Diamagnetism() {
    }

    /** 0 to 1: the push on this stack, 0 for a thing that is not diamagnetic. */
    public static float of(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null)
            return of(MaterialTable.get().materials().get(made.material()));
        return of(stack.getItem());
    }

    public static float of(Material material) {
        return material == null ? 0f : material.separation().diamagnetism();
    }

    /** The push on a material's loose items; shaped parts are read by {@link #of(ItemStack)} through their {@code MadeOf}. */
    public static float of(Item item) {
        if (generation != MaterialTable.generation()) {
            generation = MaterialTable.generation();
            Map<Item, Float> found = new HashMap<>();
            for (Map.Entry<ResourceLocation, Material> entry : MaterialTable.get().materials().entrySet()) {
                float d = entry.getValue().separation().diamagnetism();
                if (d > 0f)
                    // Tailings are the gangue left over: the same material, but not what the field takes (as for the pull).
                    entry.getValue().items(entry.getKey()).entrySet().stream().filter(e -> !e.getKey().equals("tailings")).map(Map.Entry::getValue)
                            .filter(i -> !(i instanceof ModularPartItem)).forEach(i -> found.put(i, d));
            }
            byItem = found;
        }
        return byItem.getOrDefault(item, 0f);
    }

    /** The material an item is made of, if any (for a readout). */
    public static Optional<Material> material(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null)
            return Optional.ofNullable(MaterialTable.get().materials().get(made.material()));
        return Material.find(MaterialTable.get().materials(), stack).map(Material.Located::material);
    }
}
