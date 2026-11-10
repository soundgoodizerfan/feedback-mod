# Airship

A [vehicle](Vehicles.md) that floats on a lift gas. Its envelope is a block of [Gas Cells](Gas_Cell.md) filled with a gas lighter than air; [Propellers](Propeller.md) or [Thrusters](Thruster.md) push it along, [Control Surfaces](Control_Surfaces.md) steer it, and a [Ballast Tank](Ballast_Tank.md) and [Vent Valve](Vent_Valve.md) set how high it sits.

## Parts

| Part | Job |
| --- | --- |
| [Gas Cell](Gas_Cell.md) | Holds the lift gas. Lifts where it sits. |
| [Vent Valve](Vent_Valve.md) | Lets gas out of the cells beside it to come down or relieve a full cell. |
| [Ballast Tank](Ballast_Tank.md) | Holds water that weighs the craft down; dumping it lifts the craft. |
| [Propeller](Propeller.md) | Pulls the craft along its facing from a turning shaft. |
| [Thruster](Thruster.md) | Pushes the craft along its facing by burning fuel. |
| [Control Surfaces](Control_Surfaces.md) | Hinged wing plates that roll, pitch and yaw the craft. |

## Lift gases

- **Hydrogen**: from the [Electrolyser](Electrolyser.md) or from heating [End Stone](End_Stone.md). It burns.
- **Wisp gas**: it burns.
- **[Helium](Chemicals.md#helium)**: the lightest noble gas, from heating End Stone or a cold cut of nitrogen. It never burns.

## How lift works

Lift is physical. A cell displaces air, and when that air weighs more than the gas inside, the difference lifts the cell **at the cell's own position**. Where the cells and the load sit therefore trims the ship: a heavy engine at the tail with cells forward tips the nose up.

Air thins with height in the Overworld, so a cell lifts less the higher it climbs (see [air density](Vehicles.md#air-density)). A ship climbs until its lift and its weight match.

## Climbing, venting and ballast

- A cell is wrapped in a skin that stretches as the air thins. A full cell that climbs past its skin's stretch limit **weeps gas** until a Vent Valve lets some out first. See [Gas Cell](Gas_Cell.md#stretch).
- To come down, open a Vent Valve or fill a Ballast Tank. To rise, dump ballast.
- Both open by hand with a right-click, or from a [Controller](Controller.md) card, so an autopilot can hold altitude.

## Hazards

Hydrogen and wisp gas burn, and a burning envelope takes the ship with it. Arrows and bullets leave holes that leak. See [Gas Cell](Gas_Cell.md#fire) and [Gas Cell](Gas_Cell.md#holes-and-patches), and [Hazards](Hazards.md).
