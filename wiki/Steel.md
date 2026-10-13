# Making Steel

Steel is the first big wall in Feedback. Nothing locks it away, but you have to hold a crucible inside a **narrow, hot temperature window** for a while, and that's hard. Everything you learn about [heat](Heat.md) comes together here.

Check JEI's *Thermal process* page for **Steel Ingot** for the exact figures; they're summarised below.

## What you need

- A **[Crucible](Crucible.md)** on a **[Firebox](Firebox.md)**. A **large** crucible is strongly recommended.
- **Charcoal** to burn, and a **[Powered Bellows](Powered_Bellows.md)** on the firebox. Charcoal alone isn't hot enough.
- An **Iron Ingot** and a piece of **Charcoal** to put *in* the crucible.
- Ideally a **[Thermometer](Thermometer.md)**.

## The process

Put the iron ingot and the charcoal into the crucible, then get the crucible into steel's band and keep it there:

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
- **Heating too fast** also stops progress, even inside the band. A **small** crucible on a full fire climbs faster than steel allows, so it can't make steel at all at full blast. That's the real reason for the large crucible.

## Tips

- With only words to go on, steel's window is somewhere around *yellow / pale yellow*, which is too vague. The thermometer makes this doable by hand: heat, check, ease off, check again.
- **Insulation** helps a large crucible sit steadily in the band.
- Run the bellows through a [Clutch](Clutch.md) so you can stop the air without stopping your whole line.
- A **[Bimetallic Strip](Bimetallic_Strip.md)** can switch the bellows for you, but check its trip temperature (shown in Jade) against steel's band first. If it trips above the band, it will let the crucible run too hot.
- For proper automatic control, bolt a [Temperature Sensor](Temperature_Sensor.md) on and use a [Controller](Controller.md).

## Other routes

- **Cast Iron + Charcoal** also makes steel, in a shorter hold. Cast iron comes from an **Iron Ingot + Coal** held at a lower, wider band (check JEI).
- **Four Steel Swarf** (from [drilling](Drill_Press.md), [turning](Lathe.md) or [grinding](Grinding_Wheel.md) steel) can be melted back into a steel ingot in the same band, with an even shorter hold. Nothing has to go to waste.

## After steel

- Hammer it (hot!) into [Steel Plate](Mechanical_Hammer.md) for [boilers](Boiler.md) and [steam engines](Steam_Engine.md).
- [Harden and temper it](Heat_Treatment.md).
