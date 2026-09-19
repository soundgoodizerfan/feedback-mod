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
package io.github.soundgoodizerfan.feedback.machine.lathe;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.process.Removing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stock chucked and spun by the network; a mounted tool bit removes material continuously toward
 * whatever the workpiece's {@code Removal} entry calls its target. Same removal primitive as the
 * Drill Press, running every tick like the deformation family rather than on discrete pushes --
 * a lathe's cut is continuous over the whole pass, not a series of separate feeds.
 *
 * <h2>Calipers' real second job</h2>
 * Nothing new is built for reading a live diameter. Calipers already read {@link Quantity#WORK}
 * off a workpiece's accumulated-Fu components ({@code WorkpieceTooltip}, {@code CalipersItem}) --
 * a Lathe's workpiece is no different from a Hammer's, so it is already "readable" the moment it
 * exists. What is genuinely new is exposing that same number to a *controller* -- this class
 * implements {@link SensorFitting} directly (an unsided sensor, the same shape a Clutch implements
 * {@code DataNode} directly rather than through a fitting slot) so a {@code Read Sensor} card can
 * wire it straight to a comparator, which can drive a Clutch on the same network for auto-stop.
 * Nothing new was added to {@code control/}.
 *
 * <h2>The tool bit wears; the machine does not</h2>
 * {@code Wearing}'s second implementer (the Hammer is first). Unlike the Hammer's own condition,
 * this one lives on the mounted {@link ItemStack} (see {@code FDataComponents#TOOL_CONDITION}),
 * because philosophy 4 calls the bit itself the physical component you point at -- "a machine-wide
 * condition figure" was explicitly the wrong shape to reuse here.
 */
public class LatheBlockEntity extends RotationNode implements SensorFitting {

    private static final String LINKS = "Links";

    private ItemStack workpiece = ItemStack.EMPTY;
    private ItemStack toolBit = ItemStack.EMPTY;
    private final Set<DataNodeRef> links = new HashSet<>();

    public LatheBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LATHE.get(), pos, state);
    }

    public ItemStack getWorkpiece() {
        return workpiece;
    }

    public boolean insertWorkpiece(ItemStack stack) {
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

    public ItemStack getToolBit() {
        return toolBit;
    }

    public boolean insertToolBit(ItemStack stack) {
        if (!toolBit.isEmpty() || stack.isEmpty())
            return false;
        toolBit = stack.split(1);
        sync();
        return true;
    }

    public ItemStack removeToolBit() {
        ItemStack taken = toolBit;
        toolBit = ItemStack.EMPTY;
        sync();
        return taken;
    }

    public boolean isHeavyPass() {
        return getBlockState().getValue(LatheBlock.HEAVY);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.LATHE_LOAD_SU;
    }

    /** How sound the mounted bit still is -- absent bit reads as brand new, so nothing to cut with
     * never shows up as a worn tool. */
    private float bitCondition() {
        return toolBit.isEmpty() ? 1f : toolBit.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    /** The bit's effective hardness scales with its own condition -- a worn bit clears less,
     * exactly the way a worn Hammer head delivers less of every blow. */
    private St toolHardness() {
        return new St(FTuning.LATHE_BIT_HARDNESS.value() * bitCondition());
    }

    public void tickServer() {
        if (level == null || level.isClientSide || workpiece.isEmpty() || toolBit.isEmpty())
            return;
        if (Math.abs(getRpm().value()) <= FTuning.STOPPED_RPM_THRESHOLD.value())
            return;

        St driveForce = isHeavyPass() ? FTuning.LATHE_ST_HEAVY : FTuning.LATHE_ST_LIGHT;
        Removing.Cut cut = Removing.apply(workpiece, driveForce, toolHardness());

        if (cut.outcome() == Removing.Outcome.WRONG_TOOL) {
            wearBit(driveForce.value());
            return;
        }
        if (!cut.landed())
            return;

        workpiece = cut.result();
        sync();
    }

    /** The bit absorbs force it could not cut with -- the Hammer's own rule, one component down. */
    private void wearBit(float stAbsorbed) {
        if (toolBit.isEmpty() || stAbsorbed <= 0)
            return;
        float condition = bitCondition();
        condition = Math.max(FTuning.LATHE_BIT_CONDITION_FLOOR,
                condition - stAbsorbed * FTuning.LATHE_BIT_WEAR_PER_ST);
        toolBit.set(FDataComponents.TOOL_CONDITION.get(), condition);
        setChanged();
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // --- Instrument / SensorFitting ------------------------------------------------------------

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.WORK;
    }

    @Override
    public float resolution(Quantity quantity) {
        return 1f;   // Fu is whole numbers, same as the Hand Calipers reading Quantity.WORK.
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.lathe");
    }

    @Override
    public float readRaw() {
        int worked = workpiece.getOrDefault(FDataComponents.WORK.get(), 0);
        return Instruments.quantise((float) worked, resolution(Quantity.WORK));
    }

    @Override
    public Component readValue() {
        return Instruments.signed(this, Component.translatable("feedback.readout.work_raw", Math.round(readRaw())));
    }

    /** Not a removable fitting -- this is the whole block implementing the sensor contract
     * directly (the same shape a Clutch implements {@code DataNode} directly), so there is
     * nothing to hand back on removal. */
    @Override
    public ItemStack getPickItem() {
        return ItemStack.EMPTY;
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return getBlockPos();
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Workpiece", workpiece.saveOptional(registries));
        tag.put("ToolBit", toolBit.saveOptional(registries));
        ListTag list = new ListTag();
        for (DataNodeRef ref : links)
            list.add(ref.toTag());
        tag.put(LINKS, list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpiece = ItemStack.parseOptional(registries, tag.getCompound("Workpiece"));
        toolBit = ItemStack.parseOptional(registries, tag.getCompound("ToolBit"));
        links.clear();
        for (Tag entry : tag.getList(LINKS, Tag.TAG_COMPOUND))
            links.add(DataNodeRef.fromTag((CompoundTag) entry));
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
