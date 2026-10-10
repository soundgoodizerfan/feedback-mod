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
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleCoreBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Sable smoke test, {@code feedback_vehicles_spec.md} section 6 step 2: a core with a block
 * stuck to it assembles into one Sable body, and the core is inside that body afterwards.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class VehicleGameTests {

    @GameTest(template = "empty")
    public static void coreAssemblesIntoASableBody(GameTestHelper helper) {
        BlockPos core = new BlockPos(0, 4, 0); // clear of the floor, or the floodfill takes the world
        helper.setBlock(core, FVehicles.VEHICLE_CORE.get());
        helper.setBlock(core.east(), Blocks.IRON_BLOCK);
        BlockPos absolute = helper.absolutePos(core);

        var body = VehicleCoreBlock.assemble(helper.getLevel(), absolute);
        helper.assertTrue(body != null, "core and one iron block should assemble into a body");
        helper.succeed();
    }
}
