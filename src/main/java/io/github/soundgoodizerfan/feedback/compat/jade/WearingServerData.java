// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.machine.WorkpieceHolder;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * The condition of a worn block that holds no workpiece (a thruster, a gearbox), which
 * {@link WorkpieceComponent} does not cover; the client does not hold the figure, so it is sent.
 */
public class WearingServerData implements IServerDataProvider<BlockAccessor> {

    public static final WearingServerData INSTANCE = new WearingServerData();

    static final String CONDITION = "WornCondition", JAMMED = "Jammed";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof Wearing wearing && !(accessor.getBlockEntity() instanceof WorkpieceHolder))
            data.putFloat(CONDITION, wearing.getCondition());
        if (accessor.getBlockEntity() instanceof io.github.soundgoodizerfan.feedback.machine.cutter.PowerCutterBlockEntity cutter)
            data.putBoolean(JAMMED, cutter.isJammed());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("wearing");
    }
}
