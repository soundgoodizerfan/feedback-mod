// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cutter;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Lets hoppers and pipes feed a Power Cutter and empty it: slot 0 is the input buffer (insert only), slot 1 the output (extract only).
 * It leaks no completion: an extractor takes whatever sits in the output and cannot ask whether a cut is finished, the same argument as
 * {@code CrucibleItemHandler}. The cutter takes any item, since it knows no recipe; what it cannot cut is voided.
 */
public class CutterItemHandler implements IItemHandler {

    private final PowerCutterBlockEntity cutter;

    public CutterItemHandler(PowerCutterBlockEntity cutter) {
        this.cutter = cutter;
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? cutter.getInput() : cutter.getOutput();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return slot == 0 ? cutter.insertInput(stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot == 1 ? cutter.takeOutput(amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0;
    }
}
