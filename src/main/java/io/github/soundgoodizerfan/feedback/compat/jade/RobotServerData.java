// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.robot.RobotEntity;

import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** What a robot is doing and how full its battery is, as a step 0 (flat) to 3 (full): the figure stays on the server, the words go out. */
public class RobotServerData implements IServerDataProvider<EntityAccessor> {

    public static final RobotServerData INSTANCE = new RobotServerData();

    static final String CHARGE = "RobotCharge", BATTERY = "RobotBattery", PROGRAM = "RobotProgram", TETHER = "RobotTether", DOING = "RobotDoing", SLOW = "RobotSlow";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof RobotEntity robot))
            return;
        float share = robot.chargeShare();
        data.putInt(CHARGE, share < 0.05f ? 0 : share < 0.3f ? 1 : share < 0.7f ? 2 : 3);
        data.putBoolean(BATTERY, robot.batteryState() != null);
        data.putBoolean(PROGRAM, !robot.core().getItem(0).isEmpty());
        data.putBoolean(TETHER, robot.tether() != null);
        data.putInt(SLOW, robot.slowed());
        if (robot.doing() != null)
            data.putString(DOING, robot.doing().langKey());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("robot");
    }
}
