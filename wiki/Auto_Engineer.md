# Auto Engineer

A machine that repeats one assembly you made by hand at the [Engineering Table](Engineering_Table.md). It reads the assembly from a [Recipe Data Stick](Data_Stick.md), takes parts from a bay, wears its own hand tools and spends [fasteners](Fasteners.md) from its stock.

## Crafting

```
S S S
E M S
S S S
```

S is a Steel Plate, E is an Engineering Table and M is a [Dynamotor](Dynamotor.md).

## Recording

Hold a blank Recipe Data Stick in your **off hand** while you take an Engineering Table craft from the output slot. The stick keeps what you made, the kinds of stock you used (a kind and a form, such as a metal plate, or the exact item for anything else), which hand tools the craft wore, how many fasteners it spent and the treatise volumes you held. Vanilla crafts, engineering recipes and modular assemblies are all recorded alike. A stick holds one recording or one [Power Cutter](Power_Cutter.md) cut, and another recording overwrites it.

## Using it

- **Right-click** with a stick to seat it. **Right-click** with a hand tool to put it in one of the four tool slots, with a fastener to fill the fastener stock, and with anything else to fill the parts bay.
- **Right-click** empty-handed to take the output, and then the stick. **Sneak** and right-click empty-handed to take back the tools, fasteners and parts.
- A **hopper** can fill the bay and the fastener stock, and empty the output. The tool slots are filled by hand.

It is an [EHV](Electricity.md#volts-and-amps) sink, about **2.6 A at 384 V** while parts are in the bay. An assembly takes about 3 seconds.

## How it assembles

When the bay holds stock of every kind the stick recorded, in at least the recorded amounts, the machine builds what the stick recorded. The stock may be a different material from the one you used, so the same recording turns out the same item in another metal. It lays the parts out as you did, wears each tool the recording called for one point and spends the fasteners from its stock. There is no hand-work game, and the item starts no better than the floor start: fasteners below the floor lower it. A [blueprint](Blueprint_Table.md) is not used.

It waits, spending nothing, when a tool is missing or broken, or the fastener stock is short.

### Stock the stick does not cover

Parts that do not match the recording are not refused. The machine picks one thing the whole bay could legally form, the same thing every time in a world, and consumes the bay to make it. If nothing could be formed, it scraps one stack to nuggets of its material, with half the material lost. Stock with no material or no nugget is lost. This never wears the tools and spends no fasteners.

## Wear

A correct assembly never wears the machine. It wears from stock too hard for it, from a full output slot, from scrapping and from a supply over its voltage rating. A worn machine works slower and never stops. [Calipers](Instruments.md#calipers) read its condition while it is stopped.
