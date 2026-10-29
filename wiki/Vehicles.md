# Vehicles

> Build a frame, put an engine on it, run tubes to tires, lift it and drop it: it drives. Most of the vehicle tools are creative-only.

Vehicles in Feedback are built the way you'd build a car in a garage: on a lift, around a welded frame, out of real materials that have real weight.

## The tools

| Item | Use |
| --- | --- |
| **Lift** | The stand you build on. Turns what's on it into a single moving object. |
| **Chassis Frame** | The block a welded frame is built around. |
| **Welder** | Places points, joins them with tubes, lays plates between them, and welds points to blocks. |
| **Pivot Setter** | Turns a point into a hinge, a ball joint or a locked joint. |
| **[Engine](Engine.md)** | Power. Designed on the Engine Stand. |
| **[Car Design Stand](Car_Design_Stand.md)** | Where engines and transmissions are designed. |
| **[Transmission](Transmission.md)** | Gears and a clutch. Designed on the Car Design Stand. |
| **[Thruster](Thruster.md)** | Pushes the whole body along its facing; burns liquid fuel. |
| **Driver's Seat** | Sit in it; your keyboard becomes the car's controls. |

## The Lift

1. Place a **Lift** and build on top of it. Whatever rests on the lift, and only that, becomes the vehicle -- including anything a frame on it is **welded** to, and whatever touches that, even if only a tube joins it to the rest. **It mustn't touch the ground or anything else**, or the lift will complain *"Too big -- is it touching something besides the lift?"*
2. **Right-click** the lift. Everything on it becomes one object, held still: *"Lifted."*
   The message also shows the vehicle's **total mass** in Mu and its **centre of mass**, as an offset from the middle of the lift's top face; it shows again each time you raise or lower the lift. If thrusters with fuel are fitted, it also gives how many times its own weight their upward push would lift.
   If something is wrong with the frame it says so after *"Lifted."*: chassis cells left out because a block stands where they would go, or bearings with nothing on their far side, which swing nothing. Frame cells left out while you edit a lifted frame are reported to players nearby.
3. **Right-click** again to raise it a step, and **sneak-right-click** to lower it. It goes up to 10 steps.
4. **Sneak-right-click at the bottom** (step 0) to put the vehicle back into the world as ordinary blocks, where it stands -- swinging parts and any frame joined to it included: *"Back in the world as blocks."* Your frame is a frame again, ready to edit and lift anew. If anything is in the way you'll get *"Something is in the way..."* and nothing moves.
5. **Break the lift** to drop the vehicle into the world, where gravity takes over. A [Wrench](Wrench.md) takes a lift apart quickly.

A lift that's part of a vehicle itself can't lift anything.

### Carrying a creation

Right-click any creation with a **Lift item** and the whole thing goes *into the item*, with every block and everything in every chest: *"Picked up (N blocks)."* Clicking any part of a vehicle -- a swinging arm included -- takes the whole vehicle. Two vehicles welded frame to frame can't be carried as one: cut the joint first. The tooltip shows how big it is. Right-click the ground to set it back down on a new lift, already held. If anything is in the way you'll get *"No room to put it down here,"* and nothing is placed.

## Building a frame

Place a **Chassis Frame** block. Everything you weld is measured from it, and it's what holds your frame to the vehicle. **Breaking the Chassis Frame block takes its whole frame with it**: every collision part it made and every swinging part, even on a vehicle that's lifted.

