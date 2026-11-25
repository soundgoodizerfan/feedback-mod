// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreMenu;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreTuning;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreView;
import io.github.soundgoodizerfan.feedback.net.DockNamePayload;
import io.github.soundgoodizerfan.feedback.net.StoreRequestPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A warehouse screen, a dock's or the catalogue desk's: the stock on the left (searched by name; click a line, set a
 * number, fetch), the retrievals waiting on the right (click one to drop it), and a line saying what is going on or why
 * nothing is. With several docks, hovering a stock line says which hold it and hovering the status line lists them.
 */
public class StoreScreen<M extends StoreMenu> extends SkinnedScreen<M> {

    private static final int W = 308, H = 214, ROW = 18, ROWS = 7;
    private static final int LIST_X = 12, LIST_Y = 42, LIST_W = 188;
    private static final int QUEUE_X = 208, QUEUE_W = 88;
    private static final int STATUS_Y = LIST_Y + ROWS * ROW + 26;
    /** Most docks a tooltip lists before it counts the rest. */
    private static final int LISTED = 12;

    private final boolean desk;
    private EditBox search, amount;
    /** A dock's name, typed where the title stands; null on the desk. */
    private EditBox name;
    /** The name the server last gave, to tell typing from news. */
    private String named = "";
    private ItemStack picked = ItemStack.EMPTY;
    private int scroll, queueScroll;
    /** The stock lines matching the search, rebuilt when the list or the text changes. */
    private final List<StoreView.Line> shown = new ArrayList<>();
    private int filtered = -1;
    private String filteredBy = "";
    private int namedAt;

    private StoreScreen(M menu, Inventory inventory, Component title, boolean desk) {
        super(menu, inventory, title, GuiSkin.WOOD);
        this.desk = desk;
        imageWidth = W;
        imageHeight = H;
    }

    public static <M extends StoreMenu> StoreScreen<M> dock(M menu, Inventory inventory, Component title) {
        return new StoreScreen<>(menu, inventory, title, false);
    }

    public static <M extends StoreMenu> StoreScreen<M> desk(M menu, Inventory inventory, Component title) {
        return new StoreScreen<>(menu, inventory, title, true);
    }

