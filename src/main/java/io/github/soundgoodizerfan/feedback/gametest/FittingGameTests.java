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
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.RefractoryLiningFitting;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** An upgrade changes its holder's own numbers, and never lets it past its flame. */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class FittingGameTests {

    private static final Tu FIRE = new Tu(1500f);
    private static final int TICKS = 6000;

    /**
     * A lined crucible is slower to climb and still settles at {@code Heat.equilibrium} --
     * strictly below the fire, the same figure the bare one reaches. Mass is the only thing the
     * lining touched.
     */
    @GameTest(template = "empty")
    public static void refractoryLiningSlowsTheClimbButNeverPassesTheFlame(GameTestHelper helper) {
        BlockPos bare = new BlockPos(1, 1, 1);
        BlockPos lined = new BlockPos(3, 1, 1);
        helper.setBlock(bare, FBlocks.SMALL_CRUCIBLE.get());
        helper.setBlock(lined, FBlocks.SMALL_CRUCIBLE.get());
        CrucibleBlockEntity plain = (CrucibleBlockEntity) helper.getBlockEntity(bare);
        CrucibleBlockEntity lining = (CrucibleBlockEntity) helper.getBlockEntity(lined);

        helper.assertTrue(lining.canAdd(new RefractoryLiningFitting()), "a crucible accepts a lining");
        lining.addUpgrade(new RefractoryLiningFitting());
        helper.assertTrue(!lining.canAdd(new RefractoryLiningFitting()), "but only one");
        helper.assertTrue(lining.getThermalMass().workPerTu()
                        == plain.getThermalMass().workPerTu() + FTuning.REFRACTORY_LINING_MASS.workPerTu(),
                "the lining adds exactly its mass");

        Tu equilibrium = Heat.equilibrium(lining, FIRE, FTuning.AMBIENT_TU);
        helper.assertTrue(equilibrium.value() < FIRE.value(), "equilibrium is under the flame");

        for (int i = 0; i < 200; i++) {
            Heat.tick(plain, FIRE, FTuning.AMBIENT_TU, true);
            Heat.tick(lining, FIRE, FTuning.AMBIENT_TU, true);
        }
        helper.assertTrue(lining.getTemperature().value() < plain.getTemperature().value(),
                "after the same ticks the lined crucible is cooler -- it climbs slower");

        for (int i = 0; i < TICKS; i++) {
            Heat.tick(plain, FIRE, FTuning.AMBIENT_TU, true);
            Heat.tick(lining, FIRE, FTuning.AMBIENT_TU, true);
            helper.assertTrue(lining.getTemperature().value() < FIRE.value(),
                    "the lined crucible never reaches the flame, tick " + i);
        }
        helper.assertTrue(Math.abs(lining.getTemperature().value() - equilibrium.value()) < 0.5f,
                "it settles at equilibrium, got " + lining.getTemperature().value());
        helper.assertTrue(Math.abs(lining.getTemperature().value() - plain.getTemperature().value()) < 0.5f,
                "the same place the bare one does");
        helper.succeed();
    }
}
