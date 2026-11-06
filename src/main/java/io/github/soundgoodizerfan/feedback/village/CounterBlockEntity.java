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
package io.github.soundgoodizerfan.feedback.village;

import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

/**
 * Hands what arrives to the nearest villager within {@link #REACH} blocks, as a delivery would, and pushes the answer out: emeralds into the
 * payout slots, goods the villager would not take into the return slots. It does not choose or read what it handles. A line with no sorting
 * upstream fills the return slots with rejects, and then the intake stalls: the jam. Spam of poor work pays little and saturates the villager's
 * bin for days; the ledger decides, not the Counter.
 *
 * <p>Slot layout of the exposed handler: 0 intake (insert only), 1..9 payout, 10..18 returns (extract only).
 */
public class CounterBlockEntity extends BlockEntity {

    /** Blocks within which a villager is found; the same reach a villager's own screen keeps. */
    public static final int REACH = 8;
    private static final int CYCLE_TICKS = 20;

    private final ItemStackHandler intake = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler payout = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler returns = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private boolean near;

    public CounterBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.COUNTER_ENTITY.get(), pos, state);
    }

    public IItemHandler handler() {
        return new CombinedInvWrapper(intake, payout, returns) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot == 0 ? super.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == 0 ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
            }
        };
    }

    public void tickServer() {
        if (level == null || level.getGameTime() % CYCLE_TICKS != 0)
            return;
        Villager v = level.getEntitiesOfClass(Villager.class, new AABB(worldPosition).inflate(REACH), x -> !x.isBaby()).stream()
                .min(java.util.Comparator.comparingDouble(x -> x.distanceToSqr(worldPosition.getCenter()))).orElse(null);
        near = v != null;
        ItemStack work = intake.getStackInSlot(0);
        if (v == null || work.isEmpty() || full(payout) || full(returns))
            return;
        VillageEconomy.Handed done = VillageEconomy.handOver(v, work);
        if (done.fate() == VillageEconomy.Fate.TAKEN) {
            if (done.emeralds() > 0)
                ItemHandlerHelper.insertItem(payout, new ItemStack(Items.EMERALD, done.emeralds()), false);
            setChanged();
        } else {
            // Not taken: the whole stack goes back out the return slots.
            ItemStack back = work.copy();
            intake.setStackInSlot(0, ItemStack.EMPTY);
            ItemStack left = ItemHandlerHelper.insertItem(returns, back, false);
            if (!left.isEmpty())
                intake.setStackInSlot(0, left);
        }
    }

    private static boolean full(ItemStackHandler h) {
        for (int i = 0; i < h.getSlots(); i++)
            if (h.getStackInSlot(i).isEmpty())
                return false;
        return true;
    }

    public Component report() {
        return Component.translatable(near ? "feedback.counter.near" : "feedback.counter.alone");
    }

    public void dropAll() {
        if (level == null)
            return;
        for (ItemStackHandler h : new ItemStackHandler[]{intake, payout, returns})
            for (int i = 0; i < h.getSlots(); i++)
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), h.getStackInSlot(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Intake", intake.serializeNBT(registries));
        tag.put("Payout", payout.serializeNBT(registries));
        tag.put("Returns", returns.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        intake.deserializeNBT(registries, tag.getCompound("Intake"));
        payout.deserializeNBT(registries, tag.getCompound("Payout"));
        returns.deserializeNBT(registries, tag.getCompound("Returns"));
    }
}
