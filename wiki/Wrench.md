# Wrench

Turns a placed block without breaking it.

## Crafting

Hammered hot in a crafting grid: put a [Hand Hammer](Hand_Hammer.md) (or another striking hand tool) and **4 Iron Ingots** anywhere in the grid, with every ingot at iron's working temperature. Heat the ingots in a furnace or [Firebox](Firebox.md) first; a cold ingot makes nothing. The hammer loses one point of wear.

## Use

- **Right-click** a block to turn it a quarter turn around the face you clicked, clockwise as you look at that face.
- **Sneak-right-click** turns it the other way.

The face you click is the axle the block turns on. So clicking the top of a block spins it like a turntable; clicking a side tips it over.

- A block that faces somewhere (an [Engine](Engine.md), a [Dynamo](Dynamo.md), a [Motor](Motor.md)) turns its facing. If the block can't face the way a quarter turn would point it -- one that only ever faces sideways, turned onto its back -- the wrench skips ahead to the next way it can face.
- A block that lies along an axis (a [Shaft](Shaft.md), a log) swings to a new axis.
- A block facing straight along the axle doesn't change: turning it about itself does nothing.
- A block with no direction at all is left alone.

Shafts and wires reconnect at once: a turned shaft joins whatever it now points at, and leaves what it pointed at before.

The wrench takes the click before the block does, so wrenching an engine turns it rather than switching it on.

The wrench also takes a **[Lift](Vehicles.md#the-lift)** apart quickly: mine it with the wrench and it comes off in a moment.
