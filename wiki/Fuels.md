# Fuels

Fuel is a property of a material, not of an item. Anything that burns has a **flame temperature** (how hot the fire is) and an **ignition point** (how hot it must get to light by itself). The [Fluid Burner](Fluid_Burner.md), [Gas Turbine](Gas_Turbine.md), [Engine](Engine.md) and [Thruster](Thruster.md) all burn whatever fluid reaches them, and read these two figures. Items burn in a [Firebox](Firebox.md).

| Fuel | Flame | Ignition | Notes |
| --- | --- | --- | --- |
| Naphtha | 1400 Tu | 250 Tu | lightest liquid cut |
| Kerosene | 1250 Tu | 290 Tu | |
| Diesel | 1350 Tu | 320 Tu | |
| Heavy Oil | 1200 Tu | 330 Tu | |
| Crude Oil | 1200 Tu | 300 Tu | burns as it comes, at its worst |
| Soul Oil | 1050 Tu | 260 Tu | carries oxygen of its own (see below) |
| Ethylene | 1300 Tu | 450 Tu | gas |
| Wisp Gas | 1050 Tu | 1000 Tu | gas |

Where the cuts come from: [Crude Oil](Crude_Oil.md) is split in a [Fractionating Column](Fractionating_Column.md); heavy oil cracks in a [Reactor](Reactor.md); [soul oil and wisp gas](Soul_Oil.md) come from the reactor's soul retort.

## What the figures do

- **Flame temperature** sets how hard a fuel works. An engine, turbine or thruster gives power in proportion to it (1300 Tu is the reference), and a hotter fuel is burned more slowly for the same power. A burner's flame is as hot as it.
- **Ignition point** decides whether an [engine](Engine.md#compression-fuel-and-knock) can burn the fuel at its compression.
- **Oxygen.** A fuel that brings its own oxidiser (soul oil) burns as if it were being blown on: hotter in a burner or firebox, and a thruster with it can burn where there is no air.

## Mixtures

An engine's or thruster's tank takes any fluid and **mixes it by mass**: the blend's flame temperature and ignition point are the mass-weighted averages of what is in it. A blend of diesel and naphtha has an ignition point between theirs. A fluid that does not burn dilutes the flame without adding an ignition point. A blend is a fact of the tank, not a fight with it: nothing is refused.
