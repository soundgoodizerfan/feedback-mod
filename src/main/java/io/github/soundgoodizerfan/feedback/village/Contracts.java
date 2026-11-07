// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * A village's big orders, derived from the board's position, the day and the standing tier, so two villages post different ones. A contract is
 * an ordinary {@link Order} (drawn by {@link OrderGen} for a stand-in villager of a profession) that asks {@code count} pieces, each at least a class.
 * Nothing is stored here; the board keeps only progress.
 */
public final class Contracts {

    private Contracts() {
    }

    /** @param id the day posted times eight plus the index, the key progress is kept under */
    public record Contract(long id, long day, ResourceLocation profession, Order order, int count, CommissionJudge.Grade min) {

        /** Days left including today, 1 on the last day. */
        public long daysLeft(long today) {
            return day + VillageTuning.CONTRACT_DAYS - today;
        }
    }

    /** Contracts posted on {@code day} (1 + tier, at most 4). */
    public static List<Contract> posted(BlockPos board, long day, int tier) {
        List<Contract> out = new ArrayList<>();
        List<ResourceLocation> professions = CommissionTable.get().professions();
        if (professions.isEmpty())
            return out;
        int n = Math.min(4, 1 + tier);
        for (int i = 0; i < n; i++) {
            long id = day * 8 + i;
            RandomSource rnd = RandomSource.create(board.asLong() * 31L + id * 0x9E3779B97F4A7C15L);
            ResourceLocation prof = professions.get(rnd.nextInt(professions.size()));
            UUID stand = new UUID(board.asLong(), id);
            List<Order> orders = OrderGen.orders(stand, prof, "plains", Math.min(5, 2 + tier), day);
            if (orders.isEmpty())
                continue;
            int count = VillageTuning.CONTRACT_MIN + rnd.nextInt(VillageTuning.CONTRACT_MAX - VillageTuning.CONTRACT_MIN + 1);
            out.add(new Contract(id, day, prof, orders.get(rnd.nextInt(orders.size())), count, CommissionJudge.Grade.GOOD));
        }
        return out;
    }

    /** Every contract still open on {@code today}: those posted within the last {@link VillageTuning#CONTRACT_DAYS} days, oldest first. */
    public static List<Contract> open(BlockPos board, long today, int tier) {
        List<Contract> out = new ArrayList<>();
        for (long d = today - VillageTuning.CONTRACT_DAYS + 1; d <= today; d++)
            if (d >= 0)
                out.addAll(posted(board, d, tier));
        return out;
    }
}
