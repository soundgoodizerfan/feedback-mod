# Enchant roster -- proposal (2026-10-23, collapsed)

Every vanilla enchantment (1.21.1, all 42) reimagined as an **attunement**, then collapsed: most of the 42 were one
coupling on different parts of a thing, so the vanilla roster is **12 effects** and the part's *role* picks which vanilla enchant
you get. Four novel effects sit beside them (section 3), making 16. Companion to `feedback_enchanting_spec.md`. **Proposal, not built.** Of the 12 only `heat` (as `fire_aspect`), `edge`
(as `keen`) and `ward` exist as rows today, and polarity is not built. Every number and most mechanisms are `[OPEN]` until played.

Brainstormed with the `feedback-brainstorm` skill. Grounded in the XP chapter of `speculative_physics_inspo_doc.md` (1.1, 1.2),
`feedback_guns_spec.md` (3, 13), the existing hazard and thermal code, and the roles in `data/feedback/modular_type/`.

## 1. Structure

- **An effect is a coupling.** An attuned part holds a frozen polarization, so it couples to something: heat, impulse, a
  hardness, a fluid's drag. The effect row names the coupling; the **role** decides what the coupling does there (an edge
  ignites, a handle burns you, a plate ignites an attacker). A new effect is a row, not a class, wherever its kind exists. A new
  kind is one handler plus one `tooltip.feedback.attuned.desc.<kind>` lang key (a kind without one shows its raw key in the
  effect tooltip).
- **Polarity is settled (author, 2026-10-23), and built (2026-10-23, unplayed).** `target` stays positive. An effect row may
  carry a `mirror` map (role to behaviour, same shape as `roles`); when it does, the gauge shows a second band at `-target`,
  and where P lands decides the side: above zero is the effect, below zero is its mirror. The level is stored signed
  (`attuned.<id> = -2` is a level-2 mirror) and every reader takes the magnitude and picks the behaviour from the sign
  (`Effect.on(role, negative)`). An effect with no `mirror` treats a landing below zero as a miss, as before. The player
  chooses a polarity by steering, not by a menu. `Attunements.Effect`, `EnchantGame.judge`, `MaterialPerks.attunements`,
  `Attunements.flawShift`, the gauge and the part tooltip all follow. `keen` carries a placeholder mirror (`Glance`, a plate
  that turns a blow aside) only so the path can be played; its real mirror is the roster's Edge-.
- **Handlers are a registry.** `AttunementKinds` holds the runtime half of a kind in three small maps: the wielder's blow
  lands (`onStrike`), the wearer is struck (`onStruck`), the part is a flame in the hand (`onFlame`). A new kind is one
  registration there, plus its `tooltip.feedback.attuned.desc.<kind>` lang key; a pure bake kind (`fracture`) needs none.
- **A mirror is the same coupling reversed, not offence against defence (author, 2026-10-23).** Repel and attract, push and pull, emit and absorb, couple and decouple. A weapon and the armour made for the same job are the same side of one effect, split only by role (Bane on a blade and its protection on a plate are both +). So a mirror can be a real trade: Bane's mirror is Kin, which is attraction to the undead. Check any new effect for this: if its "mirror" is only the defensive twin of its +, it belongs on the same side.
- **Bad pairings come from material figures, never from authoring.** Conductivity makes a Heat- plate a heat pipe into the
  wearer; combustion figures make a Heat+ string burn; magnetism decides what a Recall can pull back. Nothing keys on a
  material id (digest rule 1).
- **Properties and tags, not identity.** "Water" is *fluid drag*; "undead" is a tag; "magnetic" is `curie_temperature`. A pack's
  honey-like fluid or modded mob works with no new row.
- **Capacity is one effect per part** (spec 6, `[OPEN]`), so every effect must be a real choice. Ward and Edge pay for their
  strength through `flaw`, or they always win.
- **Unstable** is an option on any effect, not an effect: a wide band (easy to set) and the pole dies with its holder. It is
  what Vanishing Curse becomes.
- **Curses are legal effects with an upside.** Philosophy 7: a part is never destroyed, so a curse is a trade.
- **Overlap with material perks.** `Material.Perk` already has smite, fire aspect, impaling, echo, chill, toxic, bounce, cling,
  siphon, blink. Attunements reuse those handlers; whether an attunement and a same-named material perk stack or take the
  maximum (`MaterialPerks.of` merges by max) is `[OPEN]`.
- **Gun rows respect the ownership line** (guns spec 13.1): a gun attunement changes only figures the *gun* owns (barrel speed,
  recoil, heat, cycle, spread, condition), never the round's mass, hardness or charge.
- **"Interaction" lines** are things that *work* but are not written on the tooltip: another effect, a material, a circumstance in
  the world. They are not bugs and not surprises for their own sake. The tooltip states what an effect does; these are what the
  player finds out. Each follows from numbers the mod already has.
