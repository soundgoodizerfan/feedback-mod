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
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
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
        double density = AirDensityTable.at(level);
        var bodies = ServerSubLevelContainer.getContainer(level).getAllSubLevels();

        Set<UUID> frameBodies = new HashSet<>();
        for (ServerSubLevel body : bodies) {
            if (body.isRemoved())
                continue;
            for (BlockEntitySubLevelActor a : body.getPlot().getBlockEntityActors())
                if (a instanceof FrameBlockEntity f) {
                    frameBodies.add(body.getUniqueId());
                    f.groupBodies().values().forEach(g -> frameBodies.add(g.body()));
                }
        }

        for (ServerSubLevel body : bodies) {
            if (body.isRemoved())
                continue;
            var handle = event.getPhysicsSystem().getPhysicsHandle(body);
            Vector3d vel = handle.getLinearVelocity(new Vector3d());
            double speed = vel.length();
            if (speed < 1e-3)
                continue;
            double mass = body.getMassTracker().getMass();
            Vector3d back = body.logicalPose().transformNormalInverse(new Vector3d(vel).div(speed), new Vector3d()).negate();
            ForceTotal force = new ForceTotal();
            if (density > 0 && !frameBodies.contains(body.getUniqueId()))
                force.applyLinearImpulse(new Vector3d(back).mul(
                        Math.min(FTuning.AIR_DRAG * density * speed * speed * dt, mass * speed)));
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
