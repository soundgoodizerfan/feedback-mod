# Dynamotor

Turns [rotation](Rotation.md) into [electricity](Electricity.md) and back. One block, built to order from a frame, a winding, a core and a housing, shaped at a bench and assembled in a crafting grid or at the [Engineering Table](Engineering_Table.md), and switched between **generator** (shaft in, electricity out) and **motor** (electricity in, shaft out).

## Building it

At the [Smithing Table](Smithing_Table.md), the Dynamotor takes four parts:

| Part | What it is | What it sets |
| --- | --- | --- |
| **Machine Frame** (plate) | **size** axis, 2 to 8 | The most torque the machine carries: 2 Su at size 2, 16 at 4, 64 at 8, between them in between. Also how much heat it holds and how large a wall it sheds heat through. |
| **Machine Winding** (wire) | **turns** axis, 1 to 8 | The band it makes and the speed it is rated for, and with the wire's material the current it carries. |
| **Core** (an ingot) | any metal ingot | The core's loss: the share of the power through it that turns to heat. Iron and steel are tuned for it, and a [Rare-Earth Magnet](Rare_Earth_Magnet.md) wastes far less. |
| **Machine Housing** (plate) | one shape | How well it sheds heat to the air: the more heat-conducting its metal, the better. |
| **Cooling** (optional, two plates) | plate or pipe | An addon fitted to the finished machine. A plate is fins, and sheds more heat to the air, more the better its metal conducts. A pipe is a water jacket. |

A bigger frame or winding takes more cells of its stock. Each part also has a fixed standard surface that is never picked ([Composed Parts](Composed_Parts.md)). Every part takes the material it is made of, and the tooltip of the finished machine shows the **band it makes** and its **rated amps**.

### Winding

The winding's turns pick a **band** and a **rated speed**, and the choice is discrete: the turns snap to the nearest of four windings.

| Turns | Winding | Band | Rated speed | Fits |
| --- | --- | --- | --- | --- |
| **1** | Coarse | LV | 16 RPM | a [Steam Engine](Steam_Engine.md) |
| **2** | Fine | MV | 16 RPM | a [Steam Engine](Steam_Engine.md) |
| **4** | Dense | UHV | 240 RPM | a [Gas Turbine](Gas_Turbine.md) |
| **8** | Heavy-dense | EHV | 240 RPM | a [Gas Turbine](Gas_Turbine.md) |

No winding makes Trunk. The band does not change with shaft speed; to turn a shaft into a different band, change the winding. A generator on a shaft below the rated speed still makes its band, but fewer packets each tick: the output scales with speed up to the rated speed. A stopped shaft gives nothing.

The conductor must carry current: the winding's wire material sets how many amps it holds (copper is the reference; [aluminum](Making_Aluminum.md) carries less). **Rated amps** are the smaller of two limits: what the frame's torque allows at that band and what the wire can carry, which grows with the winding's size. A large frame on a heavy winding is held to the frame's limit; a small frame on one is held to almost nothing.

### Bearing

An optional **bearing** sets how much torque the rotor carries and how much friction heat it makes. A **Sleeve Bearing** is cheap and runs warm. A **Ball Bearing** runs cool and carries a fifth more torque. A **Roller Bearing** carries the most, nearly half as much again, with a little friction. A machine with no bearing carries the frame's own torque. A bearing is a race and a set of **balls**: the balls' material is its own stock slot, and their friction scales the bearing's drag. See [Composed Parts](Composed_Parts.md).

## Placing and switching

Place it **against the end of a shaft**. That face, the shaft face, is where it reads or drives the shaft. Any other face connects to [cable](Copper_Cable.md). A [Wrench](Wrench.md) turns its facing.

**Sneak-right-click with an empty hand** to switch between generator and motor; a strip under the crosshair lists both with the current one lit.

It drops as the item it was built as, with its condition, temperature and water.

## As a generator

It puts its winding's band on the line whenever the shaft turns. The packets a tick it offers are the rated amps, scaled by how much of its rated speed the shaft turns at. The shaft feels what the line draws: the more Eu a tick taken, the more load on the shaft, and a machine nobody draws from costs its shaft nothing. Fully loaded, a small machine on a coarse winding costs a shaft very little, while a large machine on a heavy-dense winding costs the full 64 Su of its frame, so only a turbine-class shaft such as a Gas Turbine or [Steam Turbine](Steam_Turbine.md) can drive it.

A line can draw up to twice the rated amps; past that every load gets the same share of what it asked for. See [Electricity](Electricity.md#how-a-line-behaves).

## As a motor

A motor drives its shaft toward a no-load speed set by the band it is fed: at its winding's own band that is its rated speed, and each band above is four times as fast. A long cable that shrinks the packet slows it.

It asks for current in proportion to how far below its no-load speed it is running. An idle motor already at speed draws almost nothing; a loaded motor slows, draws more and settles where its current carries the load; an under-powered motor runs slow and never stalls outright. Two motors on one shaft share the load, and a motor can share a shaft with a [belt](Pulleys.md).

### Changing band

A motor turning a generator wound differently is a **motor-generator set**, one way to change band in Feedback. A motor on a higher-band winding driving a generator on a lower-band winding steps the band down with more amps; the other way round steps it up. The only losses are the shaft's friction and the machines' own heat. See [Electricity](Electricity.md#changing-band).

## Heat and wear

The winding (amps squared times the wire's resistance) and the core (a share of the power through it) both give off heat. The housing sheds it to the air, better the more heat-conducting its metal, over the frame's wall area.

A machine with a **pipe cooling** addon (a water jacket) has a tank of **2,000 mB**. Right-click it with a water bucket, or any container of water, to fill it; nothing drains back out. While it holds water and the machine is above room temperature, it sheds heat twice as fast and uses water slowly.

Misuse heats the machine more and wears it:

- as a generator, drawing more than the rated amps;
- as a motor, being fed a band above the one its winding makes;
- running hotter than the housing metal's service temperature.

**Ward** on any of its parts slows this wear ([Enchanting Table](Enchanting_Table.md#on-a-machine)). Wear takes condition down to **half** and no further. The machine never refuses a line and is never destroyed; a worn one delivers less. An [Overspeed Trip](Overspeed_Trip.md) on the shaft protects a motor that is fed too high a band and spins too fast.

## Uses

- A steady supply that never runs out, from any shaft you already have.
- A fine winding on a steam engine makes MV, enough to charge an MV-rated [Battery](Battery.md) through a [Charger](Charger.md).
- A heavy-dense winding on a turbine makes EHV and powers an [Arc Furnace](Arc_Furnace.md#power).
- A motor starts a [Gas Turbine](Gas_Turbine.md) spinning.
