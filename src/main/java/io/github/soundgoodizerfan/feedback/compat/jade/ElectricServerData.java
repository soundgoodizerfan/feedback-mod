// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.capacitor.LeydenJarBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Sends an electrical node's reading to the client for the HUD, on demand and only for the block
 * being looked at -- the same reason {@link RotationServerData} does. The reading is not synced
 * to clients by the block entity, so this is the only way it arrives, and a server without Jade
 * sends nothing and the HUD says nothing rather than guessing.
 */
public class ElectricServerData implements IServerDataProvider<BlockAccessor> {

    public static final ElectricServerData INSTANCE = new ElectricServerData();

    static final String ROLE = "ElectricRole";
    static final String PACKET = "ElectricPacket", BAND = "ElectricBand";
    static final String AMPS = "ElectricAmps";
    static final String WANTED = "ElectricWanted";
    static final String BLOCKED = "ElectricBlocked";
    static final String INSULATION = "WireInsulation";
    static final String JAR = "JarState", JAR_BAND = "JarBand";
    static final String BAT = "BatteryState", BAT_SOC = "BatterySoc", BAT_WATER = "BatteryWater", BAT_VENT = "BatteryVent", BAT_COND = "BatteryCondition", BAT_READY = "BatteryReady", BAT_UNDER = "BatteryUnder", BAT_NEED = "BatteryNeed";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ElectricNode node))
            return;
        data.putInt(ROLE, node.role().ordinal());
        data.putFloat(PACKET, node.getPacketEu());
        data.putByte(BAND, (byte) node.getBand().ordinal());
        data.putFloat(AMPS, node.getAmps());
        data.putFloat(WANTED, node.getWantedAmps());
        data.putBoolean(BLOCKED, node.isBlocked());
        if (node instanceof CableBlockEntity wire && wire.getInsulation() != null)
            data.putByte(INSULATION, (byte) wire.getInsulation().ordinal());
        if (node instanceof LeydenJarBlockEntity jar) {
            data.putByte(JAR_BAND, (byte) jar.band().ordinal());
            data.putByte(JAR, (byte) (jar.isSupplying() ? 2 : jar.getAmps() > 0f ? 1 : 0));
        }
        if (node instanceof io.github.soundgoodizerfan.feedback.machine.battery.BatteryBlockEntity battery) {
            data.putByte(BAT, (byte) (battery.isSupplying() ? 2 : battery.getAmps() > 0f ? 1 : 0));
            data.putFloat(BAT_SOC, battery.stateOfCharge());
            data.putFloat(BAT_WATER, battery.water() / io.github.soundgoodizerfan.feedback.core.FTuning.BATTERY_WATER_TANK_MB);
            data.putBoolean(BAT_VENT, battery.isVenting() || battery.gas() >= io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning.BATTERY_GAS_WEEP_MB);
            data.putFloat(BAT_COND, battery.getCondition());
            data.putBoolean(BAT_READY, battery.ready());
            data.putBoolean(BAT_UNDER, !battery.isSupplying() && battery.isLive() && battery.getBand().compareTo(battery.caseBand()) < 0);
            data.putByte(BAT_NEED, (byte) battery.caseBand().ordinal());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("electric");
    }
}
