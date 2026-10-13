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
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.process.CastingTable;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunHazard;
import io.github.soundgoodizerfan.feedback.weapon.Magazine;
import io.github.soundgoodizerfan.feedback.weapon.MagazineItem;
import io.github.soundgoodizerfan.feedback.weapon.MagazineLoadRecipe;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunRecipe;
import io.github.soundgoodizerfan.feedback.weapon.GunStandBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
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

    /** A shot leaves a muzzle and lands on the zombie in front of it. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aShotHitsWhatIsInFront(GameTestHelper helper) {
        GunFigures f = bake(helper, GunDesign.starter(Feedback.id("steel")));
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 1.0, 2.2));
        player.setPos(feet.x, feet.y, feet.z);

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        zombie.setNoAi(true);
        zombie.setSilent(true);
        float before = zombie.getHealth();

        // Yaw 0 is +z. Both stand outside the test's barrier shell (a ray from inside hits it),
        // and close: the shells of the next row of tests sit a few blocks further on, and a
        // longer shot passed or failed with where this test happened to be laid out.
        // The zombie's body spans the eye height.
        GunShots.fire(level, player, f, 0f, 0f);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(zombie.getHealth() < before, "the zombie took damage, health " + zombie.getHealth() + " of " + before);
            helper.succeed();
        });
    }

    /**
     * The Stand bakes the figures at print and writes them with the design onto the paper; the
     * recipe builds the gun from that design's material's ingots, the count the design says, and
     * the gun it makes needs no first-tick bake. A material that cannot make a gun prints nothing.
     */
    @GameTest(template = "empty")
    public static void standPrintsAndTheRecipeBuildsTheGun(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        helper.setBlock(at, FWeapons.GUN_STAND.get());
        var stand = (GunStandBlockEntity) helper.getBlockEntity(at);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var materials = MaterialTable.get().materials();
        GunDesign design = new GunDesign(12, Barrel.LONG, Wall.THICK, Feedback.id("steel"));
        GunFigures expected = design.bake(materials).orElseThrow();

        // Something no metal names prints nothing: the paper stays paper.
        stand.setItem(GunStandBlockEntity.PAPER, new ItemStack(Items.PAPER));
        stand.commit(new GunDesign(9, Barrel.MEDIUM, Wall.STANDARD, Feedback.id("nonsense")), true, player);
        helper.assertTrue(stand.getItem(GunStandBlockEntity.PAPER).is(Items.PAPER), "an unmade material prints nothing");

        stand.commit(design, true, player);
        ItemStack blueprint = stand.getItem(GunStandBlockEntity.PAPER);
        helper.assertTrue(blueprint.is(FWeapons.GUN_BLUEPRINT.get()), "the paper becomes a gun blueprint");
        helper.assertTrue(design.equals(blueprint.get(FWeapons.GUN_DESIGN.get())), "it carries the design");
        helper.assertTrue(expected.equals(blueprint.get(FWeapons.GUN_FIGURES.get())), "and the figures baked from it");

        helper.assertTrue(stand.canPlaceItem(GunStandBlockEntity.SAMPLE, new ItemStack(Items.IRON_INGOT)), "an iron ingot is a sample");
        helper.assertFalse(stand.canPlaceItem(GunStandBlockEntity.SAMPLE, new ItemStack(Items.OAK_PLANKS)), "wood is not a metal");
        stand.setItem(GunStandBlockEntity.SAMPLE, new ItemStack(Items.COPPER_INGOT));
        helper.assertTrue(stand.design().material().equals(Feedback.id("copper")), "a copper ingot makes it a copper gun, got " + stand.design().material());

        var recipe = new GunRecipe(CraftingBookCategory.MISC);
        ItemStack steel = new ItemStack(FItems.STEEL_INGOT.get());
        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        int n = design.ingots();
        helper.assertTrue(n == Barrel.LONG.ingots() + Wall.THICK.ingots(), "the cost is the bands' ingots, got " + n);
        helper.assertTrue(recipe.matches(grid(blueprint, steel, n), helper.getLevel()), "the blueprint and " + n + " steel ingots build it");
        helper.assertFalse(recipe.matches(grid(blueprint, steel, n - 1), helper.getLevel()), "one ingot short does not");
        helper.assertFalse(recipe.matches(grid(blueprint, copper, n), helper.getLevel()), "the wrong metal does not");
        ItemStack gun = recipe.assemble(grid(blueprint, steel, n), helper.getLevel().registryAccess());
        helper.assertTrue(gun.is(FWeapons.GUN.get()), "it makes a gun");
        helper.assertTrue(design.equals(gun.get(FWeapons.GUN_DESIGN.get())) && expected.equals(GunItem.figures(gun)),
                "the gun carries design and figures together");
        helper.succeed();
    }

    private static CraftingInput grid(ItemStack blueprint, ItemStack ingot, int count) {
        var list = new java.util.ArrayList<ItemStack>();
        list.add(blueprint);
        for (int i = 0; i < count; i++)
            list.add(ingot.copy());
        while (list.size() < 9)
            list.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, list);
    }

    /**
     * A round is three materials and a count, and what it asks of a gun comes out of them: more
     * charge is more pressure, a slow powder wants a long barrel, and a round for another bore is
     * not fired. No item is named anywhere in it.
     */
    @GameTest(template = "empty")
    public static void aRoundIsFiguresAndTheGunFitsIt(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        helper.assertTrue(materials.get(Feedback.id("gunpowder")).propellant().isPresent(), "gunpowder is a propellant");
        helper.assertTrue(materials.get(Feedback.id("lead")).propellant().isEmpty(), "lead is not");

        Cartridge nominal = Cartridge.reference(9);
        Cartridge hot = new Cartridge(9, nominal.caseMaterial(), nominal.propellant(), 8, nominal.projectile());
        GunDesign pistol = GunDesign.starter(Feedback.id("steel"));
        GunFigures a = pistol.bake(materials, nominal).orElseThrow();
        GunFigures b = pistol.bake(materials, hot).orElseThrow();
        helper.assertTrue(b.peakPressure().value() > a.peakPressure().value(), "a heavier charge makes more pressure");
        helper.assertTrue(Math.abs(a.peakPressure().value() - 3f) < 1e-4, "the nominal charge is the propellant's figure");
        helper.assertTrue(b.muzzleEnergy() > a.muzzleEnergy(), "and more of it is more work");

        // A slow powder in a short barrel leaves work unspent; the barrel gives it room.
        GunFigures shortBarrel = new GunDesign(9, Barrel.SHORT, Wall.STANDARD, Feedback.id("steel")).bake(materials, nominal).orElseThrow();
        GunFigures longBarrel = new GunDesign(9, Barrel.LONG, Wall.STANDARD, Feedback.id("steel")).bake(materials, nominal).orElseThrow();
        helper.assertTrue(shortBarrel.muzzleSpeed() < longBarrel.muzzleSpeed(), "a longer barrel burns more of the charge");

        helper.assertTrue(pistol.bake(materials, Cartridge.reference(12)).isEmpty(), "a 12 mm round does not fit a 9 mm gun");
        Cartridge notAPropellant = new Cartridge(9, nominal.caseMaterial(), Feedback.id("lead"), 4, nominal.projectile());
        helper.assertTrue(pistol.bake(materials, notAPropellant).isEmpty(), "a charge that is not a propellant makes no figures");
        helper.assertTrue(nominal.caseRating(materials).orElseThrow().value() > 0, "a brass case has a rating");
        helper.succeed();
    }

    /**
     * The press seats whatever is in it and names no cartridge: a case, a charge and a bullet
     * give a round of those three materials, and an over-full charge is refused.
     */
    @GameTest(template = "empty")
    public static void pressSeatsWhatIsInIt(GameTestHelper helper) {
        BlockPos at = new BlockPos(0, 2, 0);
        helper.setBlock(at, FWeapons.RELOADING_PRESS.get());
        var press = (ReloadingPressBlockEntity) helper.getBlockEntity(at);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var brassCase = BuiltInRegistries.ITEM.get(Feedback.id("brass_case"));
        var leadBullet = BuiltInRegistries.ITEM.get(Feedback.id("lead_bullet"));

        helper.assertTrue(press.canPlaceItem(ReloadingPressBlockEntity.CASE, new ItemStack(brassCase)), "a case goes in the case slot");
        helper.assertFalse(press.canPlaceItem(ReloadingPressBlockEntity.CASE, new ItemStack(leadBullet)), "a bullet does not");
        helper.assertTrue(press.canPlaceItem(ReloadingPressBlockEntity.CHARGE, new ItemStack(Items.GUNPOWDER)), "gunpowder is a charge");
        helper.assertFalse(press.canPlaceItem(ReloadingPressBlockEntity.CHARGE, new ItemStack(Items.COPPER_INGOT)), "copper is not");

        press.setItem(ReloadingPressBlockEntity.CASE, new ItemStack(brassCase, 2));
        press.setItem(ReloadingPressBlockEntity.PROJECTILE, new ItemStack(leadBullet, 2));
        press.setItem(ReloadingPressBlockEntity.CHARGE, new ItemStack(Items.GUNPOWDER, 9));
        press.commit(12, true, player);
        helper.assertTrue(press.getItem(ReloadingPressBlockEntity.OUTPUT).isEmpty(), "nine charge does not fit a case");

        press.setItem(ReloadingPressBlockEntity.CHARGE, new ItemStack(Items.GUNPOWDER, 5));
        press.commit(12, true, player);
        ItemStack out = press.getItem(ReloadingPressBlockEntity.OUTPUT);
        Cartridge round = out.get(FWeapons.CARTRIDGE.get());
        helper.assertTrue(round != null, "it seated a round");
        helper.assertTrue(round.boreMm() == 12 && round.charge() == 5, "of the die set's bore and the charge in the slot, got " + round);
        helper.assertTrue(round.caseMaterial().equals(Feedback.id("brass")) && round.propellant().equals(Feedback.id("gunpowder"))
                && round.projectile().equals(Feedback.id("lead")), "its parts are the materials of what was put in");
        helper.assertTrue(press.getItem(ReloadingPressBlockEntity.CASE).getCount() == 1
                && press.getItem(ReloadingPressBlockEntity.PROJECTILE).getCount() == 1
                && press.getItem(ReloadingPressBlockEntity.CHARGE).isEmpty(), "one case and bullet and the whole charge are used");
        helper.succeed();
    }

    /** Loose rounds make a fuller magazine, one bore only; reload swaps it into the gun, old rounds back out. */
    @GameTest(template = "empty")
    public static void magazinesLoadAndReloadSwaps(GameTestHelper helper) {
        var recipe = new MagazineLoadRecipe(CraftingBookCategory.MISC);
        ItemStack empty = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
        ItemStack nine = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
        nine.set(FWeapons.CARTRIDGE.get(), Cartridge.reference(9));
        ItemStack twelve = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
        twelve.set(FWeapons.CARTRIDGE.get(), Cartridge.reference(12));

        var level = helper.getLevel();
        helper.assertTrue(recipe.matches(CraftingInput.of(3, 3, java.util.Arrays.asList(empty, nine, nine, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)), level), "two rounds load");
        helper.assertFalse(recipe.matches(CraftingInput.of(3, 3, java.util.Arrays.asList(empty, nine, twelve, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)), level), "two bores in one magazine do not");
        ItemStack loaded = recipe.assemble(CraftingInput.of(3, 3, java.util.Arrays.asList(empty, nine, nine, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)), level.registryAccess());
        helper.assertTrue(MagazineItem.rounds(loaded).rounds().size() == 2, "a magazine of two");

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), GunDesign.starter(Feedback.id("steel")));
        gun.set(FWeapons.MAGAZINE.get(), new Magazine(java.util.List.of(Cartridge.reference(9))));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gun);
        player.getInventory().setItem(5, loaded);
        GunShots.reload(player, System.currentTimeMillis());
        helper.assertTrue(GunAmmo.count(gun) == 2 && gun.has(FWeapons.CHAMBER.get()),
                "the gun now holds the magazine's two rounds, one of them chambered");
        helper.assertTrue(MagazineItem.rounds(player.getInventory().getItem(5)).rounds().size() == 1,
                "the slot the magazine left holds the old rounds");
        helper.succeed();
    }

    /** One melt, two molds: an ingot in the ingot mold, bullets in the bullet mold. */
    @GameTest(template = "empty")
    public static void theMoldShapePicksTheCasting(GameTestHelper helper) {
        var lead = new net.neoforged.neoforge.fluids.FluidStack(BuiltInRegistries.FLUID.get(Feedback.id("molten_lead")), 144);
        var ingot = CastingTable.get().find(lead, "ingot");
        var bullets = CastingTable.get().find(lead, "bullet");
        helper.assertTrue(ingot.isPresent() && bullets.isPresent(), "lead casts in both molds");
        helper.assertTrue(ingot.get().result().getCount() == 1, "one ingot");
        helper.assertTrue(bullets.get().result().is(BuiltInRegistries.ITEM.get(Feedback.id("lead_bullet")))
                && bullets.get().result().getCount() > 1, "several bullets");
        helper.succeed();
    }

    /** A trigger pull fires the chambered round and the chamber refills from the magazine. */
    @GameTest(template = "empty")
    public static void theChamberFeedsFromTheMagazine(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 1.0, 2.2));
        player.setPos(feet.x, feet.y, feet.z);
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), GunDesign.starter(Feedback.id("steel")));
        gun.set(FWeapons.MAGAZINE.get(), new Magazine(java.util.List.of(Cartridge.reference(9), Cartridge.reference(9))));
        helper.assertTrue(GunAmmo.count(gun) == 2 && !gun.has(FWeapons.CHAMBER.get()), "two rounds, none chambered yet");

        long t = 2_000_000L;
        helper.assertTrue(GunShots.trigger(helper.getLevel(), player, gun, 0f, 0f, t), "the first pull fires");
        helper.assertTrue(GunAmmo.count(gun) == 1 && gun.has(FWeapons.CHAMBER.get()), "one left, and it is chambered");
        helper.assertTrue(GunShots.trigger(helper.getLevel(), player, gun, 0f, 0f, t + 1000), "the second fires");
        helper.assertTrue(GunAmmo.count(gun) == 0, "now empty");
        helper.assertFalse(GunShots.trigger(helper.getLevel(), player, gun, 0f, 0f, t + 2000), "and an empty gun does not");

        // Unloading gives the rounds back.
        gun.set(FWeapons.MAGAZINE.get(), new Magazine(java.util.List.of(Cartridge.reference(9), Cartridge.reference(9))));
        GunAmmo.feed(gun);
        GunAmmo.eject(player, gun);
        helper.assertTrue(GunAmmo.count(gun) == 0, "ejected: the gun holds nothing");
        helper.assertTrue(player.getInventory().contains(new ItemStack(FWeapons.MAGAZINE_ITEM.get()))
                || player.getInventory().items.stream().anyMatch(st -> st.is(FWeapons.MAGAZINE_ITEM.get())), "the magazine came back");
        helper.assertTrue(player.getInventory().items.stream().anyMatch(st -> st.is(FWeapons.CARTRIDGE_ITEM.get())), "and the chambered round");
        helper.succeed();
    }

    /**
     * Overpressure is read off the spec sheet and is design, not uptime: a sound gun is never
     * touched, copper only wears, and a lead chamber under black powder lets go the first shot
     * and hurts the one holding it. A spent gun stays an item and refuses the trigger.
     */
    @GameTest(template = "empty")
    public static void overpressureIsDesignAndSpendsTheGun(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        Cartridge round = Cartridge.reference(9);
        float caseRating = round.caseRating(materials).orElseThrow().value();
        helper.assertTrue(caseRating >= 3f, "a brass case holds the nominal charge, got " + caseRating);

        GunFigures steel = GunDesign.starter(Feedback.id("steel")).bake(materials, round).orElseThrow();
        GunFigures copper = GunDesign.starter(Feedback.id("copper")).bake(materials, round).orElseThrow();
        GunFigures lead = GunDesign.starter(Feedback.id("lead")).bake(materials, round).orElseThrow();
        var sound = GunHazard.assess(steel.peakPressure().value(), steel.chamberRating().value(), caseRating, 1f);
        var soft = GunHazard.assess(copper.peakPressure().value(), copper.chamberRating().value(), caseRating, 1f);
        var bad = GunHazard.assess(lead.peakPressure().value(), lead.chamberRating().value(), caseRating, 1f);
        helper.assertFalse(sound.any(), "a sound gun takes nothing");
        helper.assertTrue(soft.gunDamage() > 0 && soft.gunDamage() < 0.1f && soft.shooterHp() == 0f,
                "copper only wears, got " + soft);
        helper.assertTrue(bad.gunDamage() >= 1f && bad.shooterHp() > 0, "lead lets go and hurts, got " + bad);
        helper.assertTrue(lead.muzzleEnergy() < steel.muzzleEnergy(), "a venting chamber delivers less, not more");

        // The whole path, with a copper gun one shot from spent. (Not a lead one: hurting a mock
        // player in a game test hung Sable's game-event listener walk, see the spec's notes.)
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), GunDesign.starter(Feedback.id("copper")));
        gun.set(FWeapons.MAGAZINE.get(), new Magazine(java.util.List.of(round, round)));
        gun.set(FWeapons.GUN_DAMAGE.get(), 0.99f);
        helper.assertFalse(GunHazard.isSpent(gun), "not yet spent");
        helper.assertTrue(GunShots.trigger(helper.getLevel(), player, gun, 0f, 0f, 3_000_000L), "it still fires, once");
        helper.assertTrue(GunHazard.isSpent(gun), "and is spent");
        helper.assertFalse(GunShots.trigger(helper.getLevel(), player, gun, 0f, 0f, 3_001_000L), "a spent gun does not fire");
        helper.assertTrue(gun.getCount() == 1, "it is spent, not removed");
        helper.succeed();
    }

    /** The casting table crosses the wire intact, mold shape and all, for the recipe browser. */
    @GameTest(template = "empty")
    public static void castingSurvivesTheWire(GameTestHelper helper) {
        var entries = CastingTable.get().entries();
        helper.assertTrue(!entries.isEmpty(), "there are castings");
        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                helper.getLevel().registryAccess());
        io.github.soundgoodizerfan.feedback.net.CastingSyncPayload.STREAM_CODEC
                .encode(buf, new io.github.soundgoodizerfan.feedback.net.CastingSyncPayload(entries));
        var back = io.github.soundgoodizerfan.feedback.net.CastingSyncPayload.STREAM_CODEC.decode(buf).entries();
        helper.assertTrue(back.size() == entries.size(), "same count");
        for (int k = 0; k < entries.size(); k++) {
            var a = entries.get(k);
            var b = back.get(k);
            helper.assertTrue(a.mold().equals(b.mold()) && a.solidifyTemperature().equals(b.solidifyTemperature())
                    && ItemStack.matches(a.result(), b.result()), "entry " + k + " arrives as it left");
        }
        helper.succeed();
    }
}
