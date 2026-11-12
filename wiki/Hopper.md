# Hopper

A hopper carries one item at a time from the container above it into the block it faces. It holds nothing itself, so it links two blocks and does not store, queue or sort.

## Crafting

```
P   P
P C P
  P
```

P is a [Wrought Iron Plate](Making_Wrought_Iron.md), C is a chest.

## What vanilla does

A hopper has five slots of its own. It pulls items out of the container above it into those slots, then pushes them on into the container it faces. Because it holds items, a line of hoppers can queue, store and sort.

## What Feedback does

Every few moments the hopper takes **one item** from the container above it, or one item lying on top of it, and puts it **straight into the block it faces**, in a single step. If the block it faces cannot take that item, nothing moves and the item stays where it was. A chain of hoppers is a longer conveyor.

The hopper faces the way you point it when you place it (down, or one of the four sides). A redstone signal switches it off.

Vanilla hoppers found in the world turn into Feedback hoppers and keep their direction.
