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
                else if (g.block() != null)
                {
                    ResourceLocation sprite = Feedback.id("block/form/block");
                    var model = models().getBuilder(g.block().getId().getPath()).parent(models().getExistingFile(mcLoc("block/block")))
                            .texture("particle", sprite).texture("all", sprite);
                    model.element().from(0, 0, 0).to(16, 16, 16)
                            .allFaces((direction, face) -> face.texture("#all").cullface(direction).tintindex(0));
                    simpleBlockWithItem(g.block().get(), model);
                }
        }
    }

    public static LootTableProvider loot(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(Loot::new, LootContextParamSets.BLOCK)), lookup);
    }

    private static class Loot extends BlockLootSubProvider {
        Loot(HolderLookup.Provider lookup) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), lookup);
        }

        @Override
        protected void generate() {
            FormItems.all().stream().filter(g -> g.block() != null).forEach(g -> {
                Block block = g.block().get();
                var lump = FormItems.materials().get(g.material()).item(g.material(), "lump");
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
            var raw = java.util.stream.Stream.of("raw", "lump", "gem").map(f -> material.item(id, f))
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

    public static class BlockTagsGen extends BlockTagsProvider {
        public BlockTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, Feedback.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            blocks().forEach(block -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block));
            // Vanilla-replacing blocks: same tool tag as the vanilla block they stand in for.
            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.FURNACE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.SMOKER.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.BLAST_FURNACE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FModular.ENCHANTING_TABLE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FModular.STONECUTTER.get());
            tag(BlockTags.MINEABLE_WITH_AXE)
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.CAMPFIRE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.SOUL_CAMPFIRE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FModular.SMITHING_TABLE.get());
            FormItems.all().stream().filter(g -> g.block() != null && g.form().endsWith("ore"))
                    .forEach(g -> {
                        tag(BlockTags.NEEDS_STONE_TOOL).add(g.block().get());
                        // c:ores is what the miner's pick (Vein) and other mods look for.
                        tag(net.neoforged.neoforge.common.Tags.Blocks.ORES).add(g.block().get());
                    });
            // Hand-registered ore blocks (mechanics §9.4), not generated forms.
            // Each ore needs the tool tier of the vanilla block it replaces: iron and copper a
            // stone tool, gold an iron one, coal and graphite any pickaxe (no needs tag).
            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.GRAPHITE_ORE.get())
                    .add(io.github.soundgoodizerfan.feedback.registry.FBlocks.DEEPSLATE_GRAPHITE_ORE.get())
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
                    FBlocks.GRAPHITE_ORE, FBlocks.DEEPSLATE_GRAPHITE_ORE, FBlocks.COAL_ORE, FBlocks.DEEPSLATE_COAL_ORE,
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
                compress(output, e.getKey(), e.getValue(), "nugget", "ingot");
                compress(output, e.getKey(), e.getValue(), "ingot", "block");
            }
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
