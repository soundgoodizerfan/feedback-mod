# Test and input checklist (live)

Kept current by Claude. Items are added when built, removed when the author confirms. Everything listed is **compile-only, not played** unless stated. Nothing has been deployed since the combat work began (`./build-and-deploy.sh` not run).

## Test (in game)

### Already merged, unplayed
- Combat feel: `/feedback feel light|heavy|custom <flash> <roll> <fov> <sparks> <heavy>` (no freeze).
- Parry (`V`, clashes with gun fire mode `V`) / roll (left alt): whiff thud + puff, press swish, deny click, ready tick, roll whoosh, success flash; `/feedback combat` readout; arrow redirect off a skeleton (biggest risk).
- Swept hitbox: several mobs in one swing, empty-swing hit with sound, spear narrow vs maul wide, block stops it.
- Gun stance `Z` (hold): gun swings as a bludgeon, dual wield each hand.
- Off-hand strike: right-click with an off-hand sword (empty/weapon main hand), chest still opens, shield still blocks; "Off-hand attack" key.
- Tether: two harpoons in one mob, one detaches, bonus stays x1.25.
- Material perks: silver smite, blaze rod burn without gloves, prismarine impaling, echo shard glow; silver ingot/plate/block look.
- Variant textures: mace, trident, pitchfork, katana, dagger, machete, prospector, miner.

### Merged overnight (2026-10-22), unplayed
- Parry/roll cues (swish, whiff thud+puff, deny click, ready tick, roll whoosh).
- Pistol-whip free shot: hold Z, swing a loaded gun at a zombie, it staggers and the gun fires once; empty/jammed gun: no shot; light gun: no stagger.
- Grayscale sprites: machines (lift, valve, steam engine, galvanic cell, bimetallic strip, clutch collar, leyden jar, charger, condenser, pressure vessel, electrostatic generator) still look tinted copper, in inventory too; overspeed trip especially. Coil blocks unchanged (`copper_coil_side.png` kept, composite).
- New perks: lead poison, slime bounce, honey slow, ice/blue ice freeze, sculk XP on kill, ender pearl blink on kill, breeze rod featherlight (swing faster, balance, real mass unchanged); thrown weapons now carry perks (smite/impaling too); off-hand blows use off-hand perks.
- Procedural poses: parry arm raise (dagger fast vs greatsword slow/low), roll tuck, stagger recoil, first-person hands. If tipped backward or arms wrong way flip `TUCK_DEG` / `ACROSS_RAD` in `CombatPoseTuning`.
- Grapple (craft: iron plate, iron rod, string, pattern `PR_`/`_RS`/`__S`; or equipment tab): wall pulls you and releases within 1.8 blocks; light mob (pig) flies to you, heavy (golem/ravager) pulls you; stalled pull parts the line in about a second (20 wear); sneak/use again releases; rope visible in flight and reeling; harpoon rope no double line.
- Parry key is now `G` (R reload, V fire mode, U unload taken). Buckler/kite folded into the one parry system: buckler parries on `G` (6-tick window, attacker thrown back, 1 wear); kite returns projectiles on `G` only (melee still lands); tower/plain shield no longer parries (block unchanged); passive raise-block behaviour for buckler/kite is now plain (narrow cone / melee cap), kite no longer auto-returns arrows on raise.
- Combat gear (Curios slots bracer/belt/back): craft shapeless, plates + leather: 2 plates+1 leather = Bracer, 1+2 = Sash, 3+1 = Back piece (one plate metal, one strap material; mixed refuses); slots show in Curios screen; bracer widens parry window (up to +2 ticks, `/feedback combat`), sash lengthens roll and shaves recovery (steel +1 i-frame), back piece shortens your stagger. Worn gear invisible (no renderer yet).
- Skills: `/feedback skill grant parry 100` (op) then `K`: tabs, pool, nodes, Learn button; buy `wider_window` (+2 ticks, see `/feedback combat`), `long_roll`/`extra_iframes`; pools rise from parry/roll-dodge/gun hits/melee with diminishing returns on the same target; points persist across death; unlearning token (shapeless paper + ink sac + iron nugget) refunds all, not consumed if you own nothing.
- Kick `X`: shove + brief stagger on a zombie; raised-shield target loses guard (shield on cooldown, heavy feel); iron golem only shoved; miss whooshes; 30-tick cooldown also blocks parry/roll. Grab `C` (needs a glove in the `hands` slot): rope draws, target pinned, can't act, takes 1.25x; release by `C` again, 3 s, 3+ blocks away, rolling, or taking a 4+ hp hit; iron golem not holdable (you get yanked); no glove: deny click.
- Feedbacker (bracer curio; `/give` or craft shaped `PCP/PEP/_L_` = tempered steel plate, copper coil, electromagnet, leather): press parry bare-handed with it worn, a skeleton arrow returns at ~2.5x along your look; ghast fireball returns with no explosion on you, costing much more durability; bracer never destroyed (stops granting at last point); ~4% rare in dungeon/stronghold/ancient city chests, damaged 30-70%; tooltip shows Grants line.
  - Bullets: a gun shot at you while you punch (Feedbacker, in the window, facing the shooter) is negated and a return shot hits the shooter for 2.5x its damage, credited to you; a punched-back shot is never re-parried. A weapon parry no longer negates or staggers on bullets (they land). Test: shoot yourself from a dummy/second player with the gauntlet worn, press parry as it fires.

