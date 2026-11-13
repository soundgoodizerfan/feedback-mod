// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintTableMenu;
import io.github.soundgoodizerfan.feedback.net.ModularBlueprintPayload;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Blueprint Table: the drafting board ({@link BlueprintBoard}, which the Computer's Blueprint app shows too) on a panel with the
 * table's own slots: the paper (a blueprint put in opens on the sheet, Print writes one), one sample slot that sets the selected part's
 * material, and the hotbar. Everything about drawing a design is in the board; this class is the panel, the slots, the Print button and
 * the stamp that lands when it is pressed.
 */
public class BlueprintTableScreen extends SkinnedScreen<BlueprintTableMenu> implements BlueprintBoard.Host {

    private final BlueprintBoard board;
    private long stampAt;
    private net.minecraft.client.gui.components.EditBox titleBox;
    /** What the title field holds, kept across a resize. */
    private String title = "";
    private ItemStack titledPaper = ItemStack.EMPTY;

    @Override
    public int schools() {
        return menu.schools();
    }

    public BlueprintTableScreen(BlueprintTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.BLUEPRINT);
        imageWidth = BlueprintTableMenu.W;
        imageHeight = BlueprintTableMenu.H;
        inventoryLabelY = 10_000; // only the hotbar is shown
        titleLabelY = 10_000; // the type's name is drawn on the canvas
        board = new BlueprintBoard(this, skin);
    }

    @Override
    protected void init() {
        super.init();
        board.origin(leftPos, topPos);
        board.build();
        titleBox = new net.minecraft.client.gui.components.EditBox(font, leftPos + BlueprintTableMenu.TITLE_X + 3, topPos + BlueprintTableMenu.TITLE_Y + 3,
                BlueprintTableMenu.TITLE_W - 6, 9, Component.translatable("gui.feedback.blueprint_table.title"));
        titleBox.setBordered(false);
        titleBox.setMaxLength(io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintCredit.MAX_TITLE);
        titleBox.setTextColor(skin.ink());
        titleBox.setHint(Component.translatable("gui.feedback.blueprint_table.title").withColor(skin.dim()));
        titleBox.setValue(title);
        titleBox.setResponder(t -> title = t);
        addRenderableWidget(titleBox);
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.blueprint_table.print"), b -> {
            var drawing = board.drawing();
            if (drawing.isPresent() && menu.getSlot(0).hasItem()) {
                skin.cue(GuiSkin.Cue.FINISH); // the stamp
                stampAt = System.currentTimeMillis();
                PacketDistributor.sendToServer(new ModularBlueprintPayload(drawing.get(), board.pinnedDesigns(), title));
            } else
                skin.cue(GuiSkin.Cue.REJECT);
        }, leftPos + BlueprintTableMenu.PAPER_X + 20, topPos + BlueprintTableMenu.PAPER_Y, 38, 16));
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.blueprint_table.copy"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BlueprintTableMenu.BTN_COPY),
                leftPos + BlueprintTableMenu.SOURCE_X + 20, topPos + BlueprintTableMenu.SOURCE_Y, 30, 16));
    }

    // --- BlueprintBoard.Host ------------------------------------------------------------------

    @Override
    public ItemStack carried() {
        return menu.getCarried();
    }

    @Override
    public void selectionChanged() {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BlueprintTableMenu.BTN_CLEAR);
    }

    @Override
    public void sampleWanted(boolean wanted) {
        menu.shown(wanted ? List.of(true) : List.of());
    }

    // --- drawing and input --------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        board.applySample(menu.sample(0));
        ItemStack paper = menu.getSlot(0).getItem();
        board.openPaper(paper);
        // a blueprint put in brings its title into the field to be edited
        if (!ItemStack.isSameItemSameComponents(paper, titledPaper)) {
            titledPaper = paper.copy();
            var credit = io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintCredit.of(paper);
            if (credit != null && titleBox != null)
                titleBox.setValue(credit.title());
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + BlueprintTableMenu.PAPER_X, topPos + BlueprintTableMenu.PAPER_Y);
        skin.slot(g, leftPos + BlueprintTableMenu.SOURCE_X, topPos + BlueprintTableMenu.SOURCE_Y);
        for (int i = 0; i < 9; i++)
            skin.slot(g, leftPos + BlueprintTableMenu.HOTBAR_X + i * 18, topPos + BlueprintTableMenu.HOTBAR_Y);
        if (board.hasSelection())
            skin.slot(g, leftPos + BlueprintTableMenu.SAMPLE_X, topPos + BlueprintTableMenu.SAMPLE_Y);
        skin.inset(g, leftPos + BlueprintTableMenu.TITLE_X, topPos + BlueprintTableMenu.TITLE_Y, BlueprintTableMenu.TITLE_W, 15);
        board.origin(leftPos, topPos);
        board.drawBoard(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        board.drawOverlay(g, mouseX, mouseY);
        if (stampAt > 0)
            GuiStyle.stamp(g, Component.translatable("message.feedback.blueprint_table.printed"), board.centreXAbs(), board.centreYAbs(),
                    System.currentTimeMillis() - stampAt, skin.accent() & 0xFFFFFF);
    }

    /** Hovering says what a thing is; the paper and sample slots say what they are for while empty. */
    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (board.popupActive())
            return;
        super.renderTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.hasItem())
            return;
        if (in(mouseX, mouseY, leftPos + BlueprintTableMenu.SAMPLE_X - 1, topPos + BlueprintTableMenu.SAMPLE_Y - 1) && board.hasSelection() && menu.sample(0).isEmpty()) {
            g.renderTooltip(font, Component.translatable("gui.feedback.blueprint_table.sample"), mouseX, mouseY);
            return;
        }
        if (in(mouseX, mouseY, leftPos + BlueprintTableMenu.PAPER_X - 1, topPos + BlueprintTableMenu.PAPER_Y - 1) && !menu.getSlot(0).hasItem()) {
            g.renderTooltip(font, Component.translatable("gui.feedback.blueprint_table.paper"), mouseX, mouseY);
            return;
        }
        board.tooltip(g, mouseX, mouseY);
    }

    private static boolean in(int mx, int my, int x, int y) {
        return mx >= x && mx < x + 18 && my >= y && my < y + 18;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return board.keyPressed(key, scan, modifiers) || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return board.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!settled())
            return true;
        return board.mouseClicked(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return settled() && board.mouseDragged(mx, my, button, dx, dy) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        board.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        return settled() && board.mouseScrolled(mx, my, scrollY) || super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
