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

import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Prep Table's third tab: one food in the work slot, cut with the knife, kneaded, or finished by pouring. The four buttons
 * open the cut, pour and knead screens over the table (or take a dish apart); the left column says what the work is.
 */
final class PrepPanel implements WorkPanel {

    private final ToolTableScreen host;
    private Button cutBtn, finishBtn, kneadBtn, takeBtn;
    /** The tooltip key each button shows now, so a tooltip is rebuilt only when the reason changes. */
    private final String[] tipKey = new String[4];

    PrepPanel(ToolTableScreen host) {
        this.host = host;
    }

    @Override
    public void init() {
        java.util.Arrays.fill(tipKey, null);
        int x = host.left() + ToolTableMenu.GRID_X, y = host.top() + ToolTableMenu.GRID_Y;
        cutBtn = host.add(GuiStyle.button(host.skin, Component.translatable("gui.feedback.prep_table.cut"), b -> prepare(0), x, y, 70, 16));
        finishBtn = host.add(GuiStyle.button(host.skin, Component.translatable("gui.feedback.prep_table.finish"), b -> prepare(1), x, y + 20, 70, 16));
        kneadBtn = host.add(GuiStyle.button(host.skin, Component.translatable("gui.feedback.prep_table.knead"), b -> prepare(2), x, y + 40, 70, 16));
        takeBtn = host.add(GuiStyle.button(host.skin, Component.translatable("gui.feedback.prep_table.apart"),
                b -> host.press(ToolTableMenu.BTN_APART), x, y + 60, 70, 16));
    }

    @Override
    public void tick() {
        ToolTableMenu menu = host.menu();
        boolean prep = menu.mode() == ToolTableMenu.MODE_PREPARE && !host.busy();
        cutBtn.visible = finishBtn.visible = kneadBtn.visible = takeBtn.visible = prep;
        takeBtn.active = prep && menu.hasHammer() && ToolTableMenu.isDish(menu.work());
        kneadBtn.active = prep && menu.preparable(host.level(), 2);
        cutBtn.active = prep && menu.preparable(host.level(), 0);
        finishBtn.active = prep && menu.preparable(host.level(), 1);
        if (!prep)
            return;
        ItemStack work = menu.work();
        tip(0, cutBtn, "cut", cutBtn.active ? "" : !menu.hasHammer() ? ".no_knife" : ".no_food");
        tip(1, finishBtn, "finish", finishBtn.active ? "" : !ToolTableMenu.isDish(work) ? ".no_dish"
                : ToolTableMenu.finished(host.level(), work) ? ".done" : ".no_seasoning");
        tip(2, kneadBtn, "knead", kneadBtn.active ? "" : ".none");
        tip(3, takeBtn, "apart", takeBtn.active ? "" : ".none");
    }

    /** A one-line tooltip: what the button does, or while it is dead, what it lacks. */
    private void tip(int i, Button b, String what, String lack) {
        String key = "gui.feedback.prep_table.tip." + what + lack;
        if (key.equals(tipKey[i]))
            return;
        tipKey[i] = key;
        b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(key)));
    }

    @Override
    public boolean owns(int mode) {
        return mode == ToolTableMenu.MODE_PREPARE;
    }

    /** Open the cut (0), the pour (1) or the knead (2) over this screen. */
    private void prepare(int kind) {
        if (!host.settled() || !host.menu().preparable(host.level(), kind))
            return;
        if (kind == 0)
            PrepCutScreen.open(host, host.menu());
        else if (kind == 2)
            PrepKneadScreen.open(host, host.menu());
        else
            PrepPourScreen.open(host, host.menu());
    }

    @Override
    public void body(GuiGraphics g, int mouseX, int mouseY) {
        host.column(g, help(), 0);
    }

    @Override
    public void labels(GuiGraphics g) {
        // Above the slots: beside them "Work" runs into the seasoning slot.
        g.drawString(host.font(), Component.translatable("gui.feedback.prep_table.work"), host.left() + ToolTableMenu.TOOL_X,
                host.top() + ToolTableMenu.TOOL_Y - 9, host.skin.dim(), false);
        g.drawString(host.font(), Component.translatable("gui.feedback.prep_table.seasoning"), host.left() + ToolTableMenu.SEASON_X,
                host.top() + ToolTableMenu.SEASON_Y - 9, host.skin.dim(), false);
    }

    private List<Component> help() {
        ItemStack work = host.menu().work();
        List<Component> out = new ArrayList<>();
        var cut = work.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.FOOD_CUT.get());
        if (ToolTableMenu.isDish(work)) {
            // A dish's finish is a part of its design; the pour it was given is on that part.
            var d = io.github.soundgoodizerfan.feedback.modular.ModularStacks.design(work);
            var type = io.github.soundgoodizerfan.feedback.modular.ModularTypes.known().get(d.type());
            var finish = type == null || type.afterRole() == null ? null : d.part(type.afterRole());
            out.add(work.getHoverName());
            out.add(finish == null ? Component.translatable("prep.feedback.finish.none")
                    : ToolTableMenu.pourName(finish.dim("pour", io.github.soundgoodizerfan.feedback.core.FTuning.POUR_AUTOMATION)));
        } else if (!work.isEmpty()) {
            out.add(work.getHoverName());
            out.add(cut == null ? Component.translatable("prep.feedback.cut.whole") : ToolTableMenu.cutName(cut));
            var knead = work.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.FOOD_KNEAD.get());
            if (knead != null)
                out.add(Component.translatable("prep.feedback.knead." + knead.grade()));
        }
        return out;
    }
}
