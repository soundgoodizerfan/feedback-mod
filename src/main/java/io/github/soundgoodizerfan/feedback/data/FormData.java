// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.process.FormType;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Datagen for generated block forms (storage blocks) and the compression recipes between a
 * metal's nugget, ingot and block. Item models for item forms stay in {@link FormItemModels}.
 */
public final class FormData {

    private FormData() {}

    private static List<Block> blocks() {
        return FormItems.all().stream().filter(g -> g.block() != null).map(g -> (Block) g.block().get()).toList();
    }

    /** One greyscale cube sprite, tinted per material by {@code client/FormTint}. */
    public static class BlockStates extends BlockStateProvider {
        public BlockStates(PackOutput output, ExistingFileHelper helper) {
            super(output, Feedback.MOD_ID, helper);
        }

        @Override
        protected void registerStatesAndModels() {
            for (FormItems.Generated g : FormItems.all())
                if (g.block() != null && FormType.overlaid(g.form()))
                {
                    boolean loose = g.form().equals("loose_rock");
                    boolean deepslate = g.form().startsWith("deepslate");
                    // Ores found in the Nether sit in netherrack.
                    String stone = g.material().getPath().equals("ilmenite") && !loose ? "netherrack" : loose ? "cobblestone" : deepslate ? "deepslate" : "stone";
                    var model = models().getBuilder(g.block().getId().getPath())
                            .parent(models().getExistingFile(mcLoc("block/block")))
                            .renderType("cutout")
                            .texture("particle", mcLoc("block/" + stone))
                            .texture("base", mcLoc("block/" + stone))
                            .texture("fleck", Feedback.id("block/ore_overlay_" + Math.floorMod(g.material().getPath().hashCode(), 4)));
                    float lo = loose ? 3 : 0, hi = loose ? 13 : 16, top = loose ? 7 : 16, pad = loose ? 0.02f : 0.02f;
                    model.element().from(lo, 0, lo).to(hi, top, hi)
                            .allFaces((direction, face) -> face.texture("#base").cullface(loose ? null : direction));
                    model.element().from(lo - pad, -pad, lo - pad).to(hi + pad, top + pad, hi + pad)
                            .allFaces((direction, face) -> face.texture("#fleck").cullface(loose ? null : direction).tintindex(0));
                    simpleBlockWithItem(g.block().get(), model);
                }
                else if (g.block() != null && g.form().equals("pipe"))
                {
                    var core = models().getExistingFile(Feedback.id("block/pipe_core"));
                    var arm = models().getExistingFile(Feedback.id("block/pipe_arm"));
                    var builder = getMultipartBuilder(g.block().get());
                    builder.part().modelFile(core).addModel();
                    int[][] turns = {{0, 0}, {0, 90}, {0, 180}, {0, 270}, {270, 0}, {90, 0}};
                    String[] arms = {"north", "east", "south", "west", "up", "down"};
                    for (int i = 0; i < arms.length; i++)
                        builder.part().modelFile(arm).rotationX(turns[i][0]).rotationY(turns[i][1]).addModel()
                                .condition(net.minecraft.world.level.block.state.properties.BooleanProperty.create(arms[i]), true);
                    itemModels().getBuilder(g.item().getId().getPath()).parent(models().getExistingFile(Feedback.id("block/pipe_inventory")));
                }
                else if (g.block() != null)
                {
                    var rule = FormTexture.find(FormItems.materials().get(g.material()), g.form());
                    ResourceLocation sprite = rule.map(FormTexture::sprite).orElse(Feedback.id("block/form/block"));
                    var model = models().getBuilder(g.block().getId().getPath()).parent(models().getExistingFile(mcLoc("block/block")))
                            .texture("particle", sprite).texture("all", sprite);
                    rule.flatMap(FormTexture::renderType).ifPresent(model::renderType);
                    model.element().from(0, 0, 0).to(16, 16, 16)
                            .allFaces((direction, face) -> face.texture("#all").cullface(direction).tintindex(0));
                    simpleBlockWithItem(g.block().get(), model);
                }
        }
    }

