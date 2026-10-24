# Feedback — TODO

**File hold only work still to do. Cannot contain anything done.** No
`- [x]`, no "built and verified" sections, no decision records, no history. Item close: delete its line (and sub-bullets) in same change. Decisions and reasons go in design docs; history live in git. Only `- [ ]` items, plus minimum context to start one cold.

Source of truth for *what's next*; `feedback_philosophy.md` stay source of truth for *why*.

---

## Pick-up list

- [ ] **Skill tree content, 2026-10-24** (spec `docs/feedback_skill_content_spec.md`): built and compiled, UNPLAYED. Play pass needed per stem: parry (riposte + Throwback/Slip/Answer, Roll With It, Pry Open, Last Stand), evasion (Wall Smash/Rebound, Breach Kick), melee (Far End/Close Work, Throw, Feint, Draw Switch, Searing Strike), gun-fu (whip variants Brand/Muzzle Stagger/Bayonet Drill, Recoil Vault, Braced Fire, Hot Clear, Dodge Charge, Spent-Case Draw, Return Fire, Marked Rounds, Coin Flip on B with a stamped coin + Second Mark/Round the Corner), `fist` tree. Not built: Hitch, Conduct ([OPEN]). Known gaps: `runData` fails on missing `feedback:item/form/substrate` texture (coin item models were hand-copied); no skill-node icons for the new nodes; bayonet part has placeholder form step and model.

- [ ] **Tight parry window, 2026-10-24, unplayed** (author: a loose window is part of why parry did not feel good; "5 frame" read as 5 ticks): `ParryTuning` window now 3 (dagger) to 7 (greatsword) ticks, ping pad cap 2, Feedbacker 5, buckler `parry_ticks` 4, parry arm raise `RAISE_TICKS_HEAVY` 4, Feedbacker punch clip: 12 ticks in all, strike 1.5 ticks in (`UpswingSwing.PUNCH_LENGTH/PUNCH_STRIKE`), so the fist lands at the window's start and draws back briskly (author, 2026-10-24: it had landed as the window ended and drawn back slowly). Bracer and `wider_window` still add up to 2 ticks each (now a large share). Check feel, and whether 5 means ticks or render frames; recovery lockouts untouched.

- [ ] **Deposits, what is left** (mechanics §9.5; outcrops and the Survey Meter built 2026-10-23, unplayed): a finer meter tier, a loose-rock indicator block with art (outcrops are lone ore blocks now), indicators on the remaining veins (only tin, zinc, lead, copper, upper iron and graphite name one). Kaolinite: see Heat, vessels, materials.
- [ ] **Held-item info HUD** (BUILT 2026-10-23, unplayed): `client/ItemInfoHud`, key H, one card per hand listing only what carried instruments read off the held stack (heat, work, barrel, coat thickness, probe deposit, charge, wear, modular mass/hardness/truth figures, circuit response). Left: toggle not saved across restarts, no per-line colour, no wrap (clipped at a third of screen). Pre-existing tooltip leaks to gate: `JarBatonItem` volts (fixed %.1f) and `ModularPartItem` 'worked to' figure print with no instrument.
- [ ] **cleannrooster assets, what is left** (free-use repo at `../Texture-reference/cleannrooster_forg-cleannrooster-assets`; skill-tree icons built 2026-10-23, unplayed: `textures/gui/skill/<node id>.png`, drawn by `SkillTreeScreen`, 22 nodes). Not taken: Blockbench gun models (`bb/rifle`, `pistol`, `mortar`, `trapmodel`; GeckoLib format, fixed shape so they fit only as a held look or the mortar launcher), humanoid armour and mob models (templar, legion, artillerist: could skin armed skeletons), ~25 ability animations (`anim/ability`: slam, fist jab, flurry, shield bash, spear charge; Bedrock animation JSON, need GeckoLib or playerAnimator; decide with the animation-library item under Guns), spell icons for attunement and enchant screens (`spell/64`), potion and tincture sprites (`item/potions`).
- [ ] **Real models and textures** for every block and item. User's call — never start unprompted. With them: cogs' diagonal large-to-small meshing, wear shown as block state
  (battered/spent), split turning part from fixed base on Hand Crank and Clutch.

---

## Document 2 — mechanics (`feedback_mechanics.md`)

Philosophy defer to it by name for each.

- [ ] Exact ranges and arithmetic for every unit (§17)
- [ ] `Su` load figures; linkage `St` pairs per machine (§17)
- [ ] Thermal model and item heat, written up from code
- [ ] Vanilla vessel bands — smoker ceiling, Crude Blast Furnace floor, plain furnace span (§15)
- [ ] Six apparatus properties as numbers — range, resolution, accuracy, control, response, stability (§8)
- [ ] Drift and recalibration mechanism (§8, §9)
- [ ] Data links: 16-block range, client-drawn cable, cable item cost, no path check (§13)
- [ ] Per-energy sensor/actuator pairings, full matrix (§10)
- [ ] Overrun band widths per machine and material (§6)
- [ ] Noise model — irreducible floor, learnable-in-aggregate, narrows never collapses (§8)

## Document 3 — content

- [ ] Seed from slice 1's 12 items / 7 machines, then slice 2

---

