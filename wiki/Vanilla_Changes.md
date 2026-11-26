# Vanilla changes

Vanilla blocks and fluids that Feedback changes in a way that is not a page of its own, one section each: what vanilla does, what Feedback does, and where the full process lives. Furnaces, the Crafting Table, the Anvil and the other changed blocks have their own pages, listed on [Home](Home.md).

## Ancient Debris

**What vanilla does:** ancient debris is smelted or blasted into netherite scrap. Four scrap and four gold ingots make a netherite ingot, and a netherite ingot, a template and diamond gear in a smithing table make netherite gear.

**What Feedback does:** the block is mined as before (a pick of hardness 40 digs it) and found where vanilla puts it ([Ore Deposits](Ore_Deposits.md#what-did-not-change)). Furnaces and blast furnaces do not turn it into scrap, the scrap-and-gold recipe does not exist, and the [Smithing Table](Smithing_Table.md) does not upgrade gear to netherite. Debris and scrap are metal to melt: at 2600 Tu they become Molten Cast Netherite, which is cast and soul-quenched into a netherite ingot. See [Netherite](Netherite.md).

## Beds

**What vanilla does:** a bed is three wool of one color over three planks in a crafting grid. A bed can be recolored by crafting it with a dye. A placed bed has two blocks, sleeps one player, skips the night, sets your respawn point, and explodes if you try to sleep in the Nether or the End. Villagers claim a bed as their home and sleep in it.

**What Feedback does:** the sixteen colored beds cannot be crafted or dyed, and the vanilla beds are hidden from JEI. You build a [Bed](Bed.md) from a **Bed Mattress** part and a **Bed Frame** part, shaped at the benches and joined in a crafting grid (see [Bed](Bed.md#crafting)). A placed Feedback bed does everything a vanilla bed does:

- It sleeps you, skips the night and sets your respawn point.
- It explodes if you sleep in it in the Nether or the End.
- Villagers sleep in it and claim it as their home. A villager claims the head half; the foot half is not claimed.

Any vanilla bed that turns up in a world, village or structure, or that is placed by a command, becomes a Feedback bed a moment later. Both halves change together and keep their facing. Each bed is drawn at random, and the same bed always draws the same way:

- **Frame:** mostly wood (oak, spruce, birch, dark oak, jungle or acacia), sometimes bronze or wrought iron. Most are bedsteads; some are cots.
- **Mattress:** a flat mattress with a cover of wool, coated cloth or leather, over padding of wool, feather down, short grass, tall grass or rabbit hide.
- **Color:** the cover keeps the color the vanilla bed had.

A placed bed cannot be dyed. Take it up and rework its parts to change the cover. Breaking a found bed returns it as one item with its parts, as with any placed bed.

## Blaze powder and blaze rods

**What vanilla does:** a blaze drops a blaze rod. One rod crafts into two blaze powder, and a rod or powder fuels furnaces and brewing stands.

**What Feedback does:** a blaze drops a **charged** Blaze Rod, a hot item that cools very slowly, over about 20 minutes. When it has cooled it becomes a **Spent Blaze Rod**. A rod is not a fuel and the crafting recipe for blaze powder does not exist. Put a [Mortar](Brewing_Stand.md#mortar) and one rod in a crafting grid to grind it:

- A hot rod gives 2 Blaze Powder, which is the fuel ([Firebox](Firebox.md#fuels)).
- A cold or spent rod gives 2 Inert Blaze Dust, which does not burn.

The mortar is kept. A spent rod heated anywhere between 700 Tu and 1100 Tu (any heat source) becomes a charged rod again. See [Heat](Heat.md).

## Comparator

**What vanilla does:** a comparator reads how full the container behind it is and puts out a redstone signal of that strength. It also compares or subtracts two signals and delays them.

**What Feedback does:** it is replaced by the [Fill Gauge](Sensors.md), a sensor that reads how full the inventory or tank on the face it looks at is, in eighths, for a [Controller](Controller.md). The Comparator has no crafting recipe and is hidden from JEI. A Comparator already in a world, a structure or placed by a player turns into a Fill Gauge looking at the container behind where the Comparator sat. Subtract mode and the signal delay are not carried over.

## End Stone

**What vanilla does:** a pale, hard building block found on the End island.

**What Feedback does:** it is a source of hydrogen and helium and a low-grade tungsten ore.

- Heated in a [Reactor](Reactor.md#the-reactions) it releases hydrogen and [helium](Chemicals.md#helium) and leaves Porous Aggregate.
- Hammered to Crushed End Stone and taken up the [ore ladder](Ore_Processing.md#leaching-and-winning), it leaches to sodium tungstate and gives tungsten.

## Iron Ore

**What vanilla does:** Iron Ore and Deepslate Iron Ore drop Raw Iron. Raw Iron, or the ore block itself, smelts to an Iron Ingot in a furnace or blast furnace.

**What Feedback does:** the ores are found and mined as in vanilla and drop Raw Iron ([Ore Deposits](Ore_Deposits.md#mining-them)). Raw Iron smelts to an Iron Ingot only with charcoal in a [crucible](Crucible.md) or [furnace](Furnaces.md) held between 1420 and 1650 Tu. The Iron Ingot is cast iron, the start of [steel](Guide_Making_Steel.md). Held lower in the bloom window, the ore becomes an Iron Bloom that is hammered into a Wrought Iron Ingot ([Guide: Making Wrought Iron](Guide_Making_Wrought_Iron.md)). Bare, it melts to molten cast iron. Raw iron also takes the [ore processing](Ore_Processing.md) stages like any other ore. Feedback's own iron minerals (hematite, magnetite, limonite) follow the same route ([Materials](Materials.md#ore-minerals)).

## Iron items

**What vanilla does:** the Iron Ingot, Nugget and Block, and everything made of iron, are one plain iron.

**What Feedback does:** the Iron Ingot, Nugget and Block are **cast iron**: hard and brittle, easy to smelt from ore, and the feed for [steel](Guide_Making_Steel.md). Iron tools, armor and horse armor, Iron Bars, Door and Trapdoor, chain, anvil, bucket, shears, cauldron, hopper, heavy weighted pressure plate and chainmail are **wrought iron**, the soft tough iron that has its own Wrought Iron Ingot, Nugget and Block ([Materials](Materials.md#the-metals)).

## Redstone Lamp

**What vanilla does:** the Redstone Lamp lights while it is powered by a redstone signal and goes dark when the signal stops. It is crafted from glowstone and redstone dust.

**What Feedback does:** it is replaced by the [Glowstone Lamp](Lamp.md). It has no crafting recipe, and Redstone Lamps already in a world turn into Glowstone Lamps. The Glowstone Lamp is a load on a power line, not a block watching a redstone signal: it takes 0.5 A on an MV line, glows in four steps by how well the line feeds it, dims when starved and burns out if given twice its rated packet. See [Electricity](Electricity.md).

## Shulker

**What vanilla does:** a shulker drops a shell, which crafts into a shulker box.

**What Feedback does:** the shell is a ceramic, light, tough and heat-resistant, so it is a material like any other. Five shells and three phantom membranes craft an [Elytra](Elytra.md). A shell can also be ground flat on the [Grinding Wheel](Grinding_Wheel.md) into a Shulker Shell Plate, which any part that takes plate stock accepts: light, tough and heat resistant. Shulker boxes stay out of the [Ender Port](Ender_Port.md#using-it) system: a shulker box or Shulker Shell beside a port blocks pairing, and a shulker box never crosses a port.

## Water

**What vanilla does:** water extinguishes fire, carries items and mobs along, lets you swim, and fills cauldrons and farmland.

**What Feedback does:** all of that works. On top of it, water cools a workpiece by hand. Right-click a water source with a hot piece in hand and it snaps to room temperature; drop a hot piece in water and it is cooled instantly. For hot steel this is [quenching](Guide_Heat_Treatment.md#hardening-quenching). Room temperature depends on the dimension ([Heat](Heat.md#room-temperature)).
