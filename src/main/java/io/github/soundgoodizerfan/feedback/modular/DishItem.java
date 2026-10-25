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

import java.util.List;
import java.util.function.Consumer;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.DishFigures;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A dish ({@code docs/feedback_modular_dishes_spec.md}): a modular item that is eaten. Its hunger, saturation and
 * hazards are the vanilla {@code food} component and its compounds the {@code meal} component, both written by
 * {@link ModularStacks#assemble}. It has no durability and is no tool or weapon, so every hook that would spend a
 * point of a modular item does nothing here.
 */
public class DishItem extends ModularItem implements io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.Cooling {

    public DishItem(Properties properties) {
        super(properties);
    }

    /** Named from structure, never an id: the main and base foods, then the container's class, then the finish. */
    @Override
    public Component getName(ItemStack stack) {
        // A dish rule's name (FoodNames#stamp, written on the server) replaces the structural one.
        if (stack.has(net.minecraft.core.component.DataComponents.ITEM_NAME))
            return super.getName(stack);
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        if (type == null)
            return super.getName(stack);
        Component container = Component.translatable("modular.feedback.container.default");
        Component main = null, base = null, other = null, finish = null, soup = null;
        for (Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            if (role == null)
                continue;
            Component material = Component.translatable("material." + p.material().getNamespace() + "." + p.material().getPath());
            switch (role.name()) {
                case "container" -> container = Component.translatable("modular.feedback.container." + (p.kind().isEmpty() ? p.material().getPath() : p.kind()));
                case "finish" -> finish = material;
                case "main" -> main = material;
                case "base" -> {
                    // A soup is its serving: named for the broth or stew poured in, not for a material.
                    float serving = p.dim(DishBake.SERVING, -1f);
                    if (serving >= 0f)
                        soup = Component.translatable("item.feedback.meal." + DishBake.SERVINGS.get(Math.round(serving)));
                    base = material;
                }
                default -> other = material;
            }
        }
        if (soup != null) {
            Component bowl = Component.translatable("item.feedback.soup_name", soup);
            return finish == null ? bowl : Component.translatable("item.feedback.dish_name.finish", bowl, finish);
        }
        Component dominant = main != null && base != null ? Component.translatable("item.feedback.dish_name.pair", main, base)
                : main != null ? main : base != null ? base : other;
        Component name = dominant == null ? container : Component.translatable("item.feedback.dish_name", dominant, container);
        return finish == null ? name : Component.translatable("item.feedback.dish_name.finish", name, finish);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // How the finish was poured; the spoilage and the live-organism lines are the food tooltip's.
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        Part finish = type == null || type.afterRole() == null ? null : design.part(type.afterRole());
        if (finish != null)
            tooltip.add(ToolTableMenu.pourName(finish.dim("pour", FTuning.POUR_AUTOMATION)).withStyle(ChatFormatting.GRAY));
        DishFigures figures = stack.get(FDataComponents.DISH_FIGURES.get());
        if (figures == null)
            return;
        if (figures.seal() > 0)
            tooltip.add(Component.translatable("modular.feedback.dish.seal." + figures.seal()).withStyle(ChatFormatting.GRAY));
        if (figures.spilling())
            tooltip.add(Component.translatable("modular.feedback.dish.spilling").withStyle(ChatFormatting.RED));
        if (figures.spilled() > 0)
            tooltip.add(Component.translatable("modular.feedback.dish.spilled", Math.round(figures.spilled() * 100)).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return 0;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        return false;
    }

    @Override
    public io.github.soundgoodizerfan.feedback.core.unit.TuRate coolingRate(ItemStack stack) {
        return FTuning.ITEM_COOLING_TU_PER_TICK;
    }

    /** A container with a warming perk (blaze rod) holds what is on it warm, wherever it is, with nothing ticked. */
    @Override
    public io.github.soundgoodizerfan.feedback.core.unit.Tu holdsAt(ItemStack stack) {
        return MaterialPerks.of(stack).containsKey(io.github.soundgoodizerfan.feedback.process.Material.Perk.WARMING) ? FTuning.WARMING_HOLD_TU : null;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // A dish is not a weapon: hitting with it costs it nothing.
    }
}
