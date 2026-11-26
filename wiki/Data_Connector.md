# Data Connector

The tool that links sensors and switchable blocks to a [Controller](Controller.md), so the Controller's [program](Programming.md) can read and drive them.

> **Recipe:** a copper wire on top of two iron rods, in a column.

## Using it

Hold the connector in either hand and every block that is a **data node** shows a small socket with a bright core, outlined through walls. Linked nodes take the color of their link; a node nothing has linked is gray-blue and blinks. A spark runs along each link.

Every link is also drawn in the world whether or not you hold the connector: a thin wire hangs between the two sockets and sags more the longer the link is. It is bare copper when the link has no color and takes the dye's color when the link was colored. Walls hide it, and it is drawn within 48 blocks of you. Cutting a link removes its wire.

A data node is any block that offers a reading or can be switched: a [Temperature Sensor](Instruments.md#temperature-sensor) or [Current Sensor](Instruments.md#current-sensor) face, a crucible or other vessel, a heater, a [Lathe](Lathe.md), a [Clutch](Clutch.md), [Valve](Valve.md), [Damper](Damper.md), [Breaker](Breaker.md), [Engine](Engine.md), [Thruster](Thruster.md), a [Driver's Seat](Vehicles.md#the-drivers-seat), an [Optical Terminal](Optical_Terminal.md), a [Warehouse Dock](Warehouse_Dock.md), a [Catalog Desk](Catalog_Desk.md), and the Controllers themselves.

1. **Right-click** the node. You see *"Node selected."* For a sensor attached to a face, click **the face the sensor is on**.
2. **Right-click** the [Controller](Controller.md), [Electric Controller](Electric_Controller.md) or [Computer](Electric_Controller.md#computer). The node is linked to it.

The Controller's program finds the node by what it is, not where it is: see [Programming](Programming.md#the-cards). A Controller binds **8** nodes, and **4** more for each [Card Rack](Controller.md#memory-and-upgrades) fitted.

To color the link, hold a **dye in the other hand** while you make the second click: the link takes that dye's color (16 dyes). With nothing in the other hand the link has no color. A card can ask for a link color, so colors sort several nodes of one kind into groups.

Look at any node with Jade and the tooltip lists its kinds as *Data kind: ...*, one line each, and *Link color: ...* when a Controller linked it with a dye. This shows which card a node answers to without opening the Programmer.

On [finespace](Finespace.md), the connector picks the single joint voxel you aim at instead of the block. A card then reads or sets that voxel: a [Piston Joint](Joints.md#piston-joint)'s **Piston level**, a hinge's **Angle** or **Brake** (see [Rotary Bearing](Rotary_Bearing.md#roles)), a [Clutch](Clutch.md#fine-clutch)'s **Engaged**. With the connector in hand, the voxels a card can set (pistons, hinges, clutches) are outlined in the block.

## Modes

The connector has four modes. Sneak and scroll with it in your main hand to change mode; a strip under the crosshair lists the modes with the current one lit. Changing mode drops a half-made selection.

| Mode | Use on a node |
| --- | --- |
| Link nodes | Select the node, then a second one to join them with wire. This is the mode in effect until you change it. |
| Cut a link | Select one node, then a second that is linked to it: the wire between them is cut. If they are not linked, the bar says "Those aren't linked". |
| Cut every link on a node | Cuts every wire the node carries at once. The bar says "All links cut", or "Nothing connected" when the node had none. |
| Color a link | Select the Controller, then a node linked to it, with a dye in the other hand: the link takes that color. With no dye the color is cleared. The bar says "Link colored", or "Select the Controller, then a node linked to it". |

A cut wire is not refunded.

## Linking nodes

A Controller reaches a node only through a path of linked nodes. Right-click the first node, then right-click the second: the two are linked. After the first click a chip left of the crosshair says *"Linking from"* and the node's name until the link is made, fails or is cancelled. A chain of links works as a path, so a sensor can reach a Controller through other linked nodes. A node reached through another node has no link color.

While you hold the connector in your main hand after selecting the first node, a wire stretches from that node to wherever you aim. It is blue while it hangs in the air, turns green when it snaps to a node under your crosshair, and turns red when the distance is longer than the longest wire reaches.

Each link costs fine wire from your inventory, one piece per block of distance. Creative players pay nothing. The wire is taken from the stack you carry when the link is made, and the link remembers its grade. If you carry too little, the bar message says how much wire the link needs; if the distance is beyond every wire, it says no wire reaches that far.

| Wire | Reach | Reading it carries |
| --- | --- | --- |
| Copper Fine Wire (poor grade) | up to 8 blocks | rounded to one significant figure |
| Redstone-Chromium Fine Wire (good grade) | up to 32 blocks | exact |

A link uses copper wire when it reaches and you have enough, otherwise redstone-chromium. A path is as good as its worst wire. A live [endernet](Data_Port.md#endernet) pair counts as one hop of the best grade, so a path can cross any distance through it.

**Longer links.** A link that ends at a block holding a [circuit](Basic_Electronic_Circuit.md) with gain reaches further: each point of gain adds the wire's plain reach again, and the better of the two ends counts. The blocks that take a circuit are the [Controller](Controller.md#circuit-slot), [Electric Controller](Electric_Controller.md#circuit-slot), [Computer](Electric_Controller.md#circuit-slot), [Data Port](Data_Port.md#circuit), [Optical Terminal](Optical_Terminal.md#circuit), [Power Cutter](Power_Cutter.md#circuit), [Auto Engineer](Auto_Engineer.md#circuit) and [Electrolytic Cell](Electrolytic_Cell.md#circuit). Gain comes from a [Vacuum Tube](Vacuum_Tube.md) on the board and reads at its warmth, so a cold tube gives none. The wire still costs one piece per block, and the grade and quenching rules are unchanged. Gain lengthens a [fiber](#fiber) the same way.

Redstone-chromium wire quenches: if either end of a good line carries more than 3 links, the line goes dark for 10 seconds and then recovers undamaged. A reader with no working path reads 0.

**Cold raises the limit.** A line laid in a dimension colder than the room carries more links before it quenches: the limit grows with every Tu the ambient sits below room temperature, up to three times the plain figure. A line on the frozen moon carries about two and a half times what the same wire carries at home.

## Fiber

Two [Optical Terminals](Optical_Terminal.md) are joined only by **fiber**, never by wire. Link them as any two nodes: select one terminal, then the other. Fiber is the *Fiber* form of a glass or polymer, [drawn from a glass rod](Wire_Drawer.md#what-it-makes) at the Wire Drawer. Nodes reach a terminal with ordinary wire.

A fiber link costs one piece of fiber per block of distance, taken from your inventory (creative players pay nothing). Of the fibers you carry, the connector spends the poorest one that still reaches the whole distance cleanly, and the best one you carry when none does. The link is refused only when even that fiber would be lost; the bar says no fiber you carry reaches that far, or how much a link that long needs. It also refuses when an end has no crystal seated.

Every fiber loses some light per meter, and a block is a meter. The terminals' crystals and circuits set a loss budget; the **clean reach** is that budget divided by the fiber's loss, up to 512 blocks. See the [Optical Terminal](Optical_Terminal.md#how-far-it-reaches) for the budget and [Materials](Materials.md#glass-fiber) for each glass's loss.

| Distance | Reading it carries |
| --- | --- |
| Within the clean reach | exact |
| Up to twice the clean reach | rounded to two significant figures |
| Up to eight times the clean reach | rounded to one significant figure |
| Beyond | lost: the link goes dark |

Fiber does not quench: a fiber line carries any number of links without going dark.

The mechanical [Controller](Controller.md) is a data node like the others and is linked with wire in the same way.
