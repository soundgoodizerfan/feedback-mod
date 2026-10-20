/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.core.rotation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;


import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * One connected run of rotating blocks: its Su ledger, its momentum, and its speed.
 *
 * <h2>Speed lives here, not on the blocks</h2>
 * A rigid shaft run turns as one object, so there is exactly one speed for the whole network and
 * each node simply scales it by its own gear ratio. That is not only simpler than a speed per
 * block -- it is what makes inertia expressible at all, because momentum is a property of the
 * whole spinning mass and cannot be stored in any one block.
 *
 * <h2>Inertia</h2>
 * The network has a <em>target</em> speed, which is whatever the sources are asking for, and a
 * <em>current</em> speed, which chases it. How fast it chases depends on total inertia -- the
 * mass bolted to the shaft.
 * <ul>
 *   <li><b>Spinning up</b> is paid for out of surplus Su. Plenty of spare power, or very little
 *       mass, and the network comes up to speed quickly.</li>
 *   <li><b>Slowing down</b> is driven by load. A network with machines hanging off it stops
 *       promptly; an unloaded flywheel coasts for a long time.</li>
 * </ul>
 * Two things fall out of that without being designed. Cutting power makes a network wind down
 * rather than stop, which is the coasting behaviour philosophy 13 asks of a mechanical cutoff.
 * And a heavy network is slow to change but hard to disturb, while a light one is responsive and
 * twitchy -- the same trade the philosophy already describes for thermal mass, arriving in a
 * different energy type without being installed there.
 *
 * <h2>Overstress</h2>
 * An overloaded network does not snap to zero. It simply stops being driven and winds down under
 * its own load, which is both more honest and more readable than a hard stop.
 *
 * <h2>Outside pushes</h2>
 * Only sources drive a run, except through two per-tick channels that are not members: a belt
 * ({@link #driveByBelt}) and {@link #driveExternally}, a signed push for something that can push
 * back -- a car's wheels rolling it downhill. Both are written each tick and forgotten after it.
 * <p>
 * Derived in design from Create's {@code KineticNetwork} (MIT) -- see THIRD-PARTY-LICENSES.md.
 * Create has no inertia; its networks change speed instantly.
 */
public class RotationNetwork {

    /** Ticks between complaints. An overloaded network is more insistent than a strained one. */
    private static final int OVERLOADED_INTERVAL = 40;
    private static final int STRAINING_INTERVAL = 80;

    public final long id;

    /** Nodes that drive the network. Their capacity is read live in {@link #recalculate}, like load. */
    private final Set<RotationNode> sources = new HashSet<>();
    /** Every node on the network. */
    private final Set<RotationNode> members = new HashSet<>();

    private float capacitySu;
    /** Demand that does not depend on speed: machines doing work. */
    private float staticLoadSu;
    /** Demand per RPM: friction. Costs nothing while stopped. */
    private float dragSuPerRpm;
    private float inertia = FTuning.MINIMUM_INERTIA.suTicksPerRpm();

    /** Speed the sources are asking for. Set by {@link RotationPropagator}. */
    private float targetRpm;
    /** Speed the network is actually turning at. */
    private float currentRpm;

    /**
     * What belts are doing to this run this tick -- see {@code PulleyBlockEntity}. Pulleys set
     * these during block entity ticking and {@link #tick} clears them after use, so a belt that
     * stops being written (unlinked, broken, unloaded) stops driving or dragging at once.
     */
    private float beltCapacitySu;
    private float beltTargetRpm;
    private float beltLoadSu;

    /**
     * What something outside the block network is doing to this run this tick -- see
     * {@link #driveExternally}. Cleared by {@link #tick} after use, like the belt's.
     */
    private float externalPushSu;
    private float externalDragSuPerRpm;
    private float externalInertia;

    /** Counts down to the next complaint, so a labouring network does not smoke every tick. */
    private int ticksUntilComplaint;

    /**
     * How far this run has turned, in degrees, as one rigid object.
     *
     * <h3>Why the network owns this and not the blocks</h3>
     * A shaft run is bolted together: its parts cannot be at different angles. When each block
     * accumulated its own angle they drifted apart the moment their histories differed -- stop one
     * shaft, let it settle, reconnect it, and it would turn at exactly the right speed while
     * visibly out of phase with the shaft it was bolted to.
     * <p>
     * Phase is not physically meaningful on its own; two runs at the same speed may sit at any
     * angle to each other. What matters is that members of the <em>same</em> run agree, which they
     * do for free once there is only one number.
     */
    private float phase;

    public RotationNetwork(long id) {
        this.id = id;
    }

    public void add(RotationNode node) {
        if (!members.add(node))
            return;
        if (node.isSource())
            sources.add(node);
        recalculate();
    }

    public void remove(RotationNode node) {
        if (!members.remove(node))
            return;
        sources.remove(node);
        node.onNetworkChanged(new Su(0), new Su(0));
        if (members.isEmpty()) {
            RotationNetworks.of(node.getLevel()).discard(id);
            return;
        }
        recalculate();
    }

    /**
     * Su actually being demanded right now: working draw plus friction at the current speed.
     * <p>
     * Held as two figures rather than recomputed from the members each tick, so a long run costs
     * no more to simulate than a short one.
     */
    public Su getLoadSu() {
        return new Su(liveLoadSu());
    }

    /**
     * The same figure as {@link #getLoadSu()}, unwrapped, for this class's own arithmetic.
     * <p>
     * Internals hold plain floats -- see {@code core/unit}'s package javadoc -- and the tick loop
     * reads this several times per pass. Going through the wrapper each time would be honest and
     * pointless.
     */
    private float liveLoadSu() {
        return totalStaticLoadSu() + totalDragSuPerRpm() * Math.abs(currentRpm);
    }

    private float totalDragSuPerRpm() {
        return dragSuPerRpm + externalDragSuPerRpm;
    }

    /** Own sources plus whatever a belt is feeding in. */
    private float totalCapacitySu() {
        return capacitySu + beltCapacitySu;
    }

    /** Own machines, whatever a belt is carrying out to another run, and an outside push against it. */
    private float totalStaticLoadSu() {
        return staticLoadSu + beltLoadSu + (externalPushResists() ? Math.abs(externalPushSu) : 0);
    }

    /**
     * Whether this tick's outside push is against the run rather than with it.
     * <p>
     * "Against" means against the drive while anything drives -- so a car rolling back down the
     * hill it stalled on is still load on the engine, and ends in the stall rather than in a run
     * spinning backwards against a running source -- and against the motion while nothing does.
     * With neither, it can only be with the run: a push on a run at rest starts it.
     */
    private boolean externalPushResists() {
        float drive = capacitySu > 0 ? targetRpm : beltTargetRpm;
        float direction = drive != 0 ? Math.signum(drive) : Math.signum(currentRpm);
        return externalPushSu * direction < 0;
    }

    /**
     * A belt or a motor drives this run toward {@code rpm} with {@code capacity}. Capacities add
     * up and the fastest target wins, so two motors on one shaft share its load. The target counts
     * only on a run with no source of its own; the capacity counts either way.
     */
    public void driveByBelt(Rpm rpm, Su capacity) {
        if (Math.abs(rpm.value()) > Math.abs(beltTargetRpm))
            beltTargetRpm = rpm.value();
        beltCapacitySu += capacity.value();
    }

    /** A belt is carrying {@code load} out of this run to the one it drives. */
    public void loadByBelt(Su load) {
        beltLoadSu += load.value();
    }

    /**
     * Something that is not a block on this run -- the free-form drivetrain of
     * {@code docs/feedback_vehicles_spec.md} section 5 -- acts on it this tick. Call every tick it
     * should count; like a belt, one that stops calling stops counting at once. Figures add up,
     * and are given in this run's own frame: the caller refers them through its coupling node's
     * ratio (push by 1/ratio, drag and inertia by ratio squared, as {@link #recalculate} does).
     *
     * <h3>The push is signed, and its two halves do different things</h3>
     * Positive pushes toward +RPM. A push <em>against</em> the run (see
     * {@link #externalPushResists}) is load like a machine's: it eats spin-up surplus, lowers the
     * friction ceiling, brakes a coasting run, and counts toward overstress -- a hill in too tall a
     * gear is a stall. A push <em>with</em> the run is its own acceleration, applied after the run
     * has chased its target and not bounded by it: a car rolling downhill overspeeds the engine,
     * and the ordinary slowing branch pulling back toward target is engine braking, unwritten.
     * <p>
     * An assisting push never offsets anybody else's overstress (author ruling, 2026-10-11): a car
     * coasting downhill does not help a hammer on the same run. A negative term in the overstress
     * test would be a new way for the ledger to lie every time a car bounces over a kerb.
     */
    public void driveExternally(Su push, Drag drag, Inertia inertia) {
        externalPushSu += push.value();
        externalDragSuPerRpm += drag.suPerRpm();
        externalInertia += inertia.suTicksPerRpm();
    }

    public void setTargetRpm(Rpm targetRpm) {
        this.targetRpm = targetRpm.value();
    }

    public Rpm getTargetRpm() {
        return new Rpm(targetRpm);
    }

    public Rpm getCurrentRpm() {
        return new Rpm(currentRpm);
    }

    /** Used when rebuilding a network, so a spinning run does not lose its momentum. */
    public void inheritSpeed(float rpm) {
        this.currentRpm = rpm;
    }

    /**
     * Carry the angle across a rebuild, alongside the momentum.
     * <p>
     * A fresh network starts at zero, and rebuilding is not a physical event -- it happens
     * whenever anything anywhere on the run changes. Without this, cutting a shaft free snapped
     * the whole severed run to straight, which looked exactly like it had stopped dead and
     * quietly hid the coast-down it was actually doing.
     * <p>
     * Which angle gets inherited is decided by which node is the reference, and that falls out
     * correctly on its own: splitting a run takes the reference from the severed side, so it
     * keeps its own angle and nothing jumps; joining a run takes it from the driving source, so
     * the newcomer snaps into line with what it was bolted to.
     */
    public void inheritPhase(float degrees) {
        this.phase = degrees % 360f;
    }

    /**
     * Advance the network one tick: move current speed toward target.
     * <p>
     * Called once per level tick by {@link RotationTicker}, not by the blocks -- momentum belongs
     * to the network, and letting each block nudge it would advance it once per member.
     */
    public void tick() {
        tickSpeed();
        beltCapacitySu = 0;
        beltTargetRpm = 0;
        beltLoadSu = 0;
        externalPushSu = 0;
        externalDragSuPerRpm = 0;
        externalInertia = 0;
    }

    private void tickSpeed() {
        advancePhase();

        // Before the early-out below: a network that is stuck at zero because it is overloaded is
        // exactly the case worth complaining about, and it never changes speed.
        if (!members.isEmpty())
            complainIfLabouring(members.iterator().next().getLevel());

        float effectiveTarget = isOverstressed() ? 0 : capacitySu > 0 ? targetRpm : beltTargetRpm;
        float liveLoad = liveLoadSu();
        float capacity = totalCapacitySu();
        float staticLoad = totalStaticLoadSu();
        float drag = totalDragSuPerRpm();
        float totalInertia = Math.max(inertia + externalInertia, FTuning.MINIMUM_INERTIA.suTicksPerRpm());
        // An outside push (driveExternally) keeps the run live even at its target or at rest, so
        // the three shortcuts below that assume nothing but sources push -- settled, braking
        // floor, snap to zero -- stand down while there is one.
        boolean pushed = externalPushSu != 0;
        // Decided once, before the chase moves the speed: a push that brakes the run to zero this
        // tick must not also be applied as an assist the same tick.
        boolean assisted = pushed && !externalPushResists();

        // Friction rises with speed, so a run can be unable to reach the speed its source is
        // asking for. It is not blocked from trying -- it simply runs out of surplus torque
        // first and settles there. That settling point is the whole of "shaft loss".
        if (effectiveTarget != 0 && drag > 0) {
            float terminal = Math.max(0, capacity - staticLoad) / drag;
            if (Math.abs(effectiveTarget) > terminal)
                effectiveTarget = Math.signum(effectiveTarget) * terminal;
        }

        if (currentRpm == effectiveTarget && !pushed)
            return;

        // From standstill, any non-zero target is speeding up -- signum(0) matches nothing.
        boolean speedingUp = Math.abs(effectiveTarget) > Math.abs(currentRpm)
                && (currentRpm == 0 || Math.signum(effectiveTarget) == Math.signum(currentRpm));
        boolean reversing = effectiveTarget != 0 && currentRpm != 0
                && Math.signum(effectiveTarget) != Math.signum(currentRpm);

        // Accelerating is paid for out of spare power; slowing is driven by whatever is dragging.
        // A reversal has to brake to a stop first, so it is charged like slowing down.
        // Accelerating force ignores friction and the terminal clamp above handles it instead.
        // Using live surplus here is more literal but behaves badly: surplus goes to zero exactly
        // at terminal speed, so a run approaches its own top speed asymptotically and spends
        // fifteen seconds creeping the last revolution. Constant drive up to a hard ceiling is
        // both more readable and far easier to reason about.
        // The braking floor would fight a gentle outside push, so it only applies without one.
        float force = (speedingUp && !reversing)
                ? Math.max(0, capacity - staticLoad)
                : pushed ? liveLoad : Math.max(liveLoad, FTuning.MINIMUM_BRAKING_SU.value());

        float step = force / totalInertia * FTuning.INERTIA_RESPONSE;

        if (Math.abs(effectiveTarget - currentRpm) <= step)
            currentRpm = effectiveTarget;
        else
            currentRpm += Math.signum(effectiveTarget - currentRpm) * step;

        // A push with the run, after the chase and unbounded by the target -- see driveExternally.
        // A push against it is already in staticLoad.
        if (assisted)
            currentRpm += externalPushSu / totalInertia * FTuning.INERTIA_RESPONSE;

        // Not while pushed: a slow creep downhill would be zeroed every tick.
        if (!pushed && effectiveTarget == 0 && Math.abs(currentRpm) < FTuning.STOPPED_RPM_THRESHOLD.value())
            currentRpm = 0;

        for (RotationNode member : members)
            member.onNetworkSpeedChanged();
    }

    /** Advance the shared angle. Called every tick, including when the speed is settled. */
    public void advancePhase() {
        if (currentRpm != 0)
            phase = (phase + currentRpm * RotationNode.DEGREES_PER_TICK_PER_RPM) % 360f;
    }

    public float getPhase() {
        return phase;
    }

    /**
     * Let a labouring network be seen and heard.
     *
     * <h2>Why this is not optional polish</h2>
     * An overloaded network simply fails to turn, and a network near its limit takes a minute and
     * a half to reach speed instead of two seconds. Both were completely silent -- the factory
     * just did not work, with nothing to look at and nowhere to start. That is the same failure
     * as a linkage facing the wrong way: no error, it just quietly does nothing.
     * <p>
     * Philosophy 8 permits this for free because it is an <em>adjective</em>. Smoke and a creak
     * say "this one, and it is struggling" without handing over the Su figures an instrument is
     * supposed to sell. A player learns where to look; they still cannot read the ledger.
     */
    private void complainIfLabouring(Level level) {
        if (!(level instanceof ServerLevel server) || members.isEmpty())
            return;

        boolean overloaded = isOverstressed();
        boolean straining = !overloaded && totalCapacitySu() > 0
                && liveLoadSu() > totalCapacitySu() * FTuning.STRAINING_LOAD_FRACTION;
        if (!overloaded && !straining) {
            ticksUntilComplaint = 0;
            return;
        }

        if (--ticksUntilComplaint > 0)
            return;
        ticksUntilComplaint = overloaded ? OVERLOADED_INTERVAL : STRAINING_INTERVAL;

        // Complain at the sources. They are where a person would put their hand to feel a machine
        // labouring, there are far fewer of them than members, and it points at the half of the
        // problem the player can actually do something about.
        for (RotationNode source : sources) {
            BlockPos pos = source.getBlockPos();
            server.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    overloaded ? 4 : 1, 0.2, 0.1, 0.2, 0.01);
            server.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS,
                    overloaded ? 0.35f : 0.2f, overloaded ? 0.45f : 0.6f);
        }
    }

    /**
     * Recompute the ledger and tell every member, but only when something actually moved.
     * Members are told whenever a figure changes, because "am I overstressed" is a network
     * answer and a node has no way to work it out for itself.
     */
    public void recalculate() {
        float newCapacity = 0;
        float newStaticLoad = 0;
        float newDrag = 0;
        float newInertia = 0;

        dropStale(sources);
        for (RotationNode source : sources)
            newCapacity += source.getCapacitySu().value();

        dropStale(members);
        for (RotationNode member : members) {
            // Friction and mass are referred to the network's own shaft through the square of the
            // gear ratio, which is the standard result and not a fudge: a geared-up branch turns
            // faster, so its bearings eat more and its mass is harder to accelerate, both by the
            // same factor twice over. It is also the answer to "does gearing cost anything" --
            // it does, continuously, without a rule having to say so.
            //
            // A machine's working draw is deliberately NOT referred. Referring it would make a
            // geared-down machine cost the network less -- fewer blows a second is less power --
            // which is physically right and a real balance decision, since it turns a gear train
            // into a way of fitting more hammers on one wheel. Left flat for now because 80 Su is
            // a figure stamped on the block (§17), and a spec that changed with the gearing would
            // stop being one.
            float referred = member.getRatio() * member.getRatio();
            newStaticLoad += member.getLoadSu().value();
            newDrag += member.getDragSuPerRpm().suPerRpm() * referred;
            newInertia += member.getInertia().suTicksPerRpm() * referred;
        }

        inertia = newInertia;

        if (newCapacity == capacitySu && newStaticLoad == staticLoadSu && newDrag == dragSuPerRpm)
            return;
        capacitySu = newCapacity;
        staticLoadSu = newStaticLoad;
        dragSuPerRpm = newDrag;
        for (RotationNode member : members)
            member.onNetworkChanged(new Su(capacitySu), getLoadSu());
    }

    /**
     * Drop entries whose block entity is no longer the one at its position. A chunk reload
     * replaces block entities, and a stale key would keep the old object alive with its Su
     * counted forever.
     */
    private void dropStale(Set<RotationNode> ledger) {
        for (Iterator<RotationNode> it = ledger.iterator(); it.hasNext(); ) {
            RotationNode node = it.next();
            if (node.getLevel() == null || node.getLevel().getBlockEntity(node.getBlockPos()) != node)
                it.remove();
        }
    }

    /**
     * True only when something is driving the network and it still cannot turn.
     * <p>
     * Two things are deliberately <em>not</em> overstress. A network with no source is not
     * overloaded, it is unpowered -- an idle shaft reporting OVERSTRESSED because its own
     * friction exceeds a capacity of zero is nonsense. And a network that simply cannot reach
     * the speed asked of it is not faulty either; it settles at a lower speed, which is what
     * friction is supposed to feel like.
     * <p>
     * What remains is the real fault: machines demanding more than the supply can give even
     * before anything starts turning.
     */
    public boolean isOverstressed() {
        return totalCapacitySu() > 0 && totalStaticLoadSu() > totalCapacitySu();
    }

    /** This run's own sources only -- not what a belt is feeding in this tick. */
    public Su getCapacitySu() {
        return new Su(capacitySu);
    }

    public Su getStaticLoadSu() {
        return new Su(staticLoadSu);
    }

    public Drag getDragSuPerRpm() {
        return new Drag(dragSuPerRpm);
    }

    public Inertia getInertia() {
        return new Inertia(inertia);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public int size() {
        return members.size();
    }
}
