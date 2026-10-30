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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunCalibres;
import io.github.soundgoodizerfan.feedback.weapon.GunDesignation;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunHazard;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.weapon.Magazine;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A gun is a modular item like a pick: its figures come out of its
 * parts and their metals with no row anywhere, the Tool Table's inference reads the parts back as a gun, the
 * variants, traits and designation are read off the parts, and a shot that leaves the muzzle lands on what is in
 * front of it. Tests are built from a design the way a player's parts make one: {@link #steel}, {@link #with}.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class GunGameTests {

    private static final ResourceLocation GUN = Feedback.id("gun");

    /** The gun type's starter design in steel: a 9 mm self-loading handgun, medium barrel, standard wall. */
    public static ModularDesign steel() {
        return ToolDesigns.starter(GUN, ModularTypes.get().types(), MaterialTable.get().materials()).orElseThrow();
    }

    /** {@code design} with {@code part} in its role, added if the design had none; roles kept in the type's order. */
    public static ModularDesign with(ModularDesign design, ModularDesign.Part part) {
        List<ModularDesign.Part> parts = new ArrayList<>(design.parts().stream().filter(p -> !p.role().equals(part.role())).toList());
        parts.add(part);
        List<String> order = ModularTypes.get().types().get(GUN).order();
        parts.sort(Comparator.comparingInt(p -> order.indexOf(p.role())));
        return new ModularDesign(design.type(), parts, design.edge());
    }

    public static ModularDesign.Part part(String role, String material, String form, int count, String kind, String variant,
                                          Map<String, Float> dims) {
        return new ModularDesign.Part(role, Feedback.id(material), form, count, kind, variant, 0, dims);
    }

    /** The barrel as the lathe left it: a bore, an outside and a length. */
    public static ModularDesign barrel(ModularDesign d, String material, float bore, float wall, float length) {
        return with(d, part("B", material, "ingot", 1, "gun_barrel", "", Map.of("bore", bore, "outer", bore + 2 * wall, "length", length)));
    }

    /** {@code d} wound as a coil gun: two blocks of redstone on the chamber's parts. */
    public static ModularDesign coil(ModularDesign d) {
        return with(d, part("W", "redstone", "block", 2, "", "", Map.of()));
    }

    public static GunSpec spec(ModularDesign d) {
        return GunSpec.of(d, ModularTypes.get().types().get(GUN), MaterialTable.get().materials()).orElseThrow(() -> new AssertionError("a gun: " + d));
    }

    public static GunFigures bake(ModularDesign d) {
        return spec(d).bake(MaterialTable.get().materials()).orElseThrow();
    }

    private static boolean legal(ModularDesign d) {
        return GunSpec.of(d, ModularTypes.get().types().get(GUN), MaterialTable.get().materials()).isPresent();
    }

    @GameTest(template = "empty")
    public static void figuresComeOutOfThePartsAndTheMetal(GameTestHelper helper) {
        ModularDesign base = steel();
        GunFigures steel = bake(base);
        float speed = steel.muzzleSpeed() * 20;
        helper.assertTrue(speed > 150 && speed < 500, "a 9 mm medium barrel is a pistol's speed, got " + speed + " m/s");
        helper.assertTrue(steel.overpressure() < 1, "steel holds black powder, got " + steel.overpressure());

        GunFigures lead = bake(barrel(base, "lead", 9, 3, 0.2f));
        helper.assertTrue(lead.chamberRating().value() < steel.chamberRating().value(), "lead holds less than steel");
        helper.assertTrue(lead.overpressure() > 1, "a lead chamber is over its rating, got " + lead.overpressure());

        GunFigures thin = bake(barrel(base, "steel", 9, 1.5f, 0.2f)), thick = bake(barrel(base, "steel", 9, 5, 0.2f));
        helper.assertTrue(thin.chamberRating().value() < thick.chamberRating().value(), "thicker wall, higher rating");
        helper.assertTrue(thin.gunMass().value() < thick.gunMass().value(), "thicker wall, heavier gun");
        helper.assertTrue(thin.muzzleSpeed() == thick.muzzleSpeed(), "the wall does not change the barrel's push");

        GunFigures shortBarrel = bake(barrel(base, "steel", 9, 3, 0.1f)), longBarrel = bake(barrel(base, "steel", 9, 3, 0.4f));
        helper.assertTrue(longBarrel.muzzleSpeed() > shortBarrel.muzzleSpeed(), "a longer barrel is faster");
        helper.assertTrue(longBarrel.spreadDeg() < shortBarrel.spreadDeg(), "and tighter");
        helper.assertTrue(longBarrel.gunMass().value() > shortBarrel.gunMass().value(), "and heavier");

        GunFigures wide = bake(barrel(base, "steel", 12, 3, 0.2f));
        helper.assertTrue(wide.muzzleEnergy() > steel.muzzleEnergy() && wide.kickDeg() > steel.kickDeg(), "a bigger bore: more work and more kick");
        helper.assertTrue(longBarrel.spreadDeg() > 0, "spread has a floor");
        helper.succeed();
    }

    /** The table reads the parts back as a gun, and its stack's figures are the reference bake of the same parts. */
    @GameTest(template = "empty")
    public static void theTableMakesTheGunTheParts(GameTestHelper helper) {
        var types = ModularTypes.get().types();
        var materials = MaterialTable.get().materials();
        for (ModularDesign d : List.of(steel(), barrel(steel(), "steel", 12, 3, 0.4f))) {
            Optional<ItemStack> built = ToolDesigns.build(d, types, materials);
            helper.assertTrue(built.isPresent() && built.get().is(FWeapons.GUN.get()), "the Tool Table logic makes a gun of " + d.parts());
            GunFigures stacked = GunFigures.of(ModularStacks.figures(built.get()));
            GunFigures direct = bake(ModularStacks.design(built.get()));
            helper.assertTrue(stacked != null && stacked.equals(direct), "the stack carries the reference bake of its parts");
            helper.assertTrue(ModularBake.bake(ModularStacks.design(built.get()), types.get(GUN), materials, 0).map(f -> f.get(Fig.MASS)).orElse(0f) > 0,
                    "the gun's mass is the core's MASS figure");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void calibresAreAShortListOfClasses(GameTestHelper helper) {
        helper.assertTrue(GunCalibres.classes().size() >= 4, "named calibre classes");
        helper.assertTrue(GunCalibres.nearest(10).boreMm() == 9 && GunCalibres.step(9, 1).boreMm() == 12, "a die steps between classes, never in between");
        helper.succeed();
    }

    /** An action works only with a feed that suits it, and a second barrel only on a break action. */
    @GameTest(template = "empty")
    public static void theActionAndTheFeedHaveToWork(GameTestHelper helper) {
        ModularDesign d = steel();
        ModularDesign.Part cylinder = part("F", "steel", "ground_ingot", 1, "cylinder", "", Map.of());
        helper.assertTrue(legal(with(d, cylinder)), "a self-loading hammer action takes a cylinder");
        ModularDesign bolt = new ModularDesign(d.type(), d.parts().stream().filter(p -> !p.role().equals("A")).toList(), d.edge());
        bolt = with(bolt, part("K", "steel", "rod", 1, "bolt", "", Map.of()));
        helper.assertTrue(legal(bolt) && !legal(with(bolt, cylinder)), "a bolt action has no hammer for a cylinder");
        ModularDesign lever = with(new ModularDesign(bolt.type(), bolt.parts().stream().filter(p -> !p.role().equals("K")).toList(), bolt.edge()),
                part("K", "steel", "rod", 1, "bolt", "lever", Map.of()));
        helper.assertTrue(legal(with(lever, cylinder)), "a lever action does");
        ModularDesign second = with(d, part("D", "steel", "turned_rod", 1, "gun_barrel", "", Map.of()));
        helper.assertFalse(legal(second), "a second barrel needs a break action");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fittingsArePartsThatTrade(GameTestHelper helper) {
        ModularDesign plain = steel();
        GunFigures bare = bake(plain);
        GunFigures quiet = bake(with(plain, part("M", "steel", "cylinder", 1, "suppressor", "", Map.of())));
        helper.assertTrue(quiet.report() < bare.report() && quiet.heatFactor() > 1f && quiet.gunMass().value() > bare.gunMass().value(),
                "a suppressor is quieter, hotter and heavier");
        GunFigures braked = bake(with(plain, part("N", "steel", "magazine_tube", 1, "muzzle_brake", "", Map.of())));
        helper.assertTrue(braked.kickDeg() < bare.kickDeg() && braked.report() > bare.report(), "a brake kicks less and is louder");
        helper.assertFalse(legal(with(with(plain, part("M", "steel", "cylinder", 1, "suppressor", "", Map.of())),
                part("N", "steel", "magazine_tube", 1, "muzzle_brake", "", Map.of()))), "one muzzle, one fitting on it");
        GunFigures shrouded = bake(with(plain, part("H", "steel", "drilled_plate", 1, "barrel_shroud", "", Map.of())));
        helper.assertTrue(shrouded.coolingRate().tuPerTick() > bare.coolingRate().tuPerTick(), "a shroud sheds heat faster");
        helper.succeed();
    }

    /** Traits are aspects read from the parts; the designation is a rule over them. */
    @GameTest(template = "empty")
    public static void traitsAndDesignationAreReadOffTheParts(GameTestHelper helper) {
        var types = ModularTypes.get().types();
        var materials = MaterialTable.get().materials();
        ModularDesign d = steel();
        ItemStack gun = ToolDesigns.build(d, types, materials).orElseThrow();
        List<String> ids = ModularTraits.allOf(gun).stream().map(ModularTraits.Active::id).toList();
        helper.assertTrue(ids.contains("blowback") && ids.contains("closed_bolt"),
                "a slide is blowback, got " + ids);
        helper.assertTrue(ModularTraits.cooksOff(gun), "a closed bolt can cook off");
        helper.assertTrue(GunDesignation.of(gun, spec(d)).orElse("").equals("pistol"), "a self-loading handgun is a pistol");
        ModularDesign revolver = with(d, part("F", "steel", "ground_ingot", 1, "cylinder", "", Map.of()));
        helper.assertTrue(GunDesignation.of(ToolDesigns.build(revolver, types, materials).orElseThrow(), spec(revolver)).orElse("").equals("revolver"),
                "a handgun with a cylinder is a revolver");
        ModularDesign mg = with(d, part("R", "steel", "ingot", 3, "receiver", "machine_gun", Map.of()));
        ItemStack mgGun = ToolDesigns.build(mg, types, materials).orElseThrow();
        helper.assertFalse(ModularTraits.cooksOff(mgGun), "an open bolt has nothing in the hot chamber to cook off");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aHotChamberCooksOff(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 1.0, 2.2));
        player.setPos(feet.x, feet.y, feet.z);
        var level = helper.getLevel();
        ItemStack gun = ToolDesigns.build(steel(), ModularTypes.get().types(), MaterialTable.get().materials()).orElseThrow();
        gun.set(FWeapons.CHAMBER.get(), Cartridge.reference(9));
        gun.set(FWeapons.MAGAZINE.get(), new Magazine(java.util.List.of(Cartridge.reference(9))));
        helper.assertFalse(GunShots.cookOff(level, player, gun, 5_000_000L), "a cold barrel does not cook off");
        gun.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TEMPERATURE.get(), 450f);
        gun.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.HEATED_AT.get(), level.getGameTime());
        helper.assertTrue(GunShots.cookOff(level, player, gun, 5_000_100L), "past the ignition temperature it goes off");
        helper.assertTrue(GunAmmo.count(gun) == 1, "the chambered round is gone and the next is fed");
        helper.assertTrue(GunHazard.cookOffHp(4, 1f) > 0, "it is a hazard to the one holding it");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theCycleIsWallClockAndToleratesJitter(GameTestHelper helper) {
        GunFigures f = bake(steel());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        long t = 1_000_000L;
        helper.assertTrue(GunShots.accept(player, f, t), "the first shot is accepted");
        helper.assertFalse(GunShots.accept(player, f, t + 10), "ten ms later is far inside the cycle");
        helper.assertTrue(GunShots.accept(player, f, t + f.cycleMs()), "one cycle later is accepted");
        helper.succeed();
    }

    /** A shot leaves a muzzle and lands on the zombie in front of it. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aShotHitsWhatIsInFront(GameTestHelper helper) {
        GunFigures f = bake(steel());
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 1.0, 2.2));
        player.setPos(feet.x, feet.y, feet.z);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        zombie.setNoAi(true);
        zombie.setSilent(true);
        float before = zombie.getHealth();
        GunShots.fire(level, player, f, 0f, 0f);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(zombie.getHealth() < before, "the zombie took damage, health " + zombie.getHealth() + " of " + before);
            helper.succeed();
        });
    }
}
