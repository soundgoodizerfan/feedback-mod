/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.NotNull;

/**
 * A machine's one workpiece slot, as something a hopper can reach into -- the Hammer's anvil
 * first, now every {@link WorkpieceHolder}.
 *
 * <h2>Why automating this does not automate completion</h2>
 * A hopper under the hammer takes whatever is lying on the anvil. It cannot ask whether the
 * workpiece is finished, because nothing in the mod can: the hammer does not know what it is
 * making and the item does not announce that it has arrived. So an extractor is not a completion
 * sensor bolted to the machine -- it is a second machine running at its own rate, and the player's
 * problem becomes the race between how fast work goes in and how fast product comes out.
 * <p>
 * That is the whole design working rather than a hole in it. Pull too eagerly and you bank
 * half-worked ingots; pull too late and the plate is foil. Timing the extraction is the same
 * decision as timing the drive, which is what the Clutch and the Timer are for.
 *
 * <h2>Anything may be placed on it</h2>
 * {@link #isItemValid} accepts everything, deliberately. A hammer that refused items it had no
 * process for would be checking identity (§1) and would quietly be a recipe list. It accepts
 * whatever it is handed and hits it; whether anything happens to it is the material's business.
 */
public class WorkpieceItemHandler implements IItemHandler {

    private final WorkpieceHolder hammer;

    public WorkpieceItemHandler(WorkpieceHolder hammer) {
        this.hammer = hammer;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return hammer.getWorkpiece();
    }

    /** One item at a time, and only onto a clear anvil. A hammer strikes one thing. */
    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !hammer.getWorkpiece().isEmpty())
            return stack;

        ItemStack remainder = stack.copy();
        ItemStack single = remainder.split(1);
        if (!simulate)
            hammer.insert(single);
        return remainder;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack workpiece = hammer.getWorkpiece();
        if (amount <= 0 || workpiece.isEmpty())
            return ItemStack.EMPTY;
        return simulate ? workpiece.copy() : hammer.removeWorkpiece();
    }

    /**
     * One, whatever the item says it stacks to. Two ingots on an anvil are not a workpiece, and a
     * stack of them would have to share a single {@code Fu} figure -- which would silently make
     * the overrun chain operate on a stack at a time.
     */
    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }
}
