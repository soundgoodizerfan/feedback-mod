// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * A sink that charges a robot's battery as the {@code ChargerBlockEntity} charges a battery item: it asks for the battery's charge current
 * while the line is at its band or above and puts what it is delivered into the robot. The dock serves the robot standing on it, the reel
 * the robot tied to it within the cable's length; the robot is found by a short scan every {@link #SCAN_TICKS} ticks, not every tick.
 */
public class RobotDockBlockEntity extends ElectricNode {

    private static final int SCAN_TICKS = 10;

    @Nullable
    private RobotEntity served;

    public RobotDockBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ROBOT_DOCK.get(), pos, state);
    }

    private boolean reel() {
        return getBlockState().getBlock() instanceof RobotDockBlock b && b.reel();
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        var battery = served == null || served.isRemoved() ? null : served.batteryState();
        return battery == null ? 0f : BatteryStack.wantedAmps(battery, getBand(), getPacketEu());
    }

    /** Whether the line gives it current and no robot stands on it (a dock) or hangs from it (a reel), as of the last scan. */
    public boolean isFree() {
        return isLive() && (served == null || served.isRemoved());
    }

    public void tickServer() {
        if (level == null)
            return;
        if (level.getGameTime() % SCAN_TICKS == 0)
            served = find();
        if (served != null && !served.isRemoved() && getAmps() > 0f)
            served.charge(getAmps(), getBand(), getPacketEu());
    }

    @Nullable
    private RobotEntity find() {
        if (reel()) {
            AABB reach = new AABB(worldPosition).inflate(RobotTuning.TETHER_LENGTH);
            for (RobotEntity r : level.getEntitiesOfClass(RobotEntity.class, reach))
                if (worldPosition.equals(r.tether()) && r.withinTether())
                    return r;
            return null;
        }
        var on = level.getEntitiesOfClass(RobotEntity.class, new AABB(worldPosition.above()).deflate(0.1, 0, 0.1));
        return on.isEmpty() ? null : on.get(0);
    }
}
