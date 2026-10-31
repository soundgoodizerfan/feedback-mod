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

import io.github.soundgoodizerfan.feedback.core.FTuning;

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
        mass = FTuning.CULTURE_START_MASS;
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

        upkeepOwed += FTuning.CULTURE_UPKEEP_PER_TICK * mass * dt;
        int want = (int) upkeepOwed;
        boolean fed = available >= Math.max(want, 1);
        int eaten = 0;
        if (fed) {
            eaten = want;
            upkeepOwed -= want;
            hungry = 0;
            if (mass < 1f && breathing) {
                growthOwed = Math.min(growthOwed + FTuning.CULTURE_GROWTH_POINTS_PER_TICK * dt, 4f);
                int room = (int) Math.ceil((1f - mass) * FTuning.CULTURE_POINTS_PER_MASS);
                int grown = Math.min(Math.min((int) growthOwed, available - eaten), room);
                if (grown > 0) {
                    growthOwed -= grown;
                    eaten += grown;
                    mass = Math.min(1f, mass + grown / FTuning.CULTURE_POINTS_PER_MASS);
                }
            }
        } else {
            upkeepOwed = 0;
            hungry += dt;
        }

        float loss = 0f;
        if (hungry >= FTuning.CULTURE_ATROPHY_TICKS)
            loss += FTuning.CULTURE_ATROPHY_PER_TICK * dt;
        if (airless >= FTuning.CULTURE_AIRLESS_GRACE_TICKS)
            loss += FTuning.CULTURE_SUFFOCATE_PER_TICK * dt;
        if (loss > 0f)
            mass = Math.max(0f, mass - loss);
        if (!alive()) {
            ripe = 0;
            return eaten;
        }

        if (isRipe()) {
            ripe += dt;
            if (ripe >= FTuning.CULTURE_OVERRIPE_TICKS) {
                leakOwed += Math.max(0, available - eaten) * FTuning.CULTURE_LEAK_PER_TICK * dt;
                int lost = (int) leakOwed;
                leakOwed -= lost;
                leaked += lost;
                if (!shrieked && ripe >= FTuning.CULTURE_OVERRIPE_TICKS + FTuning.CULTURE_SHRIEKER_DELAY_TICKS)
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
        return hungry == 0 && airless == 0 && ripe < FTuning.CULTURE_OVERRIPE_TICKS;
    }

    /** A harvest: the mass falls back to the harvest size and the ripe clock restarts. */
    public void harvested() {
        mass = FTuning.CULTURE_HARVEST_MASS;
        shrieked = false;
        ripe = 0;
        leakOwed = 0;
        shriekerDue = false;
    }

    public Stage stage() {
        if (!alive())
            return Stage.EMPTY;
        if (shrieked && ripe >= FTuning.CULTURE_OVERRIPE_TICKS)
            return Stage.SHRIEKING;
        if (ripe >= FTuning.CULTURE_OVERRIPE_TICKS)
            return Stage.CLOTTED;
        if (airless > 0)
            return Stage.SUFFOCATING;
        if (hungry >= FTuning.CULTURE_ATROPHY_TICKS)
            return Stage.ATROPHYING;
        if (hungry >= FTuning.CULTURE_DORMANT_TICKS)
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
