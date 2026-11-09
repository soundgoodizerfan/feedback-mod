// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.Random;

import io.github.soundgoodizerfan.feedback.client.desktop.Desktop;
import io.github.soundgoodizerfan.feedback.client.desktop.DesktopApp;
import io.github.soundgoodizerfan.feedback.client.desktop.Glyph;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerApps;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBays;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerMenu;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerSettings;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerSettingsPayload;
import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkedPayload;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Computer's desktop, laid out the way a Plasma desktop is, in phosphor. This class is the shell: the wallpaper, the drive icons, the
 * widgets on the wallpaper, the hotbar dock and the panel's tray. The windows and the panel's launcher and tasks belong to the
 * {@link Desktop} compositor; each app (Drive, Monitor, Links, Editor, Settings) is a {@link io.github.soundgoodizerfan.feedback.client.desktop.DesktopWindow}.
 *
 * <p>The icons and the dock are real slots, which a screen always draws above its background, so windows keep to a work area that
 * excludes them. How it looks is the Computer's {@link ComputerSettings}: phosphor (the skin), wallpaper, CRT effects, which widgets show
 * and which side the icons are on. A change shows at once here (the screen keeps its own copy until the server's arrives) and is sent to
 * the server, which keeps it on the block.
 */
public class ComputerScreen extends SkinnedScreen<ComputerMenu> implements ComputerContext {

    private static final int W = ComputerMenu.PANEL_W, H = ComputerMenu.PANEL_H, BAR = ComputerMenu.BAR_H, RUN = ComputerBays.SIZE;
    private static final int DESK_TOP = 6, TRACE = 84, ICONS_W = 50;
    /** Where the desktop ends and the panel begins, relative to the screen. */
    private static final int BAR_Y = H - 4 - BAR, DESK_BOTTOM = BAR_Y - 3;
    private static final int WIDGET_W = 88;

    private final Desktop desktop;
    private GuiSkin look;
    private ComputerSettings local, lastSeen;
    private boolean rebuild;
    private int shown;

    /** The scope's last readings of the running program's first sensor, oldest first (a ring). */
    private final float[] trace = new float[TRACE];
    private int traceAt, ticks;
    private int[] circuit;

    private final Button[] driveLabels = new Button[ComputerBays.SIZE + 1];

    public ComputerScreen(ComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, skinFor(settingsOf(menu)));
        local = lastSeen = settingsOf(menu);
        look = skinFor(local);
        applyEffects();
        desktop = new Desktop(look);
        desktop.register(new DesktopApp("drive", Component.translatable("gui.feedback.computer.drives"), Glyph.DRIVE, false, () -> new DriveWindow(this)));
        desktop.register(new DesktopApp("monitor", Component.translatable("gui.feedback.computer.monitor"), Glyph.MONITOR, true, () -> new MonitorWindow(this)));
        desktop.register(new DesktopApp("links", Component.translatable("gui.feedback.computer.links"), Glyph.LINKS, true, () -> new LinksWindow(this)));
        desktop.register(new DesktopApp("editor", Component.translatable("gui.feedback.computer.editor"), Glyph.EDITOR, false, () -> new EditorWindow(this)));
        desktop.register(new DesktopApp("settings", Component.translatable("gui.feedback.computer.settings"), Glyph.SETTINGS, true, () -> new SettingsWindow(this)));
        desktop.register(new DesktopApp("console", Component.translatable("gui.feedback.computer.console"), Glyph.CONSOLE, true, () -> new ConsoleWindow(this)));
        desktop.register(new DesktopApp("scope", Component.translatable("gui.feedback.computer.scope_app"), Glyph.SCOPE, true, () -> new ScopeWindow(this), () -> installed("scope")));
        desktop.register(new DesktopApp("linkmap", Component.translatable("gui.feedback.computer.linkmap"), Glyph.MAP, true, () -> new LinkMapWindow(this), () -> installed("linkmap")));
        desktop.register(new DesktopApp("notes", Component.translatable("gui.feedback.computer.notes"), Glyph.NOTES, true, () -> new NotesWindow(this), () -> installed("notes")));
        desktop.register(new DesktopApp("converter", Component.translatable("gui.feedback.computer.converter"), Glyph.CONVERT, true, () -> new ConverterWindow(this), () -> installed("converter")));
        desktop.register(new DesktopApp("calc", Component.translatable("gui.feedback.computer.calculator"), Glyph.CALC, true, () -> new CalculatorWindow(this), () -> installed("calc")));
        desktop.register(new DesktopApp("library", Component.translatable("gui.feedback.computer.library"), Glyph.LIBRARY, true, () -> new LibraryWindow(this), () -> installed("library")));
        desktop.register(new DesktopApp("power", Component.translatable("gui.feedback.computer.power"), Glyph.POWER, true, () -> new PowerWindow(this), () -> installed("power")));
        desktop.register(new DesktopApp("arcade", Component.translatable("gui.feedback.computer.arcade"), Glyph.GAME, true, () -> new ArcadeWindow(this), () -> installed("arcade")));
        desktop.register(new DesktopApp("blueprint", Component.translatable("gui.feedback.computer.blueprint"), Glyph.BLUEPRINT, true, () -> new BlueprintWindow(this), () -> installed("blueprint")));
        desktop.tray(new Desktop.Tray() {
            @Override
            public int width() {
                return 100;
            }

            @Override
            public void paint(GuiGraphics g, int x, int y, int w, int h) {
                drawTray(g, x, y, w, h);
            }
        });
    }

    /** Whether a disk with this app is in a bay, as the menu's slots show it. */
    private boolean installed(String id) {
        for (int i = 0; i < ComputerBays.SIZE; i++)
            if (id.equals(ComputerApps.idOf(menu.getSlot(i).getItem())))
                return true;
        return false;
    }

    private static ComputerSettings settingsOf(ComputerMenu menu) {
        ComputerBlockEntity computer = menu.getComputer();
        return computer == null ? ComputerSettings.DEFAULT : computer.settings();
    }

    private static GuiSkin skinFor(ComputerSettings settings) {
        return switch (settings.phosphor()) {
            case GREEN -> GuiSkin.TERMINAL;
            case AMBER -> GuiSkin.TERMINAL_AMBER;
            case WHITE -> GuiSkin.TERMINAL_WHITE;
            case BLUE -> GuiSkin.TERMINAL_BLUE;
        };
    }

    private void applyEffects() {
        TerminalLook.effects(local.scanlines(), local.glow(), local.band(), local.boot());
    }

    // --- ComputerContext --------------------------------------------------------------------

    @Override
    public ComputerMenu menu() {
        return menu;
    }

    @Override
    public ComputerBlockEntity computer() {
        return menu.getComputer();
    }

    @Override
    public void send(int action, int drive) {
        PacketDistributor.sendToServer(new ComputerActionPayload(menu.getPos(), action, drive));
    }

    @Override
    public ProgramGraph running() {
        return menu.getSlot(RUN).getItem().getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
    }

    @Override
    public int drive() {
        return shown;
    }

    @Override
    public void showDrive(int drive) {
        shown = drive;
    }

    @Override
    public ComputerSettings settings() {
        return local;
    }

    @Override
    public void change(ComputerSettings settings) {
        if (settings.equals(local))
            return;
        boolean recolour = settings.phosphor() != local.phosphor();
        local = settings;
        applyEffects();
        rebuild |= recolour;
        PacketDistributor.sendToServer(new ComputerSettingsPayload(menu.getPos(), settings));
    }

    // --- set-up -----------------------------------------------------------------------------

    @Override
    protected void init() {
        imageWidth = W;
        imageHeight = H;
        super.init();
        look = skinFor(local);
        boolean right = menu.iconX() == ComputerMenu.ICON_X_RIGHT;
        int workX = leftPos + (right ? 8 : ICONS_W);
        desktop.area(workX, topPos + DESK_TOP + 2, W - ICONS_W - 8, ComputerMenu.DOCK_Y - 8 - DESK_TOP - 2);
        desktop.bar(leftPos + 4, topPos + BAR_Y, W - 8, BAR);
        desktop.rebuild(look);
        for (int i = 0; i <= RUN; i++) {
            final int which = i;
            String label = i == RUN ? "RUN" : String.valueOf((char) ('A' + i));
            driveLabels[i] = addRenderableWidget(GuiStyle.button(look, Component.literal(label), b -> {
                showDrive(which);
                desktop.open("drive");
            }, leftPos + menu.iconX() - 6, topPos + ComputerMenu.ICON_Y + i * ComputerMenu.ICON_STEP + 18, 28, 10));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ComputerBlockEntity computer = menu.getComputer();
        if (computer != null && !computer.settings().equals(lastSeen)) {
            lastSeen = computer.settings();
            local = lastSeen;
            applyEffects();
            rebuild = true;
        }
        if (rebuild) {
            rebuild = false;
            rebuildWidgets();
        }
        desktop.tick();
        if (++ticks % 2 == 0) {
            float[] seen = computer == null ? new float[0] : computer.readings();
            trace[traceAt] = running().nodes().isEmpty() || seen.length == 0 ? 0f : seen[0];
            traceAt = (traceAt + 1) % TRACE;
        }
    }

    /** Sends an unsent edit before the close begins, while the server still has the menu open. */
    @Override
    public void onClose() {
        EditorWindow editor = desktop.window("editor");
        editor.flush();
        NotesWindow notes = desktop.window("notes");
        notes.flush();
        super.onClose();
    }

    /** The editor's link reply, routed here from the network handler. */
    void applyLink(ProgrammerLinkedPayload payload) {
        EditorWindow editor = desktop.window("editor");
        editor.applyLink(payload);
    }

    // --- drawing --------------------------------------------------------------------------------

    private static int fade(int colour, int alpha) {
        return (colour & 0x00FFFFFF) | (alpha << 24);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        applyEffects();
        look.panel(g, leftPos, topPos, imageWidth, imageHeight);
        drawWallpaper(g);
        drawIcons(g, mx, my);
        drawWidgets(g);
        drawDock(g);
        desktop.render(g, mx, my, partial);
    }

    /** The wallpaper: a sunken screen, dressed as the settings say, with the system's name watermarked large across it. */
    private void drawWallpaper(GuiGraphics g) {
        int x = leftPos + 6, y = topPos + DESK_TOP, w = imageWidth - 12, h = DESK_BOTTOM - DESK_TOP;
        look.inset(g, x, y, w, h);
        switch (local.wallpaper()) {
            case GRID -> {
                int dot = fade(look.ink(), 0x22);
                for (int gx = x + 6; gx < x + w - 3; gx += 12)
                    for (int gy = y + 6; gy < y + h - 3; gy += 12)
                        g.fill(gx, gy, gx + 1, gy + 1, dot);
            }
            case SCANS -> {
                int line = fade(look.ink(), 0x12);
                for (int yy = y + 2; yy < y + h - 2; yy += 4)
                    g.fill(x + 1, yy, x + w - 1, yy + 1, line);
                g.fillGradient(x + 1, y + 1, x + w - 1, y + h / 3, fade(look.ink(), 0x10), fade(look.ink(), 0x00));
            }
            case CIRCUIT -> drawCircuit(g, x, y, w, h);
            case BLANK -> {
            }
        }
        if (local.wallpaper() != ComputerSettings.Wallpaper.BLANK) {
            var pose = g.pose();
            pose.pushPose();
            pose.translate(leftPos + imageWidth / 2f - 52, topPos + 118, 0);
            pose.scale(2.5f, 2.5f, 1f);
            g.drawString(font, "FEEDBACK", 0, 0, fade(look.ink(), 0x26), false);
            pose.popPose();
        }
    }

    /** Traces and pads on a 12 px grid, from a fixed seed so they never crawl. */
    private void drawCircuit(GuiGraphics g, int x, int y, int w, int h) {
        if (circuit == null) {
            Random random = new Random(0xFEEDL);
            circuit = new int[24 * 6];
            for (int i = 0; i < 24; i++) {
                int px = 1 + random.nextInt(w / 12 - 2), py = 1 + random.nextInt(h / 12 - 2);
                boolean horizontal = random.nextBoolean();
                int a = 2 + random.nextInt(5), b = 1 + random.nextInt(4);
                circuit[i * 6] = px * 12;
                circuit[i * 6 + 1] = py * 12;
                circuit[i * 6 + 2] = horizontal ? 1 : 0;
                circuit[i * 6 + 3] = a * 12 * (random.nextBoolean() ? 1 : -1);
                circuit[i * 6 + 4] = b * 12 * (random.nextBoolean() ? 1 : -1);
                circuit[i * 6 + 5] = random.nextInt(3);
            }
        }
        int wire = fade(look.ink(), 0x30), pad = fade(look.ink(), 0x55);
        for (int i = 0; i < 24; i++) {
            int px = x + circuit[i * 6], py = y + circuit[i * 6 + 1];
            boolean horizontal = circuit[i * 6 + 2] == 1;
            int a = circuit[i * 6 + 3], b = circuit[i * 6 + 4];
            int mx = horizontal ? px + a : px, my = horizontal ? py : py + a;
            int ex = horizontal ? mx : mx + b, ey = horizontal ? my + b : my;
            line(g, px, py, mx, my, x, y, w, h, wire);
            line(g, mx, my, ex, ey, x, y, w, h, wire);
            for (int[] p : new int[][] {{px, py}, {ex, ey}})
                if (p[0] > x + 2 && p[0] < x + w - 4 && p[1] > y + 2 && p[1] < y + h - 4)
                    g.fill(p[0] - 1, p[1] - 1, p[0] + 2, p[1] + 2, pad);
        }
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int bx, int by, int bw, int bh, int colour) {
        int ax = Math.max(bx + 2, Math.min(x0, x1)), ay = Math.max(by + 2, Math.min(y0, y1));
        int cx = Math.min(bx + bw - 3, Math.max(x0, x1)), cy = Math.min(by + bh - 3, Math.max(y0, y1));
        if (ax < cx || ay < cy)
            g.fill(ax, ay, Math.max(cx, ax + 1), Math.max(cy, ay + 1), colour);
    }

    /** The drive icons: a socket for the medium, a label under it, the open one lit like a picked desktop icon. */
    private void drawIcons(GuiGraphics g, int mx, int my) {
        boolean driveOpen = desktop.window("drive").isOpen();
        for (int i = 0; i <= RUN; i++) {
            int x = leftPos + menu.iconX(), y = topPos + ComputerMenu.ICON_Y + i * ComputerMenu.ICON_STEP;
            boolean hover = mx >= x - 7 && mx < x + 25 && my >= y - 2 && my < y + 28 && !desktop.covers(mx, my);
            look.row(g, x - 7, y - 2, 30, 29, driveOpen && shown == i, hover);
            look.slot(g, x, y);
        }
    }

    private int widgetX() {
        return leftPos + (menu.iconX() == ComputerMenu.ICON_X_RIGHT ? 12 : W - 100);
    }

    /** The widgets on the wallpaper: a clock, a scope on the first sensor of the running program, and each drive's memory use. */
    private void drawWidgets(GuiGraphics g) {
        int x = widgetX(), y = topPos + DESK_TOP + 6;
        if (local.clock()) {
            look.plaque(g, x, y, WIDGET_W, 34);
            long time = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getDayTime();
            String clock = clockText(time);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(x + WIDGET_W / 2f - font.width(clock), y + 5, 0);
            pose.scale(2f, 2f, 1f);
            g.drawString(font, clock, 0, 0, look.ink(), false);
            pose.popPose();
            Component day = Component.translatable("gui.feedback.computer.day", time / 24000 + 1);
            g.drawString(font, day, x + (WIDGET_W - font.width(day)) / 2, y + 23, look.dim(), false);
            y += 40;
        }
        if (local.scope()) {
            drawScope(g, x, y);
            y += 52;
        }
        if (local.memory())
            drawMemory(g, x, y);
    }

    private void drawScope(GuiGraphics g, int x, int sy) {
        look.inset(g, x, sy, WIDGET_W, 46);
        Quantity q = firstQuantity();
        boolean signal = !running().nodes().isEmpty() && q != null;
        g.drawString(font, Component.translatable("gui.feedback.computer.scope"), x + 4, sy + 3, look.dim(), false);
        int px = x + 2, py = sy + 14, pw = WIDGET_W - 4, ph = 28;
        int grid = fade(look.ink(), 0x28);
        for (int i = 1; i < 4; i++)
            g.fill(px, py + ph * i / 4, px + pw, py + ph * i / 4 + 1, grid);
        for (int i = 1; i < 6; i++)
            g.fill(px + pw * i / 6, py, px + pw * i / 6 + 1, py + ph, grid);
        if (signal) {
            float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
            for (float v : trace) {
                lo = Math.min(lo, v);
                hi = Math.max(hi, v);
            }
            float span = Math.max(hi - lo, 1e-3f);
            int prev = -1;
            for (int i = 0; i < TRACE; i++) {
                float v = trace[(traceAt + i) % TRACE];
                int cy = py + ph - 2 - (int) ((v - lo) / span * (ph - 4));
                int cx = px + i * pw / TRACE;
                if (prev >= 0)
                    GuiStyle.line(g, cx - 1, prev, cx, cy, look.accent());
                prev = cy;
            }
            float now = trace[(traceAt + TRACE - 1) % TRACE];
            String text = font.plainSubstrByWidth(q == Quantity.STATE ? (now > 0.5f ? "on" : "off") : Readout.figure(q, now), WIDGET_W - 50);
            g.drawString(font, text, x + WIDGET_W - 4 - font.width(text), sy + 3, look.accent(), false);
        } else {
            Component none = Component.translatable("gui.feedback.computer.no_signal");
            g.drawString(font, none, px + (pw - font.width(none)) / 2, py + ph / 2 - 4, look.dim(), false);
        }
    }

    private void drawMemory(GuiGraphics g, int x, int my) {
        look.plaque(g, x, my, WIDGET_W, 54);
        g.drawString(font, Component.translatable("gui.feedback.computer.memory"), x + 4, my + 3, look.dim(), false);
        int limit = Math.max(1, ControllerLimits.MAX.nodes());
        for (int i = 0; i <= RUN; i++) {
            int row = my + 13 + i * 8;
            g.drawString(font, i == RUN ? "R" : String.valueOf((char) ('A' + i)), x + 4, row, look.ink(), false);
            ProgramGraph graph = menu.getSlot(i).getItem().getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
            look.bar(g, x + 16, row + 1, WIDGET_W - 22, 5, Math.min(1f, graph.nodes().size() / (float) limit), -1f);
        }
    }

    static String clockText(long dayTime) {
        long hours = (dayTime / 1000 + 6) % 24, minutes = dayTime % 1000 * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

    private Quantity firstQuantity() {
        for (ProgramNode node : running().nodes())
            if (node instanceof SensorNode sensor)
                return sensor.quantity() == null ? Quantity.FIGURE : sensor.quantity();
        return null;
    }

    /** The dock: the hotbar, floating over the wallpaper in a plate. */
    private void drawDock(GuiGraphics g) {
        look.plaque(g, leftPos + ComputerMenu.DOCK_X - 6, topPos + ComputerMenu.DOCK_Y - 5, 9 * 18 + 10, 28);
        for (int c = 0; c < 9; c++)
            look.slot(g, leftPos + ComputerMenu.DOCK_X + c * 18, topPos + ComputerMenu.DOCK_Y);
    }

    /** The panel's tray: a lamp per bay and the running slot, the power lamp, the clock. */
    private void drawTray(GuiGraphics g, int x, int y, int w, int h) {
        int right = x + w;
        String clock = clockText(Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getDayTime());
        g.drawString(font, clock, right - font.width(clock), y + 6, look.ink(), false);
        int lamp = right - font.width(clock) - 12;
        ComputerBlockEntity computer = menu.getComputer();
        boolean powered = computer != null && computer.poweredSeen();
        g.fill(lamp, y + 7, lamp + 6, y + 13, powered ? look.accent() : fade(look.warn(), 0xB0));
        lamp -= 8;
        for (int i = RUN; i >= 0; i--) {
            lamp -= 8;
            boolean in = !menu.getSlot(i).getItem().isEmpty();
            int colour = in ? (i == RUN ? look.accent() : look.ink()) : fade(look.dim(), 0x70);
            g.fill(lamp, y + 7, lamp + 6, y + 13, colour);
        }
    }

    // --- input ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!settled())
            return true;
        return desktop.mouseClicked(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!settled())
            return true;
        return desktop.mouseDragged(mx, my, button, dx, dy) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return desktop.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return settled() && desktop.mouseScrolled(mx, my, sy) || super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return closing() || desktop.keyPressed(key, scan, modifiers) || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return closing() || desktop.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }

    /** The desktop has no inventory label; its name is the launcher's. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }
}
