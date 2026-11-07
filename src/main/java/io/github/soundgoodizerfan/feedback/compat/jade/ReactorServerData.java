// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.reactor.ControlAssemblyBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.reactor.FuelChannelBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.reactor.ReactorCoreBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** What a reactor core, a fuel channel or a control assembly reports: power, decay heat, multiplication, fuel health, flaws, rod positions. */
public class ReactorServerData implements IServerDataProvider<BlockAccessor> {

    public static final ReactorServerData INSTANCE = new ReactorServerData();

    static final String KEY = "Fission";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        CompoundTag out = new CompoundTag();
        if (accessor.getBlockEntity() instanceof ReactorCoreBlockEntity core) {
            out.putString("Kind", "core");
            out.putFloat("Power", core.power());
            out.putFloat("Decay", core.decayWork());
            out.putFloat("K", core.meanK());
            out.putFloat("Health", core.health());
            out.putInt("Fuel", core.fuelCount());
            out.putBoolean("Formed", core.formed());
            out.putBoolean("Scrammed", core.scrammed());
            out.putFloat("Pressure", core.getPressure().value());
            StringBuilder rods = new StringBuilder();
            for (ControlAssemblyBlockEntity ca : core.controls())
                rods.append(ca.position());
            out.putString("Rods", rods.toString());
            out.putString("Flaws", String.join("; ", core.flaws()));
        } else if (accessor.getBlockEntity() instanceof FuelChannelBlockEntity ch && ch.hasRod()) {
            out.putString("Kind", "channel");
            out.putFloat("Burn", ch.burn());
            out.putFloat("Condition", ch.condition());
            out.putBoolean("Live", ch.live());
        } else if (accessor.getBlockEntity() instanceof ControlAssemblyBlockEntity ca) {
            out.putString("Kind", "rods");
            out.putInt("Position", ca.position());
            out.putInt("Target", ca.target());
            out.putFloat("Strength", ca.strength());
        } else
            return;
        data.put(KEY, out);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("fission");
    }
}
