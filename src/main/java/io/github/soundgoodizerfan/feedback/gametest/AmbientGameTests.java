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
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A warmer room pulls a vessel nearer its flame and never past it. */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class AmbientGameTests {

    private static final class Body implements ThermalBody {
        private Tu temperature = FTuning.AMBIENT_TU;

        @Override
        public Tu getTemperature() {
            return temperature;
        }

        @Override
        public void setTemperature(Tu tu) {
            temperature = tu;
        }

        @Override
        public ThermalMass getThermalMass() {
            return new ThermalMass(200f);
        }
    }

    @GameTest(template = "empty")
    public static void netherRoomIsWarmerAndOverworldIsTheDefault(GameTestHelper helper) {
        helper.assertTrue(AmbientTable.of(Level.OVERWORLD.location()).equals(FTuning.AMBIENT_TU),
                "overworld reads the default");
        helper.assertTrue(AmbientTable.of(Level.NETHER.location()).value() > FTuning.AMBIENT_TU.value(),
                "the nether is listed warmer than the default");
        helper.assertTrue(AmbientTable.of(ResourceLocation.parse("nowhere:void")).equals(FTuning.AMBIENT_TU),
                "an unlisted dimension reads the default");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void warmerRoomNarrowsTheGapButNeverPassesTheFlame(GameTestHelper helper) {
        Tu fire = new Tu(1200f);
        Tu cool = AmbientTable.of(Level.OVERWORLD.location());
        Tu warm = AmbientTable.of(Level.NETHER.location());
        Body home = new Body();
        Body nether = new Body();
        for (int i = 0; i < 20000; i++) {
            Heat.tick(home, fire, cool, true);
            Heat.tick(nether, fire, warm, true);
            helper.assertTrue(nether.getTemperature().value() <= fire.value(), "never past the flame");
        }
        helper.assertTrue(nether.getTemperature().value() > home.getTemperature().value(),
                "the warmer room sits nearer the flame");
        helper.assertTrue(Math.abs(nether.getTemperature().value() - Heat.equilibrium(nether, fire, warm).value()) < 0.5f,
                "simulation settles on equilibrium");
        helper.assertTrue(Heat.equilibrium(nether, fire, warm).value() < fire.value(),
                "equilibrium is under the flame");
        helper.succeed();
    }
}
