# Fractionating Column

Splits [crude oil](Oil.md#crude-oil) into naphtha, kerosene and diesel by boiling it at the bottom and letting the vapor climb through trays that are colder the higher they sit. A reboiler at the bottom, one or more trays above it, and optionally a [Condenser](Condenser.md) on top: stack them upward and they join into one column on their own. There is no controller block.

## Building it

Stack the blocks straight up, in this order:

1. one **Reboiler**
2. one or more **[Column Trays](#trays)** (any mix of the three deck kinds)
3. optionally one **[Condenser](Condenser.md)** on top

A column with a single tray works, but smears its cuts. A column with no condenser still works, but runs warm at the top.

## Reboiler

The base of the column. Like a [Boiler](Boiler.md) it takes heat from **below**: a [Firebox](Firebox.md), lava, or a [Heating Coil](Heating_Coil.md). It holds **4,000 mB** of crude.

- **Feed it** with a bucket (right-click), a [Pipe](Pipe.md), or a [Pump](Pump.md). It also reaches for crude from any neighbor at its sides and underneath, the way a [Steam Engine](Steam_Engine.md) reaches for steam. It only accepts a crude: water or anything else that is not boiled in cuts is refused.
- **Steam cleans the trays.** Steam pumped into the reboiler is not stored. While the reboiler is under **420 Tu** it lifts coke off every tray.
- The heavy bottoms stay in the reboiler while it still has lights to boil; only the light cuts leave. Once the lights are boiled off, the spent bottoms drain out of the reboiler through a pipe.
- Each cut starts boiling once the reboiler passes its boiling point: **naphtha 100 Tu, kerosene 180 Tu, diesel 300 Tu**. A hotter reboiler makes every tray hotter, so every cut moves heavier.

The feed's grade matters. A crude with more light fraction gives more of every cut. As the lights are boiled off, the feed left in the reboiler holds less of them. See [grades](Oil.md#grades).

### Reflux

The reboiler is also the return valve. **Right-click it with an empty hand** to cycle reflux: **none**, **partial**, **total**. Sneak-click to read the setting without changing it.

| Reflux | Boil-up | Cuts |
| --- | --- | --- |
| None | full | loosest |
| Partial | 70% | sharper |
| Total | 40% | sharpest |

More reflux trades throughput for cleaner cuts.

## Trays

Each tray is a cut point. The vapor rising from the reboiler meets the tray at a temperature: the column is a straight line from the reboiler's temperature at the bottom to the condenser's body temperature at the top (with no condenser, the top sits halfway between the reboiler and the air). A tray **colder than a cut's boiling point** holds back part of that cut as liquid and lets the rest rise to the next tray.

The liquid a tray catches collects in its **draw tank (1,000 mB)** and is pushed out sideways, up to 40 mB a tick, into any fluid block beside it: a [Pipe](Pipe.md), a [Pump](Pump.md), a tank. Nothing can be put into a tray from outside. **Right-click** a tray with an empty hand to read its condition and what it is drawing.

### Building a tray

A tray is one modular block, the **Column Tray**. Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Column Tray* in the type row). It has one part, the **Tray Deck**, shaped from any sheet of a metal; the tray is built from it at the Stonecutter. The tray takes the color of the deck's metal.

The deck comes in three variants:

| Deck | Holds back | Flow | Fouls |
| --- | --- | --- | --- |
| Bubble-Cap | the most | least | normally |
| Sieve | middling | middling | fastest |
| Packed | the least | most | slowest |

Two measures tune it. **Hole** (5 to 30 mm) is how fine the drilling is: a finer hole holds back more of the vapor and cokes faster. **Pitch** (20 to 80 mm) is the spacing of the holes. The **open area** (the hole against the pitch) sets how much the tray takes a tick: a wider hole or a tighter pitch flows more. The tray's tooltip and the [Blueprint Table](Blueprint_Table.md) figures show what it holds back and takes.

The sharper the tray the less it flows. Vapor a tray cannot take rides up to the next one.

### Coke

A tray hotter than **420 Tu** cokes up, faster for a heavier feed and faster the hotter it runs. Coke lowers both what the tray holds back and what it can take, down to nothing. Running correctly never cokes a tray: only running it too hot does. Clear it with steam through the reboiler (once the reboiler is cooler than 420 Tu).

## Echo crude

The reboiler takes [echo crude](Oil.md#echo-crude) like any crude and distils it through the same cuts, with a smaller share of them. The stored experience stays behind and concentrates in the heavy bottoms. Drain the bottoms into a vessel and heat it above about **200 Tu**: the bottoms release their experience into the vessel's bank. A [Smelter Tap](Experience.md#taps) on the vessel drains the bank into an XP run; a full bank spills as orbs.

## The top of the column

Whatever rises past the top tray goes into whatever sits above it, if that takes it. If nothing does, it escapes as flammable vapor. See [Oil](Oil.md#fire).

A cold condenser on top pulls the whole profile colder, so lighter cuts stand at each tray and the cuts are sharper. Keep it cool the way you would any [Condenser](Condenser.md), with a [Heat Exchanger](Heat_Exchanger.md).

## Spatial slurry

The reboiler also takes **Raw Spatial Slurry** ([Liquid Teleportant](Liquid_Teleportant.md#making-it)). The slurry gives up water and steam first, then **Unstable Spatial Solvent**, which boils at 160 Tu. Vapor that escapes the top of the column hops nearby items and creatures. When the lights are gone, drain the spent bottoms from the reboiler with a pipe.

## Notes

- The column does nothing for time spent unloaded.
- Water is not a crude, and is boiled in a [Boiler](Boiler.md).
