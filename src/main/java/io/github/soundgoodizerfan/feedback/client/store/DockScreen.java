// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;
import io.github.soundgoodizerfan.feedback.logistics.store.DockMenu;
import io.github.soundgoodizerfan.feedback.logistics.store.DockView;
import io.github.soundgoodizerfan.feedback.net.DockRequestPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The dock: the aisle's stock on the left (searched by name; click a line, set a number, fetch), the retrievals waiting
 * on the right (click one to drop it), and a line saying what the carriage is doing or why it is not.
 */
public class DockScreen extends SkinnedScreen<DockMenu> {

    private static final int W = 308, H = 214, ROW = 18, ROWS = 7;
    private static final int LIST_X = 12, LIST_Y = 42, LIST_W = 188;
    private static final int QUEUE_X = 208, QUEUE_W = 88;

    private EditBox search, amount;
    private ItemStack picked = ItemStack.EMPTY;
    private int scroll, queueScroll;
    /** The stock lines matching the search, rebuilt when the view or the text changes. */
    private final List<DockView.Line> shown = new ArrayList<>();
    private DockView filtered;
    private String filteredBy = "";

    public DockScreen(DockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.WOOD);
        imageWidth = W;
        imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        filtered = null;
        search = new EditBox(font, leftPos + LIST_X, topPos + 24, LIST_W, 14, Component.translatable("screen.feedback.dock.search"));
        search.setHint(Component.translatable("screen.feedback.dock.search"));
        addRenderableWidget(search);
        int y = topPos + LIST_Y + ROWS * ROW + 6;
        amount = new EditBox(font, leftPos + LIST_X, y, 44, 14, Component.translatable("screen.feedback.dock.amount"));
        amount.setFilter(t -> t.matches("\\d{0,6}"));
        amount.setValue("64");
        addRenderableWidget(amount);
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("screen.feedback.dock.fetch"), b -> fetch(),
                leftPos + LIST_X + 50, y, 60, 14, skin::click));
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("screen.feedback.dock.all"), b -> {
            for (DockView.Line line : menu.view().stock())
                if (ItemStack.isSameItemSameComponents(line.item(), picked))
                    amount.setValue(Integer.toString(line.count()));
        }, leftPos + LIST_X + 114, y, 36, 14));
    }

    private void fetch() {
        if (picked.isEmpty() || amount.getValue().isEmpty())
            return;
        PacketDistributor.sendToServer(new DockRequestPayload(menu.containerId, picked, Integer.parseInt(amount.getValue()), -1));
    }

    private List<DockView.Line> shown() {
        DockView view = menu.view();
        String by = search.getValue().toLowerCase(Locale.ROOT);
        if (view != filtered || !by.equals(filteredBy)) {
            filtered = view;
            filteredBy = by;
            shown.clear();
            for (DockView.Line line : view.stock())
                if (line.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(by))
                    shown.add(line);
            shown.sort((a, b) -> a.item().getHoverName().getString().compareToIgnoreCase(b.item().getHoverName().getString()));
        }
        scroll = Math.max(0, Math.min(scroll, shown.size() - ROWS));
        return shown;
    }

    /** The list row under the pointer, 0 to ROWS-1, or -1. */
    private int rowAt(double mouseX, double mouseY, int x, int w) {
        double dx = mouseX - leftPos - x, dy = mouseY - topPos - LIST_Y;
        return dx >= 0 && dx < w && dy >= 0 && dy < ROWS * ROW ? (int) (dy / ROW) : -1;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        skin.panel(g, x, y, imageWidth, imageHeight);
        skin.inset(g, x + LIST_X, y + LIST_Y, LIST_W, ROWS * ROW);
        skin.inset(g, x + QUEUE_X, y + LIST_Y, QUEUE_W, ROWS * ROW);
        List<DockView.Line> lines = shown();
        int over = rowAt(mouseX, mouseY, LIST_X, LIST_W);
        for (int i = 0; i < ROWS && scroll + i < lines.size(); i++) {
            DockView.Line line = lines.get(scroll + i);
            int ry = y + LIST_Y + i * ROW;
            skin.row(g, x + LIST_X + 1, ry + 1, LIST_W - 5, ROW - 2, ItemStack.isSameItemSameComponents(line.item(), picked), i == over);
            g.renderItem(line.item(), x + LIST_X + 3, ry + 1);
            String count = Integer.toString(line.count());
            int cx = x + LIST_X + LIST_W - 8 - font.width(count);
            g.drawString(font, count, cx, ry + 5, skin.ink(), false);
            GuiStyle.marquee(g, font, line.item().getHoverName(), x + LIST_X + 23, ry + 5, cx - x - LIST_X - 27, skin.ink());
        }
        skin.scrollbar(g, x + LIST_X + LIST_W - 3, y + LIST_Y + 1, ROWS * ROW - 2, lines.size(), ROWS, scroll);
        List<DockView.Line> queue = menu.view().queue();
        int waiting = rowAt(mouseX, mouseY, QUEUE_X, QUEUE_W);
        queueScroll = Math.max(0, Math.min(queueScroll, queue.size() - ROWS));
        skin.scrollbar(g, x + QUEUE_X + QUEUE_W - 3, y + LIST_Y + 1, ROWS * ROW - 2, queue.size(), ROWS, queueScroll);
        for (int i = 0; i < ROWS && queueScroll + i < queue.size(); i++) {
            DockView.Line line = queue.get(queueScroll + i);
            int ry = y + LIST_Y + i * ROW;
            skin.row(g, x + QUEUE_X + 1, ry + 1, QUEUE_W - 5, ROW - 2, false, i == waiting);
            g.renderItem(line.item(), x + QUEUE_X + 3, ry + 1);
            g.drawString(font, "×" + line.count(), x + QUEUE_X + 23, ry + 5, skin.ink(), false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        DockView view = menu.view();
        g.drawString(font, title, LIST_X, 11, skin.ink(), false);
        g.drawString(font, Component.translatable("screen.feedback.dock.queue"), QUEUE_X, 29, skin.dim(), false);
        Component state = Component.translatable("screen.feedback.dock.state." + view.state().name().toLowerCase(Locale.ROOT));
        int y = LIST_Y + ROWS * ROW + 26;
        g.drawString(font, state, LIST_X, y, view.state() == DockView.State.READY ? skin.dim() : skin.warn(), false);
        Component trip = view.tripTicks() > 0
                ? Component.translatable("screen.feedback.dock.trip", view.racks(), (view.tripTicks() + 19) / 20)
                : Component.translatable("screen.feedback.dock.racks", view.racks());
        g.drawString(font, trip, imageWidth - LIST_X - font.width(trip), 11, skin.dim(), false);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int row = rowAt(mouseX, mouseY, LIST_X, LIST_W);
        List<DockView.Line> lines = shown();
        if (row >= 0 && scroll + row < lines.size()) {
            g.renderTooltip(font, lines.get(scroll + row).item(), mouseX, mouseY);
            return;
        }
        row = rowAt(mouseX, mouseY, QUEUE_X, QUEUE_W);
        if (row >= 0 && queueScroll + row < menu.view().queue().size())
            g.renderTooltip(font, Component.translatable("screen.feedback.dock.cancel"), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (settled() && !closing()) {
            int row = rowAt(mouseX, mouseY, LIST_X, LIST_W);
            List<DockView.Line> lines = shown();
            if (row >= 0 && scroll + row < lines.size()) {
                picked = lines.get(scroll + row).item().copy();
                GuiSkin.play(skin.click(), 1f);
                return true;
            }
            row = rowAt(mouseX, mouseY, QUEUE_X, QUEUE_W);
            if (row >= 0 && queueScroll + row < menu.view().queue().size()) {
                PacketDistributor.sendToServer(new DockRequestPayload(menu.containerId, ItemStack.EMPTY, 0, queueScroll + row));
                GuiSkin.play(skin.click(), 1f);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (rowAt(mouseX, mouseY, LIST_X, LIST_W) >= 0) {
            scroll -= (int) Math.signum(scrollY);
            shown();
            return true;
        }
        if (rowAt(mouseX, mouseY, QUEUE_X, QUEUE_W) >= 0) {
            queueScroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** A focused text box keeps the inventory key as a letter. */
    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && getFocused() instanceof EditBox box && box.canConsumeInput()) {
            if (box == amount && (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER))
                fetch();
            else
                box.keyPressed(key, scan, modifiers);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
