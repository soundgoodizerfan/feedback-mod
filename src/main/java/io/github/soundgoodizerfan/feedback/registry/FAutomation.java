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
    }

    /** Every automation item, for the creative tab. */
    public static void fill(net.minecraft.world.item.CreativeModeTab.Output output) {
        SENSOR_ITEMS.values().forEach(i -> output.accept(i.get()));
        output.accept(PIVOT_MAST_ITEM.get());
        ARMS.values().forEach(i -> output.accept(i.get()));
        TIPS.values().forEach(i -> output.accept(i.get()));
    }
}
