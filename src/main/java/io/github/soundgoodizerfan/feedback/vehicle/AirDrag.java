// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.soundgoodizerfan.feedback.vehicle;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3d;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Air on every moving body: Sable's one pre-physics-tick event, so each body is dragged exactly once a
 * step however many thrusters or frames it carries. Force is a constant times the dimension's air
 * density ({@link AirDensityTable}) times speed squared, against the velocity, capped at the body's own
 * momentum so it never reverses it inside a step. A Drivetrain frame (and the wheel bodies of its
 * groups) is left out: it drags itself by its silhouette in {@code Drivetrain.physicsTick}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class AirDrag {
    private AirDrag() {}

    @SubscribeEvent
    public static void onPhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        ServerLevel level = event.getPhysicsSystem().getLevel();
        double dt = event.getTimeStep();
        var bodies = ServerSubLevelContainer.getContainer(level).getAllSubLevels();

        for (ServerSubLevel body : bodies) {
            if (body.isRemoved())
                continue;
            var handle = event.getPhysicsSystem().getPhysicsHandle(body);
            Vector3d vel = handle.getLinearVelocity(new Vector3d());
            double speed = vel.length();
            if (speed < 1e-3)
                continue;
            double mass = body.getMassTracker().getMass();
            double density = AirDensityTable.at(level, body.logicalPose().position().y());
            Vector3d back = body.logicalPose().transformNormalInverse(new Vector3d(vel).div(speed), new Vector3d()).negate();
            ForceTotal force = new ForceTotal();
            // A body of micro cells drags by its size: the constant below is tuned for a heavy thruster
            // body and would hold a small light one to a crawl. Its cross-section is a cube's of its voxels.
            int voxels = 0;
            for (BlockEntitySubLevelActor a : body.getPlot().getBlockEntityActors())
                if (a instanceof MicroCellBlockEntity cell)
                    voxels += cell.voxelCount();
            double drag = voxels == 0 ? FTuning.AIR_DRAG
                    : FTuning.MICRO_AIR_DRAG * Math.pow(voxels, 2.0 / 3) / (MicroCellBlockEntity.N * MicroCellBlockEntity.N);
            if (density > 0)
                force.applyLinearImpulse(new Vector3d(back).mul(
                        Math.min(drag * density * speed * speed * dt, mass * speed)));
            // shortcut: speed ceiling = BODY_MAX_STEP_BLOCKS / step, a tunnelling guard against walls, only
            // for bodies with a thruster (the ones that outrun it); upgrade to continuous collision if
            // other bodies need it.
            double ceiling = FTuning.THRUSTER_MAX_STEP_BLOCKS / dt;
            if (speed > ceiling && hasThruster(body))
                force.applyLinearImpulse(new Vector3d(back).mul((speed - ceiling) * mass));
            handle.applyForcesAndReset(force);
        }
    }

    private static boolean hasThruster(ServerSubLevel body) {
        for (BlockEntitySubLevelActor a : body.getPlot().getBlockEntityActors())
            if (a instanceof ThrusterBlockEntity)
                return true;
        return false;
    }
}
