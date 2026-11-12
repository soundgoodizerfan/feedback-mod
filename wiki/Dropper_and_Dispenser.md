# Dropper and Dispenser

A dropper pushes an item out of its inventory, and a dispenser uses one (fires an arrow, empties a bucket, lights a fire charge). A **spring** does the pushing; the redstone signal only releases it.

## Crafting

Dropper:

```
C C C
C W C
C R C
```

C is any stone-crafting block (cobblestone and its kin), W is [Copper Wire](Wire_Drawer.md), R is redstone.

Dispenser (shapeless): a Dropper + string + a stick.

## What vanilla does

A redstone signal makes a dropper drop one item from its inventory, and a dispenser use one. The signal is all it needs.

## What Feedback does

Redstone only carries a **signal**; it cannot push an item out. A signal **releases** a cocked spring, which pushes the item out, and the spring is then **spent**. A signal on a spent spring only clicks.

**Cock it again** by sneaking and using it with an empty hand, or by putting it against a **turning shaft**, which winds it. A new one starts spent.

A dispenser is a dropper with a fitting that uses the item: arrows are fired and buckets emptied, as in vanilla.

Vanilla droppers and dispensers found in the world turn into these.
