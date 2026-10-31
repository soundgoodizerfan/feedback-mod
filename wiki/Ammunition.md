# Ammunition

There's no "9 mm round" item that you craft. A cartridge is made of up to three things:

- a **case**, made of some metal
- a **charge**: powder, or redstone for a [coilgun](Coilgun.md), or nothing at all for a [jar round](#jar-rounds)
- a **projectile**: a bullet, or a nugget for [shot](#shot-shells)

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

A case weaker than the pressure inside it can hurt you even if the gun is fine. The press shows this before you seat anything.

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

## Charges

The charge is loose powder. The press takes **everything** in its charge slots, so the size of the stack you put in is how much powder goes into the round. A case holds **at most 8** items of charge. **Four** of anything is the "standard" charge the spec sheets are read against.

**More charge means more pressure.** More pressure means a faster bullet, but too much is dangerous to the gun and to you. See [Overpressure](Guns.md#overpressure).

### Gunpowder

The standard propellant: **0.75 Pu per item**, so a standard four makes **3 Pu**. It's reliable and fairly gentle. It burns slowly, so it needs a long barrel to burn completely: a 0.2 m barrel burns 85% of it, a 0.1 m barrel less than half. Cooks off at **300 Tu**.

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

Put a **nugget** in the projectile slot instead of a bullet and the press forms it into **shot**: a shell of buckshot pellets. Only a **shotgun** fires shells, and a shotgun fires only shells and **slugs** (an ordinary bullet of its bore).

The pellet count is the bore's projectile volume cut into 8 mm balls, at most 12:

| Bore | Pellets |
| --- | --- |
| 10 mm | 4 |
| 12 mm | 8 |
| 14 mm and up | 12 |

The pellets share the shell's energy and leave at the same speed, spread in a cone at least **4°** wide however long the barrel.

## Jar rounds

Seat a case and an iron slug with **nothing in the charge slots** and you get a **jar round**: a coilgun round that carries its drive as an electric charge, a tiny capacitor. It's flat when made. Charge it on a [Round Charger](Round_Charger.md).

## Payloads

The **payload** slot is optional. Whatever you put there is carried in the bullet and used up, one per round:

| Payload | Result |
| --- | --- |
| Glowstone Dust or Redstone Dust | **Tracer**: the bullet draws a bright line along its path. |
| Gunpowder or Blaze Powder | **Incendiary**: sets whatever it hits on fire for 5 s, and lights fire where it lands if fire spread is on and you're allowed to build there. |

## Loading

Box and belt guns take a [Magazine](Magazine.md). Guns with a cylinder, tube or barrels load loose rounds, one at a time, while you hold **R**.
