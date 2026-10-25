# Skill trees: definitions (2026-10-24)

Seven trees, one pool each. No tree is a weapon, stance or body-part tree. Each tree covers the same
domains; the tree is defined by **how** it uses them. The data in `src/main/resources/data/feedback/skill_tree/`
follows this file; if they disagree, fix the one that is wrong.

## The domains

Every tree should have something in each domain, or say why it does not.

- **Gun**: rounds, barrel heat, cycle, ammo, the gun as a body.
- **Parry**: the weapon guard, its window, wear, the answer.
- **Fist**: empty hands and feet: chains, kicks, falls.
- **Weapon**: the held weapon's shape, family, reach, heft.
- **Grip**: holding a body, throwing, reeling, swinging on a line.
- **Movement**: rolls, walls, vaults, slides, falls.
- **Support**: healing, stamina, self-buffs, recovery. Survivability.
- **Status**: blinds, slows, pins, burns, marks; what the target is left with.

## The seven trees

Each tree gives its style, then its nodes by domain. [new] marks a planned cast or support not yet built.

### Brute (overpower)
Uses every domain as weight: the bigger hit, the heavier body, the harder push.
- Gun: pistol whip, heavy butt, chamber, spin, dual draw, muzzle stagger, bayonet, brand (the whip payoffs, one exclusive group).
- Parry: punish.
- Fist: stomp, crush.
- Weapon: cleave, whirl, mace quake, spear vault, hamstring.
- Grip: grab, firm grip, iron grip, throw, bowling, vice, point blank (a held body).
- Movement: quake leap.
- Support: [new] a self-buff that adds weight (armour counts).
- Status: hamstring (the slow).

### Anchor (hold ground)
Uses every domain to stay where you are: taking blows and not moving.
- Gun: braced, braced fire.
- Parry: wider window, quick recovery, roll with it, brace, shield wall, riposte, throwback, answer, tight redirect, hard stagger, reckoning, reprisal, rebuke, return fire, slip (the parry payoffs, one exclusive group).
- Fist: [new] a guard stance that trades movement for a held block.
- Weapon: advance (a shield-type shove).
- Grip: human shield (hold a body in place, it takes your blows).
- Movement: [new] rooted: no roll, but the knockback you take is held.
- Support: settle.
- Status: challenge.

### Veil (act unseen)
Uses every domain from cover: the strike is the one nobody sees coming.
- Gun: dodge charge (a roll through a blow clears the gun's cycle wait), spent-case draw (a reload during a roll takes only the roll's remainder); both under quiet roll, moved from Proficiency 2026-10-24. [new] a silenced or smoke shot that does not give your position.
- Parry: slip (see Anchor; placed there with its parry group).
- Fist: ambush (the blow after a shadow step) is listed under Veil's movement limb in the code; [OPEN] whether it belongs in Fist.
- Weapon: [new] a short blade's unseen cut, the first blow from behind.
- Movement: shadow step, smoke step, quiet roll, slip past, long roll, far roll, wall kick, wall run, wall rebound, wall smash (one wall-kick payoff group).
- Support: vanish (less armour, longer hide).
- Status: pocket sand, thick smoke, grit, long shadow.

### Onslaught (relentless rhythm)
Uses every domain in chains: each blow sets up the next.
- Gun: walking fire, snap volley, running bolt (support: an open bolt kept cycling loses spread and kick), twin reload (support: halves the extra reload time of a gun in each hand).
- Parry: [new] counter stance (each parried blow answered as the next one lands).
- Fist: jab, lunge, sweep, chop, uppercut, pummel (flurry), rising tempo, frenzy, haymaker, kick, breach kick, dropkick, axe kick, hard kick, wall force, wall crush.
- Weapon: blade flourish, feint, draw switch.
- Grip: swing.
- Movement: [new] a chain that continues through a slide.
- Support: catch breath, spring heel, twin reload, running bolt.
- Status: [new] a bleed that stacks while the chain holds.

### Observation (see and reach)
Uses every domain at range or with timing: the one that sees the gap and takes it.
- Gun: called shot, dead eye, coin flip, second mark, round the corner, mark and fire, recoil vault (one coin payoff group).
- Parry: keen eye.
- Fist: none; fists are close.
- Weapon: far end, close work (one reach-stance group).
- Grip: reel in, fast reel, winch.
- Movement: vault.
- Support: patient hand.
- Status: [new] a mark that tells you where a target will be; read by called shot and mark and fire.

