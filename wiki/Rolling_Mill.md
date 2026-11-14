# Rolling Mill

A pair of driven rollers that rolls ingots into rods and plates into pipes, and rolls a plate of one metal onto a plate of another as a coat.

## Crafting

```
I B I
S S S
```

I is a Wrought Iron Plate, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 [fasteners](Fasteners.md).

## Using it

The mill sits **directly on a shaft line**; connect it from any side. The faster it turns, the faster it rolls.

The mill has two slots side by side: the **workpiece**, and a **skin** for [cladding](#cladding).

- **Right-click with an item** to put it in the workpiece slot (or swap it for what's there). With a workpiece already in and the skin slot empty, a plate is laid in the skin slot instead.
- **Right-click with an empty hand** to take the skin out first, then the workpiece.
- Hoppers can take items out too.
- Breaking the mill drops both.

## What it makes

**Ingot → Rod** and **Plate → Pipe**, for every metal.

There's nothing further to roll a rod or a pipe into. That makes the mill more forgiving than the [hammer](Mechanical_Hammer.md): a finished piece doesn't get ruined if you leave it. But the mill keeps rolling, and **rolling a finished rod or pipe wears the mill**. So does rolling a piece that is cold-worked to its limit; anneal it first ([Work hardening](Metalworking.md#work-hardening)). Take it out promptly, or use a hopper.

The mill rolls at **44 St**, hard enough for steel (heat steel to 900–1100 Tu first; see [Metalworking](Metalworking.md#hot-metal)). A copper rod is about two strokes; a steel rod about ten. It's a **50 Su** load.

## Grinding to dust

A **brittle** stock (a mineral, crystal, ceramic or glass ingot or gem) does not flatten under the rolls: they grind it to **Dust**. Crushed ore of a brittle ore grinds to dust the same way. A ductile metal cannot be ground; its dust comes from a melt shattered in water in an [Atomising Mold](Casting.md#atomising-mold). Dust reacts and melts faster than the lump it came from ([Materials](Materials.md#dust)). See [Ore Processing](Ore_Processing.md#crushing-and-grinding).

## Cladding

A plate of a better metal can be bonded onto a plate of a cheaper one. The cheap plate is the **base** and the better plate is the **skin**. The base comes out carrying the skin's metal as a coat.

1. Heat both plates to their anneal point or above. This is the *Anneals at* figure JEI shows for the metal; a metal without one needs half its melting point. Heat them in a [Hearth](Hearth.md) or a vessel, and carry them with [tongs](Hazards.md#tongs).
2. Right-click the mill with the **base** plate. It becomes the workpiece.
3. Right-click the mill again with the **skin** plate. It is laid in the skin slot.
4. Power the mill. It rolls the skin onto the base.

If either plate is below its anneal point, the mill says **Too cold to bond: heat both plates first** and does nothing. Nothing is used up and the mill does not wear.

The base comes out as a **clad plate**, with a coat of the skin's metal **1 Qu** thick. That is in the plated band (0.45 to 1.8 Qu), so it is a good coat. The skin plate is used up, and the clad plate takes the cooler of the two temperatures.

A skin rolled at nine tenths of its melting point or hotter comes out **burnt dark**: rough, and it holds poorly. Keep the skin well below that.

A clad plate used as the wearing part at the bench gives the finished tool the coat, and the coat's metal adds its corrosion resistance (see [Plating](Tools_and_Weapons.md#plating)). Shaping a clad plate into [armor](Armor.md) does the same.

If the workpiece is an ingot and not a plate, the mill rolls it as usual and the skin stays laid.

## Why not just use a hammer?

The hammer needs a linkage and someone to stop it in time. A mill running into a hopper can be left alone. It costs more to build and to drive, but it needs less attention.

Pipes go to the [Pipe](Pipe.md) network. Rods go on to the [Wire Drawer](Wire_Drawer.md) and the [Lathe](Lathe.md).
