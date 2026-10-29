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
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.ModularModifiers;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.Station;
import io.github.soundgoodizerfan.feedback.modular.ToolTableAssembly;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.modular.ToolTableShaping;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The figures of parts and results, written once ({@code docs/gui_flow_information.md}): what the assembled parts make and which
 * role each fills, the Gun Table's spec sheet, a shape's description and the number format. Every station and the Blueprint
 * Table call here, so a figure never has two spellings; every number goes through {@link Readout}. Stateless: a panel keeps the
 * {@link Assembly} it is handed.
 */
final class PartPresenter {

    private PartPresenter() { }

    /** The left column of assemble, the role boxed on each slot (null: none) and, at the Prep Table, the hint for each empty slot. */
    record Assembly(List<Component> lines, String[] slotRole, String[] slotHint) { }

    /** The left column in assemble: what the parts make and which role each filled, or what they could become. */
    static Assembly assemble(ToolTableMenu menu, Station station, Level level, Map<ResourceLocation, Material> materials,
                             Map<ResourceLocation, ModularType> types) {
        List<Component> out = new ArrayList<>();
        String[] slotRole = new String[ToolTableAssembly.GUN_SLOTS], slotHint = new String[ToolTableAssembly.GUN_SLOTS];
        List<ItemStack> slots = menu.assembly();
        var match = ToolTableAssembly.infer(slots, menu.plan(), materials, types);
        if (station.dishes() && !types.isEmpty()) {
            ModularType type = types.values().iterator().next();
            List<String> order = type.placed();
            for (int i = 0; i < order.size() && i < slotHint.length; i++)
                slotHint[i] = roleName(type, order.get(i));
        }
        if (match.isPresent()) {
            ModularType type = types.get(match.get().type());
            out.add(Component.translatable("gui.feedback.tool_table.making", name(match.get().type())).withStyle(net.minecraft.ChatFormatting.GOLD));
            var fig = match.get().tool().get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.DISH_FIGURES.get());
            if (fig != null && fig.overflow() > 0)
                out.add(Component.translatable("gui.feedback.prep_table.overfull").withStyle(net.minecraft.ChatFormatting.RED));
            for (int i = 0; i < slots.size(); i++)
                if (match.get().role()[i] != null) {
                    slotRole[i] = Component.translatable("modular.feedback.role." + type.roles().get(match.get().role()[i]).name()).getString();
                    out.add(Component.translatable("gui.feedback.tool_table.role",
                            Component.translatable("modular.feedback.role." + type.roles().get(match.get().role()[i]).name()),
                            slots.get(i).getHoverName(), slots.get(i).getCount()));
                }
            if (station.gun())
                sheet(out, menu, level, match.get().tool(), materials);
            return new Assembly(out, slotRole, slotHint);
        }
        if (station.dishes()) {
            // The picked type's required roles that no stack in the slots can fill, said plainly.
            ModularType type = types.values().iterator().next();
            List<String> missing = new ArrayList<>();
            for (String r : type.baseForms().get(0).keySet()) {
                ModularType.Role role = type.roles().get(r);
                boolean has = slots.stream().anyMatch(st -> !st.isEmpty() && Material.find(materials, st).filter(role::accepts).isPresent()
                        && (role.part().isEmpty() || st.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get())));
                if (!has)
                    missing.add(roleName(type, r));
            }
            if (!missing.isEmpty())
                out.add(Component.translatable("gui.feedback.prep_table.needs", String.join(", ", missing)));
            return new Assembly(out, slotRole, slotHint);
        }
        List<ResourceLocation> could = ToolTableAssembly.candidates(slots, menu.plan(), materials, types);
        if (could.isEmpty() || could.size() > 4)
            return new Assembly(out, slotRole, slotHint);
        for (ResourceLocation id : could) {
            ModularType type = types.get(id);
            List<String> roles = new ArrayList<>();
            for (String r : type.order())
                roles.add(Component.translatable("modular.feedback.role." + type.roles().get(r).name()).getString());
            out.add(Component.translatable("gui.feedback.tool_table.could", name(id), String.join(", ", roles)));
        }
        return new Assembly(out, slotRole, slotHint);
    }

    /**
     * The Gun Table's spec sheet: the gun the parts make, baked against the round in the round slot -- chamber against
     * rating, speed, energy, report, kick, spread and cycle, free and exact (philosophy 8, an equipment spec). The
     * table changes no figure (guns spec 13.1); it lets the player see the interaction and which side to change.
     */
    static void sheet(List<Component> out, ToolTableMenu menu, Level level, ItemStack gun, Map<ResourceLocation, Material> materials) {
        net.minecraft.ChatFormatting dim = net.minecraft.ChatFormatting.GRAY;
        var cartridge = menu.round().get(io.github.soundgoodizerfan.feedback.registry.FWeapons.CARTRIDGE.get());
        if (menu.round().isEmpty() || cartridge == null) {
            out.add(Component.translatable("screen.feedback.gun_table.add_round").withStyle(dim));
            return;
        }
        var spec = io.github.soundgoodizerfan.feedback.weapon.GunSpec.of(gun);
        var f = spec.flatMap(g -> g.bake(materials, cartridge.at(level.getGameTime())));
        if (f.isEmpty()) {
            out.add(Component.translatable("screen.feedback.gun_table.no_fit").withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        var g = f.get();
        net.minecraft.ChatFormatting pressure = g.overpressure() > 1 ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.WHITE;
        out.add(Component.translatable("screen.feedback.gun_table.sheet").withStyle(net.minecraft.ChatFormatting.GOLD));
        out.add(Component.translatable("screen.feedback.gun_table.charge", num(g.peakPressure().value())).withStyle(pressure));
        out.add(Component.translatable("screen.feedback.gun_table.rating", num(g.chamberRating().value())));
        out.add(Component.translatable("screen.feedback.gun_table.over", num(g.overpressure())).withStyle(pressure));
        out.add(Component.translatable("screen.feedback.gun_table.speed", num(g.muzzleSpeed() * 20),
                Component.translatable(g.muzzleSpeed() * 20 >= io.github.soundgoodizerfan.feedback.core.FTuning.SOUND_MS
                        ? "screen.feedback.gun_table.supersonic" : "screen.feedback.gun_table.subsonic")));
        out.add(Component.translatable("screen.feedback.gun_table.energy", num(g.muzzleEnergy())));
        out.add(Component.translatable("screen.feedback.gun_table.report", num(g.report())));
        out.add(Component.translatable("screen.feedback.gun_table.kick", num(g.kickDeg())));
        out.add(Component.translatable("screen.feedback.gun_table.spread", num(g.spreadDeg())));
        out.add(Component.translatable("screen.feedback.gun_table.cycle", num(g.cycleMs())));
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
        for (ModularModifiers.Modifier m : ModularModifiers.forPart(o))
            if (m.kind() == ModularModifiers.Kind.TRAIT)
                lines.add(m.line());
        return lines;
    }
}
