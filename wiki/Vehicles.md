# Vehicles

> **Early, experimental feature.** All of the vehicle tools are creative-only for now. You can build a chassis, lift it, drop it into physics and carry it around, but **wheels, tires and driving don't exist yet**.

Vehicles in Feedback are built the way you'd build a car in a garage: on a lift, around a welded frame, out of real materials that have real weight.

## The tools

| Item | Use |
| --- | --- |
| **Lift** | The stand you build on. Turns what's on it into a single moving object. |
| **Chassis Frame** | The block a welded frame is built around. |
| **Welder** | Places points, joins them with tubes, and lays plates between them. |
| **Pivot Setter** | Turns a point into a hinge, a ball joint or a locked joint. |
| **[Engine](Engine.md)** | Power. Designed on the Engine Stand. |

## The Lift

1. Place a **Lift** and build on top of it. Whatever rests on the lift, and only that, becomes the vehicle. **It mustn't touch the ground or anything else**, or the lift will complain *"Too big -- is it touching something besides the lift?"*
2. **Right-click** the lift. Everything on it becomes one object, held still: *"Lifted."*
3. **Right-click** again to raise it a step, and **sneak-right-click** to lower it. It goes up to 10 steps.
4. **Break the lift** to drop the vehicle into the world, where gravity takes over.

A lift that's part of a vehicle itself can't lift anything.

### Carrying a creation

Right-click any creation with a **Lift item** and the whole thing goes *into the item*, with every block and everything in every chest: *"Picked up (N blocks)."* The tooltip shows how big it is. Right-click the ground to set it back down on a new lift, already held. If anything is in the way you'll get *"No room to put it down here,"* and nothing is placed.

## Building a frame

Place a **Chassis Frame** block. Everything you weld is measured from it, and it's what holds your frame to the vehicle.

The **Welder** has three modes. **Sneak + scroll** to switch between them; the mode is shown when you switch.

### Place points

- Right-click a block face, or empty air, to place a **point** there. In the air, it goes at the same distance as the point you're holding (or two blocks out).
- Right-click a point to select it. Right-click it again to pick it up, then right-click somewhere else to put it down. Tubes and plates attached to it move with it.
- Right-click the middle of an edge to split it with a new point.

### Connect points

- Right-click one point, then another, to join them with an **edge**. Do it again to remove the edge.
- Hold a **block in your off-hand** and the new edge becomes a **tube** of that material. Right-click a tube with a block to make it thicker (the thickest wraps back to thin).
- With a point selected, right-click a **block** to **weld the point to it**. Do it again to unweld. **Only welded points hold the frame to the vehicle.** Any part of the frame that isn't welded down (directly or through a joint) will simply fall off when the vehicle drops.
- With a point selected, right-click a point on **another frame** to weld the two frames together. Do it again to cut the joint.

### Make plates

Hold a block in your off-hand and right-click three points in turn. The third click lays a **triangular plate** of that material between them, adding any missing edges. Right-click a closed triangle to re-plate it in a different block. Each plate or tube uses up one of the block.

### Removing

**Sneak-right-click**, in any mode:

- on a **point**: removes it and everything attached to it
- on a **plate**: removes the plate, keeps the edges
- on a **tube**: turns it back into a plain edge
- anywhere else: lets go of whatever you're holding

All of this works on a vehicle that's already lifted or moving, too.

### Weight

Plates and tubes **weigh what their material weighs**, so a frame of iron plates is far heavier than one of wood. An [Engine](Engine.md) adds its own design weight.

> Right now players and mobs don't collide with plates and tubes, only with the blocks on the vehicle.

## Joints: the Pivot Setter

Look at a point and right-click:

- On a point of a frame: cycles **plain → hinge → ball → locked → plain**.
  - **Hinge** turns about one axis only. If one tube runs through the point, it turns around that tube, like a shaft. Otherwise it turns around the grid axis nearest to where you're looking.
  - **Ball** turns freely in every direction.
  - **Locked** is rigid, but you can switch it back.
- On a point where **two frames are welded together**: cycles that joint between hinge, ball and locked.
- **Sneak-right-click**: toggles whether the bearing **carries rotation across** or **spins free**.

Use hinges and ball joints to make things like suspension arms. Weld the inner end to a block, set it to a hinge, and the arm swings.

## Connecting to the drive

Weld a **tube** to a [Shaft](Shaft.md) so that it runs **straight out of the shaft, along its axis**. The frame is then coupled into that shaft's network and puts a small load on it. A tube at an angle doesn't couple. This is groundwork for driven wheels, which aren't in yet.
