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
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.fitting.sensor.CurrentSensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.item.ElectroscopeItem;
import io.github.soundgoodizerfan.feedback.item.GalvanometerItem;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.capacitor.LeydenJarBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.crank.HandCrankBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.dynamo.DynamoBlock;
import io.github.soundgoodizerfan.feedback.machine.dynamo.DynamoBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.dynamo.MotorBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.electromagnet.ElectromagnetBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.core.Direction;
import io.github.soundgoodizerfan.feedback.machine.cell.GalvanicCellBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.electrostatic.ElectrostaticGeneratorBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import io.github.soundgoodizerfan.feedback.machine.electrolytic.ElectrolyticCellBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
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

    // --- sensing ----------------------------------------------------------------------------

    /**
     * A wire is a data node, the galvanometer fitting on it is a sided sensor reached through the
     * same resolve every fitting uses, and its figure is the wire's own current quantised to the
     * fitting's resolution -- never the true value.
     */
    @GameTest(template = "empty")
    public static void currentSensorFittingReadsTheWiresCurrentAtItsResolution(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        CableBlockEntity wire = placeCable(helper, at);
        helper.assertTrue(wire instanceof DataNode, "every wire is a data node (mechanics 6.7)");

        CurrentSensorFitting sensor = new CurrentSensorFitting(wire, Direction.UP);
        wire.setSidedFitting(Direction.UP, sensor);
        DataNode found = DataNode.resolve(helper.getLevel(), new DataNodeRef(helper.absolutePos(at), Direction.UP))
                .orElse(null);
        helper.assertTrue(found == sensor, "the sided reference resolves to the fitting on the wire");
        helper.assertTrue(((SensorFitting) found).canRead(Quantity.CURRENT)
                && !((SensorFitting) found).canRead(Quantity.VOLTAGE), "it reads current and only current");

        wire.onResolved(1.2f, 2.3f, 0f, false);
        helper.assertTrue(Math.abs(sensor.readRaw() - 2.5f) < 1e-4f,
                "2.3 A at 0.5 A resolution reads 2.5, got " + sensor.readRaw());
        helper.assertTrue(Math.abs(wire.getAmps() - 2.3f) < 1e-4f, "while the wire's own figure is untouched");
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** The carried instruments are different instruments: neither reads what the other does. */
    @GameTest(template = "empty")
    public static void electroscopeAndGalvanometerReadDifferentQuantities(GameTestHelper helper) {
        ElectroscopeItem electroscope = (ElectroscopeItem) FItems.ELECTROSCOPE.get();
        GalvanometerItem galvanometer = (GalvanometerItem) FItems.GALVANOMETER.get();
        helper.assertTrue(electroscope.canRead(Quantity.VOLTAGE) && !electroscope.canRead(Quantity.CURRENT),
                "the electroscope reads potential, never current (6.7)");
        helper.assertTrue(galvanometer.canRead(Quantity.CURRENT) && !galvanometer.canRead(Quantity.VOLTAGE),
                "the galvanometer reads current");
        helper.assertTrue(electroscope.resolution(Quantity.VOLTAGE) > 0 && galvanometer.resolution(Quantity.CURRENT) > 0,
                "and neither is exact (philosophy 8)");
        helper.succeed();
    }

    // --- the electromagnet, end to end ------------------------------------------------------

    private static ElectromagnetBlockEntity placeMagnet(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, FBlocks.ELECTROMAGNET.get().defaultBlockState());
        return (ElectromagnetBlockEntity) helper.getBlockEntity(at);
    }

    /**
     * cell, `cables` cables, magnet in a column going up from {@code from}. Returns the magnet.
     * Up and not along, because the test structures sit a few blocks apart and a long row would
     * overwrite -- and be cleared over -- the neighbour's rig.
     */
    private static ElectromagnetBlockEntity row(GameTestHelper helper, BlockPos from, int cables) {
        helper.setBlock(from, FBlocks.GALVANIC_CELL.get().defaultBlockState());
        for (int i = 1; i <= cables; i++)
            placeCable(helper, from.above(i));
        return placeMagnet(helper, from.above(cables + 1));
    }

    private static void clearRow(GameTestHelper helper, BlockPos from, int cables) {
        for (int i = 0; i <= cables + 1; i++)
            helper.setBlock(from.above(i), Blocks.AIR.defaultBlockState());
    }

    /**
     * Source, wire, sink, with loss: the voltage a magnet sees is the cell's less the stated drop
     * per block per amp, and its field falls with it -- weaker, never refused.
     */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void magnetOnALongerRunSeesLessAndPullsWeaker(GameTestHelper helper) {
        BlockPos near = new BlockPos(0, 2, 1), far = new BlockPos(0, 2, 4);
        ElectromagnetBlockEntity a = row(helper, near, 2);
        ElectromagnetBlockEntity b = row(helper, far, 8);
        helper.runAfterDelay(6, () -> {
            float drop = 0.05f;   // copper.json electrical.drop
            helper.assertTrue(Math.abs(a.getVolts() - (1.1f - 2 * drop)) < 1e-3f,
                    "2 cables, 1 A: expected 1.0 V, saw " + a.getVolts());
            helper.assertTrue(Math.abs(b.getVolts() - (1.1f - 8 * drop)) < 1e-3f,
                    "8 cables, 1 A: expected 0.7 V, saw " + b.getVolts());
            helper.assertTrue(Math.abs(a.getAmps() - 1f) < 1e-3f && Math.abs(b.getAmps() - 1f) < 1e-3f,
                    "both are delivered what they asked for");
            helper.assertTrue(b.getFieldStrength() > 0f && b.getFieldStrength() < a.getFieldStrength(),
                    "the far magnet pulls weaker, not zero: " + b.getFieldStrength() + " vs " + a.getFieldStrength());
            helper.assertTrue(a.getCondition() == 1f && b.getCondition() == 1f, "under-voltage never wears");
            clearRow(helper, near, 2);
            clearRow(helper, far, 8);
            helper.succeed();
        });
    }

    /** Far enough and the magnet does nothing; it is not refused, not worn, not destroyed. */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aRunTooLongSlowsAMagnetToNothingAndNoFurther(GameTestHelper helper) {
        BlockPos from = new BlockPos(0, 2, 1);
        ElectromagnetBlockEntity magnet = row(helper, from, 24);
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(magnet.getVolts() == 0f, "24 cables at 1 A drop more than the cell has");
            helper.assertTrue(magnet.getFieldStrength() == 0f && !magnet.isRunning(), "so it does nothing");
            helper.assertTrue(magnet.getCondition() == 1f && !magnet.isRemoved(), "and is none the worse for it");
            clearRow(helper, from, 24);
            helper.succeed();
        });
    }

    /** A shortfall is shared: three magnets on one cell each get the same fraction. */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void magnetsOnOneCellShareTheShortfall(GameTestHelper helper) {
        BlockPos hub = new BlockPos(2, 2, 2);
        helper.setBlock(hub.west(), FBlocks.GALVANIC_CELL.get().defaultBlockState());
        placeCable(helper, hub);
        ElectromagnetBlockEntity a = placeMagnet(helper, hub.north());
        ElectromagnetBlockEntity b = placeMagnet(helper, hub.south());
        ElectromagnetBlockEntity c = placeMagnet(helper, hub.above());
        helper.runAfterDelay(6, () -> {
            float limit = FTuning.SOURCE_OVERDRAW_LIMIT * FTuning.GALVANIC_CELL_AMPS;
            float each = limit / 3f;
            for (ElectromagnetBlockEntity m : new ElectromagnetBlockEntity[] {a, b, c})
                helper.assertTrue(Math.abs(m.getAmps() - each) < 1e-3f,
                        "each magnet should get " + each + " A of its 1, got " + m.getAmps());
            helper.assertTrue(a.getWantedAmps() == 1f, "and each still reports wanting 1 A: a starved sink shows it");
            helper.assertTrue(a.getFieldStrength() > 0f, "starved is slow, not off");
            for (BlockPos p : new BlockPos[] {hub, hub.west(), hub.north(), hub.south(), hub.above()})
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    /**
     * Five amps down a 4 A cable warms it, over a settle point the thermal model fixes, and no
     * further -- the whole chain: sources, wire, sinks, resolver, thermal body.
     */
    @GameTest(template = "empty", timeoutTicks = 700)
    public static void overRatedRunWarmsItsWireToTheThermalModelsLimit(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 2, 2), b = a.east(), c = b.east();
        CableBlockEntity wa = placeCable(helper, a);
        placeCable(helper, b);
        CableBlockEntity wc = placeCable(helper, c);
        BlockPos[] cells = {a.west(), a.north(), a.south(), a.above(), a.below()};
        BlockPos[] magnets = {c.east(), c.north(), c.south(), c.above(), c.below()};
        for (BlockPos cell : cells)
            helper.setBlock(cell, FBlocks.GALVANIC_CELL.get().defaultBlockState());
        for (BlockPos magnet : magnets)
            placeMagnet(helper, magnet);
        float ambient = AmbientTable.at(helper.getLevel()).value();
        float over = 5f - wa.getRatingAmps();
        float ceiling = ambient + FTuning.WIRE_OVERLOAD_WORK_PER_AMP * over / FTuning.WIRE_LEAK.workPerTickPerTu();
        float[] peak = {0};
        helper.onEachTick(() -> peak[0] = Math.max(peak[0], Math.max(wa.getTemperature().value(), wc.getTemperature().value())));
        helper.runAfterDelay(500, () -> {
            helper.assertTrue(Math.abs(wa.getAmps() - 5f) < 1e-3f, "five magnets draw 5 A through the first cable, got " + wa.getAmps());
            helper.assertTrue(wa.getAmps() > wa.getRatingAmps(), "which is over its " + wa.getRatingAmps() + " A rating");
            helper.assertTrue(peak[0] > ambient + 10f, "the cable should be warm, peaked at " + peak[0]);
            helper.assertTrue(peak[0] <= ceiling + 1e-3f, "and never past " + ceiling + ", reached " + peak[0]);
            helper.assertTrue(wa.isInsulated() && wa.getCondition() == 1f, "a mild overload does no lasting harm");
            for (BlockPos p : cells)
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            for (BlockPos p : magnets)
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            helper.setBlock(a, Blocks.AIR.defaultBlockState());
            helper.setBlock(b, Blocks.AIR.defaultBlockState());
            helper.setBlock(c, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    /** Over-voltage is wear and heat on the machine, bounded, and the machine is never destroyed. */
    @GameTest(template = "empty")
    public static void overVoltageWearsAndHeatsAMagnetButNeverDestroysIt(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        ElectromagnetBlockEntity magnet = placeMagnet(helper, at);
        float ambient = AmbientTable.at(helper.getLevel()).value();
        float excess = 6f / FTuning.ELECTRIC_LV_VOLTS - 1f;   // fed an MV-reference voltage
        float ceiling = ambient + FTuning.ELECTROMAGNET_HEAT_PER_EXCESS * excess / FTuning.ELECTROMAGNET_LEAK.workPerTickPerTu();
        float peak = 0;
        for (int i = 0; i < 4000; i++) {
            magnet.onResolved(6f, 1f, 1f, false);
            peak = Math.max(peak, magnet.getTemperature().value());
        }
        helper.assertTrue(magnet.getCondition() == FTuning.ELECTROMAGNET_CONDITION_FLOOR,
                "worn to its floor and no further, condition is " + magnet.getCondition());
        helper.assertTrue(peak > ambient + 10f && peak <= ceiling + 1e-3f,
                "heated, but not past " + ceiling + ": " + peak);
        helper.assertFalse(magnet.isRemoved(), "never destroyed");
        helper.assertTrue(magnet.getFieldStrength() > 0f, "and still working");
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** What it pulls is decided by the material's magnetic property, not by what the item is called. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void magnetPullsMagneticItemsAndLeavesTheRest(GameTestHelper helper) {
        helper.assertTrue(ElectromagnetBlockEntity.isMagnetic(Items.IRON_INGOT), "iron is magnetic");
        helper.assertTrue(ElectromagnetBlockEntity.isMagnetic(Items.NETHERITE_INGOT),
                "so is netherite, by the same property and no special case");
        helper.assertFalse(ElectromagnetBlockEntity.isMagnetic(Items.COPPER_INGOT), "copper is not");

        BlockPos cell = new BlockPos(1, 2, 1), at = cell.east();
        helper.setBlock(cell, FBlocks.GALVANIC_CELL.get().defaultBlockState());
        ElectromagnetBlockEntity magnet = placeMagnet(helper, at);
        // Out at 2.2 blocks: inside the pull range of 0.73 field x 4 blocks, so it should come in.
        ItemEntity iron = helper.spawnItem(Items.IRON_INGOT, 4.2f, 2.5f, 1.5f);
        ItemEntity copper = helper.spawnItem(Items.COPPER_INGOT, 1.5f, 2.5f, 4.2f);
        iron.setNoGravity(true);
        copper.setNoGravity(true);
        iron.setDeltaMovement(0, 0, 0);
        copper.setDeltaMovement(0, 0, 0);
        double ironStart = iron.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(at)));
        double copperStart = copper.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(at)));
        helper.runAfterDelay(40, () -> {
            net.minecraft.world.phys.Vec3 centre = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(at));
            helper.assertTrue(magnet.getFieldStrength() > 0f, "the magnet is energised");
            helper.assertTrue(iron.position().distanceTo(centre) < ironStart - 0.3,
                    "the iron should have moved toward it, started " + ironStart + " now "
                            + iron.position().distanceTo(centre));
            helper.assertTrue(Math.abs(copper.position().distanceTo(centre) - copperStart) < 0.05,
                    "the copper should not have moved");
            iron.discard();
            copper.discard();
            helper.setBlock(cell, Blocks.AIR.defaultBlockState());
            helper.setBlock(at, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    // --- the dynamo (mechanics 6.8) -----------------------------------------------------------

    /**
     * A crank turning two shafts into a dynamo, and a magnet on the dynamo: the dynamo holds the
     * shaft's RPM times its winding's volts-per-RPM, the magnet is delivered current, and the
     * same figure times that current is charged to the shaft. Coarse on the north rig, fine on
     * the south: same shaft speed, a quarter the voltage.
     */
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void dynamoVoltsFollowTheShaftAndDrawLoadsIt(GameTestHelper helper) {
        BlockPos coarseAt = new BlockPos(0, 2, 0), fineAt = new BlockPos(0, 2, 2);
        HandCrankBlockEntity coarseCrank = RotationGameTests.crankRun(helper, coarseAt, 2);
        HandCrankBlockEntity fineCrank = RotationGameTests.crankRun(helper, fineAt, 2);
        helper.setBlock(coarseAt.east(3), FBlocks.DYNAMO.get().defaultBlockState()
                .setValue(DynamoBlock.FACING, Direction.WEST));
        helper.setBlock(fineAt.east(3), FBlocks.DYNAMO.get().defaultBlockState()
                .setValue(DynamoBlock.FACING, Direction.WEST).setValue(DynamoBlock.WINDING, DynamoBlock.Winding.FINE));
        ElectromagnetBlockEntity coarseMagnet = placeMagnet(helper, coarseAt.east(4));
        ElectromagnetBlockEntity fineMagnet = placeMagnet(helper, fineAt.east(4));
        DynamoBlockEntity coarse = (DynamoBlockEntity) helper.getBlockEntity(coarseAt.east(3));
        DynamoBlockEntity fine = (DynamoBlockEntity) helper.getBlockEntity(fineAt.east(3));

        RotationGameTests.each(helper, () -> {
            coarseCrank.turn();
            fineCrank.turn();
        });
        helper.runAtTickTime(110, () -> {
            float rpm = FTuning.HAND_CRANK_RPM.value();
            helper.assertTrue(Math.abs(coarse.getShaftRpm() - rpm) < 1e-3, "the coarse dynamo is turned at the crank's speed, got " + coarse.getShaftRpm());
            helper.assertTrue(Math.abs(coarse.getVolts() - rpm * FTuning.DYNAMO_COARSE_VOLTS_PER_RPM) < EPS,
                    "coarse: expected " + rpm * FTuning.DYNAMO_COARSE_VOLTS_PER_RPM + " V, held " + coarse.getVolts());
            helper.assertTrue(Math.abs(fine.getVolts() - rpm * FTuning.DYNAMO_FINE_VOLTS_PER_RPM) < EPS,
                    "fine: expected " + rpm * FTuning.DYNAMO_FINE_VOLTS_PER_RPM + " V, held " + fine.getVolts());
            helper.assertTrue(Math.abs(coarseMagnet.getAmps() - FTuning.ELECTROMAGNET_AMPS) < EPS
                    && Math.abs(fineMagnet.getAmps() - FTuning.ELECTROMAGNET_AMPS) < EPS, "both magnets are delivered what they want");
            helper.assertTrue(Math.abs(fine.getShaftLoad().value() - FTuning.ELECTROMAGNET_AMPS * FTuning.DYNAMO_FINE_VOLTS_PER_RPM) < EPS,
                    "the fine dynamo charges its shaft volts-per-RPM x amps, got " + fine.getShaftLoad().value());
            for (BlockPos p : BlockPos.betweenClosed(0, 2, 0, 4, 2, 2))
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            RotationGameTests.done(helper);
        });
    }

    /**
     * A crank turns a fine dynamo, a cable carries its current to a fine motor, and the motor turns
     * a shaft of its own: the shaft spins, never past the motor's no-load speed (the voltage it
     * sees over the winding figure), and the motor's torque is that same figure times its current.
     */
    @GameTest(template = "empty", timeoutTicks = 160)
    public static void motorTurnsAShaftFromTheLine(GameTestHelper helper) {
        BlockPos crankAt = new BlockPos(0, 2, 1);
        HandCrankBlockEntity crank = RotationGameTests.crankRun(helper, crankAt, 2);
        helper.setBlock(crankAt.east(3), FBlocks.DYNAMO.get().defaultBlockState()
                .setValue(DynamoBlock.FACING, Direction.WEST).setValue(DynamoBlock.WINDING, DynamoBlock.Winding.FINE));
        // Up, not along: a long row reaches the neighbouring test's rig.
        BlockPos motorAt = crankAt.east(3).above(2);
        placeCable(helper, crankAt.east(3).above());
        helper.setBlock(motorAt, FBlocks.MOTOR.get().defaultBlockState()
                .setValue(DynamoBlock.FACING, Direction.EAST).setValue(DynamoBlock.WINDING, DynamoBlock.Winding.FINE));
        helper.setBlock(motorAt.east(), RotationGameTests.alongX(FBlocks.SHAFT.get()));
        MotorBlockEntity motor = (MotorBlockEntity) helper.getBlockEntity(motorAt);

        RotationGameTests.each(helper, crank::turn);
        helper.runAtTickTime(140, () -> {
            float rpm = motor.getShaftRpm();
            helper.assertTrue(rpm > 0, "the motor turns its shaft");
            helper.assertTrue(rpm <= motor.noLoadRpm() + EPS, "never past no-load speed " + motor.noLoadRpm() + ", got " + rpm);
            helper.assertTrue(Math.abs(motor.getShaftTorque().value() - motor.getAmps() * FTuning.DYNAMO_FINE_VOLTS_PER_RPM) < EPS,
                    "torque is volts-per-RPM x amps, got " + motor.getShaftTorque().value());
            for (BlockPos p : BlockPos.betweenClosed(0, 2, 1, 4, 4, 1))
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            RotationGameTests.done(helper);
        });
    }

    // --- the Leyden jar (mechanics 6.6) -------------------------------------------------------

    /**
     * One turn of an electrostatic generator charges a jar through a cable; when the pulse ends
     * the jar becomes the source, and a magnet set against it afterwards runs on what it kept --
     * the generator alone would give it nothing by then.
     */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void leydenJarKeepsAPulseAndGivesItBack(GameTestHelper helper) {
        BlockPos generatorAt = new BlockPos(1, 2, 1), jarAt = generatorAt.east(2);
        helper.setBlock(generatorAt, FBlocks.ELECTROSTATIC_GENERATOR.get().defaultBlockState());
        placeCable(helper, generatorAt.east());
        helper.setBlock(jarAt, FBlocks.LEYDEN_JAR.get().defaultBlockState());
        ElectrostaticGeneratorBlockEntity generator = (ElectrostaticGeneratorBlockEntity) helper.getBlockEntity(generatorAt);
        LeydenJarBlockEntity jar = (LeydenJarBlockEntity) helper.getBlockEntity(jarAt);
        float[] charged = {0};

        helper.runAtTickTime(2, generator::turn);
        helper.runAtTickTime(12, () -> helper.assertFalse(jar.isSupplying(), "mid-pulse the jar is charging"));
        helper.runAtTickTime(30, () -> {
            helper.assertFalse(generator.isPulsing(), "the pulse is over");
            helper.assertTrue(jar.isSupplying(), "with the pulse gone the jar is the source");
            charged[0] = jar.getStoredVolts();
            helper.assertTrue(charged[0] > 0.9f * FTuning.ELECTROSTATIC_VOLTS && charged[0] <= FTuning.ELECTROSTATIC_VOLTS + EPS,
                    "the jar charged to about its source's voltage and never past it, held " + charged[0]);
            placeMagnet(helper, jarAt.east());
        });
        helper.runAtTickTime(40, () -> {
            ElectromagnetBlockEntity magnet = (ElectromagnetBlockEntity) helper.getBlockEntity(jarAt.east());
            helper.assertTrue(magnet.getAmps() > 0.5f && magnet.getFieldStrength() > 0f,
                    "the magnet runs on the jar, delivered " + magnet.getAmps() + " A");
            helper.assertTrue(jar.getStoredVolts() < charged[0], "and the jar empties as it does");
            for (BlockPos p : BlockPos.betweenClosed(generatorAt, jarAt.east()))
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    // --- coil rounds --------------------------------------------------------------------------

    /**
     * A jar round drives a coil by its volts: nominal volts is a nominal redstone charge, a cell's
     * 1.1 V barely pushes, twice nominal overdrives a thin winding, and the charge leaks to
     * nothing on its own. A charger on a cell fills a round to the cell's voltage and no further.
     */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aChargedRoundDrivesACoil(GameTestHelper helper) {
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        var coil = io.github.soundgoodizerfan.feedback.weapon.GunDesign.starter(Feedback.id("steel"))
                .withLauncher(io.github.soundgoodizerfan.feedback.weapon.GunDesign.Launcher.COIL);
        var dust = io.github.soundgoodizerfan.feedback.weapon.Cartridge.referenceCoil(9);
        var dry = new io.github.soundgoodizerfan.feedback.weapon.Cartridge(9, dust.caseMaterial(), List.of(), dust.projectile(),
                java.util.Optional.empty(), false, java.util.Optional.of(new io.github.soundgoodizerfan.feedback.weapon.Cartridge.Stored(0, 0)));
        float nominal = coil.bake(materials, dust).orElseThrow().muzzleEnergy();
        float jar = coil.bake(materials, dry.withVolts(FTuning.GUN_COIL_NOMINAL_VOLTS, 0)).orElseThrow().muzzleEnergy();
        helper.assertTrue(Math.abs(jar - nominal) < 1e-3f * nominal, "nominal volts is a nominal charge, got " + jar + " against " + nominal);
        helper.assertTrue(coil.bake(materials, dry.withVolts(1.1f, 0)).orElseThrow().muzzleEnergy() < 0.3f * nominal,
                "a cell's volts barely push");
        helper.assertTrue(coil.withWall(io.github.soundgoodizerfan.feedback.weapon.GunDesign.Wall.THIN)
                .bake(materials, dry.withVolts(2 * FTuning.GUN_COIL_NOMINAL_VOLTS, 0)).orElseThrow().overpressure() > 1f,
                "twice nominal overdrives a thin winding");
        helper.assertTrue(io.github.soundgoodizerfan.feedback.weapon.GunDesign.starter(Feedback.id("steel"))
                .bake(materials, dry.withVolts(FTuning.GUN_COIL_NOMINAL_VOLTS, 0)).isEmpty(), "a chamber does not fire a jar round");
        helper.assertTrue(dry.withVolts(FTuning.GUN_COIL_NOMINAL_VOLTS, 0).at(6000).volts() == 0f, "and it goes flat by itself");

        BlockPos cell = new BlockPos(1, 2, 1), cable = cell.east(), clip = cable.east();
        helper.setBlock(cell, FBlocks.GALVANIC_CELL.get().defaultBlockState());
        placeCable(helper, cable);
        helper.setBlock(clip, FBlocks.ROUND_CHARGER.get().defaultBlockState());
        var charger = (io.github.soundgoodizerfan.feedback.machine.charger.RoundChargerBlockEntity) helper.getBlockEntity(clip);
        var stack = io.github.soundgoodizerfan.feedback.weapon.GunAmmo.loose(dry.withVolts(0, helper.getLevel().getGameTime()));
        stack.setCount(2);
        charger.insert(stack);
        helper.runAfterDelay(40, () -> {
            float volts = charger.getRounds().get(io.github.soundgoodizerfan.feedback.registry.FWeapons.CARTRIDGE.get())
                    .at(helper.getLevel().getGameTime()).volts();
            helper.assertTrue(volts > 1.0f && volts <= 1.1f + 1e-3f, "charged to the cell and no further, got " + volts);
            for (BlockPos p : List.of(cell, cable, clip))
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    // --- the electrolytic cell (mechanics 6.14) ----------------------------------------------

    private static final ResourceLocation COPPER = Feedback.id("copper"), ZINC = Feedback.id("zinc");

    /** A cell with a full tank and {@code brass} brass ingots hung as its anode. */
    private static ElectrolyticCellBlockEntity brassCell(GameTestHelper helper, BlockPos at, int brass) {
        helper.setBlock(at, FBlocks.ELECTROLYTIC_CELL.get().defaultBlockState());
        ElectrolyticCellBlockEntity cell = (ElectrolyticCellBlockEntity) helper.getBlockEntity(at);
        cell.getWater().fill(new FluidStack(Fluids.WATER, FTuning.ELECTROLYTIC_CELL_TANK_MB),
                IFluidHandler.FluidAction.EXECUTE);
        cell.insertAnode(new ItemStack(FItems.BRASS_INGOT.get(), brass));
        return cell;
    }

    /** Ticks until {@code ingots} ingots' worth of metal has gone through at the cell's own current. */
    private static int ticksFor(int ingots) {
        return Math.round(ingots * FTuning.QU_PER_INGOT.value()
                / (FTuning.ELECTROLYTIC_CELL_AMPS * FTuning.QU_PER_AMP_TICK));
    }

    private static void run(ElectrolyticCellBlockEntity cell, float volts, int ticks) {
        for (int i = 0; i < ticks; i++)
            cell.onResolved(volts, FTuning.ELECTROLYTIC_CELL_AMPS, FTuning.ELECTROLYTIC_CELL_AMPS, false);
    }

    /**
     * Below zinc's deposition potential (copper's plus the 1.1 V gap) brass gives up its copper
     * and keeps its zinc in the water. Left running with the anode gone, below water's potential
     * the cell simply stops: the finished cathode comes off as copper.
     */
    @GameTest(template = "empty")
    public static void cellRefinesCopperOutOfBrassBelowZincsPotential(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        ElectrolyticCellBlockEntity cell = brassCell(helper, at, 2);
        run(cell, 1.1f, ticksFor(2));
        run(cell, 1.1f, ticksFor(2));   // overrun, anode long gone
        helper.assertTrue(cell.getAnode().isEmpty(), "the anode dissolved");
        helper.assertFalse(cell.getDeposit().containsKey(ZINC), "no zinc plated: " + cell.getDeposit());
        helper.assertTrue(Math.abs(cell.getDeposit().getOrDefault(COPPER, 0f) - 13f) < 0.05f,
                "the copper share of two ingots plated: " + cell.getDeposit());
        helper.assertTrue(Math.abs(cell.getSolution().getOrDefault(ZINC, 0f) - 7f) < 0.05f,
                "and the zinc stayed in the water: " + cell.getSolution());
        List<ItemStack> off = cell.takeCathode();
        helper.assertTrue(off.size() == 1 && off.get(0).is(Items.COPPER_INGOT) && off.get(0).getCount() == 1,
                "one copper ingot off the cathode, the part-ingot left on: " + off);
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /**
     * The same refined cathode, overrun at a voltage above water's: with the anode gone the
     * current strips the water, the zinc that waited behind the copper plates over it, and the
     * finished deposit comes off as scrap.
     */
    @GameTest(template = "empty")
    public static void overrunAboveWatersPotentialPlatesZincOverTheFinishedCopper(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        ElectrolyticCellBlockEntity cell = brassCell(helper, at, 2);
        run(cell, 1.1f, ticksFor(2));
        run(cell, 1.5f, ticksFor(1));
        helper.assertTrue(cell.getDeposit().getOrDefault(ZINC, 0f) > 6.9f, "the zinc plated: " + cell.getDeposit());
        List<ItemStack> off = cell.takeCathode();
        helper.assertTrue(off.size() == 1 && off.get(0).is(FItems.COPPER_SCRAP.get()) && off.get(0).getCount() == 2,
                "the whole deposit is copper scrap now: " + off);
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    // --- electroplating (mechanics 6.15) ------------------------------------------------------

    /**
     * Copper onto an iron ingot, three ways: a cell's single amp for 1 Qu comes out sound; a coarse
     * dynamo's eight amps for the same coat burns it, too much current for an ingot's surface; and
     * the single amp left on for 2.5 Qu grows a coat too thick to hold, which flakes off as the
     * piece comes out -- bare iron and copper nuggets.
     */
    @GameTest(template = "empty")
    public static void platingIsSoundBurntOrSpalledByCurrentAndTime(GameTestHelper helper) {
        ItemStack sound = plate(helper, new BlockPos(0, 2, 0), 1f, 1f).get(0);
        Plating p = sound.get(FDataComponents.PLATING.get());
        helper.assertTrue(p != null && p.metal().equals(COPPER) && p.quality() == Plating.Quality.SOUND
                && Math.abs(p.thickness() - 1f) < 0.01f, "one amp, 1 Qu: a sound copper coat, got " + p);

        Plating burnt = plate(helper, new BlockPos(2, 2, 0), 8f, 1f).get(0).get(FDataComponents.PLATING.get());
        helper.assertTrue(burnt != null && burnt.quality() == Plating.Quality.BURNT, "eight amps on an ingot burns it, got " + burnt);

        List<ItemStack> spalled = plate(helper, new BlockPos(0, 2, 2), 1f, 2.5f);
        helper.assertTrue(spalled.get(0).is(Items.IRON_INGOT) && !spalled.get(0).has(FDataComponents.PLATING.get()),
                "a coat too thick flakes off and leaves the iron bare: " + spalled);
        helper.assertTrue(spalled.size() == 2 && spalled.get(1).getCount() == 2,
                "and the copper comes off as two nuggets: " + spalled);
        for (BlockPos at : BlockPos.betweenClosed(0, 2, 0, 2, 2, 2))
            helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** A copper anode, an iron ingot hung to be plated, {@code qu} Qu put through at {@code amps}. */
    private static List<ItemStack> plate(GameTestHelper helper, BlockPos at, float amps, float qu) {
        helper.setBlock(at, FBlocks.ELECTROLYTIC_CELL.get().defaultBlockState());
        ElectrolyticCellBlockEntity cell = (ElectrolyticCellBlockEntity) helper.getBlockEntity(at);
        cell.getWater().fill(new FluidStack(Fluids.WATER, FTuning.ELECTROLYTIC_CELL_TANK_MB),
                IFluidHandler.FluidAction.EXECUTE);
        cell.insertAnode(new ItemStack(Items.COPPER_INGOT));
        cell.hangWork(new ItemStack(Items.IRON_INGOT));
        int ticks = Math.round(qu / (amps * FTuning.QU_PER_AMP_TICK));
        for (int i = 0; i < ticks; i++)
            cell.onResolved(0.5f, amps, amps, false);
        return cell.takeWork();
    }
}