- **Count.** Twelve vanilla-derived (not eleven as first said: ten single couplings plus Conduct and Mend), four novel: sixteen.
- **Coupled parts** (an engine rule, not an effect). Parts of one carried tool or kit couple by sign: like signs reinforce, opposite signs make a flow between them (Heat- handle and Heat+ head is a heat pump; Impulse- handle and Impulse+ head is a strike that does not kick back). It gives set bonuses with no id or set definition. `[OPEN]` until the sign-ruling in `judge` is built.

## 2. The twelve

### 1. Heat (absorbs Fire Aspect, Flame, Fire Protection, Frost Walker)
- **+ emits.** Edge ignites the target; string or barrel carries heat into the shot; plate ignites an attacker; a handle or grip burns the wielder (`HotContact`, at the part's flame temperature).
- **- absorbs.** A lining insulates (low conductivity good); a plate acts as thermal mass and slows heating; a sole conducts heat out of water under it, freezing it.
- **Bad pairings:** a conductive plate under Heat- carries the heat to the wearer faster. A combustible string under Heat+ burns. Heat- in a hand chills it.
- **Interactions:**
  - The flame a Heat+ part carries is put out by standing in water. `[OPEN]` whether `HotContact` checks wetness today.
  - A Heat- sole warms as it freezes water. Keep walking and it climbs toward its attuning point, where heat erases the pole (spec 6, not built): the effect limits itself.
  - A Heat- *handle or grip* is a forging grip: the hand holds a hot part longer before `HotContact` bites.
  - A hot enough Heat+ arrow ignites a propellant or explosive charge from range (explosives built 2026-10-23): a demolition tool, and the hazard a quiver of them is.
- **Gun:** Heat+ on a barrel is an incendiary round, but the barrel now runs at that heat, which is exactly the cook-off condition (guns spec 3.4). Heat- on a barrel or vent is a heat sink that bleeds into water if dipped: the cleanest cooling fitting the mod could have. Heat- on a grip or shroud keeps a hot barrel off the hand.

### 2. Impulse (absorbs Knockback, Punch, Wind Burst, Thorns, Density, Blast Protection, Feather Falling, Multishot, Sweeping Edge)
- **+ pushes.** Head: knockback. Limb or string: punch. Maul head or handle: a rebound that launches the wielder. Studs: reflects a share of the force back. A maul scales its blow with the height fallen.
- **- damps.** A lining spreads a blast; a sole or lining takes a landing.
- **Split (a role's reading of +).** A stock splits the impulse among bolts; a wide blade splits it among targets. Momentum is conserved, so each share is smaller.
- **Bad pairings:** Impulse+ on a handle kicks the wielder back; on a grip, studs bite the hand. A brittle plate under a big impulse gains flaws instead of absorbing it.
- **Interactions:**
  - The shove scales against the target's mass (`BodyMass`), and a target held by a grapple tether takes it as tension and does not move.
  - Damping a landing also damps the vibration it sends through the ground, so an Impulse- sole is quieter to a sculk sensor (compendium 1.2: mechanoreception).
  - The shooter's own overpressure blast (`GunHazard`) is an impulse, so an Impulse- chest also softens a bad chamber.
  - A Blast-damped lining under Thorns studs leaves less impulse to reflect; damping and reflecting compete across parts of one suit.
  - Wind Burst launches you and the fall is the Density: two parts of one maul chain.
  - Sweep with Knockback on the head shoves each target less than a single blow would.
  - The burst is air displaced; underwater it is a thud (it reads fluid drag, as Tide does).
- **Gun:** Impulse- on a receiver redirects the blast of an over-rated chamber from the holder (less hurt, not less wear). Impulse+ on a muzzle brake adds a point-blank shove; on a stock or recoil pad it is recoil. Split on a barrel is a pattern: one round's energy over several pellets, with a choke tightening it (guns spec 13.5). Wind Burst on a muzzle (a gust that clears `GunSmoke`) is `[OPEN]` and probably not worth a kind.

### 3. Edge (absorbs Sharpness, Breach, Piercing, Projectile Protection)
- **+ bites.** Edge or tip: extra damage, armour bypass or penetration, by hardness against hardness; brittle (flaw).
- **- spreads the force.** The same hardness coupling reversed: a harder surface concentrates force, a padded or broad one spreads it. On a plate or studs it turns projectiles aside (`deflect`). On an edge it is padding (`pad`): less damage, and the rest becomes a stagger (slowness and weakness), so you can take a target alive.
- **Interactions:**
  - The flaw is added on top of forging flaw, so a part forged well leaves room for Edge and a sloppy forge can hit the cap.
  - Edge+ shatters a plate with no fracture resistance and bounces off a Ward plate, which is what Ward's flaw buy-back is for.
  - Deflection is hardness against hardness, the same comparison guns use for penetration (guns spec 3.5): a soft lead bullet glances off, a hard one punches through.
  - Split bolts (Impulse) can drop each bolt under the hardness floor Edge needed.
  - Edge- on a blade is a parry; `[OPEN]` against `Parrying` and just-parry.
- **Gun:** on a bayonet blade only for damage. The round owns hardness, so a gun attunement may not touch it; on a barrel the only gun-owned lever is muzzle speed. `[OPEN]` whether that is Edge or just Spring.

### 4. Bane (absorbs Smite, Bane of Arthropods, Impaling)
- **+ repels the quarry.** Edge, blade or tip: extra damage to a target tag. Plate, lining or studs: the same coupling worn, the quarry's blows land softer (`bane_ward`). The tag is undead or arthropod in the first cut; the table's tag picker stays `[OPEN]` (digest rule 3: a named list, never a free value). `[OPEN]` whether Bane is just Edge with a tag variant.
- **- attracts the quarry: Kin (author, 2026-10-23).** Worn (`kin_hide`): undead notice the wearer from less far away (`LivingVisibilityEvent`), but a quarry that does hit them hits harder (damage multiplier, 0.25 per level). On an edge (`kin_calm`): a struck quarry loses interest in its striker for a while, and hits harder while it lasts. The trade is stealth for danger: the undead leave you alone and are worse when they do not.
- **Interactions:**
  - Silver already has the Smite perk; `[OPEN]` whether the attunement stacks with it or takes the maximum.
  - The arthropod slow adds to Chill and Cling slows; whether three slowers multiply or take the worst is `[OPEN]`.
  - Impaling as a spear head that pins a struck target is `[OPEN]` against `Tether`.
  - A calmed quarry still attacks anyone else; Kin changes who it hunts, not whether.
- **Gun:** none.

### 5. Ward (absorbs Protection, Unbreaking)
- **+ soaks.** Plate resists fracture (costs mass). Any other role soaks wear from use.
- **No mirror.**
- **Interactions:**
  - The flaw a Ward plate buys back lets a hard but brittle metal (cast iron) be worth wearing as armour at all.
  - It soaks wear from use, not damage from misuse: a blow too hard for the material still lands. `[OPEN]` whether that is readable.
- **Gun:** on a receiver or barrel it lowers flaw, which raises the chamber's effective margin under the rating (guns spec 3.2); on a barrel it plays the role of a liner (13.5).

### 6. Yield (absorbs Looting, Fortune, Silk Touch)
- **+ more, finer.** A head or blade recovers more from a block or a kill (compendium 1.4).
- **- intact.** Fracture control: the block comes out whole, slower to dig.
- **Interactions:**
  - The mod sorts by particle size, so an intact lode and a crushed one reach a crusher as different inputs; Yield+ gives finer pieces and Yield- bigger ones.
  - `[OPEN]` against the armed-skeleton spec: Yield+ could be how a skeleton's gun comes back intact rather than as scrap.
- **Gun:** none.

### 7. Tide (absorbs Aqua Affinity, Depth Strider, Respiration, Riptide)
- **+ thrust.** A trident head pushes against fluid drag.
- **- cuts or seals.** A tool head works through a fluid without being slowed; a sole cuts drag on the wearer; a helmet lining seals, holding breath longer.
- **Bad pairing:** a negative Tide on armour that has nothing to cut does nothing.
- **Interactions:**
  - It reads drag, not "water": it also helps in honey, works in rain, and in lava if the head survives 1200 Tu.
  - A cut-drag sole under a thrust trident subtracts from the thrust: they push against the same drag.
  - A seal also turns aside the scalding steam a weeping boiler gives off first (digest, pressure holders).
- **Gun:** none.

### 8. Recall (absorbs Infinity, Loyalty)
- **+ pulls back.** An arrow or thrown head that did not stay comes back.
- **- anchors.** The thrown weapon plants itself and holds a struck target (the pin Bane's Impaling wanted).
- **Interactions:**
  - Recall reads the tip for magnetism (`curie_temperature`: iron, steel, cast iron, netherite). A flint tip never returns, and a part heated past its Curie point stops being pullable, so a Heat+ arrow does not come home.
  - The returning weapon pulls whatever it is lodged in; a small mob flies to you, a heavy one holds it (`BodyMass`).
- **Gun:** on a receiver or bolt it recovers spent cases, but only steel ones; a brass case does not return. Gun-owned (ejection), the case is not touched.

### 9. Spring (absorbs Power, Quick Charge)
- **+ stores more.** Limbs or a spring hold more draw energy at the same draw time.
- **- releases faster.** Less energy, quicker cycle.
- **Interactions:**
  - Energy becomes speed through the arrow's mass, so + helps a light shaft far more than a heavy one. Same formula as a gun's muzzle speed.
  - `[OPEN]` whether the quick side multiplies with the player's Haste.
- **Gun:** on a `main_spring` + is a stronger spring (work scales with yield squared, still bounded by the rating); - shortens the recock (`GUN_SPRING_COCK_MS_PER_WORK`). On a repeater - raises cycle rate, and cycle rate is barrel heat.

### 10. Hold (absorbs Binding Curse, Swift Sneak, Soul Speed)
- **+ clamps.** A grip or strap cannot be dropped or swapped: it cannot be disarmed. A sole bites the ground.
- **- frees.** A leggings lining cuts crouch drag.
- **Bad pairing:** a clamped part keeps burning you if it is hot.
- **Interactions:**
  - Bound to a Heat+ handle on the same tool, it is a trap that cannot be put down.
  - The sneak gain is capped by the mass of what else is worn: a Ward chestplate under freed legs is slower than the tooltip implies.
  - Soul Speed as a sole coupling to soul soil: `[OPEN]` whether soul soil holds trapped excitation like sculk (compendium 1.2); if so, sculk nearby could mute or boost it. Needs a compendium ruling.
- **Gun:** on a grip it keeps the gun in hand through a skeleton's hit, and through a barrel too hot to hold.

### 11. Conduct (Channeling)
- **+ draws.** A conductive head draws lightning.
- **- sheds.** A part that diverts a strike round the wielder.
- **Bad pairing:** in a storm the + wielder is holding the tallest conductor.
- **Interaction:** the strike runs through the wielder unless the grip or handle material insulates (`Insulator`, `Shock`): a rubber or wooden handle saves you, a bare metal one does not.
- **Strike:** a drawing head also lands a bolt on whatever its blow hits, and on the ground a sledge slam strikes, every time in a storm under open sky (`ConductKinds.call`). The same grip rule spares the wielder the bolt they called; anyone else near it is not spared. `[OPEN]` always-on, no cooldown, until played.
- **Gun:** none (a metal barrel is a rod already).

### 12. Mend (Mending)
- **+ repairs.** Any role. The part absorbs the holder's XP while held and repairs.
- **- pays out.** The flow reversed: durability becomes experience. A kill with the weapon pays XP and wears it a little (`tithe`), never to nothing. It is wear from use, not from uptime.
- **Bad pairing:** it drains the XP you wanted for the table.
- **Interactions:**
  - A Siphon blade (extra XP on a kill) feeding a Mending pick is a closed XP loop, and neither tooltip says so.
  - A Tithe weapon and a Mend tool in one kit are the same loop made on purpose: scrap the blade's wear into XP, spend it on the pick.
- **Gun:** probably leave it off guns. A receiver that mends erases the "worn machine is evidence of a specific mistake" the wear rule wants. `[OPEN]`.

### Modifier: Unstable (was Vanishing Curse)
- A wide band and the pole dies with its holder.
- **Interaction:** a part left in a chest keeps its pole; only one carried through a death loses it. A rental for the base-bound.

### Parked: Luck of the Sea, Lure
- There is no fishing-rod modular type, so no part to attune. Added when a rod is.

## 3. Novel effects (not in vanilla)

These come from what poling actually is, not from the vanilla list. A poled ferroelectric is piezoelectric and
pyroelectric, and a polarized body can store and sense. Each is a row over a new kind; none keys on identity.
Proposals, brainstormed 2026-10-23.

### 13. Piezo
- **+ generates.** A struck part pushes a voltage pulse (`Eu`) into a conductor at the strike. A hammer head is a hand generator; a part under load charges a battery.
- **- senses.** The same part reports a strike or a temperature change instead of making power: a sensor part for the measurement layer. It controls nothing (digest: measurement, control and actuation are separate).
- **Minecraft lens:** none. This is the reason ceramics are poled in the first place; the mod's electricity is the only dependency.
- **Hard-rule check:** electricity is not the universal currency, and this is a small source, not a replacement. A creative source for `Eu` already exists, so nothing new is needed first.
- **Decision:** a hammer head near the bench charges a jar slowly by hand (manual stays possible, at tiny rates); a sensor part reads cheaply but needs wiring.
- **Interactions:**
  - The pulse needs a conductor at the strike point. A strike on an insulator is wasted, so the anvil's or the jar's material matters.
  - A part held in a hot hand or hot furnace-side loses polarization toward its attuning point, and the pulse falls with it. Heat is the clock here too.
  - The negative side on a part that is also Heat-attuned elsewhere on the tool has nowhere to report to; coupling by sign (section 1) applies.
- **Gun:** on a bolt or striker the cycle's strike is the pulse. That powers the thermal scope (guns spec 13.5) from the shot itself: the first rifle that needs a power source is fed by its own recoil.
- **Open:** pulse size per strike; whether a strike on a non-conductor is a hazard or just lost.

### 14. Dowse
- **+ points.** A needle or tip role leans toward the nearest block with a chosen property (magnetism, density, conductivity), within a short range. It points and does nothing else.
- **- inverts.** It points away; a repelling needle is a cheap way to find the far side of a vein.
- **Minecraft lens:** vanilla already has a compass that tracks a lodestone, a physical precedent for a needle that reads a block. This generalises it to a property, so a pack's ore works with no row.
- **Hard-rule check:** instrumentation is a yield technology, not a key: it unlocks nothing and does not enter any block's result (the world resolves on truth). The property is picked from a short named list at the table, never a free value.
- **Decision:** a magnetic needle finds iron-bearing rock cheaply and misses everything else; a different property finds different rock. Choosing the property is the player's call and costs a pole each.
- **Interactions:**
  - A needle of a magnetic material above its Curie point stops pointing (`curie_temperature`): a hot needle is a dead one.
  - Neighbouring metal in the same kit pulls a magnetic needle off true. The kit around the part matters.
  - It reads the nearest block, so a vein behind a thicker wall of the same property reads as the wall.
- **Gun:** none (a gun's sensing is the sight's job).
- **Open:** range and how many blocks it samples per tick; whether it needs a screen readout (`Readout.figure`, truth tier) or only the needle.

### 15. Reservoir
- **+ stores.** A part holds XP levels as polarization. The player charges it at the table and draws on it later; Mend draws from it first, which closes the Siphon loop (a Siphon blade feeding a Mending pick through the same part).
- **No mirror.**
- **Minecraft lens:** the Bottle o' Enchanting is already a charged storage medium (compendium 1.1, leap 2). This is a second, in a part you carry. It stays tied to the player lineage and Villagers as the only producers: the part stores, it does not make.
- **Hard-rule check (blocking):** XP is a new energy type, and every new energy type gets a creative source first. Reservoir is blocked on a creative XP source under `machine/creative/`.
- **Decision:** a bigger reservoir is a thicker part, more mass and a wider band, and the capacity is held on the part, not in a counter.
- **Interactions:**
  - Heat past the part's attuning point does not just erase the pole: it dumps what is stored. A reservoir on a hearth is a hazard, and the hazard is worth a tooltip.
  - A full reservoir in a part that is also Mend-attuned elsewhere pays for repairs without draining the holder.
  - Death costs the stored XP like any held charge; `[OPEN]` whether it drops as orbs.
- **Gun:** none.
- **Open:** capacity per material; how it is read (`Quantity.FIGURE`, truth tier).

### 16. Charge
- **+ builds.** Strikes charge the part. One release is a big burst and the pole drops partway, so it must be set again; it is the effect with a cost that arrives later.
- **- bleeds.** A part that sheds charge into the wielder's hand harmlessly: a ground strap.
- **Minecraft lens:** a charged creeper's blast or a lightning strike could charge it. `[OPEN]` whether that is a dependency on Conduct or a separate pickup.
- **Hard-rule check:** wear is from misuse, not uptime. Charge builds from *strikes* (use) and bleeds only by the release it was built for; overcharge is the misuse, and it arcs onto the wielder (`Shock`).
- **Decision:** a quick small release against a slow big one: a part that is released early is weaker, and one held too long arcs.
- **Interactions:**
  - The wielder's insulation decides whether an overcharge hurts: a rubber or wooden grip (`Insulator`) saves them, a bare metal one does not. The same material figures Conduct reads.
  - A Charge part next to a Piezo part in one kit couples by sign (section 1): like signs reinforce, opposite signs flow.
  - Rain or a wet hand bleeds it; it is a material and world interaction, not a rule.
- **Gun:** on a coil launcher it is a single big shot: the stored polarization is the charge. `[OPEN]` whether that is a launcher option or an attunement.
- **Open:** release curve; whether a part can be released into a neighbour.

## 3a. Build status (first cut, 2026-10-23, compiled, unplayed)

All twelve vanilla-derived effects exist as rows in `data/feedback/attunement/` with runtime kinds in `modular/attunement/<Effect>Kinds.java`.
Every number is `[OPEN]` until played. What each built, and what the first cut left out:

- **heat:** + `ignite_target` (head, blade, tip), `burn_wielder` (handle, grip), `ignite_attacker` (plate). Mirror "Cold": head, blade and tip `freeze` (each blow stacks freezing as powder snow does, capped a little past the point it hurts); lining `insulate`, fire damage turned away by how poor a conductor the lining is (copper barely helps). Left out: plate thermal mass, the forging grip (`Protection` only reads `Insulator` items, no clean hook), Frost Walker, bow string flame.
- **impulse:** + `shove` (head, blade, tip, scaled by `BodyMass`), `recoil` (handle, grip; the wielder is kicked back when they strike), `kickback` (crossbow stock: the shooter is thrown back along the shot and the target the other way, the force of nature; aiming down is a launch, a Damp lining takes the fall), `reflect` (plate, studs). Mirror "Pull": head, blade and tip `pull` (the blow draws the target in), crossbow stock `drag` (the shooter is pulled after the shot and the target back toward them), plate and lining `damp` of explosion and fall damage. Left out: Density and Wind Burst on the maul, split. Untested risk: `reflect` hurts the attacker inside the damage event.
- **edge:** + `extra_damage` (head, blade, tip, flaw +1). Mirror "Spread": plate and studs `deflect` projectile damage by material hardness (flaw +1, a hard plate is brittle); head, blade and tip `pad` (damage cut 25% per level, plus slowness and weakness). Left out: shield face and rim, blade parry.
- **bane:** + `bane` (head, blade, tip) against undead or arthropod tags. Mirror "Aegis": `bane_ward`. The tag picker is still `[OPEN]`; the first cut covers both tags at once.
- **ward:** plate `fracture` flaw -1; every other role `soak`, which shortens wear through `ModularStacks.cap` (`WardKinds.soak`). No mirror.
- **yield:** + `fortune` (head, blade, tip) on block drops and mob drops. Mirror "Intact": head and tip `intact`, the block drops itself. Skips block entities and blocks that already drop themselves.
- **tide:** + side: head and blade `douse` (a blow puts the target out and hurts what water hurts, via `isSensitiveToWater`: blazes, endermen, snow golems; the held tool puts out the holder, and used beside a fire it puts the fire out); Riptide thrust skipped; `walker` on a plate (boots only, checked in the handler, flaw +1): lava source under the feet cools to obsidian that runs again after `WALKER_TICKS` (Frost Walker for lava; the pending-melt list is lost on restart, the rock stays). Mirror "Slip": `seal` (helmet lining, `OXYGEN_BONUS`), `stride` (boots plate, `WATER_MOVEMENT_EFFICIENCY`), `affinity` (tool head, tip, `SUBMERGED_MINING_SPEED`). A role takes one kind, so helmet plate and boots lining do nothing.
- **recall:** + `recall` (head, tip) on a thrown weapon, only if the head material has a `curie_temperature` and is not heated past it at the throw. Mirror "Anchor": `anchor`, heavy slowness on the struck target. Left out: bows, the `BodyMass` drag, the jump lock. One added line in `Throwing.java`.
- **spring:** + `store` (bow limbs, crossbow prod): arrow velocity x(1 + 0.1 per level), flat. Mirror "Snap": `release` (bow and crossbow string): draw and load time cut. Untested cost: handlers run per draw tick and per arrow spawn.
- **hold:** + `clamp` (handle, grip, binding): cancels `ItemTossEvent`. Mirror "Free": `limber` (leggings lining, `SNEAKING_SPEED`) and `slip` (handle, grip, binding: a landed blow may drop the weapon, with flaw -1 as the upside; a clamp elsewhere still cancels the drop). Left out: armour clamp (no clean hook), hotbar swap block, Soul Speed.
- **conduct:** + `draw` (head, blade): in a thunderstorm under open sky, a small per-second chance a bolt is spawned at the holder, harmless if the handle or grip insulates (`Exposure.conduction`). Mirror "Ground": `shed` on plate, 25% of lightning damage per level, the strike cancelled at a summed level of 3. Left out: tool types with no handle or grip part are not checked for insulation.
- **mend:** + every role: every 40 ticks a held or worn damaged stack takes 1 XP point and repairs `2 x level` durability. Mirror "Tithe": every role `tithe`: a kill with the weapon pays `3 x level` experience and wears it `1 x level`, never to nothing.
- **piezo** (built 2026-10-23, compiled, unplayed): + `pulse` (head, second head, blade, flaw +1): a left-click on a Leyden jar with it injects `PULSE_PER_LEVEL` charge (`LeydenJarBlockEntity.inject`), no further than 5 V, so by hand and at a tiny rate; a blow on anything else is lost (ruled 2026-10-23: lost, not a hazard). Mirror "Gauge": `sense`, a landed blow reads glancing / solid / heavy in the action bar, free words, no wiring. Left out: heat eroding the pulse (the heat hook is still open), the data-link sensor, the gun cycle.
- **dowse** (built, unplayed): four effects, `dowse_magnetic`, `dowse_dense`, `dowse_conductive`, `dowse_flammable`, the property chosen by which effect is attuned, not by a picker (so each costs a pole). + `point_<property>` on a tip, mirror "Shun" `away_<property>`. An arrow in the action bar every second, relative to where the holder faces, at the nearest such block within 12; the property is read off the material table (magnetic; density from 8 Mu/mB; has conductivity; flammable trait). A magnetic needle whose part material has a Curie point dies once the stack is heated past it. Left out: neighbouring metal pulling it off true, a distance readout.
- **reservoir** (built, unplayed): `reserve` on handle, grip, binding, plate, lining, pommel, stock; no mirror. Holds 60 XP points a level in `feedback:reservoir_xp` on the stack. Sneak-use in the air stores 20 of the holder's points; sneak-use on the new **Creative Experience Source** (`CreativeXpBlockEntity`, operator tab, the first thing built for XP) fills it. Mend draws on it before the holder and Tithe fills it before orbs drop (`MendKinds`). Left out: heat dumping it, an exact reading (only the part's own state), the table slot to charge it (the table's menu is shared core).
- **charge** (built, unplayed): + `build` (head, second head, blade, flaw +1): each landed blow adds charge in `feedback:charge_stored`, held safely to 8 a level; sneak-use arms a release that spends it all as extra damage in the next blow (0.5 each). Past the safe amount every blow arcs through the wielder (`Shock`, MV) unless the handle insulates (`ConductKinds.insulated`). + `capacitor` (plate, lining, flaw +1): each worn level adds, times its material's `ampacity` (copper 1; none adds nothing, so a plate of insulator is dead weight), a level of capacity to the held tool's bar (safe amount and maximum both scale), but only on a tool that already has `build`. Mirror "Drain": `bleed` on handle or grip drains charge each blow and means no arc. A head that also has Conduct's `draw` spends `BOLT_COST` charge on the armed release to call a bolt at the victim in any weather (the insulated-grip rule applies). Left out: lightning pickup (`[OPEN]` still), the pole dropping partway after a release, gun rows, release into a neighbour.
- **Not built:** Coupled parts, Unstable, Bane's tag picker, all gun rows, Luck of the Sea and Lure.

## 4. Open

1. The effect-per-role table in section 2 is a first cut; roles a type does not have simply never appear.
2. Names: keep vanilla names where a role maps one-to-one, or new ones as "Protection" became "Ward".
3. Bane as its own effect or an Edge variant.
4. Mending and the curses: early, or after the XP-drain design (XP as a stored charge needs a creative source first, digest rule).
5. Gun attunements: which rows to ship, and whether they sit on the gun table or the enchanting table.
6. Soul Speed and anything touching sculk: a compendium ruling first.
7. Hold and Spring are the weakest merges; split them back if they read as three feelings under one name.
8. Reservoir needs a creative XP source first; Piezo needs a check that the electric network can take an item-held pulse; Charge's arc through the wielder needs `Shock` to see a held part.

## 5. Trap (what was tempting)

The first idea was one handler per vanilla enchant, 42 of them, each a dressed-up stat. Most vanilla enchants are the same
three or four coupling ideas on different parts of a thing: Fire Aspect and Flame are one effect on an edge and on a string.
The second was to hand every enchant a clever twist. The test used here for an interaction is that it is true of the numbers
the mod already has (a hardness, a conductivity, a Curie point, a mass), so it falls out of the system rather than being scripted in.

## 6. Second wave (brainstormed and ruled 2026-10-23, author)

Looser than section 3: these need not be physics, only cool and a real play choice. Each side of an effect is a real trade
(a good role and a bad role), and a mirror that would almost never be worth picking is given a different job, not the plain
opposite. Every number `[OPEN]` until played. None is hard.

### 17. Echo
- **+ echoes.** Weapon: the blow lands again a beat later on the same target, reduced (the good side). Armour: the hit you take echoes, so the plate takes MORE durability damage per hit (the bad side, no damage change).
- **- delays.** Armour: the damage you take arrives over the next few seconds instead of at once (the good side). Weapon: the main swing literally takes a few seconds to land (the bad side).
- So + is good on weapons and bad on armour; - is good on armour and bad on weapons. Pick the side by role.

### 18. Grudge (shares Charge's meter)
- **Same stored meter as Charge** (one number on the part, not two). Charge fills from blows you land; Grudge fills from damage you take, and the next blow releases it as bonus damage, exactly as Charge's release does. A kit with both fills one meter two ways.
- Only liked on those terms (author). If it cannot share the meter cleanly, do not ship it.
- **Mirror:** Calm, stored grudge bleeds away instead. `[OPEN]`, may be none.

### 19. Momentum
- **+ charges.** Damage and knockback scale with how far you ran in a straight line before the blow (cap `[OPEN]`). Strong spear synergy: a spear jab after a sprint is the best hit.
- **- braces.** The part is stronger the longer you have stood still; moving breaks it.

### 20. Pack
- **+** Stronger for each ally within a few blocks. **-** Lone: stronger with no ally near.
- "Ally" = same team (scoreboard team), tamed by the same owner, or a tag. **Make a note: feed it from FTB Teams** (and similar) when present, as a soft compat hook; never a hard dependency, never an id check.

### 21. Hunger
- **+ feeds.** A kill restores food and saturation. Costs a little hunger per blow.
- **- drains.** Each blow takes hunger AWAY from what it hits (a player's food level; `[OPEN]` for a mob: Hunger or Weakness status).

### 22. Mark
- **+ marks.** A hit marks the target: it glows and the next blow on it, from anyone, does extra. For parties.
- **- taunts** (author said the old Veil mirror needed work; this is the proposal, `[OPEN]`). The part marks its WEARER: hostile mobs in a short range retarget onto the wearer. A tank plate that draws aggro for the spotter. Costs brittleness (positive `flaw`).

### 23. Bloom
- **+ grows.** A hoe or shovel part: struck farmland or grass advances crop growth or spreads grass in a small area. Cooldown per swing.
- **- reaps** (not blight; blight is rarely worth picking). Takes growth out as yield: harvests ripe crops in the small area and replants them.

### 24. Bait
- **+ lures.** Holding it draws passive animals toward you. **- scares.** Animals and weak hostiles flee. Short radius, cheap pathing.

### 25. Wager (a gimmick, on purpose)
- **No mirror.** A coin flip per hit. Weapon: the blow does 2x damage, or HEALS the target for your attack damage. Armour: an incoming blow does 2x damage, or is nullified entirely.

### 6a. Also approved from the first wave (not yet built, no ruling beyond "yes")
- **Lode** (magnetism acts outward; draws or repels metal-bearing drops and projectiles; gated by `curie_temperature`).
- **Ring** (sound; a struck part rings so sculk hears it further, - damps it).
- **Cling** (friction; + grips, - slicks; numbers from the material friction figure, slime best until rubber; split from Hold's clamp).
- **Gleam** (light; + part emits light while held, - dims it; needs a vanilla hook check for the mirror).

### 6b. Armour for weapon-only effects (ruled 2026-10-23, author: every effect gets both a weapon and an armour face)
Audit found twelve effects with weapon roles only: Bait, Bloom, Dowse (x4), Hunger, Momentum, Piezo, Recall, Spring, Yield. Armour roles are `plate`, `lining`, `studs`; a kind that must act only on one slot checks the equipment slot in its handler. Each gets an armour face that is a real play choice, not a stat. Every number `[OPEN]`.
- **Bait.** + Pied piper: a worn part lures animals (as in hand, no item needed). - Scent-free: weak hostiles keep their distance.
- **Bloom.** + Green boots (a boots `plate`/`lining`): walking grows crops and grass underfoot, a step-count cooldown. - Reap boots: walking over ripe crops harvests and replants them.
- **Dowse.** Worn on a helmet or chestplate part: the same needle readout (action bar or the weapon's existing readout), same property per effect, - inverts it. Dowse armour is "a worn instrument".
- **Hunger.** + Reserves: worn parts cut hunger drain from sprinting and fighting. - Famine thorns: whoever hits you loses hunger (a player's food) or gets Weakness (a mob).
- **Momentum.** + Legs/boots: the longer you run straight, the faster you go, to a cap, reset on stopping or a sharp turn. - Stance: standing still builds damage reduction on any piece, moving resets it.
- **Piezo.** + A struck plate sparks: the hit you take arcs onto the attacker (`Shock`), harder for a harder hit. - Soak: the part absorbs the spike of a big hit (hits over a threshold are cut, the part wears). If no good mirror, omit it.
- **Recall.** + Rewind: sneak-use on a worn part snaps you back to where you stood a few seconds ago; long cooldown and a little wear per use. Mirror only if a good one appears; otherwise none.
- **Spring.** + Coil: hold sneak to compress, jump to release a high launch (the longer the hold, the higher, capped); on boots/leggings. Mirror only if good; otherwise none.
- **Yield.** + Scavenger: worn parts boost the experience you pick up. Mirror only if good; otherwise none.

### 6c. First-wave four, ruled to build (2026-10-23, author), each with a weapon AND an armour face
Numbers `[OPEN]`. Rows: 26 Lode, 27 Ring, 28 Cling, 29 Gleam.
- **Lode (magnetism).** Gated by the part material's `curie_temperature` (a hot part goes dead). Weapon `head`/`tip`: + the drops of what the blow kills fly to the wielder, - they are thrown away from them. Armour: + an item magnet (loose item entities in a short radius drift to the wearer), - a repeller that turns metal-bearing projectiles (arrows with a metal tip, thrown tridents) aside.
- **Ring (sound).** Weapon: + a landed blow rings, so sculk sensors and mobs hear it from further out; - the blow is damped and silent to sculk. Armour: + steps and hits taken ring (sculk and nearby mobs notice you from further), - silent: steps and hits do not register as vibrations. Ring length from hardness and density figures; no ids.
- **Cling (friction).** Numbers from the material's friction figure (slime best until rubber). Weapon: + a landed blow sticks, slowing the target a few seconds; - slick: the target slides, knocked further along the ground. Armour (boots `plate`/`lining`): + grip, no slipping on ice and keeps speed uphill; - slick soles: ice-skate speed on any ground, poor stopping. Split from Hold: Hold clamps a grip, Cling is a friction figure.
- **Gleam (light).** Weapon/armour +: the holder or wearer carries light (cheapest sound way: a temporary vanilla `light` block at the holder, moved and cleaned up, including on logout and death). - dims: hostile mobs lose track of a wearer or wielder at range in low light (cancel retargeting past a short distance when the holder's own light level is low). Per-tick work gated by tickCount % N.
