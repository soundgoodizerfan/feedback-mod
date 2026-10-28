# petro-lang-phase1

## Goal
Every block, item, fluid, material, message and tooltip added by petrochemistry Phase 1 has an English name in `en_us.json`, so nothing shows a raw key in game.

## Assumes
Base is the branch `worktree-agent-a7427d68520f6f60c` (it holds the registrations for these blocks and the code that reads the keys). Do not branch from `main`; it does not have them.

## Files
Read first: `src/main/resources/assets/feedback/lang/en_us.json` (see `block.feedback.pump` ~line 416, `fluid.feedback.steam` ~459, `material.feedback.copper` ~729, `feedback.survey.none` ~2583 for style and placement).
Edit: `src/main/resources/assets/feedback/lang/en_us.json` only. Do not touch `src/generated/**`.

## Spec
Add these keys next to their siblings. Prose says "tire" not "tyre". Players see volume in ingots, not mB, except where an argument is already formatted by code.

Blocks, and items where one exists (`block.feedback.X`, `item.feedback.X`): `pipe` "Pipe", `creative_fluid` "Creative Fluid Source", `derrick_base` "Derrick Base", `derrick_casing` "Derrick Casing", `wellhead` "Wellhead", `seep` "Tar Seep", `crude_pool` "Crude Pool" (block only), `casting_basin` "Casting Basin". Item only: `item.feedback.derrick_bit` "Boring Bit".

Fluids (`fluid.feedback.X`): `crude_oil` "Crude Oil", `heavy_oil` "Heavy Oil", `naphtha` "Naphtha", `kerosene` "Kerosene", `diesel` "Diesel", `naphtha_vapour` "Naphtha Vapour", `kerosene_vapour` "Kerosene Vapour", `diesel_vapour` "Diesel Vapour".

Materials (`material.feedback.X`): `crude_oil`, `heavy_oil`, `naphtha`, `kerosene`, `diesel`, same names as the fluids.

Messages (argument order is the order the code passes them):
- `message.feedback.pipe.empty` (pipe count, capacity mB): "Empty run: %s pipes, holds up to %s mB."
- `message.feedback.pipe.contents` (fluid name, amount mB, volume mB, temperature Tu, pressure Pu, pipe count): "%1$s: %2$s of %3$s mB at %4$s Tu, %5$s Pu, %6$s pipes."
- `message.feedback.creative_fluid.set` (fluid name): "Now sourcing %s."
- `message.feedback.seep.dry`: "The seep is dry for now."
- `message.feedback.derrick.<status>` (metres bored, bit condition %), statuses `idle`, `no_bit`, `no_casing`, `too_soft`, `bedrock`, `boring`: idle "Idle at %1$s m.", no_bit "No bit fitted at %1$s m.", no_casing "Needs another casing below %1$s m.", too_soft "Bit too soft for this rock at %1$s m (%2$s%%): it is wearing, not cutting.", bedrock "Cannot bore past %1$s m.", boring "Boring at %1$s m, bit at %2$s%%."
- `message.feedback.wellhead.<state>` (choke name, danger Pu, cone %), states `flowing`, `shut_in`, `lifting`, `needs_lift`: flowing "Flowing, %1$s choke (rated %2$s Pu), coning %3$s%%.", shut_in "Shut in, %1$s choke (rated %2$s Pu).", lifting "Lifting, %1$s choke, coning %3$s%%.", needs_lift "Pressure is below the column's weight: turn the lift. Coning %3$s%%."
- `message.feedback.wellhead.choke.small` "small", `.half` "half", `.open` "open".
- `gui.feedback.casting_basin.molds` "Molds", `gui.feedback.casting_basin.pool` "Pool".
- `tooltip.feedback.derrick_bit` (hardness, condition %): "Hardness %1$s, condition %2$s%%".
- `feedback.survey.oil` "Oil basin".

## Out of scope
No Java. No keys beyond these. Do not reword existing keys.

## Done when
- [ ] `./gradlew compileJava` passes and the JSON parses (`python3 -m json.tool`)
- [ ] every key above is present exactly once

## Questions
(agent writes here instead of guessing)

## Report
(agent fills: files changed, verified, not verified)
