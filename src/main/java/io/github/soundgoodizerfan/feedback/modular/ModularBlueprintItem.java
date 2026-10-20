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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A modular design on paper, printed at the Blueprint Table ({@code docs/feedback_modular_items_spec.md}
 * section 6): the design and the figures it bakes to, as drawn. A blueprint <i>predicts</i> -- it
 * gives the figures every part would make if it came out exactly to drawing, for free, because it
 * is paper. What an item actually has is measured on the item, with an instrument.
 *
 * <p>A type that says {@code blueprint} (the crossbow) takes one of its own in the grid to
 * assemble; the craft keeps it, as the gun's does. Everything else is its parts and needs none.
 */
public class ModularBlueprintItem extends Item {

    public ModularBlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return;
        tooltip.add(Component.translatable("item." + design.type().getNamespace() + "." + design.type().getPath())
                .withStyle(ChatFormatting.WHITE));
        ModularType type = ModularTypes.known().get(design.type());
        if (type != null)
            tooltip.add(ModularTooltip.parts(design, type).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.feedback.modular.edge." + design.edge().getSerializedName())
                .withStyle(ChatFormatting.GRAY));
        ModularFigures figures = ModularStacks.figures(stack);
        if (figures != null)
            figures(figures).forEach(line -> tooltip.add(line.copy().withStyle(ChatFormatting.DARK_AQUA)));
    }

    /** One line per figure the design has: a figure a type does not use bakes to zero and is left out. */
    public static List<Component> figures(ModularFigures figures) {
        return figures(figures, false);
    }

    /** The figures {@link #figures} has a line for, in the same order. */
    public static List<Fig> present(ModularFigures figures) {
        return java.util.Arrays.stream(Fig.values()).filter(fig -> figures.get(fig) != 0).toList();
    }

    /** What a figure is: its full label, then what it does to the item. */
    public static List<Component> explain(Fig fig) {
        String key = "modular.feedback.fig." + fig.name().toLowerCase(Locale.ROOT);
        return List.of(Component.translatable(key), Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY));
    }

    /** As {@link #figures(ModularFigures)}; {@code brief} cuts each label at its comma (the unit) to fit a column. */
    public static List<Component> figures(ModularFigures figures, boolean brief) {
        List<Component> out = new ArrayList<>();
        for (Fig fig : Fig.values()) {
            float v = figures.get(fig);
            if (v == 0)
                continue;
            Component label = Component.translatable("modular.feedback.fig." + fig.name().toLowerCase(Locale.ROOT));
            if (brief)
                label = Component.literal(label.getString().split(",", 2)[0]);
            out.add(Component.empty().append(label).append(": " + String.format(Locale.ROOT,
                    v >= 100 ? "%.0f" : v >= 10 ? "%.1f" : "%.2f", v)));
        }
        return out;
    }

    /**
     * Every material a role can be drawn in, each in the first of its forms the role takes. A role
     * that takes a shaped part is drawn in the part's form ({@code axe_head}), which no material
     * lists -- the material fits if it has stock the part is shaped from.
     */
    public static List<Material.Located> choices(ModularType.Role role, Map<ResourceLocation, Material> materials) {
        List<Material.Located> out = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (role.part().isPresent()) {
                if (hasStock(role, id, m))
                    out.add(new Material.Located(id, m, role.forms().get(0)));
                return;
            }
            m.items(id).keySet().stream()
                    .map(form -> new Material.Located(id, m, form))
                    .filter(role::accepts)
                    .findFirst()
                    .ifPresent(out::add);
        });
        return out;
    }

    private static boolean hasStock(ModularType.Role role, ResourceLocation id, Material m) {
        return m.items(id).keySet().stream().anyMatch(form -> role.acceptsStock(new Material.Located(id, m, form)));
    }

    /**
     * What {@code design} bakes to, as drawn and new, if the type can be put together that way: its
     * parts are one of the type's patterns, each in a form of its material its role takes. The
     * server's check at print -- a client names materials, it does not vouch for them.
     */
    public static Optional<ModularFigures> predict(ModularDesign design, Map<ResourceLocation, ModularType> types,
                                                   Map<ResourceLocation, Material> materials) {
        ModularType type = types.get(design.type());
        if (type == null)
            return Optional.empty();
        Map<String, Integer> drawn = new LinkedHashMap<>();
        for (Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            Material m = materials.get(p.material());
            if (role == null || m == null || !role.accepts(new Material.Located(p.material(), m, p.form()))
                    || !(role.part().isPresent() ? hasStock(role, p.material(), m) : m.items(p.material()).containsKey(p.form())))
                return Optional.empty();
            drawn.put(p.role(), p.count());
        }
        if (IntStream.range(0, type.patterns().size()).noneMatch(i -> type.counts(i).equals(drawn)))
            return Optional.empty();
        return ModularBake.bake(design, type, materials, 0);
    }
}
