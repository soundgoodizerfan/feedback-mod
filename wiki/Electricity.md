# Electricity

Electricity in Feedback is made from chemistry, static and shafts; carried on cable; switched and protected; stored in jars and batteries; and spent on magnets, heat, refining and motors.

It is not the "universal" power of other tech mods. Your mechanical and thermal setups never need to be converted into electricity. Electricity is one option among several, good at some jobs and bad at others.

## Bands and amps

Electricity has two numbers, and they're not interchangeable:

- **Band** is the size of the packets a line carries. Some jobs need a minimum band before anything happens at all.
- **Current (A)** is how many packets flow each tick. For most jobs, current is how *fast* things happen.

The band is named, never a number. There are six: **LV**, **MV**, **HV**, **UHV**, **EHV** and **Trunk**. A packet of LV holds **32 Eu** of energy, and every band up holds four times the band below: MV 128, HV 512, UHV 2,048, EHV 8,192, Trunk 32,768 Eu. The power a supply delivers each tick is **packet × amps**, in Eu per tick, so a 1 A cell on LV gives 32 Eu per tick and a 0.5 A pulse on HV gives 256: that is how to compare two supplies.

Each source makes one band (a [Dynamotor](Dynamotor.md) makes the band its winding sets). The [Arc Furnace](Arc_Furnace.md) runs on EHV. Only PTFE insulates Trunk.

Bands don't lock anything. A machine is *rated* for a band; feed it a higher band and it wears and warms, feed it less and it runs slow.

## The pieces

