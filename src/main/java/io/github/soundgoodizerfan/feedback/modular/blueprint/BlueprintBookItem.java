// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.blueprint;

import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;

/**
 * A book of plan sheets that works like a bundle: click a sheet onto it to put it in, click it on an empty slot to take one out. It takes
 * only plan sheets. A bench reads the sheets in a book as it reads a sheet in its plan slot ({@link ModularBlueprints}); the book carries nothing
 * a sheet does not.
 */
public class BlueprintBookItem extends BundleItem {

    public BlueprintBookItem(Properties properties) {
        super(properties);
    }

    private static boolean refuses(ClickAction action, ItemStack other) {
        return action == ClickAction.SECONDARY && !other.isEmpty() && !(other.getItem() instanceof ModularBlueprintItem);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack book, Slot slot, ClickAction action, Player player) {
        return !refuses(action, slot.getItem()) && super.overrideStackedOnOther(book, slot, action, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack book, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        return !refuses(action, other) && super.overrideOtherStackedOnMe(book, other, slot, action, player, access);
    }
}
