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
 * Engine Stand v1's figures ({@code feedback_vehicles_spec.md} section 6): the trades the spec
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
        helper.assertTrue(square.redline(copper).value() < square.redline(steel).value(), "copper redlines below steel");
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
}
