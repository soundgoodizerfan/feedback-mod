// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What an open dock screen shows.
 *
 * @param stock     one line per distinct stack on the aisle
 * @param queue     retrievals waiting, in order
 * @param state     what holds the dock up, if anything
 * @param racks     rack cells on the aisle
 * @param tripTicks round trip to the farthest rack at the present speed, 0 when still
 */
public record DockView(List<Line> stock, List<Line> queue, State state, int racks, int tripTicks) {

    public enum State { READY, NO_HEAD, STILL, NO_ROOM, OUTPUT_FULL }

    public record Line(ItemStack item, int count) {
        static final StreamCodec<RegistryFriendlyByteBuf, Line> CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Line::item, ByteBufCodecs.VAR_INT, Line::count, Line::new);
    }

    public static final DockView EMPTY = new DockView(List.of(), List.of(), State.READY, 0, 0);

    public static final StreamCodec<RegistryFriendlyByteBuf, DockView> STREAM_CODEC = StreamCodec.composite(
            Line.CODEC.apply(ByteBufCodecs.list()), DockView::stock,
            Line.CODEC.apply(ByteBufCodecs.list()), DockView::queue,
            ByteBufCodecs.VAR_INT.map(i -> State.values()[i], State::ordinal), DockView::state,
            ByteBufCodecs.VAR_INT, DockView::racks,
            ByteBufCodecs.VAR_INT, DockView::tripTicks,
            DockView::new);

    /** Whether {@code other} would draw the same. */
    public boolean same(DockView other) {
        return state == other.state && racks == other.racks && tripTicks == other.tripTicks
                && same(stock, other.stock) && same(queue, other.queue);
    }

    private static boolean same(List<Line> a, List<Line> b) {
        if (a.size() != b.size())
            return false;
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).count != b.get(i).count || !ItemStack.isSameItemSameComponents(a.get(i).item, b.get(i).item))
                return false;
        return true;
    }
}
