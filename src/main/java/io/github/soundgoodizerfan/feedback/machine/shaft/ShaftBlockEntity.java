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
package io.github.soundgoodizerfan.feedback.machine.shaft;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A relay. Generates nothing and demands nothing while stopped, but drags once it turns: every
 * shaft charges bearing friction to the network in proportion to speed.
 * <p>
 * So a sprawling layout costs Su that a compact one does not, and a long run cannot be driven as
 * fast as a short one -- not because anything forbids it, but because friction runs out of
 * torque first.
 * <p>
 * A flywheel also has a rim speed limit (user ruling, 2026-10-23): past
 * {@link FTuning#FLYWHEEL_RIM_RPM_PER_ROOT} times the root of its metal's yield strength over density the rim
 * strains and its condition falls, to a floor and never to a burst. A strained rim carries less inertia, read the
 * next time the run is rebuilt.
 */
public class ShaftBlockEntity extends RotationNode implements Wearing {

    private float condition = 1f;

    public ShaftBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.SHAFT.get(), pos, state);
    }

    /** One block entity type serves both the Shaft and the Bearing; the figures live on the block,
     * the same split {@code CogBlockEntity} already uses for its two sizes. */
    @Override
    public Drag getDragSuPerRpm() {
        return getBlockState().getBlock() instanceof ShaftBlock shaft
                ? shaft.getDragSuPerRpm()
                : FTuning.SHAFT_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return getBlockState().getBlock() instanceof ShaftBlock shaft
                ? new Inertia(shaft.getInertia().suTicksPerRpm() * condition)
                : FTuning.SHAFT_INERTIA;
    }

    /** The rim speed, RPM, a flywheel of cast iron holds without strain. */
    public static float rimLimitRpm(Material metal) {
        float yield = metal.mechanical().yieldStrength().map(St::value).orElse(1f);
        return FTuning.FLYWHEEL_RIM_RPM_PER_ROOT * (float) Math.sqrt(yield / metal.density().muPerMb());
    }

    public void tickServer() {
        Material metal = MaterialTable.get().materials().get(Feedback.id("cast_iron"));
        if (metal == null)
            return;
        float over = Math.abs(getRpm().value()) / rimLimitRpm(metal) - 1f;
        if (over > 0f && condition > FTuning.FLYWHEEL_CONDITION_FLOOR) {
            condition = Math.max(FTuning.FLYWHEEL_CONDITION_FLOOR, condition - FTuning.FLYWHEEL_WEAR_PER_OVER * over);
            setChanged();
        }
    }

    @Override
    public float getCondition() {
        return condition;
    }

    /** A turning flywheel is working and cannot be measured, like any machine (philosophy 8). */
    @Override
    public boolean isRunning() {
        return Math.abs(getRpm().value()) > FTuning.STOPPED_RPM_THRESHOLD.value();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        condition = input.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (condition < 1f)
            components.set(FDataComponents.TOOL_CONDITION.get(), condition);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Condition");
    }
}
