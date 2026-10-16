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

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * What the hand can tell about a modular item: adjectives, never figures (philosophy 8, spec 6).
 * "Head-heavy", "keen", "brittle", "light in the hand". Exact mass and hardness need a balance
 * scale and a scratch file set, because what you have costs an instrument; what you made is on the
 * item for anyone to see. Silent Gear's tooltip shows each stat with the reason behind it; the
 * reasons here are the parts, and that is the first line.
 */
public final class ModularTooltip {

    private ModularTooltip() {
    }

    /** Heads heavier than this share of the swing's moment read head-heavy. */
    private static final float HEAD_HEAVY = 0.85f;
    private static final float BALANCED_BELOW = 0.6f;
    /** Slower than this swings heavy, quicker than that light. */
    private static final float HEAVY_SWING = 0.9f;
    private static final float LIGHT_SWING = 1.8f;
    /** Toughness under this share of hardness is brittle; hardness under this is soft. */
    private static final float BRITTLE_SHARE = 0.4f;
    private static final float SOFT_HARDNESS = 1f;

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
        List<String> traits = new ArrayList<>();
        if (type != null && isMelee(type)) {
            if (f.get(Fig.BALANCE) > HEAD_HEAVY)
                traits.add("head_heavy");
            else if (f.get(Fig.BALANCE) < BALANCED_BELOW)
                traits.add("balanced");
            if (f.get(Fig.SWING) < HEAVY_SWING)
                traits.add("heavy");
            else if (f.get(Fig.SWING) > LIGHT_SWING)
                traits.add("light");
            if (design.edge() == Edge.KEEN)
                traits.add("keen");
            else if (design.edge() == Edge.BLUNT)
                traits.add("blunt");
        }
        if (f.get(Fig.EDGE_TOUGHNESS) < BRITTLE_SHARE * f.get(Fig.HARDNESS))
            traits.add("brittle");
        else if (f.get(Fig.HARDNESS) < SOFT_HARDNESS)
            traits.add("soft");
        if (f.get(Fig.JOINT) > 1)
            traits.add("loose");
        if (!traits.isEmpty()) {
            MutableComponent line = Component.empty();
            for (String t : traits) {
                if (!line.getSiblings().isEmpty())
                    line.append(", ");
                line.append(Component.translatable("tooltip.feedback.modular." + t));
            }
            tooltip.add(line.withStyle(ChatFormatting.GRAY));
        }
        if (ModularStacks.broken(stack))
            tooltip.add(Component.translatable("tooltip.feedback.modular.broken").withStyle(ChatFormatting.RED));
        else if (f.step() > 0)
            tooltip.add(Component.translatable("tooltip.feedback.modular.wear." + Math.min(f.step(), 3)).withStyle(ChatFormatting.YELLOW));
    }

    private static boolean isMelee(ModularType type) {
        return switch (type.family()) {
            case DIG, BLADE, MACE, SPEAR -> true;
            default -> false;
        };
    }

    /** "Iron head, Wood handle". */
    public static MutableComponent parts(ModularDesign design, ModularType type) {
        MutableComponent out = Component.empty();
        for (Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            if (role == null)
                continue;
            if (!out.getSiblings().isEmpty())
                out.append(", ");
            out.append(Component.translatable("tooltip.feedback.modular.part",
                    Component.translatable("material." + p.material().getNamespace() + "." + p.material().getPath()),
                    Component.translatable("modular.feedback.role." + role.name())));
        }
        return out;
    }
}
