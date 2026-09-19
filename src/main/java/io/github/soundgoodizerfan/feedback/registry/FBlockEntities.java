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
import io.github.soundgoodizerfan.feedback.control.bimetallic.BimetallicStripBlockEntity;
import io.github.soundgoodizerfan.feedback.control.controller.ControllerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.debug.DebugControllerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.timer.TimerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.bellows.BellowsBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.boiler.BoilerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.damper.DamperBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.firebox.FireboxBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.cog.CogBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.crank.HandCrankBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.drillpress.DrillPressBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.gearbox.GearboxBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.grindingwheel.GrindingWheelBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.hammer.MechanicalHammerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.lathe.LatheBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.heatexchanger.HeatExchangerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.linkage.CrankLinkageBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.press.MechanicalPressBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.pulley.PulleyBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.piston.PistonBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.pressurevessel.PressureVesselBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.rollingmill.RollingMillBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.shaft.ShaftBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.steamengine.SteamEngineBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.valve.ValveBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.wiredrawer.WireDrawerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselBlock;
import io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselKind;
import io.github.soundgoodizerfan.feedback.machine.waterwheel.WaterWheelBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Feedback.MOD_ID);

    /** One type for the Shaft and the Bearing -- the difference is entirely in the block. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShaftBlockEntity>> SHAFT =
            BLOCK_ENTITIES.register("shaft", () -> BlockEntityType.Builder
                    .of(ShaftBlockEntity::new, FBlocks.SHAFT.get(), FBlocks.BEARING.get())
                    .build(null));

    /** One type for both cog sizes: the difference is entirely in the block, not in the state. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CogBlockEntity>> COG =
            BLOCK_ENTITIES.register("cog", () -> BlockEntityType.Builder
                    .of(CogBlockEntity::new, FBlocks.SMALL_COG.get(), FBlocks.LARGE_COG.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GearboxBlockEntity>> GEARBOX =
            BLOCK_ENTITIES.register("gearbox", () -> BlockEntityType.Builder
                    .of(GearboxBlockEntity::new, FBlocks.GEARBOX.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HandCrankBlockEntity>> HAND_CRANK =
            BLOCK_ENTITIES.register("hand_crank", () -> BlockEntityType.Builder
                    .of(HandCrankBlockEntity::new, FBlocks.HAND_CRANK.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterWheelBlockEntity>> WATER_WHEEL =
            BLOCK_ENTITIES.register("water_wheel", () -> BlockEntityType.Builder
                    .of(WaterWheelBlockEntity::new, FBlocks.WATER_WHEEL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrankLinkageBlockEntity>> CRANK_LINKAGE =
            BLOCK_ENTITIES.register("crank_linkage", () -> BlockEntityType.Builder
                    .of(CrankLinkageBlockEntity::new, FBlocks.CRANK_LINKAGE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalHammerBlockEntity>> MECHANICAL_HAMMER =
            BLOCK_ENTITIES.register("mechanical_hammer", () -> BlockEntityType.Builder
                    .of(MechanicalHammerBlockEntity::new, FBlocks.MECHANICAL_HAMMER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WireDrawerBlockEntity>> WIRE_DRAWER =
            BLOCK_ENTITIES.register("wire_drawer", () -> BlockEntityType.Builder
                    .of(WireDrawerBlockEntity::new, FBlocks.WIRE_DRAWER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RollingMillBlockEntity>> ROLLING_MILL =
            BLOCK_ENTITIES.register("rolling_mill", () -> BlockEntityType.Builder
                    .of(RollingMillBlockEntity::new, FBlocks.ROLLING_MILL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPressBlockEntity>> MECHANICAL_PRESS =
            BLOCK_ENTITIES.register("mechanical_press", () -> BlockEntityType.Builder
                    .of(MechanicalPressBlockEntity::new, FBlocks.MECHANICAL_PRESS.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PistonBlockEntity>> PISTON =
            BLOCK_ENTITIES.register("piston", () -> BlockEntityType.Builder
                    .of(PistonBlockEntity::new, FBlocks.PISTON.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrillPressBlockEntity>> DRILL_PRESS =
            BLOCK_ENTITIES.register("drill_press", () -> BlockEntityType.Builder
                    .of(DrillPressBlockEntity::new, FBlocks.DRILL_PRESS.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LatheBlockEntity>> LATHE =
            BLOCK_ENTITIES.register("lathe", () -> BlockEntityType.Builder
                    .of(LatheBlockEntity::new, FBlocks.LATHE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrindingWheelBlockEntity>> GRINDING_WHEEL =
            BLOCK_ENTITIES.register("grinding_wheel", () -> BlockEntityType.Builder
                    .of(GrindingWheelBlockEntity::new, FBlocks.GRINDING_WHEEL.get())
                    .build(null));

    /** One type for both pulley sizes, same as the cogs. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulleyBlockEntity>> PULLEY =
            BLOCK_ENTITIES.register("pulley", () -> BlockEntityType.Builder
                    .of(PulleyBlockEntity::new, FBlocks.SMALL_PULLEY.get(), FBlocks.LARGE_PULLEY.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClutchBlockEntity>> CLUTCH =
            BLOCK_ENTITIES.register("clutch", () -> BlockEntityType.Builder
                    .of(ClutchBlockEntity::new, FBlocks.CLUTCH.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TimerBlockEntity>> TIMER =
            BLOCK_ENTITIES.register("timer", () -> BlockEntityType.Builder
                    .of(TimerBlockEntity::new, FBlocks.TIMER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControllerBlockEntity>> CONTROLLER =
            BLOCK_ENTITIES.register("controller", () -> BlockEntityType.Builder
                    .of(ControllerBlockEntity::new, FBlocks.CONTROLLER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProgrammerBlockEntity>> PROGRAMMER =
            BLOCK_ENTITIES.register("programmer", () -> BlockEntityType.Builder
                    .of(ProgrammerBlockEntity::new, FBlocks.PROGRAMMER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireboxBlockEntity>> FIREBOX =
            BLOCK_ENTITIES.register("firebox", () -> BlockEntityType.Builder
                    .of(FireboxBlockEntity::new, FBlocks.FIREBOX.get())
                    .build(null));

    /** One type for both crucible sizes, for the same reason the cogs share one: size is the block. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register("crucible", () -> BlockEntityType.Builder
                    .of(CrucibleBlockEntity::new, FBlocks.SMALL_CRUCIBLE.get(), FBlocks.LARGE_CRUCIBLE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ValveBlockEntity>> VALVE =
            BLOCK_ENTITIES.register("valve", () -> BlockEntityType.Builder
                    .of(ValveBlockEntity::new, FBlocks.VALVE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BellowsBlockEntity>> BELLOWS =
            BLOCK_ENTITIES.register("bellows", () -> BlockEntityType.Builder
                    .of(BellowsBlockEntity::new, FBlocks.BELLOWS.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BimetallicStripBlockEntity>> BIMETALLIC_STRIP =
            BLOCK_ENTITIES.register("bimetallic_strip", () -> BlockEntityType.Builder
                    .of(BimetallicStripBlockEntity::new, FBlocks.BIMETALLIC_STRIP.get())
                    .build(null));

    /** The Bellows' opposite -- see {@code DamperBlockEntity}'s own doc. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DamperBlockEntity>> DAMPER =
            BLOCK_ENTITIES.register("damper", () -> BlockEntityType.Builder
                    .of(DamperBlockEntity::new, FBlocks.DAMPER.get())
                    .build(null));

    /** Thermal -> Steam. See {@code FTuning}'s {@code --- the boiler ---} section for why this
     * is not the same block as {@link #STEAM_ENGINE}. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BOILER =
            BLOCK_ENTITIES.register("boiler", () -> BlockEntityType.Builder
                    .of(BoilerBlockEntity::new, FBlocks.BOILER.get())
                    .build(null));

    /** Steam -> rotation. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE =
            BLOCK_ENTITIES.register("steam_engine", () -> BlockEntityType.Builder
                    .of(SteamEngineBlockEntity::new, FBlocks.STEAM_ENGINE.get())
                    .build(null));

    /**
     * One type for all three vessels -- {@link VesselKind} is read off the block at the
     * position, not the type, the same way the crucible's two sizes share one type.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermalVesselBlockEntity>> THERMAL_VESSEL =
            BLOCK_ENTITIES.register("thermal_vessel", () -> BlockEntityType.Builder
                    .of((pos, state) -> new ThermalVesselBlockEntity(pos, state,
                                    state.getBlock() instanceof ThermalVesselBlock block
                                            ? block.getKind() : VesselKind.FURNACE),
                            FBlocks.FURNACE.get(), FBlocks.SMOKER.get(), FBlocks.BLAST_FURNACE.get(),
                            FBlocks.KILN.get(), FBlocks.ANNEALING_FURNACE.get(), FBlocks.IMPROVED_FURNACE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatExchangerBlockEntity>> HEAT_EXCHANGER =
            BLOCK_ENTITIES.register("heat_exchanger", () -> BlockEntityType.Builder
                    .of(HeatExchangerBlockEntity::new, FBlocks.HEAT_EXCHANGER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureVesselBlockEntity>> PRESSURE_VESSEL =
            BLOCK_ENTITIES.register("pressure_vessel", () -> BlockEntityType.Builder
                    .of(PressureVesselBlockEntity::new, FBlocks.PRESSURE_VESSEL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DebugControllerBlockEntity>> DEBUG_CONTROLLER =
            BLOCK_ENTITIES.register("debug_controller", () -> BlockEntityType.Builder
                    .of(DebugControllerBlockEntity::new, FBlocks.DEBUG_CONTROLLER.get())
                    .build(null));

    private FBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
