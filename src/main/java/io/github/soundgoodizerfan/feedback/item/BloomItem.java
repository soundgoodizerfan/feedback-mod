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

import java.util.List;

import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A bloom: spongy iron full of slag, the intermediate of the bloomery and the puddling furnace. The slag it still holds is the
 * {@link MadeOf#flaw()} on the stack (0 clean to 3 packed, how many pockets the anvil has to herd out); an unstamped one is clean.
 */
public class BloomItem extends Item {

    public BloomItem(Properties properties) {
        super(properties);
    }

    /** How slaggy the bloom is, 0 (clean) to 3 (packed). */
    public static int slag(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made == null ? 0 : Math.max(0, Math.min(3, made.flaw()));
    }

    /** Nuggets of its iron a bloom has already lost to scale in reheating. */
    public static int scale(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made == null ? 0 : Math.round(made.dims().getOrDefault("scale", 0f));
    }

    /** A bloom of the given slag grade, as the furnaces make it. */
    public static ItemStack of(Item bloom, int slag) {
        ItemStack stack = new ItemStack(bloom);
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("feedback", "iron_bloom"), "bloom", 1, "", slag));
        return stack;
    }

    public static ItemStack of(Item bloom, int slag, int scale) {
        ItemStack stack = of(bloom, slag);
        if (scale > 0)
            stack.set(FDataComponents.MADE_OF.get(), stack.get(FDataComponents.MADE_OF.get()).withDim("scale", scale));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.feedback.bloom.slag." + slag(stack)));
    }
}
