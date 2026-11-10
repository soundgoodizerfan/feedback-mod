# Control Surfaces

Hinged wing plates that roll, pitch or yaw a flying [vehicle](Vehicles.md). A deflected surface changes the lift on its own plate, and the force on that plate turns the craft.

## Setting a hinge

Look at a hinge point on a frame and right-click with the Pivot Setter. A hinge steps through these roles, then on to a ball:

1. plain
2. steered (see [Steering](Vehicles.md#steering))
3. **aileron** (roll), normal then reversed
4. **elevator** (pitch), normal then reversed
5. **rudder** (yaw), normal then reversed

The message names the role. A reversed surface deflects the opposite way for the same command, so the two halves of a pair of ailerons roll the craft one way.

## Driving them

The frame takes commands as analog channels: **2 is Roll**, **3 is Pitch** and **4 is Yaw** (channel 0 is steering and 1 is the brake). Link a [Controller](Controller.md) card to the frame and feed it from the [Driver's Seat](Vehicles.md#the-drivers-seat) keys, a sensor or any program. A surface swings up to about 29 degrees.

## How it works

A wing plate on a hinged part rides that part's own body, so a deflected aileron changes the lift on its own plate and not the whole wing. See [Wings](Vehicles.md#wings) for how plates lift and stall.
