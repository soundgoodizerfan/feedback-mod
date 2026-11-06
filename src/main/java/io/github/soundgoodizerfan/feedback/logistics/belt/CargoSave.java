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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The cargo over a cell, as a belt cell saves it and a rebuilt run takes it back. */
final class CargoSave {

    private CargoSave() {
    }

    /** {@code now} stamps each unit with when it was saved, so a reload can tell how long the run sat unloaded. */
    static ListTag write(List<Object[]> saved, HolderLookup.Provider registries, long now) {
        ListTag list = new ListTag();
        for (Object[] s : saved) {
            CompoundTag t = new CompoundTag();
            t.put("Stack", ((ItemStack) s[0]).save(registries));
            t.putFloat("Off", (Float) s[1]);
            t.putLong("At", s.length > 2 ? (Long) s[2] : now);
            list.add(t);
        }
        return list;
    }

    static List<Object[]> read(ListTag list, HolderLookup.Provider registries) {
        List<Object[]> out = new ArrayList<>();
        for (Tag tag : list) {
            CompoundTag t = (CompoundTag) tag;
            ItemStack stack = ItemStack.parse(registries, t.getCompound("Stack")).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty())
                out.add(new Object[]{stack, t.getFloat("Off"), t.getLong("At")});
        }
        return out;
    }

    /** Cargo a cell loaded with and has not yet handed to a run (consumed once). */
    interface Holder {
        List<Object[]> takePending();
    }

    static List<Object[]> take(BlockEntity be) {
        return be instanceof Holder h ? h.takePending() : List.of();
    }
}
