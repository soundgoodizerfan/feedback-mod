# Dynamotor

Turns [rotation](Rotation.md) into [electricity](Electricity.md) and back. One block, built to order at a bench from a frame, a winding, a core and a housing, and switched between **generator** (shaft in, volts out) and **motor** (volts in, shaft out).

## Building it

At the [Smithing Table](Smithing_Table.md), the Dynamotor takes four parts:

| Part | What it is | What it sets |
| --- | --- | --- |
| **Machine Frame** (plate) | small, medium or large | The most torque the machine carries: 2, 16 or 64 Su. Also how much heat it holds and how large a wall it sheds heat through. |
| **Machine Winding** (wire) | coarse, fine, dense or heavy-dense | The volts per RPM, and with the wire's material the current it carries. |
| **Core** (an ingot) | any metal ingot | The core's loss: the share of the power through it that turns to heat. Iron and steel are tuned for it, and a [Rare-Earth Magnet](Rare_Earth_Magnet.md) wastes far less. |
| **Machine Housing** (plate) | finned or jacketed | How well it sheds heat to the air, and whether it takes a water jacket. |

A bigger frame, winding or housing variant takes more cells of its stock. Every part takes the material it is made of, and the tooltip of the finished machine shows its **volts per RPM** and its **rated amps**.

### Winding

The winding's turns set the volts per RPM. The same figure is the **Su each amp costs the shaft**, so a machine nobody draws from costs its shaft nothing.

| Winding | Volts per RPM | At a [Steam Engine](Steam_Engine.md)'s 16 RPM | At a [Gas Turbine](Gas_Turbine.md)'s 240 RPM |
| --- | --- | --- | --- |
| **Coarse** | 0.094 V | 1.5 V (LV) | 22.5 V |
| **Fine** | 0.375 V | 6 V (MV) | 90 V |
| **Dense** | 1.5 V | 24 V (HV) | 360 V (EHV) |
| **Heavy-dense** | 6 V | 96 V (UHV) | 1,440 V (Trunk) |

Voltage is **shaft speed times the winding's figure**, and nothing else. To get a higher voltage, gear the shaft up with [cogs](Cogs.md) or [pulleys](Pulleys.md). A slow river gives a low voltage, and a shaft that is slowing down gives a falling one.

The conductor must carry current: the winding's wire material sets how many amps it holds (copper is the reference; [aluminum](Making_Aluminum.md) carries less). **Rated amps** are the smaller of two limits: what the frame's torque allows (frame Su divided by volts per RPM) and what the wire can carry, which grows with the winding's size. A large frame on a dense winding is held to the frame's limit; a small frame on a heavy-dense winding is held to almost nothing.

### Bearing

An optional **bearing** sets how much torque the rotor carries and how much friction heat it makes. A **Sleeve Bearing** is cheap and runs warm. A **Ball Bearing** runs cool and carries a fifth more torque. A **Roller Bearing** carries the most, nearly half as much again, with a little friction. A machine with no bearing carries the frame's own torque.

## Placing and switching

Place it **against the end of a shaft**. That face, the shaft face, is where it reads or drives the shaft. Any other face connects to [cable](Copper_Cable.md). A [Wrench](Wrench.md) turns its facing.

**Sneak-right-click with an empty hand** to switch between generator and motor.

It drops as the item it was built as, with its condition, temperature and water.

## As a generator

Volts follow the shaft's speed; the current available is the machine's rated amps. Every amp drawn costs the shaft the winding's volts-per-RPM figure in Su. Fully loaded, a small coarse machine costs a shaft very little, while a large machine on a dense winding costs the full 64 Su of its frame, so only a turbine-class shaft such as a Gas Turbine or [Steam Turbine](Steam_Turbine.md) can drive it.

Drawing more than the rated amps makes the voltage **sag**, down to a quarter at twice the rating. See [Electricity](Electricity.md#how-a-line-behaves).

## As a motor

Two rules, both from the winding's figure:

- **Speed comes from voltage.** The motor drives its shaft toward **volts ÷ volts-per-RPM**, its no-load speed.
- **Force comes from current.** Every amp it is given pushes with the volts-per-RPM figure in Su.

It asks for current in proportion to how far below its no-load speed it is running. An idle motor already at speed draws almost nothing; a loaded motor slows, draws more and settles where its current carries the load; an under-powered motor runs slow and never stalls outright. Two motors on one shaft share the load, and a motor can share a shaft with a [belt](Pulleys.md).

### Converting voltage

A motor turning a generator wound differently is a **motor-generator set**, the way to change voltage in Feedback. A fine motor driving a coarse generator turns MV into LV with more amps; a coarse motor driving a fine generator turns LV into MV. The only losses are the shaft's friction and the machines' own heat. See [Electricity](Electricity.md#changing-voltage).

## Heat and wear

The winding (amps squared times the wire's resistance) and the core (a share of the power through it) both give off heat. The housing sheds it to the air, better the more heat-conducting its metal, over the frame's wall area.

A **jacketed** housing has a tank of **2,000 mB**. Right-click it with a water bucket, or any container of water, to fill it; nothing drains back out. While it holds water and the machine is above room temperature, it sheds heat twice as fast and uses water slowly.

Misuse heats the machine more and wears it:

- as a generator, drawing more than the rated amps;
- as a motor, being fed more voltage than its winding is built for (the winding's volts per RPM at 240 RPM);
- running hotter than the housing metal's service temperature.

Wear takes condition down to **half** and no further. The machine never refuses a line and is never destroyed; a worn one delivers less. An [Overspeed Trip](Overspeed_Trip.md) on the shaft protects a motor that is fed too much voltage and spins too fast.

## Uses

- A steady supply that never runs out, from any shaft you already have.
- A fine winding at a steam engine's speed is the reference voltage for a [Charger](Charger.md).
- A dense or heavy-dense winding on a turbine powers an [Arc Furnace](Arc_Furnace.md#power).
- A motor starts a [Gas Turbine](Gas_Turbine.md) spinning.
