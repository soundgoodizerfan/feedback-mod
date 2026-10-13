# Heat Treatment

What happens to steel depends not only on how hot it is, but on **how it got there**.

## Hardening (quenching)

Take a [Steel Ingot](Steel.md) out of the crucible while it's still **very hot** and **throw it into water**. If it's hot enough when it lands, it becomes **Hardened Steel**. You'll hear the hiss.

- The ingot starts cooling the moment it leaves the crucible, so **have water right next to the crucible**. A long walk and it'll be too cool by the time it lands.
- Any water works: a pool, a river, a single water block.
- A steel ingot that's too cool just gets cooled. Nothing is lost except the heat, so reheat and try again.

Hardened steel is extremely hard, too hard for the [Drill Press](Drill_Press.md) or [Lathe](Lathe.md). Only the [Grinding Wheel](Grinding_Wheel.md) can shape it.

## Tempering

Hardened steel is hard but brittle. Tempering it gives **Tempered Steel**. JEI's figures:

| | |
| --- | --- |
| Band | 300–400 Tu |
| Fastest at | 350 Tu |
| Hold | 20 seconds, at 350 |
| Special rule | **only counts while it's cooling** |

The catch is that last line. Holding hardened steel steady at 350 does nothing. It only makes progress while the temperature is **falling** through the band. If it's flat or rising, progress just pauses (you don't lose any).

So the method is:

1. Heat it a bit past the top of the band.
2. Cut the heat and let it **cool down through** the band.
3. If it drops out of the bottom before it's done, heat it up again and repeat.

Ways to do it:

- **[Annealing Furnace](Furnaces.md)**: it physically can't get hotter than the band, so you can't overcook it, and it's heavy enough to cool slowly. Fuel it, let it top out, let the fuel run down. It's the safe, simple way.
- **[Crucible](Crucible.md) with [Dampers](Damper.md)**: heat past the band, then open the dampers to cool on cue. It's faster and more controllable, but you can overshoot. A [Controller](Controller.md) can run the dampers for you.
- **[Heat Exchanger](Heat_Exchanger.md)** cooling counts too.
