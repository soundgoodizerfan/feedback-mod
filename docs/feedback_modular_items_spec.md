# Feedback — Modular Items

**Working document, not one of the three.** Tools, melee weapons, ranged weapons and armour as
*baked designs*: parts, each a material plus a few discrete geometry choices, baked once into
figures stored on the item. The gun's pattern (`docs/feedback_guns_spec.md`, `weapon/GunDesign`)
widened into a shared core. Built in part: §12 says exactly what.

Prior art read 2026-10-15 (`THIRD-PARTY-LICENSES.md` gets its rows when code lands):
**Silent Gear** (MIT, 1.21.1), **Tinkers' Construct** (MIT, 1.20.1 only — no public 1.21 branch),
**Tetra** (no public source, All Rights Reserved — design read from its wiki only, nothing
adaptable). What was taken and rejected is in §9.

---

## 1. What the author asked for (rulings, 2026-10-15)

- **One core for tools, melee weapons and armour**, and guns move onto it once it holds (§8).
- **Every vanilla weapon is represented** — sword, axe, mace, trident, bow, crossbow, shield — not
  "most of them". Maces and tridents become modular even though it makes them less special.
- **Custom types** beyond the vanilla set (§5).
- **Two assembly routes.** Simple tools: the parts *are* the design, put together at the Tool Table
  (Tinkers' shape; §4.2). Complicated items (crossbows, compound tools): designed on a stand, printed
  as a blueprint, parts made to it (the gun's shape).
- **A blueprint predicts; it does not promise.** It gives the figures you *will* make if every
  part comes out exactly to drawing, which early on it won't. What you actually *have* is
  measured with instruments (§6).
- **Vanilla enchanting stays, fully.** (Footnote: a later rework may enchant *parts* rather than
  whole items — not this spec.)
- **Vanilla tools and armour go the way of the furnace**, and loot, mob gear and villager trades
  are converted, not dropped: a stone shovel from a chest is still a "Stone Shovel", but it is a
  modular item underneath (§7).
- **Depth like Silent Gear**: many figures that interact, so a bad build is possible, legal and
  learnable. Nothing stops you making a terrible sword; you find out by testing it.

## 2. The model

A **design** is a layout (what kind of item) plus an ordered list of **parts**. A part is:

- a **role** in the layout (head, edge, handle, binding, limb, plate, ...);
- a **material** (`material/*.json`, by id — data, not an identity check on an item);
- a few **discrete geometry choices** for that role (§3);
- its **as-built state**: what the forge actually produced (§4), and its heat-treatment state.

**Baking** is a pure function of design and the material table, run once when the item is
assembled or changed, and stored on the stack as a component — exactly the gun's old `GunDesign.bake` into
`GunFigures`, which is now the `Fig`s of 8.3. Nothing reads a stat table per material; every figure is derived.

### 2.1 Figures, all derived

| Figure | From |
| --- | --- |
| mass (`Mu`) | each part's volume (from its geometry) × `density` |
| balance | where the mass sits: head mass × handle length over total — *head-heavy* or *light in the hand* |
| swing speed | falls with the moment of inertia about the hand (mass and how far out it sits) |
| impact | head mass × swing speed, the work a blow carries |
| bite | edge `hardness` against the target's — under it the edge skids, over it it cuts |
| chip / bend | `toughness` and `yield_strength`: a hard, brittle edge chips; a soft one rolls and dulls |
| reach | total length |
| harvest | edge hardness against the block's hardness — a hard gate by arithmetic, the hammer's `St ÷ hardness` rule again |
| durability | volume of the wearing part × `toughness`, scaled by heat treatment |
| enchantability | a new `enchantability` material figure (vanilla already treats it as a material property: gold enchants well) |
| protection (armour) | plate `hardness` and thickness against a blow's `Fu`; coverage by piece |

The depth argument: in Silent Gear most stats are weighted averages and the interaction lives
only in traits. Here every figure pulls on others. A heavier head hits harder and swings slower;
a longer handle reaches further and loads the binding; a harder edge bites more and chips; a
softer spine absorbs and dulls. None of that is authored, so it cannot be balanced away into one
right answer.

### 2.1a Material perks (2026-10-21)

A figure is a stat; a *qualitative* thing a material does is a perk, a `perks` map in its file
(`{"smite": 1}`), read by `modular/MaterialPerks` from every part of a weapon. Lead being dense
is a stat and stays one; silver being anathema to the undead is a perk. The level is the highest
any part gives (two silver parts are not two levels), applied free of the enchanting table, and
nothing keys on a material id: a pack gives a perk by writing a line in a material file. Built:

| perk | material | effect |
| --- | --- | --- |
| `smite` | silver (a new raw ingot and material, no ore or melt yet) | +2.5 damage per level to `#minecraft:undead` |
| `fire_aspect` | blaze rod | sets the target alight 4 s per level; costs the wielder, below |
| `impaling` | prismarine | +2.5 damage per level to any target, not only the wet ones |
| `echo` | echo shard | **[OPEN]** a struck target is outlined (Glowing) for 3 s |

The roadmap's "(handle)" and "(tip)" are where those materials are used, not a restriction: a
part of the material gives the perk in whatever role it fills.

**Blaze rod: a burn, not a held heat.** The roadmap offered two: burn the wielder without
gloves, or keep the weapon at a heat. Burn won. A workpiece's heat is stamped once and cools
linearly (`ItemHeat`), so a weapon "kept at a heat" would need a fixed-temperature source in
the hand, which is `HeatSource`'s idea (lava is a fire fixed at 1200 Tu), not a workpiece's, and
would invent a second way to be hot. A blaze rod already has a `combustion` flame temperature
in its file, and `core/hazard` already answers "what does careless contact cost": every half
second a hand holding a fire-aspect part at a flame hotter than `Protection.handlesUpToTu`
takes `HotContact.burn` at that flame's temperature. Gloves in the Curios `hands` slot (or tongs)
rated for it make it free. A consequence, never a lock: the weapon swings either way.

**Echo shard: echo, [OPEN].** Sound and resonance: an echo shard rings when struck, and the
return of that ring outlines what it struck, as echolocation does. Qualitative (it shows where,
not how much), harmless, and it uses the Glowing effect rather than a new system. Why not
louder: a trait that woke wardens or sculk would be the shard's real-world cousin (vibration),
but is a penalty with a sculk-sensor system behind it; left as a candidate. Open: the duration,
whether it should instead emit a vibration game event sculk reads, and whether an armour piece
of it should hear the same (a sculk-sensor-style warning of nearby footsteps).

### 2.2 Integrity is a joint, not a number

Tetra's integrity is a capacity budget that parts spend and handles provide. Here it has a
cause: **the handle and binding carry the head's leverage** (head mass × handle length). A
binding's capacity comes from its material (`yield_strength`, `friction`) and its choice
(wrapped, pinned, socketed). A design over its joint is *legal* — it builds — and its joint
works loose with use, which is wear from a specific mistake. Readable, never instant.

### 2.3 Wear

- **Durability is the accepted consumable exception** (CLAUDE.md, Hand Hammer ruling,
  2026-10-09). A pick is spent by digging.
- **Misuse costs extra, and says which misuse.** An edge on something harder than itself chips;
  an axe into stone dulls; an over-leveraged joint loosens. Same rule as the Mechanical Hammer:
  force that cannot go into the work goes into the tool.
- **Broken is not destroyed** (Tinkers' `tic_broken`). At zero the item stays in hand and acts as
  an empty hand until repaired. Figures degrade in steps on the way down (Silent Gear), so a worn
  tool is visibly worse before it stops.
- Repair puts material back into the worn part. **Decided 2026-10-16 (overnight, delegated):**
  by hand at the Tool Table (it was the Assembly Bench's crafting grid until 2026-10-21; §4.2) -- the worn item and the stuff its wearing part is made of, each
  item putting back its share of the durability (`1 / the part's cells`). Re-forging stays
  possible and is the same recipe: the item and a *different* material, in as many cells as one
  of its parts has, swaps that part, and a swapped wearing part is a fresh one.

## 3. Discrete choices

Per the house rule (philosophy §3): named options, never sliders. Each is a real trade, and where
it lives was decided by asking where the player would naturally *do* it (2026-10-16):

- **Edge:** keen / standard / blunt -- bite against chipping. Ground on a finished tool **at a
  grindstone** (2026-10-20): shift-use a block in `#feedback:grindstones` with the tool in hand
  steps to the next edge, costs `GRIND_COST` points of durability (never the last) and rebakes the
  item. It was sneak-scroll anywhere, which made an edge a free, instant toggle -- keen for the
  dig, blunt for the walk home -- so the choice cost nothing and so was not one. The tag, not the
  vanilla block's id, so a pack's grinding block joins it.
- **Length:** short / long. The *pattern* is the choice: a handle is a number of cells, one per
  stick, and a longer pattern is a longer handle. Reach and leverage against swing speed and
  joint load. Nothing is dialled.
- **Binding:** wrapped / pinned / socketed. Read off what is put in: a fibre (leather, string)
  wraps, a metal wire pins, and no binding is a socket -- the handle simply fits the head.
  Joint capacity against cost.
- **Section** (flat / ribbed, mass against stiffness) and **heat treatment** are **not choices
  here.** Heat treatment is the *material*: hardened steel is its own file (mechanics 5.3), so a
  quenched blade is a hardened-steel blade and tempering is a material change, not a flag on the
  part. Section is **[OPEN]**: it needs a part that can carry a shape, and a part is an ordinary
  form item.

Tetra's *mutually exclusive improvement groups* are this list: one choice per axis, swapping
replaces.

## 4. As built vs as drawn

Each part was to carry its **target** geometry and its **built** geometry, as the gun barrel does
(`BarrelGeometry`), with the bake reading the built one. **Decided 2026-10-16 (overnight,
delegated):** kept minimal. Parts are ordinary form items (an ingot, a plank, a rod), so there is
no part to carry a geometry, and a head comes out at its target size. Built = target for now.
**Hand forging's answer is built (2026-10-21, §4.1):** the work the blows did against the work the pattern asked
is stored on the part (`MadeOf.dims["work"]`, read by `ModularDesign.Part.built`), and the bake reads it as thickness and mass. The
Mechanical Hammer shares it (§4.1). **[OPEN]** The Press and grinding: the Press should deviate least (a die
stamps one variant exactly), grinding corrects an edge; neither writes a deviation yet.

### 4.1 Forging by rhythm (author, 2026-10-20)

Hand forging is a **rhythm game**, not a bar to land a cursor on and not a sum to solve (the author
ruled out TerraFirmaCraft's: it is arithmetic on a slider). It is the way a player makes a part
by hand, and the way most **variants** (spec 3) are reached once recipe shapes run out.

**What the player does.** Hammer in hand, a hot blank on the anvil: a row of notes scrolls toward a
hit line, the anvil rings on the beat, and the player plays the pattern with two inputs, **tap**
and **heavy** (the hammer's existing swings, `HandSwing`). Each hit is judged clean, loose or miss.
There is nothing to calculate and nothing to plan.

**A variant is a rhythm.** The blank comes from one plain recipe shape; the pattern played on it
decides which variant it becomes: a rapier an even run of taps (drawing out), a machete slow heavies, a
katana a fold of heavies then taps. A new variant is a pattern line in data, so the library is not bounded by
the number of shapes a crafting grid could lay (a variant shape had to be unique across every part,
which is a wall the Tool Table's shape list removed for every variant: the player picks the shape,
nothing infers it from a layout). Each variant keeps its figures as now (`reach`, `section`,
spec 12) -- the rhythm chooses it, the type's data still says what it does.

**What makes it forging and not a toy** (every item is already in the mod):
- **Heat is the clock.** The blank cools on `ItemHeat`'s linear curve while it is played. Under the
  material's hot-working window the notes go *dead*: the ring turns dull and hits do nothing
  useful, so the player reheats. One heat allows a limited number of passes, so a long or hard
  pattern needs a reheat. Reheating costs fuel and loses a little mass as scale above the
  material's `oxidation_temperature`. The window check is the existing hard gate, now read
  mid-play.
- **Hardness sets the tempo and the notes.** A harder material plays slower and wants more heavy
  notes; one the hammer cannot move is the existing `St / hardness` gate and needs Strength
  (`MIGHTY`) or the Mechanical Hammer. The gate stays physical. **Built (2026-10-21):** a tap is the `BLOW` swing (3 St) and a heavy the `HEAVY` swing (5 St), `MIGHTY` (7 St) under Strength
  (`ForgeRhythm.Strikes.hand`). A pattern none of whose notes can land refuses to begin (`ForgeRhythm.movable`, "No blow here
  moves this metal"): the hard gate. A pattern some of whose blows land is playable and comes out short; each blow that goes into
  cold metal, metal still too hot, or under the hardness is *wasted*, and costs the Hand Hammer one extra point of durability
  (the accepted hand-tool exception) -- the force went into the tool, as CLAUDE.md's rule says. The table prints how many.
- **Misses never fail the part** (precision is never a hard gate, philosophy 7). Sloppy play adds
  flaws -- less edge toughness, a cold shut -- and moves the part's **as-built geometry** off its
  target, which is §4's deviation, now with a cause. **Built (2026-10-21):** `ForgeRhythm.work` sums the Fu of every blow that landed on live metal
  (`Deformation.landed`: `St / hardness`, the hammer's own rule, with the noise floor's jitter on each blow) against the Fu a clean
  run of the pattern asks (one landed blow per note, at least 1 Fu each, so a note a blow cannot land is a shortfall rather than
  a free note). The ratio, clamped to 0.5 to 2 (`MIN_WORK`, `MAX_WORK`), is kept on the part; `Part.built()` is its reciprocal and
  `Role.kept` (mass) and `Role.section` (thickness) multiply by it. **Short** (hits missed, notes struck cold, blows under the
  hardness) is thick and heavy, so tougher and blunter (`section` already trades bite for toughness); **overrun** (strays beyond the pattern)
  is thin and light, keener and weaker. Exactly 1 stores nothing, so a blueprint, which predicts the drawing, is unchanged. Flaws and
  work are separate signals on purpose: flaws judge timing, work judges how much metal moved, and a player who stops halfway and one
  who hammers twice as long can both be "a little rough". The part's tooltip prints the ratio at the truth tier.
  **Trap:** flaws alone were tempting (one integer, the judge already counted misses), but they cannot tell overrun from short,
  and the old model could not say that overrun is a different mistake from falling behind, which is the whole point of the
  Deformation overrun mechanic. The clamp is the one number invented here: unclamped, a bar nobody hit would be infinitely thick. The world resolves on what was truly built;
  nothing a screen shows enters it (philosophy 8).
- **There is a noise floor.** Each strike jitters a little whatever the player does, so a perfect
  part is never certain.
- **Two answers** (philosophy 5): a die or press stamps one variant exactly and quickly, a
  Mechanical Hammer is a perfect metronome with one fixed pattern; the hand player has the whole
  library and the unique shapes, at the cost of attention and time. Manual stays possible.

**No instrument.** Considered and cut: a metronome would only repeat the notes and the ring the screen already gives. A pyrometer (the blank's temperature, so the heat left) is the one forging instrument that would add information; forging upgrades of any kind are later and **[OPEN]**.

**Minecraft lens.** Minecraft already has a rhythm culture: note blocks, the anvil's sound and
redstone repeaters, whose delays are *tick-quantised*. So beats sit on a tick grid with a window of
about one tick either side, which makes judging fair and cheap on the server: the screen plays the
notes, the client reports the ticks it hit, the server checks them against the grid and decides.
The same lens makes automation natural: a Mechanical Hammer's cam is a pattern on the same grid.
A second player is an optional twist: one holds the tongs and one strikes, as smiths and strikers
did. Not required, not built for.

**Built (2026-10-20), first slice, moved into the Tool Table (2026-10-21, §4.2):** it began as the hammer in the main hand and a hot standard part in the off hand, `client/ForgeScreen` listing the variants that part could be forged into. That route is gone (`HandHammerItem.use` no longer opens anything, and `ForgeScreen` was deleted): a forged variant is now chosen on the table's shape list, its stock (the standard shape's cells; a standard part is no longer forged into a variant) put in the cells, and the pattern played inside the table's screen. The lanes are the same: tap on the left lane (left click, Z) and heavy on the right (right click, X). Notes are `t`, `h` and `.` in a string, one step of `ForgeRhythm.step` ticks each (6 to 10, slower for a harder metal). **A metronome clicks every step** from the start, higher on the first of each four, and the first note falls after a four-click count-in. `ForgeBeginPayload` names the open table's container and the shape on its list (the menu notes when and how hot the stock is); `ForgeFinishPayload` sends the blows and the menu checks it is still stocked and hammered; `ForgeRhythm.judge` (pure, same code as the screen) grades each note clean (within three ticks, a third of a second wide), loose (five) or missed, with a 4% noise floor on clean hits, counts stray blows and notes struck while the part is outside its hot-working window (dead), and turns the lost share into 0 to 3 **flaws** (`MadeOf.flaw`, then `Part.flaw`), each costing `FLAW_TOUGHNESS_LOSS` (15%) of the wearing part's edge toughness and `FLAW_DURABILITY_LOSS` (a sixth) of its durability, so a ruined part starts at half. The part item's name carries the variant and flaw (*rough drawn Copper Sword Blade*). A result sent faster than the pattern runs is refused. The part is handed to the player's inventory when it is judged, flaws and all. (The first shipped patterns, *drawn* and *upset*, were replaced by the archetype variants of §12.) **Volume is conserved:** a part's mass is its cells alone, no longer scaled by `section` (a drawn blade is longer and thinner, not lighter), which also changes the laid *narrow* blade. **Not built:** geometry deviation from sloppy play (only flaws so far), the hardness gate on the heavy blow, the Mechanical Hammer's pattern, per-pattern balance, scale loss in the Hearth (spec 4.2).

**[OPEN]**, each with a lean:
- Two inputs (tap, heavy) or all three swings? Lean two.
- Falling notes with audio, or audio only as a harder mode? Lean notes and audio.
- One fixed pattern per variant, or short patterns that chain? Lean fixed.
- ~~What the Mechanical Hammer's pattern is set by.~~ **Settled 2026-10-21:** a driven Mechanical Hammer touching the Tool Table (any
  of the six sides) replaces the player when a forged shape is begun (`ToolTableMenu.machineForge`). It plays the shape's own pattern, every
  note on its tick, at the one force its installed throw and gearing give (throw times gear advantage, to its ceiling: the same
  figure `onStroke` would strike with), so it is a perfect metronome and *cannot tell a tap from a heavy*: every note lands at one
  St. It is consistent and never perfect: the noise floor is seeded from the game time as well as the blows, so no two runs are the
  same part; it cannot reheat, so a pattern that outlasts the heat leaves dead notes; and a blow it cannot land wears the machine's
  head (`MechanicalHammerBlockEntity.absorb`), not the hand hammer. Because work is judged against the machine's own blows, a
  machine strong enough lands cleanly on a metal a hand cannot move (the 12 St short throw works iron, 6; steel, 15, wants gearing or
  a stronger machine), and is no better than a good hand on soft metal. The result is handed over at once rather than after the pattern's
  length. **[OPEN]** the pattern time (instant now), a table that does not need the hand hammer in its slot when a machine plays, and a machine that
  plays a pattern of its own cam rather than the shape's.
- ~~How the as-built figures reach the bake and how a flaw reads on a tooltip.~~ **Settled 2026-10-21:** `MadeOf.dims` and `Part.dims` carry
  the work ratio under `ForgeRhythm.WORK`; the bake reads it through `Role.kept`/`Role.section`; the tooltip prints it.
- Hot-working window per material for wood and stone: carving as the same act without heat.

### 4.2 The Tool Table (author, 2026-10-21)

One block makes every modular tool, weapon, shield, bow and piece of armour (guns are made at the Gun Table, the same bench with a type filter, 8.3). It is Tinkers' tool station by shape and the Blueprint Table's opposite by purpose: **no numbers, and it does the work.** It replaced three things at once, each of which had worked and each of which had a wall the table removes:

| Was | Wall | Now |
| --- | --- | --- |
| `ModularPartRecipe`, a hammer and stock laid in a shape in a crafting grid | a shape had to be unique across every part, a spear needed a 4x4 bench, laid shapes were the only way to name a variant | a shape is *chosen* from a list; no layout is read |
| `ModularRecipe`'s tool assembly, parts laid in a pattern | the layout told the types apart, so tips and bindings had to live outside every pattern and were added by a second recipe | the parts imply the type; a tip and a binding are parts like any other |
| the off-hand route to `ForgeScreen` (hammer in the main hand, a standard part in the other) | forging a *part* that was already made, so a forged variant cost a hammer and a laid standard part; the screen floated over the world | the rhythm is played inside the table, from stock |

**The table is built** from two copper plates over four planks (`feedback:tool_table`): a plate is a bar of metal worked flat, so the plate form not the ingot, and copper plate is hand-hammered from the first ingot, before the Rolling Mill. **The hammer is the table's.** A Hand Hammer sits in its own slot (any `HandHammerItem`: a class, not an id), and every act below spends one point of it with `hurtAndBreak` -- the hand tool's accepted wear (CLAUDE.md). No hammer, no work. Everything put in stays in the block and drops when it is broken.

**Three modes, one bench.**
- *Parts.* A scrolling list of every shape of every shaped role, once each: the standard shape, each `Shaping.laid` variant and each `Shaping.forged` variant (`ToolTableShaping.options`). The stock goes in a 3x3 of cells; the pile must be exactly the shape's cells (a forged variant costs the standard cell count), one material and one form from `Shaping.stock`, never a part. A material with a hot-working window must be inside it, judged on the coldest piece, and the part is stamped at that heat (`ItemHeat`). A laid or standard shape comes out at once; a forged one is played in the screen.
- *Assemble.* Six slots in code, always six, all alike (two rows of three), each holding up to eight (`ToolTableAssembly.CAPACITY`). The eight is an arbitrary ceiling, not physics: a shaped part is one item whatever the slot, a role's count is checked by `fits`, and the audit reports any type whose role asks for more than eight (a shield's six planks are the most today). It replaced a split of heads one, rod three, misc eight, which let capacity tell slots apart and forced a shield's planks into a "misc" slot. Which slot holds what never matters; the screen **labels each filled slot by the role its part fills** in the tool the parts make (a part the parts do not use reads "?"), and the left column names what the parts could still become. The type is **implied**, never chosen. Every viewer's output is remade within a tick of any slot changing, by whoever changed it (`ToolTableMenu.broadcastChanges` compares the contents with what the output was made from).
- *Rework.* A finished tool in a tool slot, plus stock: mend, swap a part, add a tip or binding, or take it apart (`ModularRework`, moved out of `ModularRecipe` unchanged but for the dismantling).

**The inference** (`ToolTableAssembly.infer`). For each type, every non-empty slot is assigned a distinct role that accepts it (`Role.accepts`; a shaped role wants a shaped part, one item) so that the role-to-count map equals one the type is made from (`ModularType.assemblies`: each pattern's letters counted, plus each pattern with and without a role no pattern lays, a tip or a binding). It is a backtracking search over at most six slots and six roles. When several assignments fit (the twin's two heads) the one whose parts sort lowest wins, so a slot's position never matters and **the same parts always make the same tool**.

**One set of parts, one tool.** `ToolTableAssembly.audit` runs when the types load (`ModularTypes.apply`) and logs an error naming both types whenever two accept the same part set: some bijection of their roles with equal counts and overlapping forms. Wildcards (`*`, `*_planks`) are conservative, so it errs towards reporting. It also reports a type that needs more than six slots (none does; a sword with everything is five). **Trap, found by the check:** the four armour pieces' lining accepts anything fibrous (`*`), so a wool plate meant for boots would have been a lining for a chestplate. A role that is not shaped now takes a shaped part only if it names that part (the crossbow's lock takes a trigger group), which is the rule `ToolTableAssembly.fits` states and the check mirrors.

**Taking apart.** Shaped parts come back as `MadeOf` stacks, raw stock as stacks, to the player. The obvious dupe is repair: break a tool, take it apart, build it again with a full wearing part. So **the wearing part returns short by the wear** (rounded down), and a short shaped part comes back as raw stock, because the shape went with the metal. Enchantments, a coat and the edge are lost with the tool.

**What a player's forging is, now.** The rhythm (spec 4.1) is unchanged -- `ForgeRhythm`'s judge, live window, noise seeded from the hits, flaws, perfect and fanfare -- but its payloads name the open container and the shape: the menu validates that it is open, the hammer is in, the stock is exactly there and workable, and the result did not come faster than the pattern. The part goes to the player's inventory.

**Explicit part items (author, 2026-10-21).** The slot an item sits in must never decide what it is, and neither may an ambiguous item. So `ToolTableAssembly.audit` also fails any type in which two roles accept an overlapping item (the twin's two heads excepted: they are symmetric by design and sorted), and every role that overlapped became a shaped part with an item of its own, shaped in the Parts tab: `tool_tip` (gem, shard, nugget; the tip role of nine types) and `tool_binding` (leather, wire, string) -- raw stock no longer fits either role; `sword_guard` and `sword_pommel` (a guard and a pommel both took an ingot, a plank, bone); `shield_rim` (the rim took an ingot the face took, and a rod the grip took); `crossbow_prod` (two cells; prod and stock both took sticks, rods and bone). A shaped part counts one item in `ModularType.assemblies` however many cells its shape spans. Armour studs stay counted raw gems: the lining is a wildcard narrowed by a required trait (fibrous), and the audit trusts such a wildcard not to meet an explicit form. `ModularConvert` fills a tip through `ToolDesigns.part` like any drawing, and `/feedback build parts` hands out the part items.

**Heat: the Hearth (2026-10-21).** Heat at the table is the stock's own (`ItemHeat`), cooling linearly at 1.5 Tu/t, so a long pattern was a race from the fire. The **Hearth** (`machine/hearth/`) is a brick pan set on a Firebox (or lava, or any `HeatSource` below it, the way a crucible is) with a Tool Table touching one of its four sides. It is a `ThermalBody` driven by `Heat.tick` on the difference, so it approaches its fire and never passes it: open on every face (`HEARTH_LEAK` 0.06 against the fire's 0.6) it settles about nine tenths of the flame, with a mass (`HEARTH_MASS` 60) between the two crucibles. Each tick it moves every piece of stock in the table's cells `HEARTH_STOCK_GRIP` (3%) of the way up to its own temperature, if hotter, and stamps it (`ItemHeat.set`); cooling stays the item's own, so stock beside a pan settles 1.5 / 0.03 = 50 Tu under it, never over. *Why the pan reaches into the table, not the table reading the pan:* the table is a bench with no thermal code, and a source publishing heat for whoever sits by it is the shape of every other pairing (a crucible does not know it is over a firebox either). *Why one rule for all stock:* only a material with a hot-working window is reheated (read off the material, as `ToolTableShaping` already does when it stamps a part), so planks and cobblestone beside a red pan stay cold, and no item is named. *Why too hot is not punished here:* stock above its material's window is already refused by `ToolTableShaping.check` until it cools, a gate with a visible cause and no loss; the pan is a tool for holding heat, and the player who leaves a part in it too long meets the same wall as one who carried it too hot. **Lens:** the world adds nothing exotic; the choice is where the fire is (a firebox must be next to the table, so the bench gets a chimney and a fuel chore, which a bench away from the fire does not), and lava under the pan holds it at about 1060 Tu forever, just under steel's window, as everywhere else. A hot pan is a hot block, so `HotContact` burns a bare hand on it with no code of its own. Wear: none, by design; nothing misused wears it. **Not modelled:** stock cooling the pan (an ingot against a mass of 60 would not move the figure), reheating across an unloaded gap (the pan is caught up, the stock is not), and an air supply (a bellows on the firebox beneath raises the pan as it raises a crucible). **[OPEN]** scale loss: a part left in a pan above the material's `oxidation_temperature` should lose a share (an overrun on the finished stock, philosophy 6) rather than only be refused; unbuilt because no flaw channel exists for stock yet.

**One model, so the two tables cannot drift (2026-10-21).** The Blueprint Table was built around the grid recipe's patterns and was left behind when the table moved to roles and counts: it drew one pattern index, a material per role and `Part(role, material, form, count)` with no kind or variant, and validated by matching the drawn counts to `type.counts(i)`, so it could not draw a tip, a binding, a guard, a twin, a variant, or any part item. `ToolDesigns` is the one place that says what can be made, and everything asks it: `ModularType.forms()` and `extras()` (the ways to make a type before and after its optional parts; `assemblies()` is their product), `shapes` (the Tool Table's own shape list, per role), `materials` (what a role takes, for a shaped part the stock it is made from), `part` and `starter` (the design a type's defaults make), `stacks` and `layout` (a design as the six slots would hold it) and **`make`, which puts a design on the Tool Table's inference and reads it back**. `predict` (the blueprint's figures), `build` (`/feedback build`), `ModularConvert` (the starter it converts from) and the JEI tool cards are that one computation or its parts; the Tool Table's assemble tab is `ToolTableAssembly.infer`, which `make` calls, and `ModularRework.dismantle` is `items` run the other way. The load-time `ToolTableAssembly.audit(types, materials)` now also runs `ToolDesigns.consistency`: every type's starter design, and for every form and every combination of extras a plain drawing in the first material that fits each role, must be read back by the table as that same type, or the load logs an error naming the drawing. **Not shared:** the bake itself, which is `ModularBake` on whichever design is handed to it. A gun is drawn and made through the same model since 2026-10-21 (8.3).

**What was tempting, and wrong.** *A type picker.* Tinkers' station fixes the slot count by the tool; here the parts name the tool, which is why two types accepting the same parts is a load error rather than a menu. *Output computed into the player's hand for a forged part.* A forged part is not a preview (it depends on the playing), so it is handed over when judged rather than sitting in the result slot, which only ever holds what the contents make. *Keeping the grid recipe beside the table.* Two routes to one tool is two rule sets that drift; the gun's branch is the only part of `ModularRecipe` left, and it is deleted when the gun has its own table.

## 5. Types

Defined as data, one file per type (Tinkers' tool definitions), listing roles, actions, the
block tags it is effective on, and which choices apply. A new type is a JSON file and a lang
line, not a class. A type has a parent (Silent Gear's tree), so a cleaver inherits from axe.

**Vanilla, all of it:**

| Item | Parts | Physics |
| --- | --- | --- |
| pickaxe, shovel, axe, hoe | head, handle, binding | §2.1 straight |
| sword | blade, guard, grip, pommel | pommel is counterweight: it moves balance, the swing-speed choice |
| mace | head, haft | **damage from the fall**: impact is head mass × the wielder's fall speed. The heavy core is a material (§5.1) |
| trident | tines, shaft | thrown: mass sets the speed, the baked damage is the blow; Riptide/Loyalty stay enchantments |
| bow | limbs, string, grip | draw weight from limb `yield_strength` × section; heavier draw, faster arrow, slower draw |
| crossbow | prod, stock, lock | blueprint route; the lock is a machined part like a gun's action |
| shield | face, rim, grip | blocks a blow's `Fu`; face material takes heat (the approved thermal shield idea) |
| helmet, chestplate, leggings, boots | plate, lining | §5.2 |

**Decided 2026-10-16 (overnight, delegated):** shears are modular (two blades and a pivot), built
after the vanilla weapons. **Fishing rod, flint and steel and brush stay vanilla:** none is a tool
whose figures a material changes in a way a player would act on -- a rod's cast, a spark's
strike and a brush's sweep do not get better by being iron rather than copper in any way this
model can say. If one ever does, it is a type file and an item.

**Custom, decided 2026-10-16 (overnight, delegated): mattock** (dig and chop, each at half),
**sickle** (area harvest), **cleaver** (heavy, short, parent axe), **spear** (long, slow) and
**maul** (a head on a handle that stuns what it hits), built after every vanilla type. **Pry bar**
(lifts a block whole) and **prospector's pick** (strikes rock and reports an adjective about what
is in it -- an instrument, so it reads and never acts) stay **[OPEN]**, future.

### 5.1 Heavy core

A material file like any other. **Minecraft lens:** the heavy core is found only in trial
chambers, sealed, and it is the one item vanilla makes *dense on purpose* (the mace's whole
mechanic is its mass). Smallest departure: a metal far denser than anything smeltable — about
three times iron — that cannot be melted in any vessel the game has (so it is found, never
made), very hard and brittle. As a mace head its density is the point; anywhere else its mass
is a cost. **Decided 2026-10-16 (overnight, delegated):** `material/heavy_core.json`, kind metal,
density 23.6 (three times iron), hardness 9, toughness 20 (brittle against its hardness),
`yield_strength` high, `melting_point` 4000 (above any flame the game has, so it is found and
never made), and its form `heavy_core` is `minecraft:heavy_core`. **Not workable**: no form
step reaches it, so it is used whole, as a mace head.

### 5.2 Armour

Plate, mail and padding as layers. Plate stops a blow by hardness and thickness; mail spreads it;
padding insulates (`ThermalBody` heat from a furnace or lava) and absorbs. Mass (`Mu`) slows the
wearer — protection against mobility is a trade from physics, not a tier. Netherite stays
property-driven (`netherite.json`), never id-driven.

### 5.3 Families and twin-headed tools (author, 2026-10-20)

Types group into **families** (names not final): blades (sword, mace, trident, spear), twin-headed
(pick, axe, hoe, twin). `family` (still the bake's enum) and the parent chain exist already; this is naming and shared
parents.

A **twin-headed tool carries two head roles, each its own kind and material** (pick+axe, axe+axe,
a hard pick head against a tough axe head). The old mattock was *one* head digging two tags at a
flat `0.5` each -- an authored tax, replaced (see Built, below).

**Why not a multitool** (the incentive is real: Minecraft's scarce resource is hotbar slots, so
the cost has to be physical). What the bake does today (`ModularBake.melee`): every non-rod part
adds to the swing's moment of inertia, tip speed falls with it, and impact goes with tip speed
squared. So an off head slows the swing and weakens the blow with no number written; a dedicated
tool carries one head and pays none of it. Light heads and handle shrink the tax; dense ones grow
it. **Read from the code, not assumed:** every non-rod part adds to the swing's moment of inertia, tip speed falls with it, and impact goes with tip speed squared. So an off head slows the swing and weakens the blow with no number written; a dedicated tool carries one head and pays none of it. Light heads and handle shrink the tax; dense ones grow it. **Decided and built 2026-10-20 (author):** the off head adds to joint load too, since the handle accelerates both -- `ModularBake.joint` sums every striking head's mass and takes the weakest of them, so a long handle no longer carries a free counterweight.

**Why a second head at all, and why not a harder block.** The dig gate is the edge's hardness
(`ModularStacks`), a material fact two alike heads cannot change, so there is no "+1 mining
level" for two alike (it would be a flat bonus with no cause). Two alike heads give a **spare
edge**: the striking head is picked by the block's tag, never a free flip (a free toggle is not
a choice, §3), and among heads that match, the keener strikes first until it chips, then the
other. A counterweight head also lowers the bending a long handle sees. **[OPEN] build:** the
bake makes one figure set per stack; per-head dig speed and wear need one per head.

### 5.4 Signature behaviours (author, 2026-10-21)

A variant should be a different *way to fight*, not different numbers. Each archetype gets one
**signature behaviour**, a `trait` on its `Variant` with its parameters in data, built from a small
set of reusable pieces. Precedent: vanilla's mace (damage scales with a fall) and, in the newest
Minecraft, the spear (damage scales with velocity). Realism is not the test; being a choice is,
and where possible a trait reads a figure the bake already makes (mass, bite, swing, reach).
**Minecraft lens:** the game already gives the player sneaking, sprinting, riding, shield blocking
and attack-cooldown charge, so every trait keys off one of those or a block tag; none needs a new
input. Proposed, only the spear's is approved:

| Archetype | Trait | Shape |
| --- | --- | --- |
| rapier | flurry | hits within about a second on one target stack bonus damage; a miss or a new target resets it |
| katana | opening cut | the first hit after a few seconds unattacking deals a big bonus and bleeds (a keener edge bleeds longer) |
| machete | hack | hits the unarmoured much harder and the armoured less; clears leaves, crops and vines in a sweep |
| claymore | overhead | a fully charged swing sweeps all around and knocks back; a half charge is weak |
| dagger | backstab | big bonus from behind or against a mob that has not noticed you (sneaking counts) |
| cutlass | flourish | a kill resets the swing; more damage with two or more enemies close |
| felling axe | timber | chopping a log fells the connected tree; wear scales with the logs |
| hatchet | throw | use throws it; damage scales with the distance flown |
| bearded axe | hook | disables a shield for much longer |
| prospector's pick | sounding | use sends a pulse that outlines ore (by block tag) within a short range for a few seconds, at a durability cost and a cooldown; an instrument, it never mines faster; range could grow with the handle |
| miner's pick | vein | breaking an ore breaks the connected vein up to a small cap, at extra wear per block |
| maul | stun | already built |

**Spear: the newest spear (author, with the wiki's figures).** Reach 4.5 blocks with a minimum of 2
(it cannot hit what is within two blocks of the wielder), hitboxes inflated by 0.125, it attacks
through non-solid blocks (cobweb, tall grass), cannot crit or sprint-knock-back, and can hit several
entities in one attack. Two attacks. **Jab**: the ordinary attack, long reach, weak. **Charge**:
hold use to lower it. If wielder and target close at 4.6 blocks a second or faster it deals the
spear's damage multiplier times the **relative velocity** (blocks a second), knocks back from 5.1,
and dismounts a rider; it goes through three stages held out (*engaged*: damage, knockback and
dismount; *tired*: damage and knockback; *disengaged*: damage only), can hit several entities, and
works while the jab is on cooldown; it is not touched by Strength or Weakness. **On this mod's
numbers:** reach comes from the baked reach figure (a spear is already the long type), the charge
multiplier should come from the head's mass and `strike` (a heavy head hits harder at the same
speed), and the stage lengths could come from the joint figure (a weak binding tires sooner).
**Build notes for 1.21.1** (the spear does not exist there): a custom use animation
(`IClientItemExtensions` pose), `onUseTick` and `releaseUsing` for the stages, the relative velocity
tracked from per-tick positions (a player's server-side delta movement is unreliable), a min-range
and pass-through check in the attack event or a custom ray, and server-side validation of the
charge. Hitbox inflation and attacking through grass are the awkward parts (picking is done by the
client); they can follow the first cut. **[OPEN]**: the
sounding pulse's range and whether it names what it finds; per-variant dig tags.

## 6. Reading what you made

Philosophy §8: what you need is free, what you have costs. **The tooltip gives adjectives**
("head-heavy", "keen", "brittle", "light in the hand"). **Exact figures need an instrument.**
The blueprint gives the *target's* figures for free, because it is paper.

The calipers are carrying too much already (dimensions, condition, work done). New physical
instruments, each reading one thing:

- **Balance scale** — mass (`Mu`).
- **Scratch file set** — hardness, by which file bites (an adjective ladder, then a figure).
- **Impact pendulum** (a block, Charpy-style) — toughness: strike a sample, read the energy it took.
- **Strike post** (a block) — what a weapon delivers per blow, swing after swing.
- **Calipers** — back to dimensions only.

## 7. Replacing vanilla

The furnace precedent (`TODO.md`, memory *vanilla-furnace-total-conversion*):

- Vanilla tool, weapon and armour **recipes disabled**.
- Every vanilla item has a **canonical design**: `iron_pickaxe` is an iron head, wood handle,
  standard everything. A converted item **keeps its vanilla name** ("Stone Shovel"), derived from
  the design, so nothing reads differently to a player who never opens a bench.
- **The head is rolled from its family (author, 2026-10-21).** The canonical design is the
  *default* of a family, not the only outcome: the primary part is drawn from every shape that shares
  its part item (an iron pickaxe may arrive as a heavy, prospector's or miner's pick; a trident as any
  spear; a mace as any maul; boots as sabatons). The roll is deterministic from the stack plus a salt
  the source supplies (loot: the loot roll; mob: its UUID and slot; trade: the trade's roll; pocket:
  player UUID and slot), because a plain vanilla stack carries nothing else to tell two apart.
  Material is never rolled; a roll the material cannot be drawn in falls back to the canonical shape.
- **Converted on arrival**: chest and mob loot (a global loot modifier), mob equipment (on spawn),
  villager trades (trade event), and any vanilla stack that still turns up in an inventory.
- Everything that asked for a vanilla item by id moves to tags (`ecosystem-compat-priority`).
- **Bootstrap exception (2026-10-21):** the Tool Table is built from planks, iron and cobblestone, and the hammer from cobblestone and sticks, but nobody could dig the iron. Wooden and stone **pickaxe, axe and shovel** keep their vanilla crafting recipes (the `neoforge:false` condition is off those six files in `data/minecraft/recipe/`); `ModularConvert` turns them modular in the pocket, and they can be taken apart at the table. Every other vanilla tool, weapon, shield, bow, crossbow, shears and armour recipe (the turtle helmet included) stays off, and so do the netherite smithing upgrades.

## 8. Guns on the same core

**Started 2026-10-20, finished as a tool type 2026-10-21 (user ruling, 8.3; compile only, nothing played).** 8.1 is the history of the first migration and 8.2 the reasons it was reversed; 8.3 is what stands, 8.4 the variants, traits and names. `GunDesign`, `GunBlueprintItem`, the Gun Stand, `GunAssembly` and `ModularRecipe` are deleted. No survival world uses the mod, so nothing converts old guns.

### 8.1 The mapping (the first migration, 2026-10-20; the gun-specific rows below no longer stand, see 8.3)

**Shared, and moved onto the core:**

| Core idea | Gun, after |
| --- | --- |
| Parts are single items carrying `MadeOf` | `feedback:gun_barrel`, `receiver`, `slide`, `bolt`, `trigger_group`, `cylinder`, `magazine_tube`, `suppressor`, `muzzle_brake`, `relief_vent`, `barrel_shroud`: one `ModularPartItem` each, material on the stack. Form types flagged `"part": true` generate no per-material item; a material's form of that name *is* the shared item, and the form steps that make it (`ingot_to_receiver`...) emit a `MadeOf` stack and match one by component. |
| Target vs built geometry | The blueprint holds the target (bore, band, wall); the barrel part carries its built `BarrelGeometry` beside `MadeOf`. Machining (`process/Machining`) still cuts it; it now finds the material through `Material.find(stack)`. Unchanged in meaning, and it is the one place the core's open "as-built" question (section 4) already has an answer. |
| A type as data | `modular_type/gun.json`: family `gun`, `blueprint: true`, a role per part kind and the forms it takes. It has **no patterns**: the blueprint's `GunDesign.assembly()` is the pattern, because which parts a gun has depends on its action, feed, sight and fittings. |
| One assembly recipe | `ModularRecipe` assembles a gun when a Gun Blueprint is in the grid (the same gate the crossbow's blueprint passes at the Tool Table) and refits one when a built gun is. `GunRecipe` and `GunRefitRecipe` are gone. |
| Parts found by what they are made of | `Material.find(stack)` (reads `MadeOf` first) is the only lookup. |
| Condition and heat live on the item | Already so: `GUN_WEAR`, `GUN_DAMAGE`, and barrel heat as `ItemHeat`'s stamp. Left where they are. |
| Tint | A gun part is tinted by its `MadeOf` material through `ModularClient`, like a head. |

**Gun-specific, and staying so:** the physics (`GunDesign.bake`: chamber rating, peak pressure,
muzzle work, kick, cycle), the cartridge, magazine and chamber, the launcher fork (chamber or
coil), what a fitting does to the bake, shot logic, `GunFigures` and its tooltip, the Gun Stand
and its screen, and the Reloading Press.

### 8.2 Rejected, and why

**Superseded 2026-10-21 (user ruling): the first four bullets below (a gun as a `ModularDesign`, `GunFigures` as `ModularFigures`, one type per layout, a grid pattern) were the 2026-10-20 position and are reversed by section 8.3.** They stay as the record of why each was tempting; the trap each names (fields no other type uses; one-off figures) is answered in 8.3 by a design that has a place for them.

- **A gun as a `ModularDesign`.** Its record is type, a list of `(role, material, form, count)`
  and an edge. A gun's design is a bore in whole millimetres, a barrel band, a wall band, a sight,
  a launcher, an action and a feed, an optional built barrel shape, fittings, a wrap and a second
  barrel. Every one would be a field on a record the other thirty types do not use. The gun keeps
  `GunDesign` as its design component; the *type* is shared, the *record* is not.
- **`GunFigures` as `ModularFigures`.** Section 2 and the brief: no gun fields on the shared
  figures. A gun's figures are pressures and impulses; a pick's are bite and swing. The type
  names which bake runs (`family: gun` runs `GunDesign.bake`), and the result is stored under its
  own component.
- **One type per `GunLayout`** (pistol, marksman rifle, shotgun, LMG). A layout does not decide
  the part list; the action, feed, sight and fittings do, and those vary inside a layout. One
  type with optional roles says the same thing once.
- **A grid pattern for a gun.** Patterns are how a type is told from another by shape. A gun is
  told by its blueprint, and its parts may sit in any cells.
- **Collapsing `grip`.** It is a general form (tongs use it, and organics have it); only the
  parts that exist solely for guns collapse. A gun reads a `grip` item by `Material.find` as before.
- **Repair of a gun by its stock.** A tool's wear is a consumable's; a gun's is bore erosion that
  bottoms out at a floor, and overpressure damage is misuse's record. Putting stock back is a new
  mechanic the gun spec never asked for, not a migration. **[OPEN]**
- **Old-stack conversion.** Ruled out by the user the same day.

### 8.3 Guns are a tool type (author, 2026-10-21)

**Ruling:** *guns literally a tool type, the same way as single-headed weapons, blades and two-headed tools.* A gun is a `ModularDesign` of parts with materials and variants; it gets the same traits, adjectives, Shift glosses, tooltips, JEI cards, wear and Tool-Table-style making as a pick or a sword. `GunDesign` and the Gun Stand do not survive. **A Gun Table** works exactly as the Tool Table does and takes only guns: the reason is cost later (a gun table built on its own would be harder to make once the Tool Table has moved on), so it is the Tool Table's code with a type filter and a spec sheet against a loaded round, not a second implementation.

What each tempting reason in 8.2 gets instead:

- **Fields no other type uses (bore, wall, sight, launcher, action, feed, built barrel).** They become the parts they always were. Calibre, barrel band and wall are *variants of the barrel part*; the frame class (handgun, rifle, shotgun, machine gun: 8.4) is a variant of the receiver, the primary role, exactly as rapier, katana and claymore are variants of the blade; the action and the feed are parts with a `kind` (slide, bolt, lever, pump; box, tube, cylinder, belt); sight, muzzle, vent and shroud are parts. `GunLayout` stops being stored: it is read off the receiver's variant, the action and the feed.
- **Continuous numbers.** Bore was a free 5 to 20 mm scalar, against philosophy 3, and becomes a set of **calibre classes** (named, a barrel variant parameter). The core also gains the *place* for a continuous figure so a type can have one later without a redesign: a part may carry `dims`, a name-to-float map of as-built measures (a barrel's real bore and length as the lathe left them), declared by its role with a range and a unit; the bake reads them, a type that declares none stores none, and the Tool Table writes them from what the machine actually did. The barrel's built shape (`BarrelGeometry`) moves there.
- **Figures that are pressures and impulses.** `ModularFigures` is already a fixed-order list indexed by an enum precisely so a new figure is one line: chamber pressure and rating, muzzle speed and energy, bullet mass, impulse, cycle, spread, kick, report, heat and shed factors, overpressure are `Fig`s. A type has the figures its family bakes; a pick leaves the gun's at zero as a gun leaves the pick's. `GunFigures` survives as a read-only view over them, so shot logic, the HUD and the spec sheet change little.
- **A bake that needs a round.** `GunDesign.bake(materials, round)` is the physics and stays (moved, not rewritten); the stack stores the figures at the reference load, as now, and a shot or the spec sheet bakes against the real round.
- **The signature behaviour.** The receiver's variant names a trait with its parameters in data (spec 5.4), and `ModularTrait` gains the gun's hooks (fired, reloaded, a round landing). The variants, designations and traits are 8.4.

**Build order, each stage compiling and none played until the author says:**

1. The core's two additions: `Part.dims` with role declarations, and the gun `Fig`s and `Family.GUN` bake entry (a pure move of `GunDesign.bake`).
2. Calibre and barrel variants, receiver variants, action and feed kinds in `modular_type/gun.json`; rounds fit by calibre class.
3. Guns as `ModularDesign` everywhere: the stack, `GunShots`, `GunHud`, `GunTooltip` through `ModularModifiers`, `FCommands`, JEI; the paper design becomes a blueprint of a `ModularDesign` (Blueprint Table draws it).
4. The Gun Table on the Tool Table's menu and shaping code, with the spec sheet; the Gun Stand removed.
5. Gun traits: the hooks and the first archetype traits the author rules on.
6. Delete `GunDesign`, `GunLayout`'s stored form and the gun component; the wiki and the guns spec follow.

**Why this was tempting to avoid, and the trap:** a second record was cheaper the first time, and it left guns without traits, adjectives, variants, wear steps or a table that grows with the core: every capability added to tools since 2026-10-20 had to be written twice or skipped for guns. That is what the author saw missing.

### 8.4 Gun variants, traits and names (author's rule, 2026-10-21; the lists are the model's proposal for the author to cut)

**The rule.** A **variant** is a *fundamentally different gun*, told by something that changes what the gun is, not how it is built. A **trait** is an aspect of a gun: a mechanism it has, read off its parts. A gun has several traits and one variant. The hierarchy and names are **real-world firearm designations**, not game labels (unlike the blades' fun archetypes): a revolver and a pistol are one thing, a handgun, because the only difference is a hammer and a cylinder; that difference is a trait (anything with a hammer and a cylinder can be fanned).

**Variants (the receiver's shapes), four:**

| Variant | What makes it fundamentally different |
| --- | --- |
| **handgun** | Held in the hand, no stock: nothing braces it against the shoulder, so kick and spread are the wrist's, and a second can be held in the off hand. Pistol, revolver, derringer and machine pistol are all this. |
| **rifle** | Rifled bore and a shoulder stock: the barrel is long enough to spin and steady the bullet, and the stock takes the kick into the body. Carbine, assault rifle, battle rifle, marksman and sniper rifle are rifles told by barrel, action and sight. |
| **shotgun** | Smoothbore, made to throw shot: a bore with no rifling and a pattern that spreads with distance rather than a bullet that stays a bullet. The only variant that takes shot (it still takes a slug). |
| **machine gun** | Built for *sustained* automatic fire: a heavy barrel and a feed that does not run out (belt or drum), a receiver that has to shed heat for as long as the trigger is held. Its trouble is thermal, not ballistic. |

Not variants, and why: **launcher** (chamber, coil; pneumatic, spring and steam later) is a part with its own role, because a coil rifle and a gas rifle are both rifles; **submachine gun, carbine, assault rifle, sniper rifle, derringer, sawn-off** are *designations*, read by a rule from variant, calibre class, barrel band, action, feed and fittings (below), never chosen.

**Designation (the item's real name).** `gun.json` carries an ordered list of `designations`: each a name and conditions over the variant and the traits and calibre class, the first that matches names the item as a tooltip's class line ("Revolver", "Submachine gun", "Sniper rifle"), exactly as the adjectives are read off figures. Nothing in play depends on it; it is how a player and the wiki call the thing. Proposed: handgun with the cylinder trait = revolver, else pistol; handgun with select fire = machine pistol; handgun with two barrels = derringer; rifle with select fire and a small calibre class = submachine gun; rifle with select fire and a medium or large one = assault rifle; rifle with a short barrel = carbine; rifle with a long barrel, a scope and a bolt or semi-automatic action = sniper rifle; shotgun with two barrels and a short barrel = sawn-off; machine gun with a box and a bipod = light machine gun, with a belt = general-purpose machine gun.

**Traits (aspects), each a real mechanism and one `ModularTrait` with parameters in data; a gun gets every one its parts show:**

| Trait | Read from | What it does |
| --- | --- | --- |
| **hammer** | an action with a hammer (break, lever, pump, self-loading) | Single action: cocked by the cycle. A cylinder plus a hammer gives **fan**. |
| **fan** | hammer and cylinder | Hold use to dump the cylinder as fast as the hand can slap the hammer: a rate no cycle limit gives, paid in spread and kick that grow with each shot and in a hot cylinder. |
| **double barrel** | two barrels (break action) | Sneak-use fires both at once: double the shot, one shared recoil. |
| **select fire** | automatic action | Semi, burst or full automatic, switched in hand (`GunModePayload` already does this). |
| **bolt lockup** | bolt action | The strongest receiver (it rates a higher chamber pressure for the same steel) and a tighter group, paid in a manual cycle. |
| **manual cycle** | lever, pump, bolt, break | The next round is brought up by the shooter (`GunLayout.Action.manualMs`), not the gun. |
| **blowback** | self-loading or automatic without a locked breech | The bolt's mass alone holds the breech shut, so past a pressure the bolt is thrown back hard: a high-pressure round wears the gun and kicks harder. Pistol calibres only. |
| **gas operated** | self-loading or automatic with a locked breech | Cycling energy is taken from the gas: a smooth, fast cycle that a high-pressure round does not hurt, paid in mass and in heat at the port. |
| **closed bolt** | the default for every action that chambers a round | A round sits in the hot chamber: above its charge's ignition temperature it **cooks off** on its own. A real hazard that the heat model already has the figures for. |
| **open bolt** | machine gun | Fires from an open bolt: no round in the chamber to cook off and the air cools it, paid in a poorer first shot. |
| **belt fed** | a belt feed | The rounds are a chain: a long reload and a magazine that is the belt's whole length. |
| **braced** | a bipod, or sneaking still | Supported fire: kick and spread fall while braced. |
| **off hand** | handgun | A handgun in each hand. |

Fittings (suppressor, brake, vent, shroud, eddy sleeve) are *parts*, not traits; they change figures, and a trait may read them (a brake helps a fan).

**Where each lives:** a variant is a `Variant` on the receiver's role (`trait` and `params`); a trait that a part implies is named by the part's `kind` in the type's data (`hammer` by the action kinds, `belt fed` by the belt feed). `ModularTraits` already collects the type's, the variant's and (new) every part's.

**Built 2026-10-21 (compile only), in the order of 8.3.**

1. **Core.** `ModularDesign.Part.dims` (name to float; also `MadeOf.dims`, so a machine writes them on the part item) and `ModularType.Role.dims` (a `Dim`: min, max, unit, default; `Role.dim(part, name)` reads the part, else the shape's nominal (a variant's `params`), else the default, clamped). `Role.alt` (a second part of another role's kind, exempt from the overlap audit) and `Role.bulk` (items an optional raw part takes). `Shaping.base`: the name of the standard shape (a receiver's is a *handgun*; its variant of that name, which has no shape, holds the traits). `ModularType.extras()` is now every role no pattern lays, so a gun's fittings, sight, second barrel, winding and wrap are optional parts exactly as a tip is. The gun `Fig`s (`PEAK_PRESSURE`, `CHAMBER_RATING`, `MUZZLE_SPEED`, `MUZZLE_ENERGY`, `BULLET_MASS`, `IMPULSE`, `CYCLE`, `SPREAD`, `KICK`, `COIL`, `REPORT`, `HEAT_FACTOR`, `SHED_FACTOR`, `OVERPRESSURE`; gun mass is `MASS`). `Family.GUN` in `ModularBake` calls `weapon/GunSpec`, the old `GunDesign.bake` moved: `GunSpec.of(design, type, materials)` reads variant, action, feed, sight, launcher, fittings and the barrel's real shape off the parts; `GunFigures` is a read-only record over the `Fig`s (`GunFigures.of`). `BarrelGeometry` is a view of the barrel part's `bore`, `outer` and `length`; `Machining` writes them on the `MadeOf`.
2. **Data.** `modular_type/gun.json`: receiver variants handgun (the base), rifle, shotgun, machine gun; barrel variants are the calibre classes (`calibre_rimfire` 5 mm, `calibre_pistol` 9, `calibre_intermediate` 12, `calibre_magnum` 20; `GunCalibres`; the Reloading Press steps between them), barrel length and wall are dims, so *short/medium/long* are read off the length, never chosen; the action is a `slide` (self-loading, automatic) or a `bolt` (bolt, lever, pump, break, self-loading, automatic), the feed an optional cylinder, magazine tube or belt feed (none is a box, or the barrels of a break action), a sight (reflex, scope; none is iron), and muzzle device, vent and shroud as optional parts. Eight patterns (action times feed) and nine optional parts. An action the feed does not suit, a second barrel off a break action or two devices in one muzzle slot is a design the bake refuses, so the table makes no gun of it.
3. **Everywhere.** `GunItem` is a `ModularItem` (named by its designation, tinted by its parts); `GunShots`, `GunHud`, `GunClient`, `GunTooltip` (plus `ModularTooltip`), `GunAmmo`, `FCommands` and JEI read `GunSpec.of(stack)`; the Blueprint Table draws a gun (its rows scroll, 7 sample slots).
4. **The Gun Table** is `ToolTableMenu`, `ToolTableBlockEntity`, `ToolTableBlock` and `ToolTableScreen` with a `gun` flag (`GunTableMenu`, `GunTableBlockEntity`, `GunTableBlock` are thin): sixteen part slots in a 4x4 grid, a round slot, the types filtered by `ToolTableAssembly.ofKind`, and the spec sheet printed in the left column from the output gun baked against the round.
5. **Hooks** on `ModularTrait`: `gunBaked` (pure, in the bake), `gunAimed` (per shot, both sides), `gunFired`, `gunReloaded`, `gunRoundLanded`, `gunReloadMult`; `ModularTraits.allOf` collects every trait (a variant's `trait` may list several ids, comma separated) from every part and from each fitted `GunOption` (`trait`, `params`). A fitting is a tip-like optional part that may add a trait, change figures (blast, heat, shed, gas cut, relief, bleed, eddy, mass) or both; it never changes a round figure. **Traits built:** hammer, manual cycle, select fire, double barrel (sneak-pull fires both), closed bolt (gates cook-off), blowback (kick and bore wear past a pressure), bolt lockup (rating and spread), gas operated (cycle, heat, mass), braced (`gun_braced`), open bolt (no cook-off, a poor first shot), belt fed (reload time), fan (flat costs). **Not built:** off hand.
6. **Deleted:** `GunDesign`, `GunAssembly`, `ModularRecipe` (and its recipe and serializer), the Gun Stand (block, entity, menu, screen, payload, lang, model, loot), the gun components `GUN_DESIGN`, `GUN_FIGURES`, `BARREL_GEOMETRY`. `GunGameTests` was cut to the tests that still describe the gun (figures from parts, the table reading them back, calibre classes, legality, fittings, traits and designations, cook-off, cycle, a shot hitting); the rest (blends, shot, feeds, glass, the press, commands) tested things that were not moved and are not ported.

**Differences from the proposal in 8.4, and why.** Fittings, the sight, the second barrel, the winding and the wrap are optional *parts* (a role each), because a role holds one part item. The action's kind is a variant of the `slide` or `bolt` part, so the gun does not gain one item per action. A *single-shot* feed is gone (a bolt or lever gun with no feed takes a box). Barrel *bands* are not variants: with `dims` they are the length the lathe left, so a band is a word read off it. A *designation* is a data list read by `GunDesignation`, for example a handgun on a cylinder is a revolver (by the feed, not the `fan` trait, so it holds if a pack removes fanning).

## 9. Prior art — taken and rejected

**Taken:**
- Parts on the stack and figures baked and cached on change — Silent Gear
  `GearConstructionData`/`GearData.recalculateGearData`, Tinkers `ToolStack.rebuildStats`; the
  gun already does it.
- Types as data with a parent chain — Tinkers tool definitions, Silent Gear `GearType`.
- Harvest by block tag — Silent Gear `GearDiggerTool`.
- Broken-not-destroyed — Tinkers.
- Stepwise degradation as it wears — Silent Gear `GearHelper`.
- Integrity budget (made physical, §2.2) and exclusive choice groups (§3) — Tetra, design only.
- One sprite per part tinted by material (technique, not assets) — Tinkers `ToolModel`.
- **Screen code to adapt** (both MIT): Tinkers `tables/client/inventory/module/InfoPanelScreen`
  (scrolling stat panel; depends on Mantle — port the panel, licence of Mantle unchecked) and
  `library/tools/helper/TooltipBuilder`; Silent Gear `client/tooltip/GearComponentTooltips`
  (a stat shown with *why*), `TooltipHandler`, the material book screens. Never their textures.

**Rejected, and why:**
- Hand-authored stats per material per part (both) — figures are derived here.
- Silent Gear's random **grades** and `rarity` stat — luck is not a property.
- Silent Gear's hidden 0.1–2.0 **synergy** scalar — a continuous knob nobody can see.
- Tinkers' **modifier catalogue** (sharpness bought with quartz, diamond reinforcement) — flat
  bonuses with no physical cause. Its hook architecture is fine; its contents are not.
- Tinkers' `percent` handle bonuses and continuous per-part `scale`.
- Tinkers' Part Builder (pattern + "material value") — parts come from Feedback's own forming,
  casting and machining.
- Tinkers' repair keyed on (tool id, material id) — identity.
- Per-use wear with no misuse concept (both) — kept only as the consumable exception.

**Truly Modular / Modular-Item-API (PolyForm Shield 1.0.0, design only, 2026-10-20).** Licence
bars adapting anything; nothing was copied, no key names or lang strings either. *Taken (ideas):*
a before/after figure comparison when swapping a part, and a stat shown with its cause.
*Rejected:* authored stat expressions per module (`[material.durability]*0.25`); the synergy rule
engine (hidden bonus, no physical cause); an unbounded module tree (a design becomes a graph, and
the choices stop being discrete); free colour/cosmetic knobs; item-id converters (identity);
importing foreign mods' tiers (a tool-stat scanner was declined, the mod is authoritative).

## 10. Build order

1. Core: design, part, bake, figures component, tooltip adjectives. Pickaxe first, end to end.
2. The other four vanilla tools; vanilla conversion (§7) — vanilla tools cannot be deleted
   until replacements exist.
3. Sword, mace (with heavy core), trident, shield.
4. Bow; crossbow on the blueprint route.
5. Armour.
6. Instruments (§6).
7. Custom types.
8. Guns migrated onto the core. **Started 2026-10-20** (user ruling: the core has been played).
   Staged; see section 8 for the mapping and section 12 for what is built.

## 11. Decided, and still open

**Decided 2026-10-16 (overnight, delegated):**
- Repair: by hand at the Tool Table's tool slot, and swapping a part there too (§2.3, §4.2).
- Forming deviation: minimal, built = target (§4).
- Shears modular; fishing rod, flint and steel, brush vanilla (§5).
- Heavy core: figures above (§5.1).
- Custom types: mattock, sickle, cleaver, spear, maul (§5).
- Enchantability: **one material figure**, `enchantability`, read from the wearing part -- the
  head, the blade, the plate. Vanilla scale (gold 22, iron 14, stone 5); absent reads as poor.
- Assembly is by pattern, as vanilla lays its recipes out (§12).

**Open:**
- **[OPEN]** Section (flat / ribbed) and a part that carries an as-built shape (§3, §4).
- Hand-hammered parts record work done against work required as built geometry (built 2026-10-21, §4.1); the Press and grinding do not yet (§4).
- **[OPEN]** Pry bar and prospector's pick (§5).
- **[OPEN]** A crossbow's draw time and a lining's heat: the vanilla classes read fixed
  constants for these (§12 says which). A thrown trident's damage is not one any more: it is the
  baked damage (§12, *thrown weapons*).

## 12. Build status

Package `modular/` (the system the code is named for, beside `weapon/` and `vehicle/`); data in
`data/feedback/modular_type/`; registry in `registry/FModular`.

**Built (2026-10-21): the Tool Table** (§4.2): `modular/ToolTableBlock`, `ToolTableBlockEntity`, `ToolTableMenu`, `ToolTableShaping`, `ToolTableAssembly`, `ModularRework`; screen `client/ToolTableScreen` (which holds the forging rhythm screen); `net/ForgeBeginPayload`, `ForgeFinishPayload` re-keyed to the open container. Removed: `ModularPartRecipe`, `client/ForgeScreen`, the hand hammer's `use`, the modular branch of `ModularRecipe` (now the gun's alone), the *Tool assembly* and *Shaping a part* JEI cards. **Status below was written for the grid recipe and is corrected where it said so;** each bullet says "Tool Table" where it now applies.

**Built (2026-10-21): material perks** (§2.1a): `Material.Perk` and the `perks` field, `modular/MaterialPerks` (damage, ignite, echo, and the blaze rod's hand burn through `core/hazard`), a tooltip line per perk, `silver` material and `silver_ingot`. Melee only: a thrown weapon does not yet carry them. Unplayed.

**Built (2026-10-16, steps 1 and 2 of §10).**

- **Assembly is by roles and counts.** Each `modular_type` file lists roles (a letter, what forms of material
  fit, the volume of one cell) and patterns of letters. Since 2026-10-21 (§4.2) a pattern is only *how many
  of each role there are*: the Tool Table reads the roles the parts fill and the count of each, and
  where the letters sit is only how the Blueprint Table draws it. The count of a raw role is the part's
  size: two sticks are a longer handle than one. Parts are found by what the item is
  *made of* (`Material.find`), never by id, so wooden, stone, gold, glass and a datapack's own
  materials all work with no edit. Types are told apart by the parts, and a load-time check
  (`ToolTableAssembly.audit`) refuses two that accept the same ones.
- **The Tool Table's tool slot repairs and swaps** (§2.3, §4.2), and keeps whatever the item carried
  (enchantments, name).
- **The bake** (`ModularBake`, constants in `ModularTuning`): mass from volume and density;
  inertia and the moment the arm holds out set swing speed; a blow carries the striking mass at
  the tip's speed; bite and durability both come from the edge's competence,
  `sqrt(hardness x strength)`; the joint carries the head's leverage on handle and binding. Calibrated
  so that canonical vanilla designs land near vanilla: an iron pickaxe digs about 6 and lasts about
  250, a diamond one about 7 and 1600. **Damage is fitted to diamond-tipped steel (2026-10-21):** `DAMAGE_K` was 1.4, fitted to nothing, so an iron sword came out at 4.3 (vanilla 6) and a netherite one at 5.8 (8). The yardstick is what vanilla diamond *converts to* (a steel head with diamond set in it, `CONVERT_STUDS`), never a whole diamond, which is brittle and breaks fast; at 1.8 that sword lands 6.7 (7), pick 5.4 (5), shovel 5.3 (5.5), and the iron sword 5.2 and netherite 7.2 follow from their materials with nothing written for them. Dig keeps its own constant (`DIG_K` 2, fitted to the iron pick): fitting both to one rate would have slowed the iron pick to 5.4. The tempting fix was a per-item or per-material bump; the ratios between materials were already right, only the scale was off. The converted axe overshoots (10.5 against 9) on the axe's strike of 1.6; vanilla's diamond axe is no better than its iron one. Attack *speed* is untouched: an iron sword swings 2.2 a second against vanilla's 1.6. **Stone is the anomaly:** its table hardness (30) is the
  hammer's, not an edge's, so a stone head passes the iron class and digs gold and diamond ore. It
  chips doing it (toughness 6), which is the physical answer; vanilla's stone tools do not.
- **Vanilla's own components are written from the figures**: `TOOL` (the tags it digs, at its
  speed, with the three tier tags denied by edge hardness: 3, 6 and 40) and the attribute
  modifiers (damage, swing speed, reach), so combat and mining need no further code. Durability is
  `MAX_DAMAGE`, enchantability a per-stack override.
- **Misuse costs extra and says why** (`ModularStacks.misuse`): an edge on a block that strains it
  past twice its toughness chips (keen chips sooner), and a head heavier than its joint carries
  works loose a little each block. A broken item stops one point short and acts as an empty hand;
  figures drop at 50, 75 and 90 percent worn.
- **Tooltip is adjectives** (head-heavy, keen, brittle, loose-jointed, worn). Exact figures wait on
  the instruments (§6).
- **Vanilla replaced** for pickaxe, axe, shovel and hoe: recipes off (`neoforge:false`, the
  furnace's pattern; wooden and stone pickaxe, axe and shovel are the bootstrap exception, §7)
  and `ModularConvert` makes any vanilla stack modular -- a global loot modifier,
  a mob's gear as it joins the level, villager and wanderer offers, and a player's pockets once a
  second. The canonical design is read off the material table (iron's `pickaxe` form is
  `minecraft:iron_pickaxe`): an iron head on the type's default handle. The converted item keeps its
  name, enchantments and how worn it was.

**Built (steps 3-5).**
- **Sword** (blade, optional guard, grip, optional pommel, which is a counterweight: it lowers the moment the arm holds out), **mace** (head `heavy_core` or a metal block over a haft; the smash is vanilla's fall damage scaled by the item's mass over a heavy core on a full haft, so an iron-block mace is a weak one, and a heavy head on a wood haft works its joint loose), **trident** (tines and shaft; thrown, its blow is the baked damage, §12 *thrown weapons*), **shield** (face, rim, optional grip; stops a blow up to `6 + the face's competence` and only that much of a harder one, via `LivingShieldBlockEvent`; the thermal face of the approved shield idea is not built).
- **Heavy core** (`material/heavy_core.json`, §5.1) with every metal form type excluded, so nothing can work it, and `breeze_rod` (the mace's canonical haft, strong and light).
- **Bow and crossbow**, by pattern like everything else. Limbs (or prod) stiffer than the string holds make the arrow faster and the draw slower; a string weaker than half the limbs' strength is the limit and is strained (misuse, a chance of a point of wear per shot). Wool's `yield_strength` was added for this. The crossbow is assembled at the Tool Table from a prod, string, stock and lock, **with a crossbow blueprint in the plan slot** (see *Blueprint Table* below). **[OPEN]** a charge time: vanilla's is a constant that cannot be overridden without a mixin.
- **Shears** (two blades, optional pivot). Fishing rod, flint and steel and brush stay vanilla.
- **Armour** (plate, optional fibrous lining), one item per slot. Points are the piece's maximum times `s / (s + 2.87)` for the plate's competence `s` (iron near vanilla's 6 for a chestplate, soft gold well under); armour toughness from the last fifth of that share; knockback resistance from density above 8 (mass is hard to push); the wearer slows by the mass over 4 kg. Mail (iron `chain`) covers three quarters as much as plate. The one worn-armour sprite sheet is a dyeable grey tinted by the plate. **[OPEN]** a lining keeping heat from the wearer: it adds a little protection only.
- Vanilla recipes off for every type built, including netherite smithing; conversion covers all of them.

**Built (step 6): the instruments.** A **balance scale** (`Quantity.MASS`, to 0.1 Mu) and a **scratch file set** (`Quantity.HARDNESS`), both carried like the calipers, which are left to dimensions and condition. With one in the pack, a modular item's tooltip gains the figure under its adjectives, signed by the instrument. The files are `ModularTuning.SCRATCH_FILES` (0.5 to 80), and the reading is the rung the edge falls between -- "scratched by file 4, not by file 3" -- because a file set cannot stand behind a finer number. **Guns on the core: see 8.4 "Built 2026-10-21".** The 2026-10-20 migration (shared part items, `ModularRecipe`, `GunDesign` as the design) was reversed the next day; the part items it made (`FModular.PARTS`, `MadeOf`, `Material.stack`/`ingredient`, the form steps) stand.

**Not built:** the impact pendulum (toughness) and the strike post (what a weapon delivers per blow), which are blocks and wait.

**Built (2026-10-20): cause text and before/after comparison.**
- **Cause.** Holding Shift on a modular item's tooltip appends *from Iron head, Wood handle* to each adjective's gloss and to every figure line an instrument reads (mass, hardness, and every figure at the truth tier), via `Readout.gloss`. `ModularCause` maps a figure to the parts it is down to, mirroring `ModularBake`: the wearing edge (and any tip or studs) for hardness, toughness, bite, durability and armour; the heaviest part for mass, balance, swing and impact; the rods for reach; the weakest of handle and edge, plus the binding, for the joint; string and limbs for a bow's draw. Found by role and by the material's own figures, never by id. The edge's grind (keen, blunt) is a choice, not a part, so it carries no cause.
- **Comparison.** `client/FigureCompare` lists the figures that changed between two sets, grouped raised and lowered, the largest relative change first, each printed at the truth tier with the share it moved. It judges nothing better or worse (a lighter piece swings quicker and hits softer). Two places use it: the Blueprint Table keeps the first drawing of the type or opened blueprint and a hover line under the figures lists what later swaps changed (paper, so free); and the tooltip of the Tool Table's result over a tool in its tool slot lists what the work changes, for a player at the truth tier (an item's figures cost an instrument). **[OPEN]** the table comparison needs the truth tier because no instrument yet reads every figure; once more exist it should show the figures each one reads.

**Built (step 7): the custom types**, each a `modular_type` file and a lang line: **mattock** (superseded 2026-10-20 by the twin-headed type below), **cleaver** (parent `axe`: four heads on one grip, twice the axe's strike), **spear** (one head over three handles, so it reaches further), **maul** (six heads on a handle; splits wood, and a blow slows its target for as long as the impact is worth) and **sickle** (cutting a crop or leaf takes its neighbours on the level too, one point of wear per four taken). The cleaver proves the parent chain; the spear proves a longer handle is only a longer pattern.

**Built (2026-10-20): the Blueprint Table** (`modular/BlueprintTableBlock`, `BlueprintTableMenu`, `ModularBlueprintItem`; screen `client/BlueprintTableScreen`; `net/ModularBlueprintPayload`). The Gun Stand's shape for every modular type, under the author's ruling: *a blueprint is a plan, so it shows the numbers for a tool before you make it, and is required to craft only what is complicated.*
- **What is drawn** (rebuilt 2026-10-21 onto `ToolDesigns`, §4.2): a type, one of its forms (which roles and how many of each -- §3's length choice is the form), the optional parts as toggles (a tip, a binding), and for each role a material (a sample item in its slot names it) and, where the part has shapes, which shape: a laid or forged variant, or for a twin's head which kind of head, each shown with its trait and shape by `ModularModifiers`. Heat and flaws are not drawn: a blueprint is paper and flaws are what forging adds. The screen lists roles off the type's file, so a role added to a type appears with no change; every choice is `ToolDesigns`' own answer, by property, never id.
- **What it shows** is `ModularBake.bake` run on that drawing, new: the design's figures, exact, because requirements and specs are free and a blueprint is paper (philosophy 8, §6). It is never a reading of an item. The item built is baked from the parts actually put in, and what it *has* still costs an instrument.
- **Printing** turns paper (`blueprint_paper` tag) into a Blueprint carrying the design and its figures; the server runs `ToolDesigns.predict` on its own tables, which puts the design's parts on the Tool Table and reads them back, so it refuses exactly the designs the table would (a part set that is none of the type's, a shape a role does not take, more than the six slots hold). A blueprint put back on the table opens for drawing again. The table keeps nothing; the paper comes back when it is closed.
- **Which types need one**: a type file's `blueprint: true`, and only the crossbow says it. The crossbow is the one type with a *mechanism* -- a lock that holds a drawn prod and lets it go -- whose parts have to be made to fit each other (§5's table: "the lock is a machined part like a gun's action"). Everything else is a material on a stick, plate or string, where the parts are the design (§1's Tinkers route). The shield, bow and armour were weighed and left out for the same reason: nothing in them works against anything else. A blueprint needed is any blueprint of that type; the parts need not match it -- the plan is the gate, the parts are the item. The table keeps the blueprint, as the gun's craft does; repair and swapping never need one.

**Built (2026-10-20): the tip.** A role with `"set": true` is a hard piece set into the wearing part, not a part of its own: `T` in nine types (pickaxe, axe, shovel, hoe, mattock, cleaver, sickle, spear, sword), one gem, shard or nugget, in a spare slot of the Tool Table with the other parts (or added to a finished tool). `ModularBake.zone` bakes the wearing zone: **hardness is the tip's** (it gates what the edge can dig), **bite is `sqrt(tip hardness x the body's strength)`** -- the tip cuts, the body holds it -- and **toughness and wear are 70% body, 30% tip** (`ModularTuning.TIP_SHARE`). A tip no harder than its body does nothing (a gold tip on iron is a pad). Mass is small and real. So a steel pick with a diamond tip bites at 38 where steel alone is 15 and diamond whole is 45, chips like steel (toughness 43, not 5) and lasts about 1.6x as long as steel: the strength of a hard material without its brittleness and without much of it (diamond saw blades, carbide inserts). **Which types take one, and which do not:** edged tools and blades do, because an edge is where hardness pays; **maul (so its mace head), bow, crossbow, shield and shears do not** -- a mace's point is mass, a maul's is a blunt blow, a shield's face is struck whole, and none of them is an edge a tip would change. **Armour takes none** (studs, above, are its analogue): an armour piece is a plate, and nothing sits at its "edge". Swapping one gem into a tipped tool replaces the tip, and a tool made without one can be given one at the table (it was once ruled out; the table has the hammer).

**Studs: the tip's analogue on armour** (the same `set` role, named `studs`, on helmet, chestplate, leggings and boots). The user's ruling is that armour takes no *tip* -- there is no edge on a plate for one to sit at (Silent Gear gives armour a tip; that is the thing not copied). What armour has is hardness spread over a surface, so gems are set into the plate as studs (brigandine; diamond studs in steel plate): `T` or `TT` under the plate, one gem per cell, each covering a tenth of the piece (`STUD_SHARE`, to half, `STUD_MAX`). The covered share carries the studs' hardness, toughness and wear and the rest stays plate, in one linear mix: plate protection reads `sqrt(mixed hardness x plate strength)`, so four diamond studs in a steel chestplate lift it from steel's 6.7 points to near 7.2 and keep steel's toughness, where a whole diamond plate is 7.5 points and glass. Studs add a little mass and nothing else.

**Whole brittle material is bad, not forbidden.** Diamond is not a metal and cannot be hammered into a blade, but a diamond head can still be made (the gem is stock like any other, `ModularBake.fracture`): its durability reads `competence x min(1, toughness / (hardness x 0.2))^2`, so a material tougher than a fifth of its hardness is unaffected (stone, iron, cast iron, hardened steel all keep every point) and diamond keeps one sixteenth. It bites superbly, shatters in use and is tagged `brittle`. Precision and brittleness, never a hard gate (philosophy 7). The square: a crack runs, so a little brittleness costs little and a lot costs nearly everything. A tip does not pay it, because a tip is not whole -- it is supported.

**Built (2026-10-20): diamond converts to a tipped steel tool.** `ModularConvert` reads the material's *kind*, not its id: a vanilla item whose material is a **crystal** (diamond is the only one with tool and armour forms) becomes the type's body in `ModularTuning.CRYSTAL_BODY` (steel) with the crystal set in it -- one tip on a tool, four studs (`CONVERT_STUDS`) on an armour piece -- and is named for it ("Diamond-tipped Steel Pickaxe", "Diamond-studded Steel Chestplate"; every other conversion keeps its vanilla name). The one function is the route for chest and mob loot, mob gear, villager trades and pockets, so all of them agree. Enchantments and wear carry over as before. **Why steel, and why not a diamond head as a conversion:** a pack author adding a crystal with tool forms gets the same conversion with no edit; the alternative, a whole-diamond head, would be the brittle-and-bad design the section below describes, and a player who never opens a bench would find every diamond pick they have ever been dropped is junk. **Cost, accepted:** the converted pick lasts about 860 where vanilla's lasts 1561, and bites at 38 against a whole diamond's 45 -- a diamond tool is now the work of a forge and a gem, not the gem alone, and it is no longer the best tool in the game (netherite is).

**Built (2026-10-20): coatings are the plating that already exists.** A modular tool or armour piece whose wearing part is a metal (`ModularStacks.platable`) can hang on a side of the electrolytic cell like any metal piece (`hangWork`, which now takes `hangable` -- the anode's metal *or* a platable modular item) and comes out carrying the same `process/Plating` component an ingot does -- same window (porous, sound, spalled), same burn current, same ledger. No schema was added and no parallel coating exists: `ModularBake.bake` takes the stack's `Plating` and reads the *coat metal's own material file*:
- **corrosion** (`Fig.CORROSION`, new): `1 - (1 - body resistance) x (1 - coat resistance x hold)`, `hold` being how closed the coat is (thickness over the porous line) halved if it grew burnt or mixed (`COAT_FAULTY_HOLD`). The consumer is `ModularStacks.rust`: a tool mining while wet (`isInWaterOrRain`) gives up an extra point with a chance `CORRODE_CHANCE x (1 - corrosion)` -- water and misuse, never time, so a dry shelf rusts nothing and gold, stone and a sound copper coat barely at all. Armour does the same on a *blow taken* while the wearer is wet (`ModularArmor.damageItem` calls the same `rust`, same chance, same figure): water and a hit, never time, so plate on a dry shelf or worn dry does not rust. **[OPEN]** the chance is the tool's, not tuned for armour, which takes more hits than a tool breaks blocks.
- **surface hardness**, **armour only**: a sound coat of a harder metal is a skin over the whole plate, mixed in at a tenth (`COAT_SURFACE_SHARE`) by the same `mix` the studs use. On a tool the **edge is ground bare**, so a coat protects the flats and leaves bite and hardness alone -- which is also why a plated blade is not a way round the tip.
- **friction** has one small reader, **boots only**: a sound coat's friction against the plate's own adds to (or, if slicker, takes from) knockback resistance, `hold x COAT_GRIP_PER_FRICTION x (coat - plate)`, inside the same `KNOCKBACK_MAX` cap. Why boots: they are the only piece that meets the ground, and being shoved is a foot slipping. A tool's grip is its handle's, a string is not plated, and friction on a chestplate decides nothing, so nothing else reads it and no field was added. Honest limit: metals sit at 0.5 to 0.6, so today the effect is a hundredth or so; a coat of a stickier or slicker material would matter, and the size (`COAT_GRIP_PER_FRICTION`) is **[OPEN]**. Cost of the choice: the grippy coat is not always the corrosion-proof or hard one.
Re-forging the wearing part removes the coat (the part it was on is gone). The cell sizes a modular piece by its baked mass over its wearing metal's density, in ingots of `CELL_MB` (144 mB; `ModularStacks.volumeMb`), so a chestplate burns at its larger surface's current, not an ingot's. Handle and lining are counted as the metal (close enough to size a cathode by).

**Built (2026-10-21): JEI, as the Tool Table's cards** (`compat/jei/ToolTableToolCategory`, `ToolTablePartCategory`; they replaced *Tool assembly* and *Shaping a part*, and the Tool Table is their catalyst). *Tool cards*: one per form of each type -- a slot per role (with its count, and the optional parts marked) the form has, cycling every part or stock that fits it (`Role.accepts`, so a pack's material appears with no edit), with no grid: the table has six slots and the roles sit in any, so a card says what goes in and not where (which also ends the spear's four-tall grid). The options that are not part of the pattern are said in words: the set-in tip or studs, a binding, regrinding at a grindstone, plating. *Part cards*: one per shape in the table's list -- the standard, each laid variant (its cell diagram drawn) and each forged variant (it says it is forged and shows no rhythm, since the pattern is played at the table), the stock in the number of cells, a hand hammer, the part out cycling beside the stock. Both are built from the client's copies of the type and material tables and republish when either arrives (`ModularTypes.onChanged`). **Rejected:** a card per material per part (hundreds of near-identical entries; the material is a stack component the cycling slot already shows) and baked figures on the cards (a pattern has no materials until the player picks them; the Blueprint Table is where a design's figures are free paper).

**Not built:** the impact pendulum and strike post, and the other `[OPEN]`s of §11.

**Built (2026-10-20): twin-headed tools** (§5.3). One type, `twin` (`modular_type/twin.json`), with two head roles `H` and `J`, each taking any head kind (`pickaxe_head`, `axe_head`, `shovel_head`, `hoe_head`) and its own material; pattern `HSJ /  S `. **A head part records its kind** (`Part.kind`, the part item's path), and **what a kind digs and how it strikes is read off the single-head type it belongs to** (`ModularHeads`: the pickaxe's wearing role is made of `pickaxe_head`, so that head digs the pickaxe's tag at the pickaxe's `strike`). No table per pair: any two heads make a tool, and the pair's name is only a label (`combos` in the type file: pick+axe is a *mattock*, axe+axe a *double-bit axe*; an unnamed pair reads "Pick-Hoe Tool"). The mattock's flat `0.5` is gone: both heads add to the swing's moment, so the tool swings slower and hits softer with no number written for it, and the off head loads the joint too (`joint` sums every striking head's mass and takes the weakest of them). **Per head:** dig speed, edge hardness and edge toughness (`Fig.*_OFF` for the second); the tool digs the union of both heads' tags, the better speed where both dig one, what drops is gated by the head that digs stone, and a block's chip strain is read against the toughness of the head that digs it. The blow that lands is the better head's. **Decided:** the inertia tax applies to dig speed too (a heavier swing is a slower one). **Left open:** one durability pool for both edges (their wear litres summed), so a head not in use still wears down the shared bar; a tip sets on the first head only.

**Built (2026-10-20): part variants** (spec 3). A shaped part may list `variants` in its `part` block: each is another layout of the same item's cells (`ModularType.Variant`: `name`, `shape`, `reach` in metres from the hand, `section`, the thickness against standard). The standard shape is the unnamed first. The Tool Table's shape list makes whichever shape is chosen and records it on the part (`MadeOf.variant`, then `Part.variant`); **the geometry is read from the type at bake time, never stored on the part**, so retuning a variant retunes every part already made. What a variant does is derived: the cells are the size (more cells, more mass, as everywhere), **a thin edge bites by the inverse square root of its section and a thick one resists chipping in proportion** (`ModularBake.section`, applied to the wearing part; the second head of a twin too), and `reach` moves the part from the hand, so it changes inertia, balance, reach and joint load with no stat written. **Built:** sword blade (arming, *broad* L of four cells at 1.3 thick and 5 cm shorter, *narrow* diagonal of two cells at 0.7 thick and 15 cm longer: longer, bites, chips), pickaxe head (*heavy* T of four), axe head (*bearded* of four). **Left open** (a variant's shape no longer has to be unique across parts: the list names it): the Blueprint Table draws standard shapes only; raw-stock roles (guard, pommel) have none; no leftover swarf. **Revised the same day (volume conserved, §4.1):** `section` no longer scales mass, so the laid *narrow* blade is no lighter than the arming one, only longer, thinner and sharper.

**Built (2026-10-21): variants on raw-stock roles (compile only).** The tip, binding, sword guard, sword pommel and shield rim are shaped parts now, so they take `variants` like a head. Nothing new is stored; what a variant does is read at bake time, mostly through channels that already existed: `reach`, `centre` and `mass` move a guard or pommel in the swing like any part not on the rod, and **a tip's `section` multiplies the wearing part's** (a needle bites more and chips sooner, a capped tip the reverse). Two named numbers in a variant's `params` are read by the bake: `joint_mult` on a binding (its strength share in the joint) and `capacity_mult` on a shield rim (multiplies the face's capacity), and `digs` on a pickaxe head set to 0 leaves the item with no `TOOL` component (the **war pick**: a forged thin spike, strike 1.4, digs nothing, so it takes the hand's rules for every block). **A trait may now come from any part's variant**, the primary part's first (`ModularTraits.traitVariant`): the sword's *crossguard* carries `blocking`, which raises the blade with the blocking use animation, answers vanilla's `SHIELD_BLOCK` test through `ModularSword.canPerformAction`, and in `onShieldBlock` lets through `1 - block_mult` of a blow from the front for `wear` points of wear. `ModularEvents.onShieldBlock` now acts only for a `ModularShield`, since the capacity figure is a shield's. **Trap:** the sword-block is not a free upgrade of a guard; it costs three pieces of stock and the bar's weight in the swing, and it is no shield (half of a blow comes through, nothing is held against a blow from behind). **Not built:** a rim variant has no cost but stock and mass; the heavier shield does not slow its bearer.

**Built (2026-10-21): one blueprint item, one build command.** `feedback:modular_blueprint` carries a `ModularDesign` of any type, a gun included; `/feedback build` hands out the finished item of whatever blueprint is held and `/feedback build parts` every part it lists (`FCommands.toolFor`, `toolPartsFor`).

**Built (2026-10-21): archetype variants** (author: *sword / rapier / katana / machete*, not *drawn / upset*). A `Variant` gained `strike`, a damage multiplier for the shape (geometry, as the type's own is; it does not touch what is dug); the forged variants `drawn` and `upset` are replaced and the laid *broad* and *narrow* renamed. **Sword blade:** *claymore* (laid L of four cells, long, strike 1.2), *dagger* (laid diagonal, short, strike 0.8), and forged *rapier* (reach +0.25, section 0.6, strike 0.6, an even run of taps), *katana* (+0.1, 0.8, 1.15, a fold: `hh..t.t.hh.t`) and *machete* (-0.05, 1.25, 1.35, slow heavies). **Axe head:** *bearded* (laid), *hatchet* and *felling axe* (forged). **Pickaxe head:** *heavy* (laid), *prospector's* (thin, long) and *miner's* (thick). The item is named for its main part's variant (*Iron Rapier*, `modular.feedback.archetype.*`); the part reads *Iron Sword Blade (rapier)*. The same data on any shaped part makes any other tool's archetypes. **Left open:** the katana's edge harder than its spine (a hardened-steel pairing, a later feature); a block-guard (1.8-style) as a guard-role option, which needs guard variants first; per-variant dig tags (a war pick that digs nothing, a machete that clears crops -- a sword already digs vegetation).

**Revised (2026-10-21): swing speed.** Playtest: a perfectly forged copper rapier swung at 1.29 against a machete's 1.55, because a longer blade has more inertia and the swing rate fell only with the fourth root of it. Three changes. **The swing rate now falls with the square root of the inertia** (`SWING_EXPONENT` 0.5, `SWING_K` 1.6; a constant torque turns a pendulum's period with it), so a light blade is far quicker and a heavy head slower, while a sword still swings as it did (a pickaxe drops from about 1.25 to 1.0). **A variant's tip and its mass are separate** (`reach` moves the tip, `centre` moves the centre of mass): a rapier reaches far with its mass kept at the hand. **A variant keeps a share of its stock** (`mass`): a blade ground thin loses the rest as swarf, not handed back yet. Copper blade swing rates now: dagger 2.7, rapier 2.3, cutlass 1.7, katana 1.5, arming sword 1.45, machete 1.3, claymore 0.9. A *cutlass* joined the sword blade (forged: a light, slightly long, single-edged slasher, strike 0.95). Attack speed is swings a second, so higher is quicker.

**Revised (2026-10-21): tips and bindings are added, not laid; the table took both; and they became items of their own (§4.2).** A tip (`set` role) or a binding is in no pattern of a non-armour type (the twin, spear, pick and the rest keep one pattern each; the sword keeps its three guard/pommel patterns). At the Tool Table both go in a slot with the other parts, because `ModularType.assemblies` offers every pattern with and without a role that no pattern lays (a tip, a binding); or a Tip or Binding part is added to a finished tool in the tool slot (`ModularRework`, the *add* branch). Armour studs are counted, so stay in their patterns. The Blueprint Table draws both (§4.2, one model); tips and bindings are items of their own (`feedback:tool_tip`, `feedback:tool_binding`).

**Built (2026-10-21): the trait system and eleven traits** (spec 5.4). `ModularTrait` (every hook defaulting to nothing: attack, kill, block break, use, use-held, release, client), `ModularTraits` (the registry and the one place events are subscribed; the trait is the main part's variant's `trait` with its `params`, else the type's own `trait` with `trait_params`), a class per trait in `modular/trait/`, `ModularItem` and `ModularAxe` forwarding their use hooks. Every number is data. **Spear (`spear_charge`):** the jab cannot hit within `min_range` (2) of the wielder; the charge (hold use, the vanilla spear pose) hits each living entity once per charge when the relative speed along the look line, tracked from per-tick positions, is at least `min_speed` (4.6 blocks a second), for `damage_mult x strike x sqrt(head mass / ref_mass) x speed`; it knocks back from 5.1, dismounts in the engaged stage, never crits and ignores Strength and Weakness. Stages (engaged 40, tired 30, disengaged 30 ticks) each scale by `min(1, 0.5 / JOINT)` (floor 0.25), so a weak binding tires sooner; a hit costs `1 + speed / 6` wear. **Prospector's pick (`sounding`):** right click outlines ore (block tag `c:ores`, never an id) within 12 blocks through the rock for 6 seconds, to the wielder alone; a 200 tick cooldown and 4 wear; an instrument, it never mines faster. **Miner's pick (`vein`):** breaking an ore also breaks up to 8 connected blocks of the same ore by the normal break path, 1 extra wear each, never the last point, sneaking turns it off. **Swords:** *flurry* (+12% a stack on one target within 25 ticks, 5 stacks, reset by a miss, a new target or damage taken), *opening cut* (the first hit after 80 quiet ticks does 1.8x and bleeds for 1 a second over 4 seconds scaled by the edge's bite), *hack* (1.4x on the unarmoured, 0.8x on the armoured, clears leaves, crops and replaceable plants within 2 blocks at one wear per three), *overhead* (only a fully charged swing sweeps 0.6x to everything within 2.5 of the target, 2 wear; a half charge does 0.6x and no sweep), *backstab* (2x from behind or on an unaware mob, 1.3x more sneaking, 0.9x face to face), *flourish* (a kill readies the swing and grants Speed for 40 ticks; 1.2x with two or more hostiles within 3). **Unverified, all of it (compile only).** **Left open:** the spear's hitbox inflation (0.125) and attacking through non-solid blocks, and a custom first-person pose (the vanilla spear pose stands in); bleed damage credits no one; flurry, opening cut and the spear's no-crit rule carry their own event handlers; the sounding's range per handle length and tint per ore family.

**Built (2026-10-21): the axe traits** (`modular/trait/Timber`, `Throw`, `Hook`, `registry/FEntities`; the thrown entity is now `ThrownWeapon`, below). **Felling axe (`timber`):** breaking a log (`#minecraft:logs`) with the right tool fells the connected logs (26-neighbour flood fill, nearest first, leaves untouched), up to `cap` 24 within `max_radius` 12, each by the normal break path with one extra wear per log, stopping before the tool breaks; sneaking fells one. **Hatchet (`throw`):** hold use and release (at least 10 ticks charged) to throw it as `ThrownAxe`; the blow is the item's baked damage plus 0.35 per block flown, capped at 2.5x; 2 wear per throw; after its first hit it falls, and it lands as an ordinary dropped item, or flies back to the owner when `loyal` is above 0 (the brief's wording was the reverse; the agent chose this reading). **Bearded axe (`hook`):** a blow on a blocked shield pulls the target 0.4 toward the wielder and, on a player, disables the shield for 160 ticks (a task a tick later, because vanilla's own axe disable lands after the damage event); on an unshielded target it deals 0.9 of its damage. **Unverified, compile only:** flight, landing and loyal return, the pull direction, the shield cooldown on the used shield; a hatchet that falls out of the world is lost; the renderer is the vanilla thrown-item billboard.

**Built and played (2026-10-21): thrown weapons** (`modular/ThrownWeapon`, `Throwing`, `client/ThrownWeaponRenderer`; replaces `ThrownAxe` and vanilla's `ThrownTrident` for modular tridents). One entity for the hatchet and every trident-shaped weapon (javelin, harpoon, pitchfork), an `AbstractArrow` as Spartan Weaponry's is (design read, Apache-2.0; `THIRD-PARTY-LICENSES.md`). **Figures are derived, never a per-weapon table:** launch speed is `THROW_SPEED_K / sqrt(mass)` clamped, times *effort* (`THROW_EFFORT_FLOOR` at the first tick a throw is allowed, rising to 1 over `full_charge_ticks`); the blow is the baked damage times effort, growing `damage_per_block` a block to `max_mult`; a vanilla Loyalty enchantment or a variant's `return` is a return rate, scaled up for a light weapon. **A blow that fails to hurt does not spend it**: it glances off and keeps flying (it used to fall after any hit, even a blocked one). **Landing:** it sticks in a block and shakes, is picked up by touch by its thrower, and falls out as a dropped item after `THROW_STUCK_LIFE`; one with a return rate lets go four ticks after it sticks and flies back, steered like Loyalty (damped velocity, a pull to the eye, a caught-at-1.3-blocks), not in a straight line. **Wear:** once at the throw, and again by `ModularStacks.misuse` if it strikes a block harder than its edge takes. **Tumbling** (`tumble: 1`, the hatchet): it turns `THROW_SPIN_DEG` a tick, the blow is full head-first and `TUMBLE_FLOOR` side-on, so a fixed range lands it right and others do not (the range the player learns is the decision), and it drags in water; a javelin flies true. *Why a flag and not derived from balance:* a head-heavy balance figure would have said a javelin tumbles (its tines are the heavy end), and no run has checked the figures yet; `tumble` is a discrete choice a variant makes. **The harpoon is a rope** (`rope` blocks, `pull` blocks a tick, `water_mult`, `return`): the line is drawn from the thrower's hand; the harpoon cannot fly past it (it turns back); on a living thing it stays in it and reels it in (the target's velocity is set toward the thrower each tick, less for knockback resistance, more in water) until within `REEL_STOP_DISTANCE` or `REEL_MAX_TICKS`, then lets go and flies back. A miss flies back too: it always comes back (`return` 1.5 of Loyalty I, `RETURN_UNATTACHED_MULT` times as fast when it struck nothing (roped weapons only), and a roped weapon still falls under gravity on the way, so it arcs, and it collides with blocks (it is moved as a plain entity on the way back, which slides along a wall or the ground where the arrow's own tick would stick it, and drops if caught for `ROPE_RETURN_MAX_TICKS`); a Loyalty trident has no rope and defies gravity, as vanilla's does). *Why the old pull was weak:* it added one velocity once on impact, which is what knockback already is. **Renderer:** the item model laid along the flight, a tumbler turning, a stuck one shaking and frozen at the turn it stuck at. **Unverified, played and working (author, 2026-10-21); the throw, spin, tumble and reel figures stay `[OPEN]` placeholders.

**Revised (2026-10-21): the spear's charge is a held key, not item use.** Playtest: vanilla item use slows the wielder to 20% and drops the sprint the charge needs (a sprint is about 5.6 blocks a second, the charge wants 4.6 closing), and the vanilla spear pose held the spear behind the head like a trident. The client sends `SpearChargePayload` on each change of the use key and every 10 ticks while held (only with the spear in the main hand, no screen, not using another item). The server keeps the per-wielder charge, runs it from a `PlayerTickEvent`, and ends it on a missed heartbeat (25 ticks), death, logout, spectating, an item change or breakage, or the end of the disengaged stage; a spent charge cannot restart until the key is let go (`SPENT`). `SpearStagePayload` tells tracking players the stage so others see the pose. The pose is client-only: a `FEEDBACK_SPEAR` arm pose (an enum extension, `META-INF/enumextensions.json`, because `ArmPose.create` does not exist in this NeoForge) holds the spear out in front, shakes when tired and lowers when disengaged, with a matching first-person transform (`client/SpearClient`, `SpearPose`; the values are guesses). No Mixins. A right click on a door while the key is held also starts a charge, since the charge no longer depends on `use()`. **Unverified:** the enum extension loading, the pose in third person, the first-person offsets, timing under latency.

**Built (2026-10-21): modifiers** (`modular/ModularModifiers`). A modifier is anything that makes this item behave or read differently from the plain one, whatever its source: an **adjective** (derived from figures: head-heavy, keen, brittle, loose-jointed), the **trait** (the signature behaviour the main part's variant or the type names) or the **shape** (the variant the main part was made in). One list, in that order, is the only source: the tooltip's compact name line (gray, gold and yellow by kind), its Shift glossary, the blueprint paper and the Tool Table's part hover all read it. Shift gives one line per modifier: an adjective's line says which figure decides it, where the threshold is (printed at the truth tier) and which part it is down to; a trait's line is written by hand in lang (`modular.feedback.desc.trait.<id>`) with its numbers read from the active params; a shape's line is its difference from the standard part. Adding a modifier is one entry and its lang keys. The Tool Table's part hover now shows the trait (the spear head shows the charge). The old per-adjective name keys (`tooltip.feedback.modular.<adj>`) are `modular.feedback.modifier.adjective.*`.

**Built (2026-10-21): HUD readouts** (`client/HudReadouts`, `net/HudReadoutPayload`). The action bar is no longer the channel for modular and forging feedback. Server code calls `HudReadouts.send(player, text, ticks)`; the client keeps up to three live readouts and draws them as `GuiSkin.PANEL` cards at the ammo readout's anchor (right edge, `GunHud`'s), newest nearest it, older above; with a gun in hand the stack sits above the ammo card. A line with the same text as one showing refreshes it (so the Flurry count holds one card); a fourth drops the lowest priority, then the oldest; a card fades over its last half second and nothing moves. Converted: Flurry's stack count, the forging begin and result messages, the grindstone edge message and the Blueprint Table's print messages. **Left on the action bar** (not the same kind of message): instruments, the Programmer, data and belt links, machine and hazard warnings, gun and vehicle messages.

**Built (2026-10-21): archetypes for the rest of the tools** (played and working, author, 2026-10-21). The same variant-and-trait system as the sword, axe and pick now covers every shaped part; each archetype is a choice with a cost, every number is in its `params`, and the Tool Table, Blueprint Table, JEI and hover text pick them up from data and lang.

*Ground and crop tools.* Shovel: **scoop** (laid, `scoop`: digs a row three wide, +1 wear per extra block, slower swing), **trench** (forged, `trench`: digs one block deeper along the look or down/up past 50 degrees of pitch, +2 wear per extra block). Hoe: **kama** (laid, `kama`: tills two more blocks along the facing, each a tilling's wear, a 1 s cooldown), **rake** (forged, `rake`: pulls a crop's drops within 4 blocks to the wielder, 1 wear per stack, weak hit). Sickle: **scythe** (laid, `scythe`: also cuts the ring 2 out, a 5x5, 1 wear per three outer blocks, mobs take 70%), **billhook** (forged, `billhook`: a non-stacking bleed of 1 a second for 5 s, the blow at 80%), **reaping hook** (forged, `reaping`: replants from its own drops at 1 wear, cuts one plant at a time). All sneak-off. The hoe class runs the kama extras as extra `useOn` calls; the scythe's outer ring and the sickle sweep break by `level.destroyBlock` and so skip the break event.

*Heavy and thrust.* Maul: **sledge** (laid, `slam`: a fully charged swing at the top of a block throws the mace's smash -- dust, thud, shove within 3.5 -- through `Smash`, the ring the mace and sabatons share; spends the charge, 3 wear), **war hammer** (forged, `pierce`: ignores half of armour and toughness, slows the armoured target 4 s, a small head so little stagger). Cleaver (its axe inheritance kept): **butcher** (forged, `butcher`: x1.5 on animals and a second helping of food drops half the time, x0.85 on monsters), **bardiche** (laid, `line_cut`: the blow carries to the nearest mob in line behind at 0.7, the main blow 0.9). Trident: **javelin** (laid, `lunge`: x1.4 sprinting, x0.85 standing), **harpoon** (forged, `harpoon`: thrown on a rope, a hit reels the target in with the harpoon still in it, x1.8 in water, always flies back, weak melee), **pitchfork** (laid, `fork`: up to two more mobs in a cone at 0.7, the main blow 0.7). **Mace: a variant of the maul head** (see the merge note below). **Open:** the sledge does not mine stone faster (the maul digs axe-tagged blocks only);  **Renamed (2026-10-21):** the type once called *pike* is now *spear* (the newest game spear: jab, held charge), and the trident variant once called *spear* is now *javelin* (thrown, lunges at a run), so the name a player knows from the game is the weapon that behaves like it. *Lance* stays free for a mounted charge.

**Revised (2026-10-21): stock is volume, not item count.** A shaped part's stock used to be "as many items as the shape has cells". Each form now has a volume in millibuckets (`form_type/<form>.json` `mb`: ingot 144, nugget 16, block 1296, default 144; the heavy core is a form of 1296 with no generated items), a cell is one ingot (`ModularTuning.CELL_MB`), and `ToolTableShaping.check` needs the pile's volume to equal the shape's cells exactly. Nine ingots or one block fill a nine-cell shape; a block never fills a smaller one (it cannot be split). Volume is always millibuckets in code; screens say ingots. Part stock lists gained `block`. Repair counts stock by volume too. Why it was tempting to leave: item count was the simplest rule. It could not say that a heavy core or a block is nine ingots of stuff, which the mace needed.

*Armour.* Traits here are worn, not held: `ModularTrait` gained default hooks for hurt, fall, knockback, visibility, a per-tick player hook and a removal hook; `ModularTraits.worn(entity)` reads the four armour slots and **fires each trait id once, from the first piece that carries it** (head, chest, legs, feet), so four pieces never give four bonuses. A laid variant's cells set both mass and cover. Helmet: **open helm** (`open_face`: projectiles x1.3), **great helm** (`closed_visor`: projectiles x0.6, mobs see you x1.5), **sallet** (`neck_guard`: blows from behind x0.8). Chestplate: **brigandine** (`studded`: melee attackers take 1 per stud, at most 3, 1 wear per bite), **breastplate** (`rigid`: fall and explosion x0.7, the heaviest), **hauberk** (`mail`: bladed x0.75, maces x1.3). Leggings: **greaves** (`kick`: a sprinting wearer runs into a hostile mob in front and knocks it back, 0.3 hunger and 1 wear, a 30 tick cooldown), **cuisses** (`braced`: knockback x0.6, swim speed -15%). Boots: **sabatons** (`trample`: a fall of 3 or more blocks hurts hostiles within 1.5 for 1 per block fallen (12 at most) and a landing on wool, snow or leaves costs no fall damage, 1 wear), **riding boots** (`spurs`: starting a sprint gives Speed I for 60 ticks, once per 200). **Not built:** the open helm's wide vision, the hauberk's bleed. **Unconfirmed:** `Studded` takes the stud count as `design.part("T").count()`.

*Ranged and defensive.* The bow's limbs (`bow_limbs`, 3 cells) and the shield's face (`shield_face`, 6 cells) are now shaped parts, like the crossbow prod and the shield rim. Bow: **shortbow** (laid, `snap_shot`: draw x0.6, velocity x0.8), **longbow** (laid, `heavy_draw`: draw x1.5, velocity x1.3, wielder slowed 30% while drawn), **recurve** (forged, `recurve`: a full draw does +25% and pierces one mob, a part draw neither, draw x1.15), **hunting** (forged, `stalker`: a silent shot, x1.5 on a creature's head, velocity x0.85). Crossbow: **arbalest** (laid, `heavy_bolt`: damage x1.6, pierce 2, load x2.2, slowed while loading), **pistol** (laid, `snap_bolt`: load x0.45, velocity x0.7, damage x0.8; no one-handed rule), **repeater** (forged, `repeater`: loads three bolts and looses all with spread x5, load x1.3, damage x0.85). Shield: **buckler** (laid, `parry`: a blow in the 6 ticks after the 5 tick raise delay is stopped whole at no durability and the attacker knocked back; guards a 110 degree arc, 65% capacity otherwise), **kite** (laid, `deflect`: a blocked projectile is returned along the look at 80% speed, half the wear, melee capped at 75% capacity), **tower** (laid, `bulwark`: every angle, capacity x1.5, wear x1.5, wielder slowed 25% while raised, sprint plus use bashes instead: 2 damage, knockback 1.4, an 80 tick cooldown). The four ranged params `draw_mult`, `velocity_mult`, `spread_mult` and `quiet` are read by `RangedDispatch` whatever the class; shields get an `onShieldBlock` hook at LOW priority after `ModularEvents`' capacity clamp; `Burden` is the transient slow, cleared the first tick the item is not in use. **Unconfirmed:** that `LivingShieldBlockEvent` fires when vanilla's angle test says not blocked (if not, the tower's all-angles guard and the buckler's narrow cone work only from the front); the kite's return timing; the client pull animation still uses vanilla's duration. **Not built:** a sword/guard block route (the shield hooks are the place), a one-handed pistol, sprinting while drawing a shortbow.

**Merged (2026-10-21, compile only): the trident is a variant of the spear, the mace a variant of the maul.** There is no `trident` or `mace` type, item, part or table entry. *Spear*: one head part (`spear_head`) whose standard shape charges (`spear_charge`) and whose variants are the **trident** (three tines, trait `trident`: thrown), **javelin** (two cells, `lunge`), **harpoon** (forged, `harpoon`) and **pitchfork** (`fork`); two patterns (head and three handles, head and two). `ModularSpear` is a `TridentItem`: the standard head has no use of its own (the charge is a held key), any other variant is thrown on release; the enchantments are the trident's, per item, for now. *Maul*: the maul head gains a `mace` variant (nine cells, trait `smash`) and takes `heavy_core` as stock (a core is one block's volume, so it fills the nine cells exactly). `ModularMaul` is a `MaceItem`: the vanilla smash attack, scaled by head mass, runs only for a `smash` head; any other maul head staggers as before. The maul's family is `mace`, so every maul is crushing to a hauberk. Variant `mass` above 1 means a wider head than a standard cell holds (the spear head's cell is 0.0417 L, a tine head's 0.08), not swarf. A vanilla item converts to the variant named like its form (`trident`, `mace`), so a vanilla mace is a maul with a mace head. **Open:** enchantments on a part, so only a mace head carries the mace's and only a trident head the trident's (author, 2026-10-21); a converted trident takes the long pattern (three handles); the mace variant digs wood like any maul; a mace head's haft defaults to wood, not breeze rod.