| Block / item | What it does |
| --- | --- |
| **Sources** | |
| [Galvanic Cell](Galvanic_Cell.md) | A small, steady supply of 1 A while its anode lasts: LV for a close pair of electrode metals, MV for a wide pair, one band more for a pack of cells. |
| [Electrostatic Generator](Electrostatic_Generator.md) | A one-second burst each time you turn it: 0.5 A in the HV band. |
| [Dynamotor](Dynamotor.md) | Shaft in, electricity out, as a generator. The winding sets the band; coarse and fine windings make LV and MV at a steam engine's speed, dense and heavy-dense make UHV and EHV at a turbine's. |
| [Battery](Battery.md) | Lead-acid or nickel-iron cells, rated a band: they charge from a line in that band or higher and supply their own band when the line is lower. Beside a generator of the same band they are reserve: the generator carries the load, they cover the shortfall and charge from the surplus. |
| [Leyden Jar](Leyden_Jar.md) | Stores a charge and gives it back as a burst. Fed a band above its rating, it cracks and shocks. |
| **Carrying and stopping** | |
| [Copper Cable](Copper_Cable.md) | Carries 1, 4, 16 or 64 A, by gauge. |
| [Breaker](Breaker.md) | A switch. Trips itself above the rating of its gauge. |
| [Solid-State Converter](Converter.md) | Steps a line's band down one, down two or up one, with a Converter Tap. |
| [Fuse Holder](Fuse.md) | Blows when the current is too high. |
| [Redstone Coherer](Redstone_Coherer.md) | Closed by a nearby spark, opened by a tap. |
| [Cat's Whisker Detector](Cats_Whisker_Detector.md) | Closes briefly on a nearby spark, if you found a good spot. |
| [Relay](Relay.md) | A small current in an electromagnet switches another line. |
| [Oscilloscope](Instruments.md#oscilloscope) | Shows a cable's current on a [Phosphor Screen](Phosphor_Screen.md). |
| **Machines** | |
| [Electromagnet](Electromagnet.md) | Pulls dropped iron and steel items toward itself. |
| [Dynamotor](Dynamotor.md) | Electricity in, shaft out, as a motor. |
| [Heating Coil](Heating_Coil.md) | Electricity in, heat out, under a vessel. |
| [Electrolytic Cell](Electrolytic_Cell.md) | Refines and plates metal. |
| [Charger](Charger.md) | Charges one battery. |
| [Charging Gantry](Charging_Gantry.md) | Charges the battery of a worn exosuit. |
| [Glowstone Lamp](Lamp.md) | Light in four steps, by how well it is fed. |
| [Rectifier](Rectifier.md) | Makes a battery beside it charge about three times faster. |
| **Measuring** | |
| [Electroscope](Instruments.md#electroscope) | The band of a live line. |
| [Galvanometer](Instruments.md#galvanometer) | Current. |
| [Hydrometer](Instruments.md#hydrometer) | A battery's charge. |
| [Current Sensor](Instruments.md#current-sensor) | A galvanometer you bolt onto a cable so a [Controller](Controller.md) can read it. |

## How a line behaves

Everything that touches is connected. Put a source, some cable and a magnet next to each other and they form one line. You don't need to configure anything.

**The highest band wins.** If sources of different bands are on one line, the line runs at the highest band that is supplying; only sources of that band do any supplying, and they share the work by rating. A source of a lower band just sits there: look at it with Jade and it says *"Outbid by a higher band."* That's why several Galvanic Cells on one line add up, but a Galvanic Cell next to a turning Electrostatic Generator goes quiet until the pulse ends.

**Long lines lose energy.** Every block of cable takes a slice off every packet, whatever the current: standard copper cable loses **0.4 Eu** of each packet per block; thicker gauges lose less ([Copper Cable](Copper_Cable.md#how-much-it-carries)). The band arrives unchanged, but the packet is smaller, so a machine at the far end runs slower. Twenty blocks of standard copper take 8 Eu off a 32 Eu LV packet. Shorten the run, use a thicker gauge, or send it in a higher band: a bigger packet loses the same slice and keeps more of itself. Poor conductors, thin gauges and worn cable lose more.

**Overdrawn sources cap out.** Every source has a rated current. A line can draw up to **twice** that rating; past it, every machine gets the same share of what it asked for. No source is ever damaged by this.

**Starved machines share fairly.** If there isn't enough to go round, every machine gets the same share of what it wanted. The one closest to the source doesn't hog it all. Jade shows any machine that's short as *"Starved."*

**Machines don't pass power along.** A machine is the end of the line. To feed two magnets, run cable to each of them. Don't chain them through each other.

**An open [Breaker](Breaker.md) or blown [Fuse](Fuse.md)** splits a line into two.

## Under and over

- **Too little** packet or current makes a machine **slow**, never stopped and never damaged. A magnet that gets half the packet it is rated for (a long cable run) and half the current it wants pulls at a quarter strength.
- **Too high a band** goes into the machine as **heat and wear**, more the further over: each band over its rating is four times the packet. It never explodes and never refuses to run; cheap parts such as a lamp's panel burn out.
- A few jobs have a real minimum: the [Electrolytic Cell](Electrolytic_Cell.md) plates a less noble metal only inside the window its liquid allows, however much current you give it.

## Changing band

There's no transformer: Feedback's electricity is DC, and transformers need AC. A band changes with a **motor-generator set**: a [Dynamotor](Dynamotor.md) run as a motor, turning a shaft that turns another run as a generator with the **other** winding.

- **Many-turn motor → few-turn generator:** a lower band, more amps. Send power a long way in a high band, then step it down next to a machine that wants less.
- **Few-turn motor → many-turn generator:** a higher band, fewer amps.

The only loss is the friction of the shaft between them.

A [Solid-State Converter](Converter.md) steps a line down one band, down two or up one with no shaft, at about 95 percent efficiency. A [Substation](Substation.md) builds on one.

## Reading a line

You can always tell whether a line is working just by looking at it with Jade:

- **Live**: there's charge here.
- **Dead**: nothing's reaching this block.
- **Starved**: this machine is getting less than it wants.
- **Outbid by a higher band**: this source is being overruled by a source of a higher band.

For actual numbers you need an instrument somewhere in your inventory: an [Electroscope](Instruments.md#electroscope) adds the band to the Jade tooltip, and a [Galvanometer](Instruments.md#galvanometer) adds amps. You can also right-click any electrical block with one for a direct reading.

## Things that will hurt you

- **Overloaded cable** gets hot. Push it far enough and the insulation burns off, and then **bare cable carrying current shocks anyone touching it**. Keep pushing and the cable burns out completely. See [Copper Cable](Copper_Cable.md#overloading).
- A **[Leyden Jar](Leyden_Jar.md)** rated MV or more and holding charge shocks you.

Armor doesn't stop a shock; insulating gloves do, up to the band their materials are rated for. See [Hazards](Hazards.md#shock).
