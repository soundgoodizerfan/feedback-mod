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

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricMember;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRun;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.cell.GalvanicCellBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.electrostatic.ElectrostaticGeneratorBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The electrical resolver and the wire's thermal body (mechanics 6, slice 4).
 *
 * <p>The resolver is pure arithmetic over a graph, so most of these build a run out of plain
 * {@link Fake} members and tick it -- no blocks, no world, and the figures are checked exactly. The
 * wire tests then place a real cable and drive its own {@code step}, because a wire's thermal
 * life is what is being pinned there and a source is not needed to over-rate one.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class ElectricGameTests {

    private static final float EPS = 1e-4f;

    /** A hand-built member, with whatever the last tick told it kept for the assertions. */
    static final class Fake implements ElectricMember {
        final Role role;
        float volts, amps, cap, wants, drop;
        float seenVolts, seenAmps, seenWanted;
        boolean seenBlocked;

        Fake(Role role) {
            this.role = role;
        }

        static Fake source(float volts, float cap) {
            Fake f = new Fake(Role.SOURCE);
            f.volts = volts;
            f.cap = cap;
            return f;
        }

        static Fake wire(float drop) {
            Fake f = new Fake(Role.WIRE);
            f.drop = drop;
            return f;
        }

        static Fake sink(float wants) {
            Fake f = new Fake(Role.SINK);
            f.wants = wants;
            return f;
        }

        @Override public Role role() { return role; }
        @Override public float sourceVolts() { return volts; }
        @Override public float sourceAmps() { return cap; }
        @Override public float wantedAmps() { return wants; }
        @Override public float dropPerAmp() { return drop; }
        @Override public VoltageBand ratedBand() { return VoltageBand.LV; }

        @Override
        public void onResolved(float volts, float amps, float wanted, boolean blocked) {
            seenVolts = volts;
            seenAmps = amps;
            seenWanted = wanted;
            seenBlocked = blocked;
        }
    }

    /** members[0] - members[1] - ... in a line. */
    static ElectricRun line(Fake... members) {
        int[][] adjacent = new int[members.length][];
        for (int i = 0; i < members.length; i++) {
            List<Integer> n = new ArrayList<>();
            if (i > 0)
                n.add(i - 1);
            if (i < members.length - 1)
                n.add(i + 1);
            adjacent[i] = n.stream().mapToInt(Integer::intValue).toArray();
        }
        return new ElectricRun(List.of(members), adjacent);
    }

    static Fake[] chain(Fake source, int wires, float drop, Fake sink) {
        Fake[] out = new Fake[wires + 2];
        out[0] = source;
        for (int i = 1; i <= wires; i++)
            out[i] = Fake.wire(drop);
        out[wires + 1] = sink;
        return out;
    }

    /** The voltage a sink sees falls with the distance and with the current, and by a stated amount. */
    @GameTest(template = "empty")
    public static void sinkSeesLessWithDistanceAndCurrent(GameTestHelper helper) {
        float drop = 0.05f;
        Fake nearSink = Fake.sink(1);
        line(chain(Fake.source(1.1f, 2), 2, drop, nearSink)).tick();
        Fake farSink = Fake.sink(1);
        line(chain(Fake.source(1.1f, 2), 10, drop, farSink)).tick();
        Fake heavySink = Fake.sink(2);
        line(chain(Fake.source(1.1f, 2), 10, drop, heavySink)).tick();

        helper.assertTrue(Math.abs(nearSink.seenVolts - (1.1f - 2 * drop * 1)) < EPS,
                "2 blocks at 1 A: expected 1.0 V, saw " + nearSink.seenVolts);
        helper.assertTrue(Math.abs(farSink.seenVolts - (1.1f - 10 * drop * 1)) < EPS,
                "10 blocks at 1 A: expected 0.6 V, saw " + farSink.seenVolts);
        helper.assertTrue(Math.abs(heavySink.seenVolts - (1.1f - 10 * drop * 2)) < EPS,
                "10 blocks at 2 A: expected 0.1 V, saw " + heavySink.seenVolts);
        helper.assertTrue(farSink.seenVolts < nearSink.seenVolts && heavySink.seenVolts < farSink.seenVolts,
                "more distance or more current must never raise the voltage a sink sees");
        helper.assertTrue(Math.abs(farSink.seenAmps - 1f) < EPS, "a covered sink is delivered what it wanted");
        helper.succeed();
    }

    /** A path whose drop reaches the supply delivers zero volts, and never a negative figure. */
    @GameTest(template = "empty")
    public static void tooLongAWireDeliversZeroNotNegative(GameTestHelper helper) {
        Fake sink = Fake.sink(1);
        line(chain(Fake.source(1.1f, 2), 40, 0.05f, sink)).tick();
        helper.assertTrue(sink.seenVolts == 0f, "40 blocks at 0.05 V per amp should read 0 V, got " + sink.seenVolts);
        helper.succeed();
    }

    /** A run with no source reads zero on everything, and a sink still reports what it wanted. */
    @GameTest(template = "empty")
    public static void runWithNoSourceReadsZero(GameTestHelper helper) {
        Fake wire = Fake.wire(0.05f);
        Fake sink = Fake.sink(1);
        ElectricRun run = line(wire, sink);
        wire.seenVolts = 9;
        sink.seenVolts = 9;
        sink.seenAmps = 9;
        run.tick();
        helper.assertTrue(wire.seenVolts == 0 && wire.seenAmps == 0, "wire reads zero with no supply");
        helper.assertTrue(sink.seenVolts == 0 && sink.seenAmps == 0 && sink.seenWanted == 1f,
                "sink reads zero delivered, one wanted: " + sink.seenVolts + " V " + sink.seenAmps + " A "
                        + sink.seenWanted + " wanted");
        helper.succeed();
    }

    /** When supply cannot cover demand every sink gets the same fraction, not nearest-first. */
    @GameTest(template = "empty")
    public static void shortfallIsSharedInProportion(GameTestHelper helper) {
        Fake source = Fake.source(1.5f, 1);
        Fake hub = Fake.wire(0.05f);
        Fake a = Fake.sink(2);
        Fake b = Fake.sink(1);
        // source - hub - {a, b}
        ElectricRun run = new ElectricRun(List.of(source, hub, a, b),
                new int[][] {{1}, {0, 2, 3}, {1}, {1}});
        run.tick();
        // limit is 1 A x 2.0; demand 3 A; every sink gets 2/3 of what it wants.
        float share = FTuning.SOURCE_OVERDRAW_LIMIT * 1f / 3f;
        helper.assertTrue(Math.abs(a.seenAmps - 2 * share) < EPS && Math.abs(b.seenAmps - 1 * share) < EPS,
                "expected " + 2 * share + " and " + share + " A, got " + a.seenAmps + " and " + b.seenAmps);
        helper.assertTrue(Math.abs(a.seenAmps / a.seenWanted - b.seenAmps / b.seenWanted) < EPS,
                "both sinks must be short by the same fraction");
        helper.assertTrue(Math.abs(hub.seenAmps - 2f) < EPS, "the shared wire carries what was delivered");
        helper.succeed();
    }

    /** Drawn past its rating a source sags but does not drop to zero, and is not destroyed. */
    @GameTest(template = "empty")
    public static void overDrawSagsASourceAndNothingMore(GameTestHelper helper) {
        Fake source = Fake.source(1.5f, 1);
        Fake sink = Fake.sink(1.5f);
        line(source, sink).tick();
        float expected = 1.5f * (1f - (1f - FTuning.SOURCE_SAG_FLOOR) * 0.5f / (FTuning.SOURCE_OVERDRAW_LIMIT - 1f));
        helper.assertTrue(Math.abs(source.seenVolts - expected) < EPS,
                "1.5x rated draw should hold " + expected + " V, held " + source.seenVolts);
        helper.assertTrue(Math.abs(sink.seenVolts - expected) < EPS, "the sink sees the sagged voltage");

        Fake hard = Fake.source(1.5f, 1);
        Fake hungry = Fake.sink(50);
        line(hard, hungry).tick();
        helper.assertTrue(Math.abs(hard.seenVolts - 1.5f * FTuning.SOURCE_SAG_FLOOR) < EPS,
                "a source drawn far past its rating holds the floor, not zero");
        helper.assertTrue(Math.abs(hungry.seenAmps - FTuning.SOURCE_OVERDRAW_LIMIT) < EPS,
                "and supplies no more than its limit");
        helper.succeed();
    }

    /** A lower-voltage source on a run with a higher one is blocked and says so. */
    @GameTest(template = "empty")
    public static void lowerVoltageSourceIsBlocked(GameTestHelper helper) {
        Fake low = Fake.source(1.1f, 1);
        Fake wire = Fake.wire(0f);
        Fake high = Fake.source(1.5f, 1);
        Fake sink = Fake.sink(1);
        // low - wire - high, and the sink on the wire
        ElectricRun run = new ElectricRun(List.of(low, wire, high, sink),
                new int[][] {{1}, {0, 2, 3}, {1}, {1}});
        run.tick();
        helper.assertTrue(low.seenBlocked && low.seenAmps == 0f, "the 1.1 V source must be blocked");
        helper.assertFalse(high.seenBlocked, "the 1.5 V source is not");
        helper.assertTrue(Math.abs(sink.seenVolts - 1.5f) < EPS, "the sink sees the higher voltage, not a blend");
        helper.assertTrue(Math.abs(high.seenAmps - 1f) < EPS, "the higher source carries the whole draw");
        helper.succeed();
    }

    /** The tree is cached between ticks and rebuilt only when what is supplying changes. */
    @GameTest(template = "empty")
    public static void routingRebuildsOnlyWhenSupplyChanges(GameTestHelper helper) {
        Fake source = Fake.source(0f, 1);   // idle, as a pulse source is between pulses
        Fake sink = Fake.sink(1);
        ElectricRun run = line(source, sink);
        run.tick();
        run.tick();
        run.tick();
        helper.assertTrue(run.rebuilds() == 1, "an idle run is routed once, not per tick: " + run.rebuilds());
        helper.assertTrue(sink.seenVolts == 0f, "and an idle source feeds nothing");

        source.volts = 1.5f;
        run.tick();
        helper.assertTrue(run.rebuilds() == 2, "the source going live re-routes once: " + run.rebuilds());
        helper.assertTrue(Math.abs(sink.seenVolts - 1.5f) < EPS, "the sink is now fed");
        run.tick();
        run.tick();
        helper.assertTrue(run.rebuilds() == 2, "a steady live run is not re-routed: " + run.rebuilds());

        source.volts = 0f;
        run.tick();
        helper.assertTrue(run.rebuilds() == 3 && sink.seenVolts == 0f, "the pulse ending re-routes and reads zero");
        helper.succeed();
    }

    // --- the wire ---------------------------------------------------------------------------

    private static CableBlockEntity placeCable(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, FBlocks.COPPER_CABLE.get().defaultBlockState());
        return (CableBlockEntity) helper.getBlockEntity(at);
    }

    /** Inside its rating a wire is given no heat, so it never leaves ambient and never wears. */
    @GameTest(template = "empty")
    public static void wireInsideItsRatingNeverHeats(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        CableBlockEntity wire = placeCable(helper, at);
        float rating = wire.getRatingAmps();
        helper.assertTrue(Math.abs(rating - FTuning.WIRE_AMPS_PER_AMPACITY) < EPS,
                "copper (ampacity 1) should be rated " + FTuning.WIRE_AMPS_PER_AMPACITY + " A, is " + rating);
        float ambient = AmbientTable.at(helper.getLevel()).value();
        for (int i = 0; i < 2000; i++)
            wire.step(rating);
        helper.assertTrue(Math.abs(wire.getTemperature().value() - ambient) < EPS,
                "a wire at its rating must sit at ambient, is " + wire.getTemperature().value());
        helper.assertTrue(wire.isInsulated() && wire.getCondition() == 1f, "and shows no wear");
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /**
     * Over its rating a wire warms, and never past the figure the thermal model allows for the
     * input it was given: ambient plus input over leak.
     */
    @GameTest(template = "empty")
    public static void overRatingWarmsButNeverPastEquilibrium(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        CableBlockEntity wire = placeCable(helper, at);
        float ambient = AmbientTable.at(helper.getLevel()).value();
        float over = 1f;   // 1 A over, so the burn-off is not reached
        float ceiling = ambient + FTuning.WIRE_OVERLOAD_WORK_PER_AMP * over / FTuning.WIRE_LEAK.workPerTickPerTu();
        float highest = 0;
        for (int i = 0; i < 3000; i++) {
            wire.step(wire.getRatingAmps() + over);
            highest = Math.max(highest, wire.getTemperature().value());
        }
        helper.assertTrue(highest > ambient + 10f, "over-rating must warm the wire, peaked at " + highest);
        helper.assertTrue(highest <= ceiling + EPS, "must never exceed " + ceiling + ", reached " + highest);
        // The input lands before the tick's leak, so the discrete settle point sits one tick's
        // worth of input (input / mass) under the continuous ceiling.
        float settles = ceiling - FTuning.WIRE_OVERLOAD_WORK_PER_AMP * over / FTuning.WIRE_MASS.workPerTu();
        helper.assertTrue(Math.abs(highest - settles) < 0.05f, "and should settle on " + settles + ", reached " + highest);
        helper.assertTrue(wire.isInsulated() && wire.getCondition() == 1f, "a mild overload leaves insulation and conductor alone");
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** Insulation goes first and the conductor is untouched; only then does the conductor degrade. */
    @GameTest(template = "empty")
    public static void insulationFailsBeforeTheConductor(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        CableBlockEntity wire = placeCable(helper, at);
        // 2 A over settles 100 Tu over ambient: past the insulation, under copper's service temperature.
        for (int i = 0; i < 3000; i++)
            wire.step(wire.getRatingAmps() + 2f);
        helper.assertFalse(wire.isInsulated(), "the insulation should have burned off at "
                + wire.getTemperature().value() + " Tu");
        helper.assertTrue(wire.getCondition() == 1f, "the conductor is untouched by that");
        helper.assertTrue(wire.getAmps() == 0f && !wire.isShocking(), "bare but carrying nothing shocks nobody");
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** Left overloaded, the order is: insulation, then degradation, then destruction. */
    @GameTest(template = "empty")
    public static void unfixedDegradationEndsInDestruction(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        CableBlockEntity wire = placeCable(helper, at);
        int insulationFailed = -1, degraded = -1, destroyed = -1;
        for (int i = 0; i < 20000 && destroyed < 0; i++) {
            wire.step(wire.getRatingAmps() + 10f);
            if (insulationFailed < 0 && !wire.isInsulated())
                insulationFailed = i;
            if (degraded < 0 && wire.getCondition() < 1f)
                degraded = i;
            if (wire.isRemoved())
                destroyed = i;
        }
        helper.assertTrue(insulationFailed >= 0 && degraded >= 0 && destroyed >= 0,
                "all three stages should be reached: " + insulationFailed + " " + degraded + " " + destroyed);
        helper.assertTrue(insulationFailed < degraded && degraded < destroyed,
                "in order: insulation " + insulationFailed + ", degradation " + degraded + ", destruction " + destroyed);
        helper.assertTrue(destroyed - degraded > 20, "and never in one step: " + (destroyed - degraded) + " ticks of degradation");
        helper.succeed();
    }

    /** Placing builds one run, cutting it makes two, and a wire with nothing on it reads zero. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void topologyChangeRebuildsTheRun(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 2, 1), b = a.east(), c = b.east();
        CableBlockEntity wa = placeCable(helper, a);
        CableBlockEntity wb = placeCable(helper, b);
        CableBlockEntity wc = placeCable(helper, c);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(wa.getRun() != null && wa.getRun() == wb.getRun() && wb.getRun() == wc.getRun(),
                    "three touching cables are one run");
            helper.assertTrue(wa.getRun().size() == 3, "of three members, was " + wa.getRun().size());
            helper.assertTrue(wb.getVolts() == 0f && wb.getAmps() == 0f, "with no source it reads zero");
            helper.setBlock(b, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(wa.getRun() != null && wc.getRun() != null && wa.getRun() != wc.getRun(),
                        "cutting the middle leaves two separate runs");
                helper.assertTrue(wa.getRun().size() == 1 && wc.getRun().size() == 1, "of one each");
                helper.setBlock(a, Blocks.AIR.defaultBlockState());
                helper.setBlock(c, Blocks.AIR.defaultBlockState());
                helper.succeed();
            });
        });
    }

    // --- sources ----------------------------------------------------------------------------

    /** A cell holds the difference of its electrodes' potentials, read from the material files. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void cellHoldsItsElectrodePairsPotential(GameTestHelper helper) {
        BlockPos cell = new BlockPos(1, 2, 1), cable = cell.east();
        helper.setBlock(cell, FBlocks.GALVANIC_CELL.get().defaultBlockState());
        CableBlockEntity wire = placeCable(helper, cable);
        helper.runAfterDelay(4, () -> {
            // zinc -0.76 V against copper +0.34 V
            helper.assertTrue(Math.abs(wire.getVolts() - 1.1f) < 1e-3f,
                    "a zinc/copper cell should put 1.1 V on the wire, got " + wire.getVolts());
            helper.assertTrue(wire.getAmps() == 0f, "and nothing flows with nothing to load it");
            GalvanicCellBlockEntity be = (GalvanicCellBlockEntity) helper.getBlockEntity(cell);
            helper.assertTrue(be.offered().voltage() == VoltageBand.LV, "1.1 V is the LV band");
            helper.setBlock(cell, Blocks.AIR.defaultBlockState());
            helper.setBlock(cable, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    /**
     * A pulse source is live only while it pulses, and while it does it outbids a lower-voltage
     * cell on the same line, which is blocked for exactly that long and no longer.
     */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void pulseSourceIsLiveOnlyWhilePulsingAndBlocksALowerCell(GameTestHelper helper) {
        BlockPos pulse = new BlockPos(1, 2, 1), wirePos = pulse.east(), cellPos = wirePos.east();
        helper.setBlock(pulse, FBlocks.ELECTROSTATIC_GENERATOR.get().defaultBlockState());
        CableBlockEntity wire = placeCable(helper, wirePos);
        helper.setBlock(cellPos, FBlocks.GALVANIC_CELL.get().defaultBlockState());
        ElectrostaticGeneratorBlockEntity generator = (ElectrostaticGeneratorBlockEntity) helper.getBlockEntity(pulse);
        GalvanicCellBlockEntity cell = (GalvanicCellBlockEntity) helper.getBlockEntity(cellPos);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(Math.abs(wire.getVolts() - 1.1f) < 1e-3f, "idle generator: the cell alone holds the line");
            helper.assertFalse(cell.isBlocked(), "and is not blocked");
            int before = wire.getRun().rebuilds();
            generator.turn();
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(Math.abs(wire.getVolts() - FTuning.ELECTROSTATIC_VOLTS) < 1e-3f,
                        "pulsing: the line is at the generator's voltage, got " + wire.getVolts());
                helper.assertTrue(cell.isBlocked(), "and the lower-voltage cell is blocked");
                helper.assertTrue(wire.getRun().rebuilds() > before, "the supply changing re-routed the run");
                helper.runAfterDelay(FTuning.ELECTROSTATIC_PULSE_TICKS + 3, () -> {
                    helper.assertFalse(generator.isPulsing(), "the pulse ends");
                    helper.assertTrue(Math.abs(wire.getVolts() - 1.1f) < 1e-3f,
                            "and the line falls back to the cell, got " + wire.getVolts());
                    helper.assertFalse(cell.isBlocked(), "which is no longer blocked");
                    helper.setBlock(pulse, Blocks.AIR.defaultBlockState());
                    helper.setBlock(wirePos, Blocks.AIR.defaultBlockState());
                    helper.setBlock(cellPos, Blocks.AIR.defaultBlockState());
                    helper.succeed();
                });
            });
        });
    }
}
