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
package io.github.soundgoodizerfan.feedback.machine.wiredrawer;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.process.Deforming;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Wire Drawer: the hammer's own model, minus the hammer -- {@code Fu} accrues every tick from
 * network RPM rather than one blow per stroke, through the exact same {@link Deforming#apply}
 * every hand and machine tool in the mod already shares. Pulling stock through a die is tension,
 * not a blow, so it always applies {@link Operation#DRAW} -- see that enum's own doc for why a
 * hammer's {@code BLOW} would collide with this on the same input item.
 *
 * <h2>Draw speed is the block's one setting</h2>
 * Fast clears a higher hardness -- {@link FTuning#WIRE_DRAWER_ST_FAST} reaches steel, {@link
 * FTuning#WIRE_DRAWER_ST_SLOW} does not, a genuine hard gate rather than patience -- but risks the
 * wire snapping outright each tick it runs. A snap is not a wear tick: nothing here has a
 * condition figure, because what absorbs a mistimed draw is the wire, and a snapped wire is simply
 * gone. Slow is always safe and is what a plain copper draw actually wants.
 */
public class WireDrawerBlockEntity extends RotationNode {

    private ItemStack workpiece = ItemStack.EMPTY;

    public WireDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.WIRE_DRAWER.get(), pos, state);
    }

    public ItemStack getWorkpiece() {
        return workpiece;
    }

    public boolean insert(ItemStack stack) {
        if (!workpiece.isEmpty() || stack.isEmpty())
            return false;
        workpiece = stack.split(1);
        sync();
        return true;
    }

    public ItemStack removeWorkpiece() {
        ItemStack taken = workpiece;
        workpiece = ItemStack.EMPTY;
        if (level != null)
            ItemHeat.settle(taken, level);
        sync();
        return taken;
    }

    public boolean isFastDraw() {
        return getBlockState().getValue(WireDrawerBlock.FAST);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.WIRE_DRAWER_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide || workpiece.isEmpty())
            return;
        if (Math.abs(getRpm().value()) <= FTuning.STOPPED_RPM_THRESHOLD.value())
            return;

        boolean fast = isFastDraw();
        if (fast && level.random.nextFloat() < FTuning.WIRE_SNAP_CHANCE) {
            workpiece = ItemStack.EMPTY;
            level.playSound(null, worldPosition, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.6f, 0.7f);
            sync();
            return;
        }

        St strength = fast ? FTuning.WIRE_DRAWER_ST_FAST : FTuning.WIRE_DRAWER_ST_SLOW;
        Deforming.Blow blow = Deforming.apply(workpiece, strength, level, Operation.DRAW);
        if (blow.landed()) {
            workpiece = blow.result();
            sync();
        }
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Workpiece", workpiece.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpiece = ItemStack.parseOptional(registries, tag.getCompound("Workpiece"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
