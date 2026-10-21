# Tool Table

The one bench every [tool, weapon, shield, bow and piece of armour](Tools_and_Weapons.md) is made at. It makes parts, puts them together, mends and reworks what is already made, and takes it apart. Guns are made elsewhere ([Gun Stand](Gun_Stand.md), [Assembly Bench](Assembly_Bench.md)).

## Crafting

```
P P P
I _ I
C C C
```

P is any Planks, I is an Iron Ingot and C is Cobblestone. Nothing in it needs the table itself.

## The hammer

The table holds a **[Hand Hammer](Hand_Hammer.md)** in its own slot, and **nearly everything you do here costs it a point of durability**: shaping a part, taking the result of an assembly, reworking, taking a tool apart. No hammer in the slot, no work. (Like any hand tool, the hammer wears out from correct use.)

Everything you put in stays in the table when you close it, and drops if you break it.

## Three tabs

### Parts

Pick a shape from the scrolling list (a long name slides to show its end when you point at it): every part there is (a pickaxe head, a sword blade, a chestplate's plate...), its **laid** variants (a claymore blade, a heavy pick head) and its **forged** variants (a rapier, a katana, a hatchet). Put stock in the cells: **as many pieces as the shape has cells**, all one material and one form (ingots, gems, planks, cobblestone, bone; the list of what a shape takes is on its [Tools and Weapons](Tools_and_Weapons.md#making-the-parts) card in JEI). A forged variant costs the same stock as the standard shape.

- A laid shape comes out as soon as the stock is in: take the part from the result slot.
- A **forged** shape is played, as a rhythm, inside the table: press **Forge** and see [Forging](#forging).

**Steel and other hot-worked metals have to be hot** to be shaped, judged on the coldest piece, and the part comes out as hot as the stock was. Metals without a hot-working window (iron, wood, stone) shape cold. The table has no fire of its own: heat is only what your stock carries, so heat it first and carry it over.

### Assemble

Six slots. Put parts and stock in any of them; **the parts decide what is made**, you never choose. When what is in the slots is exactly what some tool is made from, the tool appears in the result slot and the left side names it and which part filled which role. Take it and the parts are used up, at a point of the hammer.

- A shaped part (a head, a blade, a plate) is one item. Stock (handles, grip, string, a block) is as many as its size: two sticks are a handle two cells long.
- Every piece of a tool is an **item of its own**, so the same items always make the same tool, whichever slots they sit in. A **tip** (made from one gem, shard or nugget), a **binding** (one string, leather or wire), a sword's **guard** and **pommel**, a shield's **rim** and a crossbow's **prods** are shaped in the Parts tab like a head, then put in a slot. Only armour **studs** are raw gems, counted into the plate's slots.
- The slots are grouped and labelled on screen: **Head x1** (two slots), **Rod x3** (one slot, so its count is a handle's length) and **Misc x8** (three slots, a stack each). They hold different amounts, but any part may sit in any slot; hover an empty one to read what it holds.
- A **crossbow** also needs a **Crossbow Blueprint** in the table's plan slot ([Blueprint Table](Blueprint_Table.md)). The blueprint is kept.
- If the parts could become several tools, the left side lists them with the roles each wants, so you can see what is missing. The same parts never make two different tools.

### Rework

Put a finished tool in the tool slot, and:

- **Mend** it with the stuff its wearing part is made of (an iron pickaxe, iron ingots): each piece puts back its share. The result shows what you get.
- **Swap a part**: stock of a different material, as many pieces as the part has (or one shaped part), replaces it. A new wearing part is fresh and sheds any coat. Enchantments stay.
- **Add** a tip or a binding to a tool made without one: put in a Tip or Binding part.
- **Take apart**: the parts come back to you, shaped parts as parts, stock as stock. A worn tool's wearing part comes back short by what the wear spent, so taking a tool apart and rebuilding it is not a free repair. Enchantments, a coat and the edge are lost.

## Forging

A forged variant is played in the table's screen. **Tap** on the left lane (left click or Z) and **heavy** on the right (right click or X) as notes fall to the ring. A metronome clicks the tempo and counts four beats in before the first note. Each blow flashes Clean, Loose or Miss; a harder metal plays slower.

The stock cools while you play: notes struck while it is too cold (or still too hot) are dead and count against you. A sloppy run never fails the part, but it leaves **flaws**: each takes a share of the edge's toughness and durability (a ruined part starts at half). A clean run leaves none, though even a good player is sometimes a hair off. A perfect run rings a fanfare. The part goes to your inventory when it is done.

## Same rules as the Blueprint Table

What the [Blueprint Table](Blueprint_Table.md) lets you draw is what this table makes: both read one set of rules, so a drawn tool's parts, put in the slots, give that tool. `/feedback build` (for operators) hands out the same item.
