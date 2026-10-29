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

import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ToolTableAssembly;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Assembly at the Tool Table, Gun Table and Prep Table: parts in the slots, named by what they make. Owns the left column
 * ({@link #lines}: what the parts make and which role each filled, or what they could become), the role boxed on each slot
 * ({@link #slotRole}, {@link #slotHint}), the Gun Table's spec sheet and the Prep Table's dish type picker.
 */
final class AssemblePanel implements WorkPanel {

    private final ToolTableScreen host;
    private AbstractWidget typePicker;
    private List<Component> lines = List.of();
    /** For each of the six slots, the role its part fills in the tool the parts make, or null. */
    private final String[] slotRole = new String[ToolTableAssembly.GUN_SLOTS];
    /** At the Prep Table: the role the picked dish type has for each slot, shown on empty slots as a hint. */
    private final String[] slotHint = new String[ToolTableAssembly.GUN_SLOTS];

    AssemblePanel(ToolTableScreen host) {
        this.host = host;
    }

    @Override
    public void init() {
        if (!host.station().dishes())
            return;
        typePicker = host.add(GuiStyle.stepper(host.skin, host.left() + ToolTableScreen.LIST_X - 2, host.top() + ToolTableScreen.LIST_Y - 2,
                ToolTableScreen.LIST_W + 4, 14, "<", ">", () -> typeName(), d -> {
                    int n = Math.max(1, host.menu().dishTypes(host.level()).size());
                    host.press(ToolTableMenu.BTN_TYPE + Math.floorMod(host.menu().typePicked(host.level()) + d, n));
                }, true, null));
        typePicker.visible = false;
    }

    @Override
    public void tick() {
        if (typePicker != null)
            typePicker.visible = host.menu().mode() == ToolTableMenu.MODE_ASSEMBLE;
    }

    @Override
    public boolean owns(int mode) {
        return mode == ToolTableMenu.MODE_ASSEMBLE;
    }

    @Override
    public void read(net.minecraft.world.level.Level level, Map<ResourceLocation, Material> materials) {
        lines = host.menu().mode() == ToolTableMenu.MODE_ASSEMBLE ? assembleLines(materials, host.menu().assemblyTypes(level)) : List.of();
    }

    @Override
    public void body(GuiGraphics g, int mouseX, int mouseY) {
        host.column(g, lines, host.station().dishes() ? 18 : 0);
        slotGroups(g);
    }

    @Override
    public int statusY() {
        return host.station().dishes() ? ToolTableMenu.ASM_ROW_B + 22 : host.station().planY() + 20;
    }

    @Override
    public void labels(GuiGraphics g) {
        if (!host.station().dishes())
            g.drawString(host.font(), Component.translatable("gui.feedback.tool_table.plan"), host.left() + ToolTableMenu.PLAN_X + 20,
                    host.top() + host.station().planY() + 4, host.skin.dim(), false);
        if (host.station().gun())
            g.drawString(host.font(), Component.translatable("gui.feedback.gun_table.round"), host.left() + ToolTableMenu.GUN_ROUND_X + 20,
                    host.top() + host.station().planY() + 4, host.skin.dim(), false);
    }

    /** The picked dish type's name, for the picker. */
    private String typeName() {
        var level = host.level();
        List<ResourceLocation> types = level == null ? List.of() : host.menu().dishTypes(level);
        return types.isEmpty() ? "" : name(types.get(host.menu().typePicked(level))).getString();
    }

    /** The left column in assemble: what the parts make and which role each filled, or what they could become. */
    private List<Component> assembleLines(Map<ResourceLocation, Material> materials, Map<ResourceLocation, ModularType> types) {
        List<Component> out = new ArrayList<>();
        List<ItemStack> slots = host.menu().assembly();
        var match = ToolTableAssembly.infer(slots, host.menu().plan(), materials, types);
        java.util.Arrays.fill(slotRole, null);
        java.util.Arrays.fill(slotHint, null);
        if (host.station().dishes() && !types.isEmpty()) {
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
            if (host.station().gun())
                sheet(out, match.get().tool(), materials);
            return out;
        }
        if (host.station().dishes()) {
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
            return out;
        }
        List<ResourceLocation> could = ToolTableAssembly.candidates(slots, host.menu().plan(), materials, types);
        if (could.isEmpty() || could.size() > 4)
            return out;
        for (ResourceLocation id : could) {
            ModularType type = types.get(id);
            List<String> roles = new ArrayList<>();
            for (String r : type.order())
                roles.add(Component.translatable("modular.feedback.role." + type.roles().get(r).name()).getString());
            out.add(Component.translatable("gui.feedback.tool_table.could", name(id), String.join(", ", roles)));
        }
        return out;
    }

    /**
     * The Gun Table's spec sheet: the gun the parts make, baked against the round in the round slot -- chamber against
     * rating, speed, energy, report, kick, spread and cycle, free and exact (philosophy 8, an equipment spec). The
     * table changes no figure (guns spec 13.1); it lets the player see the interaction and which side to change.
     */
    private void sheet(List<Component> out, ItemStack gun, Map<ResourceLocation, Material> materials) {
        net.minecraft.ChatFormatting dim = net.minecraft.ChatFormatting.GRAY;
        var cartridge = host.menu().round().get(io.github.soundgoodizerfan.feedback.registry.FWeapons.CARTRIDGE.get());
        if (host.menu().round().isEmpty() || cartridge == null) {
            out.add(Component.translatable("screen.feedback.gun_table.add_round").withStyle(dim));
            return;
        }
        var spec = io.github.soundgoodizerfan.feedback.weapon.GunSpec.of(gun);
        var f = spec.flatMap(g -> g.bake(materials, cartridge.at(host.level().getGameTime())));
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

    private static String num(float value) {
        return Readout.figure(io.github.soundgoodizerfan.feedback.instrument.Quantity.FIGURE, value);
    }

    private static String roleName(ModularType type, String role) {
        return Component.translatable("modular.feedback.role." + type.roles().get(role).name()).getString();
    }

    private static Component name(ResourceLocation type) {
        return BuiltInRegistries.ITEM.get(type).getDescription();
    }


    /**
     * The six slots, each boxed and labelled by the role its part fills in the tool the parts make;
     * a part the parts do not use says "?". An empty slot is unlabelled: it takes anything, and the
     * left column names what the parts could still become.
     */
    private void slotGroups(GuiGraphics g) {
        for (int i = 0; i < (host.station().gun() ? ToolTableAssembly.GUN_SLOTS : ToolTableAssembly.SLOTS); i++) {
            int x = host.left() + (host.station().gun() ? ToolTableMenu.gunAsmX(i) : ToolTableMenu.asmX(i)) - 1,
                    y = host.top() + (host.station().gun() ? ToolTableMenu.gunAsmY(i) : ToolTableMenu.asmY(i)) - 1;
            g.renderOutline(x, y, 18, 18, host.skin.dim() & 0x60FFFFFF | 0x40000000);
            boolean filled = host.menu().getSlot(i < ToolTableAssembly.SLOTS ? ToolTableMenu.ASM + i : ToolTableMenu.EXTRA + i - ToolTableAssembly.SLOTS).hasItem();
            if (!filled && slotHint[i] == null)
                continue;
            String label = filled ? slotRole[i] == null ? "?" : slotRole[i] : slotHint[i];
            g.pose().pushPose();
            g.pose().translate(x + 1, y - 6, 0);
            g.pose().scale(0.5f, 0.5f, 1f);
            g.drawString(host.font(), host.font().plainSubstrByWidth(label, 34), 0, 0, filled && slotRole[i] == null ? host.skin.warn() : host.skin.dim(), false);
            g.pose().popPose();
        }
    }
}
