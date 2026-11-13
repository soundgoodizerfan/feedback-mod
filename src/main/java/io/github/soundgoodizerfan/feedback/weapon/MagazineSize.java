// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * How many rounds a magazine item holds, a band set by the recipe that made it (spec 12): the
 * magazines are crafted apart from the gun, and any gun of the bore and the feed takes any of
 * them. A belt is a magazine too, for a belt feed. Placeholder counts.
 */
public enum MagazineSize implements StringRepresentable {
    SMALL("small", GunLayout.Feed.BOX, GunTuning.GUN_MAGAZINE_SMALL),
    STANDARD("standard", GunLayout.Feed.BOX, GunTuning.GUN_MAGAZINE_STANDARD),
    EXTENDED("extended", GunLayout.Feed.BOX, GunTuning.GUN_MAGAZINE_EXTENDED),
    DRUM("drum", GunLayout.Feed.BOX, GunTuning.GUN_MAGAZINE_DRUM),
    BELT("belt", GunLayout.Feed.BELT, GunTuning.GUN_MAGAZINE_BELT);

    public static final Codec<MagazineSize> CODEC = StringRepresentable.fromEnum(MagazineSize::values);
    public static final StreamCodec<ByteBuf, MagazineSize> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], MagazineSize::ordinal);

    private final String id;
    private final GunLayout.Feed feed;
    private final int capacity;

    MagazineSize(String id, GunLayout.Feed feed, int capacity) {
        this.id = id;
        this.feed = feed;
        this.capacity = capacity;
    }

    /** The feed this goes into: a box gun takes any box, a belt gun a belt. */
    public GunLayout.Feed feed() {
        return feed;
    }

    public int capacity() {
        return capacity;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
