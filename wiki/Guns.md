# Guns

Feedback doesn't have a list of guns. A gun is a **tool type** like a pick or a sword: you make **parts** (a receiver, a barrel, an action, a trigger group, a grip, and any fittings), each of a metal, and put them together at the [Gun Table](Gun_Table.md). You make the ammunition yourself at the [Reloading Press](Reloading_Press.md). Everything about how the gun behaves (how hard it hits, how far it throws, how much it kicks, how hot it gets, whether it blows up in your hand) comes from those parts and the round.

There are two kinds of launcher: an ordinary **gun**, which burns powder in a chamber, and a **[coilgun](Coilgun.md)**, which throws an iron slug with a redstone-wound coil.

## Getting one

1. Draw the gun on paper at the [Blueprint Table](Blueprint_Table.md) and print it: a **blueprint** of the gun. It shows the figures the gun will have (at the reference round) before you make it.
2. Make the parts: shape them at the [Gun Table](Gun_Table.md) or machine them ([Gun Parts](Gun_Parts.md)).
3. Put the blueprint and the parts on the Gun Table. You get the **Gun**.
4. Make [ammunition](Ammunition.md) and load it.

A ready-made 9 mm steel handgun is also in the creative menu.

## Variants, traits and names

A gun has one **variant**, told by the **receiver**: what kind of gun it fundamentally is.

| Variant | What makes it different |
| --- | --- |
| **Handgun** | Held in one hand, no stock: kick and spread are the wrist's. |
| **Rifle** | A shoulder stock and a barrel long enough to steady the bullet. Braced fire is steadier. |
| **Shotgun** | A smoothbore; the only gun that takes a shell. |
| **Machine gun** | A heavy frame and an open bolt, built for sustained fire. |

Everything else is a **trait**, read from the parts, and a gun has as many as its parts show (hold Shift on the tooltip for what each does):

| Trait | From | Does |
| --- | --- | --- |
| Hammer | lever, pump, break, revolver | lets a cylinder be fitted and fanned |
| Fan | a cylinder on a hammer action | crouched, hold attack to slap the hammer as fast as the hand can, with more spread, kick and heat |
| Double barrel | a second barrel | crouched, a pull fires both barrels at once for one recoil |
| Select fire | an automatic action | semi, burst or full auto, switched in hand |
| Manual cycle | bolt, lever, pump, break | the shooter brings the next round up: the action's time between shots |
| Bolt lockup | a bolt action | the chamber is rated 25% stronger for the same steel, and the group tighter |
| Blowback | a slide | only the bolt's mass holds the breech: a round above 3.6 Pu kicks harder and wears the bore faster |
| Gas operated | a bolt-type self-loading or automatic action | a quicker cycle that pressure does not hurt, paid in heat and mass |
| Closed bolt | most actions | a round in the hot chamber can cook off |
| Open bolt | a machine gun | nothing in the chamber to cook off; the first shot after a pause sprays wider |
| Belt fed | a belt feed | a long reload |
| Braced | a rifle or machine gun | crouched and still, less kick and tighter spread |

*Not built yet:* off hand (a handgun in each hand).

The **name** on the tooltip is the real-world designation, read by a rule from the variant, calibre, barrel length, sight, action and traits: handgun with a cylinder is a **revolver**, a long rifle with a scope and a bolt a **sniper rifle**, a short double shotgun a **sawn-off**. It changes nothing in play.

### Calibre classes

Bore is a short list of named classes, the barrel's shape: **rimfire** (5 mm), **pistol** (9 mm), **intermediate** (12 mm) and **magnum** (20 mm). A round is seated for one class at the [Reloading Press](Reloading_Press.md). The barrel's *real* length, bore and wall are what the drill, lathe and cutoff wheel left on it ([Gun Parts](Gun_Parts.md)), and how loosely the round sits is in the figures.

### Actions and feeds

