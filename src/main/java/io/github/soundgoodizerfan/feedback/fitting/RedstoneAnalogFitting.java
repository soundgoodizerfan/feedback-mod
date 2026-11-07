// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The sided redstone fitting (author ruling, 2026-11-05): fitted on one face of a machine that is {@link Analog}, it sets the machine's
 * first channel from the redstone power arriving at that face, power over 15: 15 is full, 0 is none, the rest evenly between. Stateless
 * beyond being there; the holder's block calls {@link #apply} when a neighbour changes and the item calls it on fitting.
 */
public final class RedstoneAnalogFitting implements SidedFitting {

    private final BlockEntity holder;
    private final Direction side;

    public RedstoneAnalogFitting(BlockEntity holder, Direction side) {
        this.holder = holder;
        this.side = side;
    }

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.ANALOG_ADAPTER.get());
    }

    /** Taken off, the machine goes back to full: nothing is left holding it at a level it can no longer be told to leave. */
    @Override
    public void onRemoved() {
        if (holder instanceof Analog analog && holder.getLevel() != null && !holder.getLevel().isClientSide)
            analog.setLevel(0, 1f);
    }

    /** Sets every analog machine at {@code pos} from the redstone at its fitted faces. Server side; a machine with no fitting is left alone. */
    public static void apply(Level level, BlockPos pos) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof Fittable fittable) || !(level.getBlockEntity(pos) instanceof Analog analog))
            return;
        for (Direction d : Direction.values())
            if (fittable.getSidedFitting(d) instanceof RedstoneAnalogFitting)
                analog.setLevel(0, Math.min(15, level.getSignal(pos.relative(d), d)) / 15f);
    }
}
