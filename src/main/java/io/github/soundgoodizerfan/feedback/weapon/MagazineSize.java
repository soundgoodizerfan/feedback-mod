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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.Codec;
import io.github.soundgoodizerfan.feedback.core.FTuning;
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
    SMALL("small", GunLayout.Feed.BOX, FTuning.GUN_MAGAZINE_SMALL),
    STANDARD("standard", GunLayout.Feed.BOX, FTuning.GUN_MAGAZINE_STANDARD),
    EXTENDED("extended", GunLayout.Feed.BOX, FTuning.GUN_MAGAZINE_EXTENDED),
    DRUM("drum", GunLayout.Feed.BOX, FTuning.GUN_MAGAZINE_DRUM),
    BELT("belt", GunLayout.Feed.BELT, FTuning.GUN_MAGAZINE_BELT);

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