    @Override
    protected void init() {
        super.init();
        filtered = -1;
        if (!desk) {
            name = new EditBox(font, leftPos + LIST_X, topPos + 11, 130, 10, Component.translatable("screen.feedback.dock.name"));
            name.setBordered(false);
            name.setMaxLength(StoreTuning.DOCK_NAME_MAX);
            name.setTextColor(skin.ink());
            name.setHint(title.copy().append(" ✎"));
            name.setValue(named);
            addRenderableWidget(name);
        }
        search =new EditBox(font, leftPos + LIST_X, topPos + 24, LIST_W, 14, Component.translatable("screen.feedback.dock.search"));
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
            for (StoreView.Line line : menu.stock())
                if (ItemStack.isSameItemSameComponents(line.item(), picked))
                    amount.setValue(Integer.toString(line.count()));
        }, leftPos + LIST_X + 114, y, 36, 14));
    }

    /** Sends the typed name if it differs from the dock's. */
    private void commitName() {
        if (name != null && !name.getValue().trim().equals(named)) {
            named = name.getValue().trim();
            PacketDistributor.sendToServer(new DockNamePayload(menu.containerId, named));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (name == null || menu.view().sources().isEmpty())
            return;
        String now = menu.view().sources().get(0).name().getString();
        if (menu.version() != namedAt) {
            namedAt = menu.version();
            if (!name.isFocused()) {
                named = now;
                name.setValue(now);
            }
        }
        // Leaving the box keeps what was typed.
        if (!name.isFocused())
            commitName();
    }

    @Override
    public void onClose() {
        commitName();
        super.onClose();
    }

    private void fetch() {
        if (picked.isEmpty() || amount.getValue().isEmpty())
            return;
        PacketDistributor.sendToServer(new StoreRequestPayload(menu.containerId, picked, Integer.parseInt(amount.getValue()), -1));
    }

    private List<StoreView.Line> shown() {
        String by = search.getValue().toLowerCase(Locale.ROOT);
        if (menu.version() != filtered || !by.equals(filteredBy)) {
            filtered = menu.version();
            filteredBy = by;
            shown.clear();
            for (StoreView.Line line : menu.stock())
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
        List<StoreView.Line> lines = shown();
        int over = rowAt(mouseX, mouseY, LIST_X, LIST_W);
        for (int i = 0; i < ROWS && scroll + i < lines.size(); i++) {
            StoreView.Line line = lines.get(scroll + i);
            int ry = y + LIST_Y + i * ROW;
            skin.row(g, x + LIST_X + 1, ry + 1, LIST_W - 5, ROW - 2, ItemStack.isSameItemSameComponents(line.item(), picked), i == over);
            g.renderItem(line.item(), x + LIST_X + 3, ry + 1);
            String count = Integer.toString(line.count());
            int cx = x + LIST_X + LIST_W - 8 - font.width(count);
            g.drawString(font, count, cx, ry + 5, skin.ink(), false);
            GuiStyle.marquee(g, font, line.item().getHoverName(), x + LIST_X + 23, ry + 5, cx - x - LIST_X - 27, skin.ink());
        }
        skin.scrollbar(g, x + LIST_X + LIST_W - 3, y + LIST_Y + 1, ROWS * ROW - 2, lines.size(), ROWS, scroll);
        List<StoreView.Queued> queue = menu.view().queue();
        int waiting = rowAt(mouseX, mouseY, QUEUE_X, QUEUE_W);
        queueScroll = Math.max(0, Math.min(queueScroll, queue.size() - ROWS));
        skin.scrollbar(g, x + QUEUE_X + QUEUE_W - 3, y + LIST_Y + 1, ROWS * ROW - 2, queue.size(), ROWS, queueScroll);
        for (int i = 0; i < ROWS && queueScroll + i < queue.size(); i++) {
            StoreView.Queued line = queue.get(queueScroll + i);
            int ry = y + LIST_Y + i * ROW;
            skin.row(g, x + QUEUE_X + 1, ry + 1, QUEUE_W - 5, ROW - 2, false, i == waiting);
            g.renderItem(line.item(), x + QUEUE_X + 3, ry + 1);
            g.drawString(font, "×" + line.count(), x + QUEUE_X + 23, ry + 5, skin.ink(), false);
        }
    }

    private static Component stateWords(StoreView.State state) {
        return Component.translatable("screen.feedback.dock.state." + state.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        StoreView view = menu.view();
        if (desk)
            g.drawString(font, title, LIST_X, 11, skin.ink(), false);
        g.drawString(font, Component.translatable("screen.feedback.dock.queue"), QUEUE_X, 29, skin.dim(), false);
        Component status = stateWords(view.state());
        boolean fine = view.state() == StoreView.State.READY;
        Component corner = Component.empty();
        if (desk) {
            if (fine) {
                int held = 0;
                for (StoreView.Source s : view.sources())
                    if (s.state() != StoreView.State.READY)
                        held++;
                fine = held == 0;
                status = Component.translatable(fine ? "screen.feedback.desk.ready" : "screen.feedback.desk.held", fine ? view.sources().size() : held);
            }
            corner = Component.translatable("screen.feedback.desk.docks", view.sources().size());
        } else if (!view.sources().isEmpty()) {
            StoreView.Source own = view.sources().get(0);
            corner = own.tripTicks() > 0
                    ? Component.translatable("screen.feedback.dock.trip", own.racks(), (own.tripTicks() + 19) / 20)
                    : Component.translatable("screen.feedback.dock.racks", own.racks());
        }
        g.drawString(font, status, LIST_X, STATUS_Y, fine ? skin.dim() : skin.warn(), false);
        g.drawString(font, corner, imageWidth - LIST_X - font.width(corner), 11, skin.dim(), false);
    }

    private Component sourceName(int source) {
        List<StoreView.Source> sources = menu.view().sources();
        return source < sources.size() ? sources.get(source).name() : Component.empty();
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int row = rowAt(mouseX, mouseY, LIST_X, LIST_W);
        List<StoreView.Line> lines = shown();
        if (row >= 0 && scroll + row < lines.size()) {
            StoreView.Line line = lines.get(scroll + row);
            if (!desk) {
                g.renderTooltip(font, line.item(), mouseX, mouseY);
                return;
            }
            List<Component> text = new ArrayList<>();
            text.add(line.item().getHoverName());
            for (int i = 0; i < line.shares().size() && i < LISTED; i++)
                text.add(Component.translatable("screen.feedback.desk.share", sourceName(line.shares().get(i).source()),
                        line.shares().get(i).count()).withStyle(net.minecraft.ChatFormatting.GRAY));
            if (line.shares().size() > LISTED)
                text.add(Component.translatable("screen.feedback.desk.more", line.shares().size() - LISTED));
            g.renderComponentTooltip(font, text, mouseX, mouseY);
            return;
        }
        row = rowAt(mouseX, mouseY, QUEUE_X, QUEUE_W);
        List<StoreView.Queued> queue = menu.view().queue();
        if (row >= 0 && queueScroll + row < queue.size()) {
            StoreView.Queued line = queue.get(queueScroll + row);
            List<Component> text = new ArrayList<>();
            text.add(line.item().getHoverName());
            if (desk)
                text.add(sourceName(line.source()).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            if (line.order() > 0)
                text.add(Component.translatable("screen.feedback.desk.order", line.order()).withStyle(net.minecraft.ChatFormatting.GRAY));
            text.add(Component.translatable("screen.feedback.dock.cancel").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, text, mouseX, mouseY);
            return;
        }
        double dx = mouseX - leftPos - LIST_X, dy = mouseY - topPos - STATUS_Y;
        List<StoreView.Source> sources = menu.view().sources();
        if (desk && !sources.isEmpty() && dx >= 0 && dx < LIST_W && dy >= -2 && dy < 10) {
            List<Component> text = new ArrayList<>();
            for (int i = 0; i < sources.size() && i < LISTED; i++)
                text.add(Component.translatable("screen.feedback.desk.source", sources.get(i).name(), stateWords(sources.get(i).state())));
            if (sources.size() > LISTED)
                text.add(Component.translatable("screen.feedback.desk.more", sources.size() - LISTED));
            g.renderComponentTooltip(font, text, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (settled() && !closing()) {
            int row = rowAt(mouseX, mouseY, LIST_X, LIST_W);
            List<StoreView.Line> lines = shown();
            if (row >= 0 && scroll + row < lines.size()) {
                picked = lines.get(scroll + row).item().copy();
                GuiSkin.play(skin.click(), 1f);
                return true;
            }
            row = rowAt(mouseX, mouseY, QUEUE_X, QUEUE_W);
            List<StoreView.Queued> queue = menu.view().queue();
            if (row >= 0 && queueScroll + row < queue.size()) {
                PacketDistributor.sendToServer(new StoreRequestPayload(menu.containerId, ItemStack.EMPTY, 0, queue.get(queueScroll + row).key()));
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
            boolean enter = key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
            if (box == amount && enter)
                fetch();
            else if (box == name && enter)
                setFocused(null);
            else
                box.keyPressed(key, scan, modifiers);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
