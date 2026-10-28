# petro-phase2-lang-and-polymers

## Goal
Names for the Phase 2 blocks and messages, blockstates/models/loot/recipes for them, and the polymer materials, so they show in game and polymers exist as data.

## Assumes
Base is branch `worktree-agent-a7427d68520f6f60c` (not `main`). It has the registrations `FBlocks.REACTOR`, `GAS_TURBINE`, `FLARE` (ids `reactor`, `gas_turbine`, `flare`, `compressor`, `gas_tank`, `burner` (and `thruster`, registered in `FVehicles`)), `Material.Kind` with `fluid` and `polymer`, and the reaction schema.

## Files
Read first: `src/main/resources/assets/feedback/blockstates/derrick_casing.json`, `.../models/block/derrick_casing.json`, `.../models/item/derrick_casing.json`, `data/feedback/loot_table/blocks/derrick_casing.json`, `data/feedback/recipe/derrick_casing.json` (plain block pattern); `data/feedback/material/copper.json` and `data/feedback/material/crude_oil.json` (material shape); `data/feedback/form_type/` for `forms` names.
Edit: `src/main/resources/assets/feedback/lang/en_us.json`.
Create: per id above blockstate, block model, item model, loot table, recipe; `data/feedback/material/<polymer>.json`.
Do not touch `src/generated/**`.

## Spec
- Blocks: reactor and gas_turbine use `block/cube_all` with `feedback:block/metal_alloy_block`; flare has `lit=false` and `lit=true` variants both on a `cube_all` with `minecraft:block/iron_block`. No new textures, so no licence row. Recipes: shaped, `PPP / P P / PPP`, `P` the plate item existing recipes use; flare `P P / PPP`.
- Lang: `block.feedback.reactor` "Reactor", `block.feedback.gas_turbine` "Gas Turbine", `block.feedback.flare` "Flare Stack", `block.feedback.compressor` "Gas Compressor", `block.feedback.gas_tank` "Gas Tank", `block.feedback.burner` "Fluid Burner", `block.feedback.thruster` "Thruster". `message.feedback.thruster.on` arg (fuel mB) "Thruster on, %s mB of fuel", `message.feedback.thruster.off` arg (fuel mB) "Thruster off, %s mB of fuel". `message.feedback.gas_tank` args (gas name, mB, Pu) "%1$s: %2$s mB at %3$s Pu", `message.feedback.gas_tank.empty` "Empty." Blocks compressor and gas_tank also need blockstate, models, loot, recipe like the others (compressor `PPP / PBP / PPP` is not needed: use the reactor pattern). `message.feedback.reactor` args (Tu, Pu, bed activity %) "Reactor at %1$s Tu, %2$s Pu, bed %3$s%%". `message.feedback.reboiler.reflux` arg (setting) "Reflux: %s"; `message.feedback.reboiler.reflux.none` "none", `.partial` "partial", `.total` "total". `gui.feedback.engine_stand.compression.low` "Compression: low", `.medium` "Compression: medium", `.high` "Compression: high".
- Polymer materials, their forms (pipe, insulation, tire), the polymer ingot casting rows and the lang for them are already in the branch; do not add them again. Needed still: `item.feedback.husk_sand`, and `form.feedback.*` already exist for pipe, insulation and tire (tire is now a block form, so it also needs a block model from datagen) "Husk Sand", and note that generated form items (`<metal>_pipe`, `<polymer>_ingot`, `<polymer>_insulation`, `<polymer>_tire`) need a datagen run (`./gradlew runData`) for models, which only the caller can do.
- Vapour materials, `"kind": "fluid"`, flammable, copy `naphtha.json` and set flash_point 10 / oxidation_temperature 250 for `naphtha_vapour`, 290/290-flash 60 for `kerosene_vapour`, 320/flash 80 for `diesel_vapour`; lang `material.feedback.<id>` "Naphtha Vapour" etc.

- Also lang: `message.feedback.turbine` args (spool %, blade condition %, flooded mB) "Spool %1$s%%, blades %2$s%%, flooded %3$s mB". Fluids and materials already added by code and needing names (`fluid.feedback.X` and `material.feedback.X`): `air` "Air", `ethylene` "Ethylene", `wisp_gas` "Wisp Gas", `soul_oil` "Soul Oil", `polyethylene_melt` "Polyethylene Melt", `soul_resin_melt` "Soul Resin Melt". Skip any polymer or vapour material file whose id already exists in `data/feedback/material/`.

## Out of scope
No Java. No `form` entries for polymers, no recipes using them. Do not reword existing keys or change existing materials.

## Done when
- [ ] `./gradlew compileJava` passes and every JSON parses
- [ ] each id has its files and lang keys exactly once

## Questions
(agent writes here instead of guessing)

## Report
(agent fills: files changed, verified, not verified)
