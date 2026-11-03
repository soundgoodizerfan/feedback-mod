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
