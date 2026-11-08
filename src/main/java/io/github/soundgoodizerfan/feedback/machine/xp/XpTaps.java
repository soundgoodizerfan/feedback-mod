// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.xp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Per-level positions of the Ore and Trade taps, and the two events that feed them. Runtime only, like
 * {@code SculkSites}: a tap adds itself when it loads; a stale position is dropped when found empty.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class XpTaps {

    private static final Map<LevelAccessor, Map<XpTapBlock.Kind, Set<BlockPos>>> PER_LEVEL = new HashMap<>();

    private XpTaps() {
    }

    static void add(Level level, XpTapBlock.Kind kind, BlockPos pos) {
        PER_LEVEL.computeIfAbsent(level, l -> new HashMap<>()).computeIfAbsent(kind, k -> new HashSet<>()).add(pos.immutable());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    /** Puts up to {@code points} into the taps of {@code kind} within {@code radius} of {@code at}; returns what the runs took. */
    private static int feed(Level level, XpTapBlock.Kind kind, net.minecraft.world.phys.Vec3 at, double radius, int points) {
        Map<XpTapBlock.Kind, Set<BlockPos>> sites = PER_LEVEL.get(level);
        Set<BlockPos> taps = sites == null ? null : sites.get(kind);
        if (taps == null || taps.isEmpty() || points <= 0)
            return 0;
        int left = points;
        for (Iterator<BlockPos> it = taps.iterator(); it.hasNext() && left > 0;) {
            BlockPos p = it.next();
            if (!level.isLoaded(p))
                continue;
            if (!(level.getBlockEntity(p) instanceof XpTapBlockEntity tap) || tap.isRemoved() || tap.kind() != kind) {
                it.remove();
                continue;
            }
            if (p.distToCenterSqr(at) <= radius * radius)
                left -= tap.take(left);
        }
        return points - left;
    }

    /** Ore (or anything that pays XP) broken near an Ore Tap: the run takes what it can hold, the rest drops as usual. */
    @SubscribeEvent
    public static void onBreak(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        if (event.getDroppedExperience() <= 0)
            return;
        int took = feed(level, XpTapBlock.Kind.ORE, event.getPos().getCenter(), FTuning.ORE_TAP_RADIUS, event.getDroppedExperience());
        if (took > 0)
            event.setDroppedExperience(event.getDroppedExperience() - took);
    }

    /**
     * A trade near a Trade Tap. The orb a trade pays is spawned inside the trade call, before or after this event
     * depending on the path, so the catch runs after the tick's work: any fresh orb at the villager is taken.
     */
    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        AbstractVillager villager = event.getAbstractVillager();
        if (!(villager.level() instanceof ServerLevel level))
            return;
        level.getServer().execute(() -> {
            List<ExperienceOrb> fresh = level.getEntitiesOfClass(ExperienceOrb.class, new AABB(villager.blockPosition()).inflate(2.5),
                    orb -> orb.tickCount <= 2);
            for (ExperienceOrb orb : fresh) {
                int took = feed(level, XpTapBlock.Kind.TRADE, villager.position(), FTuning.TRADE_TAP_RADIUS, orb.getValue());
                if (took <= 0)
                    continue;
                orb.discard();
                if (took < orb.getValue())
                    ExperienceOrb.award(level, orb.position(), orb.getValue() - took);
            }
        });
    }
}
