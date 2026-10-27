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
package io.github.soundgoodizerfan.feedback.machine.derrick;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A boring bit: the part of the derrick that meets the rock. What it is made of ({@code MADE_OF})
 * sets its hardness, so the gate is the material's, not a tier; wear rides on it as {@code
 * TOOL_CONDITION}, exactly as the lathe's bit does.
 */
public class DerrickBitItem extends Item {

    public DerrickBitItem(Properties properties) {
        super(properties);
    }

    /** The bit's hardness: its material's, or steel's if nothing is stamped. */
    public static float hardness(ItemStack bit) {
        MadeOf made = bit.get(FDataComponents.MADE_OF.get());
        Material m = made == null ? null : MaterialTable.get().materials().get(made.material());
        return m == null ? FTuning.DERRICK_BIT_DEFAULT_HARDNESS : m.hardness().value();
    }

    public static float condition(ItemStack bit) {
        return bit.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null)
            tooltip.add(Component.translatable("material." + made.material().getNamespace() + "." + made.material().getPath()));
        tooltip.add(Component.translatable("tooltip.feedback.derrick_bit", Math.round(hardness(stack)),
                Math.round(condition(stack) * 100f)));
    }
}
