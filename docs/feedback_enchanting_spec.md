# Enchanting -- spec (2026-10-23)

Enchanting reworked from a gacha into a **part-based treatment with a minigame**. Vanilla's enchanting
table is replaced furnace-style; the enchant lives on a *part*, not a tool; what it does depends on the
part's *role* and *material*; some pairings are bad on purpose. Companion to
`feedback_modular_items_spec.md` (parts, roles, perks) and physics doc §1.4 (enchanting as material
preparation). The user's brief, in their words: "a way to make enchantment more interesting than a gacha
game", "a LOT more interesting enchants", "enchants that do different things depending on what, with some
of them probably bad (fire aspect on a handle burns YOU)". And: the forging has a minigame, so enchanting
gets one.

## 1. Pitch

Enchanting is attuning: a hot part is cooled through its **attuning window** under a field supplied by XP
(`XPu`), and the polarization it freezes in *is* the enchant. The player steers that field against a
lagging response (a hysteresis loop) to land the polarization in a target band. Nothing is rolled.
Skill, material and heat decide the result; a noise floor (philosophy 8) keeps it from being a timer.

Why hysteresis: it is real (ferroelectric poling follows a P-E loop with a coercive field, remanence and
saturation), it is a **control** game (read a lagging system, steer it) rather than a **timing** game, so
it plays nothing like the forging rhythm, and it is the mod's own subject -- feedback.

## 2. What an enchant is

An **effect** is a data row, `data/feedback/attunement/<id>.json`. One effect, many behaviours:

```json
{
  "target": 0.6, "band": 0.12,
  "reagent": "tag or item the player burns for it",
  "roles": {
    "edge":   { "kind": "ignite_target" },
    "handle": { "kind": "burn_wielder" },
    "plate":  { "kind": "ignite_attacker" }
  }
}
```

- `target`/`band`: where `P` must land (0..1 of saturation) and how wide the good zone is. A stronger
  effect has a narrower band.
- `roles`: what the effect does on a part of that role. **A role with no entry does nothing**, which is
  itself a design choice (a fire-aspect *binding* is just a warm string). Role names are the ones
  `ModularType` already defines; the agent verifies them against the code.
- The count of enchants comes from effect x role, so shipping eight effects gives dozens of behaviours.
  New effects are new rows, not new classes, wherever a `kind` already exists. A new `kind` is a small
  handler in the resolver (section 5).

Material scales it and can sour it. The level is read from the attuned part's material figures
(`enchantability`, conductivity, hardness, flame temperature where relevant); nothing keys on a material
id. Steel Fire Aspect is strong, a wooden handle with Fire Aspect is a torch in your hand
(`HotContact` already models that burn; `MaterialPerks` already does it for the perk of the same name).

### Initial effects (first slice; more are rows later)

| id | edge | handle | plate |
| --- | --- | --- | --- |
| `fire_aspect` | ignites target | burns the wielder, by the part's flame temperature (`HotContact`, gloves rate) | ignites an attacker |
| `keen` | extra damage, costs toughness (brittle) | nothing | nothing |
| `ward` | nothing | steadies the grip (less kick/handling penalty) | fracture resistance, costs mass |

Brainstorm queue (not first slice, write as rows when built): `siphon`/`echo`/`chill`/`toxic` already
exist as material perks (`Material.Perk`) and can become attuned effects reusing the same handlers;
silk-touch-as-fracture-control, fortune/looting-as-recovery are in physics doc §1.4.

## 3. The minigame (`EnchantGame`)

Pure class, the same shape as `ForgeRhythm`: the model, the judge and the server session live in one
`modular/EnchantGame` with no world access, so the server judges exactly what the screen showed.

**State per tick.** `E` field (the player's input), `P` polarization (output, -1..1), `T` part temperature.

**Model (constants in `ModularTuning`, all `[OPEN]` until played):**

- Field `E` is a **continuous scalar** the player sets (mouse axis or held keys with a ramp). The
  "discrete, never a scalar" rule is a balancing rule for machine settings, not a limit on a minigame
  (user ruling 2026-10-23). The *result* (level, flaw) stays discrete.
- Below the material's coercive field `Ec`, `P` does not move.
- Above it, `P` relaxes toward `Ps * tanh((|E| - Ec) / w) * sign(E)` with first-order lag `tau`.
  Backing `E` off to zero leaves `P` where it was (remanence). Reversing `E` past `-Ec` swings it back.
  `Ec`, `w`, `tau` come from the material: a wide loop is stubborn but forgiving, a narrow loop quick
  but twitchy.
- **The clock is heat.** `T` falls on the item curve (`ItemHeat`, `FTuning.ITEM_COOLING_TU_PER_TICK`).
  The field only acts while `T` is inside the material's attuning window (below its attuning point, above
  the lock temperature). Under the window `P` locks and the result is judged as it stands. Over it the
  field does nothing yet. Same idea as `ForgeRhythm.live`.
