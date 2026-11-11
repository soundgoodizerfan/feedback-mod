# Boiler

Turns heat into **steam**. Sits over a fire like a [Crucible](Crucible.md) does. Its steam drives a [Steam Engine](Steam_Engine.md) or, from a Large Boiler, a [Steam Turbine](Steam_Turbine.md).

**A boiler with nowhere to send its steam builds pressure, and can explode.** Read [Pressure](#pressure) before you put a valve on one.

A boiler can take its heat from a [Fission Reactor](Fission_Reactor.md) through a [Heat Exchanger](Heat_Exchanger.md).

## Designing and building

A boiler is a modular build. Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Boiler*, or *Large Boiler*, in the type row) and print it on paper; drawing it takes Chemical Treatise I ([Treatise](Treatise.md)). It has one part, the **Boiler Shell** (the **Large Boiler Shell** for the large one), with three choices:

| Choice | What it sets |
| --- | --- |
| **Shape** | Flat, fire tube or water tube. See [Shapes](#shapes). |
| **Wall** | Thickness from 1 to 20 cm. A thicker wall holds more pressure. |
| **Metal** | The plate it is made from. A stronger metal holds more. It must be a metal with a measured yield strength. |

At the [Smithing Table](Smithing_Table.md), put the blueprint in the blueprint slot and shape the shell from plate of the metal you chose. The boiler comes out with the shape, wall and metal you drew, and the blueprint is kept.

The shell sets the pressure the boiler **risks letting go at** (its danger line) and the pressure it **gives way at**. Its seams weep, and a relief valve opens, at pressures that scale with the danger line, so a stronger shell holds steam at a higher pressure before venting.

### Shapes

| Shape | Pressure it carries | Stock |
| --- | --- | --- |
| Flat | 0.35 of a fire tube's rating | least |
| Fire tube | standard | standard |
| Water tube | 1.4 times a fire tube's rating | most |

### Addon

**Hoop bands** (rod) are an optional extra part. Bands round the shell raise the danger line.

## Using it

1. Put it on top of a [Firebox](Firebox.md), over lava, or on a [Heating Coil](Heating_Coil.md).
2. **Fill it with water**: right-click it with a Water Bucket, or feed it with a [Pump](Pump.md). It holds 4000 mB of water and 4000 mB of steam.
3. Once it reaches **120 Tu**, it starts turning water into steam.

Water that is already hot, such as condensate returned from a [Condenser](Condenser.md), needs less heat to boil than cold water: cold feed costs about a sixth more over its climb to 100 Tu.

A boiler needs far less heat than metalwork. It starts boiling well below a red glow, so even a modest fire runs it.

## How much steam

At full output it makes **10 mB of steam a tick**, using 10 mB of water. That keeps **one Steam Engine** running.

Making steam **uses up the boiler's heat**: 20 Work for every mB. A weak fire holds the boiler just at boiling and makes a trickle. Full output needs a fire about **330 Tu hotter** than the boiler, so roughly **450 Tu** or more. Logs (760 Tu) are plenty. If your engine is starving, the fire is too weak, not the boiler too small.

## Getting steam out

A [Steam Engine](Steam_Engine.md) placed **touching** the boiler draws steam from it directly. Put a [Valve](Valve.md) between them to cut the steam off. A [Condenser](Condenser.md) can turn the engine's exhaust back into water for the pump to return. For a longer run, a [Pump](Pump.md) moves steam from the boiler into a [Pipe](Pipe.md).

## Pressure

A boiler holds pressure in Pu. The figures below are for a shell whose danger line is 5 Pu; a stronger or weaker shell moves the weeping and bursting lines with it.



- **Steam it's holding:** 1 Pu for every 1000 mB, so a full steam tank is **4 Pu**.
- **Heat with nowhere to go:** while the steam tank is **full**, the fire keeps pushing and every Tu the boiler climbs past 120 adds **0.004 Pu**.

A boiler sending its steam somewhere spends its heat boiling water and sits near 120 Tu, however big the fire. A **shut** one (valve closed, engine gone, nothing drawing) has no outlet. Its steam tank fills, and then the fire drives it toward the fire's own temperature, and the pressure with it:

| Pressure | What happens |
| --- | --- |
| up to 3.5 Pu | nothing |
| above 3.5 Pu (70% of the danger line) | **Weeping**: the seams leak steam (6 mB a tick for every Pu over), hiss, and **scald** anyone within about 1.5 blocks for 1 heart |
| above the danger line (5 Pu) | each tick there's a chance it **bursts**, climbing the further over it is |
| at the pressure the shell gives way at (7 Pu) | it **bursts** |

A shut boiler on a fire at 870 Tu or hotter reaches 7 Pu, the give-way point of a shell with a 5 Pu line. That's any coal or charcoal fire, or lava. Even a log fire gets it well into the danger zone.

A burst is an explosion that wrecks blocks around it and scalds everyone within 4 blocks for 4 hearts. Steam gets in under gloves; only Fire Resistance helps. See [Hazards](Hazards.md#steam).

Weeping lets a little pressure off, and a boiler only slightly over the line may settle there, hissing. One on a real fire won't. **The warning is the hiss and the cloud.** Open the line or put the fire out.

A boiler that's merely full and idle sits at 4 Pu and weeps a little. That's harmless; it's the fire under a shut boiler that kills it.

## Large Boiler

A **Large Boiler** is ten Boilers in one block: ten times the mass, tanks of 40000 mB of water and 40000 mB of steam, and **100 mB of steam a tick** at full output, enough for a [Steam Turbine](Steam_Turbine.md). It stands on ten times the fire, so it wants a hotter, bigger fire than one Boiler. It builds pressure and bursts like a Boiler if its line is shut under a real fire.

It is built the same way, from a Large Boiler Shell and its own blueprint.

## Notes

- Remember to refill the water, or close the loop with a [Condenser](Condenser.md) and [Pump](Pump.md).
- A [Pressure Gauge](Instruments.md#pressure-gauge) reads the pressure. Without one, watch for the hiss.