| Action | Fires | Hand time added to every shot |
| --- | --- | --- |
| **Break** | Semi | 150 ms |
| **Lever** | Semi | 450 ms |
| **Pump** | Semi | 550 ms |
| **Bolt** | Semi | 800 ms |
| **Self-loading** | Semi | none |
| **Automatic** | Auto, Burst, Semi | none |

Only an automatic can be held down. **Burst** fires three rounds, then forces a pause. A feed has to suit its action, or the parts make no gun: a cylinder needs a hammer (lever, pump, break or a slide), a tube a lever, pump or self-loading action, a belt an automatic one. A second barrel needs a break action. With no feed part a gun takes a **box magazine** (a break action holds its barrels).

| Feed | Holds | Reloads |
| --- | --- | --- |
| **Barrels** (break action) | one per barrel | a round at a time |
| **Cylinder** | 6 | a round at a time |
| **Tube**, under the barrel | 4 / 6 / 8 for a short / medium / long barrel | a round at a time |
| **Box magazine** | whatever [Magazine](Magazine.md) you load: 6 to 50 | swapped whole |
| **Belt** | an Ammo Belt: 100 | swapped whole, twice as slowly |

## Controls

| Key | Action |
| --- | --- |
| Attack (left mouse) | Fire |
| Use (right mouse), held | Aim down the sights |
| **R**, held | Reload |
| **V** | Switch fire mode (only the modes the action has) |
| **U** | Unload: takes the magazine and the chambered round out |

