# Conveyor Belt

A strip of moving band that carries items, dropped stacks and creatures along a straight run. A [Belt Head Roller](Belt_Head_Roller.md) on a [shaft](Shaft.md) drives it, and [belt modules](Belt_Modules.md) fitted into its cells load, sort, drop and condition what rides. The belt between two pulleys that carries rotation is a different thing: see [Pulleys and Belt](Pulleys.md).

## Crafting

A belt is a modular item made at the Tool Table from a **band** and an optional **cleat**, as for any [modular tool](Tools_and_Weapons.md).

- **Band** (Belt Band): leather or a plate of any material that can bend into a strip.
  - The standard band is one strip.
  - A **textured** band has a rougher face and grips harder.
  - A **link** band is a chain of short plates. It grips less but takes much hotter cargo, so a steel link band carries what would char a hide.
- **Cleat**: bars across the band, from a rod or stick. They roughen the face so the belt holds cargo on a steeper slope.

The [Blueprint Table](Blueprint_Table.md) shows a drawing's **grip on cargo** and the hottest cargo it carries, in Tu, before you make it. The band's material sets both: leather and rubber grip, steel link takes the heat.

## Laying a run

Place cells end to end facing the same way, and finish the run with a [Belt Head Roller](Belt_Head_Roller.md) at the exit end. A cell fed from the side or round a corner ends its run and hands its cargo to the middle of the next belt cell.

- **Sneak and use a belt item on a cell** to step it level, up one block, or down one block.
- **Sneak with an empty hand** on a bare cell to turn it a quarter turn.
- **Sneak with an empty hand** on a cell with a module pulls the module out.

## Using it

- **Dropped items** that land on a belt ride it. So do creatures standing on it, unless they sneak. A belt carries a player along on their own screen.
- **Cargo** leaving a run goes onto the next belt, into a container or inventory the end faces, or falls to the floor.
- **Right-click the top** of a cell with an empty hand to take a unit of cargo off the belt. That clears a jam.
- A run carries a limited mass of cargo, and a single unit is capped too: a heavy stack rides as several units, and heavier cargo spaces itself further apart.
- **Look.** The band moves while the run moves and stands still when it stops. A cleated band shows its cleat bars sliding along the run.
- **Away from you.** Cargo on a run in an unloaded chunk catches up when the chunk loads: the run moves on by the time it waited, at most about a minute of travel.
- **Speed** follows the head roller's RPM; a head turning the other way runs the belt backward. The roller loads its shaft with its own weight plus the cargo on the run, so a weak shaft slows a heavily loaded belt.

## Slopes

Cargo holds on a rise only if the belt's grip times the cargo's grip is enough. A cleated or rubber band climbs where plain leather lets cargo slide back, slippery cargo climbs worst, and a wet belt slips sooner. A unit that cannot hold waits at the foot of the rise.

A band turns **wet** when rain or a fluid falls on it, or when cargo that holds fluid rides it, and stays wet for about 30 seconds afterward. A wet band has about half the grip. A [Belt Hood](Belt_Modules.md#belt-hood) over a cell keeps rain and fluid from above off it; cargo that holds fluid still wets the band under it.

## Heat

- **Hot cargo wears the band.** Cargo hotter than the band's limit wears the cells it lies on. A worn band slips and slows the run but never stops it. Cool the cargo first with a [Belt Cooler](Belt_Modules.md#belt-cooler) or [Belt Fan](Belt_Modules.md#belt-fan), or use a link band.
- **Heat above a belt** burns creatures riding it. A plain [Heating Coil](Heating_Coil.md) block set on a belt heats the cargo riding that cell.

Hot cargo and hot sources are [hazards](Hazards.md) like any other.

## Tips and automation

A [Belt Counter](Belt_Modules.md#belt-counter) reports counts and rate to a [Controller](Controller.md). A [Linear Motor Coil](Belt_Modules.md#linear-motor-coil) drives a run on current alone. A [Belt Gate](Belt_Modules.md#belt-gate), diverter, electromagnet and air jet can be engaged and released by a controller.

[Tube Stations](Tube_Station.md) take items from a belt end and [funnels](Belt_Modules.md#belt-funnel-input) move cargo on and off a run.
