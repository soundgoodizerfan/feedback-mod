# Propeller

A modular propeller that sits on a [shaft](Shaft.md) and pulls a [vehicle](Vehicles.md) along its facing. It loads the shaft like a drag.

## Building it

Draw the propeller at the [Blueprint Table](Blueprint_Table.md) (pick *Propeller* in the type row), set the diameter, and build it at the Tool Table. A blueprint is required.

| Part | Choices | What it sets |
| --- | --- | --- |
| **Prop Hub** | two, three or four blades | The number of blades. More blades pull harder for the same diameter and weigh more. Made from ingots. |
| **Prop Blades** | fine, medium or coarse pitch | The blade angle. Made from plate. |

The **diameter** is set on the blades, from 80 to 600 cm. The blades' metal needs a measured yield strength: it sets the tip speed the blades survive.

## Using it

Place the propeller on the end of a shaft. The shaft runs along the propeller's facing, and the craft is pushed along that facing. It pulls only when the shaft turns.

With [Jade](https://modrinth.com/mod/jade) it reads **Still**, **Pulling**, **Stalled** or **Blade tips too fast**, and **Blades worn** when they are.

## How it works

Thrust grows with the shaft's turns, the disc diameter, the blade count, the pitch and the air density, and falls as the craft gains airspeed.

- **Pitch.** Fine pitch pulls hard from a standstill and quits at low speed. Coarse pitch pulls less but keeps pulling at speed. Medium is between.
- **Thin air.** Thrust and the load on the shaft both fall with air density. See [air drag](Vehicles.md#air-density).
- **Stall.** Past the speed where a prop's thrust reaches zero it reads *Stalled*.
- **Reaction.** The torque the blades take from the shaft pushes the airframe the other way, so a lone propeller rolls the craft against its spin.
- **Water.** Under water a propeller is a ship's screw, with a load about 816 times that of air.

## Misuse and hazards

Blade tips faster than the metal survives lose thrust and **wear the blades**. The limit is lower in water (about 40 m/s) and near the speed of sound in air. Worn blades pull less. See [Hazards](Hazards.md).
