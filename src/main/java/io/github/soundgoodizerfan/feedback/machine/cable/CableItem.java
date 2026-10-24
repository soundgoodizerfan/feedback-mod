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
package io.github.soundgoodizerfan.feedback.machine.cable;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.item.FormBlock;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A length of cable in some metal: the material rides on the stack ({@link MadeOf}) and passes to
 * the block entity when it is placed (with the {@code INSULATION} band it was crafted with, if any), so the name is a pattern over the material's
 * ({@code form.feedback.cable} = {@code %s Cable}) and the sprite is tinted by it.
 */
public class CableItem extends BlockItem {

    public CableItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** {@code count} bare cable of {@code material}. */
    public static ItemStack of(ResourceLocation material, int count) {
        return of(material, count, null);
    }

    /** {@code count} cable of {@code material} coated to hold off {@code insulation}, or bare when null. */
    public static ItemStack of(ResourceLocation material, int count, @Nullable VoltageBand insulation) {
        ItemStack stack = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.CABLE.get(), count);
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(material, "wire", 1));
        if (insulation != null)
            stack.set(FDataComponents.INSULATION.get(), insulation);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (getBlock() instanceof FenceBlock)
            return;
        VoltageBand band = stack.get(FDataComponents.INSULATION.get());
        tooltip.add(band == null ? Component.translatable("feedback.tooltip.cable.bare").withStyle(ChatFormatting.GRAY)
                : Component.translatable("feedback.tooltip.cable.insulated", Component.translatable(band.key()))
                        .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made == null ? super.getName(stack) : FormBlock.name(made.material(), getBlock() instanceof FenceBlock ? "fence" : "cable");
    }
}
