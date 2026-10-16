# Tools and Weapons

Pickaxes, axes, swords, bows, shields and the rest are no longer a fixed list of wooden, stone, iron, golden, diamond and netherite. Every one is **assembled from parts**, and what it does comes from what those parts are made of. The recipes for vanilla's tools are gone; put the parts together yourself, in a crafting table or an [Assembly Bench](Assembly_Bench.md).

Armour works the same way: see [Armour](Armour.md).

## Putting one together

Lay the parts out the way you always laid out a tool. **Anything made of a material that fits the place works**, so a head can be iron, bronze, gold, diamond, stone, a plank, a bone, even glass.

| Item | Layout | Parts |
| --- | --- | --- |
| **Pickaxe** | `HHH / _S_ / _S_` | three heads, two handles |
| **Axe** | `HH_ / HS_ / _S_` (either way round) | three heads, two handles |
| **Shovel** | `H / S / S` | a head, two handles |
| **Hoe** | `HH / _S / _S` | two heads, two handles |
| **Sword** | `B / B / S` | two blades, a grip |
| **Mace** | `M / S / S` | a [heavy core](Heavy_Core.md) (or a block of metal), two rods |
| **Trident** | `_T_ / TST / _S_` | three tines, two shafts |
| **Bow** | `_LW / L_W / _LW` | three limbs, three lengths of string |
| **Crossbow** | `PWP / SLS / _S_` | two prods, string, three stock, a lock |
| **Shield** | `FRF / FFF / _F_` | six faces, a rim |
| **Shears** | `_B / B_` | two blades |
| **Mattock** | `HH_ / HSH / _S_` | four heads, two handles: digs stone and wood, each at half speed |
| **Cleaver** | `HH / HH / _S` | four heads, a grip: a short, heavy axe that bites deeper than a full one |
| **Pike** | `H / S / S / S` | a head, three handles (needs the 4×4 bench): reaches further |
| **Maul** | `HHH / HHH / _S_ / _S_` | six heads, two handles (bench): splits wood and staggers what it hits |
| **Sickle** | `HH_ / __H / __S` | cutting a crop or a leaf takes its neighbours too |

**Heads and blades** are ingots, gems, planks, cobblestone or bone. **Handles** are sticks, rods or bone. All the cells of one part must be the same material.

You can swap in a few extras:

- A **binding** (leather, string, wire) beside the handle makes a pickaxe, axe, shovel or hoe: `HHH / BS_ / _S_`.
- A sword takes a **guard** and a **pommel**: `B / B / G / S` and `B_ / B_ / G_ / SP`. The pommel is a counterweight.
- Armour and shields can take a **grip** or **lining**.

## What the parts do

You won't see a stat sheet. Hold the tool and read the words on its tooltip: *head-heavy*, *light in the hand*, *heavy*, *keen*, *blunt*, *brittle*, *soft*, *loose-jointed*, *worn*. A tool is a physical object, and these are the things you'd feel picking it up.

- **A heavier head hits harder and swings slower.** Gold is nearly three times iron's weight; a gold pickaxe is slow and hits hard, but gold is soft.
- **A longer handle reaches further** and puts more strain on the joint.
- **Hardness decides what it can dig.** An edge needs to be at least **3** hardness for iron ore and the stone class, **6** for gold, diamond and redstone ore, and **40** for obsidian and ancient debris. So copper and gold can't mine iron ore, iron can't mine obsidian, and diamond and netherite can. (Stone has a very high number, so a stone pickaxe *can* mine diamond ore. It chips doing it.)
- **How long it lasts** comes from how hard and strong the edge is. Iron lasts about as long as it used to; diamond lasts far longer; gold barely lasts.
- **Enchanting** works as normal. How good a tool is at taking enchantments depends on its main part: gold is best, stone is worst.

### Wearing it out the wrong way

A tool wears with use, like any hand tool, but misuse costs extra and tells you which kind:

- **An edge on something much harder than it can take chips.** A diamond pickaxe on obsidian wears quickly, because diamond is hard but brittle. A **keen** edge chips sooner; a **blunt** one is tougher.
- **A head too heavy for its handle works the joint loose.** Netherite or gold on a wooden handle gets *loose-jointed* and loses a bit more each swing. A stronger handle (bone, a rod) or a binding fixes it.
- **A bow's string is the limit.** Stiff limbs on a weak string shoot faster and draw slower, until the string is the weak point. Use wire for a string.

## Edge

Hold a tool with an edge and **sneak-scroll** to grind it **keen**, **standard** or **blunt**. Keen bites harder and chips sooner.

## When it breaks, mending, and swapping parts

A tool doesn't vanish when it's spent. It stops with one point left and acts like an empty hand until you repair it. It also gets visibly worse on the way down: *Worn*, *Battered*, *Nearly spent*.

**Repairing:** put the tool in the crafting grid with the material its wearing part is made of (an iron pickaxe with iron ingots). Each ingot puts back its share.

**Swapping a part:** put the tool in the grid with a *different* material, in as many cells as the part has (three bronze ingots over an iron pickaxe replace its head). A new wearing part is fresh. Enchantments stay.

## Old vanilla tools

Vanilla tools, weapons and armour that turn up (in chests, on mobs, from villagers or in your pockets) are converted on the spot into the modular item they would have been, with the same name, enchantments and wear. An iron pickaxe is an iron head on a wooden handle.

## Measuring

The tooltip gives words for free. For numbers, carry a [Balance Scale](Balance_Scale.md) (mass) or a [Scratch File Set](Scratch_Files.md) (hardness).
