# Modular items -- overnight progress

Worktree build of `feedback_modular_items_spec.md` (delegated, author asleep). Spec section 12 is the record of what exists.

## Done (committed, build and game tests green)
- Steps 1-2: core + pickaxe/axe/shovel/hoe, vanilla conversion.
- Step 3: sword, mace + heavy core + breeze rod, trident, shield.
- Steps 4-5: bow, crossbow, shears, armour.

- Step 6: balance scale, scratch file set.
- Step 7: mattock, cleaver, pike, maul, sickle.
- Wiki: Tools_and_Weapons, Armour, Balance_Scale, Scratch_Files, Heavy_Core; Home, Materials, Assembly_Bench, Electromagnet updated.

## Next
- Nothing owed from the brief but guns migration (deliberately waits).

## Decisions made (all in spec section 11)
- Assembly by vanilla-shaped pattern; parts are ordinary form items; no part forms generated.
- Edge by sneak-scroll; length = pattern; binding = what is put in; section deferred; heat treatment = material.
- Repair/re-forge: one recipe, on the bench. Forming deviation: built = target (open).
- Crossbow by pattern at the bench, not on a stand.

## Notes
- Game tests: one vehicle test (enginedrivesacaronitstires) is flaky/physics-dependent; wiping run/world and rerunning passes. The server sometimes hangs, kill it and rerun.
