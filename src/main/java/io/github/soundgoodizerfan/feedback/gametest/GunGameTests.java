// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.gametest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunHazard;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.weapon.Magazine;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A gun is a modular item like a pick: its figures come out of its
 * parts and their metals with no row anywhere, and the Tool Table's inference reads the parts back as a gun. Tests are built from a design the way a player's parts make one: {@link #steel}, {@link #with}.
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
                                          Map<String, Float> axes) {
        return new ModularDesign.Part(role, Feedback.id(material), form, count, kind, variant, 0, io.github.soundgoodizerfan.feedback.process.Measures.ofAxes(axes));
    }

    /** The barrel as the lathe left it: a bore, an outside and a length. */
    public static ModularDesign barrel(ModularDesign d, String material, float bore, float wall, float length) {
        return with(d, part("B", material, "ingot", 1, "gun_barrel", "", Map.of("bore", bore, "outer", bore + 2 * wall, "length", length)));
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

    /** An action works only with a feed that suits it, and a second barrel only on a break action. */
    @GameTest(template = "empty")
    public static void theActionAndTheFeedHaveToWork(GameTestHelper helper) {
        ModularDesign d = steel();
        ModularDesign.Part cylinder = part("F", "steel", "ground_ingot", 1, "cylinder", "", Map.of());
        helper.assertFalse(legal(with(d, cylinder)), "a self-loading slide is striker-fired: no hammer for a cylinder");
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
}
