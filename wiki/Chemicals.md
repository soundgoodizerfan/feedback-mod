# Chemicals

The gases and liquids of the chemical chain: hydrogen and oxygen from the [Electrolyser](Electrolyser.md), brine, lye, acids, carbon dioxide, leach liquors, liquid teleportant, heavy water, fluorine compounds, bleach, nitrogen, argon, helium, ammonia and the molten cryolite bath. Gases are stored in a [Gas Tank](Gas_Tank.md), carried by a [Pipe](Pipe.md) and compressed by a [Gas Compressor](Gas_Compressor.md). Most are made in the [Reactor](Reactor.md).

## Hydrogen

A very light, flammable gas. The Electrolyser makes it from water.

- **Fuel.** It burns at a flame temperature of **2200 Tu**, hotter than any fuel in [Fuels](Fuels.md). A [Fluid Burner](Fluid_Burner.md), [Gas Turbine](Gas_Turbine.md), [Engine](Engine.md) or [Thruster](Thruster.md) burns it like any other fluid that reaches it.
- **Light.** It weighs very little for its volume, so it lifts an [airship](Airship.md) in [Gas Cells](Gas_Cell.md). It burns there too.
- A flammable gas in a hot line is a [hazard](Hazards.md).

## Oxygen

A gas that makes fire burn harder and that a lung can breathe at full strength. The Electrolyser makes it from water.

- **Oxidizer.** Oxygen carries oxygen of its own, so a fuel burned with it burns as if blown on. See [Fuels](Fuels.md).
- **Breathable.** Its oxygen share is 1: all of it is usable. Open air has about a fifth of that. A [Culture Dish](Sculk_Culture.md) breathes it at full rate.

JEI's Material Stats page lists the figure as **Oxygen share**.

## Brine

Salt water. Dissolve Salt in water in a [Reactor](Reactor.md). The [Electrolyser](Electrolyser.md) splits it into hydrogen, chlorine and lye.

## Lye

A strong base. It is left in the Electrolyser's cell when brine is split; draw it off a side face. It dissolves bauxite in the Reactor ([Making Aluminum](Making_Aluminum.md)), turns chlorine into bleach, and can serve as the electrolyte of an [Electrolytic Cell](Electrolytic_Cell.md). It bites vessels that are not lined for it ([Hazards](Hazards.md)).

## Chlorine

A heavy toxic gas from the anode (front) of an Electrolyser running on brine. It is the feed for hydrochloric acid and bleach. A full electrode tank with nowhere for it to go seeps out and poisons anyone nearby.

## Acids

**Hydrochloric Acid** is made in a Reactor from hydrogen and chlorine. **Sulfuric Acid** is made from sulfur dioxide, oxygen and water over a catalyst bed. Both work as the electrolyte of an Electrolytic Cell, where an ore item hung as the anode dissolves as the metals it bears.

## Sulfur dioxide

