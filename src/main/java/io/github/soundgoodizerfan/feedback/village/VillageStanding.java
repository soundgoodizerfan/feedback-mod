// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

/**
 * A village's standing toward a player, read and never stored: the mean of positive gossip (major and minor) across the villagers in range of
 * a point. Vanilla already spreads gossip between villagers who meet at the bell, so the figure moves on its own. Bounded: at most
 * {@link VillageTuning#STANDING_SAMPLE_MAX} villagers, read only when a board opens.
 */
public final class VillageStanding {

    private VillageStanding() {
    }

    /** @param mean mean positive reputation across those sampled; @param tier 0 for none up to the number of {@link VillageTuning#STANDING_TIERS}; @param sampled villagers read */
    public record Reading(int mean, int tier, int sampled) {
    }

    /** The villagers within the village's reach of {@code centre}, nearest first, at most {@code max}. */
    public static List<Villager> villagers(ServerLevel level, BlockPos centre, int max) {
        List<Villager> found = level.getEntitiesOfClass(Villager.class, new AABB(centre).inflate(VillageTuning.VILLAGE_RADIUS),
                v -> v.isAlive() && !v.isBaby());
        found.sort(Comparator.comparingDouble(v -> v.distanceToSqr(centre.getCenter())));
        return found.size() > max ? found.subList(0, max) : found;
    }

    public static Reading read(ServerLevel level, BlockPos centre, UUID player) {
        List<Villager> vs = villagers(level, centre, VillageTuning.STANDING_SAMPLE_MAX);
        long sum = 0;
        for (Villager v : vs)
            sum += v.getGossips().getReputation(player, t -> t == GossipType.MAJOR_POSITIVE || t == GossipType.MINOR_POSITIVE);
        int mean = vs.isEmpty() ? 0 : (int) (sum / vs.size());
        return new Reading(mean, tier(mean), vs.size());
    }

    public static int tier(int standing) {
        int tier = 0;
        for (int t : VillageTuning.STANDING_TIERS)
            if (standing >= t)
                tier++;
        return tier;
    }

    /** Gives every villager in range {@code points} of major-positive gossip toward {@code player}. */
    public static void praise(ServerLevel level, BlockPos centre, UUID player, int points) {
        for (Villager v : villagers(level, centre, VillageTuning.STANDING_SAMPLE_MAX))
            v.getGossips().add(player, GossipType.MAJOR_POSITIVE, points);
    }
}
