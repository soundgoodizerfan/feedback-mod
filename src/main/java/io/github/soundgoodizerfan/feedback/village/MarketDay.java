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

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Market day: every eighth day (an octal week) a paved square with a Market Cross becomes the village's market. The wanderer arrives at the
 * cross and stays the day; at honoured standing a second trader sells the regional table beside it. Villagers near the cross want more
 * (the market pool) and make one more errand, and shop stock doubles. Nothing is stored: the day number and the loaded crosses decide it all.
 */
public final class MarketDay {

    private MarketDay() {
    }

    /** The pool of extra wants a villager draws from on market day. */
    public static final ResourceLocation MARKET_POOL = Feedback.id("market");
    /** Persistent-data keys on a market trader. */
    public static final String REGIONAL = "feedback_regional", TYPE = "feedback_type";

    public static boolean isMarketDay(long day) {
        return day % VillageTuning.MARKET_PERIOD == 0;
    }

    /** Days until the next market day: 0 when today is one. */
    public static int daysUntil(long day) {
        return (int) Math.floorMod(-day, (long) VillageTuning.MARKET_PERIOD);
    }

    /** The nearest active cross within market reach of {@code from}, or null. */
    @Nullable
    public static MarketCrossBlockEntity cross(Level level, BlockPos from) {
        MarketCrossBlockEntity best = null;
        double bd = (double) VillageTuning.MARKET_REACH * VillageTuning.MARKET_REACH;
        for (MarketCrossBlockEntity c : MarketCrossBlockEntity.INDEX.in(level)) {
            double d = c.getBlockPos().distSqr(from);
            if (!c.isRemoved() && d <= bd && c.active()) {
                best = c;
                bd = d;
            }
        }
        return best;
    }

    /** Whether it is market day and {@code m} stands within reach of an active cross. */
    public static boolean feels(AbstractVillager m) {
        Level l = m.level();
        if (l.isClientSide || !isMarketDay(l.getGameTime() / VillageTuning.DAY_TICKS))
            return false;
        return cross(l, m.blockPosition()) != null;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getDayTime() % VillageTuning.DAY_TICKS != 1000L)
            return;
        long day = level.getDayTime() / VillageTuning.DAY_TICKS;
        if (!isMarketDay(day))
            return;
        for (MarketCrossBlockEntity cross : new ArrayList<>(MarketCrossBlockEntity.INDEX.in(level)))
            if (!cross.isRemoved() && cross.active() && level.isLoaded(cross.getBlockPos()))
                open(level, cross.getBlockPos());
    }

    /** Brings the market trader to the cross, and a second, regional one at honoured standing. */
    private static void open(ServerLevel level, BlockPos cross) {
        if (!level.getEntitiesOfClass(WanderingTrader.class, new AABB(cross).inflate(24)).isEmpty())
            return;
        BlockPos stand = standing(level, cross);
        spawn(level, stand, cross, false);
        ServerPlayer near = level.getNearestPlayer(cross.getX(), cross.getY(), cross.getZ(), VillageTuning.MARKET_REACH, false) instanceof ServerPlayer p ? p : null;
        if (near != null && VillageStanding.read(level, cross, near.getUUID()).tier() >= VillageTuning.STANDING_TIERS.length)
            spawn(level, stand.relative(net.minecraft.core.Direction.EAST), cross, true);
    }

    private static BlockPos standing(ServerLevel level, BlockPos cross) {
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos p = cross.relative(d);
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty())
                return p;
        }
        return cross.above();
    }

    private static void spawn(ServerLevel level, BlockPos at, BlockPos cross, boolean regional) {
        WanderingTrader t = EntityType.WANDERING_TRADER.spawn(level, at, MobSpawnType.EVENT);
        if (t == null)
            return;
        t.setDespawnDelay((int) (VillageTuning.DAY_TICKS - 1000L));
        t.setWanderTarget(cross);
        t.restrictTo(cross, 4);
        if (regional) {
            t.getPersistentData().putBoolean(REGIONAL, true);
            t.getPersistentData().putString(TYPE, BuiltInRegistries.VILLAGER_TYPE.getKey(VillagerType.byBiome(level.getBiome(cross))).getPath());
        }
    }

    /** The regional wanderer's stand-in profession in the goods table. */
    public static final ResourceLocation REGIONAL_PROFESSION = Feedback.id("wanderer_regional");

    /** The extra wants for a villager near an active cross on market day: indices into the buys table, drawn from the market pool. */
    static List<Integer> extras(long seed) {
        List<Want> all = BuysTable.get().entries();
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < all.size(); i++)
            if (all.get(i).profession().equals(MARKET_POOL))
                pool.add(i);
        java.util.Collections.shuffle(pool, new java.util.Random(seed));
        return new ArrayList<>(pool.subList(0, Math.min(pool.size(), VillageTuning.MARKET_EXTRA_WANTS)));
    }
}
