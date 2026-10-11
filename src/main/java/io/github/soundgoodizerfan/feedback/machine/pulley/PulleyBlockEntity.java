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
        other.beltPartner = this.getBlockPos();
        sync();
        other.sync();
        return true;
    }

    /** Drops this pulley's belt from both ends. The far end is only touched if it is loaded. */
    public void unlinkBelt() {
        if (beltPartner == null || level == null)
            return;
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
        if (!(level.getBlockEntity(beltPartner) instanceof PulleyBlockEntity partner)) {
            beltPartner = null;
            sync();
            return;
        }

        RotationNetwork own = getNetwork();
        RotationNetwork theirs = partner.getNetwork();
        if (own == null || theirs == null || own == theirs || getRatio() == 0)
            return;

        // Only the side with no source of its own is driven -- see the class doc's "accepted limitation".
        if (own.getCapacitySu().value() > 0)
            return;

        // Partner's own speed (its gear ratio included), across the diameters, then back to this
        // network's reference through this pulley's ratio -- sign carried throughout.
        float desired = partner.getRpm().value() * partner.getRadius() / getRadius() / getRatio();

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

    // sync() is inherited from RotationNode -- no need to redefine it here.

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (beltPartner != null)
            tag.putLong("BeltPartner", beltPartner.asLong());
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        beltPartner = tag.contains("BeltPartner") ? BlockPos.of(tag.getLong("BeltPartner")) : null;
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
