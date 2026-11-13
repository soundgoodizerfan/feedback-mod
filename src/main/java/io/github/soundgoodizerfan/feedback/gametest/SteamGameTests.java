// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.gametest;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.boiler.BoilerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.condenser.CondenserBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.steamengine.SteamEngineBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.valve.ValveBlock;
import io.github.soundgoodizerfan.feedback.machine.valve.ValveBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Steam on a line: the valve is the same block that gates an air line, gating a fluid one. And
 * the way back: an engine's exhaust into a condenser, which makes water until its own heat stops it.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class SteamGameTests {

    /**
     * A boiler, a valve facing the engine, and an engine. Closed, the engine's pull reaches nothing
     * and the boiler's steam does not move; thrown open, steam crosses; closed again, it stops.
     * Neither end was edited for the valve being there.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void valveGatesASteamLine(GameTestHelper helper) {
        BlockPos boilerAt = new BlockPos(1, 2, 1), valveAt = boilerAt.east(), engineAt = valveAt.east();
        helper.setBlock(boilerAt, FBlocks.BOILER.get());
        helper.setBlock(valveAt, FBlocks.VALVE.get().defaultBlockState()
                .setValue(ValveBlock.FACING, Direction.EAST).setValue(ValveBlock.OPEN, false));
        helper.setBlock(engineAt, FBlocks.STEAM_ENGINE.get());
        BoilerBlockEntity boiler = (BoilerBlockEntity) helper.getBlockEntity(boilerAt);
        ValveBlockEntity valve = (ValveBlockEntity) helper.getBlockEntity(valveAt);
        SteamEngineBlockEntity engine = (SteamEngineBlockEntity) helper.getBlockEntity(engineAt);
        boiler.getSteamTank().fill(io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat.stamp(new FluidStack(FFluids.STEAM.get(), 1000), 150f), IFluidHandler.FluidAction.EXECUTE);

        int[] seen = new int[2];
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(boiler.getSteamTank().getFluidAmount() == 1000, "a closed valve passes nothing");
            helper.assertTrue(engine.getTank().getFluidAmount() == 0, "the engine is starved behind it");
            helper.assertTrue(valve.getFluidLine(Direction.NORTH) == null, "its sides are walls");
            valve.setEngaged(true);
        });
        helper.runAtTickTime(40, () -> {
            seen[0] = boiler.getSteamTank().getFluidAmount();
            helper.assertTrue(seen[0] < 1000, "open, steam crosses to the engine, boiler holds " + seen[0]);
            valve.setEngaged(false);
        });
        helper.runAtTickTime(45, () -> seen[1] = boiler.getSteamTank().getFluidAmount());
        helper.runAtTickTime(70, () -> {
            helper.assertTrue(boiler.getSteamTank().getFluidAmount() == seen[1], "closed again, it stops");
            helper.succeed();
        });
    }

    /**
     * An engine run on a tank of steam exhausts spent steam; a condenser beside it pulls that and
     * makes water, and the latent heat warms it toward boiling but never past it.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void condenserTurnsExhaustBackIntoWater(GameTestHelper helper) {
        BlockPos engineAt = new BlockPos(1, 2, 1), condenserAt = engineAt.east();
        helper.setBlock(engineAt, FBlocks.STEAM_ENGINE.get());
        helper.setBlock(condenserAt, FBlocks.CONDENSER.get());
        SteamEngineBlockEntity engine = (SteamEngineBlockEntity) helper.getBlockEntity(engineAt);
        CondenserBlockEntity condenser = (CondenserBlockEntity) helper.getBlockEntity(condenserAt);
        engine.getTank().fill(io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat.stamp(new FluidStack(FFluids.STEAM.get(), VesselTuning.STEAM_ENGINE_TANK_MB), 150f), IFluidHandler.FluidAction.EXECUTE);

        helper.runAtTickTime(60, () -> {
            int water = condenser.getWaterTank().getFluidAmount();
            float tu = condenser.getTemperature().value();
            helper.assertTrue(water > 0, "the exhaust came back as water");
            helper.assertTrue(engine.getTank().isEmpty(), "the engine burned its steam");
            helper.assertTrue(tu > ThermalTuning.AMBIENT_TU.value(), "condensing warmed the condenser, it is at " + tu);
            helper.assertTrue(tu <= MachineTuning.BOILER_WORKING_TU.value() + 1e-3f, "but never past boiling, it is at " + tu);
            helper.succeed();
        });
    }
}