| **Z**, held | [Melee stance](#swinging-a-gun): attack swings the gun instead of firing it |

All of these keys can be rebound under the *Feedback* category in Controls.

**Reloading is held, not tapped.**

- A **magazine** gun swaps in the fullest magazine of the right size from your inventory once R has been held for the whole **1.4 s**. Let go early and nothing changes.
- A gun with its **own feed** (cylinder, tube, barrels) loads one loose round every **0.45 s** while R is down, and keeps whatever is in when you let go.

Firing or putting the gun away stops a reload.

## The HUD

While you hold a gun, the bottom-right corner shows:

- **Rounds** in the gun: feed plus the one in the chamber. The count turns **orange-red** when the next round will put more pressure on the chamber than it can take (see [Overpressure](#overpressure)).
- **Spare** rounds of the right size you're carrying, loose or in magazines.
- **Barrel**: *cool*, *warm*, *hot* or ***SMOKING***.
- Your **fire mode**.
- *RELOADING*, *EMPTY*, *NO MAGAZINE* or *SPENT* when one of those applies.

## Reading the tooltip

Every gun and blueprint shows its spec sheet:

- **Chamber**: the pressure a standard round makes, against what the chamber can hold. Red if the chamber is over its limit.
- **Muzzle**: bullet speed and **energy**, in Work. Energy is what decides damage.
- **Cycle**: the time between shots, and the spread in degrees.
- **Mass**: the gun's weight, and the bullet's.
- The tooltip also lists the parts and what is fitted, part by part, and the Shift glossary.

Those figures are for a standard round: four gunpowder behind a lead bullet (an iron slug and four redstone for a coilgun). What you actually load can push them up or down. JEI's material pages list each metal's own figures (hardness, density, melting point) to read against them.

## Damage

**Every 25 Work of energy a bullet still carries when it hits is 1 damage** (half a heart). The tooltip's muzzle energy is that figure at point-blank range.

For a sense of scale, a steel 9 mm pistol with a standard wall on a standard round:

| Barrel | Muzzle energy | Damage |
| --- | --- | --- |
| Short (0.1 m) | about 95 Work | 4 (2 hearts) |
| Medium (0.2 m) | about 375 Work | 15 (7½ hearts) |
| Long (0.4 m) | about 880 Work | 35 (17½ hearts) |

Gunpowder burns slowly and a short barrel wastes most of it, which is why the short barrel is so much weaker. See [Ammunition](Ammunition.md#gunpowder).

## Handling

**Spread** starts at **2°** for a medium barrel, scales down with a longer barrel (and up with a shorter one), and never goes below **0.25°**. What you're doing multiplies it:

| Doing | Spread |
| --- | --- |
| Aiming (on the ground, not sprinting) | × 0.4 |
| Crouching | × 0.8 |
| Walking | × 1.6 |
| Sprinting | × 2 |
| In mid-air | × 3 |

**Sights:**

| Sight | Zoom (field of view) | Time to raise | Extra parts |
| --- | --- | --- | --- |
| Iron | 85% | 0.12 s | none |
| Reflex | 80% | 0.15 s | 1 plate |
| Scope | 40% | 0.3 s | 2 plates |

**Recoil** throws your view upward. It's the bullet's push (plus a quarter again for the gas) divided by the gun's weight, and scaled by how grippy the grip is (see [Gun Parts](Gun_Parts.md#wrapping-the-grip)). A heavy gun with a grippy grip kicks least. Aiming takes 35% off it. A long string of shots climbs up to about twice the first shot's kick by the tenth, and settles when you stop.

**Cycle** is 90 ms, plus a little for every bit of recoil, plus the action's hand time. Big bores and fast bullets cycle a little slower. **Haste** speeds the cycle and the reload by 20% a level, and Mining Fatigue slows them the same way (never below a fifth).

### One hand or two

A gun light enough to carry at arm's length (**4 Mu or less**) and soft enough to recoil against (**6 m/s or less** of recoil speed) can be fired with **one hand**, so you can hold a gun in each hand. Fired one-handed, spread and kick grow by up to 60%, in proportion to how close the gun is to that recoil limit. Heavier guns need both hands.

## Swinging a gun

Hold **Z** and attack, and you swing the gun as a **bludgeon** instead of firing it. No gun has a row for this: everything comes from its baked mass.

| | |
| --- | --- |
| Damage | 1 plus 1 per Mu, up to 10 |
| Time between swings | 4 ticks per Mu, between 8 and 30 |
| Knockback | 0.3 plus 0.08 per Mu |
| Hits | the nearest thing on a short line from your eye |

A swing doesn't wear the gun (a swing isn't misuse). Traits and [material perks](Material_Perks.md) resolve as for any hit.

This is **gun-fu**, and the moves are learned in the [Brute skill tree](Skills.md#brute): the swing itself (*pistol whip*), a **free shot** (*chamber*: a blow that staggers its target chambers one shot, which fires at it if the gun is loaded and working; a gun too light to stagger gives nothing), swinging the **off-hand** gun too (*dual*), and a sneaking **spin** that hits everything within 2 blocks.

## What bullets do

- Damage depends on the energy the bullet still has on arrival. Bullets **slow down in the air and drop** over distance, so far targets take less. A bullet with under 3% of its muzzle energy left falls where it is.
- Bullets travel **much** slower in water.
- A bullet that kills something **keeps going** with whatever energy it has left, and can hit whatever is behind.
- Every shot is a **sound sculk and wardens can hear**, and how far depends on how loud the gun really is: a bare supersonic shot carries furthest (64 blocks), a muffled one less (32), and a suppressed subsonic one only a short way (8). The crack of a supersonic round is the bullet's own speed, so a suppressor alone does not hide it; pair it with a slow load. Skeletons holding guns are heard the same way.
- Ordinary rounds leave a faint glint along their path. **Tracers** draw a bright line, and **incendiary** rounds set targets alight for 5 s and light the ground (see [Ammunition](Ammunition.md#payloads)).

### Hardness: glass, glancing, armour

What the **bullet** is made of matters when it hits something hard. Hardness figures are on the [Materials](Materials.md) page: lead 0.3, copper 1, iron 6, steel 15.

- **Glass, ice and other brittle blocks** break if the bullet hits with enough momentum, and it carries on, a little slower. A pistol round has plenty. Stone, wood and anything tougher don't break. Protected areas and adventure mode stop this.
- A bullet **softer than the face it hits**, coming in at a shallow angle (more than about 70° from head-on), **glances off** with half its energy. It can glance twice.
- **Armour** is rated by what it's made of: leather 0.3, gold 0.5, iron 6, netherite 40, diamond 100. A bullet **harder** than the hardest piece someone wears puts a share of the hit straight through, ignoring the armour: **1 − (armour ÷ bullet)**. A steel bullet against iron armour puts 60% through. A bullet no harder than the armour is stopped by it like any hit. Lead never pierces anything but bare skin.

## Heat

Every shot warms the barrel, and the barrel cools at a steady rate in between. A steel 9 mm pistol on standard rounds warms about **3.5 Tu a shot** and cools about **6 Tu a second**, so anything faster than a shot every half second or so heats it up. A light gun heats up fast. A heavy gun heats more slowly and also cools more slowly. A hot gun **visibly smokes** in your hand, and other players can see it.

As it heats up:

- Spread opens up, to **2.5×** at the powder's ignition temperature.
- The bore wears faster.
- **Cook-off:** if the barrel gets hotter than the powder in the chambered round can stand, that round **fires by itself**, in whatever direction you're facing, and the blast hurts you (1 heart per four powder, up to 4 hearts). It keeps happening at the gun's firing rate until the gun is empty or you take the round out. **If the HUD says SMOKING, stop firing and press U.**

| Powder | Cooks off at |
| --- | --- |
| Gunpowder | 300 Tu |
| Blaze Powder (or any blend with it) | 220 Tu |

A gun cools down wherever it is: in your hand, in your hotbar, in a chest. A [Barrel Shroud](Gun_Parts.md#fittings) makes it cool faster; a suppressor makes it heat faster.

## Wear

The bore wears a little with every shot, faster when the barrel is hot. The tooltip shows it as **Bore wear**. A worn gun hits less hard and spreads wider, down to a floor: at its worst it delivers **30% less** energy, but it never stops working from wear alone. There's no way to repair wear yet.

## Overpressure

Every gun's chamber has a **rating**: the pressure it holds. It's the barrel metal's strength times the wall, divided by half the bore (see [Gun Parts](Gun_Parts.md#the-chamber)). A steel 9 mm with a standard wall holds **10 Pu**; a standard gunpowder round makes **3 Pu**.

Load a round that makes more pressure than the rating, and every shot **damages the gun**: **15% for each whole rating over** (a round at 1.5× the rating costs 7.5% a shot). The tooltip shows *Damaged by overpressure* as a percentage. At 100% the gun is **Spent**: *"the chamber has let go."* It stays in your inventory but will never fire again.

Go **more than 1.5×** over the limit, the chamber's or the cartridge case's, and the blast **hurts you** too: about 3 hearts for every rating past 1.5×, never more than 8 hearts.

None of this is random. If the tooltip and the HUD aren't red, a round can't hurt the gun. A [Relief Vent](Gun_Parts.md#fittings) keeps the chamber under its rating, at a price.

To make a gun that holds more pressure: use a stronger metal, a thicker chamber wall, or a smaller bore.

## Test commands

Operators can use `/feedback build` (the item built exactly to the held blueprint, a gun or any tool), `/feedback build parts`, `/feedback barrel`, `/feedback round` and `/feedback magazine`. They hand out items carrying data no recipe browser can show, for testing. They aren't a survival route.

## See also


## Armed skeletons

A few skeletons (and wither skeletons, a little more often) spawn holding an early gun. The gun is as hot as the dimension it is carried in: in the Nether it starts hot, heats as it fires, and can cook off or burst sooner. It may hurt the skeleton holding it.

- [Combat](Combat.md) · [Skills](Skills.md#brute)
- [Gun Table](Gun_Table.md) · [Gun Parts](Gun_Parts.md) · [Ammunition](Ammunition.md) · [Reloading Press](Reloading_Press.md) · [Magazine](Magazine.md) · [Coilgun](Coilgun.md) · [Round Charger](Round_Charger.md)
