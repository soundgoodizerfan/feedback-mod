# Electricity

Electricity is the newest power type in Feedback, and right now it's small: two ways to make it, one wire to carry it, one machine that uses it, and three ways to measure it. It's closer to a science-fair table than a power grid. Don't expect it to run your factory yet.

It is **not** the "universal" power you might be used to from other tech mods. Your mechanical and thermal setups never need to be converted into electricity, and nothing here turns electricity back into rotation or heat on purpose.

## The pieces

| Block / item | What it does |
| --- | --- |
| [Galvanic Cell](Galvanic_Cell.md) | Gives a small, steady supply forever. |
| [Electrostatic Generator](Electrostatic_Generator.md) | Gives a short burst each time you turn it. |
| [Copper Cable](Copper_Cable.md) | Carries power between everything. |
| [Electromagnet](Electromagnet.md) | Pulls dropped iron and steel items toward itself. |
| [Electroscope](Electroscope.md) | Tells you a line's voltage, roughly. |
| [Galvanometer](Galvanometer.md) | Tells you how much current is flowing. |
| [Current Sensor](Current_Sensor.md) | A galvanometer you bolt onto a cable so a [Controller](Controller.md) can read it. |

## How a line behaves

Everything that touches is connected. Put a source, some cable and a magnet next to each other and they form one line. You don't need to configure anything.

**The highest voltage wins.** If two different sources are on one line, the stronger one does all the supplying and the weaker one just sits there. Look at the weaker one with Jade and it says *"Outbid by a higher voltage."* Sources at the same voltage share the work instead. That's why several Galvanic Cells on one line add up, but a Galvanic Cell next to a turning Electrostatic Generator goes quiet until the pulse ends.

**Long lines lose power.** Every block of cable takes a little off the voltage, and the more current it carries, the more it takes. A magnet at the end of a long run is noticeably weaker than one right next to its cell. That's normal. Shorten the run, or put the source closer.

**Overloaded sources sag.** If you hang more machines on a source than it's meant to feed, it keeps going, but its voltage drops and every machine on it slows down. No source ever breaks from this.

**Starved machines share fairly.** If there isn't enough to go round, every machine gets the same share of what it wanted. The one closest to the source doesn't hog it all. Jade shows any machine that's short as *"Starved."*

**Machines don't pass power along.** A magnet is the end of the line. To feed two magnets, run cable to each of them. Don't chain them through each other.

## Reading a line

You can always tell whether a line is working just by looking at it with Jade:

- **Live**: there's voltage here.
- **Dead**: nothing's reaching this block.
- **Starved**: this machine is getting less than it wants.
- **Outbid by a higher voltage**: this source is being overruled by a stronger one.

For actual numbers you need an instrument somewhere in your inventory: an [Electroscope](Electroscope.md) adds volts to the Jade tooltip, and a [Galvanometer](Galvanometer.md) adds amps. You can also right-click any electrical block with one for a direct reading.

## Things that will hurt you

Overloaded cable gets hot. Push it far enough and the insulation burns off, and then **bare cable carrying current shocks anyone standing in it**. Keep pushing and the cable burns out completely. See [Copper Cable](Copper_Cable.md#overloading).

## See also

- [Coilgun](Coilgun.md): unrelated to the wiring above, but the other place redstone and magnetism meet.
