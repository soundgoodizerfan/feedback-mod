// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.induction.PreheatCoilBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** The target temperature a forging preheat coil is set to, 0 when it is off: a setting the player chose, so it is shown plainly. */
public class PreheatServerData implements IServerDataProvider<BlockAccessor> {

    public static final PreheatServerData INSTANCE = new PreheatServerData();

    static final String TARGET = "PreheatTarget", HELD = "PreheatHeld", HELD_TU = "PreheatHeldTu", COUPLES = "PreheatCouples", HEATING = "PreheatHeating";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof PreheatCoilBlockEntity coil)
        {
            data.putInt(TARGET, Math.round(coil.targetTu()));
            if (!coil.held().isEmpty()) {
                data.putString(HELD, coil.held().getHoverName().getString());
                data.putFloat(HELD_TU, coil.heldTemperature().value());
                data.putBoolean(COUPLES, coil.couples());
                data.putBoolean(HEATING, coil.heating());
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("preheat");
    }
}
