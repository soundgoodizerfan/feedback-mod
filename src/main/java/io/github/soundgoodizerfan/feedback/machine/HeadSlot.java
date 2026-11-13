// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.modular.attunement.EchoKinds;
import io.github.soundgoodizerfan.feedback.modular.attunement.EdgeKinds;
import io.github.soundgoodizerfan.feedback.modular.attunement.WagerKinds;
import io.github.soundgoodizerfan.feedback.modular.attunement.MachineKinds;
import io.github.soundgoodizerfan.feedback.modular.item.ModularHandTool;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
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
        return new St(ModularHandTool.blow(head) * EdgeKinds.headBlow(head) * condition());
    }

    /**
     * The St of one stroke: the ceiling, as a coin flip if the head carries {@code gamble} (double or a stroke that does nothing) and
     * repeated a beat later at a share if it carries {@code echo_blow}. Used where a machine strikes once per stroke.
     */
    public St stroke(net.minecraft.util.RandomSource random) {
        float st = ceiling().value();
        if (st <= 0f)
            return ceiling();
        st *= EchoKinds.strokeShare(head) * WagerKinds.strokeCoin(head, random);
        return new St(st);
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
        // A soaking head turns some of the wear away, as it does on a tool.
        head.set(FDataComponents.TOOL_CONDITION.get(), Math.max(floor, condition() - stAbsorbed * perSt * MachineKinds.wearShare(head)));
        return FTuning.conditionBand(condition()) != before;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Head", head.saveOptional(registries));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        head = ItemStack.parseOptional(registries, tag.getCompound("Head"));
    }
}
