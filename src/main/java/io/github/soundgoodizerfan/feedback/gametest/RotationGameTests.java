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

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
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
 * {@code docs/feedback_vehicles_spec.md} section 5's drivetrain plan changes it: spin-up, overstress,
 * coasting, belts and gearing. A Hand Crank turned every tick is the source -- 32 RPM, 12 Su --
 * because it needs no water, steam or design. Speeds are read off the network, whose arithmetic
 * is deterministic, so the per-tick rates are checked exactly; only when things happen is loose.
 * <p>
 * The {@code push...} tests drive {@link RotationNetwork#driveExternally} straight from the test,
 * as the free-form drivetrain will (spec section 5, step 1). The test ticks after the level, so a
 * push written here is used by the next network tick and gone by the next test tick -- which is
 * why overstress is asked about straight after pushing, never later.
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
        FINISHED.add(helper);
        for (BlockPos p : BlockPos.betweenClosed(0, 1, 0, 3, 3, 2))
            helper.setBlock(p, net.minecraft.world.level.block.Blocks.AIR);
        helper.succeed();
    }

    /** Tests whose rig is gone; their per-tick work stops, since it can outlive the same tick. */
    private static final Set<GameTestHelper> FINISHED = Collections.newSetFromMap(new WeakHashMap<>());

    /** {@code helper.onEachTick}, but only until {@link #done}. */
    static void each(GameTestHelper helper, Runnable task) {
        helper.onEachTick(() -> {
            if (!FINISHED.contains(helper))
                task.run();
        });
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
        each(helper, () -> {
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
        each(helper, () -> {
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
        each(helper, crank::turn);
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
        each(helper, () -> {
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
        each(helper, crank::turn);
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
        each(helper, crank::turn);
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(output.getNetwork() == crank.getNetwork(), "cogs join one run");
            helper.assertTrue(Math.abs(output.getRatio() + 0.5f) < 1e-4, "1 tooth on 2 is -0.5, got " + output.getRatio());
            helper.assertTrue(Math.abs(output.getRpm().value() + CRANK_RPM / 2) < 1e-3,
                    "the output turns at half the crank's speed, reversed; got " + output.getRpm().value());
            done(helper);
        });
    }

    // --- the signed external channel (docs/feedback_vehicles_spec.md section 5, step 1) ------------

    private static final Drag NO_DRAG = new Drag(0);
    private static final Inertia NO_INERTIA = new Inertia(0);

    static void push(RotationNode on, float su) {
        if (on.getNetwork() != null)
            on.getNetwork().driveExternally(new Su(su), NO_DRAG, NO_INERTIA);
    }

    /** A push turns a run with no source, either way, and when it stops the run coasts. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void pushTurnsARunWithNoSource(GameTestHelper helper) {
        BlockPos forwardAt = new BlockPos(0, 2, 0), backAt = new BlockPos(0, 2, 2);
        for (int i = 0; i < 3; i++) {
            helper.setBlock(forwardAt.east(i), alongX(FBlocks.SHAFT.get()));
            helper.setBlock(backAt.east(i), alongX(FBlocks.SHAFT.get()));
        }
        RotationNode forward = node(helper, forwardAt), back = node(helper, backAt);
        float[] atRelease = {0};
        each(helper, () -> {
            if (helper.getTick() < 60) {
                push(forward, 5);
                push(back, -5);
            }
        });
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(forward.getNetwork().getCapacitySu().value() == 0, "no source on the run");
            helper.assertTrue(rpm(forward) > 5, "a positive push turns it toward +RPM, got " + rpm(forward));
            helper.assertTrue(rpm(back) < -5, "a negative push turns it toward -RPM, got " + rpm(back));
            atRelease[0] = rpm(forward);
        });
        helper.runAtTickTime(70, () -> {
            float now = rpm(forward);
            helper.assertTrue(now > 0 && now < atRelease[0], "let go, it coasts down: " + atRelease[0] + " then " + now);
            done(helper);
        });
    }

    /**
     * A push against the drive is load: below capacity it eats spin-up surplus and lowers the
     * friction ceiling; past capacity it overstresses, and a turning run winds down to a stall
     * and stays there -- it is not driven backwards against its source.
     */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void pushAgainstASourceIsLoadAndCanStallIt(GameTestHelper helper) {
        BlockPos lightAt = new BlockPos(0, 2, 0), stallAt = new BlockPos(0, 2, 2);
        HandCrankBlockEntity light = crankRun(helper, lightAt, 2), stall = crankRun(helper, stallAt, 2);
        RotationNode lightShaft = node(helper, lightAt.east(2)), stallShaft = node(helper, stallAt.east(2));
        float against = 6;
        float[] last = {0};
        int[] checked = {0};
        each(helper, () -> {
            light.turn();
            stall.turn();
            RotationNetwork net = lightShaft.getNetwork();
            if (net != null) {
                float now = rpm(lightShaft);
                if (last[0] > 3 && now < 25) {
                    float expected = (net.getCapacitySu().value() - against) / net.getInertia().suTicksPerRpm();
                    helper.assertTrue(Math.abs(now - last[0] - expected) < 1e-3,
                            "spin-up against a " + against + " Su push should gain " + expected + " a tick, gained " + (now - last[0]));
                    checked[0]++;
                }
                last[0] = now;
                push(lightShaft, -against);
                helper.assertFalse(net.isOverstressed(), "6 Su against a 12 Su crank is not overstress");
            }
            helper.assertTrue(rpm(stallShaft) >= 0, "a stalled run is never driven backwards, got " + rpm(stallShaft));
            if (helper.getTick() >= 40 && stallShaft.getNetwork() != null) {
                push(stallShaft, -20);
                RotationNetwork stalled = stallShaft.getNetwork();
                helper.assertTrue(stalled.isOverstressed(), "20 Su against a 12 Su crank is overstress; tick " + helper.getTick()
                        + " capacity " + stalled.getCapacitySu().value() + " target " + stalled.getTargetRpm().value()
                        + " rpm " + stalled.getCurrentRpm().value() + " load " + stalled.getLoadSu().value() + " size " + stalled.size());
            }
        });
        helper.runAtTickTime(39, () -> helper.assertTrue(Math.abs(rpm(stallShaft) - CRANK_RPM) < 1e-3,
                "up to speed before the push, got " + rpm(stallShaft)));
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(checked[0] > 5, "the loaded spin-up rate was checked on " + checked[0] + " ticks");
            RotationNetwork net = lightShaft.getNetwork();
            float ceiling = (net.getCapacitySu().value() - against) / net.getDragSuPerRpm().suPerRpm();
            helper.assertTrue(ceiling < CRANK_RPM && Math.abs(rpm(lightShaft) - ceiling) < 1e-2,
                    "the push lowers the friction ceiling to " + ceiling + ", got " + rpm(lightShaft));
            helper.assertTrue(rpm(stallShaft) == 0, "past capacity the run stalls, got " + rpm(stallShaft));
            done(helper);
        });
    }

    /**
     * A push with the drive is not capped by the source's speed -- a car rolling downhill
     * overspeeds the engine -- and once it stops, load pulls the run back to the source's speed:
     * engine braking, with nothing written for it.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void pushWithASourceOverspeedsItThenFallsBack(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        HandCrankBlockEntity crank = crankRun(helper, at, 2);
        RotationNode shaft = node(helper, at.east(2));
        each(helper, () -> {
            crank.turn();
            if (helper.getTick() >= 40 && helper.getTick() < 100)
                push(shaft, 20);
        });
        helper.runAtTickTime(39, () -> helper.assertTrue(Math.abs(rpm(shaft) - CRANK_RPM) < 1e-3,
                "at the crank's speed first, got " + rpm(shaft)));
        helper.runAtTickTime(100, () -> helper.assertTrue(rpm(shaft) > CRANK_RPM + 10,
                "pushed along, it runs past the crank's " + CRANK_RPM + ", got " + rpm(shaft)));
        helper.runAtTickTime(180, () -> {
            helper.assertTrue(Math.abs(rpm(shaft) - CRANK_RPM) < 1e-3, "and falls back to it, got " + rpm(shaft));
            done(helper);
        });
    }

    /** A push with the run never offsets a machine's overstress (author ruling, 2026-10-11). */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void pushWithARunDoesNotRescueOverstress(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        HandCrankBlockEntity crank = crankRun(helper, at, 1);
        helper.setBlock(at.east(2), FBlocks.DRILL_PRESS.get()); // 35 Su against the crank's 12
        RotationNode shaft = node(helper, at.east());
        each(helper, () -> {
            crank.turn();
            if (shaft.getNetwork() != null && shaft.getNetwork().getTargetRpm().value() > 0) {
                push(shaft, 50);
                helper.assertTrue(shaft.getNetwork().isOverstressed(), "50 Su of push does not pay a drill press's 35");
            }
        });
        helper.runAtTickTime(60, () -> done(helper));
    }

    // --- the centrifugal overspeed trip ---------------------------------------------------------

    private static net.minecraft.world.level.block.state.BlockState tripAlongX() {
        return FBlocks.OVERSPEED_TRIP.get().defaultBlockState()
                .setValue(io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock.FACING, Direction.EAST);
    }

    /**
     * A run turning at its source's own speed is under the trip's figure and the trip stays
     * closed: it wears and costs nothing until something overspeeds it.
     */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void overspeedTripHoldsAtSourceSpeed(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        HandCrankBlockEntity crank = crankRun(helper, at, 0);
        helper.setBlock(at.east(), tripAlongX());
        helper.setBlock(at.east(2), alongX(FBlocks.SHAFT.get()));
        RotationNode far = node(helper, at.east(2));
        each(helper, crank::turn);
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(CRANK_RPM < FTuning.OVERSPEED_TRIP_RPM.value(), "the crank is under the trip");
            helper.assertTrue(far.getNetwork() == crank.getNetwork(), "a closed trip couples the run");
            helper.assertTrue(Math.abs(far.getRpm().value() - CRANK_RPM) < 1e-3,
                    "the far shaft turns at the crank's speed, got " + far.getRpm().value());
            done(helper);
        });
    }

    /**
     * Pushed past the trip's figure, it lets go and stays let go: the far run is a separate
     * network with nothing driving it, and the trip itself is still bolted to the driving side.
     * (A push stands in for a source: a Hand Crank's 12 Su is friction-limited far under the
     * trip's speed even through a gear-up, and nothing else in a game test makes Su on demand.)
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void overspeedTripLetsGoAndLatches(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        helper.setBlock(at, alongX(FBlocks.SHAFT.get()));
        helper.setBlock(at.east(), tripAlongX());
        helper.setBlock(at.east(2), alongX(FBlocks.SHAFT.get()));
        var trip = (io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlockEntity) helper.getBlockEntity(at.east());
        RotationNode driving = node(helper, at), far = node(helper, at.east(2));
        boolean[] closedWhileSlow = {false};
        each(helper, () -> {
            if (helper.getTick() < 150)
                push(driving, 100);
            if (Math.abs(rpm(driving)) < FTuning.OVERSPEED_TRIP_RPM.value() && trip.isEngaged())
                closedWhileSlow[0] = true;
        });
        helper.runAtTickTime(180, () -> {
            helper.assertTrue(closedWhileSlow[0], "it held while the run was under its figure");
            helper.assertTrue(!trip.isEngaged(), "pushed past " + FTuning.OVERSPEED_TRIP_RPM.value()
                    + " RPM the trip is open, driving side at " + rpm(driving));
            helper.assertTrue(far.getNetwork() != driving.getNetwork(), "the far run is cut off");
            helper.assertTrue(trip.getNetwork() == driving.getNetwork(), "the trip stays on the driving run");
            done(helper);
        });
    }
}
