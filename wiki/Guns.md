# Guns

Feedback doesn't have a list of guns. A gun is a **tool type** like a pick or a sword: you make **parts** (a receiver, a barrel, an action, a trigger group, a grip, and any fittings), each of a metal, and put them together at the [Gun Table](Gun_Table.md). You make the ammunition yourself at the [Reloading Press](Reloading_Press.md). Everything about how the gun behaves (how hard it hits, how far it throws, how much it kicks, how hot it gets, whether it blows up in your hand) comes from those parts and the round.

What throws the round is the **launcher**, and the barrel you choose sets it (see [Barrels and launchers](#barrels-and-launchers)): an ordinary **chamber gun** burns powder, a **[coilgun](Coilgun.md)** throws an iron slug with a redstone-wound coil, a **spring gun** releases a wound spring, and a **steam gun** lets out steam. The [flintlock](#flintlock) is a chamber gun of its own, made without a blueprint.

## What it looks like

A gun is drawn from its parts, each in its own metal's or wood's color. The barrel shows at the length and thickness it was drilled to, the stock and forend take the grip's material, and every fitting appears where it mounts: a suppressor or brake on the muzzle, a tube under the barrel, a scope or reflex sight on top, a bipod, a bayonet, a steam chest on the side. Wooden parts are drawn in the bark of their wood, softened, metal parts are brushed or plated, and plastic ones are smooth. A hot barrel glows, dull red through orange to yellow as it heats, and lights itself in the dark. A long gun or machine gun is held in both hands. In first person your own arms are drawn on the gun: the grip hand always, the support hand when the other hand is empty. The client setting **gunArms** (in the mod's client config) switches the arms off. A scope is an open tube: you see straight down it, with no lens in the way.

In your hands it moves: it kicks back and up on each shot (barely at all when aimed), the slide or bolt carrier snaps back and returns, and a bolt, lever or pump is worked between shots. An empty self-loading gun stays open, for you and for anyone looking at it. Reloading tips the gun over: a magazine drops out and a fresh one goes in, a revolver's cylinder swings out, a break action hinges open, a tube is thumbed full round by round. The gun lags a little behind fast turns, dips when you land, and lowers while you sprint.

## Getting one

1. Draw the gun on paper at the [Blueprint Table](Blueprint_Table.md) and print it: a **blueprint** of the gun. It shows the figures the gun will have (at the reference round) before you make it.
2. Make the parts: shape them at the [Gun Table](Gun_Table.md) or machine them ([Gun Parts](Gun_Parts.md)).
3. Put the blueprint and the parts on the Gun Table. You get the **Gun**.
4. Make [ammunition](Ammunition.md) and load it.

A ready-made 9 mm steel handgun is also in the creative menu.

A [flintlock](#flintlock) needs no blueprint: it is put together from its parts at the [Smithing Table](Smithing_Table.md).

## Frames, barrels, traits and names

A gun has one **variant**, told by the **receiver**: its frame, which is only a matter of size.

| Frame | Cells of stock | What makes it different |
| --- | --- | --- |
| **Handgun** | 1 | Held in one hand: kick and spread are the wrist's, unless it has a stock. |
| **Long gun** | 2 | A longer receiver for a long barrel to steady the bullet. It has no shoulder of its own: add a stock (see below). |
| **Machine gun** | 3 | A heavy frame and an open bolt, built for sustained fire. |

A **stock** is its own part, on any frame, and changes what a gun is like to hold. A **fixed** stock (two cells) kicks 30% less and groups 40% tighter when braced, a **skeleton** one (one cell) 20% and 25%, a **folding** one (one cell) 15% each. A gun with none has no braced bonus and, being a long gun, is a long handgun: hard on your hands (see Strain under Recoil). A stock is cut from a plate or from planks.

What the gun is called (rifle or shotgun, pistol or revolver) follows from the other parts, see [Names](#names).

### Barrels and launchers

The **barrel** is the launcher. Its shape picks how the round is thrown, and the part that launcher needs hangs on it in the [Blueprint Table](Blueprint_Table.md); slots for another launcher's parts are grayed out. A barrel without the part for its launcher makes no gun.

| Barrel | Stock | Launcher | Needs |
| --- | --- | --- | --- |
| **Smoothbore Barrel** | 1 cell | powder in a chamber | nothing. Bullets scatter more than from a rifled bore |
| **Rifled Barrel** | 2 cells | powder in a chamber | nothing. A single bullet's spread is x0.6 of a smoothbore's; it cannot fire [shot](Ammunition.md#shot-shells) |
| **Coil Barrel** | 2 cells | a pulsed field: a [coilgun](Coilgun.md) | a winding |
| **Spring Tube** | 1 cell | a released spring | a main spring |
| **Steam Tube** | 2 cells | steam: see [Steam launchers](#steam-launchers) | a steam chest |

A **spring gun** fires a round with nothing in the case: no gas, no powder, no cook-off. The **main spring** is a single or a double spring (one or two coils of stock). Its stored energy goes as the stress its metal bears, squared, so only a good spring steel is worth fitting; each coil stores more and takes longer to recock. The tooltip shows the energy it stores.

**Bore** is a free measure from **5 to 20 mm** in 1 mm steps, set with a **< >** stepper on the barrel in the Blueprint Table. The round is seated for a bore at the [Reloading Press](Reloading_Press.md); how the real barrel and the round fit is in [Gun Parts](Gun_Parts.md#hitting-the-target).

### Flintlock

A **Flintlock** is a gun made without a blueprint, from five parts: a **receiver** (handgun or long-gun frame), a **smoothbore barrel**, a **trigger**, a **grip** and a **lock** (shaped from a rod or an ingot). Nothing else fits it: no sights, feeds or fittings. It fires **one round at a time, loaded from the muzzle by hand**, and takes about **2.2 s** between shots. Roughly **one pull in eight flashes in the pan**: a click and a puff of smoke, the charge stays loaded and the pull still costs the cycle. A flintlock with a handgun frame is a **flintlock pistol**, with a long-gun frame a **flintlock musket**.

It is slow and unreliable beside a modern gun: the weapon of the time before real guns.

Everything else is a **trait**, read from the parts, and a gun has as many as its parts show (hold Shift on the tooltip for what each does):

| Trait | From | Does |
| --- | --- | --- |
| Hammer | lever, pump, break, flintlock | lets a cylinder be fitted and fanned |
| Fan | a cylinder on a hammer action | crouched, hold attack to slap the hammer as fast as the hand can, with more spread, kick and heat |
| Double barrel | a second barrel | crouched, a pull fires both barrels at once for one recoil |
| Select fire | an automatic action | semi, burst or full auto, switched in hand |
| Manual cycle | bolt, lever, pump, break, flintlock | the shooter brings the next round up: the action's time between shots |
| Bolt lockup | a bolt action | the chamber is rated 25% stronger for the same steel, and the group tighter |
| Blowback | a slide | only the bolt's mass holds the breech: a round above 3.6 Pu kicks harder and wears the bore faster |
| Gas operated | a bolt-type self-loading or automatic action | a quicker cycle that pressure does not hurt, paid in heat and mass |
| Closed bolt | most actions | a round in the hot chamber can cook off |
| Open bolt | a machine gun frame | nothing in the chamber to cook off; the first shot after a pause sprays wider |
| Belt fed | a belt feed | a long reload |
| Braced | a stock | crouched and still, less kick and tighter spread |


### Names

The **name** on the tooltip, and beside the type name in the Blueprint Table, is read by rule from the frame, action, feed, barrel finish, gauge and traits. A bore of **14 mm or wider** is a *wide* gauge; narrower is *narrow*. The launcher comes first: a coil barrel is a **Lightning lance** (long gun, long barrel, scope), a **Gauss rifle** (any other long gun), an **Arc chaingun** (machine gun), a **Rotary driver** (handgun with a cylinder), a **Sparkler** (any other handgun), or a plain **coilgun**; a spring tube a **spring gun**, a steam tube a **steam gun**. A chamber gun is named by its frame; the first row that fits is the name:

| Frame | Name | What gives it |
| --- | --- | --- |
| Handgun | **Derringer** | two barrels |
| | **Big Iron** | a cylinder and a bore of exactly 20 mm (named with its material, "Steel Big Iron"; firing one earns a hidden advancement of the same name) |
| | **Shot revolver** | a cylinder, smoothbore, wide gauge |
| | **Shot pistol** | smoothbore, wide gauge |
| | **Revolver** | a cylinder |
| | **Machine pistol** | select fire |
| | **Pistol** | anything else |
| Long gun | **Sawn-off** | smoothbore, wide gauge, short barrel, two barrels |
| | **Double shotgun** | smoothbore, wide gauge, two barrels |
| | **Pump shotgun** | smoothbore, wide gauge, pump action |
| | **Shotgun** | smoothbore, wide gauge |
| | **Musket** | smoothbore, narrow gauge |
| | **Slug gun** | rifled, wide gauge |
| | **Submachine gun** | rifled, select fire, bore up to 10 mm |
| | **Assault rifle** | rifled, select fire, bore from 11 mm (firing one with a pistol grip earns the hidden advancement "This is Very Illegal") |
| | **Sniper rifle** | rifled, long barrel, a scope, a bolt or self-loading action |
| | **Carbine** | rifled, short barrel |
| | **Rifle** | rifled, anything else |
| Machine gun | **General-purpose machine gun** | a belt feed |
| | **Light machine gun** | anything else |

A [flintlock](#flintlock) is a flintlock pistol or musket. A name changes nothing in play.

### Bore

Bore is any whole number from 5 to 20 mm, and the [Reloading Press](Reloading_Press.md) steps through the same range. The barrel's *real* length, bore and wall are what the drill, lathe and cutoff wheel left on it ([Gun Parts](Gun_Parts.md)), and how loosely the round sits is in the figures.

### Actions and feeds

| Action | Fires | Hand time added to every shot |
| --- | --- | --- |
| **Break** | Semi | 150 ms |
| **Lever** | Semi | 450 ms |
| **Pump** | Semi | 550 ms |
| **Bolt** | Semi | 800 ms |
| **Flintlock** | Semi | 2200 ms |
| **Self-loading** | Semi | none |
| **Automatic** | Auto, Burst, Semi | none |

Only an automatic can be held down. **Burst** fires three rounds, then forces a pause. A feed has to suit its action, or the parts make no gun: a cylinder needs a hammer (lever, pump, break or a slide), a tube a lever, pump or self-loading action, a belt an automatic one. A second barrel needs a break action. With no feed part a gun takes a **box magazine** (a break action holds its barrels); a [flintlock](#flintlock) takes one round at a time.

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

**Strain.** A view only turns so far, so kick that is too much for your hands goes into them. Every shot adds its kick in degrees to your strain (aimed shots add less), and the strain drains at 90 degrees a second while you are not firing. When it passes 90 degrees, a view's worth from level to straight up, **the gun leaves your hand** and drops at your feet. Past 180 degrees it also **hurts you**, more the further over you are, and armor does not help. One huge shot does it alone, and so does a long burst from a gun that is merely hard to hold, but moderate kick fired while looking up does not, because strain counts the kick and not where you are facing. The Gun Table lists **Throws the gun**: the first shot, after a number of shots on the trigger, or never. A gun with an **Impulse** grip stays in your hand, and the overshoot throws **you** back along the line of the shot instead.

**Cycle** is 90 ms, plus a little for every bit of recoil, plus the action's hand time. Big bores and fast bullets cycle a little slower. **Haste** speeds the cycle and the reload by 20% a level, and Mining Fatigue slows them the same way (never below a fifth).

### One hand or two

A gun light enough to carry at arm's length (**4 Mu or less**) and soft enough to recoil against (**6 m/s or less** of recoil speed) can be fired with **one hand**, so you can hold a gun in each hand. Fired one-handed, spread and kick grow by up to 60%, in proportion to how close the gun is to that recoil limit. Heavier guns need both hands.

## Swinging a gun

In barehanded mode (**B** toggles it), attacking with a gun swings it as a **bludgeon** instead of firing it. No gun has a row for this: everything comes from its baked mass. With anything else in hand, barehanded mode punches.

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

### Hardness: glass, glancing, armor

What the **bullet** is made of matters when it hits something hard. Hardness figures are on the [Materials](Materials.md) page: lead 0.3, copper 1, iron 6, steel 15.

- **Glass, ice and other brittle blocks** break if the bullet hits with enough momentum, and it carries on, a little slower. A pistol round has plenty. Stone, wood and anything tougher don't break. Protected areas and adventure mode stop this.
- A bullet **softer than the face it hits**, coming in at a shallow angle (more than about 70° from head-on), **glances off** with half its energy. It can glance twice.
- **Armor** is rated by what it's made of: leather 0.3, gold 0.5, iron 6, netherite 40, diamond 100. A bullet **harder** than the hardest piece someone wears puts a share of the hit straight through, ignoring the armor: **1 − (armor ÷ bullet)**. A steel bullet against iron armor puts 60% through. A bullet no harder than the armor is stopped by it like any hit. Lead never pierces anything but bare skin.

## Heat

Every shot warms the barrel, and the barrel cools at a steady rate in between. A steel 9 mm pistol on standard rounds warms about **3.5 Tu a shot** and cools about **6 Tu a second**, so anything faster than a shot every half second or so heats it up. A light gun heats up fast. A heavy gun heats more slowly and also cools more slowly. A hot gun **visibly smokes** in your hand, and other players can see it.

As it heats up:

- Spread opens up, to **2.5×** at the powder's ignition temperature.
- The bore wears faster.
- **Cook-off:** if the barrel gets hotter than the powder in the chambered round can stand, that round **fires by itself** in whatever direction you're facing. The blast hurts you (1 heart per four powder, up to 4 hearts), and the gun kicks your view and sounds like a shot you pulled. It keeps happening at the gun's firing rate until the gun is empty or you take the round out. **If the HUD says SMOKING, stop firing and press U.**

| Powder | Cooks off at |
| --- | --- |
| Gunpowder | 300 Tu |
| Blaze Powder (or any blend with it) | 220 Tu |

A gun cools down wherever it is: in your hand, in your hotbar, in a chest. A [Barrel Shroud](Gun_Parts.md#fittings) makes it cool faster; a suppressor makes it heat faster.

## Wear

The bore wears a little with every shot, faster when the barrel is hot. The tooltip shows it as **Bore wear**. A worn gun hits less hard and spreads wider, down to a floor: at its worst it delivers **30% less** energy, but it never stops working from wear alone. An [Arc Welder](Arc_Welder.md) repairs a worn gun, at the cost of a lower best condition each time.

## Overpressure

Every gun's chamber has a **rating**: the pressure it holds. It's the barrel metal's strength times the wall, divided by half the bore (see [Gun Parts](Gun_Parts.md#the-chamber)). A steel 9 mm with a standard wall holds **10 Pu**; a standard gunpowder round makes **3 Pu**.

Load a round that makes more pressure than the rating, and every shot **damages the gun**: **15% for each whole rating over** (a round at 1.5× the rating costs 7.5% a shot). The tooltip shows *Damaged by overpressure* as a percentage. At 100% the gun is **Spent**: *"the chamber has let go."* It stays in your inventory but will never fire again.

Go **more than 1.5×** over the limit, the chamber's or the cartridge case's, and the blast **hurts you** too: about 3 hearts for every rating past 1.5×, never more than 8 hearts.

None of this is random. If the tooltip and the HUD aren't red, a round can't hurt the gun. A [Relief Vent](Gun_Parts.md#fittings) keeps the chamber under its rating, at a price.

To make a gun that holds more pressure: use a stronger metal, a thicker chamber wall, or a smaller bore.

## Steam launchers

A gun built with a **steam chest** (a plate part, small or large) fires from steam instead of powder. The round is a plain slug. Right-click a [Boiler](Boiler.md) with the gun to fill the chest. The chest fills only as far as the boiler's pressure allows: a boiler at 1.5 Pu fills it halfway, a boiler near 3 Pu fills it full. Each shot lets out 100 mB, a gun with less than that only hisses, and steam in a carried gun slowly condenses away.

Two fittings change how a steam launcher behaves. A **Steam Valve** is metered (about 60% of the steam per shot, a slower cycle) or a dump valve (about 150%, a faster cycle). **Lagging** round the chest makes carried steam condense away at a quarter of the rate.

The HUD shows the steam in the chest in place of spare rounds. The chest has a pressure rating from its metal's strength. Fill it past that rating and it lets go: the gun is damaged, steam scalds whoever is holding it, and the chest empties. A shot taken at a pressure above the rating hurts the same way as an overpressure round.

## Test commands

Operators can use `/feedback build` (the item built exactly to the held blueprint, a gun or any tool), `/feedback build parts`, `/feedback barrel`, `/feedback round` and `/feedback magazine`. They hand out items carrying data no recipe browser can show, for testing. They aren't a survival route.

## See also


## Armed skeletons

A few skeletons (and wither skeletons, a little more often) spawn holding an early gun. The gun is as hot as the dimension it is carried in: in the Nether it starts hot, heats as it fires, and can cook off or burst sooner. It may hurt the skeleton holding it.

- [Combat](Combat.md) · [Skills](Skills.md#brute)
- [Gun Table](Gun_Table.md) · [Gun Parts](Gun_Parts.md) · [Ammunition](Ammunition.md) · [Reloading Press](Reloading_Press.md) · [Magazine](Magazine.md) · [Coilgun](Coilgun.md) · [Round Charger](Round_Charger.md)
