// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.air.BallastTankBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.air.GasCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.air.PropBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** What an airship's gas cell, ballast tank or a propeller says, in words: state, never a figure. */
public class AirServerData implements IServerDataProvider<BlockAccessor> {

    public static final AirServerData INSTANCE = new AirServerData();

    static final String KIND = "AirKind", GAS = "AirGas", FILL = "AirFill", LEAK = "AirLeak", BURNING = "AirBurning",
            STATUS = "AirStatus", WORN = "AirWorn";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof GasCellBlockEntity cell) {
            data.putString(KIND, "cell");
            var tank = cell.getTank();
            data.putFloat(FILL, tank.getFluidAmount() / (float) tank.getCapacity());
            if (!tank.isEmpty())
                data.putString(GAS, BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()).toString());
            data.putBoolean(LEAK, cell.hole() > 0f);
            data.putBoolean(BURNING, cell.burning());
        } else if (accessor.getBlockEntity() instanceof BallastTankBlockEntity ballast) {
            data.putString(KIND, "ballast");
            data.putFloat(FILL, ballast.getTank().getFluidAmount() / (float) ballast.getTank().getCapacity());
        } else if (accessor.getBlockEntity() instanceof PropBlockEntity prop) {
            data.putString(KIND, "prop");
            data.putInt(STATUS, prop.status());
            data.putBoolean(WORN, prop.condition() < 0.7f);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("air");
    }
}
