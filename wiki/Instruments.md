# Instruments

Instruments turn the words a machine or item gives you ("glowing orange", "nearly there", "battered") into numbers. Most work from anywhere in your inventory: carry one and tooltips and Jade show exact figures instead of words. Some are used with a right-click on the thing to measure, and a few are bolted onto a block so a [Controller](Controller.md) can read them.

Nothing is locked behind an instrument; they are how you do on purpose what you could do by luck. Every reading has a limit, shown in the last column. Sorted by what they measure: tools and materials, heat and pressure, electricity, other. A [Reach Probe](Reach_Probe.md) is not an instrument: it links a machine so recipe cards show its reach.

| Name | What it detects | Quality |
| --- | --- | --- |
| [Calipers](#calipers) | Work done on a piece, machine wear, gun barrel size | Wear to 5%; barrel to 0.1 mm |
| [Balance Scale](#balance-scale) | Mass of a tool, weapon or armor piece | 0.1 Mu |
| [Scratch File Set](#scratch-file-set) | Hardness of an edge or plate | One of eight rungs, not a number |
| [Spectrometer](#spectrometer) | Makeup of an alloy, metals an ore bears | Shares to the nearest 5% |
| [Impact Pendulum](#impact-pendulum) | Toughness of a made tool's edge, or of a made armor piece | Nearest 5; nearest 1 when the piece is broken |
| [Strike Post](#strike-post) | Work a blow carries and the damage it deals | Nearest 0.5 |
| [Test Strip](#test-strip) | Toxin in a food | Steps of 0.025 Qu/mB |
| [Thermometer](#thermometer) | Temperature of items and blocks | 25 Tu steps |
| [Pyrometer](Optics.md#pyrometer) | Temperature of a vessel up to 24 blocks away | 10 Tu steps |
| [Temperature Sensor](#temperature-sensor) | Temperature of a crucible or Improved Furnace, to a program | 10 Tu steps |
| [Pressure Gauge](#pressure-gauge) | Pressure in a boiler, vessel, pipe or tank | 0.5 Pu steps |
| [Electroscope](#electroscope) | Band of a live line | Whole bands |
| [Galvanometer](#galvanometer) | Current | 0.25 A steps |
| [Light Meter](Light_Meter.md) | Block and sky light at a face | 2 levels |
| [Hydrometer](#hydrometer) | Charge of a battery | 5% steps |
| [Current Sensor](#current-sensor) | Current in a cable, to a program | 0.5 A steps |
| [Oscilloscope](#oscilloscope) | Current in a cable over time, on a screen | 0.25 A steps, 10 samples a second, pegs above 4 A |
| [Phosphor Screen](Phosphor_Screen.md) | Shows the Oscilloscope's signal | Four brightness levels |
| [Component Tester](#component-tester) | Response and condition of a placed electronic part | 5% steps |
| [Dosimeter](#dosimeter) | Radiation dose your body has taken | Nearest 10 Ru s |
| [Flux Meter](#flux-meter) | Neutron flux of a reactor core | 5% steps |
| [Geiger Counter](#geiger-counter) | Radiation at your position | 0.1 Ru from a five-second click count, jittery |
| [XP Meter](#xp-meter) | Experience levels and points | Whole levels, points to the nearest 5 |
| [Survey Meter](#survey-meter) | Ore and oil within 16 blocks | Counts rounded to 8, wobbling |
| [Mass Probe](#mass-probe) | Metal the Electrolytic Cell plates onto it | Depends on the cut: 0.009 to 0.09 Qu |
| [Flight Sensor](#flight-sensor) | Altitude, airspeed, pitch, roll, vertical speed and air density of a vehicle | Nearest block, 0.5 block/s, 2 degrees; forces 0.5 Fu, mass 10 Mu |

The [Phosphor Screen](Phosphor_Screen.md) is a block with its own sealing and burn-in rules and has its own page. The [Instrument Bag](#instrument-bag) holds instruments in one slot.

## The reading card

An instrument you use with a right-click (Thermometer, Pyrometer, Calipers, Galvanometer, Electroscope, Pressure Gauge, Flux Meter, Hydrometer, XP Meter) takes one reading and shows it on a **card to the right of the crosshair**: the instrument's name, the figure, and a half-round dial with a needle. The dial picks its own scale (1, 2 or 5 times a power of ten) to fit the figure; a fraction reads 0 to 100%. A needle past full scale turns the warning color.

The card stays while you keep aiming at the block or creature you read, and fades when you look away. A reading is taken once: click again for a fresh one.

Many blocks answer a right-click with a report of their own (a pressure vessel, drying rack, stockpot, tube junction, tray, reboiler, dryer, gas tank, counter, sensor and others). Those reports show on the same card, titled with the block's name, and are held while you look at the block.

A setting you change by clicking (a converter ratio, a differential mode, a drum size, a timer wind) shows as a **strip under the crosshair** that lists every option with the current one lit, plus a line saying what it does.

A tool that links two things (the Data Connector, the Belt, the Joint Link) shows a chip left of the crosshair, *"Linking from"* and the first block's name, from the first click until the link is made, fails or is cancelled.

A refusal or result inside an open screen (the Mixer, a part bench filing, the enchanting table, the Blueprint Table, the Programmer, the Reloading Press) shows on a strip just under the screen's panel. On the Computer it appears as a desktop toast.

The card takes the look of the tool in your hand (brass and wood, forge iron, lab enamel, console, mystic). The **aimHudSize** option in `feedback-client.toml` sets the size of the crosshair elements: small, medium or large.

## Calipers

Turns "kind of flattened" into an exact number, and tells you exactly how worn a machine is.

```
P   P
  R
```

P is a Steel Plate and R is a Steel Rod. Craft it at the [Engineering Table](Engineering_Table.md) with a [file](Hand_Tools.md#file) in the tool row, holding Materials Treatise I: Machine-Shop Consumables ([Treatise](Treatise.md)); it spends no fasteners.

**Work in progress.** Without calipers, a half-worked item tells you what it **needs** as a number (*"Needs 14 Wu"*) but only roughly how far it has got: *Barely marked*, *Taking shape*, *Visibly worked*, *Nearly there*. Carry calipers anywhere in your inventory and that changes to an exact figure, like *"Calipers: 9 / 14 Wu"*, on the item's tooltip and in Jade, on whatever is sitting in a hammer, press, mill, drawer, drill, lathe or grinder. You do not need calipers to make a plate; the words are enough to avoid ruining one. You need them to **aim** at something that only lasts a blow or two, like copper foil, and hit it every time.

**Part dimensions.** A part shaped by hand shows what its maker aimed each measurement at (*"Length cut to 15 cm"*); calipers show where the cut actually landed. See the [Smithing Table](Smithing_Table.md#what-feedback-does).

**Machine wear.** **Right-click a machine** with the calipers to measure its condition, e.g. *"Calipers: 85% condition"*, in steps of 5%. The machine has to be stopped; on a running machine you get *"Stop the machine before measuring it."* Without calipers, Jade still warns when a machine is worn (*Head is marked / battered / spent*), just not by how much. See [Metalworking](Metalworking.md) for what work, hardness and wear mean.

**Gun barrel.** With calipers in your inventory, a [Gun Barrel](Gun_Parts.md#the-barrel)'s tooltip shows its **bore**, **wall** and **length**, to 0.1 mm. Without them it says *"A bored barrel. Calipers would say how wide."*

## Balance Scale

Tells you exactly how heavy a [tool, weapon or piece of armor](Tools_and_Weapons.md) is. The tooltip already says *head-heavy* or *heavy* for free; the scale says how many.

```
  R
I   I
I I I
```

R is a Stick and I is a Wrought Iron Ingot.

Carry it anywhere in your inventory. Every modular item's tooltip gains a line like *"Balance scale: 3.4 Mu"*. Mass matters because a heavier head hits harder and swings slower, and because a head that is too heavy for its handle works the joint loose.

### Stamping a reading

The balance scale, the calipers and the [draw gauge](#draw-gauge) also stamp a reading on a made item: hold the instrument in one hand and the item in the other and use it. The balance scale stamps the item's mass and balance, the calipers each part's dimensions and the fit, the draw gauge a bow's draw and launch figures. A stamp is a past reading: it stays when the item wears. A stamped figure reads exact on the tooltip; a figure only a carried instrument reads (the mass, with the balance scale in your pocket) shows as a range as wide as the fit left it uncertain, narrower for an item built to a blueprint.

## Draw Gauge

A spring balance with a hook, made from a string, a stick and wrought iron plates. Held with a bow, a crossbow, a spring or a launcher in the other hand it stamps the weapon's draw time, launch speed and stored energy.

## Scratch File Set

Tells you how hard an edge or a plate is, by which file bites it.

```
I I I
S S S
G G L
```

I is an ingot of a soft metal (hardness 12 or less), S is an ingot of a harder metal (hardness 14 to 60), G is a gem of hardness 100 or more and L is Leather. Craft it at the [Engineering Table](Engineering_Table.md) with a [file](Hand_Tools.md#file) in the tool row, holding Materials Treatise I: Machine-Shop Consumables ([Treatise](Treatise.md)); it spends no fasteners.

Carry it anywhere in your inventory. A modular item's tooltip gains a line like *"Scratch files: scratched by file 5, not by file 4"*. The set has eight files, each harder than the last. A file scratches anything softer than itself, so the reading is a **rung**, not a number: your edge is harder than file 4 and softer than file 5.

Hardness decides what a tool can dig (see [Tools and Weapons](Tools_and_Weapons.md#what-the-parts-do)) and how hard a plate is to pierce. [Materials](Materials.md) lists the numbers if you would rather look them up than measure.

## Spectrometer

Tells you what an alloy is made of and which metals an ore bears.

```
L G
X P
```

L is an Optical Glass Lens, G is a Quartz Glass Ingot (the prism), X is a [Photocell](Sensors.md) and P is a Wrought Iron Plate (the housing). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 1 screw ([Fasteners](Fasteners.md)) and Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)).

Carry it anywhere in your inventory. The tooltip of an alloy gains a line listing its constituents and their shares, like *"Spectrometer: Makeup: Copper 90%, Tin 10%"*. The tooltip of an ore lists the metals it bears and how much of each. Shares are told to the nearest 5%. With the Thaumaturge mod, a further line lists the aspects of the sample you have already discovered; see [Mod Compats](Mod_Compats.md#spectrometer-and-aspects).

## Impact Pendulum

A block that swings into an edge and reports its toughness. The tooltip gives toughness in words; the pendulum gives a figure.

```
P B P
  R
  I
```

P is a Wrought Iron Plate, B is a [Bearing](Bearing.md), R is a Wrought Iron Rod and I is an Iron Ingot.

**Right-click** it holding a tool or armor piece made at a bench. The card shows the edge's toughness, to the nearest 5. A twin-headed tool reports both heads. An armor piece reports its armor toughness. Anything else says *"Nothing here to test."* A plain right-click does not mark or wear the piece.

**Sneak and right-click** with the specimen and the bob breaks it. The piece is spent, with the item-break sound, and the card gives the toughness at which it broke, to the nearest 1. The break test is five times finer than the plain reading and costs the piece.

## Strike Post

A block that takes a blow and reports what it carried.

```
P
L
L
```

P is a Wrought Iron Plate and L is any log.

**Swing at it** (left-click, or a [Better Combat](Combat.md#better-combat-required) swing) with a weapon or tool made at a bench. The card shows the Work the blow carries and the damage it deals, both to the nearest half. A tool that is not made at a bench says *"Nothing here to test."* The post does not mark or wear the tool.

## Test Strip

Turns color with the toxin in a food, in four shades.

Shapeless: Paper and a Beetroot.

Carry it anywhere in your inventory. A food or dish's tooltip, and the held-item card (key H), gain a line like *"Test strip: Toxin: 0.050 Qu/mB"*. It reads in quarter steps of a full dose, so it tells a trace from a lot and no more.

It reads toxin only. Heat does not clear a toxin, and your senses do not show one until it is deep in the food, so this is how you check a dish before you eat it. A live organism (cooking cures it) the strip does not read; the bite tells you. You can also [bite](Food.md#reading-what-you-made) the food and find out the hard way.

## Thermometer

Turns "glowing orange" into a number.

With Legendary Survival Overhaul installed, carrying it also shows your body temperature ([Mod Compats](Mod_Compats.md#legendary-survival-overhaul)).

```
G
F
P
```

G is a Glass Pane, F is [Copper Foil](Mechanical_Hammer.md) and P is a [Brass Plate](Mechanical_Hammer.md).

Carry it anywhere in your inventory; you do not need to hold it.

- Hot **items** show a temperature in Tu on their tooltip instead of a color word.
- **Crucibles**, cable, the Improved Furnace and other open blocks show a figure in Jade.
- **Right-click** any hot block, even a **sealed** furnace, to take one reading through the door. Sealed furnaces cannot be *watched* (Jade says *"Sealed - cannot be measured"*), but you can always check them by hand.

It reads in steps of **25 Tu**. Steel's window is 60 Tu wide, so that is about two steps: you know you are in it but not exactly where. It is enough to make steel reliably by hand: heat, check, wait, check, adjust. For finer readings, and readings a Controller can use, bolt on a [Temperature Sensor](#temperature-sensor); for distance, use the [Pyrometer](Optics.md#pyrometer). A circuit can narrow the step ([below](#fitting-a-circuit)).

## Temperature Sensor

A thermometer you bolt onto a vessel so it reads all the time, and so a [Controller](Controller.md) can use the reading.

```
P T P
W C W
```

P is a Steel Plate, T is a [Thermometer](#thermometer), W is [Copper Wire](Wire_Drawer.md) and C is a [circuit](Basic_Electronic_Circuit.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

Right-click a face of a [Crucible](Crucible.md) or an **Improved Furnace** with it. It attaches to that side; one sensor per face. It does not attach to the sealed [furnaces](Furnaces.md) (Furnace, Smoker, Crude Blast Furnace, Kiln, Annealing Furnace): they have no opening for it. Being unwatchable is the price of a sealed furnace.

Link it to the Controller with a [Data Connector](Data_Connector.md) and read it with an *Input* card of signal *Temperature* on the [Programmer](Programming.md), then compare the reading with *Greater Than* / *Less Than*. It reads in steps of **10 Tu**, fine enough to hold steel's 60 Tu window.

- **Bellows thermostat**: *Input → Less Than (window bottom) → Switch* on the bellows' shaft.
- **Two-sided control**: one comparison runs the bellows, another opens a [Damper](Damper.md) when it is too hot.

## Pressure Gauge

Turns "the boiler is groaning" into a number.

```
G
T
P
```

G is a Glass Pane, T is a Copper Tubing and P is a [Brass Plate](Mechanical_Hammer.md).

**Right-click** a Boiler, a Pressure Vessel, a [Pipe](Pipe.md) or a [Gas Tank](Gas_Tank.md) to read its pressure in Pu on the card. It only reads; it cannot vent or relieve anything. It reads in steps of **0.5 Pu**. A boiler's danger line is 5 Pu, its critical pressure 7, and it starts to seep at 3.5, so you can see the danger line coming without the exact figure. A circuit can narrow the step ([below](#fitting-a-circuit)).

## Electroscope

Two thin leaves of copper foil in a glass jar that spread apart when they touch something charged. The cheapest [electrical](Electricity.md) instrument, and a deliberately rough one.

A [Copper Foil](Mechanical_Hammer.md) above a Glass block.

```
F
G
```

- **Right-click** any electrical block (cable, cell, generator, magnet) to get a band reading on the card, like *LV*. A dead line reads *No charge reaches it*.
- **Carry it anywhere in your inventory** and Jade's tooltip on electrical blocks shows the band as well as *Live/Dead*.

It shows the band only, in whole bands, so the smaller packet at the far end of a long cable reads the same band as the source. It tells you a line is live and how big its packets are; for current, use a [Galvanometer](#galvanometer).

- Touch it to an [Electrostatic Generator](Electrostatic_Generator.md) while someone turns the handle, and the leaves jump and fall again.
- Read two lines side by side to see which band each runs in, for example before wiring a machine to one of them.

## Galvanometer

A needle that swings when current runs through it. It tells you how much current is flowing through an [electrical](Electricity.md) block.

```
W W W
N G N
```

W is [Copper Wire](Wire_Drawer.md), N is a Wrought Iron Nugget and G is a Glass Pane.

- **Right-click** any electrical block to read the current on the card, e.g. *1.00 A*, in steps of **0.25 A**.
- On a **machine**, it shows what the machine is getting next to what it wants, e.g. *"0.50 of 1.00 A"*: the quickest way to tell a starved machine from one that is simply weak.
- **Carry it anywhere in your inventory** and Jade's tooltip on electrical blocks gains an amps line.

Reading a live cable is free and safe while it is insulated, and nothing has to stop while you measure. Right-clicking a **bare** live cable or a live [fence](Electrified_Fence.md) is a hand on the wire: you still get the reading, and it shocks you as touching it would. A glove rated for the band stops it. Idle wires shock nothing.

- Find which cable on a busy line carries the most (and is about to overheat).
- Check whether adding another [Galvanic Cell](Galvanic_Cell.md) helped.
- Work out why a magnet is weak: low amps means not enough supply, while full amps on a weak magnet means the packets are too small (a long run, or a line in a lower band than the magnet is rated for). Check the band with an [Electroscope](#electroscope).

A trickle under **0.2 A** is **too faint to read** (the card says it needs a circuit with gain). A circuit with gain, fitted as [below](#fitting-a-circuit), brings the faintest readable current down; a tube that has not warmed gives none.

A circuit can narrow the step ([below](#fitting-a-circuit)).

## Hydrometer

Samples the acid of a [Battery](Battery.md), whose density falls as the cells discharge, and so reads the charge.

```
  W
P G P
```

W is [Copper Wire](Wire_Drawer.md), P is a Glass Pane and G is a Glass Bottle.

- **Right-click** a battery to read its charge on the card, in steps of **5%**.
- **Carry it in your inventory** (or an [Instrument Bag](#instrument-bag)) and a battery's Jade tooltip gains a charge figure.

## Current Sensor

A small galvanometer that you bolt onto one face of a [Copper Cable](Copper_Cable.md). It reads the current through that cable all the time, so a [Controller](Controller.md) program can act on it.

```
P G P
W C W
```

P is a Steel Plate, G is a [Galvanometer](#galvanometer), W is [Copper Wire](Wire_Drawer.md) and C is a [circuit](Basic_Electronic_Circuit.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

Right-click the face of a cable you want it on. You can put one on each of the six faces. Breaking the cable drops the sensor. It fits on cable only, not directly on sources or machines.

To use it: link the sensor to the Controller with a [Data Connector](Data_Connector.md), read it on the [Programmer](Programming.md) with an *Input* card of signal *Current*, then compare it with *Greater Than* / *Less Than*. It reads in steps of **0.5 A**, coarser than the hand-held Galvanometer.

- Watch a cable close to its 4 A limit, and open a [Breaker](Breaker.md) (or flip a [Clutch](Clutch.md) on whatever drives the load) when the current climbs too high.
- Detect when a machine at the end of a line starts or stops drawing.

## Oscilloscope

Watches the current in a cable and shows it on a [Phosphor Screen](Phosphor_Screen.md).

```
P S P
W C W
```

P is a Steel Plate, S is a [Phosphor Screen](Phosphor_Screen.md), W is [Copper Wire](Wire_Drawer.md) and C is a [circuit](Basic_Electronic_Circuit.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

Put it **against a cable** and **against a screen**. It joins no line; it only reads. Every tenth of a second it samples the cable and drives the screen to match (four levels). **Right-click** it to see the last 16 samples as a row of bars, to the nearest 0.25 A.

Above 4 A the trace sits pegged at the top, exact and wrong. A pegged screen is also at full brightness, which is what [burns it in](Phosphor_Screen.md#overrun). It changes nothing in the line.

## Component Tester

Says how a part responds. It is never needed; it saves guesswork.

```
W   W
C G C
```

W is Copper Wire, C is Ceramic and G is a [Galvanometer](#galvanometer). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 1 screw ([Fasteners](Fasteners.md)) and Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)).

**Right-click a placed part** to read it, to the nearest 5%:

- [Cat's Whisker Detector](Cats_Whisker_Detector.md): the response of the spot the whisker rests on (or "burnt spot" / "no crystal").
- [Redstone Coherer](Redstone_Coherer.md): welded or open, and its condition.
- [Relay](Relay.md): closed or open, and its condition.

**Right-click in the air with a [Trace Board](Trace_Board.md) in your other hand** to read its traces, substrate and joints.

**Right-click in the air with a [circuit](Basic_Electronic_Circuit.md) in your other hand** to read its figures: speed, noise, joints, and where it has them **gain** (a tube), **stability**, **channels** and **bandwidth** (a [Resonator](Resonator.md)), and **memory**, **adaptation** and whether it holds (a [Lattice](Lattice.md)).

## Dosimeter

Reads the dose your body has taken: the running total that makes you ill in steps, after your armor has had its say. It is not the rate; the [Geiger Counter](#geiger-counter) reads that. Carry one and instruments show your dose as a figure in Ru s, to the nearest 10; hold it and the figure shows on your hand card. See [Radiation](Radiation.md).

```
  T
  F
  I
```

T is Glass Tubing, F is Gold Foil and I is a Wrought Iron Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 1 screw ([Fasteners](Fasteners.md)) and Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)).

## Flux Meter

Reads the neutron flux of a [Fission Reactor](Fission_Reactor.md) core as a fraction of full power, to the nearest 5%. Right-click the core with it.

```
  T
W P W
  S
```

T is Glass Tubing, W is [Copper Wire](Wire_Drawer.md), P is a Lead Plate and S is a Stainless Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Nuclear Treatise I: Reactors and Fuel ([Treatise](Treatise.md)).

## Geiger Counter

Listens for [radiation](Radiation.md). Hold it and it clicks: a few clicks a second with nothing near, more the stronger the field. It reads the field at you **before** your armor, so it tells you why to put lead on, not how much got through.

```
  T
I W I
  I
```

T is Glass Tubing, I is a Wrought Iron Plate and W is [Copper Wire](Wire_Drawer.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)).

A field under **0.3 Ru** is **too faint to read**: the counter gives no figure and says so.

Carry one and instruments read radiation as a figure in Ru. Hold it and the same figure shows on your hand card. The figure comes from counting the clicks of the last five seconds, so it jitters the way a real counter does, and a weak source can read as nothing for a moment. Listen as much as you look.

## XP Meter

Reads experience as numbers: the level something stands at, and the points it holds. See [Experience](Experience.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) and 2 screws, holding Experience Treatise I: Orbs and Vessels ([Treatise](Treatise.md)).

Right-click:

- a Crystal Holder, to read the container in it;
- an XP wire or a Creative XP Source, to read the whole run: its level, total points, points moving per tick and points leaking;
- the air, to read the container in your other hand, or yourself if it is empty;
- a player, for theirs;
- a villager, for its trade level and trade XP.

Carried in your inventory, it also turns the words on container tooltips and Jade into figures. It reads levels to whole levels and points to the nearest five. Without a meter, containers say only how high their level stands and how full they are.

## Survey Meter

Tells you what ore, and what oil, lies around you, so you do not dig blind. Read-only and needs no power.

```
  N
I W I
  R
```

N is a Compass, I is a Wrought Iron Plate, W is [Copper Wire](Wire_Drawer.md) and R is a Wrought Iron Rod. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)).

Right-click. It lists up to four kinds of ore within 16 blocks, each with a rough block count and which way most of it lies. It then needs a couple of seconds before the next reading. The count is rounded to the nearest 8 and wobbles a little each time, so a thin vein may read as nothing and a fat one does not read the same twice.

An **oil basin** reads as a line of its own, from how close the nearest basin's circle comes, with a bearing towards its center. Same rounding, same wobble. A **soul oil basin** in the Nether reads the same way. See [Oil](Oil.md#finding-a-basin) and [Soul oil basins](Oil.md#soul-oil-basins).

With [Thaumaturge](Mod_Compats.md#thaumaturge) installed, the Survey Meter adds a line, **Field anomaly, no ore**, with a bearing towards a chunk within 16 blocks whose vis stands well above its base. It shows only some of the time at the margin.

With [Xaero's Minimap](Mod_Compats.md#xaeros-minimap) installed, each reading also pins itself to the map.

Every vein can show itself on the surface above it, if it is not buried too deep: up to three small signs scattered over the vein, each on dirt, sand or bare stone. Follow one down. See [Ore Deposits](Ore_Deposits.md#reading-the-surface) for which sign marks which ore.

## Mass Probe

A slice of quartz or amethyst on a metal backing. Hang it in an [Electrolytic Cell](Electrolytic_Cell.md) and it tells you how much metal has plated onto it. A crystal rings at one pitch, and a coat of metal on it drags the pitch down; the drop is the mass.

**Making one.** Grind a quartz or amethyst into a plate on the Grinding Wheel; it comes out a **coarse cut**. Put the plate back on the wheel and it goes **standard cut**, then **fine cut**: stop when it is as thin as you want. Hover a plate to see its cut. Craft the plate with any metal plate (shapeless) and the probe has the same cut. Only crystals that ring work (quartz and amethyst; a diamond does not). Quartz and amethyst (hardness 108) are within reach of either [Grinding Wheel](Grinding_Wheel.md).

| Cut | Reads up to | Tells apart |
| --- | --- | --- |
| Coarse | 1.71 Qu | 0.09 Qu |
| Standard | 1.08 Qu | 0.045 Qu |
| Fine | 0.54 Qu | 0.009 Qu |

A thin cut sees fine detail and not far. A thick cut sees far and coarsely. Pick by how much coat you mean to lay down. A good coat on a piece is 0.45 to 1.8 Qu.

**Using it.** Right-click a side of the cell with the probe, as for any piece to plate. While it hangs, the metal the cell plates goes onto it, and a Jade tooltip on the cell shows the reading. Take it out with an empty hand and the probe's tooltip shows the coat it carries. Carrying a probe also counts as carrying that instrument. It does not change what the cell does; it only tells you.

- **Past its range the reading stops at the top and stays there**, while the coat keeps growing underneath. Leave it long enough and the coat spalls off, as on any piece (you get the crystal back bare and the metal as nuggets). Trust a stuck reading and you lose the coat.
- Every reading wobbles a little. Even the fine cut is never perfectly steady.
- Run the cell above its rating and the crystal cracks: condition falls and the reading gets noisier. It never refuses to work.
- A coat cannot be cleaned off. Hang it again and the coat grows on from where it was.

## Flight Sensor

A block that reads how its vehicle is flying and hands the figure to a [Controller](Controller.md), so an [autopilot](Vehicles.md#autopilot) is only cards.

Place it on the vehicle. Its front is the way it faces; pitch and roll are measured about that. **Sneak-right-click** steps what it reads; a strip under the crosshair lists the readings with the current one lit:

| Reading | What it is |
| --- | --- |
| **Altitude** | Height above the bottom of the world, in blocks, to the nearest block. |
| **Airspeed** | How fast the vehicle is moving, in blocks per second, to the nearest half. There is no wind, so this is also its speed over the ground. |
| **Pitch** | Nose up (positive) or down, in degrees, to the nearest two. |
| **Roll** | Tilt to either side of the front, in degrees, to the nearest two. |
| **Vertical speed** | How fast the vehicle climbs, in blocks per second (negative when it sinks). |
| **Air density** | The [air](Vehicles.md#air-density) around it as a share of surface density: less the higher it climbs. |
| **Heading** | The way the front points, in degrees round from south. |
| **Position east** | Blocks east of the world's origin, in steps of 4 blocks. |
| **Position south** | Blocks south of the world's origin, in steps of 4 blocks. |

**Right-click** opens the [Contraption Diagram](#contraption-diagram).

To use it, link it to the Controller with a Data Connector and read it with an *Input* card on the [Programmer](Programming.md) whose signal is what the sensor reads, such as *Altitude* or *Airspeed*. The card carries that kind of reading, so the program cannot compare altitude with speed. Lifting the vehicle with the Lift keeps its wiring and link colors.

### Contraption Diagram

A blueprint-style screen showing the vehicle the sensor is built into, drawn as its real blocks in ink line work. Arrow buttons turn the view to another angle. Off a vehicle the screen says the sensor must be on a vehicle.

An arrow marks every force on the vehicle, starting where it acts and colored by cause: **gravity**, **drag**, **levitation**, **buoyancy**, **ballast**, **propulsion** and **wing lift**. Each force is the mean of what the physics applied over the last second. Hover an arrow for its figure in [Fu](Units.md#force-fu); the total mass shows in [Mu](Units.md#mass-mu). Figures are as fine as the sensor reads: forces to 0.5 Fu, mass to 10 Mu.

- The legend toggles each cause on and off.
- **Merge arrows** joins the forces of one cause into a single arrow; unmerged, each force has its own.
- **Center of mass** marks the vehicle's center of mass.
- **Whole assembly** draws every body joined to this one by hinges and joints, and adds their forces, instead of the body alone.

The [Computer](Electric_Controller.md#built-in-apps)'s **Diagram** app shows the same page for a Flight Sensor linked to the Computer.

## Fitting a circuit

The Thermometer, Pressure Gauge, Electroscope, Galvanometer, [Light Meter](Light_Meter.md), Dosimeter, Geiger Counter, Survey Meter, Flux Meter, Pyrometer and Component Tester can carry a circuit. Put the instrument and a live [Basic Electronic Circuit](Basic_Electronic_Circuit.md) in a crafting grid, in any arrangement: the instrument comes out carrying the circuit and reads in finer steps than the usual figure in the table. A quiet circuit takes the step down to about half; a noisy one, such as a cold joint, helps little; a dead one cannot be fitted. A fitted instrument still never reads the exact figure. Its tooltip says how fine it reads, and its signature on readings says it is circuit-fitted.

## Instrument Bag

A bag for your instruments. Instruments read from your inventory, so a pack with eight of them spends eight slots on things nobody holds. The bag keeps up to **6 instruments** in one slot, and they **still read**: Jade and tooltips look inside it as they look along your hotbar. It is not an instrument itself and has no figures of its own.

```
S L S
L _ L
L L L
```

S is String and L is Leather.

- Click an instrument onto the bag (or the bag onto an instrument) with a **right-click** to stow it.
- Right-click the bag on an empty slot, or on its own slot with an empty cursor, to take the last one stowed back out.

Only instruments go in (those above and the like). The limit is a count, not a weight.
