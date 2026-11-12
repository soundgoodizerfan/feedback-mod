// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringAssembly;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringRework;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The Engineering Table: four folder tabs (Crafting, Fit, Repair, Treatise) over one panel. The menu owns the slots and which are live; this
 * draws a socket at each live one, the labels, the Crafting tab's arrow and one-line assembly status, and tooltips on empty tool and fastener
 * slots saying what they take. The Fit, Repair and Treatise tabs show their slots and buttons; the server decides what a button does and the menu's note says what it did.
 */
public class EngineeringTableScreen extends SkinnedScreen<EngineeringTableMenu> {

    private static final GuiSkin SKIN = GuiSkin.CONSOLE;
    /** Status text box (relative to the panel): under the arrow, right of the grid. */
    private static final int STATUS_X = 92, STATUS_Y = 62, STATUS_W = 116;

    private Button apply, repair, swap, apart, learn;

    public EngineeringTableScreen(EngineeringTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SKIN);
        imageWidth = EngineeringTableMenu.W;
        imageHeight = EngineeringTableMenu.H;
        titleLabelX = 10;
        titleLabelY = 5;
        inventoryLabelX = EngineeringTableMenu.INV_X;
        inventoryLabelY = EngineeringTableMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        folderTabs(List.of(tab("crafting"), tab("fit"), tab("repair"), tab("treatise")), menu::tab,
                i -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, EngineeringTableMenu.BTN_TAB + i));
        apply = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.engineering.apply"),
                b -> press(EngineeringTableMenu.BTN_FIT_APPLY), leftPos + 22, topPos + 62, 52, 16, null));
        repair = action("repair", EngineeringTableMenu.BTN_REPAIR, 18);
        swap = action("swap", EngineeringTableMenu.BTN_SWAP, 36);
        apart = action("apart", EngineeringTableMenu.BTN_APART, 54);
        learn = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.engineering.learn"),
                b -> press(EngineeringTableMenu.BTN_LEARN), leftPos + (imageWidth - 80) / 2, topPos + 68, 80, 16, null));
    }

    private Button action(String key, int id, int y) {
        return addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.engineering." + key), b -> press(id),
                leftPos + 32, topPos + y, 60, 16));
    }

    /** Asks the server to do a tab's action; it decides and answers through the menu's note. */
    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private static FolderTabs.Def tab(String key) {
        return new FolderTabs.Def(Component.translatable("gui.feedback.engineering.tab." + key));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        apply.visible = menu.tab() == EngineeringTableMenu.TAB_FIT;
        repair.visible = swap.visible = apart.visible = menu.tab() == EngineeringTableMenu.TAB_REPAIR;
        learn.visible = menu.tab() == EngineeringTableMenu.TAB_TREATISE;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        // The popout first, so the panel covers its seam: hand tools and fixings, a desk's drawer rather than more of the grid.
        skin.panel(g, leftPos + imageWidth - 4, topPos + 14, EngineeringTableMenu.POP_W + 4, 170);
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots)
            if (s.index < EngineeringTableMenu.INV && s.isActive())
                skin.slot(g, leftPos + s.x, topPos + s.y);
        inventory(g, EngineeringTableMenu.INV_X, EngineeringTableMenu.INV_Y);
        if (menu.tab() == EngineeringTableMenu.TAB_CRAFT)
            arrow(g, leftPos + 98, topPos + 41);
    }

    /** A right-pointing arrow, 30 wide, its shaft centred on y + 4. */
    private void arrow(GuiGraphics g, int x, int y) {
        int c = skin.dim() | 0xFF000000;
        g.fill(x, y + 3, x + 24, y + 5, c);
        for (int i = 0; i < 5; i++)
            g.fill(x + 24 + i, y + 4 - (4 - i), x + 25 + i, y + 4 + (4 - i) + 1, c);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        int tab = menu.tab();
        Component head = Component.translatable("gui.feedback.engineering.tab." + switch (tab) {
            case EngineeringTableMenu.TAB_FIT -> "fit";
            case EngineeringTableMenu.TAB_REPAIR -> "repair";
            case EngineeringTableMenu.TAB_TREATISE -> "treatise";
            default -> "crafting";
        });
        g.drawString(font, title, titleLabelX, titleLabelY, skin.ink(), false);
        g.drawString(font, head, imageWidth - 10 - font.width(head), titleLabelY, skin.dim(), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, skin.dim(), false);
        popLabel(g, "tools", EngineeringTableMenu.TOOL_Y - 11);
        popLabel(g, "fasteners", EngineeringTableMenu.FASTENER_Y - 11);
        if (tab != EngineeringTableMenu.TAB_CRAFT)
            noteLine(g);
        switch (tab) {
            case EngineeringTableMenu.TAB_CRAFT -> {
                Component t = Component.translatable("gui.feedback.engineering.treatises");
                g.drawString(font, t, EngineeringTableMenu.BINDER_X + 16 - font.width(t), EngineeringTableMenu.BINDER_Y - 12, skin.dim(), false);
                Component s = statusText();
                g.drawString(font, font.plainSubstrByWidth(s.getString(), STATUS_W, false).equals(s.getString()) ? s.getString()
                        : font.plainSubstrByWidth(s.getString(), STATUS_W - font.width("...")) + "...", STATUS_X, STATUS_Y, statusColour(), false);
            }
            case EngineeringTableMenu.TAB_FIT -> {
                label(g, "item", EngineeringTableMenu.FIT_ITEM_X, EngineeringTableMenu.FIT_ITEM_Y - 12);
                gridLabel(g, "attachments", EngineeringTableMenu.ATTACH_X);
            }
            case EngineeringTableMenu.TAB_REPAIR -> {
                label(g, "item", EngineeringTableMenu.REPAIR_ITEM_X, EngineeringTableMenu.REPAIR_ITEM_Y - 12);
                gridLabel(g, "stock", EngineeringTableMenu.STOCK_X);
            }
            default -> {
                label(g, "binder", EngineeringTableMenu.BINDER_TREATISE_X, EngineeringTableMenu.TREATISE_Y - 12);
                label(g, "blank", EngineeringTableMenu.BLANK_X, EngineeringTableMenu.TREATISE_Y - 12);
                label(g, "specimen", EngineeringTableMenu.SPECIMEN_X, EngineeringTableMenu.TREATISE_Y - 12);
            }
        }
    }

    /** The last Fit, Repair or Treatise action's answer, in the strip above the tool row. */
    private void noteLine(GuiGraphics g) {
        Component t = noteText();
        if (t.getString().isEmpty())
            return;
        int x = 10, w = imageWidth - 20;
        String text = font.plainSubstrByWidth(t.getString(), w, false).equals(t.getString()) ? t.getString()
                : font.plainSubstrByWidth(t.getString(), w - font.width("...")) + "...";
        int ok = menu.note();
        boolean good = ok == EngineeringRework.REPAIRED || ok == EngineeringRework.SWAPPED || ok == EngineeringRework.TAKEN_APART
                || ok == EngineeringRework.FITTED || ok == EngineeringRework.WRITTEN;
        g.drawString(font, text, x, 97, (good ? skin.accent() : skin.warn()) | 0xFF000000, false);
    }

    private Component noteText() {
        String[] names = {"", "no_tool", "broken_tool", "not_modular", "nothing", "repaired", "swapped", "taken_apart", "fitted", "does_not_fit",
                "school", "no_school", "no_blank", "no_specimen", "written", "dish"};
        int n = menu.note();
        if (n <= 0 || n >= names.length)
            return Component.empty();
        if (n == EngineeringRework.SCHOOL && !menu.noteVolumeKey().isEmpty())
            return Component.translatable("gui.feedback.engineering.note.school_named", Component.translatable(menu.noteVolumeKey()));
        return Component.translatable("gui.feedback.engineering.note." + names[n]);
    }

    /** A 4x4 grid's caption: right-aligned to the grid's edge, so it clears the title that fills the row above the grid's left end. */
    private void gridLabel(GuiGraphics g, String key, int gridX) {
        Component t = Component.translatable("gui.feedback.engineering.label." + key);
        g.drawString(font, t, gridX + 4 * 18 + 4 - font.width(t), EngineeringTableMenu.ATTACH_Y - 9, skin.dim(), false);
    }

    /** A caption for a popout slot group, small enough for the popout's width. */
    private void popLabel(GuiGraphics g, String key, int y) {
        Component t = Component.translatable("gui.feedback.engineering.label." + key);
        g.pose().pushPose();
        g.pose().translate(EngineeringTableMenu.TOOL_X - 2, y, 0);
        g.pose().scale(0.8f, 0.8f, 1f);
        g.drawString(font, t, 0, 0, skin.dim(), false);
        g.pose().popPose();
    }

    /** The popout is part of the table: a click on it is not a click outside. */
    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        boolean inPopout = mx >= left + imageWidth && mx < left + imageWidth + EngineeringTableMenu.POP_W && my >= top + 14 && my < top + 184;
        return !inPopout && super.hasClickedOutside(mx, my, left, top, button);
    }

    private void label(GuiGraphics g, String key, int x, int y) {
        g.drawString(font, Component.translatable("gui.feedback.engineering.label." + key), x, y, skin.dim(), false);
    }

    private Component statusText() {
        return switch (menu.status()) {
            case EngineeringAssembly.OK -> Component.translatable("gui.feedback.engineering.status.ok");
            case EngineeringAssembly.SCHOOL -> menu.missingVolumeKey().isEmpty() ? Component.translatable("gui.feedback.engineering.status.school")
                    : Component.translatable("gui.feedback.engineering.status.school_named", Component.translatable(menu.missingVolumeKey()));
            case EngineeringAssembly.NO_TOOL -> Component.translatable("gui.feedback.engineering.status.no_tool");
            case EngineeringAssembly.BROKEN_TOOL -> Component.translatable("gui.feedback.engineering.status.broken_tool");
            case EngineeringAssembly.HAMMER_TOO_WEAK -> Component.translatable("gui.feedback.engineering.status.hammer_weak");
            case EngineeringAssembly.NO_FASTENERS -> Component.translatable("gui.feedback.engineering.status.no_fasteners");
            default -> Component.empty();
        };
    }

    private int statusColour() {
        return (menu.status() == EngineeringAssembly.OK ? skin.accent() : menu.status() == EngineeringAssembly.NOTHING ? skin.dim() : skin.warn())
                | 0xFF000000;
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.isActive() && !hoveredSlot.hasItem()
                && ("gui.feedback.engineering.tip.tool".equals(emptyTip(hoveredSlot.index))
                        || "gui.feedback.engineering.tip.blank".equals(emptyTip(hoveredSlot.index)))) {
            g.renderTooltip(font, Component.translatable(emptyTip(hoveredSlot.index)), mx, my);
            return;
        }
        if (menu.getCarried().isEmpty() && menu.tab() == EngineeringTableMenu.TAB_CRAFT) {
            if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.isActive()) {
                String key = emptyTip(hoveredSlot.index);
                if (key != null) {
                    g.renderTooltip(font, Component.translatable(key), mx, my);
                    return;
                }
            }
            int x = mx - leftPos, y = my - topPos;
            if (x >= STATUS_X && x < STATUS_X + STATUS_W && y >= STATUS_Y - 1 && y < STATUS_Y + 10 && !statusText().getString().isEmpty()) {
                g.renderTooltip(font, statusText(), mx, my);
                return;
            }
        }
        super.renderTooltip(g, mx, my);
    }

    /** What an empty slot takes, for the slots that are not self-evident. */
    private static String emptyTip(int menuSlot) {
        int c = menuSlot - 1;
        if (c >= EngineeringTableBlockEntity.TOOLS && c < EngineeringTableBlockEntity.TOOLS + 4)
            return "gui.feedback.engineering.tip.tool";
        if (c >= EngineeringTableBlockEntity.FASTENERS && c < EngineeringTableBlockEntity.FASTENERS + 3)
            return "gui.feedback.engineering.tip.fastener";
        if (c == EngineeringTableBlockEntity.BLUEPRINT)
            return "gui.feedback.engineering.tip.blueprint";
        if (c == EngineeringTableBlockEntity.BINDER)
            return "gui.feedback.engineering.tip.binder";
        if (c == EngineeringTableBlockEntity.BLANK)
            return "gui.feedback.engineering.tip.blank";
        return null;
    }
}
