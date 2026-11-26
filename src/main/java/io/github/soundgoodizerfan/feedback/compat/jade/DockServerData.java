// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.robot.RobotDockBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Whether a robot dock or reel is free for a robot: powered and nothing standing on it or tied to it. */
public class DockServerData implements IServerDataProvider<BlockAccessor> {

    public static final DockServerData INSTANCE = new DockServerData();

    static final String FREE = "DockFree", DOCK = "Dock";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof RobotDockBlockEntity dock) {
            data.putBoolean(DOCK, true);
            data.putBoolean(FREE, dock.isFree());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("robot_dock");
    }
}
