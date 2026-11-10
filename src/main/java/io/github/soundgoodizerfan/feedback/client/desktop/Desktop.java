// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.client.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.GuiStyle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The compositor of a Computer's desktop: it owns the windows and everything between them, the way a display server and window manager
 * do. It keeps the stacking order and the focus, draws each window's frame (title bar, minimise, maximise, game-window fullscreen, close) and clips its
 * content, moves a window by its title bar and resizes it by its right and bottom edges, keeps every window inside the work area, hands
 * each input event to the right window in window-local coordinates, and runs the panel along the bottom: launcher menu, one task button
 * per open window, a tray the shell paints, and a show-desktop sliver.
 *
 * <p>Apps are {@link DesktopApp}s registered once; their windows are made when the desktop is built and live as long as it does, shown
 * and hidden by {@link #open} / {@link #close}. The shell (the screen) supplies the geometry ({@link #area}, {@link #bar}) and paints
 * what is behind and beside the windows (wallpaper, widgets, icons, tray); it forwards input and calls {@link #render}.
 *
 * <p>Everything a window or the panel shows is drawn through the {@link GuiSkin} the desktop is given, and every control is a
 * {@link GuiStyle} widget. After a skin change call {@link #rebuild}: widgets are made again, window state is kept.
 */
public final class Desktop {

    /** What the shell paints in the panel, right of the tasks: lamps, a clock. */
    public interface Tray {
        int width();

        void paint(GuiGraphics g, int x, int y, int w, int h);
    }

    private record Chrome(Button min, Button max, Button full, Button close, Button task) {
    }

    /** A notice in the corner of the work area: shown for {@link #TOAST_LIFE} ticks, fading over the last {@link #TOAST_FADE}. */
    private record Toast(Component text, long born) {
    }

    private static final int BAR_PAD = 4, LAUNCHER_W = 46, TASK_W = 58, SNAP = 6, MENU_W = 88, ENTRY_H = 14;
    private static final int TOAST_LIFE = 120, TOAST_FADE = 20, TOAST_W = 112, TOAST_MAX = 4;
    /** How near the work area's edge the pointer must be, dragging a title bar, for the window to snap there. */
    private static final int SNAP_EDGE = 3;
    private static final long DOUBLE_CLICK_MS = 350;
    private static final int SNAP_NONE = 0, SNAP_LEFT = 1, SNAP_RIGHT = 2, SNAP_TOP = 3;

    private final ContextMenu contextMenu = new ContextMenu();
    private final List<Toast> toasts = new ArrayList<>();
    private long clock;
    private int spawnX;
    private int snapPreview, pointerX, pointerY;
    private DesktopWindow lastTitleWindow;
    private long lastTitleClick;
    private final Map<String, DesktopApp> apps = new LinkedHashMap<>();
    private final Map<String, DesktopWindow> byId = new LinkedHashMap<>();
    private final List<DesktopWindow> stack = new ArrayList<>();
    private final Map<DesktopWindow, Chrome> chrome = new IdentityHashMap<>();
    private final Map<String, Button> menuEntries = new LinkedHashMap<>();
    private final Ui ui = new Ui(Minecraft.getInstance().font);
    private GuiSkin skin;
    private Tray tray;
    private Button launcher, showDesktop;
    private boolean menuOpen;
    private int workX, workY, workW, workH, barX, barY, barW, barH;

    private DesktopWindow dragging, resizing, pressed;
    private double grabX, grabY;
    private int resizeW, resizeH;
    private boolean resizeX, resizeY;

    public Desktop(GuiSkin skin) {
        this.skin = skin;
    }

    // --- set-up -----------------------------------------------------------------------------

    /** The rectangle windows live in, in screen coordinates. */
    public void area(int x, int y, int w, int h) {
        workX = x;
        workY = y;
        workW = w;
        workH = h;
        for (DesktopWindow win : stack)
            if (win.fullscreen) {
                fitFullscreen(win);
            } else if (win.maximised) {
                win.x = x;
                win.y = y;
                win.w = w;
                win.h = h;
                win.layout(win.contentW(), win.contentH());
            } else if (win.snap != SNAP_NONE) {
                fitSnap(win);
            } else if (win.placed) {
                clamp(win);
            }
    }

    /** The rectangle (x, y, w, h) a snap target fills in the work area. */
    private int[] snapRect(int mode) {
        return switch (mode) {
            case SNAP_LEFT -> new int[] {workX, workY, workW / 2, workH};
            case SNAP_RIGHT -> new int[] {workX + workW / 2, workY, workW - workW / 2, workH};
            default -> new int[] {workX, workY, workW, workH};
        };
    }

    /** The whole game window in GUI-scaled pixels: x, y, w, h. */
    private static int[] gameRect() {
        var window = Minecraft.getInstance().getWindow();
        return new int[] {0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight()};
    }

    private void fitFullscreen(DesktopWindow win) {
        int[] r = gameRect();
        win.x = r[0];
        win.y = r[1];
        win.w = r[2];
        win.h = r[3];
        win.layout(win.contentW(), win.contentH());
    }

    private void fitSnap(DesktopWindow win) {
        int[] r = snapRect(win.snap);
        win.x = r[0];
        win.y = r[1];
        win.w = r[2];
        win.h = r[3];
        win.layout(win.contentW(), win.contentH());
    }

    /**
     * Puts a window in the left half, the right half or the whole of the work area, remembering the size it had so that dragging it away,
     * or maximise again, gives it back. A window that cannot be resized does not snap.
     */
    public void snap(DesktopWindow win, int mode) {
        if (win.fullscreen)
            exitFullscreen(win);
        if (!win.resizable || !win.placed)
            return;
        if (mode == SNAP_TOP) {
            if (!win.maximised)
                toggleMaximise(win);
            return;
        }
        if (!win.maximised && win.snap == SNAP_NONE) {
            win.restoreX = win.x;
            win.restoreY = win.y;
            win.restoreW = win.w;
            win.restoreH = win.h;
        }
        win.maximised = false;
        win.snap = mode;
        fitSnap(win);
    }

    public void snapLeft(DesktopWindow win) {
        snap(win, SNAP_LEFT);
    }

    public void snapRight(DesktopWindow win) {
        snap(win, SNAP_RIGHT);
    }

    // --- notices and menus ------------------------------------------------------------------

    /** Shows a short notice in the corner of the work area for a few seconds; at most four at once, the oldest dropping off. */
    public void toast(Component text) {
        toasts.removeIf(t -> t.text().getString().equals(text.getString()));
        toasts.add(new Toast(text, clock));
        while (toasts.size() > TOAST_MAX)
            toasts.remove(0);
    }

    /** Opens a right-click menu at ({@code mx}, {@code my}); nothing happens for an empty list. */
    public void openMenu(int mx, int my, List<ContextMenu.Item> items) {
        contextMenu.open(skin, ui.font(), mx, my, items, barX, workY, barW, barY + barH - workY);
    }

    /** Opens a menu at the pointer, for a follow-up question or a menu opened by a release. */
    public void openMenuAtPointer(List<ContextMenu.Item> items) {
        openMenu(pointerX, pointerY, items);
    }

    private List<ContextMenu.Item> windowMenu(DesktopWindow win) {
        List<ContextMenu.Item> items = new ArrayList<>();
        if (win.minimised)
            items.add(new ContextMenu.Item(Component.translatable("gui.feedback.desktop.restore"), () -> show(win)));
        else
            items.add(new ContextMenu.Item(Component.translatable("gui.feedback.desktop.minimise"), () -> win.minimised = true));
        if (win.resizable) {
            boolean big = win.maximised || win.snap != SNAP_NONE || win.fullscreen;
            items.add(new ContextMenu.Item(Component.translatable(big ? "gui.feedback.desktop.restore" : "gui.feedback.desktop.maximise"),
                    () -> toggleMaximise(win)));
            items.add(new ContextMenu.Item(Component.translatable(win.fullscreen ? "gui.feedback.desktop.leave_fullscreen" : "gui.feedback.desktop.fullscreen"),
                    () -> toggleFullscreen(win)));
            items.add(new ContextMenu.Item(Component.translatable("gui.feedback.desktop.snap_left"), () -> snapLeft(win)));
            items.add(new ContextMenu.Item(Component.translatable("gui.feedback.desktop.snap_right"), () -> snapRight(win)));
        }
        items.add(new ContextMenu.Item(Component.translatable("gui.feedback.desktop.close"), () -> close(win)));
        return items;
    }

    /** The panel's rectangle, in screen coordinates. */
    public void bar(int x, int y, int w, int h) {
        barX = x;
        barY = y;
        barW = w;
        barH = h;
    }

    /** Moves where a window opens the first time, right by {@code dx}: past the shell's icons, which a window may still be dragged over. */
    public void spawn(int dx) {
        spawnX = dx;
    }

    public void tray(Tray tray) {
        this.tray = tray;
    }

    public void register(DesktopApp app) {
        apps.put(app.id(), app);
        DesktopWindow win = app.factory().get();
        win.desktop = this;
        byId.put(app.id(), win);
        stack.add(win);
    }

    /** Makes every control again in {@code skin}; window positions, sizes and open state are kept. */
    public void rebuild(GuiSkin skin) {
        this.skin = skin;
        chrome.clear();
        menuEntries.clear();
        contextMenu.close();
        for (DesktopWindow win : stack) {
            win.clearWidgets();
            win.build(skin);
            Button min = GuiStyle.button(skin, Component.literal("_"), b -> win.minimised = true, 0, 0, 12, 11);
            Button max = GuiStyle.button(skin, Component.literal("o"), b -> toggleMaximise(win), 0, 0, 12, 11);
            Button full = GuiStyle.button(skin, Component.literal("[]"), b -> toggleFullscreen(win), 0, 0, 12, 11);
            Button close = GuiStyle.button(skin, Component.literal("x"), b -> close(win), 0, 0, 12, 11);
            Button task = GuiStyle.button(skin, Component.empty(), b -> clickTask(win), 0, 0, TASK_W - 2, 14);
            chrome.put(win, new Chrome(min, max, full, close, task));
            if (win.placed)
                win.layout(win.contentW(), win.contentH());
        }
        launcher = GuiStyle.primary(skin, Component.translatable("gui.feedback.desktop.menu"), b -> menuOpen = !menuOpen,
                0, 0, LAUNCHER_W, 14, null);
        showDesktop = GuiStyle.button(skin, Component.empty(), b -> minimiseAll(), 0, 0, SNAP, 14);
        for (DesktopApp app : apps.values())
            if (app.inMenu())
                menuEntries.put(app.id(), GuiStyle.button(skin, app.name(), b -> {
                    menuOpen = false;
                    open(app.id());
                }, 0, 0, MENU_W - 24, 12));
    }

    // --- windows ----------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public <T extends DesktopWindow> T window(String id) {
        return (T) byId.get(id);
    }

    /** Shows the app's window, restored if minimised and raised to the front, placing it the first time. */
    public void open(String id) {
        DesktopWindow win = byId.get(id);
        if (win != null && apps.get(id).available().getAsBoolean())
            show(win);
    }

    public void close(String id) {
        DesktopWindow win = byId.get(id);
        if (win != null)
            close(win);
    }

    private void show(DesktopWindow win) {
        boolean fresh = !win.open;
        win.open = true;
        win.minimised = false;
        if (!win.placed) {
            win.placed = true;
            win.x = workX + spawnX + win.startX;
            win.y = workY + win.startY;
            clamp(win);
            win.layout(win.contentW(), win.contentH());
        }
        raise(win);
        if (fresh)
            win.opened();
    }

    private void close(DesktopWindow win) {
        if (!win.open)
            return;
        win.open = false;
        win.focusOn(null);
        win.closed();
    }

    public void minimiseAll() {
        menuOpen = false;
        for (DesktopWindow win : stack)
            win.minimised = true;
    }

    private void raise(DesktopWindow win) {
        stack.remove(win);
        stack.add(win);
    }

    /** The topmost window that is on screen, or null. */
    public DesktopWindow focused() {
        for (int i = stack.size() - 1; i >= 0; i--)
            if (shown(stack.get(i)))
                return stack.get(i);
        return null;
    }

    private static boolean shown(DesktopWindow win) {
        return win.open && !win.minimised;
    }

    private DesktopWindow windowAt(double mx, double my) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            DesktopWindow win = stack.get(i);
            if (shown(win) && mx >= win.x && mx < win.x + win.w && my >= win.y && my < win.y + win.h)
                return win;
        }
        return null;
    }

    private void clickTask(DesktopWindow win) {
        if (win.minimised)
            show(win);
        else if (win == focused())
            win.minimised = true;
        else
            raise(win);
    }

    /**
     * Fills the whole game window (not only the desktop's work area), over the panel and the shell, with the window's own title bar kept.
     * Whatever the window was (free, snapped, maximised) is remembered and comes back when it is toggled off.
     */
    private void toggleFullscreen(DesktopWindow win) {
        if (!win.resizable)
            return;
        if (win.fullscreen) {
            exitFullscreen(win);
        } else {
            win.fsX = win.x;
            win.fsY = win.y;
            win.fsW = win.w;
            win.fsH = win.h;
            win.fullscreen = true;
            fitFullscreen(win);
        }
    }

    private void exitFullscreen(DesktopWindow win) {
        win.fullscreen = false;
        win.x = win.fsX;
        win.y = win.fsY;
        win.w = win.fsW;
        win.h = win.fsH;
        win.layout(win.contentW(), win.contentH());
    }

    private void toggleMaximise(DesktopWindow win) {
        if (!win.resizable)
            return;
        if (win.fullscreen) {
            exitFullscreen(win);
            return;
        }
        if (win.maximised || win.snap != SNAP_NONE) {
            unsnap(win);
            clamp(win);
        } else {
            win.restoreX = win.x;
            win.restoreY = win.y;
            win.restoreW = win.w;
            win.restoreH = win.h;
            win.x = workX;
            win.y = workY;
            win.w = workW;
            win.h = workH;
            win.maximised = true;
        }
        win.layout(win.contentW(), win.contentH());
    }

    /** Gives a maximised or snapped window the place and size it had before. */
    private void unsnap(DesktopWindow win) {
        win.x = win.restoreX;
        win.y = win.restoreY;
        win.w = win.restoreW;
        win.h = win.restoreH;
        win.maximised = false;
        win.snap = SNAP_NONE;
    }

    /** Keeps a window's size within its limits and the work area, and the whole of it inside the work area. */
    private void clamp(DesktopWindow win) {
        win.w = Math.max(Math.min(win.minW, workW), Math.min(win.w, workW));
        win.h = Math.max(Math.min(win.minH, workH), Math.min(win.h, workH));
        win.x = Math.max(workX, Math.min(win.x, workX + workW - win.w));
        win.y = Math.max(workY, Math.min(win.y, workY + workH - win.h));
    }

    private static int contentX(DesktopWindow win) {
        return win.x + DesktopWindow.FRAME;
    }

    private static int contentY(DesktopWindow win) {
        return win.y + DesktopWindow.TITLE + 3;
    }

    public void tick() {
        clock++;
        toasts.removeIf(t -> clock - t.born() > TOAST_LIFE);
        for (DesktopApp app : apps.values()) {
            DesktopWindow win = byId.get(app.id());
            if (win.open && !app.available().getAsBoolean())
                close(win);
        }
        for (DesktopWindow win : stack)
            if (win.open)
                win.tick();
    }

    /** The apps the menu lists now: those that ask for a place there and are available. */
    private List<String> menuIds() {
        List<String> ids = new ArrayList<>();
        for (String id : menuEntries.keySet())
            if (apps.get(id).available().getAsBoolean())
                ids.add(id);
        return ids;
    }

    // --- drawing ----------------------------------------------------------------------------

    /** Draws the windows and the panel. Call it after the wallpaper, widgets and icons, before anything meant to sit on top. */
    public void render(GuiGraphics g, int mx, int my, float partial) {
        pointerX = mx;
        pointerY = my;
        boolean menuHit = menuOpen && onMenu(mx, my) || contextMenu.covers(mx, my) || toastAt(mx, my) >= 0;
        DesktopWindow under = menuHit ? null : windowAt(mx, my);
        DesktopWindow focus = focused();
        int z = 0;
        for (DesktopWindow win : stack) {
            if (!shown(win))
                continue;
            g.pose().pushPose();
            g.pose().translate(0, 0, z += 40);
            drawWindow(g, win, win == under ? mx : -1000, win == under ? my : -1000, partial, win == focus);
            g.pose().popPose();
        }
        if (snapPreview != SNAP_NONE && dragging != null) {
            int[] r = snapRect(snapPreview);
            g.pose().pushPose();
            g.pose().translate(0, 0, z + 20);
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], (skin.accent() & 0x00FFFFFF) | 0x28000000);
            g.renderOutline(r[0], r[1], r[2], r[3], (skin.accent() & 0x00FFFFFF) | 0xC0000000);
            g.pose().popPose();
        }
        drawBar(g, mx, my, partial, focus);
        if (menuOpen) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            drawMenu(g, mx, my, partial);
            g.pose().popPose();
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 430);
        drawToasts(g);
        g.pose().popPose();
        if (contextMenu.isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 460);
            contextMenu.render(g, skin, mx, my, partial);
            g.pose().popPose();
        }
    }

    /** The toasts, newest at the bottom of the stack, down from the work area's top-right corner. */
    private void drawToasts(GuiGraphics g) {
        var font = ui.font();
        int y = workY + 3;
        for (Toast t : toasts) {
            long age = clock - t.born();
            int alpha = age > TOAST_LIFE - TOAST_FADE ? (int) (0xFF * (TOAST_LIFE - age) / (float) TOAST_FADE) : 0xFF;
            if (alpha < 8)
                continue;
            var lines = font.split(t.text(), TOAST_W - 10);
            int h = 6 + Math.min(2, lines.size()) * 10;
            int x = workX + workW - TOAST_W - 3;
            skin.plaque(g, x, y, TOAST_W, h);
            g.fill(x + 2, y + 2, x + 4, y + h - 2, (skin.accent() & 0x00FFFFFF) | (alpha << 24));
            for (int i = 0; i < Math.min(2, lines.size()); i++)
                g.drawString(font, lines.get(i), x + 7, y + 4 + i * 10, (skin.ink() & 0x00FFFFFF) | (alpha << 24), false);
            y += h + 2;
        }
    }

    /** Index of the toast under the pointer, or -1. Mirrors {@link #drawToasts}. */
    private int toastAt(double mx, double my) {
        var font = ui.font();
        int y = workY + 3;
        for (int i = 0; i < toasts.size(); i++) {
            Toast t = toasts.get(i);
            if (clock - t.born() > TOAST_LIFE - TOAST_FADE / 2)
                continue;
            int h = 6 + Math.min(2, font.split(t.text(), TOAST_W - 10).size()) * 10;
            int x = workX + workW - TOAST_W - 3;
            if (mx >= x && mx < x + TOAST_W && my >= y && my < y + h)
                return i;
            y += h + 2;
        }
        return -1;
    }

    private void drawWindow(GuiGraphics g, DesktopWindow win, int mx, int my, float partial, boolean focus) {
        skin.panel(g, win.x, win.y, win.w, win.h);
        skin.plaque(g, win.x + 3, win.y + 3, win.w - 6, DesktopWindow.TITLE - 3);
        if (focus)
            g.fill(win.x + 4, win.y + DesktopWindow.TITLE, win.x + win.w - 4, win.y + DesktopWindow.TITLE + 1, skin.accent());
        win.glyph().draw(g, win.x + 6, win.y + 4, focus ? skin.accent() : skin.dim());
        var font = ui.font();
        int buttons = win.resizable ? 55 : 16;
        g.drawString(font, font.plainSubstrByWidth(win.title().getString(), win.w - 20 - buttons), win.x + 17, win.y + 5,
                focus ? skin.ink() : skin.dim(), false);
        Chrome c = chrome.get(win);
        c.close.setPosition(win.x + win.w - 16, win.y + 3);
        c.close.render(g, mx, my, partial);
        if (win.resizable) {
            c.max.setPosition(win.x + win.w - 29, win.y + 3);
            c.max.render(g, mx, my, partial);
            c.min.setPosition(win.x + win.w - 42, win.y + 3);
            c.full.setPosition(win.x + win.w - 55, win.y + 3);
            c.full.render(g, mx, my, partial);
        } else {
            c.min.setPosition(win.x + win.w - 29, win.y + 3);
        }
        c.min.render(g, mx, my, partial);

        int cx = contentX(win), cy = contentY(win), cw = win.contentW(), ch = win.contentH();
        g.enableScissor(cx, cy, cx + cw, cy + ch);
        ui.begin(g, skin, cx, cy, cw, ch);
        win.render(ui, mx - cx, my - cy);
        win.positionWidgets(cx, cy);
        win.renderWidgets(g, mx, my, partial);
        g.disableScissor();
        if (win.resizable && !win.maximised && !win.fullscreen) {
            int gx = win.x + win.w - 8, gy = win.y + win.h - 8;
            for (int i = 0; i < 3; i++)
                g.fill(gx + 5 - i * 2, gy + 5, gx + 6 - i * 2, gy + 6, focus ? skin.ink() : skin.dim());
            g.fill(gx + 5, gy + 3, gx + 6, gy + 4, focus ? skin.ink() : skin.dim());
            g.fill(gx + 3, gy + 5, gx + 4, gy + 6, focus ? skin.ink() : skin.dim());
            g.fill(gx + 5, gy + 1, gx + 6, gy + 2, focus ? skin.ink() : skin.dim());
        }
    }

    private void drawBar(GuiGraphics g, int mx, int my, float partial, DesktopWindow focus) {
        skin.plaque(g, barX, barY, barW, barH);
        launcher.setPosition(barX + BAR_PAD, barY + 3);
        launcher.render(g, mx, my, partial);
        int trayW = tray == null ? 0 : tray.width();
        int right = barX + barW - BAR_PAD - SNAP - 4;
        if (tray != null)
            tray.paint(g, right - trayW, barY, trayW, barH);
        showDesktop.setPosition(barX + barW - BAR_PAD - SNAP, barY + 3);
        showDesktop.render(g, mx, my, partial);

        int open = 0;
        for (DesktopWindow win : stack)
            if (win.open)
                open++;
        int from = barX + BAR_PAD + LAUNCHER_W + 6, room = right - trayW - 6 - from;
        int each = open == 0 ? TASK_W : Math.min(TASK_W, room / open);
        int x = from;
        for (DesktopWindow win : stack) {
            if (!win.open)
                continue;
            Chrome c = chrome.get(win);
            c.task.setPosition(x, barY + 3);
            c.task.setWidth(each - 2);
            c.task.setMessage(Component.literal(ui.font().plainSubstrByWidth(win.title().getString(), each - 10)));
            c.task.render(g, mx, my, partial);
            int lamp = win == focus ? skin.accent() : win.minimised ? 0 : skin.dim();
            if (lamp != 0)
                g.fill(x + 3, barY + 17, x + each - 5, barY + 18, lamp);
            x += each;
        }
    }

    /** One column up to six apps, then two or three. */
    private int menuColumns() {
        int n = menuIds().size();
        return n > 12 ? 3 : n > 6 ? 2 : 1;
    }

    private int menuWidth() {
        return menuColumns() * MENU_W;
    }

    private int menuHeight() {
        return 8 + (menuIds().size() + menuColumns() - 1) / menuColumns() * ENTRY_H;
    }

    private int menuY() {
        return barY - menuHeight() - 2;
    }

    private boolean onMenu(double mx, double my) {
        return menuOpen && mx >= barX + BAR_PAD && mx < barX + BAR_PAD + menuWidth() && my >= menuY() && my < menuY() + menuHeight();
    }

    private void drawMenu(GuiGraphics g, int mx, int my, float partial) {
        int x = barX + BAR_PAD, y = menuY();
        skin.panel(g, x, y, menuWidth(), menuHeight());
        List<String> ids = menuIds();
        int rows = Math.max(1, (ids.size() + menuColumns() - 1) / menuColumns()), i = 0;
        for (String id : ids) {
            int cx = x + i / rows * MENU_W, row = y + 5 + i % rows * ENTRY_H;
            apps.get(id).glyph().draw(g, cx + 8, row + 2, skin.ink());
            menuEntries.get(id).setPosition(cx + 20, row);
            menuEntries.get(id).render(g, mx, my, partial);
            i++;
        }
    }

    // --- input ------------------------------------------------------------------------------

    public boolean mouseClicked(double mx, double my, int button) {
        if (contextMenu.isOpen()) {
            if (contextMenu.mouseClicked(mx, my, button))
                return true;
            contextMenu.close();
        }
        int toast = toastAt(mx, my);
        if (toast >= 0) {
            toasts.remove(toast);
            return true;
        }
        if (menuOpen) {
            if (onMenu(mx, my)) {
                for (String id : menuIds())
                    if (menuEntries.get(id).mouseClicked(mx, my, button))
                        return true;
                return true;
            }
            if (!launcher.isMouseOver(mx, my))
                menuOpen = false;
        }
        if (my >= barY && my < barY + barH && mx >= barX && mx < barX + barW) {
            if (button == 1) {
                for (DesktopWindow win : stack)
                    if (win.open && chrome.get(win).task.isMouseOver(mx, my)) {
                        openMenu((int) mx, (int) my - 4, windowMenu(win));
                        return true;
                    }
                return true;
            }
            if (launcher.mouseClicked(mx, my, button) || showDesktop.mouseClicked(mx, my, button))
                return true;
            for (DesktopWindow win : stack)
                if (win.open && chrome.get(win).task.mouseClicked(mx, my, button))
                    return true;
            return true;
        }
        DesktopWindow win = windowAt(mx, my);
        if (win == null)
            return false;
        if (win != focused())
            raise(win);
        for (DesktopWindow other : stack)
            if (other != win)
                other.focusOn(null);
        Chrome c = chrome.get(win);
        if (c.close.mouseClicked(mx, my, button) || (win.resizable && (c.max.mouseClicked(mx, my, button) || c.full.mouseClicked(mx, my, button))) || c.min.mouseClicked(mx, my, button))
            return true;
        if (button == 1) {
            List<ContextMenu.Item> items = my < win.y + DesktopWindow.TITLE ? windowMenu(win)
                    : win.contextItems(mx - contentX(win), my - contentY(win));
            openMenu((int) mx, (int) my, items);
            return true;
        }
        if (button != 0 && my < win.y + DesktopWindow.TITLE)
            return true;
        if (win.resizable && !win.maximised && !win.fullscreen && win.snap == SNAP_NONE && (mx >= win.x + win.w - 5 || my >= win.y + win.h - 5)) {
            resizing = win;
            resizeX = mx >= win.x + win.w - 5;
            resizeY = my >= win.y + win.h - 5;
            grabX = mx - win.w;
            grabY = my - win.h;
            return true;
        }
        if (my < win.y + DesktopWindow.TITLE) {
            long now = System.currentTimeMillis();
            if (win.resizable && win == lastTitleWindow && now - lastTitleClick < DOUBLE_CLICK_MS) {
                lastTitleWindow = null;
                toggleMaximise(win);
                return true;
            }
            lastTitleWindow = win;
            lastTitleClick = now;
            dragging = win;
            grabX = mx - win.x;
            grabY = my - win.y;
            return true;
        }
        pressed = win;
        if (win.clickWidgets(mx, my, button))
            return true;
        win.mouseClicked(mx - contentX(win), my - contentY(win), button);
        return true;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            if (dragging.fullscreen) {
                // pulled out of the game-window fill: back to what it was, under the pointer
                grabX = grabX / Math.max(1, dragging.w) * dragging.fsW;
                grabY = Math.min(grabY, DesktopWindow.TITLE - 2);
                exitFullscreen(dragging);
            }
            if (dragging.maximised || dragging.snap != SNAP_NONE) {
                // pulled out of a maximised or snapped place: the window shrinks to its old size under the pointer
                grabX = grabX / Math.max(1, dragging.w) * dragging.restoreW;
                grabY = Math.min(grabY, DesktopWindow.TITLE - 2);
                unsnap(dragging);
                dragging.layout(dragging.contentW(), dragging.contentH());
            }
            dragging.x = (int) mx - (int) grabX;
            dragging.y = (int) my - (int) grabY;
            clamp(dragging);
            snapPreview = !dragging.resizable ? SNAP_NONE : mx < workX + SNAP_EDGE ? SNAP_LEFT
                    : mx >= workX + workW - SNAP_EDGE ? SNAP_RIGHT : my < workY + SNAP_EDGE ? SNAP_TOP : SNAP_NONE;
            return true;
        }
        if (resizing != null) {
            if (resizeX)
                resizing.w = (int) (mx - grabX);
            if (resizeY)
                resizing.h = (int) (my - grabY);
            clamp(resizing);
            resizing.layout(resizing.contentW(), resizing.contentH());
            return true;
        }
        if (pressed != null && shown(pressed))
            return pressed.mouseDragged(mx - contentX(pressed), my - contentY(pressed), button, dx, dy);
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        boolean taken = dragging != null || resizing != null;
        if (dragging != null && snapPreview != SNAP_NONE)
            snap(dragging, snapPreview);
        snapPreview = SNAP_NONE;
        dragging = null;
        resizing = null;
        DesktopWindow was = pressed;
        pressed = null;
        if (was != null && shown(was)) {
            was.releaseWidgets(mx, my, button);
            return was.mouseReleased(mx - contentX(was), my - contentY(was), button) || taken;
        }
        return taken;
    }

    public boolean mouseScrolled(double mx, double my, double wheel) {
        if (onMenu(mx, my))
            return true;
        DesktopWindow win = windowAt(mx, my);
        return win != null && win.mouseScrolled(mx - contentX(win), my - contentY(win), wheel);
    }

    /** The pointer moved; windows that track it (the editor's hover) read it from {@link #render}, so there is nothing to forward. */
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (contextMenu.isOpen() && key == 256) {
            contextMenu.close();
            return true;
        }
        if (menuOpen && key == 256) {
            menuOpen = false;
            return true;
        }
        DesktopWindow win = focused();
        boolean alt = (modifiers & 4) != 0, ctrl = (modifiers & 2) != 0, shift = (modifiers & 1) != 0;
        if (alt && key == 258)
            return cycle(shift);
        if (win != null && alt) {
            switch (key) {
                case 263 -> snapLeft(win);
                case 262 -> snapRight(win);
                case 265 -> {
                    if (!win.maximised)
                        toggleMaximise(win);
                }
                case 264 -> {
                    if (win.maximised || win.snap != SNAP_NONE)
                        toggleMaximise(win);
                    else
                        win.minimised = true;
                }
                default -> {
                    return win.keyPressed(key, scan, modifiers);
                }
            }
            return true;
        }
        if (win != null && ctrl && key == 87 && win.focused() == null) {
            close(win);
            return true;
        }
        return win != null && win.keyPressed(key, scan, modifiers);
    }

    /** Alt+Tab: the window at the back of those on screen comes to the front; with Shift the front one goes to the back. */
    private boolean cycle(boolean backwards) {
        List<DesktopWindow> on = new ArrayList<>();
        for (DesktopWindow win : stack)
            if (shown(win))
                on.add(win);
        if (on.size() < 2)
            return true;
        if (backwards) {
            DesktopWindow top = on.get(on.size() - 1);
            stack.remove(top);
            stack.add(stack.indexOf(on.get(0)), top);
        } else {
            raise(on.get(0));
        }
        return true;
    }

    public boolean charTyped(char c, int modifiers) {
        DesktopWindow win = focused();
        return win != null && win.charTyped(c, modifiers);
    }

    /**
     * Whether anything the desktop draws over the work area (a shown window, the launcher menu, a context menu, a toast) overlaps the
     * rectangle. The shell uses it to switch off the real slots and buttons a window covers, which the screen would draw on top of it.
     */
    public boolean overlaps(int rx, int ry, int rw, int rh) {
        for (DesktopWindow win : stack)
            if (shown(win) && rx < win.x + win.w && rx + rw > win.x && ry < win.y + win.h && ry + rh > win.y)
                return true;
        if (menuOpen) {
            int mx = barX + BAR_PAD;
            if (rx < mx + menuWidth() && rx + rw > mx && ry < menuY() + menuHeight() && ry + rh > menuY())
                return true;
        }
        if (contextMenu.overlaps(rx, ry, rw, rh))
            return true;
        var font = ui.font();
        int y = workY + 3;
        for (Toast t : toasts) {
            int h = 6 + Math.min(2, font.split(t.text(), TOAST_W - 10).size()) * 10;
            int x = workX + workW - TOAST_W - 3;
            if (rx < x + TOAST_W && rx + rw > x && ry < y + h && ry + rh > y)
                return true;
            y += h + 2;
        }
        return false;
    }

    /** Whether (mx, my) is over a window, the panel or the open menu: a click there is the desktop's, never a slot's. */
    public boolean covers(double mx, double my) {
        return windowAt(mx, my) != null || onMenu(mx, my) || contextMenu.covers(mx, my) || toastAt(mx, my) >= 0 || (my >= barY && my < barY + barH && mx >= barX && mx < barX + barW);
    }
}
