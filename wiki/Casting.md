# Casting and Alloys

Melt metal in a [Crucible](Crucible.md), pour it into a mold, and wait for it to set.

## Melting

Put meltable things in a hot crucible and they melt into its tank once they reach their melting point:

| Put in | Melts into |
| --- | --- |
| Copper ore, raw copper | Molten Copper |
| Gold ore, raw gold | Molten Gold |
| Iron ore, raw iron | Molten Wrought Iron (needs a very hot, blown fire) |
| Hematite, Limonite, Magnetite | Molten Iron (pure iron, a little cooler to melt) |
| Cast Iron | Molten Cast Iron |
| Tin, Zinc or Lead Ingot | Molten Tin / Zinc / Lead |
| Sand or Red Sand | Molten Glass |
| Nether Quartz | Molten Quartz Glass |

JEI lists each melting point: tin 232 Tu, lead 327, zinc 420, gold 950, copper 1000, glass 1400, iron 1538, wrought iron 1600, quartz glass 1650. One item makes one ingot's worth, **144 mB**. A crucible's tank holds 1296 mB, nine ingots. If the tank is full, anything else that melts is lost.

## Molds

- **Ingot Mold**: casts one ingot. Craft it from three Bricks in a row, a flat tray. Fired clay, so no metal needed to make your first one.
- **Bullet Mold**: casts **eight bullets** from one ingot's worth. Crafted from two Wrought Iron Drilled Plates and two Sticks (see [Ammunition](Ammunition.md#bullets)).
- **Glass Block Mold**: casts a **Glass Block** from 144 mB of molten glass. Four Wrought Iron Drilled Plates in a square.
- **Glass Pane Mold**: casts **3 Glass Panes** from 144 mB of molten glass.
- **Glass Bottle Mold**: casts a **Glass Bottle** from 144 mB of molten glass.

```
Glass Block Mold   Glass Pane Mold   Glass Bottle Mold

  D D                D D               D S D
  D D                S                   D
```

D is a Wrought Iron Drilled Plate and S is a Stick.

A mold's tooltip shows what it holds: *Empty*, or the melt and its amount, what it makes, and whether it is *Still molten* or *Set: use it to turn it out*. A melt the mold cannot shape reads *This mold can make nothing of it*.

### Pouring

1. **Right-click the crucible** with an empty mold. It takes a full mold's worth, or nothing.
2. The filled mold is **as hot as the melt**. It's a hot item and cools like one.
3. Once it's cooled enough to set (glass sets below 1400 Tu), **right-click in the air** to knock out the casting.

A [Casting Basin](Casting_Basin.md) under the crucible pours into up to four molds, or into an open pool, without the hand work.

You can pour copper, gold, iron, steel, wrought iron, cast iron, lead, bronze, brass, glass and quartz glass. Tin and zinc can be melted (for alloys) but not cast on their own. Plastic melts ([Polyethylene Melt, Soul Resin Melt](Materials.md#making-polymers)) pour into an Ingot Mold the same way and set at 100 and 150 Tu.

## Alloys

Melt two metals into the **same crucible** in the right proportions and the tank becomes an alloy:

| Alloy | Mix |
| --- | --- |
| **Bronze** | about 9 parts copper to 1 part tin (90%) |
| **Brass** | about 2 parts copper to 1 part zinc (65%) |
| **Stainless steel** | about 3 parts steel to 1 part chromium and nickel together (74% steel, 18% chromium, 8% nickel) |
| **Manganese steel** | about 7 parts steel to 1 part manganese (87% steel) |
| **Cermet** | about 6 parts titanium carbide to 4 parts iron, nickel and cobalt (60% titanium carbide) |
| **Thorium-tungsten** | about 9 parts tungsten to 1 part thorium (90%) |

The ratio has some leeway, but not much. If the mix is **off**, the tank looks **empty**. The metal is still in there, but it isn't anything with a name, so nothing will pour. **Add more of whichever metal is short** to bring it back into range.

Ordinary [furnaces](Furnaces.md) can melt metal but only hold one kind at a time, so alloys need a crucible.

Some alloys take a solid fed into a melt instead. Right-click a crucible or vessel with the solid in hand and it dissolves into the metal, turning the bath into the alloy:

| Dip into | Hold | Makes |
| --- | --- | --- |
| 144 mB molten titanium (1700 to 2000 Tu) | Graphite | 144 mB **titanium carbide** |
| 144 mB molten tungsten (2400 to 2700 Tu) | Graphite | 144 mB **tungsten carbide** |
| 16 mB molten chromium | Redstone Block | 144 mB **redstone-chromium** |

## Glazing

Right-click a crucible holding **Molten Glass** with a **Ceramic** in hand to dip it, and you get **Glazed Ceramic**. The glass has to be hot.

Ceramic itself comes from firing a **Clay Ball** in a [Kiln](Furnaces.md) or other vessel (see JEI).

## Washing clay

Right-click a vessel (a Thermal Vessel or a Crucible) holding water with dirt, gravel or mud in hand to dip it. Each dip takes one item and 250 mB of water. Dirt and gravel each give 1 **Clay Ball**; mud gives 2.

## Stained glass

Cast glass takes color from a dye. Right-click a vessel (a Thermal Vessel or a Crucible) holding water with a vanilla dye in hand, the way a ceramic is [glazed](#glazing), and **288 mB** of the water becomes 288 mB of that color's dye fluid. The dye fluid exists only inside containers.

Then right-click a vessel holding the dye fluid with a **Glass Block** or **Glass Pane** in hand. It comes out as that color's stained glass block or pane, taking **36 mB** each, so 288 mB of dye stains eight.

## Notes

Tin, zinc and lead grow in the ground like any ore. Kaolinite lies as soft white deposits in the hills (see [Ore Deposits](Ore_Deposits.md)).
