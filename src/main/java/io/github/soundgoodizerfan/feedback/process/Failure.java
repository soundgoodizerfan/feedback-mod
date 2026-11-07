// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a ruined batch leaves when its row names no {@code spoiled}: the failure form of the first
 * input's {@link Material.Kind} (heat-metal spec 4). A row's own {@code spoiled} still wins.
 */
public final class Failure {

    private Failure() {}

    /** The ruined item for a batch whose first input was {@code input}; empty for a kind with no solid failure. */
    public static ItemStack of(ItemStack input) {
        return Material.find(MaterialTable.get().materials(), input)
                .map(located -> new ItemStack(item(located.material().kind().orElse(Material.Kind.ORGANIC))))
                .orElseGet(() -> new ItemStack(FItems.CHARRED_REMAINS.get()));
    }

    /** Burnt metal, slag, cracked ceramic, porous sinter; everything alive or organic (foods, plants, polymers) chars. */
    public static Item item(Material.Kind kind) {
        return switch (kind) {
            case METAL, METALLOID -> FItems.SCRAP_METAL.get();
            case MINERAL, CRYSTAL, GLASS -> FItems.SILICATE_SLAG.get();
            case CERAMIC -> FItems.CRACKED_CERAMIC.get();
            case GRANULAR -> FItems.POROUS_SINTER.get();
            default -> FItems.CHARRED_REMAINS.get();
        };
    }

    /** The row's own ruin, else the kind's failure form of {@code firstInput}. */
    public static ItemStack ruined(ThermalProcess process, ItemStack firstInput) {
        return process.spoiled().isEmpty() ? of(firstInput) : process.spoiled().copy();
    }
}
