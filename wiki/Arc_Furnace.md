# Arc Furnace

An electric furnace in two blocks: an **Arc Hearth** that holds the melt, and an **Arc Head** that burns an electric arc into it. The arc reaches temperatures no fire does, hot enough to melt tungsten.

## Crafting

**Arc Hearth**: a ring of [Insulation](Insulation.md) blocks around a Large Crucible.

```
I I I
I L I
I I I
```

I is an Insulation block and L is a Large [Crucible](Crucible.md).

**Arc Head**

```
S W S
C S C
C   C
```

S is a Steel Plate, C is Ceramic and W is Copper Fine Wire.

**Electrode**: three bars of one material in a vertical column make an electrode of that material. A bar is a rod, an ingot or a lump. The material is graphite or any conducting metal. A bar of a tougher material above an electrode in a column adds a [tip](#tipped-electrodes).

## The Arc Hearth

A crucible four times over: **6 charge slots** and a tank of **5184 mB**, 36 ingots. It has no fire of its own, and it is far heavier than a Large Crucible, so it heats and cools slowly. Its screen, pouring into molds, hopper behavior and tapping work as on the [Crucible](Crucible.md).

**Wall wear.** A bath hotter than **2,200 Tu** wears the hearth's wall, faster the further over. With a [Refractory Lining](Refractory_Lining.md) fitted the limit is **3,300 Tu**. A worn wall leaks more heat, down to a floor of half condition; the hearth keeps working.

## The Arc Head

Set the head against a [Crucible](Crucible.md) or an Arc Hearth, on top of it for the usual build. It faces the block it was placed against and burns into it. The head heats all four sides of that block as well as its face; see [Heat](Heat.md#heat-sources-have-a-facing).

- **Right-click with an electrode** to fit it. The head holds three.
- **Right-click with an empty hand** to take one back.

Jade on the head lists electrode slots 1, 2 and 3, each with its material and condition, or "empty".

Run an [EHV](Electricity.md#volts-and-amps) line to it. With three electrodes it draws **42 A at 384 V**; with fewer it draws less. The arc follows the power it actually receives, so a sagging line lowers it. A [Breaker](Breaker.md) in the line switches it. See [Power](#power) for supplying it.

### How hot

The arc is capped by the **weakest electrode**: about 97% of the temperature at which its material sublimes (graphite, 3600 Tu) or boils (any conducting metal), and never above **3500 Tu**. A crucible or hearth under the arc climbs toward that, as it does under any fire ([Heat](Heat.md)).

## Power

The arc needs EHV. A large [Electric Machine](Electric_Machine.md) on a dense or heavy-dense winding, as a generator on a [Gas Turbine](Gas_Turbine.md) or a [Steam Turbine](Steam_Turbine.md), supplies it in survival:

- **Dense** (1.5 V per RPM) at 240 RPM gives 360 V, in the EHV band, at up to about 42 A.
- **Heavy-dense** (6 V per RPM) at 240 RPM gives 1,440 V, in the Trunk band, at about 10 A, which the frame's torque limits.

The cable must be insulated to the band: quartz holds EHV, and PTFE holds Trunk ([Copper Cable](Copper_Cable.md)).

## Electrodes

An electrode is a rod of a material that can carry the arc. Its tooltip shows the temperature its arc holds to and its condition.

- **Wear.** An electrode loses condition while the arc burns. The hotter the bath, the faster, steeply so: near an electrode's limit it wears many times faster than at steel's heat.
- **Snapping.** At 5% condition it snaps. The stump drops into the bath and the arc flashes.

### Tipped electrodes

Put a bar (rod, ingot or lump) of a material that can take more heat than the electrode directly above an electrode, in a column in the crafting grid, and the electrode gets that material as a **tip**. The tip lifts the electrode's limit and slows its wear near that heat: a graphite electrode with a tungsten tip wears far slower near tungsten's melt. The electrode keeps its condition. A bar of the electrode's own material, or of one that takes less heat, does nothing.

## Hazards

- **Flash and splash.** A bath above **3000 Tu** that holds metal now and then splashes and flashes. Anything within 6 blocks takes heat damage, and a player without a helmet is blinded. A snapping electrode flashes the same way.
- **Boiling metal.** Molten metal in any vessel that is held past its boiling point boils away a share each tick, giving off a poisonous metal fume. The alloy drifts as it does: zinc, lead and tin go first. See [Hazards](Hazards.md).
- **Steel left hot** is ruined like any steel batch held past its spoil line ([Making Steel](Steel.md)).
- The line is EHV, the strongest shock band ([Hazards](Hazards.md#shock)).
