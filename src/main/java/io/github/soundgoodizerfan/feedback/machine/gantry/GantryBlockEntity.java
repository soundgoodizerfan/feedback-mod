// Copyright (C) 2026 soundgoodizerfan/feedback
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.gantry;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.suit.ExoSuit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Charges the battery in the bay of the suit whoever stands in it is wearing. It is a sink like the {@code Charger}: it asks for the
 * battery's own charge current while the line is above the battery's terminal volts, and the battery takes it exactly as a placed one does
 * ({@link BatteryStack}), wearing if the line is over its case's band. It never discharges into the line, and it has no say over the suit.
 *
 * <p>The wearer is looked for twice a second, and the charge is written onto the chestplate every ten ticks rather than every tick, so the
 * slot is not resent each tick.
 */
public class GantryBlockEntity extends ElectricNode {

    private static final int LOOK_TICKS = 10, WRITE_TICKS = 10;

    private float wanted;
    private float owed;
    private int ticks;

    public GantryBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GANTRY.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return wanted;
    }

    private Player occupant() {
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(worldPosition)))
            if (p.blockPosition().equals(worldPosition) && ExoSuit.of(p).worn())
                return p;
        return null;
    }

    public void tickServer() {
        if (level == null)
            return;
        ticks++;
        if (ticks % LOOK_TICKS == 0) {
            float before = wanted;
            Player p = occupant();
            CompoundTag t = p == null ? null : BatteryStack.state(ExoSuit.battery(p.getItemBySlot(EquipmentSlot.CHEST)));
            wanted = t == null ? 0f : BatteryStack.wantedAmps(t, seenPotential());
            if (wanted != before)
                setChanged();
        }
        if (getAmps() > 0f)
            owed += getAmps();
        if (owed > 0f && ticks % WRITE_TICKS == 0) {
            Player p = occupant();
            if (p != null) {
                ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
                ItemStack bat = ExoSuit.battery(chest);
                CompoundTag t = BatteryStack.state(bat);
                if (t != null) {
                    CompoundTag next = t.copy();
                    BatteryStack.charge(next, owed, seenPotential(), WRITE_TICKS);
                    ItemStack copy = bat.copy();
                    copy.set(FDataComponents.BATTERY.get(), next);
                    ExoSuit.setBattery(chest, copy);
                }
            }
            owed = 0f;
        }
    }
}
