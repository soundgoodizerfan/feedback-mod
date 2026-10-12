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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.gametest;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Barrel;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Wall;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Spec step 1 ({@code feedback_guns_spec.md} section 11): a gun's figures come out of its design
 * and its metal with no row anywhere, the trades the spec promises hold, and a shot that leaves
 * the server's muzzle lands on what is in front of it.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class GunGameTests {

    private static GunFigures bake(GameTestHelper helper, GunDesign design) {
        var materials = MaterialTable.get().materials();
        Material metal = materials.get(design.material());
        Material lead = materials.get(Feedback.id("lead"));
        helper.assertTrue(metal != null && lead != null, "the gun's metal and lead are materials");
        return design.bake(metal, lead).orElseThrow(() -> new AssertionError("buildable: " + design));
    }

    @GameTest(template = "empty")
    public static void figuresComeOutOfTheDesignAndTheMetal(GameTestHelper helper) {
        GunDesign base = GunDesign.starter(Feedback.id("steel"));
        GunFigures steel = bake(helper, base);

        float speed = steel.muzzleSpeed() * 20;
        helper.assertTrue(speed > 150 && speed < 500, "a 9 mm medium barrel is a pistol's speed, got " + speed + " m/s");
        helper.assertTrue(steel.overpressure() < 1, "steel holds black powder, got " + steel.overpressure());

        // The metal sets the rating, and so whether the same charge is safe.
        GunFigures lead = bake(helper, base.withMaterial(Feedback.id("lead")));
        helper.assertTrue(lead.chamberRating().value() < steel.chamberRating().value(), "lead holds less than steel");
        helper.assertTrue(lead.overpressure() > 1, "a lead chamber is over its rating, got " + lead.overpressure());

        // The wall sets it too, and costs weight.
        GunFigures thin = bake(helper, new GunDesign(9, Barrel.MEDIUM, Wall.THIN, Feedback.id("steel")));
        GunFigures thick = bake(helper, new GunDesign(9, Barrel.MEDIUM, Wall.THICK, Feedback.id("steel")));
        helper.assertTrue(thin.chamberRating().value() < thick.chamberRating().value(), "thicker wall, higher rating");
        helper.assertTrue(thin.gunMass().value() < thick.gunMass().value(), "thicker wall, heavier gun");
        helper.assertTrue(thin.muzzleSpeed() == thick.muzzleSpeed(), "the wall does not change the barrel's push");

        // Neither barrel length dominates: speed and spread, against weight.
        GunFigures shortBarrel = bake(helper, new GunDesign(9, Barrel.SHORT, Wall.STANDARD, Feedback.id("steel")));
        GunFigures longBarrel = bake(helper, new GunDesign(9, Barrel.LONG, Wall.STANDARD, Feedback.id("steel")));
        helper.assertTrue(longBarrel.muzzleSpeed() > shortBarrel.muzzleSpeed(), "a longer barrel is faster");
        helper.assertTrue(longBarrel.spreadDeg() < shortBarrel.spreadDeg(), "and tighter");
        helper.assertTrue(longBarrel.gunMass().value() > shortBarrel.gunMass().value(), "and heavier");
        helper.assertTrue(longBarrel.impulse() > shortBarrel.impulse(), "and kicks more");

        // A bigger bore is more energy and more kick.
        GunFigures wide = bake(helper, new GunDesign(14, Barrel.MEDIUM, Wall.STANDARD, Feedback.id("steel")));
        helper.assertTrue(wide.muzzleEnergy() > steel.muzzleEnergy(), "a bigger bore delivers more work");
        helper.assertTrue(wide.kickDeg() > steel.kickDeg(), "and kicks harder");

        // The noise floor never reaches zero.
        helper.assertTrue(longBarrel.spreadDeg() > 0, "spread has a floor");
        helper.assertTrue(new GunDesign(99, Barrel.MEDIUM, Wall.STANDARD, Feedback.id("steel")).boreMm() == GunDesign.MAX_BORE_MM,
                "an out-of-range bore is pulled back in");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCycleIsWallClockAndToleratesJitter(GameTestHelper helper) {
        GunFigures f = bake(helper, GunDesign.starter(Feedback.id("steel")));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        long t = 1_000_000L;
        helper.assertTrue(GunShots.accept(player, f, t), "the first shot is accepted");
        helper.assertFalse(GunShots.accept(player, f, t + 10), "ten ms later is far inside the cycle");
        helper.assertTrue(GunShots.accept(player, f, t + f.cycleMs()), "one cycle later is accepted");
        helper.succeed();
    }

    /** A shot leaves a muzzle and lands on the zombie in front of it, a few ticks later. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aShotHitsWhatIsInFront(GameTestHelper helper) {
        GunFigures f = bake(helper, GunDesign.starter(Feedback.id("steel")));
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 1.0, 3.5));
        player.setPos(feet.x, feet.y, feet.z);

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 8));
        zombie.setNoAi(true);
        zombie.setSilent(true);
        float before = zombie.getHealth();

        // Yaw 0 is +z. Both stand outside the test's barrier shell, which a ray from inside would hit.
        // The zombie is five blocks along, and its body spans the eye height.
        GunShots.fire(level, player, f, 0f, 0f);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(zombie.getHealth() < before, "the zombie took damage, health " + zombie.getHealth() + " of " + before);
            helper.succeed();
        });
    }
}
