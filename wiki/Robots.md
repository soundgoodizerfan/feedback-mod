# Robots

A small battery-powered crawler you build from three parts. It rolls, digs, places blocks and moves items by itself, following a program you load from a Punch Card.

## Crafting

A **Robot** is made from three parts ([Composed Parts](Composed_Parts.md)), each shaped from plate stock at a part bench. Lay them in a column in a crafting grid, chassis on top:

```
 C 
 D 
 T 
```

C is a Robot Chassis, D is a Robot Drive, T is a Robot Tool Head. A robot takes Robotics Treatise II: Robots ([Treatise](Treatise.md)) held, to draw it and to assemble it.

- **Robot Chassis**: the frame. It carries the drive, the battery and the program, and sets how much the robot weighs.
- **Robot Drive**: what moves it. A lighter robot on the same drive moves faster. It comes in four kinds, see [Drives](#drives).
- **Robot Tool Head**: the working end. Its material decides what the robot can dig and how fast.

Three optional parts go below the tool head in the column, in this order: a **Robot Sensor**, a **Robot Chunk Loader** and a **Robot Radio** (see [Sensors and the chunk loader](#sensors-and-the-chunk-loader) and [The radio](#the-radio)). A robot may carry one of each, and may leave any out.

```
 C 
 D 
 T 
 S 
 L 
 R 
```

S is a Robot Sensor, L is a Robot Chunk Loader, R is a Robot Radio.

A robot may also carry an electronic circuit as its **brain** (see [The brain](#the-brain)).

The [Blueprint Table](Blueprint_Table.md#robots-respirators-backpacks-and-beds) shows a robot drawing's speed, the hardest block its tool head breaks, and, for a wheeled or legged robot, how high a ledge it steps over, or for a rotor robot, how much mass the rotor can still lift.

## Using it

- **Right-click a block** with the Robot item to set it down as a robot standing there.
- **Sneak and right-click with an empty hand** to pick it up. It keeps its battery, program and cargo.
- **Right-click with a [Battery](Battery.md) item** to fit one. Without a battery the robot has no power and does nothing.
- **Right-click with a Punch Card** to load a program. It is an ordinary [Controller](Controller.md) program written at the [Programmer](Programming.md). A card already loaded comes back to you.
- **Sneak and right-click with a Punch Card** to take the loaded program back out into your inventory. The robot keeps nothing loaded.
- **Right-click with an empty hand** to read its charge and whether a program is loaded.

The robot holds nine slots of cargo.

Use a [Tether Cable](Tether_Reel.md) on a robot to tie it to a reel. A robot set down where its chunk or dimension already holds the most robots the server allows is refused, with a message in the action bar; see [Server limits](#server-limits).

## Drives

- **Wheels**: roll over flat ground and climb a slab.
- **Legs**: step up a full block, and are slower than wheels.
- **Rotor**: lifts the robot off the ground if it is light enough, and is slower than wheels. It draws power steadily to hover. A robot too heavy for its rotor stays on the ground. When the power dies the robot falls.
- **Gas cell**: floats on a gas cell, slower than a rotor but lifting far more. It holds its height with no power, and when the power dies the robot stays where it is.

## Appearance

A robot looks like the parts it is built from, whether it stands in the world, sits in a slot, lies dropped or is held in a hand. The chassis is the body. The drive sits under it: wheels, jointed legs, a spinning rotor on skids or a gas cell. The tool head rides on top with its jaw at the front. Each part takes the color of its own material, so a robot with a steel chassis and a copper tool head shows both.

A sensor shows by its kind: a probe is a thin rod reaching ahead of the head, a rangefinder is a pair of eyes, an inertial unit is a flat plate. A chunk loader is a hopper on the back. A radio is a mast topped with a ball.

## Sensors and the chunk loader

A **Robot Sensor** comes in three kinds; the probe is the basic one. Each gates the channels listed for it; without it those channels read zero.

- **Probe**: reads *Hardness ahead*.
- **Rangefinder**: reads *Distance ahead*, the blocks to the first solid thing in front, up to a short reach.
- **Inertial**: reads *Heading* and cuts the position error that wheels gather as they roll (see [Position](#position)).

A robot with no sensor still feels what is directly ahead through its other readings.

A **Robot Chunk Loader** keeps the chunk the robot stands in loaded while it runs a program, so it does not halt when you walk away. It drains the battery, and the server limits how many chunks may be held this way.

## The radio

A **Robot Radio** is the robot's end of a link to a [Radio Mast](Radio_Mast.md). Paired with a mast, the robot's senses can be read, and its actions held on or off, by an ordinary [Controller](Controller.md) at the mast, from as far as the mast's power reaches. See [Radio Mast](Radio_Mast.md) for pairing, range and noise.

## The brain

A live [circuit](Basic_Electronic_Circuit.md) seated in a robot when it is assembled at the [Engineering Table](Engineering_Table.md) is its brain. A robot with no circuit still works, only worse; nothing is refused. A [Robot Arm](Robots.md#robot-arm) takes a circuit by hand and uses it the same way.

- **Dexterity.** A faster, quieter circuit is better, and a cold joint halves it. Dexterity decides how steadily the hand lands when the robot uses a held item (see *Use held item ahead* below).
- **Heater.** A circuit with a [tube](Vacuum_Tube.md) draws its heater's current from the robot's battery, which is how a robot keeps it warm; a cold tube gives less speed and gain.

### Program size

A robot with no brain runs a program of **6 cards**. Each element on its circuit adds **5**, up to 40. A Punch Card that needs more cards than the brain allows is refused, and the robot says why.

### Senses

Gain stretches the reach of a rangefinder and of the beacon sense. The readings *Distance ahead* and *Hardness ahead* wobble with the brain's noise; a quiet circuit reads steadily and a robot with no brain reads as noisily as a circuit can.

### Remembered places

A [Lattice](Lattice.md) on the circuit gives the robot **places**: as many as the lattice has memory, up to 8. Turn *Remember this place* on and the robot stores where it is, by its own dead reckoning (see [Position](#position)), in the chosen place; *Choose next place* steps to the next place. Cards read the chosen place against where the robot stands: *East of* and *South of the chosen place*, *Blocks to the chosen place* and *At the chosen place*. A robot with no lattice remembers none.

### Endernet leash

An [Endernet Upgrade](Endernet_Upgrade.md) **used on the robot** fits it. While the robot's brain is live and its battery has charge, the robot is **leashed** to the [Data Port](Data_Port.md) that holds the other end of the bond: a [Controller](Controller.md) whose cards are linked to that port reads the robot's senses and drives its actions from any distance, the way a [Radio Mast](Radio_Mast.md) would. The leash drains the robot's battery steadily, and each signal that crosses spends [Liquid Teleportant](Liquid_Teleportant.md) and bandwidth from the port. The port offers the robot's first senses and actions: 6 of each, times the channels the circuit's [Resonator](Resonator.md) can separate. The [Network app](Electric_Controller.md#disk-apps) lists the port as a robot, and its **Flip** holds the robot's first action. With no live brain, no battery charge, an unloaded robot or an unpowered port there is no leash.

## Programming a robot

A robot's own program asks for its senses with an *Input* card, whose signal list holds them, and for its actions with a *Switch* card of kind *Robot*. Nothing needs linking: the cards read and drive the robot that carries the program. A Controller elsewhere reaches the robot through a [Radio Mast](Radio_Mast.md) or an [endernet leash](#endernet-leash).

Switches (a *Switch* card turns them on and off):

| Channel | What it does |
| --- | --- |
| Roll forward | Drives forward |
| Turn left | Turns the robot left |
| Turn right | Turns the robot right |
| Break ahead | Digs the block in front |
| Place ahead | Sets a block from cargo in front |
| Pick up ahead | Takes dropped items in front into cargo |
| Drop ahead | Puts cargo out in front |
| Put into container ahead | Moves the first stack of cargo into the container or machine in front |
| Take from container ahead | Takes the first stack the container in front lets out |
| Switch machine ahead | Holds a switchable machine in front engaged while on, released while off |
| Use held item ahead | Uses the first stack of cargo on the block in front, as a hand does, through that block's own use |
| Climb | Rises while on; only a flying robot (a rotor light enough to fly, or a gas cell) answers |
| Descend | Sinks while on |
| Remember this place | Stores where the robot is in the chosen place, as it comes on |
| Choose next place | Steps the chosen place to the next one, as it comes on |
| Claim chosen slot | Claims the chosen board slot and keeps the claim while on |
| Mark my slot done | Marks the robot's claimed slot done, as it comes on |
| Post a haul in chosen slot | Posts the chosen slot free, with the figure set to the cargo stacks carried, as it comes on |
| Choose next slot | Steps the chosen board slot to the next one, as it comes on |

Readings (an *Input* card reads them):

| Channel | Reads |
| --- | --- |
| Blocked ahead | Whether a solid block is in front |
| Breakable ahead | Whether the tool head can break the block in front |
| Items ahead | Whether dropped items lie in front |
| Cargo full | Whether all nine cargo slots are full |
| Battery charge | The charge left, as a share |
| Distance ahead | Blocks to the first solid thing in front (rangefinder) |
| Hardness ahead | How hard the block in front is (probe) |
| Heading | Which way it faces, in quarter turns from south (inertial unit) |
| Beacon near | Whether a [Robot Beacon](#robot-beacon) is within reach |
| Dock ahead | Whether a [Robot Dock](#robot-dock) or Tether Reel is the block in front |
| Position east | Blocks east of where it was set down, by dead reckoning |
| Position south | Blocks south of where it was set down, by dead reckoning |
| Height above start | Blocks above the block it was set down on |
| East of the chosen place | Blocks east of the chosen place, from where the robot stands |
| South of the chosen place | Blocks south of the chosen place |
| Blocks to the chosen place | Distance to the chosen place |
| At the chosen place | Whether the robot is within a block or so of it |
| Places remembered | How many places its brain can hold |
| Chosen board slot | Which [Job Board](Job_Board.md) slot is chosen |
| Chosen slot state | 0 free, 1 mine, 2 claimed by another, 3 done |
| Chosen slot figure | The figure held in the chosen slot |
| A job board hears me | Whether a board hears this robot |
| Dock ahead is free | Whether a dock or reel in front has live current and no robot on it |

### Fleets

A robot with a radio part shares work through a [Job Board](Job_Board.md). A program can read the chosen slot (*Chosen board slot*, *Chosen slot state*, *Chosen slot figure*), claim it with *Claim chosen slot*, mark it done, post a haul, or step to the next slot. A claim lasts only while *Claim* stays on. Without a radio part, or when no board hears it, every board channel reads 0. The chosen slot is kept when the robot is picked up.

*Dock ahead is free* tells a robot whether the dock or reel in front is powered and unoccupied, so a fleet queues for a charge instead of piling onto one pad.

### Using the held item

*Use held item ahead* lays food in a [Skillet](Skillet.md), flips it with a [Spatula](Hand_Tools.md#spatula), or does whatever a right-click with that item does on the block in front. A clumsy hand lands off the center of the face, so it may act on the wrong spot; a better brain lands closer to the center.

## How it works

- **Speed** falls as the robot gets heavier: a heavy frame or tool head slows it.
- **Digging** depends on the tool head's material. Its yield strength decides which blocks it can break and how fast. Breaking a block costs battery charge in proportion to the block's hardness.
- **Claims**: a robot claims the block it is breaking for the time it takes, so several robots on one face spread out instead of digging the same block.
- **A flat battery** stops the robot where it stands.
- **Damage** drops the robot as its item.
- A robot in an unloaded chunk halts, as any other creature does, unless it carries a chunk loader.

### Position

A robot tracks where it is by counting what it rolls. The error grows with every block, so *Position east* and *Position south* drift. A [Robot Beacon](#robot-beacon) within a few blocks wipes it.

### Charging

A [Robot Dock](#robot-dock) charges the robot standing on it, and a [Tether Reel](Tether_Reel.md) feeds a robot tied to it, both off an electrical line. A [Robot Arm](Robots.md#robot-arm) is a robot fixed in place and run off a line.

### Server limits

The server holds a limit on robots per chunk, robots per dimension and chunks kept loaded by robots, set in `feedback-server.toml`.

The same file sets a budget of program steps per tick (120 by default). When more robots are due a step than the budget allows, the extra ones wait and step in multiples, shown as *slowed x2* up to *x6* in Jade and in the empty-hand readout. A robot goes back to full speed when it gets its turn. Movement and work are not slowed.

## Robot Beacon

A landmark that corrects a robot's idea of where it is.

### Crafting

```
  G  
  W  
 P P P
```

G is Glass, W is [Copper Wire](Wire_Drawer.md) and P is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), holding Robotics Treatise II: Robots ([Treatise](Treatise.md)).

### How it works

A robot counts the blocks it rolls to know its position, and the error grows with every block. A robot within a few blocks of a beacon has that error wiped. A robot reads *Beacon near* while it is within reach. See [Position](#position).

## Robot Dock

A pad on an [electrical](Electricity.md) line that charges the robot standing on it.

### Crafting

```
 W P W
 P P P
```

W is [Copper Wire](Wire_Drawer.md) and P is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), holding Robotics Treatise II: Robots ([Treatise](Treatise.md)).

### Using it

Join the dock to an electrical line and stand a robot on it. The robot's [Battery](Battery.md) is charged the way a [Charger](Charger.md) charges one: the dock asks for the battery's charge current while the line is at its band or above. A robot with no battery takes nothing.

### Reading it

A robot reads *Dock ahead* when a dock is the block in front, so a program can drive onto it to recharge, and *Dock ahead is free* when that dock has live current and no robot on it. Jade shows a dock or reel as free or occupied. A [Tether Reel](Tether_Reel.md) charges a robot that stays away from the pad.

## Robot Arm

A robot fixed on a mount and run off an [electrical](Electricity.md) line instead of a battery. It works the space it faces.

### Crafting

```
 P W P
   P  
 P P P
```

W is [Copper Wire](Wire_Drawer.md) and P is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), holding Robotics Treatise II: Robots ([Treatise](Treatise.md)).

### Using it

It faces the way you looked when you placed it.

- **Right-click with a Punch Card** to load a program.
- **Sneak and right-click with a Punch Card** to take the loaded program back out into your inventory.
- **Right-click with a circuit** to seat it as the arm's brain. It gives the arm dexterity and sets its program size as on a [robot](#the-brain); an arm with no circuit is clumsy and runs a short program. A circuit already seated comes back to you.
- **Right-click with any other item** to put it in its cargo.
- **Sneak and right-click with an empty hand** to take the last stack out.

Breaking the block drops its cargo and its circuit.

### How it works

It breaks, places, picks up and drops, uses the first carried stack on the block ahead ([Using the held item](#using-the-held-item)), puts items into and takes them out of a container or machine, and switches a machine on or off, with the same channels and program as a robot. Turning swings it a quarter on its mount. It shares the readings *Blocked ahead*, *Breakable ahead*, *Items ahead*, *Cargo full* and *Dock ahead*. Its tool head has a fixed strength.

It acts only while its line feeds it.

## Tips and automation

Read *Breakable ahead* and *Battery charge* to decide when to dig and when to stop before the battery runs flat. Read *Dock ahead* to know when a charging point is in front, *Dock ahead is free* to know it is unoccupied, and *Beacon near* to know when the position has been corrected.
