// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import java.util.IdentityHashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.worldgen.VanillaVesselSwap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The vanilla blocks Feedback replaces with its own ({@link VanillaVesselSwap}), read as "ours stands where theirs is asked for":
 * the vanilla item cannot be made, so whatever another mod asks of it (an ingredient, a held item, a placed block) takes ours.
 * Nothing here converts anything. Items a player holds that turn into modular ones are {@link ConvertedIngredients}'s.
 */
public final class StandIns {

    private StandIns() {
    }

    private static Map<Item, Item> items;

    /** Vanilla item to ours; empty until the registries are filled. */
    public static Map<Item, Item> items() {
        if (items != null)
            return items;
        if (!FModular.PISTON.isBound())
            return Map.of();
        Map<Item, Item> found = new IdentityHashMap<>();
        VanillaVesselSwap.swaps().forEach((theirs, ours) -> {
            Item from = theirs.asItem();
            Item to = ours.asItem();
            if (from != Items.AIR && to != Items.AIR && BuiltInRegistries.ITEM.getKey(from).getNamespace().equals("minecraft"))
                found.put(from, to);
        });
        items = found;
        return found;
    }

    /** Whether the placed {@code state} is ours standing for the vanilla {@code block}. */
    public static boolean standsFor(BlockState state, Block block) {
        return !items().isEmpty() && VanillaVesselSwap.swaps().get(block) == state.getBlock();
    }

    /** As {@link #standsFor(BlockState, Block)}, and every property the two blocks share agrees (a piston facing up). */
    public static boolean standsFor(BlockState state, BlockState wanted) {
        if (!standsFor(state, wanted.getBlock()))
            return false;
        for (Property<?> p : wanted.getProperties())
            if (state.hasProperty(p) && !state.getValue(p).equals(wanted.getValue(p)))
                return false;
        return true;
    }
}
