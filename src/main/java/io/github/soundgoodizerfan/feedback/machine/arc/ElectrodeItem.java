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
package io.github.soundgoodizerfan.feedback.machine.arc;

import java.util.List;

import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An arc furnace electrode: a rod of any material that can carry the arc ({@link Material#electrodeLimit}).
 * What it is made of ({@code MADE_OF}) sets how hot its arc may burn; wear rides on it as {@code TOOL_CONDITION}.
 * It is consumed by nature, like a fuse or a grinding wheel's grit: the hotter and longer the bath it works, the faster it sublimes.
 */
public class ElectrodeItem extends Item {

    public ElectrodeItem(Properties properties) {
        super(properties);
    }

    /** One new electrode of {@code material}. */
    public static ItemStack of(ResourceLocation material) {
        ItemStack stack = new ItemStack(FItems.ARC_ELECTRODE.get());
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(material, "rod", 1));
        return stack;
    }

    /** The material this electrode is made of, or null if it carries none. */
    public static Material material(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made == null ? null : MaterialTable.get().materials().get(made.material());
    }

    /** The temperature its arc cannot pass; zero for an electrode that carries no usable material. */
    public static float limitTu(ItemStack stack) {
        Material material = material(stack);
        return material == null ? 0f : material.electrodeLimit().map(tu -> tu.value()).orElse(0f);
    }

    public static float condition(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null)
            tooltip.add(Component.translatable("material." + made.material().getNamespace() + "." + made.material().getPath()));
        tooltip.add(Component.translatable("tooltip.feedback.arc_electrode", Math.round(limitTu(stack)), Math.round(condition(stack) * 100f)));
    }
}
