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
package io.github.soundgoodizerfan.feedback.machine.tube;

import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.logistics.sort.SortReader;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * A capsule that rides a tube run: {@link FTuning#CARRIER_SLOTS} slots of cargo (the vanilla
 * container component) and an address, a named colour (the vanilla base-colour component). A pod is
 * the wide-bore kind, with a seat instead of slots. The address is a player-set mark read by
 * junctions; it is never an item id and never derived from the contents.
 */
public class CarrierItem extends Item {

    /** The three shapes: a plain capsule, one with a magnetic plate a linear-motor ring can drive, and a pod. */
    public enum Kind { CAPSULE, MAGNETIC, POD }

    private final Kind kind;

    public CarrierItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static Optional<DyeColor> address(ItemStack carrier) {
        return Optional.ofNullable(carrier.get(DataComponents.BASE_COLOR));
    }

    public static void setAddress(ItemStack carrier, Optional<DyeColor> address) {
        if (address.isPresent())
            carrier.set(DataComponents.BASE_COLOR, address.get());
        else
            carrier.remove(DataComponents.BASE_COLOR);
    }

    public static NonNullList<ItemStack> contents(ItemStack carrier) {
        NonNullList<ItemStack> list = NonNullList.withSize(FTuning.CARRIER_SLOTS, ItemStack.EMPTY);
        carrier.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(list);
        return list;
    }

    public static void setContents(ItemStack carrier, List<ItemStack> items) {
        if (items.stream().allMatch(ItemStack::isEmpty))
            carrier.remove(DataComponents.CONTAINER);
        else
            carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    /** Mass (Mu) of one stack, from the shared sort reader's unit mass. */
    public static float stackMass(ItemStack stack) {
        return stack.getCount() * SortReader.unitMass(stack);
    }

    /** Capsule mass: itself, its load, and a rider if it is a pod with one aboard. */
    public static float mass(ItemStack carrier, boolean rider) {
        float m = carrier.getItem() instanceof CarrierItem c && c.kind == Kind.POD ? FTuning.POD_EMPTY_MU : FTuning.CARRIER_EMPTY_MU;
        for (ItemStack s : contents(carrier))
            m += stackMass(s);
        return rider ? m + FTuning.POD_RIDER_MU : m;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        address(stack).ifPresent(c -> tooltip.add(Component.translatable("tooltip.feedback.carrier.address",
                Component.translatable("color.minecraft." + c.getName()))));
        int n = (int) contents(stack).stream().filter(s -> !s.isEmpty()).count();
        if (n > 0)
            tooltip.add(Component.translatable("tooltip.feedback.carrier.loaded", n));
    }
}
