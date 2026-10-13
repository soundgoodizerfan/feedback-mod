# Guns

Feedback doesn't have a list of guns. You **design** one on the [Gun Stand](Gun_Stand.md), print a blueprint, and build it from whatever metal you chose. You make the ammunition yourself at the [Reloading Press](Reloading_Press.md). Everything about how the gun behaves (how hard it hits, how far it throws, how much it kicks, how hot it gets, whether it blows up in your hand) comes from those choices.

There are two kinds of launcher: an ordinary **gun**, which burns powder in a chamber, and a **[coilgun](Coilgun.md)**, which throws an iron slug with a redstone-wound coil.

## Getting one

1. Open a [Gun Stand](Gun_Stand.md), pick a design and a metal, put Paper in, and press **Print**. You get a **Gun Blueprint**.
2. Put the blueprint in a crafting table with exactly the number of ingots of that metal the stand quoted. A coilgun also needs Blocks of Redstone. The shape doesn't matter. You get the **Gun** and you **keep the blueprint**, so you can build as many as you like.
3. Make [ammunition](Ammunition.md), load it into a [Magazine](Magazine.md), and reload.

A **Test Pistol** is also available in the creative menu.

## Controls

| Key | Action |
| --- | --- |
| Attack (left mouse) | Fire |
| Use (right mouse), held | Aim down the sights |
| **R** | Reload: swaps in the fullest magazine of the right size from your inventory |
| **V** | Switch fire mode: *Semi* → *Burst* → *Auto* |
| **U** | Unload: takes the magazine and the chambered round out |

All three keys can be rebound under the *Feedback* category in Controls.

**Fire modes:** *Semi* fires once per click. *Burst* fires three rounds, then forces a short pause. *Auto* keeps firing while you hold the button. New guns start on *Auto*.

## The HUD

While you hold a gun, the bottom-right corner shows:

- **Rounds** in the gun: magazine plus the one in the chamber. The count turns **orange-red** when the next round will put more pressure on the chamber than it can take (see [Overpressure](#overpressure)).
- **Spare** rounds of the right size you're carrying, loose or in magazines.
- **Barrel**: *cool*, *warm*, *hot* or ***SMOKING***.
- Your **fire mode**.
- *RELOADING*, *EMPTY* or *SPENT* when one of those applies.

## Reading the tooltip

Every gun and blueprint shows its spec sheet:

- **Chamber**: the pressure a standard round makes, against what the chamber can hold. It turns red if the chamber is over its limit.
- **Muzzle**: bullet speed and energy. Energy is what decides damage.
- **Cycle**: how fast it can fire again, and its spread in degrees.
- **Mass**: the gun's weight, and the bullet's.

Those figures are for a standard round. What you actually load can push them up or down.

## Handling

**Spread** depends on what you're doing:

- Aiming tightens it a lot, but only while you're standing on the ground and not sprinting.
- Crouching tightens it a little.
- Walking widens it, sprinting widens it more, and firing in mid-air is wild.
- A longer barrel is more accurate. No gun is ever perfectly accurate.

**Sights**: iron sights come up fastest and zoom least. A reflex sight zooms a little more. A scope zooms a lot but is slow to raise.

**Recoil** throws your view upward. It builds up over a long string of shots and settles when you stop. Aiming takes some of the edge off it. Bigger bores and faster bullets kick harder.

## What bullets do

- Damage depends on how much energy the bullet still has when it arrives. Bullets slow down in the air and drop over distance, so far targets take less.
- Bullets travel **much** slower in water.
- A bullet that kills something **keeps going** with whatever energy it has left, and can hit whatever is behind.
- Bullets smash **glass, ice and similarly brittle blocks** and carry on, a little slower. They don't break stone, wood or anything tougher. Protected areas and adventure mode stop this.
- Ordinary rounds leave a faint glint along their path. **Tracers** draw a bright line, and **incendiary** rounds set targets and the ground alight (see [Ammunition](Ammunition.md#payloads)).

## Heat

Every shot warms the barrel. A light gun heats up fast. A heavy gun takes longer to heat but also longer to cool. A hot gun **visibly smokes** in your hand, and other players can see it.

As it heats up:

- Spread opens up, so the gun gets less accurate.
- The bore wears faster.
- **Cook-off:** if the barrel gets hotter than the powder in the chambered round can stand, that round **fires by itself**, in whatever direction you're facing, and the blast hurts you. It keeps happening at the gun's firing rate until the gun is empty or you take the round out. **If the HUD says SMOKING, stop firing and press U.**

A gun cools down wherever it is: in your hand, in your hotbar, in a chest. Rounds with blaze powder in them cook off at a lower barrel heat than plain gunpowder rounds.

## Wear

The bore wears a little with every shot, faster when the barrel is hot. The tooltip shows it as **Bore wear**. A worn gun hits less hard and spreads wider, but it never stops working from wear alone. There's no way to repair wear yet.

## Overpressure

Load a round that makes more pressure than the chamber is rated for, and every shot **damages the gun**. The further over, the more damage. The tooltip shows *Damaged by overpressure* as a percentage. At 100% the gun is **Spent**: *"the chamber has let go."* It stays in your inventory but will never fire again.

Go well over the limit, either the chamber's or the cartridge case's, and the blast **hurts you** too. It's painful but won't kill you from full health in one shot.

None of this is random. If the tooltip and the HUD aren't red, a round can't hurt the gun.

To make a gun that holds more pressure: use a stronger metal, a thicker chamber wall, or a smaller bore.

## See also

- [Gun Stand](Gun_Stand.md) · [Ammunition](Ammunition.md) · [Reloading Press](Reloading_Press.md) · [Magazine](Magazine.md) · [Coilgun](Coilgun.md)
