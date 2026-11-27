# Firebox

A brick hole with a fire in it. Put fuel in and set a [Crucible](Crucible.md) or [Boiler](Boiler.md) on top.

## Crafting

```
B B B
B   B
B B B
```

B is any brick.

## Using it

- **Right-click with fuel** to load it.
- **Right-click with a piece of material** (a bar, plate or other worked stock; not a block, a hand tool or tongs) to lay one in the flame. It warms toward the flame's temperature. Only one piece lies there at a time.
- **Right-click with an empty hand** to take the laid piece out, or, when none lies there, to take back fuel that hasn't started burning.
- **With [Tongs](Hazards.md#tongs)**, see [Reheating in the fire](Hazards.md#reheating-in-the-fire).

A lit firebox smokes and flickers. Whatever sits on top (the fire always radiates up) climbs toward the fire's temperature. Items in a chest or other inventory above it, and dropped items there, warm too, and anything alive standing in the heat burns. See [Heat](Heat.md#heat-pushed-into-what-it-points-at).

## Fuels

Only real fuels burn here, and an item burns because of what it's *made of*: logs, stripped logs, wood blocks and planks are all wood, and a plank is a quarter of a log. Each tree species burns for its own time ([Woods](Materials.md#woods)); the table gives the range. Nether stems and other mods' planks don't burn. Each one has its own **flame temperature** and burn time:

| Fuel | Flame | Fully blown | Lasts | Varies by |
| --- | --- | --- | --- | --- |
| Sticks | 760 Tu | 1140 Tu | 4 s | 12% |
| Planks | 760 Tu | 1140 Tu | 5.5 to 12.5 s | 12% |
| Logs | 760 Tu | 1140 Tu | 21.5 to 50 s | 12% |
| Coal | 1180 Tu | 1770 Tu | 2 min | 10% |
| Charcoal | 1220 Tu | 1830 Tu | 80 s | 6% |
| Block of Coal | 1180 Tu | 1770 Tu | 18 min | 10% |
| Blaze Powder | 2000 Tu | 3000 Tu | 20 s | 12% |

**Blaze powder is oxygen-poor**: the figures above are what it makes *with an oxidiser*. In plain air it only smoulders, at 40% of the flame temperature and half the time (800 Tu for 10 s). A [Blaze Rod](Vanilla_Changes.md#blaze-powder-and-blaze-rods) is not a fuel; grind it to powder first. Two oxidisers:

- **Oxygen gas.** Pipe oxygen (or a bucket) into the Firebox's tank (500 mB). Each pinch of powder lit takes 25 mB and burns full strength for its full time.
- **Saltpetre.** Put a piece of saltpetre in the Firebox (the same click as fuel: it goes in its own slot). Each pinch of powder lit uses one and burns at full strength for half its time, a short violent burn.

With neither, the powder smoulders. Self-fired vessels have no oxidiser input, so blaze powder in one only smoulders. Nothing is damaged by a smouldering fuel.

Wood fires are too cool for any metalwork. Coal and charcoal will melt copper (1085 Tu) and gold (1064 Tu) but **won't reach steel** (1350 Tu) on their own.

**Every piece of fuel burns a little differently.** Each piece runs a bit hotter or cooler than the last, for its whole burn, so you'll notice good and bad batches.

## Air

A [Powered Bellows](Powered_Bellows.md) touching the firebox makes the fire up to **1.5× as hot**. It doesn't make the fuel last longer. Air raises the fire *relative to the fuel*, so a cool fuel blown hard is still a fairly cool fire. Charcoal plus a bellows is how you reach steel temperatures, and fully blown it's 1830 Tu: well past where iron burns. The trick is knowing when to stop blowing.

A fuel that **brings its own oxygen** (one whose material carries more oxidizer than it needs) burns as if it were being blown on, even with no bellows: the firebox reads the fuel's oxygen balance as well as the air. Ordinary fuels carry none.

The firebox holds a little air: about **half a second** of full burn. A full fire uses 4 mB of air a tick.

## Lava instead

You can skip the firebox and put a crucible or boiler straight over **lava**. It's a fixed, perfectly steady fire, but it's not hot enough for steel and you can't blow it hotter.

## Stoker

A screw feed that pushes solid fuel into the firebox in front of it, driven by a vertical shaft.

```
P X P
P H P
```

P is a Wrought Iron Plate, X is a [Shaft](Shaft.md) and H is a [Hopper](Hopper.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), screws or machine bolts. It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

A hopper or belt fills it from any side but the front. Three drum sizes, stepped by right-clicking with an empty hand (a strip under the crosshair lists the sizes with the current one lit), set how much each turn moves. A full firebox refuses the fuel and the feed stalls; the stoker costs only its own power while it waits. It starts and stops from a [Controller](Controller.md) or redstone. A [Kiln Zone](Furnaces.md#kiln-zone) is kept hot the same way.
