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
package io.github.soundgoodizerfan.feedback.machine.grindingwheel;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.machine.linkage.CrankLinkageBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Removing;
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
 * Same removal primitive as the Drill Press and the Lathe, with the wheel's own hardness rating
 * set above both of theirs -- the only route through a material like hardened tool steel, not a
 * finishing upgrade over them (see {@link FTuning#GRINDING_WHEEL_HARDNESS}).
 *
 * <h2>{@code Wearing}'s third implementer, and the one real exception</h2>
 * The Hammer and the Lathe's tool bit both wear <em>only</em> from misuse -- force or a cut the
 * tool cannot use, absorbed rather than wasted for free. This wheel is different on purpose: grit
 * is genuinely consumed by correct use, the same way a real abrasive wheel wears down whether or
 * not the operator made a mistake. So {@link #tickServer} costs condition on every <em>landed</em>
 * grind, not only on a failed one, and its floor is zero rather than the Hammer's 0.5 -- a bald
 * wheel's effective hardness is also zero, which is a genuine hard gate (§7) rather than a machine
 * silently refusing to run.
 *
 * <h2>The thermal tie-in is the differentiator landing a third time</h2>
 * Grinding produces real interface heat. {@link #tickServer} bumps the workpiece's own {@code
 * ItemHeat} proportionally to Fu removed -- no new mechanic, since a workpiece is hot wherever it
 * is regardless of what heated it (philosophy 9). Overrun grinding a part that was deliberately
 * hardened can walk its temperature back into a tempering-relevant range and soften it, which
 * needed nothing new to be written for it to happen.
 */
public class GrindingWheelBlockEntity extends RotationNode implements Wearing {

    private ItemStack workpiece = ItemStack.EMPTY;
    private float condition = 1f;

    public GrindingWheelBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GRINDING_WHEEL.get(), pos, state);
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

    public boolean isCoarseGrit() {
        return getBlockState().getValue(GrindingWheelBlock.COARSE);
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return CrankLinkageBlockEntity.isDriving(level, worldPosition)
                || Math.abs(getRpm().value()) > FTuning.STOPPED_RPM_THRESHOLD.value();
    }

    @Override
    public Su getLoadSu() {
        return FTuning.GRINDING_WHEEL_LOAD_SU;
    }

    private St toolHardness() {
        return new St(FTuning.GRINDING_WHEEL_HARDNESS.value() * condition);
    }

    public void tickServer() {
        if (level == null || level.isClientSide || workpiece.isEmpty())
            return;
        if (Math.abs(getRpm().value()) <= FTuning.STOPPED_RPM_THRESHOLD.value())
            return;

        boolean coarse = isCoarseGrit();
        St driveForce = coarse ? FTuning.GRINDING_WHEEL_ST_COARSE : FTuning.GRINDING_WHEEL_ST_FINE;
        Removing.Cut cut = Removing.apply(workpiece, driveForce, toolHardness());
        if (!cut.landed())
            return;

        workpiece = cut.result();

        // How much Fu actually landed this tick is exactly the driving force divided by the
        // material's own hardness -- the same arithmetic Removal#cutFrom already did internally.
        // Recomputing it here rather than threading it back through Cut keeps Removing's contract
        // identical to Deforming's, which nothing else in the removal family has needed to break.
        float fuThisTick = driveForce.value();
        float wearPerFu = coarse ? FTuning.GRINDING_WHEEL_WEAR_PER_FU_COARSE : FTuning.GRINDING_WHEEL_WEAR_PER_FU_FINE;
        condition = Math.max(FTuning.GRINDING_WHEEL_CONDITION_FLOOR, condition - fuThisTick * wearPerFu);

        Tu heated = ItemHeat.get(workpiece, level);
        ItemHeat.set(workpiece, new Tu(heated.value() + fuThisTick * FTuning.GRINDING_HEAT_PER_FU), level);

        sync();
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
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpiece = ItemStack.parseOptional(registries, tag.getCompound("Workpiece"));
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
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
