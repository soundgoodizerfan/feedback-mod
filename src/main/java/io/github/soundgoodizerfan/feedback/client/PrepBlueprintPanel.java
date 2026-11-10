// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.net.ModularBlueprintPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Prep Table's Blueprint tab: the Blueprint Table's drafting board ({@link BlueprintBoard}) over the bench, restricted to food
 * ({@link BlueprintBoard.Host#foodOnly}), with a Print button and the paper slot the menu keeps. The board and the print path are the
 * Blueprint Table's; this class is only the seat the {@link BenchScreen} gives them while the tab is open.
 */
final class PrepBlueprintPanel implements BlueprintBoard.Host {

    private final ToolTableMenu menu;
    private final GuiSkin skin;
    private final BlueprintBoard board;
    private long stampAt;

    PrepBlueprintPanel(ToolTableMenu menu, GuiSkin skin) {
        this.menu = menu;
        this.skin = skin;
        board = new BlueprintBoard(this, skin);
        board.resize(ToolTableMenu.W, ToolTableMenu.BOARD_H);
    }

    boolean active() {
        return menu.blueprintTab();
    }

    /** The Print button, placed on the bench at ({@code left}, {@code top}); the screen adds it. */
    Button printButton(int left, int top) {
        board.origin(left, top);
        board.build();
        return GuiStyle.button(skin, Component.translatable("gui.feedback.blueprint_table.print"), b -> {
            var drawing = board.drawing();
            if (drawing.isPresent() && !menu.paper().isEmpty()) {
                skin.cue(GuiSkin.Cue.FINISH);
                stampAt = System.currentTimeMillis();
                PacketDistributor.sendToServer(new ModularBlueprintPayload(drawing.get(), board.pinnedDesigns()));
            } else
                skin.cue(GuiSkin.Cue.REJECT);
        }, left + ToolTableMenu.PAPER_X + 20, top + ToolTableMenu.PAPER_Y, 38, 16);
    }

    void tick() {
        board.openPaper(menu.paper());
    }

    void draw(GuiGraphics g, int left, int top, int mx, int my, float partial) {
        board.origin(left, top);
        board.drawBoard(g, mx, my, partial);
    }

    /** Over everything: the type picker, and the stamp that lands when Print is pressed. */
    void overlay(GuiGraphics g, int mx, int my) {
        board.drawOverlay(g, mx, my);
        if (stampAt > 0)
            GuiStyle.stamp(g, Component.translatable("message.feedback.blueprint_table.printed"), board.centreXAbs(), board.centreYAbs(),
                    System.currentTimeMillis() - stampAt, skin.accent() & 0xFFFFFF);
    }

    boolean popupActive() {
        return board.popupActive();
    }

    void tooltip(GuiGraphics g, int mx, int my) {
        board.tooltip(g, mx, my);
    }

    boolean mouseClicked(double mx, double my, int button) {
        return board.mouseClicked(mx, my, button);
    }

    boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return board.mouseDragged(mx, my, button, dx, dy);
    }

    boolean mouseReleased(double mx, double my, int button) {
        return board.mouseReleased(mx, my, button);
    }

    boolean mouseScrolled(double mx, double my, double scroll) {
        return board.mouseScrolled(mx, my, scroll);
    }

    boolean keyPressed(int key, int scan, int modifiers) {
        return board.keyPressed(key, scan, modifiers);
    }

    boolean charTyped(char c, int modifiers) {
        return board.charTyped(c, modifiers);
    }

    // --- BlueprintBoard.Host ------------------------------------------------------------------

    @Override
    public ItemStack carried() {
        return menu.getCarried();
    }

    @Override
    public void selectionChanged() {
    }

    @Override
    public void sampleWanted(boolean wanted) {
    }

    @Override
    public boolean foodOnly() {
        return true;
    }
}
