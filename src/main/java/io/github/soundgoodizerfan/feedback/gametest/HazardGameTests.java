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
import io.github.soundgoodizerfan.feedback.core.hazard.HazardEvents;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.item.Tongs;
import io.github.soundgoodizerfan.feedback.machine.boiler.BoilerBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FFluids;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The hazards and the catch-up that came with them. Only the load-bearing arithmetic and the
 * decisions a mistake would silently flip: that a closed-form gap equals ticking, that a shut
 * boiler under a real fire is past its danger line and an open one is not, and that tongs and
 * gloves move a limit by their materials and not by a stamped number.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class HazardGameTests {

    private static final class Body implements ThermalBody {
        private Tu temperature;

        Body(Tu start) {
            temperature = start;
        }

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
            return new ThermalMass(120f);
        }
    }

    /** A million ticks cost one step and agree with the loop: that is the whole of catch-up's maths. */
    @GameTest(template = "empty")
    public static void relaxEqualsTickingAcrossAGap(GameTestHelper helper) {
        for (boolean conducted : new boolean[] {true, false}) {
            Body ticked = new Body(new Tu(900f));
            Body relaxed = new Body(new Tu(900f));
            Tu fire = new Tu(1200f);
            Tu room = FTuning.AMBIENT_TU;
            for (int i = 0; i < 700; i++)
                Heat.tick(ticked, fire, room, conducted);
            Heat.relax(relaxed, fire, room, conducted, 700);
            helper.assertTrue(Math.abs(ticked.getTemperature().value() - relaxed.getTemperature().value()) < 0.05f,
                    "relax agrees with 700 ticks, conducted=" + conducted + ": "
                            + ticked.getTemperature().value() + " vs " + relaxed.getTemperature().value());
        }
        helper.succeed();
    }

    /** A saved block reports how long it was away exactly once, and only if it was away a while. */
    @GameTest(template = "empty")
    public static void awayTimeIsReportedOnce(GameTestHelper helper) {
        long now = helper.getLevel().getGameTime();
        Catchup away = new Catchup();
        CompoundTag tag = new CompoundTag();
        tag.putLong("SavedAt", now - 500);
        away.load(tag);
        helper.assertTrue(away.take(helper.getLevel()) == 500, "away for the whole gap");
        helper.assertTrue(away.take(helper.getLevel()) == 0, "and only told once");

        Catchup brief = new Catchup();
        tag.putLong("SavedAt", now - (FTuning.CATCHUP_MIN_TICKS - 1));
        brief.load(tag);
        helper.assertTrue(brief.take(helper.getLevel()) == 0, "a flicker is not worth the arithmetic");
        helper.succeed();
    }

    /** Steam with nowhere to go under a real fire is past danger; the same tank cool is not. */
    @GameTest(template = "empty")
    public static void shutBoilerUnderFireIsPastDanger(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, FBlocks.BOILER.get());
        BoilerBlockEntity boiler = (BoilerBlockEntity) helper.getBlockEntity(at);
        boiler.getSteamTank().fill(new FluidStack(FFluids.STEAM.get(), FTuning.BOILER_STEAM_TANK_MB),
                IFluidHandler.FluidAction.EXECUTE);

        boiler.setTemperature(FTuning.BOILER_WORKING_TU);
        helper.assertTrue(boiler.getPressure().value() < FTuning.PRESSURE_VESSEL_DANGER_PU.value(),
                "full and merely at working temperature is not yet in danger: " + boiler.getPressure().value());

        boiler.setTemperature(new Tu(1000f));
        helper.assertTrue(boiler.getPressure().value() > FTuning.PRESSURE_VESSEL_DANGER_PU.value(),
                "full and under a real fire is past the danger line: " + boiler.getPressure().value());

        // Open: a half-empty tank has an outlet, so heat is spent boiling and not paid as pressure.
        boiler.getSteamTank().drain(FTuning.BOILER_STEAM_TANK_MB / 2, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(boiler.getPressure().value() < FTuning.PRESSURE_VESSEL_DANGER_PU.value(),
                "with room in the tank, the same heat is not pressure");
        helper.succeed();
    }

    /** What tongs hold is the lower of their grip's and their arms' limit, read from the files. */
    @GameTest(template = "empty")
    public static void tongsAndGlovesAreRatedByTheirMaterials(GameTestHelper helper) {
        var level = helper.getLevel();
        var ironBone = tongs("iron", "bone");
        var ironWood = tongs("iron", "wood");
        var copperBone = tongs("copper", "bone");
        var tongsItem = (io.github.soundgoodizerfan.feedback.item.TongsItem) FItems.TONGS.get();

        float bone = tongsItem.handlesUpToTu(ironBone, level);
        float wood = tongsItem.handlesUpToTu(ironWood, level);
        float copper = tongsItem.handlesUpToTu(copperBone, level);
        helper.assertTrue(bone > wood, "a better grip holds hotter: bone " + bone + " wood " + wood);
        helper.assertTrue(wood > FTuning.CONTACT_BURN_TU.value(), "even wooden tongs hold what a bare hand cannot: " + wood);
        helper.assertTrue(copper <= 1000f, "copper arms give out at copper's melt: " + copper);

        ItemStack wool = new ItemStack(FItems.GAUNTLET.get());
        wool.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.GEAR.get(), new io.github.soundgoodizerfan.feedback.combat.gear.GearStats(
                io.github.soundgoodizerfan.feedback.Feedback.id("wool"), io.github.soundgoodizerfan.feedback.Feedback.id("leather"), 0, 0, 0, 0, 0, 0, 0));
        float glove = FItems.GAUNTLET.get().handlesUpToTu(wool, level);
        helper.assertTrue(glove > wood,
                "wool-lined gloves hold a lit furnace: " + glove);
        helper.assertTrue(FItems.GAUNTLET.get().shockProofVolts(wool, level) > 0f,
                "dry wool insulates a conductor");
        helper.assertTrue(tongsItem.shockProofVolts(ironBone, level) == 0f, "tongs are no use against a wire");
        helper.succeed();
    }

    /** A hot piece in a bare hand hurts a survival player; tongs in the other hand stop it. */
    @GameTest(template = "empty")
    public static void hotPieceBurnsABareHandAndTongsStopIt(GameTestHelper helper) {
        var level = helper.getLevel();
        var bare = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack hot = new ItemStack(Items.IRON_INGOT);
        ItemHeat.set(hot, new Tu(900f), level);
        bare.setItemInHand(InteractionHand.MAIN_HAND, hot);
        float before = bare.getHealth();
        HazardEvents.onPlayerTick(new PlayerTickEvent.Post(bare));
        helper.assertTrue(bare.getHealth() < before, "a bare hand holding 900 Tu is hurt");

        var gripped = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack piece = new ItemStack(Items.IRON_INGOT);
        ItemHeat.set(piece, new Tu(900f), level);
        gripped.setItemInHand(InteractionHand.MAIN_HAND, piece);
        gripped.setItemInHand(InteractionHand.OFF_HAND, tongs("iron", "bone"));
        float healthy = gripped.getHealth();
        HazardEvents.onPlayerTick(new PlayerTickEvent.Post(gripped));
        helper.assertTrue(gripped.getHealth() == healthy, "tongs in the other hand: not hurt");
        helper.succeed();
    }

    private static ItemStack tongs(String arms, String grip) {
        ItemStack stack = new ItemStack(FItems.TONGS.get());
        stack.set(FDataComponents.TONGS.get(), new Tongs(Feedback.id(arms), Feedback.id(grip)));
        return stack;
    }
}
