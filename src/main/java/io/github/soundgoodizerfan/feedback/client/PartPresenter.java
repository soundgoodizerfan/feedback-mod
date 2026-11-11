// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularModifiers;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ToolTableShaping;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The figures of parts and results, written once ({@code docs/gui/gui_flow_information.md}): a shape's
 * description and the glyph bars a hovered shape shows. Every station and the Blueprint
 * Table call here, so a figure never has two spellings; every number goes through {@link Readout}.
 */
final class PartPresenter {

    private PartPresenter() { }

    /**
     * The glyph figures of a stock: how hard it is, how tough, how heavy, each as 0..1 along a log scale so a bar tells more than a
     * number would. Direct figures of the material only (a derived figure is the blueprint's job). Fills {@code out[0..2]}.
     */
    static void glyphs(Material m, float[] out) {
        out[0] = bar(m.hardness().value(), 300f);
        out[1] = bar(m.mechanical().toughness().map(io.github.soundgoodizerfan.feedback.core.unit.St::value).orElse(0f), 120f);
        out[2] = bar(m.density().muPerMb() * 10f, 90f);
    }

    private static float bar(float value, float top) {
        return net.minecraft.util.Mth.clamp((float) (Math.log1p(Math.max(0f, value)) / Math.log1p(top)), 0.04f, 1f);
    }

    static String num(float value) {
        return Readout.figure(io.github.soundgoodizerfan.feedback.instrument.Quantity.FIGURE, value);
    }

    static String roleName(ModularType type, String role) {
        return Component.translatable("modular.feedback.role." + type.roles().get(role).name()).getString();
    }

    static Component name(ResourceLocation type) {
        return BuiltInRegistries.ITEM.get(type).getDescription();
    }



    /**
     * What a shape is, and for a variant how it differs from the standard one (spec 4.2): the label, then the part's text, the
     * variant's and the trait lines, each from lang. One list for every table that offers shapes.
     */
    static List<Component> describe(ToolTableShaping.Option o) {
        List<Component> lines = new ArrayList<>();
        lines.add(o.label());
        String part = "modular.feedback.desc.part." + o.shaping().item().getPath();
        if (net.minecraft.client.resources.language.I18n.exists(part))
            lines.add(Component.translatable(part).withStyle(net.minecraft.ChatFormatting.GRAY));
        String variant = "modular.feedback.desc.variant." + o.variant().name();
        if (!o.variant().name().isEmpty() && net.minecraft.client.resources.language.I18n.exists(variant))
            lines.add(Component.translatable(variant).withStyle(net.minecraft.ChatFormatting.YELLOW));
        ModularType.Variant v = o.variant();
        if (!v.name().isEmpty()) {
            diff(lines, "reach", v.reach() - 0f);
            diff(lines, "bite", 1f - v.section());
            diff(lines, "chip", v.section() - 1f);
            diff(lines, "strike", v.strike() - 1f);
            diff(lines, "weight", v.mass() - 1f);
        }
        for (ModularModifiers.Modifier m : ModularModifiers.forPart(o))
            if (m.kind() == ModularModifiers.Kind.TRAIT)
                lines.add(m.line());
        return lines;
    }

    /** One line of how a variant departs from the standard shape: a green plus or a red minus, no figure. {@code weight} is neutral. */
    private static void diff(List<Component> out, String key, float delta) {
        if (Math.abs(delta) < 0.005f)
            return;
        boolean up = delta > 0;
        net.minecraft.ChatFormatting c = key.equals("weight") ? net.minecraft.ChatFormatting.GRAY
                : up ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED;
        out.add(Component.literal(up ? "+ " : "- ").append(Component.translatable("gui.feedback.bench.diff." + key)).withStyle(c));
    }
}
