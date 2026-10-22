# Shield as a thermal and kinetic sink

Working spec, from TODO.md "Shield as a thermal and kinetic sink". Rulings by the author, 2026-10-22.
Builds the "thermal face" that `docs/feedback_modular_items_spec.md` 8/12 says is not built.

## Rulings
1. **Which model:** `ItemHeat` (stamp plus tick, linear cooling, computed on demand), not `ThermalBody`.
   A shield is carried, so it cools in a chest and off-world for free, as a gun barrel does. Use the
   `ItemHeat.Cooling` interface for the shield's own cooling rate (a heavy face sheds slowly).
2. **Heat in from a blow:** `ModularEvents.onShieldBlock` already clamps by capacity. The blocked `Fu`
   becomes `Work` into the face: `Work = blocked_Fu x FTuning.SHIELD_HEAT_FRACTION` (start 0.5), and
   `dTu = Work / face thermal mass`. Face thermal mass comes from face material plus the baked mass
   (`ModularFigures`); use whatever thermal-mass-per-Mu the material file or `Heat` already gives and
   add a field to `material/*.json` only if none exists (author: route material properties through the
   material system). Stamp the new Tu with `ItemHeat.set`. Heat adds to the current figure, so repeated
   blows stack and cooling between them matters.
3. **Consequences (both, by the existing machinery):**
   - **Holder burns.** A raised shield above the `HotContact` bands hurts its holder per tick held,
     through `core/hazard` (`Protection`/`Grip` already rate gloves and tongs by their material's
     exposure fraction). A grip role material that insulates, or a glove, reduces it. A consequence,
     never a lock: the use goes through and the hand pays.
   - **Face loses capacity near its limit.** Capacity multiplies down as Tu approaches the face
     material's softening point (use `tempering`/`quench`/`hot-working window` figures already in
     `material/*.json`; pick the one that means "loses strength" and say which in the javadoc). Past
     it, extra wear on the shield (misuse), never an instant break, per CLAUDE.md wear rule.
4. **Nether / fixed-Tu sources:** a blocked fire attack (`#minecraft:is_fire` damage, blaze/ghast
   fireballs, magma/lava contact while raised) is blocked as a blow, and its source Tu heats the face
   toward that Tu by `Heat.relax` (difference law: face approaches the source, never passes it).
   Source Tu per damage type is data (a small table keyed by damage type tag, in
   `data/feedback/`), not a hard-coded mob id. A plate that conducts heat to the hand hurts; a
   ceramic or wood face does not conduct but burns/limits per ruling 3.
5. **Readout:** shield tooltip shows face Tu and capacity through `Readout.figure` (truth tier, with
   gloss on Shift). Debug helmet reads exact.

## Constraints
- No identity checks: face material only; never a shield or mob id.
- Hazard `Tu` numbers anchor on figures the mod already has (HotContact bands).
- Wear only by misuse: a hot, over-limit shield wearing is misuse; a cool shield taking blows gains
  no new wear beyond the existing capacity clamp.
- No game test unless asked. `compileJava`; update `docs/feedback_modular_items_spec.md` (the "not
  built" notes), the wiki Tools_and_Weapons/Shield page, and delete the TODO.md line.
- Check `LivingShieldBlockEvent`-fires-when-angle-fails remains the existing "unconfirmed" item; do
  not rely on it for the fire path (hook damage events directly for fire if needed).

## Checklist for the author
1. Block many sword blows with a thin steel-faced shield: tooltip Tu climbs; hand starts to hurt.
2. Same with a thick/heavy face: climbs slower. Let it rest: Tu falls linearly.
3. Raise a shield against a blaze fireball / stand it in fire: face warms toward the source Tu, not past.
4. Hot enough: capacity drops, wear climbs, nothing breaks instantly.
5. Gloves or an insulated grip reduce the burn.
