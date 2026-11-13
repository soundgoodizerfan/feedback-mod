// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.gametest;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
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
import io.github.soundgoodizerfan.feedback.machine.dynamo.DynamotorBlock;
import io.github.soundgoodizerfan.feedback.machine.dynamo.DynamotorBlockEntity;
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
import io.github.soundgoodizerfan.feedback.machine.coil.CoilBlockEntity;
import net.minecraft.world.item.Item;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.machine.breaker.BreakerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.breaker.FuseBlockEntity;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The electrical resolver and the wire's thermal body.
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
        VoltageBand band;
        float cap, wants, loss;
        float seenEu, seenAmps, seenWanted;
        VoltageBand seenBand;
        boolean seenBlocked;

        Fake(Role role) {
            this.role = role;
        }

        static Fake source(VoltageBand band, float cap) {
            Fake f = new Fake(Role.SOURCE);
            f.band = band;
            f.cap = cap;
            return f;
        }

        static Fake wire(float loss) {
            Fake f = new Fake(Role.WIRE);
            f.loss = loss;
            return f;
        }

        static Fake sink(float wants) {
            Fake f = new Fake(Role.SINK);
            f.wants = wants;
            return f;
        }

        @Override public Role role() { return role; }
        @Override public VoltageBand sourceBand() { return band; }
        @Override public float sourceAmps() { return cap; }
        @Override public float wantedAmps() { return wants; }
        @Override public float lossPerPacket() { return loss; }
        @Override public VoltageBand ratedBand() { return VoltageBand.LV; }

        @Override
        public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
            seenBand = band;
            seenEu = eu;
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

    static Fake[] chain(Fake source, int wires, float loss, Fake sink) {
        Fake[] out = new Fake[wires + 2];
        out[0] = source;
        for (int i = 1; i <= wires; i++)
            out[i] = Fake.wire(loss);
        out[wires + 1] = sink;
        return out;
    }

    /** The packet a sink receives falls with the distance by a stated Eu slice per block, whatever the current; the band is unchanged. */
    @GameTest(template = "empty")
    public static void sinkReceivesLessWithDistanceAndTheBandHolds(GameTestHelper helper) {
        float loss = 0.5f, packet = ElectricRules.packetEu(VoltageBand.LV);
        Fake nearSink = Fake.sink(1);
        line(chain(Fake.source(VoltageBand.LV, 2), 2, loss, nearSink)).tick();
        Fake farSink = Fake.sink(1);
        line(chain(Fake.source(VoltageBand.LV, 2), 10, loss, farSink)).tick();
        Fake heavySink = Fake.sink(2);
        line(chain(Fake.source(VoltageBand.LV, 2), 10, loss, heavySink)).tick();

        helper.assertTrue(Math.abs(nearSink.seenEu - (packet - 2 * loss)) < EPS, "2 blocks: expected " + (packet - 2 * loss) + " Eu, saw " + nearSink.seenEu);
        helper.assertTrue(Math.abs(farSink.seenEu - (packet - 10 * loss)) < EPS, "10 blocks: expected " + (packet - 10 * loss) + " Eu, saw " + farSink.seenEu);
        helper.assertTrue(Math.abs(heavySink.seenEu - farSink.seenEu) < EPS, "the slice does not depend on the amps");
        helper.assertTrue(farSink.seenEu < nearSink.seenEu, "more distance must never raise the packet a sink receives");
        helper.assertTrue(farSink.seenBand == VoltageBand.LV, "and the band arrives unchanged");
        helper.assertTrue(Math.abs(farSink.seenAmps - 1f) < EPS, "a covered sink is delivered what it wanted");
        helper.succeed();
    }

    /** A path whose loss reaches the packet delivers zero Eu, and never a negative figure. */
    @GameTest(template = "empty")
    public static void tooLongAWireDeliversZeroNotNegative(GameTestHelper helper) {
        Fake sink = Fake.sink(1);
        line(chain(Fake.source(VoltageBand.LV, 2), 40, 1f, sink)).tick();
        helper.assertTrue(sink.seenEu == 0f, "40 blocks at 1 Eu each take a whole 32 Eu packet, got " + sink.seenEu);
        helper.succeed();
    }

    /** A run with no source reads zero on everything, and a sink still reports what it wanted. */
    @GameTest(template = "empty")
    public static void runWithNoSourceReadsZero(GameTestHelper helper) {
        Fake wire = Fake.wire(0.05f);
        Fake sink = Fake.sink(1);
        ElectricRun run = line(wire, sink);
        wire.seenEu = 9;
        sink.seenEu = 9;
        sink.seenAmps = 9;
        run.tick();
        helper.assertTrue(wire.seenEu == 0 && wire.seenAmps == 0, "wire reads zero with no supply");
        helper.assertTrue(sink.seenEu == 0 && sink.seenAmps == 0 && sink.seenWanted == 1f,
                "sink reads zero delivered, one wanted: " + sink.seenEu + " Eu " + sink.seenAmps + " A "
                        + sink.seenWanted + " wanted");
        helper.succeed();
    }

    /** When supply cannot cover demand every sink gets the same fraction, not nearest-first. */
    @GameTest(template = "empty")
    public static void shortfallIsSharedInProportion(GameTestHelper helper) {
        Fake source = Fake.source(VoltageBand.LV, 1);
        Fake hub = Fake.wire(0.05f);
        Fake a = Fake.sink(2);
        Fake b = Fake.sink(1);
        // source - hub - {a, b}
        ElectricRun run = new ElectricRun(List.of(source, hub, a, b),
                new int[][] {{1}, {0, 2, 3}, {1}, {1}});
        run.tick();
        // limit is 1 A x 2.0; demand 3 A; every sink gets 2/3 of what it wants.
        float share = ElectricTuning.SOURCE_OVERDRAW_LIMIT * 1f / 3f;
        helper.assertTrue(Math.abs(a.seenAmps - 2 * share) < EPS && Math.abs(b.seenAmps - 1 * share) < EPS,
                "expected " + 2 * share + " and " + share + " A, got " + a.seenAmps + " and " + b.seenAmps);
        helper.assertTrue(Math.abs(a.seenAmps / a.seenWanted - b.seenAmps / b.seenWanted) < EPS,
                "both sinks must be short by the same fraction");
        helper.assertTrue(Math.abs(hub.seenAmps - 2f) < EPS, "the shared wire carries what was delivered");
        helper.succeed();
    }

    /** A lower-band source on a run with a higher one is blocked and says so. */
    @GameTest(template = "empty")
    public static void lowerBandSourceIsBlocked(GameTestHelper helper) {
        Fake low = Fake.source(VoltageBand.LV, 1);
        Fake wire = Fake.wire(0f);
        Fake high = Fake.source(VoltageBand.MV, 1);
        Fake sink = Fake.sink(1);
        // low - wire - high, and the sink on the wire
        ElectricRun run = new ElectricRun(List.of(low, wire, high, sink),
                new int[][] {{1}, {0, 2, 3}, {1}, {1}});
        run.tick();
        helper.assertTrue(low.seenBlocked && low.seenAmps == 0f, "the LV source must be blocked");
        helper.assertFalse(high.seenBlocked, "the MV source is not");
        helper.assertTrue(sink.seenBand == VoltageBand.MV && Math.abs(sink.seenEu - ElectricRules.packetEu(VoltageBand.MV)) < EPS, "the sink sees the higher band, not a blend");
        helper.assertTrue(Math.abs(high.seenAmps - 1f) < EPS, "the higher source carries the whole draw");
        helper.succeed();
    }

    /** The tree is cached between ticks and rebuilt only when what is supplying changes. */
    @GameTest(template = "empty")
    public static void routingRebuildsOnlyWhenSupplyChanges(GameTestHelper helper) {
        Fake source = Fake.source(null, 1);   // idle, as a pulse source is between pulses
        Fake sink = Fake.sink(1);
        ElectricRun run = line(source, sink);
        run.tick();
        run.tick();
        run.tick();
        helper.assertTrue(run.rebuilds() == 1, "an idle run is routed once, not per tick: " + run.rebuilds());
        helper.assertTrue(sink.seenEu == 0f, "and an idle source feeds nothing");

        source.band = VoltageBand.LV;
        run.tick();
        helper.assertTrue(run.rebuilds() == 2, "the source going live re-routes once: " + run.rebuilds());
        helper.assertTrue(sink.seenEu > 0f, "the sink is now fed");
        run.tick();
        run.tick();
        helper.assertTrue(run.rebuilds() == 2, "a steady live run is not re-routed: " + run.rebuilds());

        source.band = null;
        run.tick();
        helper.assertTrue(run.rebuilds() == 3 && sink.seenEu == 0f, "the pulse ending re-routes and reads zero");
        helper.succeed();
    }

    // --- the wire ---------------------------------------------------------------------------

    private static CableBlockEntity placeCable(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, FBlocks.CABLE.get().defaultBlockState());
        CableBlockEntity wire = (CableBlockEntity) helper.getBlockEntity(at);
        wire.setMaterial(io.github.soundgoodizerfan.feedback.Feedback.id("copper"));
        wire.setInsulation(io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.UHV);   // coated, as a crafted cable is
        return wire;
    }

    /** Inside its rating a wire is given no heat, so it never leaves ambient and never wears. */
    @GameTest(template = "empty")
    public static void wireInsideItsRatingNeverHeats(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        CableBlockEntity wire = placeCable(helper, at);
        float rating = wire.getRatingAmps();
        helper.assertTrue(Math.abs(rating - ElectricMachineTuning.WIRE_AMPS_PER_AMPACITY) < EPS,
                "copper (ampacity 1) should be rated " + ElectricMachineTuning.WIRE_AMPS_PER_AMPACITY + " A, is " + rating);
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
        float ceiling = ambient + ElectricMachineTuning.WIRE_OVERLOAD_WORK_PER_AMP * over / ElectricMachineTuning.WIRE_LEAK.workPerTickPerTu();
        float highest = 0;
        for (int i = 0; i < 3000; i++) {
            wire.step(wire.getRatingAmps() + over);
            highest = Math.max(highest, wire.getTemperature().value());
        }
        helper.assertTrue(highest > ambient + 10f, "over-rating must warm the wire, peaked at " + highest);
        helper.assertTrue(highest <= ceiling + EPS, "must never exceed " + ceiling + ", reached " + highest);
        // The input lands before the tick's leak, so the discrete settle point sits one tick's
        // worth of input (input / mass) under the continuous ceiling.
        float settles = ceiling - ElectricMachineTuning.WIRE_OVERLOAD_WORK_PER_AMP * over / ElectricMachineTuning.WIRE_MASS.workPerTu();
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
            helper.assertTrue(wb.getPacketEu() == 0f && wb.getAmps() == 0f, "with no source it reads zero");
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

    /** A cell's band follows the difference of its electrodes' potentials, read from the material files. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void cellHoldsItsElectrodePairsBand(GameTestHelper helper) {
        BlockPos cell = new BlockPos(1, 2, 1), cable = cell.east();
        helper.setBlock(cell, FBlocks.GALVANIC_CELL.get().defaultBlockState());
        CableBlockEntity wire = placeCable(helper, cable);
        helper.runAfterDelay(4, () -> {
            // zinc -0.76 against copper +0.34: the difference is LV
            helper.assertTrue(wire.getBand() == VoltageBand.LV && wire.isLive(), "a zinc/copper cell should put an LV packet on the wire");
            helper.assertTrue(wire.getAmps() == 0f, "and nothing flows with nothing to load it");
            GalvanicCellBlockEntity be = (GalvanicCellBlockEntity) helper.getBlockEntity(cell);
            helper.assertTrue(be.offered().voltage() == VoltageBand.LV, "the cell offers the LV band");
            helper.setBlock(cell, Blocks.AIR.defaultBlockState());
            helper.setBlock(cable, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    /**
     * A pulse source is live only while it pulses, and while it does it outbids a lower-band
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
            helper.assertTrue(wire.getBand() == VoltageBand.LV && wire.isLive(), "idle generator: the cell alone holds the line");
            helper.assertFalse(cell.isBlocked(), "and is not blocked");
            int before = wire.getRun().rebuilds();
            generator.turn();
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(wire.getBand() == ElectricMachineTuning.ELECTROSTATIC_BAND,
                        "pulsing: the line is at the generator's band, got " + wire.getBand());
                helper.assertTrue(cell.isBlocked(), "and the lower-band cell is blocked");
                helper.assertTrue(wire.getRun().rebuilds() > before, "the supply changing re-routed the run");
                helper.runAfterDelay(ElectricMachineTuning.ELECTROSTATIC_PULSE_TICKS + 3, () -> {
                    helper.assertFalse(generator.isPulsing(), "the pulse ends");
                    helper.assertTrue(wire.getBand() == VoltageBand.LV && wire.isLive(),
                            "and the line falls back to the cell, got " + wire.getBand());
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
        helper.assertTrue(wire instanceof DataNode, "every wire is a data node");

        CurrentSensorFitting sensor = new CurrentSensorFitting(wire, Direction.UP);
        wire.setSidedFitting(Direction.UP, sensor);
        DataNode found = DataNode.resolve(helper.getLevel(), new DataNodeRef(helper.absolutePos(at), Direction.UP))
                .orElse(null);
        helper.assertTrue(found == sensor, "the sided reference resolves to the fitting on the wire");
        helper.assertTrue(((SensorFitting) found).canRead(Quantity.CURRENT)
                && !((SensorFitting) found).canRead(Quantity.BAND), "it reads current and only current");

        wire.onResolved(VoltageBand.LV, 30f, 2.3f, 0f, false);
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
        helper.assertTrue(electroscope.canRead(Quantity.BAND) && !electroscope.canRead(Quantity.CURRENT),
                "the electroscope reads the band, never current (6.7)");
        helper.assertTrue(galvanometer.canRead(Quantity.CURRENT) && !galvanometer.canRead(Quantity.BAND),
                "the galvanometer reads current");
        helper.assertTrue(electroscope.resolution(Quantity.BAND) > 0 && galvanometer.resolution(Quantity.CURRENT) > 0,
                "and neither is exact (philosophy 10)");
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
     * Source, wire, sink, with loss: the packet a magnet receives is the band's less the stated Eu slice
     * per block, and its field falls with it -- weaker, never refused.
     */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void magnetOnALongerRunReceivesLessAndPullsWeaker(GameTestHelper helper) {
        BlockPos near = new BlockPos(0, 2, 1), far = new BlockPos(0, 2, 4);
        ElectromagnetBlockEntity a = row(helper, near, 2);
        ElectromagnetBlockEntity b = row(helper, far, 8);
        helper.runAfterDelay(6, () -> {
            float loss = 0.05f * ElectricMachineTuning.CABLE_LOSS_EU_PER_DROP;   // copper.json electrical.drop
            float packet = ElectricRules.packetEu(VoltageBand.LV);
            helper.assertTrue(Math.abs(a.getPacketEu() - (packet - 2 * loss)) < 1e-3f,
                    "2 cables: expected " + (packet - 2 * loss) + " Eu, saw " + a.getPacketEu());
            helper.assertTrue(Math.abs(b.getPacketEu() - (packet - 8 * loss)) < 1e-3f,
                    "8 cables: expected " + (packet - 8 * loss) + " Eu, saw " + b.getPacketEu());
            helper.assertTrue(Math.abs(a.getAmps() - 1f) < 1e-3f && Math.abs(b.getAmps() - 1f) < 1e-3f,
                    "both are delivered what they asked for");
            helper.assertTrue(b.getFieldStrength() > 0f && b.getFieldStrength() < a.getFieldStrength(),
                    "the far magnet pulls weaker, not zero: " + b.getFieldStrength() + " vs " + a.getFieldStrength());
            helper.assertTrue(a.getCondition() == 1f && b.getCondition() == 1f, "a smaller packet never wears");
            clearRow(helper, near, 2);
            clearRow(helper, far, 8);
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
            float limit = ElectricTuning.SOURCE_OVERDRAW_LIMIT * ElectricMachineTuning.GALVANIC_CELL_AMPS;
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
        float ceiling = ambient + ElectricMachineTuning.WIRE_OVERLOAD_WORK_PER_AMP * over / ElectricMachineTuning.WIRE_LEAK.workPerTickPerTu();
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
        float excess = ElectricRules.overVoltage(VoltageBand.MV, 1f, VoltageBand.LV);   // fed a band over its rating
        float ceiling = ambient + ElectricMachineTuning.ELECTROMAGNET_HEAT_PER_EXCESS * excess / ElectricMachineTuning.ELECTROMAGNET_LEAK.workPerTickPerTu();
        float peak = 0;
        for (int i = 0; i < 4000; i++) {
            magnet.onResolved(VoltageBand.MV, ElectricRules.packetEu(VoltageBand.MV), 1f, 1f, false);
            peak = Math.max(peak, magnet.getTemperature().value());
        }
        helper.assertTrue(magnet.getCondition() == ElectricMachineTuning.ELECTROMAGNET_CONDITION_FLOOR,
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

    // --- the dynamo -----------------------------------------------------------

    /**
     * A crank turning a shaft into a dynamotor run as a generator (the default figures: small frame, coarse winding),
     * and a magnet on it: it holds the winding's band, the magnet is delivered current, and the Eu a tick drawn, over what a shaft at
     * that speed carries per Su, is charged to the shaft.
     */
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void dynamoHoldsItsBandAndDrawLoadsTheShaft(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        HandCrankBlockEntity crank = RotationGameTests.crankRun(helper, at, 2);
        helper.setBlock(at.east(3), FBlocks.DYNAMOTOR.get().defaultBlockState()
                .setValue(DynamotorBlock.FACING, Direction.WEST));
        ElectromagnetBlockEntity magnet = placeMagnet(helper, at.east(4));
        DynamotorBlockEntity dynamo = (DynamotorBlockEntity) helper.getBlockEntity(at.east(3));

        RotationGameTests.each(helper, crank::turn);
        helper.runAtTickTime(110, () -> {
            float rpm = MachineTuning.HAND_CRANK_RPM.value();
            helper.assertTrue(Math.abs(dynamo.getShaftRpm() - rpm) < 1e-3, "the dynamo is turned at the crank's speed, got " + dynamo.getShaftRpm());
            helper.assertTrue(dynamo.getBand() == ModularTuning.WINDING_BAND[0],
                    "the winding sets the band, held " + dynamo.getBand());
            helper.assertTrue(Math.abs(magnet.getAmps() - ElectricMachineTuning.ELECTROMAGNET_AMPS) < EPS, "the magnet is delivered what it wants");
            float expected = ElectricMachineTuning.ELECTROMAGNET_AMPS * ElectricRules.packetEu(dynamo.getBand()) / (ElectricTuning.EU_PER_SU_RPM * rpm);
            helper.assertTrue(Math.abs(dynamo.getShaftLoad().value() - expected) < 1e-3,
                    "the dynamo loads its shaft with the Eu drawn over what the shaft carries, got " + dynamo.getShaftLoad().value() + ", wanted " + expected);
            for (BlockPos p : BlockPos.betweenClosed(0, 2, 0, 4, 2, 0))
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            RotationGameTests.done(helper);
        });
    }

    /**
     * A crank turns a generator, a cable carries its current to a machine run as a motor, and the motor turns a shaft of its
     * own: the shaft spins, never past the motor's no-load speed (its rated speed at the packet it sees), and the
     * motor's torque is its Su per amp times its current.
     */
    @GameTest(template = "empty", timeoutTicks = 160)
    public static void motorTurnsAShaftFromTheLine(GameTestHelper helper) {
        BlockPos crankAt = new BlockPos(0, 2, 1);
        HandCrankBlockEntity crank = RotationGameTests.crankRun(helper, crankAt, 2);
        helper.setBlock(crankAt.east(3), FBlocks.DYNAMOTOR.get().defaultBlockState()
                .setValue(DynamotorBlock.FACING, Direction.WEST));
        // Up, not along: a long row reaches the neighbouring test's rig.
        BlockPos motorAt = crankAt.east(3).above(2);
        placeCable(helper, crankAt.east(3).above());
        helper.setBlock(motorAt, FBlocks.DYNAMOTOR.get().defaultBlockState()
                .setValue(DynamotorBlock.FACING, Direction.EAST).setValue(DynamotorBlock.MODE, DynamotorBlock.Mode.MOTOR));
        helper.setBlock(motorAt.east(), RotationGameTests.alongX(FBlocks.SHAFT.get()));
        DynamotorBlockEntity motor = (DynamotorBlockEntity) helper.getBlockEntity(motorAt);

        RotationGameTests.each(helper, crank::turn);
        helper.runAtTickTime(140, () -> {
            float rpm = motor.getShaftRpm();
            helper.assertTrue(rpm > 0, "the motor turns its shaft");
            helper.assertTrue(rpm <= motor.noLoadRpm() + EPS, "never past no-load speed " + motor.noLoadRpm() + ", got " + rpm);
            helper.assertTrue(Math.abs(motor.getShaftTorque().value() - motor.getAmps() * motor.getSuPerAmp() * motor.getCondition()) < EPS,
                    "torque is Su-per-amp x amps x condition, got " + motor.getShaftTorque().value() + " at " + motor.getAmps() + " A, condition " + motor.getCondition());
            for (BlockPos p : BlockPos.betweenClosed(0, 2, 1, 4, 4, 1))
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            RotationGameTests.done(helper);
        });
    }

    // --- the Leyden jar -------------------------------------------------------

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
            helper.assertTrue(jar.isSupplying(), "with the pulse gone the jar is the source; held " + jar.getStoredEu() + ", sees " + jar.getPacketEu() + " Eu, " + jar.getAmps() + " A");
            charged[0] = jar.getStoredEu();
            // The jar charges at LEYDEN_JAR_CHARGE_AMPS packets for the length of the pulse, never past what it holds.
            float expected = Math.min(jar.capacity(), ElectricMachineTuning.ELECTROSTATIC_PULSE_TICKS * ElectricMachineTuning.LEYDEN_JAR_CHARGE_AMPS
                    * ElectricRules.packetEu(ElectricMachineTuning.ELECTROSTATIC_BAND));
            helper.assertTrue(charged[0] > 0.7f * expected && charged[0] <= jar.capacity() + EPS,
                    "the jar charged to about " + expected + " Eu and never past its capacity, held " + charged[0]);
            placeMagnet(helper, jarAt.east());
        });
        helper.runAtTickTime(40, () -> {
            ElectromagnetBlockEntity magnet = (ElectromagnetBlockEntity) helper.getBlockEntity(jarAt.east());
            helper.assertTrue(magnet.getAmps() > 0.5f && magnet.getFieldStrength() > 0f,
                    "the magnet runs on the jar, delivered " + magnet.getAmps() + " A");
            helper.assertTrue(jar.getStoredEu() < charged[0], "and the jar empties as it does");
            for (BlockPos p : BlockPos.betweenClosed(generatorAt, jarAt.east()))
                helper.setBlock(p, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    // --- the electrolytic cell ----------------------------------------------

    private static final ResourceLocation COPPER = Feedback.id("copper"), ZINC = Feedback.id("zinc");

    /** A cell with a full tank and {@code brass} brass ingots hung as its anode. */
    private static ElectrolyticCellBlockEntity brassCell(GameTestHelper helper, BlockPos at, int brass) {
        helper.setBlock(at, FBlocks.ELECTROLYTIC_CELL.get().defaultBlockState());
        ElectrolyticCellBlockEntity cell = (ElectrolyticCellBlockEntity) helper.getBlockEntity(at);
        cell.getWater().fill(new FluidStack(Fluids.WATER, ElectricMachineTuning.ELECTROLYTIC_CELL_TANK_MB),
                IFluidHandler.FluidAction.EXECUTE);
        cell.insertAnode(new ItemStack(FItems.BRASS_INGOT.get(), brass));
        return cell;
    }

    /** Ticks until {@code ingots} ingots' worth of metal has gone through at the cell's own current. */
    private static int ticksFor(int ingots) {
        return Math.round(ingots * FTuning.QU_PER_INGOT.value()
                / (ElectricMachineTuning.ELECTROLYTIC_CELL_AMPS * ElectricMachineTuning.QU_PER_PACKET));
    }

    private static void run(ElectrolyticCellBlockEntity cell, VoltageBand band, int ticks) {
        for (int i = 0; i < ticks; i++)
            cell.onResolved(band, ElectricRules.packetEu(band), ElectricMachineTuning.ELECTROLYTIC_CELL_AMPS, ElectricMachineTuning.ELECTROLYTIC_CELL_AMPS, false);
    }

    /**
     * In water, zinc sits too far behind copper to plate with it: brass gives up its copper
     * and keeps its zinc in the water. Left running with the anode gone, on a band under the one
     * that splits water, the cell simply stops: the finished cathode comes off as copper.
     */
    @GameTest(template = "empty")
    public static void cellRefinesCopperOutOfBrassInWater(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        ElectrolyticCellBlockEntity cell = brassCell(helper, at, 2);
        run(cell, VoltageBand.LV, ticksFor(2));
        run(cell, VoltageBand.LV, ticksFor(2));   // overrun, anode long gone
        helper.assertTrue(cell.getAnode().isEmpty(), "the anode dissolved");
        helper.assertFalse(cell.getDeposit().containsKey(ZINC), "no zinc plated: " + cell.getDeposit());
        // Brass is 65 % copper, 35 % zinc by Qu, and two ingots are 18 Qu: 11.7 and 6.3.
        helper.assertTrue(Math.abs(cell.getDeposit().getOrDefault(COPPER, 0f) - 11.7f) < 0.05f,
                "the copper share of two ingots plated: " + cell.getDeposit());
        helper.assertTrue(Math.abs(cell.getSolution().getOrDefault(ZINC, 0f) - 6.3f) < 0.05f,
                "and the zinc stayed in the water: " + cell.getSolution());
        List<ItemStack> off = cell.takeCathode();
        helper.assertTrue(off.size() == 1 && off.get(0).is(Items.COPPER_INGOT) && off.get(0).getCount() == 1,
                "one copper ingot off the cathode, the part-ingot left on: " + off);
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /**
     * The same refined cathode, overrun on a band that splits water: with the anode gone the
     * current strips the water, the zinc that waited behind the copper plates over it, and the
     * finished deposit comes off as scrap.
     */
    @GameTest(template = "empty")
    public static void overrunOnABandThatSplitsWaterPlatesZincOverTheFinishedCopper(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        ElectrolyticCellBlockEntity cell = brassCell(helper, at, 2);
        run(cell, VoltageBand.LV, ticksFor(2));
        run(cell, VoltageBand.MV, ticksFor(1));
        helper.assertTrue(cell.getDeposit().getOrDefault(ZINC, 0f) > 6.2f, "the zinc plated: " + cell.getDeposit());
        List<ItemStack> off = cell.takeCathode();
        helper.assertTrue(off.size() == 1 && off.get(0).is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Feedback.id("copper_scrap"))) && off.get(0).getCount() == 2,
                "the whole deposit is copper scrap now: " + off);
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    // --- electroplating ------------------------------------------------------

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
        cell.getWater().fill(new FluidStack(Fluids.WATER, ElectricMachineTuning.ELECTROLYTIC_CELL_TANK_MB),
                IFluidHandler.FluidAction.EXECUTE);
        cell.insertAnode(new ItemStack(Items.COPPER_INGOT));
        cell.hangWork(new ItemStack(Items.IRON_INGOT));
        int ticks = Math.round(qu / (amps * ElectricMachineTuning.QU_PER_PACKET));
        for (int i = 0; i < ticks; i++)
            cell.onResolved(VoltageBand.LV, ElectricRules.packetEu(VoltageBand.LV), amps, amps, false);
        return cell.takeWork();
    }

    // --- the heating coil ----------------------------------------------------

    /**
     * Two coils at 9 V and their own two amps, each under a small crucible. Both heat their crucible
     * and run a little above it -- the element is the fire, and what the crucible takes the
     * element gives. The copper element is past its metal's service temperature and wears; the
     * iron one, at the same power, is not.
     */
    @GameTest(template = "empty", timeoutTicks = 1300)
    public static void coilHeatsTheVesselAboveAndWearsOnlyPastItsElement(GameTestHelper helper) {
        BlockPos copperAt = new BlockPos(0, 2, 0), ironAt = new BlockPos(2, 2, 0);
        helper.setBlock(copperAt, FBlocks.COPPER_COIL.get().defaultBlockState());
        helper.setBlock(ironAt, FBlocks.IRON_COIL.get().defaultBlockState());
        helper.setBlock(copperAt.above(), FBlocks.SMALL_CRUCIBLE.get().defaultBlockState());
        helper.setBlock(ironAt.above(), FBlocks.SMALL_CRUCIBLE.get().defaultBlockState());
        CoilBlockEntity copper = (CoilBlockEntity) helper.getBlockEntity(copperAt);
        CoilBlockEntity iron = (CoilBlockEntity) helper.getBlockEntity(ironAt);
        ThermalBody pot = (ThermalBody) helper.getBlockEntity(copperAt.above());

        helper.onEachTick(() -> {
            copper.onResolved(VoltageBand.MV, ElectricRules.packetEu(VoltageBand.MV), ElectricMachineTuning.COIL_AMPS, ElectricMachineTuning.COIL_AMPS, false);
            iron.onResolved(VoltageBand.MV, ElectricRules.packetEu(VoltageBand.MV), ElectricMachineTuning.COIL_AMPS, ElectricMachineTuning.COIL_AMPS, false);
        });
        helper.runAtTickTime(1200, () -> {
            float vessel = pot.getTemperature().value(), element = copper.getTemperature().value();
            helper.assertTrue(vessel > 300f, "the crucible is heated well past ambient, got " + vessel);
            helper.assertTrue(element > vessel && element - vessel < 60f,
                    "the element runs a little above its load: element " + element + ", vessel " + vessel);
            helper.assertTrue(copper.getCondition() < 1f, "copper past its service temperature wears");
            helper.assertTrue(iron.getCondition() == 1f, "iron at the same power does not, got " + iron.getCondition());
            for (BlockPos at : BlockPos.betweenClosed(0, 2, 0, 2, 3, 0))
                helper.setBlock(at, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    // --- the breaker and the fuse ---------------------------------------------

    /**
     * Cell, breaker, magnet in a column. Closed, the magnet is fed; opened under load, the line is
     * two runs and the magnet gets nothing, and the arc has cost the breaker condition. Closed
     * again, it feeds again.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void breakerOpensTheLineAndArcsUnderLoad(GameTestHelper helper) {
        BlockPos from = new BlockPos(1, 2, 1);
        ElectromagnetBlockEntity magnet = row(helper, from, 1);
        helper.setBlock(from.above(), FBlocks.BREAKER.get().defaultBlockState());
        BreakerBlockEntity breaker = (BreakerBlockEntity) helper.getBlockEntity(from.above());
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(magnet.getAmps() > 0f, "closed, the magnet is fed");
            breaker.setEngaged(false);
            helper.assertTrue(breaker.getCondition() < 1f, "opening under load arcs, got " + breaker.getCondition());
        });
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(magnet.getAmps() == 0f, "open, the magnet gets nothing, got " + magnet.getAmps());
            breaker.setEngaged(true);
        });
        helper.runAtTickTime(15, () -> {
            helper.assertTrue(magnet.getAmps() > 0f, "closed again, it is fed again");
            clearRow(helper, from, 1);
            helper.succeed();
        });
    }

    /**
     * A tin fuse wire carried at its rating stays; past it, it warms to tin's melting point and is
     * gone, and the holder conducts nothing until it is rewired.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fuseWireMeltsPastItsRating(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, FBlocks.FUSE.get().defaultBlockState());
        FuseBlockEntity fuse = (FuseBlockEntity) helper.getBlockEntity(at);
        ResourceLocation tin = Feedback.id("tin");
        Item wire = MaterialTable.get().materials().get(tin).item(tin, "fine_wire").orElseThrow();
        helper.assertTrue(fuse.rewire(new ItemStack(wire)), "a tin fine wire fits");
        float rating = fuse.getRatingAmps();
        for (int i = 0; i < 100; i++)
            fuse.onResolved(VoltageBand.LV, 32f, rating, 0f, false);
        helper.assertTrue(fuse.isIntact(), "at its rating it holds");
        int ticks = 0;
        while (fuse.isIntact() && ticks < 100) {
            fuse.onResolved(VoltageBand.LV, 32f, rating + 2f, 0f, false);
            ticks++;
        }
        helper.assertFalse(fuse.isIntact(), "two amps over, tin melts");
        helper.assertTrue(ticks < 60, "and quickly, took " + ticks + " t");
        helper.setBlock(at, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    // --- the lead-acid battery ---------------------------------------------------------------

    /** Charging inside its means must not wear it, cook it or boil it dry. */
    private static void assertBatteryHealthy(GameTestHelper helper, io.github.soundgoodizerfan.feedback.machine.battery.BatteryBlockEntity battery) {
        helper.assertTrue(battery.getCondition() > 0.999f, "condition held, now " + battery.getCondition());
        helper.assertTrue(battery.getTemperature().value() < ElectricMachineTuning.BATTERY_HOT_TU, "case stayed under the hot line, now " + battery.getTemperature().value() + " Tu");
        helper.assertTrue(battery.water() > FTuning.BATTERY_WATER_TANK_MB - 1f, "no water vented, now " + battery.water() + " mB");
        helper.assertTrue(battery.gas() < 0.5f, "no hydrogen released, now " + battery.gas() + " mB");
    }

    /** A battery of {@code cells} cells, acid filled, charge set to {@code share} of capacity, with a creative source of {@code line} beside it by a cable. */
    private static io.github.soundgoodizerfan.feedback.machine.battery.BatteryBlockEntity chargingBattery(GameTestHelper helper, int cells, float share, VoltageBand line) {
        BlockPos sourceAt = new BlockPos(1, 2, 1), batteryAt = sourceAt.east(2);
        helper.setBlock(sourceAt, FBlocks.CREATIVE_ELECTRIC.get().defaultBlockState());
        ((io.github.soundgoodizerfan.feedback.machine.creative.CreativeElectricBlockEntity) helper.getBlockEntity(sourceAt))
                .configure(true, new float[] {line.ordinal(), 1000f});
        placeCable(helper, sourceAt.east());
        helper.setBlock(batteryAt, FBlocks.BATTERY.get().defaultBlockState());
        var battery = (io.github.soundgoodizerfan.feedback.machine.battery.BatteryBlockEntity) helper.getBlockEntity(batteryAt);
        var registries = helper.getLevel().registryAccess();
        net.minecraft.nbt.CompoundTag tag = battery.saveWithoutMetadata(registries);
        float capacity = tag.getFloat("Capacity");
        tag.putInt("Cells", cells);
        tag.putFloat("Acid", cells * FTuning.BATTERY_ACID_MB_PER_CELL);
        tag.putFloat("Water", FTuning.BATTERY_WATER_TANK_MB);
        tag.putFloat("Charge", capacity * share);
        tag.putBoolean("Supplying", false);
        tag.putString("CaseBand", VoltageBand.MV.getSerializedName());   // a built case has an insulation band; the default LV would count every line here as overvoltage
        battery.loadCustomOnly(tag, registries);
        return battery;
    }

    /** A battery rated MV on an LV line: a band under its rating adds no charge and costs no condition. */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void batteryOnALineBelowItsBandDoesNotCharge(GameTestHelper helper) {
        var battery = chargingBattery(helper, 1, 0.5f, VoltageBand.LV);
        helper.runAtTickTime(300, () -> {
            helper.assertTrue(battery.stateOfCharge() < 0.5001f, "a band under the rating adds no charge; soc " + battery.stateOfCharge());
            helper.assertTrue(battery.getCondition() > 0.999f, "and costs no condition, now " + battery.getCondition());
            helper.succeed();
        });
    }

    /** A battery rated MV on an MV line charges, and wears nothing doing it. */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void batteryOnALineAtItsBandCharges(GameTestHelper helper) {
        var battery = chargingBattery(helper, 3, 0.5f, VoltageBand.MV);
        helper.runAtTickTime(300, () -> {
            helper.assertTrue(battery.stateOfCharge() > 0.5001f, "its own band charges it; soc " + battery.stateOfCharge());
            helper.assertTrue(battery.getCondition() > 0.999f, "and costs no condition, now " + battery.getCondition());
            helper.succeed();
        });
    }

}
