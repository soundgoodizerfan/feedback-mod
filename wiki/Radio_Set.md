# Radio Set

A hand-held radio. Hold the talk key and your voice reaches everyone else holding a set tuned to the same band, over distances far beyond proximity chat.

## Crafting

```
W C
P P
```

W is [Copper Wire](Wire_Drawer.md), C is a [circuit](Basic_Electronic_Circuit.md) and P is a Wrought Iron Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and one screw as its [fastener](Fasteners.md). It needs Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

## Using it

- **Sneak and use** steps the band, 1 to 8, and back to 1 after 8. The band shows above the hotbar.
- **Use with Copper Wire in the other hand** winds 100 mm of antenna onto the set and spends one wire. An antenna holds up to 2500 mm.
- **Hold the talk key** (**.** (period) by default, rebindable under the *Feedback* category in Controls) while a set is in either hand to transmit. Your normal proximity voice is replaced while you talk.

The tooltip shows the set's band and its antenna length in mm.

## Tuning

Each band wants an antenna of its own length, a quarter wave. Band 1 wants about 2400 mm; a higher band wants 2400 mm divided by the band number.

| Band | Antenna wanted (mm) |
|---|---|
| 1 | 2400 |
| 2 | 1200 |
| 3 | 800 |
| 4 | 600 |
| 5 | 480 |
| 6 | 400 |
| 7 | about 343 |
| 8 | 300 |

The closer the antenna's length is to the band's, the better the set hears and is heard. The fit falls off smoothly on either side, and a badly matched antenna still carries a little. An antenna is only ever lengthened, so a set wound for a low band is too long for a high one.

## Reach

Only sets on the same band, in the same dimension, hear each other. A listener must hold a set in either hand.

Two well-tuned sets reach about 160 blocks. A badly tuned antenna at either end shortens the reach for both directions. Out to about two thirds of the reach the voice is clean; beyond that packets drop, more and more toward the edge, and the voice breaks up. Past the edge it is silent.

## Voice chat

The voice travels through [Simple Voice Chat](Mod_Compats.md#simple-voice-chat), which must be installed on the server and on the players. Without it the set still tunes and shows its band and antenna, and carries no voice.
