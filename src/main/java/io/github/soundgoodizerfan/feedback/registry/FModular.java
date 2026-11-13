// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.item.ModularItem;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Modular tools, weapons and armour: the two components, the one recipe and one item per type
 *. An item has to exist before any datapack does, so the
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

    /** The other drafts pinned beside a blueprint's own design when it was printed (at most {@code BlueprintTableMenu.MAX_VARIANTS}); the Blueprint Table puts them back as pins. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<ModularDesign>>> VARIANTS =
            COMPONENTS.register("blueprint_variants", () -> DataComponentType.<java.util.List<ModularDesign>>builder()
                    .persistent(ModularDesign.CODEC.listOf())
                    .networkSynchronized(ModularDesign.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()))
                    .build());

    /** A blueprint's title and designer; an item made to a blueprint carries it too (see {@code BlueprintCredit}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintCredit>> CREDIT =
            COMPONENTS.register("blueprint_credit", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintCredit>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintCredit.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintCredit.STREAM_CODEC)
                    .build());

    /** How well the item fit when it was built: the fit figure and the durability head start, kept apart (see {@code ModularFit}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<io.github.soundgoodizerfan.feedback.modular.type.ModularFit.Fit>> FIT =
            COMPONENTS.register("modular_fit", () -> DataComponentType.<io.github.soundgoodizerfan.feedback.modular.type.ModularFit.Fit>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.modular.type.ModularFit.Fit.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.modular.type.ModularFit.Fit.STREAM_CODEC)
                    .build());

    /** The quality of a treatise's notes, 0 to 1, from how well its specimen was read; it adds start levels to what is assembled with the volume held. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> NOTES =
            COMPONENTS.register("treatise_notes", () -> DataComponentType.<Float>builder()
                    .persistent(com.mojang.serialization.Codec.floatRange(0f, 1f)).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.FLOAT).build());

    /** What an instrument read off the item and stamped on it: key to the figure as it was then ({@code Measured}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.Map<String, Float>>> MEASURED =
            COMPONENTS.register("measured", () -> DataComponentType.<java.util.Map<String, Float>>builder()
                    .persistent(io.github.soundgoodizerfan.feedback.modular.bench.Measured.CODEC)
                    .networkSynchronized(io.github.soundgoodizerfan.feedback.modular.bench.Measured.STREAM_CODEC)
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
            com.mojang.serialization.MapCodec<io.github.soundgoodizerfan.feedback.modular.item.ModularEvents.ConvertLoot>> CONVERT_LOOT =
            LOOT_MODIFIERS.register("convert_vanilla", () -> io.github.soundgoodizerfan.feedback.modular.item.ModularEvents.ConvertLoot.CODEC);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>,
            com.mojang.serialization.MapCodec<io.github.soundgoodizerfan.feedback.crop.SeedLoot>> CROP_SEEDS_LOOT =
            LOOT_MODIFIERS.register("crop_seeds", () -> io.github.soundgoodizerfan.feedback.crop.SeedLoot.CODEC);

    /** Adds a suit module plan to a chest. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>,
            com.mojang.serialization.MapCodec<io.github.soundgoodizerfan.feedback.modular.blueprint.ModularBlueprints.Loot>> MODULE_PLAN_LOOT =
            LOOT_MODIFIERS.register("module_plan", () -> io.github.soundgoodizerfan.feedback.modular.blueprint.ModularBlueprints.Loot.CODEC);

    /** Adds a gun blueprint from the pool to a chest. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>,
            com.mojang.serialization.MapCodec<io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool.Loot>> GUN_BLUEPRINT_LOOT =
            LOOT_MODIFIERS.register("gun_blueprint", () -> io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool.Loot.CODEC);

    /**
     * One item per kind of shaped part, any material ({@code process/MadeOf}). Which part a type's
     * role takes, and the shape it is laid in, is the type's data; the item has to exist first.
     */
    public static final java.util.List<DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem>> PARTS = java.util.stream.Stream.of(
            "pickaxe_head", "axe_head", "shovel_head", "hoe_head", "sickle_head", "maul_head",
            "cleaver_head", "spear_head", "sword_blade",
            "helmet_plate", "chestplate_plate", "leggings_plate", "boots_plate",
            "tool_tip", "tool_binding", "sword_guard", "shield_rim", "crossbow_prod", "bow_limbs", "shield_face",
            "gun_barrel", "gun_grip", "receiver", "slide", "bolt", "trigger_group", "cylinder", "magazine_tube",
            "thruster_nozzle", "knife_blade", "saw_blade", "file_head", "driver_bit", "shears_blade", "head_blank", "suppressor", "muzzle_brake", "eddy_sleeve", "thermal_sleeve", "counterweight", "resonance_weight", "bipod", "bayonet", "selector_switch", "relief_vent", "charge_fuse", "machine_bearing", "engine_block", "piston_set", "crankshaft", "cylinder_head", "spark_plug", "gear_set", "synchro_ring", "gearbox_case", "prop_hub", "prop_blades", "tool_pommel", "charge_casing", "charge_primer", "obturator", "hydraulic_buffer", "lit_reticle", "magnetic_sear", "barrel_shroud", "sight", "gun_stock", "foregrip", "belt_feed", "box_feed", "ceramic_bowl", "ceramic_plate", "wooden_skewer", "glove_body", "backpack_shell", "bed_mattress", "bed_frame", "metacarpal_plate", "vial_body", "spatula_blade", "spoon_bowl", "battery_plates", "machine_frame", "machine_winding", "machine_housing", "belt_band", "module_casing", "module_wiring", "module_optic", "module_filter", "module_thermal", "module_plating", "module_actuator", "module_spring", "module_tread", "module_thruster", "module_coil", "module_dispenser", "spring_gun_barrel", "spring_gun_receiver", "steam_valve", "steam_lagging", "main_spring", "steam_chest", "vessel_shell", "exchanger_core", "tank_shell", "wheel_rim", "cell_anode", "lamp_filament", "pump_impeller", "compressor_rotor", "burner_nozzle", "crank_arm", "magnet_core", "store_core", "jar_glass", "electric_gun_barrel", "pulse_seat", "heat_governor", "hold_trigger", "pump_charge", "beam_lens", "zoom_coil", "heat_gauge", "flywheel_disc", "boiler_shell")
            .map(id -> ITEMS.registerItem(id, io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem::new))
            .toList();



    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularShield> SHIELD = ITEMS.register("shield",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularShield(ModularStacks.properties()));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularBow> BOW = ITEMS.register("bow",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularBow(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularCrossbow> CROSSBOW = ITEMS.register("crossbow",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularCrossbow(ModularStacks.properties()));
    /** A spring-piston air gun: a gas gun, not yet a {@code GunItem}. Cocked by hand, throws a metal pellet. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun> ELECTRIC_GUN = ITEMS.register("electric_gun",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun(ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularSpringGun> SPRING_GUN = ITEMS.register("spring_gun",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularSpringGun(ModularStacks.properties()));
    /** A placed explosive made of a casing and its filler (guns spec 13.4a). Not a tool: no durability. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.weapon.ModularCharge> CHARGE = ITEMS.register("charge",
            () -> new io.github.soundgoodizerfan.feedback.weapon.ModularCharge(new Item.Properties().stacksTo(16)));

    /** A meal put together at the Prep Table from a bowl and foods (dish spec): modular, baked once, eaten as vanilla food. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.prep.DishItem> DISH = ITEMS.register("dish",
            () -> new io.github.soundgoodizerfan.feedback.modular.prep.DishItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.prep.DishItem> SKEWER = ITEMS.register("skewer",
            () -> new io.github.soundgoodizerfan.feedback.modular.prep.DishItem(new Item.Properties().stacksTo(1)));
    /** A dish whose base is a pot serving (broth, stew) in a bowl (dish spec 2.1). */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.prep.DishItem> SOUP = ITEMS.register("soup",
            () -> new io.github.soundgoodizerfan.feedback.modular.prep.DishItem(new Item.Properties().stacksTo(1)));
    /** A dish whose container is a baked flatbread, a food and not a shaped part (food spec 6). */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.prep.DishItem> WRAP = ITEMS.register("wrap",
            () -> new io.github.soundgoodizerfan.feedback.modular.prep.DishItem(new Item.Properties().stacksTo(1)));

    /** A dish of a pastry shell, a filling and an optional lid, formed raw and baked whole (modular foods spec, pies). */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.prep.DishItem> PIE = ITEMS.register("pie",
            () -> new io.github.soundgoodizerfan.feedback.modular.prep.DishItem(new Item.Properties().stacksTo(1)));

    /** A dish of a dough, optional enrichments and a shape, formed raw and baked whole (foods spec 10). */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.prep.DishItem> BREAD = ITEMS.register("bread",
            () -> new io.github.soundgoodizerfan.feedback.modular.prep.DishItem(new Item.Properties().stacksTo(1)));

    /** Every tool held by a haft (pick, axe, shovel, hoe, sword, spear, maul, sickle, cleaver, hammer, knife, file, screwdriver): one item, whose faces decide what it does. Item id and type id are one. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularHafted> HAFTED_TOOL = ITEMS.register("hafted_tool",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularHafted(ModularStacks.properties()));
    /** Vanilla's axe, hoe and shovel use code, registered so the registry freezes and used only by {@code ModularHafted.useOn}; never held, never listed. */
    public static final DeferredItem<Item> AXE_DELEGATE = ITEMS.register("axe_delegate",
            () -> new net.minecraft.world.item.AxeItem(io.github.soundgoodizerfan.feedback.modular.type.ModularTier.INSTANCE, new Item.Properties()));
    public static final DeferredItem<Item> HOE_DELEGATE = ITEMS.register("hoe_delegate",
            () -> new net.minecraft.world.item.HoeItem(io.github.soundgoodizerfan.feedback.modular.type.ModularTier.INSTANCE, new Item.Properties()));
    public static final DeferredItem<Item> SHOVEL_DELEGATE = ITEMS.register("shovel_delegate",
            () -> new net.minecraft.world.item.ShovelItem(io.github.soundgoodizerfan.feedback.modular.type.ModularTier.INSTANCE, new Item.Properties()));
    /** The pan tool: right-click flips the piece under the cursor. */
    public static final DeferredItem<ModularItem> SPATULA = ITEMS.register("spatula",
            () -> new ModularItem(ModularStacks.properties()));
    /** The pot tool: held aimed at a stockpot it stirs, a tap skims. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularSpoon> SPOON = ITEMS.register("spoon",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularSpoon(ModularStacks.properties()));
    /** The stonecutter's tool: a bare blade, no handle. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularHandTool> SAWBLADE = ITEMS.register("sawblade",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularHandTool(ModularStacks.properties()));

    /** A pack worn on the back: a shell for the slots, a strap for the load. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBackpack> BACKPACK = ITEMS.register("backpack",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBackpack(ModularStacks.properties()));

    /** A sleeping roll carried on a pack's lashing: skips the night where you stand, by its comfort. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBed> BED = ITEMS.register("bed",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBed(ModularStacks.properties()));

    /** A hand covering in the hands slot: a glove, which may carry a metacarpal plate. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularGlove> GLOVE = ITEMS.register("glove",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularGlove(ModularStacks.properties()));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularShears> SHEARS = ITEMS.register("shears",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularShears(ModularStacks.properties()));

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

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularArmor> HELMET = ITEMS.register("helmet",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularArmor(net.minecraft.world.item.ArmorItem.Type.HELMET, ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularArmor> CHESTPLATE = ITEMS.register("chestplate",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularArmor(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularArmor> LEGGINGS = ITEMS.register("leggings",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularArmor(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, ModularStacks.properties()));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularArmor> BOOTS = ITEMS.register("boots",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularArmor(net.minecraft.world.item.ArmorItem.Type.BOOTS, ModularStacks.properties()));

    public static final DeferredItem<Item> BALANCE_SCALE = ITEMS.registerItem("balance_scale",
            io.github.soundgoodizerfan.feedback.item.BalanceScaleItem::new, new Item.Properties().stacksTo(1));
    /** Reads the draw and launch figures of a bow, a crossbow, a spring or a spring gun and stamps them on it. */
    public static final DeferredItem<Item> DRAW_GAUGE = ITEMS.registerItem("draw_gauge",
            io.github.soundgoodizerfan.feedback.item.DrawGaugeItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SURVEY_METER = ITEMS.registerItem("survey_meter",
            io.github.soundgoodizerfan.feedback.item.SurveyMeterItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> GEIGER_COUNTER = ITEMS.registerItem("geiger_counter",
            io.github.soundgoodizerfan.feedback.item.GeigerCounterItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> DOSIMETER = ITEMS.registerItem("dosimeter",
            io.github.soundgoodizerfan.feedback.item.DosimeterItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SCRATCH_FILES = ITEMS.registerItem("scratch_files",
            io.github.soundgoodizerfan.feedback.item.ScratchFilesItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> TEST_STRIP = ITEMS.registerItem("test_strip",
            io.github.soundgoodizerfan.feedback.item.TestStripItem::new, new Item.Properties().stacksTo(1));

    public static final net.neoforged.neoforge.registries.DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Feedback.MOD_ID);

    /** A framed bed set down in the world: a vanilla bed that keeps the stack it was placed from. */
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.part.ModularBedBlock> PLACED_BED =
            BLOCKS.register("placed_bed", () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBedBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.WOOL).strength(0.2f)
                            .sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion().ignitedByLava()
                            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    /** Where a modular item is drawn and its figures read before it is made (spec 6). */
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintTableBlock> BLUEPRINT_TABLE =
            BLOCKS.register("blueprint_table", () -> new io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintTableBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2.5f)
                            .sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> BLUEPRINT_TABLE_ITEM = ITEMS.registerSimpleBlockItem(BLUEPRINT_TABLE);

    /** A shelf for treatises; against a Blueprint Table, what it holds counts as held. */
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseShelfBlock> TREATISE_SHELF =
            BLOCKS.register("treatise_shelf", () -> new io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseShelfBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2f)
                            .sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> TREATISE_SHELF_ITEM = ITEMS.registerSimpleBlockItem(TREATISE_SHELF);


    /** Where dishes are made: the Tool Table's code with the dish filter and a knife slot (dish spec 3). */
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.prep.PrepTableBlock> PREP_TABLE =
            BLOCKS.register("prep_table", () -> new io.github.soundgoodizerfan.feedback.modular.prep.PrepTableBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2.5f)
                            .sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> PREP_TABLE_ITEM = ITEMS.registerSimpleBlockItem(PREP_TABLE);

    /** Where parts are put together, fitted, mended and written up (docs/dev/engineering.md). */
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlock> ENGINEERING_TABLE =
            BLOCKS.register("engineering_table", () -> new io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(3.5f)
                            .sound(net.minecraft.world.level.block.SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ENGINEERING_TABLE_ITEM = ITEMS.registerSimpleBlockItem(ENGINEERING_TABLE);

    /** The part-making benches: the Tool Table's code with a stock filter. The first two replace vanilla's smithing table and stonecutter. */
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.bench.StationBlock> SMITHING_TABLE =
            BLOCKS.register("smithing_table", () -> new io.github.soundgoodizerfan.feedback.modular.bench.StationBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(2.5f)
                            .sound(net.minecraft.world.level.block.SoundType.WOOD),
                    io.github.soundgoodizerfan.feedback.modular.bench.Station.SMITHING));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SMITHING_TABLE_ITEM = ITEMS.registerSimpleBlockItem(SMITHING_TABLE);
    /** Replaces vanilla's anvil: the smithing table's upgrade, which also forges, and the only bench a Hearth heats. */
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.bench.StationBlock> ANVIL =
            BLOCKS.register("anvil", () -> new io.github.soundgoodizerfan.feedback.modular.bench.AnvilStationBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(5.0f, 1200.0f)
                            .sound(net.minecraft.world.level.block.SoundType.ANVIL).noOcclusion()
                            .requiresCorrectToolForDrops()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ANVIL_ITEM = ITEMS.registerSimpleBlockItem(ANVIL);
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.bench.StationBlock> WOODWORKING_TABLE =
            BLOCKS.register("woodworking_table", () -> new io.github.soundgoodizerfan.feedback.modular.bench.StationBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2.5f)
                            .sound(net.minecraft.world.level.block.SoundType.WOOD),
                    io.github.soundgoodizerfan.feedback.modular.bench.Station.WOODWORKING));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> WOODWORKING_TABLE_ITEM = ITEMS.registerSimpleBlockItem(WOODWORKING_TABLE);
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.bench.StationBlock> STONECUTTER =
            BLOCKS.register("stonecutter", () -> new io.github.soundgoodizerfan.feedback.modular.bench.StationBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.STONE).strength(3.5f)
                            .sound(net.minecraft.world.level.block.SoundType.STONE).noOcclusion()
                            .requiresCorrectToolForDrops(),
                    io.github.soundgoodizerfan.feedback.modular.bench.Station.STONECUTTING));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> STONECUTTER_ITEM = ITEMS.registerSimpleBlockItem(STONECUTTER);

    /** Replaces vanilla's enchanting table (docs/specs/feedback_enchanting_spec.md): parts are attuned here, not rolled. */
    public static final net.neoforged.neoforge.registries.DeferredBlock<io.github.soundgoodizerfan.feedback.modular.enchant.EnchantingTableBlock> ENCHANTING_TABLE =
            BLOCKS.register("enchanting_table", () -> new io.github.soundgoodizerfan.feedback.modular.enchant.EnchantingTableBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED).strength(5.0f, 1200.0f)
                            .sound(net.minecraft.world.level.block.SoundType.STONE).lightLevel(state -> 7)
                            .noOcclusion().requiresCorrectToolForDrops()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ENCHANTING_TABLE_ITEM = ITEMS.registerSimpleBlockItem(ENCHANTING_TABLE);

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE, Feedback.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<io.github.soundgoodizerfan.feedback.modular.part.ModularBedBlockEntity>> BED_ENTITY =
            BLOCK_ENTITIES.register("placed_bed", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.modular.part.ModularBedBlockEntity::new, PLACED_BED.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseShelfBlockEntity>> TREATISE_SHELF_ENTITY =
            BLOCK_ENTITIES.register("treatise_shelf", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseShelfBlockEntity::new, TREATISE_SHELF.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<io.github.soundgoodizerfan.feedback.modular.prep.PrepTableBlockEntity>> PREP_TABLE_ENTITY =
            BLOCK_ENTITIES.register("prep_table", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.modular.prep.PrepTableBlockEntity::new, PREP_TABLE.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlockEntity>> ENGINEERING_TABLE_ENTITY =
            BLOCK_ENTITIES.register("engineering_table", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlockEntity::new, ENGINEERING_TABLE.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<io.github.soundgoodizerfan.feedback.modular.bench.StationBlockEntity>> STATION_ENTITY =
            BLOCK_ENTITIES.register("station", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.modular.bench.StationBlockEntity::new,
                            SMITHING_TABLE.get(), ANVIL.get(), WOODWORKING_TABLE.get(), STONECUTTER.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<io.github.soundgoodizerfan.feedback.modular.enchant.EnchantingTableBlockEntity>> ENCHANTING_TABLE_ENTITY =
            BLOCK_ENTITIES.register("enchanting_table", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(io.github.soundgoodizerfan.feedback.modular.enchant.EnchantingTableBlockEntity::new, ENCHANTING_TABLE.get()).build(null));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.blueprint.ModularBlueprintItem> BLUEPRINT = ITEMS.register("modular_blueprint",
            () -> new io.github.soundgoodizerfan.feedback.modular.blueprint.ModularBlueprintItem(new Item.Properties().stacksTo(16)));

    /** The volumes of each {@link io.github.soundgoodizerfan.feedback.modular.treatise.School}'s treatise, in order: holding one lets the Blueprint Table draw the types that name that volume. */
    public static final java.util.Map<io.github.soundgoodizerfan.feedback.modular.treatise.School, java.util.List<DeferredItem<io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseItem>>> TREATISES;

    static {
        java.util.Map<io.github.soundgoodizerfan.feedback.modular.treatise.School, java.util.List<DeferredItem<io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseItem>>> map =
                new java.util.EnumMap<>(io.github.soundgoodizerfan.feedback.modular.treatise.School.class);
        for (io.github.soundgoodizerfan.feedback.modular.treatise.School school : io.github.soundgoodizerfan.feedback.modular.treatise.School.values()) {
            java.util.List<DeferredItem<io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseItem>> volumes = new java.util.ArrayList<>();
            for (int v = 1; v <= school.volumes(); v++) {
                int volume = v;
                volumes.add(ITEMS.register(school.itemId(v),
                        () -> new io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseItem(school, volume, new Item.Properties().stacksTo(16))));
            }
            map.put(school, java.util.Collections.unmodifiableList(volumes));
        }
        TREATISES = java.util.Collections.unmodifiableMap(map);
    }

    /** Paper that becomes a school's treatise when a specimen of that school is taken apart onto it. */
    public static final DeferredItem<Item> BLANK_TREATISE = ITEMS.registerSimpleItem("blank_treatise", new Item.Properties().stacksTo(1));
    /** Every volume of every treatise at once, later ones included: operator tab only. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.treatise.CreativeTreatiseItem> CREATIVE_TREATISE = ITEMS.register("creative_treatise",
            () -> new io.github.soundgoodizerfan.feedback.modular.treatise.CreativeTreatiseItem(new Item.Properties().stacksTo(1)));
    /** A binder of treatises that works like a bundle. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseBinderItem> TREATISE_BINDER = ITEMS.register("treatise_binder",
            () -> new io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseBinderItem(new Item.Properties().stacksTo(1)
                    .component(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS, net.minecraft.world.item.component.BundleContents.EMPTY)));
    /** Adds a treatise to a chest. */
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>,
            com.mojang.serialization.MapCodec<io.github.soundgoodizerfan.feedback.modular.treatise.Treatises.Loot>> TREATISE_LOOT =
            LOOT_MODIFIERS.register("treatise", () -> io.github.soundgoodizerfan.feedback.modular.treatise.Treatises.Loot.CODEC);

    /** A suit upgrade module (family {@code suit_module}): built at the Tool Table from a casing, wiring and a core, then held whole in an armour piece's upgrade role. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.item.ModularItem> SUIT_MODULE = ITEMS.register("suit_module",
            () -> new io.github.soundgoodizerfan.feedback.modular.item.ModularItem(new Item.Properties().stacksTo(1)));

    /** A book of plan sheets that works like a bundle ({@link io.github.soundgoodizerfan.feedback.modular.blueprint.ModularBlueprints}). */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintBookItem> BLUEPRINT_BOOK = ITEMS.register("blueprint_book",
            () -> new io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintBookItem(new Item.Properties().stacksTo(1)
                    .component(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS, net.minecraft.world.item.component.BundleContents.EMPTY)));


    /** The crystal vial that holds experience (progression roadmap L2); its store is the {@code XP_STORE} component. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.XpContainerItem> XP_CONTAINER = ITEMS.register("xp_container",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.XpContainerItem(new Item.Properties().stacksTo(1)));

    /** The lead-acid battery (roadmap E4): a Tool Table build that places as the {@code battery} block. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.BatteryItem> BATTERY = ITEMS.register("battery",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.BatteryItem(FBlocks.BATTERY.get(), new Item.Properties().stacksTo(1)));

    /** Fixed blocks made from parts ({@code BlockFigures}): each places as its block and carries the figures it was baked to. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> HEAT_EXCHANGER = ITEMS.register("heat_exchanger",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.HEAT_EXCHANGER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> GAS_TANK = ITEMS.register("gas_tank",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.GAS_TANK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> WATER_WHEEL = ITEMS.register("water_wheel",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.WATER_WHEEL.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> THERMAL_STORE = ITEMS.register("thermal_store",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.THERMAL_STORE.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> LEYDEN_JAR = ITEMS.register("leyden_jar",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.LEYDEN_JAR.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> PUMP = ITEMS.register("pump",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.PUMP.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> COMPRESSOR = ITEMS.register("compressor",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.COMPRESSOR.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> BURNER = ITEMS.register("burner",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.BURNER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> THRUSTER = ITEMS.register("thruster",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(io.github.soundgoodizerfan.feedback.registry.FVehicles.THRUSTER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> HAND_CRANK = ITEMS.register("hand_crank",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.HAND_CRANK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> ELECTROMAGNET = ITEMS.register("electromagnet",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.ELECTROMAGNET.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> GALVANIC_CELL = ITEMS.register("galvanic_cell",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.GALVANIC_CELL.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> LAMP = ITEMS.register("lamp",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.LAMP.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> BOILER = ITEMS.register("boiler",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.BOILER.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem> FLYWHEEL = ITEMS.register("flywheel",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem(FBlocks.FLYWHEEL.get(), new Item.Properties().stacksTo(1)));

    /** The pressure vessel: a Tool Table build that places as the {@code pressure_vessel} block. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.PressureVesselItem> PRESSURE_VESSEL = ITEMS.register("pressure_vessel",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.PressureVesselItem(FBlocks.PRESSURE_VESSEL.get(), new Item.Properties().stacksTo(1)));

    /** The motor and dynamo (roadmap E5): a Tool Table build that places as the {@code dynamotor} block. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.DynamotorItem> DYNAMOTOR = ITEMS.register("dynamotor",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.DynamotorItem(FBlocks.DYNAMOTOR.get(), new Item.Properties().stacksTo(1)));

    /** The conveyor belt: a Tool Table build that places as the {@code conveyor_belt} block. See {@code logistics/belt}. */
    public static final DeferredItem<io.github.soundgoodizerfan.feedback.modular.part.BeltItem> CONVEYOR_BELT = ITEMS.register("conveyor_belt",
            () -> new io.github.soundgoodizerfan.feedback.modular.part.BeltItem(FLogistics.BELT.get(), new Item.Properties().stacksTo(16)));

    private FModular() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        SERIALIZERS.register(modBus);
        ARMOUR_MATERIALS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
    }

    /** Every item this registers but the parts, which mean nothing without a material: for the creative tab and the tint. */
    public static java.util.List<Item> items() {
        return ITEMS.getEntries().stream().<Item>map(DeferredHolder::get)
                .filter(i -> !(i instanceof io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem)
                        && i != AXE_DELEGATE.get() && i != HOE_DELEGATE.get() && i != SHOVEL_DELEGATE.get()).toList();
    }
}
