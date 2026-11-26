// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.derrick;

import io.github.soundgoodizerfan.feedback.machine.column.PetrochemTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * What echo crude does to the place it is lifted (petrochemistry spec, the Deep Dark): it rings under a turning lift, and sculk, which hears
 * every vibration, grows toward the noise. Stage 1 rings (a vibration for any sensor near, sculk spreading over the ground round the well), stage 2
 * shrieks (a sensor and a shrieker come up in the sculk, and a shrieker lets the vanilla warden answer it), stage 3 is the burst
 * ({@link WellheadBlockEntity}). The strain behind the stages is the lift's speed over the flywheel mass on its shaft; it builds only while the lift
 * runs fast and goes down when it slows, so every stage reverses.
 */
public final class EchoHazard {

    public static final int QUIET = 0, RINGING = 1, SHRIEKING = 2, BURST = 3;

    private EchoHazard() {
    }

    /** The speed the lift effectively turns the crude at: its RPM held back by the inertia of everything on the shaft. */
    static float effectiveRpm(float rpm, float runInertia) {
        return Math.abs(rpm) / (1f + PetrochemTuning.ECHO_DAMP_PER_INERTIA * runInertia);
    }

    /** The strain after one tick at {@code effective} RPM: up above the ringing speed, down below it. */
    static float strain(float strain, float effective) {
        if (effective > PetrochemTuning.ECHO_RING_RPM)
            return Math.min(1f, strain + (effective - PetrochemTuning.ECHO_RING_RPM) / (PetrochemTuning.ECHO_FULL_RPM - PetrochemTuning.ECHO_RING_RPM)
                    * PetrochemTuning.ECHO_STRAIN_RISE);
        return Math.max(0f, strain - PetrochemTuning.ECHO_STRAIN_FALL);
    }

    static int stage(float strain, float effective) {
        if (strain >= 1f)
            return BURST;
        if (strain >= PetrochemTuning.ECHO_SHRIEK_STRAIN)
            return SHRIEKING;
        return effective > PetrochemTuning.ECHO_RING_RPM ? RINGING : QUIET;
    }

    /** One second of a stage: a vibration the sculk near the well hears, and sculk growing over the ground round it. */
    public static void act(ServerLevel level, BlockPos pos, int stage) {
        if (stage < RINGING)
            return;
        level.gameEvent(null, GameEvent.EXPLODE, Vec3.atCenterOf(pos));
        // Sculk growing over the ground follows the world's mob-griefing rule, as the burst follows the explosion rules.
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING))
            return;
        BlockPos at = pos.offset(level.random.nextIntBetweenInclusive(-PetrochemTuning.ECHO_SPREAD_REACH, PetrochemTuning.ECHO_SPREAD_REACH), -1,
                level.random.nextIntBetweenInclusive(-PetrochemTuning.ECHO_SPREAD_REACH, PetrochemTuning.ECHO_SPREAD_REACH));
        for (int dy = 0; dy < 3; dy++) {
            BlockPos floor = at.above(dy);
            BlockState there = level.getBlockState(floor), over = level.getBlockState(floor.above());
            if (!there.is(BlockTags.SCULK_REPLACEABLE) || !over.isAir())
                continue;
            if (stage >= SHRIEKING && level.random.nextFloat() < PetrochemTuning.ECHO_SHRIEKER_CHANCE) {
                level.setBlockAndUpdate(floor, Blocks.SCULK.defaultBlockState());
                level.setBlockAndUpdate(floor.above(), (level.random.nextBoolean() ? Blocks.SCULK_SHRIEKER.defaultBlockState()
                        .setValue(SculkShriekerBlock.CAN_SUMMON, true) : Blocks.SCULK_SENSOR.defaultBlockState()));
            } else
                level.setBlockAndUpdate(floor, Blocks.SCULK.defaultBlockState());
            return;
        }
    }
}
