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

import java.util.EnumMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.creel.CreelBlock;
import io.github.soundgoodizerfan.feedback.machine.creel.CreelBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.fishwheel.FishWheelBlock;
import io.github.soundgoodizerfan.feedback.machine.fishwheel.FishWheelBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.kiln.KilnZoneBlock;
import io.github.soundgoodizerfan.feedback.machine.kiln.KilnZoneBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.stoker.StokerBlock;
import io.github.soundgoodizerfan.feedback.village.CounterBlock;
import io.github.soundgoodizerfan.feedback.village.CounterBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.stoker.StokerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.darkcell.CrushHeadBlock;
import io.github.soundgoodizerfan.feedback.machine.darkcell.CrushHeadBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.darkcell.DarkCellBlock;
import io.github.soundgoodizerfan.feedback.machine.darkcell.DarkCellBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.darkcell.FallPlateBlock;
import io.github.soundgoodizerfan.feedback.machine.darkcell.FirePlateBlock;
import io.github.soundgoodizerfan.feedback.machine.darkcell.FumePortBlock;
import io.github.soundgoodizerfan.feedback.machine.darkcell.FumePortBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.darkcell.RadiationSourceBlock;
import io.github.soundgoodizerfan.feedback.machine.darkcell.RadiationSourceBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.darkcell.ShockPlateBlock;
import io.github.soundgoodizerfan.feedback.machine.darkcell.ShockPlateBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.pivot.PivotArmItem;
import io.github.soundgoodizerfan.feedback.machine.pivot.PivotMastBlock;
import io.github.soundgoodizerfan.feedback.machine.pivot.PivotMastBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.pivot.PivotTip;
import io.github.soundgoodizerfan.feedback.machine.pivot.PivotTipItem;
import io.github.soundgoodizerfan.feedback.machine.sensor.SensorBlock;
import io.github.soundgoodizerfan.feedback.machine.sensor.SensorBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.sensor.SensorKind;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * World-facing automation ({@code docs/specs/feedback_automation_spec.md}): the sensing kit, the pivot mast, the dark cell, the tunnel
 * kiln, the Counter, the fishing gear and the recorded-run machines. Its own registers, as {@link FLogistics} has.
 */
