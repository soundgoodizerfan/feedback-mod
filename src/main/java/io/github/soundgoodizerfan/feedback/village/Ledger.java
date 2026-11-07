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

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a villager remembers of dealing with players, kept as an attachment on the villager. Small:
 * a day number, a mask of today's orders already delivered, how many of each good were sold today,
 * and one saturation bin per (type, material kind) delivered. Replaced, never edited in place.
 *
 * @param day      the day the mask and the sales belong to; a different day clears them
 * @param doneMask bit {@code i} set when today's order {@code i} has been delivered
 * @param bought   goods sold today, by index into the villager's shop
 * @param bins     deliveries recently made, by bin key
 */
public record Ledger(long day, int doneMask, Map<String, Integer> bought, Map<String, Bin> bins) {

    /** A bin: how loaded it was on the day it was last added to. */
    public record Bin(float amount, long day) {

        public static final Codec<Bin> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("amount").forGetter(Bin::amount),
                Codec.LONG.fieldOf("day").forGetter(Bin::day)).apply(i, Bin::new));
    }

    public static final Ledger EMPTY = new Ledger(-1, 0, Map.of(), Map.of());

    public static final Codec<Ledger> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("day").forGetter(Ledger::day),
            Codec.INT.fieldOf("done").forGetter(Ledger::doneMask),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("bought").forGetter(Ledger::bought),
            Codec.unboundedMap(Codec.STRING, Bin.CODEC).fieldOf("bins").forGetter(Ledger::bins)).apply(i, Ledger::new));

    /** This ledger as of {@code today}: the day's orders and sales start over, the bins stay. */
    public Ledger on(long today) {
        return day == today ? this : new Ledger(today, 0, Map.of(), bins);
    }

    public boolean done(int order) {
        return (doneMask & (1 << order)) != 0;
    }

    public int sold(int good) {
        return bought.getOrDefault(Integer.toString(good), 0);
    }

    /** How loaded a bin is on {@code today}, after draining. */
    public float load(String key, long today) {
        Bin b = bins.get(key);
        if (b == null)
            return 0f;
        return b.amount() * Math.max(0f, 1f - (today - b.day()) / VillageTuning.SATURATION_DRAIN_DAYS);
    }

    /** Pay kept after saturation: 1 for a fresh bin, falling by a step for each recent delivery, to nothing. */
    public float payKept(String key, long today) {
        return Math.max(0f, 1f - VillageTuning.SATURATION_STEP * load(key, today));
    }

    /** Count already bought of want {@code idx} today; wants share the sales map under a {@code w} key. */
    public int bought(int want) {
        return bought.getOrDefault("w" + want, 0);
    }

    public Ledger boughtMore(int want, int n, long today) {
        Map<String, Integer> next = new HashMap<>(bought);
        next.merge("w" + want, n, Integer::sum);
        return new Ledger(today, doneMask, Map.copyOf(next), bins);
    }

    /** Order {@code order} done without a delivery here (the guild's order, delivered at another member). */
    public Ledger markDone(int order) {
        return new Ledger(day, doneMask | (1 << order), bought, bins);
    }

    /** Experience a guild-mate shared with this villager today, counted under an {@code m} key. */
    public int mentored() {
        return bought.getOrDefault("m", 0);
    }

    public Ledger mentor(int xp, long today) {
        Map<String, Integer> next = new HashMap<>(bought);
        next.merge("m", xp, Integer::sum);
        return new Ledger(today, doneMask, Map.copyOf(next), bins);
    }

    /** Errands made today to a Counter, counted in the sales map under an {@code e} key. */
    public int errands() {
        return bought.getOrDefault("e", 0);
    }

    public Ledger errand(long today) {
        Map<String, Integer> next = new HashMap<>(bought);
        next.merge("e", 1, Integer::sum);
        return new Ledger(today, doneMask, Map.copyOf(next), bins);
    }

    public Ledger delivered(int order, String key, long today) {
        Map<String, Bin> next = new HashMap<>(bins);
        next.put(key, new Bin(load(key, today) + 1f, today));
        return new Ledger(today, doneMask | (1 << order), bought, Map.copyOf(next));
    }

    public Ledger sold(int good, long today) {
        Map<String, Integer> next = new HashMap<>(bought);
        next.merge(Integer.toString(good), 1, Integer::sum);
        return new Ledger(today, doneMask, Map.copyOf(next), bins);
    }
}
