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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.net.VillagerActionPayload;
import io.github.soundgoodizerfan.feedback.village.CommissionJudge;
import io.github.soundgoodizerfan.feedback.village.VillageTuning;
import io.github.soundgoodizerfan.feedback.village.VillagerMenu;
import io.github.soundgoodizerfan.feedback.village.VillagerView;
import io.github.soundgoodizerfan.feedback.village.VillagerView.GoodView;
import io.github.soundgoodizerfan.feedback.village.VillagerView.OrderView;
import io.github.soundgoodizerfan.feedback.village.VillagerView.TermView;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The villager's counter: Orders (commissions to a spec, a delivery slot and the villager's
 * answer), Goods (the shop) and Standing (rank and reputation). A wandering trader shows Goods
 * alone. Everything shown comes from the {@link VillagerView} the server sends; the screen holds
 * only which row is picked and how far a list is scrolled.
 */
public class VillagerScreen extends SkinnedScreen<VillagerMenu> {

    private static final int ORDERS = 0, GOODS = 1, SELL = 2, STANDING = 3;
    private static final int ROW = 20, ROWS = 5;
    /** Left list and right detail on Orders and Sell; the shop's list is wider. */
    private static final int LIST_W = 120, DETAIL_X = 134, DETAIL_W = 158, SHOP_W = 170, SHOP_DETAIL_X = 184, SHOP_DETAIL_W = 108;
    private static final int PAD = 8, TOP = 22;

    private int tab = -1;
    private int order, good, want, orderScroll, goodScroll, wantScroll;
    private Button deliver, buy, sell;
    private VillagerView seen = VillagerView.EMPTY;
    private long stampedAt = -1;

