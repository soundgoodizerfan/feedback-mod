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
package io.github.soundgoodizerfan.feedback.machine.relay;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricMember;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.core.electric.Sparks;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.machine.electromagnet.ElectromagnetBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The built electromagnet plus contacts: an armature that sits against an electromagnet and a
 * pair of contacts on its other faces. The magnet's field, not any wire, moves the armature --
 * the relay does not join the magnet's run ({@link #joins}), so a small coil current switches a
 * run the coil is not part of. That is the bridge: a coil run can be a detector's output, a
 * current sensor's run or a breaker a controller switches, and the contacts carry the run that
 * does the work.
 *
 * <h2>Switching</h2>
 * Pulls in at field {@link FTuning#RELAY_PULL_IN_FIELD}, lets go below {@link FTuning#RELAY_DROP_OUT_FIELD}
 * (a gap, so a sagging coil does not chatter). Opening under load arcs: condition falls by the current
 * broken (the breaker's rule) and a spark goes out for any listener. Wear only from that and from
 * carrying more than {@link FTuning#RELAY_CONTACT_AMPS}; closing, holding and idling cost nothing.
 *
 * <h2>Overrun</h2>
 * Worn contacts stick: each tick the field is below drop-out, release succeeds with probability
 * {@code condition}, so a pitted relay lets go late, and never refuses. Floor {@link FTuning#RELAY_CONDITION_FLOOR}.
 *
 * <h2>Sense and stop</h2>
 * Sense: the coil run's current, with the galvanometer fitting. Stop: cut the coil run.
 */
public class RelayBlockEntity extends ElectricNode implements Wearing, io.github.soundgoodizerfan.feedback.instrument.Testable {

    private boolean closed;
    private float condition = 1f;

    public RelayBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.RELAY.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.WIRE;
    }

    @Override
    public boolean conducting() {
        return closed;
    }

    /** The coil is magnetic, not galvanic: never part of the magnet's run. */
    @Override
    public boolean joins(ElectricMember other) {
        return !(other instanceof ElectromagnetBlockEntity);
    }

    public void tickServer() {
        if (level == null)
            return;
        float field = 0f;
        for (Direction face : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(face)) instanceof ElectromagnetBlockEntity magnet)
                field = Math.max(field, magnet.getFieldStrength());
        if (!closed && field >= FTuning.RELAY_PULL_IN_FIELD)
            set(true);
        else if (closed && field < FTuning.RELAY_DROP_OUT_FIELD && level.random.nextFloat() < condition)
            set(false);
    }

    private void set(boolean value) {
        if (!value && getAmps() > 0f) {
            condition = Math.max(FTuning.RELAY_CONDITION_FLOOR, condition - FTuning.RELAY_ARC_WEAR_PER_AMP * getAmps());
            Sparks.emit(level, worldPosition, getAmps());
        }
        closed = value;
        setChanged();
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(RelayBlock.CLOSED, value));
        ElectricPropagator.reconnect(level, worldPosition);
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        if (closed && amps > FTuning.RELAY_CONTACT_AMPS)
            condition = Math.max(FTuning.RELAY_CONDITION_FLOOR,
                    condition - FTuning.RELAY_OVERLOAD_WEAR_PER_AMP * (amps - FTuning.RELAY_CONTACT_AMPS));
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public void test(io.github.soundgoodizerfan.feedback.instrument.Instrument tester, java.util.function.Consumer<net.minecraft.network.chat.Component> out) {
        out.accept(net.minecraft.network.chat.Component.translatable(closed ? "feedback.test.closed" : "feedback.test.open"));
        out.accept(io.github.soundgoodizerfan.feedback.instrument.Testable.condition(tester, condition));
    }

    @Override
    public boolean isRunning() {
        return closed && getAmps() > 0f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Closed", closed);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        closed = tag.getBoolean("Closed");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
