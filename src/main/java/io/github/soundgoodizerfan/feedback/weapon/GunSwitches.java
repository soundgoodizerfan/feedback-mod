// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.trait.GunFitting;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.world.item.ItemStack;

/**
 * The switches every gun can have, worked out from what is on it: the fire selector (the modes its action allows, when more than one),
 * a bipod's legs (folded or deployed) and a lit reticle's shutter. In that order, so a tap of the option key steps the selector first.
 * Their state is on the stack ({@code FIRE_MODE}, {@code BIPOD_AT}, {@code RETICLE_SHUT}); nothing ticks.
 */
public final class GunSwitches {

    public static final String SELECTOR = "selector", BIPOD = "bipod", RETICLE = "reticle";

    /** Where a bipod's legs are: down and carrying the gun, coming down, being stowed, or folded. */
    public enum Legs {
        FOLDED, DEPLOYING, DEPLOYED, FOLDING;

        /** Whether the legs take the weight: only once fully down. */
        public boolean carrying() {
            return this == DEPLOYED;
        }

        /** Whether the legs are out of the way of a quick turn: only when folded. */
        public boolean handy() {
            return this == FOLDED;
        }
    }

    private GunSwitches() {
    }

    /** The fire modes {@code m}'s action lets {@code gun} be set to, in order. */
    public static List<FireMode> modes(GunMechanism m, ItemStack gun) {
        List<FireMode> out = new ArrayList<>();
        for (FireMode mode : FireMode.values())
            if (m.allow(gun, mode) == mode)
                out.add(mode);
        return out;
    }

    /** Where the bipod's legs are at game time {@code now}. */
    public static Legs legs(ItemStack gun, long now) {
        long at = gun.getOrDefault(FWeapons.BIPOD_AT.get(), 0L);
        if (at == 0)
            return Legs.FOLDED;
        long since = now - (Math.abs(at) - 1);
        if (at > 0)
            return since >= GunTuning.BIPOD_DEPLOY_TICKS ? Legs.DEPLOYED : Legs.DEPLOYING;
        return since < GunTuning.BIPOD_FOLD_TICKS ? Legs.FOLDING : Legs.FOLDED;
    }

    /** Whether the bipod was last told to deploy (the switch's state, whatever the legs are doing). */
    public static boolean deployed(ItemStack gun) {
        return gun.getOrDefault(FWeapons.BIPOD_AT.get(), 0L) > 0;
    }

    /** Whether the reticle's shutter is open (it is, unless shut). */
    public static boolean reticleOpen(ItemStack gun) {
        return !gun.getOrDefault(FWeapons.RETICLE_SHUT.get(), false);
    }

    /** The selector, bipod and reticle switches {@code gun} has, in key order. */
    public static List<GunSwitch> standard(GunMechanism m, ItemStack gun) {
        List<GunSwitch> out = new ArrayList<>();
        if (!m.built(gun))
            return out;
        List<FireMode> modes = modes(m, gun);
        if (modes.size() > 1)
            out.add(new GunSwitch(SELECTOR, modes.stream().map(FireMode::getSerializedName).toList(), Math.max(0, modes.indexOf(GunItem.mode(gun)))));
        if (GunFitting.has(gun, "bipod"))
            out.add(GunSwitch.twoState(BIPOD, "folded", "deployed", deployed(gun)));
        if (GunFitting.has(gun, "lit_reticle"))
            out.add(GunSwitch.twoState(RETICLE, "shut", "open", reticleOpen(gun)));
        return out;
    }

    /** Sets switch {@code id} of {@code gun} to {@code state}, at game time {@code now}; an id this does not own is ignored. */
    public static void set(GunMechanism m, ItemStack gun, String id, int state, long now) {
        switch (id) {
            case SELECTOR -> {
                List<FireMode> modes = modes(m, gun);
                if (state >= 0 && state < modes.size())
                    gun.set(FWeapons.FIRE_MODE.get(), modes.get(state));
            }
            case BIPOD -> {
                if (state == 1 != deployed(gun))
                    gun.set(FWeapons.BIPOD_AT.get(), state == 1 ? now + 1 : -(now + 1));
            }
            case RETICLE -> gun.set(FWeapons.RETICLE_SHUT.get(), state == 0);
            default -> {
            }
        }
    }
}
