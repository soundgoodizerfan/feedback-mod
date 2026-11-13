// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.battery;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
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
 * every figure is a function of the saved state, so a battery in a pocket costs nothing. Charge is stored in Eu; the battery is rated
 * the band of its case.
 *
 * <p>The formulas copy the block entity's (capacity times condition, cold and water shares). A change to one is a change to both.
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
        float temperature = t.contains("Temperature") ? t.getFloat("Temperature") : ThermalTuning.AMBIENT_TU.value();
        float room = ThermalTuning.AMBIENT_TU.value();
        if (temperature >= room)
            return 1f;
        float cold = t.contains("ColdFactor") ? t.getFloat("ColdFactor") : 0.5f;
        return Mth.lerp(Mth.clamp((temperature - ElectricMachineTuning.BATTERY_DEEP_COLD_TU) / (room - ElectricMachineTuning.BATTERY_DEEP_COLD_TU), 0f, 1f), cold, 1f);
    }

    private static float waterShare(CompoundTag t) {
        float low = ElectricMachineTuning.BATTERY_WATER_LOW_SHARE * FTuning.BATTERY_WATER_TANK_MB;
        float water = t.getFloat("Water");
        return water >= low ? 1f : Mth.lerp(water / low, ElectricMachineTuning.BATTERY_DRY_CAPACITY, 1f);
    }

    private static float condition(CompoundTag t) {
        return t.contains("Condition") ? t.getFloat("Condition") : 1f;
    }

    private static int cells(CompoundTag t) {
        return Math.max(1, t.getInt("Cells"));
    }

    /** Eu one cell can hold right now: its Eu of plate times condition, cold and water. */
    private static float cellCapacity(CompoundTag t) {
        return t.getFloat("Capacity") * condition(t) * coldShare(t) * waterShare(t);
    }

    /** Eu the cells can hold right now. */
    public static float effectiveCapacity(CompoundTag t) {
        return cells(t) * cellCapacity(t);
    }

    /** Eu stored. */
    public static float charge(CompoundTag t) {
        return t.getFloat("Charge");
    }

    /** State of charge, 0 to 1 of what it can hold now. */
    public static float stateOfCharge(CompoundTag t) {
        float eff = effectiveCapacity(t);
        return eff <= 0f ? 0f : Mth.clamp(charge(t) / eff, 0f, 1f);
    }

    /** Filled with its electrolyte: without it nothing happens. */
    public static boolean ready(CompoundTag t) {
        return t.getFloat("Acid") >= cells(t) * FTuning.BATTERY_ACID_MB_PER_CELL - 0.5f;
    }

    public static boolean full(CompoundTag t) {
        return charge(t) >= effectiveCapacity(t) - 1e-3f;
    }

    /** The band the battery is rated: the case's. */
    public static VoltageBand caseBand(CompoundTag t) {
        return VoltageBand.CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf(t.getString("CaseBand"))).result().orElse(VoltageBand.LV);
    }

    /** Packets a tick a battery at this line asks for: it charges when fed its band or above, and a full one asks nothing (a stack has no vent). */
    public static float wantedAmps(CompoundTag t, VoltageBand fed, float packetEu) {
        if (!ready(t) || full(t) || !ElectricRules.accepts(fed, packetEu, caseBand(t)))
            return 0f;
        return cellCapacity(t) / ElectricMachineTuning.BATTERY_CHARGE_TICKS / ElectricRules.packetEu(caseBand(t));
    }

    /**
     * Adds a tick of charge: {@code amps} packets of {@code packetEu} at band {@code fed}. Returns the Eu lost to the charge efficiency as heat,
     * which a carrier may dump into a wearer; a band over the case's wears the cells, as it does in a placed battery.
     */
    public static float charge(CompoundTag t, float amps, VoltageBand fed, float packetEu) {
        return charge(t, amps, fed, packetEu, 1);
    }

    /** As {@link #charge(CompoundTag, float, VoltageBand, float)} for {@code ticks} ticks' worth of current added at once; the over-band wear is each tick's. */
    public static float charge(CompoundTag t, float amps, VoltageBand fed, float packetEu, int ticks) {
        if (amps <= 0f)
            return 0f;
        float eff = t.contains("ChargeEfficiency") ? t.getFloat("ChargeEfficiency") : FTuning.BATTERY_CHARGE_EFFICIENCY;
        float in = amps * packetEu;
        t.putFloat("Charge", Math.min(effectiveCapacity(t), charge(t) + in * eff));
        float excess = ElectricRules.overBand(fed, caseBand(t));
        if (excess > 0f)
            t.putFloat("Condition", Math.max(ElectricMachineTuning.BATTERY_CONDITION_FLOOR, condition(t) - ElectricMachineTuning.BATTERY_OVERVOLT_WEAR * excess * ticks));
        return in * (1f - eff);
    }

    /** Puts {@code eu} straight into the cells (a gun's recoil dynamo), held to what they can hold. */
    public static void store(CompoundTag t, float eu) {
        t.putFloat("Charge", Math.min(effectiveCapacity(t), charge(t) + Math.max(0f, eu)));
    }

    /** Takes {@code eu} out; returns how much was actually there. */
    public static float draw(CompoundTag t, float eu) {
        float took = Math.min(charge(t), Math.max(0f, eu));
        t.putFloat("Charge", charge(t) - took);
        return took;
    }
}
