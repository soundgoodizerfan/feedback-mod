// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.screen;

import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkedPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerMenu;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The Programmer's screen: a full-window {@link ProgramEditor} under a strip along the top that holds the card slot and the player's
 * hotbar. Everything about editing a program lives in the editor, which the Computer's desktop hosts in a window too; this class is the
 * strip, the input gating of a {@link SkinnedScreen}, and the commit on close.
 */
public class ProgrammerScreen extends SkinnedScreen<ProgrammerMenu> {

    /** Height of the top strip; must clear the slot row at {@link ProgrammerMenu#STRIP_SLOT_Y}. */
    private static final int STRIP_H = 28;

    private final ProgramEditor editor;

    public ProgrammerScreen(ProgrammerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GuiSkin.CONSOLE);
        editor = new ProgramEditor(new ProgramEditor.Source() {
            @Override
            public BlockPos pos() {
                return menu.getPos();
            }

            @Override
            public ProgrammerBlockEntity programmer() {
                return menu.getProgrammer();
            }

            @Override
            public ItemStack medium() {
                return menu.getSlot(0).getItem();
            }
        }, GuiSkin.CONSOLE);
    }

    @Override
    protected void init() {
        imageWidth = width;
        imageHeight = height;
        super.init();
        editor.build();
        editor.viewport(0, STRIP_H, width, height - STRIP_H);
    }

    public void applyLink(ProgrammerLinkedPayload payload) {
        editor.applyLink(payload);
    }

    /** Commit before closing, not in {@link #removed}: the server only takes a commit from a player whose menu is still this one. */
    @Override
    public void onClose() {
        editor.flush();
        super.onClose();
    }

    /** The server closed the menu (out of reach, block broken): an unsent edit is lost -- say so. */
    @Override
    public void removed() {
        editor.lost();
        super.removed();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        editor.tick();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mx, int my) {
        // Title and inventory label have no place in the strip layout.
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mx, int my) {
        editor.render(graphics, mx, my, partialTick);
        // The strip is the instrument panel's plate, its edge screws pushed off the window's corners.
        GuiSkin.CONSOLE.panel(graphics, -4, -4, width + 8, STRIP_H + 4);
        for (var slot : menu.slots)
            GuiSkin.CONSOLE.slot(graphics, slot.x, slot.y);
        graphics.drawString(font, title, ProgrammerMenu.HOTBAR_X + 9 * 18 + 12, 10, GuiSkin.CONSOLE.dim(), false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!settled())
            return true;
        // The strip holds real slots; AbstractContainerScreen#mouseClicked claims any click inside the whole window, so it only gets the strip.
        return my < STRIP_H ? super.mouseClicked(mx, my, button) : editor.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (!settled())
            return true;
        return editor.mouseDragged(mx, my, button, dragX, dragY) || super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (!settled())
            return true; // a drag let go while closing must not land an edit after the commit
        return editor.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (!settled())
            return true;
        return editor.mouseScrolled(mx, my, scrollY) || super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        // While typing, every key is the box's: the inventory key must not close the screen.
        return closing() || editor.keyPressed(key, scanCode, modifiers) || super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return closing() || editor.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    /** The whole window is the panel, so a status goes on the editor's own status line rather than a strip under it. */
    @Override
    protected void say(Component text) {
        editor.setStatus(text);
    }
}
