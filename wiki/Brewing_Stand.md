# Brewing Stand

## What vanilla did

The Brewing Stand takes a bottle of water in each of three slots, an ingredient above them and blaze powder as fuel, and after a fixed time turns the bottles into the potion that ingredient's recipe names. Every potion is a fixed recipe: nether wart for an awkward potion, then one more ingredient for the effect. Redstone lengthens it and glowstone strengthens it.

## What Feedback does

Feedback **replaces** the Brewing Stand. A vanilla one found in the world, in a village or placed by a command turns into this one, and the vanilla recipe is gone, as are the vanilla potion items and tipped arrows.

The stand is a pot of water over a heat source: a [Firebox](Firebox.md), a [Campfire](Campfire.md) or lava below it. The water pins at boiling, so the working range is room temperature to 100 Tu.

### Making a potion

Right-clicking the stand with anything, or nothing, opens its screen: a parchment map in the middle, a status panel on the left, and on the right a water slot, six ingredient slots, three bottle slots and the Cook button. Your inventory is below. Everything moves in and out by the screen's slots; clicking the block with an item does nothing else.

1. **Fill it.** A water bucket in the water slot fills the pot with three bottles' worth, 250 mB a bottle, and leaves the empty bucket. Nothing dilutes.
2. **Add ingredients.** Put up to six in the ingredient slots. Each one you add puts its line on the end of the path, in the order you added them, and you can add more at any time, even while stirring. An ingredient steers the brew by the compounds it carries, never by what it is: anything with the right compounds works, and a modded plant with the same compounds behaves the same. The cut an ingredient carries from the [Prep Table](Prep_Table.md) is its grind: a whole one gives up its compounds slowly, a sliced one faster, a minced one fastest. A rotten plant gives up less and pulls the brew toward a hazard. Hovering an ingredient draws a ghost line on the map from the end of the path to where adding it would take the flask, with the same cut and the pot's heat now. The line is dashed by grind: a whole piece is mostly gap, a sliced one half and half, a minced one solid.
3. **Stir.** Moving the mouse in circles over the map walks the marker along the line. The white flask starts at the centre. Each ingredient is a line from where the marker stands, as long as what comes out of it: the cut and the pot's heat at the moment it goes in set the length, and from then the line is fixed and never grows. Below about 30 Tu nothing comes out at all. The flask moves only when you stir, pour, hit the wall or ride a vortex, and when stirring it follows the drawn line exactly. The line the flask has walked stays behind it. You can stop short of the end; an ingredient you do not reach is spent and adds nothing. When the flask has walked a whole line, that ingredient is in the water and its slot empties, free for another. Only the ingredient being stirred in right now is dimmed and cannot be taken back; any other can be taken out, and its line leaves the path.
4. **Cook.** The Cook button ends the brew where the flask is. The pot must be at least 70 Tu. A flask inside an effect node makes that potion, and a flask anywhere else makes a **dud draught**. Either way the water and every ingredient are spent. Tip out empties the pot and spends what is in it.
5. **Take the bottles.** The finished bottles appear in the three bottle slots.

### The map

The map is a plane. Each compound pulls in its own direction, and compounds come in opposing pairs: sugar against bitter alkaloids, stimulants against sedatives, minerals against volatile oils, toxins against resins. Vitamins and proteins pull a little and serve for fine correction. Conductive and luminous minerals (redstone and glowstone) tighten a near miss toward the nearest node along one axis each and never rescue a wild one.

Landing inside a node makes its effect: the flask's centre must be inside the node's ring. The further you are from the node's centre **across** the map, the weaker the potion; the further **up or down**, the shorter it lasts. A node's strongest potions sit past rarer or opposed compounds.

**Fog.** The map is covered in fog. The flask uncovers a small area as it passes, and that stays uncovered for you. Hazards, vortices and nodes you have not made show only where the fog is gone. A faint dashed line runs from the centre to every node, through the fog, so a hidden one can be found by following its line. Each line ends in the node's icon: a bottle silhouette with a question mark for a node you have not made (the same shape as the flask, so you can match them), the effect's own icon for one you have. A few fixed outcrop marks show through the fog to steer by. The map is yours: another player uncovers their own.

