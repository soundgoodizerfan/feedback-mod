# Electricity

Electricity in Feedback is a workshop power, not a grid. You can make it from chemistry, static and shafts; carry it on cable; switch and protect it; and spend it on magnets, heat, refining, motors and coilgun rounds.

It is **not** the "universal" power you might be used to from other tech mods. Your mechanical and thermal setups never need to be converted into electricity. Electricity is one option among several, good at some jobs and bad at others.

## Volts and amps

Electricity has two numbers, and they're not interchangeable:

- **Voltage (V)** is how hard it pushes. Some jobs need a minimum voltage before anything happens at all.
- **Current (A)** is how much flows. For most jobs, current is how *fast* things happen.

Voltage comes in four named bands:

| Band | Up to |
| --- | --- |
| **LV** | 1.5 V |
| **MV** | 6 V |
| **HV** | 24 V |
| **UHV** | 96 V and beyond |

Bands don't lock anything. A machine is *rated* for a band; feed it more and it wears and warms, feed it less and it runs slow.

## The pieces

| Block / item | What it does |
| --- | --- |
| **Sources** | |
| [Galvanic Cell](Galvanic_Cell.md) | A small, steady supply forever: 1.1 V, 1 A. |
| [Electrostatic Generator](Electrostatic_Generator.md) | A one-second burst each time you turn it: 1.5 V, 1 A. |
| [Dynamo](Dynamo.md) | Shaft in, electricity out. Volts set by shaft speed. |
| [Leyden Jar](Leyden_Jar.md) | Stores a charge and gives it back as a burst. |
| **Carrying and stopping** | |
| [Copper Cable](Copper_Cable.md) | Carries up to 4 A. |
| [Breaker](Breaker.md) | A switch. Trips itself above 4 A. |
| [Fuse Holder](Fuse.md) | Blows when the current is too high. |
| **Machines** | |
| [Electromagnet](Electromagnet.md) | Pulls dropped iron and steel items toward itself. |
| [Motor](Motor.md) | Electricity in, shaft out. |
| [Heating Coil](Heating_Coil.md) | Electricity in, heat out, under a vessel. |
| [Electrolytic Cell](Electrolytic_Cell.md) | Refines and plates metal. |
| [Round Charger](Round_Charger.md) | Charges jar rounds for a [coilgun](Coilgun.md). |
| **Measuring** | |
| [Electroscope](Electroscope.md) | Voltage, roughly. |
| [Galvanometer](Galvanometer.md) | Current. |
| [Current Sensor](Current_Sensor.md) | A galvanometer you bolt onto a cable so a [Controller](Controller.md) can read it. |

## How a line behaves

Everything that touches is connected. Put a source, some cable and a magnet next to each other and they form one line. You don't need to configure anything.

**The highest voltage wins.** If two different sources are on one line, the stronger one does all the supplying and the weaker one just sits there. Look at the weaker one with Jade and it says *"Outbid by a higher voltage."* Sources at the same voltage share the work instead. That's why several Galvanic Cells on one line add up, but a Galvanic Cell next to a turning Electrostatic Generator goes quiet until the pulse ends.

**Long lines lose voltage.** Every block of cable takes some voltage off, in proportion to the current through it: copper loses **0.05 V per amp per block**. At 1 A, twenty blocks of copper cost 1 V. That's most of a cell. Shorten the run, or send it at a higher voltage (a higher voltage carries the same power on less current, so it loses less).

**Overloaded sources sag.** Every source has a rated current. Draw more and its voltage drops, in a straight line down to a quarter at **twice** its rating. It won't give more than twice its rating at all. No source is ever damaged by this.

**Starved machines share fairly.** If there isn't enough to go round, every machine gets the same share of what it wanted. The one closest to the source doesn't hog it all. Jade shows any machine that's short as *"Starved."*

**Machines don't pass power along.** A machine is the end of the line. To feed two magnets, run cable to each of them. Don't chain them through each other.

**An open [Breaker](Breaker.md) or blown [Fuse](Fuse.md)** splits a line into two.

## Under and over

- **Too little** voltage or current makes a machine **slow**, never stopped and never damaged. A magnet on half the voltage and half the current it wants pulls at a quarter strength.
- **Too much** voltage goes into the machine as **heat and wear**, more the further over. It never explodes and never refuses to run.
- A few jobs have a real minimum: the [Electrolytic Cell](Electrolytic_Cell.md) can't plate a metal below its voltage, however much current you give it.

## Changing voltage

There's no transformer: Feedback's electricity is DC, and transformers need AC. Instead, the way to change voltage is a **motor-generator set**: a [Motor](Motor.md) turning a shaft that turns a [Dynamo](Dynamo.md) on the **other** winding.

- **Fine motor → coarse dynamo:** fewer volts, more amps. Send power a long way at MV, then step it down next to an LV machine.
- **Coarse motor → fine dynamo:** more volts, fewer amps.

The only loss is the friction of the shaft between them.

## Reading a line

You can always tell whether a line is working just by looking at it with Jade:

- **Live**: there's voltage here.
- **Dead**: nothing's reaching this block.
- **Starved**: this machine is getting less than it wants.
- **Outbid by a higher voltage**: this source is being overruled by a stronger one.

For actual numbers you need an instrument somewhere in your inventory: an [Electroscope](Electroscope.md) adds volts to the Jade tooltip, and a [Galvanometer](Galvanometer.md) adds amps. You can also right-click any electrical block with one for a direct reading.

## Things that will hurt you

- **Overloaded cable** gets hot. Push it far enough and the insulation burns off, and then **bare cable carrying current shocks anyone touching it**. Keep pushing and the cable burns out completely. See [Copper Cable](Copper_Cable.md#overloading).
- A **[Leyden Jar](Leyden_Jar.md)** charged to 6 V or more shocks you.

Armour doesn't stop a shock; insulating gloves do, up to 24 V. See [Hazards](Hazards.md#shock).
