// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBays;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerMenu;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramVariable;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerMenu;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Computer's desktop. The drive bays and the running slot are icons down the left (real slots: drag a medium on, drag it off), the
 * player's hotbar runs along the top, and the apps open as windows you can drag by their title bars: a Drive window for whichever icon
 * you opened (Edit, Run or Stop, Eject), a Monitor of the running program and a Links manager. The Editor is the Programmer screen
 * itself, opened full-window from a Drive; closing it comes back here. Windows keep to the right of the icon column, so an icon's item is
 * never drawn under one.
 */
public class ComputerScreen extends SkinnedScreen<ComputerMenu> {

    private static final GuiSkin SKIN = GuiSkin.TERMINAL;
    private static final int MIN_X = 64, TASKBAR = 24, TITLE = 14, RUN = ComputerBays.SIZE;

    private enum App { DRIVE, MONITOR, LINKS }

    /** One window: where it is, whether it is open. Drawn in {@link #windows} order, last on top. */
    private static final class Win {
        final App app;
        int x, y;
        final int w, h;
        boolean open;

        Win(App app, int x, int y, int w, int h) {
            this.app = app;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean hit(double mx, double my) {
            return open && mx >= x && mx < x + w && my >= y && my < y + h;
        }

        boolean onTitle(double mx, double my) {
            return hit(mx, my) && my < y + TITLE;
        }
    }

    private final Win drive = new Win(App.DRIVE, 80, 40, 176, 112);
    private final Win monitor = new Win(App.MONITOR, 270, 40, 196, 150);
    private final Win links = new Win(App.LINKS, 120, 120, 214, 130);
    private final List<Win> windows = new ArrayList<>(List.of(drive, monitor, links));
    private Win dragging;
    private double grabX, grabY;
    private int shown = 0;
    private int linkScroll;

    private final Button[] driveLabels = new Button[ComputerBays.SIZE + 1];
    private Button editButton, runButton, ejectButton, monitorTask, linksTask;
    private final Button[] close = new Button[3];

    public ComputerScreen(ComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SKIN);
    }

    @Override
    protected void init() {
        imageWidth = width;
        imageHeight = height;
        super.init();
        for (int i = 0; i <= RUN; i++) {
            final int which = i;
            String label = i == RUN ? "RUN" : String.valueOf((char) ('A' + i));
            driveLabels[i] = addRenderableWidget(GuiStyle.button(SKIN, Component.literal(label), b -> openDrive(which),
                    ComputerMenu.ICON_X - 6, ComputerMenu.ICON_Y + i * ComputerMenu.ICON_STEP + 19, 28, 12));
        }
        editButton = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.computer.edit"), b -> send(ComputerActionPayload.EDIT), 0, 0, 52, 14));
        runButton = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.computer.run"), b -> send(shown == RUN ? ComputerActionPayload.STOP : ComputerActionPayload.RUN), 0, 0, 52, 14));
        ejectButton = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.computer.eject"), b -> send(ComputerActionPayload.EJECT), 0, 0, 52, 14));
        for (int i = 0; i < 3; i++) {
            final Win win = i == 0 ? drive : i == 1 ? monitor : links;
            close[i] = addRenderableWidget(GuiStyle.button(SKIN, Component.literal("x"), b -> win.open = false, 0, 0, 12, 11));
        }
        monitorTask = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.computer.monitor"), b -> toggle(monitor), 70, height - TASKBAR + 5, 56, 14));
        linksTask = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.computer.links"), b -> toggle(links), 130, height - TASKBAR + 5, 56, 14));
    }

    private void openDrive(int which) {
        shown = which;
        drive.open = true;
        front(drive);
    }

    private void toggle(Win win) {
        win.open = !win.open;
        if (win.open)
            front(win);
    }

