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
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Engine Stand v1's figures ({@code docs/specs/feedback_vehicles_spec.md} section 6): the trades the spec
 * promises come out of four settings and the material's own figures.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class EngineGameTests {

    @GameTest(template = "empty")
    public static void designTradesComeOutOfTheSettings(GameTestHelper helper) {
        Material steel = MaterialTable.get().materials().get(Feedback.id("steel"));
        Material copper = MaterialTable.get().materials().get(Feedback.id("copper"));
        helper.assertTrue(steel != null && copper != null, "steel and copper are materials");

        EngineDesign square = new EngineDesign(4, 80, 80, Feedback.id("steel"));
        helper.assertTrue(Math.abs(square.displacementMb() - 1.608f) < 0.01f, "4 x 80 x 80 is about 1.6 L, got " + square.displacementMb());

        EngineDesign shortStroke = new EngineDesign(4, 80, 60, Feedback.id("steel"));
        helper.assertTrue(shortStroke.redline(steel).value() > square.redline(steel).value(), "a short stroke revs higher");
        helper.assertTrue(new EngineDesign(8, 80, 80, Feedback.id("steel")).peakTorque().value() > square.peakTorque().value(),
                "more displacement, more torque");
        float steelRedline = square.redline(steel).value(), copperRedline = square.redline(copper).value();
        helper.assertTrue(steelRedline > 6000 && steelRedline < 8000, "a steel 4 x 80 x 80 redlines 6000-8000 RPM, got " + steelRedline);
        helper.assertTrue(copperRedline < 0.6f * steelRedline, "copper's lower yield strength redlines well below steel, got " + copperRedline);
        helper.assertTrue(square.mass(copper).value() > square.mass(steel).value(), "copper is denser, so heavier");

        Rpm peak = square.peakRpm(steel);
        helper.assertTrue(Math.abs(square.torqueAt(peak, steel).value() - square.peakTorque().value()) < 1e-3,
                "full torque at the peak");
        helper.assertTrue(square.torqueAt(new Rpm(peak.value() * 0.3f), steel).value() < square.peakTorque().value(),
                "less low down");
        helper.assertTrue(new EngineDesign(99, 7, 333, Feedback.id("steel")).equals(new EngineDesign(8, 40, 120, Feedback.id("steel"))),
                "out-of-range settings are pulled back in");
        helper.succeed();
    }

    /**
     * An engine on a lifted vehicle puts its design's mass on it, converted to Sable's unit
     * (1 kpg = 1000 Mu), block and box together. Alone on the lift on purpose: a vehicle that is
     * only an engine must not weigh zero when lifted, or Sable discards it.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void engineCarriesItsMassOntoTheVehicle(GameTestHelper helper) {
        net.minecraft.core.BlockPos lift = new net.minecraft.core.BlockPos(0, 4, 0);
        helper.setBlock(lift, io.github.soundgoodizerfan.feedback.registry.FVehicles.LIFT.get());
        helper.setBlock(lift.above(), io.github.soundgoodizerfan.feedback.registry.FVehicles.ENGINE.get());
        EngineDesign design = new EngineDesign(4, 80, 80, Feedback.id("steel"));
        var item = new net.minecraft.world.item.ItemStack(io.github.soundgoodizerfan.feedback.registry.FVehicles.ENGINE_ITEM.get());
        item.set(io.github.soundgoodizerfan.feedback.registry.FVehicles.ENGINE_DESIGN.get(), design);
        helper.getBlockEntity(lift.above()).applyComponentsFromItemStack(item);
        var liftBe = (io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity) helper.getBlockEntity(lift);
        helper.assertTrue(liftBe.lift(helper.getLevel()).equals("lifted"), "the engine should lift");
        var body = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.getContainer(helper.getLevel()).getSubLevel(liftBe.body());
        var plot = body.getPlot().getBoundingBox();
        double expected = io.github.soundgoodizerfan.feedback.core.unit.Units.kpg(
                design.mass(MaterialTable.get().materials().get(Feedback.id("steel"))));
        helper.runAfterDelay(10, () -> {
            double carried = 0;
            for (var p : net.minecraft.core.BlockPos.betweenClosed(plot.minX(), plot.minY(), plot.minZ(), plot.maxX(), plot.maxY(), plot.maxZ()))
                if (helper.getLevel().getBlockEntity(p) instanceof io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlockEntity engine)
                    carried += engine.carriedKpg();
            helper.assertTrue(Math.abs(carried - expected) < 1e-3, "the engine should carry " + expected + " kpg, carries " + carried);
            helper.succeed();
        });
    }

    /**
     * An Engine is a rotation source (spec section 6): off when placed; switched on it turns the
     * shaft it is coupled to, its capacity the torque curve at the run's speed; switched off it
     * supplies nothing and the run coasts down.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void engineDrivesAShaftAlongItsTorqueCurve(GameTestHelper helper) {
        net.minecraft.core.BlockPos at = new net.minecraft.core.BlockPos(1, 2, 1);
        helper.setBlock(at, io.github.soundgoodizerfan.feedback.registry.FVehicles.ENGINE.get().defaultBlockState()
                .setValue(io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlock.FACING, net.minecraft.core.Direction.EAST));
        helper.setBlock(at.east(), io.github.soundgoodizerfan.feedback.registry.FBlocks.SHAFT.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.X));
        EngineDesign design = new EngineDesign(4, 80, 80, Feedback.id("steel"));
        Material steel = MaterialTable.get().materials().get(Feedback.id("steel"));
        var item = new net.minecraft.world.item.ItemStack(io.github.soundgoodizerfan.feedback.registry.FVehicles.ENGINE_ITEM.get());
        item.set(io.github.soundgoodizerfan.feedback.registry.FVehicles.ENGINE_DESIGN.get(), design);
        var engine = (io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlockEntity) helper.getBlockEntity(at);
        engine.applyComponentsFromItemStack(item);
        var shaft = (io.github.soundgoodizerfan.feedback.core.rotation.RotationNode) helper.getBlockEntity(at.east());

        helper.runAfterDelay(5, () -> {
            helper.assertFalse(engine.isEngaged(), "an engine is off when placed");
            helper.assertTrue(shaft.getRpm().value() == 0, "an engine that is off turns nothing");
            engine.getFuel().fill(new net.neoforged.neoforge.fluids.FluidStack(io.github.soundgoodizerfan.feedback.registry.FFluids.DIESEL.get(), 2000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            engine.setLevel(0, 1f); // the throttle rests shut, at idle
            engine.setEngaged(true);
        });
        helper.runAfterDelay(65, () -> {
            float rpm = Math.abs(shaft.getRpm().value());
            helper.assertTrue(rpm > 0, "the running engine turns the shaft");
            helper.assertTrue(shaft.getNetwork() == engine.getNetwork(), "engine and shaft are one run");
            float capacity = engine.getNetwork().getCapacitySu().value();
            // The curve is scaled by the fuel's share of the reference flame, and by wear.
            float share = engine.getFuel().flame() / io.github.soundgoodizerfan.feedback.core.FTuning.ENGINE_REFERENCE_FLAME_TU * engine.condition();
            float curve = design.torqueAt(new Rpm(rpm), steel).value() * share;
            helper.assertTrue(Math.abs(capacity - curve) < 3,
                    "capacity " + capacity + " should be the torque curve x fuel share " + share + " at " + rpm + " RPM, " + curve);
            helper.assertTrue(capacity < design.peakTorque().value() && capacity > 0.3f * design.peakTorque().value() * share,
                    "below the peak RPM the capacity is part of the peak, got " + capacity);
            engine.setEngaged(false);
            helper.assertTrue(engine.getNetwork().getCapacitySu().value() == 0, "switched off it supplies nothing");
            float before = rpm;
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(Math.abs(shaft.getRpm().value()) < before, "switched off the run coasts down");
                helper.succeed();
            });
        });
    }

    private static net.minecraft.world.item.crafting.CraftingInput grid(net.minecraft.world.item.ItemStack... stacks) {
        var list = new java.util.ArrayList<net.minecraft.world.item.ItemStack>(java.util.Arrays.asList(stacks));
        while (list.size() < 9)
            list.add(net.minecraft.world.item.ItemStack.EMPTY);
        return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, list);
    }
}
