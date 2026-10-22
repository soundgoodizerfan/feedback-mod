# Armed skeletons and gun blueprints in chests

Working spec, from TODO.md "Armed skeletons + gun blueprints in chests". Rulings by the author, 2026-10-22.

## Rulings

1. **Classes.** A skeleton carries early guns only: single-shot, lever or bolt feed, and a design whose
   `GunSpec` peak chamber pressure is at or under black powder's (`gunpowder` material, `peak_pressure`).
   The test is a pressure ceiling read from data, never a gun id or a hand-kept list. Chests draw from the
   full blueprint pool.
2. **Ammunition.** Skeleton spawns with the gun loaded and a fixed spare count (`FTuning.SKELETON_GUN_SPARE`,
   start at 6) baked from a `Cartridge` the gun accepts (same path as `/feedback barrel`/`round`). Rounds
   are spent for real. Empty and nothing left: skeleton goes back to its bow-less melee, no reload loop.
   On death the gun drops with its real condition, and unspent rounds drop as cartridge items. Nothing
   is infinite.
3. **Firing.** A mob-side adapter around `GunShots`. Same swept-segment physics, same `GunFigures` bake,
   same cycle time from the gun's action. No proxy arrow, no mob damage constant.

## Work, in order

### A. `GunShots` shooter generalisation
`GunShots` takes `Player` throughout (`shoot`, `fire`, `Shot`, `accept`, `LAST_SHOT`, `GunStance.of`,
`ModularTraits.gunAimed`, `GunHazard.apply`, the `displayClientMessage` calls). Widen only what a mob
needs to `LivingEntity` and keep every player overload unchanged in behaviour. Player-only calls
(`displayClientMessage`, `ServerPlayer` trait hooks, `GunItem.dualWielding`) stay guarded by `instanceof`.
Add one entry point, `GunShots.triggerMob(ServerLevel, LivingEntity, ItemStack gun, long nowGameTick)`,
that picks the chambered round, runs `shoot`, and returns whether it fired. The rate gate for a mob is
game ticks from the gun's own cycle figure, not wall-clock ms like a player's.
Hazard (overpressure, cook-off) applies to the mob too, which is the point: an early gun in a skeleton can
hurt the skeleton. Do not special-case it.

### B. Mob goal
New `RangedGunGoal` (package `weapon/`) for a `Skeleton`: needs line of sight within the gun's effective
range (derive from `GunFigures` muzzle speed and drop, not a constant), aims at the target, fires on cycle,
reloads by the gun's own reload beat when the chamber is empty and spares remain (single-shot: a reload
beat per round). Replaces the bow goal only while the gun is in hand and has rounds; falls back to
the vanilla goals otherwise. Aim error must come from `GunFigures` spread plus a stance constant for a
mob, never zero.

### C. Spawn equip
`FinalizeSpawnEvent` (or equivalent `EntityJoinLevelEvent` gated on first join) on `Skeleton`. A small
`FTuning.SKELETON_GUN_CHANCE` (start 0.03; Nether/difficulty scaling not now). Draws a blueprint from the
pool in D, bakes via the same `ToolDesigns.make` path as `/feedback build`, keeps only designs that pass
ruling 1, puts gun in main hand with `GunAmmo` loaded, adds the spare rounds as mob inventory metadata
(`ItemStack` list on a data attachment, serialised, so they survive save). Drop chance of the gun is 1.0
at the gun's real condition; spares drop with the death loot.

### D. Blueprint pool and chest loot
One pool, data-driven: `data/feedback/gun_blueprint_pool/*.json` (list of blueprint item stacks or a
reference to how `/feedback build` takes a held blueprint). Add a loot modifier or loot-table injection
(`data/feedback/loot_modifiers/`, see `feedbacker_relic.json` for the form) adding one rare gun blueprint
to dungeon, mineshaft, desert-temple, stronghold and bastion chests. Both the chest roll and the skeleton
spawn read this same pool; skeleton filters by ruling 1 afterward.

## Constraints to keep
- No identity check on gun or mob id in physics; entity type is the only mob-side gate (skeleton), as
  vanilla does for bows. Philosophy 1: this is a spawn-table statement, not a player-facing sort.
- Wear/hazard are misuse only; do not add upkeep.
- No game test unless the author asks. `compileJava`, then give the author an in-game checklist.
- Update `docs/feedback_guns_spec.md` and delete the TODO.md line once built; keep `[OPEN]` for anything
  left unresolved (e.g. Nether skeleton variants, wither skeletons, stray).

## In-game checklist to hand the author
1. `/summon skeleton` many times; roughly 3 in 100 hold a gun. (Raise chance temporarily to test.)
2. Gun skeleton shoots you with visible tracer/report and real spread; stops after rounds are out.
3. Kill it: gun drops with condition; leftover rounds drop.
4. Open generated chests; occasional gun blueprint.
5. A skeleton never carries an automatic or high-pressure gun.
