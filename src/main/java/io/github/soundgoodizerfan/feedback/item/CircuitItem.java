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

import io.github.soundgoodizerfan.feedback.process.Circuit;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Basic Electronic Circuit (mechanics 6.21). Everything about it is the {@link Circuit}
 * component; the item only carries it and shows it. The tooltip says only how many elements it
 * has; reading its quality takes a component tester, as for any part. A carried
 * circuit that gets hotter than its traces take lifts them (dead part).
 */
public class CircuitItem extends Item {

    public CircuitItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide)
            Circuit.scorch(stack, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Circuit c = Circuit.of(stack);
        tooltip.add(c.live() ? Component.translatable("feedback.circuit.elements", c.elements())
                : Component.translatable("feedback.circuit.dead"));
    }
}
