// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** What a core reads off the world: an item's or block's material, and from it neutron figures and wall density. Server side only. */
final class Neutrons {

    private Neutrons() {
    }

    private static Map<ResourceLocation, Material> builtFrom;
    private static final Map<Item, Optional<Material.Neutron>> NEUTRONS = new HashMap<>();
    private static final Map<Item, Float> DENSITIES = new HashMap<>();

    static Map<ResourceLocation, Material> materials() {
        return MaterialTable.get().materials();
    }

    /** The per-item caches are only good for one material table: a reload swaps the table and they start over. */
    private static synchronized void sync(Map<ResourceLocation, Material> materials) {
        if (builtFrom != materials) {
            NEUTRONS.clear();
            DENSITIES.clear();
            builtFrom = materials;
        }
    }

    static Optional<Material.Neutron> of(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : of(stack.getItem());
    }

    static synchronized Optional<Material.Neutron> of(Item item) {
        Map<ResourceLocation, Material> materials = materials();
        sync(materials);
        return NEUTRONS.computeIfAbsent(item, i -> Material.find(materials, i).flatMap(l -> l.material().neutron()));
    }

    static Optional<Material.Neutron> of(BlockState state) {
        Item item = state.getBlock().asItem();
        if (state.isAir() || item == net.minecraft.world.item.Items.AIR)
            return Optional.empty();
        return of(item);
    }

    /** What a block's material is worth as a wall: its density in mu per mB (0 for air or no material). */
    static synchronized float density(BlockState state) {
        Item item = state.getBlock().asItem();
        if (state.isAir() || item == net.minecraft.world.item.Items.AIR)
            return 0f;
        Map<ResourceLocation, Material> materials = materials();
        sync(materials);
        return DENSITIES.computeIfAbsent(item, i -> Material.find(materials, i).map(l -> l.material().density().muPerMb()).orElse(0f));
    }
}
