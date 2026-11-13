# Gas Compressor

Squeezes gas into a [pipe](Pipe.md) or a [Gas Tank](Gas_Tank.md) past the point where gas flows in by itself. That is what raises a line's pressure.

## Designing and building

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Gas Compressor* in the type row); drawing it takes Chemical Treatise II ([Treatise](Treatise.md)). It has one part, the **Compressor Rotor**, shaped from plate at the [Smithing Table](Smithing_Table.md). Assemble it at the [Engineering Table](Engineering_Table.md) with two [fasteners](Fasteners.md); the blueprint in the blueprint slot judges the fit against your drawing. The compressor comes out with the design of the rotor, and the blueprint is kept.

The rotor has one measure, the **diameter**, set at the Blueprint Table, from 20 to 80 cm (40 by default). A bigger rotor moves more and loads the shaft more. Its **shape** trades flow, heat and load:

| Rotor | Flow | Heat | Load |
| --- | --- | --- | --- |
| Single stage | standard | standard | standard |
| Twin stage | 1.8 times | 0.8 times | 1.7 times |
| Screw | 1.3 times | 0.6 times | 1.2 times |

**Addon.** An **intercooler** (plate) is an optional piece that takes heat out between stages: the gas leaves 0.6 times as hot.

## Placing

Like a [Pump](Pump.md): place it **against the block it should draw from**. It draws from its back face and pushes out of its front. The shaft goes in **across** the flow: from above or below on a compressor lying sideways, and from the east or west side on one pointing straight up or down.

## Using it

- It moves **gas only**. A liquid in front of it is ignored.
- A standard 40 cm rotor moves **0.4 mB per tick for every RPM** and loads the shaft with **30 Su**.
- It **heats what it moves** by 60 Tu with a standard rotor. A hot line is a cooling job, and with a flammable vapor like naphtha, a hazard. See [Oil](Oil.md#fire).
- Into a pipe run it can push a gas up to three times the run's volume (6 Pu). Into a gas tank it can push all the way up, past the tank's danger line. Into anything else it just fills.

## Air Intake

The **Air Intake** is an upgrade for the compressor: steel plates (S) and iron bars (B).

```
S B S
S B S
```

Use the item on a compressor to fit it (one per compressor); a [Crowbar](Crowbar.md) takes it off. With it, a compressor that has no gas behind it draws open air into its front. How much it draws scales with the air density of the dimension; the End gives 5% of the Overworld's. A compressor with gas behind it works as before.

## Surge

If the front can take nothing more, the compressor **surges**: its condition falls, down to a floor of 20%, and it moves proportionally less. Only this wears it. Running into a line with room never does. Back off, or open the line to a [Gas Tank](Gas_Tank.md) or a consumer.

Read the line with a [Pressure Gauge](Instruments.md#pressure-gauge).

## Attunements

**Edge** on the rotor moves more per turn and takes more to turn; **Spread** the reverse. **Heat** runs the rotor hotter, **Cold** and **Tide** cooler. **Ward** and **Mend** handle surge wear. See the [Enchanting Table](Enchanting_Table.md#on-a-machine) page.
