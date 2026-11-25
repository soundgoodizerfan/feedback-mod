// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricMember;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRun;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * An electric starter motor on one face of an engine. Starting the engine turns it over: the starter spends
 * {@link VehicleTuning#STARTER_CRANK_EU} of charge from the battery that touches its face, or from a battery in the run of wire that does,
 * and a flat battery gives no start. Stateless: the holder persists it as a flag per face ({@link FittingStore}).
 * <p>
 * It reaches the battery by what is there, never by identity: any {@link BatteryBlockEntity} in the touching block or in the touching
 * {@link ElectricNode}'s run. Without a starter the engine's use toggles it, as before.
 */
public final class StarterFitting implements SidedFitting {

    private final BlockEntity holder;
    private final Direction side;

    public StarterFitting(BlockEntity holder, Direction side) {
        this.holder = holder;
        this.side = side;
    }

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.STARTER.get());
    }

    /** Turns the engine over once: true when a battery gave the charge. */
    public boolean crank() {
        Level level = holder.getLevel();
        if (level == null || level.isClientSide)
            return false;
        BatteryBlockEntity battery = battery(level, holder.getBlockPos().relative(side));
        return battery != null && battery.spend(VehicleTuning.STARTER_CRANK_EU);
    }

    private static BatteryBlockEntity battery(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BatteryBlockEntity battery)
            return battery;
        if (!(be instanceof ElectricNode node))
            return null;
        ElectricRun run = node.getRun();
        if (run == null)
            return null;
        for (int i = 0; i < run.size(); i++) {
            ElectricMember member = run.member(i);
            if (member instanceof BatteryBlockEntity battery && battery.getLevel() == level)
                return battery;
        }
        return null;
    }
}
