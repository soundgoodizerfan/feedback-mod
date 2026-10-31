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
package io.github.soundgoodizerfan.feedback.core.xp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * An XP container's state (progression roadmap L2): points are the quantity held, level is the potential they
 * stand at, {@code points / perLevel}, the way charge over capacitance is voltage. {@code perLevel} is the
 * container's capacitance in points per level and {@code maxPoints} its ceiling, so its top level is {@code
 * maxPoints / perLevel}; a small {@code perLevel} is a little at a high level, a large one a lot at a low level.
 * Level is fractional here; a display floors it. A player is not one of these: a body's points and levels are
 * locked to vanilla's curve. Pure and immutable, no ticking, no constants: every figure is a parameter.
 */
public record XpStore(int points, float perLevel, int maxPoints) {

    public static final Codec<XpStore> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("points").forGetter(XpStore::points),
            Codec.FLOAT.fieldOf("per_level").forGetter(XpStore::perLevel),
            Codec.INT.fieldOf("max_points").forGetter(XpStore::maxPoints)).apply(i, XpStore::new));
    public static final StreamCodec<ByteBuf, XpStore> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, XpStore::points, ByteBufCodecs.FLOAT, XpStore::perLevel,
            ByteBufCodecs.VAR_INT, XpStore::maxPoints, XpStore::new);

    /** An empty container of ceiling {@code maxPoints} made of stuff with constant {@code k}: top level {@code k / maxPoints}. */
    public static XpStore empty(int maxPoints, float k) {
        return new XpStore(0, maxPoints * (float) maxPoints / k, maxPoints);
    }

    /** The same container holding {@code points}, clamped to its ceiling. */
    public XpStore with(int points) {
        return new XpStore(Math.max(0, Math.min(maxPoints, points)), perLevel, maxPoints);
    }

    public XpStore {
        if (!(perLevel > 0) || maxPoints < 0 || points < 0 || points > maxPoints)
            throw new IllegalArgumentException("bad XpStore " + points + "/" + maxPoints + " at " + perLevel);
    }

    public float level() {
        return points / perLevel;
    }

    public float maxLevel() {
        return maxPoints / perLevel;
    }

    /** What joining leaves: the stores in input order, and points that fit nowhere. */
    public record Joined(List<XpStore> stores, int spill) {
    }

    /**
     * Connects containers until their levels match, conserving points: the common level is total points over
     * combined capacity. A container that would pass its ceiling holds at its ceiling and the rest settles
     * across the others; when every one is full the remainder is {@code spill}. Whole points: the rounding
     * remainder goes to the first containers with room.
     */
    public static Joined join(List<XpStore> in) {
        int n = in.size();
        int[] out = new int[n];
        boolean[] pinned = new boolean[n];
        long remaining = 0;
        for (XpStore s : in)
            remaining += s.points;
        double level = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            double cap = 0;
            for (int i = 0; i < n; i++)
                if (!pinned[i])
                    cap += in.get(i).perLevel;
            if (cap == 0)
                break;
            level = remaining / cap;
            for (int i = 0; i < n; i++) {
                XpStore s = in.get(i);
                if (!pinned[i] && s.perLevel * level > s.maxPoints) {
                    pinned[i] = true;
                    out[i] = s.maxPoints;
                    remaining -= s.maxPoints;
                    changed = true;
                }
            }
        }
        for (int i = 0; i < n; i++)
            if (!pinned[i]) {
                out[i] = (int) Math.floor(in.get(i).perLevel * level);
                remaining -= out[i];
            }
        // shortcut: leftover points go first-come; only matters by under a point per container
        for (int i = 0; i < n && remaining > 0; i++)
            if (!pinned[i] && out[i] < in.get(i).maxPoints) {
                out[i]++;
                remaining--;
            }
        List<XpStore> res = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            res.add(new XpStore(out[i], in.get(i).perLevel, in.get(i).maxPoints));
        return new Joined(res, Math.max(0, (int) remaining));
    }
}
