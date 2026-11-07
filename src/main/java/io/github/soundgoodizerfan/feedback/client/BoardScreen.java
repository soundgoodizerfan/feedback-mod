/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.village.BoardMenu;
import io.github.soundgoodizerfan.feedback.village.BoardView;
import io.github.soundgoodizerfan.feedback.village.CommissionJudge;
import io.github.soundgoodizerfan.feedback.village.VillageTuning;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * The village's ledger board: every open order of the villagers in range grouped by type, the village's contracts with a slot to deliver a
 * piece into, and notices (standing, what the next tier opens, market day, raids). Read-only except the delivery.
 */
public class BoardScreen extends SkinnedScreen<BoardMenu> {

    private static final int ORDERS = 0, CONTRACTS = 1, NOTICES = 2;
    private static final int ROW = 20, ROWS = 4, PAD = 8, TOP = 8, LIST_W = 250;

    private int tab;
    private int picked;
    private final int[] scroll = new int[3];
    private Button deliver, banner;
    private BoardView seen = BoardView.EMPTY;
    private long stampedAt = -1;

    public BoardScreen(BoardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.WOOD);
        imageWidth = BoardMenu.WIDTH;
        imageHeight = BoardMenu.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        folderTabs(List.of(new FolderTabs.Def(Component.translatable("gui.feedback.board.orders")),
                new FolderTabs.Def(Component.translatable("gui.feedback.board.contracts")),
                new FolderTabs.Def(Component.translatable("gui.feedback.board.notices"))), () -> tab, i -> {
                    tab = i;
                    skin.cue(GuiSkin.Cue.STAGE);
                });
        deliver = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.board.deliver"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, picked),
                leftPos + BoardMenu.SLOT_X + 24, topPos + BoardMenu.SLOT_Y - 1, 72, 18, null));
        banner = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.board.banner"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BoardMenu.BANNER),
                leftPos + BoardMenu.SLOT_X + 102, topPos + BoardMenu.SLOT_Y - 1, 72, 18));
    }

    @Override
    protected void containerTick() {
        BoardView v = menu.view();
        if (v != seen) {
            if (v.grade() != -1) {
                stampedAt = System.currentTimeMillis();
                if (v.grade() < 0 || v.grade() == CommissionJudge.Grade.REJECTED.ordinal())
                    skin.cue(GuiSkin.Cue.REJECT);
                else
                    skin.cue(GuiSkin.Cue.FINISH, null, v.grade() >= CommissionJudge.Grade.GOOD.ordinal() ? 1.15f : 0.9f);
            }
            seen = v;
            picked = Mth.clamp(picked, 0, Math.max(0, v.contracts().size() - 1));
        }
        boolean contracts = tab == CONTRACTS;
        menu.slotLive(contracts);
        deliver.visible = contracts;
        banner.visible = contracts;
        deliver.active = contracts && !v.contracts().isEmpty() && menu.getSlot(0).hasItem();
        banner.active = contracts && menu.getSlot(0).hasItem();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        if (tab == CONTRACTS)
            skin.slot(g, leftPos + BoardMenu.SLOT_X, topPos + BoardMenu.SLOT_Y);
        inventory(g, BoardMenu.INV_X, BoardMenu.INV_Y);
        BoardView v = menu.view();
        int x = leftPos + PAD, y = topPos + TOP;
        skin.inset(g, x, y, LIST_W, ROW * ROWS + 2);
        switch (tab) {
            case ORDERS -> orders(g, v, x, y, mx, my);
            case CONTRACTS -> contracts(g, v, x, y, mx, my);
            default -> notices(g, v, x, y);
        }
    }

    private Component fit(Component c, int w) {
        return Component.literal(font.substrByWidth(c, w).getString());
    }

    private void orders(GuiGraphics g, BoardView v, int x, int y, int mx, int my) {
        for (int i = 0; i < ROWS && scroll[ORDERS] + i < v.orders().size(); i++) {
            BoardView.OrderLine o = v.orders().get(scroll[ORDERS] + i);
            int ry = y + 1 + i * ROW;
            g.drawString(font, fit(Component.translatable("village.feedback.board.order_head", o.type(), o.profession()), LIST_W - 50), x + 4, ry + 2,
                    o.done() ? skin.dim() : skin.ink(), false);
            g.drawString(font, o.done() ? Component.translatable("gui.feedback.villager.done") : Component.translatable("gui.feedback.villager.pay", o.pay()),
                    x + LIST_W - 4 - font.width(o.done() ? Component.translatable("gui.feedback.villager.done") : Component.translatable("gui.feedback.villager.pay", o.pay())),
                    ry + 2, skin.dim(), false);
            g.drawString(font, fit(o.text(), LIST_W - 10), x + 4, ry + 11, skin.dim(), false);
        }
        if (v.orders().isEmpty())
            g.drawString(font, Component.translatable("gui.feedback.board.no_orders"), x + 6, y + 6, skin.dim(), false);
        scrollbar(g, x + LIST_W + 2, y, ROW * ROWS + 2, v.orders().size(), ROWS, scroll[ORDERS]);
    }

    private void contracts(GuiGraphics g, BoardView v, int x, int y, int mx, int my) {
        for (int i = 0; i < ROWS && scroll[CONTRACTS] + i < v.contracts().size(); i++) {
            int idx = scroll[CONTRACTS] + i;
            BoardView.ContractLine c = v.contracts().get(idx);
            int ry = y + 1 + i * ROW;
            skin.row(g, x + 1, ry, LIST_W - 2, ROW, idx == picked, mx >= x && mx < x + LIST_W && my >= ry && my < ry + ROW);
            g.drawString(font, fit(c.what(), LIST_W - 10), x + 4, ry + 2, c.delivered() >= c.count() ? skin.dim() : skin.ink(), false);
            g.drawString(font, fit(Component.translatable("village.feedback.board.contract_line", c.delivered(), c.count(), c.min(), c.daysLeft(), c.pay()),
                    LIST_W - 10), x + 4, ry + 11, skin.dim(), false);
        }
        if (v.contracts().isEmpty())
            g.drawString(font, Component.translatable("gui.feedback.board.no_contracts"), x + 6, y + 6, skin.dim(), false);
        scrollbar(g, x + LIST_W + 2, y, ROW * ROWS + 2, v.contracts().size(), ROWS, scroll[CONTRACTS]);
    }

    private void notices(GuiGraphics g, BoardView v, int x, int y) {
        int ty = y + 6;
        for (Component n : v.notices()) {
            for (FormattedText line : font.getSplitter().splitLines(n, LIST_W - 12, net.minecraft.network.chat.Style.EMPTY)) {
                if (ty > y + ROW * ROWS - 8)
                    return;
                g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(line), x + 6, ty, skin.ink(), false);
                ty += 10;
            }
            ty += 3;
        }
        int tier = v.tier();
        float next = tier < VillageTuning.STANDING_TIERS.length ? VillageTuning.STANDING_TIERS[tier] : 0;
        float base = tier == 0 ? 0 : VillageTuning.STANDING_TIERS[tier - 1];
        skin.bar(g, x + 6, y + ROW * ROWS - 8, 120, 5, next == 0 ? 1f : Mth.clamp((v.mean() - base) / (next - base), 0f, 1f), -1f);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // No title: the folder tabs name the page.
        if (tab == CONTRACTS && stampedAt >= 0) {
            BoardView v = menu.view();
            if (v.paid() > 0)
                g.drawString(font, Component.translatable("village.feedback.board.paid", v.paid()), BoardMenu.SLOT_X + 24, BoardMenu.SLOT_Y + 22, skin.accent(), false);
            else if (v.grade() < -1)
                g.drawString(font, Component.translatable("village.feedback.board.refused"), BoardMenu.SLOT_X + 24, BoardMenu.SLOT_Y + 22, skin.warn(), false);
        }
        g.drawString(font, playerInventoryTitle, BoardMenu.INV_X, BoardMenu.INV_Y - 11, skin.dim(), false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        BoardView v = menu.view();
        if (tab == CONTRACTS && stampedAt >= 0 && v.grade() >= 0) {
            CommissionJudge.Grade grade = CommissionJudge.Grade.values()[Math.min(v.grade(), 4)];
            GuiStyle.stamp(g, Component.translatable("village.feedback.grade." + grade.name().toLowerCase(Locale.ROOT)),
                    leftPos + 200, topPos + BoardMenu.SLOT_Y + 8, System.currentTimeMillis() - stampedAt,
                    grade == CommissionJudge.Grade.POOR ? 0xC83C32 : 0x2E7D32);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (settled() && tab == CONTRACTS) {
            int x = leftPos + PAD, y = topPos + TOP;
            if (mx >= x && mx < x + LIST_W && my >= y && my < y + ROW * ROWS) {
                int idx = scroll[CONTRACTS] + (int) ((my - y) / ROW);
                if (idx < menu.view().contracts().size()) {
                    picked = idx;
                    skin.cue(GuiSkin.Cue.SEAT);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (settled() && tab <= CONTRACTS) {
            int total = tab == ORDERS ? menu.view().orders().size() : menu.view().contracts().size();
            scroll[tab] = Mth.clamp(scroll[tab] - (int) Math.signum(sy), 0, Math.max(0, total - ROWS));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
