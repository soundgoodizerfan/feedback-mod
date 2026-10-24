# Combat

Fighting in Feedback is built on the same idea as everything else: **what a blow does comes from physical figures**, not from a fixed list of items. A weapon's reach, mass and balance (see [Tools and Weapons](Tools_and_Weapons.md)) decide its swept hit, its parry and how hard it staggers. A mob's weight is worked out from its size. Nothing is keyed on the name of a weapon.

Every figure on this page is a **placeholder** that is still being felt out in play. The structure is settled; the numbers will move.

## Keys

All of these can be rebound under the *Feedback* category in Controls.

| Key | Action |
| --- | --- |
| **G** | [Parry](#parry) |
| **Left Alt** | [Roll](#roll) |
| **X** | [Kick](#kick) |
| **C** | [Grab](#grab) (needs a glove) |
| **Z**, held | [Gun melee stance](Guns.md#swinging-a-gun) |
| **K** | [Skill tree](Skills.md) |
| *(unbound)* | Off-hand attack |

## Swung weapons hit what they sweep

A melee swing covers a volume in front of you, as long as the weapon's reach and as wide as its balance makes it (a spear is a thin deep box, a maul a wide one). **Everything in it is hit**, not only the thing under the crosshair, and a swing at empty air still hits something that was standing in the volume. A block between you and the target stops it. Traits and [material perks](Material_Perks.md) apply to each target hit.

## Parry

Press **G** to open a short window in which a blow aimed at you is turned aside.

**What can parry:** a blade, mace or spear that isn't broken, in the main hand or else the off hand. A **buckler** parries too, and a **kite shield** turns projectiles back (see [Shields](#shields)). A bow, a tool, a gun or a plain shield does not. With a [Feedbacker](Feedbacker.md) worn you can parry bare-handed.

**Window and recovery come from the weapon's heft** (its mass, lengthened by its reach):

| Weapon | Window | Recovery |
| --- | --- | --- |
| Dagger (lightest) | about 4 ticks | about 10 ticks |
| Greatsword (heaviest) | about 12 ticks | about 35 ticks |

Heavy and long is a wide window and a long lockout: it sweeps across a lot of angle, and it is slow to bring back when it misses. Latency is covered by up to 4 extra ticks, never more, so ping can't buy a parry. The window only covers a **frontal cone**: 35° either side of your look for a pointlike weapon, up to 90° for a head-heavy one.

**When it lands**, the blow is cancelled before any trait or perk sees it, and the attacker is **staggered**: held back and slowed to 40% for a time set by the impact of their blow against *their own mass* (between 0.2 and 2 seconds). A heavy mob stumbles less than a light one. A parried arrow or other projectile is turned back at 1.5× its speed.

**What it costs:** a clean parry takes **1 point** of wear on your weapon. If the blow was heavier than the weapon's joint is built for (60 Work per Mu of its own mass) it takes 4 more for every whole multiple it was over. A parry that misses leaves you in recovery, open. If you press at a bad moment (still recovering, or on the roll's cooldown) you get a small click and nothing happens.

**Parry bonuses**

- **Riposte** (a [skill](Skills.md)): after a weapon parry, your next hit within 1.5 seconds does 1.5× damage.
- A **bracer** from [combat gear](Combat_Gear.md) widens the window by up to 2 ticks.

A bar under the crosshair shows the window and the recovery.

### Shields

- **Buckler:** parries a melee blow whole on **G** (a 6-tick window), throws the attacker back and costs 1 wear. It guards a narrow arc.
- **Kite shield:** on **G**, returns arrows and bolts the way you look. Melee still lands. It no longer returns arrows just by being raised.
- **Tower and plain shields:** block as before and don't parry.

## Roll

Press **Left Alt** to roll in the direction you are moving (forward if you aren't).

- The whole 8-tick roll is **invulnerable**: it cancels every damage event, including fire and other ticking damage, though not the void or `/kill`.
- It slides about **3 blocks** on flat ground.
- Afterwards you're open for **12 ticks** of recovery, then there is a **30 tick cooldown** before you can roll, kick *or* parry again.

A [sash](Combat_Gear.md#sash) lengthens the roll and shortens recovery. Dodging a real blow earns [evasion skill points](Skills.md#earning-points).

### Wall kick

A skill in the evasion tree. Sprint on the ground or roll, then jump, and jump again against a wall while in the air (within 0.75 seconds of leaving the ground): you kick up and away from it. You can do it once per sprint, and it costs a little hunger.

## Kick

Press **X** to kick whatever is in front of you, low (about waist height, so under a raised shield's face), within 1.6 blocks.

- It does **2 damage**: the kick is for position, not killing.
- It **shoves** by the kick's impact against the target's mass, and **staggers** targets light enough to be moved. An iron golem is only shoved.
- A target with a **raised shield loses its guard**: the shield is put on cooldown (1 to 5 seconds, longer the lighter the target) and the target staggers.
- A kick costs hunger, and shares the 30 tick cooldown with the roll and the parry. The leg visibly kicks.

## Grab

Press **C** to take hold of something within 2 blocks. **You need a glove** in the Curios *hands* slot ([leather gloves](Hazards.md#gloves) will do): grabbing isn't a free hook on anything, it needs a source.

A grab is a **short rope** (1.5 blocks) with no reel. The held thing is pinned close, can't parry or roll, and **takes 25% more damage**. The hold ends when:

- you press **C** again,
- 3 seconds pass (a skill lengthens it),
- it gets more than 3 blocks away (you rolled, or one of you was flung),
- you take a single hit of 4 or more damage,
- either of you dies or leaves.

**A thing more than twice your weight cannot be held**: it yanks you instead. Mass is the same stand-in as the parry's (the size of its box). With no glove, you get a deny click.

## Off-hand weapons

You can **dual wield**. Right-click with a weapon in the off hand and nothing else wants the click (a chest still opens, a shield still blocks) and the off hand swings, by its *own* damage, speed and reach, with its own cooldown, its own traits and perks, and its own wear. There is also an **Off-hand attack** key, unbound by default. The off-hand swing hits one target along your look rather than sweeping. [Guns](Guns.md#swinging-a-gun) work the same way with the stance key.

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

A spear, a heavy mace (a maul) and a heavy blade (a claymore) take both hands: while you hold one, your off hand is shut, so no off-hand strike and no dual wielding. A one-handed strike weapon in each hand lets you **dual wield**: left click alternates your hands, each with its own swing, and you attack about a fifth faster. The off hand's own attack speed sets its pace. Holding the click does not repeat a swing: let go and press again.

A two-handed weapon is also held differently when you are not swinging: a claymore across the body, a spear at the ready, a bow or crossbow braced.
