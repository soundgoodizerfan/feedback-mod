// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.item.ModularRework;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The Anvil's Repair tab: mend and rename the item in its slot, as the vanilla anvil did.
 *
 * <ul>
 *   <li>A modular item is mended by property through {@link ModularRework} ({@code MEND}), the same rule as the Engineering Table.</li>
 *   <li>Any other damaged item takes the repair ingredient its own item names ({@code Item.isValidRepairItem}, so another mod's
 *       tools work) at a quarter of their durability per unit, or is combined with a second damaged copy of itself.</li>
 *   <li>A name typed in the box is set on the result; a blank one takes the name off.</li>
 * </ul>
 * Enchantment merging and level costs of the vanilla anvil are not here.
 */
final class AnvilRepair {

    /** The longest name, as the vanilla anvil allows. */
    static final int NAME_MAX = 50;
    /** What one repair unit puts back of a plain tool's durability (the vanilla anvil's quarter) and what combining two adds (a twelfth over a tenth). */
    private static final int UNIT_DIVISOR = 4, COMBINE_BONUS_PERCENT = 12, MAX_PIECES = 64;

    private AnvilRepair() {
    }

    /** The result of the item and its material, and how many pieces of the material slot it spends; an empty result means nothing is offered. */
    record Made(ItemStack out, int used) {
        static final Made NONE = new Made(ItemStack.EMPTY, 0);
    }

    /** {@code name} is what the box holds, or null if the player has not typed (the item keeps its name). */
    static Made make(ItemStack item, ItemStack material, String name, Map<ResourceLocation, Material> materials,
                     Map<ResourceLocation, ModularType> types) {
        if (item.isEmpty())
            return Made.NONE;
        ItemStack out = item.copy();
        int used = 0;
        if (!material.isEmpty() && item.getCount() == 1) {
            Made m = ModularStacks.design(item) != null ? mendModular(item, material, materials, types)
                    : item.isDamaged() ? mendPlain(item, material) : Made.NONE;
            if (m.used() > 0) {
                out = m.out();
                used = m.used();
            }
        }
        boolean renamed = false;
        if (name != null) {
            if (name.isBlank()) {
                renamed = out.has(DataComponents.CUSTOM_NAME);
                out.remove(DataComponents.CUSTOM_NAME);
            } else if (!name.equals(item.getHoverName().getString())) {
                renamed = true;
                out.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            }
        }
        return used == 0 && !renamed ? Made.NONE : new Made(out, used);
    }

    /** Pieces of the material slot are laid on one at a time until the tool is whole; the fewest that do. */
    private static Made mendModular(ItemStack item, ItemStack material, Map<ResourceLocation, Material> materials,
                                    Map<ResourceLocation, ModularType> types) {
        List<ItemStack> pieces = new ArrayList<>();
        for (int k = 0; k < material.getCount() && k < MAX_PIECES; k++)
            pieces.add(material.copyWithCount(1));
        Optional<ItemStack> best = Optional.empty();
        int used = 0;
        for (int n = 1; n <= pieces.size(); n++) {
            Optional<ItemStack> r = ModularRework.rework(item, pieces.subList(0, n), materials, types, ModularRework.Mode.MEND);
            if (r.isEmpty()) {
                if (best.isPresent())
                    break;
                continue;
            }
            best = r;
            used = n;
            if (r.get().getDamageValue() == 0 && GunCondition.wear(r.get()) <= 0f)
                break;
        }
        return best.isPresent() ? new Made(best.get(), used) : Made.NONE;
    }

    /** A foreign tool: its own repair ingredient, a quarter of its durability a unit, or a second copy of itself. */
    private static Made mendPlain(ItemStack item, ItemStack material) {
        int max = item.getMaxDamage();
        ItemStack out = item.copy();
        if (ItemStack.isSameItem(item, material) && material.isDamaged()) {
            int left = max - item.getDamageValue() + max - material.getDamageValue() + max * COMBINE_BONUS_PERCENT / 100;
            out.setDamageValue(Math.max(0, max - left));
            return new Made(out, 1);
        }
        if (!item.getItem().isValidRepairItem(item, material))
            return Made.NONE;
        int step = Math.max(1, max / UNIT_DIVISOR), damage = item.getDamageValue(), used = 0;
        while (damage > 0 && used < material.getCount()) {
            damage = Math.max(0, damage - step);
            used++;
        }
        out.setDamageValue(damage);
        return new Made(out, used);
    }
}
