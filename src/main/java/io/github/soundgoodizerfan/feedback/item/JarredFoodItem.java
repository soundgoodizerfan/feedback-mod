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

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A jar with one food in it. Named from the food; the eating figures are baked on the stack by {@code Canning.refresh}. */
public class JarredFoodItem extends Item {

    public JarredFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Canned can = stack.get(FDataComponents.CANNED.get());
        return can == null ? super.getName(stack) : Component.translatable("item.feedback.jarred_food.of", can.food().getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Canned can = stack.get(FDataComponents.CANNED.get());
        if (can == null)
            return;
        if (can.sealed())
            tooltip.add(Component.translatable("tooltip.feedback.jar.sealed").withStyle(ChatFormatting.GRAY));
        else if (context.level() != null) {
            // An unsealed jar is just food in glass, so it says how it is keeping.
            var level = context.level();
            Cooking.findPerishable(level, can.food()).ifPresent(entry -> {
                int spoil = Cooking.spoilage(Cooking.current(can.food(), entry, level,
                        io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable.at(level).value()));
                if (spoil > 0)
                    tooltip.add(Component.translatable("tooltip.feedback.food.spoil." + spoil)
                            .withStyle(spoil == 2 ? ChatFormatting.DARK_RED : ChatFormatting.GOLD));
            });
        }
        if (can.mush() >= FTuning.CANNING_MUSH_SHOWN_AT)
            tooltip.add(Component.translatable("tooltip.feedback.jar.mushy").withStyle(ChatFormatting.GOLD));
    }
}
