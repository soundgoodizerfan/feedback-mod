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

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.modular.MachineHeadItem;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * The replaceable head of a Mechanical Hammer or Press (machine shop spec 2.5): the part that wears, and the
 * part whose material and shape set the strength of the blow. The machine body does neither.
 *
 * <h2>Condition lives on the stack</h2>
 * As the Lathe's tool bit does ({@code FDataComponents#TOOL_CONDITION}), so swapping in a fresh head resets the
 * machine's wear and a worn head carries its wear to another machine. Absent means new.
 */
public final class HeadSlot {

    private ItemStack head = ItemStack.EMPTY;

    public ItemStack get() {
        return head;
    }

    public boolean isEmpty() {
        return head.isEmpty();
    }

    /** Seats one of {@code in}, taking it off the stack, and returns the head it replaces (empty if none). */
    public ItemStack swap(ItemStack in) {
        ItemStack out = head;
        head = in.split(1);
        return out;
    }

    public ItemStack take() {
        ItemStack out = head;
        head = ItemStack.EMPTY;
        return out;
    }

    /** How sound the head is; a machine with none reads as new, so nothing to hit with never shows as worn. */
    public float condition() {
        return head.isEmpty() ? 1f : head.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    /** The most St a blow can carry now: the head's baked ceiling, less what wear has cost. Zero with no head. */
    public St ceiling() {
        return new St(MachineHeadItem.strikeSt(head) * condition());
    }

    /**
     * The head absorbs {@code stAbsorbed}: condition falls toward {@code floor} and never past it.
     *
     * @return whether the condition band changed, so the machine should sync.
     */
    public boolean wear(float stAbsorbed, float perSt, float floor) {
        if (head.isEmpty() || stAbsorbed <= 0)
            return false;
        int before = FTuning.conditionBand(condition());
        head.set(FDataComponents.TOOL_CONDITION.get(), Math.max(floor, condition() - stAbsorbed * perSt));
        return FTuning.conditionBand(condition()) != before;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Head", head.saveOptional(registries));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        head = ItemStack.parseOptional(registries, tag.getCompound("Head"));
    }
}
