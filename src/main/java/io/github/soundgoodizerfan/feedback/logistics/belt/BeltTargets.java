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
package io.github.soundgoodizerfan.feedback.logistics.belt;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Where a belt hands cargo: another belt, an item handler on a face, or the open air. A belt does
 * not know a furnace from a chest; it only offers the stack to whatever handler the face presents.
 */
final class BeltTargets {

    private BeltTargets() {
    }

    /** The cell cargo leaves {@code last} into, if {@code last} is a belt part. */
    @Nullable
    static BlockPos nextOf(Level level, BlockPos last) {
        BlockState s = level.getBlockState(last);
        BeltPart p = BeltPart.of(s);
        return p == null ? null : p.next(last, s);
    }

    /** Puts cargo that left a run travelling {@code from} onto the belt cell at {@code t}, if it is one that takes it. */
    static ItemStack intoBelt(Level level, BlockPos t, Direction from, ItemStack stack, boolean simulate) {
        BlockState s = level.getBlockState(t);
        BeltPart p = BeltPart.of(s);
        if (p == null || p.beltFacing(s) == from.getOpposite())
            return stack;
        return BeltRuns.of(level).insertAt(t, stack, p.beltFacing(s) == from ? 0.05 : 0.5, simulate);
    }

    /**
     * Offers {@code stack} to the cell {@code target}: a belt there takes it at its middle, a handler
     * on face {@code side} takes what fits, open air takes it as a dropped item when {@code drop}.
     * Returns what was not taken.
     */
    static ItemStack deliver(Level level, BlockPos target, Direction side, ItemStack stack, boolean simulate, boolean drop) {
        if (!level.isLoaded(target))
            return stack;
        BlockState s = level.getBlockState(target);
        if (BeltPart.of(s) != null)
            return BeltRuns.of(level).insertAt(target, stack, 0.5, simulate);
        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, target, side);
        if (h != null)
            return ItemHandlerHelper.insertItem(h, stack, simulate);
        if (drop && s.getCollisionShape(level, target).isEmpty()) {
            if (!simulate)
                spill(level, Vec3.atCenterOf(target), stack);
            return ItemStack.EMPTY;
        }
        return stack;
    }

    static void spill(Level level, Vec3 at, ItemStack stack) {
        ItemEntity e = new ItemEntity(level, at.x, at.y, at.z, stack.copy(), 0, 0, 0);
        e.setPickUpDelay(10);
        level.addFreshEntity(e);
    }

    /**
     * A drop straight down from just below {@code from}: it lands in the first belt or handler in the column, else on the
     * first solid block. Returns the stack left, or {@link ItemStack#EMPTY}; {@code broke} is set when a brittle unit fell
     * further than a chute may drop it.
     */
    static ItemStack fall(Level level, BlockPos from, ItemStack stack, boolean brittle, boolean[] broke) {
        for (int dy = 0; dy < 24; dy++) {
            BlockPos cell = from.below(dy);
            if (!level.isLoaded(cell))
                return stack;
            BlockState s = level.getBlockState(cell);
            boolean carrier = BeltPart.of(s) != null || level.getCapability(Capabilities.ItemHandler.BLOCK, cell, Direction.UP) != null;
            if (carrier) {
                if (brittle && dy > io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning.CHUTE_FRAGILE_DROP) {
                    broke[0] = true;
                    return ItemStack.EMPTY;
                }
                return deliver(level, cell, Direction.UP, stack, false, false);
            }
            if (!s.getCollisionShape(level, cell).isEmpty()) {
                if (brittle && dy - 1 > io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning.CHUTE_FRAGILE_DROP) {
                    broke[0] = true;
                    return ItemStack.EMPTY;
                }
                spill(level, Vec3.atCenterOf(cell.above()), stack);
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }
}
