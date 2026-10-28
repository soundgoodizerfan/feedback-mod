# jei-hide-removed-vanilla

Base: this branch (`all-in-one`). Other uncommitted changes in the tree (polymer block form, `handoff/polymer-block-form.md`) are unrelated; do not touch them.

## Goal
Vanilla items the mod removed must not show in JEI's ingredient list or have recipe pages. Today JEI still lists the vanilla campfire, enchanting table, furnace, vanilla tools and so on, which can no longer be crafted or used.

## How the mod removes vanilla things (read this, do not re-derive)
- Vanilla recipes are disabled by overriding them in `src/main/resources/data/minecraft/recipe/*.json` with `"neoforge:conditions": [{"type": "neoforge:false"}]` (example: `furnace.json`, `diamond_axe.json`). 80 files. The `result.id` in each is the item that is no longer craftable.
- Smelting/blasting/smoking JEI *categories* are already hidden in `src/main/java/io/github/soundgoodizerfan/feedback/compat/jei/FeedbackJeiPlugin.java` `onRuntimeAvailable` (~line 264, `recipes.hideRecipeCategory(...)`). Use that method; JEI runtime there has `jeiRuntime.getIngredientManager()` for `removeIngredientsAtRuntime`.
- Vanilla furnace/smoker/blast furnace/campfire are swapped for the mod's own vessels in the world (`worldgen/VanillaVesselSwap.java`). The enchanting table, smithing table and stonecutter are replaced by mod blocks too. Tool Table is gone; stations are `feedback:smithing_table`, `feedback:woodworking_table`, `feedback:stonecutter` (`registry/FModular`).

## Files
Read first: `FeedbackJeiPlugin.java` (`onRuntimeAvailable`), `data/minecraft/recipe/` (list the `result.id` of every file), `data/minecraft/tags/block/campfires.json`.
Edit: `FeedbackJeiPlugin.java`.
Create: item tag `src/main/resources/data/feedback/tags/item/hidden_from_jei.json` listing the vanilla items to hide. The plugin hides every member of that tag (so a pack can extend it; do not hardcode ids in Java).

## Spec
1. Build the tag from the `result.id` of the disabled recipes, EXCEPT: keep `minecraft:iron_ingot`, `copper_ingot`, `gold_ingot` (smelting results; the items are still real). Netherite tools and armor (`netherite_*_smithing.json`) ARE hidden too (user ruling): they upgrade a diamond tool and no diamond tool can be obtained, so they are unreachable. Include their `result.id`s.
2. Also add vanilla `minecraft:campfire`, `minecraft:soul_campfire`, `minecraft:enchanting_table`, `minecraft:furnace`, `minecraft:blast_furnace`, `minecraft:smoker`, `minecraft:smithing_table`, `minecraft:stonecutter` if they are not already covered (check whether the mod's own replacements have different item ids; if a replacement reuses the vanilla item id, do NOT hide it and say so under Questions).
3. In `onRuntimeAvailable`, hide those items with `IIngredientManager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks)` (build stacks from the tag via `BuiltInRegistries.ITEM`/tag lookup on the client; if the tag is not available at that moment, say so under Questions rather than hardcoding).
4. Recipes for hidden items: also hide their crafting recipes if any still appear (the `neoforge:false` ones should already be absent; just confirm).
5. When unsure whether an item is still obtainable, do not hide it; list it under Questions.

## Out of scope
Any gameplay change, any recipe edit, `src/generated/**`, docs, other JEI categories.

## Done when
- [ ] `./gradlew compileJava --offline` passes
- [ ] report lists every item in the tag and every item you chose not to hide, with a reason

## Questions

## Report
