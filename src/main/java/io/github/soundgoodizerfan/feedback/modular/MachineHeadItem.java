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
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.world.item.ItemStack;

/**
 * The head of a Mechanical Hammer or Press, made at the Tool Table from a shaped head blank
 * ({@code modular_type/machine_head.json}). Seated in a machine ({@code machine/HeadSlot}) it is what wears and
 * what sets the blow; held, it does nothing. No durability bar: its condition is {@code TOOL_CONDITION}, worn by
 * the machine that holds it.
 */
public class MachineHeadItem extends ModularItem {

    public MachineHeadItem(Properties properties) {
        super(properties);
    }

    /** The St ceiling its design bakes to; zero for anything that is not a baked head (a blank, an empty stack). */
    public static float strikeSt(ItemStack stack) {
        if (!(stack.getItem() instanceof MachineHeadItem))
            return 0f;
        ModularFigures f = ModularStacks.figures(stack);
        return f == null ? 0f : f.get(Fig.STRIKE_ST);
    }
}
