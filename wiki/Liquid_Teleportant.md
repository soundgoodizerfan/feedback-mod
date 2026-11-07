# Liquid Teleportant

A fluid made from chorus fruit in four stages. An [Ender Port](Ender_Port.md) spends it to send items across, and a [Synthetic Pearl](Synthetic_Pearl.md) is pressed with it.

## Making it

1. **Macerate.** A [Reactor](Reactor.md#the-reactions) holding one Chorus Fruit and 100 mB of water makes 100 mB of **Raw Spatial Slurry**. Hold it between 0 and 60 Tu, fastest at 25.
2. **Distill.** Feed the slurry to a [Fractionating Column](Fractionating_Column.md#spatial-slurry). It gives up water and steam, then **Unstable Spatial Solvent**, which boils at 160 Tu and leaves as **Unstable Solvent Vapour** if it is pushed past that.
3. **Damp.** A Reactor holding 100 mB of solvent, 5 mB of [Heavy Water](Heavy_Water.md) and one Lapis Lazuli dust makes 100 mB of **Damped Spatial Solvent**. Hold it between 5 and 70 Tu, fastest at 30. Above 70 Tu the batch is ruined into solvent vapor.
4. **Strike.** A Reactor with an [Arc Head](Arc_Furnace.md#the-arc-head) set against it, holding 100 mB of damped solvent and one Redstone dust, makes 100 mB of Liquid Teleportant. Hold it between 300 and 2200 Tu, fastest at 900. Above 2200 Tu the batch is ruined into solvent vapor.

Heavy Water is the scarce input: each 100 mB of solvent needs 5 mB of it.

## Unstable solvent

Unstable solvent and its vapor are spatially restless. Items and creatures within a few blocks hop a short, random distance when:

- a Reactor's product has nowhere to go,
- a Column's vapor escapes, or
- a Reactor holds the solvent at or above its boiling point.

The more that is lost, the likelier the hop. A hop needs a free spot to land, so something shut in a sealed cell stays put. Damped solvent and Liquid Teleportant do not hop things.

## Using it

Pipe it into an Ender Port: each port has its own tank of 2000 mB, and only this fluid fits. Right-click a port to read its tank. Each crossing draws from the tank of the port the item went into: about half a mB for each Mu of the cargo's mass over a short distance, rising with distance until the cost has doubled at 256 blocks. With too little in the tank nothing crosses.
