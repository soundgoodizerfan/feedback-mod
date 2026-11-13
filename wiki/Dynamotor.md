# Dynamotor

Turns [rotation](Rotation.md) into [electricity](Electricity.md) and back. One block, built to order from a frame, a winding, a core and a housing, shaped at a bench and assembled in a crafting grid or at the [Engineering Table](Engineering_Table.md), and switched between **generator** (shaft in, electricity out) and **motor** (electricity in, shaft out).

## Building it

At the [Smithing Table](Smithing_Table.md), the Dynamotor takes four parts:

| Part | What it is | What it sets |
| --- | --- | --- |
| **Machine Frame** (plate) | **size** axis, 2 to 8 | The most torque the machine carries: 2 Su at size 2, 16 at 4, 64 at 8, between them in between. Also how much heat it holds and how large a wall it sheds heat through. |
| **Machine Winding** (wire) | **turns** axis, 1 to 8 | The band it makes at a given speed, and with the wire's material the current it carries. |
| **Core** (an ingot) | any metal ingot | The core's loss: the share of the power through it that turns to heat. Iron and steel are tuned for it, and a [Rare-Earth Magnet](Rare_Earth_Magnet.md) wastes far less. |
| **Machine Housing** (plate) | one shape | How well it sheds heat to the air: the more heat-conducting its metal, the better. |
| **Cooling** (optional, two plates) | plate or pipe | An addon fitted to the finished machine. A plate is fins, and sheds more heat to the air, more the better its metal conducts. A pipe is a water jacket. |

A bigger frame or winding takes more cells of its stock. Each part also has a fixed standard surface that is never picked ([Composed Parts](Composed_Parts.md)). Every part takes the material it is made of, and the tooltip of the finished machine shows the **band it makes at its reference speed** (240 RPM) and its **rated amps**.

### Winding

The winding's turns set the band it makes at a given shaft speed: more turns, a higher band; turns between the rows land in between. Each row is a band above the one before at the same speed. The turns also set the **Su each amp costs the shaft**, so a machine nobody draws from costs its shaft nothing.

| Turns | Su per amp | At a [Steam Engine](Steam_Engine.md)'s 16 RPM | At a [Gas Turbine](Gas_Turbine.md)'s 240 RPM |
| --- | --- | --- | --- |
| **1** | 0.094 | LV | HV |
| **2** | 0.375 | MV | UHV |
| **4** | 1.5 | HV | EHV |
| **8** | 6 | UHV | Trunk |

The band follows **shaft speed times the winding's turns**, and nothing else. To reach a higher band, gear the shaft up with [cogs](Cogs.md) or [pulleys](Pulleys.md). A slow river gives a low band, and a shaft that is slowing down drops to a lower one; a stopped shaft gives nothing.

The conductor must carry current: the winding's wire material sets how many amps it holds (copper is the reference; [aluminum](Making_Aluminum.md) carries less). **Rated amps** are the smaller of two limits: what the frame's torque allows (frame Su divided by the Su each amp costs) and what the wire can carry, which grows with the winding's size. A large frame on many turns is held to the frame's limit; a small frame on the most turns is held to almost nothing.

### Bearing

An optional **bearing** sets how much torque the rotor carries and how much friction heat it makes. A **Sleeve Bearing** is cheap and runs warm. A **Ball Bearing** runs cool and carries a fifth more torque. A **Roller Bearing** carries the most, nearly half as much again, with a little friction. A machine with no bearing carries the frame's own torque. A bearing is a race and a set of **balls**: the balls' material is its own stock slot, and their friction scales the bearing's drag. See [Composed Parts](Composed_Parts.md).

## Placing and switching

Place it **against the end of a shaft**. That face, the shaft face, is where it reads or drives the shaft. Any other face connects to [cable](Copper_Cable.md). A [Wrench](Wrench.md) turns its facing.

**Sneak-right-click with an empty hand** to switch between generator and motor; a strip under the crosshair lists both with the current one lit.

It drops as the item it was built as, with its condition, temperature and water.

## As a generator

The band follows the shaft's speed; the current available is the machine's rated amps. Every amp drawn costs the shaft the winding's Su per amp. Fully loaded, a small machine on few turns costs a shaft very little, while a large machine on many turns costs the full 64 Su of its frame, so only a turbine-class shaft such as a Gas Turbine or [Steam Turbine](Steam_Turbine.md) can drive it.

A line can draw up to twice the rated amps; past that every load gets the same share of what it asked for. See [Electricity](Electricity.md#how-a-line-behaves).

## As a motor

Two rules, both from the winding's figure:

- **Speed comes from the band.** The motor drives its shaft toward its no-load speed: the speed at which the same winding, run as a generator, would make the band it is fed. Each band up is four times as fast, each step up in turns is four times slower, and a long cable that shrinks the packet slows it.
- **Force comes from current.** Every amp it is given pushes with the winding's Su per amp.

It asks for current in proportion to how far below its no-load speed it is running. An idle motor already at speed draws almost nothing; a loaded motor slows, draws more and settles where its current carries the load; an under-powered motor runs slow and never stalls outright. Two motors on one shaft share the load, and a motor can share a shaft with a [belt](Pulleys.md).

### Changing band

A motor turning a generator wound differently is a **motor-generator set**, one way to change band in Feedback. A motor with more turns driving a generator with fewer turns steps the band down with more amps; the other way round steps it up. The only losses are the shaft's friction and the machines' own heat. See [Electricity](Electricity.md#changing-band).

## Heat and wear

The winding (amps squared times the wire's resistance) and the core (a share of the power through it) both give off heat. The housing sheds it to the air, better the more heat-conducting its metal, over the frame's wall area.

A machine with a **pipe cooling** addon (a water jacket) has a tank of **2,000 mB**. Right-click it with a water bucket, or any container of water, to fill it; nothing drains back out. While it holds water and the machine is above room temperature, it sheds heat twice as fast and uses water slowly.

Misuse heats the machine more and wears it:

- as a generator, drawing more than the rated amps;
- as a motor, being fed a band above the one its winding makes at 240 RPM;
- running hotter than the housing metal's service temperature.

Wear takes condition down to **half** and no further. The machine never refuses a line and is never destroyed; a worn one delivers less. An [Overspeed Trip](Overspeed_Trip.md) on the shaft protects a motor that is fed too high a band and spins too fast.

## Uses

- A steady supply that never runs out, from any shaft you already have.
- A fine winding at a steam engine's speed makes MV, enough to charge an LV [Battery](Battery.md) through a [Charger](Charger.md).
- A winding of 4 or 8 turns on a turbine powers an [Arc Furnace](Arc_Furnace.md#power).
- A motor starts a [Gas Turbine](Gas_Turbine.md) spinning.
