# Bed

A bed you build from a mattress and, if you want one, a frame. Built with a frame it is placed in the world as a two-block bed that sets your respawn point; built with no frame it is a **bedroll**, used from the hand on firm ground at night, or in a storm, and light enough to lash on a pack.

## Crafting

A bed is made from up to two parts ([Composed Parts](Composed_Parts.md)). Shape each at a bench for its stock, then put them together in a crafting grid, the frame above the mattress.

The **Bed Mattress** is the part you lie on. It comes in two shapes:

- **Bedroll**: two cells, rolled, light and a little less comfortable.
- **Mattress**: three cells, laid flat, thicker, so softer and warmer, and heavier to carry.

Its stock is leather or carpet, the **cover**. It has three materials:

- **Cover**: the leather or carpet you lie on. It is the part's main stock.
- **Padding**: stuffing chosen in its own stock slot, any fibrous material: wool, leather, rabbit hide or feather down. The softer it is, the better the sleep. [Feather down](Materials.md#feather-down-and-rabbit-hide) is the lightest and warmest; rabbit hide is warm and tough.
- **Shell**: a leather case, fixed; it takes a share of the same stock.

The **Bed Frame** lifts the mattress off the cold ground. Its stock is planks, rods or ingots. It comes in two shapes:

- **Bedstead**: four cells, a tall frame that lifts the sleeper well off the ground.
- **Cot**: three cells, a low light frame that lifts the sleeper a little.

A frame adds comfort and warmth, and weight. The [Blueprint Table](Blueprint_Table.md) shows the **comfort** and **warmth** of a drawing before you make it.

## Using it

**With a frame**, right-click a block to place the bed: it takes two blocks, head and foot, like a vanilla bed. Right-click it at night to sleep, or any time to set your respawn point. It skips the night as a vanilla bed does. You wake healed by the bed's comfort (softer padding and cover, a thicker mattress and a lifted frame give more), scaled by its warmth in cold places. Each night slept wears it a little, and a worn-through bed refuses to be slept in until you take it up and rework it. Breaking either half returns the whole bed as one item, with its parts and materials as they were.

**With no frame**, a bedroll cannot be placed. Use it from your hand on firm ground at night or in a storm. It needs no hostile creature within 8 blocks; otherwise it refuses. A night's sleep:

- skips to dawn,
- gives back a share of your health equal to the bed's comfort,
- resets the insomnia count that brings phantoms.

A bedroll never sets your spawn point. Each use wears it a little.

Messages tell you why it will not work: only on firm ground at night or in a storm, or monsters nearby.

## Warmth

Warmth is how little heat the padding lets through, thicker and lifted off the ground being better. A padding that conducts heat poorly keeps you warm.

- In a biome cold enough for snow, the health a night gives back is scaled by the bed's warmth.
- A bed with a warmth of **60%** or more also clears the chill of powder snow when you wake.

## Attunements

The mattress and the frame take [attunements](Enchanting_Table.md#on-a-bed). A night's rest can heal more or less, feed or starve you, repair what you wear, give night vision, speed or resistance on waking, put out fire on you, charge or drain the Charge tools you carry, or charge a battery beside the bed; a placed bed's frame can also act on animals, monsters and crops around it, and on a placed bed the frame and mattress can slow, repel, draw in or cut the hostile creatures beside it, soften a fall onto the bed, make rest depend on the allies near, spread healing out over a few seconds, hide you in the dark after waking, or return the bed to your inventory when you break it (or hold it fast against breaking).

## On a pack

Only a bed built with no frame goes on a pack's [bedroll lashing](Backpack.md#fittings). You can use it without taking it out: press the **Bed Down in Pack Bedroll** key (comma unless you set another).
