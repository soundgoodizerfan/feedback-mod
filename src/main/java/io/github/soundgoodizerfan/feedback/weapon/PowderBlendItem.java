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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** One batch of mixed powders from the Mixer: the press seats it as the charge. The list on it is the whole of what it is. */
public class PowderBlendItem extends Item {

    public PowderBlendItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var charges = stack.get(FWeapons.BLEND.get());
        if (charges == null)
            return;
        var line = Component.empty();
        for (int k = 0; k < charges.size(); k++) {
            if (k > 0)
                line.append(" + ");
            line.append(Component.translatable("cartridge.feedback.part", charges.get(k).count(),
                    CartridgeItem.name(charges.get(k).material())));
        }
        tooltip.add(line.withStyle(ChatFormatting.GRAY));
    }
}
