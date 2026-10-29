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
package io.github.soundgoodizerfan.feedback.vehicle;

import org.joml.Vector3d;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.engine.FuelBlend;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A liquid-fuel thruster (petrochemistry spec 9.2): a fuel from the line, an oxidiser, a combustion
 * chamber and a nozzle, pushing the body it is part of along its facing at the block. The fuel is a
 * {@link FuelBlend}, so whatever arrives mixes by mass and nothing is filtered by name; thrust scales
 * with the blend's flame temperature, and the burn is the chamber's, not a timer.
 * <p>
 * The oxidiser is air, which a thruster has only in an atmosphere (not the End, not underwater), or
 * the fuel's own {@code oxygen_balance}: a fuel that carries oxygen burns anywhere, and a mixture is
 * judged by the sum. With neither, it makes no thrust: a physical impossibility, not a refusal.
 * There is no wear, because nothing about running it correctly or not damages it yet.
 * [OPEN] nozzle vectoring: thrust is along the facing for now.
 */
public class ThrusterBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    private final FuelBlend fuel = new FuelBlend(this::setChanged);
    private boolean running;
    private float owed;
    /** Share of a reference fuel's thrust this tick; read by the physics step, written by the server tick. */
    private volatile float power;

    public ThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.THRUSTER_ENTITY.get(), pos, state);
    }

    public FuelBlend getFuel() {
        return fuel;
    }

    public boolean running() {
        return running;
    }

    public void setRunning(boolean on) {
        running = on;
        setChanged();
    }

    /**
     * The push it would make with the fuel aboard and air to burn, in Sable's force unit: what the
     * Lift's balance gauge weighs against the body. Zero with an empty tank; it ignores whether the
     * switch is on, since the gauge is read before a flight.
     */
    public float potentialThrust() {
        return fuel.isEmpty() ? 0f : FTuning.THRUSTER_FORCE * fuel.flame() / FTuning.ENGINE_REFERENCE_FLAME_TU;
    }

    public float power() {
        return power;
    }

    /** Air by place: an atmosphere, with the nozzle in the open. */
    private boolean hasAir(Level level) {
        return AirDensityTable.at(level) >= FTuning.THRUSTER_MIN_AIR_DENSITY && level.getFluidState(worldPosition).isEmpty();
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server))
            return;
        float flame = running && !fuel.isEmpty() ? fuel.flame() : 0f;
        if (flame <= 0f || (!hasAir(server) && fuel.oxygen() <= 0f)) {
            power = 0f;
            return;
        }
        // Without air, what the fuel carries is the whole supply, so thrust is cut to it.
        float oxidised = hasAir(server) ? 1f : Math.min(1f, fuel.oxygen());
        float factor = flame / FTuning.ENGINE_REFERENCE_FLAME_TU;
        power = factor * oxidised;
        owed += FTuning.THRUSTER_FUEL_MB_PER_TICK / factor;
        int drawn = (int) owed;
        owed -= drawn;
        if (drawn > 0)
            fuel.burn(drawn);
        if (server.getGameTime() % 2 == 0) {
            Direction out = getBlockState().getValue(ThrusterBlock.FACING).getOpposite();
            server.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5 + out.getStepX() * 0.7,
                    worldPosition.getY() + 0.5 + out.getStepY() * 0.7, worldPosition.getZ() + 0.5 + out.getStepZ() * 0.7,
                    2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    /** Every physics step, on the server: a push at the block along the facing, in the body's own frame. */
    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        float p = power;
        if (p <= 0f)
            return;
        Direction thrust = getBlockState().getValue(ThrusterBlock.FACING);
        Vector3d at = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        Vector3d impulse = new Vector3d(thrust.getStepX(), thrust.getStepY(), thrust.getStepZ())
                .mul(FTuning.THRUSTER_FORCE * p * timeStep);
        ForceTotal one = new ForceTotal();
        one.applyImpulseAtPoint(subLevel.getMassTracker(), at, impulse);

        handle.applyForcesAndReset(one);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Running", running);
        CompoundTag fuelTag = new CompoundTag();
        fuel.save(fuelTag);
        tag.put("Fuel", fuelTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        running = tag.getBoolean("Running");
        if (tag.contains("Fuel"))
            fuel.load(tag.getCompound("Fuel"));
    }
}