### Proficiency (survive)
Uses every domain to keep you alive: the fight is lost when you are dead, so the tree buys time, health and breath.
Root: field dressing, so the first purchase is a heal. Four limbs, each three to five deep (2026-10-24: the tree had 10 nodes three deep against Anchor's 17 five deep, and read as an afterthought on screen):
- Breath: second breath, quick hands, steady lungs, adrenaline, wind back.
- Close calls: last stand, grit teeth, extra i-frames, shake off, break free.
- Heat: hot clear, callused hands, cauterize, sear shut.
- Blows: blood up, breather.

By domain:
- Gun: hot clear (a sneak-reload near cook-off ejects the round unfired). Dodge charge and spent-case draw moved to Veil on 2026-10-24: they are roll-into-shot tempo, not survival, and Veil is the tree that earns on dodges.
- Parry: last stand (a near-death roll that swallows a blow counts as a free parry); wind back (a parry landed under 30 stamina gives 15 back).
- Fist: blood up (three empty-hand hits in a clean run, three seconds apart at most, heal a heart; a hit taken breaks the run).
- Weapon: breather (a kill with a modular weapon gives stamina by its mass, 10 a Mu, 10 to 40).
- Grip: break free (Shake off first throws off ropes others put on you, a grab or a hooked weapon, and staggers the holder by mass; your own grapple is kept).
- Heat: callused hands (bare hands take the scald band, under 300 Tu, through `Protection.handlesUpToTu`); cauterize (press a hot piece from either hand to the wound: the `HotContact` band sets the heal, three hearts at burn, six at sear, and you pay the band's burn and fire; the piece loses 200 Tu. The mid-fight heal, priced in a forge you ran and a fire you must put out); sear shut (a sear cauterize also clears the worst harmful effect).
- Survival casts: field dressing (held: stand still, heal a little each tick while stamina drains, about four hearts a full bar; moving or taking damage ends it, so it is for between fights. It healed six hearts at once until 2026-10-24, which was free health mid-fight). Grit teeth (four seconds of damage cut by a share of worn armour mass, 15% bare to 55% in plate, rooted three tenths slower).
- Movement: extra i-frames.
- Support: second breath (cast: refills stamina to 60, less under heavy armour via `BodyMass.armour`, down to 30; rearms on a landed blow. It rearmed on the clock until 2026-10-24, a cost paid by waiting), quick hands and steady lungs (shorter clock rearms; steady lungs was "second wind" until it collided with second breath), adrenaline (stamina regen doubled while under 30).
- Status: shake off (cast: beats out fire if burning, else clears the one harmful effect with the most time left; rearms on a stagger. Pairs with cauterize's fire).

Proficiency has 17 nodes: five casts (field dressing, second breath, cauterize, shake off, grit teeth), eight riders, four supports. World reads: hot-contact bands and item heat (hot clear, callused hands, cauterize, sear shut), armour mass (second breath, grit teeth), weapon mass (breather), tethers (break free). [OPEN] whether field dressing should read something (a bandage item, worn armour) too.

### Chaos (break things)
Uses every domain to wear, burn or disrupt: nothing stays whole once it meets this tree.
- Gun: overheat, scorch, muzzle flash, disarm shot, marked rounds.
- Parry: pry open.
- Fist: rending heel (armour wear).
- Weapon: searing strike, forge slam.
- Grip: [new] a throw that breaks a stance and leaves a body rattled.
- Movement: slide (trips and scatters).
- Support: steady barrage (rearm).
- Status: pin down, dazzle, concussive, wide pin, held pin.

## Points

Each tree earns from one physical act, in `SkillEarning`:

- Brute: melee hits (blade, mace, spear).
- Anchor: landed parries.
- Veil: dodges that cancel a blow.
- Onslaught: fist hits (empty hand).
- Observation: gun hits at six blocks or more.
- Proficiency: surviving a blow struck by something, per attacker; falls, fire ticks and the killing blow pay nothing. It paid a little on every landed hit until 2026-10-24, which had nothing to do with surviving. [OPEN]: whether standing in front of a weak mob farms it faster than the per-attacker decay allows.
- Chaos: kills by the player, by any means, as a bonus on the hit.

## Gaps and what needs building

- Every tree has one trunk (its root), and every other node hangs off it through a parent in the same tree. A node with no parent is a bug: it floats in the screen. Trunks: Brute grab, Anchor wider window, Veil shadow step, Onslaught jab, Observation called shot, Proficiency field dressing, Chaos pin down.
- Support is thin across trees. Proficiency is the only survivability tree, so healing and stamina sit there.
- Every remaining [new] item needs a Java cast or support. Built so far: all of Proficiency (field dressing through breather), and the Equip tab.
- The Equip tab is the loadout: three cast keys on top, every castable technique you own (bought or gear-granted) below. Pick a key, then a technique.
- Status and bleed need a stacking figure; none exists yet.
- The exclusive groups are kept inside one tree each (whip payoff, riposte payoff, wall-kick payoff, coin payoff, reach stance).
- Some hard prerequisites from the old trees were dropped when the old trees were merged (for example, chamber no longer needs quick hands). The trees' root sets are therefore wider than before; check the cost ladder when playing.
