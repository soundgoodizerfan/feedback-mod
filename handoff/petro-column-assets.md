# petro-column-assets

## Goal
The four fractionating-column blocks (reboiler and three tray types) have blockstates, models, item models, loot tables, recipes and English names, so they place, render and drop in game.

## Assumes
Base is the branch `worktree-agent-a7427d68520f6f60c`, not `main`: it holds the block registrations `FBlocks.REBOILER`, `TRAY_BUBBLE_CAP`, `TRAY_SIEVE`, `TRAY_PACKED` (ids `reboiler`, `tray_bubble_cap`, `tray_sieve`, `tray_packed`) and their items.

## Files
Read first: `src/main/resources/assets/feedback/blockstates/derrick_casing.json`, `.../models/block/derrick_casing.json`, `.../models/item/derrick_casing.json`, `src/main/resources/data/feedback/loot_table/blocks/derrick_casing.json`, `src/main/resources/data/feedback/recipe/derrick_casing.json` (the pattern to copy: a plain block, one model, self-drop loot).
Create, per id above: `assets/feedback/blockstates/<id>.json`, `assets/feedback/models/block/<id>.json`, `assets/feedback/models/item/<id>.json`, `data/feedback/loot_table/blocks/<id>.json`, `data/feedback/recipe/<id>.json`.
Edit: `src/main/resources/assets/feedback/lang/en_us.json`.
Do not touch `src/generated/**`.

## Spec
- Models: `parent: block/cube_all`; reuse an existing texture, no new image. reboiler uses `feedback:block/metal_alloy_block`, trays use `minecraft:block/iron_block` (bubble cap), `minecraft:block/gray_concrete` (sieve), `minecraft:block/bricks` (packed). No new texture, so no `THIRD-PARTY-LICENSES.md` row.
- Recipes (shaped, same file shape as derrick_casing): reboiler pattern `PPP / P P / PPP`; each tray pattern `P P / PPP`, result count 2. `P` is the plate item the existing recipes use for sheet stock (check `src/main/resources/data/feedback/recipe/` for the real id, `<metal>_plate`; steel if it exists, else iron).
- Lang: `block.feedback.reboiler` "Reboiler", `block.feedback.tray_bubble_cap` "Bubble-Cap Tray", `block.feedback.tray_sieve` "Sieve Tray", `block.feedback.tray_packed` "Packed Tray", `item.feedback.*` is not needed (block items use the block key). Also `message.feedback.tray` args (condition %, draw fluid name) "Tray at %1$s%% condition, drawing: %2$s", `message.feedback.tray.empty` "nothing".

## Out of scope
No Java, no new textures, no rotation or facing variants, no change to existing recipes.

## Done when
- [ ] `./gradlew compileJava` passes and every new JSON parses
- [ ] each id has all five files and the lang keys exist exactly once

## Questions
(agent writes here instead of guessing)

## Report
(agent fills: files changed, verified, not verified)
