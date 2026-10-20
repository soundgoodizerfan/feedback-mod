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
package io.github.soundgoodizerfan.feedback.machine.electromagnet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The first consumer, and the first thing to show what a sink does with whatever it is given
 * (mechanics 6.5).
 *
 * <h2>It runs on what arrives, and never refuses a line</h2>
 * Its field strength is {@link ElectricRules#rate}: the fraction of its rated voltage it sees
 * times the fraction of its wanted current it gets, times its own condition. A long run or a weak
 * source makes it weak, not dead -- the floor is zero and nothing about the line is refused. At
 * zero it does nothing and is none the worse for it: under-voltage never wears.
 *
 * <h2>Over-voltage goes into the machine</h2>
 * Volts past its rating are force that cannot go into the work, so they go into the magnet:
 * condition falls, and heat goes into its own {@link ThermalBody} through the same {@link
 * Heat#warm} a wire's overload uses. It degrades to {@link FTuning#ELECTROMAGNET_CONDITION_FLOOR}
 * and is never destroyed and never explodes; a worn magnet is evidence of one wiring mistake, and
 * how far it wore says how wrong and for how long. (No band above LV is built, so this is reached
 * only by a test or by a future source.)
 *
 * <h2>What it does</h2>
 * Pulls dropped items toward itself, for any item whose material says it is magnetic
 * ({@code separation.magnetic} in the material file). No identity check: iron, steel, cast iron
 * and netherite are pulled because they share a property, and a pack that adds a magnetic
 * material gets the same behaviour with no code. It does not yet account for the Curie point of
 * a hot workpiece, which the material table already carries.
 */
public class ElectromagnetBlockEntity extends ElectricNode implements ThermalBody, Wearing {

    private float temperature = FTuning.AMBIENT_TU.value();
    private float condition = 1f;
    private float field;
    private int sinceLastPull;

    // Every item whose material is magnetic, rebuilt when the material table reloads.
    private static int magneticGeneration = -1;
    private static Set<Item> magneticItems = Set.of();

    public ElectromagnetBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ELECTROMAGNET.get(), pos, state);
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return FTuning.ELECTROMAGNET_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        field = ElectricRules.rate(volts, amps, wanted, ratedBand()) * condition;

        float excess = ElectricRules.overVoltage(volts, ratedBand());
        if (excess > 0f) {
            condition = Math.max(FTuning.ELECTROMAGNET_CONDITION_FLOOR,
                    condition - FTuning.ELECTROMAGNET_WEAR_PER_EXCESS * excess);
            Heat.warm(this, FTuning.ELECTROMAGNET_HEAT_PER_EXCESS * excess);
            setChanged();
        }
        Tu ambient = AmbientTable.at(level);
        Heat.tick(this, ambient, ambient, false);
    }

    /** 0 to 1: how hard it is pulling right now. Condition has already multiplied it. */
    public float getFieldStrength() {
        return field;
    }

    // --- what it does -------------------------------------------------------------------------

    public void tickServer() {
        if (field <= 0f || level == null || ++sinceLastPull < FTuning.ELECTROMAGNET_PULL_INTERVAL_TICKS)
            return;
        sinceLastPull = 0;

        double range = FTuning.ELECTROMAGNET_RANGE * field;
        Vec3 centre = Vec3.atCenterOf(worldPosition);
        AABB reach = new AABB(worldPosition).inflate(range);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, reach)) {
            if (!isMagnetic(item.getItem()))
                continue;
            Vec3 toward = centre.subtract(item.position());
            if (toward.lengthSqr() < 0.25 || toward.length() > range)
                continue;
            item.setDeltaMovement(item.getDeltaMovement()
                    .add(toward.normalize().scale(FTuning.ELECTROMAGNET_PULL * field)));
            item.hasImpulse = true;
        }
    }

    /** Whether a magnet pulls this stack: a shaped part by its {@code MadeOf}, anything else by its item. */
    public static boolean isMagnetic(net.minecraft.world.item.ItemStack stack) {
        io.github.soundgoodizerfan.feedback.process.MadeOf made = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
        if (made == null)
            return isMagnetic(stack.getItem());
        Material m = MaterialTable.get().materials().get(made.material());
        return m != null && m.separation().magnetic();
    }

    /** Whether a magnet pulls this item: its material says it is magnetic. */
    public static boolean isMagnetic(Item item) {
        if (magneticGeneration != MaterialTable.generation()) {
            magneticGeneration = MaterialTable.generation();
            Set<Item> found = new HashSet<>();
            for (Map.Entry<ResourceLocation, Material> entry : MaterialTable.get().materials().entrySet())
                if (entry.getValue().separation().magnetic())
                    entry.getValue().items(entry.getKey()).values().stream()
                            .filter(i -> !(i instanceof io.github.soundgoodizerfan.feedback.modular.ModularPartItem)).forEach(found::add);
            magneticItems = found;
        }
        return magneticItems.contains(item);
    }

    // --- Wearing ------------------------------------------------------------------------------

    @Override
    public float getCondition() {
        return condition;
    }

    /** A magnet that is pulling is working and cannot be measured, like any machine (philosophy 8). */
    @Override
    public boolean isRunning() {
        return field > 0f;
    }

    // --- ThermalBody --------------------------------------------------------------------------

    @Override
    public Tu getTemperature() {
        return new Tu(temperature);
    }

    @Override
    public void setTemperature(Tu tu) {
        temperature = tu.value();
    }

    @Override
    public ThermalMass getThermalMass() {
        return FTuning.ELECTROMAGNET_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.ELECTROMAGNET_LEAK;
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Temperature"))
            temperature = tag.getFloat("Temperature");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
