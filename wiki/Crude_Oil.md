# Crude Oil

Black, flammable and underground. Crude oil is found in **oil basins**, hidden pools of oil in the rock, and refined in a [Fractionating Column](Fractionating_Column.md) into naphtha, kerosene and diesel.

## Finding a basin

A basin is a flat circle of oil-soaked rock, about 40 blocks across the radius, lying between **y -48 and y 24** in the **Overworld**. It forms under plains, sunflower plains, desert, savanna, badlands, swamp, forest and river.

- **Tar seeps** are the sign on the surface: a thin black patch on solid ground above a basin.
- A [Survey Meter](Survey_Meter.md) lists **Oil basin** among the ores, with a rough size and a bearing towards the centre. It reads from how close the nearest basin's circle comes, rounded and wobbling like any other reading.

### Tar seeps

A seep bubbles gas, and it burns if lit. A **Copper Bucket** used on it scoops **250 mB** of weathered crude: heavy, with almost none of the light fraction left. The seep then runs dry and refills after a minute. Scooping a seep does not draw down the basin.

### Getting oil out of the ground

A derrick bores down to the basin and a wellhead on top controls the flow, with a choke you can set small, half or open. Right-click the wellhead for its state. A basin that is drawn on faster than it can sustain "cones": its light fraction is stripped. See the in-game Fluid Vein card in JEI for each basin's depth, size, drive and grade.

## Grades

Crude is one fluid, but every drop carries a **grade**: its density, its sulfur and its **light fraction** (how much distils off light). A typical basin gives density 0.85, 1% sulfur and a 30% light fraction. A seep gives a heavier, weathered crude with 5% light fraction.

- **Grades blend by mass** when crudes mix in a tank or a pipe. A tank never filters by grade; whatever you take out is the blend.
- A column reads the grade it is fed and never asks where it came from. More light fraction means more of every cut.

## Products

A column boils crude into three cuts, each as a liquid and as a vapour.

| Fluid | Boils at | Flash point | Notes |
| --- | --- | --- | --- |
| Naphtha | 100 Tu | 10 Tu | the lightest, ignites easiest |
| Kerosene | 180 Tu | 70 Tu | |
| Diesel | 300 Tu | none | does not give off vapour on its own |

Crude oil itself flashes at 15 Tu. Heavy Oil is a fluid of its own, near black. Naphtha Vapour, Kerosene Vapour and Diesel Vapour are the gases the column makes before the cuts are condensed. All of them can be piped ([Pipe](Pipe.md)), pumped ([Pump](Pump.md)) and stored in tanks. Fluids are tinted on the water texture: crude and heavy oil near black, naphtha pale straw, kerosene gold, diesel amber, vapours translucent.

## Fire

All of these burn.

- **Open and hot is dangerous.** An unsealed vessel, a pool or a seep holding liquid **hotter than its flash point** gives off flammable vapour that spreads through the air above it. Crude gives off less the less light fraction it has.
- Any open flame or lit [Firebox](Firebox.md) in or beside the cloud sets it off in a **flash fire** that runs through the cloud and burns whatever stands in it, then flashes back to the sources it touched.
- Sealed and cold is safe. A closed tank or a closed pipe gives off nothing.
- A [Gas Compressor](Gas_Compressor.md) heats the vapour it moves. Naphtha vapour on a hot outlet is the one to watch.
