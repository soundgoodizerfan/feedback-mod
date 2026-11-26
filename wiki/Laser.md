# Laser

A pumped-crystal laser. A crystal that lases sits inside it; powered, it measures the distance to the first solid thing in front of it, and switched on it fires a beam that burns living things and cuts blocks.

## Crafting

```
P L P
T W T
P P P
```

P is a Steel Plate, L is an Optical Glass Lens, T is Glass Tubing and W is Copper Fine Wire. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 4 screws. It needs Electrical Treatise IV: Lasers ([Treatise](Treatise.md)).

The lens is the beam's exit, the tubes are the flash lamps that pump the crystal, and the wire is the pump's supply. It takes no circuit: it has no figure for one to set.

## Using it

- **Place it facing the way you look.** The beam leaves the front face.
- **Power it.** The laser is a sink on an [MV](Electricity.md#bands-and-amps) line. It draws a small standing current while it ranges and a full current while it fires. A line of a higher band cracks the crystal at once.
- **Seat a crystal.** Right-click with a material that lases: ruby, or any other with the phosphor property. The one already there comes back. Sneak and right-click with an empty hand takes it out.
- **Switch it on.** A [Controller](Controller.md) Switch card fires it, or a redstone signal beside it. Without either it only ranges.
- **Right-click** empty-handed to read it: *no crystal*, *ranging*, *firing*, *firing hot* or *cooling*.

## Ranging

Powered with a crystal and switched off, the laser sends a weak beam and reads the distance to the first solid block in front of it, out to about two dozen blocks. A linked Controller reads three channels from it: *Beam range* (the distance in blocks, 0 with nothing in reach), *Crystal heat* and *Cut progress* ([Programming](Programming.md)). A [robot](Robots.md#senses) or a [Radio Mast](Radio_Mast.md) reads the same beam range from a powered Laser in the block ahead of the robot.

## Firing

A beam puts energy into the block it hits, and a clearer crystal puts in more. When the block has taken as much as its hardness is worth, it breaks and drops what it drops: soft stone goes in seconds, obsidian takes minutes, and a block that cannot be broken stays. A block breaks only where the world lets blasts break blocks. Moving the beam to another block starts that block's count from nothing.

Anything living in the beam takes fire damage and burns.

A made part lying in the beam while it fires is marked: its axes are stamped exact, as [Calipers](Instruments.md#calipers) would stamp them by hand, with a spark where it happens. Marking needs no instrument in your inventory.

## The crystal

The crystal turns the share of the pump it does not emit into heat, more the less clear it is, and sheds that heat only while the beam is off. Fired too long, it cracks and is lost, with a sound. The housing is never harmed. A hard cut is therefore made in pulses: fire, let it cool, fire again; the energy already put into a block stays while the beam rests. A clearer crystal runs longer before it cracks.

The beam is drawn in the color of the crystal's material.
