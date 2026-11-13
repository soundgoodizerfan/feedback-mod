# Electrostatic Generator

A hand-cranked static machine. Right-click it and it puts a short burst of [electricity](Electricity.md) on the line, then goes quiet again.

## Crafting

```
P G P
  R
```

P is a Copper Plate, G is Glass and R is a Copper Rod. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md).

## Using it

Right-click it with an empty hand. You hear a click, and for **one second (20 ticks)** it puts **0.5 A in the HV band** on the line. Clicking again before the pulse ends refills it to a full second; clicking faster doesn't stack it any higher. To keep something running you have to keep clicking.

It doesn't store anything between turns. When you stop clicking, the line goes dead. To keep a pulse, put a [Leyden Jar](Leyden_Jar.md) on the line: one pulse fills it.

## Compared to a Galvanic Cell

A pulse is a higher band than a [Galvanic Cell](Galvanic_Cell.md) (HV against LV) and delivers 256 Eu per tick against the cell's 32. An [Electromagnet](Electromagnet.md) is rated for LV, so a pulse is far over its rating: use it for the burst, not as a steady supply.

If both are on the same line, the generator takes over while it's turning, and Jade shows the cells as *"Outbid by a higher band"* until the pulse ends. Then the cells take over again. You can leave a cell on the line as a weak baseline and crank the generator when you want a burst.

## Tips

- Hold an [Electroscope](Instruments.md#electroscope) to the generator while someone turns it, and you'll see it go live and dead again.
- It's hand-turned only. For electricity from a shaft, use an [Dynamotor](Dynamotor.md).