The **Welder** has seven modes: **Place points, Select, Connect, Plates, Weld, Extrude** and **Remove**. **Sneak + scroll** to switch between them. While you hold the Welder or the Pivot Setter, the corner of the screen lists what each click does in the current mode, and describes the point under your crosshair (see [Reading a frame](#reading-a-frame)).

Near a frame, the Welder and the Pivot Setter take every right-click for themselves, so a point in front of a block (an engine, a chest) is clicked instead of the block behind it. **Sneak-right-click** in any mode just lets go of whatever you're holding; it never removes anything.

### Place points

- Right-click a block face, or empty air, to place a **point** there. On a face, the point always sits just outside it, touching. In the air, it goes at the same distance as the point you're holding (or two blocks out).
- Place a point on the **end of something that turns** (the end of a [Shaft](Shaft.md), an [Engine](Engine.md)'s output) and it sets itself up as the start of a drive line: **welded to that block, and a hinge that carries rotation**. See [Connecting to the drive](#connecting-to-the-drive).
- Right-click a point to **select** it; right-click more points to add them, and a selected one again to take it out. Selected points are drawn cyan. **Sneak-right-click** clears the selection.
- Right-click the middle of an edge to split it with a new point.

### Moving points: the gizmo

With points selected in **Place points**, **Select** or **Extrude** mode, six arrows stand out from the middle of the selection: red along X, green along Y, blue along Z.

- **Click** an arrow to move the selection one 1/16 step that way. **Sneak-click** moves it a whole block.
- **Hold** right-click on an arrow and look along its axis to **drag**: the selection slides along the axis, snapping to the grid, with white markers where the points will land. Let go to put them there.
- Tubes and plates move with their points. If any point would land on another point or out of the frame's reach, nothing moves.

While you look at a point, or drag, **coloured lines** join it to every other point it lines up with -- red if they line up along X, green along Y, blue along Z -- so you can see when something is square.

### Select

Selects many points at once. Right-click in two places (a block face, or the air) to set the corners of a box: everything inside it is added to the selection, and the box is drawn as you aim the second corner. Right-clicking a point adds or removes just that one. The gizmo then moves the whole selection.

### Extrude

Select points (right-click them, as in Place points), then use the gizmo's arrows: instead of moving, they **pull out a copy** of every selected point, joined to its original. Where two selected points were joined, their copies are joined too, so pulling a square gives a box. With a block in your off-hand the joins are **tubes** of it. The copies become the selection, so you can keep pulling.

### Symmetry and locking

Right-click the **Chassis Frame block itself** with the Welder to open the frame's screen.

**Locked** keeps lifts off it: a lift refuses to lift anything with a locked frame in it (*"A chassis frame here is locked..."*), and a Lift item won't carry it. Use it for a frame built into a base, or a vehicle you don't want scooped up.

**Symmetry** is three switches, **Mirror across X, Y and Z**. Each turns on a mirror plane through the middle of the frame block, drawn in pink. With a plane on, **every edit is made at its mirror image too**: placing, moving, extruding, connecting, tubes, plates, welds, bearings, tires and removing. Build one side; the other appears. Mirrored tubes and plates use up blocks like any others.

### Connect points

- Right-click one point, then another, to join them with an **edge**. Do it again to remove the edge.
- Hold a **block in your off-hand** and the new edge becomes a **tube** of that material. Right-click a tube with a block to make it thicker (the thickest wraps back to thin).
- Join a new edge to a **hinge that carries rotation** and the plain point on the other end becomes one too, turning along the new edge. A drive line builds itself as you lay it out. A point that is already a bearing is left as it is.
- With a point selected, right-click a point on **another frame** to weld the two frames together. Do it again to cut the joint.

### Make plates

Hold a block in your off-hand and right-click three points in turn. The third click lays a **triangular plate** of that material between them, adding any missing edges. Right-click a closed triangle to re-plate it in a different block. Each plate or tube uses up one of the block.

### Weld

Right-click a point to **weld it to the block it's touching**. Right-click it again to unweld. If it touches two blocks (on an edge or a corner), it welds to the one behind it as you look. A point sitting inside a block welds to that block.

**Only welded points hold the frame to the vehicle.** Any part of the frame that isn't welded down (directly or through a joint) will simply fall off when the vehicle drops. Welded points are drawn blue, and looking at one outlines the block it's welded to.

### Remove

Only this mode removes anything. Right-click:

- a **selected point**: removes the **whole selection**
- a **point**: removes it and everything attached to it
- a **plate**: removes the plate, keeps the edges
- a **tube**: turns it back into a plain edge

All of this works on a vehicle that's already lifted or moving, too. One exception: **adding or removing a hinge or ball joint** on a lifted vehicle only takes effect once you put it back as blocks and lift it again (changing a joint's kind works at once).

### Weight

Plates and tubes **weigh what their material weighs**, so a frame of wrought iron plates is far heavier than one of wood. An [Engine](Engine.md) adds its own design weight.

You can stand on plates and walk into tubes, and looking at one outlines it. The vehicle itself bumps into the world a little more roughly than it looks -- its plates collide as if they were up to half a block thick -- so leave some ground clearance.

Parts that swing on a joint collide with the rest of the frame: an arm swung into the chassis stops against it. The space right around a joint doesn't collide, so the two sides can turn there.

## Joints: the Pivot Setter

Look at a point and right-click:

- On a point of a frame: cycles **plain → hinge → steered hinge → ball → locked → slider → plain**.
  - **Hinge** turns about one axis only. If one tube runs through the point, it turns around that tube, like a shaft. Otherwise it turns around the grid axis nearest to where you're looking.
  - **Steered hinge** is a hinge that always cuts the frame: the part beyond it (the knuckle) moves on its own, and the vehicle's steering level turns it, up to about 34° either way. See [Steering](#steering).
  - **Ball** turns freely in every direction.
  - **Slider** is a spring: the part beyond it slides along the point's axis, held at rest by a spring and a shock. How far it can travel depends on the **material of the strongest tube meeting the point** (a tougher metal travels further, a weaker one less). Hold a Pivot tool in each hand and sneak-right-click it to pick **soft**, **stock** or **stiff**. Pushed against the end of its travel for three quarters of a second, the spring **snaps** and the point becomes a loose ball joint; set it back to a slider to fit a new one.
  - **Locked** is rigid, but you can switch it back. A locked joint doesn't split the frame: both sides move as one piece.
  - A **hinge that carries rotation** doesn't split the frame either: its turning is the drive's, so as far as the frame goes it's rigid. A **ball that carries rotation** still swings, but can't twist around its own axis, like a universal joint.
- On a point where **two frames are welded together**: cycles that joint between hinge, ball and locked.
- **Sneak-right-click**: toggles whether the bearing **carries rotation across** or **spins free**.
- If the point is part of a Welder selection you built by **clicking points one by one**, whatever you do to it happens to every point in the selection (a box selection doesn't do this).

Use hinges and ball joints that **spin free** to make things like suspension arms. Weld the inner end to a block, set it to a hinge, and the arm swings. When the vehicle is lifted, each piece of the frame between free hinges or ball joints becomes its own moving part, hanging on its joints; a piece welded straight to a block moves with the vehicle.

If you change bearings on a vehicle that's already lifted, lower it back to blocks and lift it again to be sure the new layout takes.

## Tires

A **hub** is a **hinge** point with a tire on it. Set the point to a hinge with the Pivot Setter, then **hold a block in your off-hand and right-click the hinge** again: *"Hub: Oak Planks tire, medium."*

- The block's **material** is the tire's: how well it **grips** (its friction) and how much it weighs. A block nobody has measured the grip of is refused: *"Nobody has measured how that grips."*
- Right-click again with the **same** block to step the size: **small, medium, large** (a quarter, three-eighths and half a block in radius). A bigger tire covers more ground per turn of the hub, like a taller gear, and is heavier to spin up.
- Cycling the hinge to anything else takes the tire off.
- **Brake:** **sneak-right-click** the hub with a block in your off-hand to line its brake with that block's material (*"Hub: brake lined with Stone."*). Do it again with the same block to take it off. A pad holds with its material's friction, so a grippier lining holds harder.
  - A pad wears only from misuse: holding the brake on while the tire slides (the hold passing the tire's grip), or braking a driven hub against an engine that is trying to turn until it stalls on the pads. A worn pad holds less. Normal braking never wears it. Look at the frame with Jade to read the worst pad's condition.
  - A **free** hub brakes at the tread, against the road, before the tire's grip limit: a brake stronger than the tire's grip makes it **skid**, and a weak pad on a grippy tire just slows it.
  - A **driven** hub brakes against its shaft: a braked engine stalls on its pads, and the tires still grip as they always do.
- **Suspension:** each hub is **soft**, **stock** or **stiff**. Hold a Pivot tool in each hand and **sneak-right-click** the hub to cycle it (*"Hub: stiff suspension."*); a mirrored hub copies it. A soft hub squats further under the car and bounces slowly; a stiff one sits higher and tighter.

A tire is not a block or a body: the game casts rays from the hub to find the ground, works out how much the tire slips, and pushes the car with as much force as its grip allows. Rays around the rest of the rim stop the tire against walls, steps, other vehicles and your own vehicle's frame, so a wheel doesn't pass through things even at speed.

**What it's made of matters, both ways:**

- **Grip** comes from the tire's material. **Slime** grips best of anything, then honey and **synthetic rubber** (a [Polymers](Polymers.md) block); ice and blue ice barely grip at all.
- **Rolling** costs something too. A **soft** tire (slime, wool) flexes as it rolls and slows the car down; a **hard** one (iron, netherite) rolls on and on. So the grippiest tire is also the draggiest: pick for the job. **Blue ice** rolls best of all, and can hardly push the car.
- **The ground** matters as much: soft ground (dirt, grass) drags more than stone, **loose** ground (sand, gravel) more again, and **soul sand** is like real quicksand: you won't sink away, but a weak engine won't get you out. A road of blue ice is the fastest thing to coast on, and the hardest to stop on.

Tires turn on screen at the speed they're really turning, and so do the tubes of a drive line, so you can see at a glance what's driven and what isn't. Each tire has one darker segment so you can see it turn.

## Connecting to the drive

A drive line starts at a point welded to the **end of something that turns**: a [Shaft](Shaft.md), the output or back of an [Engine](Engine.md), any rotating block, along its axis. The easy way: place the point on the end of the shaft, and it welds itself and becomes a hinge that carries rotation.

- If that point **carries rotation** (as it does when placed that way), it's a universal joint on the block's face, and the first tube may leave at **any angle**.
- If it's a **plain weld**, it's rigid: the tube must run **straight out along the axis**, like a coupler joining two shafts. At an angle it would be a crank, and doesn't couple.
- A weld on the **side** of a shaft never couples.

It works before the vehicle is lifted too: on the bench, a coupled drive line turns when the engine runs. A frame can have **several drive lines**, one per weld, each on its own network: two engines can drive two sets of wheels.

From there the drive follows the frame:

- along **tubes**, as long as each joins the next in a straight line (a bent shaft is a crank, not a shaft);
- through any **joint set to carry rotation** (Pivot Setter, sneak-right-click), at any angle, like a universal joint;
- to a **hub**, where it turns the tire.

**Driving two wheels:** run an **axle** -- one straight line of tubes from wheel to wheel -- through a joint that carries rotation, and bring the drive into that joint from the side. The whole axle turns as one rod, so both wheels roll the same way. Splitting the drive into two tubes that fan out at an angle from one point does *not* work: each branch turns like its own universal joint, the two wheels spin opposite ways, and the car turns on the spot. Both wheels of an axle turn at the same speed, as one rod, unless you fit a **differential**: hold a Pivot tool in each hand and right-click a joint that carries rotation to step it through **none, open, limited slip and locked**. An **open** differential lets the two half-axles turn at different speeds, so a wheel on ice spins up while the other gets nothing. A **limited slip** one holds the halves near each other's speed. A **locked** one is a single rod again.

Each driven tire pushes the car, and pushes back on the shaft: the engine feels the car's weight, the hill and the grip of the tires as load. In the air the shaft spins free. Rolling downhill in neutral turns the shaft.

The car also pays **air drag**, from the silhouette of what you welded: the bigger its face to the wind, the more power it takes to go fast.

Every moving body is slowed by the air, in proportion to its speed **squared** and the dimension's **air density**: twice as fast is four times the drag. A craft pushed by [Thrusters](Thruster.md) tops out where its push equals the drag, about 20 blocks a second for one standard thruster and higher with more. The Nether's air is a little thicker than the Overworld's and the **End has none**: no drag there, and no air for a thruster to burn. A drive-line car drags by its welded silhouette (above) instead, and a body with a thruster is held to about 40 blocks a second so it cannot fly through walls.

## Reading a frame

- **Holding the Welder or the Pivot Setter**, the corner of the screen describes the point under your crosshair: whether it's welded, which bearing it is and whether it carries rotation, its tire, how many tubes meet there, and whether it's connected to a rotation network. If it is, you see the drive line's **RPM** and the **Su** it takes from the network (the road pushing back through the tires, plus its bearings' friction).
- **Looking at the frame block**, or any part of a lifted frame, [Jade](https://modrinth.com/mod/jade) says whether the frame is connected to anything turning. Wearing the **Debug Helmet**, it lists every drive line (where it couples, its RPM and Su) and every tire (driven or free, its RPM, the load it's carrying, its grip).

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

Use *Math* cards in between to scale or combine keys (two keys and a subtract make a steering level: `D - A`).

A card that points at a part of a vehicle (a frame, an engine, a seat) remembers it as an offset from the vehicle's frame, so the same program works wherever the vehicle stands, after it is lifted and after it is put back. The controller finds the frame nearest to itself, so keep it in the vehicle it runs.

### Steering

A [Frame](#building-a-frame) is a controllable part too. Link it as a Switch card's target the way you link an engine, and a number sets it: **channel 0 is steering** (−1 to 1), **channel 1 is the brake** (0 to 1). The brake works on every hub with a lining; see [Tires](#tires).

Steering is mechanical. Set the hinge at each front knuckle to a **steered hinge** with the Pivot Setter, and weld the tie rod between knuckles yourself: with none, each knuckle turns on its own. The steering is held by the driver's arm, so there's a limit to what it can turn: parked, a heavy car's tires scrub and steering is heavy; rolling, it's light. A steered hinge takes no tire: the hub is a hinge beyond it.

**Power steering** comes from the drive: while a coupled shaft is turning, the steering gets extra force at the knuckles, more as the shaft turns faster, and the shaft pays for it as load while the wheel is off centre. With the engine off, the assist is gone and steering is heavy again.
