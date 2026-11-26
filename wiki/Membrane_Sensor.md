# Membrane Sensor

A phantom membrane film in a block that reads how sleepless the nearest player is, and gives the reading to a [Controller](Controller.md).

## Crafting

```
P M P
W W W
```

P is a Brass Plate, M is a Phantom Membrane and W is Copper Fine Wire. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 screws. It needs electrical school 2.

## Using it

Link it to a Controller with the [Data Connector](Data_Connector.md#linking-nodes), as any sensor. It needs no power.

- **Channel 0** reads the sleeplessness of the most tired player within about eight blocks, from 0 up to 1 at the sleeplessness that calls phantoms, about an hour of play without sleep. Sleeping resets it.
- **Channel 1** reads 1 when any player is within reach, else 0.

The film answers to the volatiles a tired creature gives off, the same ones that draw phantoms to a sleepless player. A creature that does not sleep gives it nothing. It only measures; what a Controller does with the reading is up to the program.
