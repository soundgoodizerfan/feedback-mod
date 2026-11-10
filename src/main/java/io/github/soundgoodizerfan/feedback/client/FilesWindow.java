// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;

import io.github.soundgoodizerfan.feedback.client.desktop.ContextMenu;
import io.github.soundgoodizerfan.feedback.client.desktop.DesktopWindow;
import io.github.soundgoodizerfan.feedback.client.desktop.Glyph;
import io.github.soundgoodizerfan.feedback.client.desktop.Ui;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerApps;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBays;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Files app: the drives as a list, the way a file manager shows its volumes. Click a row to pick a drive (the Drive window follows),
 * double-click to open it, right-click for its menu. With the Library disk in, a program can be dragged onto another drive's row to copy
 * it onto a blank Floppy or USB drive, and onto the trash to erase it (asked first); without it, Copy and drag are off.
 */
final class FilesWindow extends DesktopWindow {

    private static final int RUN = ComputerBays.SIZE, ROW = 16, TRASH_W = 42, TRASH_H = 42, DRAG = 3;
    private static final long DOUBLE_CLICK_MS = 350;

    private final ComputerContext ctx;
    private Button open, edit, run, eject;
    private int pressed = -1, dragFrom = -1;
    private boolean dragging;
    private double pressX, pressY, dragX, dragY;
    private int lastRow = -1;
    private long lastClick;

