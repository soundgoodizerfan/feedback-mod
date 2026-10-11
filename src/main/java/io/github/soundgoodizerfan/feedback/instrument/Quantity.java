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
package io.github.soundgoodizerfan.feedback.instrument;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ByIdMap;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Something a player might want a number for.
 *
 * <h2>Why measurement is never general</h2>
 * A thermometer says nothing about how flattened an ingot is, and calipers say nothing about how
 * fast a shaft turns. Precision is not a property a player accumulates — it is bought one
 * quantity at a time, and owning one instrument must never quietly sharpen every readout in the
 * game.
 * <p>
 * {@link StringRepresentable} plus the two codecs below exist for
 * {@code control.program.SensorNode} -- a controller program has to know which quantity a wired
 * sensor reads, so a comparator can refuse to compare Su against Tu instead of silently mixing
 * them (controller spec §2.3's typed ports, one level more specific than Number/Boolean).
 */
public enum Quantity implements StringRepresentable {

    /** Mechanical work beaten into a workpiece, in Fu. */
    WORK("work"),
    /** How fast a run is turning, in RPM. */
    SPEED("speed"),
    /** What a network is carrying, in Su. */
    LOAD("load"),
    /** How hot something is, in Tu. */
    TEMPERATURE("temperature"),
    /**
     * How sound a machine still is, as a fraction of as-built.
     *
     * <h3>The one quantity here with no unit</h3>
     * Every other entry names a unit from philosophy 17. Condition is a ratio of a machine to its
     * own former self, which is dimensionless on purpose -- so a resolution for it is a fraction
     * too, and 0.05 means "to the nearest five per cent" rather than to the nearest anything.
     * Nobody should mint {@code Wu} for it.
     */
    CONDITION("condition");

    private final String key;

    Quantity(String key) {
        this.key = key;
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    public static final Codec<Quantity> CODEC = StringRepresentable.fromEnum(Quantity::values);

    public static final StreamCodec<ByteBuf, Quantity> STREAM_CODEC =
            ByteBufCodecs.idMapper(ByIdMap.continuous(Quantity::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Quantity::ordinal);
}
