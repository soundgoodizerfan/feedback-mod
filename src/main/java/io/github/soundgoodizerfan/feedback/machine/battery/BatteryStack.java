// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.battery;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * A battery as a carried stack: the same physical state {@link BatteryBlockEntity} saves, read and written through the
 * {@link FDataComponents#BATTERY} component, with the same figures. It is what a Charger fills and what a suit draws from. Never ticks:
 * every figure is a function of the saved state, so a battery in a pocket costs nothing.
 *
 * <p>The formulas copy the block entity's (capacity times condition, cold and water shares; the volts cliff near empty). A change to
 * one is a change to both.
 */
public final class BatteryStack {

    private BatteryStack() {
    }

    /** The saved state of a battery stack, or null if the stack is not one. The tag is the stack's own: copy before writing. */
    public static CompoundTag state(ItemStack stack) {
        return stack.get(FDataComponents.BATTERY.get());
    }

    public static boolean is(ItemStack stack) {
        return state(stack) != null;
    }

    private static float coldShare(CompoundTag t) {
        float temperature = t.contains("Temperature") ? t.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        float room = FTuning.AMBIENT_TU.value();
        if (temperature >= room)
            return 1f;
        float cold = t.contains("ColdFactor") ? t.getFloat("ColdFactor") : 0.5f;
        return Mth.lerp(Mth.clamp((temperature - FTuning.BATTERY_DEEP_COLD_TU) / (room - FTuning.BATTERY_DEEP_COLD_TU), 0f, 1f), cold, 1f);
    }

    private static float waterShare(CompoundTag t) {
        float low = FTuning.BATTERY_WATER_LOW_SHARE * FTuning.BATTERY_WATER_TANK_MB;
        float water = t.getFloat("Water");
        return water >= low ? 1f : Mth.lerp(water / low, FTuning.BATTERY_DRY_CAPACITY, 1f);
    }

    private static float condition(CompoundTag t) {
        return t.contains("Condition") ? t.getFloat("Condition") : 1f;
    }

    /** Amp-ticks the cells can hold right now. */
    public static float effectiveCapacity(CompoundTag t) {
        return t.getFloat("Capacity") * condition(t) * coldShare(t) * waterShare(t);
    }

    public static float charge(CompoundTag t) {
        return t.getFloat("Charge");
    }

    /** State of charge, 0 to 1 of what it can hold now. */
    public static float stateOfCharge(CompoundTag t) {
        float eff = effectiveCapacity(t);
        return eff <= 0f ? 0f : Mth.clamp(charge(t) / eff, 0f, 1f);
    }

    /** Terminal volts of the whole stack. */
    public static float terminalVolts(CompoundTag t) {
        float soc = stateOfCharge(t);
        float full = t.contains("VoltsFull") ? t.getFloat("VoltsFull") : 2.1f;
        float low = t.contains("VoltsLow") ? t.getFloat("VoltsLow") : 1.95f;
        float empty = t.contains("VoltsEmpty") ? t.getFloat("VoltsEmpty") : 1.6f;
        float cell = soc > 0.1f ? Mth.lerp((soc - 0.1f) / 0.9f, low, full) : Mth.lerp(soc / 0.1f, empty, low);
        return Math.max(1, t.getInt("Cells")) * cell;
    }

    /** Filled with its electrolyte: without it nothing happens. */
    public static boolean ready(CompoundTag t) {
        return t.getFloat("Acid") >= Math.max(1, t.getInt("Cells")) * FTuning.BATTERY_ACID_MB_PER_CELL - 0.5f;
    }

    public static boolean full(CompoundTag t) {
        return charge(t) >= effectiveCapacity(t) - 1e-3f;
    }

    /** The highest voltage the case holds off, as a band. */
    public static VoltageBand caseBand(CompoundTag t) {
        return VoltageBand.CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(t.getString("CaseBand"))).result().orElse(VoltageBand.LV);
    }

    /** Amps a battery at this line asks for: it charges while the line is above its terminal, and a full one asks nothing (a stack has no vent). */
    public static float wantedAmps(CompoundTag t, float lineVolts) {
        if (!ready(t) || full(t) || lineVolts <= terminalVolts(t) + FTuning.BATTERY_SWITCH_VOLTS)
            return 0f;
        return effectiveCapacity(t) / FTuning.BATTERY_CHARGE_TICKS;
    }

    /**
     * Adds a tick of charge. Returns heat as amps times volts lost to the charge efficiency, which a carrier may dump into a wearer;
     * a line over the case's band wears the cells, as it does in a placed battery.
     */
    public static float charge(CompoundTag t, float amps, float lineVolts) {
        return charge(t, amps, lineVolts, 1);
    }

    /** As {@link #charge(CompoundTag, float, float)} for {@code ticks} ticks' worth of current added at once; the over-voltage wear is each tick's. */
    public static float charge(CompoundTag t, float amps, float lineVolts, int ticks) {
        if (amps <= 0f)
            return 0f;
        float eff = t.contains("ChargeEfficiency") ? t.getFloat("ChargeEfficiency") : FTuning.BATTERY_CHARGE_EFFICIENCY;
        t.putFloat("Charge", Math.min(effectiveCapacity(t), charge(t) + amps * eff));
        float excess = ElectricRules.overVoltage(lineVolts, caseBand(t));
        if (excess > 0f)
            t.putFloat("Condition", Math.max(FTuning.BATTERY_CONDITION_FLOOR, condition(t) - FTuning.BATTERY_OVERVOLT_WEAR * excess * ticks));
        return amps * lineVolts * (1f - eff);
    }

    /** Takes {@code ampTicks} out; returns how much was actually there. */
    public static float draw(CompoundTag t, float ampTicks) {
        float took = Math.min(charge(t), Math.max(0f, ampTicks));
        t.putFloat("Charge", charge(t) - took);
        return took;
    }
}
