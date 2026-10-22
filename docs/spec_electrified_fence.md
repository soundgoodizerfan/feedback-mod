# Electrified perimeter

Working spec, from TODO.md "Electrified perimeter". Rulings by the author, 2026-10-22.

## Starting point (verify in code)
`CableBlock` already calls `Shock.apply(entity, wire.touchVolts())` on contact; `Shock.damage` is by
`VoltageBand`; `Protection.shockProofVolts` is the player-only guard; `HazardEvents` shocks the player.
So a bare live wire already hurts anything inside it. The fence is that, plus what scales the hurt.

## Rulings
1. **Damage:** reuse `Shock` figures by `VoltageBand` and the existing shock damage type. No new type.
2. **Contact:** a fence block is a thin, tall, non-full-collision wire block (post and wire) of the
   cable family. An entity whose bounding box overlaps it is shocked, with a brief knockback away from
   the block and a spark particle and sound. Repeat hits are gated by a short per-entity cooldown
   (`FTuning.FENCE_SHOCK_COOLDOWN_TICKS`, start 10). Not an area field: touch only.
   (0 to a cap `FTuning.FENCE_EXPOSURE_MAX`, start 2.0), the product of:
   - **Wet:** entity `isInWaterOrRain()` raises it (`FENCE_WET_FACTOR`, start 1.5).
   - **Metal armour:** each worn armour piece's material, read from `material/*.json` electrical
     conductivity (field already exists: check name; add one if not), raises it in proportion to
     the share of armour slots conductive. An insulating armour material lowers it. Never an item id.
   - **Grounded:** the block under the feet, via its material/`physics_block_properties` conductivity.
     Conductive floor raises it; wood/dry insulators lower it. Flying/jumping entity counts as ungrounded.
   Put the maths in a new `core/hazard/Exposure` (one method per factor, pure, testable without blocks).
   Players already use `Protection`; fold `Exposure` into `Shock.apply` for any victim, keep the player
   `shockProofVolts` gate as is. Do not change existing bare-wire damage for the default case
   (dry, bare, standing on stone gives exposure 1.0).
4. **Draws current:** each shock pulls amps from its run for that tick (`FTuning.FENCE_HIT_AMPS`,
   start 1). The run's resolver already shares/sags by load: add a transient sink load on the
   fence's `ElectricNode`, not a new mechanism. A fence mobbed by many entities sags the network,
   which is the visible cost. If this needs more than a small hook into `ElectricRun`, stop and
   report instead of redesigning the resolver.

## Placement of work
- New `machine/cable/FenceBlock` + block entity or a property of `CableBlock` variant (prefer the
  smaller diff: a shape/collision variant of the existing cable if the model allows).
- Recipe: fence post from cable + the metal material's rod/plate form per CLAUDE.md recipe ruling
  (no ingot where a plate/wire belongs). Placeholder model from `../Texture-reference`; add the
  `THIRD-PARTY-LICENSES.md` row in the same change.
- Lang keys; wiki page `wiki/Electrified_Fence.md`; Jade shows voltage band via existing provider.
- Instrument truth tier: the galvanometer reads the hit draw like any other load.

## Constraints
- Philosophy 1 (no identity): scaling by wetness, armour conductivity, ground conductivity only.
- Do not model resistance of a body (Shock javadoc); `exposure` is a read-off-the-world multiplier,
  state that in the javadoc.
- Wear only by misuse: a fence taking hits is correct use, so it gains no wear.
- No game test unless asked. `compileJava`, then an in-game checklist.
- Delete the TODO.md line when built; record in `docs/` and the wiki.

## Checklist for the author
1. Wire fence to an LV source; walk into it, get shocked and pushed back, spark and sound.
2. Same in rain/water: hurts more. In leather vs iron armour: iron hurts more.
3. Stand on a dry planks pad vs stone: planks hurt less.
4. Herd several mobs onto it; galvanometer shows load and the run sags.
5. A flying/jumping mob takes less than one standing.
