// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Everything the villager screen shows, worded for the client: the orders as bands and never as
 * numbers, the shop as stacks and prices, standing and rank. Built on the server when the screen
 * opens and after each action; the hidden quirks and the spans behind the bands never cross.
 *
 * @param wanderer  a wandering trader: shop only, no orders and no standing
 * @param xp        experience within the current level, and {@code xpMax} what the level takes; 0 when no level remains
 * @param standing  vanilla reputation with the player
 * @param doneMask  bit {@code i} set when order {@code i} is delivered today
 * @param grade     the last delivery's {@link CommissionJudge.Grade} ordinal, or {@link #NONE} or {@link #WRONG}
 * @param fig       the figure the villager complained of, as an ordinal, or -1; {@code Fig.values().length + i} when it is ask {@code i} of the order
 *                  the delivery was for
 * @param high      whether that figure was too high
 * @param paid      emeralds the last delivery paid
 * @param hint      the hint the player asked for on the selected order, worded; empty when none
 */
public record VillagerView(boolean wanderer, int level, int xp, int xpMax, int standing, List<OrderView> orders, int doneMask,
                           List<GoodView> goods, List<WantView> wants, int grade, int fig, boolean high, int paid,
                           List<net.minecraft.network.chat.Component> hint) {

    public static final int NONE = -1, WRONG = -2, SOLD = -3, POOR_PURSE = -4, NOT_WANTED = -5, FULL = -6, SALE = -7;

    /** One term as the client words it. */
    public record TermView(Fig fig, int band, boolean must, boolean above) {
    }

    /** @param pay emeralds a Good delivery pays before saturation, which depends on the material it is made of; @param asks the parts a tool order names */
    public record OrderView(ResourceLocation type, int pay, List<TermView> terms, List<Order.Ask> asks) {
    }

    /** @param id index in the goods table, which the server checks a purchase against; @param locked the villager wants more standing before selling it; @param left how many are still for sale today */
    public record GoodView(ItemStack stack, int price, int left, boolean locked, int id) {
    }

    /** @param food any food is bought, drawn as an apple; @param left how many more are wanted today; @param id index in the buys table */
    public record WantView(ItemStack icon, boolean food, int price, int left, int id) {
    }

    public VillagerView withResult(int grade, int fig, boolean high, int paid) {
        return new VillagerView(wanderer, level, xp, xpMax, standing, orders, doneMask, goods, wants, grade, fig, high, paid, List.of());
    }

    public VillagerView withHint(List<net.minecraft.network.chat.Component> lines) {
        return new VillagerView(wanderer, level, xp, xpMax, standing, orders, doneMask, goods, wants, grade, fig, high, paid, lines);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerView> STREAM_CODEC = StreamCodec.of(VillagerView::write, VillagerView::read);

    private static void write(RegistryFriendlyByteBuf b, VillagerView v) {
        b.writeBoolean(v.wanderer);
        b.writeVarInt(v.level);
        b.writeVarInt(v.xp);
        b.writeVarInt(v.xpMax);
        b.writeVarInt(v.standing);
        b.writeVarInt(v.orders.size());
        for (OrderView o : v.orders) {
            b.writeResourceLocation(o.type());
            b.writeVarInt(o.pay());
            b.writeVarInt(o.terms().size());
            for (TermView t : o.terms()) {
                b.writeVarInt(t.fig().ordinal());
                b.writeVarInt(t.band());
                b.writeBoolean(t.must());
                b.writeBoolean(t.above());
            }
            b.writeVarInt(o.asks().size());
            for (Order.Ask a : o.asks()) {
                b.writeUtf(a.role());
                b.writeBoolean(a.material() != null);
                if (a.material() != null)
                    b.writeResourceLocation(a.material());
                else
                    b.writeVarInt(a.kind().ordinal());
            }
        }
        b.writeVarInt(v.doneMask);
        b.writeVarInt(v.goods.size());
        for (GoodView g : v.goods) {
            ItemStack.STREAM_CODEC.encode(b, g.stack());
            b.writeVarInt(g.price());
            b.writeVarInt(g.left());
            b.writeBoolean(g.locked());
            b.writeVarInt(g.id());
        }
        b.writeVarInt(v.wants.size());
        for (WantView w : v.wants) {
            ItemStack.STREAM_CODEC.encode(b, w.icon());
            b.writeBoolean(w.food());
            b.writeVarInt(w.price());
            b.writeVarInt(w.left());
            b.writeVarInt(w.id());
        }
        b.writeVarInt(v.grade + 8);
        b.writeVarInt(v.fig + 1);
        b.writeBoolean(v.high);
        b.writeVarInt(v.paid);
        b.writeVarInt(v.hint.size());
        for (net.minecraft.network.chat.Component c : v.hint)
            net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(b, c);
    }

    private static VillagerView read(RegistryFriendlyByteBuf b) {
        boolean wanderer = b.readBoolean();
        int level = b.readVarInt(), xp = b.readVarInt(), xpMax = b.readVarInt(), standing = b.readVarInt();
        int n = b.readVarInt();
        List<OrderView> orders = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ResourceLocation type = b.readResourceLocation();
            int pay = b.readVarInt();
            int k = b.readVarInt();
            List<TermView> terms = new ArrayList<>();
            for (int j = 0; j < k; j++)
                terms.add(new TermView(Fig.values()[b.readVarInt()], b.readVarInt(), b.readBoolean(), b.readBoolean()));
            int na = b.readVarInt();
            List<Order.Ask> asks = new ArrayList<>();
            for (int j = 0; j < na; j++) {
                String role = b.readUtf();
                asks.add(b.readBoolean() ? new Order.Ask(role, b.readResourceLocation(), null)
                        : new Order.Ask(role, null, io.github.soundgoodizerfan.feedback.process.Material.Kind.values()[b.readVarInt()]));
            }
            orders.add(new OrderView(type, pay, List.copyOf(terms), List.copyOf(asks)));
        }
        int done = b.readVarInt();
        int m = b.readVarInt();
        List<GoodView> goods = new ArrayList<>();
        for (int i = 0; i < m; i++)
            goods.add(new GoodView(ItemStack.STREAM_CODEC.decode(b), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readVarInt()));
        int w = b.readVarInt();
        List<WantView> wants = new ArrayList<>();
        for (int i = 0; i < w; i++)
            wants.add(new WantView(ItemStack.STREAM_CODEC.decode(b), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
        int grade = b.readVarInt() - 8, fig = b.readVarInt() - 1;
        boolean high = b.readBoolean();
        int paid = b.readVarInt();
        int h = b.readVarInt();
        List<net.minecraft.network.chat.Component> hint = new ArrayList<>();
        for (int i = 0; i < h; i++)
            hint.add(net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(b));
        return new VillagerView(wanderer, level, xp, xpMax, standing, List.copyOf(orders), done, List.copyOf(goods), List.copyOf(wants),
                grade, fig, high, paid, List.copyOf(hint));
    }

    /** What the screen shows before the server's first answer. */
    public static final VillagerView EMPTY = new VillagerView(false, 1, 0, 0, 0, List.of(), 0, List.of(), List.of(), NONE, -1, false, 0, List.of());
}