**The compass.** The frame round the map is a trim band with a dot for every class of compound, placed where a line from the middle of the map at that class's angle meets the map's edge, so you can see where a food will take you before it goes in. Each dot is the class's colour and has a short name beside it, cut to fit the band. Opposing classes sit opposite one another. The trim does not move when you zoom or pan, and nothing of it is drawn on the map. Hovering an ingredient enlarges the dots of the classes it carries, more for the stronger; hovering a dot or its name lists the class in full and the items in your inventory and the stand that carry it. The compass is never fogged.

**The flask has a size.** It touches a zone or a vortex when its edge does, not only its centre, and it is drawn that size on the map, growing and shrinking as you zoom. A node needs the flask's centre inside its ring.

**The wall.** The edge of the map pushes the flask back and costs nothing, so a flask driven into it slides along it.

**View.** The mouse wheel over the map zooms about the cursor, and dragging with the right or middle button pans it, never past the wall. R puts the view back on the whole map. Stirring is the same at any zoom, since it is read from how fast the cursor circles the middle of the view.

**Stir pace.** How fast you stir matters near a hazard. A slow stir moves the flask in fine steps and spends little path in a zone; a fast stir covers ground but loses more potency for each bit of path it crosses there.

**Pour.** The Pour button ladles water over the brew and sends the flask a fixed step straight toward the centre, at the price of a slice of the brew's potency. It is the only way to undo an overshoot.

### Hazards

- **Precipitate zones** (patches of fallen solids) pull the compounds out of the water. While the flask is inside, the brew loses potency; outside, it recovers. Drained to nothing, the brew fails.
- **Scorch zones** (charred patches) are harmless while the pot is cool and drain potency only above their temperature.
- **Vortices** (spirals) spin once the pot is hot. A spinning vortex turns a flask inside it, and a flask that reaches its centre jumps to a fixed place, which shows on your map from then on.
- **Heat only makes scorch zones bite.** An over-hot pot moves nothing; a hot pot also spins the vortices.

### A failed brew teaches

A dud leaves a **residue mark** on your map where the brew ended (or where a zone drained it away). Hovering the mark says what the nearest node you have not made *smells of*: the class of compound that node leans on, such as resin. It never names the effect. The dud draught itself tastes of that class too. The mark stays until you make that node's potion.

### Potions

A bottle of potion carries the effect, its level and length from where the brew landed, and a **potency** that fades. A bottle loses potency over a few days, faster in the warm and slower in the cold, and a faded potion is weaker, not shorter. A bottle that has faded away has no effect. Drinking applies the effect. A dud draught gives a short nausea, and its colour leans toward the class of the nearest node you have not made (plain water-blue when there is none). Each class has a colour, shown on its compass dot.

### Throwing

Sneaking and using a bottle throws it. It breaks where it lands and its potion reaches every creature within about two and a half blocks (the spread of a 250 mB bottle), the nearer the longer: the level stays, the length falls off with distance, and the potency left in the bottle applies as when drunk. A dud draught cannot be thrown. See [Thrown and imbued potions](Thrown_and_Imbued_Potions.md).

### Lacing food

A drinkable potion can be poured into food. Pour a bottle into a [stockpot](Stockpot.md) with something in it, or sneak and use a ladled serving or a dish with the bottle in your other hand. The bottle is spent and the food carries the potion's effect at its level, hidden. A pot shares the bottle over all its servings, so each carries a shorter dose and a quieter taste; a potion thrown-only (fire, explosion) and a dud draught will not mix in, and food takes one potion.

Eating a laced food gives the effect through the same path as a meal's own buffs: fat and fibre delay it. Drinking a potion plainly ignores taste, but in food the taste can give it away. Every compound class has a taste, and the potion's taste is the mean of what went in. If the potion's taste is far from the food's own and loud enough, you notice something wrong with the food as you eat; a close or weak taste goes unnoticed. A nibble (sneak and tap the food) reports a faint aftertaste of the compound class the taste leans on when it stands out the same way.

### More nodes

- **Explosion** (up past the stimulants, at about the top right of the map) is a node you cannot drink: the bottle only throws. It is a fast release of heat: creatures in reach take heat damage that falls off with distance and are set alight, and flammable blocks beside the burst catch. No blocks are blown out. Fire resistance answers the heat.
- **Fire** (far east, past sugar and mineral) sets creatures alight when thrown and cannot be drunk.
- **Frost** (far west, past the alkaloids and sedatives) is slowness; it can be drunk or thrown.