    public VillagerScreen(VillagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.WOOD);
        imageWidth = VillagerMenu.WIDTH;
        imageHeight = VillagerMenu.HEIGHT;
    }

    private boolean wanderer() {
        return menu.view().wanderer();
    }

    /** The tab showing; a wandering trader has no orders or standing, so those fall back to its shop. */
    private int tabKind() {
        return wanderer() && (tab == ORDERS || tab == STANDING) ? GOODS : Math.max(tab, ORDERS);
    }

    @Override
    protected void init() {
        super.init();
        if (tab < 0)
            tab = ORDERS;
        folderTabs(List.of(new FolderTabs.Def(Component.translatable("gui.feedback.villager.orders")),
                new FolderTabs.Def(Component.translatable("gui.feedback.villager.goods")),
                new FolderTabs.Def(Component.translatable("gui.feedback.villager.sell")),
                new FolderTabs.Def(Component.translatable("gui.feedback.villager.standing"))), () -> tabKind(), i -> {
                    if (!wanderer() || i == GOODS || i == SELL) {
                        tab = i;
                        skin.cue(GuiSkin.Cue.STAGE);
                    }
                });
        deliver = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.villager.deliver"),
                b -> PacketDistributor.sendToServer(new VillagerActionPayload(menu.containerId, VillagerActionPayload.DELIVER, order,
                        order < menu.view().orders().size() ? menu.view().orders().get(order).type().hashCode() : 0)),
                leftPos + DETAIL_X + 56, topPos + VillagerMenu.SLOT_Y - 1, 72, 18, null));
        sell = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.villager.sell_button"),
                b -> PacketDistributor.sendToServer(new VillagerActionPayload(menu.containerId, VillagerActionPayload.SELL, want,
                        want < menu.view().wants().size() ? menu.view().wants().get(want).id() : -1)),
                leftPos + DETAIL_X + 56, topPos + VillagerMenu.SLOT_Y - 1, 72, 18, null));
        buy = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.villager.buy"),
                b -> PacketDistributor.sendToServer(new VillagerActionPayload(menu.containerId, VillagerActionPayload.BUY, good,
                        good < menu.view().goods().size() ? menu.view().goods().get(good).id() : -1)),
                leftPos + SHOP_DETAIL_X + 6, topPos + VillagerMenu.SLOT_Y - 1, 96, 18, null));
    }

    @Override
    protected void containerTick() {
        VillagerView v = menu.view();
        if (v != seen) {
            if (v.grade() != VillagerView.NONE && v.grade() >= -7) {
                stampedAt = System.currentTimeMillis();
                int g = v.grade();
                if (g == CommissionJudge.Grade.REJECTED.ordinal() || g < 0)
                    skin.cue(GuiSkin.Cue.REJECT);
                else
                    skin.cue(GuiSkin.Cue.FINISH, null, g >= CommissionJudge.Grade.GOOD.ordinal() ? 1.15f : g <= CommissionJudge.Grade.POOR.ordinal() ? 0.88f : 1f);
            }
            seen = v;
            order = Mth.clamp(order, 0, Math.max(0, v.orders().size() - 1));
            good = Mth.clamp(good, 0, Math.max(0, v.goods().size() - 1));
            want = Mth.clamp(want, 0, Math.max(0, v.wants().size() - 1));
        }
        menu.slotLive(tabKind() == ORDERS || tabKind() == SELL);
        VillagerView view = menu.view();
        boolean orders = tabKind() == ORDERS;
        deliver.visible = orders;
        deliver.active = orders && order < view.orders().size() && (view.doneMask() & (1 << order)) == 0 && !menu.delivered().isEmpty();
        sell.visible = tabKind() == SELL;
        sell.active = sell.visible && want < view.wants().size() && !menu.delivered().isEmpty();
        buy.visible = tabKind() == GOODS;
        buy.active = good < view.goods().size() && !view.goods().get(good).locked() && view.goods().get(good).left() > 0;
    }

    // --- drawing ---

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        VillagerView v = menu.view();
        int x = leftPos, y = topPos;
        switch (tabKind()) {
            case ORDERS -> orders(g, v, x, y, mx, my);
            case GOODS -> goods(g, v, x, y, mx, my);
            case SELL -> sell(g, v, x, y, mx, my);
            default -> standing(g, v, x, y);
        }
        skin.inset(g, x + PAD, y + 128, imageWidth - 2 * PAD, 30);
        var lines = font.split(speech(v), imageWidth - 2 * PAD - 12);
        for (int i = 0; i < Math.min(2, lines.size()); i++)
            g.drawString(font, lines.get(i), x + PAD + 6, y + 132 + i * 11, skin.ink(), false);
        inventory(g, INV_X, VillagerMenu.INV_Y);
    }

    private static final int INV_X = VillagerMenu.INV_X;

    private void orders(GuiGraphics g, VillagerView v, int x, int y, int mx, int my) {
        skin.inset(g, x + PAD, y + TOP, LIST_W, ROW * ROWS + 2);
        for (int i = 0; i < ROWS && orderScroll + i < v.orders().size(); i++) {
            int idx = orderScroll + i;
            OrderView o = v.orders().get(idx);
            int ry = y + TOP + 1 + i * ROW;
            skin.row(g, x + PAD + 1, ry, LIST_W - 2, ROW, idx == order, over(mx, my, x + PAD, ry, LIST_W, ROW));
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(o.type()));
            g.renderItem(icon, x + PAD + 3, ry + 2);
            boolean done = (v.doneMask() & (1 << idx)) != 0;
            g.drawString(font, fit(icon.getHoverName(), LIST_W - 28), x + PAD + 22, ry + 2, done ? skin.dim() : skin.ink(), false);
            g.drawString(font, done ? Component.translatable("gui.feedback.villager.done") : Component.translatable("gui.feedback.villager.pay", o.pay()),
                    x + PAD + 22, ry + 11, skin.dim(), false);
        }
        scrollbar(g, leftPos + PAD + LIST_W + 2, topPos + TOP, ROW * ROWS + 2, v.orders().size(), ROWS, orderScroll);
        skin.inset(g, x + DETAIL_X, y + TOP, DETAIL_W, 70);
        if (v.orders().isEmpty()) {
            g.drawString(font, Component.translatable("gui.feedback.villager.no_orders"), x + DETAIL_X + 6, y + TOP + 6, skin.dim(), false);
        } else if (order < v.orders().size()) {
            OrderView o = v.orders().get(order);
            int ty = y + TOP + 5;
            g.drawString(font, fit(new ItemStack(BuiltInRegistries.ITEM.get(o.type())).getHoverName(), DETAIL_W - 12), x + DETAIL_X + 6, ty, skin.accent(), false);
            for (TermView t : o.terms()) {
                ty += 11;
                g.drawString(font, fit(term(t), DETAIL_W - 12), x + DETAIL_X + 6, ty, skin.ink(), false);
            }
        }
        skin.slot(g, x + VillagerMenu.SLOT_X, y + VillagerMenu.SLOT_Y);
    }

    private void goods(GuiGraphics g, VillagerView v, int x, int y, int mx, int my) {
        skin.inset(g, x + PAD, y + TOP, SHOP_W, ROW * ROWS + 2);
        for (int i = 0; i < ROWS && goodScroll + i < v.goods().size(); i++) {
            int idx = goodScroll + i;
            GoodView gv = v.goods().get(idx);
            int ry = y + TOP + 1 + i * ROW;
            skin.row(g, x + PAD + 1, ry, SHOP_W - 2, ROW, idx == good, over(mx, my, x + PAD, ry, SHOP_W, ROW));
            boolean off = gv.locked() || gv.left() == 0;
            g.renderItem(gv.stack(), x + PAD + 3, ry + 2);
            g.renderItemDecorations(font, gv.stack(), x + PAD + 3, ry + 2);
            g.drawString(font, fit(gv.stack().getHoverName(), SHOP_W - 46), x + PAD + 22, ry + 2, off ? skin.dim() : skin.ink(), false);
            g.drawString(font, gv.locked() ? Component.translatable("gui.feedback.villager.locked")
                    : Component.translatable("gui.feedback.villager.cost", gv.price(), gv.left()), x + PAD + 22, ry + 11, off ? skin.warn() : skin.dim(), false);
        }
        scrollbar(g, leftPos + PAD + SHOP_W + 2, topPos + TOP, ROW * ROWS + 2, v.goods().size(), ROWS, goodScroll);
        skin.inset(g, x + SHOP_DETAIL_X, y + TOP, SHOP_DETAIL_W, 70);
        if (v.goods().isEmpty())
            g.drawString(font, Component.translatable("gui.feedback.villager.no_goods"), x + SHOP_DETAIL_X + 6, y + TOP + 6, skin.dim(), false);
        else if (good < v.goods().size()) {
            GoodView gv = v.goods().get(good);
            g.drawString(font, Component.translatable("gui.feedback.villager.cost_line", gv.price()), x + SHOP_DETAIL_X + 6, y + TOP + 6, skin.ink(), false);
            g.drawString(font, Component.translatable("gui.feedback.villager.left", gv.left()), x + SHOP_DETAIL_X + 6, y + TOP + 17, skin.dim(), false);
        }
    }

    private void sell(GuiGraphics g, VillagerView v, int x, int y, int mx, int my) {
        skin.inset(g, x + PAD, y + TOP, LIST_W, ROW * ROWS + 2);
        for (int i = 0; i < ROWS && wantScroll + i < v.wants().size(); i++) {
            int idx = wantScroll + i;
            VillagerView.WantView w = v.wants().get(idx);
            int ry = y + TOP + 1 + i * ROW;
            skin.row(g, x + PAD + 1, ry, LIST_W - 2, ROW, idx == want, over(mx, my, x + PAD, ry, LIST_W, ROW));
            g.renderItem(w.icon(), x + PAD + 3, ry + 2);
            Component name = w.food() ? Component.translatable("village.feedback.want.food") : w.icon().getHoverName();
            g.drawString(font, fit(name, LIST_W - 28), x + PAD + 22, ry + 2, w.left() == 0 ? skin.dim() : skin.ink(), false);
            g.drawString(font, Component.translatable("gui.feedback.villager.want_line", w.price(), w.left()), x + PAD + 22, ry + 11, skin.dim(), false);
        }
        scrollbar(g, leftPos + PAD + LIST_W + 2, topPos + TOP, ROW * ROWS + 2, v.wants().size(), ROWS, wantScroll);
        skin.inset(g, x + DETAIL_X, y + TOP, DETAIL_W, 70);
        g.drawString(font, Component.translatable(v.wants().isEmpty() ? "gui.feedback.villager.no_wants" : "gui.feedback.villager.sell_hint"),
                x + DETAIL_X + 6, y + TOP + 6, skin.dim(), false);
        skin.slot(g, x + VillagerMenu.SLOT_X, y + VillagerMenu.SLOT_Y);
    }

    private void standing(GuiGraphics g, VillagerView v, int x, int y) {
        skin.inset(g, x + PAD, y + TOP, imageWidth - 2 * PAD, ROW * ROWS + 2);
        int tx = x + PAD + 8, ty = y + TOP + 8;
        g.drawString(font, Component.translatable("merchant.level." + Mth.clamp(v.level(), 1, 5)), tx, ty, skin.accent(), false);
        if (v.xpMax() > 0) {
            skin.bar(g, tx, ty + 14, 120, 6, Mth.clamp(v.xp() / (float) v.xpMax(), 0f, 1f), -1f);
            g.drawString(font, Component.translatable("gui.feedback.villager.rank_hint"), tx, ty + 24, skin.dim(), false);
        }
        int tier = 0;
        for (int t : VillageTuning.STANDING_TIERS)
            if (v.standing() >= t)
                tier++;
        g.drawString(font, v.standing() < 0 ? Component.translatable("village.feedback.standing.wary")
                : Component.translatable("village.feedback.standing." + tier), tx, ty + 42, skin.ink(), false);
        float next = tier < VillageTuning.STANDING_TIERS.length ? VillageTuning.STANDING_TIERS[tier] : 0;
        float base = tier == 0 ? 0 : VillageTuning.STANDING_TIERS[tier - 1];
        skin.bar(g, tx, ty + 56, 120, 6, next == 0 ? 1f : Mth.clamp((v.standing() - base) / (next - base), 0f, 1f), -1f);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, skin.ink(), false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        VillagerView v = menu.view();
        if (stampedAt >= 0 && v.grade() >= 0) {
            CommissionJudge.Grade grade = CommissionJudge.Grade.values()[v.grade()];
            GuiStyle.stamp(g, Component.translatable("village.feedback.grade." + grade.name().toLowerCase(Locale.ROOT)),
                    leftPos + DETAIL_X + 100, topPos + 62, System.currentTimeMillis() - stampedAt,
                    grade == CommissionJudge.Grade.REJECTED || grade == CommissionJudge.Grade.POOR ? 0xC83C32 : 0x2E7D32);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        super.renderTooltip(g, mx, my);
        VillagerView v = menu.view();
        if (tabKind() == GOODS)
            for (int i = 0; i < ROWS && goodScroll + i < v.goods().size(); i++)
                if (over(mx, my, leftPos + PAD, topPos + TOP + 1 + i * ROW, SHOP_W, ROW))
                    g.renderTooltip(font, v.goods().get(goodScroll + i).stack(), mx, my);
    }

    // --- words ---

    private static Component term(TermView t) {
        String fig = t.fig().name().toLowerCase(Locale.ROOT);
        Component word = Component.translatable("village.feedback.term." + fig + "." + t.band());
        return t.must() ? Component.translatable("village.feedback.must." + (t.above() ? "above" : "below"), word) : word;
    }

    private Component speech(VillagerView v) {
        int grade = v.grade();
        if (grade == VillagerView.WRONG)
            return Component.translatable("village.feedback.speech.wrong");
        if (grade == VillagerView.SOLD)
            return Component.translatable("village.feedback.speech.sold");
        if (grade == VillagerView.NOT_WANTED)
            return Component.translatable("village.feedback.speech.not_wanted");
        if (grade == VillagerView.FULL)
            return Component.translatable("village.feedback.speech.full");
        if (grade == VillagerView.SALE)
            return Component.translatable("village.feedback.speech.sale", v.paid());
        if (grade == VillagerView.POOR_PURSE)
            return Component.translatable("village.feedback.speech.purse");
        if (grade < 0)
            return Component.translatable("village.feedback.speech.greet");
        Component said = Component.translatable("village.feedback.speech." + CommissionJudge.Grade.values()[grade].name().toLowerCase(Locale.ROOT));
        if (v.fig() >= 0) {
            Fig f = Fig.values()[v.fig()];
            said = Component.empty().append(said).append(" ").append(Component.translatable(
                    "village.feedback.complaint." + (v.high() ? "high" : "low"),
                    Component.translatable("village.feedback.noun." + f.name().toLowerCase(Locale.ROOT))));
        }
        return v.paid() > 0 ? Component.empty().append(said).append(" ").append(Component.translatable("gui.feedback.villager.paid", v.paid())) : said;
    }

    private Component fit(Component text, int width) {
        String s = text.getString();
        return Component.literal(font.width(s) <= width ? s : font.plainSubstrByWidth(s, width - font.width("...")) + "...");
    }

    // --- input ---

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (settled() && button == 0) {
            VillagerView v = menu.view();
            for (int i = 0; i < ROWS; i++) {
                int ry = topPos + TOP + 1 + i * ROW;
                if (tabKind() == SELL && wantScroll + i < v.wants().size() && over((int) mx, (int) my, leftPos + PAD, ry, LIST_W, ROW)) {
                    want = wantScroll + i;
                    skin.cue(GuiSkin.Cue.SEAT);
                    return true;
                }
                if (tabKind() == ORDERS && orderScroll + i < v.orders().size() && over((int) mx, (int) my, leftPos + PAD, ry, LIST_W, ROW)) {
                    order = orderScroll + i;
                    skin.cue(GuiSkin.Cue.SEAT);
                    return true;
                }
                if (tabKind() == GOODS && goodScroll + i < v.goods().size() && over((int) mx, (int) my, leftPos + PAD, ry, SHOP_W, ROW)) {
                    good = goodScroll + i;
                    skin.cue(GuiSkin.Cue.SEAT);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (settled()) {
            VillagerView v = menu.view();
            if (tabKind() == SELL && over((int) mx, (int) my, leftPos + PAD, topPos + TOP, LIST_W, ROW * ROWS)) {
                wantScroll = Mth.clamp(wantScroll - (int) Math.signum(sy), 0, Math.max(0, v.wants().size() - ROWS));
                return true;
            }
            if (tabKind() == ORDERS && over((int) mx, (int) my, leftPos + PAD, topPos + TOP, LIST_W, ROW * ROWS)) {
                orderScroll = Mth.clamp(orderScroll - (int) Math.signum(sy), 0, Math.max(0, v.orders().size() - ROWS));
                return true;
            }
            if (tabKind() == GOODS && over((int) mx, (int) my, leftPos + PAD, topPos + TOP, SHOP_W, ROW * ROWS)) {
                goodScroll = Mth.clamp(goodScroll - (int) Math.signum(sy), 0, Math.max(0, v.goods().size() - ROWS));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
