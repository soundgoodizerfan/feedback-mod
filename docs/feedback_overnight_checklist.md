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
