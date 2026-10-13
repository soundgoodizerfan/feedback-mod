# Heat

The second half of Feedback is heat: melting, smelting, making steel, tempering, raising steam. It runs on a few simple rules.

## Temperature: Tu

Temperature is measured in **Tu**. Without an instrument you only get words, the way a smith reads metal by its glow:

*Warm → Hot → Faintly red → Dark red → Bright red → Glowing orange → Yellow → Pale yellow → White hot → Brilliant white → Too bright to judge*

Carry a [Thermometer](Thermometer.md) and those turn into figures.

## A fire is a temperature, not an amount

A fire doesn't "add heat points". It has a **temperature**, and whatever sits on it **climbs toward that temperature** and slows down as it gets close. It can never get **hotter** than the fire under it.

So:

- A crucible over a charcoal fire settles just under the charcoal fire's temperature, however long you wait.
- **Lava** is a fire that never changes. A crucible over lava sits at the same temperature forever. That's very stable, but it's **too cool for steel**.
- To get hotter, you need a hotter fire, and that means air from a [Powered Bellows](Powered_Bellows.md).
- [Insulation](Insulation.md) helps a vessel settle closer to its fire and hold its heat once the fire is cut. It can't take it past the fire.

## Heavy vs. light vessels

How fast something heats up and cools down depends on how heavy it is:

- A **light** vessel (a Small Crucible) moves fast. It heats quickly and responds quickly, but it's twitchy and overshoots.
- A **heavy** vessel (a Large Crucible, or one with a [Refractory Lining](Refractory_Lining.md)) is slow and steady. It's hard to get going and hard to stop, but it holds a temperature almost by itself.

Some processes care how **fast** the temperature is changing as well as what it is (JEI shows this as *Max rate*). A small crucible on a full fire may simply climb too fast for them.

## Hot items stay hot, and cool down anywhere

When you take something out of a hot vessel, it comes out **at that temperature** and starts cooling straight away, at a steady rate. It cools in your hand, on the ground, in a chest, in a hopper, even in an unloaded chunk. Hover over it to see how hot it is.

Because the cooling is steady, *"I have about fifteen seconds to get this to the hammer"* is something you can actually learn and rely on.

Ways to change an item's heat by hand:

- **Drop it in water**: it's cooled instantly. For hot steel this is [quenching](Heat_Treatment.md).
- **Right-click water** with it in hand: it snaps to room temperature.
- **Right-click lava** with it in hand: it's heated to lava temperature instantly. It's crude and uncontrolled, but it needs no machine.

## Room temperature

Everything cools toward the room's temperature. In the **Overworld** that's mild. **The Nether is hotter**, so things cool down less there.

## Processes

Melting, smelting, steel, ceramics and tempering are all **thermal processes**. JEI shows each one with:

- **Tu**: the temperature band where it works.
- **Optimal**: where it goes fastest.
- **Hold**: how long it takes if you keep it exactly at the optimum. Further from the optimum, it takes longer.
- **Max rate**: if listed, how fast the temperature may be changing while it works.
- **Spoils**: above this temperature the batch is **ruined instantly** (for example, iron burns to *Burnt Iron*).

If you drift out of the band, progress slowly **slips back**; it doesn't reset. Drift too hot and you risk the spoil line. The vessel doesn't know any of this. It keeps heating whether your batch is ready or not.

## The blocks

| Block | Role |
| --- | --- |
| [Firebox](Firebox.md) | Burns fuel under a crucible or boiler |
| [Crucible](Crucible.md) | The open, measurable vessel for steel, melting and alloys |
| [Furnaces](Furnaces.md) | Furnace, Smoker, Crude Blast Furnace, Improved Furnace, Kiln, Annealing Furnace |
| [Powered Bellows](Powered_Bellows.md) | Makes a fire hotter |
| [Insulation](Insulation.md) / [Damper](Damper.md) | Keep heat in / let heat out |
| [Bimetallic Strip](Bimetallic_Strip.md) | A built-in thermostat |
| [Thermometer](Thermometer.md) / [Temperature Sensor](Temperature_Sensor.md) | Measuring |
| [Heat Exchanger](Heat_Exchanger.md) | Moves heat between two blocks |
| [Boiler](Boiler.md) / [Steam Engine](Steam_Engine.md) | Heat into rotation |
| [Pressure Vessel](Pressure_Vessel.md) | Stores compressed air, and explodes |

## Guides

- [Making Steel](Steel.md)
- [Heat Treatment](Heat_Treatment.md): hardening and tempering
- [Casting and Alloys](Casting.md)
