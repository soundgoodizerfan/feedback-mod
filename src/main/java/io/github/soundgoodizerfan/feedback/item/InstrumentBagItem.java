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
package io.github.soundgoodizerfan.feedback.item;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * A bag that holds instruments, and reads for the player what they would read loose in the pack.
 *
 * <h2>Why this is not a new kind of instrument</h2>
 * Philosophy 8 already says reading is passive: <em>what you can perceive is a property of your
 * kit</em>. The bag changes where the kit is kept, not what it can do, so the bag is no
 * {@link Instrument} and has no figures of its own; {@code Instruments.best} looks inside it as it
 * looks along the hotbar. It exists because a pack that owns eight instruments has eight slots
 * spent on things nobody holds.
 *
 * <h2>Handling</h2>
 * The bundle's gesture, since players already have it in their hands: right-click an instrument
 * onto the bag (or the bag onto it) to stow it, right-click the bag with nothing to take the last
 * one out. Only instruments go in, and the capacity is a count, not a weight -- an instrument is
 * one slot of a bag whatever it is made of.
 */
public class InstrumentBagItem extends Item {

    /** How many instruments one bag holds. */
    public static final int CAPACITY = 6;

    public InstrumentBagItem(Properties properties) {
        super(properties);
    }

    /** What is in the bag, in the order it was stowed. */
    public static List<ItemStack> contents(ItemStack bag) {
        List<ItemStack> out = new ArrayList<>();
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems().forEach(out::add);
        return out;
    }

    private static void store(ItemStack bag, List<ItemStack> items) {
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    private static boolean accepts(ItemStack bag, ItemStack stack) {
        return stack.getItem() instanceof Instrument && contents(bag).size() < CAPACITY;
    }

    private static ItemStack stow(ItemStack bag, ItemStack stack) {
        List<ItemStack> items = contents(bag);
        items.add(stack.copyWithCount(1));
        store(bag, items);
        return stack;
    }

    private static ItemStack take(ItemStack bag) {
        List<ItemStack> items = contents(bag);
        if (items.isEmpty())
            return ItemStack.EMPTY;
        ItemStack out = items.remove(items.size() - 1);
        store(bag, items);
        return out;
    }

    /** The bag is on the cursor and clicks a slot: an instrument there goes in, an empty slot gets one. */
    @Override
    public boolean overrideStackedOnOther(ItemStack bag, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY)
            return false;
        ItemStack there = slot.getItem();
        if (there.isEmpty()) {
            ItemStack out = take(bag);
            if (!out.isEmpty())
                slot.safeInsert(out);
            return true;
        }
        if (accepts(bag, there) && slot.mayPickup(player)) {
            stow(bag, there);
            slot.remove(1);
            return true;
        }
        return false;
    }

    /** The bag is in a slot and the cursor clicks it: an instrument held goes in, an empty hand takes one. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack bag, ItemStack other, Slot slot, ClickAction action,
            Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player))
            return false;
        if (other.isEmpty()) {
            ItemStack out = take(bag);
            if (!out.isEmpty())
                access.set(out);
            return true;
        }
        if (accepts(bag, other)) {
            stow(bag, other);
            other.shrink(1);
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack bag, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> items = contents(bag);
        tooltip.add(Component.translatable("tooltip.feedback.instrument_bag.count", items.size(), CAPACITY)
                .withStyle(ChatFormatting.GRAY));
        for (ItemStack stack : items)
            tooltip.add(Component.literal("  ").append(stack.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
        if (items.isEmpty())
            tooltip.add(Component.translatable("tooltip.feedback.instrument_bag.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
