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
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.bimetallic.BimetallicStripBlock;
import io.github.soundgoodizerfan.feedback.control.controller.ControllerBlock;
import io.github.soundgoodizerfan.feedback.control.controller.CreativeControllerBlock;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlock;
import io.github.soundgoodizerfan.feedback.control.timer.TimerBlock;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.bellows.BellowsBlock;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlock;
import io.github.soundgoodizerfan.feedback.machine.cell.GalvanicCellBlock;
import io.github.soundgoodizerfan.feedback.machine.capacitor.LeydenJarBlock;
import io.github.soundgoodizerfan.feedback.machine.dynamo.DynamoBlock;
import io.github.soundgoodizerfan.feedback.machine.dynamo.MotorBlock;
import io.github.soundgoodizerfan.feedback.machine.coil.CoilBlock;
import io.github.soundgoodizerfan.feedback.machine.breaker.BreakerBlock;
import io.github.soundgoodizerfan.feedback.machine.breaker.FuseBlock;
import io.github.soundgoodizerfan.feedback.machine.electrolytic.ElectrolyticCellBlock;
import io.github.soundgoodizerfan.feedback.machine.electromagnet.ElectromagnetBlock;
import io.github.soundgoodizerfan.feedback.machine.creative.CreativeElectricBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.creative.CreativeHeatBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.creative.CreativeSourceBlock;
import io.github.soundgoodizerfan.feedback.machine.electrostatic.ElectrostaticGeneratorBlock;
import io.github.soundgoodizerfan.feedback.machine.boiler.BoilerBlock;
import io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock;
import io.github.soundgoodizerfan.feedback.machine.overspeedtrip.OverspeedTripBlock;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleBlock;
import io.github.soundgoodizerfan.feedback.machine.crucible.InsulationBlock;
import io.github.soundgoodizerfan.feedback.machine.damper.DamperBlock;
import io.github.soundgoodizerfan.feedback.machine.firebox.FireboxBlock;
import io.github.soundgoodizerfan.feedback.machine.cog.CogBlock;
import io.github.soundgoodizerfan.feedback.machine.gearbox.GearboxBlock;
import io.github.soundgoodizerfan.feedback.machine.grindingwheel.GrindingWheelBlock;
import io.github.soundgoodizerfan.feedback.machine.crank.HandCrankBlock;
import io.github.soundgoodizerfan.feedback.machine.drillpress.DrillPressBlock;
import io.github.soundgoodizerfan.feedback.machine.hammer.MechanicalHammerBlock;
import io.github.soundgoodizerfan.feedback.machine.lathe.LatheBlock;
import io.github.soundgoodizerfan.feedback.machine.heatexchanger.HeatExchangerBlock;
import io.github.soundgoodizerfan.feedback.machine.linkage.CrankLinkageBlock;
import io.github.soundgoodizerfan.feedback.machine.press.MechanicalPressBlock;
import io.github.soundgoodizerfan.feedback.machine.pulley.PulleyBlock;
import io.github.soundgoodizerfan.feedback.machine.pressurevessel.PressureVesselBlock;
import io.github.soundgoodizerfan.feedback.machine.rollingmill.RollingMillBlock;
import io.github.soundgoodizerfan.feedback.machine.shaft.ShaftBlock;
import io.github.soundgoodizerfan.feedback.machine.shaft.FlywheelBlock;
import io.github.soundgoodizerfan.feedback.machine.condenser.CondenserBlock;
import io.github.soundgoodizerfan.feedback.machine.pump.PumpBlock;
import io.github.soundgoodizerfan.feedback.machine.steamengine.SteamEngineBlock;
import io.github.soundgoodizerfan.feedback.machine.valve.ValveBlock;
import io.github.soundgoodizerfan.feedback.machine.wiredrawer.WireDrawerBlock;
import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselBlock;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselKind;
import io.github.soundgoodizerfan.feedback.machine.waterwheel.WaterWheelBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Feedback.MOD_ID);

    public static final DeferredBlock<ShaftBlock> SHAFT = BLOCKS.register("shaft",
            () -> new ShaftBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    /**
     * `TODO.md`'s "obvious first physical upgrade" -- a bushed, greased Shaft with a lower Su
     * loss constant. Same block class as {@link #SHAFT}, per {@code CogBlock}'s own precedent for
     * a family that differs only in the figures stamped on it.
     */
    public static final DeferredBlock<ShaftBlock> BEARING = BLOCKS.register("bearing",
            () -> new ShaftBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion(),
                    FTuning.BEARING_DRAG_SU_PER_RPM, FTuning.SHAFT_INERTIA));

    /** A shaft with a heavy rim: inertia, and the drag of carrying it. See {@code FlywheelBlock}. */
    public static final DeferredBlock<FlywheelBlock> FLYWHEEL = BLOCKS.register("flywheel",
            () -> new FlywheelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /**
     * Two sizes, one block class. Small meshed with small is 1:1 reversed; large against small
     * doubles the speed and halves the force, and the other way round going the other way.
     */
    public static final DeferredBlock<CogBlock> SMALL_COG = BLOCKS.register("small_cog",
            () -> new CogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion(),
                    1, 5, 4,
                    FTuning.SMALL_COG_DRAG_SU_PER_RPM, FTuning.SMALL_COG_INERTIA));

    public static final DeferredBlock<CogBlock> LARGE_COG = BLOCKS.register("large_cog",
            () -> new CogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion(),
                    2, 8, 4,
                    FTuning.LARGE_COG_DRAG_SU_PER_RPM, FTuning.LARGE_COG_INERTIA));

    public static final DeferredBlock<GearboxBlock> GEARBOX = BLOCKS.register("gearbox",
            () -> new GearboxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<HandCrankBlock> HAND_CRANK = BLOCKS.register("hand_crank",
            () -> new HandCrankBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<WaterWheelBlock> WATER_WHEEL = BLOCKS.register("water_wheel",
            () -> new WaterWheelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<CrankLinkageBlock> CRANK_LINKAGE = BLOCKS.register("crank_linkage",
            () -> new CrankLinkageBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<MechanicalHammerBlock> MECHANICAL_HAMMER = BLOCKS.register("mechanical_hammer",
            () -> new MechanicalHammerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    /**
     * The hammer's own model, minus the hammer -- continuous force from network RPM instead of
     * discrete blows. See {@code WireDrawerBlockEntity}, {@code RollingMillBlockEntity} and
     * {@code MechanicalPressBlockEntity}; all three sit directly on the network like a Gearbox.
     */
    public static final DeferredBlock<WireDrawerBlock> WIRE_DRAWER = BLOCKS.register("wire_drawer",
            () -> new WireDrawerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<RollingMillBlock> ROLLING_MILL = BLOCKS.register("rolling_mill",
            () -> new RollingMillBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<MechanicalPressBlock> MECHANICAL_PRESS = BLOCKS.register("mechanical_press",
            () -> new MechanicalPressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Material subtracted rather than moved -- the removal primitive's first machine. See
     * {@code DrillPressBlockEntity}. */
    public static final DeferredBlock<DrillPressBlock> DRILL_PRESS = BLOCKS.register("drill_press",
            () -> new DrillPressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Same removal primitive as the Drill Press, adding the live-diameter sensor loop and a
     * wearable physical tool bit. See {@code LatheBlockEntity}. */
    public static final DeferredBlock<LatheBlock> LATHE = BLOCKS.register("lathe",
            () -> new LatheBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** The removal primitive's hardest-hitting machine -- the one route through hardened tool
     * steel, and consumed by correct use rather than only by misuse. See {@code
     * GrindingWheelBlockEntity}. */
    public static final DeferredBlock<GrindingWheelBlock> GRINDING_WHEEL = BLOCKS.register("grinding_wheel",
            () -> new GrindingWheelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    /** The stone wheel with diamond grit: rating is the diamond material's hardness, so it cuts
     * crystals (quartz, amethyst) the stone wheel cannot. Same wear rules; grit is the consumable. */
    public static final DeferredBlock<GrindingWheelBlock> DIAMOND_GRINDING_WHEEL = BLOCKS.register("diamond_grinding_wheel",
            () -> new GrindingWheelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIAMOND)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .noOcclusion(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("feedback", "diamond")));

    /**
     * The one genuinely new engine piece in the machine shop -- a second transmission type with
     * its own failure character (slip, not stall). See {@code PulleyBlockEntity}. Two sizes, one
     * block class, the same split {@code CogBlock} already uses.
     */
    public static final DeferredBlock<PulleyBlock> SMALL_PULLEY = BLOCKS.register("small_pulley",
            () -> new PulleyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion(),
                    5, 3));

    public static final DeferredBlock<PulleyBlock> LARGE_PULLEY = BLOCKS.register("large_pulley",
            () -> new PulleyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion(),
                    8, 3));

    public static final DeferredBlock<ClutchBlock> CLUTCH = BLOCKS.register("clutch",
            () -> new ClutchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<OverspeedTripBlock> OVERSPEED_TRIP = BLOCKS.register("overspeed_trip",
            () -> new OverspeedTripBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<TimerBlock> TIMER = BLOCKS.register("timer",
            () -> new TimerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<ControllerBlock> CONTROLLER = BLOCKS.register("controller",
            () -> new ControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<ProgrammerBlock> PROGRAMMER = BLOCKS.register("programmer",
            () -> new ProgrammerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // --- beat 2: heat -----------------------------------------------------------------------

    public static final DeferredBlock<FireboxBlock> FIREBOX = BLOCKS.register("firebox",
            () -> new FireboxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(FireboxBlock.LIT) ? 13 : 0)
                    .noOcclusion()));

    /** A brick pan on a fire that keeps the Tool Table beside it hot (spec 4.2). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.hearth.HearthBlock> HEARTH = BLOCKS.register("hearth",
            () -> new io.github.soundgoodizerfan.feedback.machine.hearth.HearthBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    /** A chest kept cold by ice (food spec 8). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.icebox.IceBoxBlock> ICE_BOX = BLOCKS.register("ice_box",
            () -> new io.github.soundgoodizerfan.feedback.machine.icebox.IceBoxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));

    /** A heat pump that holds an adjacent Ice Box cold (food spec 8). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.refrigeration.RefrigerationUnitBlock> REFRIGERATION_UNIT =
            BLOCKS.register("refrigeration_unit", () -> new io.github.soundgoodizerfan.feedback.machine.refrigeration.RefrigerationUnitBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5f)
                            .sound(SoundType.METAL)));

    /** A rack that dries food (food spec 8). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.rack.DryingRackBlock> DRYING_RACK = BLOCKS.register("drying_rack",
            () -> new io.github.soundgoodizerfan.feedback.machine.rack.DryingRackBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    /** A pot on a fire that cooks a batch (food spec 5). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotBlock> STOCKPOT = BLOCKS.register("stockpot",
            () -> new io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** The campfire (vanilla's is swapped for it, VanillaVesselSwap): food on it warms through the thermal system. No item; the vanilla campfire item places the vanilla block, which swaps. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.campfire.FeedbackCampfireBlock> CAMPFIRE = BLOCKS.register("campfire",
            () -> new io.github.soundgoodizerfan.feedback.machine.campfire.FeedbackCampfireBlock(false,
                    BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CAMPFIRE)));

    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.campfire.FeedbackCampfireBlock> SOUL_CAMPFIRE = BLOCKS.register("soul_campfire",
            () -> new io.github.soundgoodizerfan.feedback.machine.campfire.FeedbackCampfireBlock(true,
                    BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)));

    /** A pan on a fire: right-click with food for the cooking minigame (food spec 6). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.skillet.SkilletBlock> SKILLET = BLOCKS.register("skillet",
            () -> new io.github.soundgoodizerfan.feedback.machine.skillet.SkilletBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Thermal -> Steam. See {@link FTuning}'s {@code --- the boiler ---} section for why this
     * is a separate block from {@link #STEAM_ENGINE} rather than one fused machine. */
    public static final DeferredBlock<BoilerBlock> BOILER = BLOCKS.register("boiler",
            () -> new BoilerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Steam back to water, its heat into its own body. */
    public static final DeferredBlock<CondenserBlock> CONDENSER = BLOCKS.register("condenser",
            () -> new CondenserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f)
                    .sound(SoundType.COPPER)));

    /** Rotation moves fluid, back face to front. */
    public static final DeferredBlock<PumpBlock> PUMP = BLOCKS.register("pump",
            () -> new PumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Steam -> rotation. */
    public static final DeferredBlock<SteamEngineBlock> STEAM_ENGINE = BLOCKS.register("steam_engine",
            () -> new SteamEngineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /**
     * Two sizes, one block class, and the only difference between them is thermal mass.
     * <p>
     * Everything the slice claims about the pair -- that the small one is twitchy and the large
     * one stubborn, that only the large one can satisfy carburizing's heating-rate limit, that a
     * twenty-tick-old reading is actionable on one and useless on the other -- falls out of that
     * single number. See {@link io.github.soundgoodizerfan.feedback.core.FTuning#CRUCIBLE_SMALL_MASS}.
     */
    public static final DeferredBlock<CrucibleBlock> SMALL_CRUCIBLE = BLOCKS.register("small_crucible",
            () -> new CrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .noOcclusion(), FTuning.CRUCIBLE_SMALL_MASS));

    public static final DeferredBlock<CrucibleBlock> LARGE_CRUCIBLE = BLOCKS.register("large_crucible",
            () -> new CrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .noOcclusion(), FTuning.CRUCIBLE_LARGE_MASS));

    /** No block entity and no behaviour. A vessel counts these; insulation itself does nothing. */
    public static final DeferredBlock<InsulationBlock> INSULATION = BLOCKS.register("insulation",
            () -> new InsulationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(1.5f)
                    .sound(SoundType.WOOL)));

    /**
     * Natural graphite, the first deposit (mechanics §9). Soft, so it digs faster than coal ore's
     * 3.0; drops come from its loot table, which names the graphite item.
     */
    public static final DeferredBlock<Block> GRAPHITE_ORE = BLOCKS.register("graphite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 3.0f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> DEEPSLATE_GRAPHITE_ORE = BLOCKS.register("deepslate_graphite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));

    // Vanilla's four overworld metals' ores, replaced wholesale (mechanics §9.6). Strength and
    // sound are the vanilla block's own, so a pickaxe digs them as before; the loot tables
    // mirror vanilla's, and the tool tier is the needs_*_tool tag in FormData.BlockTagsGen.

    /** Coal also drops 0-2 experience, as vanilla's does. */
    public static final DeferredBlock<Block> COAL_ORE = ore("coal_ore", false, true);
    public static final DeferredBlock<Block> DEEPSLATE_COAL_ORE = ore("deepslate_coal_ore", true, true);
    public static final DeferredBlock<Block> IRON_ORE = ore("iron_ore", false, false);
    public static final DeferredBlock<Block> DEEPSLATE_IRON_ORE = ore("deepslate_iron_ore", true, false);
    public static final DeferredBlock<Block> COPPER_ORE = ore("copper_ore", false, false);
    public static final DeferredBlock<Block> DEEPSLATE_COPPER_ORE = ore("deepslate_copper_ore", true, false);
    public static final DeferredBlock<Block> GOLD_ORE = ore("gold_ore", false, false);
    public static final DeferredBlock<Block> DEEPSLATE_GOLD_ORE = ore("deepslate_gold_ore", true, false);

    // Batch 2 (mechanics §9.6): the rest of the overworld's ores and the Nether's two. Experience
    // ranges are vanilla's own.
    public static final DeferredBlock<Block> LAPIS_ORE = ore("lapis_ore", false, xp(2, 5));
    public static final DeferredBlock<Block> DEEPSLATE_LAPIS_ORE = ore("deepslate_lapis_ore", true, xp(2, 5));
    public static final DeferredBlock<Block> DIAMOND_ORE = ore("diamond_ore", false, xp(3, 7));
    public static final DeferredBlock<Block> DEEPSLATE_DIAMOND_ORE = ore("deepslate_diamond_ore", true, xp(3, 7));
    public static final DeferredBlock<Block> EMERALD_ORE = ore("emerald_ore", false, xp(3, 7));
    public static final DeferredBlock<Block> DEEPSLATE_EMERALD_ORE = ore("deepslate_emerald_ore", true, xp(3, 7));

    /**
     * Redstone ore is vanilla's own class (lit when touched or mined, glowing while lit, its
     * experience dropped by the class), so the behaviour is not restated.
     */
    public static final DeferredBlock<Block> REDSTONE_ORE = BLOCKS.register("redstone_ore",
            () -> redstoneOre(false));
    public static final DeferredBlock<Block> DEEPSLATE_REDSTONE_ORE = BLOCKS.register("deepslate_redstone_ore",
            () -> redstoneOre(true));

    /** Nether ores have no deepslate kind: netherrack is their one host. */
    public static final DeferredBlock<Block> NETHER_GOLD_ORE = BLOCKS.register("nether_gold_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(xp(0, 1),
                    BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).strength(3.0f, 3.0f)
                            .sound(SoundType.NETHER_GOLD_ORE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> NETHER_QUARTZ_ORE = BLOCKS.register("nether_quartz_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(xp(2, 5),
                    BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).strength(3.0f, 3.0f)
                            .sound(SoundType.NETHER_ORE).requiresCorrectToolForDrops()));

    private static net.minecraft.util.valueproviders.UniformInt xp(int min, int max) {
        return net.minecraft.util.valueproviders.UniformInt.of(min, max);
    }

    private static Block redstoneOre(boolean deepslate) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(deepslate ? MapColor.DEEPSLATE : MapColor.STONE)
                .strength(deepslate ? 4.5f : 3.0f, 3.0f)
                .requiresCorrectToolForDrops()
                .randomTicks()
                .lightLevel(state -> state.getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT) ? 9 : 0)
                .emissiveRendering((state, level, pos) -> state.getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT));
        if (deepslate)
            properties.sound(SoundType.DEEPSLATE);
        return new net.minecraft.world.level.block.RedStoneOreBlock(properties);
    }

    // Tin, zinc and lead (mechanics 9.6): stone-tool ores, vanilla-copper strength, raw_<metal> drops.
    public static final DeferredBlock<Block> TIN_ORE = ore("tin_ore", false, false);
    public static final DeferredBlock<Block> DEEPSLATE_TIN_ORE = ore("deepslate_tin_ore", true, false);
    public static final DeferredBlock<Block> ZINC_ORE = ore("zinc_ore", false, false);
    public static final DeferredBlock<Block> DEEPSLATE_ZINC_ORE = ore("deepslate_zinc_ore", true, false);
    public static final DeferredBlock<Block> LEAD_ORE = ore("lead_ore", false, false);
    public static final DeferredBlock<Block> DEEPSLATE_LEAD_ORE = ore("deepslate_lead_ore", true, false);

    // Crystal and clay deposits (mechanics 9): one block each, stone host only. Galena and pyrite
    // are the cat's whisker's crystals; kaolinite is the firebrick clay, soft enough to dig by hand.
    public static final DeferredBlock<Block> GALENA_ORE = ore("galena_ore", false, false);
    public static final DeferredBlock<Block> PYRITE_ORE = ore("pyrite_ore", false, false);
    public static final DeferredBlock<Block> KAOLINITE_DEPOSIT = BLOCKS.register("kaolinite_deposit",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(1.0f)
                    .sound(SoundType.GRAVEL)));

    /** A chest lined with lead: it holds radiation in (mechanics 9.8). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.leadchest.LeadChestBlock> LEAD_CHEST =
            BLOCKS.register("lead_chest", () -> new io.github.soundgoodizerfan.feedback.machine.leadchest.LeadChestBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 6f).sound(SoundType.METAL)));

    private static DeferredBlock<Block> ore(String name, boolean deepslate, boolean experience) {
        return ore(name, deepslate, experience ? xp(0, 2) : null);
    }

    /**
     * One vanilla-strength ore block: 3.0 / 3.0 in stone, 4.5 / 3.0 and deepslate's sound in
     * deepslate, tool required for drops. Experience (null for none) only where vanilla drops it.
     */
    private static DeferredBlock<Block> ore(String name, boolean deepslate,
                                            net.minecraft.util.valueproviders.UniformInt experience) {
        return BLOCKS.register(name, () -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                    .mapColor(deepslate ? MapColor.DEEPSLATE : MapColor.STONE)
                    .strength(deepslate ? 4.5f : 3.0f, 3.0f)
                    .requiresCorrectToolForDrops();
            if (deepslate)
                properties.sound(SoundType.DEEPSLATE);
            return experience != null
                    ? new net.minecraft.world.level.block.DropExperienceBlock(experience, properties)
                    : new Block(properties);
        });
    }

    /**
     * The conductor, of any metal (mechanics 6, slice 4): one block, the material in its block
     * entity. Its figures come from that material's file.
     */
    public static final DeferredBlock<CableBlock> CABLE = BLOCKS.register("cable",
            () -> new CableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .noCollission()));

    /** A post of live wire you cannot walk through (docs/spec_electrified_fence.md). Cable's block entity. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.cable.FenceBlock> FENCE = BLOCKS.register("fence",
            () -> new io.github.soundgoodizerfan.feedback.machine.cable.FenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    /** Zinc against copper: the first chemical-to-electrical boundary (mechanics 6.8). Voltage is read from the electrodes' materials. */
    public static final DeferredBlock<GalvanicCellBlock> GALVANIC_CELL = BLOCKS.register("galvanic_cell",
            () -> new GalvanicCellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5f)
                    .sound(SoundType.COPPER), Feedback.id("zinc"), Feedback.id("copper")));

    /**
     * Cells in series come as a pre-built pack per band, never as a stack of any size (user ruling, 2026-10-23): this
     * one is {@link FTuning#GALVANIC_PACK_CELLS} zinc-copper cells, 5.5 V, an MV source.
     */
    public static final DeferredBlock<GalvanicCellBlock> GALVANIC_CELL_PACK = BLOCKS.register("galvanic_cell_pack",
            () -> new GalvanicCellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5f)
                    .sound(SoundType.COPPER), Feedback.id("zinc"), Feedback.id("copper"), FTuning.GALVANIC_PACK_CELLS));

    /** The first electrical consumer. See {@code ElectromagnetBlockEntity}. */
    public static final DeferredBlock<ElectromagnetBlock> ELECTROMAGNET = BLOCKS.register("electromagnet",
            () -> new ElectromagnetBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f)
                    .sound(SoundType.METAL)));

    /** Eu to chemistry: electrorefining. See {@code ElectrolyticCellBlockEntity}. */
    public static final DeferredBlock<ElectrolyticCellBlock> ELECTROLYTIC_CELL = BLOCKS.register("electrolytic_cell",
            () -> new ElectrolyticCellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f)
                    .sound(SoundType.STONE)));

    /** Mechanical to Eu: a coil on a shaft. See {@code DynamoBlockEntity}. */
    public static final DeferredBlock<DynamoBlock> DYNAMO = BLOCKS.register("dynamo",
            () -> new DynamoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f)
                    .sound(SoundType.METAL)));

    /** Eu to Mechanical: the dynamo run backwards. See {@code MotorBlockEntity}. */
    public static final DeferredBlock<MotorBlock> MOTOR = BLOCKS.register("motor",
            () -> new MotorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f)
                    .sound(SoundType.METAL)));

    /** Eu to Thermal: a heating element, one block per element metal. See {@code CoilBlockEntity}. */
    public static final DeferredBlock<CoilBlock> COPPER_COIL = BLOCKS.register("copper_coil",
            () -> new CoilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f)
                    .sound(SoundType.METAL), Feedback.id("copper")));
    public static final DeferredBlock<CoilBlock> IRON_COIL = BLOCKS.register("iron_coil",
            () -> new CoilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL), Feedback.id("iron")));

    /** Electricity's stop: opens a line, arcs under load, trips. See {@code BreakerBlockEntity}. */
    public static final DeferredBlock<BreakerBlock> BREAKER = BLOCKS.register("breaker",
            () -> new BreakerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f)
                    .sound(SoundType.METAL)));

    /** Redstone coherer: a spark welds it into a wire, a tap frees it. See {@code CohererBlockEntity}. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.detector.CohererBlock> COHERER = BLOCKS.register("coherer",
            () -> new io.github.soundgoodizerfan.feedback.machine.detector.CohererBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0f)
                    .sound(SoundType.GLASS)));

    /** Cat's-whisker detector. See {@code WhiskerBlockEntity}. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.detector.WhiskerBlock> WHISKER = BLOCKS.register("whisker",
            () -> new io.github.soundgoodizerfan.feedback.machine.detector.WhiskerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f)
                    .sound(SoundType.METAL)));

    /** Boards reflowed in a batch on a fire. See {@code ReflowOvenBlockEntity}. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.oven.ReflowOvenBlock> REFLOW_OVEN = BLOCKS.register("reflow_oven",
            () -> new io.github.soundgoodizerfan.feedback.machine.oven.ReflowOvenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    /** Vacuum glass coated with glowstone phosphor. See {@code PhosphorScreenBlockEntity}. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.phosphor.PhosphorScreenBlock> PHOSPHOR_SCREEN = BLOCKS.register("phosphor_screen",
            () -> new io.github.soundgoodizerfan.feedback.machine.phosphor.PhosphorScreenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.5f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(io.github.soundgoodizerfan.feedback.machine.phosphor.PhosphorScreenBlock.GLOW) * 5)));

    /** Shows a cable's current on a phosphor screen. See {@code OscilloscopeBlockEntity}. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.phosphor.OscilloscopeBlock> OSCILLOSCOPE = BLOCKS.register("oscilloscope",
            () -> new io.github.soundgoodizerfan.feedback.machine.phosphor.OscilloscopeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f)
                    .sound(SoundType.METAL)));

    /** Contacts moved by an adjacent electromagnet. See {@code RelayBlockEntity}. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.relay.RelayBlock> RELAY = BLOCKS.register("relay",
            () -> new io.github.soundgoodizerfan.feedback.machine.relay.RelayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.5f)
                    .sound(SoundType.METAL)));

    /** A holder for one sacrificial fine wire. See {@code FuseBlockEntity}. */
    public static final DeferredBlock<FuseBlock> FUSE = BLOCKS.register("fuse",
            () -> new FuseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(1.0f)
                    .sound(SoundType.STONE)));

    /** Holds charge and gives it back in a burst. See {@code LeydenJarBlockEntity}. */
    public static final DeferredBlock<LeydenJarBlock> LEYDEN_JAR = BLOCKS.register("leyden_jar",
            () -> new LeydenJarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(0.5f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    /** Charges jar rounds off a wire. See {@code ChargerBlockEntity}. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.charger.ChargerBlock> CHARGER =
            BLOCKS.register("charger", () -> new io.github.soundgoodizerfan.feedback.machine.charger.ChargerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(1.0f)
                            .sound(SoundType.COPPER)
                            .noOcclusion()));

    /** A hand-turned pulse source. Holds no charge. */
    public static final DeferredBlock<ElectrostaticGeneratorBlock> ELECTROSTATIC_GENERATOR =
            BLOCKS.register("electrostatic_generator",
                    () -> new ElectrostaticGeneratorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(1.5f)
                            .sound(SoundType.COPPER)));

    /** The Bellows' opposite -- a vent, stacked against a crucible the same way insulation is. */
    public static final DeferredBlock<DamperBlock> DAMPER = BLOCKS.register("damper",
            () -> new DamperBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** A manual or controller-driven cutoff on an air line -- {@code Switchable}/{@code Blown},
     * nothing new. See {@code ValveBlockEntity}. */
    public static final DeferredBlock<ValveBlock> VALVE = BLOCKS.register("valve",
            () -> new ValveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<BellowsBlock> BELLOWS = BLOCKS.register("bellows",
            () -> new BellowsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<BimetallicStripBlock> BIMETALLIC_STRIP = BLOCKS.register("bimetallic_strip",
            () -> new BimetallicStripBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    /**
     * An independent block, not a reworked vanilla one -- see {@code ThermalVesselBlockEntity}'s
     * class doc for why. Same strength/sound as vanilla's own furnace family; the texture is
     * vanilla's too (all art is placeholder), only the block and everything behind it are ours.
     */
    public static final DeferredBlock<ThermalVesselBlock> FURNACE = BLOCKS.register("furnace",
            () -> new ThermalVesselBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(ThermalVesselBlock.LIT) ? 13 : 0),
                    VesselKind.FURNACE));

    public static final DeferredBlock<ThermalVesselBlock> SMOKER = BLOCKS.register("smoker",
            () -> new ThermalVesselBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(ThermalVesselBlock.LIT) ? 13 : 0),
                    VesselKind.SMOKER));

    public static final DeferredBlock<ThermalVesselBlock> BLAST_FURNACE = BLOCKS.register("blast_furnace",
            () -> new ThermalVesselBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ThermalVesselBlock.LIT) ? 13 : 0),
                    VesselKind.BLAST_FURNACE));

    /** The steel-tier upgrade to the plain Furnace's mediocrity -- steadier, lower-loss, and the
     * one appliance-family vessel that actually carries a thermowell. See {@code
     * FTuning#IMPROVED_FURNACE_MASS}. */
    public static final DeferredBlock<ThermalVesselBlock> IMPROVED_FURNACE = BLOCKS.register("improved_furnace",
            () -> new ThermalVesselBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ThermalVesselBlock.LIT) ? 13 : 0),
                    VesselKind.IMPROVED_FURNACE));

    /** Cheap, brick-built, and hard-gated well under steel's floor -- see {@code
     * FTuning#KILN_CEILING_TU}. */
    public static final DeferredBlock<ThermalVesselBlock> KILN = BLOCKS.register("kiln",
            () -> new ThermalVesselBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(ThermalVesselBlock.LIT) ? 13 : 0),
                    VesselKind.KILN));

    /** A high-mass appliance route to a controlled slow cool -- see {@code
     * FTuning#ANNEALING_FURNACE_CEILING_TU} for why it cannot instead ruin the batch. */
    public static final DeferredBlock<ThermalVesselBlock> ANNEALING_FURNACE = BLOCKS.register("annealing_furnace",
            () -> new ThermalVesselBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ThermalVesselBlock.LIT) ? 13 : 0),
                    VesselKind.ANNEALING_FURNACE));

    public static final DeferredBlock<HeatExchangerBlock> HEAT_EXCHANGER = BLOCKS.register("heat_exchanger",
            () -> new HeatExchangerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<PressureVesselBlock> PRESSURE_VESSEL = BLOCKS.register("pressure_vessel",
            () -> new PressureVesselBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f)
                    .sound(SoundType.METAL)));

    /**
     * A controller that needs no power -- a development cheat like {@code DebugHelmetItem}, not
     * real content. See {@code FItems.CREATIVE_CONTROLLER} for why it stays out of the mod's tab.
     */
    public static final DeferredBlock<CreativeControllerBlock> CREATIVE_CONTROLLER = BLOCKS.register("creative_controller",
            () -> new CreativeControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Creative sources, one per energy type -- operator tab only. See {@code CreativeSource}. */
    public static final DeferredBlock<CreativeSourceBlock> CREATIVE_HEAT = creativeSource("creative_heat",
            CreativeHeatBlockEntity::new, MapColor.FIRE);
    public static final DeferredBlock<CreativeSourceBlock> CREATIVE_ROTATION = BLOCKS.register("creative_rotation",
            () -> new io.github.soundgoodizerfan.feedback.machine.creative.CreativeRotationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(-1f, 3600000f)
                    .sound(SoundType.METAL)));
    public static final DeferredBlock<CreativeSourceBlock> CREATIVE_XP = creativeSource("creative_xp",
            io.github.soundgoodizerfan.feedback.machine.creative.CreativeXpBlockEntity::new, MapColor.EMERALD);
    public static final DeferredBlock<CreativeSourceBlock> CREATIVE_ELECTRIC = creativeSource("creative_electric",
            CreativeElectricBlockEntity::new, MapColor.GOLD);

    // --- petrochemistry, phase 1 ---------------------------------------------------------------

    /** Carries a fluid run; see {@code core/fluid}. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlock> PIPE = BLOCKS.register("pipe",
            () -> new io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredBlock<CreativeSourceBlock> CREATIVE_FLUID = BLOCKS.register("creative_fluid",
            () -> new io.github.soundgoodizerfan.feedback.machine.creative.CreativeFluidBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .strength(-1f, 3600000f)
                    .sound(SoundType.METAL), io.github.soundgoodizerfan.feedback.machine.creative.CreativeFluidBlockEntity::new));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.derrick.DerrickBaseBlock> DERRICK_BASE = BLOCKS.register("derrick_base",
            () -> new io.github.soundgoodizerfan.feedback.machine.derrick.DerrickBaseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.derrick.DerrickCasingBlock> DERRICK_CASING = BLOCKS.register("derrick_casing",
            () -> new io.github.soundgoodizerfan.feedback.machine.derrick.DerrickCasingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.derrick.WellheadBlock> WELLHEAD = BLOCKS.register("wellhead",
            () -> new io.github.soundgoodizerfan.feedback.machine.derrick.WellheadBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.geology.SeepBlock> SEEP = BLOCKS.register("seep",
            () -> new io.github.soundgoodizerfan.feedback.geology.SeepBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.2f)
                    .sound(SoundType.MUD)
                    .noCollission()
                    .noOcclusion()
                    .randomTicks()));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.geology.CrudePoolBlock> CRUDE_POOL = BLOCKS.register("crude_pool",
            () -> new io.github.soundgoodizerfan.feedback.geology.CrudePoolBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.1f)
                    .sound(SoundType.MUD)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()
                    .randomTicks()));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.basin.CastingBasinBlock> CASTING_BASIN = BLOCKS.register("casting_basin",
            () -> new io.github.soundgoodizerfan.feedback.machine.basin.CastingBasinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.column.ReboilerBlock> REBOILER = BLOCKS.register("reboiler",
            () -> new io.github.soundgoodizerfan.feedback.machine.column.ReboilerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f)
                    .sound(SoundType.METAL)));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.column.TrayBlock> TRAY_BUBBLE_CAP = tray("tray_bubble_cap", io.github.soundgoodizerfan.feedback.machine.column.TrayType.BUBBLE_CAP);
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.column.TrayBlock> TRAY_SIEVE = tray("tray_sieve", io.github.soundgoodizerfan.feedback.machine.column.TrayType.SIEVE);
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.column.TrayBlock> TRAY_PACKED = tray("tray_packed", io.github.soundgoodizerfan.feedback.machine.column.TrayType.PACKED);

    private static DeferredBlock<io.github.soundgoodizerfan.feedback.machine.column.TrayBlock> tray(String name, io.github.soundgoodizerfan.feedback.machine.column.TrayType type) {
        return BLOCKS.register(name, () -> new io.github.soundgoodizerfan.feedback.machine.column.TrayBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0f)
                .sound(SoundType.METAL), type));
    }

    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.reactor.ReactorBlock> REACTOR = BLOCKS.register("reactor",
            () -> new io.github.soundgoodizerfan.feedback.machine.reactor.ReactorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f)
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.turbine.GasTurbineBlock> GAS_TURBINE = BLOCKS.register("gas_turbine",
            () -> new io.github.soundgoodizerfan.feedback.machine.turbine.GasTurbineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.machine.flare.FlareBlock> FLARE = BLOCKS.register("flare",
            () -> new io.github.soundgoodizerfan.feedback.machine.flare.FlareBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 12 : 0)));

    private static DeferredBlock<CreativeSourceBlock> creativeSource(String name,
            java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, net.minecraft.world.level.block.entity.BlockEntity> entity, MapColor color) {
        return BLOCKS.register(name, () -> new CreativeSourceBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(-1f, 3600000f)
                .sound(SoundType.METAL), entity));
    }

    private FBlocks() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
