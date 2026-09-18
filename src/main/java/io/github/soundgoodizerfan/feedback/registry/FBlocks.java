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
import io.github.soundgoodizerfan.feedback.control.debug.DebugControllerBlock;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlock;
import io.github.soundgoodizerfan.feedback.control.timer.TimerBlock;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.bellows.BellowsBlock;
import io.github.soundgoodizerfan.feedback.machine.boiler.BoilerBlock;
import io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleBlock;
import io.github.soundgoodizerfan.feedback.machine.crucible.InsulationBlock;
import io.github.soundgoodizerfan.feedback.machine.damper.DamperBlock;
import io.github.soundgoodizerfan.feedback.machine.firebox.FireboxBlock;
import io.github.soundgoodizerfan.feedback.machine.cog.CogBlock;
import io.github.soundgoodizerfan.feedback.machine.gearbox.GearboxBlock;
import io.github.soundgoodizerfan.feedback.machine.crank.HandCrankBlock;
import io.github.soundgoodizerfan.feedback.machine.hammer.MechanicalHammerBlock;
import io.github.soundgoodizerfan.feedback.machine.heatexchanger.HeatExchangerBlock;
import io.github.soundgoodizerfan.feedback.machine.linkage.CrankLinkageBlock;
import io.github.soundgoodizerfan.feedback.machine.pressurevessel.PressureVesselBlock;
import io.github.soundgoodizerfan.feedback.machine.shaft.ShaftBlock;
import io.github.soundgoodizerfan.feedback.machine.steamengine.SteamEngineBlock;
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

    public static final DeferredBlock<ClutchBlock> CLUTCH = BLOCKS.register("clutch",
            () -> new ClutchBlock(BlockBehaviour.Properties.of()
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

    /** Thermal -> Steam. See {@link FTuning}'s {@code --- the boiler ---} section for why this
     * is a separate block from {@link #STEAM_ENGINE} rather than one fused machine. */
    public static final DeferredBlock<BoilerBlock> BOILER = BLOCKS.register("boiler",
            () -> new BoilerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
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
                    .noOcclusion()));

    public static final DeferredBlock<CrucibleBlock> LARGE_CRUCIBLE = BLOCKS.register("large_crucible",
            () -> new CrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    /** No block entity and no behaviour. A vessel counts these; insulation itself does nothing. */
    public static final DeferredBlock<InsulationBlock> INSULATION = BLOCKS.register("insulation",
            () -> new InsulationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(1.5f)
                    .sound(SoundType.WOOL)));

    /** The Bellows' opposite -- a vent, stacked against a crucible the same way insulation is. */
    public static final DeferredBlock<DamperBlock> DAMPER = BLOCKS.register("damper",
            () -> new DamperBlock(BlockBehaviour.Properties.of()
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
     * The data-link tech demo's sink -- a development cheat like {@code DebugHelmetItem}, not
     * real content. See {@code FItems.DEBUG_CONTROLLER} for why it stays out of the mod's tab.
     */
    public static final DeferredBlock<DebugControllerBlock> DEBUG_CONTROLLER = BLOCKS.register("debug_controller",
            () -> new DebugControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    private FBlocks() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
