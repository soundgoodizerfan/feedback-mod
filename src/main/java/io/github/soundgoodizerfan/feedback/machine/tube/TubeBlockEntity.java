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
package io.github.soundgoodizerfan.feedback.machine.tube;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder;
import io.github.soundgoodizerfan.feedback.core.tube.Capsule;
import io.github.soundgoodizerfan.feedback.core.tube.TubeNetwork;
import io.github.soundgoodizerfan.feedback.core.tube.TubeNetworks;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One block of a tube run. It holds at most one {@link Capsule} (so a full tube backs up behind its
 * leader, a pinned train) and the run's {@link TubeNetwork} owns the air and the schedule. All
 * movement happens in {@link #advance}, called by the run when a capsule is due, never per tick.
 * Adjacent tubes always share a run, so a hop never crosses networks.
 */
public class TubeBlockEntity extends BlockEntity implements PressureHolder {

    @Nullable
    private TubeNetwork network;
    @Nullable
    private Capsule capsule;
    private boolean unloaded;

    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return network == null ? FluidStack.EMPTY : network.ledger.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            return network == null ? 0 : network.ledger.getTankCapacity(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return network != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return network == null ? 0 : network.accept(resource, false, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return network == null ? FluidStack.EMPTY : network.ledger.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return network == null ? FluidStack.EMPTY : network.ledger.drain(maxDrain, action);
        }
    };

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        this(FBlockEntities.TUBE.get(), pos, state);
    }

    protected TubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public TubeNetwork network() {
        return network;
    }

    public void setNetwork(@Nullable TubeNetwork network) {
        this.network = network;
    }

    @Nullable
    public Capsule capsule() {
        return capsule;
    }

    public IFluidHandler getHandler() {
        return handler;
    }

    public TubeBlock.Kind kind() {
        return getBlockState().getBlock() instanceof TubeBlock b ? b.kind() : TubeBlock.Kind.STANDARD;
    }

    public boolean evacuated() {
        return kind() == TubeBlock.Kind.EVACUATED;
    }

    @Override
    public Pu getPressure() {
        return network == null ? new Pu(0f) : network.pressure();
    }

    /** What a compressor or air source does: forces gas into the run past one volume. Returns mB taken. */
    public int compressedFill(FluidStack resource, IFluidHandler.FluidAction action) {
        return network == null ? 0 : network.accept(resource, true, action);
    }

    private static boolean isPod(Capsule c) {
        return c.stack.getItem() instanceof CarrierItem i && i.kind() == CarrierItem.Kind.POD;
    }

    /**
     * Ticks a capsule takes to arrive in this block, or -1 when it cannot be moved in: too heavy for the
     * bore, a pod in a narrow tube, or the run short of pressure (an evacuated tube that holds its vacuum
     * needs none). {@code exit} is the direction of travel, {@code corner} whether it differs from the last.
     */
    public int hopTicks(Capsule c, Direction exit, boolean corner) {
        TubeBlock.Kind k = kind();
        boolean pod = isPod(c);
        if (network == null || (pod && !k.wide))
            return -1;
        float max = k.wide ? FTuning.TUBE_WIDE_BORE_MAX_MU : FTuning.TUBE_BORE_MAX_MU;
        float mass = CarrierItem.mass(c.stack, c.rider != null);
        if (mass > max)
            return -1;
        float p = network.pressure().value();
        float drive;
        if (k == TubeBlock.Kind.EVACUATED && p <= FTuning.TUBE_EVAC_MAX_PU)
            drive = FTuning.TUBE_EVAC_FACTOR;
        else if (p < FTuning.TUBE_MIN_DRIVE_PU)
            return -1;
        else {
            drive = Math.max(0.25f, Math.min(FTuning.TUBE_MAX_DRIVE_FACTOR, p / FTuning.TUBE_FULL_DRIVE_PU));
            if (k == TubeBlock.Kind.LINED || k == TubeBlock.Kind.EVACUATED)
                drive *= FTuning.TUBE_LINER_FACTOR;
        }
        if (c.stack.getItem() instanceof CarrierItem i && i.kind() == CarrierItem.Kind.MAGNETIC && ringed())
            drive *= FTuning.TUBE_LINEAR_FACTOR;
        float t = FTuning.TUBE_BASE_HOP_TICKS * (1f + mass / (max / 2f)) / drive;
        if (corner)
            t *= pod ? FTuning.POD_CORNER_FACTOR : FTuning.TUBE_CORNER_FACTOR;
        if (pod && exit.getAxis() == Direction.Axis.Y)
            t *= FTuning.POD_DROP_FACTOR;
        return Math.max(1, Math.round(t));
    }

    /** A powered linear-motor ring touching this block. */
    private boolean ringed() {
        if (level == null)
            return false;
        for (Direction d : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof LinearMotorBlockEntity m && m.powered())
                return true;
        return false;
    }

    /** Whether a capsule can pass from this block through {@code d}: a tube, or a station that faces this block. */
    protected boolean connects(Level level, Direction d) {
        BlockPos at = worldPosition.relative(d);
        if (!level.isLoaded(at))
            return false;
        BlockEntity be = level.getBlockEntity(at);
        if (be instanceof TubeBlockEntity t)
            return !t.isRemoved();
        return be instanceof TubeStationBlockEntity s && s.faces() == d.getOpposite();
    }

    /** Picks the exit among the open faces other than the entry. A plain tube carries straight on, else takes the first. */
    @Nullable
    protected Direction route(Capsule c, List<Direction> open) {
        Direction straight = c.from.getOpposite();
        return open.contains(straight) ? straight : open.get(0);
    }

    /** Puts a capsule in from a station on face {@code from}; false if the block is taken. */
    public boolean launch(ItemStack carrier, Direction from, @Nullable UUID rider, long now) {
        if (capsule != null || network == null || level == null)
            return false;
        Capsule c = new Capsule(carrier, from, now, now, rider);
        int hop = hopTicks(c, from.getOpposite(), false);
        c.arriveAt = now + (hop < 0 ? FTuning.TUBE_STALL_RETRY_TICKS : hop);
        c.wakeAt = c.arriveAt;
        capsule = c;
        setChanged();
        network.schedule(this);
        return true;
    }

    /** The run found this block's capsule due: pass it on, or look again later. */
    public void advance(Level level, long now) {
        Capsule c = capsule;
        if (c == null || network == null)
            return;
        List<Direction> open = new ArrayList<>(6);
        for (Direction d : Direction.values())
            if (d != c.from && connects(level, d))
                open.add(d);
        Direction out = open.isEmpty() ? null : route(c, open);
        if (out == null) {
            retry(c, now);
            return;
        }
        BlockEntity next = level.getBlockEntity(worldPosition.relative(out));
        if (next instanceof TubeBlockEntity nt) {
            int hop = nt.capsule != null || nt.network != network ? -1 : nt.hopTicks(c, out, out != c.from.getOpposite());
            if (hop < 0) {
                retry(c, now);
                return;
            }
            if (!nt.evacuated() || network.pressure().value() > FTuning.TUBE_EVAC_MAX_PU)
                network.spendAir(FTuning.TUBE_AIR_PER_HOP_MB);
            c.from = out.getOpposite();
            c.enteredAt = now;
            c.arriveAt = now + hop;
            c.wakeAt = c.arriveAt;
            capsule = null;
            setChanged();
            nt.capsule = c;
            nt.setChanged();
            network.schedule(nt);
        } else if (next instanceof TubeStationBlockEntity st && st.receive(c, now)) {
            capsule = null;
            setChanged();
        } else
            retry(c, now);
    }

    private void retry(Capsule c, long now) {
        c.wakeAt = now + FTuning.TUBE_STALL_RETRY_TICKS;
        if (network != null)
            network.schedule(this);
    }

    /** Moves a pod's rider along the line this tick. False when there is no rider left to move. */
    public boolean moveRider(long now) {
        Capsule c = capsule;
        if (c == null || c.rider == null || !(level instanceof net.minecraft.server.level.ServerLevel sl))
            return false;
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(c.rider);
        if (p == null || p.level() != sl || !p.isAlive()) {
            c.rider = null;
            setChanged();
            return false;
        }
        if (p.isShiftKeyDown()) {
            eject(p);
            return false;
        }
        float t = c.arriveAt <= c.enteredAt ? 1f : Math.max(0f, Math.min(1f, (now - c.enteredAt) / (float) (c.arriveAt - c.enteredAt)));
        BlockPos prev = worldPosition.relative(c.from);
        double x = prev.getX() + 0.5 + (worldPosition.getX() - prev.getX()) * t;
        double y = prev.getY() + (worldPosition.getY() - prev.getY()) * t + 0.1;
        double z = prev.getZ() + 0.5 + (worldPosition.getZ() - prev.getZ()) * t;
        float yaw = c.from.getAxis() == Direction.Axis.Y ? p.getYRot() : c.from.getOpposite().toYRot();
        p.connection.teleport(x, y, z, yaw, 0f);
        p.setDeltaMovement(0, 0, 0);
        p.fallDistance = 0f;
        return true;
    }

    /** The emergency exit: the rider steps out into the nearest free cell beside the tube, or the tube's own. */
    public void eject(ServerPlayer p) {
        BlockPos spot = worldPosition;
        for (Direction d : Direction.values()) {
            BlockPos at = worldPosition.relative(d);
            if (level.getBlockState(at).getCollisionShape(level, at).isEmpty()
                    && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
                spot = at;
                break;
            }
        }
        p.connection.teleport(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.fallDistance = 0f;
        if (capsule != null)
            capsule.rider = null;
        setChanged();
    }

    /** Called when the block is broken: the capsule's load and carrier fall out, its rider steps out. */
    public void spill() {
        if (capsule == null || level == null || level.isClientSide)
            return;
        Capsule c = capsule;
        capsule = null;
        if (c.rider != null && level instanceof net.minecraft.server.level.ServerLevel sl) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(c.rider);
            if (p != null)
                p.connection.teleport(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5, p.getYRot(), p.getXRot());
            c.rider = null;
        }
        for (ItemStack s : CarrierItem.contents(c.stack))
            if (!s.isEmpty())
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s));
        CarrierItem.setContents(c.stack, List.of());
        level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, c.stack));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            TubeNetworks.of(level).queue(worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            if (unloaded)
                TubeNetworks.of(level).unloaded(this);
            else
                TubeNetworks.of(level).removed(this);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (capsule != null)
            tag.put("Capsule", capsule.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        capsule = tag.contains("Capsule") ? Capsule.load(tag.getCompound("Capsule"), registries) : null;
    }
}
