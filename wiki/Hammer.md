# Hammer

A hammer you swing yourself, in a crafting grid, or seat in a machine as its striking head. It is a modular tool: a **head** on a **handle**. It's the first way to make plates, before you have any machines, and it teaches you the mod's main lesson the hard way.

## Head shapes

The head is a **head blank**, shaped as a standard head (four cells of stock), a **Broad-Faced Head** (six cells, heavy, so a harder blow) or a **Narrow-Faced Head** (two cells, concentrates the blow). The head can only carry as much of a swing as its material allows, so a broad head lets a soft material carry more of a blow.

## Crafting

```
S S S
S T S
  T
```

S is Cobblestone and T is a Stick. This gives the first hammer: a stone head on a wooden handle.

## Making a better one

Shape a **Head Blank** (ingots, blocks, gems, planks or cobblestone) at a bench ([Smithing Table](Smithing_Table.md) for metal, [Stonecutter](Stonecutter.md) for stone, [Woodworking Table](Woodworking_Table.md) for wood), then put it with a handle (stick, rod or bone) in a crafting grid. The head's material sets two things. **How well it carries a swing:** a stone or iron head lands every swing in full, but a light or soft head (bone, copper) lands only a share of it, and the hammer's tooltip shows the St you actually get. **How long it lasts:** about 260 swings for stone and 330 for iron. An iron head is **not** a harder swing: your arm sets the blow, and steel stays out of reach by hand.

## Using it

Put the hammer and **one** workpiece anywhere in a crafting grid. The output slot shows what that piece will be **after one more blow**. Take it, and that's one swing: the hammer loses a little durability and the piece takes one blow's worth of work.

Keep swinging and watch the result change. An ordinary swing lands **3 St**, which is 3 Fu on copper: a copper ingot becomes a **Copper Plate** on the fifth swing, **Copper Foil** on the eighth, then **Copper Scrap**.

## At the benches

The [Smithing Table](Smithing_Table.md) holds a hammer in a slot of its own and spends a point of it on every part you shape there. The [Anvil](Anvil.md) takes a hammer the same way, and forging is played there. The [Woodworking Table](Woodworking_Table.md) takes an axe in the same slot, and the [Stonecutter](Stonecutter.md) takes a [Sawblade](Sawblade.md).

The [Engineering Table](Engineering_Table.md) takes a hammer in its tool row. Its recipes for furnaces, presses and mills ask for one, and its Fit, Repair and Treatise tabs spend a point of it on every act.

## In machines

**Right-click** a [Mechanical Hammer](Mechanical_Hammer.md), a [Mechanical Press](Mechanical_Press.md), a [Pivot Mast](Pivot_Mast.md) (for its Cutting, Breaking and Striking tips) or the crush head of a [Dark Cell](Dark_Cell.md) with a hammer to seat it as the machine's head; the one it replaces comes back to your hand. **Sneak and right-click with an empty hand** to pull it. A machine with no hammer seated does nothing, wears nothing, and says *No head seated*.

The hammer sets two things in the machine. Its **blow figure**, the St on its tooltip, is the **ceiling** of the machine's blow; a heavier or harder head lifts it, a light or soft one lowers it. And it is the part that wears: it takes the force the machine cannot put into the work (cold or too-hard metal, scrap, gearing past the ceiling), loses condition, and bottoms out at half. It never breaks in a machine and never stops. The condition stays on the hammer, so a worn one moved to another machine is still worn. [Calipers](Instruments.md#calipers) read a seated hammer's condition while the machine is stopped.

## How hard you swing

Hold the hammer, **sneak and scroll** to pick a swing. The hammer's tooltip says which one it's set to.

| Swing | Strength | Good for |
| --- | --- | --- |
| **Tap** | 1 St | Finishing, and very soft metals like lead |
| **Blow** | 3 St | Copper, tin, zinc, gold (the default) |
| **Heavy blow** | 5 St | Brass and bronze |
| **Mighty blow** | 7 St | Iron. Only while you have the **Strength** effect; it drops back to a heavy blow when that wears off |

Harder isn't better. A heavy blow on copper is 5 Fu at a time, so it's easy to swing straight past the plate.

Every swing makes you a little hungrier, and harder swings more so: sixteen heavy blows cost about one hunger point.

Shift-clicking the output is the same as taking it: **one swing**, and the piece goes to your inventory. The hammer only works one piece at a time, so put it back to swing again. The risk is in not counting: nothing stops you swinging a plate into foil. See [Metalworking](Metalworking.md).

## What it can work

A swing only moves metal at least as soft as it is strong. An ordinary blow (3 St) shapes **copper (1), tin (0.6), zinc (2), gold (0.5) and lead (0.3)**, and can hammer **nuggets into bullets** (see [Ammunition](Ammunition.md)). A heavy blow adds **brass (4) and bronze (5)**; a mighty blow adds **iron (6)**.

Nothing you can swing touches **steel (15)**. The craft simply isn't offered. For that you need a [Mechanical Hammer](Mechanical_Hammer.md) with the right gearing.

Very soft metals move a lot with each blow: 3 St on lead (0.3) is 10 Fu a swing. Tap lead.

## Wear

In your hand the hammer wears out from **normal use**, like any hand tool. A stone head is good for about **260 swings**, around fifty copper plates; an iron one lasts longer. A worn-out hammer is *broken*: it lands nothing, but you can mend it on the Repair tab of the [Engineering Table](Engineering_Table.md#repair-tab) instead of throwing it away. A worn head also carries a swing a little less well, near the end.
