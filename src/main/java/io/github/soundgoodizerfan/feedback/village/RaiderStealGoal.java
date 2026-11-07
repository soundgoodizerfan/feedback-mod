// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FVillage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;

/**
 * A raider in a live raid heads for a Counter whose shelf holds goods worth the trip, takes the most valuable stack, and carries it. What it
 * carries drops where it dies, so killing raiders recovers the goods; one that gets away keeps them. Added once to each raider as it joins the
 * level; it looks for bait only every {@link VillageTuning#RAID_LOOK_TICKS}, and vanilla's own village goals are untouched, so a raid with no
 * bait behaves as a raid does.
 */
public final class RaiderStealGoal extends Goal {

    private final Raider raider;
    private CounterBlockEntity target;
    private int cooldown;

    public RaiderStealGoal(Raider raider) {
        this.raider = raider;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (raider.getCurrentRaid() == null || !raider.isAlive() || !(raider.level() instanceof ServerLevel level))
            return false;
        if (--cooldown > 0)
            return false;
        cooldown = VillageTuning.RAID_LOOK_TICKS;
        double best = (double) VillageTuning.RAID_BAIT_RADIUS * VillageTuning.RAID_BAIT_RADIUS;
        target = null;
        for (CounterBlockEntity c : CounterIndex.in(level)) {
            double d = c.getBlockPos().distSqr(raider.blockPosition());
            if (!c.isRemoved() && d < best && c.shelfValue() >= VillageTuning.RAID_BAIT_VALUE) {
                best = d;
                target = c;
            }
        }
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && !target.isRemoved() && raider.isAlive() && raider.getCurrentRaid() != null && target.shelfValue() > 0;
    }

    @Override
    public void start() {
        goTo();
    }

    @Override
    public void stop() {
        target = null;
        raider.getNavigation().stop();
    }

    private void goTo() {
        var p = target.front();
        raider.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.1);
    }

    @Override
    public void tick() {
        if (target == null)
            return;
        if (raider.distanceToSqr(target.getBlockPos().getCenter()) <= 6.0) {
            ItemStack stolen = target.plunder();
            if (!stolen.isEmpty()) {
                List<ItemStack> loot = new ArrayList<>(raider.getData(FVillage.LOOT));
                loot.add(stolen);
                raider.setData(FVillage.LOOT, List.copyOf(loot));
                raider.playSound(SoundEvents.PILLAGER_CELEBRATE, 1f, 1f);
            }
            target = null;
            cooldown = VillageTuning.RAID_LOOK_TICKS * 3;
        } else if (raider.getNavigation().isDone()) {
            goTo();
        }
    }
}
