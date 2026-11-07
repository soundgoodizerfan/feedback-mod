# Substation

An arrangement of blocks that steps a trunk line down to a branch, with a switch and a meter on it, and sheds low-priority branches when supply sags. It is built from parts, not a block of its own.

## Building one

On the trunk side of the line:

1. A [Solid-State Converter](Converter.md), wired to the trunk, with a Converter Tap carrying the stepped line to the branches.
2. A [Breaker](Breaker.md) between the trunk and the converter, so the whole substation can be isolated.
3. A [Current Sensor](Instruments.md#current-sensor) on the trunk, so a [Controller](Controller.md) can read the load.

## Load shedding

Give each branch its own Breaker. A Controller with Current Sensor inputs opens the Breakers on the low-priority branches when the supply sags, and closes them when it recovers. Hold a band so the Breakers do not flap under load: see [Holding a band](Controller.md#holding-a-band).

Open a Breaker only after the load is off if you can: opening under heavy current wears it and, at high voltage, throws an [arc flash](Breaker.md#arc-flash).
