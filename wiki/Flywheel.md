# Flywheel

A [shaft](Shaft.md) with a heavy disc, built from a blueprint. It stores momentum, so a line with one on it **rides through bumps in load** and **coasts for longer**.

## Designing and building

A flywheel is a modular build. Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Flywheel* in the type row) and print it on paper; drawing it takes Mechanical Treatise I ([Treatise](Treatise.md)). It has one part, the **Flywheel Disc**, with four choices:

| Choice | What it sets |
| --- | --- |
| **Shape** | Spoked, solid or rimmed. See [Shapes](#shapes). |
| **Radius** | 30 to 100 cm. Momentum rises with the fourth power of the radius, but a bigger disc strains at fewer RPM. |
| **Thickness** | 5 to 30 cm. A thicker disc holds more momentum. |
| **Metal** | Plate or ingot of a metal with a measured weight and yield strength. A dense metal gives more momentum; a strong, light metal lets the disc spin faster. |

At the [Smithing Table](Smithing_Table.md), put the Flywheel Blueprint in the blueprint slot and shape the disc from plate or ingot of the metal you chose. The Flywheel comes out with the design you drew, and the blueprint is kept.

### Shapes

| Shape | Momentum | Rim speed | Stock |
| --- | --- | --- | --- |
| Spoked | 0.6 of solid | 0.8 of solid | least |
| Solid | standard | standard | standard |
| Rimmed | 1.3 times solid | 0.9 of solid | most |

### Addons

| Addon | Stock | What it does |
| --- | --- | --- |
| **Rim bands** | rod | Bands round the rim let the disc spin faster before it strains. |
| **Bearing** | nugget | Cuts the disc's drag by 30%. |

## Using it

Place it in a shaft line like any shaft. It can also be placed as a voxel of a [micro build](Micro_Core.md).

## What it does

A flywheel is far heavier than a plain shaft, and the radius, thickness, shape and metal you drew set how much. Heavy things change speed slowly, in both directions:

- **Smoother.** A [Mechanical Hammer](Mechanical_Hammer.md)'s blows, a press's strokes, a [Pump](Pump.md) cutting in: the line barely notices them.
- **Slower to start.** It takes longer to come up to speed, especially on a lightly powered line.
- **Slower to stop.** Disconnect the power and it keeps the line turning much longer. That's good for riding out a gap in supply, and bad if you need a machine to stop *now*. A hammer behind a flywheel lands more blows after you throw the [Clutch](Clutch.md).

## What it costs

Its bearing drags, more for a bigger disc, and that is the price of the momentum, paid every turn. A bearing addon cuts it. The tooltip shows the drag in Su per RPM.

A flywheel that spins past the **rim speed** its own metal and shape allow strains and loses condition, down to a floor. It never bursts. The tooltip shows the RPM it strains past.

A micro flywheel's rim holds **4 times** the full-size speed before it strains. A strained rim loses condition down to a floor of 50% and carries less inertia as it does.

## When to use one

- A line that bogs down every time a heavy machine kicks in.
- A [Steam Engine](Steam_Engine.md) or [Engine](Engine.md) that stutters.
- A [Controller](Controller.md) that flips a clutch fast and makes the speed jump around.

Not on a line you need to stop precisely.
