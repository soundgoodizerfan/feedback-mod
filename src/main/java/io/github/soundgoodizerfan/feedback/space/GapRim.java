// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3d;

/**
 * The rim of the Gap: a craft that overshoots every destination does not coast on for ever. Past {@link SpaceTuning#GAP_RIM_BLOCKS} from the Gap's
 * origin point ({@code 0, ENTRY_Y, 0}) a body feels drag that grows with how far out it is, up to {@link SpaceTuning#GAP_RIM_DRAG_MAX} of its velocity
 * each physics step, until it stops. It is left drifting at rest where it stopped: a derelict another craft can fly out to and recover, with whatever is
 * aboard. Only the Gap has a rim; the planets' own dimensions have air.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class GapRim {

    private GapRim() {
    }

    @SubscribeEvent
    public static void onPhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        var level = event.getPhysicsSystem().getLevel();
        if (!level.dimension().equals(SpaceTravel.GAP))
            return;
        var container = ServerSubLevelContainer.getContainer(level);
        if (container == null)
            return;
        for (ServerSubLevel body : container.getAllSubLevels()) {
            if (body.isRemoved())
                continue;
            var pos = body.logicalPose().position();
            double out = Math.sqrt(pos.x() * pos.x() + (pos.y() - SpaceTuning.ENTRY_Y) * (pos.y() - SpaceTuning.ENTRY_Y) + pos.z() * pos.z())
                    - SpaceTuning.GAP_RIM_BLOCKS;
            if (out <= 0)
                continue;
            double share = SpaceTuning.GAP_RIM_DRAG_MAX * Math.min(1.0, out / SpaceTuning.GAP_RIM_BLOCKS);
            RigidBodyHandle handle = RigidBodyHandle.of(body);
            Vector3d v = handle.getLinearVelocity(new Vector3d());
            handle.addLinearAndAngularVelocity(new Vector3d(v).mul(-share), new Vector3d());
        }
    }
}
