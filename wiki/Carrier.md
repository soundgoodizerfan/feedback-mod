# Carrier

The capsule that rides a [Pneumatic Tube](Pneumatic_Tube.md). A carrier holds four stacks of items and a color **address** that [junctions](Tube_Junction.md) route by. The Magnetic Carrier and the Pod are its variants.

## Crafting

**Carrier** (makes 2):

```
P P
P P
```

P is a Copper Plate.

## Using it

You do not fill a carrier by hand. Stock carriers in a [Tube Station](Tube_Station.md): the station packs items into one, stamps its address, and sends it into the tube. A carrier arriving at a station is emptied into the station's output.

The address is a dye color. Junctions read it and nothing else, never the contents. A heavy load slows a carrier.

## Magnetic Carrier

A carrier with a magnetic plate. A [Linear Motor Ring](Pneumatic_Tube.md#linear-motor-ring) drives it through a tube block at twice the speed; it otherwise rides like a plain carrier.

Crafting: Carrier + Wrought Iron Plate.

## Pod

A one-seat capsule for a wide-bore run.

```
S S S
G C G
S S S
```

S is a Steel Plate, G is a Glass Pane and C is a Carrier.

- Stock a pod in a tube station, then **right-click the station with an empty hand** to board.
- **Sneak** to leave: you step out at the tube.
- Only players ride.
- A pod needs wide-bore tube all the way. It is slower than a carrier at the same pressure and slows further in bends and vertical runs.
- If a chunk on the path is not loaded, the pod waits for it.
