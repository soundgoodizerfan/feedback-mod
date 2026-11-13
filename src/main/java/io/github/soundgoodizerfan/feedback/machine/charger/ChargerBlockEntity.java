// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.charger;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A clip on a wire that charges a battery as an item (a suit's cell). It is a sink: while the battery in it holds less
 * than the line, it asks for its own charge current, and what it is delivered raises its charge up to the voltage it sees
 * and never past it.
 *
 * <h2>Why it has no rating and never wears</h2>
 * It is a terminal, not a machine doing work. A battery stack charges as a placed battery does (same figures,
 * {@link BatteryStack}): it asks for its own charge current while the line is above its terminal, loses the charge
 * efficiency as heat it does not model here, and wears if the line is over its case's band. Only a single battery goes in
 * (a stack of them is refused).
 */
public class ChargerBlockEntity extends ElectricNode {

    private static final String ROUNDS = "Rounds";

    private ItemStack rounds = ItemStack.EMPTY;

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CHARGER.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        CompoundTag battery = BatteryStack.state(rounds);
        if (battery != null)
            return BatteryStack.wantedAmps(battery, seenPotential());
        return 0;
    }

    public void tickServer() {
        if (level == null || getAmps() <= 0)
            return;
        CompoundTag battery = BatteryStack.state(rounds);
        if (battery != null) {
            CompoundTag next = battery.copy();
            BatteryStack.charge(next, getAmps(), seenPotential());
            rounds.set(FDataComponents.BATTERY.get(), next);
            setChanged();
            return;
        }
    }

    public ItemStack getRounds() {
        return rounds;
    }

    /** Takes one battery; anything else is refused. Returns what is left in hand. */
    public ItemStack insert(ItemStack stack) {
        boolean battery = BatteryStack.is(stack) && stack.getCount() == 1;
        if (!rounds.isEmpty() || !battery)
            return stack;
        rounds = stack.copy();
        setChanged();
        return ItemStack.EMPTY;
    }

    public ItemStack remove() {
        ItemStack out = rounds;
        rounds = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!rounds.isEmpty())
            tag.put(ROUNDS, rounds.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rounds = ItemStack.parseOptional(registries, tag.getCompound(ROUNDS));
    }
}
