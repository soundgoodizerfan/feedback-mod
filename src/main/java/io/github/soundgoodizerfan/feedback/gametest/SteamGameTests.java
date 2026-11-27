// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.gametest;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.condenser.CondenserBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.steamengine.SteamEngineBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * An engine's exhaust into a condenser, which makes water until its own heat stops it.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class SteamGameTests {

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
