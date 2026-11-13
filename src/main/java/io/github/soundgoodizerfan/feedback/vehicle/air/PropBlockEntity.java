// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A propeller (flight A2): a load on the shaft that pushes the craft along {@link PropBlock#FACING}.
 * Its numbers are the design's and the air's, nothing else. With {@code n} turns a second, disc
 * diameter {@code D}, air density {@code rho} and airspeed {@code V} along the shaft, the advance ratio
 * is {@code J = V / (n D)}; thrust is {@code Ct rho n^2 D^4} with {@code Ct = Ct0 (1 - J / J0)} (to
 * zero at {@code J0}, which pitch sets: a fine prop pulls hard and quits early, a coarse one pulls
 * less and keeps on to speed), and the power it absorbs is {@code Cp rho n^3 D^5}. That torque is a drag
 * on the shaft, and the same torque pushes back on the airframe against the spin, so a lone prop rolls
 * its craft. Thin air thins both; a blade tip faster than its metal survives (or past the speed
 * of sound) loses thrust and wears, which is misuse; water is air at 816 times the density.
 *
 * <p>Cost: no work unless the shaft turns. The server tick reads one speed and one density; the
 * physics step is one impulse and one torque with nothing allocated.
 */
public class PropBlockEntity extends RotationNode implements BlockEntitySubLevelActor, io.github.soundgoodizerfan.feedback.machine.Mendable {

    /** The modular build this was made from, kept so its parts' attunements act while it is placed and it can be reworked after it is picked up. */
    @Nullable
    private io.github.soundgoodizerfan.feedback.modular.type.ModularDesign build;
    @Nullable
    private volatile PropDesign design;
    private volatile float condition = 1f;
    /** Written by the server tick, read by the physics thread. */
    private volatile float rpm;
    private volatile float rhoKgM3;
    private volatile float tipLimit = Float.MAX_VALUE;
    private float dragSu;
    /** 0 idle, 1 pulling, 2 stalled (past its zero-thrust advance ratio or too thin an air), 3 blade tips over their limit; set by the physics step. */
    private volatile int status;

    private final ForceTotal force = new ForceTotal();
    private final Vector3d centre = new Vector3d(), dir = new Vector3d(), lin = new Vector3d(), up = new Vector3d(),
            worldDir = new Vector3d(), pushed = new Vector3d(), torque = new Vector3d();

