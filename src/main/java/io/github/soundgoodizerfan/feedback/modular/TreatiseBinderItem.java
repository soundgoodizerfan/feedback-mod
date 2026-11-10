// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;

/** A binder of treatises that works like a bundle and takes only treatises. The Blueprint Table counts the treatises inside as held. */
public class TreatiseBinderItem extends BundleItem {

    public TreatiseBinderItem(Properties properties) {
        super(properties);
    }

    private static boolean refuses(ClickAction action, ItemStack other) {
        return action == ClickAction.SECONDARY && !other.isEmpty() && !(other.getItem() instanceof TreatiseItem);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack binder, Slot slot, ClickAction action, Player player) {
        return !refuses(action, slot.getItem()) && super.overrideStackedOnOther(binder, slot, action, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack binder, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        return !refuses(action, other) && super.overrideOtherStackedOnMe(binder, other, slot, action, player, access);
    }
}
