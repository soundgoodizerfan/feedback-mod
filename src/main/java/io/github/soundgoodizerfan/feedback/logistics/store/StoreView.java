// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What the server tells an open warehouse screen (a dock or a catalogue desk).
 *
 * @param full    whether {@code stock} is the whole list; else it holds only the lines that changed, and a line with no
 *                shares is gone
 * @param stock   one line per distinct stack, split by the dock holding it
 * @param queue   retrievals waiting
 * @param sources the docks read, the places shares and queue lines point at
 * @param state   what holds the screen's block up, if anything
 */
public record StoreView(boolean full, List<Line> stock, List<Queued> queue, List<Source> sources, State state) {

    public enum State { READY, NO_HEAD, STILL, NO_ROOM, OUTPUT_FULL, AWAY, NO_CIRCUIT, NO_POWER, NO_DOCKS }

    private static final StreamCodec<io.netty.buffer.ByteBuf, State> STATE = ByteBufCodecs.VAR_INT.map(i -> State.values()[i], State::ordinal);

    /** How many of a stack one dock holds, and the bands its heads recorded of them ({@link Marks}). */
    public record Share(int source, int count, int marks) {
        static final StreamCodec<RegistryFriendlyByteBuf, Share> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Share::source, ByteBufCodecs.VAR_INT, Share::count, ByteBufCodecs.VAR_INT, Share::marks,
                Share::new);
    }

    /** One stack: the mass of one in Mu, and who holds how many. */
    public record Line(ItemStack item, float unitMass, List<Share> shares) {
        static final StreamCodec<RegistryFriendlyByteBuf, Line> CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Line::item, ByteBufCodecs.FLOAT, Line::unitMass,
                Share.CODEC.apply(ByteBufCodecs.list()), Line::shares, Line::new);

        public int count() {
            int n = 0;
            for (Share s : shares)
                n += s.count();
            return n;
        }
    }

    /**
     * One retrieval waiting at a dock.
     *
     * @param order the desk order it is a share of, 0 for one asked at the dock
     * @param key   the request's own id, what a cancel names it by
     */
    public record Queued(ItemStack item, int count, int source, int order, int key) {
        static final StreamCodec<RegistryFriendlyByteBuf, Queued> CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Queued::item, ByteBufCodecs.VAR_INT, Queued::count, ByteBufCodecs.VAR_INT, Queued::source,
                ByteBufCodecs.VAR_INT, Queued::order, ByteBufCodecs.VAR_INT, Queued::key, Queued::new);

        boolean same(Queued o) {
            return count == o.count && source == o.source && order == o.order && key == o.key
                    && ItemStack.isSameItemSameComponents(item, o.item);
        }
    }

    /** A dock: its name, what holds it up, its rack cells and the round trip to its farthest one (0 when still). */
    public record Source(Component name, State state, int racks, int tripTicks) {
        static final StreamCodec<RegistryFriendlyByteBuf, Source> CODEC = StreamCodec.composite(
                ComponentSerialization.STREAM_CODEC, Source::name, STATE, Source::state, ByteBufCodecs.VAR_INT, Source::racks,
                ByteBufCodecs.VAR_INT, Source::tripTicks, Source::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, StoreView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StoreView::full,
            Line.CODEC.apply(ByteBufCodecs.list()), StoreView::stock,
            Queued.CODEC.apply(ByteBufCodecs.list()), StoreView::queue,
            Source.CODEC.apply(ByteBufCodecs.list()), StoreView::sources,
            STATE, StoreView::state,
            StoreView::new);
}
