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
package io.github.soundgoodizerfan.feedback.machine.rollingmill;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A driven roller pair. Same shape as {@code MechanicalHammerBlockEntity}'s plate-making, but the
 * force is continuous rather than delivered by discrete blows: every tick the network is turning,
 * this applies {@link FTuning#ROLLING_MILL_ST} through {@link Operation#ROLL}, the same {@link
 * Deforming#apply} the Hammer and Wire Drawer already share.
 * <p>
 * Exists alongside the Hammer for a real reason, not a cosmetic one: a mill geared correctly runs
 * unattended off the network, where the Hammer needs either a Clutch or a hand on the crank --
 * capital against attention (philosophy §5), not a second way to make the same plate.
 */
public class RollingMillBlockEntity extends RotationNode {

    private ItemStack workpiece = ItemStack.EMPTY;

    public RollingMillBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ROLLING_MILL.get(), pos, state);
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

    @Override
    public Su getLoadSu() {
        return FTuning.ROLLING_MILL_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide || workpiece.isEmpty())
            return;
        if (Math.abs(getRpm().value()) <= FTuning.STOPPED_RPM_THRESHOLD.value())
            return;

        Deforming.Blow blow = Deforming.apply(workpiece, FTuning.ROLLING_MILL_ST, level, Operation.ROLL);
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
