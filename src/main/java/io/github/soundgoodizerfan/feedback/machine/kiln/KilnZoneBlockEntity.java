// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.kiln;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.Failure;
import io.github.soundgoodizerfan.feedback.process.ProcessRun;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A tunnel kiln zone. Four car positions: preheat, fire, fire, cool, at {@link #ZONE_SHARE} of the heat above ambient. Ware enters the
 * first position from the back, rides forward one position every {@code STEP_TICKS} at 16 RPM (faster at more, never when stopped), and
 * leaves the last into whatever takes items at the front.
 *
 * <h2>The kiln knows no recipe</h2>
 * Each position runs the same thermal rows a vessel does, by the heat there: food by its cooking ladder, anything else by the one-input row
 * it matches, with the same holds and the same ruin. What it cannot do is melt (a row with a fluid result is not run). Dwell is the car's
 * speed: a slow car gives a clean result, a fast one leaves ware raw. A <em>stopped</em> car is the overrun: the ware sits in the fire and
 * keeps going past done to char, crack or ruin. Wear plays no part; the ware is what is spoiled.
 */
public class KilnZoneBlockEntity extends RotationNode implements Switchable, DataNode {

    /** Invented. Heat above ambient each of the four positions sees, as a share of the zone's. */
    public static final float[] ZONE_SHARE = {0.4f, 1f, 1f, 0.4f};
    /** Invented. Ticks per car step at 16 RPM. */
    public static final float STEP_TICKS = 200f;

    private final ItemStack[] car = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final float[] hold = new float[4];
    private float stepProgress;
    private boolean engaged = true;

    private final ItemStackHandler view = new ItemStackHandler(4) {
        @Override
        public ItemStack getStackInSlot(int slot) {
            return car[slot];
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            car[slot] = stack;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty())
                return stack;
            ItemStack held = car[0];
            if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, stack))
                return stack;
            int room = Math.min(16, stack.getMaxStackSize()) - held.getCount();
            int moved = Math.min(room, stack.getCount());
            if (moved <= 0)
                return stack;
            if (!simulate) {
                if (held.isEmpty())
                    car[0] = stack.copyWithCount(moved);
                else
                    held.grow(moved);
                setChanged();
            }
            return stack.copyWithCount(stack.getCount() - moved);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 3 || car[3].isEmpty())
                return ItemStack.EMPTY;
            int n = Math.min(amount, car[3].getCount());
            ItemStack out = car[3].copyWithCount(n);
            if (!simulate) {
                car[3].shrink(n);
                if (car[3].isEmpty())
                    car[3] = ItemStack.EMPTY;
                setChanged();
            }
            return out;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 16;
        }
    };

    public KilnZoneBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.KILN_ZONE_ENTITY.get(), pos, state);
    }

    public IItemHandler handler() {
        return view;
    }

    @Override
    public Su getLoadSu() {
        return new Su(6f);
    }

    public void tickServer() {
        if (level == null)
            return;
        float zone = HeatSource.at(level, worldPosition).value();
        float ambient = AmbientTable.at(level).value();
        for (int i = 0; i < 4; i++)
            run(i, ambient + (zone - ambient) * ZONE_SHARE[i]);
        float rpm = Math.abs(getRpm().value());
        if (!engaged || rpm < 0.5f)
            return;
        stepProgress += rpm / 16f;
        if (stepProgress < STEP_TICKS)
            return;
        stepProgress = 0f;
        Direction front = getBlockState().getValue(KilnZoneBlock.FACING);
        if (!car[3].isEmpty()) {
            IItemHandler out = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(front), front.getOpposite());
            if (out != null) {
                ItemStack left = ItemHandlerHelper.insertItem(out, car[3].copy(), false);
                car[3] = left.isEmpty() ? ItemStack.EMPTY : left;
            }
        }
        for (int i = 3; i > 0; i--)
            if (car[i].isEmpty() && !car[i - 1].isEmpty()) {
                car[i] = car[i - 1];
                hold[i] = hold[i - 1];
                car[i - 1] = ItemStack.EMPTY;
                hold[i - 1] = 0f;
            }
        setChanged();
    }

    /** One position for one tick at {@code tu}: the cooking ladder for food, else the one-input row the ware matches. */
    private void run(int i, float tu) {
        ItemStack s = car[i];
        if (s.isEmpty()) {
            hold[i] = 0f;
            return;
        }
        Tu here = new Tu(tu);
        ItemHeat.set(s, here, level);
        var food = Cooking.findCookable(level, s);
        if (food.isPresent()) {
            car[i] = Cooking.tick(s, food.get(), here, 0f, level);
            hold[i] = 0f;
            return;
        }
        var found = ThermalProcessTable.get().findSingle(level, s, here, false);
        if (found.isEmpty() || found.get().hasFluidResult()) {
            hold[i] = 0f;
            return;
        }
        ThermalProcess row = found.get();
        if (row.spoilsAt(here)) {
            ItemStack ruin = Failure.ruined(row, s).copy();
            ruin.setCount(Math.min(ruin.getMaxStackSize(), s.getCount()));
            car[i] = ruin;
            hold[i] = 0f;
            return;
        }
        boolean done;
        if (row.requiredTpu() <= 0f) {
            done = row.inBand(here);
        } else {
            float suit = row.suitability(here);
            hold[i] = ProcessRun.hold(hold[i], suit);
            done = suit > 0f && hold[i] >= row.requiredFor(List.of(s.copyWithCount(1)));
        }
        if (done && !row.result().isEmpty()) {
            ItemStack out = row.result().copy();
            out.setCount(Math.min(out.getMaxStackSize(), s.getCount() * out.getCount()));
            ItemHeat.set(out, here, level);
            car[i] = out;
            hold[i] = 0f;
        }
    }

    public void dropAll() {
        if (level == null)
            return;
        for (ItemStack s : car)
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
    }

    @Override
    public void setEngaged(boolean engaged) {
        this.engaged = engaged;
        setChanged();
    }

    @Override
    public boolean isEngaged() {
        return engaged;
    }

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return Collections.emptySet();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < 4; i++) {
            tag.put("Car" + i, car[i].saveOptional(registries));
            tag.putFloat("Hold" + i, hold[i]);
        }
        tag.putFloat("Step", stepProgress);
        tag.putBoolean("Engaged", engaged);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < 4; i++) {
            car[i] = ItemStack.parseOptional(registries, tag.getCompound("Car" + i));
            hold[i] = tag.getFloat("Hold" + i);
        }
        stepProgress = tag.getFloat("Step");
        engaged = !tag.contains("Engaged") || tag.getBoolean("Engaged");
    }
}
