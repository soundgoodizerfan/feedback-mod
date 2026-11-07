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
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.registry.FVillage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

/**
 * The derived guild: three or more villagers of one profession whose claimed job sites lie within {@link VillageTuning#GUILD_RADIUS} of one
 * another. Not a block and not saved; read from the villagers' own job-site memory when asked, so a smithing street becomes a guild by being
 * built and stops being one when a workstation is taken away. A guild posts one bigger order a day to any member and shares delivery experience
 * among its members. No profession is added and no villager is told to move.
 */
public final class Guilds {

    private Guilds() {
    }

    /** @param centre the lowest job-site position among the members, stable while the guild stands; @param members villagers, nearest to the asker first */
    public record Guild(ResourceLocation profession, List<Villager> members, BlockPos centre, int tier, int maxLevel) {
    }

    @Nullable
    private static BlockPos site(Villager v) {
        Optional<GlobalPos> pos = v.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        return pos.isPresent() && pos.get().dimension() == v.level().dimension() ? pos.get().pos() : null;
    }

    /** The guild {@code v} belongs to, or null: fewer than three, no claimed job site, or a profession that commissions nothing. */
    @Nullable
    public static Guild of(Villager v) {
        BlockPos mine = site(v);
        ResourceLocation prof = VillageEconomy.profession(v);
        if (mine == null || v.isBaby() || CommissionTable.get().forProfession(prof).isEmpty())
            return null;
        int r = VillageTuning.GUILD_RADIUS;
        List<Villager> members = new ArrayList<>();
        BlockPos centre = mine;
        int max = 0;
        for (Villager m : v.level().getEntitiesOfClass(Villager.class, new AABB(mine).inflate(r * 2.0 + 8), x -> x.isAlive() && !x.isBaby())) {
            BlockPos s = site(m);
            if (s == null || !VillageEconomy.profession(m).equals(prof) || s.distManhattan(mine) > r * 2 || s.distSqr(mine) > (double) r * r)
                continue;
            members.add(m);
            max = Math.max(max, VillageEconomy.level(m));
            if (s.asLong() < centre.asLong())
                centre = s;
        }
        if (members.size() < VillageTuning.GUILD_MIN)
            return null;
        members.sort(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(v)));
        int n = members.size();
        int tier = n >= 8 && max >= 5 ? 3 : n >= 5 && max >= 3 ? 2 : 1;
        return new Guild(prof, List.copyOf(members), centre, tier, max);
    }

    /** The guild's order for {@code day}: bigger than a member's own (one level above the best member, so more parts and tighter bands). */
    @Nullable
    public static Order order(Guild g, long day) {
        List<Order> orders = OrderGen.orders(new UUID(g.centre().asLong(), 0x6711D5L), g.profession(), "plains", Math.min(5, g.maxLevel() + 1), day);
        if (orders.isEmpty())
            return null;
        RandomSource rnd = RandomSource.create(g.centre().asLong() * 31L + day * 0x9E3779B97F4A7C15L);
        Order o = orders.get(rnd.nextInt(orders.size()));
        float mult = VillageTuning.GUILD_PAY_MULT + VillageTuning.GUILD_TIER_PAY * (g.tier() - 1);
        return new Order(o.type(), Math.round(o.pay() * mult), o.terms(), o.quirks(), o.leans(), o.asks());
    }

    /** A villager's orders for the day: its own, then its guild's bigger one if it belongs to a guild. */
    public static List<Order> ordersOf(Villager v, long day) {
        List<Order> own = OrderGen.orders(v.getUUID(), VillageEconomy.profession(v), VillageEconomy.villagerType(v), VillageEconomy.level(v), day);
        Guild g = of(v);
        Order extra = g == null ? null : order(g, day);
        if (extra == null)
            return own;
        List<Order> all = new ArrayList<>(own);
        all.add(extra);
        return all;
    }

    /** Whether order {@code index} of {@code v} is the guild's. */
    public static boolean isGuildOrder(Villager v, int index) {
        return index >= OrderGen.count(VillageEconomy.level(v));
    }

    /** Marks the guild's order done for every member: once done, done for the guild that day. */
    public static void markDone(Villager v, long day) {
        Guild g = of(v);
        if (g == null)
            return;
        for (Villager m : g.members())
            m.setData(FVillage.LEDGER, m.getData(FVillage.LEDGER).on(day).markDone(OrderGen.count(VillageEconomy.level(m))));
    }

    /** Gives each other member of {@code earner}'s guild a share of {@code xp}, to a daily cap per member. */
    public static void mentor(Villager earner, int xp, long day) {
        Guild g = of(earner);
        if (g == null || xp <= 0)
            return;
        int share = Math.max(1, Math.round(xp * VillageTuning.MENTOR_SHARE));
        for (Villager m : g.members()) {
            if (m == earner)
                continue;
            Ledger l = m.getData(FVillage.LEDGER).on(day);
            int grant = Math.min(share, VillageTuning.MENTOR_CAP_PER_DAY - l.mentored());
            if (grant <= 0)
                continue;
            m.setData(FVillage.LEDGER, l.mentor(grant, day));
            VillageEconomy.addXp(m, grant);
        }
    }
}
