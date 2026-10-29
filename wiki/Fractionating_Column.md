# Fractionating Column

Splits [crude oil](Crude_Oil.md) into naphtha, kerosene and diesel by boiling it at the bottom and letting the vapour climb through trays that are colder the higher they sit. A reboiler at the bottom, one or more trays above it, and optionally a [Condenser](Condenser.md) on top: stack them upward and they join into one column on their own. There is no controller block.

## Building it

Stack the blocks straight up, in this order:

1. one **Reboiler**
2. one or more **trays** (any mix of the three kinds)
3. optionally one **[Condenser](Condenser.md)** on top

A column with a single tray works, but smears its cuts. A column with no condenser still works, but runs warm at the top.

## Reboiler

The base of the column. Like a [Boiler](Boiler.md) it takes heat from **below**: a [Firebox](Firebox.md), lava, or a [Heating Coil](Heating_Coil.md). It holds **4,000 mB** of crude.

- **Feed it** with a bucket (right-click), a [Pipe](Pipe.md), or a [Pump](Pump.md). It also reaches for crude from any neighbour at its sides and underneath, the way a [Steam Engine](Steam_Engine.md) reaches for steam. It only accepts a crude: water or anything else that is not boiled in cuts is refused.
- **Steam cleans the trays.** Steam pumped into the reboiler is not stored. While the reboiler is under **420 Tu** it lifts coke off every tray.
- Nothing can be taken out of it. The heavy bottoms stay in the reboiler; only the light cuts leave.
- Each cut starts boiling once the reboiler passes its boiling point: **naphtha 100 Tu, kerosene 180 Tu, diesel 300 Tu**. A hotter reboiler makes every tray hotter, so every cut moves heavier.

The feed's grade matters. A crude with more light fraction gives more of every cut. As the lights are boiled off, the feed left in the reboiler holds less of them. See [grades](Crude_Oil.md#grades).

### Reflux

The reboiler is also the return valve. **Right-click it with an empty hand** to cycle reflux: **none**, **partial**, **total**. Sneak-click to read the setting without changing it.

| Reflux | Boil-up | Cuts |
| --- | --- | --- |
| None | full | loosest |
| Partial | 70% | sharper |
| Total | 40% | sharpest |

More reflux trades throughput for cleaner cuts.

## Trays

Each tray is a cut point. The vapour rising from the reboiler meets the tray at a temperature: the column is a straight line from the reboiler's temperature at the bottom to the condenser's body temperature at the top (with no condenser, the top sits halfway between the reboiler and the air). A tray **colder than a cut's boiling point** holds back part of that cut as liquid and lets the rest rise to the next tray.

The liquid a tray catches collects in its **draw tank (1,000 mB)** and is pushed out sideways, up to 40 mB a tick, into any fluid block beside it: a [Pipe](Pipe.md), a [Pump](Pump.md), a tank. Nothing can be put into a tray from outside. **Right-click** a tray with an empty hand to read its condition and what it is drawing.

| Tray | Holds back | Takes up to | Fouls |
| --- | --- | --- | --- |
| Bubble-Cap | 92% | 20 mB/t | normally |
| Sieve | 80% | 40 mB/t | fastest (1.5x) |
| Packed | 70% | 60 mB/t | slowest (0.6x) |

The sharper the tray the less it flows. Vapour a tray cannot take rides up to the next one.

### Coke

A tray hotter than **420 Tu** cokes up, faster for a heavier feed and faster the hotter it runs. Coke lowers both what the tray holds back and what it can take, down to nothing. Running correctly never cokes a tray: only running it too hot does. Clear it with steam through the reboiler (once the reboiler is cooler than 420 Tu).

## The top of the column

Whatever rises past the top tray goes into whatever sits above it, if that takes it. If nothing does, it escapes as flammable vapour. See [Crude Oil](Crude_Oil.md#fire).

A cold condenser on top pulls the whole profile colder, so lighter cuts stand at each tray and the cuts are sharper. Keep it cool the way you would any [Condenser](Condenser.md), with a [Heat Exchanger](Heat_Exchanger.md).

## Notes

- The column does nothing for time spent unloaded.
- Water is not a crude, and is boiled in a [Boiler](Boiler.md).