    public PropBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.PROPELLER_ENTITY.get(), pos, state);
    }

    @Nullable
    public PropDesign design() {
        return design;
    }

    /** 0 idle, 1 pulling, 2 stalled, 3 over-speed. */
    public int status() {
        return status;
    }

    public float condition() {
        return condition;
    }

    @Override
    public float mendableCondition() {
        return condition;
    }

    @Override
    public void mend(float by) {
        condition = Math.min(1f, condition + by);
        setChanged();
    }


    @Nullable
    private Material material() {
        return design == null ? null : MaterialTable.get().materials().get(design.material());
    }

    /** Disc area factor from the blade count, against a three-blade prop. */
    private static float solidity(PropDesign d) {
        return (float) Math.pow(d.blades() / 3f, VehicleTuning.PROP_BLADE_EXPONENT);
    }

    // --- the shaft ------------------------------------------------------------------------------

    /** The power coefficient: what the disc absorbs from the shaft, a share of its zero-airspeed thrust times where it quits. */
    private static float cp(PropDesign d) {
        return VehicleTuning.PROP_CP_PER_CT0_J0 * VehicleTuning.PROP_CT0[d.pitch()] * VehicleTuning.PROP_J0[d.pitch()] * solidity(d);
    }

    /** Torque the disc takes from the shaft in N m at {@code n} turns a second and density {@code rho}: {@code Cp rho n^2 D^5 / 2 pi}. */
    private static float torqueNm(PropDesign d, float n, float rho) {
        return (float) (cp(d) * rho * n * n * Math.pow(d.diameter(), 5) / (2 * Math.PI));
    }

    @Override
    public Drag getDragSuPerRpm() {
        return dragSu <= 0f ? NO_DRAG : new Drag(dragSu);
    }

    // --- the server tick --------------------------------------------------------------------------

    public void tickServer() {
        PropDesign d = design;
        if (level == null || d == null)
            return;
        float turn = getRpm().value();
        rpm = turn;
        Vec3 at = Vec3.atCenterOf(worldPosition);
        if (SableCompanion.INSTANCE.getContaining(this) instanceof ServerSubLevel body)
            at = body.logicalPose().transformPosition(at);
        double y = at.y;
        boolean wet = level.getFluidState(BlockPos.containing(at)).is(FluidTags.WATER);
        float rho = wet ? 1.225f * VehicleTuning.WATER_OVER_AIR_DENSITY
                : 2 * VehicleTuning.AIR_HALF_DENSITY_KPG_PER_BLOCK3 * 1000f * AirDensityTable.at(level, y);
        rhoKgM3 = rho;
        Material m = MaterialTable.get().materials().get(d.material());
        float limit = wet ? VehicleTuning.PROP_TIP_SPEED_WATER : VehicleTuning.PROP_TIP_SPEED_CAP;
        if (m != null && m.mechanical().yieldStrength().isPresent() && m.density().muPerMb() > 0f)
            limit = Math.min(limit, VehicleTuning.PROP_TIP_SPEED_PER_SQRT_SPECIFIC_YIELD
                    * (float) Math.sqrt(m.mechanical().yieldStrength().get().value() / m.density().muPerMb()));
        tipLimit = limit;
        float n = Math.abs(turn) / 60f;
        float tip = (float) (Math.PI * d.diameter() * n);
        if (tip > limit && condition > VehicleTuning.PROP_CONDITION_FLOOR) {
            condition = Math.max(VehicleTuning.PROP_CONDITION_FLOOR, condition - VehicleTuning.PROP_WEAR_PER_TICK * (tip / limit - 1f) * io.github.soundgoodizerfan.feedback.modular.attunement.MachineKinds.wearShare(this));
            setChanged();
        }
        float drag = Math.abs(turn) < 1f ? 0f : torqueNm(d, n, rho) / Math.abs(turn);
        if (drag > 0f ? Math.abs(drag - dragSu) > 0.15f * dragSu : dragSu > 0f) {
            dragSu = drag;
            RotationPropagator.rebuildFrom(level, worldPosition);
        }
    }

    // --- the physics step -------------------------------------------------------------------------

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        PropDesign d = design;
        float turn = rpm, rho = rhoKgM3;
        if (d == null || rho <= 0f || Math.abs(turn) < 1f) {
            status = 0;
            return;
        }
        Direction facing = getBlockState().getValue(PropBlock.FACING);
        dir.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        var pose = subLevel.logicalPose();
        pose.transformNormal(dir, worldDir);
        handle.getLinearVelocity(lin);
        // A reversed shaft pushes the other way and is measured against motion along its own thrust direction.
        float sense = Math.signum(turn);
        double airspeed = Math.max(0, lin.dot(worldDir) * sense);
        float n = Math.abs(turn) / 60f;
        double diameter = d.diameter();
        double j = airspeed / (n * diameter);
        float ct0 = VehicleTuning.PROP_CT0[d.pitch()], j0 = VehicleTuning.PROP_J0[d.pitch()];
        double ct = ct0 * solidity(d) * Math.max(0, 1 - j / j0);
        double tip = Math.PI * diameter * n, limit = tipLimit;
        status = tip > limit ? 3 : ct <= 1e-4 ? 2 : 1;
        if (tip > limit)
            ct *= limit / tip;
        double thrustKpg = ct * rho * n * n * Math.pow(diameter, 4) / 1000.0 * condition; // N -> kpg blocks / s^2
        if (sense < 0)
            thrustKpg *= VehicleTuning.PROP_REVERSE_EFFICIENCY;
        centre.set(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        pushed.set(dir).mul(sense * thrustKpg * dt);
        force.applyImpulseAtPoint(subLevel.getMassTracker(), centre, pushed);
        // The spin's reaction on the airframe: the torque the disc takes from the shaft, against its sense.
        double q = torqueNm(d, n, rho) / Units.SU_PER_KPG_TORQUE;
        force.applyTorqueImpulse(torque.set(dir).mul(-Math.signum(turn) * q * dt));
        handle.applyForcesAndReset(force);
    }

    // --- save ---------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        design = input.get(FVehicles.PROP_DESIGN.get());
        build = input.get(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (design != null)
            components.set(FVehicles.PROP_DESIGN.get(), design);
        if (build != null)
            components.set(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get(), build);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (design != null)
            PropDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(t -> tag.put("Design", t));
        if (build != null)
            io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.CODEC.encodeStart(NbtOps.INSTANCE, build).ifSuccess(t -> tag.put("Build", t));
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        design = tag.contains("Design") ? PropDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().orElse(null) : null;
        build = tag.contains("Build") ? io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Build")).result().orElse(null) : null;
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
