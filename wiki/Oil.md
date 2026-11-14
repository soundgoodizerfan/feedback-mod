# Oil

Black, flammable and underground. Crude oil is found in **oil basins** and refined in a [Fractionating Column](Fractionating_Column.md) into naphtha, kerosene and diesel. The Nether's counterpart is **soul oil**, found in basins under soul sand valleys or cooked from soul sand in a [Reactor](Reactor.md).

## Crude oil

### Finding a basin

A basin is a flat circle of oil-soaked rock, about 40 blocks across the radius, lying between **y -48 and y 24** in the **Overworld**. It forms under plains, sunflower plains, desert, savanna, badlands, swamp, forest and river.

- **Tar seeps** are the sign on the surface: a thin black patch on solid ground above a basin.
- A [Survey Meter](Instruments.md#survey-meter) lists **Oil basin** among the ores, with a rough size and a bearing towards the center. It reads from how close the nearest basin's circle comes, rounded and wobbling like any other reading.

### Tar seeps

A seep bubbles gas, and it burns if lit. A **Copper Bucket** used on it scoops **250 mB** of weathered crude: heavy, with almost none of the light fraction left. The seep then runs dry and refills after a minute. Scooping a seep does not draw down the basin.

### Getting oil out of the ground

A derrick bores down to the basin and a wellhead on top controls the flow, with a choke you can set small, half or open. Right-click the wellhead for its state. A basin that is drawn on faster than it can sustain "cones": its light fraction is stripped. See the in-game Fluid Vein card in JEI for each basin's depth, size, drive and grade.

### Grades

Crude is one fluid, but every drop carries a **grade**: its density, its sulfur and its **light fraction** (how much distils off light). A typical basin gives density 0.85, 1% sulfur and a 30% light fraction. A seep gives a heavier, weathered crude with 5% light fraction.

- **Grades blend by mass** when crudes mix in a tank or a pipe. A tank never filters by grade; whatever you take out is the blend.
- A column reads the grade it is fed and never asks where it came from. More light fraction means more of every cut.

### Products

A [Fractionating Column](Fractionating_Column.md) boils crude into three cuts, each as a liquid and as a vapor.

| Fluid | Boils at | Flash point | Notes |
| --- | --- | --- | --- |
| Naphtha | 100 Tu | 10 Tu | the lightest, ignites easiest |
| Kerosene | 180 Tu | 70 Tu | |
| Diesel | 300 Tu | none | does not give off vapor on its own |

Crude oil itself flashes at 15 Tu. Heavy Oil is a fluid of its own, near black; a Reactor cracks it into diesel and naphtha. Naphtha Vapor, Kerosene Vapor and Diesel Vapor are the gases the column makes before the cuts are condensed. All of them can be piped ([Pipe](Pipe.md)), pumped ([Pump](Pump.md)) and stored in tanks. Fluids are tinted on the water texture: crude and heavy oil near black, naphtha pale straw, kerosene gold, diesel amber, vapors translucent. How the cuts burn is on [Fuels](Fuels.md).

### Fire

All of these burn.

- **Open and hot is dangerous.** An unsealed vessel, a pool or a seep holding liquid **hotter than its flash point** gives off flammable vapor that spreads through the air above it. Crude gives off less the less light fraction it has.
- Any open flame or lit [Firebox](Firebox.md) in or beside the cloud sets it off in a **flash fire** that runs through the cloud and burns whatever stands in it, then flashes back to the sources it touched.
- Sealed and cold is safe. A closed tank or a closed pipe gives off nothing.
- A [Gas Compressor](Gas_Compressor.md) heats the vapor it moves. Naphtha vapor on a hot outlet is the one to watch.

## Soul oil

### Soul oil basins

Under soul sand valleys, in the Nether between **y 36 and 90**, lie **soul oil basins**: circles of soul-soaked rock about 32 blocks across the radius.

- **Soul Seeps** mark them on the surface. A **Copper Bucket** used on one scoops **250 mB** of soul oil; the seep refills after a minute, as a tar seep does.
- A [Survey Meter](Instruments.md#survey-meter) lists **Soul oil basin** with a rough size and a bearing, as for crude.
- A derrick bored over one becomes a wellhead with the same choke settings as for crude ([Getting oil out of the ground](#getting-oil-out-of-the-ground)). Soul oil has no grade.

### From soul sand

Soul sand and soul soil, both found in the Nether's soul sand valleys, are cooked in a [Reactor](Reactor.md) and give up three things: **soul oil**, a liquid fuel that carries oxygen of its own; **wisp gas**, a light gas that burns; and **husk sand**, what is left of the sand.

### Making it

1. Build a [Reactor](Reactor.md) and set it on a heat source ([Firebox](Firebox.md), [Fluid Burner](Fluid_Burner.md), [Heating Coil](Heating_Coil.md) or lava). It needs no catalyst bed and no input fluid.
2. Right-click the reactor with **Soul Sand** or **Soul Soil**. It goes into the hopper. One block is one batch.
3. Bring the reactor to **300 to 600 Tu**. It works fastest at **420 Tu** and slows toward either edge. Heat it steadily; a temperature that is swinging stalls it.
4. When the batch finishes, the output tank holds **60 mB of soul oil and 20 mB of wisp gas**, and a **Husk Sand** pops out on top.
5. The output tank pushes itself into whatever touches it: a [Pipe](Pipe.md), a tank, a column. Wisp gas is a gas and counts toward the reactor's [pressure](Reactor.md#pressure), so draw it off.

Past 900 Tu the batch spoils: the sand is used up and you get the wrong products.

### What each is for

| Output | Use |
| --- | --- |
| **Soul Oil** | A liquid fuel for the [Fluid Burner](Fluid_Burner.md), [Engine](Engine.md), [Gas Turbine](Gas_Turbine.md) and [Thruster](Thruster.md). Flame **1050 Tu**, ignition **260 Tu**. |
| **Wisp Gas** | A gas fuel in the same machines. Flame **1050 Tu**, ignition **1000 Tu**. It rises. |
| **Husk Sand** | Nothing uses it; it is the residue. |

Soul oil:

- **It brings its own oxygen.** In a burner or [Firebox](Firebox.md) it burns as if blown on by a [Powered Bellows](Powered_Bellows.md), hotter than its flame figure alone. A [Thruster](Thruster.md) fed it burns where there is no air, in the End or under water, as hard as the fuel's own oxygen allows.
- **It knocks.** Its low ignition point means an [Engine](Engine.md#compression-fuel-and-knock) at high compression lights it before the spark: the engine still runs but wears.
- It thickens the faster it is pushed, so a [pump](Pump.md) turned fast on it moves less.
- It mixes with other fuels in a tank by mass ([Fuels](Fuels.md#mixtures)).

Wisp gas:

- Its ignition point is very high, so an [Engine](Engine.md#compression-fuel-and-knock) misfires on it at medium compression and below. A burner or turbine lights it without trouble.
- A [Reactor](Reactor.md#the-reactions) with a [Cryocooler](Cryocooler.md) liquefies it, at -120 to -100 Tu, into **Liquid Wisp Gas**: 160 mB of gas makes 32 mB.
- It is a gas: it can be held in a [Gas Tank](Gas_Tank.md) and pressurized, and it follows the pressure rules of any gas.

A ghast killed by a player drops a **Ghast Bladder**, and 1 or 2 more with a [Hand Knife](Prep_Table.md) in the main hand. The bladder is the lightest gas-tight skin for a [Gas Cell](Gas_Cell.md); cloth coated in slime is the craftable alternative, heavier and slowly seeping.