## Input wanted (assumed answered "agree" overnight; fix later)
- Philosophy §7 guard wording for the world/player split (plan §0.1).
- `V` key clash default.
- All combat figures are placeholders (windows, recovery, mass stand-in, roll).
- Echo shard trait (glow) is `[OPEN]`.
- Which of the ~35 generic variants want art first.
- Breeze rod: balance reads felt mass (head-heavy feel) or real mass?
- `blink` range/trigger; `siphon` flat 3 XP/level edges toward a stat; `cling` overlaps `chill`/`bounce` (cut candidate); ender pearl/slime may not fit a weapon part role (dormant perks?).
- Pistol-whip stagger threshold 6 ticks placeholder.
- `copper_coil_side.png` composite still coloured (needs two-layer model or masked tint).
- Stray stash `preexisting-uncommitted-TODO-condense-preserve` in git stash list (not mine).
- Grapple: no swinging (rope length does not constrain), rope rating/stall figures stand-ins, `Parrying#result` should switch to shared `BodyMass.of`, no recipe-unlock advancement, hooks only bite living entities, art reuses `part_chainlinks.png`.
- Gear: joint rating faked by scaling `ParryStats.mass` (so `/feedback combat` prints scaled mass); back piece mass also shortens bludgeon stagger; only strap form is `leather`; no creative-tab samples/advancement; `grants` set by nothing yet; no curio renderer; Feedbacker + relic loot tables not built.
- Skills: respec refunds everything (ruling pending: materials, craftable); no consumers yet for riposte/wall_kick/gun-fu/melee unlocks; no tether nodes; branch field display-only; gear `grants` not consulted by `Skills.unlocked`; off-hand strikes earn no melee points; screen plain `Screen`+`Opening` not `SkinnedScreen`; award sizes placeholder (parry 10, evasion 8, gun 2/+6 kill, melee 3).
- Merge fixes done by hand: ParryStats fields, Rolling begin combines gear + skills, Creative tab has both grapple and token.
- Kick/grab gaps: no leg-kick pose (arm swings), no boots figure, held mob AI not suppressed, held player's client may fight pin, glove rating ignored, hold state in a static map, kick has no phase.
- Feedbacker gaps: Sable bodies not punched unless a Projectile, no repair route, recipe result floats hand-copied from the gear bake (go stale), unverified: recipe `components` result, loot `set_components` with the gear codec, `durability(400)`+`stacksTo(1)`, fireball keeps acceleration after punch; assumption: a weapon still parries melee, impact handler wins for projectiles.
