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

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * What the hand can tell about a modular item: modifiers, never figures (philosophy 8, spec 6).
 * "Head-heavy", "keen", "brittle", the trait and the shape, all from {@link ModularModifiers}. Exact mass and hardness need a balance
 * scale and a scratch file set, because what you have costs an instrument; what you made is on the
 * item for anyone to see. Silent Gear's tooltip shows each stat with the reason behind it; the
 * reasons here are the parts, and that is the first line.
 */
public final class ModularTooltip {

    private ModularTooltip() {
    }

    // Public so the Shift glossary (client/ModularReadings) prints the same thresholds it judges by.
    /** Heads heavier than this share of the swing's moment read head-heavy. */
    public static final float HEAD_HEAVY = 0.85f;
    public static final float BALANCED_BELOW = 0.6f;
    /** Slower than this swings heavy, quicker than that light. */
    public static final float HEAVY_SWING = 0.9f;
    public static final float LIGHT_SWING = 1.8f;
    /** Toughness under this share of hardness is brittle; hardness under this is soft. */
    public static final float BRITTLE_SHARE = 0.4f;
    public static final float SOFT_HARDNESS = 1f;

    public static void add(ItemStack stack, List<Component> tooltip) {
        ModularDesign design = ModularStacks.design(stack);
        if (design == null) {
            tooltip.add(Component.translatable("tooltip.feedback.modular.undesigned").withStyle(ChatFormatting.GRAY));
            return;
        }
        ModularType type = ModularTypes.known().get(design.type());
        if (type != null)
            tooltip.add(parts(design, type).withStyle(ChatFormatting.GRAY));
        ModularFigures f = ModularStacks.figures(stack);
        if (f == null)
            return;
        MutableComponent mods = ModularModifiers.line(ModularModifiers.of(design, type, f));
        if (mods != null)
            tooltip.add(mods);
        MaterialPerks.of(stack).forEach((perk, level) -> tooltip.add(Component.translatable(
                "tooltip.feedback.modular.perk." + perk.getSerializedName(), level).withStyle(ChatFormatting.GOLD)));
        MaterialPerks.attunements(stack).forEach(attunement -> tooltip.add(Component.translatable(
                "tooltip.feedback.attuned." + attunement.kind(), attunement.level()).withStyle(ChatFormatting.LIGHT_PURPLE)));
        if (ModularStacks.broken(stack))
            tooltip.add(Component.translatable("tooltip.feedback.modular.broken").withStyle(ChatFormatting.RED));
        else if (f.step() > 0)
            tooltip.add(Component.translatable("tooltip.feedback.modular.wear." + Math.min(f.step(), 3)).withStyle(ChatFormatting.YELLOW));
    }

    /** "Iron head, Wood handle". */
    public static MutableComponent parts(ModularDesign design, ModularType type) {
        return parts(design.parts(), type);
    }

    /** The named parts as "Iron head, Wood handle". */
    public static MutableComponent parts(List<Part> parts, ModularType type) {
        MutableComponent out = Component.empty();
        for (Part p : parts) {
            ModularType.Role role = type.roles().get(p.role());
            if (role == null)
                continue;
            if (!out.getSiblings().isEmpty())
                out.append(", ");
            Component named = p.variant().isEmpty() ? Component.translatable("modular.feedback.role." + role.name())
                    : Component.translatable("modular.feedback.variant." + p.variant());
            if (p.flaw() > 0)
                named = Component.translatable("modular.feedback.flaw." + p.flaw(), named);
            out.append(Component.translatable("tooltip.feedback.modular.part",
                    Component.translatable("material." + p.material().getNamespace() + "." + p.material().getPath()), named));
        }
        return out;
    }
}
