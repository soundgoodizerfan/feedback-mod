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
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.village.CommissionProfile.Axis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Works out a villager's orders for a day from nothing but who they are, what they do, how far
 * they have risen and the day number, so nothing is stored and nothing ticks. The same inputs give
 * the same orders on either side and across a restart.
 */
public final class OrderGen {

    private OrderGen() {
    }

    public static int count(int level) {
        return VillageTuning.ORDERS_BASE + level;
    }

    /** Half-width of a band for a villager of this level: wide for a novice, narrowing with trade level. */
    public static float tolerance(int level) {
        return Math.max(VillageTuning.TOLERANCE_FLOOR, VillageTuning.TOLERANCE_NOVICE - VillageTuning.TOLERANCE_STEP * (level - 1));
    }

    public static List<Order> orders(UUID villager, ResourceLocation profession, int level, long day) {
        List<CommissionProfile.Entry> pool = CommissionTable.get().forProfession(profession).stream()
                .filter(e -> e.level() <= level && !e.axes().isEmpty()).toList();
        List<Order> out = new ArrayList<>();
        if (pool.isEmpty())
            return out;
        for (int i = 0; i < count(level); i++) {
            RandomSource rnd = RandomSource.create(mix(villager.getMostSignificantBits(), villager.getLeastSignificantBits(), day, i));
            out.add(build(villager, pool.get(rnd.nextInt(pool.size())), level, rnd));
        }
        return out;
    }

    private static Order build(UUID villager, CommissionProfile.Entry e, int level, RandomSource rnd) {
        List<Axis> axes = new ArrayList<>(e.axes());
        for (int i = axes.size() - 1; i > 0; i--)
            java.util.Collections.swap(axes, i, rnd.nextInt(i + 1));
        int stated = Math.min(axes.size(), 2 + (level >= 3 ? 1 : 0));
        float tol = tolerance(level);
        List<Order.Term> terms = new ArrayList<>();
        for (int i = 0; i < stated; i++) {
            Axis a = axes.get(i);
            if (i == 0) {
                boolean above = rnd.nextBoolean();
                float frac = above ? 0.25f + rnd.nextFloat() * 0.3f : 0.45f + rnd.nextFloat() * 0.3f;
                terms.add(new Order.Term(a.fig(), a.lo(), a.hi(), a.lo() + frac * a.range(), 0f, true, above));
            } else {
                float frac = 0.2f + rnd.nextFloat() * 0.6f;
                terms.add(new Order.Term(a.fig(), a.lo(), a.hi(), a.lo() + frac * a.range(), tol, false, false));
            }
        }
        List<Order.Term> quirks = new ArrayList<>();
        long h = mix(villager.getMostSignificantBits(), villager.getLeastSignificantBits(), e.type().hashCode(), 7);
        int all = e.axes().size();
        for (int step = 0; step < all; step++) {
            Axis a = e.axes().get((int) Math.floorMod(h + step, (long) all));
            if (terms.stream().anyMatch(t -> t.fig() == a.fig()))
                continue;
            float frac = 0.2f + 0.6f * (((h >>> 8) & 1023) / 1023f);
            quirks.add(new Order.Term(a.fig(), a.lo(), a.hi(), a.lo() + frac * a.range(), tol, false, false));
            break;
        }
        return new Order(e.type(), e.pay(), List.copyOf(terms), List.copyOf(quirks));
    }

    private static long mix(long... parts) {
        long h = 0x9E3779B97F4A7C15L;
        for (long p : parts) {
            h ^= p + 0x9E3779B97F4A7C15L + (h << 6) + (h >>> 2);
            h *= 0xBF58476D1CE4E5B9L;
            h ^= h >>> 31;
        }
        return h;
    }
}
