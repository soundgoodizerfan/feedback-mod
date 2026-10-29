# Relay

An electromagnet and a set of contacts. A small current in the magnet's line switches a different line. This is how a weak signal (a [Coherer](Redstone_Coherer.md), a [Breaker](Breaker.md) a [Controller](Controller.md) switches) works something that draws a lot.

## Crafting

```
W P W
C C C
```

W is [Copper Wire](Wire_Drawer.md), P is a Wrought Iron Plate and C is Ceramic.

## Using it

Put the relay **directly against an [Electromagnet](Electromagnet.md)** (any side). Wire the magnet into one line and the relay into another. The relay is a wire in the second line: open until the magnet's field is strong enough, then closed. It does **not** join the magnet's line, even though they touch.

- Pulls in when the magnet's field reaches **60%**, lets go below **30%**. The gap stops a weak coil from chattering.
- To let a [Controller](Controller.md) switch a big load: put a [Breaker](Breaker.md) in the magnet's line and give the controller a *Switch* card for it. To let a controller watch the big load, link a [Current Sensor](Current_Sensor.md) on the relay's line.

## It wears when it switches under load

Like a breaker, opening with current flowing draws an arc that pits the contacts (**1% per amp** broken), and the arc is a **spark** other detectors can hear. Carrying more than **4 A** wears it a little each tick. Closing, holding and sitting idle cost nothing.

Worn contacts let go **late**: each tick it should open, it manages it with a chance equal to its condition. It never refuses. Its condition bottoms out at 20%.
