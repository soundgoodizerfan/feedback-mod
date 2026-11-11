// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.pulley;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The one genuinely new engine piece in this document -- everything else in the machine shop
 * extends `DeformationTable`/`RemovalTable` or reuses `Switchable`/`Blown`. A pulley couples to a
 * shaft locally exactly like a Cog (its own axis, {@link PulleyBlock#hasShaftTowards}), and is a
 * completely ordinary member of that local network as far as {@code RotationPropagator} is
 * concerned -- it has no idea a second, distant pulley exists.
 *
 * <h2>Why a belt does not merge two networks into one</h2>
 * A Shaft run is rigid: {@code RotationNetwork} gives it exactly one speed, and overstress reads
 * as zero across the whole thing at once. A belt's entire reason to exist is that it does *not*
 * do that -- it sheds excess load by slipping, leaving the driving side turning. That is a second
 * network with an independent speed, not a gear ratio inside one network, so a belt pair is
 * deliberately never flood-filled together. Instead, once a tick, whichever pulley's own network
 * has no source of its own is driven through {@link RotationNetwork#driveByBelt} at its partner's
 * live speed times the diameter ratio, with the belt's capacity, and the Su it carries is charged
 * to the partner's network through {@link RotationNetwork#loadByBelt}. No rebuild, no merge, and
 * the propagator never has to know belts exist.
 *
 * <h2>Slip conserves power</h2>
 * Asked to carry more than its capacity, the belt slips: the driven side turns at
 * {@code capacity / demand} of the nominal speed. The driver is charged only the capacity, and
 * the driven side gets its full demand at the reduced speed -- Su x RPM is the same on both
 * ends, so slip makes nothing for free.
 * <p>
 * The one accepted limitation: if both linked segments already have their own independent source,
 * neither side ever writes to the other, so a belt between two already-driven networks quietly
 * does nothing rather than fighting over whose speed wins.
 *
 * <h2>Wear</h2>
 * A belt wears only while slipping, which is misuse -- carrying more than it was built for -- and
 * a worn belt carries proportionally less, which makes it slip sooner.
 */
public class PulleyBlockEntity extends RotationNode implements Wearing {

    @Nullable
    private BlockPos beltPartner;
    /** -1: the partner is the pulley block at {@link #beltPartner}; else the pulley voxel of that index in the micro cell there. */
    private int beltVoxel = -1;

    private float condition = 1f;

    /** Whether the belt drove anything last tick, purely for {@link #isRunning}'s purposes. */
    private boolean transmitting;

    public PulleyBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PULLEY.get(), pos, state);
    }

    public int getRadius() {
        return getBlockState().getBlock() instanceof PulleyBlock pulley ? pulley.getRadius() : 1;
    }

    @Nullable
    public BlockPos getBeltPartner() {
        return beltPartner;
    }

    public int getBeltVoxel() {
        return beltVoxel;
    }

    /** This pulley's partner block moved to {@code pos} (a Lift carried it): the belt keeps it. */
    public void followedTo(BlockPos pos) {
        beltPartner = pos;
        sync();
    }

    /**
     * Blocks moved ({@code moves}, old to new; this pulley among them, now at its position): the partner's address follows if it moved too,
     * and a partner that did not is told this pulley's new one.
     */
    public void retarget(java.util.Map<BlockPos, BlockPos> moves) {
        if (beltPartner == null || level == null)
            return;
        BlockPos moved = moves.get(beltPartner);
        if (moved != null) {
            beltPartner = moved;
        } else if (level.isLoaded(beltPartner)) {
            if (beltVoxel >= 0 && level.getBlockEntity(beltPartner) instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell)
                cell.farMovedTo(beltVoxel, worldPosition);
            else if (level.getBlockEntity(beltPartner) instanceof PulleyBlockEntity other)
                other.followedTo(worldPosition);
        }
        sync();
    }

    /** Links this pulley to pulley voxel {@code index} of {@code cell}, symmetrically, dropping either one's previous belt. */
    public boolean linkMicro(io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell, int index) {
        if (level == null || cell.getLevel() != level || cell.get(index) == null
                || !(cell.get(index).getBlock() instanceof PulleyBlock))
            return false;
        var sub = dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(level, cell.getBlockPos());
        net.minecraft.world.phys.Vec3 there = net.minecraft.world.phys.Vec3.atCenterOf(cell.getBlockPos());
        if (sub != null)
            there = sub.logicalPose().transformPosition(there);
        if (there.distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)) > FTuning.BELT_MAX_LENGTH)
            return false;
        unlinkBelt();
        cell.unlinkBelt(index);
        beltPartner = cell.getBlockPos();
        beltVoxel = index;
        cell.setBelt(index, new io.github.soundgoodizerfan.feedback.vehicle.micro.MicroBelts.End(worldPosition, -1));
        sync();
        return true;
    }

    /**
     * Links this pulley to {@code other}, symmetrically, dropping either one's previous belt.
     * Refuses itself, and anything further than {@link FTuning#BELT_MAX_LENGTH}.
     */
    public boolean linkBelt(PulleyBlockEntity other) {
        if (other == this || level == null || other.level != level
                || !worldPosition.closerThan(other.worldPosition, FTuning.BELT_MAX_LENGTH))
            return false;
        unlinkBelt();
        other.unlinkBelt();
        beltPartner = other.getBlockPos();
        beltVoxel = -1;
        other.beltPartner = this.getBlockPos();
        other.beltVoxel = -1;
        sync();
        other.sync();
        return true;
    }

    /** Drops this pulley's belt from both ends. The far end is only touched if it is loaded. */
    public void unlinkBelt() {
        if (beltPartner == null || level == null)
            return;
        if (beltVoxel >= 0) {
            if (level.isLoaded(beltPartner) && level.getBlockEntity(beltPartner) instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell) {
                var far = cell.belt(beltVoxel);
                if (far != null && far.index() == -1 && far.pos().equals(worldPosition))
                    cell.unlinkBelt(beltVoxel);
            }
            beltPartner = null;
            beltVoxel = -1;
            sync();
            return;
        }
        if (level.isLoaded(beltPartner) && level.getBlockEntity(beltPartner) instanceof PulleyBlockEntity partner
                && worldPosition.equals(partner.beltPartner)) {
            partner.beltPartner = null;
            partner.sync();
        }
        beltPartner = null;
        sync();
    }

    @Override
    public Drag getDragSuPerRpm() {
        return FTuning.PULLEY_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.PULLEY_INERTIA;
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return transmitting;
    }

    public void tickServer() {
        transmitting = false;
        if (level == null || level.isClientSide || beltPartner == null)
            return;
        // An unloaded partner is not a missing one: keep the belt, just do nothing this tick.
        if (!level.isLoaded(beltPartner))
            return;
        RotationNetwork theirs;
        float partnerRpm, partnerRadius;
        if (beltVoxel >= 0) {
            // The other end is a pulley voxel: a quarter the size, so its radius counts a quarter.
            if (!(level.getBlockEntity(beltPartner) instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell)
                    || cell.rotor(beltVoxel) == null || !(cell.get(beltVoxel).getBlock() instanceof PulleyBlock pb)) {
                beltPartner = null;
                beltVoxel = -1;
                sync();
                return;
            }
            var rotor = cell.rotor(beltVoxel);
            theirs = rotor.getNetwork();
            partnerRpm = rotor.getRpm().value();
            partnerRadius = pb.getRadius() / (float) io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity.N;
        } else if (level.getBlockEntity(beltPartner) instanceof PulleyBlockEntity partner) {
            theirs = partner.getNetwork();
            partnerRpm = partner.getRpm().value();
            partnerRadius = partner.getRadius();
        } else {
            beltPartner = null;
            sync();
            return;
        }

        RotationNetwork own = getNetwork();
        if (own == null || theirs == null || own == theirs || getRatio() == 0)
            return;

        // Only the side with no source of its own is driven -- see the class doc's "accepted limitation".
        if (own.getCapacitySu().value() > 0)
            return;

        // Partner's own speed (its gear ratio included), across the diameters, then back to this
        // network's reference through this pulley's ratio -- sign carried throughout.
        float desired = partnerRpm * partnerRadius / getRadius() / getRatio();

        float demand = own.getLoadSu().value();
        float capacity = FTuning.BELT_CAPACITY_SU.value() * condition;
        float carried = Math.min(demand, capacity);
        if (demand > capacity) {
            desired *= capacity / demand;
            condition = Math.max(FTuning.BELT_CONDITION_FLOOR,
                    condition - (demand - capacity) * FTuning.BELT_WEAR_PER_SU_SLIPPED);
            setChanged();
        }

        own.driveByBelt(new Rpm(desired), new Su(Math.max(demand, capacity)));
        theirs.loadByBelt(new Su(carried));
        transmitting = desired != 0;
    }

    // Condition rides on the dropped item (loot table copy_components), so mining a worn machine
    // and putting it back does not make it new.

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        condition = input.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (condition < 1f)
            builder.set(FDataComponents.TOOL_CONDITION.get(), condition);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Condition");
    }

    // sync() is inherited from RotationNode -- no need to redefine it here.

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (beltPartner != null)
            tag.putLong("BeltPartner", beltPartner.asLong());
        tag.putInt("BeltVoxel", beltVoxel);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        beltPartner = tag.contains("BeltPartner") ? BlockPos.of(tag.getLong("BeltPartner")) : null;
        beltVoxel = tag.contains("BeltVoxel") ? tag.getInt("BeltVoxel") : -1;
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
