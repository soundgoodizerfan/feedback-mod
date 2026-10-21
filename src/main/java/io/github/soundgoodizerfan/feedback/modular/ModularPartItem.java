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

import io.github.soundgoodizerfan.feedback.item.FormBlock;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One kind of shaped part -- a pickaxe head, a sword blade, a helmet's plate -- in any material:
 * the material rides on the stack ({@link MadeOf}), so the name is a pattern over the material's
 * ({@code form.feedback.pickaxe_head} = {@code %s Pickaxe Head}) and the sprite is tinted by it.
 * No behaviour: what it does is decided when it is set into a tool.
 */
public class ModularPartItem extends Item {

    public ModularPartItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made == null)
            return super.getName(stack);
        Component name = made.variant().isEmpty()
                ? FormBlock.name(made.material(), BuiltInRegistries.ITEM.getKey(this).getPath())
                : Component.translatable("modular.feedback.variant_of",
                        Component.translatable("material." + made.material().getNamespace() + "." + made.material().getPath()),
                        Component.translatable("modular.feedback.variant." + made.variant()));
        if (made.flaw() > 0)
            name = Component.translatable("modular.feedback.flaw." + made.flaw(), name);
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null && made.flaw() > 0)
            tooltip.add(Component.translatable("tooltip.feedback.part.flaw", made.flaw(), ForgeRhythm.MAX_FLAW)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
