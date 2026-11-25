# Lathe

Spins a rod and cuts it continuously with a [Lathe Bit](Machine_Heads.md), for as long as it's turning. Rods become turned rods, and if you leave them they become swarf.

The first part cut on a lathe enters the [Machine Age](Ages.md) for you and any player nearby.

## Crafting

**Lathe:**

```
I I I
B S B
I I I
```

I is a Steel Plate, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [hammer](Hammer.md) in the tool row and 3 [fasteners](Fasteners.md). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors and Materials Treatise I: Machine-Shop Consumables ([Treatise](Treatise.md)).

The bit is a [machine head](Machine_Heads.md): draw it at the Blueprint Table and put it together in a crafting grid.

## Setting it up

The lathe is a vertical lathe that sits **directly on a shaft line**; connect it from any side. The faster it turns, the faster it cuts. It's a **40 Su** load.

It has two slots:

- **Right-click with a Lathe Bit** to seat it; the bit it replaces comes back.
- **Right-click with a thread rod**, rolled or cut, to seat it as the carriage screw; the screw it replaces comes back.
- **Right-click with anything else** to chuck a workpiece (or swap it). It lies on the chuck table, under the tail bearing.
- **Right-click with an empty hand** to take the workpiece out, then the bit, then the screw.
- **Sneak-right-click with an empty hand** switches between a **light** and a **heavy** cut.

There's no cutting without a bit.

### The carriage screw

The carriage screw feeds the bit, and it sets how close to its measure each piece comes out. A bare carriage, slid by hand, cuts as it always has. A seated screw feeds the bit more steadily. A **cut** thread rod has almost no play, so the cut is strongest and diameters come out steadiest. A **rolled** thread rod feeds as well but has play, so the cut varies more. A softer metal flexes more than a harder one.

## What it makes

**Rod → Turned Rod → Swarf**

A turned rod is also cut to a **Thread Cut** rod, a metal's rod with a thread on it. A cut thread rod is turned to a **Cut Screw** or **Cut Machine Bolt** ([Fasteners](Fasteners.md#thread-forms)).

Like every machine, it doesn't stop when the rod is finished. Leave it and it cuts the rod away to swarf. Swarf also pops out of the top while it works.

It also turns [gun parts](Gun_Parts.md):

- **Grips**: from an ingot of any gun metal, or from a **Stick** (wood), a **Bone** or **Bamboo**. A grip doesn't need to be strong.
- **Coil Springs**: wound from a wire (from the [Wire Drawer](Wire_Drawer.md)) of any metal. Any metal's spring works, and the metal sets the stiffness. [Clutches](Clutch.md), [Dampers](Damper.md), [Air Valves](Air_Valve.md), [Dispensers](Dropper_and_Dispenser.md) and [Magazines](Magazine.md) take one.
- **Suppressors**, from a cylinder.
- **Coins**, from a plate (the coin a [Coin flip](Skills.md) throws).
- **Gun Barrels**: chucked in the lathe, a barrel's **outside** gets thinner, 0.2 mm per Wu, for as long as it turns. Thin the wall too far and it's swarf.

The bit's **edge hardness** decides what it cuts: everything softer than the edge, as shown on its Blueprint Table card. Steel is 15, hardened steel 69, netherite 130 and tungsten carbide 157; a harder edge, or the [Grinding Wheel](Grinding_Wheel.md), reaches stock the edge cannot. The bit's material and shape also set the ceiling that the cut may drive it to.

## Light vs. heavy cut

**Heavy** (40 St) cuts more than twice as fast. **Light** (18 St) is slower, so there's more time to catch the turned rod before it's ruined. A bit with a lower ceiling than the cut caps it at its own.

## The lathe is also a sensor

The lathe reports **how much it has cut off the piece in it**. Link it to the Controller with a [Data Connector](Data_Connector.md) and read it in a [Controller](Controller.md) program, then compare it and flip a [Clutch](Clutch.md) to stop the lathe at exactly the right point. It's one of the cleanest automatic-stop setups in the game.

[Calipers](Instruments.md#calipers) in your inventory show the same progress in Jade.

## Wear

Here **the bit** wears, not the lathe. It wears when it's asked to cut something too hard for it, or keeps cutting once there's nothing left. A worn bit cuts less each pass, down to half condition, but it never stops cutting what it could cut before. Swap in a fresh bit when it gets slow. See [Machine Heads](Machine_Heads.md#wear).
