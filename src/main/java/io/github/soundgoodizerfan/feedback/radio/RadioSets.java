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

    /** The conductivity of the set's antenna conductor, copper 1. */
    public static float antennaConductivity(ItemStack set) {
        return set.getOrDefault(FWeapons.RADIO_ANTENNA_CONDUCTIVITY.get(), RadioTuning.ANTENNA_REF_CONDUCTIVITY);
    }

    /** Fits the antenna a made {@code antenna} item is (its baked length and loading, and its conductor) to the set, which it uses up. False if {@code antenna} is none. */
    public static boolean attach(ItemStack set, ItemStack antenna) {
        var figs = antenna.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BLOCK_FIGURES.get());
        if (figs == null || !figs.contains("Length"))
            return false;
        set.set(FWeapons.RADIO_ANTENNA_MM.get(), Math.round(figs.getFloat("Length") + figs.getFloat("Loading")));
        set.set(FWeapons.RADIO_ANTENNA_CONDUCTIVITY.get(), figs.getFloat("Conductivity"));
        return true;
    }

    /** The antenna length a band wants, mm. */
    public static float quarterWaveMm(int band) {
        return RadioTuning.QUARTER_WAVE_MM_AT_BAND_ONE / (float) Math.max(1, band);
    }

    /**
     * How well the set's antenna hears its band: the bell of its length around the band's quarter wave, scaled by how well its conductor conducts
     * (against copper), between {@link RadioTuning#GAIN_FLOOR} (far off, or a conductor that does not) and 1 (tuned, copper or better).
     */
    public static float gain(ItemStack set) {
        float want = quarterWaveMm(band(set));
        float off = (antennaMm(set) - want) / (want * RadioTuning.RESONANCE_WIDTH);
        float conduct = Math.min(1f, antennaConductivity(set) / RadioTuning.ANTENNA_REF_CONDUCTIVITY);
        return RadioTuning.GAIN_FLOOR + (1f - RadioTuning.GAIN_FLOOR) * (float) Math.exp(-off * off) * conduct;
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
