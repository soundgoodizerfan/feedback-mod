# Launcher

The launcher is a gun that drives a **shell** or a **rocket** with work stored in its **seat** instead of burning powder. It is built from parts like any [gun](Guns.md), but it takes only powderless rounds, and a [firearm](Guns.md) refuses them.

## Parts

Shaped at the bench that suits the stock ([Smithing Table](Smithing_Table.md) for metal, [Woodworking Table](Woodworking_Table.md) for wood) and assembled at the [Engineering Table](Engineering_Table.md). A **Blueprint** (draw one at the [Blueprint Table](Blueprint_Table.md), under Weapons) is optional; drawing it and assembling it take Weapons Treatise II, see [Treatise](Treatise.md). The parts:

| Part | Takes | Does |
| --- | --- | --- |
| **Spring Gun Receiver** | any material | The frame. It rates how much the seat may push. |
| **Spring Gun Barrel** | a metal | Its bore (5 to 15 mm) and its length set how well the push turns into speed: a longer barrel keeps the push going; a wide bore wastes it. |
| **Piston** | a metal | Moves with the push; a heavy one steals some. |
| **Launcher Seat** | wire, rod, ingot or plate of a metal (one to six) | Stores the work: its volume times its metal's stiffness. Its variant is how it stores it. |

## Seats

The seat comes in four variants. All of them act through the reload key, which also loads a round of the gun's bore from your pack when the chamber is empty.

| Seat | Fill it | What a shot does | Misuse |
| --- | --- | --- | --- |
| **Spring Seat** | The reload key **cocks** it. The time is the spring's energy over your strength. A cocked gun stays cocked and the tooltip says so. | Lets the spring go; it must be cocked again. | A spring stronger than the receiver is rated for takes a **set** each cocking (it stores less afterwards) and wears the gun. Far past the rating it **snaps**, wrecking part of the gun, hurting your hand and wearing it a great deal. |
| **Reservoir Seat** | The reload key **pumps** it. The tooltip shows the pressure as a percentage. | Spends about a third of the pressure, so the first shots are the hardest. | Pumping past 100% weeps and wears the gun. Past 125% the walls **burst**, hurting you. |
| **Solenoid Seat** | **Right-click** a [Battery](Battery.md) or [Leyden Jar](Leyden_Jar.md) onto the gun, as on an [Electric Gun](Electric_Gun.md); right-click with an empty cursor to take it out. | Draws the shot's work from the cell. A jar gives one hard push; a battery a steady, lighter one. | With no charged cell it does not fire. |
| **Steam Seat** | The reload key fills the boiler from a **water bucket** in your pack (the empty bucket comes back). Heat it by stacking it with a hot item: see [Merging heated stacks](Heat.md#merging-heated-stacks). | Pushes by its steam pressure, and each shot spends water and cools the boiler. | At or below the boiling point of water (100 Tu) or with no water it does not fire; the push reaches its full size at about 180 Tu. |

## Using it

**Left click** fires. The spring seat fires one shot a cocking; the other seats can also fire automatically, and the **Gun option** key (**V**) cycles the fire mode (see [Options](Guns.md#options)). A [magazine](Magazine.md) of the gun's bore in your pack is loaded by the reload key when the chamber is empty, and the gun then feeds from it. The tooltip shows the rounds loaded and the seat's state: cocked, pressure, boiler water or cell charge.

A launcher takes shells and rockets of its bore (within 1 mm) and refuses powder rounds. The shell flies like an arrow, nearly silent, and cannot be picked up. A denser shell hits harder. The seat's work is shared with the piston, so a lighter piston wastes less.

## Rounds

Both are made at the [Reloading Press](Reloading_Press.md).

- **Shell.** Leave both charge slots empty. The case slot takes the body of the shell and the bullet slot the projectile. The payload goes in as on any round; an explosive filler makes the shell go up where it lands. A shell of [shot](Ammunition.md#shot-shells) with a fuse is a **cluster** that bursts into pellets.
- **Rocket.** Put grain in the charge slots and leave the case slot empty. The grain burns in flight and the rocket carries on under its own thrust: more grain and a slower-burning blend give a longer, harder push. The tube only ejects it, so any seat with a little work fires it.

The press's **fuse** slot takes a **Fuse** made at the part bench. Each kind sets when the shell bursts:

- A **wick** (short, standard or long) bursts in the air when its time is up.
- A **striker** bursts on the surface that stops the shell.
- An **inertia fuse** bursts on any hit, a body the shell passes through as well as a surface; its delay is the time it takes to arm.
- A **proximity fuse** arms after its delay, then bursts when a body or a block comes near its path.

A shell with no fuse bursts on its first hit.

## Fuse control

A finished, live [circuit](Basic_Electronic_Circuit.md) clipped onto the launcher is its **fuse control**. Without one the round keeps the fuse it was pressed with. With one, **sneak and tap the Gun option key** (**V**) to step what fuses the round, shown on the HUD as *Fuse*:

- **round**: the round's own fuse.
- **proximity**: the round bursts when a body or block comes near its path. The sensing radius widens with the circuit's gain and wavers with its noise.
- **short count** and **long count**: the round bursts after a count of steps of the circuit's clock, so a slow circuit gives a long fuse. The count is held between about a third of a second and 8 seconds.

A launcher has no heater, so a [tube](Vacuum_Tube.md) in the circuit reads cold.

## Attunements

The seat takes **Spring** (stores more energy for the same size) and its mirror **Snap** (a spring is cocked in less time); see [the gun table](Enchanting_Table.md#on-a-gun). **Ward**, **Mend** and **Tithe** work on the parts as on any gun.

A worn launcher shoots slower and softer and spreads wider, as a worn [powder gun](Guns.md#wear) does.
