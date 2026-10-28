# tool-shop-assets

## Goal
Three part-making benches exist in code (branch `worktree-agent-ac5d8f3aa8c525475`; branch from it, not main, or the blocks do not exist): registry ids `feedback:smithing_table`, `feedback:woodworking_table` (new block) and `feedback:stonecutter`. The first and last replace the vanilla blocks (`VanillaVesselSwap` already swaps placed ones). This brief is the data, assets and wiki only. Needs no Java.

## Files
Read first: `src/main/resources/assets/feedback/blockstates/tool_table.json`, `models/block/tool_table.json`, `models/item/tool_table.json`, `data/feedback/loot_table/blocks/tool_table.json`, `data/feedback/recipe/tool_table.json`, `data/minecraft/recipe/enchanting_table.json` (the removed-vanilla-recipe pattern: `neoforge:false` condition), `wiki/Campfire.md` and `wiki/Enchanting_Table.md` (the "In vanilla / In Feedback" article shape), `wiki/Hearth.md`.

Create, for each of `smithing_table`, `woodworking_table`, `stonecutter`:
- `assets/feedback/blockstates/<id>.json` (single variant), `assets/feedback/models/item/<id>.json`, `data/feedback/loot_table/blocks/<id>.json` (drops itself, as the tool table's).
- `smithing_table`: blockstate model `minecraft:block/smithing_table`; item model parent the same. `stonecutter`: blockstate model `minecraft:block/stonecutter`, item model parent the same. No block model files, no new textures (vanilla models and textures are referenced from the vanilla jar, not copied).
- `woodworking_table`: `assets/feedback/models/block/woodworking_table.json`, `cube_bottom_top`, using only textures that already exist under `assets/feedback/textures/block/` (`plank_plywood` for side and bottom; pick an existing one for the top) or vanilla ones by `minecraft:block/...` reference. No new image file, so no THIRD-PARTY-LICENSES.md row.
- `data/feedback/recipe/`: `smithing_table.json` shaped `["II","PP","PP"]`, I = `feedback:iron_plate` (check it exists under `data/feedback/recipe` or the form items; if the plate id differs, use the real one), P = tag `minecraft:planks`. `woodworking_table.json` shaped `["C ","PP","PP"]`, C = `feedback:copper_plate`, P = planks. `stonecutter.json` shaped `[" I ","SSS"]`, I = iron plate, S = tag `c:cobblestones`. Results are the feedback ids.
- `data/minecraft/recipe/smithing_table.json` and `data/minecraft/recipe/stonecutter.json`: copy the vanilla recipe body, add `"neoforge:conditions": [{"type":"neoforge:false"}]` exactly as `enchanting_table.json` does (vanilla bodies: smithing table = 2 iron ingots over 4 planks `["II","##","##"]`; stonecutter = `[" I ","###"]`, I = iron ingot, # = stone).
- Lang, in `assets/feedback/lang/en_us.json` next to `block.feedback.tool_table`: `block.feedback.smithing_table` "Smithing Table", `block.feedback.woodworking_table` "Woodworking Table", `block.feedback.stonecutter` "Stonecutter"; `gui.feedback.tool_table.tab.cut` "Cut"; `gui.feedback.tool_table.status.put_cut` "Put the block to cut in the first cell."; `gui.feedback.tool_table.status.pick_cut` "Pick what to cut it into."
- Wiki, one page each (`wiki/Smithing_Table.md`, `wiki/Woodworking_Table.md`, `wiki/Stonecutter.md`), link them from `wiki/Home.md` where the other tables are linked, and add a line to `wiki/Hearth.md` that it now heats the smithing table too (it already heats any table beside it). Content:
  - Smithing Table, "In vanilla": upgrades diamond gear to netherite and applies armor trims with smithing templates. "In Feedback": that use is gone (no smithing GUI of the old kind); it is where metal parts are shaped and forged (the forging rhythm game), needing a hand hammer in the hammer slot. Stock must be metal (a material of kind metal, or any with a hot-working window). Assembly and rework are still at the [Tool Table].
  - Woodworking Table: new block, no vanilla original. Shapes parts from wood stock (a material with the "woody" trait: wood, nether wood, bamboo): tool heads, armor, bowls, doors. Never forged; shapes come out as soon as the stock is in. Hand hammer in the slot.
  - Stonecutter, "In vanilla": cuts one stone block into stairs, slabs, walls and the like, one input, pick the result. "In Feedback": that stays, as the second tab "Cut" (same recipes, same results, one input consumed per result); the first tab, "Parts", shapes parts from stone (a material of kind mineral), never forged.
  Prose says "tire" not "tyre"; no emojis; mirror Campfire.md's tone. Do not describe figures that are not in the code above.

## Out of scope
Any `.java`, `src/generated/**`, `docs/**`, the gun table, the prep table, new textures, a wood GUI skin, tags (none needed), JEI categories, the Tool Table's own files. Do not decide what else the Tool Table loses.

## Done when
- [ ] `./gradlew compileJava` passes (it may not on this branch's base; say so in the report)
- [ ] every JSON parses; ids match the registry names above
- [ ] each new wiki page is linked from Home.md

## Questions
(agent writes here instead of guessing)

## Report
(agent fills: files changed, verified, not verified)
