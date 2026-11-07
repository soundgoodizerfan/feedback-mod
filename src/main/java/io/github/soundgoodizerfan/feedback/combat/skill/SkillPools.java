// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.registry.FCombat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * A player's skill points per tree, the nodes bought (as {@code tree/node}), and a small record of
 * recent awards for diminishing returns. The attachment {@link FCombat#SKILLS}: persisted, kept on
 * death, synced to the owner only. Immutable; every change builds a new one.
 */
public record SkillPools(Map<String, Integer> points, Set<String> unlocked, List<Recent> recent) {

    /** One recent award: what it was for (tree and target), how many it has paid lately, and when last. */
    public record Recent(String key, int count, long tick) {
        static final Codec<Recent> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("key").forGetter(Recent::key),
                Codec.INT.fieldOf("count").forGetter(Recent::count),
                Codec.LONG.fieldOf("tick").forGetter(Recent::tick)).apply(i, Recent::new));
    }

    public static final SkillPools EMPTY = new SkillPools(Map.of(), Set.of(), List.of());

    public static final Codec<SkillPools> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("points").forGetter(SkillPools::points),
            Codec.STRING.listOf().<Set<String>>xmap(Set::copyOf, List::copyOf).fieldOf("unlocked").forGetter(SkillPools::unlocked),
            Recent.CODEC.listOf().fieldOf("recent").forGetter(SkillPools::recent)).apply(i, SkillPools::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillPools> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static SkillPools of(Player player) {
        return player.getData(FCombat.SKILLS);
    }

    public static void set(Player player, SkillPools pools) {
        player.setData(FCombat.SKILLS, pools);
    }

    public static String key(String tree, String node) {
        return tree + "/" + node;
    }

    public int points(String tree) {
        return points.getOrDefault(tree, 0);
    }

    public boolean has(String tree, String node) {
        return unlocked.contains(key(tree, node));
    }

    public SkillPools withPoints(String tree, int amount) {
        Map<String, Integer> m = new HashMap<>(points);
        m.put(tree, Math.max(0, amount));
        return new SkillPools(Map.copyOf(m), unlocked, recent);
    }

    public SkillPools withUnlocked(Set<String> nodes) {
        return new SkillPools(points, Set.copyOf(nodes), recent);
    }

    /** Buys {@code node} of {@code tree} for {@code cost} from {@code pool}: the points are taken, the node marked. The caller has checked it can. */
    public SkillPools bought(String pool, String tree, String node, int cost) {
        Set<String> u = new HashSet<>(unlocked);
        u.add(key(tree, node));
        return withPoints(pool, points(pool) - cost).withUnlocked(u);
    }

    /**
     * Pays an award for {@code target} (a uuid or source name) in {@code tree}, diminished by how often
     * that target has paid lately, and records it. The payout may be zero; the count still rises.
     */
    public SkillPools awarded(String tree, String target, int base, long now) {
        return awarded(tree, target, base, now, Integer.MAX_VALUE);
    }

    /** As {@link #awarded(String, String, int, long)}, but pays nothing once {@code cap} recent payouts for the target are unforgotten. */
    public SkillPools awarded(String tree, String target, int base, long now, int cap) {
        String key = tree + ":" + target;
        int count = 0;
        List<Recent> list = new ArrayList<>();
        for (Recent r : recent)
            if (r.key().equals(key))
                count = Math.max(0, r.count() - (int) ((now - r.tick()) / SkillTuning.FORGET_TICKS));
            else
                list.add(r);
        int pay = count >= cap ? 0 : Math.round(base * (float) Math.pow(SkillTuning.DECAY, count));
        list.add(0, new Recent(key, Math.min(count + 1, SkillTuning.MAX_COUNT), now));
        while (list.size() > SkillTuning.MAX_RECENT)
            list.remove(list.size() - 1);
        return new SkillPools(points, unlocked, List.copyOf(list)).withPoints(tree, points(tree) + pay);
    }
}
