# Guns

Feedback doesn't have a list of guns. You **design** one on the [Gun Stand](Gun_Stand.md), print a blueprint, **machine its parts** in the workshop and assemble it. You make the ammunition yourself at the [Reloading Press](Reloading_Press.md). Everything about how the gun behaves (how hard it hits, how far it throws, how much it kicks, how hot it gets, whether it blows up in your hand) comes from those choices and the metals you picked.

There are two kinds of launcher: an ordinary **gun**, which burns powder in a chamber, and a **[coilgun](Coilgun.md)**, which throws an iron slug with a redstone-wound coil.

## Getting one

1. On a [Gun Stand](Gun_Stand.md), choose a type, an action and a feed, set up each part and its metal, put Paper in, and press **Print**. You get a **Gun Blueprint**.
2. Machine the parts it lists: a barrel, a receiver, a trigger group, a grip and so on. See [Gun Parts](Gun_Parts.md).
3. Put the blueprint and the parts in a crafting grid (an [Assembly Bench](Assembly_Bench.md) for big guns). You get the **Gun** and **keep the blueprint**.
4. Make [ammunition](Ammunition.md) and load it.

A **Test Pistol** is also available in the creative menu.

## Types, actions and feeds

The **type** is the first choice on the Stand and decides which actions and feeds are on offer. The type itself doesn't make a gun better at anything: a marksman rifle is accurate because it's a long barrel on a bolt, not because of its name.

| Type | Actions | Feeds |
| --- | --- | --- |
| **Pistol** | self-loading, automatic, break | box magazine, cylinder, barrels |
| **Marksman rifle** | bolt, lever, self-loading, break | box magazine, single shot, tube, cylinder, barrels |
| **Shotgun** | pump, break, lever, self-loading | tube, barrels, cylinder |
| **LMG** | automatic | belt, box magazine |

### Actions

| Action | Fires | Hand time added to every shot |
| --- | --- | --- |
| **Break** | Semi | 150 ms |
| **Lever** | Semi | 450 ms |
| **Pump** | Semi | 550 ms |
| **Bolt** | Semi | 800 ms |
| **Self-loading** | Semi | none |
| **Automatic** | Auto, Burst, Semi | none |

Only an automatic can be held down. **Burst** fires three rounds, then forces a pause. Break, lever, pump and self-loading actions have a **hammer**, so any of them can take a revolver cylinder.

### Feeds

| Feed | Holds | Reloads |
| --- | --- | --- |
| **Barrels** (break action) | one per barrel; a shotgun has two | a round at a time |
| **Cylinder** | 6 | a round at a time |
| **Tube**, under the barrel | 4 / 6 / 8 for a short / medium / long barrel | a round at a time |
| **Single shot** (bolt or lever) | 1 | a round at a time |
| **Box magazine** | whatever [Magazine](Magazine.md) you load: 6 to 50 | swapped whole |
| **Belt** (automatic only) | an Ammo Belt: 100 | swapped whole |

## Controls

| Key | Action |
| --- | --- |
| Attack (left mouse) | Fire |
| Use (right mouse), held | Aim down the sights |
| **R**, held | Reload |
| **V** | Switch fire mode (only the modes the action has) |
| **U** | Unload: takes the magazine and the chambered round out |

All three keys can be rebound under the *Feedback* category in Controls.

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
- **Parts** and **Fitted**: what it's made of, part by part.

Those figures are for a standard round: four gunpowder behind a lead bullet (an iron slug and four redstone for a coilgun). What you actually load can push them up or down. JEI's *Gun Metal* page shows the same sheet for a reference 9 mm pistol in every metal.

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

**Cycle** is 90 ms, plus a little for every bit of recoil, plus the action's hand time. Big bores and fast bullets cycle a little slower.

## What bullets do

- Damage depends on the energy the bullet still has on arrival. Bullets **slow down in the air and drop** over distance, so far targets take less. A bullet with under 3% of its muzzle energy left falls where it is.
- Bullets travel **much** slower in water.
- A bullet that kills something **keeps going** with whatever energy it has left, and can hit whatever is behind.
- Every shot is a **sound sculk can hear**. A suppressed shot carries only a short way.
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

Every gun's chamber has a **rating**: the pressure it holds. It's the barrel metal's strength times the wall, divided by half the bore (see [Gun Stand](Gun_Stand.md#the-chamber)). A steel 9 mm with a standard wall holds **10 Pu**; a standard gunpowder round makes **3 Pu**.

Load a round that makes more pressure than the rating, and every shot **damages the gun**: **15% for each whole rating over** (a round at 1.5× the rating costs 7.5% a shot). The tooltip shows *Damaged by overpressure* as a percentage. At 100% the gun is **Spent**: *"the chamber has let go."* It stays in your inventory but will never fire again.

Go **more than 1.5×** over the limit, the chamber's or the cartridge case's, and the blast **hurts you** too: about 3 hearts for every rating past 1.5×, never more than 8 hearts.

None of this is random. If the tooltip and the HUD aren't red, a round can't hurt the gun. A [Relief Vent](Gun_Parts.md#fittings) keeps the chamber under its rating, at a price.

To make a gun that holds more pressure: use a stronger metal, a thicker chamber wall, or a smaller bore.

## Test commands

Operators can use `/feedback gun` (a gun built exactly to the held blueprint), `/feedback parts`, `/feedback barrel`, `/feedback round` and `/feedback magazine`. They hand out items carrying data no recipe browser can show, for testing. They aren't a survival route.

## See also

- [Gun Stand](Gun_Stand.md) · [Gun Parts](Gun_Parts.md) · [Assembly Bench](Assembly_Bench.md) · [Ammunition](Ammunition.md) · [Reloading Press](Reloading_Press.md) · [Magazine](Magazine.md) · [Coilgun](Coilgun.md) · [Round Charger](Round_Charger.md)
