// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.piston;

import io.github.soundgoodizerfan.feedback.core.fluid.FluidNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Pistons are pneumatic. Redstone is signal only (too weak to move anything), so a signal alone does not extend a piston: the stroke
 * also needs {@link #AIR_MB_PER_STROKE} of gas from a fluid handler touching the piston on any face but its front (a gas tank, a
 * pressure vessel, a compressor, a pipe), which the stroke uses up. The signal says when, the air does the work. Retracting costs nothing
 * (the spring returns it). No signal and no air means no move: a piston whose stroke is refused stays as it is until the next signal.
 * Acts on vanilla's pistons through NeoForge's cancellable {@link PistonEvent.Pre}, so no block is replaced.
 */
public final class PneumaticPistons {

    /** Gas one extension takes. */
    public static final int AIR_MB_PER_STROKE = 50;

    private PneumaticPistons() {
    }

    public static void onPistonPre(PistonEvent.Pre event) {
        if (!event.getPistonMoveType().isExtend || !(event.getLevel() instanceof Level level) || level.isClientSide)
            return;
        if (!(level.getBlockState(event.getPos()).getBlock() instanceof PistonBaseBlock))
            return;
        if (!draw(level, event.getPos(), event.getDirection()))
            event.setCanceled(true);
    }

    /** Takes one stroke's gas from a handler touching the piston, never its front; false when none has it. */
    private static boolean draw(Level level, BlockPos pos, Direction front) {
        for (Direction side : Direction.values()) {
            if (side == front)
                continue;
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(side), side.getOpposite());
            if (handler == null)
                continue;
            FluidStack probe = handler.drain(AIR_MB_PER_STROKE, IFluidHandler.FluidAction.SIMULATE);
            if (probe.getAmount() >= AIR_MB_PER_STROKE && FluidNetwork.isGas(probe)) {
                handler.drain(probe.copyWithAmount(AIR_MB_PER_STROKE), IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }
        return false;
    }
}
