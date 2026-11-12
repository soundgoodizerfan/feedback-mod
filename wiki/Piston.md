# Piston

A piston pushes blocks when it gets a redstone signal and air. Sticky pistons pull their block back as well.

## What vanilla does

A redstone signal on a piston or sticky piston pushes it out, and a sticky piston pulls its block back. The signal is all it needs.

## What Feedback does

Redstone only carries a **signal**; it is too weak to move anything. A piston needs the signal **and gas**: each stroke out uses a little gas drawn from a gas tank, pressure vessel, compressor or pipe that touches the piston on any face except the front, and the tank's gauge falls as it does. With a signal but no gas the piston does not move, and it tries again on the next signal. Pulling back uses no gas.

Sticky pistons stay sticky, and everything else about moving blocks is as in vanilla.

See [Gas Tank](Gas_Tank.md) and [Gas Compressor](Gas_Compressor.md) for gas.
