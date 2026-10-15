# Round Charger

A clip on an [electrical](Electricity.md) line that charges **jar rounds** for a [coilgun](Coilgun.md). The charge rides in the round, not in the gun: one round, one shot.

## Crafting

```
  W
F P F
```

W is [Copper Wire](Wire_Drawer.md), F is [Copper Foil](Mechanical_Hammer.md) and P is a [Copper Plate](Mechanical_Hammer.md).

## Jar rounds

A **jar round** is a coilgun round seated **dry**: a case and an iron slug at the [Reloading Press](Reloading_Press.md) with nothing in the charge slots. The press says *"Seated dry: a jar round. Charge it on a wire."* It's a tiny capacitor, and it's flat until you charge it.

## Using it

- **Right-click with a stack of jar rounds** to put the whole stack in.
- **Right-click with an empty hand** to take them out.

Connect it to a line. While its rounds hold less than the line's voltage, it draws **1 A** and charges them **up to the line's voltage, never past it**.

A round takes 2 amp-ticks per volt, so at 1 A one round reaches 6 V in **12 ticks**. A stack charges together and takes longer in proportion: sixteen rounds take sixteen times as long.

## How hard a round pushes

A round is rated against **6 V**, which drives the coil exactly as hard as a standard charge of four Redstone Dust. The energy it gives the slug goes up in step with its volts; the **pressure on the winding** goes up as the **square**, as it does with redstone:

| Charged to | Source | Winding pressure, against nominal |
| --- | --- | --- |
| 1.1 V | [Galvanic Cell](Galvanic_Cell.md) | about 3% |
| 1.5 V | coarse [Dynamo](Dynamo.md) at engine speed | 6% |
| 6 V | fine Dynamo at engine speed | 100% |
| 12 V | fine Dynamo on a [Hand Crank](Hand_Crank.md) | 400% |

Overcharging isn't stopped. A round charged past nominal presses the gun's winding harder than it's rated for, and that damages the gun exactly as [overpressure](Guns.md#overpressure) does. Check the *Winding* line on the gun's tooltip.

## Rounds go flat

A charged round loses **0.001 V every tick**, wherever it is: in the charger, in your pocket, in a chest, in a magazine. A full 6 V round is flat in **5 minutes**. Its tooltip says how charged it is: *Flat*, *Weakly charged*, *Charged*, *Crackling*.

Charge them shortly before you need them.
