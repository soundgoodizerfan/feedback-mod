# Pipe

Carries fluid from one place to another. Joined pipes act as one **run** that holds its fluid together, so a long pipe is not a chain of little tanks filling one block at a time.

## Using it

Pipes that touch join into one run. An arm shows towards every neighbour the pipe joins: another pipe, or any block that has a fluid tank or port on the facing side.

- **Each pipe block adds 500 mB** to the run's capacity. Fluid belongs to the run, not to any one block, so it can be drawn from any pipe in it.
- **A pipe never pulls.** It only pushes: each tick the run hands up to **200 mB** to whatever it touches. Something has to put fluid in: a [Pump](Pump.md), a [Gas Compressor](Gas_Compressor.md), or a [tray](Fractionating_Column.md#trays) with something drawn off it (a tray pushes its draw into a pipe beside it).
- A run holds one fluid at a time, at one temperature. Hot fluid makes a hot line; it cools as the line's contents mix.

**Right-click** a pipe with an empty hand to read its run: the fluid, how much it holds of what it can, its temperature in Tu, its pressure in Pu and how many pipes it has.

## Gas and pressure

A liquid run reads 0 Pu. A **gas** run (steam, a vapour, ethylene) has a pressure: the more gas it holds for its size, the higher.

- **Passive flow** (a boiler through a pump, a column's overhead) fills a gas run only up to its volume: **2 Pu**.
- **A [Gas Compressor](Gas_Compressor.md)** forces gas in past that, up to three times the volume: **6 Pu**.
- A gas run pushes out slower at low pressure: about 100 mB a tick for every Pu, to a ceiling of 200.

A run held over **5 Pu** can burst, the same way a [Boiler](Boiler.md#pressure) does: the chance climbs with the pressure, and the run's whole contents go with the explosion. Read it with a [Pressure Gauge](Pressure_Gauge.md) before you push a compressor harder. A [Gas Tank](Gas_Tank.md) at the end of the line gives the gas somewhere to go.

## Notes

- Crude oil, naphtha, kerosene, diesel and their vapours all travel in pipes. See [Crude Oil](Crude_Oil.md).
