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
package io.github.soundgoodizerfan.feedback.machine;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.Removing;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the six network-driven machine-shop machines share: one workpiece slot, a body that wears
 * from misuse, and progress that accumulates with speed.
 *
 * <h2>Speed is progress</h2>
 * Each of these used to do one step every tick at any speed above stopped, so gearing and RPM
 * meant nothing and a press landed twenty full blows a second. Now they accrue
 * {@code |RPM| x STROKES_PER_RPM_PER_TICK} a tick, the Crank Linkage's own rule, and act once per
 * whole stroke ({@link #strokesDue}) or by the fraction of a pass ({@link #passesDue}).
 *
 * <h2>Force that cannot go into the work goes into the machine</h2>
 * The Hammer's rule, now everyone's: a stroke that lands nothing -- under the hardness floor, the
 * wrong temperature, a tool that cannot cut, a workpiece with nothing left to become -- is
 * absorbed by the body ({@link #wear}). Condition scales the force the machine delivers, as a
 * worn hammer head delivers less of every blow.
 */
public abstract class ShopMachineBlockEntity extends RotationNode implements Wearing, WorkpieceHolder {

    protected ItemStack workpiece = ItemStack.EMPTY;
    protected float condition = 1f;
    /** Strokes owed but not yet delivered. */
    private float progress;
    /** The fraction of a Fu of removal not yet credited -- see {@link Removing#apply}. */
    protected float carriedFu;

    protected ShopMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStack getWorkpiece() {
        return workpiece;
    }

    public boolean insert(ItemStack stack) {
        if (!workpiece.isEmpty() || stack.isEmpty())
            return false;
        workpiece = stack.split(1);
        carriedFu = 0;
        sync();
        return true;
    }

    public ItemStack removeWorkpiece() {
        ItemStack taken = workpiece;
        workpiece = ItemStack.EMPTY;
        carriedFu = 0;
        if (level != null)
            ItemHeat.settle(taken, level);
        sync();
        return taken;
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return isTurning();
    }

    protected boolean isTurning() {
        return Math.abs(getRpm().value()) > FTuning.STOPPED_RPM_THRESHOLD.value();
    }

    /** Passes made this tick, fraction included, for a continuous cut. Zero while stopped. */
    protected float passesDue() {
        return isTurning() ? Math.abs(getRpm().value()) * FTuning.STROKES_PER_RPM_PER_TICK : 0f;
    }

    /** Whole strokes owed this tick, for a machine that acts in discrete strokes. */
    protected int strokesDue() {
        if (!isTurning()) {
            progress = 0;
            return 0;
        }
        progress += passesDue();
        int due = (int) progress;
        progress -= due;
        return due;
    }

    /** The body absorbs {@code stAbsorbed} -- see the class doc. */
    protected void wear(float stAbsorbed) {
        if (stAbsorbed <= 0)
            return;
        int before = FTuning.conditionBand(condition);
        condition = Math.max(FTuning.SHOP_MACHINE_CONDITION_FLOOR,
                condition - stAbsorbed * FTuning.SHOP_MACHINE_WEAR_PER_ST);
        setChanged();
        if (FTuning.conditionBand(condition) != before)
            sync();
    }

    /** A landed cut throws off a chip of its own material, some of the time. */
    protected void maybeDropSwarf(Removing.Cut cut) {
        if (level != null && !cut.swarf().isEmpty() && cut.delivered() > 0
                && level.random.nextFloat() < FTuning.SWARF_DROP_CHANCE * cut.delivered())
            Block.popResource(level, worldPosition.above(), cut.swarf().copy());
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Workpiece", workpiece.saveOptional(registries));
        tag.putFloat("Condition", condition);
        tag.putFloat("StrokeProgress", progress);
        tag.putFloat("CarriedFu", carriedFu);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpiece = ItemStack.parseOptional(registries, tag.getCompound("Workpiece"));
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        progress = tag.getFloat("StrokeProgress");
        carriedFu = tag.getFloat("CarriedFu");
    }
}
