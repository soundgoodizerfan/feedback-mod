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

import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.ToolTableAssembly;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
    private String[] slotRole = new String[ToolTableAssembly.GUN_SLOTS];
    /** At the Prep Table: the role the picked dish type has for each slot, shown on empty slots as a hint. */
    private String[] slotHint = new String[ToolTableAssembly.GUN_SLOTS];

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
        if (host.menu().mode() != ToolTableMenu.MODE_ASSEMBLE) {
            lines = List.of();
            return;
        }
        var made = PartPresenter.assemble(host.menu(), host.station(), level, materials, host.menu().assemblyTypes(level));
        lines = made.lines();
        slotRole = made.slotRole();
        slotHint = made.slotHint();
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
        return types.isEmpty() ? "" : PartPresenter.name(types.get(host.menu().typePicked(level))).getString();
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
