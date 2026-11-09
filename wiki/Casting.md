# Casting and Alloys

Melt metal in a [Crucible](Crucible.md) or [furnace](Furnaces.md), pour it into a mold, and wait for it to set.

## Melting

Put meltable things in a hot crucible and they melt into its tank once they reach their melting point:

| Put in | Melts into |
| --- | --- |
| Copper ore, raw copper | Molten Copper |
| Gold ore, raw gold | Molten Gold |
| Iron ore, raw iron, Hematite, Limonite, Magnetite | Molten Cast Iron (see [Making Wrought Iron](Making_Wrought_Iron.md) for wrought iron) |
| Cast Iron | Molten Cast Iron |
| Tin, Zinc or Lead Ingot | Molten Tin / Zinc / Lead |
| Sand or Red Sand | Molten Glass |
| Nether Quartz | Molten Quartz Glass (casts into a Quartz Glass ingot) |
| Scrap, Swarf or Dust of any metal with a molten form | Molten metal (scrap and swarf give 36 mB) |

JEI lists each melting point: tin 232 Tu, lead 327, zinc 420, gold 950, copper 1000, cast iron 1200, glass 1400, iron 1538, wrought iron 1600, quartz glass 1650. An ingot makes one ingot's worth, **144 mB**. An ore makes only part of its metal, and more of it the finer the stage it is melted at ([Ore Processing](Ore_Processing.md#how-much-metal-each-stage-gives)).

Melting takes time. A vessel melts one piece at a time: about half a second for an ingot, nine times that for a block, a fraction of it for dust, and the finer the form the sooner it is done. A crucible's tank holds 1296 mB, nine ingots. If the tank is full, anything else that melts is lost.

## Molds

A mold's shape names a form. Any metal or glass with a molten fluid freezes into that form of itself at its own melting point.

