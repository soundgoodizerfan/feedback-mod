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
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.machine.ShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Operation;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stock chucked and spun by the network; a mounted tool bit removes material continuously toward
 * whatever the workpiece's {@code Removal} entry calls its target. Same removal primitive as the
 * Drill Press, cutting continuously by speed ({@code passesDue}) rather than on discrete pushes --
 * a lathe's cut is continuous over the whole pass, not a series of separate feeds.
 *
 * <h2>Calipers' real second job</h2>
 * Nothing new is built for reading a live diameter. Calipers already read {@link Quantity#WORK}
 * off a workpiece's accumulated-Fu components ({@code WorkpieceTooltip}, {@code CalipersItem}) --
 * a Lathe's workpiece is no different from a Hammer's, so it is already "readable" the moment it
 * exists. What is genuinely new is exposing that same number to a *controller* -- this class
 * implements {@link SensorFitting} directly (an unsided sensor, the same shape a Clutch implements
 * {@code DataNode} directly rather than through a fitting slot) so a {@code Input} card can
 * wire it straight to a comparator, which can drive a Clutch on the same network for auto-stop.
 * Nothing new was added to {@code control/}.
 *
 * <h2>The tool bit wears; the machine does not</h2>
 * {@link #getCondition} is the mounted bit's, which lives on the {@link ItemStack} (see
 * {@code FDataComponents#TOOL_CONDITION}), because philosophy 4 calls the bit itself the physical
 * component you point at -- a machine-wide condition figure was explicitly the wrong shape here.
 * A worn bit cuts less of each pass; it never stops clearing what its rating clears.
 */
public class LatheBlockEntity extends ShopMachineBlockEntity implements SensorFitting {

    private static final String LINKS = "Links";

    private ItemStack toolBit = ItemStack.EMPTY;
    private final Set<DataNodeRef> links = new HashSet<>();
    /** Fu turned off the piece now chucked -- what the sensor reads. Unlike the workpiece's own
     * WORK it does not reset when a stage completes, so done and not-started read differently. */
    private float turnedFu;

    public LatheBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LATHE.get(), pos, state);
    }

    @Override
    public boolean insert(ItemStack stack) {
        boolean inserted = super.insert(stack);
        if (inserted)
            turnedFu = 0;
        return inserted;
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

    /**
     * The lathe's condition is its bit's -- the component you point at (philosophy 4) -- and an
     * absent bit reads as brand new, so nothing to cut with never shows up as a worn tool.
     */
    @Override
    public float getCondition() {
        return toolBit.isEmpty() ? 1f : toolBit.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    public void tickServer() {
        if (level == null || level.isClientSide || workpiece.isEmpty() || toolBit.isEmpty())
            return;
        float passes = passesDue();
        if (passes <= 0)
            return;

        // A blunt bit cuts less of every pass; it never stops clearing what its rating clears,
        // which would have turned wear into a hard gate on steel.
        St driveForce = new St((isHeavyPass() ? FTuning.LATHE_ST_HEAVY : FTuning.LATHE_ST_LIGHT).value() * getCondition());
        Removing.Cut cut = Removing.apply(workpiece, driveForce, FTuning.LATHE_BIT_HARDNESS, Operation.TURN,
                passes, carriedFu, level);
        carriedFu = cut.carried();

        if (!cut.landed()) {
            wearBit(driveForce.value() * passes);
            return;
        }

        turnedFu += cut.delivered();
        workpiece = cut.result();
        maybeDropSwarf(cut);
        sync();
    }

    /** The bit absorbs force it could not cut with -- the Hammer's own rule, one component down. */
    private void wearBit(float stAbsorbed) {
        if (toolBit.isEmpty() || stAbsorbed <= 0)
            return;
        int before = FTuning.conditionBand(getCondition());
        float condition = Math.max(FTuning.LATHE_BIT_CONDITION_FLOOR,
                getCondition() - stAbsorbed * FTuning.LATHE_BIT_WEAR_PER_ST);
        toolBit.set(FDataComponents.TOOL_CONDITION.get(), condition);
        setChanged();
        if (FTuning.conditionBand(condition) != before)
            sync();
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
        return Instruments.quantise(turnedFu, resolution(Quantity.WORK));
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
        tag.put("ToolBit", toolBit.saveOptional(registries));
        tag.putFloat("TurnedFu", turnedFu);
        ListTag list = new ListTag();
        for (DataNodeRef ref : links)
            list.add(ref.toTag());
        tag.put(LINKS, list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        toolBit = ItemStack.parseOptional(registries, tag.getCompound("ToolBit"));
        turnedFu = tag.getFloat("TurnedFu");
        links.clear();
        for (Tag entry : tag.getList(LINKS, Tag.TAG_COMPOUND))
            links.add(DataNodeRef.fromTag((CompoundTag) entry));
    }

}
