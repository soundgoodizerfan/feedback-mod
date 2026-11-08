// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.xp;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.sculk.SculkNodeBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselCore;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * An organ that puts one world source of experience into its run: loose orbs near it (the absorber), the XP of ore broken
 * near it, the XP of a villager's trade near it, or what a vessel beside it banked from smelting. Holds nothing of its own.
 * The absorber and the smelter tap poll once a second; the other two are found by {@link XpTaps} when the event happens.
 */
public class XpTapBlockEntity extends SculkNodeBlockEntity {

    public XpTapBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.XP_TAP.get(), pos, state);
    }

    public XpTapBlock.Kind kind() {
        return ((XpTapBlock) getBlockState().getBlock()).kind();
    }

    @Override
    protected void joined() {
        XpTaps.add(level, kind(), worldPosition);
    }

    /** Puts what the run can hold of {@code points} in and returns how many it took. */
    public int take(int points) {
        return network() == null ? 0 : network().inject(points);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, XpTapBlockEntity tap) {
        if (level.getGameTime() % 20 != 0 || tap.network() == null || !(level instanceof ServerLevel server))
            return;
        switch (tap.kind()) {
            case ORB -> {
                double r = FTuning.ABSORBER_RADIUS;
                for (ExperienceOrb orb : server.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(r))) {
                    int value = orb.getValue();
                    int took = tap.take(value);
                    if (took <= 0)
                        continue;
                    orb.discard();
                    if (took < value)
                        ExperienceOrb.award(server, orb.position(), value - took);
                }
            }
            case SMELTER -> {
                for (Direction d : Direction.values())
                    if (level.getBlockEntity(pos.relative(d)) instanceof ThermalVesselCore vessel && vessel.bankedXp() > 0) {
                        int room = tap.take(Math.min(FTuning.SMELTER_TAP_POINTS_PER_SECOND, vessel.bankedXp()));
                        vessel.drainXp(room);
                    }
            }
            default -> {
            }
        }
    }
}
