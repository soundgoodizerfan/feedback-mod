# Campfire

A vanilla block. Feedback keeps what it does and gives it a second job as a fire under a vessel.

## What vanilla does

It cooks up to four food items placed on it into a fixed cooked item after a set time, sends up smoke, hurts anything standing in it, and burns forever on no fuel once lit. Water or a shovel puts it out; flint and steel or a fire charge lights it again. The soul campfire is the same block with blue flame.

## What Feedback does

Lighting, water and shovels, smoke and damage are unchanged, and food is still placed on it (right-click, up to four). Cooking is different: nothing is output on a timer. Food on the fire **warms** toward the flame like food in a [Skillet](Skillet.md) pan, and cooks, dries and chars by the same rules as in a [Furnace](Furnaces.md): it is safe (live organisms dead) only once it has actually cooked through, and it burns if left. Right-click with an empty hand to take the last piece off, carrying its heat. Placed food and the campfire itself are Feedback's own copy of the block (an old one is swapped on load). Campfires break fastest with an axe. On top of that, a **lit** campfire is a **fire fixed at 760 Tu**, the flame temperature of wood, that never runs out. A lit **soul campfire** burns hotter, at **1050 Tu**:

- **Under a vessel.** A [Crucible](Crucible.md) or [Boiler](Boiler.md) placed straight over a lit campfire climbs toward 760 Tu and stays there. Hot enough to boil water, cook, and melt the soft metals (tin, lead, zinc); too cool for copper, gold or iron. A soul campfire also melts copper, bronze, brass, gold and silver, but stays too cool for cast iron and [steel](Steel.md); for those you still need [Lava](Lava.md) or a [Firebox](Firebox.md). For more heat, use a [Firebox](Firebox.md) or [Lava](Lava.md).
- **Put it out, the heat stops.** An unlit campfire is no fire at all.

## Buried: making charcoal

A campfire with **no air touching it** (all six neighbors, the block above included, are solid on the face towards the fire; water, leaves, fences, panes, plants and torches do not count) is **buried**. Build a hearth of dirt or stone around it, put logs on it (right-click, up to four) and cover it over.

- The fire starves: a buried lit campfire (soul fire too) smoulders at **420 Tu** instead of 760 or 1050.
- Logs on a buried campfire char like logs in a [Kiln](Furnaces.md): between 300 and 500 Tu, fastest near 400, each becomes one charcoal in about a minute. They never reach the 700 Tu at which wood burns away.
- Open any side and the fire flares back up to full heat; the logs stop charring (an open fire makes no charcoal, and does not burn them either).
- Break a block of the hearth to reach it and take the charcoal off with an empty hand. Jade shows "Buried: no air, smouldering".
