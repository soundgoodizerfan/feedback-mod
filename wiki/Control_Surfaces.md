# Control Surfaces

Hinged wing sheets that roll, pitch or yaw a flying [vehicle](Vehicles.md). A deflected surface changes the lift on its own sheet, and the force on that sheet turns the craft.

## Making one

Build a wing sheet in front of a [Rotary Bearing](Rotary_Bearing.md) voxel and set the bearing's **role** before assembling: sneak-use it with an empty hand until the message names the role you want.

- **Roll surface** (aileron)
- **Pitch surface** (elevator)
- **Yaw surface** (rudder)

Then use the bearing with an empty hand to assemble the sheet as the turning part.

## Driving them

A [Controller](Controller.md) card sets the bearing's **Angle**: point a [Data Connector](Data_Connector.md) at the bearing voxel, link a *Switch* card to it, and feed the card from the [Driver's Seat](Vehicles.md#the-drivers-seat) keys, a sensor or any program.
## How it works

A wing sheet on a hinged part turns with that part, so a deflected aileron changes the lift on its own sheet and not the whole wing. See [Wings](Vehicles.md#wings) for how sheets lift and stall.