A heavy toxic gas. Roasting pyrite in a Reactor gives it, with iron nuggets; roasting a sulfide concentrate and smelting galena concentrate give it too ([Ore Processing](Ore_Processing.md#roasting-sulfides)).

## Carbon dioxide

A gas that comes off when malachite is leached with sulfuric acid in a [Reactor](Reactor.md) ([Ore Processing](Ore_Processing.md#leaching-and-winning)). Store it in a [Gas Tank](Gas_Tank.md) or pipe it away.

## Leach liquors

Leaching an ore concentrate in the Reactor leaves its metal dissolved in a liquor: Copper Sulfate, Zinc Sulfate, Nickel Sulfate, Cobalt Sulfate, Gold Chloride, Silver Chloride, Titanyl Sulfate, Sodium Tungstate, Uranyl Sulfate, Thorium Hydroxide and Sodium Aluminate. Pipe them like any fluid. The [Electrolyser](Electrolyser.md#what-it-splits) wins copper, zinc, nickel and cobalt out of theirs; the others give up their metal to a reducing gas in the Reactor. See [Ore Processing](Ore_Processing.md#leaching-and-winning).

## Liquid Teleportant

A fluid made from chorus fruit in four stages. It pays for each crossing of an [Ender Port](Ender_Port.md). See [Liquid Teleportant](Liquid_Teleportant.md).

## Heavy Water

A scarce residue of electrolysing enriched water. See [Heavy Water](Heavy_Water.md).

## Hypochlorite Bleach

Made in a Reactor from chlorine and lye. Dip a block of dyed wool in a vessel holding it, the way a ceramic is [glazed](Casting.md#glazing), and it comes out white wool; each block takes 250 mB.

## Air separation

Air is split into its parts in two ways.

- **Catalyst bed.** A Reactor separates nitrogen from air over a catalyst bed, warm and cheap.
- **Cold.** A [Reactor](Reactor.md#the-reactions) chilled by a [Cryocooler](Cryocooler.md) liquefies compressed air into **Liquid Air**, boils nitrogen off it into a gas and leaves an **Oxygen-Rich Liquid**, then boils that liquid into **Argon** and oxygen. The cold route is the only source of argon. A further cut of cold nitrogen gives [helium](#helium).

## Nitrogen

The inert part of air. It is the feed for ammonia, and the protective atmosphere for sintering ([Kiln](Furnaces.md#sintering), [Arc Hearth](Arc_Furnace.md#sintering-under-gas)).

## Argon

An inert gas from the last cold cut of air. It does the work of nitrogen as a sintering atmosphere, and takes much less of it: 150 mB in the [Arc Hearth](Arc_Furnace.md#sintering-under-gas) against 300 mB of nitrogen.

## Helium

The lightest noble gas, and a lift gas for an [airship](Airship.md) that never burns. It has two sources:

- **End stone.** Heating [End Stone](End_Stone.md) in a Reactor releases 100 mB of helium along with the hydrogen.
- **Cold cut.** 100 mB of nitrogen in a Reactor at **-196 to -189 Tu** (best -193) gives 4 mB of helium and returns 96 mB of nitrogen. Too warm and the nitrogen comes back unchanged.

Store it in a [Gas Tank](Gas_Tank.md) and carry it by [Pipe](Pipe.md) like any gas.

## Osmium tetroxide

A volatile, toxic gas made by leaching osmiridium with sulfuric acid and oxygen in a [Reactor](Reactor.md#the-reactions). It is the way osmium leaves the ore; hydrogen reduces it to osmium sponge. Loose in a room it forms a [fume cloud](Hazards.md#fume-clouds).

## Ammonia

A toxic gas made in a Reactor from nitrogen and hydrogen over an iron bed. With sulfuric acid it makes [Fertilizer](Fertilizer.md).

## Fluorine and PTFE

**Hydrofluoric Acid** comes from Cryolite Ore and sulfuric acid in a Reactor. From there PTFE is a chain of [Reactor](Reactor.md#the-reactions) steps:

1. **Methane** from coal and hydrogen over a catalyst bed.
2. **Chloroform** from methane and chlorine, with hydrochloric acid as a byproduct.
3. **Chlorodifluoromethane** from chloroform and hydrofluoric acid, with more hydrochloric acid.
4. **Tetrafluoroethylene** gas by heating chlorodifluoromethane.
5. **PTFE Powder** from tetrafluoroethylene and water, under pressure.
6. **PTFE Insulation** by sintering the powder in any vessel that holds the heat.

PTFE does not flow when heated, so it is never cast from a melt; it is a powder sintered like cermet. Windows and amounts are in the [Reactor](Reactor.md#the-reactions) table; the finished polymer is on [Materials](Materials.md#polymers).

## Cryolite bath

Cryolite Ore melted in a Reactor at 1010 Tu gives this molten liquid. At 950 Tu or hotter it is the solvent in which an Electrolyser splits alumina into aluminum. It is not used up. See [Making Aluminum](Making_Aluminum.md).
