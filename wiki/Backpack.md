# Backpack

A pack worn in the Curios **back** slot. It holds 9, 18 or 27 items, and the strap decides how much weight you can carry in it before it slows you.

## Crafting

Made at the Tool Table from two parts, as for any [modular tool](Tools_and_Weapons.md):

- **Shell** (Backpack Shell): the body, shaped from leather, carpet or plate stock. Its shape fixes the slots: a **pouch** (one cell) holds 9, a **satchel** (three cells, the standard shape) holds 18, a **pack** (nine cells) holds 27. The shell's material only decides how heavy it is.
- **Strap**: leather, string or wire. It sets the carry rating, and it is the part that wears.

The [Blueprint Table](Blueprint_Table.md) shows the slots and the carry rating of a drawing before you make it.

## Using it

Wear it in the back slot and press **B** (Open Backpack) to open its contents as a chest screen. A pack cannot hold another pack.

## Carry rating

The rating is the strap's strength times 8, and it falls as the strap wears. The load is the mass in Mu of the pack plus everything in it; what else you carry does not count.

What an item weighs:

- items tagged as light or heavy (the tags `feedback:pack_mass/light` and `feedback:pack_mass/heavy` can be filled by a datapack) weigh a fixed small or large amount;
- otherwise a material item weighs by its density and size, so an iron ingot is a little over 1 Mu;
- anything else weighs a flat middling amount.

## Overloading

With the load over the rating you are **30% slower** and **cannot sprint**, and the strap wears one point every second. At its last point the strap lets go: the pack drops off you with everything in it. Rework the strap to mend it (see [mending](Tools_and_Weapons.md#when-it-breaks-mending-and-swapping-parts)).
