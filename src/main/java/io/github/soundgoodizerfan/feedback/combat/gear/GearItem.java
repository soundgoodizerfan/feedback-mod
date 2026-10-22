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
package io.github.soundgoodizerfan.feedback.combat.gear;

import java.util.List;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A bracer, sash or back piece, worn in the Curios slot of its {@link GearKind}. The item holds no
 * figure: a blank one does nothing, and a crafted one carries {@link GearStats} baked from its
 * materials by {@link GearRecipe}. {@link CombatGear} is what reads them.
 */
public class GearItem extends Item {

    private final GearKind kind;

    public GearItem(Properties properties, GearKind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public GearKind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GearStats g = stack.get(FDataComponents.GEAR.get());
        if (g == null)
            return;
        tooltip.add(Component.translatable("item.feedback.gear.shell", name(g.shell())));
        tooltip.add(Component.translatable("item.feedback.gear.strap", name(g.strap())));
        line(tooltip, "window", g.window());
        line(tooltip, "joint", g.joint());
        line(tooltip, "roll_distance", g.rollDistance());
        line(tooltip, "recovery_shave", g.recoveryShave());
        line(tooltip, "i_frames", g.iFrames());
        line(tooltip, "mass", g.mass());
    }

    private static void line(List<Component> tooltip, String key, float value) {
        if (value > 0)
            tooltip.add(Component.translatable("item.feedback.gear." + key, Readout.figure(Quantity.FIGURE, value)));
    }

    private static Component name(ResourceLocation material) {
        return Component.translatable("material." + material.getNamespace() + "." + material.getPath());
    }
}
