# Fission Reactor

A **Reactor Core** and the lattice of fuel and control rods under it make heat from uranium. The core makes heat only: a [Heat Exchanger](Heat_Exchanger.md) carries it to a [Boiler](Boiler.md), and the boiler's steam drives a [Steam Turbine](Steam_Turbine.md). It is a different machine from the chemical [Reactor](Reactor.md).

**A fission reactor cannot be switched off at once, and it can burst.** Read [Stopping it](#stopping-it) and [Misuse and hazards](#misuse-and-hazards) before you load a rod.

## Crafting

```
L S L
S T S
L S L
```

L is a Lead Plate, S is a Stainless Steel Plate and T is a [Thermometer](Instruments.md#thermometer).

The parts of the lattice have their own recipes: the [Fuel Channel](#fuel-channel) and [Graphite Moderator](#graphite-moderator) below, the [Control Rod Assembly](Control_Rod.md#crafting), the [Scram Trip](Scram_Trip.md#crafting) and the fuel rods ([Nuclear Fuel](Nuclear_Fuel.md#crafting)).

## Building it

The reactor is a hollow cube **7 blocks on a side**. The core sits in the **middle of the top face**.

- **The shell.** The six faces of the cube must be made of dense material: a block whose material has a **density of 7 or more** (lead, steel and iron do; see the density column of [Materials](Materials.md)). The twelve edges of the cube can stay empty or hold anything.
- **Things that count as shell.** A [Heat Exchanger](Heat_Exchanger.md), a [Boiler](Boiler.md) or any other block with a temperature, and a [Scram Trip](Scram_Trip.md), can stand in a face in place of a wall block. Put a heat exchanger in the lid beside the core, with a boiler on its far side.
- **The lattice.** The **5 by 5 by 5 space** inside the cube under the core is where the fuel goes. Fill it with [Fuel Channels](#fuel-channel), [Control Rod Assemblies](Control_Rod.md) and [Graphite Moderators](#graphite-moderator). Everything else in it does nothing, except that other blocks of moderating material count as moderator.

The core checks its shell about once a second. Right-click the core with an empty hand for a one-line status: its power, temperature, number of live fuel cells and whether the shell is sealed or has holes. A core with holes in its shell still runs, but it smokes. Jade lists the flaws, up to eight wall positions.

## Fuel Channel

A steel-and-stainless cell that holds **one fuel rod**.

```
S S S
S R S
S S S
```

S is a Stainless Steel Plate and R is a Steel Rod.

Right-click with a [fuel rod](Nuclear_Fuel.md) to load it. Right-click with an empty hand to read how burnt the rod is and what condition it is in. **Sneak and right-click with an empty hand** to take the rod out; it keeps its burn and its condition. Breaking the channel drops the rod.

Only a channel with a live rod counts as a fuel cell. An empty channel or one holding a [Spent Fuel Rod](Nuclear_Fuel.md#spent-fuel-rod) makes nothing.

## Graphite Moderator

Four [Graphite](Materials.md#graphite) in a square make one block.

```
G G
G G
```

A moderator slows neutrons, so a fuel cell touching one runs hotter. The block works because graphite's material says it moderates, not because of its name: any block of a moderating material in the lattice does the same.

## How it works

Each fuel cell has a **multiplication**: how many neutrons its fission leaves to cause the next. Jade shows the core's average.

- A uranium rod on its own sits right at **1.0**, the point where the chain just holds. Power appears once the average rises above 1.0 and reaches full at **1.8**.
- Every live fuel cell touching a cell raises it a little. A moderator touching a cell raises it more than a neighboring rod does. Pack rods together and put graphite among them to raise the average.
- [Control rods](Control_Rod.md) pull it down. Rods touching fuel cells take far more off than the same rods elsewhere in the lattice.
- Power does not jump to its target. It chases it with a lag and overshoots a little, so a change takes several seconds to settle. Watch the reading, not the setting.
- **Heat throttles it.** Above about 400 Tu the core makes less power the hotter it gets, so a core with its heat carried away runs freely and a core that cannot shed heat eases itself down. Worn fuel also runs the core lower (the mean condition of the rods scales the power).

Power is shown as a percentage of the design output. 100% is full power; the core can be pushed to 150%. At 100% each fuel cell makes heat steadily and burns its rod down in about two hours ([Nuclear Fuel](Nuclear_Fuel.md)).

### Carrying the heat

The core is heavy: it warms and cools slowly. A [Heat Exchanger](Heat_Exchanger.md) touching the core carries its heat to whatever is on the far side, hot to cold. Put a [Boiler](Boiler.md) there with water in it and the boiler makes steam for a [Steam Turbine](Steam_Turbine.md) or a [Steam Engine](Steam_Engine.md). A core with nothing taking its heat climbs until something gives.

## Stopping it

Dropping the control rods in ends the chain, but a shut-down core is not cold.

- **Decay heat.** After the rods go in, the fuel keeps heating the core at **6% of the last power**, falling away over about **30 minutes**. Keep the heat exchanger and boiler working through that time.
- A [Scram Trip](Scram_Trip.md) in the shell drops every rod by itself when the core passes **850 Tu**, and the core lets the rods out again after it falls to 650 Tu. Without one, nothing drops the rods but you or a [Controller](Controller.md).
- A core in a chunk nobody has loaded keeps cooling and gathering decay heat, but makes no power.

## Misuse and hazards

- **Fuel damage.** Rods lose condition when the core is over **1100 Tu**, and when power runs over **115%**. Worn rods make less power. A rod's condition never falls below a small remainder.
- **Pressure.** Over **900 Tu** the vessel builds pressure, reported in Pu on Jade, like any [pressure vessel](Pressure_Vessel.md). It hisses and puffs steam well before it fails. Above **5 Pu** each tick it may burst, and at **7 Pu** it does. A burst is a steam blast, and the core and a scatter of blocks around it become **Core Slag**.
- **Core Slag.** Slag drops nothing, gives off radiation and keeps doing so for hours of play. See [Radiation](Radiation.md#the-reactor).
- **Radiation while it works.** A running core, every loaded rod and any Spent Fuel Rod give off radiation. See [Radiation](Radiation.md).

## Tips and automation

- A [Controller](Controller.md) can switch a control rod assembly: engaged is all the way in, off is all the way out.
- Read the core with an empty hand, or with Jade: power, multiplication, live fuel cells and their condition, decay heat, vessel pressure, rod positions and shell flaws are all shown.