public final class FAutomation {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Feedback.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Feedback.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Feedback.MOD_ID);

    /** One block per sensing kind, all sharing {@link #SENSOR_ENTITY}. */
    public static final Map<SensorKind, DeferredBlock<SensorBlock>> SENSORS = new EnumMap<>(SensorKind.class);
    public static final Map<SensorKind, DeferredItem<BlockItem>> SENSOR_ITEMS = new EnumMap<>(SensorKind.class);

    static {
        for (SensorKind kind : SensorKind.values()) {
            var block = BLOCKS.register(kind.id(), () -> new SensorBlock(kind,
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));
            SENSORS.put(kind, block);
            SENSOR_ITEMS.put(kind, ITEMS.registerSimpleBlockItem(block));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SensorBlockEntity>> SENSOR_ENTITY =
            BLOCK_ENTITIES.register("sensor", () -> BlockEntityType.Builder.of(SensorBlockEntity::new,
                    SENSORS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    /** The pivot mast: a shaft, an arm drawn by the renderer, and a tip. */
    public static final DeferredBlock<PivotMastBlock> PIVOT_MAST = BLOCKS.register("pivot_mast",
            () -> new PivotMastBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredItem<BlockItem> PIVOT_MAST_ITEM = ITEMS.registerSimpleBlockItem(PIVOT_MAST);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PivotMastBlockEntity>> PIVOT_ENTITY =
            BLOCK_ENTITIES.register("pivot_mast", () -> BlockEntityType.Builder.of(PivotMastBlockEntity::new, PIVOT_MAST.get()).build(null));

    /** Arms of three lengths in blocks: the radius of the field. */
    public static final Map<String, DeferredItem<PivotArmItem>> ARMS = new java.util.LinkedHashMap<>();
    /** One tip per {@link PivotTip}. */
    public static final Map<PivotTip, DeferredItem<PivotTipItem>> TIPS = new EnumMap<>(PivotTip.class);

    static {
        ARMS.put("short", ITEMS.register("pivot_arm_short", () -> new PivotArmItem(3, new Item.Properties().stacksTo(1))));
        ARMS.put("medium", ITEMS.register("pivot_arm_medium", () -> new PivotArmItem(5, new Item.Properties().stacksTo(1))));
        ARMS.put("long", ITEMS.register("pivot_arm_long", () -> new PivotArmItem(8, new Item.Properties().stacksTo(1))));
        for (PivotTip tip : PivotTip.values())
            TIPS.put(tip, ITEMS.register("pivot_tip_" + tip.id(), () -> new PivotTipItem(tip, new Item.Properties().stacksTo(1))));
    }

    private static BlockBehaviour.Properties metal(float strength) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(strength).sound(SoundType.METAL);
    }

    /** The dark cell: a controller set in the wall of a sealed, dark room. */
    public static final DeferredBlock<DarkCellBlock> DARK_CELL = BLOCKS.register("dark_cell", () -> new DarkCellBlock(metal(4.0f)));
    public static final DeferredItem<BlockItem> DARK_CELL_ITEM = ITEMS.registerSimpleBlockItem(DARK_CELL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DarkCellBlockEntity>> DARK_CELL_ENTITY =
            BLOCK_ENTITIES.register("dark_cell", () -> BlockEntityType.Builder.of(DarkCellBlockEntity::new, DARK_CELL.get()).build(null));

    /** The six kill methods, set in the wall. */
    public static final DeferredBlock<FallPlateBlock> FALL_PLATE = BLOCKS.register("fall_plate", () -> new FallPlateBlock(metal(3.0f)));
    public static final DeferredItem<BlockItem> FALL_PLATE_ITEM = ITEMS.registerSimpleBlockItem(FALL_PLATE);
    public static final DeferredBlock<FirePlateBlock> FIRE_PLATE = BLOCKS.register("fire_plate", () -> new FirePlateBlock(metal(3.0f)));
    public static final DeferredItem<BlockItem> FIRE_PLATE_ITEM = ITEMS.registerSimpleBlockItem(FIRE_PLATE);
    public static final DeferredBlock<CrushHeadBlock> CRUSH_HEAD = BLOCKS.register("crush_head", () -> new CrushHeadBlock(metal(4.0f)));
    public static final DeferredItem<BlockItem> CRUSH_HEAD_ITEM = ITEMS.registerSimpleBlockItem(CRUSH_HEAD);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrushHeadBlockEntity>> CRUSH_HEAD_ENTITY =
            BLOCK_ENTITIES.register("crush_head", () -> BlockEntityType.Builder.of(CrushHeadBlockEntity::new, CRUSH_HEAD.get()).build(null));
    public static final DeferredBlock<FumePortBlock> FUME_PORT = BLOCKS.register("fume_port", () -> new FumePortBlock(metal(3.0f)));
    public static final DeferredItem<BlockItem> FUME_PORT_ITEM = ITEMS.registerSimpleBlockItem(FUME_PORT);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FumePortBlockEntity>> FUME_PORT_ENTITY =
            BLOCK_ENTITIES.register("fume_port", () -> BlockEntityType.Builder.of(FumePortBlockEntity::new, FUME_PORT.get()).build(null));
    public static final DeferredBlock<ShockPlateBlock> SHOCK_PLATE = BLOCKS.register("shock_plate", () -> new ShockPlateBlock(metal(3.0f)));
    public static final DeferredItem<BlockItem> SHOCK_PLATE_ITEM = ITEMS.registerSimpleBlockItem(SHOCK_PLATE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShockPlateBlockEntity>> SHOCK_PLATE_ENTITY =
            BLOCK_ENTITIES.register("shock_plate", () -> BlockEntityType.Builder.of(ShockPlateBlockEntity::new, SHOCK_PLATE.get()).build(null));
    public static final DeferredBlock<RadiationSourceBlock> RADIATION_SOURCE = BLOCKS.register("radiation_source", () -> new RadiationSourceBlock(metal(4.0f)));
    public static final DeferredItem<BlockItem> RADIATION_SOURCE_ITEM = ITEMS.registerSimpleBlockItem(RADIATION_SOURCE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadiationSourceBlockEntity>> RADIATION_SOURCE_ENTITY =
            BLOCK_ENTITIES.register("radiation_source", () -> BlockEntityType.Builder.of(RadiationSourceBlockEntity::new, RADIATION_SOURCE.get()).build(null));

    /** The stoker: a screw feed into a firebox. */
    public static final DeferredBlock<StokerBlock> STOKER = BLOCKS.register("stoker", () -> new StokerBlock(metal(3.0f)));
    public static final DeferredItem<BlockItem> STOKER_ITEM = ITEMS.registerSimpleBlockItem(STOKER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StokerBlockEntity>> STOKER_ENTITY =
            BLOCK_ENTITIES.register("stoker", () -> BlockEntityType.Builder.of(StokerBlockEntity::new, STOKER.get()).build(null));

    /** The creel: a basket trap for fish. */
    public static final DeferredBlock<CreelBlock> CREEL = BLOCKS.register("creel",
            () -> new CreelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> CREEL_ITEM = ITEMS.registerSimpleBlockItem(CREEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreelBlockEntity>> CREEL_ENTITY =
            BLOCK_ENTITIES.register("creel", () -> BlockEntityType.Builder.of(CreelBlockEntity::new, CREEL.get()).build(null));

    /** The fish wheel: a water wheel with baskets. */
    public static final DeferredBlock<FishWheelBlock> FISH_WHEEL = BLOCKS.register("fish_wheel",
            () -> new FishWheelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> FISH_WHEEL_ITEM = ITEMS.registerSimpleBlockItem(FISH_WHEEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FishWheelBlockEntity>> FISH_WHEEL_ENTITY =
            BLOCK_ENTITIES.register("fish_wheel", () -> BlockEntityType.Builder.of(FishWheelBlockEntity::new, FISH_WHEEL.get()).build(null));

    /** The tunnel kiln zone: ware rides a car through the heat. */
    public static final DeferredBlock<KilnZoneBlock> KILN_ZONE = BLOCKS.register("kiln_zone", () -> new KilnZoneBlock(metal(3.0f)));
    public static final DeferredItem<BlockItem> KILN_ZONE_ITEM = ITEMS.registerSimpleBlockItem(KILN_ZONE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KilnZoneBlockEntity>> KILN_ZONE_ENTITY =
            BLOCK_ENTITIES.register("kiln_zone", () -> BlockEntityType.Builder.of(KilnZoneBlockEntity::new, KILN_ZONE.get()).build(null));

    /** The Counter: delivers goods to, and sells goods to, the villager beside it. */
    public static final DeferredBlock<CounterBlock> COUNTER = BLOCKS.register("counter",
            () -> new CounterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> COUNTER_ITEM = ITEMS.registerSimpleBlockItem(COUNTER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CounterBlockEntity>> COUNTER_ENTITY =
            BLOCK_ENTITIES.register("counter", () -> BlockEntityType.Builder.of(CounterBlockEntity::new, COUNTER.get()).build(null));

    private FAutomation() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(FAutomation::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PIVOT_ENTITY.get(), (mast, side) -> mast.handler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DARK_CELL_ENTITY.get(), (cell, side) -> cell.outputView());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STOKER_ENTITY.get(), (stoker, side) -> stoker.hopper(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, KILN_ZONE_ENTITY.get(), (kiln, side) -> kiln.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, COUNTER_ENTITY.get(), (counter, side) -> counter.port(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CREEL_ENTITY.get(), (creel, side) -> creel.catches());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FISH_WHEEL_ENTITY.get(), (wheel, side) -> wheel.catches());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FUME_PORT_ENTITY.get(), (port, side) -> port.tank());
    }

    /** Every automation item, for the creative tab. */
    public static void fill(net.minecraft.world.item.CreativeModeTab.Output output) {
        SENSOR_ITEMS.values().forEach(i -> output.accept(i.get()));
        output.accept(DARK_CELL_ITEM.get());
        output.accept(FALL_PLATE_ITEM.get());
        output.accept(FIRE_PLATE_ITEM.get());
        output.accept(CRUSH_HEAD_ITEM.get());
        output.accept(FUME_PORT_ITEM.get());
        output.accept(SHOCK_PLATE_ITEM.get());
        output.accept(RADIATION_SOURCE_ITEM.get());
        output.accept(COUNTER_ITEM.get());
        output.accept(KILN_ZONE_ITEM.get());
        output.accept(STOKER_ITEM.get());
        output.accept(CREEL_ITEM.get());
        output.accept(FISH_WHEEL_ITEM.get());
        output.accept(PIVOT_MAST_ITEM.get());
        ARMS.values().forEach(i -> output.accept(i.get()));
        TIPS.values().forEach(i -> output.accept(i.get()));
    }
}
