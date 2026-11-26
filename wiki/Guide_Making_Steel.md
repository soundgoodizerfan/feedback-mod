# Guide: Making Steel

Steel is the first big wall in Feedback. Nothing locks it away, but you have to hold a crucible inside a **narrow, hot temperature window** for a while, and that's hard. Everything you learn about [heat](Heat.md) comes together here.

Check JEI's *Thermal process* page for **Steel Ingot** for the exact figures; they're summarized below.

## What you need

- A **[Crucible](Crucible.md)** on a **[Firebox](Firebox.md)**. A **small** crucible climbs too fast on a full fire; a **large** one stays in the band (see below).
- **Charcoal** to burn, and a **[Powered Bellows](Powered_Bellows.md)** on the firebox. Charcoal alone isn't hot enough.
- An **Iron Ingot** (cast iron; nugget, scrap, swarf or dust also work) and a piece of **Charcoal** to put *in* the crucible. Smelt it from ore ([Other routes](#other-routes)). A **Wrought Iron Ingot** also works, on a [slower, cooler band](#other-routes).
- Ideally a **[Thermometer](Instruments.md#thermometer)**.

## The process

Put the iron and the charcoal into the crucible, then get the crucible into steel's band and keep it there:

| | |
| --- | --- |
| Band | 1350–1410 Tu |
| Fastest at | 1380 Tu |
| Hold | 30 seconds, if held exactly at 1380 |
| Max rate | 5 Tu per tick |
| Spoils above | 1470 Tu, into **Burnt Iron** |

What that means in practice:

- **Below the band** nothing happens, and progress you'd made slowly slips away. That's safe, just slow.
- **Inside the band** it progresses, fastest near the middle.
- **Above the band** it stops progressing again, and you're getting close to the spoil line.
- **Above the spoil line** the batch is **ruined instantly**. The iron and charcoal become Burnt Iron.
- **Heating too fast** also stops progress, even inside the band. A **light** crucible (few slots, a thin wall) on a full fire climbs faster than steel allows, so it can't make steel at all at full blast. A heavier crucible climbs slower.

## Tips

- With only words to go on, steel's window straddles *yellow / pale yellow* (1300–1400 Tu), which is too vague. The thermometer reads in 25 Tu steps, about two steps inside the window, which makes this doable by hand: heat, check, ease off, check again.
- **Insulation** helps a heavy crucible sit steadily in the band.
- Run the bellows through a [Clutch](Clutch.md) so you can stop the air without stopping your whole line.
- A **[Bimetallic Strip](Bimetallic_Strip.md)** can switch the bellows for you, but the standard strip trips at **1440 Tu**, above steel's band (top 1410). It holds a heavy crucible within about 20 Tu either side of 1440: no progress, but no burnt iron either. On a light crucible it swings past 1470 and burns the batch.
- For proper automatic control, bolt a [Temperature Sensor](Instruments.md#temperature-sensor) on and use a [Controller](Controller.md).

## Other routes

- **Iron Ingot from ore.** Raw iron, crushed ore, concentrate or dust with charcoal, held between **1420 and 1650 Tu**, fastest near 1500, spoiling to Burnt Iron above 1750, gives the Iron Ingot ([Iron](Iron.md#getting-it)).
- **Wrought iron to steel.** A Wrought Iron Ingot (nugget, scrap, swarf or dust) with charcoal held between **1000 and 1250 Tu**, fastest near 1100, spoiling to Burnt Iron above 1400, gives steel more slowly ([Guide: Making Wrought Iron](Guide_Making_Wrought_Iron.md)). Dust or swarf holds for less than an ingot: a finer form carburizes sooner.
- **Four Steel Swarf** (from [drilling](Drill_Press.md), [turning](Lathe.md) or [grinding](Grinding_Wheel.md) steel) can be melted back into a steel ingot in the same band, with an even shorter hold. Nothing has to go to waste.

## After steel

- Hammer it into [Steel Plate](Mechanical_Hammer.md) for [boilers](Boiler.md) and [steam engines](Steam_Engine.md). Steel only moves between **900 and 1100 Tu**, so it has to be hot, and a fresh ingot at 1400 Tu burns your hands: carry it with [tongs](Hazards.md#tongs).
- [Harden and temper it](Guide_Heat_Treatment.md).