- **Noise floor.** `P` gets a small seeded jitter, seeded by the player's input samples (as
  `ForgeRhythm.noiseFor` is) so screen and server agree.

**Judge.** Final `P` against `target`/`band`:

| result | outcome |
| --- | --- |
| inside band, centred | full level, no flaw |
| inside band, edge | level one lower |
| outside band, same sign | reduced level, 1-2 flaws |
| overshoot / a landing below zero on an effect with no mirror / never reached `Ec` | no effect (never destroys the part: philosophy 7); the reagent and XP are spent |

A flaw is the same `flaw` integer forging uses (`MadeOf.flaw`, `FLAW_TOUGHNESS_LOSS`).

**Polarity (author, 2026-10-23, settled and built).** `target` is positive; an effect with a `mirror` map has a second band at `-target`, and where P lands picks the side (`feedback_enchant_roster.md` 1). The level is stored signed. An effect with no mirror misses below zero.

**Two correct approaches** (design digest rule): a *hot start* has a long window and a forgiving clock
but a noisy, high-`T` part (more jitter); a *cold start* has a short window but a clean signal. Starting
cold means reheating, and **reheating past the attuning point erases attunements already in the part**
(section 6). Two effects on one part is therefore an ordering decision.

**Server replay.** The client sends the input samples (field value per tick, bounded, e.g. 400) in a
finish payload (`EnchantFinishPayload`, modelled on `ForgeFinishPayload`). The server re-runs the model from the
same start state (`Session(variant, started, temperature)`-style record), checks the pace was possible
(no result faster than the window), and applies the outcome. Never trust the client's `P`.

## 4. Where it happens

**Vanilla's enchanting table goes the way of the furnace.** Per `vanilla-furnace-total-conversion` and
`wiki/Furnaces.md`:

- A new block `feedback:enchanting_table` that **inherits the vanilla table's model, loot, and recipe**
  (same ingredients, same shape). Vanilla's recipe is removed by overriding
  `data/minecraft/recipe/enchanting_table.json` the way `furnace.json`/`smoker.json` are, so the
  vanilla block is uncraftable. Replacement in generated structures (villages etc.) is the separate,
  already-open TODO item (structure-processor swap); do not do it here.
