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
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularItem;
import io.github.soundgoodizerfan.feedback.modular.ModularRecipe;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Modular tools, weapons and armour: the two components, the one recipe and one item per type
 * ({@code feedback_modular_items_spec.md}). An item has to exist before any datapack does, so the
 * list of types is here; what each is -- its roles, patterns and geometry -- is
 * {@code data/feedback/modular_type/<id>.json}, and the item's registry path is the type's id.
 */
public final class FModular {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Feedback.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Feedback.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Feedback.MOD_ID);

    /** What the item was put together as -- parts and edge, nothing derived. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ModularDesign>> DESIGN =
            COMPONENTS.register("modular_design", () -> DataComponentType.<ModularDesign>builder()
                    .persistent(ModularDesign.CODEC)
                    .networkSynchronized(ModularDesign.STREAM_CODEC)
                    .build());

    /** What that design makes, baked once and again at each step of wear. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ModularFigures>> FIGURES =
            COMPONENTS.register("modular_figures", () -> DataComponentType.<ModularFigures>builder()
                    .persistent(ModularFigures.CODEC)
                    .networkSynchronized(ModularFigures.STREAM_CODEC)
                    .build());

    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Feedback.MOD_ID);

    /** Makes whatever a loot table generates modular, if it was vanilla's. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>,
            com.mojang.serialization.MapCodec<io.github.soundgoodizerfan.feedback.modular.ModularEvents.ConvertLoot>> CONVERT_LOOT =
            LOOT_MODIFIERS.register("convert_vanilla", () -> io.github.soundgoodizerfan.feedback.modular.ModularEvents.ConvertLoot.CODEC);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<ModularRecipe>> RECIPE =
            SERIALIZERS.register("modular", () -> new SimpleCraftingRecipeSerializer<>(ModularRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.modular.ModularPartRecipe>> PART_RECIPE =
            SERIALIZERS.register("modular_part", () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.modular.ModularPartRecipe::new));

    /**
     * One item per kind of shaped part, any material ({@code process/MadeOf}). Which part a type's
     * role takes, and the shape it is laid in, is the type's data; the item has to exist first.
     */
    public static final java.util.List<DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularPartItem>> PARTS = java.util.stream.Stream.of(
            "pickaxe_head", "axe_head", "shovel_head", "hoe_head", "mattock_head", "sickle_head", "maul_head",
            "cleaver_head", "pike_head", "sword_blade", "trident_tines",
            "helmet_plate", "chestplate_plate", "leggings_plate", "boots_plate")
            .map(id -> ITEMS.registerItem(id, io.github.soundgoodizerfan.feedback.modular.ModularPartItem::new))
            .toList();

    public static final DeferredItem<ModularItem> PICKAXE = ITEMS.register("pickaxe",
            () -> new ModularItem(ModularStacks.properties()));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularAxe> AXE = ITEMS.register("axe",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularAxe(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularShovel> SHOVEL = ITEMS.register("shovel",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularShovel(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularHoe> HOE = ITEMS.register("hoe",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularHoe(ModularStacks.properties()));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularSword> SWORD = ITEMS.register("sword",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularSword(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularMace> MACE = ITEMS.register("mace",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularMace(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularTrident> TRIDENT = ITEMS.register("trident",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularTrident(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularShield> SHIELD = ITEMS.register("shield",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularShield(ModularStacks.properties()));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularBow> BOW = ITEMS.register("bow",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularBow(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularCrossbow> CROSSBOW = ITEMS.register("crossbow",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularCrossbow(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularShears> SHEARS = ITEMS.register("shears",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularShears(ModularStacks.properties()));

    public static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> ARMOUR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Feedback.MOD_ID);

    /**
     * The one armour material vanilla's armour class insists on. Everything it would say -- defence,
     * toughness, enchantability -- is on the stack, so this says nothing; its single dyeable layer
     * is the sprite every piece is drawn in, coloured by its plate.
     */
    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial, net.minecraft.world.item.ArmorMaterial> ARMOUR_MATERIAL =
            ARMOUR_MATERIALS.register("modular", () -> new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(), 1,
                    net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(Feedback.id("modular"), "", true)), 0f, 0f));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularArmor> HELMET = ITEMS.register("helmet",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularArmor(net.minecraft.world.item.ArmorItem.Type.HELMET, ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularArmor> CHESTPLATE = ITEMS.register("chestplate",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularArmor(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularArmor> LEGGINGS = ITEMS.register("leggings",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularArmor(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularArmor> BOOTS = ITEMS.register("boots",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularArmor(net.minecraft.world.item.ArmorItem.Type.BOOTS, ModularStacks.properties()));

    public static final DeferredItem<Item> BALANCE_SCALE = ITEMS.registerItem("balance_scale",
            io.github.soundgoodizerfan.feedback.item.BalanceScaleItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SCRATCH_FILES = ITEMS.registerItem("scratch_files",
            io.github.soundgoodizerfan.feedback.item.ScratchFilesItem::new, new Item.Properties().stacksTo(1));

    public static final net.neoforged.neoforge.registries.DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Feedback.MOD_ID);

    /** Where a modular item is drawn and its figures read before it is made (spec 6). */
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.BlueprintTableBlock> BLUEPRINT_TABLE =
            BLOCKS.register("blueprint_table", () -> new io.github.soundgoodizerfan.feedback.modular.BlueprintTableBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2.5f)
                            .sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> BLUEPRINT_TABLE_ITEM = ITEMS.registerSimpleBlockItem(BLUEPRINT_TABLE);
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularBlueprintItem> BLUEPRINT = ITEMS.register("modular_blueprint",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularBlueprintItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularItem> MATTOCK = ITEMS.register("mattock",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularItem(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularItem> CLEAVER = ITEMS.register("cleaver",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularItem(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularItem> PIKE = ITEMS.register("pike",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularItem(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularMaul> MAUL = ITEMS.register("maul",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularMaul(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.ModularSickle> SICKLE = ITEMS.register("sickle",
            () -> new io.github.soundgoodizerfan.feedback.modular.ModularSickle(ModularStacks.properties()));

    private FModular() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        SERIALIZERS.register(modBus);
        ARMOUR_MATERIALS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    /** Every item this registers but the parts, which mean nothing without a material: for the creative tab and the tint. */
    public static java.util.List<Item> items() {
        return ITEMS.getEntries().stream().<Item>map(DeferredHolder::get)
                .filter(i -> !(i instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem)).toList();
    }
}