- **Ingot Mold**: casts one ingot. Craft it from three Bricks in a row, a flat tray. Fired clay, so no metal needed to make your first one.
- **Ball Mold**: casts **eight round balls** from one ingot's worth (see [Bullet shapes](Ammunition.md#bullet-shapes)).
- **Bullet Mold**: casts **eight bullets** from one ingot's worth. Crafted from two Wrought Iron Drilled Plates and two Sticks (see [Ammunition](Ammunition.md#bullets)).
- **Block Mold**: casts a **Glass Block** from 144 mB of molten glass. Four Wrought Iron Drilled Plates in a square.
- **Glass Pane Mold**: casts **3 Glass Panes** from 144 mB of molten glass.
- **Glass Bottle Mold**: casts a **Glass Bottle** from 144 mB of molten glass.
- **Atomising Mold**: see [below](#atomising-mold).

```
Block Mold         Glass Pane Mold   Glass Bottle Mold

  D D                D D               D S D
  D D                S                   D
```

D is a Wrought Iron Drilled Plate and S is a Stick.

A mold's tooltip shows what it holds: *Empty*, or the melt and its amount, what it makes, and whether it is *Still molten* or *Set: use it to turn it out*. A melt the mold cannot shape reads *This mold can make nothing of it*.

### Atomising Mold

Made from three Wrought Iron Drilled Plates over a Stick:

```
D D D
_ S _
```

D is a Wrought Iron Drilled Plate and S is a Stick. Set it in a [Casting Basin](Casting_Basin.md), pour a melt into it and right-click the basin with a Water Bucket: the melt shatters at once into **Dust** of that metal. This is the way to make dust of a ductile metal; the [Rolling Mill](Rolling_Mill.md#grinding-to-dust) only grinds brittle stock. Dust melts faster than an ingot, but burns off in an open vessel; see [Materials](Materials.md#dust).

### Tool part molds

- **Pickaxe Head Mold**: casts a **Pickaxe Head**.
- **Axe Head Mold**: casts an **Axe Head**.
- **Shovel Head Mold**: casts a **Shovel Head**.
- **Hoe Head Mold**: casts a **Hoe Head**.
- **Sickle Head Mold**: casts a **Sickle Head**.
- **Saw Blade Mold**: casts a **Saw Blade**, the blade a [Sawblade](Sawblade.md) is made from.

Each takes one ingot's worth, **144 mB**, of copper, bronze, brass, iron, wrought iron, cast iron, tin, lead, zinc, gold or silver, and casts **one cell** of the part in that metal. A cast head is about a third of the weight and durability of a forged one. The part carries its metal: a Copper Pickaxe Head, a Silver Saw Blade. Assemble it at a bench as usual (see [Tools and Weapons](Tools_and_Weapons.md#putting-one-together)). Sword, spear, cleaver and maul heads, and shears, are shaped at a bench instead.

```
P P P    P P    P
  S      P S    P
                S

P P   P P     P P P
  S     S P   S   S
```

The first row is the Pickaxe, Axe and Shovel Head Molds; the second is the Hoe, Sickle and Saw Blade Molds. P is a Brick and S is a Stick.

### Pouring

1. **Right-click the crucible** with an empty mold. It takes a full mold's worth, or nothing.
2. The filled mold is **as hot as the melt**. It's a hot item and cools like one.
3. Once it's cooled enough to set (glass sets below 1400 Tu), **right-click in the air** to knock out the casting.

A [Casting Basin](Casting_Basin.md) under the crucible pours into up to four molds, or into an open pool, without the hand work.

You can pour copper, gold, iron, wrought iron, cast iron, tin, zinc, lead, silver, bronze, brass, glass and quartz glass. Plastic melts ([Polyethylene Melt, Soul Resin Melt](Materials.md#making-polymers)) pour into an Ingot Mold the same way and set at 100 and 150 Tu.

## Alloys

Melt two metals into the **same crucible or furnace** in the right proportions and the tank becomes an alloy:

| Alloy | Mix |
| --- | --- |
| **Bronze** | about 9 parts copper to 1 part tin (90%) |
| **Brass** | about 2 parts copper to 1 part zinc (65%) |
| **Stainless steel** | about 3 parts steel to 1 part chromium and nickel together (74% steel, 18% chromium, 8% nickel) |
| **Manganese steel** | about 7 parts steel to 1 part manganese (87% steel) |
| **Thorium-tungsten** | about 9 parts tungsten to 1 part thorium (90%) |
| **Redstone-chromium** | about 1 part chromium to 1 part molten redstone (each 40 to 60%) |

The ratio has some leeway, but not much. If the mix is **off**, the tank looks **empty**. The metal is still in there, but it isn't anything with a name, so nothing pours. **Add more of whichever metal is short** to bring it back into range.

JEI's *Mixing* card lists every alloy with the share of each metal it needs, and both crucibles are catalysts of that card.

[Furnaces](Furnaces.md) share the tank, so copper and tin make bronze in a furnace as in a crucible.

Alloys that need a solid item beside the melt (the carbides) are made in the [Mixer](Mixer.md#alloying-with-heat). The Mixer can also alloy molten metals with no solid item, as the crucible does. Cermet is pressed and sintered instead, see [Furnaces](Furnaces.md#sintering).

## Glazing

Right-click a crucible holding **Molten Glass** with a **Ceramic** in hand to dip it, and you get **Glazed Ceramic**. The glass has to be hot.

Ceramic itself comes from firing a **Clay Ball** in a [Kiln](Furnaces.md) or other vessel (see JEI). Past 1400 Tu the batch is ruined into Cracked Ceramic.

## Hot dipping

Dip a finished metal piece into a hot bath of zinc or tin and it comes out with a coat of that metal. Tools, weapons and armor qualify: pickaxe, axe, shovel, hoe, sword, shield, sickle, shears, saw blade, hammer, hand knife, spatula, helmet, chestplate, leggings and boots. Only finished pieces take a coat, not raw ingots or plates.

Hold the piece and right-click a vessel (a Thermal Vessel or a Crucible) that holds the melt, at the temperature below:

| Melt | Keep it at | Coat per dip |
| --- | --- | --- |
| Molten Zinc | 430 Tu or more | 0.5 Qu |
| Molten Tin | 240 Tu or more | 0.25 Qu |

Each dip draws **36 mB** from the vessel. The piece comes back at the bath's temperature, and its tooltip names the coat (as in "Zinc plated") and how it came out. The coat adds its metal's corrosion resistance, as in [electroplating](Tools_and_Weapons.md#plating). A plate can also take a coat by [cladding](Rolling_Mill.md#cladding) at the Rolling Mill.

Dipping again thickens the coat:

- **Zinc** lays 0.5 Qu a dip. One to three dips make a sound coat; a fourth burns it.
- **Tin** lays 0.25 Qu a dip. One dip is patchy; two to seven are sound; eight burn it.

The coat is judged by the same thresholds as an [electrolytic coat](Electrolytic_Cell.md#electroplating): under 0.45 Qu is **patchy**, 0.45 to 1.8 Qu is **plated** (a good coat), and over 1.8 Qu is **burnt dark**, a thick coat that holds poorly.

Dipping a piece in a different metal replaces its coat. Dipping it in the same metal adds to the coat it already has. A piece dipped in a bath hotter than its metal can stand picks up flaws, more the further past it goes.

**Overrun.** Dipping on past a coat of 1.8 Qu leaves the piece with forging flaws, one more for each further half Qu, up to 3. Dip well beyond that and every further dip thins the piece itself, the way over-hammering does, until it is scrap.

### Enamel

Enamel is fired, not dipped. Put a piece that can take a coat and a **Glass Ingot** (the frit) in a [Kiln](Furnaces.md) and hold it between **1100 and 1320 Tu**, best at 1250, for the firing time shown in JEI. The piece comes out with a **glass coat** of 0.5 Qu, which is proof against corrosion. The coat's thickness also scales how much corrosion resistance and armor skin the piece gets, and on a shield a thick coat halves the heat that reaches the hand, up to a point.

Glass is brittle. A hard hit chips a share of the coat off, and the tooltip reads **Coat chipped**. Fire the piece in the Kiln again with another Glass Ingot and the coat is made whole. Zinc and tin coats never chip.

## Washing clay

Right-click a vessel (a Thermal Vessel or a Crucible) holding water with dirt, gravel or mud in hand to dip it. Each dip takes one item and 250 mB of water. Dirt and gravel each give 1 **Clay Ball**; mud gives 2.

## Stained glass

Cast glass takes color from a dye. Right-click a vessel (a Thermal Vessel or a Crucible) holding water with a vanilla dye in hand, the way a ceramic is [glazed](#glazing), and **288 mB** of the water becomes 288 mB of that color's dye fluid. The dye fluid exists only inside containers.

Then right-click a vessel holding the dye fluid with a **Glass Block** or **Glass Pane** in hand. It comes out as that color's stained glass block or pane, taking **36 mB** each, so 288 mB of dye stains eight.

## Notes

Tin, zinc and lead grow in the ground like any ore. Kaolinite lies as soft white deposits in the hills (see [Ore Deposits](Ore_Deposits.md)).
