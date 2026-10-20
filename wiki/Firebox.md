# Firebox

A brick hole with a fire in it. Put fuel in and set a [Crucible](Crucible.md) or [Boiler](Boiler.md) on top.

## Crafting

```
B B B
B   B
B B B
```

B is a Brick.

## Using it

- **Right-click with fuel** to load it.
- **Right-click with an empty hand** to take back fuel that hasn't started burning.

A lit firebox smokes and flickers. It doesn't heat anything itself. Whatever sits on top climbs toward the fire's temperature. See [Heat](Heat.md).

## Fuels

Only real fuels burn here, and an item burns because of what it's *made of*: logs, stripped logs, wood blocks and planks are all wood, and a plank is a quarter of a log. Nether stems and other mods' planks don't burn. Each one has its own **flame temperature** and burn time:

| Fuel | Flame | Fully blown | Lasts | Varies by |
| --- | --- | --- | --- | --- |
| Sticks | 760 Tu | 1140 Tu | 4 s | 12% |
| Planks | 760 Tu | 1140 Tu | 7.5 s | 12% |
| Logs | 760 Tu | 1140 Tu | 30 s | 12% |
| Coal | 1180 Tu | 1770 Tu | 2 min | 10% |
| Charcoal | 1220 Tu | 1830 Tu | 80 s | 6% |
| Block of Coal | 1180 Tu | 1770 Tu | 18 min | 10% |
| Lava Bucket | 1200 Tu | 1800 Tu | 17 min | none |
| Blaze Rod | 1400 Tu | 2100 Tu | 2 min | 3% |

Wood fires are too cool for any metalwork. Coal and charcoal will melt copper (1000 Tu) and gold (950 Tu) but **won't reach steel** (1350 Tu) on their own.

**Every piece of fuel burns a little differently.** Each piece runs a bit hotter or cooler than the last, for its whole burn, so you'll notice good and bad batches. Lava doesn't vary at all.

## Air

A [Powered Bellows](Powered_Bellows.md) touching the firebox makes the fire up to **1.5× as hot**. It doesn't make the fuel last longer. Air raises the fire *relative to the fuel*, so a cool fuel blown hard is still a fairly cool fire. Charcoal plus a bellows is how you reach steel temperatures, and fully blown it's 1830 Tu: well past where iron burns. The trick is knowing when to stop blowing.

The firebox holds a little air: about **half a second** of full burn. A full fire uses 4 mB of air a tick.

## Lava instead

You can skip the firebox and put a crucible or boiler straight over **lava**. It's a fixed, perfectly steady fire, but it's not hot enough for steel and you can't blow it hotter.
