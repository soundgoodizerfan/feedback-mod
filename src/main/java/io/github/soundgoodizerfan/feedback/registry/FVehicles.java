// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.LiftBlock;
import io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.LiftItem;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlock;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineItem;
import net.minecraft.nbt.CompoundTag;
import io.github.soundgoodizerfan.feedback.vehicle.seat.DriverSeatBlock;
import io.github.soundgoodizerfan.feedback.vehicle.seat.DriverSeatBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.seat.DriverSeatEntity;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlock;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCoreItem;
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
 * Vehicle blocks and items ({@code docs/specs/feedback_vehicles_spec.md}). Their own registers rather than
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

    // --- Micro-blocks (docs/dev/vehicle.md) ---------------------------------------------

    /**
     * A full-size block holding up to 64 quarter-block voxels. Broken by hand, it gives them back
     * (its loot table is the entity's contents). Dynamic shape for the same reason as the chassis
     * cell: the voxels' shape is per position.
     */
    public static final DeferredBlock<MicroCellBlock> MICRO_CELL = BLOCKS.register("micro_cell",
            () -> new MicroCellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .dynamicShape()
                    .isValidSpawn((s, l, p, e) -> false)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MicroCellBlockEntity>> MICRO_CELL_ENTITY =
            BLOCK_ENTITIES.register("micro_cell",
                    () -> BlockEntityType.Builder.of(MicroCellBlockEntity::new, MICRO_CELL.get()).build(null));

    /** Starts a build: a micro cell with one voxel of stone in it. Every voxel after is a block held against the build. */
    public static final DeferredItem<MicroCoreItem> MICRO_CORE = ITEMS.register("micro_core",
            () -> new MicroCoreItem(new Item.Properties()));

    // --- Engines (spec section 6, a modular build at the Tool Table) ----------------------------------------------

    /** What an engine's parts bake to, on an Engine. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EngineDesign>> ENGINE_DESIGN =
            COMPONENTS.register("engine_design", () -> DataComponentType.<EngineDesign>builder()
                    .persistent(EngineDesign.CODEC)
                    .networkSynchronized(EngineDesign.STREAM_CODEC)
                    .build());

    public static final DeferredBlock<EngineBlock> ENGINE = BLOCKS.register("engine",
            () -> new EngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));

    public static final DeferredItem<EngineItem> ENGINE_ITEM = ITEMS.register("engine",
            () -> new EngineItem(ENGINE.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineBlockEntity>> ENGINE_ENTITY =
            BLOCK_ENTITIES.register("engine", () -> BlockEntityType.Builder.of(EngineBlockEntity::new, ENGINE.get()).build(null));

    /** Liquid-fuel thruster (petrochemistry spec 9.2). */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.ThrusterBlock> THRUSTER = BLOCKS.register("thruster",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.ThrusterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.vehicle.ThrusterBlockEntity>> THRUSTER_ENTITY =
            BLOCK_ENTITIES.register("thruster", () -> BlockEntityType.Builder.of(io.github.soundgoodizerfan.feedback.vehicle.ThrusterBlockEntity::new, THRUSTER.get()).build(null));

    /** Flight sensor: altitude, airspeed, pitch or roll for a Controller card. */
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.FlightSensorBlock> FLIGHT_SENSOR = BLOCKS.register("flight_sensor",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.FlightSensorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> FLIGHT_SENSOR_ITEM = ITEMS.registerSimpleBlockItem(FLIGHT_SENSOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.vehicle.FlightSensorBlockEntity>> FLIGHT_SENSOR_ENTITY =
            BLOCK_ENTITIES.register("flight_sensor", () -> BlockEntityType.Builder.of(io.github.soundgoodizerfan.feedback.vehicle.FlightSensorBlockEntity::new, FLIGHT_SENSOR.get()).build(null));

    /** What a propeller's parts bake to, on a Propeller. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.vehicle.air.PropDesign>> PROP_DESIGN =
            COMPONENTS.register("prop_design", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.vehicle.air.PropDesign>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.vehicle.air.PropDesign.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.vehicle.air.PropDesign.STREAM_CODEC)
                    .build());

    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.air.PropBlock> PROPELLER = BLOCKS.register("propeller",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.air.PropBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.vehicle.air.PropItem> PROPELLER_ITEM = ITEMS.register("propeller",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.air.PropItem(PROPELLER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.vehicle.air.PropBlockEntity>> PROPELLER_ENTITY =
            BLOCK_ENTITIES.register("propeller", () -> BlockEntityType.Builder.of(io.github.soundgoodizerfan.feedback.vehicle.air.PropBlockEntity::new, PROPELLER.get()).build(null));

    // --- Airships (flight A1): gas cells, vent, ballast ----------------------------------------------

    private static BlockBehaviour.Properties cellProps(MapColor color, SoundType sound, float strength) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(strength).sound(sound).noOcclusion();
    }

    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlock> GAS_CELL_CLOTH = BLOCKS.register("gas_cell_cloth",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlock(cellProps(MapColor.SNOW, SoundType.WOOL, 0.4f),
                    io.github.soundgoodizerfan.feedback.vehicle.air.Skin.CLOTH));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlock> GAS_CELL_RUBBER = BLOCKS.register("gas_cell_rubber",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlock(cellProps(MapColor.COLOR_BLACK, SoundType.WOOL, 0.6f),
                    io.github.soundgoodizerfan.feedback.vehicle.air.Skin.RUBBER));
    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlock> GAS_CELL_PLATE = BLOCKS.register("gas_cell_plate",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlock(cellProps(MapColor.METAL, SoundType.METAL, 2.0f),
                    io.github.soundgoodizerfan.feedback.vehicle.air.Skin.PLATE));
    public static final DeferredItem<BlockItem> GAS_CELL_CLOTH_ITEM = ITEMS.registerSimpleBlockItem(GAS_CELL_CLOTH);
    public static final DeferredItem<BlockItem> GAS_CELL_RUBBER_ITEM = ITEMS.registerSimpleBlockItem(GAS_CELL_RUBBER);
    public static final DeferredItem<BlockItem> GAS_CELL_PLATE_ITEM = ITEMS.registerSimpleBlockItem(GAS_CELL_PLATE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlockEntity>> GAS_CELL_ENTITY =
            BLOCK_ENTITIES.register("gas_cell", () -> BlockEntityType.Builder.of(io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlockEntity::new,
                    GAS_CELL_CLOTH.get(), GAS_CELL_RUBBER.get(), GAS_CELL_PLATE.get()).build(null));

    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.air.VentValveBlock> VENT_VALVE = BLOCKS.register("vent_valve",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.air.VentValveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> VENT_VALVE_ITEM = ITEMS.registerSimpleBlockItem(VENT_VALVE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.vehicle.air.VentValveBlockEntity>> VENT_VALVE_ENTITY =
            BLOCK_ENTITIES.register("vent_valve", () -> BlockEntityType.Builder.of(io.github.soundgoodizerfan.feedback.vehicle.air.VentValveBlockEntity::new, VENT_VALVE.get()).build(null));

    public static final DeferredBlock<io.github.soundgoodizerfan.feedback.vehicle.air.BallastTankBlock> BALLAST_TANK = BLOCKS.register("ballast_tank",
            () -> new io.github.soundgoodizerfan.feedback.vehicle.air.BallastTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> BALLAST_TANK_ITEM = ITEMS.registerSimpleBlockItem(BALLAST_TANK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.soundgoodizerfan.feedback.vehicle.air.BallastTankBlockEntity>> BALLAST_TANK_ENTITY =
            BLOCK_ENTITIES.register("ballast_tank", () -> BlockEntityType.Builder.of(io.github.soundgoodizerfan.feedback.vehicle.air.BallastTankBlockEntity::new, BALLAST_TANK.get()).build(null));

    // --- Transmissions (spec section 6, a modular build at the Tool Table) -----------------------

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign>> TRANSMISSION_DESIGN =
            COMPONENTS.register("transmission_design", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionDesign.STREAM_CODEC)
                    .build());

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
