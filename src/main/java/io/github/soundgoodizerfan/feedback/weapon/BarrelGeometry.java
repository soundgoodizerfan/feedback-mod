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
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A machined barrel's real shape (spec 12): what the drill, the lathe and the cutoff wheel have
 * made of it, carried on the item the way an ingot carries its {@code Fu}. The gun bakes from
 * this, not from the blueprint's targets -- the world resolves on the part.
 *
 * <p>Absent on a barrel means fresh from the drill: a pilot bore through the whole stock.
 *
 * @param boreMm  inside diameter, grown by drilling.
 * @param outerMm outside diameter, thinned by turning.
 * @param lengthM length, shortened by the cutoff wheel.
 */
public record BarrelGeometry(float boreMm, float outerMm, float lengthM) {

    /** A barrel as the drill first pierces it. */
    public static final BarrelGeometry STOCK = new BarrelGeometry(FTuning.GUN_BARREL_PILOT_MM,
            FTuning.GUN_BARREL_STOCK_MM, FTuning.GUN_BARREL_STOCK_M);

    public static final Codec<BarrelGeometry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("bore").forGetter(BarrelGeometry::boreMm),
            Codec.FLOAT.fieldOf("outer").forGetter(BarrelGeometry::outerMm),
            Codec.FLOAT.fieldOf("length").forGetter(BarrelGeometry::lengthM)
    ).apply(i, BarrelGeometry::new));

    public static final StreamCodec<ByteBuf, BarrelGeometry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BarrelGeometry::boreMm,
            ByteBufCodecs.FLOAT, BarrelGeometry::outerMm,
            ByteBufCodecs.FLOAT, BarrelGeometry::lengthM,
            BarrelGeometry::new);

    /** Wall thickness, what the chamber's rating is read from. */
    public float wallMm() {
        return (outerMm - boreMm) / 2;
    }

    /** Whether there is still a barrel here: a wall left, and a length. Past either it is swarf. */
    public boolean whole() {
        return wallMm() >= FTuning.GUN_BARREL_MIN_WALL_MM && lengthM >= FTuning.GUN_BARREL_MIN_M;
    }
}