## Electricity (`feedback_mechanics.md` §6)

**Open:**
- [ ] **[OPEN]** Sag curve and source current cap, blocked lower-voltage source, amps per sink per wire (§6.2)
- [ ] **[OPEN]** Band anchor at UHV (§6.3). HV is the electrostatic machine; MV has the galvanic cell pack. Built 2026-10-23, unplayed: `galvanic_cell_pack` (5 cells, 5.5 V), electrostatic machine at HV (24 V, 0.5 A).
- [ ] **[OPEN]** Over-voltage per-unit scale (§6.5).
- [ ] **[OPEN]** Motor drive one way only; reversing (swap leads) unbuilt.
- [ ] **[OPEN]** Plated coat read by modular tools and armour only (corrosion, armour skin hardness); optical sorter and plated contact conductivity still read nothing.
- [ ] Electrolytic cell's `[OPEN]`s (§6.14): conductive electrolyte, concentration instrument, current density burning deposit, gas capture.
- [ ] **[OPEN]** Breaker has one trip rating (copper cable's); discrete ratings wait on second cable metal.
- [ ] Insulation item with band rating (`electrical.insulation`, cable recipe row, over-voltage burns it off) and bare-conductor arc across one air gap (§6.4) are built and unplayed. Left: play it; insulation only on cable (fence/other conductors uncoated); JEI shows the bare recipe only; band numbers per material are placeholders; no way to strip or recoat a cable.
- [ ] **[OPEN]** Parallel sources: current routed along each sink's shortest path can overload one wire block. Revisit if layout show it.

---

## Rotation and wear

- [ ] **Bearing as own part.** Built one (second `ShaftBlock` at lower drag) wrong
  shape — bearing is support shaft turns in. Support-block vs unsupported-span rule
  undecided (`docs/feedback_machine_shop_spec.md` §4.1).
- [ ] **Replaceable heads** (spec 2.5): built, unplayed -- `machine_head` modular type (family `HEAD`, `STRIKE_ST` bake, `head_blank` part, Tool Table route), `HeadSlot` seated by right-click / pulled by sneak, Hammer throw = share of head ceiling, Press = full ceiling, wear and Jade/Calipers condition on the head, wiki `Machine_Head`. Left: play-test the St figures (`HEAD_ST_PER_MU_COMPETENCE`, throw shares), a head-specific wear rate, a machine-bench renderer showing the seated head, heads for the Rolling Mill / Wire Drawer.
- [ ] **Overspeed trip no natural trigger.** Nothing in roster reach 48 RPM except by
  `driveExternally`; wait for bigger source.

---

## Heat, vessels, materials

- [ ] **Unplayed, built 2026-10-23 from rulings:** galvanic cell anode wear (no Jade/HUD line shows the anode left), steam engine output bands, dip scorching, bad-alloy dilution spill. Check each; tune `GALVANIC_ANODE_*`, `STEAM_ENGINE_BAND*`, `DIP_FLAW_TU`.

- [ ] **[OPEN] Per-material heat capacity** (TFC has it; deferred). Worth it once player must choose which of two hot things carry first.
- [ ] **Vanilla furnace swap, unplayed** (`worldgen/VanillaVesselSwap`, built 2026-10-23): vanilla furnace, smoker and blast furnace convert on chunk load or placement. Check a village smithy and butcher, an old world's furnace, and a structure-mod enchanting table.
- [ ] **[OPEN] No steam bucket.** Add only when something must carry steam by hand.
- [ ] **Unplayed, built 2026-10-23:** pump lift cost (`PUMP_LIFT_SU_PER_BLOCK`, facing up only) and the Leyden jar's Jade state line (charging / holding / discharging).
- [ ] **Nothing need elevated pressure yet** — no `ThermalProcess` has pressure field.
- [ ] **[OPEN] Ceramic, kaolinite, firebrick, graphite have no casting:** none melt (they are `thermal_process` products), so no fluid to pour. What shape them (mould of wet clay before firing?) undecided.
- [ ] **Advancement unlocks left:** brass, bronze, tin, lead, glass, firebrick, quartz glass, graphite have no crafting recipe to unlock yet; add one with the first recipe that uses each.
- [ ] **[OPEN] Vanilla noise-carved iron and copper veins still generate vanilla ore** (shipping noise settings copy declined; mechanics §9.6).
- [ ] **Unplayed, built 2026-10-23:** galena, pyrite and kaolinite deposits (`galena_ore`, `pyrite_ore`, `kaolinite_deposit`; one block each, stone host only, no deepslate kind, sprites from `unused-textures` ore sprites; `FormData` tags and `src/generated` tags hand-edited, `runData` not run), galena melts to lead (`molten_lead`) and `silver_from_galena` (placeholder window 950-1150 Tu, the only silver source; `[OPEN]` real silver ore or a cupellation step), recipes for welder, pivot setter, data connector, ingot mold, lift, chassis frame, car design stand and damper (placeholder ingredients). Check each is findable, minable and craftable; tune vein `count`/`min_y`.

---

## Fittings and data links

- [ ] **`Adapter` has no concrete implementer.**
- [ ] **Removing fitting** (crowbar-style interaction).
- [ ] Refractory lining: no Jade line (crucible has none either).
- [ ] Link hover/selection colours (CFM's success/error/default, selected-node highlight).
- [ ] `DataNode.getMaxNodeLinks()` cap of 4 made-up number.

---

## Machine shop

- [ ] **Removal overruns spoil-only** (`steel_swarf`); degrade tier want named items (document 3).
- [ ] **Clutch's data-node overlay may not draw under Flywheel.** Harmless until node box needed.
- [ ] **Threaded rod** (rolled vs cut) and **Gear Cutter** — see `docs/feedback_machine_shop_spec.md` §6.
- [ ] **Drill Press auto-feed**: Crank Linkage + Clutch already do it — probably just document.

---

## Hazards and catch-up (`feedback_mechanics.md` §7, §8)

- [ ] **Tongs and gloves**: no unlock advancement, no Jade line (JEI info pages exist).
- [ ] **Unplayed, built 2026-10-23:** hot ground item lights flammable neighbours (`HotIgnition`, `HOT_IGNITE_TU`); flywheel rim strain (`FLYWHEEL_*`).
- [ ] **[OPEN]** Galvanometer on bare live wire shock hand; insulated probe nicer. Decide with instruments.
- [ ] Offline catch-up not applied to rotation/electric runs, Pressure Vessel leak, Condenser tank. Add if one visibly need it.
- [ ] Improved Furnace has own second `Heat.tick` path; its catch-up burn single fuel slot only. Look when next opened.

---

## Guns (`docs/feedback_guns_spec.md`)

- [ ] **Better Combat upswing, dual wielding, two-handed rule and idle poses, built 2026-10-24, unplayed** (required mod; `../BetterCombat-reference`; licence all rights reserved, never copy its files). All decided by our code from baked figures, none by its weapon-attributes files. `client/UpswingSwing`: a click on a modular blade/mace/spear or a bare hand is cancelled, the clip starts through its `PlayerAttackAnimatable` (its gearing puts the strike `upswing` of the swing in), and vanilla's attack or the sweep fires at the strike; `net/SwingStartPayload` + `SwingPlayPayload` relay the windup to onlookers. Holding the click never repeats (each swing needs a release; ruled by the author: HATES hold-to-attack). `combat/hit/Wielding`: two-handed = a spear, a blade over heaviness 0.7, a mace over 0.6 (placeholders); a two-handed weapon in either hand shuts the off hand (no off-hand strike, no dual wield); dual wielding = a one-handed strike weapon in each hand: left click is always the main hand and right click the off hand (author's ruling: never alternate) with the off-hand clip mirrored, the off hand strikes through `OffhandStrike` at the strike, right click and its key now also wind up, `DualWieldBoost` adds x1.2 attack speed (`Wielding.DUAL_SPEED`) and `OffhandStrike.cooldownTicks` follows it. `client/WeaponPose`: idle `pose_two_handed_sword/heavy/polearm/bow/crossbow` on two layers (body and item channels), from the held stack on every client. Check: feel of the delay, `READY` 0.5 click gate, `MIN_LENGTH` 10, the thresholds, stagger/parry cancelling a windup, left-handed mirroring, first-person arms and the Feedbacker skin, the sounds, a mace fall slam, others seeing the windup and hearing the strike, dual-wield hand alternation and the off-hand cooldown gate, the idle poses (fade, walking, sneaking: Better Combat's sneak arm offset is not copied). Not done: hiding the off-hand item while two-handed (Better Combat reports the slot empty by mixin; ours only blocks its use), a katana pose (no variant lookup yet), Better Combat's slash trail particles (`playAttackParticles`), a timer-based hit for a gun bludgeon, a `LICENSING.md` §7 exception, parry with the off hand under a two-hander, and everything is untested with Better Combat's own attack logic running (its fallback matches items by id; if it acts on ours set `blacklist_item_id_regex` to `pickaxe|feedback:` in `config/bettercombat/fallback_compatibility.json`).
- [ ] **Animation library, evaluated 2026-10-23, not adopted.** Lean playerAnimator (KosmX; MIT; NeoForge 1.21.1 builds listed, 2.0.x): 24 of cleannrooster's 31 animation files (`anim/ability`) are already its emote format (ticks, easing, head/torso/arms/legs/item per part). GeckoLib (4.7/4.8 on 1.21.1) does not animate the vanilla player; it suits mob and item models only, and a fixed gun model fits our baked-from-parts guns badly. Zigy's Player Animation Library (MIT, 1.1.x for 1.21.1 NeoForge) also loads Bedrock/Blockbench JSON; compare it in the spike. Not verified: Maven coordinates, first-person support, how it layers with `CombatPose` (which tips the pose stack in `RenderLivingEvent.Pre` and offsets hands in `RenderHandEvent`), whether animation speed can follow a swing's baked mass timing. Both libraries are runtime deps the pack must carry (`build-and-deploy.sh` pin list). **Built 2026-10-23; `slice` played and works, the rest unplayed:** 18 emote clips in `player_animations/`; `client/SwingClips` plays blade `slice`, mace `2h_slam` (airborne `leapslamstaff2`, both capped short after the author found them too long), spear `spearthrow_charge` then `spearthrow_throw` on hold and release, empty-hand `fist_jab_right`/`fist_jab_left`. Cut after play: `cleave`, `flourish`, `whirl`, `stabend`, `fist_flurry_right`. The roll is now only `fist_tumble_right` over the whole roll (ruled 2026-10-23: the procedural tuck is deleted from `CombatPose`; check the clip's pace, that first-person hands show it, and the i-frame window still reads). Feedbacker parry plays `fist_jab_right`; its skin arm in first person during a clip is `CurioRenderers.FirstPersonSkinLayer` (the library drops every layer but item-in-hand ones in its first-person pass; unplayed). Check the mace caps (`SLAM` 10 ticks, `AIR_SLAM` 7), the spear hold and release, and the jabs. No clip for a gun, kick or parry (`CombatPose` draws those). Left-handed players get a right-arm clip, no mirror. Unused clips stay for moves the mod lacks. Decide spec §7.2.

---

## Programmers

**Later programmers (write down, do not build):**
- [ ] Cards made of cards (functions); delay card; addressable memory; loops. **[OPEN]** against philosophy 13 "capability does not tier" and stateless-controller ruling -- see `docs/feedback_controller_spec.md` §8.
- [ ] `NOT` gate (one input) on Logic card. Greater/Less could become one Compare card same way, if wanted.

---

## Vehicle ideas

(author, 2026-10-16)
- [ ] Later experiment: tire as own rolling body of 1/16 boxes on motored hinge (spec §5 *Guard rays*). Bumpy by construction.
- [ ] Hub with three points: axis, plus two locked to frame (real hub), so drive transfer has fixed frame of reference.

---

## Materials

- [ ] **Material forms by tag**: fuel moved onto materials (mechanics 5.7) and modded logs/planks lost `#minecraft:logs`/`planks` route; let material form name tag so `Material.find` reach modded items. Also bamboo no combustion yet (need own yields).
- [ ] Tempered steel figures (hardness 20, toughness 40, service 400) placeholders between steel and hardened steel.

## Vehicles

- [ ] **Controls: see braking in client.** Everything else on controls seen. Check: full
  brake skid iron-padded hub, half does not; braked driven hub stall engine. Retune
  `STEER_*`, `BRAKE_CLAMP_SU`.
  - [ ] Card targets inside a vehicle stored as offsets from the frame (ruled 2026-10-23, vehicles spec); check a program survives lifting and lowering.
  - [ ] Brake pads wear only from misuse (skidding past grip cap, stalling the engine on the pads) -- ruled 2026-10-23, build after brakes are played.
  - [ ] Power steering (assist `Su` from drivetrain).

  Built 2026-10-23, never driven (vehicles spec, last section): open differential, slider spring joint, transmission on the Car Design Stand. Check each in a car; the spring is Sable's PD motor, not our own force yet.
  - [ ] Differential at the weld itself (two couplings share the shaft; needs a cross-link), and a lock / limited slip.
  - [ ] Spring: material sets the range; replace the PD motor with our own per-tick force once it has been driven; the tire's own spring still in series.
  - [ ] Transmission: model and art, a clutch interaction, wiki page; the Engine Stand's ids are still `engine_stand` (display name only is Car Design Stand).
- [ ] **Guns on vehicles** (`docs/feedback_guns_spec.md` §8). Want mount. Car drive now; after controls.

**Vehicles beyond cars -- ideas** (author, 2026-10-20, from reading Create Simulated, `../Simulated-reference`; write down, do not build). Each carries its Minecraft lens in the line.
- [ ] **Named assembly failure, what is left.** The Lift names its refusals (empty, too big, locked, on a vehicle, blocked); still silent: frame assembly skipping a cell that would land on a block, and bearing-specific reasons ("frame touches two bases", "bearing has no far side").
- [ ] **Balance gauge, what is left.** The Lift shows total `Mu` and centre of mass on use; still owed: lift or thrust against weight once flying parts exist.
- [ ] **Clipboard copy of settings, what is left.** Hinges, tires, brakes and suspension already copy across a Symmetry mirror or a Welder selection; cards have no copy.
- [ ] **Hot-air or steam envelope.** Flood-filled airtight enclosure (airtight as a material property with a leak rate per face, not a tag-only yes/no). Lift = (ambient air density - gas density) x `Mb` x g, so the burner is a `HeatSource` burning real fuel and the envelope a `ThermalBody`. Fill and empty lag the heater (Simulated: 9 s). A breach leaks and shows a gust. Heaters on one balloon merge. Steam is the second gas, from the Boiler. The Nether's ambient (`AmbientTable`) and altitude change the lift. Overrun: a burner left on overheats and wears the fabric. **[OPEN]** how a hole is repaired; whether lift should depend on altitude pressure.
- [ ] **Propeller.** Blade count and radius come from what is built (Simulated's sail count, thrust ~ sails^1.5). Add what it lacks: thrust falls as airspeed nears tip speed, and a named pitch band (feather, coarse, fine) that stalls when wrong. Overspeed tears blades.
- [ ] **Thrusters.** Burn fuel, so they wait for petrochemistry (`docs/feedback_progression_roadmap.md` Slice 7). No fuel-free thruster. **[OPEN]** liquid or solid propellant; whether thrust vectoring is a hinge on the nozzle.
- [ ] **Wings and fins.** Lift and drag per welded plate from its normal and airspeed squared, the way `Frame.vdrag` already reads drag; stall past an angle. The same plates as rudders and keels for boats.
- [ ] **Gyro stabiliser and passive levelling.** A spinning flywheel resists tilt (`Inertia`: heavier and faster is steadier, with spin-up time and shaft drag). A pendulum hinge keeps a thrust axis upright, as Simulated's gyroscopic and smart propellers do, by geometry only.
- [ ] **Hulls and ballast.** Mean density under 1 `Mu/mB` floats; a sealed cell is `Mb` of air displacing water; ballast fills in named levels; depth crush reuses `PressureBurst`.
- [ ] **Vehicle instruments.** Altimeter (Y and `Pu`), airspeed, attitude, tachometer, depth gauge, each a carried instrument or a fitted `Sensor`, and a `DataNode` feeding cards. Autopilot is then only cards (analog is allowed in vehicles). Simulated has altitude, velocity and gimbal sensors.
- [ ] **Hitch and docking.** A second base is a second body (trailers, already listed). A hitch has a rated load and wears or snaps when overloaded. Docking is a mechanical coupling carrying a rated load; a shaft may cross a docked pair through a clutch. **Not** Simulated's energy and fluid pass-through, which would make electricity a universal currency.
- [ ] **Rope and winch**, and a **spring** that snaps when overextended (Simulated: 0.75 s).
- [ ] **Leak marker.** Particles or heat shimmer at an envelope or hull breach.

## Modular items (`docs/feedback_modular_items_spec.md`)

Tools, melee and ranged weapons, armour as baked designs; vanilla tools and armour replaced. Build order in spec §10.

- [ ] Impact pendulum (toughness) and strike post (damage per blow) as blocks (spec 6).
- [ ] Spec §11 `[OPEN]`s: section and as-built part shape; pry bar and prospector's pick; a crossbow's draw time and a lining's heat (vanilla reads constants).
- [ ] **Twin-headed tools, what is left** (spec §5.3, built 2026-10-20): wear is one pool for both edges (only the head used should wear: needs per-head damage state); a tip sets on the first head only; family names (`family` is still the bake's enum).
- [ ] **Part variants, what is left** (spec §12, built 2026-10-20; raw-stock roles 2026-10-21): no variant return swarf (the stock-to-litres mismatch is not modelled); a rim variant is a trade in stock and mass only (a heavier shield slows nothing yet); armour studs and a lining have no variants; the Blueprint Table draws standard shapes only.
- [ ] **Forging by rhythm, what is left** (spec §4.1, played at the Tool Table since 2026-10-21): the Mechanical Hammer's pattern time (instant), its need of a hand hammer in the slot, and a cam pattern of its own (spec §4.1); deviation from the Press and grinding (§4); more patterns and more parts; a gametest of `ForgeRhythm.judge` if wanted.
- [ ] **Scale loss in the Hearth** (spec 4.2, 2026-10-21): stock left in a pan above its material's `oxidation_temperature` is only refused by the table once past its window; it should lose a share (overrun on the finished stock). Needs a flaw channel for raw stock. Hearth art is the Firebox's brick, placeholder.
- [ ] **Tool Table art** (spec §4.2): the six part items (`tool_tip`, `tool_binding`, `sword_guard`, `sword_pommel`, `shield_rim`, `crossbow_prod`) borrow existing sprites (gem lens, leather strap, the modular sword, shield and crossbow layers) tinted by material and want art of their own.
- [ ] **Gun and ammunition options** (author, 2026-10-21; spec §13): as many choices as tools have, each changing only figures its own side owns (gun: barrel, muzzle, action, launcher, sight, stock, fire control; round: projectile shape and jacket, charge load, case, payload). **Suppressor blast/crack split is built; the subsonic load is a light charge count, labelled on the Gun Table. Fittings are a data table (`gun_option/*.json`, verified in game).** Then the Gun Table, then ammunition shape and jacket, then the pneumatic, spring and steam launchers. Open questions in §13.7.
- [ ] **Signature behaviours, what is left** (spec §5.4, played and working 2026-10-21): the spear's hitbox inflation, attacking through non-solid blocks and a custom first-person pose; prospector's range per handle length and tint per ore family; bleed credits no one.
- [ ] **Archetypes, what is left** (spec §12, built 2026-10-21): open helm's wide vision and hauberk's bleed; the sledge does not mine stone faster; sprinting while drawing a shortbow; a one-handed pistol crossbow.
- [ ] **Enchantments on parts, not tools** (author, 2026-10-21): the trident is now a spear head variant and the mace a maul head variant, but enchant tags are per item, so every spear takes Loyalty/Riptide and every maul takes Density/Breach for now. Later an enchantment sits on a part, and only a trident head can carry the trident's, only a mace head the mace's. Spec §12 merge note.
- [ ] **Katana's trick** (author, 2026-10-21): an edge harder than its spine (hardened-steel pairing). Per-variant dig tags beyond "digs nothing" (a variant that digs a different tag) are not built.
- [ ] **Enchant roster** (`docs/feedback_enchant_roster.md`, 3a has the per-effect build status): all 12 vanilla-derived effects built as a compiled first cut (2026-10-23, unplayed); polarity (mirror band, signed level) and the `AttunementKinds` registry in place. The four novel effects (piezo, dowse x4, reservoir, charge) and a creative XP source were built 2026-10-23, unplayed (roster 3a has what each skipped). Not built: coupled parts, the Unstable option, Bane's tag picker, gun rows, and the roles each effect's first cut skipped (listed in 3a). Every number `[OPEN]` until played.
- [ ] **Second-wave attunements** (`docs/feedback_enchant_roster.md` 6, ruled 2026-10-23): Echo, Grudge (shares Charge's meter), Momentum, Pack (feed from FTB Teams as a soft compat hook), Hunger, Mark, Bloom, Bait, Wager sent to agents; Lode, Ring, Cling, Gleam ruled to build (roster 6c); armour faces for the weapon-only effects (roster 6b) in progress. Every number `[OPEN]` until played.
- [ ] **Enchanting rework, first slice left out** (`docs/feedback_enchanting_spec.md`, built 2026-10-23, unplayed): the table, `EnchantGame`, three effect rows (`attunement/`) and the MYSTIC skin exist. Not built:
  - [ ] Heat erasing attunements (built 2026-10-23, unplayed): `Attunements.eraseIfOverheated` in `ItemHeat.set` clears a loose part's attunements when it is stamped past its attuning point. Not reached: parts already inside an assembled tool.
  - [ ] Reagent slot: none, XP only. XP is `XPu` now (`core/unit/XPu`): a session costs `ENCHANT_XP_COST`, drawn from a Creative XP block beside the table, else the player's points. The table holds no charge of its own yet (no block entity), and no real producer exists.
  - [ ] Capacity (built 2026-10-23, unplayed): a part takes `ceil(enchantability / ENCHANT_PER_SLOT)` attunements (`EnchantGame.capacity`). Further attunements are independent sessions (ruled 2026-10-23): none disturbs another.
  - [ ] Ward's grip kind on a handle (less kick) and its mass cost on a plate; keen's brittleness is a forging flaw per level, not a toughness figure of its own.
  - [ ] Material `attuning` blocks: written for iron, steel, cast iron, copper, bronze, brass, gold, silver, zinc, tin, lead, netherite, diamond and quartz (temperature near the metal's anneal point; harder is stubborn, softer quick); all other materials still use the fallback. Every figure `[OPEN]` until played.
  - [ ] Block entity: the table keeps nothing and returns the part on close; no book model. Vanilla tables in structures convert on chunk load (`VanillaVesselSwap`).
  - [ ] Villager enchanted-book trades and loot books left as vanilla. Whether an attuned part returning from a taken-apart tool is too generous is `[OPEN]`.
  - [ ] An attuned gun part (barrel) gains non-empty `dims`, which `Machining` reads as machined; only shaped tool/armour parts are meant to be attuned for now.
  - [ ] Every constant in `ModularTuning.ENCHANT_*` is `[OPEN]` until played; no unit test (the repo has no test scaffolding).

## Approved ideas, not yet built

Written down at author's word; prior-art question still unasked for each.

- [ ] **Spring weapons from stored work** (BUILT 2026-10-23, unplayed; guns spec 13.4): `Launcher.SPRING`, role `U` main spring, fires dry rounds. Left: melee/thrown spring uses, spring set on overload, own art for `main_spring` (borrows an iron nugget sprite).
- [ ] **Steam launcher** (BUILT 2026-10-23, unplayed; guns spec 13.4): `Launcher.STEAM`, role `O` steam chest, filled by right-clicking a boiler. Left: fill capped by source pressure, chest that can burst, HUD gauge, own art for `steam_chest`.
- [ ] **Demolition charge and explosive rounds** (BUILT 2026-10-23, unplayed; guns spec 13.4a). Charge is now a modular type (`charge.json`, Blueprint and Tool Table, unplayed); Blueprint Table has name search on types and materials (unplayed); filler role lists every powder (redstone included) and should hide what the bake refuses. Left: thrown grenade (impact or delay fuse, an entity), fragmentation from casing, more fuse kinds, damp duds, own art (borrows TNT's block texture).
- [ ] **Incandescence** — not build yet. Above ~525 Tu hot body emits block light in bands: melts ice, stops spawns; insulated vessels stay dark. Which blocks and bands still unasked.
- [ ] **Villager commissions** — later. Smith orders parts *by spec*, pays by closeness; repeat orders saturate.
- [ ] **Refined redstone superconductor** — late, hard line. Chain: purify, blend to stoichiometry (mixer), calcine, pack in sheath and draw/roll, draw-anneal cycles (path-dependent), reaction anneal under `Pu`. No drop up to critical current, then quenches and runs away. **[OPEN]** sheath metal, critical Tu.
- [ ] **Fluid temperature, what is left** (built 2026-10-23, unplayed; mechanics §5.9): cooling in unloaded chunks; feedwater temperature credit to the boiler; a steam gun chest that keeps its steam's temperature; `thermal_process` still item-input only, so condenser and boiler use the separate `phase_change` table.
- [ ] **Thrown weapons, tuning** (spec §12): `[OPEN]` figures in `ModularTuning` (throw speed, spin, tumble floor, reel).
- [ ] **Novel gun options, brainstormed 2026-10-21** (rows in `data/feedback/gun_option/`; gun-owned figures only, handoff section 1): **Eddy Sleeve** (BUILT, 2026-10-21; `eddy` field on `GunOption`, drag scaled by sleeve metal's ampacity; muzzle sleeve: eddy drag on conducting or magnetic bullet turns kick into sleeve heat; redstone winding drags hardest; dead weight on lead, overheats at cadence), **Sculk Jacket** (barrel jacket stores vibration for tight cold groups; spoils above sculk's oxidation temperature), **Resonance Weight** (three thread positions tune barrel node; worse than bare when mismatched with muzzle speed), **Heavy-core counterweight** (BUILT 2026-10-22, unplayed; `GunCounterweight`: kick x0.85, cycle x1.25, wear raises kick to x1.5; plus bipod and selector rows on existing traits) (reciprocating dense mass cancels kick, slows cycle, slaps receiver if stroke cut short), **Honey hydraulic buffer** (viscous recoil damper, hot honey thins; need fluid in fitting), **Glowstone reticle** (battery-free lit sight, by `luminous` trait), **Redstone magnetic sear** (zero-standby latch; wait on battery ruling), **Phantom-membrane obturator** (seals loose chamber, cuts blowback; burns when hot), **Thermal-fit sleeve** (BUILT, 2026-10-22, unplayed; `GunThermalFit` trait on `gun_option/thermal_fit.json`: loose cold, tightest at 0.4 of ignition, binds past 0.7 by cutting chamber rating; one expansion for every metal). Rejected: ender shutter (free lunch).
- [ ] **Guns as tool type, what left** (built and played; `docs/feedback_modular_items_spec.md` 8.3, 8.4, 12). **Not built:** fan costs flat, not growing per shot; repair of gun by stock ([OPEN]); swapping fitting for another of its slot need Rework "take apart" (old refit gave displaced part back); fitting that only adds trait (bipod, selector) has schema (`trait`, `params` on `GunOption`) but no row yet; barrel at table made at variant's nominal length and wall, only machines cut it to size.
- [ ] **Gunfire as vibration** (BUILT 2026-10-23, unplayed): three game events by `report` (quiet 8 / loud 32 / thunderous 64 blocks; thresholds `FTuning.GUN_QUIET_REPORT`, `GUN_THUNDEROUS_REPORT` placeholders), in `vibrations` + `warden_can_listen`; player and mob shooters. Left: play-check sculk/warden ranges; sculk frequency (comparator) unmapped for custom events; subsonic load still not made. [OPEN] are three classes enough, or does a heavy rifle want its own tier?
- [ ] **Jar baton** (BUILT 2026-10-23, unplayed; wiki/Jar_Baton.md): `JarBatonItem`, charge drawn from a Leyden jar by use-on-block (`BATON_VOLTS` component), whole charge spent on next hit via `Shock` + `Exposure`, `CombatTicker.stagger`; overrun (above `BATON_RATING_VOLTS` or holder in water) shocks holder, gloves protect. Tuning placeholders in `FTuning`. Left: own art (borrows thermionic valve sprite), no leak while held, no modular-tool-type fit, no JEI/recipe-book check, runData not run. [OPEN] should a held baton leak like a jar?
- [ ] **Echo shard armour: footstep warning** (candidate, spec 2.1a): echo shard armour piece hears nearby footsteps as sculk sensor does. Not ruled in.
- [ ] **More material perks** (author, 2026-10-21): `perks` map (`Material.Perk`, `modular/MaterialPerks`) has first batch of spec 2.1a (silver, blaze rod, prismarine, echo shard, and second batch). Every material that earns one should get one, qualitative only (stat stays stat). Example given: breeze rod heads count lighter for handling.
- [ ] **Nether-hot guns on mobs** (BUILT 2026-10-23, unplayed; `docs/spec_armed_skeletons.md`): carriers widened to `AbstractSkeleton` (wither skeleton at `FTuning.WITHER_SKELETON_GUN_CHANCE`, placeholder); mob shots already ride `GunCondition`/`AmbientTable`, so a Nether gun starts hot and cooks off sooner. `[OPEN]` strays/bogged armed too; difficulty scaling; whether the goal should refuse a too-hot gun (player has no such check).
- [ ] **Slice 8, Basic Electronics** (brainstormed and approved 2026-10-23; all four phases BUILT 2026-10-23, unplayed; roadmap §2 Slice 8). Prior art: GTCEu Modern read for design (tag-slot components, yield ladder taken; circuit tier = voltage tier, wafers, SMD multipliers, cleanroom gate rejected; nothing for piezo or instruments). Ideas:
  - **Redstone coherer** (phase 2 BUILT 2026-10-23, unplayed; `docs/spec_basic_electronics_phase2.md`, mechanics 6.18, wiki/Redstone_Coherer.md): dust tube conducts only after a spark (game event `feedback:spark`, from breaker/relay arcs and generator turns) welds it; tap resets (hand or controller switch-off); left welded it passes everything; sinters over its rating. First detector.
  - **Cat's-whisker detector** (phase 2 BUILT 2026-10-23, unplayed; wiki/Cats_Whisker_Detector.md): wire probes a crystal by `electrical.rectifying` (galena, pyrite, quartz; material files added) for a spot, a hunt by stepping and a noisy band; overcurrent fuses the spot, re-seat. `[OPEN]` phase 2 placeholders in `FTuning`, art, burnt crystal not spent, one spark class, no physical clapper, relay armature unsided.
  - **Quartz/amethyst resonator + mass sensor** (phase 1 BUILT 2026-10-23, unplayed; `docs/spec_basic_electronics_phase1.md`, mechanics 6.17, wiki/Mass_Probe.md; grinding-wheel conflict FIXED 2026-10-23 (Diamond Grinding Wheel, mechanics 6.19, BUILT unplayed); cut is a `cut` component and `FormStep.finer` bumps it; placeholder art; runData not run, item models hand-written; solder, scope, circuit are phases 3 and 4): cut class sets frequency (discrete); budding amethyst growth = hydrothermal crystal; frequency shift reads mass deposited in the electrolytic cell (Slice 5); over-drive cracks plate. Also the signal generator clock.
  - **Solder and joint quality, two ways to lay traces** (phase 3 BUILT 2026-10-23, unplayed; `docs/spec_basic_electronics_phase3.md`, mechanics 6.20, wiki/Solder.md, Trace_Board.md, Soldering_Iron.md, Reflow_Oven.md): solder is two alloy materials (liquidus = melting point, window from viscosity), joints graded by `process/Soldering`; hand iron vs reflow oven (`ThermalBody` batch); leaded poisons (`core/hazard/Fumes`); etched or printed traces on a ceramic `substrate` form. `[OPEN]` placeholders in `FTuning` and all art; no acid (etch is the crafting); no polymer substrate; no dead-part failure (nothing mounted yet); lead-free is a narrower window only; fume protection; `runData` not run (models hand-written). Left for phase 4: the circuit that reads a board's grade.
  - **Relay** (phase 2 BUILT 2026-10-23, unplayed; wiki/Relay.md): contacts move by an adjacent electromagnet's field (`ElectricMember.joins`); arc and wear under load, worn contacts release late; bridge is existing parts (breaker a controller switches on the coil run; current sensor fitting on the contacts run).
  - **Instruments and phosphor screen** (phase 3 BUILT 2026-10-23, unplayed; wiki/Component_Tester.md, Oscilloscope.md, Phosphor_Screen.md): `Testable` parts (coherer, whisker, relay, board) read by the tester through `Quantity.RESPONSE`/`CONDITION`; oscilloscope samples an adjacent `ElectricNode`; glass envelope hot-sealed into a partial vacuum (a `PressureHolder`, no pump), glows by oscilloscope, burst on reheat, burn-in. `[OPEN]` no spatial trace on the screen (needs a block entity renderer), vacuum is partial, placeholders and art.
  - **Basic Electronic Circuit** (phase 4 BUILT 2026-10-23, unplayed; `docs/spec_basic_electronics_phase4.md`, mechanics 6.21, wiki/Basic_Electronic_Circuit.md): property-bearing `circuit` component (elements, speed, noise, joint, limit, rating), not a tier key; parts mounted on a board in the crafting grid, soldered by the existing iron or oven; read by `Circuit.of(stack)`; first consumer the cat's whisker (noise, reach, hold). `[OPEN]` placeholders in `FTuning` and art; part condition does not reach noise; no rework of a cold circuit; no creative sample; whisker is the only consumer (later machines still to eat it); runData not run.
  - **[OPEN]** Resistor as a dropper (carbon bands, drop shed as heat): digest says resistance is not modelled; needs a ruling or a cut.
  - `Instrument` fit the tester unchanged (a `Testable` interface carries the lines); the phosphor screen is built (above).
  - **Hand soldering by drag** (BUILT 2026-10-23, unplayed; `docs/spec_soldering_minigame.md`, mechanics 6.22, `client/SolderScreen`, `net/SolderFinishPayload`, `Soldering.Pass`): replaces the hold-and-release iron. `[OPEN]` answered: whole-board lift, restart after reheating, pad count 2 x parts, no keyboard fallback, fumes at open and at finish; `FTuning` soldering block is placeholder; unplayed, art is flat fills, sound is a vanilla placeholder.
- [ ] **JEI for basic electronics** (BUILT 2026-10-23, unplayed): Soldering category, probe/board crafting examples, cut/board/circuit subtypes, info pages, finer-cut grinding cards. Left: examples fix the first substrate loaded and cycle only the varying input; etched soldering lift limit is not per wire (card shows printed and lowest substrate); galena/pyrite have no recipe (info page only); play-check that real board/probe recipes show no empty card.

- [ ] **Skill tabs with several trees** (BUILT 2026-10-24, unplayed): pools per tab, 11 trees, 7 new casts; rest of spec §15 casts and the Gauntlet tree still to build. See `.claude/skills/skill-trees/SKILL.md`.
- [ ] **Gauntlet accessory** (author, 2026-10-24): a worn piece that synergises well with fist skills (not the Feedbacker specifically; trees never require equipment, single skills may). Needs a design pass.
