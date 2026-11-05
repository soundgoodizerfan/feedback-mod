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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltBlock;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltHeadBlock;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltHeadBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltModule;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltModuleItem;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Belts, their drive and their modules ({@code logistics/}). Its own registers, as {@link FVehicles} has, so the
 * logistics package sits in one place. The belt item itself is a modular build and is registered in {@link FModular}.
 */
public final class FLogistics {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Feedback.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Feedback.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Feedback.MOD_ID);

    /** One cell of belt. Placed from the modular {@code conveyor_belt} item. */
    public static final DeferredBlock<BeltBlock> BELT = BLOCKS.register("conveyor_belt",
            () -> new BeltBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.WOOL).noOcclusion()));

    /** The driven roller at the exit end of a run. */
    public static final DeferredBlock<BeltHeadBlock> HEAD = BLOCKS.register("belt_head",
            () -> new BeltHeadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredItem<BlockItem> HEAD_ITEM = ITEMS.registerSimpleBlockItem(HEAD);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeltBlockEntity>> BELT_ENTITY =
            BLOCK_ENTITIES.register("conveyor_belt", () -> BlockEntityType.Builder.of(BeltBlockEntity::new, BELT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeltHeadBlockEntity>> HEAD_ENTITY =
            BLOCK_ENTITIES.register("belt_head", () -> BlockEntityType.Builder.of(BeltHeadBlockEntity::new, HEAD.get()).build(null));

    /** One item per module: {@code belt_module_<id>}. */
    public static final Map<BeltModule, DeferredItem<BeltModuleItem>> MODULE_ITEMS = new EnumMap<>(BeltModule.class);

    static {
        for (BeltModule m : BeltModule.values())
            MODULE_ITEMS.put(m, ITEMS.register("belt_module_" + m.id(), () -> new BeltModuleItem(m, new Item.Properties())));
    }

    private FLogistics() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(FLogistics::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BELT_ENTITY.get(), (belt, side) -> belt.cargoView());
    }

    /** The head roller and every module, for the creative tab. */
    public static void fill(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(HEAD_ITEM.get());
        MODULE_ITEMS.values().forEach(i -> output.accept(i.get()));
        beltVariants().forEach(output::accept);
    }

    /** One conveyor belt per material that can be a band, so each is listed (and its tooltip read) in the creative tab. */
    public static List<ItemStack> beltVariants() {
        ResourceLocation type = Feedback.id("conveyor_belt");
        List<ItemStack> out = new ArrayList<>();
        var materials = FormItems.materials();
        var types = ModularTypes.known();
        var starter = ToolDesigns.starter(type, types, materials);
        if (starter.isEmpty())
            return out;
        materials.forEach((id, m) -> {
            if (m.mechanical().friction().isEmpty() || m.thermal().serviceTemperature().isEmpty())
                return;
            String form = m.item(id, "plate").isPresent() ? "plate" : m.item(id, "leather").isPresent() ? "leather" : null;
            if (form == null)
                return;
            List<ModularDesign.Part> parts = new ArrayList<>();
            for (ModularDesign.Part p : starter.get().parts())
                parts.add(p.role().equals("B") ? new ModularDesign.Part(p.role(), id, form, p.count(), p.kind(), p.variant(), p.flaw(), p.dims()) : p);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(type));
            if (ModularStacks.assemble(stack, new ModularDesign(type, parts, starter.get().edge()), materials, types))
                out.add(stack);
        });
        return out;
    }
}
