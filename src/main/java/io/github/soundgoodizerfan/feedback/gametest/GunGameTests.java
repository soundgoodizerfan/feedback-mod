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
import io.github.soundgoodizerfan.feedback.weapon.BarrelGeometry;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;
import io.github.soundgoodizerfan.feedback.weapon.GunLayout;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Barrel;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Wall;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.process.CastingTable;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;
import io.github.soundgoodizerfan.feedback.weapon.GunStance;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
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
        // The sample's part is the screen's selection, so the stand leaves the design alone.
        stand.setItem(GunStandBlockEntity.SAMPLE, new ItemStack(Items.COPPER_INGOT));
        helper.assertTrue(stand.design().equals(design), "a sample alone does not edit the design");

        // Assembly is the blueprint and its machined parts, each part's metal off its item.
        var recipe = new GunRecipe(CraftingBookCategory.MISC);
        java.util.List<ItemStack> parts = parts(materials, design, Feedback.id("steel"));
        helper.assertTrue(recipe.matches(grid(blueprint, parts), helper.getLevel()), "the blueprint and its steel parts build it");
        helper.assertFalse(recipe.matches(grid(blueprint, parts.subList(1, parts.size())), helper.getLevel()), "a part short does not");
        ItemStack gun = recipe.assemble(grid(blueprint, parts), helper.getLevel().registryAccess());
        GunDesign built = gun.get(FWeapons.GUN_DESIGN.get());
        helper.assertTrue(gun.is(FWeapons.GUN.get()) && built.built().isPresent(), "it makes a gun, of the barrel it was given");
        helper.assertTrue(built.boreMmReal() == BarrelGeometry.STOCK.boreMm(), "a barrel fresh from the drill is its pilot bore, not the blueprint's");

        // A copper grip in the grid makes a copper-gripped gun: the part says, not the paper.
        java.util.List<ItemStack> mixed = new java.util.ArrayList<>(parts);
        mixed.replaceAll(st -> Material.find(materials, st.getItem()).map(l -> l.form()).orElse("").equals("grip")
                ? new ItemStack(materials.get(Feedback.id("copper")).item(Feedback.id("copper"), "grip").orElseThrow()) : st);
        GunDesign copperGrip = recipe.assemble(grid(blueprint, mixed), helper.getLevel().registryAccess()).get(FWeapons.GUN_DESIGN.get());
        helper.assertTrue(copperGrip.parts().grip().equals(Feedback.id("copper")) && copperGrip.material().equals(Feedback.id("steel")),
                "the grip is copper and the barrel steel");
        helper.succeed();
    }

    private static CraftingInput grid(ItemStack blueprint, java.util.List<ItemStack> items) {
        var list = new java.util.ArrayList<ItemStack>();
        list.add(blueprint);
        items.forEach(st -> list.add(st.copy()));
        while (list.size() < 9)
            list.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, list);
    }

    /** The machined parts {@code design} is assembled from, all in {@code metal}. */
    private static java.util.List<ItemStack> parts(java.util.Map<net.minecraft.resources.ResourceLocation, Material> materials,
                                                   GunDesign design, net.minecraft.resources.ResourceLocation metal) {
        var out = new java.util.ArrayList<ItemStack>();
        design.assembly().forEach((form, n) -> {
            for (int k = 0; k < n; k++)
                out.add(new ItemStack(materials.get(metal).item(metal, form).orElseThrow(() -> new AssertionError("no " + form))));
        });
        return out;
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
        gun.set(FWeapons.MAGAZINE_SIZE.get(), MagazineSize.STANDARD); // a magazine is in
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

        // Unloading gives the rounds back, the magazine as a magazine.
        gun.set(FWeapons.MAGAZINE.get(), new Magazine(java.util.List.of(Cartridge.reference(9), Cartridge.reference(9))));
        gun.set(FWeapons.MAGAZINE_SIZE.get(), MagazineSize.STANDARD);
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

    /**
     * Heat is the barrel's own and cools at its own rate: a shot warms it by its heat over its
     * thermal mass, a heavy barrel warms less and cools slower, bloom and wear multiply the
     * spec sheet and never lock it, and a worn gun is worse but never empty.
     */
    @GameTest(template = "empty")
    public static void theBarrelHeatsWearsAndBlooms(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        Cartridge round = Cartridge.reference(9);
        GunFigures light = new GunDesign(9, Barrel.SHORT, Wall.THIN, Feedback.id("steel")).bake(materials, round).orElseThrow();
        GunFigures heavy = new GunDesign(9, Barrel.LONG, Wall.THICK, Feedback.id("steel")).bake(materials, round).orElseThrow();
        helper.assertTrue(heavy.thermalMass().workPerTu() > light.thermalMass().workPerTu(), "a heavy barrel has more thermal mass");
        helper.assertTrue(heavy.coolingRate().tuPerTick() < light.coolingRate().tuPerTick(), "and cools slower");

        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), GunDesign.starter(Feedback.id("steel")));
        GunFigures f = GunDesign.starter(Feedback.id("steel")).bake(materials, round).orElseThrow();
        gun.set(FWeapons.GUN_FIGURES.get(), f);
        var level = helper.getLevel();
        float cold = GunCondition.temperature(gun, level).value();
        GunCondition.afterShot(gun, f, level, 0f);
        float one = GunCondition.temperature(gun, level).value();
        helper.assertTrue(one > cold + 2, "a shot warms the barrel, " + cold + " to " + one);
        for (int k = 0; k < 19; k++)
            GunCondition.afterShot(gun, f, level, 0f);
        helper.assertTrue(GunCondition.temperature(gun, level).value() > one + 2 * 19, "and heat accumulates shot on shot");
        helper.assertTrue(GunCondition.wear(gun) > 0, "wear rose with use");

        // Bloom and wear multiply the sheet.
        GunFigures hot = f.aged(1f, 0.9f), worn = f.aged(GunCondition.condition(gun), 0f);
        helper.assertTrue(hot.spreadDeg() > f.spreadDeg() * 1.5f, "a hot barrel sprays wider");
        helper.assertTrue(hot.muzzleEnergy() == f.muzzleEnergy(), "but delivers the same work");
        gun.set(FWeapons.GUN_WEAR.get(), 5f);
        helper.assertTrue(GunCondition.condition(gun) == FTuning.GUN_CONDITION_FLOOR, "wear bottoms out at the floor, not at zero");
        GunFigures ruined = f.aged(GunCondition.condition(gun), 0f);
        helper.assertTrue(ruined.muzzleEnergy() > 0 && ruined.muzzleEnergy() < f.muzzleEnergy() && ruined.spreadDeg() > f.spreadDeg(),
                "a worn gun is worse, never empty");
        helper.assertTrue(worn.muzzleEnergy() <= f.muzzleEnergy(), "and wear never improves it");

        // A hot gun reads hot to an instrument: the same ItemHeat reading, at the gun's rate.
        float stamped = GunCondition.temperature(gun, level).value();
        helper.assertTrue(stamped > FTuning.WARM_TU.value(), "this one is past the warm threshold, " + stamped);
        helper.succeed();
    }

    /**
     * A chambered round in a barrel past its propellant's ignition temperature goes off by
     * itself, hurts the holder, and keeps going at the gun's cycle while the barrel stays hot.
     * (Creative mock player: hurting a survival one hung Sable's listener walk; the damage is
     * asserted as a figure instead.)
     */
    @GameTest(template = "empty")
    public static void aHotChamberCooksOff(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 1.0, 2.2));
        player.setPos(feet.x, feet.y, feet.z);
        var level = helper.getLevel();
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), GunDesign.starter(Feedback.id("steel")));
        gun.set(FWeapons.CHAMBER.get(), Cartridge.reference(9));
        gun.set(FWeapons.MAGAZINE.get(), new Magazine(java.util.List.of(Cartridge.reference(9))));

        helper.assertFalse(GunShots.cookOff(level, player, gun, 5_000_000L), "a cold barrel does not cook off");
        gun.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TEMPERATURE.get(), 450f);
        gun.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.HEATED_AT.get(), level.getGameTime());
        helper.assertTrue(GunShots.cookOff(level, player, gun, 5_000_100L), "past the ignition temperature it goes off");
        helper.assertTrue(GunAmmo.count(gun) == 1, "the chambered round is gone and the next is fed");
        helper.assertFalse(GunShots.cookOff(level, player, gun, 5_000_110L), "gated by the gun's own cycle");
        helper.assertTrue(GunShots.cookOff(level, player, gun, 5_001_000L), "and goes again when it has cycled");
        helper.assertTrue(GunHazard.cookOffHp(4, 1f) > 0, "it is a hazard to the one holding it");
        helper.assertTrue(GunHazard.cookOffHp(8, 1f) > GunHazard.cookOffHp(4, 1f), "worse for a heavier charge");
        helper.assertTrue(GunHazard.cookOffHp(8, 1.25f) <= FTuning.GUN_COOKOFF_HP_MAX, "and capped");
        helper.succeed();
    }

    /**
     * The balance this was built for: a pistol can empty a magazine, and a handheld cannon pays
     * for what it does with heat. Counted at the figures, with no cooling between shots.
     */
    @GameTest(template = "empty")
    public static void aCannonPaysInHeatAndAPistolDoesNot(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        var level = helper.getLevel();
        Cartridge nominal = Cartridge.reference(9);
        Cartridge cannonRound = new Cartridge(20, nominal.caseMaterial(), nominal.propellant(), 8, nominal.projectile());
        helper.assertTrue(shotsToIgnition(level, materials, GunDesign.starter(Feedback.id("steel")), nominal) > 10,
                "a steel pistol survives a ten round dump");
        int cannon = shotsToIgnition(level, materials,
                new GunDesign(20, Barrel.LONG, Wall.THICK, Feedback.id("netherite")), cannonRound);
        helper.assertTrue(cannon >= 2 && cannon <= 10, "a 20 mm netherite cannon cooks off within a magazine, got " + cannon);
        helper.succeed();
    }

    private static int shotsToIgnition(net.minecraft.server.level.ServerLevel level,
                                       java.util.Map<net.minecraft.resources.ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material> materials,
                                       GunDesign design, Cartridge round) {
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), design);
        GunFigures reference = design.bake(materials, round).orElseThrow();
        gun.set(FWeapons.GUN_FIGURES.get(), reference);
        float ignition = round.load(materials).orElseThrow().ignitionTu();
        for (int shots = 1; shots < 200; shots++) {
            float heat = GunCondition.heatFraction(gun, level, ignition);
            GunCondition.afterShot(gun, reference.aged(GunCondition.condition(gun), heat), level, heat);
            if (GunCondition.temperature(gun, level).value() >= ignition)
                return shots;
        }
        return 200;
    }

    /** A bullet breaks only what is brittle (glass, ice) and goes on; stone stops it. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aBulletBreaksGlassAndStopsAtStone(GameTestHelper helper) {
        helper.assertFalse(Float.isNaN(GunShots.breakCost(GunShots.toughnessOf(net.minecraft.world.level.block.Blocks.GLASS))),
                "glass breaks");
        helper.assertFalse(Float.isNaN(GunShots.breakCost(GunShots.toughnessOf(net.minecraft.world.level.block.Blocks.RED_STAINED_GLASS_PANE))),
                "so does a stained pane, by what it is");
        helper.assertFalse(Float.isNaN(GunShots.breakCost(GunShots.toughnessOf(net.minecraft.world.level.block.Blocks.ICE))), "and ice");
        helper.assertTrue(Float.isNaN(GunShots.breakCost(GunShots.toughnessOf(net.minecraft.world.level.block.Blocks.STONE))), "stone does not");
        helper.assertTrue(Float.isNaN(GunShots.breakCost(GunShots.toughnessOf(net.minecraft.world.level.block.Blocks.OAK_PLANKS))), "nor wood");
        helper.assertTrue(Float.isNaN(GunShots.breakCost(GunShots.toughnessOf(net.minecraft.world.level.block.Blocks.IRON_BLOCK))), "nor metal");
        helper.assertTrue(Float.isNaN(GunShots.breakCost(GunShots.toughnessOf(net.minecraft.world.level.block.Blocks.OBSIDIAN))), "nor obsidian");

        // In the world: glass between the muzzle and a zombie breaks and the zombie is still hit.
        GunFigures f = bake(helper, GunDesign.starter(Feedback.id("steel")));
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 1.0, 2.2));
        player.setPos(feet.x, feet.y, feet.z);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        zombie.setNoAi(true);
        zombie.setSilent(true);
        // The glass in front of the zombie, level with the shot (the player's eye is about 1.6 up).
        BlockPos pane = helper.absolutePos(new BlockPos(1, 2, 3)).relative(net.minecraft.core.Direction.NORTH);
        level.setBlockAndUpdate(pane, net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
        float before = zombie.getHealth();
        GunShots.fire(level, player, f, 0f, 0f);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(level.getBlockState(pane).isAir(), "the glass is broken");
            helper.assertTrue(zombie.getHealth() < before, "and the shot went on through it");
            helper.succeed();
        });
    }

    /** Surplus carries on: a heavy round through a weak target hits what is behind it. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aHeavyRoundPassesThroughWhatItKills(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        GunDesign cannon = new GunDesign(20, Barrel.LONG, Wall.THICK, Feedback.id("steel"));
        Cartridge round = new Cartridge(20, Feedback.id("brass"), Feedback.id("gunpowder"), 4, Feedback.id("lead"));
        GunFigures f = cannon.bake(materials, round).orElseThrow();
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 1.0, 2.2));
        player.setPos(feet.x, feet.y, feet.z);
        // One zombie behind another along the line of fire, the near one nearly dead.
        Zombie near = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        near.setNoAi(true);
        near.setSilent(true);
        near.setHealth(1f);
        Zombie behind = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        behind.setNoAi(true);
        behind.setSilent(true);
        Vec3 nearAt = helper.absoluteVec(new Vec3(1.5, 2.0, 3.2));
        Vec3 behindAt = helper.absoluteVec(new Vec3(1.5, 2.0, 4.1));
        near.teleportTo(nearAt.x, nearAt.y, nearAt.z);
        behind.teleportTo(behindAt.x, behindAt.y, behindAt.z);
        float far = behind.getHealth();
        GunShots.fire(level, player, f, 0f, 0f);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(near.isDeadOrDying(), "the first is dead");
            helper.assertTrue(behind.getHealth() < far, "the second, in its line, was hit by what was left");
            helper.succeed();
        });
    }

    /** Stance multiplies the spread from what the shooter is doing; aiming is honoured only when standing. */
    @GameTest(template = "empty")
    public static void stanceAndSightsHandle(GameTestHelper helper) {
        float hip = GunStance.multiplier(false, false, false, false, 0);
        helper.assertTrue(hip == 1f, "standing still at the hip is the sheet");
        helper.assertTrue(GunStance.multiplier(true, false, false, false, 0) < 0.5f * hip, "aimed is tighter");
        helper.assertTrue(GunStance.multiplier(true, false, true, false, 0) > GunStance.multiplier(true, false, false, false, 0),
                "a sprinter's aim is not honoured");
        helper.assertTrue(GunStance.multiplier(true, false, false, true, 0) > hip, "in the air is worse than the hip");
        helper.assertTrue(GunStance.multiplier(false, false, false, false, 0.2) > hip, "moving opens it");
        helper.assertTrue(GunStance.multiplier(false, true, false, false, 0) < hip, "crouching closes it");

        // Sights are a band with a price and a trade: more zoom, slower to raise.
        var iron = GunDesign.Sight.IRON;
        var scope = GunDesign.Sight.SCOPE;
        helper.assertTrue(scope.fov() < iron.fov() && scope.raiseMs() > iron.raiseMs() && scope.plates() > iron.plates(),
                "a scope zooms more, raises slower and costs more");
        GunDesign plain = GunDesign.starter(Feedback.id("steel"));
        GunDesign scoped = new GunDesign(9, Barrel.MEDIUM, Wall.STANDARD, Feedback.id("steel"), scope);
        helper.assertTrue(scoped.assembly().get("plate") == scope.plates() && !plain.assembly().containsKey("plate"),
                "the sight's plates are in the parts");
        helper.assertTrue(scoped.bake(MaterialTable.get().materials()).orElseThrow()
                .equals(plain.bake(MaterialTable.get().materials()).orElseThrow()), "a sight changes what the player can do, not the gun");

        // A design written before sights existed reads as iron.
        var json = new com.google.gson.JsonObject();
        json.addProperty("bore", 9);
        json.addProperty("barrel", "medium");
        json.addProperty("wall", "standard");
        json.addProperty("material", "feedback:steel");
        var old = GunDesign.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).result().orElseThrow();
        helper.assertTrue(old.sight() == GunDesign.Sight.IRON, "no sight reads as iron");
        helper.assertTrue(FireMode.SEMI.next() == FireMode.BURST && FireMode.AUTO.next() == FireMode.SEMI, "fire modes cycle");
        helper.assertTrue(GunSound.layers(plain.bake(MaterialTable.get().materials()).orElseThrow(),
                net.minecraft.util.RandomSource.create()).size() >= 2, "a shot is more than one layer");
        helper.succeed();
    }

    /**
     * A charge is a blend of materials, and what it does is read off their figures: gunpowder
     * brings an oxidizer surplus, blaze powder is fuel that needs one, so blaze powder alone
     * fizzles and a balanced blend is the hottest and quickest charge there is -- and also the one
     * that cooks off soonest. No code names either.
     */
    @GameTest(template = "empty")
    public static void blendsAreReadOffTheirOxygen(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        var gp = Feedback.id("gunpowder");
        var bp = Feedback.id("blaze_powder");
        var brass = Feedback.id("brass");
        var lead = Feedback.id("lead");
        java.util.function.BiFunction<Integer, Integer, GunDesign.Load> blend = (g, b) -> {
            var charges = new java.util.ArrayList<Cartridge.Charge>();
            if (g > 0)
                charges.add(new Cartridge.Charge(gp, g));
            if (b > 0)
                charges.add(new Cartridge.Charge(bp, b));
            return new Cartridge(9, brass, charges, lead, java.util.Optional.empty()).load(materials).orElseThrow();
        };
        float powder = blend.apply(4, 0).peakPressure();
        helper.assertTrue(Math.abs(powder - 3f) < 1e-4, "four gunpowder is the nominal 3 Pu, got " + powder);
        helper.assertTrue(blend.apply(0, 4).peakPressure() < powder, "blaze powder alone has no oxidizer and fizzles");
        float balanced = blend.apply(4, 2).peakPressure();
        helper.assertTrue(balanced > blend.apply(6, 0).peakPressure(), "fuel with its oxidizer beats more oxidizer alone");
        helper.assertTrue(blend.apply(2, 4).peakPressure() < balanced, "too much fuel starves it");
        helper.assertTrue(blend.apply(4, 2).burnRatePerM() > blend.apply(4, 0).burnRatePerM(), "and it burns faster");
        helper.assertTrue(blend.apply(4, 2).ignitionTu() < blend.apply(4, 0).ignitionTu(), "and the easiest ingredient sets the ignition");

        Cartridge fire = new Cartridge(9, brass, java.util.List.of(new Cartridge.Charge(gp, 4), new Cartridge.Charge(bp, 2)),
                lead, java.util.Optional.empty());
        helper.assertTrue(fire.load(materials).orElseThrow().peakPressure() > fire.caseRating(materials).orElseThrow().value(),
                "a balanced blend is over a brass case's rating: it wants a stronger case");
        GunDesign shortGun = new GunDesign(9, Barrel.SHORT, Wall.THICK, Feedback.id("steel"));
        helper.assertTrue(shortGun.bake(materials, fire).orElseThrow().muzzleEnergy()
                > shortGun.bake(materials, Cartridge.reference(9)).orElseThrow().muzzleEnergy(), "it does more in a short barrel");

        // A field cannot be blended with a gas.
        var redstone = Feedback.id("redstone");
        Cartridge mixed = new Cartridge(9, brass, java.util.List.of(new Cartridge.Charge(gp, 2), new Cartridge.Charge(redstone, 2)),
                lead, java.util.Optional.empty());
        helper.assertTrue(mixed.load(materials).isEmpty(), "a coil drive does not blend with powder");

        // Rounds written before blends were a single propellant: still read.
        var json = new com.google.gson.JsonObject();
        json.addProperty("bore", 9);
        json.addProperty("case", "feedback:brass");
        json.addProperty("propellant", "feedback:gunpowder");
        json.addProperty("charge", 4);
        json.addProperty("projectile", "feedback:lead");
        var old = Cartridge.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).result().orElseThrow();
        helper.assertTrue(old.charge() == 4 && old.propellant().equals(gp), "the old shape reads as a one-part blend");

        // The press takes the powder form only, and a blaze rod is not one.
        helper.setBlock(new BlockPos(0, 2, 0), FWeapons.RELOADING_PRESS.get());
        var press = (ReloadingPressBlockEntity) helper.getBlockEntity(new BlockPos(0, 2, 0));
        helper.assertTrue(press.canPlaceItem(ReloadingPressBlockEntity.CHARGE_B, new ItemStack(Items.BLAZE_POWDER)), "blaze powder is a charge");
        helper.assertFalse(press.canPlaceItem(ReloadingPressBlockEntity.CHARGE, new ItemStack(Items.BLAZE_ROD)), "a blaze rod is not");
        helper.succeed();
    }

    /** Two charge slots seat a blend, and a payload slot a tracer or incendiary, all read by property. */
    @GameTest(template = "empty")
    public static void pressSeatsABlendAndAPayload(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 2, 0), FWeapons.RELOADING_PRESS.get());
        var press = (ReloadingPressBlockEntity) helper.getBlockEntity(new BlockPos(0, 2, 0));
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var materials = MaterialTable.get().materials();
        var steelCase = BuiltInRegistries.ITEM.get(Feedback.id("steel_case"));
        var leadBullet = BuiltInRegistries.ITEM.get(Feedback.id("lead_bullet"));

        helper.assertTrue(press.canPlaceItem(ReloadingPressBlockEntity.PAYLOAD, new ItemStack(Items.GLOWSTONE_DUST)), "glowstone dust is a payload");
        helper.assertTrue(press.canPlaceItem(ReloadingPressBlockEntity.PAYLOAD, new ItemStack(Items.BLAZE_POWDER)), "so is blaze powder");
        helper.assertFalse(press.canPlaceItem(ReloadingPressBlockEntity.PAYLOAD, new ItemStack(Items.IRON_INGOT)), "an ingot is not");

        press.setItem(ReloadingPressBlockEntity.CASE, new ItemStack(steelCase));
        press.setItem(ReloadingPressBlockEntity.CHARGE, new ItemStack(Items.GUNPOWDER, 4));
        press.setItem(ReloadingPressBlockEntity.CHARGE_B, new ItemStack(Items.BLAZE_POWDER, 2));
        press.setItem(ReloadingPressBlockEntity.PROJECTILE, new ItemStack(leadBullet));
        press.setItem(ReloadingPressBlockEntity.PAYLOAD, new ItemStack(Items.GLOWSTONE_DUST));
        press.commit(9, true, player);
        Cartridge round = press.getItem(ReloadingPressBlockEntity.OUTPUT).get(FWeapons.CARTRIDGE.get());
        helper.assertTrue(round != null, "it seated a round");
        helper.assertTrue(round.charges().size() == 2 && round.charge() == 6, "a two part charge of six, got " + round.charges());
        helper.assertTrue(round.tracer(materials) && !round.incendiary(materials), "a glowstone payload is a tracer, not incendiary");
        helper.succeed();
    }

    /** A coil throws a slug with a field: it needs a magnetic slug, a winding, and it quenches hot. */
    @GameTest(template = "empty")
    public static void aCoilThrowsAMagneticSlug(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        GunDesign coil = GunDesign.starter(Feedback.id("steel")).withLauncher(GunDesign.Launcher.COIL);
        Cartridge slug = Cartridge.referenceCoil(9);
        GunFigures f = coil.bake(materials, slug).orElseThrow();
        helper.assertTrue(f.coil() && f.muzzleEnergy() > 100f, "a coil makes real work, got " + f.muzzleEnergy());

        Cartridge leadSlug = new Cartridge(9, slug.caseMaterial(), slug.propellant(), slug.charge(), Feedback.id("lead"));
        helper.assertTrue(coil.bake(materials, leadSlug).orElseThrow().muzzleEnergy() < 0.3f * f.muzzleEnergy(),
                "a slug that is not magnetic barely couples");
        helper.assertTrue(coil.bake(materials, Cartridge.reference(9)).isEmpty(), "a coil does not fire a powder round");
        helper.assertTrue(GunDesign.starter(Feedback.id("steel")).bake(materials, slug).isEmpty(), "and a chamber does not fire a slug");

        GunDesign longCoil = coil.withBarrel(Barrel.LONG), shortCoil = coil.withBarrel(Barrel.SHORT);
        helper.assertTrue(longCoil.bake(materials, slug).orElseThrow().muzzleEnergy()
                > shortCoil.bake(materials, slug).orElseThrow().muzzleEnergy(), "more stages, more work");

        // The field's pressure is a pressure: overdriving a thin winding bursts it, the same rule.
        Cartridge overdriven = new Cartridge(9, slug.caseMaterial(), slug.propellant(), 8, slug.projectile());
        GunFigures thin = coil.withWall(Wall.THIN).bake(materials, overdriven).orElseThrow();
        helper.assertTrue(thin.overpressure() > 1f, "an overdriven thin winding is over its rating, got " + thin.overpressure());
        helper.assertTrue(coil.windingBlocks() == GunDesign.starter(Feedback.id("steel")).windingBlocks() + 2
                && longCoil.windingBlocks() > shortCoil.windingBlocks(), "a coil is wound from redstone blocks, more for length");

        // Quench is a hard gate with a cause; a coil never cooks off.
        var level = helper.getLevel();
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), coil);
        var load = slug.load(materials).orElseThrow();
        helper.assertFalse(GunCondition.quenched(gun, level, load), "cold, it carries");
        gun.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TEMPERATURE.get(), 450f);
        gun.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.HEATED_AT.get(), level.getGameTime());
        helper.assertTrue(GunCondition.quenched(gun, level, load), "hot, it has quenched");
        gun.set(FWeapons.CHAMBER.get(), slug);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        helper.assertFalse(GunShots.cookOff(level, player, gun, 6_000_000L), "a coil has nothing to cook off");
        helper.assertFalse(GunShots.trigger(level, player, gun, 0f, 0f, 6_000_100L), "and a quenched winding does not fire");
        helper.succeed();
    }

    /** The type offers its actions; a manual action is slow to cycle and fires one a press; only automatic is held. */
    @GameTest(template = "empty")
    public static void theActionIsHowItCycles(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        GunLayout lmg = new GunLayout(GunLayout.Type.LMG, GunLayout.Action.BOLT, GunLayout.Feed.TUBE);
        helper.assertTrue(lmg.action() == GunLayout.Action.AUTOMATIC, "an LMG offers no bolt; it falls back to automatic");
        helper.assertTrue(lmg.feed() == GunLayout.Feed.BELT, "and no tube; it falls back to a belt");
        helper.assertTrue(GunLayout.DEFAULT.withType(GunLayout.Type.MARKSMAN_RIFLE).action() == GunLayout.Action.SELF_LOADING,
                "a self-loading pistol made a rifle stays self-loading");
        GunDesign pistol = GunDesign.starter(Feedback.id("steel"));
        GunDesign bolt = pistol.withLayout(new GunLayout(GunLayout.Type.MARKSMAN_RIFLE, GunLayout.Action.BOLT, GunLayout.Feed.BOX));
        GunFigures a = pistol.bake(materials).orElseThrow(), b = bolt.bake(materials).orElseThrow();
        helper.assertTrue(b.cycleMs() == a.cycleMs() + FTuning.GUN_CYCLE_MANUAL_MS_BOLT, "a bolt costs its hand time every cycle");
        helper.assertTrue(b.muzzleEnergy() == a.muzzleEnergy(), "and changes nothing about the shot");
        helper.assertTrue(GunLayout.Action.BOLT.allow(FireMode.AUTO) == FireMode.SEMI, "a bolt cannot be held down");
        helper.assertTrue(GunLayout.Action.AUTOMATIC.next(FireMode.AUTO) == FireMode.BURST, "an automatic switches round its modes");
        helper.assertTrue(GunLayout.Action.LEVER.hammer() && !GunLayout.Action.BOLT.hammer(), "a lever has a hammer, a bolt a striker");
        // A design saved before layouts reads as a self-loading pistol.
        var json = GunDesign.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, bolt).getOrThrow().getAsJsonObject();
        json.remove("layout");
        helper.assertTrue(GunDesign.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow().layout()
                .equals(GunLayout.DEFAULT), "an old design is a self-loading pistol");
        helper.succeed();
    }

    /**
     * A revolver is any hammer action with a cylinder, loaded round by round from loose rounds; a
     * box gun takes any box magazine of its bore and gives back the one it had, and none if it had none.
     */
    @GameTest(template = "empty")
    public static void feedsHoldWhatTheyHold(GameTestHelper helper) {
        helper.assertTrue(GunLayout.Feed.CYLINDER.fits(GunLayout.Action.LEVER) && !GunLayout.Feed.CYLINDER.fits(GunLayout.Action.BOLT),
                "a cylinder wants a hammer");
        helper.assertTrue(GunLayout.Type.SHOTGUN.feeds(GunLayout.Action.PUMP).stream().noneMatch(f -> !f.internal()),
                "a shotgun takes no magazine");
        GunDesign pistol = GunDesign.starter(Feedback.id("steel"));
        GunDesign revolver = pistol.withLayout(pistol.layout().withFeed(GunLayout.Feed.CYLINDER));
        helper.assertTrue(revolver.internalCapacity().getAsInt() == FTuning.GUN_CYLINDER_ROUNDS, "a cylinder holds its chambers");
        GunDesign tube = new GunDesign(9, Barrel.LONG, Wall.STANDARD, Feedback.id("steel"))
                .withLayout(new GunLayout(GunLayout.Type.SHOTGUN, GunLayout.Action.PUMP, GunLayout.Feed.TUBE));
        helper.assertTrue(tube.internalCapacity().getAsInt() == FTuning.GUN_TUBE_ROUNDS_LONG, "a tube is as long as its barrel");

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), revolver);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gun);
        player.getInventory().add(GunAmmo.loose(Cartridge.reference(9)).copyWithCount(10));
        GunShots.reload(player, 0);
        helper.assertTrue(GunAmmo.count(gun) == FTuning.GUN_CYLINDER_ROUNDS, "a reload fills the cylinder, got " + GunAmmo.count(gun));
        helper.assertTrue(GunAmmo.capacity(gun) == FTuning.GUN_CYLINDER_ROUNDS, "and that is its capacity");

        ItemStack boxGun = new ItemStack(FWeapons.GUN.get());
        boxGun.set(FWeapons.GUN_DESIGN.get(), pistol);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, boxGun);
        ItemStack drum = new ItemStack(FWeapons.MAGAZINE_ITEM.get());
        drum.set(FWeapons.MAGAZINE_SIZE.get(), MagazineSize.DRUM);
        drum.set(FWeapons.MAGAZINE.get(), new Magazine(java.util.Collections.nCopies(30, Cartridge.reference(9))));
        player.getInventory().add(drum);
        GunShots.reload(player, 100_000);
        helper.assertTrue(GunAmmo.capacity(boxGun) == MagazineSize.DRUM.capacity() + 1, "a drum in is a drum's worth and a chamber");
        helper.assertTrue(GunAmmo.count(boxGun) == 30, "every round of it, one chambered");
        helper.assertFalse(player.getInventory().hasAnyMatching(s -> s.is(FWeapons.MAGAZINE_ITEM.get())),
                "a gun that had no magazine gives none back");
        helper.succeed();
    }

    /** A shotgun throws shot and nothing else does; a bigger bore throws more pellets, sharing the energy. */
    @GameTest(template = "empty")
    public static void shotgunsThrowShot(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        helper.assertTrue(Cartridge.reference(18).pellets() == 1, "a bullet is one");
        helper.assertTrue(Cartridge.referenceShot(18).pellets() > Cartridge.referenceShot(9).pellets()
                && Cartridge.referenceShot(18).pellets() <= FTuning.GUN_SHOT_MAX_PELLETS, "a bigger bore throws more, to a cap");
        GunDesign shotgun = new GunDesign(18, Barrel.LONG, Wall.STANDARD, Feedback.id("steel"))
                .withLayout(new GunLayout(GunLayout.Type.SHOTGUN, GunLayout.Action.PUMP, GunLayout.Feed.TUBE));
        GunDesign pistol = GunDesign.starter(Feedback.id("steel")).withBore(18);
        helper.assertTrue(shotgun.bake(materials, Cartridge.referenceShot(18)).isPresent(), "a shotgun fires a shell");
        helper.assertTrue(shotgun.bake(materials, Cartridge.reference(18)).isEmpty(), "and not a bullet");
        helper.assertTrue(pistol.bake(materials, Cartridge.referenceShot(18)).isEmpty(), "a pistol fires no shell");
        GunFigures whole = shotgun.bake(materials, Cartridge.referenceShot(18)).orElseThrow();
        int n = Cartridge.referenceShot(18).pellets();
        GunFigures pellet = whole.asPellet(n);
        helper.assertTrue(Math.abs(pellet.muzzleEnergy() * n - whole.muzzleEnergy()) < 1e-3
                && pellet.muzzleSpeed() == whole.muzzleSpeed() && pellet.spreadDeg() >= FTuning.GUN_SHOT_SPREAD_DEG,
                "each pellet carries its share at the same speed, in shot's cone");
        helper.succeed();
    }

    /**
     * A barrel's shape is state the machines change while they run: a drill pierces a turned rod
     * into a barrel, then keeps growing its bore, and drilled until the wall is gone it is swarf.
     * The gun bakes from the barrel it was given, and a round loose in its bore leaks, a tight one squeezes.
     */
    @GameTest(template = "empty")
    public static void barrelsAreMachinedAndTheGunIsTheBarrel(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        var steelId = Feedback.id("steel");
        Material steel = materials.get(steelId);
        var level = helper.getLevel();
        var drive = new io.github.soundgoodizerfan.feedback.core.unit.St(30f);
        var tool = new io.github.soundgoodizerfan.feedback.core.unit.St(100f);
        var drill = io.github.soundgoodizerfan.feedback.process.Operation.DRILL;
        ItemStack rod = new ItemStack(steel.item(steelId, "turned_rod").orElseThrow());
        ItemStack barrel = rod;
        for (int k = 0; k < 40 && !Material.find(materials, barrel.getItem()).orElseThrow().form().equals("gun_barrel"); k++)
            barrel = io.github.soundgoodizerfan.feedback.process.Removing.apply(barrel, drive, tool, drill, 1f, 0f, level).result();
        helper.assertTrue(Material.find(materials, barrel.getItem()).orElseThrow().form().equals("gun_barrel"), "a drilled turned rod is a barrel");
        helper.assertTrue(BarrelGeometry.STOCK.equals(barrel.get(FWeapons.BARREL_GEOMETRY.get())), "fresh from the drill: the stock shape");
        ItemStack wider = io.github.soundgoodizerfan.feedback.process.Removing.apply(barrel, drive, tool, drill, 1f, 0f, level).result();
        helper.assertTrue(wider.get(FWeapons.BARREL_GEOMETRY.get()).boreMm() > BarrelGeometry.STOCK.boreMm(), "drilling on grows the bore");
        helper.assertFalse(io.github.soundgoodizerfan.feedback.process.Removing.apply(barrel, drive,
                new io.github.soundgoodizerfan.feedback.core.unit.St(1f), drill, 1f, 0f, level).landed(), "a soft tool does nothing");
        ItemStack through = barrel;
        for (int k = 0; k < 2000 && through.has(FWeapons.BARREL_GEOMETRY.get()); k++)
            through = io.github.soundgoodizerfan.feedback.process.Removing.apply(through, drive, tool, drill, 1f, 0f, level).result();
        helper.assertTrue(Material.find(materials, through.getItem()).orElseThrow().form().equals("swarf"), "drilled through, it is swarf");

        GunDesign paper = GunDesign.starter(steelId);
        var round = Cartridge.reference(9);
        GunFigures exact = paper.withBuilt(new BarrelGeometry(9f, 15f, 0.2f)).bake(materials, round).orElseThrow();
        GunFigures loose = paper.withBuilt(new BarrelGeometry(9.5f, 15f, 0.2f)).bake(materials, round).orElseThrow();
        GunFigures tight = paper.withBuilt(new BarrelGeometry(8.6f, 15f, 0.2f)).bake(materials, round).orElseThrow();
        helper.assertTrue(loose.muzzleEnergy() < exact.muzzleEnergy() && loose.spreadDeg() > exact.spreadDeg(), "a loose bore leaks and scatters");
        helper.assertTrue(tight.peakPressure().value() > exact.peakPressure().value(), "a tight one squeezes the pressure up");
        helper.assertTrue(paper.withBuilt(new BarrelGeometry(10.5f, 15f, 0.2f)).bake(materials, round).isEmpty(), "too far off, it will not chamber");
        helper.succeed();
    }

    /**
     * The test commands: holding a named blueprint, {@code /feedback parts} gives exactly what it
     * assembles from (the recipe takes them), and {@code /feedback gun} a gun built to it, figures
     * baked and the design's name on it.
     */
    @GameTest(template = "empty")
    public static void commandsHandOutWhatTheBlueprintWants(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 2, 0), FWeapons.GUN_STAND.get());
        var stand = (GunStandBlockEntity) helper.getBlockEntity(new BlockPos(0, 2, 0));
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        GunDesign design = new GunDesign(12, Barrel.LONG, Wall.THICK, Feedback.id("steel"))
                .withLayout(new GunLayout(GunLayout.Type.MARKSMAN_RIFLE, GunLayout.Action.LEVER, GunLayout.Feed.TUBE));
        stand.setItem(GunStandBlockEntity.PAPER, new ItemStack(Items.PAPER));
        stand.commit(design, "Brush Gun", true, player);
        ItemStack blueprint = stand.getItem(GunStandBlockEntity.PAPER);
        helper.assertTrue("Brush Gun".equals(blueprint.getHoverName().getString()), "the blueprint carries the design's name");
        // Registered and parsed by the real dispatcher; run here without a server player (Sable wants a real connection).
        var server = helper.getLevel().getServer();
        for (String command : new String[]{"feedback gun", "feedback parts", "feedback barrel feedback:steel 9 3 0.2",
                "feedback round 18 true", "feedback magazine drum 9"})
            helper.assertFalse(server.getCommands().getDispatcher().parse(command, server.createCommandSourceStack()).getReader().canRead(),
                    "parses: " + command);
        java.util.List<ItemStack> parts;
        try {
            parts = io.github.soundgoodizerfan.feedback.command.FCommands.partsFor(design);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new AssertionError(e);
        }
        var recipe = new GunRecipe(CraftingBookCategory.MISC);
        helper.assertTrue(recipe.matches(grid(blueprint, parts), helper.getLevel()), "the parts given assemble, got " + parts);
        ItemStack assembled = recipe.assemble(grid(blueprint, parts), helper.getLevel().registryAccess());
        helper.assertTrue(assembled.get(FWeapons.GUN_DESIGN.get()).boreMmReal() == 12f, "the barrel was cut to the blueprint's bore");
        helper.assertTrue("Brush Gun".equals(assembled.getHoverName().getString()), "and the gun is named for its design");
        ItemStack gun = io.github.soundgoodizerfan.feedback.command.FCommands.gunFor(blueprint);
        helper.assertTrue(gun.has(FWeapons.GUN_FIGURES.get()) && gun.get(FWeapons.GUN_DESIGN.get()).built().isPresent()
                && "Brush Gun".equals(gun.getHoverName().getString()), "a gun built to the blueprint, baked and named");
        helper.succeed();
    }

    /** The grip's metal is in the weight and the kick: heavier kicks less, slicker kicks more. */
    @GameTest(template = "empty")
    public static void theGripIsInTheKick(GameTestHelper helper) {
        var materials = MaterialTable.get().materials();
        GunDesign steel = GunDesign.starter(Feedback.id("steel"));
        GunFigures base = steel.bake(materials).orElseThrow();
        GunFigures lead = steel.withMaterial(GunDesign.Part.GRIP, Feedback.id("lead")).bake(materials).orElseThrow();
        GunFigures bronze = steel.withMaterial(GunDesign.Part.GRIP, Feedback.id("bronze")).bake(materials).orElseThrow();
        helper.assertTrue(lead.gunMass().value() > base.gunMass().value() && lead.kickDeg() < base.kickDeg(),
                "a lead grip is heavier and kicks less");
        helper.assertTrue(bronze.kickDeg() > base.kickDeg(), "a slick bronze grip lets more kick through");
        helper.assertTrue(lead.chamberRating().equals(base.chamberRating()) && lead.muzzleEnergy() == base.muzzleEnergy(),
                "the grip touches neither the chamber nor the shot");
        helper.succeed();
    }

    /** The Stand prints a coil, and the recipe wants redstone blocks on top of the metal. */
    @GameTest(template = "empty")
    public static void coilGunsAreWoundFromRedstone(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 2, 0), FWeapons.GUN_STAND.get());
        var stand = (GunStandBlockEntity) helper.getBlockEntity(new BlockPos(0, 2, 0));
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        GunDesign coil = GunDesign.starter(Feedback.id("steel")).withLauncher(GunDesign.Launcher.COIL);
        stand.setItem(GunStandBlockEntity.PAPER, new ItemStack(Items.PAPER));
        stand.commit(coil, true, player);
        ItemStack blueprint = stand.getItem(GunStandBlockEntity.PAPER);
        helper.assertTrue(blueprint.is(FWeapons.GUN_BLUEPRINT.get()), "a coil prints");
        helper.assertTrue(blueprint.get(FWeapons.GUN_FIGURES.get()).coil(), "with coil figures");

        var recipe = new GunRecipe(CraftingBookCategory.MISC);
        var parts = new java.util.ArrayList<>(parts(MaterialTable.get().materials(), coil, Feedback.id("steel")));
        for (int k = 0; k < coil.windingBlocks(); k++)
            parts.add(new ItemStack(Items.REDSTONE_BLOCK));
        helper.assertTrue(recipe.matches(grid(blueprint, parts), helper.getLevel()), "the parts and the winding build it");
        parts.remove(parts.size() - 1);
        helper.assertFalse(recipe.matches(grid(blueprint, parts), helper.getLevel()), "short a winding block it does not");
        helper.succeed();
    }

    /** An incendiary round sets what it hits alight. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void anIncendiaryRoundSetsAlight(GameTestHelper helper) {
        GunFigures f = bake(helper, GunDesign.starter(Feedback.id("steel")));
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 1.0, 2.2));
        player.setPos(feet.x, feet.y, feet.z);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        zombie.setNoAi(true);
        zombie.setSilent(true);
        GunShots.fire(level, player, f, 0f, 0f, false, true);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(zombie.getRemainingFireTicks() > 0 || zombie.isOnFire(), "the zombie is alight");
            helper.succeed();
        });
    }
}
