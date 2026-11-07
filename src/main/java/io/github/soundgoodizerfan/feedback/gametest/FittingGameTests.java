// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
