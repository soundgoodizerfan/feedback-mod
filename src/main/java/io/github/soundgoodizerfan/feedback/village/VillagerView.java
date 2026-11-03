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
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

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
 * @param fig       the figure the villager complained of, as an ordinal, or -1
 * @param high      whether that figure was too high
 * @param paid      emeralds the last delivery paid
 */
public record VillagerView(boolean wanderer, int level, int xp, int xpMax, int standing, List<OrderView> orders, int doneMask,
                           List<GoodView> goods, List<WantView> wants, int grade, int fig, boolean high, int paid) {

    public static final int NONE = -1, WRONG = -2, SOLD = -3, POOR_PURSE = -4, NOT_WANTED = -5, FULL = -6, SALE = -7;

    /** One term as the client words it. */
    public record TermView(Fig fig, int band, boolean must, boolean above) {
    }

    /** @param pay emeralds a Good delivery pays before saturation, which depends on the material it is made of */
    public record OrderView(ResourceLocation type, int pay, List<TermView> terms) {
    }

    /** @param id index in the goods table, which the server checks a purchase against; @param locked the villager wants more standing before selling it; @param left how many are still for sale today */
    public record GoodView(ItemStack stack, int price, int left, boolean locked, int id) {
    }

    /** @param food any food is bought, drawn as an apple; @param left how many more are wanted today; @param id index in the buys table */
    public record WantView(ItemStack icon, boolean food, int price, int left, int id) {
    }

    public VillagerView withResult(int grade, int fig, boolean high, int paid) {
        return new VillagerView(wanderer, level, xp, xpMax, standing, orders, doneMask, goods, wants, grade, fig, high, paid);
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
            orders.add(new OrderView(type, pay, List.copyOf(terms)));
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
        return new VillagerView(wanderer, level, xp, xpMax, standing, List.copyOf(orders), done, List.copyOf(goods), List.copyOf(wants),
                b.readVarInt() - 8, b.readVarInt() - 1, b.readBoolean(), b.readVarInt());
    }

    /** What the screen shows before the server's first answer. */
    public static final VillagerView EMPTY = new VillagerView(false, 1, 0, 0, 0, List.of(), 0, List.of(), List.of(), NONE, -1, false, 0);
}
