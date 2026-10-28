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
import io.github.soundgoodizerfan.feedback.vehicle.LiftBlock;
import io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.LiftItem;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlock;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlueprintItem;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineItem;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandBlock;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandBlockEntity;
import net.minecraft.nbt.CompoundTag;
import io.github.soundgoodizerfan.feedback.vehicle.seat.DriverSeatBlock;
import io.github.soundgoodizerfan.feedback.vehicle.seat.DriverSeatBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.seat.DriverSeatEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.ChassisCellBlock;
import io.github.soundgoodizerfan.feedback.vehicle.frame.ChassisCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlock;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.PivotItem;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderMode;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderPicks;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelection;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Vehicle blocks and items ({@code docs/feedback_vehicles_spec.md}). Their own registers rather than
 * rows in {@link FBlocks}/{@link FItems}: the vehicle system is being merged in from two other
 * mods and is large enough to want one place of its own -- still under {@code registry/}.
 */
public class FVehicles {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Feedback.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Feedback.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Feedback.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Feedback.MOD_ID);

    /** The stand a vehicle is built on and lifted from (spec section 4). */
    public static final DeferredBlock<LiftBlock> LIFT = BLOCKS.register("lift",
            () -> new LiftBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)));

    /** Places a lift, and picks a whole creation up into itself (spec section 4). */
    public static final DeferredItem<LiftItem> LIFT_ITEM = ITEMS.register("lift",
            () -> new LiftItem(LIFT.get(), new Item.Properties()));

    /**
     * The creation a Lift item is carrying: a vanilla structure template plus where it sits on the
     * lift. Persistent only -- no stream codec of its own, so it syncs through its codec.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> CARRIED_CREATION =
            COMPONENTS.register("carried_creation", () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiftBlockEntity>> LIFT_ENTITY =
            BLOCK_ENTITIES.register("lift", () -> BlockEntityType.Builder.of(LiftBlockEntity::new, LIFT.get()).build(null));

    /** The anchor a chassis is welded around (spec section 4). */
    public static final DeferredBlock<FrameBlock> FRAME = BLOCKS.register("chassis_frame",
            () -> new FrameBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> FRAME_ITEM = ITEMS.registerSimpleBlockItem(FRAME);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FrameBlockEntity>> FRAME_ENTITY =
            BLOCK_ENTITIES.register("chassis_frame",
                    () -> BlockEntityType.Builder.of(FrameBlockEntity::new, FRAME.get()).build(null));

    /**
     * A hidden block of an assembled frame (spec section 4, <em>cells, again</em>). Placed only
     * by assembly; not broken by hand. Dynamic shape: its fine shape is per position, and a
     * state-cached shape would hand every cell the first one asked for.
     */
    public static final DeferredBlock<ChassisCellBlock> CHASSIS_CELL = BLOCKS.register("chassis_cell",
            () -> new ChassisCellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(-1.0f, 3600000.0f)
                    .noLootTable()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .dynamicShape()
                    .isValidSpawn((s, l, p, e) -> false)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChassisCellBlockEntity>> CHASSIS_CELL_ENTITY =
            BLOCK_ENTITIES.register("chassis_cell",
                    () -> BlockEntityType.Builder.of(ChassisCellBlockEntity::new, CHASSIS_CELL.get()).build(null));

    /** The vertex a Welder is holding; synced so the client can highlight it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WelderSelection>> WELDER_SELECTION =
            COMPONENTS.register("welder_selection", () -> DataComponentType.<WelderSelection>builder()
                    .persistent(WelderSelection.CODEC)
                    .networkSynchronized(WelderSelection.STREAM_CODEC)
                    .build());

    /** Which of the Welder's three jobs a use does (shift-scroll). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WelderMode>> WELDER_MODE =
            COMPONENTS.register("welder_mode", () -> DataComponentType.<WelderMode>builder()
                    .persistent(WelderMode.CODEC)
                    .networkSynchronized(WelderMode.STREAM_CODEC)
                    .build());

    /** Points chosen for a plate so far. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelected>> WELDER_SELECTED =
            COMPONENTS.register("welder_selected", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelected>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelected.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelected.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.vehicle.frame.WelderCorner>> WELDER_CORNER =
            COMPONENTS.register("welder_corner", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.vehicle.frame.WelderCorner>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.vehicle.frame.WelderCorner.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.vehicle.frame.WelderCorner.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WelderPicks>> WELDER_PICKS =
            COMPONENTS.register("welder_picks", () -> DataComponentType.<WelderPicks>builder()
                    .persistent(WelderPicks.CODEC)
                    .networkSynchronized(WelderPicks.STREAM_CODEC)
                    .build());

    public static final DeferredItem<WelderItem> WELDER = ITEMS.register("welder",
            () -> new WelderItem(new Item.Properties().stacksTo(1)));

    /**
     * Sets a point to be a bearing and switches its kind (spec section 4a). Not the shaft Bearing
     * block: that is a support in the mechanical network, this is a joint between rigid bodies.
     */
    public static final DeferredItem<PivotItem> PIVOT_SETTER = ITEMS.register("pivot_setter",
            () -> new PivotItem(new Item.Properties().stacksTo(1)));

    // --- Engines (spec section 6, Engine Stand v1) ----------------------------------------------

    /** An engine design, on a Blueprint or an Engine. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EngineDesign>> ENGINE_DESIGN =
            COMPONENTS.register("engine_design", () -> DataComponentType.<EngineDesign>builder()
                    .persistent(EngineDesign.CODEC)
                    .networkSynchronized(EngineDesign.STREAM_CODEC)
                    .build());

    public static final DeferredItem<EngineBlueprintItem> ENGINE_BLUEPRINT = ITEMS.register("engine_blueprint",
            () -> new EngineBlueprintItem(new Item.Properties().stacksTo(1)));

    public static final DeferredBlock<EngineBlock> ENGINE = BLOCKS.register("engine",
            () -> new EngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final DeferredItem<EngineItem> ENGINE_ITEM = ITEMS.register("engine",
            () -> new EngineItem(ENGINE.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineBlockEntity>> ENGINE_ENTITY =
            BLOCK_ENTITIES.register("engine", () -> BlockEntityType.Builder.of(EngineBlockEntity::new, ENGINE.get()).build(null));

    /** Liquid-fuel thruster (petrochemistry spec 9.2). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.ThrusterBlock> THRUSTER = BLOCKS.register("thruster",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.ThrusterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> THRUSTER_ITEM = ITEMS.registerSimpleBlockItem(THRUSTER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.vehicle.ThrusterBlockEntity>> THRUSTER_ENTITY =
            BLOCK_ENTITIES.register("thruster", () -> BlockEntityType.Builder.of(io.github.soundgoodizerfan.feedback.vehicle.ThrusterBlockEntity::new, THRUSTER.get()).build(null));

    public static final DeferredBlock<EngineStandBlock> ENGINE_STAND = BLOCKS.register("engine_stand",
            () -> new EngineStandBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> ENGINE_STAND_ITEM = ITEMS.registerSimpleBlockItem(ENGINE_STAND);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineStandBlockEntity>> ENGINE_STAND_ENTITY =
            BLOCK_ENTITIES.register("engine_stand",
                    () -> BlockEntityType.Builder.of(EngineStandBlockEntity::new, ENGINE_STAND.get()).build(null));

    // --- Transmissions (spec section 6, designed on the Car Design Stand) -----------------------

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign>> TRANSMISSION_DESIGN =
            COMPONENTS.register("transmission_design", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign.STREAM_CODEC)
                    .build());

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionItems.Blueprint> TRANSMISSION_BLUEPRINT =
            ITEMS.register("transmission_blueprint",
                    () -> new io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionItems.Blueprint(new Item.Properties().stacksTo(1)));

    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionBlock> TRANSMISSION =
            BLOCKS.register("transmission", () -> new io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionItems.Carried> TRANSMISSION_ITEM =
            ITEMS.register("transmission", () -> new io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionItems.Carried(
                    TRANSMISSION.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionBlockEntity>> TRANSMISSION_ENTITY =
            BLOCK_ENTITIES.register("transmission", () -> BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionBlockEntity::new, TRANSMISSION.get()).build(null));

    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Feedback.MOD_ID);

    public static final DeferredBlock<DriverSeatBlock> SEAT = BLOCKS.register("driver_seat",
            () -> new DriverSeatBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1.0f).sound(SoundType.WOOL)));

    public static final DeferredItem<BlockItem> SEAT_ITEM = ITEMS.registerSimpleBlockItem(SEAT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DriverSeatBlockEntity>> DRIVER_SEAT_ENTITY =
            BLOCK_ENTITIES.register("driver_seat", () -> BlockEntityType.Builder.of(DriverSeatBlockEntity::new, SEAT.get()).build(null));

    /** What the driver sits on; {@code sable:retain_in_sub_level} keeps it in the vehicle's plot. */
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<DriverSeatEntity>> SEAT_RIDE =
            ENTITIES.register("driver_seat", () -> net.minecraft.world.entity.EntityType.Builder
                    .<DriverSeatEntity>of(DriverSeatEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.25f, 0.35f).noSummon().clientTrackingRange(10).build("driver_seat"));

    private FVehicles() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        ENTITIES.register(modBus);
    }
}
