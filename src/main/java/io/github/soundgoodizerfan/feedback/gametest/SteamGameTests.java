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
import io.github.soundgoodizerfan.feedback.machine.boiler.BoilerBlockEntity;
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

/** Steam on a line: the valve is the same block that gates an air line, gating a fluid one. */
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
        boiler.getSteamTank().fill(new FluidStack(FFluids.STEAM.get(), 1000), IFluidHandler.FluidAction.EXECUTE);

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
}
