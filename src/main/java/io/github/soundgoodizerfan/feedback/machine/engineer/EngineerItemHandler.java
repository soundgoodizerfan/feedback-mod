// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.engineer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Lets hoppers and pipes feed an Auto Engineer and empty it: slots 0-8 are the parts bay (insert only, anything), slot 9 the output
 * (extract only) and slots 10-12 the fastener stock (insert only, fasteners only). Tools are seated by hand. It leaks no completion: an
 * extractor takes whatever sits in the output and cannot ask whether an assembly is finished.
 */
public class EngineerItemHandler implements IItemHandler {

    private static final int OUT = AutoEngineerBlockEntity.BAY, STOCK = OUT + 1;

    private final AutoEngineerBlockEntity engineer;

    public EngineerItemHandler(AutoEngineerBlockEntity engineer) {
        this.engineer = engineer;
    }

    @Override
    public int getSlots() {
        return STOCK + AutoEngineerBlockEntity.FASTENERS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot < OUT ? engineer.getBay().get(slot) : slot == OUT ? engineer.getOutput() : engineer.getFasteners().get(slot - STOCK);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return slot < OUT ? engineer.insertPart(slot, stack, simulate) : slot == OUT ? stack : engineer.insertFastener(slot - STOCK, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot == OUT ? engineer.takeOutput(amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot < OUT || slot > OUT && io.github.soundgoodizerfan.feedback.modular.engineering.Fasteners.is(stack);
    }
}
