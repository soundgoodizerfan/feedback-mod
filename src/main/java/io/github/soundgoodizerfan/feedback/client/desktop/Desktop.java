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
 * do. It keeps the stacking order and the focus, draws each window's frame (title bar, minimise, maximise, close) and clips its
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

    private record Chrome(Button min, Button max, Button close, Button task) {
    }

    private static final int BAR_PAD = 4, LAUNCHER_W = 46, TASK_W = 58, SNAP = 6, MENU_W = 88, ENTRY_H = 14;

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
            if (win.maximised) {
                win.x = x;
                win.y = y;
                win.w = w;
                win.h = h;
                win.layout(win.contentW(), win.contentH());
            } else if (win.placed) {
                clamp(win);
            }
    }

    /** The panel's rectangle, in screen coordinates. */
    public void bar(int x, int y, int w, int h) {
        barX = x;
        barY = y;
        barW = w;
        barH = h;
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
        for (DesktopWindow win : stack) {
            win.clearWidgets();
            win.build(skin);
            Button min = GuiStyle.button(skin, Component.literal("_"), b -> win.minimised = true, 0, 0, 12, 11);
            Button max = GuiStyle.button(skin, Component.literal("o"), b -> toggleMaximise(win), 0, 0, 12, 11);
            Button close = GuiStyle.button(skin, Component.literal("x"), b -> close(win), 0, 0, 12, 11);
            Button task = GuiStyle.button(skin, Component.empty(), b -> clickTask(win), 0, 0, TASK_W - 2, 14);
            chrome.put(win, new Chrome(min, max, close, task));
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
            win.x = workX + win.startX;
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

    private void toggleMaximise(DesktopWindow win) {
        if (!win.resizable)
            return;
        if (win.maximised) {
            win.x = win.restoreX;
            win.y = win.restoreY;
            win.w = win.restoreW;
            win.h = win.restoreH;
        } else {
            win.restoreX = win.x;
            win.restoreY = win.y;
            win.restoreW = win.w;
            win.restoreH = win.h;
            win.x = workX;
            win.y = workY;
            win.w = workW;
            win.h = workH;
        }
        win.maximised = !win.maximised;
        win.layout(win.contentW(), win.contentH());
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
        DesktopWindow under = menuOpen && onMenu(mx, my) ? null : windowAt(mx, my);
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
        drawBar(g, mx, my, partial, focus);
        if (menuOpen) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            drawMenu(g, mx, my, partial);
            g.pose().popPose();
        }
    }

    private void drawWindow(GuiGraphics g, DesktopWindow win, int mx, int my, float partial, boolean focus) {
        skin.panel(g, win.x, win.y, win.w, win.h);
        skin.plaque(g, win.x + 3, win.y + 3, win.w - 6, DesktopWindow.TITLE - 3);
        if (focus)
            g.fill(win.x + 4, win.y + DesktopWindow.TITLE, win.x + win.w - 4, win.y + DesktopWindow.TITLE + 1, skin.accent());
        win.glyph().draw(g, win.x + 6, win.y + 4, focus ? skin.accent() : skin.dim());
        var font = ui.font();
        int buttons = win.resizable ? 42 : 16;
        g.drawString(font, font.plainSubstrByWidth(win.title().getString(), win.w - 20 - buttons), win.x + 17, win.y + 5,
                focus ? skin.ink() : skin.dim(), false);
        Chrome c = chrome.get(win);
        c.close.setPosition(win.x + win.w - 16, win.y + 3);
        c.close.render(g, mx, my, partial);
        if (win.resizable) {
            c.max.setPosition(win.x + win.w - 29, win.y + 3);
            c.max.render(g, mx, my, partial);
            c.min.setPosition(win.x + win.w - 42, win.y + 3);
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
        if (win.resizable && !win.maximised) {
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
        if (c.close.mouseClicked(mx, my, button) || (win.resizable && c.max.mouseClicked(mx, my, button)) || c.min.mouseClicked(mx, my, button))
            return true;
        if (button != 0 && my < win.y + DesktopWindow.TITLE)
            return true;
        if (win.resizable && !win.maximised && (mx >= win.x + win.w - 5 || my >= win.y + win.h - 5)) {
            resizing = win;
            resizeX = mx >= win.x + win.w - 5;
            resizeY = my >= win.y + win.h - 5;
            grabX = mx - win.w;
            grabY = my - win.h;
            return true;
        }
        if (my < win.y + DesktopWindow.TITLE) {
            if (!win.maximised) {
                dragging = win;
                grabX = mx - win.x;
                grabY = my - win.y;
            }
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
            dragging.x = (int) mx - (int) grabX;
            dragging.y = (int) my - (int) grabY;
            clamp(dragging);
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
        if (menuOpen && key == 256) {
            menuOpen = false;
            return true;
        }
        DesktopWindow win = focused();
        return win != null && win.keyPressed(key, scan, modifiers);
    }

    public boolean charTyped(char c, int modifiers) {
        DesktopWindow win = focused();
        return win != null && win.charTyped(c, modifiers);
    }

    /** Whether (mx, my) is over a window, the panel or the open menu: a click there is the desktop's, never a slot's. */
    public boolean covers(double mx, double my) {
        return windowAt(mx, my) != null || onMenu(mx, my) || (my >= barY && my < barY + barH && mx >= barX && mx < barX + barW);
    }
}
