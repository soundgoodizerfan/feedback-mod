# Ammunition

There's no "9 mm round" item that you craft. A cartridge is made of three things:

- a **case**, made of some metal
- a **charge**, which is powder (or redstone, for a [coilgun](Coilgun.md))
- a **bullet**, made of some metal

You can also add a **payload** if you like. You put these together at the [Reloading Press](Reloading_Press.md), at whatever size (bore) you set the press to. A gun takes any cartridge of its own bore, whoever made it and whatever it's made of.

Hover over a cartridge to see what it is: size, case metal, bullet metal, what's in the charge and how much pressure it makes against what the case can hold.

## Cases

Draw a **metal plate** through a [Wire Drawer](Wire_Drawer.md) to make a **case** of that metal. Brass is the classic choice. The case has to survive the charge too: a case that's weaker than the pressure inside it can hurt you even if the gun is fine. The press shows this before you seat anything.

## Bullets

Two ways to make them:

- **Cast them.** Pour molten metal into a **Bullet Mold** on a casting setup. One ingot's worth makes eight bullets. Copper, iron, gold, lead, bronze and brass can all be cast this way.
- **Hammer them.** Hammer a **nugget** into a bullet, by hand or with a [Mechanical Hammer](Mechanical_Hammer.md).

**Bullet Mold**: an [Ingot Mold](Casting.md) with two Iron Ingots above and two below.

```
I   I
  M
I   I
```

**Lead** is the usual bullet metal because it's dense: heavy bullets keep their energy. Iron bullets are lighter, but a coilgun *needs* them.

## Charges

The charge is loose powder. The press takes **everything** in its charge slots, so the size of the stack you put in is how much powder goes into the round. A case holds **at most 8** items of charge.

**More charge means more pressure.** More pressure means a faster bullet, but too much is dangerous to the gun and to you. See [Overpressure](Guns.md#overpressure).

### Gunpowder

The standard propellant. It's reliable and fairly gentle. It burns fairly slowly, so it needs a long barrel to burn completely; in a short barrel some of it is wasted.

### Blaze Powder

Blaze powder is pure fuel. On its own it **barely burns**, because there's nothing in it to burn *with*, so a blaze-only round fizzles.

### Blends

The press has a **second charge slot** so you can mix two powders. Gunpowder brings extra oxygen and blaze powder needs oxygen. Blended at about **two gunpowder to one blaze powder**, you get a charge that burns completely and makes **far more pressure** than gunpowder alone. It also burns fast, so it works even in short barrels.

Downsides:

- It's easy to make a round your gun (or your case) can't hold. Check the press's preview line.
- Blaze powder catches at a lower heat, so blended rounds **cook off in a cooler barrel**.

Lean blends (too much blaze powder) burn incompletely and lose most of their punch.

### Redstone

Redstone is a charge for a **coilgun** only, and it can't be mixed with powder. See [Coilgun](Coilgun.md).

## Payloads

The **payload** slot is optional. Whatever you put there is carried in the bullet and used up, one per round:

| Payload | Result |
| --- | --- |
| Glowstone Dust or Redstone Dust | **Tracer**: the bullet draws a bright line along its path. |
| Gunpowder or Blaze Powder | **Incendiary**: sets whatever it hits on fire, and lights fire where it lands if fire spread is on and you're allowed to build there. |

## Loading

Cartridges go into a [Magazine](Magazine.md), and the magazine goes into the gun.
