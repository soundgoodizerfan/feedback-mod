# Brewing Stand

## What vanilla did

The Brewing Stand takes a bottle of water in each of three slots, an ingredient above them and blaze powder as fuel, and after a fixed time turns the bottles into the potion that ingredient's recipe names. Every potion is a fixed recipe: nether wart for an awkward potion, then one more ingredient for the effect. Redstone lengthens it and glowstone strengthens it.

## What Feedback does

Feedback **replaces** the Brewing Stand. A vanilla one found in the world, in a village or placed by a command turns into this one, and the vanilla recipe is gone, as are the vanilla potion items and tipped arrows.

The stand is a pot of water over a heat source: a [Firebox](Firebox.md), a [Campfire](Campfire.md) or lava below it. The water pins at boiling, so the working range is room temperature to 100 Tu.

### Making a potion

Right-clicking the stand with anything, or nothing, opens its screen: a parchment map in the middle, a status panel on the left, and on the right a water slot, six ingredient slots, three bottle slots and the Cook button. Your inventory is below. Everything moves in and out by the screen's slots; clicking the block with an item does nothing else.

1. **Fill it.** A water bucket in the water slot fills the pot with three bottles' worth, 250 mB a bottle, and leaves the empty bucket. Nothing dilutes.
2. **Add plants.** Put up to six in the ingredient slots. Only plants go in: a flower, herb, fern, berry, fruit, vegetable, root, mushroom, kelp or nether wart. Food does not steer: a steak, a loaf, salt or honey is refused, and so is anything cooked. Each plant adds a **curve** to the end of the path, in the order you added them, and you can add more at any time, even while stirring. A plant carries a **heading** (the way its curve goes overall), a **reach** (how far it takes the flask from end to end) and a **shape** (how it gets there: straight, bowed, waving, zigzag, looping, spiralling or hooked). What a plant does is read from the material it is made of, never from what the item is, so a modded flower with the same properties behaves the same. Each also belongs to a **family** of compounds, which colours a dud draught and sets its taste. The cut a plant carries from the [Prep Table](Prep_Table.md) is its grind: a whole one gives up its reach slowly, a sliced one faster, a minced one fastest, and the cut changes how far the curve goes, never how it twists. A rotten plant reaches less and drifts the brew toward a hazard. Hovering a plant, in your inventory, in a slot or in the JEI item list, draws a ghost curve on the map from the end of the path to where adding it would take the flask, with the same cut and the pot's heat now. The curve is dashed by grind: a whole piece is mostly gap, a sliced one half and half, a minced one solid. The curves and what carries each are listed in JEI under Potion Shapes, one card per shape with a dot tracing it.
3. **Stir.** Moving the mouse in circles over the map walks the marker along the curve. The white flask starts at the centre. Each plant is a curve from where the marker stands: the cut and the pot's heat at the moment it goes in set its reach, and from then the curve is fixed and never grows. Below about 30 Tu nothing comes out at all. The flask moves only when you stir, pour, hit the wall or ride a vortex, and when stirring it follows the drawn curve exactly, by the distance it walks, so a zigzag takes more stirring than a straight line of the same reach and a spiral the most. Zones and vortices are met along the curve, so it can graze one its two ends clear and swing round one a straight line would cross. The curve the flask has walked stays behind it. You can stop short of the end; a plant you do not reach is spent and adds nothing. When the flask has walked a whole curve, that plant is in the water and its slot empties, free for another. Only the plant being stirred in right now is dimmed and cannot be taken back; any other can be taken out, and its curve leaves the path.
4. **Cook.** The Cook button ends the brew where the flask is. The pot must be at least 70 Tu. A flask inside an effect node makes that potion, and a flask anywhere else makes a **dud draught**. Either way the water and every ingredient are spent. Tip out empties the pot and spends what is in it.
5. **Take the bottles.** The finished bottles appear in the three bottle slots.

### The map

The map is a plane. Plants belong to families of compounds, and a plant's heading, reach and shape are its own: a bitter alkaloid herb may head north-west on a wave while a resin flower heads north-east on a hook, and the two can cancel or add. Reaches differ, so the near nodes take three slots and the far ones five or six. Some mineral-bearing plants steady a near miss toward the nearest node along one axis, strength or duration, and never rescue a wild one.

Landing inside a node makes its effect: the flask's centre must be inside the node's ring. The further you are from the node's centre **across** the map, the weaker the potion; the further **up or down**, the shorter it lasts. A node's strongest potions sit past rarer or opposed compounds.

**Fog.** The map is covered in fog. The flask uncovers a small area as it passes, and that stays uncovered for you. Hazards, vortices and nodes you have not made show only where the fog is gone. A faint dashed line runs from the centre to every node, through the fog, so a hidden one can be found by following its line. Each line ends in the node's icon: a bottle silhouette with a question mark for a node you have not made (the same shape as the flask, so you can match them), the effect's own icon for one you have. A few fixed outcrop marks show through the fog to steer by. The map is yours: another player uncovers their own.

