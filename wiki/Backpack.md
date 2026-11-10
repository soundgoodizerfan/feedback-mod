# Backpack

A pack worn in the Curios **back** slot. It holds 9, 18 or 27 items, and the strap decides how much weight you can carry in it before it slows you.

## Crafting

Made at the [Woodworking Table](Woodworking_Table.md) (or the [Smithing Table](Smithing_Table.md) for plate parts) from two parts, as for any [modular tool](Tools_and_Weapons.md):

- **Shell** (Backpack Shell): the body, shaped from leather, carpet or plate stock. Its shape fixes the slots: a **pouch** (one cell) holds 9, a **satchel** (three cells, the standard shape) holds 18, a **pack** (nine cells) holds 27. The shell's material only decides how heavy it is.
- **Strap**: leather, string or wire. It sets the carry rating, and it is the part that wears.

The [Blueprint Table](Blueprint_Table.md) shows the slots, the carry rating, the mounting points left and the store slots of a drawing before you make it.

## Using it

Wear it in the back slot and press **B** (Open Backpack) to open its contents as a chest screen. A pack cannot go into a pack. A worn pack shows on your back in the fabric and colors of its material, with its fittings, and a lit lantern in a lamp bracket.

## Carry rating

The rating is the strap's strength times 8, and it falls as the strap wears. The load is the mass in Mu of the pack plus everything in it, store slots included; what else you carry does not count. A pack going on or off, or its contents changing, is weighed at once.

What an item weighs:

- items tagged as light or heavy (the tags `feedback:pack_mass/light` and `feedback:pack_mass/heavy` can be filled by a datapack) weigh a fixed small or large amount;
- otherwise a material item weighs by its density and size, so an iron ingot is a little over 1 Mu;
- anything else weighs a flat middling amount.

## Overloading

With the load over the rating you are **30% slower** and **cannot sprint**, and the strap wears one point every second. At its last point the strap lets go: the pack drops off you with everything in it. Rework the strap to mend it (see [mending](Tools_and_Weapons.md#when-it-breaks-mending-and-swapping-parts)).

## Fittings

A finished pack takes **fittings**: rework it at the [Woodworking Table](Woodworking_Table.md) (the [Smithing Table](Smithing_Table.md) for plate fittings) and lay the stock on it. Take the pack apart to remove one. Every fitting costs **mounting points**; the shell gives them (pouch 1, satchel 2, pack 4) and a frame adds 2. A fitting that does not fit in the points left is refused.

| Fitting | Stock | Points | What it does |
| --- | --- | --- | --- |
| Frame | 4 rods, sticks or bones | gives 2 | Takes load off the strap: raises the carry rating. It never wears. |
| Hip belt | 3 hides | 1 | Raises the carry rating; needs a frame; wears with the strap. |
| Padded panel | 2 carpets | 1 | Raises the carry rating; softer cloth raises it more. |
| Side holster | 2 hides | 1 | One store slot for a tool or weapon, drawn without opening the pack. |
| Bottle loops | 1 leather bundle | 1 | Three store slots for potions, flasks and fluid containers. |
| Bandolier | 2 leather bundles | 1 | Three store slots for rounds and arrows. |
| Bedroll lashing | 2 sticks | 1 | One store slot that takes only a [Bedroll](Bedroll.md). |
| Lamp bracket | 1 rod | 1 | One store slot that takes only a lantern; a lantern in it lights the area around you. |
| Crafting pad | 2 planks | 2 | Press **N** on firm ground to open a 3x3 crafting grid. Heavy. |
| Hard liner | 2 plates | 1 | The strap wears half as fast under an overload. |
| Fire patch | 1 plate | 1 | A burning wearer's cloth shell scorches and wears without one; with it, not. |
| Oiled coat | 1 hide | 1 | A cloth shell without one soaks in water or rain and weighs half as much again. |
| Warm liner | 3 carpets | 1 | Powder-snow chill leaves you faster. |
| Quick-release buckle | 1 nugget | 1 | Its key (unbound until you set it) drops the pack at once, contents and all. |

## Store slots

A pack with a holster, loops, lashing, bandolier or lamp bracket has one extra row below its slots: holster 1, loops 3, lashing 1, bandolier 3, lamp bracket 1. Each store slot sits on a tinted pad that shows what it takes, and refuses anything else. What is in the stores counts toward the load.