    public static LootTableProvider loot(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        // The loot provider has no way to attach conditions, so a block of a conditional material gets its material's on the way to disk.
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(Loot::new, LootContextParamSets.BLOCK)), lookup) {
            @Override
            public CompletableFuture<?> run(net.minecraft.data.CachedOutput real) {
                return super.run((path, data, hash) -> {
                    String name = path.getFileName().toString();
                    var conditions = name.endsWith(".json") && path.getParent() != null && path.getParent().getFileName().toString().equals("blocks")
                            ? FormItems.conditionsOf(name.substring(0, name.length() - ".json".length())) : null;
                    if (conditions == null) {
                        real.writeIfNeeded(path, data, hash);
                        return;
                    }
                    var table = com.google.gson.JsonParser.parseString(new String(data, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    var stamped = new com.google.gson.JsonObject();
                    stamped.add(io.github.soundgoodizerfan.feedback.core.LoadConditions.KEY, conditions);
                    table.entrySet().forEach(e -> stamped.add(e.getKey(), e.getValue()));
                    byte[] bytes = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(stamped).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    real.writeIfNeeded(path, bytes, com.google.common.hash.Hashing.sha1().hashBytes(bytes));
                });
            }
        };
    }

    /** A material file's {@code neoforge:conditions} as conditions to stamp on generated data. */
    static net.neoforged.neoforge.common.conditions.ICondition[] conditions(com.google.gson.JsonArray array) {
        var list = new java.util.ArrayList<net.neoforged.neoforge.common.conditions.ICondition>();
        array.forEach(c -> list.add(net.neoforged.neoforge.common.conditions.ICondition.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, c).getOrThrow()));
        return list.toArray(net.neoforged.neoforge.common.conditions.ICondition[]::new);
    }

    private static class Loot extends BlockLootSubProvider {
        Loot(HolderLookup.Provider lookup) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), lookup);
        }

        @Override
        protected void generate() {
            FormItems.all().stream().filter(g -> g.block() != null).forEach(g -> {
                Block block = g.block().get();
                var material = FormItems.materials().get(g.material());
                // A cut stone is what a gem's ore gives up (a gemstone has one); everything else gives its raw lump.
                var lump = material.traits().contains(Material.Trait.GEMSTONE) && material.item(g.material(), "gem").isPresent()
                        ? material.item(g.material(), "gem") : material.item(g.material(), "lump");
                if (g.form().equals("loose_rock"))
                    add(block, looseRockDrops(block, g.material()));
                else if (g.form().endsWith("ore") && lump.isPresent())
                    add(block, createOreDrop(block, lump.get()));
                else
                    dropSelf(block);
            });
        }

        /**
         * Silk touch keeps the rock. Otherwise it gives up what the ore holds, a couple of nuggets
         * of the metal it bears up to a whole piece of the raw ore: nuggets (two to four, or five to
         * eight, the likelier the fewer) or one raw. An ore with no metal in it (a gem, coal) gives
         * its one piece about half the time.
         */
        private net.minecraft.world.level.storage.loot.LootTable.Builder looseRockDrops(Block block, ResourceLocation id) {
            Material material = FormItems.materials().get(id);
            var raw = java.util.stream.Stream.of("gem", "raw", "lump").map(f -> material.item(id, f))
                    .flatMap(Optional::stream).findFirst();
            var metalId = material.bears().entrySet().stream()
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(id);
            var nugget = Optional.ofNullable(FormItems.materials().get(metalId)).flatMap(m -> m.item(metalId, "nugget"));
            var pool = net.minecraft.world.level.storage.loot.LootPool.lootPool().setRolls(
                    net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                    .when(hasSilkTouch().invert());
            if (nugget.isPresent()) {
                pool.add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(nugget.get()).setWeight(4)
                        .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(
                                net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(2, 4))));
                pool.add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(nugget.get()).setWeight(2)
                        .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(
                                net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(5, 8))));
                raw.ifPresent(item -> pool.add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(item).setWeight(1)));
            } else {
                raw.ifPresent(item -> pool.add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(item).setWeight(1)));
                pool.add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(1));
            }
            return net.minecraft.world.level.storage.loot.LootTable.lootTable()
                    .withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool().setRolls(
                            net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                            .when(hasSilkTouch()).add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block)))
                    .withPool(pool);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return blocks();
        }
    }

    /**
     * The common tag folder of a form: {@code c:<folder>/<material>}. A block form is {@code storage_blocks}, an ore {@code ores};
     * every other form is its id pluralised ({@code ingot} to {@code ingots}, {@code tailings} stays), so a form needs no table entry.
     */
    public static String tagFolder(String form) {
        return switch (form) {
            case "block" -> "storage_blocks";
            case "ore" -> "ores";
            case "crushed" -> "crushed";
            default -> form.endsWith("s") ? form : form + "s";
        };
    }

    /** An entry of a tag: a block or item of a conditional material is optional, so the tag still loads when its mod is absent and the thing is not registered. */
    static <T> void put(net.minecraft.data.tags.IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> tag,
                                net.minecraft.core.Registry<T> registry, T entry) {
        ResourceLocation id = registry.getKey(entry);
        if (Feedback.MOD_ID.equals(id.getNamespace()) && FormItems.conditionsOf(id.getPath()) != null)
            tag.addOptional(id);
        else
            tag.add(entry);
    }

    private static ResourceLocation formTag(Material.Located located) {
        return ResourceLocation.fromNamespaceAndPath("c", tagFolder(located.form()) + "/" + located.material().tagName(located.id(), located.form()));
    }

    /** Every generated form item is in {@code c:<form>s/<material>} (neoforge's common convention; the prefix is {@code c}, not {@code forge}). */
    public static class ItemTagsGen extends net.minecraft.data.tags.ItemTagsProvider {
        public ItemTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                           CompletableFuture<net.minecraft.data.tags.TagsProvider.TagLookup<Block>> blockTags, ExistingFileHelper helper) {
            super(output, lookup, blockTags, Feedback.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            FormItems.all().forEach(g -> put(tag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    formTag(new Material.Located(g.material(), FormItems.materials().get(g.material()), g.form())))),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM, g.item().get()));
            listed((located, id) -> tag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, formTag(located))).addOptional(id));
        }
    }

    /** Every form a material lists by item id (hand-registered or vanilla) that names a generated-style form type; association-only names ({@code sensor}) are skipped, and shared part items cannot say whose material they are. */
    private static void listed(java.util.function.BiConsumer<Material.Located, ResourceLocation> out) {
        FormItems.materials().forEach((matId, material) -> material.forms().forEach((form, id) -> {
            FormType type = FormItems.types().get(form);
            if (type != null && !type.part())
                out.accept(new Material.Located(matId, material, form), id);
        }));
    }

    public static class BlockTagsGen extends BlockTagsProvider {
        public BlockTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, Feedback.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            // A generated block form is also in its form's common block tag (c:storage_blocks/<material>).
            FormItems.all().stream().filter(g -> g.block() != null).forEach(g -> put(tag(net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.BLOCK,
                    formTag(new Material.Located(g.material(), FormItems.materials().get(g.material()), g.form())))),
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK, g.block().get()));
            listed((located, id) -> {
                if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id))
                    tag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, formTag(located))).addOptional(id);
            });
            // A block of foodstuff is cut with a hoe, as vanilla's soft organic blocks are; the rest are stone and metal.
            FormItems.all().stream().filter(g -> g.block() != null).forEach(g -> {
                boolean food = FormItems.materials().get(g.material()).food().isPresent();
                put(tag(food ? BlockTags.MINEABLE_WITH_HOE : BlockTags.MINEABLE_WITH_PICKAXE), net.minecraft.core.registries.BuiltInRegistries.BLOCK, g.block().get());
            });
            // Vanilla-replacing blocks: same tool tag as the vanilla block they stand in for.
            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.FURNACE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.SMOKER.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.BLAST_FURNACE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FModular.ENCHANTING_TABLE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FModular.STONECUTTER.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FModular.ANVIL.get());
            tag(BlockTags.MINEABLE_WITH_AXE)
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.CAMPFIRE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.SOUL_CAMPFIRE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FModular.SMITHING_TABLE.get());
            // Everything else Feedback registers (machines, stations, wires) gets a tool so it is not
            // hand-mined at bare-hand speed. Instant-break plants, fluids and shovel soils stay out.
            java.util.Set<String> wooden = java.util.Set.of("drying_rack", "woodworking_table",
                    "prep_table", "blueprint_table", "ice_box", "thresher");
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.entrySet().stream()
                    // Registry order varies per run; sort so the generated tags are byte-stable.
                    .sorted(java.util.Comparator.comparing(e -> e.getKey().location().toString()))
                    .filter(e -> e.getKey().location().getNamespace().equals(Feedback.MOD_ID))
                    .forEach(e -> {
                        Block b = e.getValue();
                        var st = b.defaultBlockState();
                        if (b instanceof net.minecraft.world.level.block.LiquidBlock
                                || st.getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                                        net.minecraft.core.BlockPos.ZERO) <= 0.2f
                                || st.is(BlockTags.MINEABLE_WITH_SHOVEL) || b instanceof io.github.soundgoodizerfan.feedback.item.FormBlock fb && fb.food()) return;
                        String path = e.getKey().location().getPath();
                        put(tag(wooden.contains(path)
                                ? BlockTags.MINEABLE_WITH_AXE : BlockTags.MINEABLE_WITH_PICKAXE), net.minecraft.core.registries.BuiltInRegistries.BLOCK, b);
                        // A machine housing is built stuff, not terrain: Thaumaturge's taint does not convert it (interop tag, a no-op without that mod).
                        // A portable hole does not eat one either: it holds a tank, a charge or a heat that a hole would spill.
                        if (b instanceof net.minecraft.world.level.block.EntityBlock) {
                            put(tag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumaturge", "taint_conversion_immune"))),
                                    net.minecraft.core.registries.BuiltInRegistries.BLOCK, b);
                            put(tag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumaturge", "portable_hole_blacklist"))),
                                    net.minecraft.core.registries.BuiltInRegistries.BLOCK, b);
                        }
                        // Bolted machines and pipes come apart fast with a wrench; wires and stations do not.
                        if (!wooden.contains(path) && !path.equals("cable") && !path.equals("fence")
                                && !path.equals("xp_wire")
                                && (b instanceof net.minecraft.world.level.block.EntityBlock || path.equals("lift"))) {
                            put(tag(io.github.soundgoodizerfan.feedback.item.WrenchItem.MINEABLE), net.minecraft.core.registries.BuiltInRegistries.BLOCK, b);
                        }
                    });
            FormItems.all().stream().filter(g -> g.block() != null && g.form().endsWith("ore"))
                    .forEach(g -> {
                        tag(BlockTags.NEEDS_STONE_TOOL).add(g.block().get());
                        // c:ores is what the miner's pick (Vein) and other mods look for.
                        tag(net.neoforged.neoforge.common.Tags.Blocks.ORES).add(g.block().get());
                    });
            // Hand-registered ore blocks (mechanics §9.4), not generated forms.
            // Each ore needs the tool tier of the vanilla block it replaces: iron and copper a
            // stone tool, gold an iron one, coal and graphite any pickaxe (no needs tag).
            tag(BlockTags.MINEABLE_WITH_SHOVEL).add(io.github.soundgoodizerfan.feedback.registry.FBlocks.REGOLITH.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.NITROGEN_ICE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.SULFUR_CRUST.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.LOUDSPEAKER.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.CRYSTAL_ROCK.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.GRAPHITE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_GRAPHITE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.SALTPETRE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_SALTPETRE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.SULFUR_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.COAL_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_COAL_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.IRON_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_IRON_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.COPPER_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_COPPER_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.GOLD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_GOLD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.LAPIS_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_LAPIS_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.REDSTONE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_REDSTONE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DIAMOND_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_DIAMOND_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.EMERALD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_EMERALD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.NETHER_GOLD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.NETHER_QUARTZ_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.TIN_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_TIN_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.ZINC_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_ZINC_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.LEAD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_LEAD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.GALENA_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.PYRITE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.KAOLINITE_DEPOSIT.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.LEAD_CHEST.get());
            var ores = tag(net.neoforged.neoforge.common.Tags.Blocks.ORES);
            for (var ore : java.util.List.of(
                    FBlocks.GRAPHITE_ORE, FBlocks.DEEPSLATE_GRAPHITE_ORE, FBlocks.SALTPETRE_ORE, FBlocks.DEEPSLATE_SALTPETRE_ORE,
                    FBlocks.SULFUR_ORE, FBlocks.COAL_ORE, FBlocks.DEEPSLATE_COAL_ORE,
                    FBlocks.IRON_ORE, FBlocks.DEEPSLATE_IRON_ORE, FBlocks.COPPER_ORE, FBlocks.DEEPSLATE_COPPER_ORE,
                    FBlocks.GOLD_ORE, FBlocks.DEEPSLATE_GOLD_ORE, FBlocks.LAPIS_ORE, FBlocks.DEEPSLATE_LAPIS_ORE,
                    FBlocks.REDSTONE_ORE, FBlocks.DEEPSLATE_REDSTONE_ORE, FBlocks.DIAMOND_ORE, FBlocks.DEEPSLATE_DIAMOND_ORE,
                    FBlocks.EMERALD_ORE, FBlocks.DEEPSLATE_EMERALD_ORE, FBlocks.NETHER_GOLD_ORE, FBlocks.NETHER_QUARTZ_ORE,
                    FBlocks.TIN_ORE, FBlocks.DEEPSLATE_TIN_ORE, FBlocks.ZINC_ORE, FBlocks.DEEPSLATE_ZINC_ORE,
                    FBlocks.LEAD_ORE, FBlocks.DEEPSLATE_LEAD_ORE, FBlocks.GALENA_ORE, FBlocks.PYRITE_ORE))
                ores.add(ore.get());
            // Batch 2: lapis wants a stone tool; redstone, diamond and emerald an iron one; the
            // Nether's two take any pickaxe, as vanilla's do.
            tag(BlockTags.NEEDS_STONE_TOOL)
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.TIN_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_TIN_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.ZINC_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_ZINC_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.LEAD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_LEAD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.GALENA_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.PYRITE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.IRON_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_IRON_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.COPPER_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_COPPER_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.LAPIS_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_LAPIS_ORE.get());
            tag(BlockTags.NEEDS_IRON_TOOL)
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.GOLD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_GOLD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.REDSTONE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_REDSTONE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DIAMOND_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_DIAMOND_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.EMERALD_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_EMERALD_ORE.get());
        }
    }

    /**
     * Nine of the smaller form make one of the larger and back, for every material with both forms
     * where at least one of the pair was generated -- vanilla already pairs its own.
     */
    public static class Recipes extends RecipeProvider {
        public Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            for (Map.Entry<ResourceLocation, Material> e : FormItems.materials().entrySet()) {
                // A conditional material's recipes (and their advancements) carry its conditions, so they load only with its mod.
                RecipeOutput out = conditioned(output, FormItems.conditionsOf(e.getKey().getPath()));
                compress(out, e.getKey(), e.getValue(), "nugget", "ingot");
                compress(out, e.getKey(), e.getValue(), "ingot", "block");
            }
        }

        private static RecipeOutput conditioned(RecipeOutput output, com.google.gson.JsonArray conditions) {
            if (conditions == null)
                return output;
            return output.withConditions(FormData.conditions(conditions));
        }

        private static void compress(RecipeOutput output, ResourceLocation id, Material material,
                                     String small, String large) {
            Optional<Item> smallItem = material.item(id, small);
            Optional<Item> largeItem = material.item(id, large);
            if (smallItem.isEmpty() || largeItem.isEmpty() || !generated(id, small) && !generated(id, large))
                return;
            String path = id.getPath();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, largeItem.get())
                    .requires(smallItem.get(), 9)
                    .unlockedBy("has_" + small, has(smallItem.get()))
                    .save(output, Feedback.id(path + "_" + large + "_from_" + small));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, smallItem.get(), 9)
                    .requires(largeItem.get())
                    .unlockedBy("has_" + large, has(largeItem.get()))
                    .save(output, Feedback.id(path + "_" + small + "_from_" + large));
        }

        private static boolean generated(ResourceLocation id, String form) {
            return FormItems.all().stream().anyMatch(g -> g.material().equals(id) && g.form().equals(form));
        }
    }
}
