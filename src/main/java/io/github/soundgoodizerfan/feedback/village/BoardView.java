// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything the ledger board screen shows, worded on the server with translation keys the client resolves. Built when the board opens and
 * after each delivery.
 *
 * @param mean    the village's mean positive standing toward the viewer
 * @param tier    that standing as a tier (0 none up to honoured)
 * @param sampled villagers read
 * @param orders  every open order of the villagers in range
 * @param contracts the village's contracts and how far along each is
 * @param notices lines for the Notices tab
 * @param grade   the last delivery's grade ordinal, or -1 none, -2 refused
 * @param paid    emeralds the last delivery paid
 */
public record BoardView(int mean, int tier, int sampled, List<OrderLine> orders, List<ContractLine> contracts, List<Component> notices,
                        int grade, int paid) {

    public static final BoardView EMPTY = new BoardView(0, 0, 0, List.of(), List.of(), List.of(), -1, 0);

    /** @param profession the villager's profession, worded; @param text what is asked */
    public record OrderLine(Component type, Component profession, Component text, int pay, boolean done) {
    }

    /** @param what the type and the asks; @param min the class every piece must reach, worded; @param pay emeralds a Good piece pays */
    public record ContractLine(Component what, Component min, int delivered, int count, int daysLeft, int pay) {
    }

    public BoardView withResult(int grade, int paid) {
        return new BoardView(mean, tier, sampled, orders, contracts, notices, grade, paid);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Component> C = ComponentSerialization.STREAM_CODEC;

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardView> STREAM_CODEC = StreamCodec.of(BoardView::write, BoardView::read);

    private static void write(RegistryFriendlyByteBuf b, BoardView v) {
        b.writeVarInt(v.mean);
        b.writeVarInt(v.tier);
        b.writeVarInt(v.sampled);
        b.writeVarInt(v.orders.size());
        for (OrderLine o : v.orders) {
            C.encode(b, o.type);
            C.encode(b, o.profession);
            C.encode(b, o.text);
            b.writeVarInt(o.pay);
            b.writeBoolean(o.done);
        }
        b.writeVarInt(v.contracts.size());
        for (ContractLine c : v.contracts) {
            C.encode(b, c.what);
            C.encode(b, c.min);
            b.writeVarInt(c.delivered);
            b.writeVarInt(c.count);
            b.writeVarInt(c.daysLeft);
            b.writeVarInt(c.pay);
        }
        b.writeVarInt(v.notices.size());
        for (Component n : v.notices)
            C.encode(b, n);
        b.writeVarInt(v.grade + 2);
        b.writeVarInt(v.paid);
    }

    private static BoardView read(RegistryFriendlyByteBuf b) {
        int mean = b.readVarInt(), tier = b.readVarInt(), sampled = b.readVarInt();
        List<OrderLine> orders = new ArrayList<>();
        for (int i = b.readVarInt(); i > 0; i--)
            orders.add(new OrderLine(C.decode(b), C.decode(b), C.decode(b), b.readVarInt(), b.readBoolean()));
        List<ContractLine> contracts = new ArrayList<>();
        for (int i = b.readVarInt(); i > 0; i--)
            contracts.add(new ContractLine(C.decode(b), C.decode(b), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
        List<Component> notices = new ArrayList<>();
        for (int i = b.readVarInt(); i > 0; i--)
            notices.add(C.decode(b));
        return new BoardView(mean, tier, sampled, List.copyOf(orders), List.copyOf(contracts), List.copyOf(notices), b.readVarInt() - 2, b.readVarInt());
    }
}