    FilesWindow(ComputerContext ctx) {
        super(14, 8, 196, 126, 170, 118, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.files");
    }

    @Override
    public Glyph glyph() {
        return Glyph.FILES;
    }

    @Override
    protected void build(GuiSkin skin) {
        open = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.open"), b -> {
            ctx.showDrive(ctx.drive());
            desktop().open("drive");
        }, 0, 0, 40, 14), 0, 0);
        edit = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.edit"), b -> {
            ctx.send(ComputerActionPayload.EDIT, ctx.drive());
            desktop().open("editor");
        }, 0, 0, 40, 14), 0, 0);
        run = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.run"),
                b -> ctx.send(ctx.drive() == RUN ? ComputerActionPayload.STOP : ComputerActionPayload.RUN, ctx.drive()), 0, 0, 40, 14), 0, 0);
        eject = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.eject"),
                b -> ctx.send(ComputerActionPayload.EJECT, ctx.drive()), 0, 0, 40, 14), 0, 0);
    }

    @Override
    protected void layout(int cw, int ch) {
        Button[] bar = {open, edit, run, eject};
        java.util.List<Layout.Rect> cells = new Layout.Rect(0, 0, cw, ch).bottom(14).columns(bar.length, 3);
        for (int i = 0; i < bar.length; i++) {
            bar[i].setWidth(cells.get(i).w());
            place(bar[i], cells.get(i).x(), cells.get(i).y());
        }
    }

    private static ProgramGraph graphOf(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
    }

    private int rowAt(double lx, double ly, int cw) {
        if (lx < 0 || lx >= cw - TRASH_W - 4 || ly < 0)
            return -1;
        int row = (int) (ly / ROW);
        return row <= RUN ? row : -1;
    }

    private boolean onTrash(double lx, double ly, int cw) {
        return lx >= cw - TRASH_W && lx < cw && ly >= 0 && ly < TRASH_H;
    }

    /** A program that can be dragged: written on a medium in a bay, with the Library disk in. */
    private boolean draggable(int drive) {
        if (drive < 0 || drive >= RUN || !ctx.installed("library"))
            return false;
        ItemStack stack = ctx.menu().getSlot(drive).getItem();
        return !stack.isEmpty() && !ComputerApps.isApp(stack) && !graphOf(stack).nodes().isEmpty();
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        int cw = ui.w(), ch = ui.h();
        int sel = Math.min(ctx.drive(), RUN);
        boolean editing = ctx.computer() != null && ctx.computer().editing();
        int overRow = dragging ? rowAt(mx, my, cw) : -1;
        int rowW = cw - TRASH_W - 4;

        for (int i = 0; i <= RUN; i++) {
            ItemStack stack = ctx.menu().getSlot(i).getItem();
            int y = i * ROW;
            boolean target = dragging && i == overRow && i < RUN && i != dragFrom;
            ui.plaque(0, y, rowW, ROW - 1);
            ui.row(0, y, rowW, ROW - 1, sel == i || target, i == rowAt(mx, my, cw) && !dragging);
            ui.text(3, y + 4, DriveMenu.letter(i), skin.accent());
            if (stack.isEmpty()) {
                ui.text(28, y + 4, Component.translatable("gui.feedback.computer.empty_short"), skin.dim());
                continue;
            }
            ui.item(stack, 22, y - 1);
            int textX = 40;
            if (ComputerApps.isApp(stack)) {
                ui.clipped(textX, y + 4, rowW - textX - 2, stack.getHoverName().getString(), skin.accent());
                continue;
            }
            int size = graphOf(stack).nodes().size();
            String text = size == 0 ? Component.translatable("gui.feedback.computer.blank").getString()
                    : Component.translatable("gui.feedback.computer.cards", size).getString();
            ui.right(rowW - 3, y + 4, text, size == 0 ? skin.dim() : i == RUN ? skin.accent() : skin.ink());
        }

        int tx = cw - TRASH_W;
        boolean overTrash = dragging && onTrash(mx, my, cw);
        ui.inset(tx, 0, TRASH_W, TRASH_H);
        if (overTrash)
            ui.fill(tx + 1, 1, TRASH_W - 2, TRASH_H - 2, (skin.warn() & 0x00FFFFFF) | 0x30000000);
        ui.glyph(Glyph.TRASH, tx + TRASH_W / 2 - 4, 10, overTrash ? skin.warn() : skin.dim());
        String label = Component.translatable("gui.feedback.computer.files.trash").getString();
        ui.text(tx + (TRASH_W - ui.font().width(label)) / 2, 26, label, overTrash ? skin.warn() : skin.dim());

        int below = (RUN + 1) * ROW + 4;
        if (ch - 18 - below >= 20)
            ui.wrapped(0, below, cw, Component.translatable(ctx.installed("library") ? "gui.feedback.computer.files.hint"
                    : "gui.feedback.computer.files.hint_plain"), skin.dim(), ch - 18);

        ItemStack picked = ctx.menu().getSlot(sel).getItem();
        boolean has = !picked.isEmpty(), app = ComputerApps.isApp(picked);
        open.active = has;
        edit.active = has && sel != RUN && !app && !editing;
        run.active = has && !app && !editing;
        run.setMessage(Component.translatable(sel == RUN ? "gui.feedback.computer.stop" : "gui.feedback.computer.run"));
        eject.active = has && sel != RUN && !editing;

        if (dragging && dragFrom >= 0)
            ui.item(ctx.menu().getSlot(dragFrom).getItem(), (int) dragX - 8, (int) dragY - 8);
    }

    @Override
    protected boolean mouseClicked(double lx, double ly, int button) {
        int row = rowAt(lx, ly, contentW());
        if (row < 0)
            return false;
        ctx.showDrive(row);
        long now = System.currentTimeMillis();
        if (row == lastRow && now - lastClick < DOUBLE_CLICK_MS && !ctx.menu().getSlot(row).getItem().isEmpty()) {
            lastRow = -1;
            desktop().open("drive");
            return true;
        }
        lastRow = row;
        lastClick = now;
        pressed = row;
        pressX = lx;
        pressY = ly;
        return true;
    }

    @Override
    protected boolean mouseDragged(double lx, double ly, int button, double dx, double dy) {
        if (pressed < 0)
            return false;
        if (!dragging && draggable(pressed) && Math.hypot(lx - pressX, ly - pressY) > DRAG) {
            dragging = true;
            dragFrom = pressed;
        }
        dragX = lx;
        dragY = ly;
        return dragging;
    }

    @Override
    protected boolean mouseReleased(double lx, double ly, int button) {
        boolean was = dragging;
        int from = dragFrom;
        pressed = -1;
        dragging = false;
        dragFrom = -1;
        if (!was)
            return false;
        int cw = contentW();
        int to = rowAt(lx, ly, cw);
        if (onTrash(lx, ly, cw))
            DriveMenu.confirmErase(ctx, desktop(), from);
        else if (to >= 0 && to < RUN && to != from)
            ctx.send(ComputerActionPayload.COPY_TO, from + 8 * to);
        return true;
    }

    @Override
    protected List<ContextMenu.Item> contextItems(double lx, double ly) {
        int row = rowAt(lx, ly, contentW());
        if (row < 0)
            return List.of();
        ctx.showDrive(row);
        return DriveMenu.of(ctx, desktop(), row);
    }

    @Override
    protected void closed() {
        pressed = -1;
        dragging = false;
        dragFrom = -1;
    }
}
