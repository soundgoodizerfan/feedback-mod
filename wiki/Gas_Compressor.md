# Gas Compressor

Squeezes gas into a [pipe](Pipe.md) or a [Gas Tank](Gas_Tank.md) past the point where gas flows in by itself. That is what raises a line's pressure.

## Placing

Like a [Pump](Pump.md): place it **against the block it should draw from**. It draws from its back face and pushes out of its front. The shaft goes in **across** the flow: from above or below on a compressor lying sideways, and from the east or west side on one pointing straight up or down.

## Using it

- It moves **gas only**. A liquid in front of it is ignored.
- It moves **0.4 mB per tick for every RPM** and loads the shaft with **30 Su**.
- It **heats what it moves** by 60 Tu. A hot line is a cooling job, and with a flammable vapour like naphtha, a hazard. See [Crude Oil](Crude_Oil.md#fire).
- Into a pipe run it can push a gas up to three times the run's volume (6 Pu). Into a gas tank it can push all the way up, past the tank's danger line. Into anything else it just fills.

## Surge

If the front can take nothing more, the compressor **surges**: its condition falls, down to a floor of 20%, and it moves proportionally less. Only this wears it. Running into a line with room never does. Back off, or open the line to a [Gas Tank](Gas_Tank.md) or a consumer.

Read the line with a [Pressure Gauge](Pressure_Gauge.md).
