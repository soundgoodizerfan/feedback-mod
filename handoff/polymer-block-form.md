# polymer-block-form

Base: this branch (`all-in-one`).

## Goal
Remove the redundant polymer-only `tire` form. Tires are add-ons on a frame that read grip from any block's material (`vehicle/frame/FrameMaterial.java`: "the block id a stack stands for as plate or tire stock"). So polymers just need the ordinary `block` form, like metals have. Widen the existing schema, do not fork it.

## Files
Read first: `src/main/resources/data/feedback/form_type/block.json` (currently `"kinds": ["metal"]`), `form_type/tire.json`, `form/ingot_to_tire.json`, `tags/item/polymers.json`.
Edit: `form_type/block.json` (add `"polymer"` to kinds), `tags/item/polymers.json` (remove every `*_tire` entry; add `*_block` entries for the six polymers: polyethylene, polypropylene, synthetic_rubber, pvc, soul_resin, bitumen, but ONLY if the generated block items are named `feedback:<material>_block`; check how an existing metal block item is named, e.g. grep `iron_block` / `copper_block` in `src/main/resources` and `src/main/java`).
Delete: `form_type/tire.json`, `form/ingot_to_tire.json`.
Lang: remove the `form.feedback.tire` key from `assets/feedback/lang/en_us.json`; add nothing else.

## Spec
1. After the change every polymer material gets a block form exactly the way a metal does. Find how metal block items are produced from the `block` form_type (grep the form/material code under `src/main/java` for where `form_type` `block` is read: `FormTable`, `FormItems`, `FormData`, or similar) and confirm polymers now go through the same path with no other code change. If a metal block is reached by a form step or a recipe that polymers lack (there is no `form/ingot_to_block.json`), find how metal gets there and give polymers the same route with a data file; if that needs Java, STOP and write the question under ## Questions.
2. Do not touch the `pipe` or `insulation` forms.
3. `grep -rn "polyethylene_tire\|_tire\b" src` must find nothing stale in data, tags or lang (frame/tire Java code is unrelated, leave it).

## Out of scope
Models/textures (datagen later), `src/generated/**`, docs, any Java unless step 1 forces it (then stop and ask).

## Done when
- [x] `./gradlew compileJava --offline` passes
- [x] polymers have a block form by data only

## Questions

## Report
- Added `"polymer"` to `"kinds"` in `src/main/resources/data/feedback/form_type/block.json`.
- Deleted `src/main/resources/data/feedback/form_type/tire.json` and `src/main/resources/data/feedback/form/ingot_to_tire.json`.
- Removed `form.feedback.tire` key from `src/main/resources/assets/feedback/lang/en_us.json` without adding any extra entries (`form.feedback.block` already exists).
- Updated `src/main/resources/data/feedback/tags/item/polymers.json` replacing each polymer `*_tire` item with `*_block` for all six polymers (polyethylene, synthetic_rubber, polypropylene, pvc, soul_resin, bitumen).
- Verified FormItems registration path: `FormItems.registerAll` dynamically registers `FormBlock` and `FormBlockItem` named `feedback:<material>_block` for any material whose kind matches a `form_type` with `"block": true`. Metal blocks also generate 9:1 ingot<->block compression crafting recipes via `FormData.Recipes.compress`, which loops over all materials and generated forms. No Java code changes were needed.
- Confirmed `grep -rn "polyethylene_tire\|_tire\b" src` returns zero stale references in data, tags, or lang.
- Confirmed `./gradlew compileJava --offline` passes successfully.
