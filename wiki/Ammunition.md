# Ammunition

There's no "9 mm round" item that you craft. A cartridge is made of up to three things:

- a **case**, made of some metal
- a **charge**: powder, or redstone for a [coilgun](Coilgun.md), or nothing at all for a [jar round](#jar-rounds)
- a **projectile**: a bullet, or a nugget for [shot](#shot-shells), with an optional **core** inside a bullet

You can also add a **payload** if you like. You put these together at the [Reloading Press](Reloading_Press.md), at whatever size (bore) you set the press to. A gun takes any cartridge that fits its bore, whoever made it and whatever it's made of.

Hover over a cartridge to see what it is: size, case metal, bullet metal, what's in the charge and how much pressure it makes against what the case can hold.

## Cases

Draw a **metal plate** through a [Wire Drawer](Wire_Drawer.md) to make a **case** of that metal. A case holds **0.75 × its metal's strength** in Pu, whatever its size:

| Case | Holds |
| --- | --- |
| Steel | 11.3 Pu |
| Wrought Iron | 6 Pu |
| Bronze | 4.5 Pu |
| Brass | 3.75 Pu |
| Copper | 3 Pu |

A case weaker than the pressure inside it can hurt you even if the gun is fine. The press shows this before you seat anything. A **Rolled Paper Case** stands in for a metal one and holds almost nothing: see [Paper cases](Reloading_Press.md#paper-cases).

## Bullets

Two ways to make them:

- **Cast them.** Pour molten metal into a **Bullet Mold** from a crucible. One ingot's worth makes eight bullets. Copper, gold, wrought iron, lead, bronze and brass can all be cast this way.
- **Hammer them.** Hammer a **nugget** into a bullet, by hand or with a [Mechanical Hammer](Mechanical_Hammer.md).

**Bullet Mold**: two Wrought Iron Drilled Plates (from the [Drill Press](Drill_Press.md)) with a Stick on each side.

```
  D
S D S
```

**Lead** is the usual bullet metal because it's dense (11.3, against iron's 7.9): heavy bullets keep their energy. But lead is soft. A **harder** bullet goes through armor and breaks less on hard faces: see [What bullets do](Guns.md#hardness-glass-glancing-armor). A coilgun *needs* a magnetic slug.

### Bullet shapes

A bullet has a shape. Each shape trades one thing for another and none is best; figures are against a plain round-nose bullet of the same metal.

| Shape | Drag | Hurt | Armor and stone | Glass | Glances off | Bore wear |
| --- | --- | --- | --- | --- | --- | --- |
| **Round nose** | 1 | 1 | 1 | 1 | 1 | 1 |
| **Round ball** | 1.35 | 1 | 0.85 | 1 | more | 0.7 |
| **Flat nose** | 1.15 | 1.1 | 0.9 | breaks more | less | 1 |
| **Pointed** | 0.65 | 0.9 | 1.4 | 1 | a little less | 1.2 |
| **Hollow point** | 1 | 1.45 | 0.55 | a little less | 1 | 1 |

Low drag keeps a bullet's speed over distance. *Armor and stone* is how hard the bullet presents itself, so it decides what it goes through. A hollow point opens on a soft target and hurts more, but armor stops it.

A pointed bullet in a lever's tube chain-fires the round ahead of it ([Guns](Guns.md#traits)).

The shape is the bullet you seat: each shape is its own item, a Round Ball, a Flat-Nose Bullet, a Pointed Bullet or a Hollow-Point Bullet of some metal, and the press reads it off the bullet slot. Two ways to get one:

- **Cast it.** The **Ball Mold** casts **round balls** the way the Bullet Mold casts round-nose bullets, eight to an ingot's worth. Any metal that melts in a crucible can be cast.
- **Forge it.** Hammer a nugget into any shape, by hand or with a [Mechanical Hammer](Mechanical_Hammer.md). Pointed and hollow-point bullets take more work than round balls and flat noses, and a harder metal takes more again.

**Ball Mold**: two Wrought Iron Drilled Plates with a Stick on each side, the bullet mold's pattern turned over.

```
S D S
  D
```

### Jacket and core

Put a **nugget** in the press's **core** slot and the bullet becomes a **jacket** (the bullet in the bullet slot) around a **core**. The core gives the bullet its weight in proportion (70% of the volume), its magnetism for a coilgun, and its hardness against armor and stone; the jacket protects the bore. A jacket softer than its core wears the bore less, in proportion to how much softer, down to a third. A hard core in a lead jacket goes through armor like the hard metal and wears like lead.

A **polymer** jacket round a core is a **sabot**: the carrier is light, so the round leaves fast, and it flies with 0.6 of the drag and meets armor 1.2 times harder than the core alone would. Shot has no core.

### Rubber rounds

A bullet of a [polymer](Materials.md#polymers), such as synthetic rubber, seats in the press as a projectile like a metal one. A round with a polymer projectile is a **rubber round**. Hardness and density come from the polymer, as for any bullet metal.

Two advancements belong to rubber rounds:

- *Restoring Order*: hit another player with a rubber round.
- *Rage Against the Machine*: kill a player who has hit you with a rubber round. The record of who hit whom lasts until the server stops.

## Charges

The charge is loose powder. The press takes **everything** in its charge slots, so the size of the stack you put in is how much powder goes into the round. A case holds only as much charge as its room allows, and the room grows quickly with the bore: **at most 3** items at 5 mm, **8** at 9 mm, **12** at 12 mm and **27** at 20 mm. The "standard" charge the spec sheets are read against grows with it: about **2** at 5 mm, **4** at 9 mm, **6** at 12 mm and **13** at 20 mm. The same count in a wider case makes less pressure, because the powder has more room.

The powder also caps what a shot can do. A wider bore or a longer barrel could throw a bullet harder, but never with more energy than the powder holds, so a big gun costs a lot of powder.

**More charge means more pressure.** More pressure means a faster bullet, but too much is dangerous to the gun and to you. See [Overpressure](Guns.md#overpressure).

### Gunpowder

The standard propellant: **0.75 Pu per item** in a 9 mm case, so a standard four makes **3 Pu**. It's reliable and fairly gentle. It burns slowly, so it needs a long barrel to burn completely: a 0.2 m barrel burns 85% of it, a 0.1 m barrel less than half. Cooks off at **300 Tu**. Besides wild creepers, it is crafted from 3 [saltpetre](Materials.md#saltpetre) + 1 [sulfur](Materials.md#sulfur) + 1 charcoal (shapeless) for 4 gunpowder.

### Creeper Charge

Cut from a [Creeper Sac](Creeper_Pen.md#creeper-sac). It carries its own oxygen, so it burns completely on its own, and its peak pressure is **12 Pu**, four times gunpowder's 3, with a fast burn. A chamber rated below that fails ([Overpressure](Guns.md#overpressure)).

### Blaze Powder

**2.25 Pu per item**, three times gunpowder, but it's pure fuel. On its own it **barely burns**, because there's nothing in it to burn *with*: a blaze-only round gives about a tenth of its pressure.

### Blends

The press has a **second charge slot** so you can mix two powders. Gunpowder brings spare oxygen and blaze powder needs it: **two gunpowder to one blaze powder** is exactly balanced and burns completely.

| Charge | Pressure |
| --- | --- |
| 4 gunpowder | 3 Pu |
| 2 gunpowder + 1 blaze | 3.75 Pu |
| 4 gunpowder + 2 blaze | 7.5 Pu |
| 6 gunpowder + 2 blaze | 9 Pu |

A blend also burns fast, so it works even in a short barrel. Downsides:

- It's easy to make a round your gun or your case can't hold. Check the press's preview line.
- Blaze powder catches at **220 Tu**, so blended rounds **cook off in a cooler barrel**.

Lean blends (more than one blaze to two gunpowder) don't burn completely and lose much of their punch.

### Redstone

Redstone is a charge for a **coilgun** only, and it can't be mixed with powder. See [Coilgun](Coilgun.md).

## Shot shells

Put a **nugget** or another kind of shot in the projectile slot instead of a bullet and the press makes a **shot shell**. A **smoothbore** barrel fires shells; a rifled barrel cannot, because rifling spins the pellets into a ring.

Shot is its own item. A plain **nugget** seats as buckshot; **Birdshot**, **Large Shot** and **Flechettes** are forged from a nugget the same way bullets are. The pellet count is the bore's projectile volume cut into balls of the shot's size, up to the shot's own limit:

| Shot | Pellet size | Most pellets | Notes |
| --- | --- | --- | --- |
| **Birdshot** | 2.5 mm | 24 | many small pellets, a wider pattern, a gentle bore wear |
| **Buckshot** (a nugget) | 8 mm | 12 | |
| **Large shot** | 12 mm | 6 | few large pellets, a tighter pattern |
| **Flechette** | 3.5 mm | 16 | darts: low drag, a tight pattern, 1.5 times harder against armor and stone |

For example a 12 mm bore throws about 8 buckshot pellets and a 10 mm bore about 4. The pellets share the shell's energy and leave at the same speed, spread in a cone at least **4°** wide however long the barrel.

## Jar rounds

Seat a case and an iron slug with **nothing in the charge slots** and you get a **jar round**: a coilgun round that carries its drive as an electric charge, a tiny capacitor. It's flat when made. Charge it on a [Round Charger](Round_Charger.md).

## Payloads

The **payload** slot is optional. Whatever you put there is carried in the bullet and used up, one per round:

| Payload | Result |
| --- | --- |
| Glowstone Dust or Redstone Dust | **Tracer**: the bullet draws a bright line along its path. |
| Gunpowder or Blaze Powder | **Incendiary / fuse round**: sets whatever it hits on fire for 5 s, and lights fire where it lands if fire spread is on and you're allowed to build there. It also **lights charges**: TNT within a block of where it lands goes up, candles and campfires light, and a creeper it hits starts its fuse. |
| A dust with a **toxic** perk (lead, osmium) | **Gas round**: poisons what it hits and lets a lingering [fume cloud](Hazards.md) go where it lands, once per round. A stronger perk makes a stronger cloud. |
| **Chill** (ice) | Chills what it hits, as powder snow does. |
| **Cling** | Slows what it hits, as honey does. |
| **Radiant** (uranium) | Withers what it hits and outlines it. |
| **Bounce** (slime) | Throws what it hits upward. |
| **Fire aspect** | Sets what it hits alight. |
| **Echo** | Outlines what it hits so you can see it for a few seconds. |
| **Smite** (silver) or **impaling** (prismarine) | **Silvered or barbed round**: extra damage to the undead, or to sea creatures. |
| **Blink** (ender pearl) | **Hop round**: throws what it hits a few blocks in a random direction, further for a stronger perk. |
| **Siphon** (sculk) | A kill with it yields a little extra experience. |
| A brewed **potion** (any bottle that isn't a dud) | **Potion round**: the potion is sealed in a glass vial in the bullet and let go where the round lands, as a [thrown bottle](Thrown_and_Imbued_Potions.md) lets it go, over half the reach. It breaks on the first thing the round meets, and its strength fades on its own clock like any potion's, so an old round is a weak one. The bottle is used up. |
| A lump of something sticky (**slime ball**, **magma cream**, red mud) | **Anchor round**: glues what it hits to the spot for 5 s (Slowness VIII). Slime also **bounces** (below). |
| A strand of **string** (or any fibrous thread) | **Tether round**: trails a line to the hit. A creature is reeled to you, and if the round hits a block it reels *you* to the block, a grapple with no hook. The pull is stronger the further away it is, up to a cap. |
| A **seed** (any item that plants a crop) | **Seed round**: planted on the farmland the round lands on, if the crop can stand there. The round is the seed spent. |
| Slime (**bounce**) fired at a surface within 4 blocks | **Recoil cancel**: the moment you pull the trigger it throws you *backwards along your aim*, a rocket jump with a pellet. Aim down at your feet and you go up; aim straight ahead and you go back. Nothing within 4 blocks in line, nothing to push off. |

## Loading

Box and belt guns take a [Magazine](Magazine.md). Guns with a cylinder, tube or barrels load loose rounds, one at a time, while you hold **R**.
