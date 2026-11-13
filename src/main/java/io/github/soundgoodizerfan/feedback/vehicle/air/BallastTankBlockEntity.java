// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import org.joml.Vector3d;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Ballast: water in a tank, weighed by its material and pulled down on the craft at the tank (Sable
 * counts only blocks, so the water's weight is a force, equal to extra mass for how the craft climbs).
 * Dumping it lightens the craft. Idle, it neither ticks nor works; only a dump costs anything.
 */
public class BallastTankBlockEntity extends AnalogPartBlockEntity implements BlockEntitySubLevelActor {

    private final FluidTank tank = new FluidTank(VehicleTuning.BALLAST_TANK_MB) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            FluidStack f = getFluid();
            weightMu = f.getAmount() * muPerMb(f);
        }
    };
    private boolean hand;
    private float card, owed;
    private volatile float weightMu;
    private final ForceTotal force = new ForceTotal();
    private final Vector3d at = new Vector3d(), down = new Vector3d(), local = new Vector3d();

    public BallastTankBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.BALLAST_TANK_ENTITY.get(), pos, state);
    }

    private static float muPerMb(FluidStack f) {
        if (f.isEmpty())
            return 0f;
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(f.getFluid()));
        return m == null ? 1f : m.density().muPerMb();
    }

    public FluidTank getTank() {
        return tank;
    }

    public boolean handOpen() {
        return hand;
    }

    public void toggleHand() {
        hand = !hand;
        setChanged();
    }

    public float opening() {
        return Math.max(hand ? 1f : 0f, card);
    }

    @Override
    public void setLevel(int channel, float level) {
        card = Math.clamp(level, 0f, 1f);
    }

    @Override
    public String[] channelKeys() {
        return new String[] {"feedback.analog.ballast_dump"};
    }

    public void tickServer() {
        float open = opening();
        if (open <= 0f || tank.isEmpty() || !(level instanceof ServerLevel server))
            return;
        owed += VehicleTuning.BALLAST_DUMP_MB_PER_TICK * open;
        int dump = (int) owed;
        if (dump <= 0)
            return;
        owed -= dump;
        tank.drain(dump, IFluidHandler.FluidAction.EXECUTE);
        net.minecraft.world.phys.Vec3 w = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition);
        if (dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(this) instanceof ServerSubLevel body)
            w = body.logicalPose().transformPosition(w);
        server.sendParticles(ParticleTypes.FALLING_WATER, w.x, w.y - 0.6, w.z, 2, 0.2, 0.0, 0.2, 0.0);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        float mu = weightMu;
        if (mu <= 0f)
            return;
        double weight = mu / Units.MU_PER_KPG * FTuning.GRAVITY_BLOCKS_PER_S2;
        subLevel.logicalPose().transformNormalInverse(down.set(0, -weight * dt, 0), local);
        force.applyImpulseAtPoint(subLevel.getMassTracker(), at.set(worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5), local);
        handle.applyForcesAndReset(force);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Water", tank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("Hand", hand);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Water"))
            tank.readFromNBT(registries, tag.getCompound("Water"));
        hand = tag.getBoolean("Hand");
        FluidStack f = tank.getFluid();
        weightMu = f.getAmount() * muPerMb(f);
    }
}
