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
package io.github.soundgoodizerfan.feedback.gametest;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.machine.crank.HandCrankBlock;
import io.github.soundgoodizerfan.feedback.machine.crank.HandCrankBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.pulley.PulleyBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The rotation network as built ({@code core/rotation/}), pinned before
 * {@code feedback_vehicles_spec.md} section 5's drivetrain plan changes it: spin-up, overstress,
 * coasting, belts and gearing. A Hand Crank turned every tick is the source -- 32 RPM, 12 Su --
 * because it needs no water, steam or design. Speeds are read off the network, whose arithmetic
 * is deterministic, so the per-tick rates are checked exactly; only when things happen is loose.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class RotationGameTests {

    private static final float CRANK_RPM = FTuning.HAND_CRANK_RPM.value();

    /** A crank driving along X at {@code at}, then {@code shafts} shafts east of it. */
    static HandCrankBlockEntity crankRun(GameTestHelper helper, BlockPos at, int shafts) {
        helper.setBlock(at, FBlocks.HAND_CRANK.get().defaultBlockState().setValue(HandCrankBlock.FACING, Direction.EAST));
        for (int i = 1; i <= shafts; i++)
            helper.setBlock(at.east(i), alongX(FBlocks.SHAFT.get()));
        return (HandCrankBlockEntity) helper.getBlockEntity(at);
    }

    static net.minecraft.world.level.block.state.BlockState alongX(Block block) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    }

    /**
     * Clear the rig, then pass. A rotation run left standing at shutdown can keep the server
     * from stopping (about half of runs without this, none with it). Probably: unloading one of
     * its chunks removes a node, the network recalculates, and {@code RotationNetwork.dropStale}
     * asks the level for members in chunks already unloaded, which loads them again. These rigs
     * are wide enough to straddle a chunk border; the two-block rigs elsewhere rarely are.
     */
    static void done(GameTestHelper helper) {
        for (BlockPos p : BlockPos.betweenClosed(0, 1, 0, 3, 3, 2))
            helper.setBlock(p, net.minecraft.world.level.block.Blocks.AIR);
        helper.succeed();
    }

    static RotationNode node(GameTestHelper helper, BlockPos at) {
        return (RotationNode) helper.getBlockEntity(at);
    }

    static float rpm(RotationNode node) {
        return node.getNetwork() == null ? 0 : node.getNetwork().getCurrentRpm().value();
    }

    /**
     * A source brings its run to its own speed, gaining exactly surplus Su / inertia RPM a tick
     * on the way (constant drive up to the ceiling -- {@code RotationNetwork.tickSpeed}).
     */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void sourceSpinsARunUpToItsSpeed(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        HandCrankBlockEntity crank = crankRun(helper, at, 2);
        RotationNode shaft = node(helper, at.east(2));
        float[] last = {0};
        int[] checked = {0};
        helper.onEachTick(() -> {
            crank.turn();
            float now = rpm(shaft);
            RotationNetwork net = shaft.getNetwork();
            if (net != null && last[0] > 3 && now < CRANK_RPM - 3) {
                float expected = (net.getCapacitySu().value() - net.getStaticLoadSu().value()) / net.getInertia().suTicksPerRpm();
                helper.assertTrue(Math.abs(now - last[0] - expected) < 1e-3,
                        "spin-up should gain " + expected + " RPM a tick, gained " + (now - last[0]));
                checked[0]++;
            }
            last[0] = now;
        });
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(checked[0] > 5, "the spin-up rate was checked on " + checked[0] + " ticks");
            helper.assertTrue(Math.abs(shaft.getRpm().value() - CRANK_RPM) < 1e-3,
                    "the run settles at the crank's " + CRANK_RPM + " RPM, got " + shaft.getRpm().value());
            helper.assertFalse(shaft.isOverstressed(), "12 Su against friction alone is not overstress");
            done(helper);
        });
    }

    /** A machine drawing more than the source supplies overstresses the run and it does not turn. */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void loadPastCapacityOverstresses(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        HandCrankBlockEntity crank = crankRun(helper, at, 1);
        helper.setBlock(at.east(2), FBlocks.DRILL_PRESS.get()); // 35 Su against the crank's 12
        RotationNode shaft = node(helper, at.east());
        helper.onEachTick(() -> {
            crank.turn();
            helper.assertTrue(rpm(shaft) == 0, "an overstressed run never turns, got " + rpm(shaft));
        });
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(shaft.isOverstressed(), "35 Su on a 12 Su crank is overstress");
            done(helper);
        });
    }

    /**
     * Overstress does not snap a turning run to zero: it stops being driven and winds down under
     * its own load (the class javadoc's promise).
     */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void overstressWindsATurningRunDown(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        HandCrankBlockEntity crank = crankRun(helper, at, 1);
        RotationNode shaft = node(helper, at.east());
        helper.onEachTick(crank::turn);
        helper.runAtTickTime(50, () -> {
            helper.assertTrue(Math.abs(rpm(shaft) - CRANK_RPM) < 1e-3, "up to speed first, got " + rpm(shaft));
            helper.setBlock(at.east(2), FBlocks.DRILL_PRESS.get());
        });
        helper.runAtTickTime(53, () -> {
            helper.assertTrue(shaft.isOverstressed(), "the drill press overstresses the run");
            float now = rpm(shaft);
            helper.assertTrue(now > 0 && now < CRANK_RPM, "it winds down rather than stopping dead, got " + now);
        });
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(rpm(shaft) == 0, "and it does come to rest, got " + rpm(shaft));
            done(helper);
        });
    }

    /**
     * Cutting the source makes a run coast rather than stop, slowing at
     * max(load, MINIMUM_BRAKING_SU) / inertia a tick; a loaded run stops sooner than a bare one.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void cutRunsCoastAndLoadStopsThemSooner(GameTestHelper helper) {
        BlockPos bareAt = new BlockPos(0, 2, 0), loadedAt = new BlockPos(0, 2, 2);
        HandCrankBlockEntity bare = crankRun(helper, bareAt, 1);
        HandCrankBlockEntity loaded = crankRun(helper, loadedAt, 1);
        helper.setBlock(loadedAt.east(2), FBlocks.CONTROLLER.get()); // 2 Su: loaded, not overstressed
        RotationNode bareShaft = node(helper, bareAt.east()), loadedShaft = node(helper, loadedAt.east());
        int release = 50;
        long[] stopped = {-1, -1};
        float[] last = {0};
        int[] checked = {0};
        helper.onEachTick(() -> {
            long tick = helper.getTick();
            if (tick < release) {
                bare.turn();
                loaded.turn();
                return;
            }
            float now = rpm(bareShaft);
            RotationNetwork net = bareShaft.getNetwork();
            if (net.getTargetRpm().value() == 0 && now > 1) {
                float load = net.getStaticLoadSu().value() + net.getDragSuPerRpm().suPerRpm() * last[0];
                float expected = Math.max(load, FTuning.MINIMUM_BRAKING_SU.value()) / net.getInertia().suTicksPerRpm();
                helper.assertTrue(Math.abs(last[0] - now - expected) < 1e-3,
                        "coasting should lose " + expected + " RPM a tick, lost " + (last[0] - now));
                checked[0]++;
            }
            last[0] = now;
            if (stopped[0] < 0 && now == 0)
                stopped[0] = tick;
            if (stopped[1] < 0 && rpm(loadedShaft) == 0)
                stopped[1] = tick;
            if (stopped[0] >= 0 && stopped[1] >= 0) {
                helper.assertTrue(checked[0] > 5, "the coast-down rate was checked on " + checked[0] + " ticks");
                helper.assertTrue(stopped[1] - release > 20, "a cut run coasts, it does not stop dead: stopped "
                        + (stopped[1] - release) + " ticks after the crank was let go");
                helper.assertTrue(stopped[1] < stopped[0], "the loaded run stops first: loaded at "
                        + stopped[1] + ", bare at " + stopped[0]);
                done(helper);
            }
        });
        helper.runAtTickTime(release - 1, () -> {
            helper.assertTrue(Math.abs(rpm(bareShaft) - CRANK_RPM) < 1e-3 && Math.abs(rpm(loadedShaft) - CRANK_RPM) < 1e-3,
                    "both runs up to speed before the cut");
            last[0] = rpm(bareShaft);
        });
    }

    /** A belt drives a separate run with no source of its own toward its partner's speed. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void beltDrivesASeparateRun(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        HandCrankBlockEntity crank = crankRun(helper, at, 0);
        helper.setBlock(at.east(), alongX(FBlocks.SMALL_PULLEY.get()));
        BlockPos far = at.east().south(2);
        helper.setBlock(far, alongX(FBlocks.SMALL_PULLEY.get()));
        helper.setBlock(far.east(), alongX(FBlocks.SHAFT.get()));
        helper.assertTrue(((PulleyBlockEntity) helper.getBlockEntity(at.east()))
                .linkBelt((PulleyBlockEntity) helper.getBlockEntity(far)), "the belt links");
        RotationNode driven = node(helper, far.east());
        helper.onEachTick(crank::turn);
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(driven.getNetwork() != crank.getNetwork(), "a belt does not merge the runs");
            helper.assertTrue(driven.getNetwork().getCapacitySu().value() == 0, "the driven run has no source of its own");
            float speed = Math.abs(driven.getRpm().value());
            helper.assertTrue(Math.abs(speed - CRANK_RPM) < 1, "equal pulleys turn it at the crank's speed, got " + speed);
            done(helper);
        });
    }

    /** Meshed cogs refer speed by their teeth: a small cog on a large one halves it and reverses it. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void cogsReferSpeedThroughTheirRatio(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        HandCrankBlockEntity crank = crankRun(helper, at, 0);
        helper.setBlock(at.east(), alongX(FBlocks.SMALL_COG.get()));
        helper.setBlock(at.east().above(), alongX(FBlocks.LARGE_COG.get()));
        helper.setBlock(at.east(2).above(), alongX(FBlocks.SHAFT.get()));
        RotationNode output = node(helper, at.east(2).above());
        helper.onEachTick(crank::turn);
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(output.getNetwork() == crank.getNetwork(), "cogs join one run");
            helper.assertTrue(Math.abs(output.getRatio() + 0.5f) < 1e-4, "1 tooth on 2 is -0.5, got " + output.getRatio());
            helper.assertTrue(Math.abs(output.getRpm().value() + CRANK_RPM / 2) < 1e-3,
                    "the output turns at half the crank's speed, reversed; got " + output.getRpm().value());
            done(helper);
        });
    }
}
