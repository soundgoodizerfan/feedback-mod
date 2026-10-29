# Heat

The second half of Feedback is heat: melting, smelting, making steel, tempering, raising steam. It runs on a few simple rules.

## Temperature: Tu

Temperature is measured in **Tu**. Without an instrument you only get words, the way a smith reads metal by its glow:

| Word | Up to |
| --- | --- |
| Warm | 200 Tu |
| Hot | 480 Tu |
| Faintly red | 580 Tu |
| Dark red | 730 Tu |
| Bright red | 930 Tu |
| Glowing orange | 1100 Tu |
| Yellow | 1300 Tu |
| Pale yellow | 1400 Tu |
| White hot | 1500 Tu |
| Brilliant white | 1600 Tu |
| Too bright to judge | above that |

The words crowd together where metalwork happens, but steel's 60 Tu window still sits across the yellow/pale yellow line, so by eye you can get close and never quite land it. Carry a [Thermometer](Thermometer.md) and those turn into figures.

Some landmarks: water boils at 100 Tu, wood chars at 300, lava is 1200, steel is made at 1350–1410, and iron burns at 1470 in a steel batch.

## A fire is a temperature, not an amount

A fire doesn't "add heat points". It has a **temperature**, and whatever sits on it **climbs toward that temperature** and slows down as it gets close. It can never get **hotter** than the fire under it.

So:

- A crucible over a charcoal fire (1220 Tu) settles just under 1220 Tu, however long you wait.
- **Lava** is a fire fixed at **1200 Tu** that never changes. A crucible over lava sits just under 1200 Tu forever. That's very stable, but it's **too cool for steel**.
- To get hotter, you need a hotter fire, and that means air from a [Powered Bellows](Powered_Bellows.md): full air makes a fire **1.5×** as hot.
- [Insulation](Insulation.md) helps a vessel settle closer to its fire and hold its heat once the fire is cut. It can't take it past the fire.

## Heavy vs. light vessels

How fast something heats up and cools down depends on how heavy it is:

- A **light** vessel (a Small Crucible) moves fast: it closes most of the gap to its fire in about **2 seconds**. It responds quickly, but it's twitchy and overshoots.
- A **heavy** vessel (a Large Crucible) takes about **20 seconds** for the same. It's hard to get going and hard to stop, but it holds a temperature almost by itself. A [Refractory Lining](Refractory_Lining.md) doubles a small crucible's sluggishness.

Some processes care how **fast** the temperature is changing as well as what it is (JEI shows this as *Max rate*). Steel allows 5 Tu a tick. A small crucible on a full blown fire is still climbing at about 9 Tu a tick inside steel's band, so it simply can't make steel at full blast; a large one climbs at under 1 Tu a tick.

## Hot items stay hot, and cool down anywhere

When you take something out of a hot vessel, it comes out **at that temperature** and starts cooling straight away, at a steady **1.5 Tu per tick (30 Tu a second)**, every item the same. It cools in your hand, on the ground, in a chest, in a hopper, even in an unloaded chunk. Hover over it to see how hot it is.

Because the cooling is steady, you can work out your time exactly. Steel straight out of the crucible at about 1400 Tu has **about 10 seconds** before it drops under its 1100 Tu working limit, and 17 seconds before it's too cold to work at all (900 Tu). Hot pieces also hurt to hold: see [Hazards](Hazards.md).

Ways to change an item's heat by hand:

- **Drop it in water**: it's cooled instantly. For hot steel this is [quenching](Heat_Treatment.md).
- **Right-click water** with it in hand: it snaps to room temperature.
- **Right-click lava** with it in hand: it's heated to lava temperature instantly. It's crude and uncontrolled, but it needs no machine.

### Merging heated stacks

Heat is part of what a stack is, so two ingots taken from a crucible a moment apart differ by a few Tu and do not stack. **Right-click** a held stack onto a stack of the same item in any inventory, chest or machine slot, and the two take the **average heat of both** (weighted by count; heat is shared, not made) and merge, as many as the slot holds. It works on any item that differs only in heat. A left-click never does this: the stacks just do not combine.

## Room temperature

Everything cools toward the room's temperature: **20 Tu** in the Overworld, **40 Tu** in the Nether.

## Processes

Melting, smelting, steel, ceramics and tempering are all **thermal processes**. JEI shows each one with:

- **Tu**: the temperature band where it works.
- **Optimal**: where it goes fastest.
- **Hold**: how long it takes if you keep it exactly at the optimum. Further from the optimum, it takes longer.
- **Max rate**: if listed, how fast the temperature may be changing while it works.
- **Spoils**: above this temperature the batch is **ruined instantly** (for example, iron burns to *Burnt Iron*).

If you drift out of the band, progress **slips back** as fast as it would have gained; it doesn't reset. Drift too hot and you risk the spoil line. The vessel doesn't know any of this. It keeps heating whether your batch is ready or not.

## While you're away

A vessel in a chunk that unloads doesn't freeze. When you come back, it's brought up to date as if you'd been there: its fire **burns the fuel it had** (unblown; nobody was working the bellows), the vessel follows the fire while it was lit, and cools toward the room once it went out.

What isn't caught up is **progress**. A batch in the crucible doesn't finish while you're away; it picks up where it left off, at whatever temperature the crucible is now.

## The blocks

| Block | Role |
| --- | --- |
| [Firebox](Firebox.md) | Burns fuel under a crucible or boiler |
| [Fluid Burner](Fluid_Burner.md) | Burns liquid or gas fuel under a vessel |
| [Heating Coil](Heating_Coil.md) | Electric heat under a vessel |
| [Crucible](Crucible.md) | The open, measurable vessel for steel, melting and alloys |
| [Furnaces](Furnaces.md) | Furnace, Smoker, Crude Blast Furnace, Improved Furnace, Kiln, Annealing Furnace |
| [Powered Bellows](Powered_Bellows.md) | Makes a fire hotter |
| [Insulation](Insulation.md) / [Damper](Damper.md) | Keep heat in / let heat out |
| [Bimetallic Strip](Bimetallic_Strip.md) | A built-in thermostat |
| [Thermometer](Thermometer.md) / [Temperature Sensor](Temperature_Sensor.md) | Measuring |
| [Heat Exchanger](Heat_Exchanger.md) | Moves heat between two blocks |
| [Boiler](Boiler.md) / [Steam Engine](Steam_Engine.md) / [Condenser](Condenser.md) | Heat into rotation, and the water back |
| [Reactor](Reactor.md) | A heated, pressurised vessel for oil and plastic chemistry |
| [Gas Turbine](Gas_Turbine.md) / [Flare Stack](Flare_Stack.md) | Burn fuel gas for rotation / burn it off |
| [Pressure Vessel](Pressure_Vessel.md) | Stores compressed air, and explodes |

## Guides

- [Making Steel](Steel.md)
- [Heat Treatment](Heat_Treatment.md): hardening and tempering
- [Casting and Alloys](Casting.md)
