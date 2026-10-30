# Hazards

Hot metal is hot wherever it is, a full boiler is full of pressure, and a bare wire is live. Feedback lets those things hurt you. None of it stops you doing anything: you can grab a red-hot ingot bare-handed, once, and learn why tongs exist.

## Hot things

### Holding a hot piece

A hot item **in either hand** hurts you **twice a second** while it's hotter than you can handle. In your pockets, your hotbar or a chest it does nothing; only what you're holding counts.

There are three bands, named the way they feel:

| Item temperature | Band | Damage, twice a second | Also |
| --- | --- | --- | --- |
| 100 Tu (water boils) | **Scald** | ½ heart | |
| 300 Tu (wood chars) | **Burn** | 1 heart | sets you alight for 2 s |
| 700 Tu (first red glow) | **Sear** | 2 hearts | sets you alight for 4 s |

You'll see *"Too hot to hold."*

A [Smelter's blood](Brewing_Stand.md) draught raises what a bare hand holds by 150 Tu for a while. It moves the bands up for bare skin only; a piece hotter than the raised ceiling burns as usual. Gloves and tongs are unaffected.

### Touching a hot block

Right-click a hot block (a crucible, a furnace, a hot cable, anything with a temperature) with a bare hand and you take one hit from the same table, *"Too hot to touch."* Whatever you were doing still happens. You just pay for it.

### Protection

**Fire Resistance** protects you from all of it. Otherwise there are two physical answers:

- **[Gloves](#gloves)**: a leather or cloth hand covering; always on, covers warm work.
- **[Tongs](#tongs)**: cost you a hand, cover glowing work.

## Gloves

A [glove](Combat_Gear.md#glove) has a leather or cloth body (wool carpet, shaped at the [Woodworking Table](Woodworking_Table.md)); a metacarpal plate on it adds punch and armour but protects from neither heat nor shock.

```
L   L
L L L
```

L is Leather. Wear them in the **hands** slot (from Curios).

A glove sits against your palm, so you feel a **quarter** of how much hotter a piece is than the room. Wool stands 100 Tu, so in wool gloves you can hold things up to **about 340 Tu**, warm work but nothing glowing. Over that, the gloves aren't protecting you.

Wool gloves also stop electric **shocks below 24 V** (HV). See [Shock](#shock).

Gloves don't wear out. A glove in the hands slot is also what lets you [grab](Combat.md#grab) something, and a [blaze rod](Material_Perks.md) weapon burns a bare hand like any hot piece.

## Tongs

Two **rods** of one metal and one **grip**, in any arrangement in a crafting grid. The tongs are made of exactly those: the tooltip shows *Grip* and *Arms*.

Hold the tongs in **one hand** and the hot piece in the **other**. Tongs put an arm's length between you and the work, so you feel only **a tenth** of the excess. Their limit is the lower of two things:

- **The grip**, which has to insulate. Metal grips don't. Wood and bamboo stand 150 Tu, bone 200 Tu.
- **The arms**, which give out at their metal's melting point.

| Grip | Arms | Safe up to | |
| --- | --- | --- | --- |
| Wood or bamboo | Iron | 1320 Tu | yellow heat: hot-working steel (900–1100 Tu) |
| Bone | Iron | 1600 Tu | iron's melting point: steel fresh from the crucible |
| Bone | Steel | 1450 Tu | steel's melting point |
| Any | Copper | 1000 Tu | copper's melting point |
| Any | Brass | 900 Tu | |

So for **steel straight out of the crucible** (1350–1410 Tu) you want **bone-gripped tongs with iron or steel arms**. Wooden grips are fine once the steel has cooled into its working window.

Holding tongs when the piece isn't hot enough to need them: *"Not hot enough to need them."* Tongs do nothing against electricity. They don't wear out.

Grips are turned on the [Lathe](Lathe.md) from a stick, a bone or a bamboo stalk. Rods come from the [Rolling Mill](Rolling_Mill.md).

## Steam

A [Boiler](Boiler.md) with nowhere to send its steam builds pressure:

- **Weeping:** above 3.5 Pu its seams leak. It hisses, vents a cloud, and **scalds** anyone within about 1.5 blocks for 1 heart at a time.
- **Bursting:** a boiler that keeps climbing explodes, wrecking blocks and scalding everyone within 4 blocks for 4 hearts.

Steam gets in under a glove. Only **Fire Resistance** helps.

## Shock

Two things are live to the touch:

- **Bare cable** carrying current. Cable becomes bare when its insulation burns off from overloading. See [Copper Cable](Copper_Cable.md#overloading).
- A **[Leyden Jar](Leyden_Jar.md)** charged to 6 V or more.

Walking into one shocks you twice a second; right-clicking one shocks you once. Damage goes by voltage band:

| Band | Volts | Damage |
| --- | --- | --- |
| LV | up to 1.5 V | ½ heart |
| MV | over 1.5 V, up to 6 V | 1 heart |
| HV | over 6 V, up to 24 V | 2½ hearts |
| UHV | over 24 V | 5 hearts |

A [Static ground](Brewing_Stand.md) potion cuts shock damage to two fifths while it lasts, and warms any metal held in your hands.

**Armour doesn't help**; a chestplate isn't insulation. Insulating gloves stop anything below 24 V.

Right-clicking a bare live wire with a [Galvanometer](Galvanometer.md) or [Electroscope](Electroscope.md) is still a hand on the wire, and it still shocks you.

## Wards

A [brewed](Brewing_Stand.md) ward lets only a fraction of one kind of hazard through, never all of it, and costs something else while it lasts. Antivenom cuts poison damage to two fifths and slows stamina refilling; Static ground cuts shock damage to two fifths and warms held metal; Heat ward shields from fire at the price of colder cold; Radiation ward lets a little over half of the dose through and slows all healing.

## When you're away

Blocks keep living while you're gone. A crucible over a lit [Firebox](Firebox.md) in a chunk you walked away from **burns its fuel and cools** across the time you were away. When you come back it's where it would have been, not frozen as you left it. See [Heat](Heat.md#while-youre-away).
