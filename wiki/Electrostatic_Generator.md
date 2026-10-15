# Electrostatic Generator

A hand-cranked static machine. Right-click it and it puts a short burst of [electricity](Electricity.md) on the line, then goes quiet again.

## Crafting

```
P G P
  R
```

P is [Copper Plate](Mechanical_Hammer.md), G is Glass, and R is a [Copper Rod](Rolling_Mill.md).

## Using it

Right-click it with an empty hand. You hear a click, and for **one second (20 ticks)** it puts **1.5 V at 1 A** on the line. Clicking again before the pulse ends refills it to a full second; clicking faster doesn't stack it any higher. To keep something running you have to keep clicking.

It doesn't store anything between turns. When you stop clicking, the line goes dead. To keep a pulse, put a [Leyden Jar](Leyden_Jar.md) on the line: one pulse fills it.

## Compared to a Galvanic Cell

A pulse has a higher voltage than a [Galvanic Cell](Galvanic_Cell.md) (1.5 V against 1.1 V), so an [Electromagnet](Electromagnet.md) pulls at full strength on a pulse and about three-quarters on a cell.

If both are on the same line, the generator takes over while it's turning, and Jade shows the cells as *"Outbid by a higher voltage"* until the pulse ends. Then the cells take over again. You can leave a cell on the line as a weak baseline and crank the generator when you want a strong pull.

## Tips

- Hold an [Electroscope](Electroscope.md) to the generator while someone turns it, and you'll see it go live and dead again.
- It's hand-turned only. For electricity from a shaft, use a [Dynamo](Dynamo.md).
