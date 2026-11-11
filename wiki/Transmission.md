# Transmission

A gearbox built from parts. It couples the shaft behind it to the shaft in front at the ratio of the gear it is in.

## Designing one

Draw the gearbox at the [Blueprint Table](Blueprint_Table.md) (pick *Transmission* in the type row) and print it on paper. Drawing it takes Mechanical Treatise II ([Treatise](Treatise.md)). The gear set's shape is how many forward gears it has (2 to 6), and its panel has a stepper for each gear's ratio, in steps of 0.05, from 0.5 to 5.0. A ratio is input turns per output turn: 3.5 is a low gear, 0.8 an overdrive.

| Part | Choices | What it sets |
| --- | --- | --- |
| **Gear Set** | two to six speeds | The gears. Their metal sets how fast a shift at speed grinds them: softer metals wear faster. |
| **Synchro Ring** | dog clutch, synchromesh | A dog clutch is plain. A synchromesh matches speeds before the gear meshes, so a shift at speed wears the box about two fifths less, for a tenth more weight. |
| **Gearbox Case** | dry, splash, pressure oiled | The oiling. Dry is the lightest and wears most; splash wears a quarter less; pressure oiling wears half as much, in the heaviest case. |

At the [Smithing Table](Smithing_Table.md), put the **Transmission Blueprint** in the blueprint slot and the three parts in the slots to get the block. The blueprint is kept.

## Using it

Place it with its back to the input shaft and its front to the output. **Right click** shifts up through neutral, the gears and reverse; **sneak right click** shifts down. Reverse is first gear's ratio, backwards. In neutral nothing is coupled. A program or the Driver's Seat can shift it too, through its gear input.

## The clutch

A transmission also has a **clutch** input, channel 1 (the shifter is channel 0). Pressed past halfway it parts the link, so the input shaft coasts free of the output, and a shift made while it is pressed does not grind. Link a *Switch* card to the transmission and send it a key to use it; let it go and the gears couple again at once.

## Wear

Shifting while the input shaft is turning fast grinds the teeth, in proportion to the speed, softer metals worse, and less with a synchromesh or a better-oiled case. A badly worn transmission jumps out of gear under load, more often the more worn it is. Stopping, going through neutral or pressing the clutch costs nothing, and running it never wears it.
