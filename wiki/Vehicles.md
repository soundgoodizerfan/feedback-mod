# Vehicles

> **Early, experimental feature.** Most of the vehicle tools are creative-only for now. You can build a chassis, lift it, drop it into physics and carry it around. **Tires, a driven drivetrain and a Driver's Seat are new and not yet tested in play.** Steering, brakes and springs don't exist yet.

Vehicles in Feedback are built the way you'd build a car in a garage: on a lift, around a welded frame, out of real materials that have real weight.

## The tools

| Item | Use |
| --- | --- |
| **Lift** | The stand you build on. Turns what's on it into a single moving object. |
| **Chassis Frame** | The block a welded frame is built around. |
| **Welder** | Places points, joins them with tubes, and lays plates between them. |
| **Pivot Setter** | Turns a point into a hinge, a ball joint or a locked joint. |
| **[Engine](Engine.md)** | Power. Designed on the Engine Stand. |
| **Driver's Seat** | Sit in it; your keyboard becomes the car's controls. |

## The Lift

1. Place a **Lift** and build on top of it. Whatever rests on the lift, and only that, becomes the vehicle. **It mustn't touch the ground or anything else**, or the lift will complain *"Too big -- is it touching something besides the lift?"*
2. **Right-click** the lift. Everything on it becomes one object, held still: *"Lifted."*
3. **Right-click** again to raise it a step, and **sneak-right-click** to lower it. It goes up to 10 steps.
4. **Sneak-right-click at the bottom** (step 0) to put the vehicle back into the world as ordinary blocks, where it stands: *"Back in the world as blocks."* Your frame is a frame again, ready to edit and lift anew. If anything is in the way you'll get *"Something is in the way..."* and nothing moves.
5. **Break the lift** to drop the vehicle into the world, where gravity takes over.

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

All of this works on a vehicle that's already lifted or moving, too. One exception: **adding or removing a hinge or ball joint** on a lifted vehicle only takes effect once you put it back as blocks and lift it again (changing a joint's kind works at once).

### Weight

Plates and tubes **weigh what their material weighs**, so a frame of iron plates is far heavier than one of wood. An [Engine](Engine.md) adds its own design weight.

You can stand on plates and walk into tubes, and looking at one outlines it. The vehicle itself bumps into the world a little more roughly than it looks -- its plates collide as if they were up to half a block thick -- so leave some ground clearance.

## Joints: the Pivot Setter

Look at a point and right-click:

- On a point of a frame: cycles **plain → hinge → ball → locked → plain**.
  - **Hinge** turns about one axis only. If one tube runs through the point, it turns around that tube, like a shaft. Otherwise it turns around the grid axis nearest to where you're looking.
  - **Ball** turns freely in every direction.
  - **Locked** is rigid, but you can switch it back. A locked joint doesn't split the frame: both sides move as one piece.
- On a point where **two frames are welded together**: cycles that joint between hinge, ball and locked.
- **Sneak-right-click**: toggles whether the bearing **carries rotation across** or **spins free**.

Use hinges and ball joints to make things like suspension arms. Weld the inner end to a block, set it to a hinge, and the arm swings. When the vehicle is lifted, each piece of the frame between hinges or ball joints becomes its own moving part, hanging on its joints; a piece welded straight to a block moves with the vehicle.

## Tires

A **hub** is a **hinge** point with a tire on it. Set the point to a hinge with the Pivot Setter, then **hold a block in your off-hand and right-click the hinge** again: *"Hub: Oak Planks tire, medium."*

- The block's **material** is the tire's: how well it **grips** (its friction) and how much it weighs. A block nobody has measured the grip of is refused: *"Nobody has measured how that grips."*
- Right-click again with the **same** block to step the size: **small, medium, large** (a quarter, three-eighths and half a block in radius). A bigger tire covers more ground per turn of the hub, like a taller gear, and is heavier to spin up.
- Cycling the hinge to anything else takes the tire off.

A tire is not a block or a body: the game casts rays down from the hub to find the ground, works out how much the tire slips, and pushes the car with as much force as its grip allows.

## Connecting to the drive

Weld a **tube** to a [Shaft](Shaft.md) so that it runs **straight out of the shaft, along its axis**. The frame is then coupled into that shaft's network. A tube at an angle doesn't couple.

From there the drive follows the frame:

- along **tubes**, as long as each joins the next in a straight line (a bent shaft is a crank, not a shaft);
- through any **joint set to carry rotation** (Pivot Setter, sneak-right-click), at any angle, like a universal joint;
- to a **hub**, where it turns the tire.

Each driven tire pushes the car, and pushes back on the shaft: the engine feels the car's weight, the hill and the grip of the tires as load. In the air the shaft spins free. Rolling downhill in neutral turns the shaft.

The car also pays **air drag**, from the silhouette of what you welded: the bigger its face to the wind, the more power it takes to go fast.

## The Driver's Seat

```
W
S
```

W is any Wool and S is any Wooden Slab.

Place it on the vehicle and right-click it to sit. **While you're seated, the keyboard is the seat's**: every key you press goes to the seat instead of the game (sneak, Escape, chat and the F-keys still work). Each key held **ramps from 0 to 1 over a second**, and falls back the same way when you let go: tap for a little, hold for full.

The seat drives nothing on its own. Link it to a [Controller](Controller.md):

1. Right-click the seat with a [Data Connector](Data_Connector.md).
2. On the [Programmer](Programming.md), link an *Input* card to it. Its second row says which key it reads (*"key: W"*); click it and press a key to change it.
3. Wire that to a *Switch* card linked to an [Engine](Engine.md). A number sets the engine's **throttle**.

Use *Math* cards in between to scale or combine keys. Steering and brakes are still to come.