- Its own `BlockEntity` and `AbstractContainerMenu`: the **menu is in scope** (user ask). Slots: the
  part (a shaped part item, `ModularPartItem`, or raw stock in a form some role takes -- a rod, a grip, a bone; the table stamps `MadeOf` on raw stock to carry the attunement and the item still says what it is), the reagent, and a fuel/XP source slot. The XP source
  is `XPu` (`core/unit/XPu`, built 2026-10-23): a session costs `ENCHANT_XP_COST`, drawn from a Creative XP block
  next to the table (the digest's creative-source-first rule), else the player's own experience points, spent
  whatever the outcome. `[OPEN]`: a charge held in the table itself, and any real producer.
- A **metal** part must be **hot**. It is heated elsewhere (furnace, hearth) and carried, exactly as forging
  stock is; the table does not heat it. Cooling during the game uses the part's `ItemHeat` stamp.
  A cold metal part is refused with a plain message (a genuine physical gate, not a precision one).
  Anything that is not a metal is worked cold, so it is attuned cold (user ruling, 2026-10-23): no heat gate, no cooling during
  the game (`Attuning.cold`, true for a fallback material whose kind is not metal), the window held open for the whole session.
  Trap: the table first took only shaped parts, so grips, handles and bindings -- roles the effect tables name -- could never be attuned.
- Open question: **material attuning window.** Only `iron`, `steel`, `cast_iron`, `netherite` have
  `curie_temperature` (a *magnetic* property). Physics doc §1.4 means *ferroelectric* poling, a
  different property. Add a new optional material field (suggest `attuning_temperature`, `attuning_lock`,
  coercive field `coercive`, loop width `loop_width`, `lag`) read through `Material`; materials without
  it get a documented fallback (`shortcut:` comment) derived from existing figures (e.g. the cold end of
  its working window), so every material is enchantable but only some are good. Route it through the
  material system as the project requires (`material/*.json`).

## 5. Storage and effects at runtime

- An attuned effect on a part is stored in that part's **`dims` map** as `attuned.<effect_id> = level`
  (`MadeOf.dims`, already saved and synced, carried into `ModularDesign.Part.dims` when the part is set
  into a tool). **shortcut:** this overloads `dims` ("as-built measures"); upgrade to a real field in
  `Part`/`MadeOf` if effects grow per-effect state (fatigue, age, wear), since that needs both stream
  codecs touched.
- `MaterialPerks.of(stack)` already walks every part of a tool for perks. Add the part's `attuned.*` keys
  there and resolve each through the effect's `roles` entry for **that part's role**. This is what makes
  a handle differ from an edge: the resolver takes `(effect, part.role, part.material)`.
- Handlers by `kind` reuse existing paths: `ignite_target` and `ignite_attacker` use
  `target.igniteForSeconds`; `burn_wielder` uses `HotContact` / `Protection` as `MaterialPerks.flame`
  does today; stat kinds (`extra_damage`, `grip`, `fracture`) feed the bake figures
  (`ModularBake`). Add a kind only when no existing one fits.
- **Enchantments leave the tool and live on the part.** Vanilla `ItemEnchantments` on a modular tool is
  no longer how new enchants are made. `ModularConvert` still carries a converted vanilla item's
  enchantments across (kept as is, do not break it). Taking a tool apart already loses enchants; an attuned
  *part* returns with its `attuned.*` dims intact when the tool is taken apart, so a good enchant can be
  recovered, short by the same wear rule. `[OPEN]` whether that is too generous.

## 6. Fragility and bad outcomes

- **Heat erases it.** If a part holding attunements is heated above its attuning temperature (furnace, lava,
  Hearth) its `attuned.*` keys are cleared. Cheapest hook: wherever `ItemHeat` stamps a part's temperature,
  compare to the material's attuning point. `[OPEN]` exact hook; find it before building. If it is not
  cheap, ship without and list it in TODO.md.
- **Bad pairings are legal**: that is the point of section 2. No warning other than a tooltip line
  naming what the effect does on this part's role (so the player can read it before they swing).
- Capacity per part is a function of enchantability (user ruling, 2026-10-23): `ceil(enchantability / ENCHANT_PER_SLOT)`
  attunements, at least one (`EnchantGame.capacity`). Built as a hard cap. **Ruled 2026-10-23:** a second attunement is an independent session; it does not disturb the first (an earlier draft had its `P` partly relax).

## 7. GUI

- A new `GuiSkin` constant, **MYSTIC** (mystical theme: deep indigo and violet with gold-leaf trim,
  faint glyph noise, soft glow on hover). Use the `add-gui-skin` skill: it is code-drawn, no texture
  files, fixed-seed noise, and it implements `panel`, `slot`, `button` and `click`. Apply it to the
  enchanting table's screen (a `SkinnedScreen`).
- Screen: the part and reagent slots on the left; on the right the **gauge** -- a vertical track for
  `P` with the target band shaded, a marker for the current `P`, a second marker for the field `E`
  the player is holding, and a temperature bar showing the attuning window with the part's `T` on it.
  Field control: hold the mouse and drag vertically, or W/S, whichever is easier to build (one is
  enough; say which). A Begin button starts the session (server `begin`), the game ends on lock or on
  a Commit button, then the finish payload is sent.
- Readout: format figures through `Readout.figure` per the truth-tier convention.

## 8. Build order for the first slice

1. `EnchantGame` (pure) + its constants in `ModularTuning`. Include one assert-based self-check or small
   unit test of the judge and replay determinism if there is existing test scaffolding for pure classes;
   do not write a game test (CLAUDE.md).
2. Material fields (`attuning_*`) with fallback; `attunement` data loader (follow how `gun_option`
   rows are loaded); the three initial effect rows.
3. Storage in `dims` and the role resolver in `MaterialPerks`; tooltip line on an attuned part and on a
   tool that carries one.
4. Block, block entity, menu, screen, `MYSTIC` skin, payloads, registry, lang, blockstate/model/loot
   inheriting the vanilla table's, recipe copy, vanilla recipe removal.
5. Wiki: new page `wiki/Enchanting_Table.md` (original vanilla behaviour and the new one, as the
   project's wiki rule asks for any changed vanilla block), and update `wiki/` pages for modular tools if
   they mention enchanting. `docs/source_layout.md` row. Delete the "Later, footnote only: rework vanilla
   enchanting" line from `TODO.md` and replace it with one line per piece the first slice left out.
6. `./gradlew compileJava` must pass. Do **not** run `./build-and-deploy.sh` (only after the user
   confirms), do not push, no game tests. Hand back a short in-game checklist.

## 9. Open (`[OPEN]`)

- Reagent per effect: item/tag burned, or a plain XP-level cost. First slice: XP levels only.
- XP as a stored `XPu` charge in the block (the creative source exists; the table still keeps nothing).
- Whether recovering an attuned part from a taken-apart tool is too generous.
- Heat-erasing attunements inside an assembled tool (a loose part is handled where `ItemHeat` stamps it).
- Villager enchanted-book trades and loot books: not part of this slice. Leave them and note it in
  `TODO.md`.
- Every constant in section 3 until played.

## 10. Trap (record of what was tempting)

The first idea was to pick the outcome from a table and show it as a progress bar. That is the gacha
with a coat of paint: the player's hands do nothing. The reason hysteresis was chosen is that the same
inputs, replayed, give the same grade, while the *skill* is in reading a lagging response. A second
tempting shortcut is to key effects by tool id or by item type; do not, an effect is resolved by part
role and material, never by what the whole tool is called.
