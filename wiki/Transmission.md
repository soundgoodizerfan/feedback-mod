# Transmission

A gearbox you design yourself on the [Car Design Stand](Car_Design_Stand.md) (its second tab). It couples the shaft behind it to the shaft in front at the ratio of the gear it is in.

## Designing one

Open the stand, switch to the **Transmission** tab, and set the number of gears (2 to 6) and each gear's ratio in steps of 0.05, from 0.5 to 5.0. A ratio is input turns per output turn: 3.5 is a low gear, 0.8 an overdrive. Drop a sample of the metal in the sample slot, put paper in, and **Print**. Craft the Transmission Blueprint with that metal's ingots (2 per gear) to get the block. The blueprint is kept.

## Using it

Place it with its back to the input shaft and its front to the output. **Right click** shifts up through neutral, the gears and reverse; **sneak right click** shifts down. Reverse is first gear's ratio, backwards. In neutral nothing is coupled. A program or the Driver's Seat can shift it too, through its gear input.

## The clutch

A transmission also has a **clutch** input, channel 1 (the shifter is channel 0). Pressed past halfway it parts the link, so the input shaft coasts free of the output, and a shift made while it is pressed does not grind. Link a *Switch* card to the transmission and send it a key to use it; let it go and the gears couple again at once.

## Wear

Shifting while the input shaft is turning fast grinds the teeth, in proportion to the speed and softer metals worse. A badly worn transmission jumps out of gear under load, more often the more worn it is. Stopping, going through neutral or pressing the clutch costs nothing, and running it never wears it.
