// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.radio;

import io.github.soundgoodizerfan.feedback.item.RadioSetItem;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** What a radio set stack says: its band and its antenna, and what those make of its ear. Pure arithmetic, server and client safe. */
public final class RadioSets {

    private RadioSets() {
    }

    public static int band(ItemStack set) {
        return Math.max(1, Math.min(RadioTuning.BANDS, set.getOrDefault(FWeapons.RADIO_BAND.get(), 1)));
    }

    public static int antennaMm(ItemStack set) {
        return set.getOrDefault(FWeapons.RADIO_ANTENNA_MM.get(), 0);
    }

    /** The antenna length a band wants, mm. */
    public static float quarterWaveMm(int band) {
        return RadioTuning.QUARTER_WAVE_MM_AT_BAND_ONE / (float) Math.max(1, band);
    }

    /** How well the set's antenna hears its band: 1 when its length is the band's quarter wave, a bell around it, never under the floor. */
    public static float gain(ItemStack set) {
        float want = quarterWaveMm(band(set));
        float off = (antennaMm(set) - want) / (want * RadioTuning.RESONANCE_WIDTH);
        return Math.max(RadioTuning.GAIN_FLOOR, (float) Math.exp(-off * off));
    }

    /** The radio set a person holds, in either hand, or empty. */
    public static ItemStack held(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof RadioSetItem)
                return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * How clean the link from one set to another is at {@code blocks} apart: reach is the base reach times the geometric mean of the two ears' gains (a
     * badly tuned end hurts both ways); 1 is no distance, 0 the edge of reach, negative beyond. Sets on different bands do not hear each other.
     */
    public static float quality(ItemStack from, ItemStack to, double blocks) {
        if (band(from) != band(to))
            return -1f;
        float reach = RadioTuning.BASE_REACH_BLOCKS * (float) Math.sqrt(gain(from) * gain(to));
        return reach <= 0f ? -1f : (float) (1.0 - blocks / reach);
    }

    /** The share of packets a link of this quality loses: none while it is clean, rising to {@link RadioTuning#DROP_AT_EDGE} at the edge. */
    public static float dropShare(float quality) {
        if (quality >= RadioTuning.GARBLE_BELOW)
            return 0f;
        return RadioTuning.DROP_AT_EDGE * Math.min(1f, (RadioTuning.GARBLE_BELOW - Math.max(0f, quality)) / RadioTuning.GARBLE_BELOW);
    }
}