    private void front(Win win) {
        windows.remove(win);
        windows.add(win);
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new ComputerActionPayload(menu.getPos(), action, shown));
    }

    private ItemStack mediumShown() {
        return menu.getSlot(shown).getItem();
    }

    private ProgramGraph running() {
        return menu.getSlot(RUN).getItem().getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
    }

    // --- drawing --------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        SKIN.panel(g, 0, 0, width, height);
        SKIN.inset(g, 6, 24, width - 12, height - 24 - TASKBAR - 2);
        for (int i = 0; i <= RUN; i++) {
            int y = ComputerMenu.ICON_Y + i * ComputerMenu.ICON_STEP;
            SKIN.slot(g, ComputerMenu.ICON_X, y);
        }
        for (int c = 0; c < 9; c++)
            SKIN.slot(g, ProgrammerMenu.HOTBAR_X + c * 18, ProgrammerMenu.STRIP_SLOT_Y);
        SKIN.plaque(g, 4, height - TASKBAR, width - 8, TASKBAR - 4);
        g.drawString(font, title, 8, 10, SKIN.ink(), false);
        boolean live = !menu.getSlot(RUN).getItem().isEmpty();
        g.drawString(font, Component.translatable(live ? "gui.feedback.computer.running" : "gui.feedback.computer.idle"),
                width - 10 - font.width(Component.translatable(live ? "gui.feedback.computer.running" : "gui.feedback.computer.idle")),
                height - TASKBAR + 8, live ? SKIN.accent() : SKIN.dim(), false);

        for (Win win : windows)
            if (win.open)
                drawWindow(g, win);
        placeWidgets();
    }

    private void drawWindow(GuiGraphics g, Win win) {
        SKIN.panel(g, win.x, win.y, win.w, win.h);
        SKIN.plaque(g, win.x + 3, win.y + 3, win.w - 6, TITLE - 3);
        Component name = switch (win.app) {
            case DRIVE -> Component.translatable("gui.feedback.computer.drive", driveName(shown));
            case MONITOR -> Component.translatable("gui.feedback.computer.monitor");
            case LINKS -> Component.translatable("gui.feedback.computer.links");
        };
        g.drawString(font, name, win.x + 7, win.y + 5, SKIN.ink(), false);
        int cx = win.x + 8, cy = win.y + TITLE + 6;
        switch (win.app) {
            case DRIVE -> drawDrive(g, cx, cy, win.w - 16);
            case MONITOR -> drawMonitor(g, cx, cy, win.w - 16, win.h - TITLE - 12);
            case LINKS -> drawLinks(g, cx, cy, win.w - 16, win.h - TITLE - 12);
        }
    }

    private static String driveName(int index) {
        return index == RUN ? "RUN" : String.valueOf((char) ('A' + index)) + ":";
    }

    private void drawDrive(GuiGraphics g, int x, int y, int w) {
        ItemStack medium = mediumShown();
        if (medium.isEmpty()) {
            g.drawString(font, Component.translatable("gui.feedback.computer.empty"), x, y, SKIN.dim(), false);
            return;
        }
        g.renderItem(medium, x, y);
        g.drawString(font, font.plainSubstrByWidth(medium.getHoverName().getString(), w - 22), x + 22, y + 4, SKIN.ink(), false);
        ProgramGraph graph = medium.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
        if (graph.nodes().isEmpty())
            g.drawString(font, Component.translatable("gui.feedback.computer.blank"), x, y + 24, SKIN.dim(), false);
        else {
            g.drawString(font, Component.translatable("gui.feedback.computer.program", graph.nodes().size(), graph.variables().size()),
                    x, y + 24, SKIN.ink(), false);
            g.drawString(font, Component.translatable(shown == RUN ? "gui.feedback.computer.is_running" : "gui.feedback.computer.stored"),
                    x, y + 36, shown == RUN ? SKIN.accent() : SKIN.dim(), false);
        }
    }

    private void drawMonitor(GuiGraphics g, int x, int y, int w, int h) {
        ProgramGraph graph = running();
        if (graph.nodes().isEmpty()) {
            g.drawString(font, Component.translatable("gui.feedback.computer.nothing_running"), x, y, SKIN.dim(), false);
            return;
        }
        ComputerBlockEntity computer = menu.getComputer();
        Map<String, Object> values = computer == null ? Map.of() : computer.core().variables();
        g.drawString(font, Component.translatable("gui.feedback.computer.variables"), x, y, SKIN.dim(), false);
        int row = y + 11;
        for (ProgramVariable v : graph.variables()) {
            if (row > y + h - 10)
                break;
            Object value = values.get(v.name());
            String text = value instanceof Float f ? Readout.figure(Quantity.FIGURE, f) : Boolean.TRUE.equals(value) ? "on" : "off";
            g.drawString(font, font.plainSubstrByWidth(v.name(), w - 60), x, row, SKIN.ink(), false);
            g.drawString(font, text, x + w - font.width(text), row, SKIN.accent(), false);
            row += 10;
        }
        if (graph.variables().isEmpty())
            g.drawString(font, Component.translatable("gui.feedback.computer.no_variables"), x, row, SKIN.dim(), false);
        row = Math.max(row, y + 11) + 4;
        g.drawString(font, Component.translatable("gui.feedback.computer.sensors"), x, row, SKIN.dim(), false);
        row += 11;
        float[] seen = computer == null ? new float[0] : computer.readings();
        int index = 0;
        for (ProgramNode node : graph.nodes()) {
            if (!(node instanceof SensorNode sensor))
                continue;
            if (row > y + h - 2)
                break;
            Quantity q = sensor.quantity() == null ? Quantity.FIGURE : sensor.quantity();
            float v = index < seen.length ? seen[index] : 0f;
            String text = q == Quantity.STATE ? (v > 0.5f ? "on" : "off") : Readout.figure(q, v);
            g.drawString(font, font.plainSubstrByWidth(q.getSerializedName(), w - 60), x, row, SKIN.ink(), false);
            g.drawString(font, text, x + w - font.width(text), row, SKIN.accent(), false);
            row += 10;
            index++;
        }
    }

    private List<ProgramNode> linked() {
        List<ProgramNode> out = new ArrayList<>();
        for (ProgramNode node : running().nodes())
            if (node instanceof SensorNode || node instanceof ActuatorNode)
                out.add(node);
        return out;
    }

    private void drawLinks(GuiGraphics g, int x, int y, int w, int h) {
        List<ProgramNode> rows = linked();
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable("gui.feedback.computer.no_links"), x, y, SKIN.dim(), false);
            return;
        }
        int visible = Math.max(1, h / 10);
        linkScroll = Math.max(0, Math.min(linkScroll, rows.size() - visible));
        for (int i = 0; i < visible && linkScroll + i < rows.size(); i++) {
            ProgramNode node = rows.get(linkScroll + i);
            DataNodeRef ref = node instanceof SensorNode s ? s.target() : ((ActuatorNode) node).target();
            String kind = node instanceof SensorNode ? "in" : "out";
            String where = "unlinked";
            if (ref != null && Minecraft.getInstance().level != null) {
                var pos = ref.linkedAt();
                where = Minecraft.getInstance().level.getBlockState(pos).getBlock().getName().getString() + " " + pos.getX() + "," + pos.getY() + "," + pos.getZ();
            }
            g.drawString(font, kind, x, y + i * 10, SKIN.dim(), false);
            g.drawString(font, font.plainSubstrByWidth(where, w - 20), x + 18, y + i * 10, ref == null ? SKIN.warn() : SKIN.ink(), false);
        }
        SKIN.scrollbar(g, x + w + 3, y, visible * 10, rows.size(), visible, linkScroll);
    }

    /** Puts every window button where its window is now, and hides those whose window is shut or does not need them. */
    private void placeWidgets() {
        // the close buttons were made in the order drive, monitor, links, whatever order the windows are stacked in
        Win[] byApp = {drive, monitor, links};
        for (int i = 0; i < 3; i++) {
            close[i].visible = byApp[i].open;
            close[i].setPosition(byApp[i].x + byApp[i].w - 16, byApp[i].y + 3);
        }
        boolean show = drive.open && !mediumShown().isEmpty();
        boolean running = shown == RUN;
        editButton.visible = show;
        runButton.visible = show && !running || show && running;
        ejectButton.visible = show && !running;
        editButton.active = show && !running;
        if (show) {
            editButton.setPosition(drive.x + 8, drive.y + drive.h - 22);
            runButton.setPosition(drive.x + 62, drive.y + drive.h - 22);
            ejectButton.setPosition(drive.x + 116, drive.y + drive.h - 22);
            runButton.setMessage(Component.translatable(running ? "gui.feedback.computer.stop" : "gui.feedback.computer.run"));
        }
    }

    // --- input ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!settled())
            return true;
        for (int i = windows.size() - 1; i >= 0; i--) {
            Win win = windows.get(i);
            if (!win.hit(mx, my))
                continue;
            if (win.onTitle(mx, my) && mx < win.x + win.w - 18) {
                dragging = win;
                grabX = mx - win.x;
                grabY = my - win.y;
            }
            front(win);
            // widgets in the window first; the window swallows the rest so a slot behind it is never clicked
            for (var child : children())
                if (child.mouseClicked(mx, my, button))
                    return true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            dragging.x = (int) Math.max(MIN_X, Math.min(width - dragging.w, mx - grabX));
            dragging.y = (int) Math.max(22, Math.min(height - TASKBAR - dragging.h, my - grabY));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (settled() && links.hit(mx, my)) {
            linkScroll -= (int) Math.signum(sy);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    /** The desktop has no inventory label; the title is drawn in {@link #renderBg}. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }
}
