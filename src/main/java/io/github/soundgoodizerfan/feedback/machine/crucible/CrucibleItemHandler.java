// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.crucible;


import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Lets vanilla hoppers feed and empty a crucible.
 *
 * <h2>It leaks no completion detection, and that is checked rather than hoped</h2>
 * The same argument as the hammer's anvil. An extractor takes whatever is in the vessel, finished
 * or not -- a hopper under a crucible is a second machine racing the first, not a sensor that
 * knows when the steel is ready. That keeps the mod's central claim intact: nothing in it can tell
 * you a process is complete, and building something that appears to is the player's problem to
 * solve with instruments.
 * <p>
 * What a hopper <em>does</em> buy is real and worth having: the player stops standing there. It
 * buys attention, not capability, which is the trade the whole slice is about.
 */
public class CrucibleItemHandler implements IItemHandler {

    private final CrucibleBlockEntity crucible;

    public CrucibleItemHandler(CrucibleBlockEntity crucible) {
        this.crucible = crucible;
    }

    @Override
    public int getSlots() {
        return crucible.getContents().size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return crucible.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !crucible.getItem(slot).isEmpty())
            return stack;
        if (simulate) {
            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            return remainder;
        }
        ItemStack offered = stack.copy();
        crucible.insert(offered);
        return offered;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || crucible.getItem(slot).isEmpty())
            return ItemStack.EMPTY;
        if (simulate)
            return crucible.getItem(slot).copy();
        return crucible.removeItem(slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
