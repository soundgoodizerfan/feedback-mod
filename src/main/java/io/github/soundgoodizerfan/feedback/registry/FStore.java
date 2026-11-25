// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.store.DockBlock;
import io.github.soundgoodizerfan.feedback.logistics.store.DockBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.store.DockMenu;
import io.github.soundgoodizerfan.feedback.logistics.store.Fitting;
import io.github.soundgoodizerfan.feedback.logistics.store.FittingItem;
import io.github.soundgoodizerfan.feedback.logistics.store.RackBlock;
import io.github.soundgoodizerfan.feedback.logistics.store.RackBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.store.RailBlock;
import io.github.soundgoodizerfan.feedback.logistics.store.ShuttleHeadBlock;
import io.github.soundgoodizerfan.feedback.logistics.store.ShuttleHeadBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The warehouse ({@code logistics/store/}): rack, rail, shuttle head, dock and bay fittings, in their own registers as
 * {@link FLogistics} has. The rack item is a modular build and is registered in {@link FModular}.
 */
public final class FStore {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Feedback.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Feedback.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Feedback.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Feedback.MOD_ID);

    public static final DeferredBlock<RackBlock> RACK = BLOCKS.register("pallet_rack",
            () -> new RackBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<RailBlock> RAIL = BLOCKS.register("warehouse_rail",
            () -> new RailBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<ShuttleHeadBlock> HEAD = BLOCKS.register("shuttle_head",
            () -> new ShuttleHeadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<DockBlock> DOCK = BLOCKS.register("warehouse_dock",
            () -> new DockBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> RAIL_ITEM = ITEMS.registerSimpleBlockItem(RAIL);
    public static final DeferredItem<BlockItem> HEAD_ITEM = ITEMS.registerSimpleBlockItem(HEAD);
    public static final DeferredItem<BlockItem> DOCK_ITEM = ITEMS.registerSimpleBlockItem(DOCK);

    /** One item per fitting that is a thing; a pallet bay is the bare shelf. */
    public static final Map<Fitting, DeferredItem<FittingItem>> FITTINGS = new EnumMap<>(Fitting.class);

    static {
        for (Fitting f : Fitting.values())
            if (f != Fitting.PALLET)
                FITTINGS.put(f, ITEMS.register("rack_fitting_" + f.id(), () -> new FittingItem(f, new Item.Properties())));
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RackBlockEntity>> RACK_ENTITY =
            BLOCK_ENTITIES.register("pallet_rack", () -> BlockEntityType.Builder.of(RackBlockEntity::new, RACK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShuttleHeadBlockEntity>> HEAD_ENTITY =
            BLOCK_ENTITIES.register("shuttle_head", () -> BlockEntityType.Builder.of(ShuttleHeadBlockEntity::new, HEAD.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DockBlockEntity>> DOCK_ENTITY =
            BLOCK_ENTITIES.register("warehouse_dock", () -> BlockEntityType.Builder.of(DockBlockEntity::new, DOCK.get()).build(null));

    /** Extended: the dock's position rides in the open packet; what it shows is sent as a view. */
    public static final DeferredHolder<MenuType<?>, MenuType<DockMenu>> DOCK_MENU =
            MENUS.register("warehouse_dock", () -> IMenuTypeExtension.create(DockMenu::new));

    private FStore() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(FStore::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DOCK_ENTITY.get(), (dock, side) -> dock.handler());
    }

    /** The warehouse blocks, fittings and one rack per metal, for the creative tab. */
    public static void fill(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(DOCK_ITEM.get());
        output.accept(HEAD_ITEM.get());
        output.accept(RAIL_ITEM.get());
        FITTINGS.values().forEach(i -> output.accept(i.get()));
        rackVariants().forEach(output::accept);
    }

    /** One rack per material that can be both its frame and its shelf. */
    public static List<ItemStack> rackVariants() {
        ResourceLocation type = Feedback.id("pallet_rack");
        List<ItemStack> out = new ArrayList<>();
        var materials = FormItems.materials();
        var types = ModularTypes.known();
        var starter = ToolDesigns.starter(type, types, materials);
        if (starter.isEmpty())
            return out;
        materials.forEach((id, m) -> {
            if (m.item(id, "rod").isEmpty() || m.item(id, "plate").isEmpty())
                return;
            List<ModularDesign.Part> parts = new ArrayList<>();
            for (ModularDesign.Part p : starter.get().parts())
                parts.add(new ModularDesign.Part(p.role(), id, p.form(), p.count(), p.kind(), p.variant(), p.flaw(), p.measures()));
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(type));
            if (ModularStacks.assemble(stack, new ModularDesign(type, parts, starter.get().edge()), materials, types))
                out.add(stack);
        });
        return out;
    }
}
