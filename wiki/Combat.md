# Combat

Fighting in Feedback is built on the same idea as everything else: **what a blow does comes from physical figures**, not from a fixed list of items. A weapon's reach, mass and balance (see [Tools and Weapons](Tools_and_Weapons.md)) decide its swept hit, its parry and how hard it staggers. A mob's weight is worked out from its size. Nothing is keyed on the name of a weapon.


## Keys

All of these can be rebound under the *Feedback* category in Controls.

| Key | Action |
| --- | --- |
| **G** | [Parry](#parry) |
| **Left Alt** | [Roll](#roll) |
| **Z**, **Y**, **G** | [Cast slots](#casts-and-stamina) 1, 2 and 3. Slots 4 and 5 are unbound until you set them. |
| **C** | Cast radial: tap to cast the selected technique, hold to choose another |
| **B** | Toggle [barehanded mode](#barehanded-mode) |
| **Z**, held | [Gun melee stance](Guns.md#swinging-a-gun) |
| **K** | [Skill tree](Skills.md) |
| *(unbound)* | Off-hand attack |

[Kick](#kick) and [grab](#grab) have no keys of their own: they are skills you slot into a cast slot like any other technique. A jump tap against a wall does the [wall kick](#wall-kick).

## Swung weapons hit what they sweep

A blade, mace or spear swing covers a volume in front of you, as long as the weapon's reach. Its half-width is **0.2 blocks plus 0.6 times the weapon's balance** (a spear is a thin deep box, a maul a wide one). **Everything in it is hit**, nearest first and up to **6 targets**, not only the thing under the crosshair, and a swing at empty air still hits something that was standing in the volume. A block between you and the target stops it.

Each extra target takes the weapon's damage scaled by the swing's charge, as vanilla does, but never a critical hit. Traits and [material perks](Material_Perks.md) apply to each target hit, and every target knocks back a little. The weapon wears once per target landed.

## Parry

Press **G** to open a short window in which a blow aimed at you is turned aside.

**What can parry:** a blade, mace or spear that isn't broken, in the main hand or else the off hand. A **buckler** parries too, and a **kite shield** turns projectiles back (see [Shields](#shields)). A bow, a tool, a gun or a plain shield does not. With a [Feedbacker](Feedbacker.md) worn you can parry bare-handed, and so can a bare hand in a [fighting glove](Combat_Gear.md#glove) (blows only, not projectiles). The Feedbacker's window is a fixed **5 ticks** with **20 ticks** of recovery and a 75° cone, and it punches any projectile back at 2.5× its speed.

**Window and recovery come from the weapon's heft** (its mass, lengthened by its reach):

| Weapon | Window | Recovery |
| --- | --- | --- |
| Dagger (lightest) | about 3 ticks | about 10 ticks |
| Greatsword (heaviest) | about 7 ticks | about 35 ticks |

Heavy and long is a wide window and a long lockout: it sweeps across a lot of angle, and it is slow to bring back when it misses. Latency is covered by up to 2 extra ticks, never more, so ping can't buy a parry. The window only covers a **frontal cone**: 35° either side of your look for a pointlike weapon, up to 90° for a head-heavy one.

**When it lands**, the blow is cancelled before any trait or perk sees it, and the attacker is **staggered**: held back and slowed to 40% for a time set by the impact of their blow against *their own mass*: 16 ticks for every joule of impact per Mu of their mass, kept between 0.2 and 2 seconds. A blow with no figure of its own counts 10 joules per point of damage. A heavy mob stumbles less than a light one. A parried arrow or other projectile is turned back at 1.5× its speed.

**What it costs:** a clean parry takes **1 point** of wear on your weapon. If the blow was heavier than the weapon's joint is built for (60 Work per Mu of its own mass) it takes 4 more for every whole multiple it was over. A parry that misses leaves you in recovery, open. If you press at a bad moment (still recovering, or on the roll's cooldown) you get a small click and nothing happens.

**Parry bonuses**

- **Riposte** (a [skill](Skills.md)): after a weapon parry, your next hit within 1.5 seconds does 1.5× damage.
- **Last Stand** (a skill): a hit that leaves you on 3 hearts or fewer arms it for 5 seconds; the next roll that swallows a blow then counts as a parry, with everything that follows from one.
- A **bracer** from [combat gear](Combat_Gear.md) widens the window by up to 2 ticks.

A bar under the crosshair shows the window and the recovery.

### Shields

- **Buckler:** parries a melee blow whole on **G** (a 4-tick window), throws the attacker back and costs 1 wear. It guards a narrow arc.
- **Kite shield:** on **G**, returns arrows and bolts the way you look. Melee still lands. Raising it alone returns nothing.
- **Tower and plain shields:** block as before and don't parry.

## Casts and stamina

Many [skills](Skills.md#techniques-keys-and-stamina) are **casts**: techniques you slot into one of five cast slots on the skill screen and fire with that slot's key, or with the radial (**C**: tap to fire the selected one, hold to pick). Held casts end when you let go of the key.

A cast needs the skill, to be rearmed (each lists its own rearm: a timer, a landed blow, a stagger, a parry) and enough **stamina**.

Stamina is the one meter in the combat kit. It runs to **100**. A second and a quarter after you last spent any, it refills at **0.6 a tick** (about 12 a second), and refilling costs hunger: a full bar is about 3 hunger points. A wall kick spends 10, a kick spends stamina too, and most casts 15 to 30.

## Roll

Press **Left Alt** to roll in the direction you are moving (forward if you aren't).

- The whole 8-tick roll is **invulnerable**: it cancels every damage event, including fire and other ticking damage, though not the void or `/kill`.
- It slides about **3 blocks** on flat ground.
- Afterwards you're open for **12 ticks** of recovery, then there is a **30 tick cooldown** before you can roll, kick *or* parry again.

A [sash](Combat_Gear.md#sash) lengthens the roll and shortens recovery. Dodging a real blow earns [Veil skill points](Skills.md#earning-points).

### Wall kick

A skill in the Veil tree. Sprint on the ground or roll, then jump; tap jump again in the air while facing a wall, within **1.2 seconds** of the sprint or roll, and you kick up and away from it. It costs **10 stamina** and uses up the momentum, so you have to touch down and sprint again before the next, and it resets your fall. *Wall rebound* keeps the momentum for one more kick off a second wall.

## Kick

A skill (Onslaught tree; gear can grant it) you slot into a cast slot. It kicks whatever is in front of you, low (about waist height, so under a raised shield's face), within 1.6 blocks. You can't kick while holding someone.

- It does **2 damage**: the kick is for position, not killing.
- It **shoves** by the kick's impact against the target's mass, and **staggers** a free target only if the stagger comes to at least half a second. An iron golem is only shoved. The kick carries 1 joule for every Mu of your own mass (armour counts, so a heavy fighter kicks harder), plus 20 for the foot.
- A target with a **raised shield loses its guard**: the shield goes on cooldown (1 to 5 seconds, longer the lighter the target) and the target staggers at least half a second.
- A kick costs stamina and shares the 30 tick cooldown with the roll and the parry. The leg visibly kicks for 7 ticks.
- Skills build on it. **Wall smash**: a target kicked into a wall takes the weight of the armour it wears as extra stagger. **Breach kick**: a target already staggered is knocked down for 2.5 seconds; a steady one only shoves. **Dropkick** and **Axe kick** are casts of their own.

## Grab

A skill (Brute tree) you slot into a cast slot: pressing it takes hold of something within 2 blocks, pressing it again lets go. It is free to cast, so you can always let go. A glove in the Curios *hands* slot ([leather gloves](Hazards.md#gloves) will do) isn't required, but it improves the grab.

A grab is a **short rope** (1.5 blocks) with no reel. The held thing is pinned close, can't parry or roll, and **takes 25% more damage**. The hold ends when:

- you press it again,
- the hold runs out: **3 seconds**, plus 2 more with a glove, plus the grip skills (*Firm grip*, *Iron grip*, *Vice*),
- it gets more than 3 blocks away (you rolled, or one of you was flung),
- you take a single hit of 4 or more damage,
- either of you dies or leaves.

**A thing more than twice your weight cannot be held**: it yanks you toward it instead. A glove raises that to three times. Mass is the same stand-in as the parry's (the size of its box).

**Throw** (a skill): let go of a hold while the held body is staggered and it is thrown past you along your look, farther the lighter it is against you; whatever stands there does the rest. It leaves you recovering for 14 ticks, and is spent until you stagger something. *Bowling* and *Human shield* are casts that need a hold.

## Off-hand weapons

You can **dual wield**. Right-click with a weapon in the off hand and nothing else wants the click (a chest still opens, a shield still blocks) and the off hand swings, by its *own* damage, speed and reach, with its own cooldown, its own traits and perks, and its own wear. There is also an **Off-hand attack** key, unbound by default. The off-hand swing hits one target along your look rather than sweeping, and its blow wears the off-hand weapon. [Guns](Guns.md#swinging-a-gun) work the same way with the stance key.

## Barehanded mode

Press **B** to fight with your fists whatever you are holding: both hands count as empty, and **left click** punches with the main fist and **right click** with the off fist, each on its own recharge. Press **B** again to put the weapons back in play. A fighting glove adds damage to every punch, and the fist skills below scale with it.

Your movement when a punch lands picks the move (each is a separate skill in the Onslaught tree):

| State | Move | What it does |
| --- | --- | --- |
| Standing | **Jab** | 0.6 blocks more reach |
| Standing | **Sweep** | the blow is also dealt across a 3 block, 140° arc at 60% damage |
| Sprinting | **Lunge** | staggers 1.5× longer and leaves you guarded (invulnerable) for 4 ticks |
| Falling | **Chop** | drives the target down and staggers it |

**Chain them**: a Lunge or Sweep within 1.5 seconds of a Jab, or a Chop after a Lunge, hits **30% harder**. The chain is a bonus, not a gate. [Uppercut](Skills.md) is a cast, not a stance.

## Melee riders

Weapon skills that read how a blow landed, never what the weapon is called (see [Skills](Skills.md)):

- **Far End** / **Close Work** (choose one): a blow landing in the outer quarter of your reach hits up to 45% harder the more head-heavy the weapon is; a blow inside half your reach recharges the next swing 4 ticks sooner.
- **Pry Open**: a blow on a target whose weapon was just beaten (staggered, or guard up) binds that hand for about 1.5 seconds, longer for softer or more worn steel. A raised shield is disabled instead. Spent until you complete a parry.
- **Blade flourish** takes 10 ticks off the roll cooldown on a blade hit. **Mace quake**: a mace blow of 5+ damage shoves and staggers every body within 2.5 blocks of the target. **Spear vault**: a sprinting thrust that lands carries you up and over.
- **Draw switch**: swapping to a weapon of a different kind gives the swing back at once, once in a while.

## Tethers

A **tether** is a line that holds a living thing. A [harpoon](Tools_and_Weapons.md#other-tools-other-shapes) barb, a [grapple](Grapple.md) hook and a grab are all tethers. A tethered creature takes extra damage while held, but the extra is the **largest** of its tethers, not the sum: two harpoons in one mob wound it no more than one, and one letting go leaves the other's bonus standing.

## How a hit feels

Landing a parry gives an **impact frame**: a one-to-two-frame white flash, a camera roll and field-of-view pop, sparks, and a sound that's heavier for a heavy blow. The two fighters get all of it; bystanders within 16 blocks get a third of the flash. There is no hit-stop (the world never freezes). Missed windows thud dully, a press you can't use clicks, and a faint tick tells you when you're free again.

Fighters also move: the arm raises for a parry (a dagger fast, a greatsword slow and low), you tumble through a roll, and a stagger recoils the body.

## Commands

Operator-only testing commands:

- `/feedback combat`: prints the window, recovery and cone your held weapon would parry with, and what your worn gear adds.
- `/feedback feel light|heavy|custom ...`: plays the impact feel at the look point.
- `/feedback skill grant <tree> <points>`: gives [skill points](Skills.md).

## See also

[Skills](Skills.md) · [Combat Gear](Combat_Gear.md) · [Feedbacker](Feedbacker.md) · [Grapple](Grapple.md) · [Material Perks](Material_Perks.md) · [Tools and Weapons](Tools_and_Weapons.md) · [Guns](Guns.md) · [Jar Baton](Jar_Baton.md)


## Better Combat (required)

Feedback needs [Better Combat](https://modrinth.com/mod/better-combat) and its Cloth Config installed. It supplies the animation player, the clips and the swing sounds; Feedback decides everything else (what a swing hits, how hard, how fast). **Report any problem to Feedback, never to Better Combat's author.**

## The upswing

A blade, mace, spear or bare hand does not strike the instant you click. The swing winds up first, and the blow lands at the clip's strike, a fraction of the way through the swing: quick for a dagger, late for a maul. A new swing waits until the weapon is about half recharged, and a stagger or a parry, roll or kick during the windup cancels it.

## Two hands, or one in each

A spear, a heavy mace (a maul) and a heavy blade (a claymore) take both hands: while you hold one, your off hand is shut, so no off-hand strike and no dual wielding. A one-handed strike weapon in each hand lets you **dual wield**: left click always swings your main hand and right click your off hand, each with its own swing, and you attack about a fifth faster. The off hand's own attack speed sets its pace. Holding the click does not repeat a swing: let go and press again.

A two-handed weapon is also held differently when you are not swinging: a claymore across the body, a spear at the ready, a bow or crossbow braced.
