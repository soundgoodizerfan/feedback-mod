# Crops

Wheat, carrots, potatoes, beetroots, saplings, cactus, sugar cane, cocoa, bamboo and sweet berries. They still grow from random ticks and bonemeal works as before. Feedback changes how often a tick lets them grow.

## In vanilla

A crop grows at a fixed chance whatever the weather, in any biome or dimension, as long as there is light and the soil is right.

## In Feedback

- **Weather sets the rate.** The place a crop stands has a temperature: its dimension's ambient, shifted by the biome (a snowy plain is cold, a desert hot, the Nether hotter still). Inside the plant's comfortable band it grows as normal. Outside the band it grows less and less often, down to a quarter of normal. Nothing is forbidden; it is a rate.
- **Frost halts, never kills.** Below the plant's frost line it stops growing. It is not hurt, and it carries on when it warms.
- **Each plant has its own band.**
  - Crops (wheat, carrots, potatoes, beetroots): mild weather, stop below freezing.
  - Saplings: cooler and more tolerant.
  - Cactus, sugar cane, cocoa and bamboo: warm weather; they stop growing in the cold much sooner.
  - Sweet berries: cold weather; they are the plant for a snowy biome.
- **Bonemeal ignores weather**, as it always did.
- A modded crop follows the same rule if it is added to one of the plant block tags (`feedback:warm_plants`, `feedback:cold_plants`, or the vanilla crop tags), or has its own line in `data/<namespace>/crop_climate/`.

Not yet: a warmed block (a hearth under a planter) does not warm the crop above it.

## New crops

Cabbage, onion, garlic, turnip, soybeans, tomato, corn, chile pepper, rice, ginger and black pepper grow from seeds on farmland through four stages. Breaking grass has a small chance to drop a seed. A grown plant drops its food (one or two) and a seed to replant.

- **Cool crops** (cabbage, onion, garlic, turnip, soybeans) like mild to cool weather and take a frost better than wheat.
- **Warm crops** (tomato, corn, chile pepper, ginger, black pepper) want warm weather and halt in anything near freezing.
- **Rice** wants the same warmth and standing water within two blocks: without water it grows at its slowest rate.

Click a plant with a [thermometer](Thermometer.md) to read the weather it stands in and how well it is growing there (halted, poorly, slowly, well).
