// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3d;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;
import io.github.soundgoodizerfan.feedback.vehicle.ThrusterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * One cell of a lift-gas envelope. Lift is physical: the air the gas displaces weighs more than the
 * gas, and the difference lifts the cell at its own block, so where the cells sit trims the ship. The
 * numbers come from materials (air and the gas) and the dimension's density curve at the cell's height;
 * there is no lift stat.
 *
 * <p>Adjacent cells are one envelope: the lowest-positioned cell leads, finds the connected group when
 * a neighbour changes, and every ten ticks evens the gas out across it, spends its leaks, watches the
 * skin stretch and runs the fire. Gas stretches as air thins (a cell holds a volume, not a pressure),
 * so a full cell climbing past its skin's limit weeps until vented; a hole from an arrow leaks until
 * patched. A cell whose gas burns lights the whole group: the gas goes at its material's burn rate,
 * creatures nearby catch fire and a cloth or rubber skin goes with it. Wear is only misuse: a cell flown
 * within its skin's stretch never leaks.
 *
 * <p>Cost: the tick returns at once except one in ten ticks, and only a group's leader does work then.
 * The physics step is one density lookup and one impulse, allocation-free.
 */
public class GasCellBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    private static final int SLOW = 10;

    private final FluidTank tank = new FluidTank(VehicleTuning.GAS_CELL_MB, Gases::isGas) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            mirror();
        }
    };
    private float hole;
    private float seepOwed;
    private boolean burning;
    private boolean dirty = true;
    /** Lowest-positioned cell of this group; null when this cell is it. */
    private BlockPos leader;
    private final List<BlockPos> members = new ArrayList<>();

    // Read on the physics thread, written on the server thread.
    private record Gas(float mb, float mu) {
    }

    private volatile Gas gas = new Gas(0f, 0f);
    private final ForceTotal force = new ForceTotal();
    private final Vector3d centre = new Vector3d(), world = new Vector3d(), up = new Vector3d(), local = new Vector3d();

    public GasCellBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.GAS_CELL_ENTITY.get(), pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    public Skin skin() {
        return getBlockState().getBlock() instanceof GasCellBlock b ? b.skin() : Skin.CLOTH;
    }

    public float hole() {
        return hole;
    }

    public boolean burning() {
        return burning;
    }

    public int groupSize() {
        return leader == null ? Math.max(1, members.size()) : 1;
    }

    private void mirror() {
        FluidStack f = tank.getFluid();
        gas = new Gas(f.getAmount(), f.getAmount() * Gases.muPerMb(f));
    }

    public void invalidate() {
        dirty = true;
        if (level != null && leader != null && level.getBlockEntity(leader) instanceof GasCellBlockEntity l)
            l.dirty = true;
    }

    /** An arrow or a bullet through the skin. */
    public void puncture() {
        hole += VehicleTuning.GAS_HOLE_LEAK_MB_PER_TICK;
        setChanged();
    }

    /** Seals holes; whether there was anything to seal. */
    public boolean patch() {
        if (hole <= 0f)
            return false;
        hole = 0f;
        setChanged();
        return true;
    }

    // --- the slow tick --------------------------------------------------------------------------

    public void tickServer() {
        if (!(level instanceof ServerLevel server) || (server.getGameTime() + worldPosition.hashCode()) % SLOW != 0)
            return;
        if (dirty)
            regroup(server);
        if (leader != null)
            return;
        List<GasCellBlockEntity> cells = new ArrayList<>(members.size());
        for (BlockPos p : members) {
            if (!(server.getBlockEntity(p) instanceof GasCellBlockEntity c)) {
                dirty = true;
                return;
            }
            cells.add(c);
        }
        fire(server, cells);
        weep(server, cells);
        even(cells);
    }

    private void regroup(ServerLevel server) {
        dirty = false;
        List<BlockPos> found = new ArrayList<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();
        queue.add(worldPosition);
        seen.add(worldPosition);
        BlockPos lowest = worldPosition;
        while (!queue.isEmpty() && found.size() < VehicleTuning.GAS_GROUP_MAX_CELLS) {
            BlockPos p = queue.poll();
            found.add(p);
            if (p.asLong() < lowest.asLong())
                lowest = p;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && server.getBlockEntity(n) instanceof GasCellBlockEntity) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        if (!lowest.equals(worldPosition)) {
            leader = lowest;
            members.clear();
            if (server.getBlockEntity(lowest) instanceof GasCellBlockEntity l)
                l.dirty = true;
            return;
        }
        leader = null;
        members.clear();
        members.addAll(found);
        for (BlockPos p : found)
            if (!p.equals(worldPosition) && server.getBlockEntity(p) instanceof GasCellBlockEntity c) {
                c.leader = worldPosition;
                c.dirty = false;
                c.members.clear();
            }
    }

    private static void even(List<GasCellBlockEntity> cells) {
        FluidStack kind = FluidStack.EMPTY;
        int total = 0;
        for (GasCellBlockEntity c : cells) {
            FluidStack f = c.tank.getFluid();
            if (f.isEmpty())
                continue;
            if (kind.isEmpty())
                kind = f;
            else if (!FluidStack.isSameFluidSameComponents(kind, f))
                return; // mixed gases do not blend; the player vents one
            total += f.getAmount();
        }
        if (kind.isEmpty())
            return;
        int n = cells.size(), share = total / n, extra = total - share * n;
        for (GasCellBlockEntity c : cells) {
            int want = share + (extra-- > 0 ? 1 : 0);
            if (c.tank.getFluidAmount() != want)
                c.tank.setFluid(kind.copyWithAmount(want));
        }
    }

    /** Skin stretch past its limit and holes: gas lost, a puff where it goes. */
    private void weep(ServerLevel server, List<GasCellBlockEntity> cells) {
        for (GasCellBlockEntity c : cells) {
            if (c.tank.isEmpty())
                continue;
            double d = Math.max(AirDensityTable.at(server, c.worldY()), 0.02);
            float stretch = (float) (c.tank.getFluidAmount() / (VehicleTuning.GAS_CELL_MB * d));
            float over = Math.max(0f, stretch - c.skin().maxStretch);
            float lost = (c.hole + over * VehicleTuning.GAS_LEAK_PER_STRETCH) * SLOW;
            // The skin's own seep is a fraction of a mB a pass: carried until it makes a whole one.
            c.seepOwed += c.skin().seep() * c.tank.getFluidAmount() / VehicleTuning.GAS_CELL_MB * SLOW;
            int seeped = (int) c.seepOwed;
            c.seepOwed -= seeped;
            if (lost <= 0f && seeped <= 0)
                continue;
            c.tank.drain((lost > 0f ? Math.max(1, Math.round(lost)) : 0) + seeped, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            Vec3 w = c.worldCentre();
            server.sendParticles(ParticleTypes.CLOUD, w.x, w.y + 0.5, w.z, 1, 0.2, 0.1, 0.2, 0.01);
        }
    }

    private void fire(ServerLevel server, List<GasCellBlockEntity> cells) {
        FluidStack kind = FluidStack.EMPTY;
        for (GasCellBlockEntity c : cells)
            if (!c.tank.isEmpty()) {
                kind = c.tank.getFluid();
                break;
            }
        int burnTicks = Gases.burnTicks(kind);
        if (!burning && burnTicks > 0)
            for (GasCellBlockEntity c : cells)
                if (!c.tank.isEmpty() && c.lit(server)) {
                    for (GasCellBlockEntity all : cells)
                        all.burning = true;
                    break;
                }
        if (!burning)
            return;
        float share = Math.max(VehicleTuning.GAS_BURN_MIN_SHARE, burnTicks > 0 ? (float) SLOW / burnTicks : 1f);
        boolean left = false;
        for (GasCellBlockEntity c : cells) {
            if (!c.tank.isEmpty()) {
                int take = Math.max(1, Math.round(c.tank.getFluidAmount() * share));
                c.tank.drain(take, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
            if (!c.tank.isEmpty())
                left = true;
            Vec3 w = c.worldCentre();
            server.sendParticles(ParticleTypes.FLAME, w.x, w.y, w.z, 4, 0.4, 0.4, 0.4, 0.02);
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(w, w).inflate(1.6)))
                e.igniteForSeconds(VehicleTuning.GAS_BURN_ENTITY_SECONDS);
            for (Direction d : Direction.values()) {
                BlockPos n = c.worldPosition.relative(d);
                if (server.getBlockState(n).isAir() && server.random.nextFloat() < VehicleTuning.GAS_BURN_IGNITE_CHANCE)
                    server.setBlockAndUpdate(n, Blocks.FIRE.defaultBlockState());
            }
        }
        if (left)
            return;
        for (GasCellBlockEntity c : cells) {
            c.burning = false;
            if (c.skin().burns)
                server.setBlockAndUpdate(c.worldPosition, Blocks.FIRE.defaultBlockState());
        }
    }

    /** A flame at this cell's skin: fire, lava, or a thruster's nozzle running against it. */
    private boolean lit(ServerLevel server) {
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            BlockState s = server.getBlockState(n);
            if (s.is(BlockTags.FIRE) || s.getFluidState().is(FluidTags.LAVA) || s.is(io.github.soundgoodizerfan.feedback.registry.FTags.HOT_SURFACE))
                return true;
            if (server.getBlockEntity(n) instanceof ThrusterBlockEntity t && t.power() > 0f)
                return true;
        }
        return false;
    }

    // --- place in the world -----------------------------------------------------------------------

    private Vec3 worldCentre() {
        Vec3 here = Vec3.atCenterOf(worldPosition);
        if (SableCompanion.INSTANCE.getContaining(this) instanceof ServerSubLevel body)
            return body.logicalPose().transformPosition(here);
        return here;
    }

    private double worldY() {
        return worldCentre().y;
    }

    // --- physics (Sable's thread) -------------------------------------------------------------------

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        Gas held = gas;
        float mb = held.mb();
        if (mb <= 0f || level == null)
            return;
        centre.set(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        var pose = subLevel.logicalPose();
        double y = pose.transformPosition(centre, world).y;
        double airMu = mb * Gases.airMuPerMb() * AirDensityTable.at(level, y);
        double lift = (airMu - held.mu()) / Units.MU_PER_KPG * FTuning.GRAVITY_BLOCKS_PER_S2; // kpg blocks / s^2
        if (Math.abs(lift) < 1e-9)
            return;
        pose.transformNormalInverse(up.set(0, lift * dt, 0), local);
        force.applyImpulseAtPoint(subLevel.getMassTracker(), centre, local);
        handle.applyForcesAndReset(force);
    }

    // --- save ---------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Gas", tank.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("Hole", hole);
        tag.putBoolean("Burning", burning);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Gas"))
            tank.readFromNBT(registries, tag.getCompound("Gas"));
        hole = tag.getFloat("Hole");
        burning = tag.getBoolean("Burning");
        mirror();
        dirty = true;
    }
}
