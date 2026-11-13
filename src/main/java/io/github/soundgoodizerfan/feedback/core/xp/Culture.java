// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.xp;


import net.minecraft.nbt.CompoundTag;

/**
 * A sculk culture (progression roadmap 6.8): wetware in a dish, fed XP and air. Pure and server-safe; the dish steps it
 * a second at a time and does the world side (the store, the orbs, the shrieker).
 * <p>
 * Mass runs 0 to 1. XP is its food: upkeep first, then growth while below full. No XP for a while stops growth
 * (dormant) and later wastes the mass (atrophy); no air wastes it faster after a short grace. A full culture left
 * alone clots and bleeds its store, and left longer buds a shrieker. Every loss is a slope, never a cliff, and every
 * one is caused by neglect, not by uptime.
 */
public final class Culture {

    public enum Stage {
        /** No culture seated. */
        EMPTY,
        GROWING,
        RIPE,
        /** Starved of XP: growth has stopped, the mass is kept. */
        DORMANT,
        /** Starved long enough that the mass is wasting. */
        ATROPHYING,
        /** Out of air: the mass is dying once the grace is spent. */
        SUFFOCATING,
        /** Left ripe too long: the store is bleeding away. */
        CLOTTED,
        /** Has budded a shrieker. */
        SHRIEKING
    }

    private float mass;
    private int hungry;
    private int airless;
    private int ripe;
    private float upkeepOwed;
    private float growthOwed;
    private float leakOwed;
    private boolean shrieked;
    private int leaked;
    private boolean shriekerDue;

    public float mass() {
        return mass;
    }

    public boolean alive() {
        return mass > 0f;
    }

    public boolean isRipe() {
        return mass >= 1f;
    }

    /** Starts a culture from a seed. */
    public void start() {
        mass = XpTuning.CULTURE_START_MASS;
        hungry = 0;
        airless = 0;
        ripe = 0;
        upkeepOwed = 0;
        growthOwed = 0;
        leakOwed = 0;
        shrieked = false;
        leaked = 0;
        shriekerDue = false;
    }

    /** Points the last step took out of the store as a leak; read once, then it is zero. */
    public int takeLeaked() {
        int out = leaked;
        leaked = 0;
        return out;
    }

    /** True once when the culture has been neglected long enough to bud a shrieker. */
    public boolean takeShriekerDue() {
        boolean out = shriekerDue;
        shriekerDue = false;
        return out;
    }

    public void shriekerBudded() {
        shrieked = true;
    }

    /**
     * Advances {@code dt} ticks. {@code available} is the points in the store, {@code breathing} whether it had air.
     * Returns the points eaten (upkeep and growth), which the dish takes out of the store.
     */
    public int step(int dt, int available, boolean breathing) {
        if (!alive())
            return 0;
        if (breathing)
            airless = 0;
        else
            airless += dt;

        upkeepOwed += XpTuning.CULTURE_UPKEEP_PER_TICK * mass * dt;
        int want = (int) upkeepOwed;
        boolean fed = available >= Math.max(want, 1);
        int eaten = 0;
        if (fed) {
            eaten = want;
            upkeepOwed -= want;
            hungry = 0;
            if (mass < 1f && breathing) {
                growthOwed = Math.min(growthOwed + XpTuning.CULTURE_GROWTH_POINTS_PER_TICK * dt, 4f);
                int room = (int) Math.ceil((1f - mass) * XpTuning.CULTURE_POINTS_PER_MASS);
                int grown = Math.min(Math.min((int) growthOwed, available - eaten), room);
                if (grown > 0) {
                    growthOwed -= grown;
                    eaten += grown;
                    mass = Math.min(1f, mass + grown / XpTuning.CULTURE_POINTS_PER_MASS);
                }
            }
        } else {
            upkeepOwed = 0;
            hungry += dt;
        }

        float loss = 0f;
        if (hungry >= XpTuning.CULTURE_ATROPHY_TICKS)
            loss += XpTuning.CULTURE_ATROPHY_PER_TICK * dt;
        if (airless >= XpTuning.CULTURE_AIRLESS_GRACE_TICKS)
            loss += XpTuning.CULTURE_SUFFOCATE_PER_TICK * dt;
        if (loss > 0f)
            mass = Math.max(0f, mass - loss);
        if (!alive()) {
            ripe = 0;
            return eaten;
        }

        if (isRipe()) {
            ripe += dt;
            if (ripe >= XpTuning.CULTURE_OVERRIPE_TICKS) {
                leakOwed += Math.max(0, available - eaten) * XpTuning.CULTURE_LEAK_PER_TICK * dt;
                int lost = (int) leakOwed;
                leakOwed -= lost;
                leaked += lost;
                if (!shrieked && ripe >= XpTuning.CULTURE_OVERRIPE_TICKS + XpTuning.CULTURE_SHRIEKER_DELAY_TICKS)
                    shriekerDue = true;
            }
        } else {
            ripe = 0;
            leakOwed = 0;
        }
        return eaten;
    }

    /** Whether a harvest now may bud a spare seed: a culture neglected in no way, not yet overripe. */
    public boolean canBud() {
        return hungry == 0 && airless == 0 && ripe < XpTuning.CULTURE_OVERRIPE_TICKS;
    }

    /** A harvest: the mass falls back to the harvest size and the ripe clock restarts. */
    public void harvested() {
        mass = XpTuning.CULTURE_HARVEST_MASS;
        shrieked = false;
        ripe = 0;
        leakOwed = 0;
        shriekerDue = false;
    }

    public Stage stage() {
        if (!alive())
            return Stage.EMPTY;
        if (shrieked && ripe >= XpTuning.CULTURE_OVERRIPE_TICKS)
            return Stage.SHRIEKING;
        if (ripe >= XpTuning.CULTURE_OVERRIPE_TICKS)
            return Stage.CLOTTED;
        if (airless > 0)
            return Stage.SUFFOCATING;
        if (hungry >= XpTuning.CULTURE_ATROPHY_TICKS)
            return Stage.ATROPHYING;
        if (hungry >= XpTuning.CULTURE_DORMANT_TICKS)
            return Stage.DORMANT;
        return isRipe() ? Stage.RIPE : Stage.GROWING;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Mass", mass);
        tag.putInt("Hungry", hungry);
        tag.putInt("Airless", airless);
        tag.putInt("Ripe", ripe);
        tag.putFloat("Upkeep", upkeepOwed);
        tag.putFloat("Growth", growthOwed);
        tag.putFloat("Leak", leakOwed);
        tag.putBoolean("Shrieked", shrieked);
        return tag;
    }

    public void load(CompoundTag tag) {
        mass = tag.getFloat("Mass");
        hungry = tag.getInt("Hungry");
        airless = tag.getInt("Airless");
        ripe = tag.getInt("Ripe");
        upkeepOwed = tag.getFloat("Upkeep");
        growthOwed = tag.getFloat("Growth");
        leakOwed = tag.getFloat("Leak");
        shrieked = tag.getBoolean("Shrieked");
    }
}
