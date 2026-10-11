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
import io.github.soundgoodizerfan.feedback.vehicle.VehicleCoreBlock;
import io.github.soundgoodizerfan.feedback.vehicle.frame.ChassisCellBlock;
import io.github.soundgoodizerfan.feedback.vehicle.frame.ChassisTubeBlock;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlock;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderMode;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderPicks;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelection;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
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
 * Vehicle blocks and items ({@code feedback_vehicles_spec.md}). Their own registers rather than
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

    public static final DeferredBlock<VehicleCoreBlock> VEHICLE_CORE = BLOCKS.register("vehicle_core",
            () -> new VehicleCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> VEHICLE_CORE_ITEM = ITEMS.registerSimpleBlockItem(VEHICLE_CORE);

    /** The anchor a chassis is welded around (spec section 4). */
    public static final DeferredBlock<FrameBlock> FRAME = BLOCKS.register("chassis_frame",
            () -> new FrameBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<ChassisTubeBlock> CHASSIS_TUBE = BLOCKS.register("chassis_tube",
            () -> new ChassisTubeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0f)
                    .noLootTable()
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> FRAME_ITEM = ITEMS.registerSimpleBlockItem(FRAME);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FrameBlockEntity>> FRAME_ENTITY =
            BLOCK_ENTITIES.register("chassis_frame",
                    () -> BlockEntityType.Builder.of(FrameBlockEntity::new, FRAME.get()).build(null));

    /**
     * A plate's collision at 1/16, placed on assembly; no item, never in a player's hands. Mines as
     * its plate's material -- the strength here is overridden per position.
     */
    public static final DeferredBlock<ChassisCellBlock> CHASSIS_CELL = BLOCKS.register("chassis_cell",
            () -> new ChassisCellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0f)
                    .noLootTable()
                    .noOcclusion()));

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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WelderPicks>> WELDER_PICKS =
            COMPONENTS.register("welder_picks", () -> DataComponentType.<WelderPicks>builder()
                    .persistent(WelderPicks.CODEC)
                    .networkSynchronized(WelderPicks.STREAM_CODEC)
                    .build());

    public static final DeferredItem<WelderItem> WELDER = ITEMS.register("welder",
            () -> new WelderItem(new Item.Properties().stacksTo(1)));

    /**
     * What the Welder makes a pivot from, held in the off hand while joining two frames (spec
     * section 4a): anything tagged, never one item id. The shaft bearing is the first member.
     */
    public static final TagKey<Item> BEARINGS = TagKey.create(Registries.ITEM, Feedback.id("bearings"));

    private FVehicles() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
    }
}
