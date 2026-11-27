# Radio Set

A hand-held radio. Hold the talk key and the sounds around you, and your voice with Simple Voice Chat, reach everyone else holding a set tuned to the same band, over distances far beyond proximity chat. A [Loudspeaker](#loudspeaker) block plays the room sound aloud.

## Crafting

```
W C
P P
```

W is [Copper Wire](Wire_Drawer.md), C is a [circuit](Basic_Electronic_Circuit.md) and P is a Wrought Iron Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and one screw as its [fastener](Fasteners.md). It needs Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

## Using it

- **Sneak and use** steps the band, 1 to 8, and back to 1 after 8. The band shows above the hotbar.
- **Use with Copper Wire in the other hand** winds 100 mm of antenna onto the set and spends one wire. A wound antenna holds up to 2500 mm. Use an [Antenna](Antenna.md) with the set in the other hand, or right-click it onto the set in an inventory, to replace the set's antenna with the Antenna's length and metal.
- **Right-click the set with a charged [Battery](Battery.md) on the cursor** loads it; right-click with an empty cursor takes the battery out. A set with no charge only listens.
- **Hold the talk key** (**.** (period) by default, rebindable under the *Feedback* category in Controls) while a set is in either hand to transmit; a keyed set draws **1 Eu/s** from its battery, and stops transmitting when the battery runs flat. Listening costs nothing. While you talk the set's microphone also picks up the room (see [Room sound](#room-sound)).

The tooltip shows the set's band, its antenna length in mm and whether a charged battery is loaded.

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

The closer the antenna's length is to the band's, the better the set hears and is heard. The fit falls off smoothly on either side, and a badly matched antenna still carries a little. Winding wire only lengthens the antenna, so a set wound for a low band is too long for a high one; fit an [Antenna](Antenna.md) to set a new length. The metal of the antenna counts too: a metal that conducts worse than copper hears less at any length.

## Reach

Only sets on the same band, in the same dimension, hear each other. A listener must hold a set in either hand.

Two well-tuned sets reach about 160 blocks. A badly tuned antenna at either end shortens the reach for both directions. Out to about two thirds of the reach the voice is clean; beyond that packets drop, more and more toward the edge, and the voice breaks up. Past the edge it is silent.

## Noise

An [Arc Head](Arc_Furnace.md#the-arc-head) or Arc Hearth within **4 blocks** of a person drowns that end of the link: the link is much poorer, so the reach shortens and the voice breaks up sooner. A person with Lead Chests on four or more of the six sides of the block they stand in is shielded and hears no such loss.

On a marginal link, sounds relayed from a distant set arrive with clicks of static between them, more of it nearer the edge of reach.

## Room sound

While you hold the talk key, the set's microphone hears game sounds made within 16 blocks of you: footsteps, doors, machines, mobs, block breaks. Everyone holding a set on the same band in the same dimension hears them as a quiet, slightly flat copy at their own ear. The link is the same as for voice: the louder the link, the louder the copy, and packets drop in patches near the edge of reach. A speaker relays at most four sounds a tick, so a noisy machine hall does not flood a listener. Music is not relayed. This needs no other mod.

## Voice chat

With [Simple Voice Chat](Mod_Compats.md#simple-voice-chat) installed on the server and on the players, your voice travels over the set as well, and replaces your proximity voice while you talk. Without it the set carries room sound only.

## Loudspeaker

A block that plays aloud the room sound a keyed set relays on its band. It is a sink on an [electrical](Electricity.md) line and is silent without power.

### Crafting

```
P C P
W E W
P P P
```

P is a Steel Plate, C is paper, W is [Copper Wire](Wire_Drawer.md) and E is an [Electromagnet](Electromagnet.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)).

### Using it

- **Use with a radio set in hand** copies the set's band onto the speaker.
- **Use with an empty hand** steps the band, 1 to 8, and back to 1 after 8.

The band shows above the hotbar. The speaker has no antenna to tune.

### How it works

While a person holds the talk key on a set on the speaker's band, the sounds the set's microphone hears ([Room sound](#room-sound)) come out of the speaker, at the speaker's own place, if that person is within **64 blocks** of it and in the same dimension. The farther away they are, the quieter it plays, and near the edge it drops out in patches. Voice chat does not play from it.

A speaker with no power stays silent. A speaker does not replay what another speaker plays.

Noise cuts the link as it does a set's ([Noise](#noise)): an Arc Head or Arc Hearth within 4 blocks of the speaker, or of the person talking, weakens it, and Lead Chests around the speaker shield it.
