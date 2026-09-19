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
 * do that -- it sheds excess load by slipping, leaving the driving side completely unaffected.
 * That is a second network with an independent speed, not a gear ratio inside one network, so a
 * belt pair is deliberately never flood-filled together. Instead, once a tick, whichever pulley's
 * own local network is unpowered on its own reads the *other* pulley's live network speed,
 * applies the diameter ratio and the {@link FTuning#BELT_CAPACITY_SU} slip cap, and pushes the
 * result onto its own network with a plain {@link RotationNetwork#setTargetRpm}. No rebuild, no
 * merge, and the propagator never has to know belts exist.
 * <p>
 * The one accepted limitation: if both linked segments already have their own independent source,
 * neither side ever writes to the other (each only drives when its own network has zero capacity),
 * so a belt between two already-driven networks quietly does nothing rather than fighting over
 * whose speed wins. A real, if narrower, case than the common one of a driven segment feeding an
 * otherwise unpowered one.
 *
 * <h2>{@code Wearing}'s fourth implementer</h2>
 * Unlike the Hammer or the Lathe's bit, a belt does not wear from misuse -- it wears from being
 * asked to carry more than {@link FTuning#BELT_CAPACITY_SU}, which is ordinary operation under
 * load, the same "real wear under correct use" the Grinding Wheel already established.
 */
public class PulleyBlockEntity extends RotationNode implements Wearing {

    @Nullable
    private BlockPos beltPartner;

    private float condition = 1f;

    /** Whether the belt actually slipped last tick, purely for {@link #isRunning}'s purposes. */
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

    /** Links this pulley to {@code other}, symmetrically. Refuses linking to itself. */
    public boolean linkBelt(PulleyBlockEntity other) {
        if (other == this || level == null)
            return false;
        beltPartner = other.getBlockPos();
        other.beltPartner = this.getBlockPos();
        sync();
        other.sync();
        return true;
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
        if (!(level.getBlockEntity(beltPartner) instanceof PulleyBlockEntity partner)) {
            beltPartner = null;
            sync();
            return;
        }

        RotationNetwork own = getNetwork();
        RotationNetwork theirs = partner.getNetwork();
        if (own == null || theirs == null || own == theirs)
            return;

        // Only the unpowered side ever writes -- see the class doc's "accepted limitation".
        if (own.getCapacitySu().value() > 0)
            return;

        float ratio = partner.getRadius() / (float) getRadius();
        float desired = theirs.getCurrentRpm().value() * ratio;
        if (desired == 0)
            return;

        float demand = own.getStaticLoadSu().value();
        float capacity = FTuning.BELT_CAPACITY_SU.value();
        float actual = desired;
        if (demand > capacity) {
            actual = desired * (capacity / demand);
            float slippedSu = demand - capacity;
            condition = Math.max(FTuning.BELT_CONDITION_FLOOR,
                    condition - slippedSu * FTuning.BELT_WEAR_PER_SU_SLIPPED);
        }

        own.setTargetRpm(new Rpm(actual));
        transmitting = true;
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