**The family key.** A plant's family colours the dud draught and sets its taste; the map carries no compass, and where a plant goes is shown by its curve on the map and its heading in JEI.

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

A bottle of potion carries the effect, its level and length from where the brew landed, and a **potency** that fades. A bottle loses potency over a few days, faster in the warm and slower in the cold, and a faded potion is weaker, not shorter. A bottle that has faded away has no effect. Drinking applies the effect; every bottle can be drunk. The fire potion sets the drinker alight for the bottle's length times one plus its level. The explosion potion releases its heat at the drinker: two and a half times the thrown damage at the centre (10 at level I, 20 at II, 30 at III, against 20 health) and ten seconds alight. Do not drink it unprotected: fire resistance and armour answer the heat, and no blocks are blown out.  A dud draught gives a short nausea, and its colour leans toward the class of the nearest node you have not made (plain water-blue when there is none). Each family has a colour.

### Throwing

Sneaking and using a bottle throws it. It breaks where it lands and its potion reaches every creature within about two and a half blocks (the spread of a 250 mB bottle), the nearer the longer: the level stays, the length falls off with distance, and the potency left in the bottle applies as when drunk. A dud draught cannot be thrown. See [Thrown and imbued potions](Thrown_and_Imbued_Potions.md).

### Lacing food

Any potion can be poured into food. Pour a bottle into a [stockpot](Stockpot.md) with something in it, or sneak and use a ladled serving or a dish with the bottle in your other hand. The bottle is spent and the food carries the potion's effect at its level, hidden. A pot shares the bottle over all its servings, so each carries a shorter dose and a quieter taste; a dud draught will not mix in, and food takes one potion.

Eating a laced food gives the effect through the same path as a meal's own buffs: fat and fibre delay it. Drinking a potion plainly ignores taste, but in food the taste can give it away. Every compound class has a taste, and the potion's taste is the mean of what went in. If the potion's taste is far from the food's own and loud enough, you notice something wrong with the food as you eat; a close or weak taste goes unnoticed. A fine palate lowers both thresholds. A nibble (sneak and tap the food) reports a faint aftertaste of the compound class the taste leans on when it stands out the same way.

### Potion effects

Each effect sits at a fixed place on the map, given as a direction from the centre.

| Potion | Direction | Effect |
|---|---|---|
| Adrenaline | north-north-west | Halves the stamina every cast technique costs; the body does not heal while it lasts. |
| Antivenom | south-west | Only two fifths of poison damage gets through; stamina refills at seven tenths of its rate. |
| Assay | west | Every readout resolves at the exact figure for about a minute; stamina refills a fifth slower. |
| Explosion | north-north-west | A fast release of heat: thrown, it damages and ignites creatures in reach; drunk, the drinker takes it whole. No blocks are blown out. |
| Featherstep | west-north-west | Slow falling that also holds you up on powder snow; sneak to sink. Pressure plates still trigger. |
| Fine palate | west | Laced food is noticed at a lower taste threshold; a meal gives a quarter less saturation. |
| Fire | east-south-east | Sets creatures alight when thrown, and the drinker when drunk. |
| Frost | west-south-west | Slowness, drunk or thrown. |
| Heat ward | south-south-east | Fire resistance; frost builds half again as fast in powder snow and freezing hurts half again as much. |
| Night vision | north | Night Vision for about five minutes. |
| Poison | south-west | Poison up to level II, drunk or thrown; a tool soaked in it carries it into its blows. |
| Quench | north-west | Hot machines within five blocks and hot workpieces in your pack lose heat four times as fast; frost builds a third faster in powder snow. See [Heat](Heat.md). |
| Quickdraw | north-north-west | Gun reloads take four fifths of the time at level I and three fifths at level II; mining fatigue follows when it ends. |
| Radiation ward | south-south-west | A little over half of the [radiation](Radiation.md) dose gets through; every heal lands at three fifths. |
| Regeneration | east-north-east | Regeneration up to level II for about forty-five seconds. |
| Resistance | south-east | Resistance up to level II for about three minutes. |
| Second wind | north-north-east | Stamina refills faster and after a shorter pause; when it ends, stamina does not refill at all for a few seconds. |
| Smelter's blood | south | A bare hand holds a piece 150 Tu hotter before it hurts; swinging and digging run 15% slower. |
| Speed | north-east | Speed up to level III for about three minutes. |
| Static ground | south-east | Only two fifths of electric shock damage gets through; metal held in either hand warms steadily. |
| Steady hand | south | Halves a gun's spread and camera kick. |
| Strength | north | Strength up to level II for about ninety seconds. |
